# payment-aggregator

Subscription과 Payment를 WebClient로 병렬 조회하여 Mono.zip으로 합칩니다.

- Port: `8086`
- Application: `com.lcs.paymentaggregator.PaymentAggregatorApplication`
- Health: `http://localhost:8086/actuator/health`
