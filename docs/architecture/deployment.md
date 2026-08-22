# Architecture de déploiement

Statut : développement Compose opérationnel; P11 (production-like) terminée et fusionnée; P15 (registry GHCR, release, staging, rollback) terminée localement, pas encore fusionnée; labs planifiés.

## Développement actuel

```text
Angular dev server :4200
Spring Boot       :8080
PostgreSQL        :5432
```

Docker Compose reste l'environnement de référence. Angular conserve le hot reload; le backend utilise une image multi-stage Java 21.

## Production-like P11 (terminée et fusionnée)

```text
Client
  |  HTTP (pas de certificat - TLS réel planifié en P15+)
  v
Nginx :8080 (nginxinc/nginx-unprivileged, non-root, publié)
  |-- /               -> build Angular statique (index.html sans cache, assets hashés en cache long)
  |-- /healthz        -> 200 (signal de santé dédié)
  `-- /api/*          -> proxy_pass http://backend:8080 (résolution DNS différée via resolver Docker)
       |
       v (app_net)
     Spring Boot :8080/:9091 (non-root, aucun port publié)
       |
       v (db_net)
     PostgreSQL :5432 (aucun port publié)

Prometheus/Grafana : profil Compose optionnel "observability", réseau observability_net dédié, aucun port publié même actif.
```

`infra/docker-compose.prodlike.yml` (fichier séparé de `infra/docker-compose.yml`, nom de projet Compose explicite pour éviter toute collision) segmente trois réseaux Docker (`app_net`, `db_net`, `observability_net`) : le frontend ne peut jamais joindre PostgreSQL, même en cas de mauvaise configuration Nginx — la segmentation réseau est la garde-fou, pas seulement l'absence de route applicative. Seul le service `frontend` publie un port hôte.

`SPRING_PROFILES_ACTIVE=prod` est actif dans ce runtime, avec une exception documentée : `SERVER_SERVLET_SESSION_COOKIE_SECURE=false` en variable d'environnement (jamais dans `application-prod.yaml`) puisque ce runtime sert du HTTP local sans certificat — le défaut réel de production (`Secure=true`) reste inchangé pour une vraie mise en HTTPS ultérieure. Voir [ADR-0019](../adr/0019-production-runtime.md) pour le détail complet, y compris les bugs réels trouvés uniquement en démarrant réellement la stack complète (collision de nom de projet Compose, résolution DNS Nginx, écoute IPv6).

Secrets requis (`POSTGRES_PASSWORD`, `GRAFANA_ADMIN_PASSWORD`) via `${VAR:?message}` — le Compose refuse de démarrer sans eux plutôt que d'utiliser un mot de passe faible par défaut. Aucun secret réel commité; `infra/.env.prodlike.example` documente les variables.

## Registry et staging P15 (terminée localement, pas encore fusionnée)

```text
git push origin vX.Y.Z
  |
  v .github/workflows/release.yml
Build + push ghcr.io/joe-azar/taskira-{backend,frontend}:vX.Y.Z et :<SHA>
  |
  v
Déploiement réel sur runner Ubuntu via infra/docker-compose.staging.yml
(pull direct depuis GHCR, jamais un build local)
  |
  v
Suite Playwright complète (e2e/playwright/tests/) exécutée contre le staging réel
  |
  v (seulement si tout précède réussit réellement)
GitHub Release publiée (notes auto-générées)
```

`infra/docker-compose.staging.yml` reprend la topologie à trois réseaux de `docker-compose.prodlike.yml` (P11) — `db_net`/`app_net`, seul `frontend` publie un port — sans Prometheus/Grafana (déjà validés en P10/P11, staging se concentre sur la vérification de déploiement, pas la duplication de l'observabilité). Seule différence structurelle : chaque service backend/frontend référence `image: ghcr.io/joe-azar/taskira-{backend,frontend}:${VERSION:?...}` au lieu d'un bloc `build:` — c'est le seul fichier Compose de ce dépôt qui ne construit jamais d'image localement. `VERSION` (tag ou SHA publié) et `POSTGRES_PASSWORD` sont requis via `${VAR:?message}`, `infra/.env.staging.example` documente les variables (jamais de secret réel commité, même convention que `.env.prodlike.example`). Voir [ADR-0013](adr/0013-versioning-strategy.md) pour les décisions complètes de versionnage.

### Rollback

Redéployer une version antérieure ne rejoue jamais une migration Flyway en sens inverse (`AGENTS.md` §13, migrations forward-only) : il s'agit uniquement de redéployer une image applicative antérieure, déjà compatible avec le schéma en place.

```powershell
# Rollback local ou sur tout hôte Docker atteignable :
# 1. Éditer infra/.env.staging (VERSION=<tag ou SHA antérieur))
# 2. Redéployer :
docker compose -p taskira-staging -f infra/docker-compose.staging.yml --env-file infra/.env.staging up -d
```

Le mécanisme complet (pull d'une image immuable référencée par tag, healthcheck, resservi derrière le même Nginx) est exactement celui déjà exercé par le workflow de release lui-même à chaque publication — rollback et déploiement normal empruntent le même chemin de code, pas une procédure séparée et non testée.

## Livraison P16+

- Production manuelle, versionnée et rollbackable (au-delà du staging local/CI de P15 — un vrai hôte de production reste hors périmètre tant qu'aucune décision d'hébergement n'est prise).
- Backups PostgreSQL avec restauration testée, pas seulement `pg_dump` produit.

## Labs

- P17 Kubernetes, P18 Helm et P19 Azure.
- Aucun de ces labs ne remplace le runtime principal ni ne déclenche une dépense cloud sans contexte explicite.

Voir [ADR-0008](../adr/0008-docker-compose.md) et [ADR-0011](../adr/0011-kubernetes-training-only.md).
