# Architecture de déploiement

Statut : développement Compose opérationnel; P11 (production-like) terminée localement, pas encore fusionnée; staging et labs planifiés.

## Développement actuel

```text
Angular dev server :4200
Spring Boot       :8080
PostgreSQL        :5432
```

Docker Compose reste l'environnement de référence. Angular conserve le hot reload; le backend utilise une image multi-stage Java 21.

## Production-like P11 (terminée localement)

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

## Livraison P15/P16

- Images GHCR versionnées par SemVer/SHA.
- Staging Ubuntu/Compose/Nginx et Playwright post-déploiement.
- Production manuelle, versionnée et rollbackable.
- Backups PostgreSQL avec restauration testée, pas seulement `pg_dump` produit.

## Labs

- P17 Kubernetes, P18 Helm et P19 Azure.
- Aucun de ces labs ne remplace le runtime principal ni ne déclenche une dépense cloud sans contexte explicite.

Voir [ADR-0008](../adr/0008-docker-compose.md) et [ADR-0011](../adr/0011-kubernetes-training-only.md).
