# ADR-0007 — Utiliser GitHub Actions pour la CI/CD

- Statut : Accepted
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

`ci.yml` passe `actionlint` 1.7.12. Le run GitHub #3 valide Backend, Frontend avec lint, Containers and E2E et CI Gate; les permissions restent minimales et les actions pinées.

`main` est protégée depuis le 15 août 2026 (`protected=true`) : `CI Gate` est un check obligatoire, `strict=true` (branche à jour requise), PR obligatoire avec `required_approving_review_count=0` (revue humaine non requise, cohérent avec un développeur unique), force-push et suppression interdits, `enforce_admins=false` pour ne pas bloquer le propriétaire du dépôt. `security.yml`, `quality.yml` et `codeql.yml` (P4) ne sont volontairement pas encore des checks obligatoires : une seule exécution distante ne constitue pas un historique suffisant pour les qualifier de stables.
