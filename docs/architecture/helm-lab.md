# Lab Helm

Voir [ADR-0025](adr/0025-helm-lab.md) pour les décisions complètes. Ce document est la référence
opérationnelle. Construit après les manifestes bruts de [P17](kubernetes-lab.md) — voir ce document pour
le lab Kubernetes de base et les deux bugs DNS Nginx déjà corrigés là et repris tels quels ici.

## Ce qui est empaqueté

```text
labs/helm/
  taskira/
    Chart.yaml
    values.yaml           images GHCR réelles v0.1.0, replicas, ressources, taille PVC
    templates/
      configmap.yaml       postgres-config, backend-config
      secret.yaml           `required` : échoue au rendu sans postgres.password
      postgres.yaml         PVC + Deployment (strategy: Recreate) + Service
      backend.yaml          initContainer wait-for-postgres + Deployment + Service
      frontend-nginx-configmap.yaml   correctif DNS de P17, paramétré par .Release.Namespace
      frontend.yaml
      ingress.yaml
  scripts/
    up.ps1     crée le cluster (kind-config.yaml de P17), helm lint, helm upgrade --install
    down.ps1   détruit le cluster entier
```

ingress-nginx reste une installation d'infrastructure de cluster préalable (le manifeste vendu de P17),
pas une dépendance de ce chart — un vrai chart applicatif ne réinstalle généralement pas le contrôleur
Ingress partagé du cluster à chaque déploiement.

## Déployer

```powershell
& .\labs\helm\scripts\up.ps1 -PostgresPassword "un-mot-de-passe-local-seulement"
```

Aucune valeur par défaut pour le mot de passe dans `values.yaml` : `templates/secret.yaml` utilise la
fonction Helm `required` et échoue explicitement au rendu si absent.

```powershell
helm lint labs\helm\taskira --set postgres.password=...
helm template taskira labs\helm\taskira -n taskira --set postgres.password=...
helm status taskira -n taskira
kubectl get all -n taskira
```

## Détruire

```powershell
& .\labs\helm\scripts\down.ps1
```

## Un bug réel corrigé par un `initContainer`, trouvé en installant réellement le chart

`helm upgrade --install --wait` soumet tous les templates du chart à l'API server ensemble et n'attend
la disponibilité qu'à la fin — contrairement à `labs/kubernetes/scripts/up.ps1` (P17), qui applique
explicitement `postgres` et attend son rollout avant d'appliquer `backend`. La toute première
installation réelle a fait crash-looper `backend` deux fois
(`FlywaySqlUnableToConnectToDbException: Connection to postgres:5432 refused`) avant que le
redémarrage automatique de Kubernetes ne finisse par réussir une fois `postgres` prêt. Corrigé avec un
`initContainer` (`templates/backend.yaml`) qui réutilise l'image `postgres` déjà épinglée dans
`values.yaml` et le même `pg_isready` déjà utilisé par les sondes de `postgres` lui-même, pour bloquer
`backend` tant que `postgres` n'accepte pas réellement les connexions — la manière idiomatique
Kubernetes d'exprimer cette dépendance d'ordre, plutôt que de simplement tolérer le bruit du
crash-loop. Revérifié sur une installation entièrement propre : zéro redémarrage sur les quatre pods.

## Deux bugs réels de délai de démarrage du cluster, corrigés par une nouvelle tentative réelle

Trouvés en réinstallant réellement le chart depuis zéro, pas supposés :

1. `kubectl wait` appelé juste après `kubectl apply` peut s'exécuter avant même que le contrôleur
   Deployment n'ait créé l'objet Pod — kubectl répond alors immédiatement `no matching resources found`
   plutôt que d'attendre qu'un pod apparaisse. Corrigé avec une boucle de nouvelle tentative
   (`Wait-ForPodsReady`, même forme de correctif que la condition de course PostgreSQL de P16).
2. Même une fois le pod du contrôleur ingress-nginx `Ready`, son webhook d'admission
   (`ingress-nginx-controller-admission`) peut mettre un instant de plus à accepter réellement des
   connexions — `helm upgrade --install` a échoué une fois avec
   `dial tcp ...:443: connect: connection refused` en essayant de créer l'`Ingress`. Corrigé en
   réessayant l'opération réelle (`helm upgrade --install`) plutôt qu'une attente à durée fixe devinée.

Ces deux mêmes bugs se sont révélés latents dans `labs/kubernetes/scripts/up.ps1` (P17, déjà fusionné) —
corrigés là aussi avec exactement le même schéma, puis revérifiés par un run complet réel de ce script :
zéro redémarrage, `curl` à travers l'Ingress confirmant `/` (200) et `/api/v1/auth/me` anonyme (401, vrai
`ProblemDetail`).

## Vérification réelle

`helm lint` (0 échec) et `helm template` (12 ressources rendues : 3 `Deployment`, 3 `Service`, 3
`ConfigMap`, 1 `Secret`, 1 `PersistentVolumeClaim`, 1 `Ingress`) vérifiés avant toute installation réelle.
Cluster créé, chart réellement installé (`helm upgrade --install --wait`, `STATUS: deployed`), zéro
redémarrage sur les quatre pods à l'état stable, `curl` à travers `http://localhost/` confirmant `/`
(200), `/healthz` (200) et `/api/v1/auth/me` anonyme (401, vrai `ProblemDetail` avec `requestId`), puis
une vraie inscription via `POST /api/v1/auth/register` confirmant la chaîne complète jusqu'à PostgreSQL
(201). Cluster détruit proprement ensuite (`down.ps1`).
