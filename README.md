# Taskira

Taskira est une plateforme web de gestion de tickets, tâches et anomalies, conçue comme un projet full-stack avec Angular, Spring Boot, PostgreSQL et Flyway. L'authentification repose sur une session côté serveur (cookie `HttpOnly`) et une protection CSRF, pas sur un jeton JWT (voir [ADR-0006](docs/adr/0006-session-cookie-auth.md)).

## Structure du projet

- `backend/` : API Spring Boot
- `frontend/` : application Angular
- `infra/` : environnement Docker Compose local
- `docs/` : documentation complémentaire

## Documentation

- [Règles de travail et de contribution](AGENTS.md)
- [Rapport cumulatif de migration](ENTERPRISE_MIGRATION_REPORT.md)
- [Matrice de migration par module](MIGRATION_MATRIX.md)
- [Architecture actuelle et cible progressive](docs/architecture.md)
- [Stratégie de tests](docs/testing-strategy.md)
- [Feuille de route des phases 0 à 20](docs/migration-matrix.md)
- [Index des décisions d'architecture](docs/adr/README.md)

## Développement local avec Docker

### Prérequis

- Git
- Docker Desktop avec WSL2
- VS Code

Java, Maven, Node.js, npm, Angular CLI et PostgreSQL sont fournis par les conteneurs Docker. Leur installation globale sur Windows n'est pas nécessaire.

### Cloner

```powershell
git clone https://github.com/Joe-azar/taskira.git
cd taskira
```

### Démarrer

Depuis la racine du dépôt :

```powershell
docker compose -f infra/docker-compose.yml up --build
```

Pour démarrer en arrière-plan :

```powershell
docker compose -f infra/docker-compose.yml up -d --build
```

### Vérifier l'état et consulter les logs

```powershell
docker compose -f infra/docker-compose.yml ps
docker compose -f infra/docker-compose.yml logs -f
```

### URLs locales

- Frontend : http://localhost:4200
- Backend : http://localhost:8080
- Swagger : http://localhost:8080/swagger-ui.html
- OpenAPI : http://localhost:8080/v3/api-docs

### Premier compte

Une base neuve ne contient aucun utilisateur créé par l'application elle-même, mais le profil `dev` (celui utilisé par `infra/docker-compose.yml`) crée automatiquement un compte administrateur au démarrage s'il n'existe pas déjà : `admin@taskira.test` / `Taskira-Admin-42!` par défaut, surchargeable via les variables d'environnement `APP_DEV_ADMIN_EMAIL`/`APP_DEV_ADMIN_PASSWORD`. Ce bootstrap est strictement réservé au profil `dev` et n'existe jamais en profil `prod`.

Un compte supplémentaire (rôle `USER`) peut être créé avec `POST /api/v1/auth/register` depuis Swagger.

### Arrêter

```powershell
docker compose -f infra/docker-compose.yml down
```

Pour supprimer également la base locale et repartir d'un schéma vide :

```powershell
docker compose -f infra/docker-compose.yml down -v
```

> Attention : `down -v` supprime définitivement toutes les données PostgreSQL locales du volume Docker.

### Développement

Le frontend est monté dans le conteneur et recompilé automatiquement lorsqu'un fichier Angular change. Une modification Java nécessite actuellement de reconstruire et recréer le backend :

```powershell
docker compose -f infra/docker-compose.yml up -d --build backend
```
