# ADR-0018 — Module audit, table audit_events et corrélation par request id

- Statut : Accepted
- Date : 2026-08-16

## Contexte

Avant la phase 9, deux mécanismes existaient déjà, tous deux insuffisants pour un journal d'audit générique : `AuditableEntity` (`created_at`/`updated_at`/`version` via JPA auditing, sans capture d'acteur) et `ticket_history` (journal de changement de champs propre à un ticket, FK cascade à sa suppression, autorisation par appartenance au projet). Aucun des deux ne peut représenter un événement générique (login, changement de rôle, archivage de projet) ni survivre à la suppression de son acteur. Aucune corrélation request id/MDC n'existait non plus.

## Décision

### Module `audit` et table `audit_events`

Nouveau module Spring Modulith fermé par défaut `com.joe.taskira.audit`, ne dépendant que de `common`/`security` (`OPEN`) — jamais d'un autre module métier, donc aucun risque de cycle. Seuls `AuditService` (classe, `@NamedInterface`) et le package `enums` sont exposés, à l'identique du seul autre seam délibéré du projet (`TicketHistoryService`, exposé à `comment`).

Table `audit_events` (Flyway `V8`) : `actor_id` en FK `ON DELETE SET NULL` (pas `RESTRICT` comme `ticket_history.changed_by`) avec `actor_email` dénormalisé — un journal d'audit doit survivre à la suppression de son acteur, contrairement à un journal produit. `entity_type`/`entity_id` polymorphes sans FK. `occurred_at` posé une seule fois en Java (`@PrePersist`), pas de `DEFAULT` SQL redondant (contrairement à `ticket_history`, qui a les deux).

`AuditService.record(actorId, actorEmail, entityType, entityId, action, detail)` prend l'acteur en paramètre explicite plutôt que de le résoudre depuis `SecurityContextHolder` — seul moyen de couvrir correctement un login échoué (aucun principal authentifié) ou une déconnexion (le contexte de sécurité peut déjà être vidé au moment où `LogoutSuccessHandler` s'exécute). `record(...)` s'exécute dans `TxType.REQUIRES_NEW`, pas la transaction de l'appelant : un login échoué doit écrire son événement `LOGIN_FAILURE` malgré le rollback de l'authentification qu'il décrit — un bug réel trouvé pendant l'implémentation, où l'événement d'échec disparaissait silencieusement avec la transaction qui échouait.

Déclencheurs : `AuthService` (`LOGIN_SUCCESS`/`LOGIN_FAILURE`), `SecurityConfig` (`LOGOUT`, lu depuis le paramètre `Authentication` du handler, pas `SecurityContextHolder`), `TicketService` (`TICKET_CREATED`, `TICKET_STATUS_CHANGED` seulement — pas les autres champs, déjà couverts par `ticket_history`), `ProjectService` (`PROJECT_CREATED`, `PROJECT_ARCHIVED`, `PROJECT_MEMBER_ADDED`/`REMOVED`), `UserService` (`USER_CREATED`; `USER_ROLE_CHANGED`, `USER_ACTIVATED`/`DEACTIVATED` uniquement si la valeur change réellement). Aucun déclencheur sur les commentaires : `ticket_history` les couvre déjà, et dupliquer leur contenu dans `audit_events.detail` répéterait le risque de contenu utilisateur brut déjà présent dans `ticket_history`.

`GET /api/v1/audit/events` (`hasRole('ADMIN')`, paginé comme `TicketService.searchTicketsPage`) est le seul point de lecture; visibilité admin uniquement, contrairement à `ticket_history` qui est visible à tout membre du projet.

### `audit_events` complète `ticket_history`, ne le remplace pas

Formes structurellement différentes pour des besoins différents : `ticket_history` est orienté produit (diff de champ, un ticket, membres du projet), `audit_events` est orienté sécurité/conformité (générique, corrélé par `request_id`, admin seulement). `MIGRATION_MATRIX.md` demandait explicitement de « créer le module ET `audit_events` » — additif, pas un remplacement.

### Request id : filtre maison, pas Micrometer Observation

`pom.xml` ne déclare (ni ne reçoit transitivement) `spring-boot-starter-actuator`/`micrometer-tracing`/`micrometer-observation`. [ADR-0012](0012-observability-stack.md) reporte délibérément Actuator/Micrometer à P10. `RequestIdFilter` (`common.web`) est donc un filtre `OncePerRequestFilter` fait main, à l'identique du style de `CsrfCookieFilter` (P8) : accepte `X-Request-Id` s'il correspond à `^[a-zA-Z0-9-]{1,64}$` (jamais fait confiance tel quel — une valeur non validée atteindrait MDC et les logs, vecteur d'injection), sinon en génère un. Placé en premier dans la chaîne (`addFilterBefore(..., DisableEncodeUrlFilter.class)`, le tout premier filtre interne de Spring Security), pour que CORS/CSRF/échecs d'authentification soient eux aussi corrélés.

`ProblemDetails.of(...)` (`common.web`) devient le point unique de construction des réponses d'erreur — remplace trois constructions indépendantes et dupliquées (`GlobalExceptionHandler`, `RestAuthenticationEntryPoint`, `RestAccessDeniedHandler`, ces deux derniers s'exécutant dans la chaîne de sécurité, avant `DispatcherServlet`, donc invisibles à `GlobalExceptionHandler`) — et ajoute `requestId` comme propriété d'extension à partir de MDC.

### Logs structurés

`logging.pattern.console` (dev/test) inclut `%X{requestId}`. `application-prod.yaml` bascule sur `logging.structured.format.console: logstash`, fonctionnalité native de Spring Boot 4.1 — aucune dépendance ajoutée (pas de `logstash-logback-encoder`), les entrées MDC apparaissent automatiquement dans le JSON.

## Critères d'acceptation

- `audit_events` créée par Flyway `V8`, validée contre PostgreSQL réel (Testcontainers).
- Connexion réussie/échouée et déconnexion tracées, sans qu'aucun mot de passe n'atteigne jamais `AuditService` ni les logs.
- Création/changement de statut de ticket, création/archivage de projet, ajout/retrait de membre, changement de rôle/statut utilisateur tracés.
- `X-Request-Id` présent sur toute réponse (succès et erreur), généré ou repris du client selon un format validé; `requestId` du corps `ProblemDetail` correspond à l'en-tête.
- `GET /api/v1/audit/events` : 200 pour un ADMIN, 403 pour un `USER`.

## État d'implémentation

Implémenté et vérifié en phase 9, sur `feat/phase9-audit-logging`. Voir la section phase 9 du rapport cumulatif pour le détail des incréments et des tests.
