<#
.SYNOPSIS
    Destroys the Taskira Kubernetes lab cluster entirely.

.DESCRIPTION
    The lab is ephemeral by design (ADR-0024): nothing created inside it is meant to
    survive. This deletes the whole kind cluster (all namespaces, all PVCs, all data)
    in one step, not just the taskira namespace.

.EXAMPLE
    & .\labs\kubernetes\scripts\down.ps1
#>

param(
    [string]$ClusterName = "taskira-lab"
)

$ErrorActionPreference = "Stop"

kind delete cluster --name $ClusterName
if ($LASTEXITCODE -ne 0) {
    throw "kind delete cluster failed with exit code $LASTEXITCODE"
}

Write-Host "Taskira lab cluster '$ClusterName' deleted."
