# LXP MSA Infrastructure Starter

기존 모놀리식 도메인 코드를 옮기기 전에, **MSA 인프라와 독립 서버 골격부터 실행할 수 있도록 만든 프로젝트**입니다.

## 서버 구조

```text
Client
  ↓
Public API Gateway
  ├─ auth-service
  ├─ member-service
  ├─ course-service
  └─ subscription-service   # 구독 + 결제 (payment 도메인 임시 통합)

AI 서비스
  └─ curriculum-service     # 맞춤형 커리큘럼 설계, 현재 직접 접근

공통 인프라
  ├─ config-server
  ├─ Consul 3-node
  ├─ Prometheus : CPU, JVM, HTTP 요청 수 같은 메트릭 수집
  ├─ Grafana : 메트릭과 로그를 시각화
  ├─ Loki : 로그 저장
  ├─ Alloy : 서비스 로그를 Loki로 전달
  └─ Zipkin : 분산 트레이싱 저장/조회
```

Spring 기반 도메인 서비스와 FastAPI 기반 봇 서비스(`ai-tutor-service`, `curriculum-service`)는 모두 Gateway를 단일 진입점으로 두고 경로 기반으로 라우팅합니다. `config-server`와 Consul 등 공통 인프라는 Gateway에 노출하지 않고 서비스가 직접 사용합니다.

> 구독(subscription)과 결제(payment) 도메인은 현재 결합도가 높아 `subscription-service` 하나로 통합해 운영합니다. `/api/subscriptions/**`와
`/api/payments/**` 모두 이 서비스가 서빙하며, 도메인 경계가 명확해지면 별도 서비스로 분리할 예정입니다.

## 프로젝트 구성

이 프로젝트는 **하나의 저장소 안에 서비스별 하위 프로젝트를 두는 모노레포**입니다. Spring 기반 서비스는 자체 `settings.gradle`, `build.gradle`, Gradle Wrapper(`gradlew`)를 사용하고, Python 기반 `curriculum-service`는 `pyproject.toml`과 `uv.lock`을 사용합니다. 각 서비스는 자체 `Dockerfile`을 가지며 독립적으로 빌드·실행됩니다. **루트에는 빌드 설정을 두지 않습니다.**

```text
lxp-msa-infrastructure-starter
├─ gateway
├─ config-server
├─ auth-service
├─ member-service
├─ course-service
├─ curriculum-service      # 맞춤형 커리큘럼 설계 AI 서비스
├─ subscription-service
├─ frontend               # Next.js 프론트엔드 (frontend/README.md 참고)
├─ config-repo            # config-server가 서빙하는 설정 파일
├─ infrastructure         # prometheus·grafana·loki·alloy 설정, 로그
├─ compose.infra.yaml     # 공통 인프라(consul·관측성)만 실행
└─ compose.yaml           # 전체 스택(인프라 + 모든 서비스) 실행
```

서비스별 빌드 설정(의존성, 포트 등)은 각 하위 폴더의 `build.gradle`에 있습니다. `frontend`만 예외로 Node/npm 기반이며, 자체 `package.json`으로 독립 실행됩니다.

> `localhost:3000`은 `frontend` 개발 서버(`npm run dev`) 몫으로 비워뒀습니다. Gateway의 CORS 설정(`gateway/src/main/java/com/lcs/gateway/config/CorsConfig.java`)이 이미 이 origin을 전제하고 있어, 대신 Grafana를 3001로 옮겼습니다(아래 표 참고).


## 사용 버전

- Java 17
- Python 3.12
- Spring Boot 3.5.15
- Spring Cloud 2025.0.3
- Consul 1.18
- Ollama 0.32.9
- Prometheus 3.1.0

## 실행 방법

Spring 기반 도메인 서비스는 **Consul(서비스 디스커버리)** 과 **config-server(중앙 설정)** 에 의존합니다. `curriculum-service`는 Course Service와 Ollama에 의존합니다.

> ⚠️ Consul은 `bootstrap-expect=3` 구성이라 **consul-1·2·3 세 노드가 모두** 떠야 리더가 선출됩니다. 하나만 띄우면 서비스가 config-server를 기다리며 멈춥니다.

### 필수 환경 변수: `JWT_SECRET`

`gateway`(토큰 검증)와 `auth-service`(토큰 서명)는 **동일한 `JWT_SECRET`** 이 있어야 기동합니다. 기본값을 두지 않으므로 값이 없으면 기동에 실패합니다(fail-fast).
HS512 서명이라 **64바이트(= hex 64자) 이상**이어야 합니다.

루트에 `.env` 파일을 만들어 값을 넣으면 `docker compose`가 자동으로 각 서비스에 주입합니다. `.env`는 git에 커밋되지 않습니다(`.gitignore` 등록됨).

**macOS / Linux**

```bash
echo "JWT_SECRET=$(openssl rand -hex 32)" > .env
cat .env   # 값 확인 (hex 64자)
```

**Windows (PowerShell)**

```powershell
"JWT_SECRET=$(-join ((1..32) | ForEach-Object { '{0:x2}' -f (Get-Random -Maximum 256) }))" | Out-File -Encoding ascii -NoNewline .env
Get-Content .env   # 값 확인 (hex 64자)
```

> Windows에서 Git Bash를 쓴다면 macOS/Linux 방식의 `openssl` 명령을 그대로 사용해도 됩니다.

- **IntelliJ로 서비스만 실행**할 때는 `.env` 대신 각 서비스 Run Configuration의 Environment variables에 `JWT_SECRET`을 직접 추가합니다.
- 테스트(`gradlew test`)는 별도 설정이 필요 없습니다(빌드 설정에서 테스트 전용 값 주입).

