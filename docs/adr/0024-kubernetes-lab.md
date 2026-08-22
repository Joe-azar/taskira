# ADR-0024 — Lab Kubernetes local, séparé du runtime principal

- Statut : Accepted
- Date : 2026-08-22

## Contexte

AGENTS.md §38 est explicite : Kubernetes est un environnement de formation et de démonstration pour
Taskira, pas un remplacement du runtime Docker Compose principal sans décision explicite. La feuille de
route (`docs/migration-matrix.md`, P17) fixe le critère de sortie : « kind/k3d déploie manifests,
Ingress, probes, ConfigMaps, Secrets et PVC; scaling/update/rollback démontrés. » Rien de tout cela
n'existe encore dans ce dépôt.

## Décision

### `kind`, pas k3d ni minikube ni le Kubernetes intégré de Docker Desktop

`kind` (Kubernetes IN Docker) fait tourner ses nœuds comme des conteneurs Docker ordinaires, directement
sur le moteur Docker déjà utilisé par tout le reste de ce dépôt (`infra/docker-compose*.yml`, la suite
E2E) — aucune bascule de paramètre Docker Desktop, aucune fonctionnalité globale à activer/désactiver
sur le poste, un cluster jetable créé et détruit à la demande, cohérent avec la philosophie déjà établie
pour la stack E2E (P2) et pour le staging de P15. Le binaire (`kind.exe` v0.32.0, Windows amd64) a été
téléchargé depuis la release GitHub officielle et son SHA-256 vérifié contre le fichier
`kind-windows-amd64.sha256sum` publié par le projet avant toute exécution.

### Lab séparé du runtime principal : `labs/kubernetes/`, pas `infra/`

`infra/` reste exclusivement le runtime Docker Compose (dev, production-like P11, staging P15) — AGENTS.md
§8 nomme explicitement `labs/` comme l'emplacement des technologies étudiées sans devenir des dépendances
obligatoires. Rien dans `labs/kubernetes/` n'est requis pour démarrer, tester ou déployer Taskira par les
chemins déjà validés.

### Réutilisation des images GHCR réelles de P15, pas des images locales inventées

`ghcr.io/joe-azar/taskira-backend:v0.1.0` et `taskira-frontend:v0.1.0` (P15) sont des images publiques,
réellement publiées, déjà testées par 10/10 Playwright contre un vrai déploiement staging. Les déployer
telles quelles dans le lab est un test plus honnête qu'une image reconstruite localement pour l'occasion
— exactement le scénario d'un vrai cluster qui tire une version déjà publiée d'un registre. Le frontend
de production (P11, image Nginx) proxifie déjà `/api/` vers un hôte nommé littéralement `backend` (voir
`frontend/nginx/default.conf`) — le Service Kubernetes du backend est donc nommé `backend` pour que ce
même artefact fonctionne sans aucune modification, ni nouvelle configuration Nginx spécifique à
Kubernetes à maintenir en parallèle.

### `Deployment` + PVC pour PostgreSQL, pas un `StatefulSet`

Une seule replica PostgreSQL, exactement comme dans `infra/docker-compose*.yml` — aucun besoin des
garanties d'identité réseau stable ou de scaling ordonné qu'un `StatefulSet` apporte pour un ensemble
multi-replica. `strategy: Recreate` (pas `RollingUpdate`, le défaut) parce qu'un PVC `ReadWriteOnce` ne
peut être monté que par un pod à la fois — `RollingUpdate` tenterait de démarrer le nouveau pod avant
d'arrêter l'ancien et resterait bloqué en attente du volume. `kind` fournit une `StorageClass` par défaut
(`standard`, provisioning dynamique local-path) : aucun `PersistentVolume` manuel à écrire.

### Secret jamais commité, généré à l'apply, même discipline que P6/P11/P15

Aucune valeur réelle de mot de passe PostgreSQL n'est écrite dans un manifeste versionné. Le script
`scripts/up.ps1` exige un paramètre `-PostgresPassword` obligatoire (pas de valeur par défaut) et crée le
`Secret` de façon impérative (`kubectl create secret ... --dry-run=client -o yaml | kubectl apply -f -`)
— même idiome que `infra/.env.prodlike.example`/`infra/.env.staging.example` (P11/P15) : un fichier
`.example` documente la forme attendue, jamais une vraie valeur.

