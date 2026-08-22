# Architecture CI/CD

Statut : `main` protégée; `ci.yml`/`quality.yml`/`security.yml`/`codeql.yml` (P3, P4) et `release.yml`/`backup-restore-drill.yml` (P15, P16) tous vérifiés verts sur des runs GitHub distants réels.

## Implémentation locale P3

```text
Pull Request
├── backend compile + tests + Testcontainers/Flyway
├── frontend npm ci + lint + Vitest + couverture + build
├── builds Docker
└── stack E2E isolée + Playwright + artefacts
```

Le fichier `.github/workflows/ci.yml` définit les jobs backend, frontend et containers+E2E sur runners GitHub-hosted, puis un gate agrégé. Le job navigateur utilise `e2e/playwright/compose.e2e.yml`, conserve les rapports/logs utiles et exécute `down --volumes --remove-orphans` avec `always()`. Les actions sont pinées par SHA, les permissions sont minimales et `actionlint` 1.7.12 passe.

`ci.yml` a d'abord été validé vert sur la [PR draft #1](https://github.com/Joe-azar/taskira/pull/1) (P3), puis rejoué avec succès sur chaque Pull Request de phase depuis — consulter l'onglet Actions de GitHub pour le run le plus récent plutôt qu'un ancien numéro figé. Le lint frontend reste sans erreur bloquante; une dette de typage `any` visible diminue progressivement (voir `ENTERPRISE_MIGRATION_REPORT.md` pour le chiffre courant).

`main` est protégée depuis le 15 août 2026 (`protected: true`, vérifié via l'API GitHub) : PR obligatoire (`required_approving_review_count: 0`, cohérent avec un développeur unique), `CI Gate` obligatoire (`strict: true`, branche à jour requise), force-push et suppression interdits, `enforce_admins: false` pour ne pas bloquer le propriétaire du dépôt. Aucun runner persistant sur le poste personnel ne doit exécuter du code de PR publique.

## Implémentation locale P4

```text
.github/workflows/quality.yml
├── build backend (JaCoCo) + build frontend (coverage)
├── SonarQube Community Build éphémère (Docker, base Postgres dédiée)
├── bootstrap admin/projet/token (scripts/sonarqube/bootstrap.ps1)
├── analyse (sonar-scanner-cli)
├── vérification /api/qualitygates/project_status -> échec du job si != OK
└── destruction de la stack (down --volumes)

.github/workflows/security.yml
├── Trivy fs (vuln + secret) sur le dépôt -> SARIF -> code scanning
└── Trivy image (backend, frontend) -> SARIF -> code scanning

.github/workflows/codeql.yml
└── CodeQL java-kotlin + javascript-typescript, déclenché sur push/PR et hebdomadaire

.github/dependabot.yml
└── maven (backend), npm (frontend), docker (backend/frontend/e2e), github-actions
```

SonarQube Community Build tourne sans service distant ni secret GitHub : la stack est démarrée, amorcée et détruite dans le même job (voir [ADR-0015](../adr/0015-sonarqube-quality-gate.md)). Quality Gate `OK` validée localement le 15 août 2026 puis reconfirmée sur un run GitHub distant réel (`quality.yml`, PR #28) avant la fusion de la phase 6.

Trivy (dépendances + images) a d'abord confirmé, avant la phase 6, des CVE réelles corrigibles uniquement par la montée de version : 21 sur `backend/pom.xml` et 8 sur `frontend/package-lock.json`. Après la migration de la phase 6 (Spring Boot 4.1.0, Spring Security 7.1.0, pilote PostgreSQL 42.7.13, Angular 22.1.2), un rescanning post-migration a été exécuté et confirmé sur `security.yml` : la dette de dépendances applicatives backend et frontend est revenue à 0 CVE. Le détail avant/après, les CVE résiduelles hors périmètre (outillage de l'image de base, non corrigibles par le projet) et l'analyse complète figurent dans la section « Rescanning sécurité post-phase 6 » du [rapport cumulatif](../../ENTERPRISE_MIGRATION_REPORT.md). Aucun secret détecté dans le dépôt.

L'édition Community de SonarQube ne décore pas les pull requests ni n'analyse les branches séparément; seule l'analyse de la branche par défaut est disponible. Cette limite est documentée plutôt que contournée par une offre payante.

## Release, staging et sauvegarde (P15, P16)

```text
.github/workflows/release.yml
├── déclenché sur push d'un tag v*.*.* (pas sur chaque push)
├── build + push GHCR (backend, frontend), double tag version + SHA, jamais `latest` seul
├── déploiement réel sur infra/docker-compose.staging.yml (image déjà publiée, jamais de build local)
├── suite Playwright complète contre ce déploiement réel
└── GitHub Release publiée (notes auto-générées) seulement si tout précède réussit

.github/workflows/backup-restore-drill.yml
├── hebdomadaire (cron) + workflow_dispatch
├── démarre un vrai PostgreSQL + backend, applique toutes les migrations Flyway
├── sème de vraies données via l'API réelle, sauvegarde, restaure dans un conteneur jetable
└── vérifie que les données reviennent réellement (pas seulement pg_restore exit 0)
```

Voir [deployment.md](deployment.md) et [backup.md](backup.md) pour le détail complet. Images GHCR jamais référencées par `latest` seul (ADR-0013); le déploiement consomme des images immuables, pas `git pull` sur le serveur.

Voir [ADR-0007](../adr/0007-github-actions.md). Le pipeline est prouvé par des runs GitHub distants réels et `main` est protégée; les deux garanties sont en place, pas seulement l'une des deux.
