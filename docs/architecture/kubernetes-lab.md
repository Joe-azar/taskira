# Lab Kubernetes local

Voir [ADR-0024](../adr/0024-kubernetes-lab.md) pour les décisions complètes. Ce document est la référence
opérationnelle : comment démarrer le lab, ce qu'il démontre, comment le détruire.

Ce lab est pédagogique et jetable — voir AGENTS.md §38 : il ne remplace pas le runtime Docker Compose
principal (`infra/`), et rien dans `infra/`, dans l'application ou dans la CI n'en dépend.

## Ce qui est déployé

```text
labs/kubernetes/
  cluster/kind-config.yaml            configuration du cluster kind (1 nœud, ports 80/443 exposés)
  vendor/ingress-nginx-kind-deploy.yaml   manifeste officiel ingress-nginx, épinglé controller-v1.15.1
  manifests/
    00-namespace.yaml
    01-configmap.yaml                 postgres-config, backend-config
    02-postgres-secret.example.yaml   gabarit seulement, jamais appliqué directement
    03-postgres.yaml                  PVC + Deployment (strategy: Recreate) + Service
    04-backend.yaml                   Deployment + Service, image GHCR réelle v0.1.0 (P15)
    05-frontend.yaml                  ConfigMap nginx corrigé + Deployment (2 replicas) + Service
    06-ingress.yaml
  scripts/
    up.ps1            crée le cluster, déploie tout, idempotent
    down.ps1           détruit le cluster entier
    demo-rollout.ps1   scaling + rolling update + rollback réels
```

Les images backend/frontend déployées sont les vraies images publiées sur GHCR par la release `v0.1.0`
de P15 — pas des images reconstruites pour l'occasion (ADR-0024).

## Démarrer le lab

```powershell
& .\labs\kubernetes\scripts\up.ps1 -PostgresPassword "un-mot-de-passe-local-seulement"
```

Le mot de passe n'est jamais écrit sur disque ni commité — il n'existe que dans le `Secret` Kubernetes
généré à la volée. Une fois prêt :

```powershell
curl http://localhost/                    # 200, l'app Angular
curl http://localhost/healthz              # 200
curl http://localhost/api/v1/auth/me       # 401 ProblemDetail réel, avec requestId
kubectl get all -n taskira
```

## Démontrer scaling, rolling update et rollback

```powershell
& .\labs\kubernetes\scripts\demo-rollout.ps1
```

Exécute réellement, dans l'ordre :

1. `kubectl scale deployment/frontend --replicas=3` puis attend et affiche les pods réellement démarrés.
2. Construit une image backend locale depuis la source courante (`docker build`), la charge dans le
   cluster (`kind load docker-image` — jamais publiée sur GHCR, le pipeline de release réel de P15 n'est
   jamais touché par ce lab), puis `kubectl set image` et `kubectl rollout status`.
3. `kubectl rollout undo deployment/backend`, puis `kubectl rollout history` et une vérification directe
   de l'image du Deployment pour confirmer le retour à `ghcr.io/joe-azar/taskira-backend:v0.1.0`.

## Détruire le lab

```powershell
& .\labs\kubernetes\scripts\down.ps1
```

Supprime le cluster `kind` entier (tous les namespaces, tout le PVC, toutes les données) — le lab est
jetable par construction, rien n'y est censé survivre entre deux exécutions.

## Deux bugs réels trouvés en démarrant réellement le lab

Aucun des deux n'était visible avant de vraiment démarrer le cluster et vraiment envoyer une requête à
travers l'Ingress — ni `kubectl apply` seul ni une relecture des manifestes ne les auraient révélés.

1. **`resolver 127.0.0.11 valid=10s;`** : ce résolveur est celui de Docker (DNS intégré des réseaux
   définis par l'utilisateur), présent dans `infra/docker-compose*.yml` mais absent de l'espace de noms
   réseau d'un pod Kubernetes. Nginx journalisait `send() failed (111: Connection refused) ... resolver:
   127.0.0.11:53` et toute requête `/api/` échouait en `502`.
2. **Le nom court `backend` ne se résout pas depuis le résolveur intégré de Nginx**, même après avoir
   corrigé le point 1 (`backend could not be resolved (2: Server failure)`) : contrairement à
   `wget`/`curl`/la JVM, le résolveur intégré de Nginx interroge le DNS avec le nom littéral configuré et
   n'applique jamais la liste `search` de `/etc/resolv.conf` (`taskira.svc.cluster.local` en premier,
   `ndots:5`) — c'est cette expansion, propre aux résolveurs basés sur libc, qui fait fonctionner un nom
   court partout ailleurs dans le pod.

Corrigés sans jamais toucher à `frontend/nginx/default.conf` — ce fichier reste inchangé, partagé et déjà
validé par trois runtimes Docker Compose (dev/prodlike/staging). `labs/kubernetes/manifests/05-frontend.yaml`
monte un `ConfigMap` qui remplace `/etc/nginx/conf.d/default.conf` à l'intérieur du pod, identique au
fichier réel à deux lignes près : `resolver kube-dns.kube-system.svc.cluster.local valid=10s;` (le nom de
Service DNS interne réel du cluster, confirmé contre `kubectl get svc -n kube-system` et le
`/etc/resolv.conf` d'un pod réel) et `proxy_pass` vers `http://backend.taskira.svc.cluster.local:8080`
(nom pleinement qualifié) au lieu du nom court `backend`.

## Vérification réelle

Cycle complet exécuté contre le vrai cluster, pas seulement décrit : `up.ps1` a construit un cluster kind,
déployé ingress-nginx (`controller-v1.15.1`), PostgreSQL (PVC + Deployment), le backend et le frontend
(images GHCR réelles `v0.1.0`), et l'Ingress. Après les deux correctifs ci-dessus, `curl` à travers
`http://localhost/` a confirmé `/` (200), `/healthz` (200) et `/api/v1/auth/me` anonyme (401, vrai corps
`ProblemDetail` avec `requestId` réel) — puis une vraie inscription via `POST /api/v1/auth/register` a
confirmé la chaîne complète jusqu'à PostgreSQL (utilisateur créé, `201`). `demo-rollout.ps1` a ensuite
démontré scaling (2 → 3 replicas frontend, pods réellement observés), rolling update (nouvelle image
locale réellement construite et chargée, `kubectl rollout status` confirmant le déploiement), et rollback
(`kubectl rollout undo`, retour confirmé à `ghcr.io/joe-azar/taskira-backend:v0.1.0`). `down.ps1` a
ensuite détruit le cluster entier proprement.
