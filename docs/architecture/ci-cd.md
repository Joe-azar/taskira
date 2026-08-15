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

## Étapes ultérieures

- P4 : SonarQube/Quality Gate, CodeQL, Dependabot, Trivy et secrets.
- P15 : images GHCR versionnées, release par tag, staging, E2E post-déploiement et production manuelle.

Éviter `latest` comme seule référence et éviter de reconstruire plusieurs fois le même artefact. Le déploiement cible consomme des images immuables, pas `git pull` sur le serveur.

Voir [ADR-0007](../adr/0007-github-actions.md). Le run distant prouve le pipeline; il ne remplace pas la protection de branche.
