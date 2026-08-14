# ADR-0013 — Adopter Semantic Versioning et des images immuables

- Statut : Proposed
- Date : 2026-08-15

## Contexte

Le projet utilise encore une version Maven snapshot et ne possède pas de workflow release/GHCR validé.

## Proposition

En P15, adopter Semantic Versioning pour les releases, créer les tags après tests et publier backend/frontend dans GHCR avec version et SHA. `latest` ne sera jamais l'unique référence.

Une rupture contractuelle majeure pourra justifier une version majeure après ADR/revue. Les migrations DB doivent préserver une fenêtre de rollback documentée.

## Critères d'acceptation

- Workflow release sur tag, artefacts et images traçables.
- Changelog/release notes et procédure rollback.
- Aucun tag ou numéro ne déclare une migration enterprise complète avant les critères correspondants.

## État d'implémentation

Non implémenté; décision à confirmer en P15.
