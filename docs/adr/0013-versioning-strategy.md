# ADR-0013 — Adopter Semantic Versioning et des images immuables

- Statut : Accepted
- Date : 2026-08-15 (révisée 2026-08-22, P15)

## Contexte

Le projet utilisait encore une version Maven snapshot et ne possédait pas de workflow release/GHCR validé.

## Décision

Semantic Versioning pour les tags de release (`vMAJOR.MINOR.PATCH`), créés uniquement sur des commits déjà présents sur `main` (donc déjà passés par `CI Gate` — pas de nouvelle exécution de la suite de tests dans le workflow de release lui-même, qui se concentre sur la publication et la vérification de déploiement). `.github/workflows/release.yml` se déclenche sur `push` d'un tag `v*.*.*` et publie `ghcr.io/joe-azar/taskira-backend` et `taskira-frontend` sous **deux** tags à chaque fois — la version (`vX.Y.Z`) et le SHA du commit — jamais `latest` comme seule référence, pour qu'une image publiée reste toujours traçable jusqu'à son commit source exact indépendamment de ce à quoi le tag de version pointe plus tard.

La première version publiée est `v0.1.0`, pas `v1.0.0` : le SemVer `0.x` signale explicitly « pré-1.0, en évolution » — cohérent avec le dernier critère d'acceptation ci-dessous, puisque les phases 15 à 20 ne sont pas terminées. Le numéro de version n'est délibérément pas indexé sur le numéro de phase de la feuille de route (`v0.1.0` ne signifie pas « phase 1 ») : ce sont deux échelles indépendantes, l'une suit le contenu réellement publié, l'autre le déroulement de la migration.

### Vérification de déploiement avant publication officielle

Après la publication des images, le workflow déploie réellement la version tout juste publiée via `infra/docker-compose.staging.yml` (pull direct depuis GHCR, jamais un build local) et exécute la suite Playwright complète contre cette instance réellement démarrée. La `GitHub Release` (notes générées automatiquement, `gh release create --generate-notes`) n'est créée que si ce déploiement et cette suite réussissent réellement — un tag existe toujours, mais l'entrée de release formelle sur GitHub n'apparaît que pour une version réellement vérifiée déployable. Voir `docs/architecture/deployment.md` pour la topologie du runtime staging et la procédure de rollback.

### Migrations et rollback

Une rupture contractuelle majeure pourra justifier une version majeure après ADR/revue. Les migrations Flyway restent forward-only (voir `AGENTS.md` §13) — le rollback d'une release ne rejoue jamais une migration en sens inverse; il redéploie une image applicative antérieure compatible avec le schéma déjà en place. Une release qui nécessiterait une migration destructive incompatible avec la version précédente doit être traitée comme un cas exceptionnel documenté séparément, pas comme le cas par défaut.

## Critères d'acceptation

- [x] Workflow release sur tag, artefacts et images traçables (version + SHA, jamais `latest` seul).
- [x] Déploiement réel vérifié (staging + Playwright) avant publication de la release formelle.
- [x] Changelog/release notes (notes GitHub auto-générées) et procédure de rollback documentée et réellement testée (`docs/architecture/deployment.md`).
- [x] Aucun tag ou numéro ne déclare une migration enterprise complète avant les critères correspondants (`v0.1.0`, pas `v1.0.0`).

## État d'implémentation

Terminée localement P15 (`feat/phase15-registry-staging`) : `.github/workflows/release.yml`, `infra/docker-compose.staging.yml`, `infra/.env.staging.example`.
