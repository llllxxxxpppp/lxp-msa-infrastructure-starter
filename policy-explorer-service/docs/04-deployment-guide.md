# 04. 배포 가이드 (Docker Compose)

> 🔄 **이식 반영 — 이 문서는 원본을 대체했습니다**
> PoC 리포의 `docs/04`는 "예시 초안"(`docs/docker/` 폴더의 Dockerfile 2종 + docker-compose
> 초안)이었고, "환경변수 리팩터링 전까지는 참고 자료로만 쓰라"는 단서가 붙어 있었습니다.
> 이식하면서 그 리팩터링이 완료되고 실제 [`Dockerfile`](../Dockerfile)과 리포 루트
> `compose.yaml`이 만들어졌으므로, 이 문서는 **실제 구성**을 설명합니다.
> 원본 초안은 PoC 리포 `docs/docker/`에 그대로 있습니다.

## 컨테이너 분리 전략

```text
┌─────────────────────────────────────────┐
│ policy-explorer-service 컨테이너 :8086    │   GPU 불필요
│  FastAPI + LangGraph + Chroma + BM25     │
└────────────────┬────────────────────────┘
                 │ HTTP  ${OLLAMA_BASE_URL}
                 │   /api/chat   (생성)
                 │   /api/embed  (임베딩)
                 ▼
        ┌──────────────────────┐
        │ Ollama :11434         │   GPU 필요
        │  qwen2.5:7b, bge-m3   │
        └──────────────────────┘
```

- **이 서비스 컨테이너는 GPU가 필요 없습니다.** 추론과 임베딩 모두 Ollama가 담당합니다.
  (원본은 임베딩만 컨테이너 안에서 torch로 계산했는데, 이식 시 그것도 Ollama로 넘겼습니다.)
- **Consul에는 등록하고 config-server는 쓰지 않습니다.** 등록은 gateway가
  `lb://policy-explorer-service`로 찾게 하려는 것이며, Java 서비스와 라우팅 방식을 통일합니다.
  등록은 [`app/consul.py`](../app/consul.py)가 Consul HTTP API를 직접 호출해 수행하며 새
  의존성이 없습니다.
  config-server는 Spring 형식 YAML을 서빙하고 이 서비스의 설정은 환경변수 10여 개뿐이라
  실익이 적어 쓰지 않습니다.

## Ollama를 compose에 두지 않은 이유 ⭐

`compose.yaml`에 `ollama` 서비스가 **없습니다.** 호스트에서 실행 중인 Ollama에 붙습니다.

| 구성 | macOS에서 | 이유 |
|---|---|---|
| Ollama를 compose 컨테이너로 | **CPU 추론** | Docker Desktop은 리눅스 VM 안에서 돌고, Apple Metal GPU는 그 VM으로 전달되지 않는다 |
| Ollama를 호스트에서 (현재) | **GPU 100%** | `ollama ps` 실측: `qwen2.5:7b / 4.6GB / 100% GPU` |

실측 비교: 규정 팩트 추출(Node 1) 한 번이 호스트 Ollama에서 **약 7초**입니다. 컨테이너 CPU
추론이면 같은 작업이 수 분 단위가 됩니다. 이 파이프라인은 요청당 LLM을 여러 번
(팩트 추출 1회 + 검색 결과 개수만큼 충돌 분석 N회) 호출하므로 그 차이가 곱해집니다.

컨테이너에서 호스트로 나가는 통로는 compose의 두 줄이 만듭니다.

```yaml
    environment:
      OLLAMA_BASE_URL: ${OLLAMA_BASE_URL:-http://host.docker.internal:11434}
    extra_hosts:
      - "host.docker.internal:host-gateway"
```

### 리눅스 + NVIDIA 서버로 옮길 때

**코드도 `compose.yaml`도 고치지 않습니다.** 리포 루트 `.env`에 한 줄 추가하고, `ollama`
서비스를 추가하면 됩니다.

```dotenv
OLLAMA_BASE_URL=http://ollama:11434
```