### 전체 실행

모든 서비스 + 인프라를 Docker로 한 번에 빌드·실행합니다.

```bash
docker compose up --build # 포그라운드
```

```bash
docker compose up --build -d # 백그라운드 
```

처음 실행할 때는 각 프로젝트의 Gradle·Python 의존성과 Ollama의 채팅 및 임베딩 모델을 내려받기 때문에 시간이 걸릴 수 있습니다. Ollama 모델은 `ollama-model-init` 컨테이너가 자동으로 준비합니다.

### 특정 서비스만 개발 / 실행

특정 서비스 하나만 개발할 때 사용합니다. 서비스와 공통 의존성(Consul 3노드 + config-server)을 함께 띄웁니다.

**방법 A.도커로 실행**

```bash
# 예: auth-service 개발 중인 경우
docker compose up --build consul-1 consul-2 consul-3 config-server auth-service
```

여기에도 `-d`를 붙이면 백그라운드로 실행됩니다. (종료는 `docker compose down`)

관측성(Prometheus/Grafana/Zipkin 등)까지 필요하면 목록에 추가합니다.

```bash
docker compose up --build \
  consul-1 consul-2 consul-3 \
  prometheus grafana loki alloy zipkin \
  config-server auth-service
```

Curriculum Service만 확인할 때는 Course Service의 공통 의존성과 Ollama를 함께 실행합니다.

```bash
docker compose up --build \
  consul-1 consul-2 consul-3 config-server rabbitmq \
  course-service ollama ollama-model-init curriculum-service
```

기본적으로 Course Service의 강좌 데이터를 사용합니다. 개발 및 테스트 목적으로
`curriculum-service/tests/fixtures/courses.json`을 사용하려면 `PROVIDER=json`을
설정한 뒤 실행합니다.

**macOS / Linux**

```bash
PROVIDER=json docker compose up --build curriculum-service
```

**Windows (PowerShell)**

```powershell
$env:PROVIDER = "json"
docker compose up --build curriculum-service
```

**방법 B. 서비스만 IntelliJ에서 실행**

코드를 자주 고치는 개발 중에는 IDE로 돌리는 편이 재시작이 빨라 편합니다.

1. Consul·관측성을 도커로 기동합니다.

   ```bash
   docker compose -f compose.infra.yaml up -d
   ```

2. IntelliJ에서 `config-server`와 서비스 폴더를 Gradle 프로젝트로 엽니다. (Gradle JVM: Java 17)

3. `gateway`·`auth-service`를 실행한다면, 각 Run Configuration의 Environment variables에 `JWT_SECRET`을 추가합니다. IntelliJ는 `.env`를 자동으로 읽지 않으므로 직접 넣어야 하며, 두 서비스는 **같은 값**을 써야 합니다. (→ 위 `필수 환경 변수: JWT_SECRET` 참고)

4. `ConfigServerApplication`을 먼저 실행한 뒤, 서비스의 Application 클래스를 실행합니다.

> `compose.infra.yaml`에는 **config-server가 없습니다.** 모든 서비스의 공통 의존성이므로 IntelliJ에서 직접 실행해야 합니다.
> 전체를 IDE에서 띄운다면 `config-server → auth → member → course → subscription → gateway` 순서를 권장합니다.

## 확인 엔드포인트

**서비스** — 서비스는 포트로 직접 확인하고, Gateway 경유 라우팅까지 보려면 실행 목록에 `gateway`를 함께 띄웁니다.

| 담당                   | 직접 확인                                 | Gateway 경유                                |
|----------------------|---------------------------------------|-------------------------------------------|
| gateway              | http://localhost:8080/actuator/health | -                                         |
| auth-service         | http://localhost:8081/actuator/health | http://localhost:8080/api/auth/ping       |
| member-service       | http://localhost:8082/actuator/health | http://localhost:8080/api/members/ping    |
| course-service       | http://localhost:8083/actuator/health | http://localhost:8080/api/courses/ping    |
| curriculum-service   | -                                     | -                                         |
| subscription-service | http://localhost:8084/actuator/health | http://localhost:8080/api/subscriptions/1 |

> `subscription-service`는 payment 도메인도 서빙합니다: http://localhost:8080/api/payments/subscriptions/1

`curriculum-service`는 호스트 포트를 게시하지 않습니다. 상태는 `docker compose ps`의 healthy 표시로 확인하고, 컨테이너 안에서 직접 볼 때는 `docker compose exec curriculum-service curl -s localhost:8000/health`를 사용합니다. Gateway 경유 경로는 `/api/curriculum/**`이며 로그인 토큰이 필요합니다.

Curriculum Service의 API 문서는 단독 실행(`uv run uvicorn app.main:app --reload`) 시 http://localhost:8000/docs 에서 확인할 수 있습니다.

**공통 인프라**

| 대상             | 주소                                    |
|----------------|---------------------------------------|
| Config Server  | http://localhost:8888/gateway/default |
| Consul UI      | http://localhost:8500                 |
| Prometheus     | http://localhost:9090                 |
| Grafana        | http://localhost:3001 (admin / admin) |
| Loki readiness | http://localhost:3100/ready           |
| Zipkin         | http://localhost:9411                 |

## 현재 구현 범위

포함:

- Gateway 라우팅
- Config Server Native backend
- Consul 서비스 등록 및 탐색
- Actuator / Prometheus metrics
- Grafana 데이터소스 자동 설정
- Loki + Alloy 로그 수집
- Zipkin 분산 트레이싱
- 각 서비스의 최소 테스트 API

미포함:

- gRPC 구현
- Kafka 또는 RabbitMQ
