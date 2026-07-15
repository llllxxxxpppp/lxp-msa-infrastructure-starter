# payment-service

결제 도메인 코드를 옮길 서버이며, 현재 Gateway를 통해 노출되는 최소 조회 API가 있습니다. (`GET /api/payments/subscriptions/{id}`)

- Port: `8085`
- Application: `com.lcs.payment.PaymentServiceApplication`
- Health: `http://localhost:8085/actuator/health`
