# Stratégie de tests

## Objectif

Le filet de tests permet de faire évoluer l'architecture et les versions sans régression fonctionnelle ou de sécurité. Les tests vérifient d'abord les comportements et contrats observables; la couverture est un indicateur, pas un objectif isolé.

## État de la phase 2

La baseline ne contenait qu'un test de contexte, trois tests des réponses de sécurité et deux tests Angular élémentaires. La phase 2 a ajouté une séparation Maven Surefire/Failsafe, des tests métier et MockMvc, PostgreSQL Testcontainers, une suite Vitest ciblée et Playwright.

La chaîne validée le 14 août 2026 comprend :

- 11 tests backend rapides;
- 3 tests d'intégration backend sur PostgreSQL 16.15 avec Flyway, soit 14 tests backend au total;
- 20 tests Vitest portant sur l'authentification, l'intercepteur, les guards et le rendu asynchrone de la connexion;
- 3 parcours Playwright obligatoires sans compte;
- 1 parcours de connexion réelle optionnel, validé séparément avec des variables d'environnement;
- le build Angular de production.

Cette livraison est validée mais la phase 2 reste partielle : les rapports et seuils JaCoCo/V8 sont verts, mais Playwright ne couvre pas encore les workflows projets, tickets, membres et commentaires exigés par le référentiel.

## Pyramide retenue

| Niveau | Outils | Responsabilité | Dépendances actuelles |
| --- | --- | --- | --- |
| Backend unitaire | JUnit 5, Mockito, AssertJ | Règles métier, transitions, autorisations de service, cas limites | Aucune infrastructure |
| Web/API | MockMvc, Spring Security Test | Routes, validation, sérialisation, statuts et droits | Contexte web ciblé |
| Persistance/intégration | Spring Boot Test, Testcontainers PostgreSQL | Requêtes JPA, contraintes, transactions et migrations Flyway | PostgreSQL éphémère réel |
| Frontend unitaire/composant | Vitest, Angular TestBed | Services, guards, intercepteurs, formulaires et rendu | Doubles HTTP ciblés |
| Parcours navigateur, phase 2 | Playwright | Connexion et protections de route | Image Playwright vers stack Compose locale |
| Parcours navigateur, phase 3 | Playwright dans GitHub Actions | Parcours critiques reproductibles | Stack Compose dédiée, isolée et éphémère |

H2 n'est pas utilisé pour simuler PostgreSQL. Les tests d'intégration démarrent la base, appliquent Flyway de `V1` à la dernière migration, exécutent les scénarios puis détruisent le conteneur.

## Répartition attendue par changement

- Une règle métier ou un calcul : tests unitaires rapides, avec cas nominal, refus et limites.
- Un endpoint : tests MockMvc du contrat JSON, de la validation et des réponses `2xx`/`4xx` pertinentes.
- Une autorisation : au moins un cas autorisé et un cas interdit côté backend; un guard Angular n'est jamais la seule preuve.
- Une requête ou migration : test d'intégration PostgreSQL Testcontainers, y compris contraintes et index importants.
- Un service ou état Angular : test Vitest; ajouter un test de composant si le rendu ou l'interaction change.
- Un parcours critique : test Playwright stable avec données contrôlées dans la stack isolée définie par le workflow de phase 3; son exécution GitHub distante reste à valider.
- Un défaut corrigé : test de régression reproductible avant le correctif.

## Isolation et données

- Les tests sont déterministes, indépendants de la base de développement et parallélisables lorsque possible.
- Utiliser des builders/fixtures minimaux. Ne jamais copier de données personnelles ni de secrets de production.
- Contrôler explicitement le temps, les identifiants et l'aléatoire lorsque le résultat en dépend.
- Nettoyer les données par transaction ou recréation contrôlée du conteneur; ne pas dépendre de l'ordre d'exécution.
- Les futurs scénarios métier E2E créeront leurs données dans la stack dédiée déjà définie par le workflow et ne publieront traces, captures ou vidéos qu'en cas utile au diagnostic.

## Portes de qualité

Un lot applicatif est acceptable lorsque :

1. les tests nouveaux et existants sont verts;
2. le backend compile et son package est produit avec Java 21;
3. le frontend passe Vitest et le build Angular de production;
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

La stack locale doit être démarrée avant l'image E2E :

```powershell
docker compose -f infra/docker-compose.yml up -d --build
docker build -f frontend/Dockerfile.e2e -t taskira-frontend-e2e frontend
docker run --rm --add-host=host.docker.internal:host-gateway taskira-frontend-e2e
```

L'image utilise des proxies TCP locaux pour conserver les origines `http://localhost:4200` et `http://localhost:8080` dans le navigateur tout en atteignant la stack Compose sur l'hôte Docker.

Le scénario de connexion réelle s'active uniquement avec `TASKIRA_E2E_EMAIL` et `TASKIRA_E2E_PASSWORD` fournis à l'exécution. Aucune valeur n'est stockée dans le dépôt. Sans ces variables, les trois scénarios obligatoires passent et le quatrième est explicitement ignoré.

Ce flux manuel ne provisionne pas lui-même une stack E2E isolée. Le workflow local de phase 3 définit un projet Compose dédié avec base et volume éphémères, attend la disponibilité, exécute Playwright puis détruit la stack avec ses volumes; son exécution GitHub distante reste à valider.

## Dette npm connue

Le scan du lockfile effectué le 14 août 2026 signalait 6 vulnérabilités de sévérité élevée dans l'arbre de production et 35 vulnérabilités au total, dont 1 critique dans l'outillage de développement. Cette dette est connue et doit être traitée explicitement pendant les phases 4 et 6.

Ne pas lancer `npm audit fix` automatiquement : cela modifierait les versions sans la non-régression ni la revue prévues pour les montées de version. Refaire l'audit au moment du traitement, car les avis de sécurité évoluent.

## CI de phase 3

Le workflow local `.github/workflows/ci.yml` exécute les tests backend et frontend, les rapports de couverture, le build Angular, puis Playwright sur une stack Compose dédiée et supprimée avec ses volumes. `actionlint` 1.7.12 passe.

La phase reste partielle tant qu'un run GitHub distant n'est pas vert, que le lint frontend n'est pas intégré et que les checks requis ne protègent pas `main` lorsque les permissions le permettent. SonarQube et les scans bloquants appartiennent à la phase 4.
