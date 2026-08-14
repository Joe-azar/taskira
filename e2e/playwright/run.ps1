[CmdletBinding()]
param(
  [string]$ProjectName = "taskira-e2e-$PID"
)

$ErrorActionPreference = 'Stop'
$composeFile = Join-Path $PSScriptRoot 'compose.e2e.yml'
$exitCode = 0

New-Item -ItemType Directory -Force -Path (Join-Path $PSScriptRoot 'playwright-report') | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $PSScriptRoot 'test-results') | Out-Null

try {
  & docker compose -p $ProjectName -f $composeFile up --build --abort-on-container-exit --exit-code-from e2e
  $exitCode = $LASTEXITCODE
}
finally {
  & docker compose -p $ProjectName -f $composeFile down --volumes --remove-orphans
  if ($LASTEXITCODE -ne 0 -and $exitCode -eq 0) {
    $exitCode = $LASTEXITCODE
  }
}

exit $exitCode
