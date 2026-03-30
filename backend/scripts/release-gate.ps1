param(
    [string]$BaseUrl = "http://localhost:8080",
    [switch]$SkipEnvCheck,
    [switch]$SkipTests,
    [switch]$SkipSmoke
)

$ErrorActionPreference = 'Stop'

Write-Host "== Standardized Release Gate ==" -ForegroundColor Cyan

$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot

$qualityGateArgs = @()
if ($SkipEnvCheck) { $qualityGateArgs += "-SkipEnvCheck" }
if ($SkipTests) { $qualityGateArgs += "-SkipTests" }

Write-Host "[STEP] Quality gate" -ForegroundColor Cyan
& .\scripts\quality-gate.ps1 @qualityGateArgs
if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] quality-gate failed." -ForegroundColor Red
    exit $LASTEXITCODE
}

Write-Host "[STEP] DB migration validation" -ForegroundColor Cyan
& .\scripts\validate-migrations.ps1
if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] migration validation failed." -ForegroundColor Red
    exit $LASTEXITCODE
}

if (-not $SkipSmoke) {
    Write-Host "[STEP] Post-release smoke" -ForegroundColor Cyan
    & .\scripts\post-release-smoke.ps1 -BaseUrl $BaseUrl
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[ERROR] post-release smoke failed." -ForegroundColor Red
        exit $LASTEXITCODE
    }
}

Write-Host "[OK] Release gate passed." -ForegroundColor Green
exit 0
