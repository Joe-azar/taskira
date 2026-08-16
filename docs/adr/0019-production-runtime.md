# ADR-0019 — Runtime production-like Nginx/non-root

- Statut : Accepted
- Date : 2026-08-16

## Contexte

Le développement utilise `infra/docker-compose.yml` : Angular servi par le serveur de développement (`ng serve`, hot reload), backend et PostgreSQL avec ports publiés pour le confort local. Rien ne prouve aujourd'hui que l'application fonctionne servie par un vrai serveur statique de production, derrière un reverse proxy, avec des conteneurs non-root et une exposition réseau minimale — le seul artefact de production réel jusqu'ici est le `backend/Dockerfile` multi-stage (image runtime distincte du build), sans plus.

P11 introduit ce runtime « production-like » : local, HTTP (pas de certificat réel), mais avec la même topologie réseau, le même reverse proxy et les mêmes contraintes d'exposition qu'une vraie production. GHCR, staging réel, TLS et déploiement VM restent hors périmètre (P15+).

## Décision

### Nginx non-root

`nginxinc/nginx-unprivileged`, image maintenue par l'organisation NGINX elle-même pour ce cas d'usage exact, plutôt que l'image `nginx` standard reconfigurée à la main. Vérifié par inspection directe du conteneur avant adoption : tourne déjà en `uid=101(nginx)`, écoute par défaut sur le port non privilégié `8080`, PID dans `/tmp` (`drwxrwxrwt`), `/var/cache/nginx` déjà accessible en écriture au groupe `nginx`. Aucun contournement manuel de permissions n'est nécessaire — seul `conf.d/default.conf` est remplacé par la configuration applicative (SPA + reverse proxy).

### Séparation par rapport au build context Angular

`frontend/nginx/default.conf` (pas `infra/nginx/`) : le contexte de build Docker de `frontend/Dockerfile.prod` reste `frontend/`, cohérent avec `frontend/Dockerfile` et `frontend/Dockerfile.e2e` existants — Docker ne peut pas `COPY` un fichier situé hors de son build context sans élargir ce contexte à la racine du dépôt, ce qui aurait ralenti le build et cassé la convention par-projet déjà en place.

### URL API relative

`environment.ts` (configuration `production` utilisée par `ng build` par défaut) passe de `http://localhost:8080/api/v1` (URL absolue, un artefact qui n'a jamais été pensé pour un déploiement réel) à `/api/v1` (relative). Le navigateur appelle alors la même origine que celle servie par Nginx, qui route en interne vers `backend:8080` — condition nécessaire pour que les cookies de session `SameSite=Lax` et la double-soumission CSRF fonctionnent sans configuration CORS supplémentaire. `environment.development.ts` (`ng serve`) n'est pas concerné : le serveur de développement Angular ne passe jamais par Nginx.

### Cookie de session et HTTP local

Conflit réel découvert avant toute implémentation : `application-prod.yaml` fixe `server.servlet.session.cookie.secure: true`, mais ce runtime sert du HTTP simple sur `:8080` (pas de certificat, TLS réel planifié en P15+ avec une vraie terminaison HTTPS). Un cookie `Secure` n'est jamais envoyé par le navigateur sur une connexion HTTP — lancer le profil `prod` tel quel casserait silencieusement l'authentification.

Résolu par variable d'environnement, uniquement dans `infra/docker-compose.prodlike.yml`, sans toucher au fichier `application-prod.yaml` : `SERVER_SERVLET_SESSION_COOKIE_SECURE=false`. Les variables d'environnement OS ont une priorité supérieure aux fichiers `application-{profile}.yaml` packagés dans le jar dans l'ordre de résolution des propriétés Spring Boot, donc cette variable l'emporte sur `secure: true` sans recompilation ni changement de code. Le reste du comportement `prod` (Swagger désactivé, logs structurés JSON) reste actif tel quel. Le défaut réel de `application-prod.yaml` n'est pas modifié : une vraie production derrière HTTPS (P15+) doit continuer à recevoir `Secure=true` sans cette variable.

### Segmentation réseau

Trois réseaux Docker plutôt qu'un seul réseau par défaut, contrairement à `infra/docker-compose.yml` (dev) :

```text
app_net           frontend, backend
db_net            backend, postgres
observability_net backend, prometheus, grafana
```

Le frontend ne peut donc jamais joindre PostgreSQL, même en cas de mauvaise configuration Nginx — la segmentation réseau est la garde-fou, pas seulement l'absence de route applicative. Seul le service `frontend` publie un port hôte (`${FRONTEND_HTTP_PORT:-8080}`); `backend` (8080 et 9091), `postgres` (5432), `prometheus` (9090) et `grafana` (3000) restent strictement internes.

### Observabilité optionnelle

Prometheus/Grafana restent dans `infra/docker-compose.prodlike.yml` mais sous `profiles: ["observability"]` — cohérent avec la décision déjà prise que les outils d'observabilité doivent rester séparables ([ADR-0012](0012-observability-stack.md)). Le runtime minimal (`docker compose -f infra/docker-compose.prodlike.yml up`) démarre uniquement frontend/backend/PostgreSQL; `--profile observability` ajoute Prometheus/Grafana, toujours sans port hôte publié par défaut — contrairement au Compose de développement, qui les publie pour la commodité locale.

### Non-root backend

Utilisateur/groupe `taskira` créés explicitement dans le stage runtime de `backend/Dockerfile` (`groupadd`/`useradd`, `COPY --chown`, `USER taskira`) plutôt que de dépendre de l'utilisateur `ubuntu` (uid 1000) présent accidentellement dans l'image de base `eclipse-temurin` — un détail d'implémentation de l'image de base, pas une garantie contractuelle, qui pourrait disparaître à une future mise à jour de l'image sans avertissement. `backend/Dockerfile` est partagé avec `infra/docker-compose.yml` (dev) et `e2e/playwright/compose.e2e.yml` : ce changement est donc vérifié contre les trois usages, pas seulement le runtime production-like.

## Conséquences

- Un runtime HTTP local ne prouve pas la compatibilité HTTPS réelle (redirection, HSTS, certificats) — hors périmètre de P11, traité en P15+.
- `SERVER_SERVLET_SESSION_COOKIE_SECURE=false` est une exception documentée et confinée à `infra/docker-compose.prodlike.yml`; toute vraie production doit repartir du défaut `application-prod.yaml` sans cette variable.
- La segmentation réseau à trois réseaux ajoute une petite complexité de configuration Compose par rapport à un réseau unique, jugée justifiée par le gain réel d'isolation pour un runtime explicitement présenté comme durci.
- Les headers de sécurité Nginx introduits restent volontairement simples (`X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy`) : pas de CSP (nécessiterait des tests dédiés contre le bundle Angular réel pour éviter de casser l'application) ni de HSTS (n'a pas de sens sur un runtime HTTP sans TLS) à ce stade.
