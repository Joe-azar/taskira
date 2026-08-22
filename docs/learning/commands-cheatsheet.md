# Fiche de commandes — Taskira

Commandes ajoutées progressivement, partie par partie. Toutes adaptées à PowerShell et au dépôt
`D:\ALL DATA\France\Taskira`.

## Partie 1 — Docker

### Démarrer / arrêter la stack de développement

```powershell
# Démarrer tout (construit les images si nécessaire)
docker compose -f infra/docker-compose.yml up -d

# Voir l'état des 6 services
docker compose -f infra/docker-compose.yml ps

# Arrêter sans supprimer (les données restent)
docker compose -f infra/docker-compose.yml stop

# Redémarrer ce qui a été arrêté par `stop`
docker compose -f infra/docker-compose.yml start

# Supprimer les containers (les données restent, dans les volumes)
docker compose -f infra/docker-compose.yml down

# ⚠️ Supprimer containers ET données PostgreSQL/Grafana/pièces jointes - IRRÉVERSIBLE
docker compose -f infra/docker-compose.yml down -v
```

### Inspecter

```powershell
# Containers en cours d'exécution
docker ps

# Tous les containers, même arrêtés
docker ps -a

# Toutes les images sur la machine
docker images

# Volumes (là où vivent les données)
docker volume ls

# Réseaux
docker network ls
```

### Un service en particulier

```powershell
# Logs (remplacer taskira-backend par le nom du container voulu)
docker logs taskira-backend
docker logs -f taskira-backend        # en direct

# Entrer dans un container
docker exec -it taskira-postgres psql -U taskira -d taskira   # client SQL direct
docker exec -it taskira-backend sh                              # shell dans le backend
docker exec -it taskira-frontend sh                             # shell dans le frontend

# Redémarrer un seul service
docker restart taskira-backend

# Vérifier si un container est "healthy"
docker inspect --format='{{.State.Health.Status}}' taskira-backend
```

### Une vraie requête SQL sans ouvrir de session interactive

```powershell
docker exec -it taskira-postgres psql -U taskira -d taskira -c "SELECT id, email, global_role FROM users;"
```

## Partie 2 — Backend + PostgreSQL + Flyway

```powershell
# Se connecter à PostgreSQL en session interactive
docker exec -it taskira-postgres psql -U taskira -d taskira
# \dt              liste les tables
# \d tickets        décrit une table
# \q                quitter

# Migrations Flyway réellement appliquées
docker exec -it taskira-postgres psql -U taskira -d taskira -c "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;"

# Compter des lignes
docker exec -it taskira-postgres psql -U taskira -d taskira -c "SELECT status, count(*) FROM tickets GROUP BY status;"

# Vérifier que Flyway a bien tourné au démarrage
docker logs taskira-backend 2>&1 | Select-String "Flyway"

# Vérifier quel profil Spring est actif
docker logs taskira-backend 2>&1 | Select-String "The following"
```

## Partie 3 — Frontend Angular

```powershell
# Ouvrir l'application
start http://localhost:4200

# Suivre les requêtes réelles reçues par le backend pendant que tu navigues
docker logs -f taskira-backend
```

Dans le navigateur : F12 → onglet Network pour inspecter les vraies requêtes/réponses HTTP et les
cookies (`TASKIRA_SESSION`, `XSRF-TOKEN`).

## Partie 4 — Authentification et sécurité

```powershell
# Sans être connecté - doit refuser (401)
curl.exe -i http://localhost:8080/api/v1/tickets

# Amorcer une session anonyme pour récupérer le cookie CSRF
curl.exe -i -c cookies.txt http://localhost:8080/api/v1/auth/me
Get-Content cookies.txt

# Login SANS l'en-tête CSRF - doit échouer en 403 (preuve que la protection fonctionne)
curl.exe -i -b cookies.txt -X POST http://localhost:8080/api/v1/auth/login `
  -H "Content-Type: application/json" `
  -d '{\"email\":\"admin@taskira.test\",\"password\":\"Taskira-Admin-42!\"}'
```

## Partie 5 — Tests

```powershell
# Backend complet (rapides + intégration + ModularityTests)
docker build --target build -t taskira-backend-build backend
docker run --rm `
  -e TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal `
  -v /var/run/docker.sock:/var/run/docker.sock `
  -v taskira_maven_cache:/root/.m2 `
  -v "${PWD}\backend:/workspace" `
  -w /workspace `
  taskira-backend-build `
  ./mvnw verify

