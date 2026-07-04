param(
    [switch]$NoBrowser
)

$ErrorActionPreference = "Continue"
$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $ProjectRoot -ErrorAction Stop

function Write-Step {
    param([string]$Message)
    Write-Host ""
    Write-Host "==> $Message" -ForegroundColor Cyan
}

function Write-Ok {
    param([string]$Message)
    Write-Host "[OK] $Message" -ForegroundColor Green
}

function Write-Fail {
    param([string]$Message)
    Write-Host "[ERROR] $Message" -ForegroundColor Red
}

Write-Host "Short Link Service launcher" -ForegroundColor Green

Write-Step "Checking Docker"
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Fail "Docker command was not found. Please install and start Docker Desktop."
    exit 1
}

docker info *> $null
if ($LASTEXITCODE -ne 0) {
    Write-Fail "Docker Desktop is not ready. Start Docker Desktop and wait until Docker Engine is running."
    exit 1
}
Write-Ok "Docker is ready"

Write-Step "Starting MySQL, Redis, backend and Nginx"
docker compose up -d --build
if ($LASTEXITCODE -ne 0) {
    Write-Fail "Startup failed. Run 'docker compose logs app' to inspect backend logs."
    exit 1
}

Write-Step "Waiting for health check"
$healthUrl = "http://localhost/api/health"
$deadline = (Get-Date).AddSeconds(90)
$healthy = $false

while ((Get-Date) -lt $deadline) {
    try {
        $response = Invoke-RestMethod -Uri $healthUrl -TimeoutSec 3
        if ($response.code -eq 200 -and $response.data -eq "ok") {
            $healthy = $true
            break
        }
    } catch {
        Start-Sleep -Seconds 3
    }
}

if (-not $healthy) {
    Write-Fail "Containers started, but health check did not pass. Recent backend logs:"
    docker compose logs --tail=80 app
    exit 1
}

Write-Ok "Service is ready"
Write-Host ""
Write-Host "Web page:     http://localhost/" -ForegroundColor Yellow
Write-Host "Health check: http://localhost/api/health" -ForegroundColor Yellow
Write-Host ""
Write-Host "Stop service: .\stop.ps1"
Write-Host "Reset data:   .\reset.ps1"

if (-not $NoBrowser) {
    Start-Process "http://localhost/"
}
