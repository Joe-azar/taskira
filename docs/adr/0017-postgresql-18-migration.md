# ADR-0017 — Migration PostgreSQL 16 vers 18

- Statut : Accepted
- Date : 2026-08-15

## Contexte

ADR-0004 acte PostgreSQL comme moteur unique. La phase 6 planifie une montée vers PostgreSQL 17 ou 18. `postgres:18-alpine` résout vers PostgreSQL 18.6, la ligne stable la plus récente au moment de l'exécution; retenue plutôt que 17 pour rester cohérente avec le reste de la phase 6 (Spring Boot 4, Angular 22, Node 24 : toutes les dernières versions stables majeures, pas des versions intermédiaires).

## Découverte pendant la migration

La première tentative de démarrer un conteneur `postgres:18.6-alpine` avec un volume monté sur `/var/lib/postgresql/data` (la convention utilisée par toutes les versions antérieures et par le reste du dépôt) échoue au démarrage. Depuis PostgreSQL 18, l'image Docker officielle attend un point de montage unique sur `/var/lib/postgresql`; les données de version majeure sont placées dans un sous-répertoire pour permettre `pg_upgrade --link` sans franchir de frontière de point de montage (voir docker-library/postgres#1259 et #37). Tout service ou script montant un volume PostgreSQL a dû être mis à jour vers ce nouveau chemin : `infra/docker-compose.yml`, `e2e/playwright/compose.e2e.yml` et les scripts de sauvegarde/restauration. Testcontainers n'est pas concerné : `PostgreSQLContainer` ne monte pas de volume externe.

## Procédure de migration suivie

Conformément à l'exigence de ne jamais réutiliser le volume PostgreSQL 16 existant pour une image PostgreSQL 18 :

1. `pg_dump -F custom` de la base de développement réelle, volume PostgreSQL 16 d'origine jamais modifié.
2. Nouveau volume Docker créé séparément (`taskira_postgres_data_pg18`), nommé explicitement (`name:` dans `infra/docker-compose.yml`) pour éviter le préfixage automatique du nom de projet Compose — un préfixage qui, lors d'un premier essai, a fait pointer Compose vers un volume différent et vide malgré une restauration déjà effectuée dans le volume nommé manuellement. Corrigé en épinglant le nom exact avant la bascule finale.
3. Flyway `V1`–`V6` rejoué sur une base PostgreSQL 18 totalement vide : succès sans modification des scripts de migration.
4. Restauration du dump réel dans une base de test séparée, avec vérification ligne à ligne (nombre de lignes et contenu réel des utilisateurs) contre l'original.
5. Backend Spring Boot 4/Hibernate 7 démarré contre les deux bases (fraîchement migrée et restaurée) : `ddl-auto=validate` accepté dans les deux cas.
6. Restauration finale dans le volume de développement réel, bascule de `infra/docker-compose.yml`, revérification complète (Flyway « up to date » sans nouvelle migration, Hibernate validate, flux de login HTTP réel).

Le volume PostgreSQL 16 d'origine n'a jamais été supprimé et reste le chemin de retour arrière.

## Décision

Adopter PostgreSQL 18.6 comme moteur de développement et de test, avec le nouveau chemin de montage `/var/lib/postgresql`. `flyway-database-postgresql` et le pilote JDBC `org.postgresql:postgresql` gérés par le BOM Spring Boot 4 sont compatibles sans changement de code applicatif.

## Conséquences

- `scripts/backup/backup-postgres.ps1` et `scripts/restore/restore-postgres.ps1` formalisent la procédure ci-dessus pour toute migration future ou sauvegarde ponctuelle; `scripts/restore` ne restaure jamais directement sur la base active, toujours sur un conteneur jetable séparé.
- Le volume PostgreSQL 16 (`infra_taskira_postgres_data`) reste sur disque, non référencé, jusqu'à ce que la phase 6 soit validée en usage réel; sa suppression est une décision manuelle ultérieure, pas automatisée par cette migration.
- Toute nouvelle infrastructure montant un volume PostgreSQL (staging P15, Kubernetes P17, etc.) doit utiliser la convention PostgreSQL 18 dès sa création.
