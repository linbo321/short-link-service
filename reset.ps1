param(
    [switch]$Force
)

$ErrorActionPreference = "Continue"
$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $ProjectRoot -ErrorAction Stop

if (-not $Force) {
    Write-Host "This will delete Docker MySQL and Redis volumes. All generated short links will be removed." -ForegroundColor Yellow
    $answer = Read-Host "Type YES to continue"
    if ($answer -ne "YES") {
        Write-Host "Canceled."
        exit 0
    }
}

Write-Host "Resetting data and restarting Short Link Service..." -ForegroundColor Cyan
docker compose down -v --remove-orphans
if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] Reset failed. Please check Docker Desktop." -ForegroundColor Red
    exit 1
}

& "$ProjectRoot\start.ps1" -NoBrowser
