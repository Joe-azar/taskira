# Taskira

Taskira est une plateforme web de gestion de tickets, tâches et anomalies, conçue comme un projet full-stack avec Angular, Spring Boot, PostgreSQL, Flyway et JWT.

## Structure du projet

- `backend/` : API Spring Boot
- `frontend/` : application Angular
- `infra/` : environnement Docker Compose local
- `docs/` : documentation complémentaire

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

Une base neuve ne contient aucun utilisateur. Le premier compte peut être créé avec `POST /api/auth/register` depuis Swagger. L'inscription crée un utilisateur actif avec le rôle `USER`; aucun administrateur n'est initialisé automatiquement.

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
