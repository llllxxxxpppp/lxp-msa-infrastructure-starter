$urls = @(
    "http://localhost:8888/actuator/health",
    "http://localhost:8080/actuator/health",
    "http://localhost:8080/api/auth/ping",
    "http://localhost:8080/api/members/ping",
    "http://localhost:8080/api/courses/ping",
    "http://localhost:8080/api/subscriptions/1",
    "http://localhost:8080/api/payments/subscriptions/1"
)

foreach ($url in $urls) {
    try {
        $response = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 5
        Write-Host "[OK] $url -> $($response.StatusCode)"
    } catch {
        Write-Host "[FAIL] $url -> $($_.Exception.Message)"
    }
}
