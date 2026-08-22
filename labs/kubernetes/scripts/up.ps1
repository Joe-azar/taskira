<#
.SYNOPSIS
    Creates the Taskira Kubernetes lab cluster (kind) and deploys the full stack.

.DESCRIPTION
    Idempotent: safe to re-run against an already-created cluster. Never writes a real
    secret to disk or to git (ADR-0024) - the PostgreSQL password is only ever held in
    memory and inside the cluster's own Secret object.

.PARAMETER PostgresPassword
    Required, no default on purpose (same discipline as infra/.env.prodlike.example /
    infra/.env.staging.example) - this script refuses to invent a password for you.

.EXAMPLE
    & .\labs\kubernetes\scripts\up.ps1 -PostgresPassword "some-local-only-password"
#>

param(
    [Parameter(Mandatory = $true)]
    [string]$PostgresPassword,
    [string]$ClusterName = "taskira-lab"
)

$ErrorActionPreference = "Stop"
$labRoot = Resolve-Path (Join-Path $PSScriptRoot "..")

function Assert-LastExitCodeZero {
    param([string]$Action)
    if ($LASTEXITCODE -ne 0) {
        throw "$Action failed with exit code $LASTEXITCODE"
    }
}

# A real bug found while building the Helm lab (P18) that turned out to be latent here
# too, not assumed: `kubectl wait` called immediately after `kubectl apply` can run
# before the Deployment controller has even created the Pod object yet - kubectl treats
# that as "no matching resources found" and exits non-zero right away instead of waiting
# for a pod to appear. Retrying the whole wait call rides out that window instead of
# racing it - the same shape of fix already applied to the Postgres temporary-server
# race in scripts/restore/restore-postgres.ps1 (P16).
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

Write-Host "=== 1/7: kind cluster ==="
# Deliberately not redirecting stderr: `kind get clusters` writes "No kind clusters
# found." to stderr (with exit code 0) when the list is empty, and PowerShell 5.1 wraps
# a redirected native-command stderr line in a terminating NativeCommandError under
# $ErrorActionPreference = "Stop" - the same class of bug already found and fixed in
# scripts/restore/restore-postgres.ps1 (P16). Letting it print directly is harmless and
# keeps $existing correctly empty in that case.
$existing = kind get clusters
if ($existing -contains $ClusterName) {
    Write-Host "Cluster '$ClusterName' already exists, reusing it."
}
else {
    kind create cluster --config (Join-Path $labRoot "cluster/kind-config.yaml")
    Assert-LastExitCodeZero "kind create cluster"
}

Write-Host "=== 2/7: ingress-nginx (vendored, controller-v1.15.1) ==="
kubectl apply -f (Join-Path $labRoot "vendor/ingress-nginx-kind-deploy.yaml")
Assert-LastExitCodeZero "kubectl apply ingress-nginx"
Wait-ForPodsReady -Namespace "ingress-nginx" -Selector "app.kubernetes.io/component=controller" -TimeoutSeconds 180

Write-Host "=== 3/7: namespace + config ==="
kubectl apply -f (Join-Path $labRoot "manifests/00-namespace.yaml")
Assert-LastExitCodeZero "kubectl apply namespace"
kubectl apply -f (Join-Path $labRoot "manifests/01-configmap.yaml")
Assert-LastExitCodeZero "kubectl apply configmaps"

Write-Host "=== 4/7: postgres secret (generated, never written to disk) ==="
kubectl create secret generic postgres-secret `
    --namespace taskira `
    --from-literal=password=$PostgresPassword `
    --dry-run=client -o yaml | kubectl apply -f -
Assert-LastExitCodeZero "kubectl apply postgres-secret"

Write-Host "=== 5/7: postgres ==="
kubectl apply -f (Join-Path $labRoot "manifests/03-postgres.yaml")
Assert-LastExitCodeZero "kubectl apply postgres"
kubectl rollout status deployment/postgres -n taskira --timeout=180s
Assert-LastExitCodeZero "postgres rollout"

Write-Host "=== 6/7: backend + frontend ==="
kubectl apply -f (Join-Path $labRoot "manifests/04-backend.yaml")
Assert-LastExitCodeZero "kubectl apply backend"
kubectl apply -f (Join-Path $labRoot "manifests/05-frontend.yaml")
Assert-LastExitCodeZero "kubectl apply frontend"
kubectl rollout status deployment/backend -n taskira --timeout=180s
Assert-LastExitCodeZero "backend rollout"
kubectl rollout status deployment/frontend -n taskira --timeout=180s
Assert-LastExitCodeZero "frontend rollout"

Write-Host "=== 7/7: ingress ==="
# A second real bug found while building the Helm lab (P18), also latent here: even
# once the ingress-nginx controller pod reports Ready, its admission webhook HTTPS
# endpoint (ingress-nginx-controller-admission) can take a little longer to actually
# start accepting connections - applying an Ingress resource too soon after can fail
# with "dial tcp ...:443: connect: connection refused" against that webhook. Retrying
# the real apply rides out that short window rather than guessing at a fixed sleep.
$maxAttempts = 5
for ($attempt = 1; $attempt -le $maxAttempts; $attempt++) {
    kubectl apply -f (Join-Path $labRoot "manifests/06-ingress.yaml")
    if ($LASTEXITCODE -eq 0) { break }
    if ($attempt -eq $maxAttempts) {
        throw "kubectl apply ingress failed after $maxAttempts attempts (exit code $LASTEXITCODE)"
    }
    Write-Host "kubectl apply ingress failed (attempt $attempt/$maxAttempts), retrying in 5s ..."
    Start-Sleep -Seconds 5
}

Write-Host ""
Write-Host "Taskira lab is up: http://localhost/"
Write-Host "Inspect it with: kubectl get all -n taskira"
Write-Host "Tear it down with: & .\labs\kubernetes\scripts\down.ps1"
