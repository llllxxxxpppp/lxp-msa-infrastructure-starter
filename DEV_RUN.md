# 로컬 실행 & 확인 가이드

백엔드(MSA + 인프라)와 프론트엔드(`frontend/`)를 처음부터 띄우고, 실제로 회원가입 → 로그인 → 강좌 조회까지 눈으로 확인하는 순서입니다. 각 항목의 배경 설명은 루트 `README.md`, `frontend/README.md`를 참고하세요. 여기서는 "무엇을 어떤 순서로 실행하고, 무엇으로 확인하는지"만 다룹니다.

전제: Docker Desktop, Node.js(프론트엔드용)가 설치되어 있어야 합니다. 백엔드를 IntelliJ로 직접 돌릴 게 아니라면 Java/Gradle은 따로 설치할 필요 없습니다(컨테이너 안에서 빌드됨).

---

## 1. 백엔드 실행

### 1-1. JWT_SECRET 준비

`gateway`(검증)와 `auth-service`(서명)가 같은 `JWT_SECRET`을 공유해야 기동합니다. 값이 없으면 두 서비스 모두 기동에 실패합니다.

**PowerShell**
```powershell
"JWT_SECRET=$(-join ((1..32) | ForEach-Object { '{0:x2}' -f (Get-Random -Maximum 256) }))" | Out-File -Encoding ascii -NoNewline .env
Get-Content .env
```

**macOS/Linux/Git Bash**
```bash
echo "JWT_SECRET=$(openssl rand -hex 32)" > .env
cat .env
```

리포 루트(`lxp-msa-infrastructure-starter/`)에 `.env` 파일이 생겼는지 확인하세요. `docker compose`가 이 파일을 자동으로 읽습니다.

### 1-2. 전체 스택 기동

```bash
docker compose up --build
```

처음 실행하면 각 서비스의 Gradle 의존성을 내려받느라 몇 분 걸릴 수 있습니다. 백그라운드로 띄우려면 `-d`를 붙이세요.

> ⚠️ Consul은 3노드(`consul-1/2/3`)가 모두 떠야 리더가 선출되고, 그래야 `config-server`와 나머지 서비스가 정상 기동합니다. `docker compose up`은 전체를 한 번에 띄우므로 이 부분은 신경 쓸 필요 없지만, 로그에 `config-server` 관련 서비스들이 계속 재시작하면 Consul 리더 선출을 기다리는 중일 수 있습니다(30초~1분 정도 소요).

### 1-3. 정상 기동 확인

각 서비스의 헬스체크 엔드포인트로 확인합니다.

| 서비스 | 확인 URL |
|---|---|
| gateway | http://localhost:8080/actuator/health |
| auth-service | http://localhost:8081/actuator/health |
| member-service | http://localhost:8082/actuator/health |
| course-service | http://localhost:8083/actuator/health |
| subscription-service | http://localhost:8084/actuator/health |
| Consul UI | http://localhost:8500 (모든 서비스가 Services 탭에 등록돼 있어야 함) |
| Config Server | http://localhost:8888/gateway/default |

브라우저 대신 터미널로 한 번에 확인하려면:

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8080/api/auth/ping
curl http://localhost:8080/api/members/ping
curl http://localhost:8080/api/courses/ping
```

`{"status":"UP"}` 또는 `{"service":"...","status":"UP",...}` 형태 응답이 오면 게이트웨이 라우팅까지 정상입니다.

### 1-4. 관측 도구 (선택)

| 도구 | 주소 |
|---|---|
| Grafana | http://localhost:3001 (admin/admin) — 3000은 프론트엔드용으로 비워둠 |
| Prometheus | http://localhost:9090 |
| Zipkin | http://localhost:9411 |

---

## 2. 프론트엔드 실행

새 터미널을 열어 진행합니다(백엔드는 계속 띄워둔 채로).

```bash
cd frontend
cp .env.local.example .env.local   # 기본값이 위 백엔드 설정과 맞으면 수정 불필요
npm install
npm run dev
```

브라우저에서 http://localhost:3000 접속 → 정상적으로 "LXP" 랜딩 페이지가 보이면 성공입니다.

---

## 3. End-to-End 확인 (회원가입 → 로그인 → 강좌 목록)

> ⚠️ **주의**: `member-service`의 `data.sql`에 시드된 계정(`admin@lxp.local`, `instructor@lxp.local`, `member@lxp.local` 등)은 비밀번호가 실제 로그인 가능한 값이 아닙니다(`auth-service`가 쓰는 `BCryptPasswordEncoder`로 검증 가능한 해시가 아님). 로그인을 확인하려면 **먼저 회원가입으로 새 계정을 만들어야** 합니다.

### 화면으로 확인

1. http://localhost:3000/signup 에서 이메일/비밀번호(6자 이상)로 회원가입 → 성공하면 로그인 페이지로 이동.
2. 방금 만든 계정으로 로그인 → 성공하면 `/courses`로 이동.
3. 강좌 목록이 보이면 성공입니다 (course-service의 시드 강좌 20개가 카테고리/난이도/학습시간과 함께 표시됩니다). 로그인 상태에서 새로고침해도 유지되는지, 로그아웃 버튼이 동작하는지도 함께 확인하세요.
4. `/subscriptions`에서 임의의 구독 ID(예: `1`)를 조회해봐도 됩니다. 시드된 구독이 없다면 404/에러 메시지가 뜨는 게 정상입니다 — API 연동 자체가 되는지만 보면 됩니다.

### curl로 확인 (프론트 없이 백엔드만 검증하고 싶을 때)

```bash
# 1) 회원가입
curl -X POST http://localhost:8080/api/members/signup \
  -H "Content-Type: application/json" \
  -d '{"email":"tester@example.com","password":"password123"}'

# 2) 로그인 (accessToken/refreshToken 발급)
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"tester@example.com","password":"password123"}'

# 3) 위에서 받은 accessToken으로 강좌 목록 조회
curl http://localhost:8080/api/courses \
  -H "Authorization: Bearer <위에서 받은 accessToken>"
```

세 번째 요청이 `courses` 배열을 반환하면, Gateway JWT 검증 → course-service 라우팅까지 전체 흐름이 정상입니다.

---

## 4. 종료 / 재시작

```bash
docker compose down          # 컨테이너 종료 (볼륨은 유지 → H2는 인메모리라 데이터는 어차피 초기화됨)
docker compose down -v       # 볼륨까지 삭제하고 싶을 때 (Grafana 대시보드 설정 등도 초기화됨)
```

프론트엔드는 `Ctrl+C`로 `next dev`를 종료하면 됩니다.

---

## 문제 해결

- **gateway/auth-service가 기동 실패**: `.env`에 `JWT_SECRET`이 있는지, 64자리 hex인지 확인하세요.
- **member/course/subscription-service가 계속 재시작**: RabbitMQ 헬스체크 대기 중일 수 있습니다. `docker compose ps`로 `rabbitmq`가 `healthy` 상태인지 확인하세요.
- **로그인이 계속 401**: 위 "주의" 항목대로 시드 계정이 아니라 직접 회원가입한 계정으로 시도하고 있는지 확인하세요.
- **CORS 에러가 프론트 콘솔에 뜸**: 프론트를 3000 포트가 아닌 다른 포트로 띄운 건 아닌지 확인하세요(`gateway`의 CORS 설정이 `http://localhost:3000`으로 고정돼 있습니다).
- 그 밖의 인프라(로그/Loki 등) 이슈는 `docs/troubleshooting/`을 참고하세요.
