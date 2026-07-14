$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

docker compose -f docker-compose.infra.yml up -d
Write-Host ""
Write-Host "Infrastructure started."
Write-Host "Consul    : http://localhost:8500"
Write-Host "Prometheus: http://localhost:9090"
Write-Host "Grafana   : http://localhost:3000 (admin/admin)"
Write-Host "Loki      : http://localhost:3100/ready"
