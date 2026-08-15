# Règles de travail Taskira

Ce fichier s'applique à l'ensemble du dépôt. Des consignes placées dans un sous-répertoire peuvent le préciser sans affaiblir les exigences de sécurité, de migration ou de test.

## État de référence et ordre des phases

- La phase 0 est terminée : baseline récupérable au commit `fd84c54`, tag `pre-enterprise-migration` et branche `feat/enterprise-platform-migration`.
- La phase 1 documentaire est terminée au commit local `cccf2ee` : règles, matrices, rapport cumulatif, documentation d'architecture et ADR sont présents et validés.
- La phase 2 est terminée localement : 14 tests backend, 20 tests Vitest, leurs rapports/seuils de couverture et 9 parcours Playwright sont validés avec Docker. Le runner E2E utilise une stack dédiée et éphémère, puis la détruit intégralement.
- La phase 3 est terminée : `main` est protégée depuis le 15 août 2026 (API GitHub vérifiée : `protected: true`) — PR obligatoire (`required_approving_review_count: 0`, pas de revue humaine requise), `CI Gate` obligatoire et à jour (`strict: true`), force-push et suppression de `main` interdits, admin non bloqué (`enforce_admins: false`). Le workflow `ci.yml` est vert sur GitHub.
- La phase 4 est terminée : SonarQube Community Build (Docker, éphémère dans `quality.yml`) donne une Quality Gate `OK` vérifiée sur un run GitHub réel; CodeQL et Trivy (dépendances + 2 images) sont verts sur GitHub via `codeql.yml` et `security.yml`. Ces trois workflows ainsi que `.github/dependabot.yml` sont exécutés à distance, pas seulement validés localement. Les CVE détectées en phase 4 (21 backend, 8 frontend) ont été rescannées après la phase 6 : voir la section sécurité post-P6 de `ENTERPRISE_MIGRATION_REPORT.md` pour les chiffres réels avant/après, ne pas réutiliser les anciens chiffres comme dette courante.
- La phase 5 est terminée localement pour son critère mécanique : Spring Modulith (`ModularityTests`) vérifie les frontières de module et l'absence de cycle à chaque `mvn verify`. `common`/`config`/`security` sont des modules `OPEN`; `project`, `ticket`, `user` exposent explicitement via `@NamedInterface` les sous-packages réellement consommés ailleurs (voir `docs/architecture/modules.md` et [ADR-0016](docs/adr/0016-spring-modulith-boundaries.md)). Un cycle réel `project -> ticket -> project` a été détecté et corrigé par inversion de dépendance (port `ProjectMemberAssignmentCheck` dans `project`, implémenté dans `ticket`), sans changement de comportement transactionnel. Le couplage direct restant aux repositories/entités d'autres modules est documenté comme dette, pas éliminé : la couche `api`/`application`/`domain`/`infrastructure` complète, les événements métier (`TicketCreatedEvent`, etc.) et le remplacement des accès repository directs par de vraies façades applicatives restent à faire. Ne pas les présenter comme faits.
- La phase 6 est **terminée et fusionnée** dans `main` (PR #28, commit de fusion `a7463afce0b9ce454519ae35ce493faaa2cffed5`, branche `feat/phase6-stack-upgrade` supprimée après fusion). Backend : Spring Boot 4.1.0 (Spring Framework 7.0.8, Spring Security 7.1.0, Hibernate 7.4.1), Spring Modulith 2.1.0, Springdoc 3.1.0, MapStruct 1.6.3, Jackson 3 (`tools.jackson`, plus `com.fasterxml.jackson`). Frontend : Angular/CLI/Material 22.1.2, TypeScript 6.0.3, Node 24.19.0 LTS, npm 12.0.2. Base de données : PostgreSQL 18.6, nouveau point de montage `/var/lib/postgresql` (voir [ADR-0017](docs/adr/0017-postgresql-18-migration.md)), migration par sauvegarde/restauration réelle avec l'ancien volume conservé intact. 18 tests backend, 20 Vitest et 9/9 Playwright verts; `CI Gate`, Quality Gate SonarQube, CodeQL et Trivy tous vérifiés verts sur GitHub avant la fusion. Deux bugs réels indépendants du contenu de la migration ont aussi été trouvés et corrigés en route (fins de ligne CRLF cassant l'entrypoint E2E, assertion Playwright fragile face à un rechargement applicatif, plus un bug cross-plateforme dans `bootstrap.ps1` découvert par le run CI distant). Voir `ENTERPRISE_MIGRATION_REPORT.md` pour le détail complet.
- Les phases 7 à 20 sont planifiées et aucune technologie future ne doit être présentée comme installée.
- Depuis la phase 6 : Java 21 (conservé, cible LTS), Spring Boot 4.1.x, Angular 22.1.x, Node 24 LTS, PostgreSQL 18.6. Ne pas effectuer de nouvelle mise à niveau majeure opportuniste hors d'une phase planifiée.
- Préférer des lots petits, réversibles et compatibles avec le comportement existant.

## Principes d'architecture

- Taskira reste un monolithe modulaire : une application Angular, une API Spring Boot et une base PostgreSQL.
- Organiser le code par fonctionnalité métier. Côté backend, faire évoluer progressivement `auth`, `user`, `project`, `ticket`, `comment` et `dashboard`. Côté frontend, conserver `core/`, `layout/` et `features/`.
- Chaque module backend expose une API publique interne étroite. Un module ne doit pas accéder directement aux repositories, entités internes ou détails d'infrastructure d'un autre module; il passe par l'API du module propriétaire.
- Garder `common`/`shared` strictement transversal. Une fonctionnalité ne doit pas devenir un dossier générique de classes réutilisables.
- Introduire les couches `api`, `application`, `domain` et `infrastructure` uniquement lorsqu'elles clarifient une fonctionnalité suffisamment complexe.
- Spring Modulith est introduit depuis la phase 5 (`ModularityTests`, `docs/architecture/modules.md`, [ADR-0016](docs/adr/0016-spring-modulith-boundaries.md)); toute nouvelle dépendance entre modules doit rester vérifiée par ce test, sans cycle. Toutes les routes sont versionnées sous `/api/v1` depuis la phase 7 (`common.web.ApiVersion`); ne pas introduire de route non versionnée ni renommer massivement les packages sans mettre à jour ce préfixe partout où il est utilisé (contrôleurs, `SecurityConfig`, frontend, E2E).
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
- Ne pas simuler un parcours dont l'API n'existe pas. Le désarchivage projet et la suppression ticket attendent leurs endpoints (gap sans phase assignée, hors du périmètre réel de la phase 7) avant ajout à la suite E2E.
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
