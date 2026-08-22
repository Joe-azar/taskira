# ADR-0023 — Sauvegarde PostgreSQL et restauration réellement testée

- Statut : Accepted
- Date : 2026-08-22

## Contexte

P6 avait déjà produit `scripts/backup/backup-postgres.ps1` et `scripts/restore/restore-postgres.ps1` (`pg_dump`/`pg_restore` réels, restauration systématique dans un conteneur jetable, jamais sur la base active) — utilisés ponctuellement pour la migration PostgreSQL 16 → 18 ([ADR-0017](0017-postgresql-18-migration.md)). Ce qui manquait, nommé explicitement dans la dette de ce dépôt : une politique récurrente, pas seulement un script actionnable à la demande.

Contrainte réelle à ne pas ignorer : ce dépôt n'a, à ce stade, aucune base de données de production persistante et accessible par un runner GitHub-hosted — le staging de P15 est déployé et détruit à la demande (localement ou dans un job CI éphémère), la vraie production reste hors périmètre tant qu'aucune décision d'hébergement n'est prise (P19/Azure). Un job planifié ne peut donc pas « sauvegarder la production » : elle n'existe pas encore en continu. Prétendre le contraire serait exactement le genre de faux succès qu'AGENTS.md §41 interdit.

## Décision

### Deux mécanismes distincts, pas un seul mal défini

1. **Sauvegarde/restauration à la demande** (déjà en place depuis P6, revérifiée et corrigée dans cette phase) : `scripts/backup/backup-postgres.ps1` contre n'importe quelle base réelle (dev local aujourd'hui, un vrai hôte de production plus tard), `scripts/restore/restore-postgres.ps1` pour vérifier une sauvegarde avant de lui faire confiance. C'est ce qu'un développeur ou un futur opérateur de production exécute réellement, à la main ou via son propre ordonnanceur (Tâches planifiées Windows, cron) — ce dépôt documente la procédure plutôt que de configurer silencieusement l'ordonnanceur du poste de quelqu'un, une modification d'état persistant hors du dépôt.
2. **Exercice planifié de sauvegarde/restauration** (nouveau, `.github/workflows/backup-restore-drill.yml`) : hebdomadaire, démarre un vrai PostgreSQL et le vrai backend (toutes les migrations Flyway réellement appliquées), sème de vraies données via l'API HTTP réelle, sauvegarde, restaure dans un conteneur jetable, puis **vérifie** que les données semées sont réellement revenues (pas seulement que `pg_restore` a rendu un code de sortie zéro). Son rôle n'est pas de protéger des données réelles - il n'y en a pas ici - mais de prouver en continu que le mécanisme de sauvegarde/restauration ne se dégrade pas silencieusement à mesure que le schéma évolue (nouvelle migration Flyway, nouvelle version de PostgreSQL, changement de nom de conteneur). Cadence hebdomadaire, pas quotidienne : rien de réel n'est en jeu ici à protéger plus fréquemment.

### Réutilisation directe des scripts existants, pas une réécriture

Le workflow appelle `scripts/backup/backup-postgres.ps1` et `scripts/restore/restore-postgres.ps1` tels quels via `shell: pwsh` (déjà un idiome établi dans ce dépôt : `scripts/sonarqube/bootstrap.ps1` s'exécute de la même façon dans `quality.yml`) — le même code que celui qu'un développeur exécute localement, pas une seconde implémentation en bash à maintenir séparément.

### Bug réel trouvé en exécutant réellement le cycle complet, pas supposé

Un vrai cycle sauvegarde → restauration contre la base de développement réelle (10 migrations, données accumulées réelles) a révélé que `restore-postgres.ps1` échouait avec `FATAL: the database system is shutting down`. Cause réelle : le point d'entrée officiel de l'image PostgreSQL démarre un serveur *temporaire* (socket Unix seulement) pour exécuter les scripts d'initialisation, l'arrête, puis démarre le serveur réel — `pg_isready` seul pouvait réussir contre ce serveur temporaire, juste avant son arrêt. Corrigé en remplaçant la vérification par une boucle de nouvelle tentative autour d'une vraie requête `psql -c "SELECT 1"` contre la base cible, qui traverse naturellement cette fenêtre transitoire au lieu d'entrer en course avec elle. Voir aussi le correctif associé sur la redirection `stderr` d'une commande native sous PowerShell 5.1 (`$ErrorActionPreference = "Stop"`).

## Conséquences

- Une vraie politique de sauvegarde de production (fréquence, rétention longue durée, stockage hors site) reste à définir quand une vraie production existera (P19+) - ce serait prématuré et non vérifiable aujourd'hui.
- Le fichier `.dump` produit localement par `backup-postgres.ps1` reste dans `backups/` (gitignoré) - aucune sauvegarde n'est jamais commitée, aucun secret n'y figure au-delà de ce que la base elle-même contient déjà (mots de passe déjà hachés).
- L'exercice planifié consomme des minutes GitHub Actions chaque semaine pour une vérification qui ne protège rien de réel aujourd'hui - un compromis assumé en échange de ne jamais laisser le mécanisme de restauration se dégrader silencieusement en attendant qu'une vraie production en ait besoin.
