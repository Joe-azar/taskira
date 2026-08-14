# Architecture des tests

Statut : socle P2 terminé et validé localement.

La stratégie détaillée, les commandes Docker-first et la dette connue sont maintenues dans [testing-strategy.md](../testing-strategy.md).

## Résultat actuel

- 11 tests backend rapides via Surefire.
- 3 tests d'intégration via Failsafe/PostgreSQL Testcontainers/Flyway.
- 20 tests Vitest.
- 9/9 scénarios Playwright en 1,3 minute sur une stack dédiée.
- Build Angular de production validé.

## Limites actuelles

- JaCoCo 0.8.13 : lignes 20,17 %, seuil 19 % vert; XML/HTML fusionné UT/IT.
- `coverage-v8` 4.1.0 : statements 12,44 %, branches 11,27 %, fonctions 11,78 %, lignes 11,84 %; seuils verts et LCOV/HTML produits.
- La couverture unitaire et d'intégration reste une baseline basse à relever progressivement.
- Playwright couvre login/logout, login invalide, guard anonyme, refus admin pour `USER`, projets, membres, tickets et commentaires.
- `e2e/playwright/compose.e2e.yml` isole PostgreSQL 16.15 en `tmpfs`, sans port hôte, `container_name` ni volume de base persistant; le runner détruit ensuite tous ses conteneurs, son réseau et ses volumes.

## Cible suivante

Le désarchivage projet et la suppression ticket ne disposent pas encore d'endpoints; ils seront ajoutés et testés en P7, sans simulation E2E. P3 possède localement le workflow de stack éphémère et les artefacts de diagnostic, mais attend un run GitHub distant, le lint et la protection de branche. P4 ajoute SonarQube et les scans.