# Seulement les tests rapides (sans Docker/Testcontainers)
docker run --rm -v taskira_maven_cache:/root/.m2 -v "${PWD}\backend:/workspace" -w /workspace taskira-backend-build ./mvnw test

# Seulement ModularityTests
docker run --rm -v taskira_maven_cache:/root/.m2 -v "${PWD}\backend:/workspace" -w /workspace taskira-backend-build ./mvnw -Dtest=ModularityTests test

# Frontend
docker build -f frontend/Dockerfile -t taskira-frontend-tests frontend
docker run --rm taskira-frontend-tests npm run lint
docker run --rm taskira-frontend-tests npm run test:unit
docker run --rm taskira-frontend-tests npm run test:coverage
docker run --rm taskira-frontend-tests npm run build

# E2E complet (construit et détruit sa propre stack isolée)
& .\e2e\playwright\run.ps1
```

## Partie 6 — GitHub Actions + SonarQube + CodeQL + Trivy

```powershell
# Valider la syntaxe de tous les workflows sans rien exécuter (aucun accès GitHub requis)
docker run --rm -v "${PWD}:/repo" -w /repo rhysd/actionlint:1.7.12

# Analyse SonarQube en local (mêmes scripts que le workflow quality.yml)
docker compose -f infra/sonarqube/docker-compose.yml up -d
& .\scripts\sonarqube\bootstrap.ps1
docker compose -f infra/sonarqube/docker-compose.yml down -v
```

## Partie 7 — Observabilité

```powershell
# Santé et métriques brutes, depuis l'intérieur du réseau Docker uniquement
docker exec taskira-backend wget -qO- http://localhost:9091/actuator/health
docker exec taskira-backend wget -qO- http://localhost:9091/actuator/prometheus | Select-String "taskira_"

# Depuis l'hôte, doit échouer (preuve que le port 9091 n'est pas publié)
curl.exe http://localhost:9091/actuator/health

# Interfaces web
start http://localhost:9090/targets    # Prometheus - vérifier que la cible backend est UP
start http://localhost:9090/graph      # Prometheus - taper "taskira_tickets" par exemple
start http://localhost:3000            # Grafana (admin / taskira par défaut)
```

## Partie 8 — Nginx + production-like + GHCR + release/staging

```powershell
# Voir la vraie config Nginx utilisée par un container en cours d'exécution
docker exec taskira-frontend-prodlike cat /etc/nginx/conf.d/default.conf

# Démarrer la stack production-like localement (nécessite infra/.env.prodlike)
docker compose -f infra/docker-compose.prodlike.yml --env-file infra/.env.prodlike up -d --build

# Déployer une version précise déjà publiée sur GHCR (staging) - VERSION doit exister
$env:VERSION = "v0.1.0"
$env:POSTGRES_PASSWORD = "un-mot-de-passe-local"
docker compose -p taskira-staging -f infra/docker-compose.staging.yml up -d
docker compose -p taskira-staging -f infra/docker-compose.staging.yml down --volumes --remove-orphans

# Voir les tags réellement publiés d'une image GHCR (nécessite d'être connecté)
docker pull ghcr.io/joe-azar/taskira-backend:v0.1.0
```

## Partie 9 — Backup / restore

```powershell
# Sauvegarder la base de développement réelle
& .\scripts\backup\backup-postgres.ps1

# Restaurer dans un container jetable et voir les comptes de lignes par table
& .\scripts\restore\restore-postgres.ps1 -DumpFile ".\backups\taskira-20260822-120000.dump"

# Nettoyer le container de vérification après inspection
docker rm -f taskira-postgres-restore-check
```

## Partie 10 — Kubernetes + kind

```powershell
# Créer le cluster de lab et déployer Taskira dedans
& .\labs\kubernetes\scripts\up.ps1 -PostgresPassword "un-mot-de-passe-de-lab"

# Vérifier que ça répond réellement à travers l'Ingress
curl.exe http://localhost/
curl.exe http://localhost/healthz
curl.exe http://localhost/api/v1/auth/me   # 401 attendu, anonyme

# Observer les Pods
kubectl get pods -n taskira
kubectl get pods -n taskira -w             # en direct
kubectl describe pod <nom-du-pod> -n taskira
kubectl logs <nom-du-pod> -n taskira

# Scaling manuel
kubectl scale deployment/frontend --replicas=3 -n taskira

# Rolling update / rollback (voir aussi demo-rollout.ps1)
kubectl rollout status deployment/backend -n taskira
kubectl rollout undo deployment/backend -n taskira
kubectl rollout history deployment/backend -n taskira

