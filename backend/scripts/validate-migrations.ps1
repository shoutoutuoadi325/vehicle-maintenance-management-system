$ErrorActionPreference = 'Stop'

Write-Host "== DB Migration Validation ==" -ForegroundColor Cyan

$projectRoot = Split-Path -Parent $PSScriptRoot
$migrationDir = Join-Path $projectRoot "src\main\resources\db\migration"

if (-not (Test-Path $migrationDir)) {
    Write-Host "[ERROR] Migration directory not found: $migrationDir" -ForegroundColor Red
    exit 1
}

$files = Get-ChildItem -Path $migrationDir -Filter "V*__*.sql" | Sort-Object Name
if (-not $files -or $files.Count -eq 0) {
    Write-Host "[ERROR] No migration files found under $migrationDir" -ForegroundColor Red
    exit 1
}

$versionMap = @{}
$versions = New-Object System.Collections.Generic.List[int]

foreach ($file in $files) {
    if ($file.Name -notmatch '^V([0-9]+)__.+\.sql$') {
        Write-Host "[ERROR] Invalid migration name format: $($file.Name)" -ForegroundColor Red
        exit 1
    }

    $version = [int]$matches[1]
    if ($versionMap.ContainsKey($version)) {
        Write-Host "[ERROR] Duplicate migration version V${version}: $($versionMap[$version]) and $($file.Name)" -ForegroundColor Red
        exit 1
    }

    $versionMap[$version] = $file.Name
    $versions.Add($version)
}

$sortedVersions = $versions | Sort-Object
for ($i = 1; $i -lt $sortedVersions.Count; $i++) {
    $prev = $sortedVersions[$i - 1]
    $curr = $sortedVersions[$i]
    if ($curr -ne ($prev + 1)) {
        Write-Host "[ERROR] Migration version gap detected between V${prev} and V${curr}" -ForegroundColor Red
        exit 1
    }
}

Write-Host "[OK] Migration naming and sequence validation passed." -ForegroundColor Green
Write-Host "[INFO] Version range: V$($sortedVersions[0]) .. V$($sortedVersions[$sortedVersions.Count - 1])" -ForegroundColor Cyan
Write-Host "[INFO] Total migration files: $($sortedVersions.Count)" -ForegroundColor Cyan

exit 0
