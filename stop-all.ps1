$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

docker compose down --remove-orphans

docker compose -f docker-compose.infra.yml down --remove-orphans
Write-Host "LXP containers stopped."