# Démonstration complète (scaling + rolling update + rollback)
& .\labs\kubernetes\scripts\demo-rollout.ps1

# Détruire le cluster proprement
& .\labs\kubernetes\scripts\down.ps1
```

## Partie 11 — Helm

```powershell
# Vérifier le Chart sans rien installer
helm lint .\labs\helm\taskira
helm template .\labs\helm\taskira --set postgres.password=un-mot-de-passe-de-lab

# Vérifier que "required" bloque bien un rendu sans mot de passe (doit échouer)
helm template .\labs\helm\taskira

# Installer/mettre à jour pour de vrai
& .\labs\helm\scripts\up.ps1 -PostgresPassword "un-mot-de-passe-de-lab"

helm list -n taskira
helm status taskira -n taskira
helm get values taskira -n taskira

# Désinstaller
helm uninstall taskira -n taskira
& .\labs\helm\scripts\down.ps1
```

## Partie 12 — Terraform + Azure

⚠️ Seules ces trois commandes sont utilisées dans ce lab - jamais `terraform plan`/`terraform apply`.

```powershell
cd labs\azure

terraform init          # télécharge le provider, aucun identifiant Azure requis
terraform fmt -check    # vérifie le style, ne modifie rien
terraform validate      # vérifie la cohérence syntaxique, entièrement hors ligne
terraform validate -json  # même chose, sortie exploitable par script
```

## Partie 13 — 20 commandes de survie

Les 20 commandes à connaître par cœur pour se débrouiller seul sur Taskira au quotidien, sans avoir
à rouvrir ce document à chaque fois.

```powershell
# 1. Démarrer toute la stack de développement
docker compose -f infra/docker-compose.yml up -d

# 2. Voir l'état de tous les services
docker compose -f infra/docker-compose.yml ps

# 3. Logs d'un service en direct
docker logs -f taskira-backend

# 4. Arrêter sans perdre de données
docker compose -f infra/docker-compose.yml stop

# 5. Tout supprimer SAUF les données (sûr)
docker compose -f infra/docker-compose.yml down

# 6. ⚠️ Tout supprimer Y COMPRIS les données (irréversible)
docker compose -f infra/docker-compose.yml down -v

# 7. Requête SQL directe sans session interactive
docker exec -it taskira-postgres psql -U taskira -d taskira -c "SELECT * FROM users LIMIT 5;"

# 8. Vérifier les migrations Flyway réellement appliquées
docker exec -it taskira-postgres psql -U taskira -d taskira -c "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;"

# 9. Ouvrir l'application dans le navigateur
start http://localhost:4200

# 10. Tester l'API sans être connecté (doit répondre 401)
curl.exe -i http://localhost:8080/api/v1/auth/me

# 11. Backend : suite complète de tests (rapides + intégration)
docker build --target build -t taskira-backend-build backend
docker run --rm -e TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal -v /var/run/docker.sock:/var/run/docker.sock -v taskira_maven_cache:/root/.m2 -v "${PWD}\backend:/workspace" -w /workspace taskira-backend-build ./mvnw verify

# 12. Frontend : lint + tests + build
docker build -f frontend/Dockerfile -t taskira-frontend-tests frontend
docker run --rm taskira-frontend-tests npm run lint
docker run --rm taskira-frontend-tests npm run test:unit

# 13. E2E complet (construit et détruit sa propre stack)
& .\e2e\playwright\run.ps1

# 14. Vérifier la santé du backend (depuis l'intérieur du réseau Docker uniquement)
docker exec taskira-backend wget -qO- http://localhost:9091/actuator/health

# 15. Sauvegarder la base de développement
& .\scripts\backup\backup-postgres.ps1

# 16. Restaurer une sauvegarde dans un container jetable pour vérification
& .\scripts\restore\restore-postgres.ps1 -DumpFile ".\backups\<nom-du-fichier>.dump"

# 17. Valider la syntaxe de tous les workflows GitHub Actions sans rien exécuter
docker run --rm -v "${PWD}:/repo" -w /repo rhysd/actionlint:1.7.12

# 18. Voir toutes les images Docker présentes sur la machine
docker images

# 19. Voir tous les volumes (là où vivent les données réelles)
docker volume ls

# 20. En cas de doute total : repartir d'une stack fraîche sans perdre les données
docker compose -f infra/docker-compose.yml down
docker compose -f infra/docker-compose.yml up -d --build
```
