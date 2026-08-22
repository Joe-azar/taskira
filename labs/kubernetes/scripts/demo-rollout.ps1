<#
.SYNOPSIS
    Demonstrates real scaling, a real rolling update and a real rollback against the
    Taskira Kubernetes lab (P17 exit criterion, ADR-0024).

.DESCRIPTION
    Requires the lab to already be up (scripts/up.ps1). Every step here is a real
    kubectl/docker/kind action against the running cluster, not a description of what
    Kubernetes could do:
      1. Scale the frontend Deployment and show the new pods actually running.
      2. Build a fresh backend image from the current repo source (a distinct tag, kept
         entirely separate from the real GHCR release pipeline of P15 - ADR-0024), load
         it into the cluster, and roll the backend Deployment forward to it.
      3. Roll the backend Deployment back to the previous (GHCR v0.1.0) image and prove
         it via rollout history.

.EXAMPLE
    & .\labs\kubernetes\scripts\demo-rollout.ps1
#>

param(
    [string]$ClusterName = "taskira-lab"
)

$ErrorActionPreference = "Stop"
$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "../../..")

function Assert-LastExitCodeZero {
    param([string]$Action)
    if ($LASTEXITCODE -ne 0) {
        throw "$Action failed with exit code $LASTEXITCODE"
    }
}

Write-Host "=== 1/3: scaling the frontend Deployment ==="
kubectl scale deployment/frontend -n taskira --replicas=3
Assert-LastExitCodeZero "kubectl scale frontend"
kubectl rollout status deployment/frontend -n taskira --timeout=120s
Assert-LastExitCodeZero "frontend scale rollout"
kubectl get pods -n taskira -l app=frontend -o wide

Write-Host ""
Write-Host "=== 2/3: rolling update (real image, built from current source) ==="
$demoTag = "taskira-backend:k8s-lab-demo"
docker build -t $demoTag (Join-Path $repoRoot "backend")
Assert-LastExitCodeZero "docker build backend demo image"
kind load docker-image $demoTag --name $ClusterName
Assert-LastExitCodeZero "kind load docker-image"
kubectl set image deployment/backend backend=$demoTag -n taskira
Assert-LastExitCodeZero "kubectl set image backend"
kubectl rollout status deployment/backend -n taskira --timeout=180s
Assert-LastExitCodeZero "backend update rollout"
kubectl get pods -n taskira -l app=backend -o wide

Write-Host ""
Write-Host "=== 3/3: rollback ==="
kubectl rollout undo deployment/backend -n taskira
Assert-LastExitCodeZero "kubectl rollout undo backend"
kubectl rollout status deployment/backend -n taskira --timeout=180s
Assert-LastExitCodeZero "backend rollback rollout"
kubectl rollout history deployment/backend -n taskira
kubectl get deployment/backend -n taskira -o jsonpath='{.spec.template.spec.containers[0].image}'
Write-Host ""
Write-Host "(should read ghcr.io/joe-azar/taskira-backend:v0.1.0 again - the rollback target)"

Write-Host ""
Write-Host "Restoring frontend to its original replica count ..."
kubectl scale deployment/frontend -n taskira --replicas=2
Assert-LastExitCodeZero "kubectl scale frontend back down"
kubectl rollout status deployment/frontend -n taskira --timeout=120s

Write-Host ""
Write-Host "Demo complete: scale, rolling update and rollback all verified against the real cluster."