### Ingress-nginx vendu dans le dépôt, épinglé par version

Manifeste officiel `kind`-spécifique d'ingress-nginx (`controller-v1.15.1`, déjà épinglé par digest par
le projet amont pour ses propres images) copié dans `labs/kubernetes/vendor/` plutôt que récupéré depuis
`main` à chaque exécution — reproductible même si la branche amont change, cohérent avec la préférence
déjà établie dans ce dépôt contre les références flottantes pour un composant dont dépend tout le reste
du lab.

### Démonstration réelle de scaling/update/rollback, pas seulement des manifestes déclaratifs

Le critère de sortie de P17 exige que scaling/update/rollback soient *démontrés*, pas seulement rendus
possibles par la présence de `Deployment`. `scripts/demo-rollout.ps1` exécute réellement : `kubectl scale`
sur le frontend, une vraie image de mise à jour du backend (reconstruite localement depuis `main` avec un
tag distinct, chargée dans le cluster via `kind load docker-image` — sans jamais publier de nouvelle
version sur GHCR ni toucher au pipeline de release réel de P15, qui reste réservé à de vraies décisions de
version) via `kubectl set image` puis `kubectl rollout status`, et enfin `kubectl rollout undo` avec
`kubectl rollout history` pour prouver le retour à l'image précédente.

### Deux bugs réels trouvés en démarrant réellement le lab, pas supposés

Aucun `kubectl apply --dry-run` ni relecture de manifeste n'aurait révélé ceci — seule une vraie requête
à travers l'Ingress d'un vrai cluster l'a fait. L'image frontend publique de P15
(`ghcr.io/joe-azar/taskira-frontend:v0.1.0`) embarque `frontend/nginx/default.conf` tel quel, jamais écrit
en pensant à Kubernetes :

1. `resolver 127.0.0.11 valid=10s;` est le DNS intégré de Docker, absent de l'espace de noms réseau d'un
   pod — `502` sur toute requête `/api/`, `send() failed (111: Connection refused) ... resolver:
   127.0.0.11:53` dans les logs Nginx.
2. Même corrigé vers `kube-dns.kube-system.svc.cluster.local` (le vrai Service DNS interne du cluster,
   confirmé contre `kubectl get svc -n kube-system`), le nom court `backend` restait irrésolu
   (`backend could not be resolved (2: Server failure)`) : le résolveur intégré de Nginx interroge le DNS
   avec le nom littéral configuré, sans jamais appliquer la liste `search` de `/etc/resolv.conf` du pod
   — contrairement à `wget`/`curl`/la JVM, qui reposent sur cette expansion côté libc. Corrigé en utilisant
   le nom pleinement qualifié `backend.taskira.svc.cluster.local` dans `proxy_pass`.

Corrigés sans jamais modifier `frontend/nginx/default.conf` lui-même (partagé, déjà validé par trois
runtimes Compose) : `labs/kubernetes/manifests/05-frontend.yaml` monte un `ConfigMap` qui remplace
`/etc/nginx/conf.d/default.conf` à l'intérieur du pod, identique au fichier réel à ces deux lignes près.

## Conséquences

- Le lab est jetable par construction (`scripts/down.ps1` détruit tout le cluster `kind`) : aucune donnée
  qui y est créée n'est censée survivre, cohérent avec son rôle pédagogique/démonstratif plutôt que de
  production.
- Les pièces jointes (P13) n'ont aucun stockage persistant dans ce lab (pas de PVC dédié) : leur
  démonstration n'est pas l'objet de P17, et le stockage éphémère du conteneur suffit pour l'exercice.
- Aucune dépense cloud, aucune ressource Azure : ce lab tourne entièrement sur le poste local, cohérent
  avec l'interdiction d'AGENTS.md §38 de créer automatiquement une infrastructure cloud payante.
- Prometheus/Grafana (P10) ne sont pas déployés dans ce lab : l'observabilité elle-même est déjà validée
  ailleurs (P10/P11), et le dupliquer ici n'apporterait rien au critère de sortie de P17.
