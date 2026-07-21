# 트러블슈팅: Grafana에서 모든 로그가 단일 서비스로 조회되는 문제

- **관련 이슈**: #36
- **관련 PR**: #37
- **영역**: 공통 인프라 (Grafana / Loki / Alloy)

## 1. 증상

Grafana Explore에서 Loki 로그를 조회할 때, 모든 Docker 컨테이너의 로그가
`service_name = lxp-services` **단일 라벨**로 묶여 서비스별 조회가 불가능했다.

- 기대: `service_name`으로 gateway / auth-service / member-service 등 서비스별 필터링
- 실제: `service_name`이 `lxp-services` 하나뿐이라 6개 서비스 로그가 한데 섞여 조회됨

## 2. 원인 분석

### 진단

Loki API로 라벨 상태를 직접 확인했다.

```bash
# service_name 값 목록
curl -s "http://localhost:3100/loki/api/v1/label/service_name/values"
# → {"data":["lxp-services"]}         ← 값이 하나뿐

# 전체 라벨 목록
curl -s "http://localhost:3100/loki/api/v1/labels"
# → {"data":["filename","job","service_name"]}

# filename 라벨은 서비스별로 정상 분리되어 있었음
curl -s "http://localhost:3100/loki/api/v1/label/filename/values"
# → [".../auth-service.log", ".../gateway.log", ...]  (6개)
```

즉, 데이터 자체는 `filename`으로 서비스가 구분되어 있는데, 정작 조회에 쓰이는
`service_name`만 하나로 뭉쳐 있었다.

### 근본 원인

로그 수집기 Alloy 설정(`infrastructure/alloy/config.alloy`)이 모든 로그 파일에
정적 라벨 `job = "lxp-services"` 하나만 부여하고 있었다.

```alloy
local.file_match "lxp_file_logs" {
  path_targets = [
    { __path__ = "/var/log/lxp/*.log", job = "lxp-services" },  // 6개 파일 전부 동일 라벨
  ]
}
```

여기에 Loki 3.x의 `discover_service_name` 동작이 겹쳤다. Loki는 로그에
`service_name` 라벨이 없으면, 정해진 우선순위 라벨(`service_name` → `service` →
`app` → ... → `job`)을 순서대로 훑어 첫 번째 값을 `service_name`으로 자동 승격시킨다.
우리 데이터에는 이 목록 중 `job`만 존재했으므로, `job`의 값 `lxp-services`가 그대로
`service_name`이 되어 모든 서비스가 하나로 묶였다.

> 핵심: `config.alloy`에는 `service_name`이라는 단어가 아예 없었다. 원인은 설정 파일이
> 아니라 **Loki의 라벨 자동 승격 규칙**에 있었기 때문에 증상만으로는 추적이 어렵다.
> `filename`은 자동 승격 후보 목록에 포함되지 않아 `service_name`으로 쓰이지 못했다.

## 3. 해결책 비교

서비스별 라벨링을 구현하는 방식은 크게 두 가지다.

|         | 파일 기반 (채택)                                  | Docker 네이티브 (미채택)                            |
|---------|---------------------------------------------|----------------------------------------------|
| 로그 출처   | 서비스가 파일로 쓴 로그 (`infrastructure/logs/*.log`) | 컨테이너 stdout/stderr (도커 소켓 경유)                |
| 컴포넌트    | `local.file_match` + `loki.source.file`     | `discovery.docker` + `loki.source.docker`    |
| 서비스 구분  | 파일명에서 추출 (`gateway.log` → `gateway`)        | 컨테이너 메타데이터 (`__meta_docker_container_name`)  |
| 추가 요구사항 | 로그 폴더 볼륨 마운트 (기존에 이미 존재)                    | 도커 소켓(`/var/run/docker.sock`) 마운트 필요         |
| 장점      | 변경 최소, 기존 인프라와 정합, 도커 소켓 노출 없음              | 파일 로깅 불필요, 라벨이 견고, 로테이션 관리 부담 없음             |
| 단점      | 파일명 규칙에 의존                                  | 도커 소켓 노출(호스트 root급 접근) 보안 리스크, compose 변경 동반 |

### 선택 기준과 결정

**최소 변경과 기존 인프라와의 정합을 최우선 기준으로 삼아 파일 기반을 채택했다.**

- 이 저장소는 이미 각 서비스가 파일로 로그를 남기고(`logging.file.name`) 그 폴더를
  볼륨 마운트하는 구조를 갖추고 있어, 파일 기반이 기존 구조와 자연스럽게 맞물린다.
- Docker 네이티브는 도커 소켓 노출(보안 검토 필요) + `compose.yaml`/`compose.infra.yaml`
  수정 + 기존 파일 로깅 정리가 동반되는 **아키텍처 변경**이라 이슈 #36의 버그 수정
  범위를 넘어선다.

