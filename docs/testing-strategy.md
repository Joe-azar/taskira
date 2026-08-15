# Stratégie de tests

## Objectif

Le filet de tests permet de faire évoluer l'architecture et les versions sans régression fonctionnelle ou de sécurité. Les tests vérifient d'abord les comportements et contrats observables; la couverture est un indicateur, pas un objectif isolé.

## État de la phase 2

La baseline ne contenait qu'un test de contexte, trois tests des réponses de sécurité et deux tests Angular élémentaires. La phase 2 a ajouté une séparation Maven Surefire/Failsafe, des tests métier et MockMvc, PostgreSQL Testcontainers, une suite Vitest ciblée et Playwright.

La chaîne validée les 14 et 15 août 2026 comprend :

- 11 tests backend rapides;
- 3 tests d'intégration backend sur PostgreSQL 16.15 avec Flyway, soit 14 tests backend au total;
- 20 tests Vitest portant sur l'authentification, l'intercepteur, les guards et le rendu asynchrone de la connexion;
- 9/9 parcours Playwright en 1,3 minute sur une stack Docker isolée;
- le build Angular de production.

Playwright couvre la page de connexion, login/logout, login invalide, guard anonyme, refus de l'administration à un `USER`, projet create/update/archive, membre add/remove, ticket create/update/status/assign et commentaire create/update/delete. Les rapports et seuils JaCoCo/V8 sont également verts : la phase 2 est terminée localement.

## Pyramide retenue

| Niveau | Outils | Responsabilité | Dépendances actuelles |
| --- | --- | --- | --- |
| Backend unitaire | JUnit 5, Mockito, AssertJ | Règles métier, transitions, autorisations de service, cas limites | Aucune infrastructure |
| Web/API | MockMvc, Spring Security Test | Routes, validation, sérialisation, statuts et droits | Contexte web ciblé |
| Persistance/intégration | Spring Boot Test, Testcontainers PostgreSQL | Requêtes JPA, contraintes, transactions et migrations Flyway | PostgreSQL éphémère réel |
| Frontend unitaire/composant | Vitest, Angular TestBed | Services, guards, intercepteurs, formulaires et rendu | Doubles HTTP ciblés |
| Parcours navigateur, phase 2 | Playwright | Authentification, autorisation et workflows métier | Runner racine et stack Compose dédiée, isolée et éphémère |
| Parcours navigateur, phase 3 | Playwright dans GitHub Actions | Rejouer le même filet sur chaque changement | Même stack isolée; run GitHub #3 validé |

H2 n'est pas utilisé pour simuler PostgreSQL. Les tests d'intégration démarrent la base, appliquent Flyway de `V1` à la dernière migration, exécutent les scénarios puis détruisent le conteneur.

## Répartition attendue par changement

- Une règle métier ou un calcul : tests unitaires rapides, avec cas nominal, refus et limites.
- Un endpoint : tests MockMvc du contrat JSON, de la validation et des réponses `2xx`/`4xx` pertinentes.
- Une autorisation : au moins un cas autorisé et un cas interdit côté backend; un guard Angular n'est jamais la seule preuve.
- Une requête ou migration : test d'intégration PostgreSQL Testcontainers, y compris contraintes et index importants.
- Un service ou état Angular : test Vitest; ajouter un test de composant si le rendu ou l'interaction change.
- Un parcours critique : test Playwright stable avec données `.test` contrôlées dans la stack isolée du runner racine. Ne pas simuler un endpoint absent.
- Un défaut corrigé : test de régression reproductible avant le correctif.

## Isolation et données

- Les tests sont déterministes, indépendants de la base de développement et parallélisables lorsque possible.
- Utiliser des builders/fixtures minimaux. Ne jamais copier de données personnelles ni de secrets de production.
- Contrôler explicitement le temps, les identifiants et l'aléatoire lorsque le résultat en dépend.
- Nettoyer les données par transaction ou recréation contrôlée du conteneur; ne pas dépendre de l'ordre d'exécution.
- Les scénarios E2E créent des identités uniques sous le domaine réservé `.test`; ils ne copient ni compte ni donnée personnelle réelle.
- Les rapports HTML, traces et captures utiles sont écrits dans `e2e/playwright/playwright-report/` et `e2e/playwright/test-results/`. Ces répertoires sont ignorés par Git.

## Portes de qualité

Un lot applicatif est acceptable lorsque :

1. les tests nouveaux et existants sont verts;
2. le backend compile et son package est produit avec Java 21;
3. le frontend passe le lint, Vitest et le build Angular de production;
4. les migrations démarrent sur une base PostgreSQL vide;
5. les contrôles d'autorisation et contrats affectés sont couverts;
6. aucune vulnérabilité ou régression connue n'est masquée.

La première baseline de couverture possède des seuils conservateurs empêchant une baisse immédiate : backend lignes 19 %; frontend statements 12 %, branches 11 %, fonctions 11 % et lignes 11 %. P4 pourra les relever progressivement à partir des mesures validées, sans encourager des assertions artificielles.

