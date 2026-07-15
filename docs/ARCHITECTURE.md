# LXP MSA Architecture

```text
Client
  ↓
Public API Gateway :8080
  ├─ auth-service :8081
  ├─ member-service :8082
  ├─ course-service :8083
  └─ payment-aggregator :8086

payment-aggregator internal calls
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

- Aggregator는 결제 영역에만 사용합니다.
- subscription-service와 payment-service는 Gateway에 직접 노출하지 않습니다.
- 각 서비스는 독립 Spring Boot 프로젝트이며 추후 독립 Repository/Submodule로 관리합니다.
- 아직 분리되지 않은 모놀리식 도메인 코드는 이 Starter에 포함하지 않습니다.
