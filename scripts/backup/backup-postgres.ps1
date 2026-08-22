<#
.SYNOPSIS
    Backs up the running Taskira PostgreSQL database with pg_dump (custom format).

.DESCRIPTION
    Runs pg_dump inside the taskira-postgres container and copies the resulting
    dump to backups/ on the host (gitignored). Safe to run against a live
    database; pg_dump takes a consistent snapshot without blocking writers.

.PARAMETER OutputDirectory
    Where to write the dump file on the host. Defaults to backups/ at the repo root.

.EXAMPLE
    & .\scripts\backup\backup-postgres.ps1
    Used before the PostgreSQL 16 -> 18 migration (P6) to produce
    taskira_pre_pg18_migration.dump, later restored and verified with
    scripts/restore/restore-postgres.ps1.
#>

param(
    [string]$ContainerName = "taskira-postgres",
    [string]$Database = "taskira",
    [string]$User = "taskira",
    [string]$OutputDirectory
)

$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")
if (-not $OutputDirectory) {
    $OutputDirectory = Join-Path $repoRoot "backups"
}
if (-not (Test-Path $OutputDirectory)) {
    New-Item -ItemType Directory -Path $OutputDirectory | Out-Null
}

$timestamp = $env:TASKIRA_BACKUP_TIMESTAMP
if (-not $timestamp) {
    $timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
}
$fileName = "taskira-$timestamp.dump"
$containerPath = "/tmp/$fileName"
$hostPath = Join-Path $OutputDirectory $fileName

Write-Host "Dumping '$Database' from container '$ContainerName' ..."
docker exec $ContainerName pg_dump -U $User -d $Database -F custom -f $containerPath
if ($LASTEXITCODE -ne 0) { throw "pg_dump failed with exit code $LASTEXITCODE" }

docker cp "${ContainerName}:${containerPath}" $hostPath
if ($LASTEXITCODE -ne 0) { throw "docker cp failed with exit code $LASTEXITCODE" }

docker exec $ContainerName rm -f $containerPath

$size = (Get-Item $hostPath).Length
Write-Host "Backup written to $hostPath ($size bytes)."
Write-Host "Restore and verify it with: & .\scripts\restore\restore-postgres.ps1 -DumpFile `"$hostPath`""

# Write-Host above is for a human watching the console - it never reaches the success
# output stream, so `$result = & backup-postgres.ps1` would otherwise capture nothing.
# A real bug, not assumed: the backup-restore-drill workflow used to regex-parse the
# Write-Host text above to recover this path, which threw "Cannot index into a null
# array" on the real GitHub Actions run, because $result was empty. This bare
# expression is the script's actual return value - the one thing a caller should rely
# on programmatically.
$hostPath
