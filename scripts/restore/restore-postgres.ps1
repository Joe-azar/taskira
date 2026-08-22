<#
.SYNOPSIS
    Restores a pg_dump backup into a temporary, disposable PostgreSQL container
    and prints row counts so the restore can be verified before it is trusted.

.DESCRIPTION
    Never restores over the live database. Always starts a throwaway container
    with its own Docker volume, restores into it, prints per-table row counts,
    then leaves the container running (named below) for manual inspection -
    remove it and its volume yourself once you're done, so a real restore
    check is never silently skipped.

.PARAMETER DumpFile
    Path to a .dump file produced by scripts/backup/backup-postgres.ps1.

.PARAMETER Image
    Postgres image to restore into. Defaults to the version currently used by
    infra/docker-compose.yml.

.EXAMPLE
    & .\scripts\restore\restore-postgres.ps1 -DumpFile .\backups\taskira-20260815-114700.dump
#>

param(
    [Parameter(Mandatory = $true)]
    [string]$DumpFile,
    [string]$Image = "postgres:18.6-alpine3.23@sha256:f65cfc1a73466fc0807f4a33496e1c438b19269091c8f998faec871874df8e5e",
    [string]$ContainerName = "taskira-postgres-restore-check",
    [string]$Database = "taskira",
    [string]$User = "taskira",
    [string]$Password = "taskira"
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path $DumpFile)) {
    throw "Dump file not found: $DumpFile"
}
$DumpFile = (Resolve-Path $DumpFile).Path

$existing = docker ps -a --filter "name=^/$ContainerName`$" --format "{{.Names}}"
if ($existing) {
    throw "Container '$ContainerName' already exists. Remove it first: docker rm -f $ContainerName"
}

Write-Host "Starting a disposable PostgreSQL container for the restore check ..."
docker run -d --name $ContainerName `
    -e "POSTGRES_DB=$Database" `
    -e "POSTGRES_USER=$User" `
    -e "POSTGRES_PASSWORD=$Password" `
    $Image | Out-Null

Write-Host "Waiting for PostgreSQL to accept connections ..."
# A real bug, not assumed: the official Postgres image's first-boot entrypoint starts a
# *temporary* server (Unix socket only) to run init scripts (create the database/role),
# stops it, then starts the real one. pg_isready alone can report success against that
# temporary server, moments before it shuts down - pg_restore then hit "the database
# system is shutting down" despite the readiness check having just passed. A single
# psql query against the actual target database is the functional check that matters;
# retrying it (not just pg_isready once) rides out the temporary-server window instead
# of racing it.
$deadline = (Get-Date).AddMinutes(2)
do {
    # Stdout only, deliberately not stderr: PowerShell 5.1 wraps a native command's
    # stderr output in a terminating NativeCommandError under
    # $ErrorActionPreference = "Stop" (set above), which would abort this retry loop on
    # its very first expected failure instead of letting it wait out the temporary-server
    # window described above. The occasional "connection refused" line printing to the
    # console while this loop retries is expected and harmless.
    docker exec $ContainerName psql -U $User -d $Database -c "SELECT 1" | Out-Null
    if ($LASTEXITCODE -eq 0) { break }
    if ((Get-Date) -gt $deadline) {
        throw "PostgreSQL did not become ready within 2 minutes."
    }
    Start-Sleep -Seconds 2
} while ($true)

docker cp $DumpFile "${ContainerName}:/tmp/restore.dump"
Write-Host "Restoring $DumpFile ..."
docker exec $ContainerName pg_restore -U $User -d $Database --no-owner --role=$User /tmp/restore.dump
if ($LASTEXITCODE -ne 0) {
    Write-Warning "pg_restore reported a non-zero exit code; review the output above before trusting this backup."
}

Write-Host ""
Write-Host "Row counts in the restored database:"
docker exec $ContainerName psql -U $User -d $Database -c "SELECT table_name, (xpath('/row/c/text()', query_to_xml(format('SELECT count(*) AS c FROM %I.%I', table_schema, table_name), false, true, '')))[1]::text::int AS row_count FROM information_schema.tables WHERE table_schema = 'public' AND table_type = 'BASE TABLE' ORDER BY table_name;"

Write-Host ""
Write-Host "Restore check container '$ContainerName' is still running for manual inspection."
Write-Host "When you're done verifying, remove it: docker rm -f $ContainerName"
