# LXP MSA Architecture

```text
Client
  ↓
Public API Gateway :8080
  ├─ auth-service :8081
  ├─ member-service :8082
  ├─ course-service :8083
  ├─ subscription-service :8084
  └─ payment-service :8085

Shared Infrastructure
  ├─ config-server :8888
  ├─ Consul :8500
  ├─ Prometheus :9090
  ├─ Grafana :3000
  ├─ Loki :3100
  └─ Zipkin :9411
```

## 적용 원칙

- 모든 도메인 서비스는 Gateway를 단일 진입점으로 두고 경로 기반으로 라우팅합니다.
- 각 서비스는 독립 Spring Boot 프로젝트이며, 하나의 모노레포 안에서 서비스별 하위 프로젝트로 관리합니다.
- config-server, Consul 등 공통 인프라는 Gateway에 노출하지 않고 서비스가 직접 사용합니다.
- 설정은 코드가 아닌 데이터로 취급하여 루트 `config-repo`에서 중앙 관리하며, config-server가 이를 읽어 각 서비스에 배포합니다. (특정 서비스에 종속시키지 않음)
- 아직 분리되지 않은 모놀리식 도메인 코드는 이 Starter에 포함하지 않습니다.