## Exécution Docker-first

Exécuter toutes les commandes suivantes depuis la racine du dépôt.

### Configuration et frontend

```powershell
docker compose -f infra/docker-compose.yml config
docker build -f frontend/Dockerfile -t taskira-frontend-tests frontend
docker run --rm taskira-frontend-tests npm run lint
docker run --rm taskira-frontend-tests npm run test:unit
docker run --rm taskira-frontend-tests npm run test:coverage
docker run --rm taskira-frontend-tests npm run build
```

### Backend autonome avec Testcontainers

Construire d'abord le stage backend reproductible. Il fournit Java 21.0.11 et `unzip`, requis pour vérifier le SHA-256 de la distribution Maven Wrapper. Le runner monte uniquement le socket Docker, le dépôt backend et un cache Maven nommé; il ne dépend ni de la base de développement ni du réseau Compose.

```powershell
docker build --target build -t taskira-backend-build backend
docker run --rm `
  -e TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal `
  -v /var/run/docker.sock:/var/run/docker.sock `
  -v taskira_maven_cache:/root/.m2 `
  -v "${PWD}\backend:/workspace" `
  -w /workspace `
  taskira-backend-build `
  ./mvnw verify
```

`verify` exécute les tests rapides via Surefire, puis les tests `*IT` via Failsafe. Les tests d'intégration démarrent l'image épinglée `postgres:16.15-alpine3.23`, appliquent Flyway `V1` à `V6`, valident Hibernate et détruisent leur conteneur temporaire.

Le run validé du 15 août 2026 produit les rapports JaCoCo XML/HTML après fusion UT/IT : lignes 20,17 %, branches 3,75 %, instructions 18,11 %, méthodes 23,01 % et classes 42,25 %. Le seuil ligne 19 % passe avec `BUILD SUCCESS`.

Le frontend produit LCOV/HTML avec statements 12,44 %, branches 11,27 %, fonctions 11,78 % et lignes 11,84 %. Les seuils 12/11/11/11 % passent avec 20/20 tests.

### Playwright de phase 2

Depuis la racine du dépôt, une seule commande construit la stack dédiée, attend les services, exécute les tests puis détruit les ressources :

```powershell
& .\e2e\playwright\run.ps1
```

Le runner utilise `e2e/playwright/compose.e2e.yml`, distinct de `infra/docker-compose.yml`. Il démarre backend, frontend, PostgreSQL 16.15 et Playwright 1.62.1 sous Node 22.23.2/npm 11.9.0. Les images Node et PostgreSQL sont épinglées par digest.

La base utilise un `tmpfs` et la stack n'expose aucun port hôte, ne fixe aucun `container_name` et ne crée aucun volume de données persistant. Les proxies internes conservent les origines applicatives `http://localhost:4200` et `http://localhost:8080` sans dépendre de la stack locale.

Le bloc `finally` exécute toujours `docker compose down --volumes --remove-orphans`. Le run validé laisse zéro conteneur, réseau ou volume. Les deux répertoires de sortie ignorés restent disponibles sur l'hôte grâce à leurs bind mounts.

Le désarchivage projet et la suppression ticket ne disposent pas d'endpoints. Ils constituent un gap de P7 et seront testés lorsque l'API existera; aucun faux parcours n'est ajouté pour gonfler le résultat P2.

## Dette npm connue

Le scan du lockfile effectué le 14 août 2026 signalait 6 vulnérabilités de sévérité élevée dans l'arbre de production et 35 vulnérabilités au total, dont 1 critique dans l'outillage de développement. Cette dette est connue et doit être traitée explicitement pendant les phases 4 et 6.

Ne pas lancer `npm audit fix` automatiquement : cela modifierait les versions sans la non-régression ni la revue prévues pour les montées de version. Refaire l'audit au moment du traitement, car les avis de sécurité évoluent.

## CI de phase 3

Le workflow `.github/workflows/ci.yml` exécute les tests backend, puis le lint, les 20 Vitest, les rapports de couverture et le build Angular, avant le même fichier Compose et la même image Playwright sur une stack dédiée. Un job `always()` la supprime avec ses volumes. `actionlint` 1.7.12 passe.

Le run GitHub #3 (`31851279947`) est vert sur la PR draft #1 au commit `6db6115` : Backend, Frontend, Containers and E2E et CI Gate réussissent. Le lint utilise `angular-eslint` 21.4.0, ESLint 10.3.0 et `typescript-eslint` 8.59.2; il produit 0 erreur et 41 avertissements `any`, laissés visibles comme dette de typage.

La phase reste partielle uniquement parce que les checks ne protègent pas encore `main` : l'API signale `protected=false` et aucun ruleset. Le connecteur reçoit `403` pour les réglages administratifs; l'activation, disponible sans offre payante, nécessite l'interface GitHub ou un jeton administrateur interactif. SonarQube et les scans bloquants appartiennent à la phase 4.
