param(
    [switch]$RunTests
)

$ErrorActionPreference = 'Stop'

Write-Host "== Backend Environment Check ==" -ForegroundColor Cyan

$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot

$wrapperProperties = Join-Path $projectRoot ".mvn\wrapper\maven-wrapper.properties"
$wrapperJar = Join-Path $projectRoot ".mvn\wrapper\maven-wrapper.jar"

function Test-CommandExists {
    param([string]$Name)
    return [bool](Get-Command $Name -ErrorAction SilentlyContinue)
}

function Ensure-MavenWrapperJar {
    if (-not (Test-Path $wrapperProperties)) {
        Write-Host "[ERROR] Missing maven-wrapper.properties: $wrapperProperties" -ForegroundColor Red
        return $false
    }

    if (Test-Path $wrapperJar) {
        Write-Host "[OK] Maven wrapper jar exists." -ForegroundColor Green
        return $true
    }

    $wrapperUrlLine = Get-Content $wrapperProperties | Where-Object { $_ -like 'wrapperUrl=*' } | Select-Object -First 1
    if (-not $wrapperUrlLine) {
        Write-Host "[ERROR] wrapperUrl missing in maven-wrapper.properties" -ForegroundColor Red
        return $false
    }

    $wrapperUrl = $wrapperUrlLine.Substring('wrapperUrl='.Length)
    Write-Host "[INFO] Downloading Maven wrapper jar..." -ForegroundColor Yellow
    Invoke-WebRequest -Uri $wrapperUrl -OutFile $wrapperJar
    Write-Host "[OK] Downloaded Maven wrapper jar." -ForegroundColor Green
    return $true
}

if (-not (Test-CommandExists -Name 'java')) {
    $temurinHome = Get-ChildItem "C:\Program Files\Eclipse Adoptium" -Directory -ErrorAction SilentlyContinue |
        Sort-Object Name -Descending |
        Select-Object -First 1
    if ($temurinHome) {
        $env:JAVA_HOME = $temurinHome.FullName
        $env:Path = "$env:JAVA_HOME\bin;" + $env:Path
        Write-Host "[INFO] Auto-detected JDK and updated current session PATH: $env:JAVA_HOME" -ForegroundColor Cyan
    }
}

$javaExists = Test-CommandExists -Name 'java'
if ($javaExists) {
    Write-Host "[OK] Java available in PATH." -ForegroundColor Green
} else {
    Write-Host "[ERROR] Java not found in PATH." -ForegroundColor Red
    Write-Host "Suggested fix (Windows, Admin PowerShell):" -ForegroundColor Yellow
    Write-Host "  winget install EclipseAdoptium.Temurin.17.JDK"
    Write-Host "Then restart terminal and verify:" -ForegroundColor Yellow
    Write-Host "  java -version"
}

$javaHome = $env:JAVA_HOME
if ([string]::IsNullOrWhiteSpace($javaHome)) {
    Write-Host "[WARN] JAVA_HOME is not set." -ForegroundColor Yellow
} else {
    $javaHomeExe = Join-Path $javaHome "bin\java.exe"
    if (Test-Path $javaHomeExe) {
        Write-Host "[OK] JAVA_HOME looks valid: $javaHome" -ForegroundColor Green
    } else {
        Write-Host "[WARN] JAVA_HOME does not contain bin\\java.exe: $javaHome" -ForegroundColor Yellow
    }
}

$wrapperReady = Ensure-MavenWrapperJar
if (-not $wrapperReady) {
    exit 1
}

if ($javaExists) {
    Write-Host "[INFO] Running Maven wrapper version check..." -ForegroundColor Cyan
    & .\mvnw.cmd -v
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[ERROR] mvnw -v failed with exit code $LASTEXITCODE" -ForegroundColor Red
        exit $LASTEXITCODE
    }

    if ($RunTests) {
        Write-Host "[INFO] Running backend tests..." -ForegroundColor Cyan
        & .\mvnw.cmd test
        exit $LASTEXITCODE
    }

    Write-Host "[OK] Environment looks ready. Use '.\\mvnw.cmd test' to run tests." -ForegroundColor Green
    exit 0
}

Write-Host "[INFO] Java is required before running backend tests." -ForegroundColor Yellow
exit 2
