# Diagnostic — Taskira

Scénarios de panne, construits progressivement au fil de la formation. Plus de 40 scénarios au total
à ce stade - largement au-delà des 20 initialement visés. La méthode générale de diagnostic (que
faire face à un problème encore jamais vu) et les fiches de référence transversales (comparatif des
environnements, ports) sont dans [`taskira-from-zero.md`](taskira-from-zero.md), Partie 13.

## Partie 1 — Docker

| Symptôme | Cause probable | Première commande à taper |
|---|---|---|
| `port is already allocated` au démarrage | Un autre programme (ou un vieux container Taskira jamais arrêté) utilise déjà ce port | `docker ps -a` pour voir ce qui existe déjà |
| Un service reste bloqué sur `starting` dans `docker compose ps` | Son healthcheck échoue en boucle | `docker logs <nom-du-service>` |
| `Cannot connect to the Docker daemon` | Docker Desktop n'est pas lancé sur Windows | Ouvrir Docker Desktop, réessayer |
| Le frontend ne recharge pas après une modification | Bind mount ou polling en souci (rare) | `docker logs -f taskira-frontend` |
| `volume already exists but was not created by Docker Compose` | Avertissement normal (le volume `taskira_postgres_data_pg18` existait déjà avant que Compose le gère) | Aucune action nécessaire |

## Partie 2 — Backend + PostgreSQL + Flyway

| Symptôme | Cause probable | Première commande à taper |
|---|---|---|
| `Validate failed: Migrations have failed validation` au démarrage | Un fichier de migration déjà appliqué a été modifié après coup (empreinte Flyway différente) | `docker logs taskira-backend \| Select-String "Flyway"` |
| `Schema-validation: missing column` | Une entité Java référence une colonne que Flyway n'a jamais créée | Comparer l'entité avec `\d nom_table` dans `psql` |
| `relation "xxx" does not exist` en SQL direct | Faute de frappe, ou migration pas encore appliquée | `\dt` dans `psql` pour voir les tables réelles |
| Le backend refuse de démarrer avec une erreur JPA | Désynchronisation entité/schéma réel | Comparer l'entité Java avec `\d nom_table` |

## Partie 3 — Frontend Angular

| Symptôme | Cause probable | Première commande à taper |
|---|---|---|
| Page blanche après connexion | Boucle guard/intercepteur (401 mal interprété comme session expirée) | F12 → onglet Console, regarder les erreurs |
| `Cannot GET /tickets/42` en rechargeant directement | Le serveur ne redirige pas vers `index.html` (SPA fallback manquant) | Voir la config Nginx en Partie 8 |
| 403 sur une requête qui modifie des données | Cookie CSRF absent/expiré | F12 → Network → vérifier le cookie `XSRF-TOKEN` et l'en-tête `X-XSRF-TOKEN` |
| Le formulaire refuse de s'envoyer | Un `Validator` Reactive Forms bloque | Inspecter `form.errors` dans les DevTools Angular ou ajouter un `console.log` temporaire |

## Partie 4 — Authentification et sécurité

| Symptôme | Cause probable | Première commande à taper |
|---|---|---|
| `403` sur une requête `POST`/`PUT`/`PATCH`/`DELETE` | En-tête `X-XSRF-TOKEN` manquant ou incohérent avec le cookie | F12 → Network → vérifier le cookie `XSRF-TOKEN` et l'en-tête envoyé |
| `401` juste après une connexion réussie | Le cookie `TASKIRA_SESSION` n'a pas été renvoyé par le client | Vérifier que l'appel utilise bien `withCredentials`/les cookies du navigateur |
| Connexion refusée avec un mot de passe correct | Compte désactivé (`active = false` en base) | `docker exec -it taskira-postgres psql -U taskira -d taskira -c "SELECT email, active FROM users WHERE email = '...';"` |
| `CORS error` dans la console du navigateur | L'origine appelante n'est pas dans `app.cors.allowed-origins` | Vérifier `APP_CORS_ALLOWED_ORIGINS` dans `infra/docker-compose.yml` |

## Partie 5 — Tests

| Symptôme | Cause probable | Première commande à taper |
|---|---|---|
| `Could not find a valid Docker environment` pendant un test | Le socket Docker n'est pas monté dans le conteneur qui lance Maven | Vérifier `-v /var/run/docker.sock:/var/run/docker.sock` |
| Un test échoue seulement de temps en temps | Contention de ressources (plusieurs stacks Docker en même temps) | `docker ps` pour voir ce qui tourne en plus |
| Playwright échoue avec un timeout générique | La stack de dev manuelle tourne en même temps que la stack E2E isolée | `docker compose -f infra/docker-compose.yml stop` avant de relancer |
| `mkdir C:\Program Files\Git\...: Access is denied` en montant un volume | Traduction de chemin par Git Bash | Préférer PowerShell pour ces commandes |

## Partie 6 — GitHub Actions + SonarQube + CodeQL + Trivy

| Symptôme | Cause probable | Première commande à taper |
|---|---|---|
| `CI Gate` rouge sur GitHub | Un des 3 jobs (Backend/Frontend/E2E) a échoué | Ouvrir le run sur github.com et lire les logs du job précis en rouge |
| Un check GitHub rouge mais tout vert en local | Différence d'environnement (version d'outil, secret manquant sur le runner) | Comparer les versions exactes utilisées dans le workflow YAML |
| SonarQube Quality Gate rouge | Nouveau bug/vulnérabilité/seuil de couverture non atteint | Lire le rapport détaillé du job `quality.yml` |
| Trivy signale une CVE | Une dépendance (directe ou transitive) ou une image de base contient une faille connue | Vérifier si c'est une dépendance applicative directe ou hors périmètre (voir `ENTERPRISE_MIGRATION_REPORT.md`) |

