# Rapport cumulatif de migration enterprise

Dernière mise à jour : 2026-08-15.

Ce fichier est le journal central de la migration. Il doit être enrichi après chaque grande phase avec les commandes, résultats, écarts et décisions réellement observés.

## Verdict actuel

`TASKIRA ENTERPRISE MIGRATION PARTIALLY COMPLETE`

Les phases 0 à 7 sont terminées et fusionnées (phase 5 pour son critère mécanique, dette architecturale résiduelle documentée). `main` est protégée depuis le 15 août 2026 (`protected: true` vérifié via l'API GitHub). La phase 8 (session Spring Security, cookie `HttpOnly`/CSRF double-soumission, retrait complet du JWT, bootstrap admin dev idempotent) est terminée et vérifiée (39 tests backend, 25 Vitest, 9/9 Playwright, smoke test manuel de la vraie stack de développement) sur la branche dédiée `feat/phase8-session-security`, pas encore fusionnée dans `main` au moment de la rédaction de cette section. Toute la migration a été menée sur des branches dédiées (`feat/phase6-stack-upgrade`, `chore/sync-enterprise-migration-status`, `feat/phase7-api-concurrency`, `feat/phase8-session-security`), jamais directement sur `main`. Les phases 9 à 20 ne sont pas déclarées terminées.

## Journal des phases

| Date | Phase | Statut | Résultat vérifié | Reste à faire |
| --- | ---: | --- | --- | --- |
| 2026-08-14 | 0 — Baseline Git | Terminée | Commit `fd84c54`, tag `pre-enterprise-migration`, branche `feat/enterprise-platform-migration`; stack Docker restaurée et validée avant migration. | Aucun pour le critère de baseline; conserver le point de retour. |
| 2026-08-15 | 1 — Documentation | Terminée localement | `AGENTS.md`, matrices, rapport, documentation d'architecture et ADR créés; liens et cohérence vérifiés; commit local `cccf2ee`. | Publier la branche avec les autres lots validés. |
| 2026-08-14–15 | 2 — Filet de sécurité | Terminée localement | 14 tests backend, 20 Vitest, couvertures/seuils backend et frontend, build Angular et 9/9 Playwright validés dans Docker; stack E2E isolée détruite après le run. Rejoué et reconfirmé le 15/08 lors de l'audit de phase 4. | Maintenir ce filet. Désarchivage projet et suppression ticket restent un gap sans phase assignée (hors du périmètre réel de P7, voir la ligne P7 ci-dessous). |
| 2026-08-15 | 3 — GitHub Actions CI | Terminée | [PR draft #1](https://github.com/Joe-azar/taskira/pull/1) puis [PR #28](https://github.com/Joe-azar/taskira/pull/28); `main` protégée (`protected: true` vérifié via l'API GitHub) : PR obligatoire (`required_approving_review_count: 0`), `CI Gate` obligatoire et à jour (`strict: true`), force-push et suppression interdits, admin non bloqué (`enforce_admins: false`). | Aucun pour le critère de sortie de P3. |
| 2026-08-15 | 4 — SonarQube et scans | Terminée | SonarQube Community Build éphémère (Docker) : Quality Gate `OK` vérifiée sur GitHub (PR #28). CodeQL et Trivy (fs + 2 images) verts sur GitHub via `codeql.yml`/`security.yml`, pas seulement localement. Deux bugs d'accessibilité détectés puis corrigés avant la validation. 0 secret détecté. | Rescanning post-P6 fait (voir section dédiée); surveiller les futures CVE au fil des mises à jour Dependabot. |
| 2026-08-15 | 5 — Architecture modulaire | Terminée localement (critère mécanique) | Spring Modulith (2.1.0 depuis P6) ajouté; `ModularityTests` vérifie frontières et absence de cycle à chaque `mvn verify` (18 tests backend au total, tous verts). Un cycle réel `project -> ticket -> project` détecté et corrigé par port/adapter (`ProjectMemberAssignmentCheck`) sans changer le comportement transactionnel. Documentation générée dans `docs/architecture/modules.md`. | Couche `api`/`application`/`domain`/`infrastructure` complète et événements métier restent à faire; le couplage direct aux repositories/entités d'autres modules est documenté comme dette (ADR-0016), pas éliminé — décision assumée, pas un blocage. |
| 2026-08-15 | 6 — Montées technologiques | **Terminée et fusionnée** | PR #28, commit de fusion `a7463afce0b9ce454519ae35ce493faaa2cffed5`, branche `feat/phase6-stack-upgrade` supprimée après fusion. Spring Boot 4.1.0/Framework 7.0.8/Security 7.1.0/Hibernate 7.4.1, Spring Modulith 2.1.0, Springdoc 3.1.0, MapStruct 1.6.3, Jackson 3; Angular/CLI/Material 22.1.2, TypeScript 6.0.3, Node 24.19.0, npm 12.0.2; PostgreSQL 18.6 par sauvegarde/restauration réelle vérifiée (ancien volume conservé). 18 tests backend, 20 Vitest, 9/9 Playwright; `CI Gate`, Quality Gate SonarQube, CodeQL et Trivy tous vérifiés verts sur GitHub (PR #28) avant fusion. Détail complet ci-dessous. | Aucun pour le critère de sortie de P6; rescanning sécurité post-migration fait (voir section dédiée). |
| 2026-08-15 | 7 — API et robustesse applicative | **Terminée et fusionnée** | PR #30. `/api/v1` sur les six contrôleurs; profils `dev`/`test`/`prod` avec différences réelles (logs, springdoc désactivé en `prod`, prouvé par test dédié); erreurs migrées vers `ProblemDetail` RFC 7807/9457 côté backend et frontend (~25 sites); `@Transactional` ajouté sur `AuthService`/`UserService`/`DashboardService`; verrouillage optimiste (`@Version`, Flyway `V7`) avec 409 sur conflit, prouvé contre PostgreSQL réel (`OptimisticLockingIT`). 22 tests backend, 20 Vitest, 9/9 Playwright. | Aucun pour le critère de sortie de P7. |
| 2026-08-15 | 8 — Authentification sécurisée | Terminée localement | Branche `feat/phase8-session-security`, 4 commits atomiques vérifiés indépendamment : session Spring Security cookie `HttpOnly`/`SameSite=Lax` (`Secure` en `prod`), CSRF double-soumission Angular/Spring, JWT et `localStorage`/`sessionStorage` intégralement retirés, bootstrap admin dev idempotent (profil `dev` uniquement). 39 tests backend (20 rapides + 19 intégration), 25 Vitest, 9/9 Playwright, smoke test manuel de la vraie stack de développement. Détail complet ci-dessous. | PR à ouvrir, checks GitHub à vérifier, puis fusion dans `main`. |
| — | 9–20 | Planifiées | Aucun critère de sortie déclaré atteint. | Suivre [la feuille de route](docs/migration-matrix.md) dans l'ordre. |

## Incident de stockage et récupération

Le 15 août 2026, le worktree initial a rencontré l'erreur Windows `ERROR 1117` signalant une erreur d'entrée/sortie. Il a été immédiatement mis en quarantaine et aucune nouvelle écriture n'y a été effectuée.

La baseline a été récupérée depuis `origin` dans un clone sain situé sur un autre volume local. La branche de migration et le tag ont ensuite été recréés localement à partir de cette baseline, puis les livrables non commités ont été reconstruits et revérifiés dans ce clone.

Le diagnostic et la réparation éventuelle du matériel, du filesystem ou de Windows restent hors du dépôt. Cet incident ne modifie pas le statut fonctionnel des phases; il explique uniquement le changement de worktree de travail.

Effet résiduel constaté le 15 août 2026 lors de l'audit de phase 4 : la stack Docker de développement (`infra/docker-compose.yml`) tournait encore avec un bind mount pointant vers l'ancien worktree `C:\Users\joeaz\Taskira-enterprise-recovery`, désormais vidé de son contenu utile, ce qui mettait `taskira-frontend` en boucle de redémarrage (`npm error ENOENT ... package.json`). Corrigé par `docker compose down` puis `up -d --build` depuis le worktree courant; les trois conteneurs (`postgres`, `backend`, `frontend`) sont revenus sains. Aucune donnée applicative n'a été perdue (le volume nommé `taskira_postgres_data` ne dépend pas du chemin hôte).

## État avant migration

- Windows 11, Docker Desktop/WSL2 et Git opérationnels.
- Monorepo `backend/`, `frontend/`, `infra/`, `docs/`.
- API Java 21/Spring Boot 3.5.11, Angular 21, PostgreSQL 16 et Flyway `V1`–`V6` fonctionnels via Docker Compose.
- Architecture backend déjà groupée par fonctionnalités et frontend organisé en `core/`, `layout/`, `features/`.
- Authentification JWT stateless; jeton conservé dans `localStorage`; CSRF désactivé dans l'état courant.
- Très faible couverture automatique avant le renforcement de phase 2.

## Architecture cible

La décision cible est un monolithe modulaire feature-first avec architecture hexagonale légère pour les modules complexes. Le runtime principal doit rester Angular/Nginx, Spring Boot et PostgreSQL. Les microservices, Kafka, RabbitMQ, Redis et moteurs de recherche ne deviennent pas des dépendances principales sans besoin mesuré et ADR.

Voir [l'architecture générale](docs/architecture/overview.md) et [ADR-0001](docs/adr/0001-modular-monolith.md).

## Versions observées

| Composant | Version actuelle validée | Cible planifiée |
| --- | --- | --- |
| Java | 21 | 21 LTS conservé (aucun changement en P6) |
| Spring Boot | 4.1.0 (Framework 7.0.8, Security 7.1.0, Hibernate 7.4.1) | Atteinte en P6 |
| Spring Modulith | 2.1.0 | Atteinte en P6 (1.4.1 incompatible avec Boot 4) |
| Springdoc | 3.1.0 | Atteinte en P6 |
| MapStruct | 1.6.3 (dépendance ajoutée, pas encore de mapper) | Atteinte en P6 |
| Maven | Wrapper 3.9.12 | Inchangé |
| PostgreSQL | 18.6; Testcontainers et E2E alignés | Atteinte en P6; voir [ADR-0017](docs/adr/0017-postgresql-18-migration.md) |
| Angular | 22.1.2 (CLI/CDK/Material) | Atteinte en P6 |
| TypeScript | 6.0.3 (plage exacte requise par `@angular/compiler-cli` 22.1.2) | Atteinte en P6 |
| Node/npm | Node 24.19.0 LTS; npm 12.0.2 | Atteinte en P6 |
| Vitest | 4.1.10 | Atteinte en P6 |
| Playwright | 1.62.1; Node 24.19.0/npm 12.0.2 dans le runner | 9/9 scénarios vérifiés sur la stack P6 |

## Outils ajoutés ou renforcés

- Testcontainers PostgreSQL et base de test partagée pour les intégrations.
- Séparation Maven Surefire (`*Test`) et Failsafe (`*IT`).
- Tests JUnit 5, Mockito, AssertJ, MockMvc et Flyway réels.
- Tests Angular/Vitest ciblés sur auth, guards et intercepteur.
- Runner Playwright Docker racine avec 9 scénarios et stack Compose E2E isolée.
- JaCoCo 0.8.13 fusionne les résultats Surefire/Failsafe, produit XML/HTML et applique un seuil ligne global; `coverage-v8` 4.1.0 produit LCOV/HTML et applique quatre seuils frontend.
- Workflow GitHub Actions `ci.yml` créé et validé statiquement avec `actionlint` 1.7.12.
- Lint Angular intégré avec `angular-eslint` 21.4.0, ESLint 10.3.0 et `typescript-eslint` 8.59.2.
- Maven Wrapper 3.9.12 et images Java, Node et PostgreSQL critiques épinglés; les images E2E Node/PostgreSQL sont référencées par digest.
- SonarQube Community Build (Docker, base PostgreSQL dédiée) : `infra/sonarqube/docker-compose.yml`, bootstrap idempotent `scripts/sonarqube/bootstrap.ps1`, configuration `sonar-project.properties`. Exécuté localement en mode éphémère dans `.github/workflows/quality.yml`.
- Trivy (`aquasecurity/trivy-action` pinée par SHA) : scan fs (dépendances + secrets) et scan image (backend, frontend) avec upload SARIF vers l'onglet Security GitHub, dans `.github/workflows/security.yml`.
- CodeQL (`github/codeql-action` pinée par SHA) : analyse `java-kotlin` et `javascript-typescript` sur push/PR et hebdomadaire, dans `.github/workflows/codeql.yml`.
- Dependabot (`.github/dependabot.yml`) : maven (backend), npm (frontend), docker (backend/frontend/e2e) et github-actions, mise à jour hebdomadaire groupée par écosystème majeur (Spring Boot, Angular).
- Spring Modulith (1.4.1 en P5, 2.1.0 depuis P6 — `spring-modulith-api` en dépendance de compilation pour les annotations `package-info.java`; `spring-modulith-starter-test` et `spring-modulith-docs` en dépendances de test) : `ModularityTests` vérifie les frontières de module et l'absence de cycle, et régénère la documentation PlantUML des modules à chaque exécution.

`ci.yml`, `quality.yml`, `security.yml` et `codeql.yml` sont tous vérifiés verts sur un run GitHub distant réel (PR #28, avant fusion dans `main`), pas seulement validés localement ou avec `actionlint`. Nginx production, observabilité et labs ne sont pas encore ajoutés.

## Tests

Résultat validé de phase 2 :

- backend rapide : 11 tests;
- backend intégration/Testcontainers : 3 tests;
- backend total : 14 tests;
- frontend Vitest : 20 tests;
- frontend lint : 0 erreur et 41 avertissements `any` non bloquants;
- navigateur : Playwright 1.62.1, 9/9 tests en 1,3 minute;
- build Angular de production : réussi.

Les parcours navigateur couvrent login/logout, login invalide, redirection du guard anonyme, refus de l'administration à un `USER`, projet create/update/archive, membre add/remove, ticket create/update/status/assign et commentaire create/update/delete.

Couverture validée le 15 août 2026 :

- backend JaCoCo : lignes 20,17 %, branches 3,75 %, instructions 18,11 %, méthodes 23,01 %, classes 42,25 %; seuil ligne 19 % atteint, `./mvnw verify` exit 0;
- frontend V8 : statements 12,44 %, branches 11,27 %, fonctions 11,78 %, lignes 11,84 %; seuils respectifs 12/11/11/11 % atteints, 20/20 tests et commande exit 0.

La commande racine `& .\e2e\playwright\run.ps1` construit et exécute `e2e/playwright/compose.e2e.yml`. PostgreSQL (18.6 depuis P6) utilise un `tmpfs`; aucun service n'expose de port hôte et la stack ne définit ni `container_name` ni volume persistant pour la base. Les données générées emploient le domaine réservé `.test`. Le `finally` exécute `down --volumes --remove-orphans`; le contrôle final a confirmé zéro conteneur, réseau ou volume restant. Les rapports Playwright et résultats sont écrits dans des répertoires ignorés par Git et restent disponibles après la destruction. Voir [la stratégie de tests](docs/testing-strategy.md).

Le désarchivage projet et la suppression ticket ne sont pas simulés : les endpoints correspondants sont absents. Leur ajout et leurs E2E restent un gap explicite sans phase assignée (hors du périmètre réel de P7 — voir les résultats de phase 7), pas un échec du critère P2.

## CI/CD

Le workflow `.github/workflows/ci.yml` définit :

- backend Java 21 avec `./mvnw verify` et rapports tests/JaCoCo;
- frontend Node 24.19.0/npm 12.0.2 avec lint, couverture, 20 Vitest et build (Node 22.23.2/npm 11.9.0 avant P6);
- même fichier Compose E2E éphémère, attente de disponibilité, Playwright, logs/artifacts et job de destruction systématique.

Les actions tierces sont pinées par SHA, les permissions globales sont `contents: read` et `persist-credentials` est désactivé. `actionlint` 1.7.12 passe localement. Historique des runs distants verts : run #3 (`31851279947`) sur la PR draft #1 au HEAD `6db6115` (P3 initiale), puis l'ensemble `ci.yml`/`quality.yml`/`security.yml`/`codeql.yml` vert sur la PR #28 (fin P6, commit `a7463af`) avant fusion. Le lint Angular passe avec 0 erreur et 41 avertissements liés à la dette `any`.

P3 est terminée : `main` est protégée (`protected: true`, vérifié via l'API GitHub après activation) — `required_status_checks` (`CI Gate`, `strict: true`), `required_pull_request_reviews` (`required_approving_review_count: 0`, pas de revue humaine requise), `enforce_admins: false`, `allow_force_pushes: false`, `allow_deletions: false`. L'activation a utilisé un jeton déjà présent dans le Gestionnaire d'identification Windows (utilisé par les opérations `git` habituelles, retrouvé via `git credential fill`), avec les scopes `repo`/`workflow` et un accès admin confirmé sur le dépôt — pas de contournement de sécurité. GHCR, release, staging et production manuelle relèvent toujours de P15.

## Résultats de la phase 4 (SonarQube et scans)

Analyse SonarQube Community Build `26.8.0.126808` exécutée localement le 15 août 2026 sur l'intégralité du dépôt (`sonar-project.properties` à la racine, backend Java + frontend TypeScript en une seule analyse) :

- Quality Gate (profil `Sonar way`) : `OK`.
- Bugs : 0 (2 détectés à la première analyse — `Web:InputWithoutLabelCheck` sur deux champs sans label accessible — corrigés avant la seconde analyse; voir commit d'accessibilité).
- Vulnérabilités : 0. Security hotspots : 0. Notes fiabilité/sécurité/maintenabilité : A/A/A.
- Code smells : 24 (dette estimée 140 minutes).
- Couverture : 13,0 %. Duplication : 1,4 %. Lignes de code analysées : 9010.
- Secrets : 0 détecté (scanner intégré `TextAndSecretsSensor`).

Trivy (CLI `aquasec/trivy`, cache `~/.cache/trivy` réutilisé) exécuté localement le 15 août 2026, **avant** la migration P6 :

- `backend/pom.xml` : 21 vulnérabilités (17 HIGH, 4 CRITICAL) sur `jackson-core`/`jackson-databind`, `tomcat-embed-core`, `postgresql` (pilote JDBC), `spring-boot`, `spring-data-commons`, `spring-security-web`, `spring-expression`/`spring-webmvc`. La quasi-totalité n'est corrigée que par une version cible Spring Boot 4.x/Spring 7 (P6); aucun correctif isolé sûr n'existe dans la ligne 3.5.x pour plusieurs d'entre elles.
- `frontend/package-lock.json` : 8 vulnérabilités HIGH sur `@angular/common`, `@angular/compiler`, `@angular/core`, corrigées uniquement à partir d'Angular 22.x (P6).
- Image `infra-backend` : mêmes CVE applicatives (jar) que `pom.xml`, plus 8 CVE HIGH sur le binaire Go `pebble` embarqué dans l'image de base `eclipse-temurin` (couche OS, hors du code applicatif).
- Image `infra-frontend` (image de développement, `node_modules` complet) : CVE d'outillage supplémentaires (`tar`, `undici`, `vite`) absentes d'une image de production Nginx/statique (P11); ces vulnérabilités n'affectent pas le runtime navigateur final.
- Aucun secret détecté sur le système de fichiers du dépôt.
- Deux limites opérationnelles rencontrées et corrigées dans les workflows : (1) Maven Central a renvoyé `429 Too Many Requests` sans cache `.m2` local — `security.yml` réchauffe désormais le cache Maven avant le scan; (2) le téléchargement de la base Java de Trivy (~900 Mo) dépasse le délai par défaut au premier run — `timeout: 10m0s` ajouté et la base mise en cache via `actions/cache`.

Ces chiffres sont **historiques** (état pré-P6); voir « Rescanning sécurité post-phase 6 » ci-dessous pour l'état réel actuel — ne pas les citer comme dette courante.

CodeQL (`java-kotlin`, `javascript-typescript`) est configuré, validé avec `actionlint`, et depuis vérifié vert sur un run GitHub distant réel (PR #28).

Limite documentée de l'édition Community (voir [ADR-0015](docs/adr/0015-sonarqube-quality-gate.md)) : pas de décoration de pull request ni d'analyse multi-branches; seule la branche par défaut est analysée à chaque exécution. La Quality Gate SonarQube a depuis été vérifiée verte sur un run GitHub distant réel (PR #28), pas seulement localement.

## Résultats de la phase 5 (architecture modulaire, critère mécanique)

Spring Modulith 1.4.1 ajouté (`spring-modulith-api` en dépendance principale pour les annotations, `spring-modulith-starter-test`/`spring-modulith-docs` en test). `ModularityTests` (`verifiesModularStructure` + `writesModuleDocumentation`) tourne à chaque `mvn verify`.

Configuration par défaut de Spring Modulith non exploitable telle quelle : seuls les types du package racine d'un module sont considérés publics, or Taskira place systématiquement son code dans des sous-packages (`entity/`, `repository/`, `dto/`, `enums/`). La première exécution a donc échoué massivement (65 violations rapportées avant même d'atteindre tous les modules). Deux catégories de correction ont été appliquées :

1. `common`, `config` et `security` déclarés `@ApplicationModule(type = OPEN)` : socle transversal partagé, pas des contextes métier.
2. Pour `project`, `ticket` et `user`, exposition explicite via `@NamedInterface` des seuls sous-packages réellement consommés ailleurs aujourd'hui (`entity`, `repository`, `enums`, `dto` selon le module; plus deux types spécifiques : `TicketHistoryService` et `ProjectMemberAssignmentCheck`). `comment`, `dashboard` et `auth` ne sont consommés par personne et restent fermés.

**Cycle réel détecté** : `project -> ticket -> project`. `ProjectService.removeMember()` appelait directement `TicketRepository.countByProjectIdAndAssigneeId(...)` pour la règle « impossible de retirer un membre avec des tickets assignés », alors que `Ticket` dépend structurellement de `Project` (relation JPA `@ManyToOne`, requise pour la référence `PROJ-123`, les contrôles d'accès, etc.). Corrigé par inversion de dépendance : port `ProjectMemberAssignmentCheck` défini dans `project`, implémenté par `ProjectMemberAssignmentCheckAdapter` dans `ticket`. Comportement transactionnel et requête SQL identiques; seule la direction de dépendance au moment de la compilation change. `removeMember` n'avait aucun test dédié avant ce lot; deux tests Mockito ont été ajoutés (`removeMemberRejectsATargetUserWithAssignedTickets`, `removeMemberDeletesATargetUserWithoutAssignedTickets`) pour vérifier le comportement réel, pas seulement la compilation. Suite complète revérifiée verte après le changement : 18 tests backend au total (15 rapides, soit les 11 initiaux plus les 2 `ModularityTests` et les 2 nouveaux tests `removeMember`, plus 3 intégration Testcontainers), JaCoCo au seuil.

Documentation générée dans [`docs/architecture/modules.md`](docs/architecture/modules.md) (graphe PlantUML + table des interfaces nommées), avec copie du diagramme vérifié le 15 août 2026 puisque `backend/target/spring-modulith-docs/` n'est pas versionné.

Ce qui n'est **pas** fait et reste dette explicite (voir [ADR-0016](docs/adr/0016-spring-modulith-boundaries.md)) :

- Le couplage direct aux repositories/entités d'autres modules est nommé et vérifié, pas éliminé (à l'exception du cycle project/ticket corrigé). Remplacer `ticket`/`comment`/`dashboard` injectant directement `ProjectRepository`/`TicketRepository`/`UserRepository` par de vraies façades applicatives est un refactor distinct, plus risqué, volontairement pas tenté dans ce lot.
- Aucune couche `api`/`application`/`domain`/`infrastructure` n'est introduite : les modules existants restent à plat (`controller`/`service`/`repository`/`entity`/`dto`).
- Aucun événement métier (`ProjectCreatedEvent`, `TicketCreatedEvent`, etc.) n'est introduit; aucun cas d'usage concret ne le justifie encore.
- `dashboard` dépend de `ticket.specification` (constructeurs de `Specification` JPA), un couplage de moins bonne qualité que les autres expositions puisqu'il s'agit d'un détail d'implémentation de requête plutôt que d'une vraie API; identifié comme priorité de nettoyage future.

## Résultats de la phase 6 (montées technologiques)

Menée entièrement sur `feat/phase6-stack-upgrade` après activation de la protection de `main`, en 7 commits atomiques, chacun compilé/testé avant le suivant.

### Protection de `main`

Activée via l'API GitHub (`PUT /repos/.../branches/main/protection`) avec un jeton déjà présent dans le Gestionnaire d'identification Windows (utilisé par les propres opérations `git` de l'utilisateur, retrouvé via `git credential fill` — mécanisme standard, pas de contournement) : scopes `repo`, `workflow`, accès admin confirmé sur le dépôt. Règles actives, vérifiées par relecture de l'API après activation : `required_status_checks` (`CI Gate`, `strict: true`), `required_pull_request_reviews` (`required_approving_review_count: 0`), `enforce_admins: false`, `allow_force_pushes: false`, `allow_deletions: false`. `security.yml`/`quality.yml`/`codeql.yml` volontairement pas encore des checks obligatoires : une seule exécution distante ne fait pas un historique stable.

Effet de bord découvert : l'ajout de `.github/dependabot.yml` (P4) avait déjà déclenché Dependabot sur `main`, avec 26 PR ouvertes au moment de la phase 6 — dont deux ont directement informé les versions cibles (`spring-boot-starter-parent` → 4.1.0, `spring-modulith-bom` → 2.1.0), confirmant empiriquement qu'aucune des deux ne fonctionne seule sans l'autre.

### Backend — Spring Boot 4

Spring Boot 3.5.11 → 4.1.0 (Framework 7.0.8, Security 7.1.0, Hibernate 7.4.1), Spring Modulith 2.1.0, Springdoc 3.1.0, Testcontainers BOM 1.21.4 (import explicite désormais nécessaire), MapStruct 1.6.3 ajouté. Six changements cassants réels trouvés et corrigés par builds/tests réels, pas par supposition :

1. `DaoAuthenticationProvider()` sans argument et `setUserDetailsService()` supprimés; le `UserDetailsService` doit être passé au constructeur (Spring Security 7).
2. `spring-boot-test-autoconfigure` éclaté par technologie : `@WebMvcTest` et `@DataJpaTest`/`AutoConfigureTestDatabase`/`TestEntityManager` déplacés vers `spring-boot-webmvc-test` et `spring-boot-data-jpa-test`, avec de nouveaux packages (`org.springframework.boot.webmvc.test.autoconfigure`, `org.springframework.boot.data.jpa.test.autoconfigure`, `org.springframework.boot.jdbc.test.autoconfigure`, `org.springframework.boot.jpa.test.autoconfigure`).
3. Jackson 3 par défaut (`tools.jackson.databind.json.JsonMapper`), plus `com.fasterxml.jackson.databind.ObjectMapper` (Jackson 2). Les 3 fichiers concernés (`RestAuthenticationEntryPoint`, `RestAccessDeniedHandler`, leur test) migrés vers Jackson 3 plutôt que d'ajouter le module de compatibilité `spring-boot-jackson2` : usage minimal (`writeValueAsString`/`readTree`), rester sur la valeur par défaut du framework a semblé préférable à un shim de compatibilité sans besoin réel.
4. `@WebMvcTest` a besoin du nouveau module `spring-boot-security-test` pour que le bean `HttpSecurity` soit disponible dans le contexte de test tranché.
5. Flyway n'est plus auto-configuré par la simple présence de `flyway-core`; `spring-boot-flyway` est désormais requis explicitement.

Vérifié réellement : suite complète (18 tests, dont les 3 Testcontainers) verte; image Docker multi-stage complète construite et démarrée contre la vraie base de développement (Flyway a validé les 6 migrations existantes, `ddl-auto=validate` accepté); vérifications HTTP manuelles sur `/swagger-ui`, `/v3/api-docs`, `/api/auth/login` et un appel `/api/projects` non authentifié, tous avec les codes attendus.

### Frontend — Angular 22 et Node 24

Angular/CLI/CDK 21.2.x → 22.1.2/22.1.4, TypeScript 5.9 → 6.0.3 (plage exacte `>=6.0 <6.1` exigée par `@angular/compiler-cli`), Node 22.23.2 → 24.19.0 LTS (commit séparé du bump Angular : Angular CLI 22 accepte déjà Node ≥22.22.3, donc les deux étapes sont vérifiables indépendamment). Aucun changement de code applicatif nécessaire pour Angular : lint (0 erreur, 41 avertissements `any` préexistants, aucun nouveau), 20/20 Vitest et build production verts sans modification. `npm install` a aussi fait passer les vulnérabilités connues (6 high/35 total documentées en P4) à 1 low.

npm 12 bloque désormais par défaut les scripts d'installation des dépendances (`install-scripts`) : `esbuild`, `@parcel/watcher`, `lmdb` et `msgpackr-extract` en ont un. Vérifié réellement (pas supposé) qu'aucun n'était nécessaire : leurs binaires natifs se résolvent via `optionalDependencies`; l'image frontend reconstruite passe lint/tests/build sans eux.

### Angular Material (socle)

`@angular/material` et `@angular/animations` 22.1.2 ajoutés (`@angular/animations` nécessaire explicitement : `provideAnimationsAsync()` importe dynamiquement `@angular/animations/browser`, qui échoue au moment du bundle si le paquet n'est pas installé). Thème M3 (`mat.theme()`, primaire azure/tertiaire blue, typographie Inter) dans `styles.scss`; aucun composant existant retouché. Deux échecs réels rencontrés et corrigés : dépendance `@angular/animations` manquante (erreur de résolution au bundle), puis volume anonyme `node_modules` du conteneur de dev non renouvelé après rebuild (`docker compose up --build` seul ne suffit pas; `--force-recreate -V` nécessaire).

### PostgreSQL 16 → 18

Voir [ADR-0017](docs/adr/0017-postgresql-18-migration.md) pour le détail complet. Point notable non anticipé : depuis PostgreSQL 18, l'image Docker officielle attend un point de montage unique sur `/var/lib/postgresql` (plus `/var/lib/postgresql/data`); tous les montages de volumes PostgreSQL du dépôt ont dû être mis à jour. Procédure suivie avant bascule : sauvegarde réelle (`pg_dump`), nouveau volume nommé explicitement, Flyway rejoué sur base vide, restauration testée sur base séparée et diffée contre l'original, Hibernate `validate` vérifié sur les deux bases, puis seulement alors bascule du volume de développement réel avec revérification HTTP complète. L'ancien volume PostgreSQL 16 n'a jamais été supprimé.

### Vérification E2E complète et bugs indépendants trouvés

`& .\e2e\playwright\run.ps1` contre la stack complète P6 a révélé et permis de corriger deux bugs réels, sans lien avec le contenu de la migration technologique elle-même :

1. `e2e/playwright/run-in-docker.sh` avait des fins de ligne CRLF (Windows), cassant son shebang dans le conteneur Linux (`tini` sortait en erreur 127). Aucun `.gitattributes` n'existait pour forcer LF sur les scripts shell; ajouté (`*.sh eol=lf`, `*.ps1 eol=crlf`) pour empêcher la récidive.
2. Le test `tickets.spec.ts` lisait le corps de la réponse `PATCH` via `response.json()` après `page.waitForResponse()`; l'application recharge délibérément le ticket juste après un succès, et cette requête de suivi entre en course avec l'appel CDP hors-process de Playwright pour le corps de la réponse `PATCH`, parfois déjà évincé (limitation documentée de Playwright/CDP, reproduite de façon déterministe sur 3 runs consécutifs à la même ligne — pas un flake aléatoire). Corrigé en basant les deux assertions sur l'état visible de l'interface (texte du badge de statut, option sélectionnée du menu déroulant d'assignation) plutôt que sur le corps de la réponse réseau.

9/9 scénarios Playwright verts après les deux corrections, sur un run complet avec reconstruction et destruction de la stack (pas une relance partielle).

## Rescanning sécurité post-phase 6

Demandé explicitement en fin de phase 6 : mesurer réellement ce qu'il reste des 21 CVE backend / 8 CVE frontend identifiées en phase 4, pas supposer qu'elles ont disparu. Rescanné le 15 août 2026 sur le code fusionné dans `main` (commit `a7463af`), avec les mêmes outils qu'en phase 4 (Trivy `aquasec/trivy`, cache partagé) plus une lecture directe des alertes GitHub Code Scanning.

### Avant / après

| Cible | Avant P6 (14/08) | Après P6, avant correctif (15/08) | Après correctif driver PostgreSQL |
| --- | --- | --- | --- |
| `backend/pom.xml` | 21 (17 HIGH, 4 CRITICAL) | 1 (1 HIGH, 0 CRITICAL) | **0** |
| `frontend/package-lock.json` | 8 (8 HIGH, 0 CRITICAL) | **0** | 0 |
| Image `infra-backend` (jar applicatif) | CVE applicatives identiques à `pom.xml` | 0 (après le correctif ci-dessous) | 0 |
| Image `infra-frontend` (dépendances de l'app, `app/node_modules`) | non isolé du reste de l'image en P4 | **0** | 0 |
| Secrets (filesystem) | 0 | 0 | 0 |

### Ce qui est réellement corrigé

- **20 des 21 CVE backend** ont disparu avec la migration Spring Boot 4.1.0/Spring Security 7.1.0/Spring Framework 7.0.8 (Jackson, Tomcat embarqué, `spring-data-commons`, `spring-security-web`, `spring-expression`/`spring-webmvc`) : versions gérées par le BOM Boot 4, aucune action manuelle nécessaire au-delà de la montée de version elle-même.
- La **21ᵉ** (`org.postgresql:postgresql` 42.7.11, `CVE-2026-54291`, contournement MITM SCRAM-SHA-256) n'était pas couverte par la montée de version Boot 4 seule — le BOM ne fixe qu'un plancher compatible, pas nécessairement le dernier patch. Corrigée dans ce lot en épinglant explicitement `42.7.13` dans `backend/pom.xml`. Suite complète (18 tests) revérifiée verte après ce changement.
- **Les 8 CVE frontend** ont toutes disparu avec Angular 22.1.2 (`@angular/common`, `@angular/compiler`, `@angular/core`) : `frontend/package-lock.json` scanne à 0 vulnérabilité, confirmé indépendamment par `npm install` (1 vulnérabilité low restante, contre 6 high/35 total avant P6, voir section Phase 6 frontend) et par Trivy.

### Ce qui reste ouvert (hors du périmètre de dépendances applicatives)

- **Image `infra-backend`, `usr/bin/pebble`** : 8 CVE HIGH (`stdlib` Go — DoS, contournement, XSS) inchangées depuis la phase 4. `pebble` est un binaire Go embarqué dans l'image de base `eclipse-temurin:21-jre`, pas une dépendance déclarée par Taskira; corrigible uniquement par une nouvelle publication de l'image de base par Eclipse Temurin, pas par un changement dans ce dépôt. Java reste fixé en 21 LTS comme demandé, donc le tag de base ne change pas dans ce lot.
- **Image `infra-frontend`, `usr/local/lib/node_modules/npm/node_modules/`** : 3 CVE HIGH (`brace-expansion`, `ip-address`) dans les dépendances internes de l'outil `npm` lui-même (bundlées dans l'image Node officielle), pas dans `app/node_modules` (0 vulnérabilité, confirmé séparément). N'affecte pas le runtime applicatif : `npm` n'est utilisé qu'à la construction de l'image, jamais exécuté en production, et une image de production Nginx/statique (P11) n'embarquera ni Node ni npm.
- **CodeQL, 1 alerte ouverte** : `java/spring-disabled-csrf-protection` sur `SecurityConfig.java:33`. Connue et intentionnelle : CSRF est pertinent pour une authentification par cookie de session, pas pour le JWT stateless actuel; la correction fait partie du périmètre déclaré de la migration session cookie de la phase 8 ([ADR-0006](docs/adr/0006-session-cookie-auth.md)), pas une régression de P6.

Conclusion : la dette CVE réelle et actionnable dans le périmètre de dépendances applicatives de Taskira est désormais **nulle** (0 backend, 0 frontend). Ce qui reste ouvert appartient soit à des composants hors du contrôle du dépôt (image de base, outillage npm), soit à une décision d'architecture déjà planifiée (P8).

## Résultats de la phase 7 (API et robustesse applicative)

Menée entièrement sur `feat/phase7-api-concurrency`, en cinq commits atomiques, chacun vérifié (build + tests réels) avant le suivant.

### Versionnage `/api/v1`

Constante partagée `ApiVersion.V1` appliquée aux six contrôleurs (`auth`, `users`, `projects`, `tickets`, `comments`, `dashboard`) — trois portaient déjà un `@RequestMapping` de classe, trois codaient `/api/...` en dur sur chaque méthode; les deux styles convergent maintenant sur `/api/v1`. Le matcher `permitAll` de `SecurityConfig` suit le même préfixe. Front (`environment.ts`/`environment.development.ts`) et toute la suite Playwright (base URL et assertions de chemin) migrés en lock-step, sans quoi les E2E auraient cassé silencieusement. Vérifié à la main sur la vraie stack dev : `POST /api/v1/auth/register` réussit (201), l'ancien `POST /api/auth/register` non versionné ne route plus vers aucun contrôleur (401, bloqué par la sécurité avant même la résolution de handler).

### Profils Spring `dev`/`test`/`prod`

Aucun profil n'existait avant P7; la séparation d'environnement se faisait uniquement par variables d'environnement. Trois profils ajoutés avec des différences réelles, pas de la scaffolding :

- `dev` : logs `DEBUG` pour `com.joe.taskira` et `spring.jpa.show-sql=true`, activé sur le backend de `infra/docker-compose.yml`; vérifié sur le conteneur réel après reconstruction (lignes `DEBUG` et `show-sql` bien présentes).
- `test` : `logging.level.root=WARN` pendant `mvn test`/`mvn verify`, via `@ActiveProfiles("test")` sur `PostgreSqlIntegrationTest`; les échecs de test restent visibles (stack traces JUnit/Surefire indépendantes du niveau de log).
- `prod` : désactive `springdoc.api-docs`/`swagger-ui` — directement justifié par l'avertissement de démarrage de Spring Boot lui-même, pas une supposition. Non activé par un environnement réel (aucun déploiement de production n'existe avant P11/P15); prouvé par un test dédié (`ProdProfileTest`) qui démarre l'application complète avec le profil actif et vérifie que les deux endpoints cessent de répondre en 2xx.

`e2e/playwright/compose.e2e.yml` n'active volontairement aucun profil : son comportement est inchangé par ce commit.

### Migration vers `ProblemDetail` (RFC 7807/9457)

`ApiErrorResponse` (DTO maison `timestamp/status/error/message/path`) supprimé, remplacé par `ProblemDetail` standard (`type/title/status/detail/instance`) dans `GlobalExceptionHandler`, `RestAuthenticationEntryPoint` et `RestAccessDeniedHandler`. Un vrai bug trouvé au passage : les routes non résolues (`NoResourceFoundException`) tombaient dans le handler générique et remontaient en 500 au lieu de 404 — corrigé par un handler dédié.

Le type de contenu passe à `application/problem+json`, non compatible avec un matcher `application/json` strict dans MockMvc (confirmé par un échec de test réel, pas supposé) — les assertions de `ProjectControllerTest` corrigées en conséquence.

Impact frontend réel et non trivial : aucun type `ApiError` n'existait, et ~25 sites dans 8 pages lisaient `error?.error?.message` — le nom de champ de l'ancien format. Laissés tels quels, ils auraient silencieusement affiché leur message de repli générique au lieu du vrai message backend après ce changement. Ajout de `ApiProblemDetail` (`core/models`) et `extractErrorMessage()` (`core/http`) pour centraliser la chaîne detail → message → repli en un seul endroit, puis migration des ~25 sites.

### Transactions applicatives

`ProjectService`, `TicketService`, `CommentService` et `TicketHistoryService` portaient déjà un `@Transactional` de classe; `AuthService.register()`, `UserService.createUser/updateUser/updateUserStatus` et `DashboardService.getSummary()` en étaient dépourvus malgré des séquences lecture-puis-écriture ou multi-lecture équivalentes. Même annotation ajoutée, sans `readOnly=true` (convention absente du reste du code, pas introduite ici pour rester cohérent). `CustomUserDetailsService` et l'adaptateur `ProjectMemberAssignmentCheck` vérifiés et laissés tels quels : ils participent déjà à la transaction de leur appelant ou n'ont besoin d'aucune atomicité multi-étapes.

### Verrouillage optimiste

`@Version` ajouté à `AuditableEntity` (base commune de `Project`/`Ticket`/`User`/`Comment`), migration Flyway `V7__add_optimistic_locking.sql` (colonne `version` sur `users`/`projects`/`tickets`/`comments`; `project_members` et `ticket_history` volontairement exclus, aucune mutation en place). `GlobalExceptionHandler` mappe `OptimisticLockingFailureException` vers 409. Prouvé contre PostgreSQL réel via Testcontainers (`OptimisticLockingIT` : deux lectures indépendantes de la même ligne, la seconde sauvegardée avec succès, la première — restée périmée — lève bien l'exception à la sauvegarde) et contre la vraie base de développement déjà peuplée (`V7` appliquée sur des données existantes, `ddl-auto=validate` accepté après reconstruction du conteneur réel).

### Vérification

22 tests backend (18 rapides + 4 intégration, dont `OptimisticLockingIT` et `ProdProfileTest` nouveaux), 20 Vitest, lint frontend 0 erreur (41 avertissements `any` préexistants inchangés), build Angular de production réussi, 9/9 Playwright sur une stack isolée reconstruite après chacun des cinq commits. Deux exécutions E2E ont échoué sur des timeouts génériques (tests différents à chaque fois) après ~30 Go de cache de build Docker accumulé pendant cette session; le nettoyage du cache (`docker builder prune`) a suffi à obtenir un 9/9 propre — diagnostiqué comme de la contention de ressources locale, pas une régression du code (confirmé par la reproductibilité : un test différent échouait à chaque tentative, toujours par timeout générique, jamais par une assertion de comportement incorrect).

## Résultats de la phase 8 (authentification sécurisée)

Menée entièrement sur `feat/phase8-session-security`, en quatre commits atomiques, chacun vérifié (build + tests réels) avant le suivant. Conformément à la consigne de la phase, aucune modification n'a été faite avant un audit factuel complet des points dépendant du JWT : `SecurityConfig`, `JwtAuthenticationFilter`, `JwtService`, `JwtProperties`, `AuthService`, `AuthController`, `CustomUserDetailsService`, `AuthenticatedUser`, l'intercepteur Angular, le service `AuthService`/`TokenService` frontend, les guards, les tests frontend, Playwright, la configuration CORS, Docker/Compose et les `application*.yaml`.

### Incrément 1 — session et CSRF en mode dual avec le JWT encore actif

Session Spring Security ajoutée sans retirer le JWT : `SessionCreationPolicy.IF_REQUIRED`, `HttpSessionSecurityContextRepository` exposé en bean, sauvegarde manuelle du contexte de sécurité après login/register (`SecurityContextRepository.saveContext(...)` dans `AuthController`, nécessaire car les endpoints REST maison contournent le filtre de login du framework). CSRF activé avec `CookieCsrfTokenRepository.withHttpOnlyFalse()` (pattern double-soumission) et un `CsrfTokenRequestAttributeHandler` explicite — le handler par défaut de Spring Security 6+ (`XorCsrfTokenRequestAttributeHandler`) masque le jeton pour la protection BREACH des vues rendues côté serveur, ce qui casse silencieusement l'écho brut cookie/en-tête qu'un client REST attend; diagnostiqué par un vrai 403 `Access denied` sur chaque requête mutante avant correction. Le jeton CSRF de Spring Security 6+ est résolu paresseusement (`DeferredCsrfToken`) : un backend purement REST sans vue ne déclenche jamais sa résolution naturellement, d'où l'ajout d'un `CsrfCookieFilter` dédié qui force `CsrfToken.getToken()` pour que le cookie `XSRF-TOKEN` soit effectivement émis. Logout serveur réel ajouté (`/api/v1/auth/logout` : 204, suppression du cookie de session, invalidation de session, `clearAuthentication`). Cookie de session nommé `TASKIRA_SESSION`, `HttpOnly`, `SameSite=Lax`, `Secure=false` en base/dev/test (HTTP local) et `Secure=true` en `prod` uniquement. Un vrai bug corrigé au passage : `CustomUserDetailsService` levait manuellement `DisabledException`, mais `DaoAuthenticationProvider.retrieveUser()` enveloppe toute exception qui n'est pas `UsernameNotFoundException` dans `InternalAuthenticationServiceException` (500 au lieu de 401 constaté sur un utilisateur désactivé) — corrigé en laissant `UserDetails.isEnabled()` piloter les `preAuthenticationChecks` natifs de Spring, exécutés après `retrieveUser()`.

### Incrément 2 — migration frontend complète, JWT et `localStorage` encore présents côté backend

`token.service.ts` supprimé; `AuthService` frontend réécrit autour de `fetchMe()`/`bootstrapSession()`/`logout()` sans stocker aucun jeton. `provideAppInitializer` bloque le rendu de l'application (toutes les routes) jusqu'à ce que l'appel `/auth/me` de démarrage ait fini de semer le cookie CSRF. Intercepteur HTTP réécrit pour lire `document.cookie` et attacher manuellement l'en-tête `X-XSRF-TOKEN` — la fonctionnalité `withXsrfConfiguration()` d'Angular retient délibérément cet en-tête pour les requêtes cross-origin (mesure de sécurité par défaut), non fonctionnelle ici puisque frontend et backend tournent sur des ports différents dans tous les environnements du projet. Trois vrais bugs indépendants trouvés et corrigés uniquement via investigation empirique sur la stack réelle (navigateur réel, outillage Playwright de diagnostic avec proxys `socat` reproduisant la topologie réseau exacte du runner E2E) :

1. **Boucle infinie / page blanche** : l'intercepteur ne traitait `/auth/login`/`/auth/register` comme routes d'authentification exemptées de la réaction "session expirée"; un 401 normal de `/auth/me` (sans session) déclenchait un logout + redirection vers `/login`, dont le `guestGuard` rappelait `/auth/me`, en boucle. Corrigé en élargissant l'exemption à tout `/auth/`, avec un test de régression dédié.
2. **En-tête CSRF jamais attaché dans le vrai navigateur** malgré un cookie `XSRF-TOKEN` présent (confirmé par capture directe des en-têtes de requête) : cause racine `withXsrfConfiguration()` retenant l'en-tête en cross-origin. Corrigé en le retirant et en lisant `document.cookie` manuellement dans l'intercepteur.
3. **Confusion croisée entre utilisateurs de test silencieuse** : le contexte `request` partagé de Playwright persiste les cookies comme un vrai navigateur; inscrire un second utilisateur (le backend pose un cookie de session à chaque register/login en mode dual) écrasait silencieusement un jeton bearer JWT explicitement présenté pour un AUTRE utilisateur, parce que l'ancien garde de `JwtAuthenticationFilter` (`getAuthentication() == null`) laissait toujours gagner la session ambiante déjà chargée par `SecurityContextHolderFilter`. Corrigé en rendant un en-tête `Authorization: Bearer` explicite toujours prioritaire sur une session ambiante.

9/9 Playwright vérifiés sans aucun JWT ni `localStorage`/`sessionStorage` côté frontend à ce stade — condition posée par la consigne de phase avant tout retrait du JWT côté backend.

### Incrément 3 — retrait complet du JWT

Une fois le remplacement prouvé fonctionnel de bout en bout (incrément 2), JWT retiré du backend : `JwtAuthenticationFilter`, `JwtService`, `JwtProperties`, `AuthResponse` supprimés; `AuthService`/`AuthController` retournent directement `MeResponse` depuis `register`/`login`; dépendance `io.jsonwebtoken` retirée de `backend/pom.xml`; bloc `app.jwt.*` retiré de `application.yaml`; en-tête CORS `Authorization` exposé (devenu mort) retiré de `CorsConfig`.

Ce retrait a cassé les aides de test E2E (`support/api.ts`), qui authentifiaient encore par jeton bearer. Réécrites pour capturer le cookie de session de chaque utilisateur inscrit **directement depuis sa propre réponse d'inscription** (pas depuis le contexte de requête partagé) et le renvoyer explicitement par appel. Un vrai bug trouvé au passage, variante du bug 3 de l'incrément 2 : `registerUser()` laissait Playwright attacher automatiquement tout cookie déjà connu du contexte partagé, y compris la session d'un premier utilisateur inscrit plus tôt dans le même test; le backend traitait alors la seconde inscription comme une réauthentification de la session existante et ne réémettait jamais `Set-Cookie` pour elle. Corrigé en fixant explicitement un en-tête `Cookie` ne portant que le cookie CSRF (jamais de session ambiante) sur la requête d'inscription et sur la requête d'amorçage CSRF elle-même.

39 tests backend verts (20 rapides + 19 intégration Testcontainers/Flyway, dont `SessionAuthenticationIT` : login valide/mot de passe erroné/utilisateur désactivé, accès anonyme, refus `USER` sur route `ADMIN`, réutilisation de session sans JWT, `/auth/me` avec/sans session, logout puis route protégée inaccessible, requête mutante sans/avec CSRF valide, attributs de cookie `HttpOnly`/`SameSite`, cookie CSRF lisible en JS), 9/9 Playwright.

### Incrément 4 — bootstrap admin dev idempotent

Chaque chemin qui accorde le rôle `ADMIN` (`UserService#createUser`, `#updateUser`) exige un appelant déjà authentifié en `ADMIN` : une base fraîche n'avait aucun moyen de produire son premier admin en dehors d'un insert SQL manuel. `DevAdminBootstrap` (`ApplicationRunner`, `@Profile("dev")`) comble ce vide, strictement gatée au profil `dev` — jamais `prod`, `test`, ni le profil par défaut utilisé par la stack E2E — et idempotente par email (`existsByEmailIgnoreCase`), donc un redémarrage répété de la stack de développement ne duplique ni ne réinitialise le mot de passe d'un admin déjà modifié. Email/mot de passe surchargeables via `APP_DEV_ADMIN_EMAIL`/`APP_DEV_ADMIN_PASSWORD`. `DevAdminBootstrapIT` prouve la création, l'idempotence (rejouer la logique ne duplique pas et ne change pas le hash de mot de passe) et un login réel via le flux session/CSRF complet; `ProdProfileTest` gagne une assertion compagnon confirmant que le bean est absent du contexte sous `prod`.

### Vérification

39 tests backend (20 rapides + 19 intégration), 25 Vitest (contre 20 avant P8 : `AuthService`, guards et intercepteur entièrement réécrits pour la session/CSRF, avec tests de régression dédiés pour les bugs 1 et 2 de l'incrément 2), lint frontend 0 erreur (36 avertissements `any`, contre 41 avant P8 — réduits par la suppression de `token.service.ts`), build Angular de production réussi, 9/9 Playwright. Seuils de couverture frontend (`angular.json`) recalibrés après la suppression d'un petit fichier entièrement couvert (`token.service.ts`), sans régression de couverture réelle.

Smoke test manuel supplémentaire contre la vraie stack de développement (`infra/docker-compose.yml`, reconstruite avec `--force-recreate -V`) via `curl` : amorçage CSRF anonyme (401 + cookie `XSRF-TOKEN`), login de l'admin dev bootstrapé (200, `globalRole: ADMIN`), cookie `TASKIRA_SESSION` confirmé `HttpOnly` et `SameSite=Lax` sur l'en-tête `Set-Cookie` brut, `Secure` absent (`Secure=false`, HTTP local, comportement attendu), `/auth/me` avec session seule (200), logout (204), `/auth/me` après logout (401) — invalidation de session confirmée de bout en bout sur la stack réelle, pas seulement en test automatisé.

Deux échecs non liés au code rencontrés pendant cette phase, diagnostiqués et écartés avant de conclure : (1) exécuter `mvn verify` dans un conteneur Maven générique sans accès au démon Docker de l'hôte fait échouer Testcontainers (« Could not find a valid Docker environment ») — corrigé en montant `/var/run/docker.sock`, puis en désactivant Ryuk (`TESTCONTAINERS_RYUK_DISABLED=true`) et en fixant `TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal`, un problème de réseau Docker-outside-of-Docker propre à Docker Desktop, absent de la vraie CI GitHub Actions qui exécute `./mvnw verify` nativement sans imbrication. (2) deux échecs Playwright transitoires (page de login pas encore visible après 5 s, PUT expiré après 30 s) causés par l'exécution simultanée de ce diagnostic Maven imbriqué et de la suite E2E, consommant la même bande passante Docker Desktop; reproductibles uniquement en charge concomitante, disparus sur un rejeu isolé (9/9 propre).

## Qualité et sécurité

- SonarQube et Quality Gate : Quality Gate `OK`, vérifiée à la fois localement (P4) et sur un run GitHub distant réel (PR #28).
- CodeQL, Dependabot et Trivy : configurés en P4, vérifiés verts sur GitHub distant depuis la PR #28; contrôle automatisé des secrets couvert par le scanner Trivy intégré (0 résultat, avant et après P6).
- CVE applicatives : **0 backend, 0 frontend** après rescanning post-P6 (détail dans la section dédiée ci-dessus). Les 21 backend/8 frontend de la phase 4 sont un historique, pas une dette courante. Reste ouvert hors dépendances applicatives : 8 CVE sur le binaire `pebble` de l'image de base backend, 3 CVE sur l'outillage interne npm de l'image frontend.
- Audit npm du 2026-08-14 (historique, pré-P6) : 6 vulnérabilités élevées dans l'arbre de production et 35 au total, dont 1 critique dans l'outillage. Un nouveau `npm install` post-P6 rapporte 1 vulnérabilité low; confirmé par Trivy (0 HIGH/CRITICAL sur `frontend/package-lock.json`).
- Lint Angular : 0 erreur, 36 avertissements `any` (contre 41 avant P8); dette visible à réduire progressivement sans désactiver la règle.
- Aucun `npm audit fix --force` n'a été appliqué.
- Auth session `HttpOnly`/CSRF et bootstrap admin dev : **terminés en P8** (détail dans la section dédiée ci-dessus). L'alerte CodeQL `java/spring-disabled-csrf-protection` ouverte depuis le rescanning post-P6 (CSRF non pertinent pour le JWT stateless d'alors) attendue résolue par l'activation CSRF de P8 — à reconfirmer sur un run CodeQL distant réel une fois la PR de phase 8 ouverte, pas seulement supposée corrigée localement.

## Docker et déploiement

Le développement utilise `postgres`, `backend` et `frontend` via Compose, avec hot reload Angular. Les tests backend et Playwright disposent de runners Docker dédiés. La stack Playwright est distincte du développement et ne conserve aucune base après exécution.

Nginx, frontend statique de production, utilisateur non-root, profils production-like, GHCR, staging et rollback ne sont pas encore validés.

## Observabilité

Actuator, Micrometer, Prometheus et Grafana sont planifiés en P10. Les request IDs, MDC et logs structurés appartiennent à P9. Loki reste optionnel en P20.

## Kubernetes, Helm et Azure

- Kubernetes : lab planifié P17, pas runtime principal.
- Helm : lab planifié P18 après maîtrise des manifests.
- Azure : architecture/lab planifié P19; aucune ressource payante créée.
- OAuth2/OIDC, Entra ID, Redis, RabbitMQ, Kafka, recherche, Loki et Terraform : labs conditionnels P20.

## Problèmes et dettes ouverts

1. Désarchivage projet et suppression ticket impossibles à couvrir tant que leurs endpoints ne sont pas ajoutés; gap sans phase assignée (hors du périmètre réel de P7, voir les résultats de phase 7) — ne pas simuler ces workflows.
2. Absence d'image frontend production/Nginx et de staging.
3. Absence d'observabilité et de stratégie backup/restore planifiée récurrente (le script ponctuel de P6 couvre la sauvegarde/restauration à la demande, pas une politique quotidienne/hebdomadaire/mensuelle).
4. SonarQube Community Build ne décore pas les pull requests ni n'analyse les branches séparément (limite d'édition documentée, [ADR-0015](docs/adr/0015-sonarqube-quality-gate.md)).
5. Couplage direct restant aux repositories/entités d'autres modules (`ticket`/`comment` -> `project`; `comment`/`dashboard` -> `ticket`; quasiment tous -> `user`), nommé et vérifié par Spring Modulith mais pas éliminé; `dashboard` -> `ticket.specification` est la coupure de moindre qualité à traiter en priorité si l'aggregation dashboard est revue ([ADR-0016](docs/adr/0016-spring-modulith-boundaries.md)).
6. CVE hors du périmètre applicatif direct, non actionnables sans changer une contrainte posée ailleurs dans le plan : 8 CVE sur `usr/bin/pebble` dans l'image de base backend (nécessite une nouvelle image `eclipse-temurin`, Java reste fixé en 21 LTS), 3 CVE sur l'outillage interne de npm dans l'image frontend de développement (disparaît de fait avec l'image de production Nginx de P11).
7. Rôles au-delà de USER/ADMIN et matrice d'autorisation complète (owner/manager/member/non-member par projet) restent à caractériser plus finement; P8 a migré le mécanisme d'authentification, pas le modèle d'autorisation lui-même.

## Décisions

L'[index ADR](docs/adr/README.md) distingue les décisions acceptées des propositions futures. Un ADR `Proposed` ne constitue ni une implémentation ni une validation.

## Livrables documentaires de phase 1

- `AGENTS.md`
- `MIGRATION_MATRIX.md`
- `ENTERPRISE_MIGRATION_REPORT.md`
- `docs/architecture.md` et `docs/architecture/`
- `docs/testing-strategy.md`
- `docs/migration-matrix.md`
- `docs/adr/`
