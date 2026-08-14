# Rapport cumulatif de migration enterprise

Dernière mise à jour : 2026-08-15.

Ce fichier est le journal central de la migration. Il doit être enrichi après chaque grande phase avec les commandes, résultats, écarts et décisions réellement observés.

## Verdict actuel

`TASKIRA ENTERPRISE MIGRATION PARTIALLY COMPLETE`

Les phases 0 et 1 sont terminées localement. Les phases 2 et 3 possèdent des livrables validés localement mais restent partielles. Les phases 4 à 20 ne sont pas déclarées terminées.

## Journal des phases

| Date | Phase | Statut | Résultat vérifié | Reste à faire |
| --- | ---: | --- | --- | --- |
| 2026-08-14 | 0 — Baseline Git | Terminée | Commit `fd84c54`, tag `pre-enterprise-migration`, branche `feat/enterprise-platform-migration`; stack Docker restaurée et validée avant migration. | Aucun pour le critère de baseline; conserver le point de retour. |
| 2026-08-15 | 1 — Documentation | Terminée localement | `AGENTS.md`, matrices, rapport, documentation d'architecture et ADR créés; liens et cohérence vérifiés. | Revue/commit/push par le workflow Git autorisé. |
| 2026-08-14–15 | 2 — Filet de sécurité | Partielle | 14 tests backend, 20 Vitest, couvertures/seuils backend et frontend, 3 Playwright obligatoires, 1 connexion réelle optionnelle et build Angular validés dans Docker. | Étendre les tests métier et couvrir les workflows E2E minimum du référentiel. |
| 2026-08-15 | 3 — GitHub Actions CI | Partielle | `ci.yml` local : jobs backend, frontend coverage/build et stack Compose+E2E; actions pinées par SHA, permissions `contents: read`, cleanup volumes; `actionlint` 1.7.12 réussi. | Obtenir un run GitHub distant vert, ajouter le lint frontend et configurer les checks/protection de `main` si permis. |
| — | 4–20 | Planifiées | Aucun critère de sortie déclaré atteint. | Suivre [la feuille de route](docs/migration-matrix.md) dans l'ordre. |

## Incident de stockage et récupération

Le 15 août 2026, le worktree initial a rencontré l'erreur Windows `ERROR 1117` signalant une erreur d'entrée/sortie. Il a été immédiatement mis en quarantaine et aucune nouvelle écriture n'y a été effectuée.

La baseline a été récupérée depuis `origin` dans un clone sain situé sur un autre volume local. La branche de migration et le tag ont ensuite été recréés localement à partir de cette baseline, puis les livrables non commités ont été reconstruits et revérifiés dans ce clone.

Le diagnostic et la réparation éventuelle du matériel, du filesystem ou de Windows restent hors du dépôt. Cet incident ne modifie pas le statut fonctionnel des phases; il explique uniquement le changement de worktree de travail.

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
| Maven | Wrapper 3.9.x | 3.9.x |
| PostgreSQL | 16; Testcontainers 16.15 | 17/18 en P6 après compatibilité |
| Angular | 21.2.x | 22.x en P6 |
| TypeScript | 5.9.x | Version compatible Angular 22 en P6 |
| Node/npm | Node 22; npm 11.9.0 | Node 24 LTS en P6 |
| Vitest | 4.x | Branche compatible Angular cible |
| Playwright | 1.62.1 dans le socle P2 | Maintenu et exécuté en CI P3 |

## Outils ajoutés ou renforcés

- Testcontainers PostgreSQL et base de test partagée pour les intégrations.
- Séparation Maven Surefire (`*Test`) et Failsafe (`*IT`).
- Tests JUnit 5, Mockito, AssertJ, MockMvc et Flyway réels.
- Tests Angular/Vitest ciblés sur auth, guards et intercepteur.
- Image Playwright Docker avec trois scénarios obligatoires et un scénario réel optionnel.
- JaCoCo 0.8.13 fusionne les résultats Surefire/Failsafe, produit XML/HTML et applique un seuil ligne global; `coverage-v8` 4.1.0 produit LCOV/HTML et applique quatre seuils frontend.
- Workflow GitHub Actions `ci.yml` créé et validé statiquement avec `actionlint` 1.7.12.

