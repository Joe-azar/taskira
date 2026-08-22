# ADR-0025 — Chart Helm construit après les manifestes bruts

- Statut : Accepted
- Date : 2026-08-22

## Contexte

La feuille de route (`docs/migration-matrix.md`, P18) fixe le critère de sortie : « Chart créé après les
manifestes bruts; `helm lint`, `helm template` et déploiement local réussissent. » P17 a produit et
réellement vérifié un ensemble complet de manifestes Kubernetes bruts pour Taskira
(`labs/kubernetes/manifests/`) — la base explicitement requise par la feuille de route avant d'écrire un
chart, pas un point de départ arbitraire.

## Décision

### `labs/helm/taskira/`, un chart applicatif seulement — pas ingress-nginx

Le chart empaquette uniquement ce que P17 a modélisé comme « l'application Taskira » : namespace,
ConfigMaps, Secret (valeur jamais commitée), PostgreSQL (PVC + Deployment + Service), backend, frontend et
Ingress. ingress-nginx reste une installation d'infrastructure de cluster préalable, identique à celle de
P17 (`labs/kubernetes/vendor/ingress-nginx-kind-deploy.yaml`, réutilisée telle quelle) — un vrai chart
applicatif d'entreprise ne réinstalle généralement pas le contrôleur Ingress partagé du cluster à chaque
déploiement d'application, et mélanger les deux aurait rendu le chart moins représentatif de ce que P18
est censé enseigner.

### Paramétrage via `values.yaml`, pas de duplication du YAML brut de P17

Chaque valeur qui variait déjà entre un déploiement de développement et un déploiement réel dans P17
devient un paramètre de `values.yaml` : tags d'image backend/frontend (défaut `v0.1.0`, les vraies images
GHCR de P15), nombre de replicas frontend, taille du volume PostgreSQL, taille de sauvegarde/restauration
Ingress (`proxy-body-size`). Le mot de passe PostgreSQL n'a **aucune valeur par défaut dans
`values.yaml`** — `required` explicite côté template (fonction Helm `required`, échoue au rendu si absente)
plutôt qu'une valeur d'exemple qui inviterait à l'oubli, cohérent avec `-PostgresPassword` obligatoire de
`scripts/up.ps1` (P17) et avec `infra/.env.prodlike.example`/`infra/.env.staging.example`.

### Le correctif DNS de P17 devient un template, pas une note à part

Le `ConfigMap` qui surcharge `/etc/nginx/conf.d/default.conf` (P17, deux bugs réels : résolveur DNS de
Docker absent d'un pod, résolveur intégré de Nginx n'appliquant jamais la liste `search` de
`/etc/resolv.conf`) devient `templates/frontend-nginx-configmap.yaml`, paramétré par
`.Release.Namespace` plutôt que la valeur `taskira` codée en dur du lab brut — un chart Helm doit rester
correct sous n'importe quel nom de release/namespace, pas seulement celui utilisé pendant le lab.

### Validation avant tout déploiement réel : `helm lint` puis `helm template`

`helm lint` détecte les erreurs structurelles du chart avant de solliciter un cluster. `helm template`
rend les manifestes finaux et les compare visuellement au résultat déjà validé de P17 avant tout
`helm install` réel — la même discipline que ce dépôt applique déjà à `docker compose config` avant un
`up` réel (P11/P15) ou `actionlint` avant de pousser un workflow.

### Trois bugs réels trouvés en installant réellement le chart, pas supposés

1. **Course de démarrage PostgreSQL/backend** : `helm upgrade --install --wait` soumet tous les templates
   ensemble et n'attend la disponibilité qu'à la fin — contrairement au script raw-manifest de P17, qui
   sérialise explicitement `postgres` avant `backend`. La première installation réelle a fait
   crash-looper `backend` deux fois avant que le redémarrage automatique ne réussisse une fois `postgres`
   prêt. Corrigé avec un `initContainer` bloquant sur `pg_isready` (réutilisant l'image `postgres` déjà
   pinnée et le même outil déjà utilisé par les sondes de `postgres`), plutôt que de tolérer le bruit du
   crash-loop.
2. **`kubectl wait` appelé trop tôt** : peut s'exécuter avant que le contrôleur Deployment n'ait créé
   l'objet Pod, et répond alors `no matching resources found` immédiatement plutôt que d'attendre.
   Corrigé avec une boucle de nouvelle tentative sur l'opération réelle.
3. **Webhook d'admission ingress-nginx pas encore prêt** : même une fois le pod du contrôleur `Ready`,
   son webhook d'admission peut mettre un instant de plus à accepter des connexions — `helm upgrade
   --install` a échoué une fois avec `connection refused` en créant l'`Ingress`. Corrigé en réessayant
   l'opération réelle plutôt qu'une attente à durée fixe devinée.

Les bugs 2 et 3 se sont révélés latents dans `labs/kubernetes/scripts/up.ps1` (P17, déjà fusionné) —
corrigés là aussi avec le même schéma, puis revérifiés par un run complet réel de ce script.

## Conséquences

- Le chart dépend implicitement d'ingress-nginx déjà installé sur le cluster cible — documenté dans
  `docs/architecture/kubernetes-lab.md`, pas géré comme une dépendance Helm formelle (`Chart.yaml`
  `dependencies:`) : ingress-nginx n'est pas un composant de l'application Taskira.
- Comme P17, ce chart reste un lab jetable (AGENTS.md §38) : rien dans le runtime principal, l'application
  ou la CI n'en dépend.
