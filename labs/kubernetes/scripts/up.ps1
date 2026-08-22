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
kubectl wait --namespace ingress-nginx `
    --for=condition=ready pod `
    --selector=app.kubernetes.io/component=controller `
    --timeout=180s
Assert-LastExitCodeZero "waiting for ingress-nginx controller"

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
kubectl apply -f (Join-Path $labRoot "manifests/06-ingress.yaml")
Assert-LastExitCodeZero "kubectl apply ingress"

Write-Host ""
Write-Host "Taskira lab is up: http://localhost/"
Write-Host "Inspect it with: kubectl get all -n taskira"
Write-Host "Tear it down with: & .\labs\kubernetes\scripts\down.ps1"
