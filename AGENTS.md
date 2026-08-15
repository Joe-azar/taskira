# Règles de travail Taskira

Ce fichier s'applique à l'ensemble du dépôt. Des consignes placées dans un sous-répertoire peuvent le préciser sans affaiblir les exigences de sécurité, de migration ou de test.

## État de référence et ordre des phases

- La phase 0 est terminée : baseline récupérable au commit `fd84c54`, tag `pre-enterprise-migration` et branche `feat/enterprise-platform-migration`.
- La phase 1 documentaire est terminée au commit local `cccf2ee` : règles, matrices, rapport cumulatif, documentation d'architecture et ADR sont présents et validés.
- La phase 2 est terminée localement : 14 tests backend, 20 tests Vitest, leurs rapports/seuils de couverture et 9 parcours Playwright sont validés avec Docker. Le runner E2E utilise une stack dédiée et éphémère, puis la détruit intégralement.
- La phase 3 est partielle : le workflow GitHub Actions #3 est vert sur le commit `6db6115` de la PR draft #1, y compris le lint frontend. Seule la protection de `main` reste à activer; les phases 4 à 20 sont planifiées et aucune technologie future ne doit être présentée comme installée.
- Conserver Java 21, Spring Boot 3.5.x, Angular 21, Node 22 et PostgreSQL 16 jusqu'à la phase de montée de version prévue. Ne pas effectuer de mise à niveau opportuniste.
- Préférer des lots petits, réversibles et compatibles avec le comportement existant.

## Principes d'architecture

- Taskira reste un monolithe modulaire : une application Angular, une API Spring Boot et une base PostgreSQL.
- Organiser le code par fonctionnalité métier. Côté backend, faire évoluer progressivement `auth`, `user`, `project`, `ticket`, `comment` et `dashboard`. Côté frontend, conserver `core/`, `layout/` et `features/`.
- Chaque module backend expose une API publique interne étroite. Un module ne doit pas accéder directement aux repositories, entités internes ou détails d'infrastructure d'un autre module; il passe par l'API du module propriétaire.
- Garder `common`/`shared` strictement transversal. Une fonctionnalité ne doit pas devenir un dossier générique de classes réutilisables.
- Introduire les couches `api`, `application`, `domain` et `infrastructure` uniquement lorsqu'elles clarifient une fonctionnalité suffisamment complexe.
- Ne pas modifier le préfixe `/api`, introduire Spring Modulith ou renommer massivement les packages avant la phase architecture.
- Ne pas extraire de microservice ni ajouter de broker, cache, moteur de recherche ou orchestrateur sans besoin mesuré et ADR accepté.
- Documenter toute décision structurante ou difficilement réversible dans `docs/adr/`.

## Règles backend Java/Spring

- Utiliser les fonctionnalités compatibles Java 21 et conserver cette version jusqu'à la phase de montée technologique planifiée.
- Injecter les dépendances uniquement par constructeur. L'injection de champs avec `@Autowired` est interdite dans le code applicatif.
- Les contrôleurs valident et traduisent HTTP, puis délèguent. Ils ne contiennent ni règle métier, ni orchestration de persistance, ni gestion transactionnelle.
- Placer les frontières `@Transactional` dans la couche application/service qui porte le cas d'usage. Ne pas piloter une transaction depuis un contrôleur.
- Ne jamais écrire avec `System.out` ou `System.err`. Utiliser SLF4J avec des messages structurés, sans secret ni donnée personnelle.

## Développement Docker-first

- Exécuter les builds et validations dans Docker avec les wrappers et versions du dépôt. Le projet ne doit pas dépendre d'une installation globale de Java, Maven, Node, npm, Angular CLI ou PostgreSQL.
- Utiliser `infra/docker-compose.yml` comme environnement local de référence et exécuter les commandes documentées depuis la racine du dépôt.
- Valider au minimum `docker compose -f infra/docker-compose.yml config` après un changement d'infrastructure.
- Conserver le hot reload Angular en développement et la reconstruction explicite du backend tant qu'un autre flux n'est pas documenté.
- Toute nouvelle dépendance ou image doit être justifiée, versionnée de façon reproductible et analysée avant intégration.

## Base de données et Flyway

