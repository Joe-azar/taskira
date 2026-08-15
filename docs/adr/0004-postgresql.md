# ADR-0004 — Utiliser PostgreSQL comme base principale

- Statut : Accepted
- Date : 2026-08-15

## Contexte

Taskira utilise déjà PostgreSQL et ne porte aucun héritage MySQL à migrer.

## Décision

PostgreSQL reste l'unique base applicative principale. Les tests d'intégration utilisent le même moteur via Testcontainers; H2 n'est pas un substitut accepté.

L'upgrade vers PostgreSQL 17 ou 18 sera évalué en P6 selon compatibilité driver, Flyway et Hibernate. Voir [ADR-0017](0017-postgresql-18-migration.md) pour la décision et la procédure de migration réelles.

## Conséquences

- Requêtes, contraintes, index et migrations sont testés sur le moteur réel.
- Les fonctionnalités spécifiques sont possibles si elles restent explicites et testées.

## État d'implémentation

PostgreSQL 18.6 est actif depuis P6 (voir [ADR-0017](0017-postgresql-18-migration.md)).
