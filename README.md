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
  ├─ subscription-service
  └─ payment-service

공통 인프라
  ├─ config-server
  ├─ Consul 3-node
  ├─ Prometheus : CPU, JVM, HTTP 요청 수 같은 메트릭 수집
  ├─ Grafana : 메트릭과 로그를 시각화
  ├─ Loki : 로그 저장
  └─ Promtail : 서비스 로그를 Loki로 전달
```

모든 도메인 서비스는 Gateway를 단일 진입점으로 두고 경로 기반으로 라우팅합니다. `config-server`와 Consul 등 공통 인프라는 Gateway에 노출하지 않고 서비스가 직접 사용합니다.

## 프로젝트 구성

각 하위 폴더는 자체 `settings.gradle`, `build.gradle`, Gradle Wrapper(`gradlew`), `Dockerfile`, `src`를 가진 완전히 독립된 프로젝트입니다. 루트에는 빌드 설정을 두지 않습니다.

```text
lxp-msa-infrastructure-starter
├─ gateway
├─ config-server
├─ auth-service
├─ member-service
├─ course-service
├─ subscription-service
├─ payment-service
├─ config-repo
├─ infrastructure
├─ docker-compose.infra.yml
└─ docker-compose.yml
```

## 사용 버전

- Java 17
- Spring Boot 3.5.15
- Spring Cloud 2025.0.3
- Consul 1.18
- Prometheus 3.1.0

## IntelliJ에서 실행

### 1. 프로젝트 열기

각 서비스는 독립 프로젝트이므로, IntelliJ에서 각 서비스 폴더(`gateway`, `auth-service` 등)를 Gradle 프로젝트로 각각 열거나 임포트합니다. Gradle JVM은 Java 17로 설정합니다.

### 2. 공통 인프라 실행

Docker Desktop을 실행한 후 루트 터미널에서 다음 명령을 실행합니다.

```bash
docker compose -f docker-compose.infra.yml up -d
```

### 3. IntelliJ 실행 순서

각 프로젝트의 Application 클래스를 다음 순서로 실행합니다.

```text
1. ConfigServerApplication
2. AuthServiceApplication
3. MemberServiceApplication
4. CourseServiceApplication
5. SubscriptionServiceApplication
6. PaymentServiceApplication
7. GatewayApplication
```

### 4. 확인 주소

| 대상 | 주소 |
|---|---|
| Gateway Health | http://localhost:8080/actuator/health |
| Auth via Gateway | http://localhost:8080/api/auth/ping |
| Member via Gateway | http://localhost:8080/api/members/ping |
| Course via Gateway | http://localhost:8080/api/courses/ping |
| Subscription via Gateway | http://localhost:8080/api/subscriptions/1 |
| Payment via Gateway | http://localhost:8080/api/payments/subscriptions/1 |
| Config Server | http://localhost:8888/gateway/default |
| Consul UI | http://localhost:8500 |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3000 |
| Loki readiness | http://localhost:3100/ready |

Grafana 기본 계정은 `admin / admin`입니다.

## 전체 Docker 실행

모든 Spring Boot 프로젝트까지 Docker로 빌드하고 실행하려면:

```bash
docker compose up --build
```

처음 실행할 때는 각 프로젝트의 Gradle 의존성을 내려받기 때문에 시간이 걸릴 수 있습니다.

## 현재 구현 범위

포함:

- Gateway 라우팅
- Config Server Native backend
- Consul 서비스 등록 및 탐색
- Actuator / Prometheus metrics
- Grafana 데이터소스 자동 설정
- Loki + Promtail 로그 수집
- 각 서비스의 최소 테스트 API

미포함:

- 기존 모놀리식 도메인 코드
- JWT 실제 검증
- 서비스별 DB
- gRPC 구현
- Kafka 또는 RabbitMQ
- Zipkin
- 실제 결제 로직

## 저장소 구조

이 프로젝트는 **하나의 저장소 안에 서비스별 하위 프로젝트를 두는 모노레포**로 운영합니다.

**루트에는 빌드 설정이 없습니다.** 각 서비스 폴더가 자체 `settings.gradle` + `build.gradle` + Gradle Wrapper(`gradlew`)를 가진 완전히 독립된 빌드이며, 각자 자기 폴더 안에서 단독으로 빌드·실행됩니다.

- 서비스별 빌드 설정(의존성, 포트 등)은 각 하위 폴더의 `build.gradle`에 있습니다.
- 특정 서비스만 빌드/실행: `cd <service> && ./gradlew bootRun`
- 특정 서비스만 도커로 실행: `docker compose up --build <service>`
- 전체 실행: `docker compose up --build` (아래 참고)
