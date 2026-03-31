param(
    [switch]$SkipEnvCheck,
    [switch]$SkipTests
)

$ErrorActionPreference = 'Stop'

Write-Host "== Backend Quality Gate ==" -ForegroundColor Cyan

$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot

if (-not $SkipEnvCheck) {
    Write-Host "[INFO] Running environment precheck..." -ForegroundColor Cyan
    & .\scripts\verify-dev-env.ps1
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[ERROR] Environment check failed: exit code $LASTEXITCODE" -ForegroundColor Red
        exit $LASTEXITCODE
    }
}

Write-Host "[INFO] Running compile verification (skip tests)..." -ForegroundColor Cyan
& .\mvnw.cmd -q -DskipTests compile
if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] Compile verification failed: exit code $LASTEXITCODE" -ForegroundColor Red
    exit $LASTEXITCODE
}

if (-not $SkipTests) {
    Write-Host "[INFO] Running full test suite..." -ForegroundColor Cyan
    & .\mvnw.cmd -q test
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[ERROR] Test suite failed: exit code $LASTEXITCODE" -ForegroundColor Red
        exit $LASTEXITCODE
    }
}

Write-Host "[OK] Quality gate passed." -ForegroundColor Green
exit 0
