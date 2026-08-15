# Rapport cumulatif de migration enterprise

Dernière mise à jour : 2026-08-15.

Ce fichier est le journal central de la migration. Il doit être enrichi après chaque grande phase avec les commandes, résultats, écarts et décisions réellement observés.

## Verdict actuel

`TASKIRA ENTERPRISE MIGRATION PARTIALLY COMPLETE`

Les phases 0, 1, 2 et 4 sont terminées localement. Le pipeline distant de phase 3 est vert, mais la phase reste partielle tant que `main` n'est pas protégée. Aucune authentification GitHub interactive (`gh auth login`) n'est disponible sur ce poste : ceci bloque la protection de `main` (P3) et la vérification distante des nouveaux workflows `quality.yml`, `security.yml` et `codeql.yml` (P4). C'est une action humaine, pas un choix technique. Les phases 5 à 20 ne sont pas déclarées terminées.

## Journal des phases

| Date | Phase | Statut | Résultat vérifié | Reste à faire |
| --- | ---: | --- | --- | --- |
| 2026-08-14 | 0 — Baseline Git | Terminée | Commit `fd84c54`, tag `pre-enterprise-migration`, branche `feat/enterprise-platform-migration`; stack Docker restaurée et validée avant migration. | Aucun pour le critère de baseline; conserver le point de retour. |
| 2026-08-15 | 1 — Documentation | Terminée localement | `AGENTS.md`, matrices, rapport, documentation d'architecture et ADR créés; liens et cohérence vérifiés; commit local `cccf2ee`. | Publier la branche avec les autres lots validés. |
| 2026-08-14–15 | 2 — Filet de sécurité | Terminée localement | 14 tests backend, 20 Vitest, couvertures/seuils backend et frontend, build Angular et 9/9 Playwright validés dans Docker; stack E2E isolée détruite après le run. Rejoué et reconfirmé le 15/08 lors de l'audit de phase 4. | Maintenir ce filet. Ajouter désarchivage projet et suppression ticket en P7, après création de leurs endpoints. |
| 2026-08-15 | 3 — GitHub Actions CI | Partielle | [PR draft #1](https://github.com/Joe-azar/taskira/pull/1), HEAD `6db6115`; [run #3](https://github.com/Joe-azar/taskira/actions/runs/31851279947) vert : Backend, Frontend avec lint, Containers and E2E et CI Gate. | Activer la protection de `main`; `protected=false` et aucun ruleset sont encore observés. |
| 2026-08-15 | 4 — SonarQube et scans | Terminée localement | SonarQube Community Build éphémère (Docker) : Quality Gate `OK`, 0 bug, 0 vulnérabilité, 0 security hotspot, 24 code smells, couverture 13,0 %, duplication 1,4 %, 9010 ncloc. Deux bugs d'accessibilité détectés puis corrigés avant la seconde analyse. Trivy (fs + 2 images) et CodeQL configurés et exécutés localement; 0 secret détecté. | Vérifier `quality.yml`/`security.yml`/`codeql.yml` sur un run GitHub distant dès l'authentification disponible; traiter les CVE identifiées en P6. |
| — | 5–20 | Planifiées | Aucun critère de sortie déclaré atteint. | Suivre [la feuille de route](docs/migration-matrix.md) dans l'ordre. |

## Incident de stockage et récupération

Le 15 août 2026, le worktree initial a rencontré l'erreur Windows `ERROR 1117` signalant une erreur d'entrée/sortie. Il a été immédiatement mis en quarantaine et aucune nouvelle écriture n'y a été effectuée.

La baseline a été récupérée depuis `origin` dans un clone sain situé sur un autre volume local. La branche de migration et le tag ont ensuite été recréés localement à partir de cette baseline, puis les livrables non commités ont été reconstruits et revérifiés dans ce clone.

Le diagnostic et la réparation éventuelle du matériel, du filesystem ou de Windows restent hors du dépôt. Cet incident ne modifie pas le statut fonctionnel des phases; il explique uniquement le changement de worktree de travail.

Effet résiduel constaté le 15 août 2026 lors de l'audit de phase 4 : la stack Docker de développement (`infra/docker-compose.yml`) tournait encore avec un bind mount pointant vers l'ancien worktree `C:\Users\joeaz\Taskira-enterprise-recovery`, désormais vidé de son contenu utile, ce qui mettait `taskira-frontend` en boucle de redémarrage (`npm error ENOENT ... package.json`). Corrigé par `docker compose down` puis `up -d --build` depuis le worktree courant; les trois conteneurs (`postgres`, `backend`, `frontend`) sont revenus sains. Aucune donnée applicative n'a été perdue (le volume nommé `taskira_postgres_data` ne dépend pas du chemin hôte).

## État avant migration

- Windows 11, Docker Desktop/WSL2 et Git opérationnels.
- Monorepo `backend/`, `frontend/`, `infra/`, `docs/`.
- API Java 21/Spring Boot 3.5.11, Angular 21, PostgreSQL 16 et Flyway `V1`–`V6` fonctionnels via Docker Compose.
- Architecture backend déjà groupée par fonctionnalités et frontend organisé en `core/`, `layout/`, `features/`.
- Authentification JWT stateless; jeton conservé dans `localStorage`; CSRF désactivé dans l'état courant.
- Très faible couverture automatique avant le renforcement de phase 2.

## Architecture cible

La décision cible est un monolithe modulaire feature-first avec architecture hexagonale légère pour les modules complexes. Le runtime principal doit rester Angular/Nginx, Spring Boot et PostgreSQL. Les microservices, Kafka, RabbitMQ, Redis et moteurs de recherche ne deviennent pas des dépendances principales sans besoin mesuré et ADR.

Voir [l'architecture générale](docs/architecture/overview.md) et [ADR-0001](docs/adr/0001-modular-monolith.md).

## Versions observées

| Composant | Version actuelle validée | Cible planifiée |
| --- | --- | --- |
| Java | 21 | 21 LTS conservé |
| Spring Boot | 3.5.11 | 4.x en P6 |
| Maven | Wrapper 3.9.12 | 3.9.x |
| PostgreSQL | 16; Testcontainers et E2E 16.15 | 17/18 en P6 après compatibilité |
| Angular | 21.2.x | 22.x en P6 |
| TypeScript | 5.9.x | Version compatible Angular 22 en P6 |
| Node/npm | Node 22; npm 11.9.0 | Node 24 LTS en P6 |
| Vitest | 4.x | Branche compatible Angular cible |
| Playwright | 1.62.1; Node 22.23.2/npm 11.9.0 dans le runner P2 | Réutilisé par le workflow local P3; run distant à valider |

## Outils ajoutés ou renforcés

- Testcontainers PostgreSQL et base de test partagée pour les intégrations.
- Séparation Maven Surefire (`*Test`) et Failsafe (`*IT`).
- Tests JUnit 5, Mockito, AssertJ, MockMvc et Flyway réels.
- Tests Angular/Vitest ciblés sur auth, guards et intercepteur.
- Runner Playwright Docker racine avec 9 scénarios et stack Compose E2E isolée.
- JaCoCo 0.8.13 fusionne les résultats Surefire/Failsafe, produit XML/HTML et applique un seuil ligne global; `coverage-v8` 4.1.0 produit LCOV/HTML et applique quatre seuils frontend.
- Workflow GitHub Actions `ci.yml` créé et validé statiquement avec `actionlint` 1.7.12.
- Lint Angular intégré avec `angular-eslint` 21.4.0, ESLint 10.3.0 et `typescript-eslint` 8.59.2.
- Maven Wrapper 3.9.12 et images Java, Node et PostgreSQL critiques épinglés; les images E2E Node/PostgreSQL sont référencées par digest.
- SonarQube Community Build (Docker, base PostgreSQL dédiée) : `infra/sonarqube/docker-compose.yml`, bootstrap idempotent `scripts/sonarqube/bootstrap.ps1`, configuration `sonar-project.properties`. Exécuté localement en mode éphémère dans `.github/workflows/quality.yml`.
- Trivy (`aquasecurity/trivy-action` pinée par SHA) : scan fs (dépendances + secrets) et scan image (backend, frontend) avec upload SARIF vers l'onglet Security GitHub, dans `.github/workflows/security.yml`.
- CodeQL (`github/codeql-action` pinée par SHA) : analyse `java-kotlin` et `javascript-typescript` sur push/PR et hebdomadaire, dans `.github/workflows/codeql.yml`.
- Dependabot (`.github/dependabot.yml`) : maven (backend), npm (frontend), docker (backend/frontend/e2e) et github-actions, mise à jour hebdomadaire groupée par écosystème majeur (Spring Boot, Angular).

Le run GitHub distant #3 (P3) est vert. Les workflows `quality.yml`, `security.yml` et `codeql.yml` (P4) sont validés localement et avec `actionlint` mais pas encore vérifiés sur un run GitHub distant, faute d'authentification GitHub interactive sur ce poste. Nginx production, observabilité et labs ne sont pas encore ajoutés.

## Tests

Résultat validé de phase 2 :

- backend rapide : 11 tests;
- backend intégration/Testcontainers : 3 tests;
- backend total : 14 tests;
- frontend Vitest : 20 tests;
- frontend lint : 0 erreur et 41 avertissements `any` non bloquants;
- navigateur : Playwright 1.62.1, 9/9 tests en 1,3 minute;
- build Angular de production : réussi.

Les parcours navigateur couvrent login/logout, login invalide, redirection du guard anonyme, refus de l'administration à un `USER`, projet create/update/archive, membre add/remove, ticket create/update/status/assign et commentaire create/update/delete.

Couverture validée le 15 août 2026 :

- backend JaCoCo : lignes 20,17 %, branches 3,75 %, instructions 18,11 %, méthodes 23,01 %, classes 42,25 %; seuil ligne 19 % atteint, `./mvnw verify` exit 0;
- frontend V8 : statements 12,44 %, branches 11,27 %, fonctions 11,78 %, lignes 11,84 %; seuils respectifs 12/11/11/11 % atteints, 20/20 tests et commande exit 0.

La commande racine `& .\e2e\playwright\run.ps1` construit et exécute `e2e/playwright/compose.e2e.yml`. PostgreSQL 16.15 utilise un `tmpfs`; aucun service n'expose de port hôte et la stack ne définit ni `container_name` ni volume persistant pour la base. Les données générées emploient le domaine réservé `.test`. Le `finally` exécute `down --volumes --remove-orphans`; le contrôle final a confirmé zéro conteneur, réseau ou volume restant. Les rapports Playwright et résultats sont écrits dans des répertoires ignorés par Git et restent disponibles après la destruction. Voir [la stratégie de tests](docs/testing-strategy.md).

Le désarchivage projet et la suppression ticket ne sont pas simulés : les endpoints correspondants sont absents. Leur ajout et leurs E2E constituent un gap explicite de P7, pas un échec du critère P2.

## CI/CD

Le workflow `.github/workflows/ci.yml` définit :

- backend Java 21 avec `./mvnw verify` et rapports tests/JaCoCo;
- frontend Node 22.23.2/npm 11.9.0 avec lint, couverture, 20 Vitest et build;
- même fichier Compose E2E éphémère, attente de disponibilité, Playwright, logs/artifacts et job de destruction systématique.

Les actions tierces sont pinées par SHA, les permissions globales sont `contents: read` et `persist-credentials` est désactivé. `actionlint` 1.7.12 passe localement. Sur la PR draft #1 au HEAD `6db6115`, le run GitHub #3 (`31851279947`) valide Backend, Frontend, Containers and E2E et CI Gate. Le lint Angular passe avec 0 erreur et 41 avertissements liés à la dette `any`.

P3 reste partielle pour une seule raison : `main` est encore signalée `protected=false` et aucun ruleset n'est configuré. Le connecteur a reçu `403` sur les réglages administratifs. La protection est disponible sans coût supplémentaire, mais son activation nécessite l'interface GitHub ou un jeton administrateur interactif; aucune élévation n'est contournée. GHCR, release, staging et production manuelle relèvent de P15.

## Résultats de la phase 4 (SonarQube et scans)

Analyse SonarQube Community Build `26.8.0.126808` exécutée localement le 15 août 2026 sur l'intégralité du dépôt (`sonar-project.properties` à la racine, backend Java + frontend TypeScript en une seule analyse) :

- Quality Gate (profil `Sonar way`) : `OK`.
- Bugs : 0 (2 détectés à la première analyse — `Web:InputWithoutLabelCheck` sur deux champs sans label accessible — corrigés avant la seconde analyse; voir commit d'accessibilité).
- Vulnérabilités : 0. Security hotspots : 0. Notes fiabilité/sécurité/maintenabilité : A/A/A.
- Code smells : 24 (dette estimée 140 minutes).
- Couverture : 13,0 %. Duplication : 1,4 %. Lignes de code analysées : 9010.
- Secrets : 0 détecté (scanner intégré `TextAndSecretsSensor`).

Trivy (CLI `aquasec/trivy`, cache `~/.cache/trivy` réutilisé) exécuté localement le 15 août 2026 :

- `backend/pom.xml` : 21 vulnérabilités (17 HIGH, 4 CRITICAL) sur `jackson-core`/`jackson-databind`, `tomcat-embed-core`, `postgresql` (pilote JDBC), `spring-boot`, `spring-data-commons`, `spring-security-web`, `spring-expression`/`spring-webmvc`. La quasi-totalité n'est corrigée que par une version cible Spring Boot 4.x/Spring 7 (P6); aucun correctif isolé sûr n'existe dans la ligne 3.5.x pour plusieurs d'entre elles.
- `frontend/package-lock.json` : 8 vulnérabilités HIGH sur `@angular/common`, `@angular/compiler`, `@angular/core`, corrigées uniquement à partir d'Angular 22.x (P6).
- Image `infra-backend` : mêmes CVE applicatives (jar) que `pom.xml`, plus 8 CVE HIGH sur le binaire Go `pebble` embarqué dans l'image de base `eclipse-temurin` (couche OS, hors du code applicatif).
- Image `infra-frontend` (image de développement, `node_modules` complet) : CVE d'outillage supplémentaires (`tar`, `undici`, `vite`) absentes d'une image de production Nginx/statique (P11); ces vulnérabilités n'affectent pas le runtime navigateur final.
- Aucun secret détecté sur le système de fichiers du dépôt.
- Deux limites opérationnelles rencontrées et corrigées dans les workflows : (1) Maven Central a renvoyé `429 Too Many Requests` sans cache `.m2` local — `security.yml` réchauffe désormais le cache Maven avant le scan; (2) le téléchargement de la base Java de Trivy (~900 Mo) dépasse le délai par défaut au premier run — `timeout: 10m0s` ajouté et la base mise en cache via `actions/cache`.

CodeQL (`java-kotlin`, `javascript-typescript`) est configuré et validé avec `actionlint`; non exécuté sur GitHub faute d'authentification distante disponible sur ce poste.

Limite documentée de l'édition Community (voir [ADR-0015](docs/adr/0015-sonarqube-quality-gate.md)) : pas de décoration de pull request ni d'analyse multi-branches; seule la branche par défaut est analysée à chaque exécution.

## Qualité et sécurité

- SonarQube et Quality Gate : Quality Gate `OK` validée localement en P4 (voir section précédente); non vérifiée sur un run GitHub distant.
- CodeQL, Dependabot et Trivy : configurés et exécutés/validés localement en P4; contrôle automatisé des secrets couvert par le scanner Trivy intégré (0 résultat).
- CVE ouvertes identifiées par Trivy le 15 août 2026 : 21 backend (17 HIGH/4 CRITICAL) et 8 frontend (HIGH), corrigibles principalement par la montée de version P6; traitées comme dette documentée, pas comme régression P4.
- Audit npm du 2026-08-14 : 6 vulnérabilités élevées dans l'arbre de production et 35 au total, dont 1 critique dans l'outillage. Recoupe partiellement le résultat Trivy du 15/08 (Angular); à re-scanner avant correction en P6.
- Lint Angular : 0 erreur, 41 avertissements `any`; dette visible à réduire progressivement sans désactiver la règle.
- Aucun `npm audit fix --force` n'a été appliqué.
- Auth session HttpOnly/CSRF et bootstrap admin dev : proposés pour P8; JWT/localStorage reste le comportement actuel.

## Docker et déploiement

Le développement utilise `postgres`, `backend` et `frontend` via Compose, avec hot reload Angular. Les tests backend et Playwright disposent de runners Docker dédiés. La stack Playwright est distincte du développement et ne conserve aucune base après exécution.

Nginx, frontend statique de production, utilisateur non-root, profils production-like, GHCR, staging et rollback ne sont pas encore validés.

## Observabilité

Actuator, Micrometer, Prometheus et Grafana sont planifiés en P10. Les request IDs, MDC et logs structurés appartiennent à P9. Loki reste optionnel en P20.

## Kubernetes, Helm et Azure

- Kubernetes : lab planifié P17, pas runtime principal.
- Helm : lab planifié P18 après maîtrise des manifests.
- Azure : architecture/lab planifié P19; aucune ressource payante créée.
- OAuth2/OIDC, Entra ID, Redis, RabbitMQ, Kafka, recherche, Loki et Terraform : labs conditionnels P20.

## Problèmes et dettes ouverts

1. Désarchivage projet et suppression ticket impossibles à couvrir tant que leurs endpoints ne sont pas ajoutés en P7; ne pas simuler ces workflows.
2. CVE réelles ouvertes (21 backend, 8 frontend, détail en section phase 4) à traiter en P6 avec la montée de version; ne pas corriger isolément hors du processus de migration planifié.
3. Auth actuelle fondée sur JWT/localStorage, sans session cookie ni CSRF.
4. Aucune authentification GitHub interactive disponible sur ce poste (`gh auth login`) : bloque la protection de `main` (P3) et la vérification distante de `quality.yml`/`security.yml`/`codeql.yml` (P4). Action humaine requise; tout le reste continue indépendamment.
5. Absence d'image frontend production/Nginx et de staging.
6. Absence d'observabilité et de stratégie backup/restore testée.
7. SonarQube Community Build ne décore pas les pull requests ni n'analyse les branches séparément (limite d'édition documentée, [ADR-0015](docs/adr/0015-sonarqube-quality-gate.md)).

## Décisions

L'[index ADR](docs/adr/README.md) distingue les décisions acceptées des propositions futures. Un ADR `Proposed` ne constitue ni une implémentation ni une validation.

## Livrables documentaires de phase 1

- `AGENTS.md`
- `MIGRATION_MATRIX.md`
- `ENTERPRISE_MIGRATION_REPORT.md`
- `docs/architecture.md` et `docs/architecture/`
- `docs/testing-strategy.md`
- `docs/migration-matrix.md`
- `docs/adr/`
