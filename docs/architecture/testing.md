# Architecture des tests

Statut : socle P2 partiellement livré et validé.

La stratégie détaillée, les commandes Docker-first et la dette connue sont maintenues dans [testing-strategy.md](../testing-strategy.md).

## Résultat actuel

- 11 tests backend rapides via Surefire.
- 3 tests d'intégration via Failsafe/PostgreSQL Testcontainers/Flyway.
- 20 tests Vitest.
- 3 scénarios Playwright obligatoires plus 1 connexion réelle optionnelle.
- Build Angular de production validé.

## Limites actuelles

- JaCoCo 0.8.13 : lignes 20,17 %, seuil 19 % vert; XML/HTML fusionné UT/IT.
- `coverage-v8` 4.1.0 : statements 12,44 %, branches 11,27 %, fonctions 11,78 %, lignes 11,84 %; seuils verts et LCOV/HTML produits.
- Couverture métier concentrée sur projets et authentification.
- E2E projets, tickets, membres et commentaires absents.
- Dans le flux manuel P2, l'image E2E cible la stack Compose de développement. Le workflow P3 réutilise cette image contre une stack CI dédiée et éphémère définie localement.

## Cible suivante

P2 doit compléter couverture et scénarios. P3 possède localement un workflow de stack éphémère et d'artefacts de diagnostic, mais attend un run GitHub distant et le lint. P4 ajoute SonarQube et les scans.
