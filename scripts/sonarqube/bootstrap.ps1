<#
.SYNOPSIS
    Idempotent local bootstrap for the SonarQube Community Build stack.

.DESCRIPTION
    Local development only. Rotates the default admin/admin credentials, creates the
    "taskira" project and generates a user token for scanner runs. The token and the
    rotated admin password are written to infra/sonarqube/.env.local, which is covered
    by the repository .env.* gitignore rule and must never be committed.

    Safe to re-run: if a working token already exists in .env.local, the script exits
    without touching SonarQube again.
#>

param(
    [string]$SonarUrl = "http://localhost:9000",
    [string]$ProjectKey = "taskira",
    [string]$ProjectName = "Taskira",
    [string]$TokenName = "taskira-local-scan"
)

$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$envFile = Join-Path $repoRoot "infra\sonarqube\.env.local"

function Get-BasicAuthHeader {
    param([string]$User, [string]$Pass)
    $bytes = [System.Text.Encoding]::UTF8.GetBytes("$User`:$Pass")
    return @{ Authorization = "Basic $([Convert]::ToBase64String($bytes))" }
}

function Get-HttpErrorBody {
    # Invoke-RestMethod's error object shape differs by PowerShell edition: Windows
    # PowerShell 5.1 (used locally) exposes a System.Net.HttpWebResponse with
    # GetResponseStream(); PowerShell Core / pwsh (used by GitHub Actions' Linux
    # runners, ci quality.yml) exposes a System.Net.Http.HttpResponseMessage instead,
    # which has no such method. Handle both so a real failure is never masked by a
    # second error thrown while trying to read the first one's body.
    param($ErrorRecord)
    $response = $ErrorRecord.Exception.Response
    if (-not $response) {
        return $ErrorRecord.Exception.Message
    }
    if ($response.PSObject.Methods.Name -contains "GetResponseStream") {
        $stream = $response.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($stream)
        return $reader.ReadToEnd()
    }
    if ($response.Content) {
        try {
            return $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
        } catch {
            return $ErrorRecord.Exception.Message
        }
    }
    if ($ErrorRecord.ErrorDetails -and $ErrorRecord.ErrorDetails.Message) {
        return $ErrorRecord.ErrorDetails.Message
    }
    return $ErrorRecord.Exception.Message
}

function Test-SonarToken {
    param([string]$Token)
    try {
        $headers = Get-BasicAuthHeader -User $Token -Pass ""
        $result = Invoke-RestMethod -Method Get -Uri "$SonarUrl/api/authentication/validate" -Headers $headers
        return [bool]$result.valid
    } catch {
        return $false
    }
}

if (Test-Path $envFile) {
    $existing = Get-Content $envFile -Raw | ConvertFrom-StringData
    if ($existing.SONAR_TOKEN -and (Test-SonarToken -Token $existing.SONAR_TOKEN)) {
        Write-Host "SonarQube already bootstrapped; reusing token from $envFile"
        exit 0
    }
}

Write-Host "Waiting for SonarQube to report UP at $SonarUrl ..."
$deadline = (Get-Date).AddMinutes(5)
do {
    try {
        $status = Invoke-RestMethod -Method Get -Uri "$SonarUrl/api/system/status"
        if ($status.status -eq "UP") { break }
    } catch { }
    if ((Get-Date) -gt $deadline) {
        throw "SonarQube did not report UP within 5 minutes."
    }
    Start-Sleep -Seconds 5
} while ($true)

$alnum = (48..57) + (65..90) + (97..122) | Get-Random -Count 20 | ForEach-Object { [char]$_ }
$special = "!@#%*-_" -split "" | Where-Object { $_ } | Get-Random -Count 1
$newPassword = (-join $alnum) + $special
$adminPass = "admin"

$headers = Get-BasicAuthHeader -User "admin" -Pass "admin"
# Body is a hashtable, not a hand-built query string: Invoke-RestMethod URL-encodes each
# value itself, so a randomly generated password containing a reserved/escape character
# (e.g. a bare "%", which Tomcat's UDecoder rejects as an incomplete escape sequence) is
# always transmitted correctly.
$body = @{ login = "admin"; previousPassword = "admin"; password = $newPassword }
$attempts = 0
$rotated = $false
$lastReason = $null
do {
    $attempts++
    try {
        Invoke-RestMethod -Method Post -Uri "$SonarUrl/api/users/change_password" -Headers $headers -Body $body -ContentType "application/x-www-form-urlencoded"
        $adminPass = $newPassword
        $rotated = $true
        Write-Host "Rotated default admin/admin credentials."
    } catch {
        $lastReason = Get-HttpErrorBody -ErrorRecord $_
        if ($attempts -lt 5) {
            Write-Host "change_password attempt $attempts failed ($lastReason); SonarQube may still be finishing startup, retrying ..."
            Start-Sleep -Seconds 5
        }
    }
} while (-not $rotated -and $attempts -lt 5)

if (-not $rotated) {
    throw "Cannot rotate admin/admin credentials after $attempts attempts (and no valid token was found in $envFile): $lastReason"
}

$authHeaders = Get-BasicAuthHeader -User "admin" -Pass $adminPass

$projects = Invoke-RestMethod -Method Get -Uri "$SonarUrl/api/projects/search?projects=$ProjectKey" -Headers $authHeaders
if (-not $projects.components -or $projects.components.Count -eq 0) {
    Invoke-RestMethod -Method Post -Uri "$SonarUrl/api/projects/create" -Headers $authHeaders -Body @{ project = $ProjectKey; name = $ProjectName } -ContentType "application/x-www-form-urlencoded" | Out-Null
    Write-Host "Created SonarQube project '$ProjectKey'."
} else {
    Write-Host "SonarQube project '$ProjectKey' already exists."
}

try {
    Invoke-RestMethod -Method Post -Uri "$SonarUrl/api/user_tokens/revoke" -Headers $authHeaders -Body @{ name = $TokenName } -ContentType "application/x-www-form-urlencoded" | Out-Null
} catch { }

$tokenResponse = Invoke-RestMethod -Method Post -Uri "$SonarUrl/api/user_tokens/generate" -Headers $authHeaders -Body @{ name = $TokenName } -ContentType "application/x-www-form-urlencoded"

$envContent = "SONAR_HOST_URL=$SonarUrl`nSONAR_TOKEN=$($tokenResponse.token)`nSONAR_ADMIN_PASSWORD=$adminPass`n"
[System.IO.File]::WriteAllText($envFile, $envContent, (New-Object System.Text.UTF8Encoding($false)))

Write-Host "Bootstrap complete. Token and rotated admin password written to $envFile (gitignored, local only)."
