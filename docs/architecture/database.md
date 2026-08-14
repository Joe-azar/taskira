# Architecture de la base de données

Statut : PostgreSQL/Flyway opérationnels; aucune migration de schéma dans la phase documentaire.

## État actuel

- PostgreSQL 16 en développement.
- PostgreSQL 16.15 dans les tests Testcontainers.
- Flyway `V1` à `V6` crée `users`, `projects`, `project_members`, `tickets`, `comments` et `ticket_history`.
- Hibernate utilise `spring.jpa.hibernate.ddl-auto=validate` et `open-in-view=false`.

## Règles

- PostgreSQL reste le moteur; aucune migration MySQL.
- Flyway est l'unique propriétaire du schéma.
- `V1` à `V6` sont immuables; toute évolution commence à `V7`.
- Aucune migration destructive sans revue explicite, compatibilité applicative, sauvegarde et stratégie de rollback.
- Les tests repository/intégration utilisent PostgreSQL Testcontainers, jamais H2.
- Les contraintes, index et requêtes critiques doivent être testés sur le moteur réel.

## Validation actuelle

Les tests d'intégration démarrent une base vide, appliquent Flyway `V1`–`V6`, valident le mapping Hibernate et exercent des requêtes repository réelles.

## Évolutions planifiées

- P6 : évaluer PostgreSQL 17/18 après compatibilité drivers/Flyway.
- P7 : optimistic locking et réponse `409` pour les collisions pertinentes.
- P9 : migration additive `audit_events`.
- P13/P14 : métadonnées de pièces jointes et états d'exports.
- P16 : scripts de sauvegarde et restauration réellement testée.
