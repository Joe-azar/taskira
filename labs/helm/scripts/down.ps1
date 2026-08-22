<#
.SYNOPSIS
    Destroys the Taskira Helm lab cluster entirely.

.DESCRIPTION
    The lab is ephemeral by design (ADR-0024/ADR-0025): nothing created inside it is
    meant to survive. This deletes the whole kind cluster in one step - simpler and more
    thorough than `helm uninstall` (which would leave the cluster, ingress-nginx and the
    PVC's underlying volume behind).

.EXAMPLE
    & .\labs\helm\scripts\down.ps1
#>

param(
    [string]$ClusterName = "taskira-lab"
)

$ErrorActionPreference = "Stop"

kind delete cluster --name $ClusterName
if ($LASTEXITCODE -ne 0) {
    throw "kind delete cluster failed with exit code $LASTEXITCODE"
}

Write-Host "Taskira Helm lab cluster '$ClusterName' deleted."
