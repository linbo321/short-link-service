$ErrorActionPreference = "Continue"
$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $ProjectRoot -ErrorAction Stop

Write-Host "Stopping Short Link Service..." -ForegroundColor Cyan
docker compose down

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "[OK] Service stopped. MySQL and Redis data volumes were kept." -ForegroundColor Green
    Write-Host "To delete all data and restart, run: .\reset.ps1"
} else {
    Write-Host "[ERROR] Stop failed. Please check Docker Desktop." -ForegroundColor Red
    exit 1
}