Aucun run GitHub distant n'est encore disponible. SonarQube, scans, Nginx production, observabilité et labs ne sont pas encore ajoutés.

## Tests

Résultat validé de phase 2 :

- backend rapide : 11 tests;
- backend intégration/Testcontainers : 3 tests;
- backend total : 14 tests;
- frontend Vitest : 20 tests;
- navigateur : 3 tests obligatoires, plus 1 connexion réelle optionnelle validée avec variables d'environnement;
- build Angular de production : réussi.

Couverture validée le 15 août 2026 :

- backend JaCoCo : lignes 20,17 %, branches 3,75 %, instructions 18,11 %, méthodes 23,01 %, classes 42,25 %; seuil ligne 19 % atteint, `./mvnw verify` exit 0;
- frontend V8 : statements 12,44 %, branches 11,27 %, fonctions 11,78 %, lignes 11,84 %; seuils respectifs 12/11/11/11 % atteints, 20/20 tests et commande exit 0.

La commande E2E manuelle cible la stack Compose locale. Le workflow CI local définit désormais une stack isolée et éphémère, sans run GitHub distant validé. Les workflows complets projets/tickets/membres/commentaires restent absents. Voir [la stratégie de tests](docs/testing-strategy.md).

## CI/CD

Un workflow local `.github/workflows/ci.yml` définit :

- backend Java 21 avec `./mvnw verify` et rapports tests/JaCoCo;
- frontend Node 22.23.2/npm 11.9.0 avec couverture et build;
- stack Compose éphémère, attente de disponibilité, Playwright, logs/artifacts et `down --volumes` systématique.

Les actions tierces sont pinées par SHA, les permissions globales sont `contents: read` et `persist-credentials` est désactivé. `actionlint` 1.7.12 passe localement. Aucun run GitHub distant, lint frontend, check requis ou protection de `main` n'est encore validé; P3 reste partielle. GHCR, release, staging et production manuelle relèvent de P15.

## Qualité et sécurité

- SonarQube et Quality Gate : non installés, P4.
- CodeQL, Dependabot, Trivy et contrôle automatisé des secrets : non configurés, P4.
- Audit npm du 2026-08-14 : 6 vulnérabilités élevées dans l'arbre de production et 35 au total, dont 1 critique dans l'outillage. Ces valeurs sont historiques et doivent être rescannées avant correction.
- Aucun `npm audit fix --force` n'a été appliqué.
- Auth session HttpOnly/CSRF et bootstrap admin dev : proposés pour P8; JWT/localStorage reste le comportement actuel.

## Docker et déploiement

Le développement utilise `postgres`, `backend` et `frontend` via Compose, avec hot reload Angular. Les tests backend et Playwright disposent de runners Docker dédiés.

Nginx, frontend statique de production, utilisateur non-root, profils production-like, GHCR, staging et rollback ne sont pas encore validés.

## Observabilité

Actuator, Micrometer, Prometheus et Grafana sont planifiés en P10. Les request IDs, MDC et logs structurés appartiennent à P9. Loki reste optionnel en P20.

## Kubernetes, Helm et Azure

- Kubernetes : lab planifié P17, pas runtime principal.
- Helm : lab planifié P18 après maîtrise des manifests.
- Azure : architecture/lab planifié P19; aucune ressource payante créée.
- OAuth2/OIDC, Entra ID, Redis, RabbitMQ, Kafka, recherche, Loki et Terraform : labs conditionnels P20.

## Problèmes et dettes ouverts

1. Phase 2 incomplète : large couverture E2E métier absente malgré tests et seuils de couverture verts.
2. Vulnérabilités npm connues à réévaluer et traiter pendant P4/P6.
3. Auth actuelle fondée sur JWT/localStorage, sans session cookie ni CSRF.
4. CI seulement locale : run GitHub distant, lint et protection de branche manquants; Quality Gate et scans absents.
5. Absence d'image frontend production/Nginx et de staging.
6. Absence d'observabilité et de stratégie backup/restore testée.

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
