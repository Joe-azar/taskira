# Architecture CI/CD

Statut : exécution GitHub distante validée; phase partielle faute de protection de `main`.

## Implémentation locale P3

```text
Pull Request
├── backend compile + tests + Testcontainers/Flyway
├── frontend npm ci + lint + Vitest + couverture + build
├── builds Docker
└── stack E2E isolée + Playwright + artefacts
```

Le fichier `.github/workflows/ci.yml` définit les jobs backend, frontend et containers+E2E sur runners GitHub-hosted, puis un gate agrégé. Le job navigateur utilise `e2e/playwright/compose.e2e.yml`, conserve les rapports/logs utiles et exécute `down --volumes --remove-orphans` avec `always()`. Les actions sont pinées par SHA, les permissions sont minimales et `actionlint` 1.7.12 passe.

La [PR draft #1](https://github.com/Joe-azar/taskira/pull/1) au HEAD `6db6115` possède un [run GitHub #3 vert](https://github.com/Joe-azar/taskira/actions/runs/31851279947) : Backend, Frontend avec lint, Containers and E2E et CI Gate réussissent. Le lint produit 0 erreur et 41 avertissements `any` non bloquants.

`main` est encore non protégée (`protected=false`) et aucun ruleset n'est configuré. Le connecteur reçoit `403` sur ces réglages administratifs; l'activation doit être réalisée dans l'interface GitHub ou avec un jeton administrateur interactif. La fonctionnalité ne nécessite pas d'offre payante. Aucun runner persistant sur le poste personnel ne doit exécuter du code de PR publique.

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

SonarQube Community Build tourne sans service distant ni secret GitHub : la stack est démarrée, amorcée et détruite dans le même job (voir [ADR-0015](../adr/0015-sonarqube-quality-gate.md)). Validée localement le 15 août 2026 : Quality Gate `OK`, 0 bug, 0 vulnérabilité, 0 security hotspot, couverture 13,0 %, 24 code smells (dette 140 min). Deux bugs d'accessibilité (`Web:InputWithoutLabelCheck`) détectés puis corrigés avant la seconde analyse.

Trivy (dépendances + images, exécuté localement le 15 août 2026) confirme des CVE réelles corrigibles uniquement par la montée de version P6 : 21 (17 HIGH, 4 CRITICAL) sur `backend/pom.xml` (Jackson, Tomcat embarqué, pilote PostgreSQL, Spring Boot/Data/Security/Framework) et 8 HIGH sur `frontend/package-lock.json` (`@angular/common|compiler|core`). Aucun secret détecté dans le dépôt. L'image `infra-backend` reprend les CVE applicatives plus des CVE Go du binaire `pebble` embarqué dans l'image de base; l'image `infra-frontend` (dev, avec `node_modules` complet) ajoute des CVE d'outillage (`tar`, `undici`, `vite`) qui n'atteignent pas une image de production Nginx (P11). Ces vulnérabilités sont un gap connu à traiter en P6, pas un échec de P4 : l'objectif de P4 est la détection automatisée, pas la correction immédiate d'une dette déjà documentée.

L'édition Community de SonarQube ne décore pas les pull requests ni n'analyse les branches séparément; seule l'analyse de la branche par défaut est disponible. Cette limite est documentée plutôt que contournée par une offre payante.

## Étapes ultérieures

- P15 : images GHCR versionnées, release par tag, staging, E2E post-déploiement et production manuelle.

Éviter `latest` comme seule référence et éviter de reconstruire plusieurs fois le même artefact. Le déploiement cible consomme des images immuables, pas `git pull` sur le serveur.

Voir [ADR-0007](../adr/0007-github-actions.md). Le run distant prouve le pipeline; il ne remplace pas la protection de branche.