## 4. 구현 결과

`config.alloy`에 `discovery.relabel` 단계를 추가해, 파일 경로에서 서비스명을 추출하여 `service_name` 라벨로 명시적으로 부여했다.

```alloy
discovery.relabel "lxp_file_logs" {
  targets = local.file_match.lxp_file_logs.targets

  rule {
    source_labels = ["__path__"]
    regex         = ".*/(.+)\\.log"   // /var/log/lxp/gateway.log → gateway
    target_label  = "service_name"
  }
}

loki.source.file "lxp_file_logs" {
  targets    = discovery.relabel.lxp_file_logs.output
  forward_to = [loki.write.default.receiver]
}
```

`filename` 라벨에 경로가 이미 있었지만 Loki의 `service_name` 자동 승격 대상이 아니므로,
`service_name`을 직접 부여하는 방식으로 해결했다.

### 검증

```bash
# 1) service_name이 서비스별로 분리되었는지
curl -s "http://localhost:3100/loki/api/v1/label/service_name/values"
# → auth-service, config-server, course-service, gateway,
#    member-service, subscription-service (+ 과거 lxp-services 잔재)

# 2) 특정 서비스에 요청을 보내 로그 발생시킨 뒤, 그 로그가 해당 service_name에만 잡히는지
curl -X POST http://localhost:8082/api/admin/members/instructors \
  -H "Content-Type: application/json" -d '{"bad":"data"}'
```

- `{service_name="member-service"}` 조회 → 방금 발생한 검증 에러 로그 3건만 매칭
- 전체 서비스 대상 교차 검색 → 해당 로그가 member-service에만 존재, gateway 등
  타 서비스에는 나타나지 않음을 확인

> 참고: 수정 이전에 저장된 과거 로그는 `service_name = lxp-services` 라벨로 남아 있다.
> retention 만료 또는 `loki-data` 볼륨 초기화 시 사라지며, 기능상 문제는 없다.

## 5. 남은 과제

