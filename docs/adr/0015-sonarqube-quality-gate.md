# ADR-0015 — SonarQube Community Build local et éphémère pour la Quality Gate

- Statut : Accepted
- Date : 2026-08-15

## Contexte

La phase 4 exige une Quality Gate automatisée sans dépendre d'un service payant. Taskira n'a pas de serveur distant ni de compte SonarCloud, et le poste de développement ne doit pas devenir un runner self-hosted persistant pour un dépôt potentiellement public.

## Décision

Utiliser SonarQube Community Build en conteneur Docker, avec sa propre base PostgreSQL dédiée (`infra/sonarqube/docker-compose.yml`), démarré à la demande :

- en local, via `docker compose -f infra/sonarqube/docker-compose.yml up -d` puis `scripts/sonarqube/bootstrap.ps1` pour faire tourner les identifiants admin par défaut, créer le projet `taskira` et générer un jeton local (écrit uniquement dans `infra/sonarqube/.env.local`, ignoré par Git) ;
- en CI (`quality.yml`), la même stack est démarrée de façon éphémère sur le runner GitHub-hosted, analysée, vérifiée puis détruite avec ses volumes dans le même job. Aucun serveur Sonar distant persistant n'est requis et aucun secret GitHub n'est nécessaire.

Le scanner s'exécute via l'image officielle épinglée par digest `sonarsource/sonar-scanner-cli`, cohérent avec l'approche Docker-first du dépôt.

## Limite connue de l'édition Community

L'édition Community ne fournit pas la décoration de pull request ni l'analyse multi-branches native. Le Quality Gate ne s'applique donc qu'à l'analyse de la branche par défaut lors de chaque exécution CI, pas à un diff de PR décoré directement dans l'interface GitHub. Passer à une édition payante ou à SonarCloud n'est pas retenu pour ce projet; cette limite est documentée plutôt que contournée.

## Conséquences

- Le pipeline peut échouer explicitement si la Quality Gate échoue (`quality.yml`), conformément à l'exigence de la phase 4.
- Aucun secret GitHub (`SONAR_TOKEN` distant) n'est nécessaire; le jeton est généré et consommé dans le même job.
- Le rapport de couverture JaCoCo/LCOV doit exister avant le scan (backend `verify`, frontend `test:coverage`), donc `quality.yml` reconstruit ces artefacts indépendamment de `ci.yml`.
- La configuration de projet est centralisée dans `sonar-project.properties` à la racine du dépôt.
