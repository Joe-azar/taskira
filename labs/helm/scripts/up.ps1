<#
.SYNOPSIS
    Creates a kind cluster (reusing the P17 cluster config) and deploys Taskira via the
    Helm chart in labs/helm/taskira (P18, ADR-0025).

.DESCRIPTION
    Idempotent: safe to re-run against an already-created cluster (`helm upgrade
    --install`). Never writes a real secret to disk or to git - the PostgreSQL password
    is only ever passed to Helm via --set, held in memory.

.PARAMETER PostgresPassword
    Required, no default on purpose (ADR-0025) - this script refuses to invent a
    password for you.

.EXAMPLE
    & .\labs\helm\scripts\up.ps1 -PostgresPassword "some-local-only-password"
#>

param(
    [Parameter(Mandatory = $true)]
    [string]$PostgresPassword,
    [string]$ClusterName = "taskira-lab"
)

$ErrorActionPreference = "Stop"
$helmRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$k8sLabRoot = Resolve-Path (Join-Path $PSScriptRoot "../../kubernetes")

function Assert-LastExitCodeZero {
    param([string]$Action)
    if ($LASTEXITCODE -ne 0) {
        throw "$Action failed with exit code $LASTEXITCODE"
    }
}

# A real bug found by an actual re-run of this script, not assumed: `kubectl wait`
# called immediately after `kubectl apply` can run before the Deployment controller has
# even created the Pod object yet - kubectl treats that as "no matching resources
# found" and exits non-zero right away, instead of waiting for a pod to appear. A single
# `kubectl apply` + `kubectl wait` pair is therefore inherently racy immediately after
# creating a fresh Deployment. Retrying the whole wait call rides out that window instead
# of racing it - the same shape of fix already applied to the Postgres
# temporary-server race in scripts/restore/restore-postgres.ps1 (P16).
function Wait-ForPodsReady {
    param(
        [string]$Namespace,
        [string]$Selector,
        [int]$TimeoutSeconds = 180
    )
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    do {
        kubectl wait --namespace $Namespace --for=condition=ready pod --selector=$Selector --timeout=10s
        if ($LASTEXITCODE -eq 0) { return }
        if ((Get-Date) -gt $deadline) {
            throw "Pods matching '$Selector' in namespace '$Namespace' were not ready within $TimeoutSeconds seconds."
        }
        Start-Sleep -Seconds 3
    } while ($true)
}

Write-Host "=== 1/4: kind cluster ==="
# Deliberately not redirecting stderr - see labs/kubernetes/scripts/up.ps1 (P16/P17) for
# why: `kind get clusters` writes to stderr with exit code 0 when the list is empty, and
# PowerShell 5.1 wraps a redirected native-command stderr line in a terminating error
# under $ErrorActionPreference = "Stop".
$existing = kind get clusters
if ($existing -contains $ClusterName) {
    Write-Host "Cluster '$ClusterName' already exists, reusing it."
}
else {
    kind create cluster --config (Join-Path $k8sLabRoot "cluster/kind-config.yaml")
    Assert-LastExitCodeZero "kind create cluster"
}

Write-Host "=== 2/4: ingress-nginx (vendored, controller-v1.15.1) ==="
kubectl apply -f (Join-Path $k8sLabRoot "vendor/ingress-nginx-kind-deploy.yaml")
Assert-LastExitCodeZero "kubectl apply ingress-nginx"
Wait-ForPodsReady -Namespace "ingress-nginx" -Selector "app.kubernetes.io/component=controller" -TimeoutSeconds 180

Write-Host "=== 3/4: helm lint ==="
helm lint (Join-Path $helmRoot "taskira") --set postgres.password=$PostgresPassword
Assert-LastExitCodeZero "helm lint"

Write-Host "=== 4/4: helm upgrade --install ==="
# A second, distinct real bug found by an actual re-run, on top of the one above: even
# once the controller pod reports Ready, its admission webhook HTTPS endpoint
# (ingress-nginx-controller-admission) can take a little longer to actually start
# accepting connections - Helm's attempt to create the Ingress resource failed with
# "dial tcp ...:443: connect: connection refused" against that webhook. Retrying the
# real `helm upgrade --install` call rides out that short window, the same shape of fix
# as Wait-ForPodsReady above - a blind sleep before the first attempt would have been
# guessing at how long is "long enough"; retrying the actual operation is not.
$maxAttempts = 5
for ($attempt = 1; $attempt -le $maxAttempts; $attempt++) {
    helm upgrade --install taskira (Join-Path $helmRoot "taskira") `
        --namespace taskira --create-namespace `
        --set postgres.password=$PostgresPassword `
        --wait --timeout 5m
    if ($LASTEXITCODE -eq 0) { break }
    if ($attempt -eq $maxAttempts) {
        throw "helm upgrade --install failed after $maxAttempts attempts (exit code $LASTEXITCODE)"
    }
    Write-Host "helm upgrade --install failed (attempt $attempt/$maxAttempts), retrying in 10s ..."
    Start-Sleep -Seconds 10
}

Write-Host ""
Write-Host "Taskira (Helm release 'taskira') is up: http://localhost/"
Write-Host "Inspect it with: helm status taskira -n taskira / kubectl get all -n taskira"
Write-Host "Tear it down with: & .\labs\helm\scripts\down.ps1"