- **성능**: [Alloy 공식 문서](https://grafana.com/docs/alloy/latest/reference/components/local/local.file_match/)는
  성능 관련해 다음을 언급한다.
    - **별도 컴포넌트 오버헤드**: 가능하면 `loki.source.file`에 내장된 `file_match`를 쓰는 편이
      성능상 낫고, 이 이점은 **감시 파일이 많을수록 커진다**. `local.file_match`는 타깃을 여러
      컴포넌트가 공유하거나 `discovery.relabel` 등 다른 탐색 컴포넌트로부터 타깃을 받아야 할 때
      쓰라고 권한다.
    - **`sync_period`(기본 10초)**: 이 주기마다 파일시스템을 재스캔한다. 짧을수록, 파일이 많을수록
      스캔 부하가 커진다.
    - **`ignore_older_than`(기본 0s)**: 기본값에서는 오래된 파일도 매번 대상에 포함된다. 값을 주면
      오래된 파일을 건너뛰어 처리량을 줄일 수 있다.

  현재 우리는 감시 파일이 6개뿐이라 위 비용이 모두 무시할 수준이다. 또한 `service_name` 추출을
  위해 `discovery.relabel`로 타깃을 넘겨야 하므로, 문서가 명시한 "`local.file_match`를 써야 하는
  예외 상황"에 정확히 해당한다 — 즉 **현재 구조는 성능 권고에 어긋나지 않는다**. 다만 향후 파일 수가
  크게 늘면 `sync_period` 조정이나 파일 탐색 통합을 재검토할 수 있다. (문서는 CPU/IO 수치·벤치마크는
  제공하지 않으며, 팀에서 별도 확인 예정)
- **Docker 네이티브 전환**: `discovery.docker` 기반 수집은 라벨 견고성 면에서 이점이
  있으나 **도커 소켓(`/var/run/docker.sock`) 노출 보안 검토가 선행**되어야 한다. 전환한다면
  소켓 프록시로 조회 전용 API만 여는 것이 사실상 필수다. 상세한 위험 분석·검증·대응은
  [부록 A](#부록-a-도커-소켓-노출-보안-분석) 참고. 별도 이슈로 논의 대상.

---

## 부록 A. 도커 소켓 노출 보안 분석

Docker 네이티브 방식(`discovery.docker` + `loki.source.docker`)은 Alloy 컨테이너에
도커 소켓을 마운트해야 한다. 이 소켓 노출의 위험을 세 가지로 정리한다.

### A-1. 왜 보안 리스크인가 — "호스트 root급 접근"

- `/var/run/docker.sock`은 **Docker 데몬의 REST API 엔드포인트**다. `docker` CLI의 모든
  동작(컨테이너 생성/삭제/실행)이 이 소켓을 통한 API 호출이다.
- 데몬은 보통 **root로 실행되고 소켓에는 인증이 없다**. 소켓에 접근할 수 있으면 데몬에게
  무엇이든 시킬 수 있다.
- 취약점: API로 새 컨테이너를 만들 때 마운트·권한을 자유롭게 지정할 수 있다. 소켓 접근
  권한이 있는 컨테이너는 `-v /:/host`로 호스트 루트 전체를 마운트하거나 `--privileged`,
  `--pid=host` 등으로 호스트 커널·프로세스에 접근하는 컨테이너를 띄울 수 있다.
- 결과: 컨테이너 하나가 뚫리면(예: Alloy RCE) → 소켓으로 호스트 루트 마운트 컨테이너 생성
  → 호스트 전체 장악 가능. 그래서 "소켓 마운트 = 컨테이너에 사실상 root 부여"라고 한다.
- **오해 주의**: `:ro`(읽기 전용) 마운트는 소켓 파일 자체 변경만 막을 뿐, 그 소켓으로 보내는
  API 요청(컨테이너 생성 등 쓰기 동작)은 그대로 통한다. 즉 `:ro`는 방어가 되지 못한다.

### A-2. 시나리오 기반 검증 (로컬, 읽기 전용·비파괴)

먼저 현재 인프라는 소켓을 노출하지 않음을 확인했다.

```bash
grep -rn "docker.sock" compose.yaml compose.infra.yaml
# → 없음. 파일 기반 방식은 소켓을 노출하지 않음
```

**시나리오 A — 소켓 마운트 컨테이너는 인증 없이 데몬 전체를 조종**

```bash
docker run --rm -v /var/run/docker.sock:/var/run/docker.sock docker:cli docker ps
# → 컨테이너 '내부'에서 호스트의 실행 컨테이너 목록을 자격증명 없이 조회 성공
```

**시나리오 B — 소켓 접근 → 호스트 파일시스템 전체 장악 (권한 상승)**

```bash
docker run --rm -v /var/run/docker.sock:/var/run/docker.sock docker:cli \
  docker run --rm -v /:/host alpine sh -c 'ls /host; cat /host/etc/hostname; ls -l /host/etc/shadow'
# → 호스트 루트(/)가 /host로 마운트됨. /etc/shadow(비밀번호 해시)까지 접근 가능함을 확인
```

> **환경 차이(중요)**: macOS Docker Desktop에서 "호스트"는 Docker Desktop이 돌리는 내부
> 리눅스 VM(위 검증에서 hostname `debuerreotype`)이라 체감 위험이 덜해 보인다. 그러나
> **리눅스 운영 서버에서는 소켓이 진짜 호스트에 직결**되므로 그대로 **실서버 root 장악**이
> 되어 심각도가 최대다.

### A-3. 실무 Best Practice (우선순위 순)

1. **아예 마운트하지 않기 (최선)** — 소켓이 필요 없는 설계를 택한다. 로그 수집이라면 파일 기반
   방식이나 노드 에이전트 방식. **우리 결정(파일 기반)이 이 원칙에 부합**한다.
2. **꼭 필요하면 소켓 프록시** — [`tecnativa/docker-socket-proxy`](https://github.com/Tecnativa/docker-socket-proxy)
   로 허용 API 엔드포인트를 화이트리스트한다. 예: 컨테이너 목록 조회만 열고 `POST /containers/create`
   ·`EXEC` 등 쓰기 API는 차단 → 시나리오 B(권한 상승)가 막힌다.
3. **Rootless Docker / Podman** — 데몬을 비-root로 실행하면 소켓이 뚫려도 권한이 제한된다.
4. **원격 접근은 TLS + mTLS** — TCP로 열어야 하면 반드시 클라이언트 인증서 상호 인증. 평문·무인증
   (`tcp://0.0.0.0:2375`)은 금지.
5. **그래도 마운트한다면** — `:ro`는 소켓에 무효(프록시가 정답), `--security-opt no-new-privileges`
   ·최소 권한·신뢰된 이미지 핀 고정·취약점 스캔·접근 감사 로깅.
6. **Alloy 맥락** — 도커 로그를 수집해야 하면 Alloy에 소켓을 직접 물리지 말고 **소켓 프록시
   경유 + 조회 전용 권한**으로 붙이는 것이 표준 패턴이다.

**결론**: 이 검증은 파일 기반 선택을 뒷받침한다. 소켓을 노출하지 않으므로 위 공격면 자체가
없다(Best Practice 1). Docker 네이티브로 전환한다면 **소켓 프록시(2)가 사실상 필수 전제**다.