- Toute évolution de schéma passe par une nouvelle migration dans `backend/src/main/resources/db/migration/`.
- Ne jamais modifier une migration déjà appliquée (`V1` à `V6` à la date de cette règle). Ajouter la version suivante.
- Hibernate reste en `ddl-auto=validate`; il ne crée ni ne corrige le schéma.
- Tester les migrations sur PostgreSQL réel avec le socle Testcontainers livré en phase 2. Ne pas substituer H2.
- Une migration destructive exige une stratégie documentée de compatibilité, sauvegarde et retour arrière.

## Tests et qualité

- Tout changement de comportement inclut les tests pertinents dans le même lot. Toute correction de bug commence par un test de régression qui échoue sans le correctif.
- Tester les règles métier avec JUnit 5, Mockito et AssertJ; les contrats HTTP et autorisations avec MockMvc; la persistance et Flyway avec PostgreSQL Testcontainers.
- Tester les services, guards, intercepteurs et composants Angular avec Vitest. Maintenir les parcours critiques avec Playwright.
- Exécuter le lint Angular dans Docker. Les avertissements existants restent visibles et doivent diminuer; toute nouvelle erreur est bloquante.
- Exécuter la suite Playwright depuis la racine avec `& .\e2e\playwright\run.ps1`. Elle crée uniquement des identités `.test`, écrit ses rapports dans des répertoires ignorés et détruit conteneurs, réseau et stockage temporaire en fin de run.
- Ne pas simuler un parcours dont l'API n'existe pas. Le désarchivage projet et la suppression ticket attendent leurs endpoints de phase 7 avant ajout à la suite E2E.
- Un lot applicatif n'est terminé que si les tests concernés, le build backend, le build frontend et les contrôles de configuration applicables sont verts.
- Pour une modification purement documentaire, vérifier les liens relatifs et `git diff --check`; un build applicatif n'est pas requis.
- Suivre les commandes et niveaux détaillés dans `docs/testing-strategy.md`.

## Sécurité et configuration

- Ne jamais ajouter de mot de passe réel, jeton, clé privée, secret JWT ou donnée personnelle au dépôt, aux fixtures, aux logs ou à la documentation.
- Utiliser des variables d'environnement et ne versionner que des valeurs factices dans les fichiers d'exemple. Les identifiants Docker locaux ne doivent jamais être réutilisés hors développement.
- Le backend est l'autorité de sécurité. Chaque autorisation et règle d'accès est imposée et testée côté serveur; un guard ou un masquage Angular n'est jamais une barrière de sécurité.
- La cible d'authentification utilise des sessions ou cookies `HttpOnly`, `Secure` et `SameSite` avec une protection CSRF adaptée. Ne pas introduire de nouveau secret durable dans `localStorage`; la migration du JWT actuel doit rester compatible, testée et décidée par ADR.
- Toute évolution de l'authentification, de CORS, CSRF ou du stockage des jetons exige des tests de sécurité et, si elle change le modèle de confiance, un ADR.

## Git et portée des changements

- Inspecter l'état du dépôt avant et après chaque intervention. Préserver les changements utilisateur et les travaux parallèles sans les reformater ni les écraser.
- Ne pas mélanger mise à niveau, refactorisation structurelle et évolution métier dans un même lot sauf nécessité démontrée.
- Ne pas exécuter `git commit`, `git push`, créer/modifier un tag ou ouvrir une pull request. Laisser les changements non commités au propriétaire, sauf demande explicite contraire dans la tâche en cours.
- Lorsqu'un commit est explicitement autorisé, il reste atomique, porte une intention unique et inclut ses tests et sa documentation. `main` évolue uniquement par pull request revue; aucun push direct.
- Ne jamais réécrire l'historique ni employer une commande Git destructive pour nettoyer le travail d'autrui.

## Documentation de référence

- Architecture : `docs/architecture.md`
- Stratégie de tests : `docs/testing-strategy.md`
- Matrice des modules : `MIGRATION_MATRIX.md`
- Feuille de route des phases : `docs/migration-matrix.md`
- Journal cumulatif : `ENTERPRISE_MIGRATION_REPORT.md`
- Décisions : `docs/adr/`
