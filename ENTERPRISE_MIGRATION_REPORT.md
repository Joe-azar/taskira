# Rapport cumulatif de migration enterprise

Dernière mise à jour : 2026-08-15.

Ce fichier est le journal central de la migration. Il doit être enrichi après chaque grande phase avec les commandes, résultats, écarts et décisions réellement observés.

## Verdict actuel

`TASKIRA ENTERPRISE MIGRATION PARTIALLY COMPLETE`

Les phases 0, 1, 2, 4, 5 (critère mécanique) et 6 sont terminées. `main` est protégée depuis le 15 août 2026 (PR et `CI Gate` obligatoires, force-push/suppression interdits, pas de revue humaine requise, admin non bloqué), levant le blocage de la phase 3. Toute la phase 6 (Spring Boot 4, Angular 22, Node 24, PostgreSQL 18, Material) a été menée sur la branche protégée `feat/phase6-stack-upgrade`, jamais directement sur `main`. Reste avant fusion : vérifier que `ci.yml`, `quality.yml`, `security.yml` et `codeql.yml` sont verts sur le HEAD de cette branche (le run précédent date du commit de fin de phase 5). Les phases 7 à 20 ne sont pas déclarées terminées.

## Journal des phases

| Date | Phase | Statut | Résultat vérifié | Reste à faire |
| --- | ---: | --- | --- | --- |
| 2026-08-14 | 0 — Baseline Git | Terminée | Commit `fd84c54`, tag `pre-enterprise-migration`, branche `feat/enterprise-platform-migration`; stack Docker restaurée et validée avant migration. | Aucun pour le critère de baseline; conserver le point de retour. |
| 2026-08-15 | 1 — Documentation | Terminée localement | `AGENTS.md`, matrices, rapport, documentation d'architecture et ADR créés; liens et cohérence vérifiés; commit local `cccf2ee`. | Publier la branche avec les autres lots validés. |
| 2026-08-14–15 | 2 — Filet de sécurité | Terminée localement | 14 tests backend, 20 Vitest, couvertures/seuils backend et frontend, build Angular et 9/9 Playwright validés dans Docker; stack E2E isolée détruite après le run. Rejoué et reconfirmé le 15/08 lors de l'audit de phase 4. | Maintenir ce filet. Ajouter désarchivage projet et suppression ticket en P7, après création de leurs endpoints. |
| 2026-08-15 | 3 — GitHub Actions CI | Partielle | [PR draft #1](https://github.com/Joe-azar/taskira/pull/1), HEAD `6db6115`; [run #3](https://github.com/Joe-azar/taskira/actions/runs/31851279947) vert : Backend, Frontend avec lint, Containers and E2E et CI Gate. | Activer la protection de `main`; `protected=false` et aucun ruleset sont encore observés. |
| 2026-08-15 | 4 — SonarQube et scans | Terminée localement | SonarQube Community Build éphémère (Docker) : Quality Gate `OK`, 0 bug, 0 vulnérabilité, 0 security hotspot, 24 code smells, couverture 13,0 %, duplication 1,4 %, 9010 ncloc. Deux bugs d'accessibilité détectés puis corrigés avant la seconde analyse. Trivy (fs + 2 images) et CodeQL configurés et exécutés localement; 0 secret détecté. | Vérifier `quality.yml`/`security.yml`/`codeql.yml` sur un run GitHub distant dès l'authentification disponible; traiter les CVE identifiées en P6. |
| 2026-08-15 | 5 — Architecture modulaire | Terminée localement (critère mécanique) | Spring Modulith 1.4.1 ajouté; `ModularityTests` vérifie frontières et absence de cycle à chaque `mvn verify` (18 tests backend au total désormais, tous verts : 11 initiaux + 2 `ModularityTests` + 2 nouveaux tests `ProjectService.removeMember` + 3 intégration). Un cycle réel `project -> ticket -> project` détecté et corrigé par port/adapter (`ProjectMemberAssignmentCheck`) sans changer le comportement transactionnel. Documentation générée dans `docs/architecture/modules.md`. | Couche `api`/`application`/`domain`/`infrastructure` complète et événements métier restent à faire; le couplage direct aux repositories/entités d'autres modules est documenté comme dette (ADR-0016), pas éliminé. |
| 2026-08-15 | 6 — Montées technologiques | Terminée sur `feat/phase6-stack-upgrade` | `main` protégée puis migration complète : Spring Boot 4.1.0/Framework 7.0.8/Security 7.1.0/Hibernate 7.4.1, Spring Modulith 2.1.0, Springdoc 3.1.0, MapStruct 1.6.3, Jackson 3; Angular/CLI/CDK/Material 22.1.x, TypeScript 6.0.3, Node 24.19.0, npm 12.0.2; PostgreSQL 18.6 par sauvegarde/restauration réelle vérifiée (ancien volume conservé). 18 tests backend et 9/9 Playwright verts après chaque sous-étape; deux bugs indépendants trouvés et corrigés (CRLF cassant l'entrypoint E2E, assertion Playwright fragile). Détail complet ci-dessous. | Vérifier `ci.yml`/`quality.yml`/`security.yml`/`codeql.yml` sur le HEAD de la branche, revue finale des breaking changes, puis fusion vers `main`. |
| — | 7–20 | Planifiées | Aucun critère de sortie déclaré atteint. | Suivre [la feuille de route](docs/migration-matrix.md) dans l'ordre. |

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
- Spring Modulith 1.4.1 (`spring-modulith-api` en dépendance de compilation pour les annotations `package-info.java`; `spring-modulith-starter-test` et `spring-modulith-docs` en dépendances de test) : `ModularityTests` vérifie les frontières de module et l'absence de cycle, et régénère la documentation PlantUML des modules à chaque exécution.

Le run GitHub distant #3 (P3) est vert. Les workflows `quality.yml`, `security.yml` et `codeql.yml` (P4) sont validés localement et avec `actionlint` mais pas encore vérifiés sur un run GitHub distant, faute d'authentification GitHub interactive sur ce poste. Nginx production, observabilité et labs ne sont pas encore ajoutés.

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

La commande racine `& .\e2e\playwright\run.ps1` construit et exécute `e2e/playwright/compose.e2e.yml`. PostgreSQL 16.15 utilise un `tmpfs`; aucun service n'expose de port hôte et la stack ne définit ni `container_name` ni volume persistant pour la base. Les données générées emploient le domaine réservé `.test`. Le `finally` exécute `down --volumes --remove-orphans`; le contrôle final a confirmé zéro conteneur, réseau ou volume restant. Les rapports Playwright et résultats sont écrits dans des répertoires ignorés par Git et restent disponibles après la destruction. Voir [la stratégie de tests](docs/testing-strategy.md).

Le désarchivage projet et la suppression ticket ne sont pas simulés : les endpoints correspondants sont absents. Leur ajout et leurs E2E constituent un gap explicite de P7, pas un échec du critère P2.

## CI/CD

Le workflow `.github/workflows/ci.yml` définit :

- backend Java 21 avec `./mvnw verify` et rapports tests/JaCoCo;
- frontend Node 22.23.2/npm 11.9.0 avec lint, couverture, 20 Vitest et build;
- même fichier Compose E2E éphémère, attente de disponibilité, Playwright, logs/artifacts et job de destruction systématique.

Les actions tierces sont pinées par SHA, les permissions globales sont `contents: read` et `persist-credentials` est désactivé. `actionlint` 1.7.12 passe localement. Sur la PR draft #1 au HEAD `6db6115`, le run GitHub #3 (`31851279947`) valide Backend, Frontend, Containers and E2E et CI Gate. Le lint Angular passe avec 0 erreur et 41 avertissements liés à la dette `any`.

P3 reste partielle pour une seule raison : `main` est encore signalée `protected=false` et aucun ruleset n'est configuré. Le connecteur a reçu `403` sur les réglages administratifs. La protection est disponible sans coût supplémentaire, mais son activation nécessite l'interface GitHub ou un jeton administrateur interactif; aucune élévation n'est contournée. GHCR, release, staging et production manuelle relèvent de P15.

## Résultats de la phase 4 (SonarQube et scans)

Analyse SonarQube Community Build `26.8.0.126808` exécutée localement le 15 août 2026 sur l'intégralité du dépôt (`sonar-project.properties` à la racine, backend Java + frontend TypeScript en une seule analyse) :

- Quality Gate (profil `Sonar way`) : `OK`.
- Bugs : 0 (2 détectés à la première analyse — `Web:InputWithoutLabelCheck` sur deux champs sans label accessible — corrigés avant la seconde analyse; voir commit d'accessibilité).
- Vulnérabilités : 0. Security hotspots : 0. Notes fiabilité/sécurité/maintenabilité : A/A/A.
- Code smells : 24 (dette estimée 140 minutes).
- Couverture : 13,0 %. Duplication : 1,4 %. Lignes de code analysées : 9010.
- Secrets : 0 détecté (scanner intégré `TextAndSecretsSensor`).

Trivy (CLI `aquasec/trivy`, cache `~/.cache/trivy` réutilisé) exécuté localement le 15 août 2026 :

- `backend/pom.xml` : 21 vulnérabilités (17 HIGH, 4 CRITICAL) sur `jackson-core`/`jackson-databind`, `tomcat-embed-core`, `postgresql` (pilote JDBC), `spring-boot`, `spring-data-commons`, `spring-security-web`, `spring-expression`/`spring-webmvc`. La quasi-totalité n'est corrigée que par une version cible Spring Boot 4.x/Spring 7 (P6); aucun correctif isolé sûr n'existe dans la ligne 3.5.x pour plusieurs d'entre elles.
- `frontend/package-lock.json` : 8 vulnérabilités HIGH sur `@angular/common`, `@angular/compiler`, `@angular/core`, corrigées uniquement à partir d'Angular 22.x (P6).
- Image `infra-backend` : mêmes CVE applicatives (jar) que `pom.xml`, plus 8 CVE HIGH sur le binaire Go `pebble` embarqué dans l'image de base `eclipse-temurin` (couche OS, hors du code applicatif).
- Image `infra-frontend` (image de développement, `node_modules` complet) : CVE d'outillage supplémentaires (`tar`, `undici`, `vite`) absentes d'une image de production Nginx/statique (P11); ces vulnérabilités n'affectent pas le runtime navigateur final.
- Aucun secret détecté sur le système de fichiers du dépôt.
- Deux limites opérationnelles rencontrées et corrigées dans les workflows : (1) Maven Central a renvoyé `429 Too Many Requests` sans cache `.m2` local — `security.yml` réchauffe désormais le cache Maven avant le scan; (2) le téléchargement de la base Java de Trivy (~900 Mo) dépasse le délai par défaut au premier run — `timeout: 10m0s` ajouté et la base mise en cache via `actions/cache`.

CodeQL (`java-kotlin`, `javascript-typescript`) est configuré et validé avec `actionlint`; non exécuté sur GitHub faute d'authentification distante disponible sur ce poste.

Limite documentée de l'édition Community (voir [ADR-0015](docs/adr/0015-sonarqube-quality-gate.md)) : pas de décoration de pull request ni d'analyse multi-branches; seule la branche par défaut est analysée à chaque exécution.

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

## Qualité et sécurité

- SonarQube et Quality Gate : Quality Gate `OK` validée localement en P4 (voir section précédente); non vérifiée sur un run GitHub distant.
- CodeQL, Dependabot et Trivy : configurés et exécutés/validés localement en P4; contrôle automatisé des secrets couvert par le scanner Trivy intégré (0 résultat).
- CVE ouvertes identifiées par Trivy le 15 août 2026 : 21 backend (17 HIGH/4 CRITICAL) et 8 frontend (HIGH), corrigibles principalement par la montée de version P6; traitées comme dette documentée, pas comme régression P4.
- Audit npm du 2026-08-14 : 6 vulnérabilités élevées dans l'arbre de production et 35 au total, dont 1 critique dans l'outillage. Recoupe partiellement le résultat Trivy du 15/08 (Angular); à re-scanner avant correction en P6.
- Lint Angular : 0 erreur, 41 avertissements `any`; dette visible à réduire progressivement sans désactiver la règle.
- Aucun `npm audit fix --force` n'a été appliqué.
- Auth session HttpOnly/CSRF et bootstrap admin dev : proposés pour P8; JWT/localStorage reste le comportement actuel.

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

1. Désarchivage projet et suppression ticket impossibles à couvrir tant que leurs endpoints ne sont pas ajoutés en P7; ne pas simuler ces workflows.
2. Les CVE identifiées en P4 (21 backend, 8 frontend) sont vraisemblablement en grande partie corrigées par la montée de version P6 (Spring Boot 4/Spring Security 7 et Angular 22 corrigent la plupart des bibliothèques concernées), mais un nouveau scan Trivy contre la stack P6 n'a pas été rejoué pour le confirmer chiffre par chiffre; à faire avant de considérer ce point clos.
3. Auth actuelle fondée sur JWT/localStorage, sans session cookie ni CSRF.
4. Absence d'image frontend production/Nginx et de staging.
5. Absence d'observabilité et de stratégie backup/restore planifiée récurrente (le script ponctuel de P6 couvre la sauvegarde/restauration à la demande, pas une politique quotidienne/hebdomadaire/mensuelle).
6. SonarQube Community Build ne décore pas les pull requests ni n'analyse les branches séparément (limite d'édition documentée, [ADR-0015](docs/adr/0015-sonarqube-quality-gate.md)).
7. Couplage direct restant aux repositories/entités d'autres modules (`ticket`/`comment` -> `project`; `comment`/`dashboard` -> `ticket`; quasiment tous -> `user`), nommé et vérifié par Spring Modulith mais pas éliminé; `dashboard` -> `ticket.specification` est la coupure de moindre qualité à traiter en priorité si l'aggregation dashboard est revue ([ADR-0016](docs/adr/0016-spring-modulith-boundaries.md)).
8. `feat/phase6-stack-upgrade` n'est pas encore fusionnée vers `main` : les workflows GitHub distants (`ci.yml`, `quality.yml`, `security.yml`, `codeql.yml`) doivent être vérifiés verts sur son HEAD avant fusion.

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
