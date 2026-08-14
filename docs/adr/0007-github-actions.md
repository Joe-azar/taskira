# ADR-0007 — Utiliser GitHub Actions pour la CI/CD

- Statut : Proposed
- Date : 2026-08-15

## Contexte

Le dépôt officiel est hébergé sur GitHub. Un workflow existe localement, sans exécution distante validée.

## Proposition

Utiliser GitHub Actions en P3 pour compiler, tester, lancer Testcontainers, construire Angular/Docker et exécuter Playwright sur une stack isolée. P4 ajoutera qualité/scans et P15 GHCR, release et staging.

Les runners GitHub-hosted sont prioritaires. Aucun runner persistant sur un poste personnel ne doit traiter du code de PR publique.

## Critères d'acceptation

- Checks requis sur pull request et protection de `main` si permissions disponibles.
- Réutilisation des artefacts, cache sûr et absence de secret dans les logs.
- Diagnostic E2E conservé comme artefact en cas d'échec.

## État d'implémentation

`ci.yml` est créé et passe `actionlint` 1.7.12. Les jobs backend, frontend et Compose+E2E sont définis avec permissions minimales et actions pinées. Le run GitHub distant, le lint et la protection de branche restent à valider; l'ADR demeure `Proposed` jusque-là.