```yaml
  ollama:
    image: ollama/ollama:latest
    volumes: [ollama-models:/root/.ollama]
    networks: [lxp-net]
    deploy:
      resources:
        reservations:
          devices:
            - driver: nvidia
              count: all
              capabilities: [gpu]
```
그 뒤 컨테이너 안에 모델을 받습니다.
```bash
docker compose exec ollama ollama pull qwen2.5:7b
docker compose exec ollama ollama pull bge-m3
```

## 볼륨

| 볼륨 | 컨테이너 경로 | 용도 | 없으면 |
|---|---|---|---|
| `policy-explorer-state` | `/data/state/chroma` | Chroma 벡터 데이터 | **재기동 시 업로드 문서 전부 유실** |
| `policy-explorer-uploads` | `/data/uploads` | 업로드 원본 파일 | 원본 파일 유실 |

두 볼륨을 나눈 이유는 [09-data-architecture.md](09-data-architecture.md)의 설계
(상태 DB는 인스턴스 로컬, 원본 파일은 공유 가능해야 함)를 따른 것입니다. 다만 원본 파일 쪽은
아직 NFS가 아니라 로컬 named volume입니다.

기동 시 `restore_from_persisted()`가 Chroma에서 청크를 읽어 **BM25 인메모리 인덱스를 다시
세웁니다.** BM25는 디스크에 저장되지 않으므로 이 복원이 없으면 키워드 검색이 죽습니다.

## 이미지 빌드

빌드 컨텍스트는 **리포 루트**입니다(Java 서비스들과 동일한 패턴).

```bash
# compose 경유 (권장)
docker compose build policy-explorer-service

# 단독 빌드
docker build -f policy-explorer-service/Dockerfile -t policy-explorer-service:dev .
```

| 항목 | 실측 |
|---|---|
| 첫 빌드 | 37.6초 |
| 코드만 수정 후 재빌드 | **0.73초** (의존성 레이어 CACHED) |
| 이미지 크기 | 797MB (의존성 408MB + 베이스 389MB) |
| 빌드 컨텍스트 전송량 | 534B |

의존성 명세(`pyproject.toml`/`uv.lock`)를 코드보다 먼저 복사해 레이어 캐시가 듣게 했습니다.
루트 `.dockerignore`에 Python 산출물을 넣지 않으면 `.venv`(360MB)가 컨텍스트로 전송됩니다.

## 기동

```bash
docker compose up -d --build policy-explorer-service
docker compose logs -f policy-explorer-service
```

`healthcheck`는 slim 베이스 이미지에 `curl`이 없어 Python `urllib`을 씁니다.

```yaml
    healthcheck:
      test: ["CMD", "/app/.venv/bin/python", "-c",
             "import urllib.request; urllib.request.urlopen('http://localhost:8086/health')"]
      interval: 10s
      timeout: 5s
      retries: 20
      start_period: 30s
```

### `gateway`의 `depends_on`에 넣지 않았습니다

`ai-bot-service`(PR #61)는 `gateway`에 `depends_on: ai-bot-service: service_healthy`를
걸었습니다. 그러면 AI 서비스가 안 뜰 때 **gateway 자체가 기동하지 않아 모든 서비스가 외부에서
접근 불가**가 됩니다. 라우팅이 정적 URI라 기동 순서 의존이 없으므로 이 결합을 만들지 않았습니다.

## 포트

| 서비스 | 포트 | 비고 |
|---|---|---|
| `policy-explorer-service` | 8086 | 8085는 `infrastructure/prometheus/prometheus-docker.yml`에 `payment-service`로 예약됨 |
| Consul (등록 대상) | 8500 | `CONSUL_HOST` 미설정 시 등록을 건너뛴다 |
| Ollama (호스트) | 11434 | compose가 관리하지 않음 |

## 다음 문서
- 서버 사양 산정 → [05-runtime-requirements.md](05-runtime-requirements.md)
- 운영/장애 대응 → [07-operations-runbook.md](07-operations-runbook.md)
- 남은 작업 → [08-migration-checklist.md](08-migration-checklist.md)
