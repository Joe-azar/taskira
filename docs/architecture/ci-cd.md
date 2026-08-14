# Architecture CI/CD

Statut : implémentation locale partielle; aucune exécution GitHub distante validée.

## Implémentation locale P3

```text
Pull Request
├── backend compile + tests + Testcontainers/Flyway
├── frontend npm ci + Vitest + couverture + build
├── builds Docker
└── stack E2E isolée + Playwright + artefacts
```

Le fichier `.github/workflows/ci.yml` définit localement les jobs backend, frontend et containers+E2E sur runners GitHub-hosted, puis un gate agrégé. Le job navigateur utilise `e2e/playwright/compose.e2e.yml`, conserve les rapports/logs utiles et exécute `down --volumes --remove-orphans` avec `always()`. Les actions sont pinées par SHA, les permissions sont minimales et `actionlint` 1.7.12 passe. Le lint frontend, le run GitHub distant et la protection de branche restent à valider.

`main` devra exiger une pull request et les checks critiques. Aucun runner persistant sur le poste personnel ne doit exécuter du code de PR publique.

## Étapes ultérieures

- P4 : SonarQube/Quality Gate, CodeQL, Dependabot, Trivy et secrets.
- P15 : images GHCR versionnées, release par tag, staging, E2E post-déploiement et production manuelle.

Éviter `latest` comme seule référence et éviter de reconstruire plusieurs fois le même artefact. Le déploiement cible consomme des images immuables, pas `git pull` sur le serveur.

Voir [ADR-0007](../adr/0007-github-actions.md). La validation statique locale ne vaut pas preuve d'exécution distante.
