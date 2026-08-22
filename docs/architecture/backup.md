# Sauvegarde et restauration PostgreSQL

Voir [ADR-0023](../adr/0023-backup-restore-strategy.md) pour les décisions complètes. Ce document est la référence opérationnelle : comment sauvegarder, comment restaurer et vérifier, comment planifier localement.

## Deux mécanismes, deux besoins différents

| | À la demande | Exercice planifié |
| --- | --- | --- |
| Où | `scripts/backup/`, `scripts/restore/` | `.github/workflows/backup-restore-drill.yml` |
| Contre quelle base | N'importe quelle base réelle atteignable (dev local aujourd'hui, un futur hôte de production) | Un PostgreSQL et un backend démarrés pour l'occasion, détruits ensuite |
| Rôle | Produire et vérifier une vraie sauvegarde qu'on veut garder | Prouver en continu que le mécanisme fonctionne toujours |
| Déclenchement | Manuel, ou via l'ordonnanceur de votre propre poste (voir plus bas) | Hebdomadaire (`cron: "17 5 * * 1"`) ou `workflow_dispatch` |

Il n'existe pas aujourd'hui de base de données de production persistante que ce dépôt pourrait sauvegarder automatiquement — voir ADR-0023 pour le raisonnement complet. Ne pas confondre les deux colonnes ci-dessus.

## Sauvegarder

```powershell
& .\scripts\backup\backup-postgres.ps1
```

Produit un fichier `.dump` (format personnalisé `pg_dump`) dans `backups/` (gitignoré — jamais commité). Fonctionne contre n'importe quel conteneur PostgreSQL nommé `taskira-postgres` par défaut (paramétrable).

## Restaurer et vérifier

```powershell
& .\scripts\restore\restore-postgres.ps1 -DumpFile ".\backups\taskira-<horodatage>.dump"
```

Ne restaure **jamais** sur la base active : démarre toujours un conteneur PostgreSQL jetable séparé, y restaure la sauvegarde, puis affiche le nombre de lignes par table pour une vérification humaine. Le conteneur reste actif après coup pour inspection manuelle — le supprimer soi-même une fois la vérification terminée (`docker rm -f taskira-postgres-restore-check`), pour qu'une vraie vérification ne soit jamais silencieusement sautée.

Une sauvegarde qui n'a jamais été restaurée avec succès ne doit pas être considérée comme fiable (`AGENTS.md` §14).

## Planifier une sauvegarde locale récurrente

Ce dépôt ne configure jamais silencieusement l'ordonnanceur d'un poste — c'est une modification d'état persistant hors du dépôt, à la décision de la personne qui l'exécute. Deux exemples pour qui veut sauvegarder sa base de développement (ou, plus tard, une vraie base de production) régulièrement :

**Windows (Tâches planifiées)** :

```powershell
$action = New-ScheduledTaskAction -Execute "pwsh.exe" `
    -Argument '-NoProfile -File "D:\chemin\vers\Taskira\scripts\backup\backup-postgres.ps1"'
$trigger = New-ScheduledTaskTrigger -Daily -At 3am
Register-ScheduledTask -TaskName "Taskira PostgreSQL Backup" -Action $action -Trigger $trigger
```

**Linux/macOS (cron)** :

```cron
0 3 * * * pwsh -NoProfile -File /chemin/vers/Taskira/scripts/backup/backup-postgres.ps1
```

Prévoir une rotation (supprimer les sauvegardes de plus de N jours) selon l'espace disque disponible — non fourni par défaut, `backups/` n'a aujourd'hui aucune politique de rétention automatique.

## Exercice planifié (`backup-restore-drill.yml`)

Chaque semaine (et sur demande via `workflow_dispatch`) : démarre un vrai PostgreSQL et le vrai backend, applique réellement toutes les migrations Flyway, sème de vraies données via l'API HTTP réelle (un utilisateur, un projet, un ticket), sauvegarde, restaure dans un conteneur jetable, puis vérifie que les données semées sont bien revenues. Échoue si `pg_restore` échoue ou si les données ne reviennent pas — un vrai signal, pas seulement un job vert par construction.
