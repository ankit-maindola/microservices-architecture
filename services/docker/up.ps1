# Build and start the stack in dependency order (Compose handles sequencing).
# From repo root:  .\docker\up.ps1

Set-Location (Resolve-Path (Join-Path $PSScriptRoot ".."))

Write-Host "Building and starting all services (mysql -> eureka -> apps -> gateway)..."
docker compose up --build -d
Write-Host ""
Write-Host "Gateway:  http://localhost:8080"
Write-Host "Eureka:   http://localhost:8079"
Write-Host "Auth:     http://localhost:8081"
Write-Host "Logs:     docker compose logs -f"
