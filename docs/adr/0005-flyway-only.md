# ADR-0005 — Confier le schéma exclusivement à Flyway

- Statut : Accepted
- Date : 2026-08-15

## Contexte

Un schéma reproductible et auditable ne peut pas dépendre de modifications implicites d'Hibernate.

## Décision

Toutes les évolutions passent par Flyway. `ddl-auto=validate` reste obligatoire. Les migrations `V1` à `V6` sont immuables; les évolutions commencent à `V7`.

Une migration destructive nécessite revue explicite, compatibilité, sauvegarde et rollback documentés.

## Conséquences

- Historique de schéma versionné.
- Démarrage depuis une base vide vérifiable par Testcontainers.
- Aucun `create`, `update` ou contournement Flyway pour rendre un test vert.

## État d'implémentation

Décision déjà appliquée et couverte par des tests d'intégration.
