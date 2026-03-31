param(
    [string]$BaseUrl = "http://localhost:8080",
    [int]$TimeoutSeconds = 20
)

$ErrorActionPreference = 'Stop'

Write-Host "== Post-Release Smoke Check ==" -ForegroundColor Cyan
Write-Host "[INFO] Target: $BaseUrl" -ForegroundColor Cyan

function Get-StatusCode {
    param(
        [string]$Url,
        [int]$Timeout
    )

    try {
        $resp = Invoke-WebRequest -Uri $Url -Method Get -TimeoutSec $Timeout
        return [int]$resp.StatusCode
    } catch {
        if ($_.Exception.Response -and $_.Exception.Response.StatusCode) {
            return [int]$_.Exception.Response.StatusCode.value__
        }
        throw
    }
}

function Assert-StatusIn {
    param(
        [string]$Path,
        [int[]]$Allowed
    )

    $url = "$BaseUrl$Path"
    $status = Get-StatusCode -Url $url -Timeout $TimeoutSeconds

    if ($Allowed -contains $status) {
        Write-Host "[OK] $Path -> HTTP $status" -ForegroundColor Green
        return
    }

    Write-Host "[ERROR] $Path -> HTTP $status, expected one of: $($Allowed -join ', ')" -ForegroundColor Red
    exit 1
}

Assert-StatusIn -Path "/v3/api-docs" -Allowed @(200)
Assert-StatusIn -Path "/swagger-ui/index.html" -Allowed @(200, 302)
Assert-StatusIn -Path "/api/materials" -Allowed @(200, 401, 403)

Write-Host "[OK] Post-release smoke passed." -ForegroundColor Green
exit 0
