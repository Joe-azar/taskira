# Stratégie de tests

## Objectif

Le filet de tests permet de faire évoluer l'architecture et les versions sans régression fonctionnelle ou de sécurité. Les tests vérifient d'abord les comportements et contrats observables; la couverture est un indicateur, pas un objectif isolé.

## Où trouver les nombres actuels

Ce document décrit la stratégie et les outils, pas des nombres figés. Conformément à AGENTS.md §16, les nombres de tests, seuils de couverture et versions d'outils évoluent à chaque phase — ne jamais réutiliser un ancien chiffre comme s'il était toujours valable. Pour l'état réellement validé le plus récent (nombre de tests backend rapides/intégration, Vitest, Playwright, seuils JaCoCo/V8), consulter :

- [`ENTERPRISE_MIGRATION_REPORT.md`](../ENTERPRISE_MIGRATION_REPORT.md), section « Résultats » de la phase la plus récente;
- ou exécuter la suite réellement (commandes ci-dessous) pour obtenir le nombre du jour.

## Pyramide retenue

| Niveau | Outils | Responsabilité | Dépendances actuelles |
| --- | --- | --- | --- |
| Backend unitaire | JUnit 5, Mockito, AssertJ | Règles métier, transitions, autorisations de service, cas limites | Aucune infrastructure |
| Web/API | MockMvc, Spring Security Test | Routes, validation, sérialisation, statuts et droits | Contexte web ciblé |
| Persistance/intégration | Spring Boot Test, Testcontainers PostgreSQL | Requêtes JPA, contraintes, transactions et migrations Flyway | PostgreSQL éphémère réel |
| Architecture modulaire | Spring Modulith (`ModularityTests`) | Frontières de module, absence de cycle, `@NamedInterface` | Vérifié à chaque `mvn verify` |
| Frontend unitaire/composant | Vitest, Angular TestBed | Services, guards, intercepteurs, formulaires et rendu | Doubles HTTP ciblés |
| Parcours navigateur | Playwright | Authentification, autorisation et workflows métier | Runner racine et stack Compose dédiée, isolée et éphémère |
| CI | GitHub Actions (`ci.yml`) | Rejouer le même filet sur chaque push/PR | Même stack isolée que localement |

H2 n'est pas utilisé pour simuler PostgreSQL. Les tests d'intégration démarrent la base, appliquent Flyway de `V1` à la dernière migration, exécutent les scénarios puis détruisent le conteneur.

## Répartition attendue par changement

- Une règle métier ou un calcul : tests unitaires rapides, avec cas nominal, refus et limites.
- Un endpoint : tests MockMvc du contrat JSON, de la validation et des réponses `2xx`/`4xx` pertinentes.
- Une autorisation : au moins un cas autorisé et un cas interdit côté backend; un guard Angular n'est jamais la seule preuve.
- Une requête ou migration : test d'intégration PostgreSQL Testcontainers, y compris contraintes et index importants.
- Un service ou état Angular : test Vitest; ajouter un test de composant si le rendu ou l'interaction change.
- Un parcours critique : test Playwright stable avec données `.test` contrôlées dans la stack isolée du runner racine. Ne pas simuler un endpoint absent.
- Un défaut corrigé : test de régression reproductible avant le correctif.
- Un en-tête ou une préoccupation d'observabilité (ex. `X-Request-Id`) : assertion structurelle uniquement (présence, format, unicité) au niveau backend et, si pertinent, E2E; jamais une assertion qui dépend de lire les lignes de log — la stack Playwright isolée n'a pas d'infrastructure de récupération de logs.
- Un flux applicatif touchant à un conteneur externe réel (Mailpit, un registre de conteneurs, un cluster `kind`) : préférer un test d'intégration qui parle réellement à ce conteneur (Testcontainers ou équivalent) plutôt qu'un mock qui masquerait un vrai problème de câblage.

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
6. aucune vulnérabilité ou régression connue n'est masquée;
7. `ModularityTests` reste vert (aucun nouveau cycle entre modules).

Les seuils de couverture JaCoCo (backend) et V8 (frontend) sont relevés progressivement à mesure que la couverture réelle augmente, jamais abaissés pour faire passer une régression — voir `backend/pom.xml` et `frontend/angular.json` pour les valeurs actuellement en vigueur.

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

Construire d'abord le stage backend reproductible (Java 21, `unzip` requis pour vérifier le SHA-256 du Maven Wrapper). Le runner monte uniquement le socket Docker, le dépôt backend et un cache Maven nommé; il ne dépend ni de la base de développement ni du réseau Compose.

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

`verify` exécute les tests rapides via Surefire, puis les tests `*IT` via Failsafe, y compris `ModularityTests`. Les tests d'intégration démarrent l'image PostgreSQL réellement utilisée par le reste du dépôt (`infra/docker-compose.yml`), appliquent toutes les migrations Flyway existantes, valident Hibernate (`ddl-auto=validate`) et détruisent leur conteneur temporaire.

Les rapports JaCoCo (backend, XML/HTML) et V8 (frontend, LCOV/HTML) sont produits après fusion UT/IT — consulter le run le plus récent pour les chiffres réels plutôt qu'un ancien rapport.

### Playwright

Depuis la racine du dépôt, une seule commande construit la stack dédiée, attend les services, exécute les tests puis détruit les ressources :

```powershell
& .\e2e\playwright\run.ps1
```

Le runner utilise `e2e/playwright/compose.e2e.yml`, distinct de `infra/docker-compose.yml`. Il démarre backend, frontend et PostgreSQL sous les mêmes versions que le reste du dépôt, et exécute Playwright (`@playwright/test`, version fixée dans `frontend/package.json`) sous Node/npm épinglés. Les images sont épinglées par digest.

La base utilise un `tmpfs` et la stack n'expose aucun port hôte, ne fixe aucun `container_name` et ne crée aucun volume de données persistant. Le bloc `finally` exécute toujours `docker compose down --volumes --remove-orphans` : un run propre laisse zéro conteneur, réseau ou volume résiduel.

Le désarchivage projet et la suppression ticket ne disposent toujours pas d'endpoints (gap connu, hors périmètre de toute phase assignée — voir `ENTERPRISE_MIGRATION_REPORT.md`, section « Problèmes et dettes ouverts »); aucun faux parcours n'est ajouté pour gonfler artificiellement le résultat E2E.

## Sécurité des dépendances

Ne pas lancer `npm audit fix --force` ou une mise à jour majeure automatique sans analyser les breaking changes (AGENTS.md §20). L'état des CVE réellement ouvertes (backend Maven, frontend npm, images Docker) est suivi dans `ENTERPRISE_MIGRATION_REPORT.md`, section « Qualité et sécurité » — refaire l'audit au moment du traitement plutôt que de se fier à un ancien chiffre, car les avis de sécurité évoluent en continu.

## CI

Le workflow `.github/workflows/ci.yml` exécute les tests backend (`mvnw verify`), le lint/Vitest/couverture/build frontend, puis la même stack Compose et la même suite Playwright que localement sur une stack dédiée détruite systématiquement (`always()`). Un job `CI Gate` agrège ces trois jobs et constitue le check obligatoire de la protection de `main`.

Des workflows séparés couvrent la qualité et la sécurité en continu : `quality.yml` (SonarQube Community Build), `codeql.yml` (CodeQL Java/TypeScript) et `security.yml` (Trivy — dépendances, secrets, images Docker) — voir [docs/architecture/ci-cd.md](architecture/ci-cd.md) pour le détail complet et l'état de chaque check. Consulter l'onglet Actions de GitHub pour le statut réellement courant plutôt qu'un ancien numéro de run.
