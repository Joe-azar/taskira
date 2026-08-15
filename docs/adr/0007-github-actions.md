# ADR-0007 — Utiliser GitHub Actions pour la CI/CD

- Statut : Proposed
- Date : 2026-08-15

## Contexte

Le dépôt officiel est hébergé sur GitHub. Le workflow est exécuté sur la PR draft #1 et son run #3 est vert au commit `6db6115`.

## Proposition

Utiliser GitHub Actions en P3 pour compiler, tester, lancer Testcontainers, construire Angular/Docker et exécuter Playwright sur une stack isolée. P4 ajoutera qualité/scans et P15 GHCR, release et staging.

Les runners GitHub-hosted sont prioritaires. Aucun runner persistant sur un poste personnel ne doit traiter du code de PR publique.

## Critères d'acceptation

- Checks requis sur pull request et protection de `main` si permissions disponibles.
- Réutilisation des artefacts, cache sûr et absence de secret dans les logs.
- Diagnostic E2E conservé comme artefact en cas d'échec.

## État d'implémentation

`ci.yml` passe `actionlint` 1.7.12. Le run GitHub #3 valide Backend, Frontend avec lint, Containers and E2E et CI Gate; les permissions restent minimales et les actions pinées. `main` est toutefois encore non protégée (`protected=false`, aucun ruleset). Le connecteur ne peut pas modifier ces réglages administratifs et reçoit `403`; l'activation doit être faite dans l'interface GitHub ou avec un jeton administrateur interactif. L'ADR demeure `Proposed` jusqu'à cette activation.