## Partie 7 — Observabilité

| Symptôme | Cause probable | Première commande à taper |
|---|---|---|
| Cible `taskira-backend` à `DOWN` dans Prometheus | Backend arrêté, ou port de gestion mal aligné entre `application.yaml` et `prometheus.yml` | `docker compose -f infra/docker-compose.yml ps` puis `docker logs taskira-backend` |
| `/actuator/health` répond `503` | Un sous-composant `readiness` (souvent `db`) est en échec | `docker exec taskira-backend wget -qO- http://localhost:9091/actuator/health` |
| Le port 9091 répond depuis l'hôte Windows | Publication accidentelle du port dans `docker-compose.yml` | Relire la section `ports:` du service `backend` |
| Grafana affiche "No data" sur un dashboard | Cible Prometheus down, ou plage de temps du dashboard ne couvrant pas maintenant | Vérifier d'abord `http://localhost:9090/targets` |

## Partie 8 — Nginx + production-like + GHCR + release/staging

| Symptôme | Cause probable | Première commande à taper |
|---|---|---|
| `502 Bad Gateway` sur `/api/...` | Backend pas encore démarré, ou nom de service Docker incorrect | `docker logs taskira-backend` puis vérifier `resolver`/`proxy_pass` dans `default.conf` |
| `Cannot GET /tickets/42` en rechargeant directement une page | Fallback SPA (`try_files ... /index.html`) absent ou mal placé dans la config Nginx | Relire `frontend/nginx/default.conf`, section `location /` |
| Nginx refuse de démarrer avec "host not found in upstream" | `proxy_pass` statique résolu une seule fois, avant que `backend` n'existe sur le réseau | Vérifier que `proxy_pass` passe bien par une variable (`set $backend_upstream`) |
| Pas de GitHub Release après un tag `v*.*.*` poussé | `staging-smoke-test` a échoué - `publish-release` ne s'exécute jamais dans ce cas | Lire les logs du job `staging-smoke-test` sur GitHub Actions |

## Partie 9 — Backup / restore

| Symptôme | Cause probable | Première commande à taper |
|---|---|---|
| `the database system is shutting down` pendant `pg_restore` | Restauration tentée contre le serveur PostgreSQL temporaire du point d'entrée officiel, pas encore le vrai | Relire la boucle de nouvelle tentative dans `restore-postgres.ps1` |
| `Container '...' already exists` au lancement de `restore-postgres.ps1` | Un ancien container de vérification traîne encore | `docker rm -f taskira-postgres-restore-check` |
| Une valeur `$null`/vide en capturant `backup-postgres.ps1` | Confusion entre `Write-Host` (console uniquement) et une vraie valeur de retour | Vérifier que la dernière ligne du script est une expression nue, pas un `Write-Host` |
| Comptes de lignes à 0 après restauration | Fichier `.dump` incorrect ou sauvegarde échouée silencieusement en amont | Relancer une sauvegarde fraîche et comparer sa taille en octets |

## Partie 10 — Kubernetes + kind

| Symptôme | Cause probable | Première commande à taper |
|---|---|---|
| `502` sur toute requête `/api/` à travers l'Ingress | Résolveur DNS Docker (`127.0.0.11`) absent d'un Pod Kubernetes | `kubectl logs <pod-frontend> -n taskira` |
| `backend could not be resolved (2: Server failure)` | Nom de service court utilisé, résolveur Nginx n'applique jamais la liste `search` du pod | Utiliser le nom pleinement qualifié `backend.taskira.svc.cluster.local` |
| Pod PostgreSQL bloqué en `Pending` | PVC non provisionné ou volume encore tenu par un ancien Pod | `kubectl describe pod <pod-postgres> -n taskira` |
| `no matching resources found` juste après un `kubectl apply` | Commande lancée trop tôt, le contrôleur n'a pas encore créé l'objet | Réessayer l'opération quelques secondes après |

## Partie 11 — Helm

| Symptôme | Cause probable | Première commande à taper |
|---|---|---|
| `execution error ... postgres.password is required` | Comportement voulu de la fonction `required` - aucun mot de passe fourni | Passer `--set postgres.password=...` ou un fichier `-f` |
| `backend` en `CrashLoopBackOff` malgré l'`initContainer` | PostgreSQL n'a pas encore réellement démarré | `kubectl get pods -n taskira` puis `kubectl logs <pod-postgres> -n taskira` |
| `cannot re-use a name that is still in use` | Une ancienne Release du même nom existe déjà dans le namespace | `helm uninstall taskira -n taskira` avant de réinstaller |
| `helm upgrade --install --wait` reste bloqué très longtemps | Webhook d'admission ingress-nginx ou API server pas encore prêt | Patienter puis relancer la même commande |

## Partie 12 — Terraform + Azure

| Symptôme | Cause probable | Première commande à taper |
|---|---|---|
| `terraform validate` échoue sur un argument de ressource | Version du provider différente de celle verrouillée | Vérifier `.terraform.lock.hcl` face à `versions.tf` |
| `terraform init` échoue | Généralement un problème réseau vers le Registry Terraform public, pas Azure | Réessayer, vérifier la connexion internet |
| Une commande demande soudain une connexion Azure | Ne devrait jamais arriver avec `init`/`fmt`/`validate` seuls | Vérifier qu'aucun `plan`/`apply` n'a été tapé par erreur |
