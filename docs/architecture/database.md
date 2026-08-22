# Architecture de la base de données

Statut : PostgreSQL 18 en développement et en tests depuis la phase 6 ([ADR-0017](../adr/0017-postgresql-18-migration.md)).

## État actuel

- PostgreSQL 18.6 en développement (`infra/docker-compose.yml`) et dans les tests Testcontainers.
- Flyway `V1` à `V10` crée `users`, `projects`, `project_members`, `tickets`, `comments`, `ticket_history`, la colonne `version` du verrouillage optimiste (P7), `audit_events` (P9), `attachments` (P13) et le schéma `BATCH_*` de Spring Batch pour les exports en masse (P14).
- Hibernate utilise `spring.jpa.hibernate.ddl-auto=validate` et `open-in-view=false`.
- Depuis PostgreSQL 18, l'image Docker officielle attend un point de montage unique sur `/var/lib/postgresql` (et non plus `/var/lib/postgresql/data`); les données de version majeure vivent dans un sous-répertoire pour permettre `pg_upgrade --link`. Tout service ou script qui monte un volume PostgreSQL doit utiliser ce nouveau chemin.

## Règles

- PostgreSQL reste le moteur; aucune migration MySQL.
- Flyway est l'unique propriétaire du schéma.
- `V1` à `V10` sont immuables; toute évolution commence à `V11` — vérifier la dernière version réellement présente dans `backend/src/main/resources/db/migration/` avant d'en créer une nouvelle (AGENTS.md §13).
- Aucune migration destructive sans revue explicite, compatibilité applicative, sauvegarde et stratégie de rollback.
- Les tests repository/intégration utilisent PostgreSQL Testcontainers, jamais H2.
- Les contraintes, index et requêtes critiques doivent être testés sur le moteur réel.

## Validation actuelle

Les tests d'intégration démarrent une base vide, appliquent toutes les migrations Flyway existantes, valident le mapping Hibernate et exercent des requêtes repository réelles.

## Migration PostgreSQL 18 (phase 6)

Procédure suivie, dans l'ordre, avant de considérer la migration réussie :

1. `pg_dump -F custom` de la base de développement réelle (`scripts/backup/backup-postgres.ps1`), volume PostgreSQL 16 d'origine jamais touché.
2. Nouveau volume Docker nommé (`taskira_postgres_data_pg18`) créé séparément.
3. Flyway `V1`–`V6` rejoué sur une base PostgreSQL 18 totalement vide (conteneur jetable) : les 6 migrations passent sans modification.
4. Restauration du dump réel dans une base de test séparée : lignes et contenu identiques à l'original (`scripts/restore/restore-postgres.ps1`).
5. Backend réel (Spring Boot 4/Hibernate 7) démarré contre les deux bases (fraîchement migrée et restaurée) : `ddl-auto=validate` accepté dans les deux cas, dialecte `PostgreSQLDialect` détecté sur PostgreSQL 18.6.
6. Restauration finale des données réelles dans le volume `taskira_postgres_data_pg18`, puis bascule de `infra/docker-compose.yml` vers ce volume; connexion, Flyway, Hibernate et un flux de login HTTP réel revérifiés après la bascule.

Le volume PostgreSQL 16 d'origine (`infra_taskira_postgres_data`, nom généré par le préfixe de projet Compose) n'a jamais été supprimé; il reste le chemin de retour arrière tant que cette migration n'est pas éprouvée en usage réel.

## `audit_events` (phase 9)

`V8__create_audit_events.sql`, migration additive : `id`, `occurred_at` (posé en Java via `@PrePersist`, pas de `DEFAULT` SQL — contrairement à `ticket_history` qui avait les deux), `actor_id` (FK `users(id)` **`ON DELETE SET NULL`**, pas `RESTRICT` comme `ticket_history.changed_by` — un journal d'audit doit survivre à la suppression de son acteur), `actor_email` (copie dénormalisée, survit même à la suppression), `entity_type`/`entity_id` (polymorphe, pas de FK), `action`, `detail`, `request_id`, `ip_address`. Table de log, immuable après insertion : `AuditEvent` n'étend pas `AuditableEntity` (pas de `version`/`updated_at`).

Complète `ticket_history` sans le remplacer : `ticket_history` reste le journal de changements orienté produit, propre à un ticket (cascade à sa suppression, autorisation par appartenance au projet); `audit_events` est le journal sécurité/conformité générique (corrélé par `request_id`, visible seulement des admins). Voir [ADR-0018](../adr/0018-audit-request-correlation.md).

## `attachments` (P13) et Spring Batch (P14)

`V9__create_attachments.sql` : table `attachments` (FK `ticket_id` en cascade, FK `uploader_id`, `storage_key` unique). `V10__create_spring_batch_tables.sql` : schéma `BATCH_*` officiel de Spring Batch, créé par Flyway et jamais par l'initialiseur intégré (`spring.batch.jdbc.initialize-schema: never`, même règle que `ddl-auto: validate` pour Hibernate) — voir [ADR-0021](../adr/0021-attachments-storage.md) et [ADR-0022](../adr/0022-exports-batch.md).

## Sauvegarde et restauration (P16)

`scripts/backup/backup-postgres.ps1`/`scripts/restore/restore-postgres.ps1` (à la demande) et `.github/workflows/backup-restore-drill.yml` (exercice hebdomadaire automatisé, sème/sauvegarde/restaure/vérifie réellement) — voir [backup.md](backup.md) et [ADR-0023](../adr/0023-backup-restore-strategy.md).
