# Architecture d'observabilité

Statut : P9 (corrélation et logs) terminée; P10 (Actuator/Prometheus/Grafana) terminée localement sur `feat/phase10-observability`, pas encore fusionnée dans `main`.

## P9 — Corrélation et logs (terminée)

- `RequestIdFilter` (`common.web`) accepte `X-Request-ID` s'il correspond à un format sûr (`^[a-zA-Z0-9-]{1,64}$` — jamais fait confiance tel quel, une valeur non validée atteindrait directement MDC et les logs), sinon en génère un.
- Placé en tout premier dans la chaîne de filtres (`addFilterBefore(..., DisableEncodeUrlFilter.class)`, le tout premier filtre interne de Spring Security), avant CORS/CSRF/authentification — les échecs à ces étapes sont donc eux aussi corrélés, pas seulement les requêtes qui atteignent un contrôleur.
- Identifiant placé dans MDC (clé `requestId`) et dans la réponse (en-tête `X-Request-Id`), sur toute réponse succès ou erreur; `MDC.remove(...)` dans un bloc `finally`.
- Logs locaux lisibles (`logging.pattern.console` avec `%X{requestId}`, dev/test); logs structurés JSON en production (`logging.structured.format.console: logstash`, fonctionnalité native de Spring Boot 4.1, sans nouvelle dépendance — les entrées MDC y apparaissent automatiquement).
- `ProblemDetails.of(...)` (`common.web`) est le point unique de construction des réponses d'erreur (`GlobalExceptionHandler`, `RestAuthenticationEntryPoint`, `RestAccessDeniedHandler`, trois sites auparavant indépendants) et ajoute `requestId` comme propriété d'extension, alignée sur l'en-tête `X-Request-Id`.
- Aucun mot de passe, cookie, token ou secret : `AuditService.record(...)` n'accepte que des identifiants/enums/chaînes courtes en paramètre, jamais une entité, une exception ou un corps de requête brut — un mot de passe ne peut structurellement pas fuiter dans une ligne de log ou une ligne `audit_events`.

Voir [ADR-0018](adr/0018-audit-request-correlation.md) pour le détail complet (module `audit`, table `audit_events`, décision de conception).

## P10 — Métriques (terminée localement)

```text
Spring Boot Actuator / Micrometer  (port de gestion isolé 9091)
                |
                v
            Prometheus  (scrute backend:9091/actuator/prometheus, 15s)
                |
                v
              Grafana  (datasource + dashboards provisionnés)
```

Actuator est exposé sur un port de gestion séparé (`management.server.port`, défaut `9091`, surchargeable via `MANAGEMENT_SERVER_PORT`), jamais publié à l'hôte dans `infra/docker-compose.yml` — c'est la vraie frontière de sécurité, pas un identifiant applicatif : Prometheus n'a pas de mécanisme d'authentification machine-à-machine simple à recevoir dans une stack Docker Compose. `management.endpoints.web.exposure.include` est une liste explicite (`health,info,prometheus`), pas `*`. `EndpointRequest.toAnyEndpoint().permitAll()` autorise ce port anonymement dans `SecurityConfig` : la séparation de port ne suffit pas seule, la correspondance de Spring Security est fondée sur le chemin et non sur le port, donc une requête sur le port de gestion traverse la même chaîne de filtres que le port applicatif principal et serait bloquée par `anyRequest().authenticated()` sans ce matcher explicite. Un endpoint non exposé (`env`, `beans`, `heapdump`...) n'a simplement aucun mapping — une requête anonyme y reçoit `401`, pas `404`, puisque Spring Security rejette la requête avant que Spring MVC n'ait la moindre chance de router.

Métriques métier (`config.BusinessMetricsBinder`, `MeterBinder`) : `taskira_tickets`/`taskira_projects` (jauges par statut, ré-interrogent leur repository à chaque scrape), `taskira_users_active` (jauge par rôle), `taskira_auth_login_attempts_total` (compteur par résultat `success`/`failure`, incrémenté dans `AuthService` aux mêmes points d'appel que l'audit). Nommage à retenir : Micrometer réserve le suffixe `_total` aux compteurs et le retire silencieusement d'un nom de jauge qui s'y termine déjà — d'où l'absence de `.total` dans le nom Java de ces jauges, pour qu'il corresponde exactement à ce que Prometheus expose réellement.

Prometheus (`v3.13.2`, épinglé par digest) et Grafana (`13.1.3`, épinglé par digest) tournent dans `infra/docker-compose.yml`, publiés à l'hôte sur `9090`/`3000` uniquement pour le confort du développement local (même logique que le port PostgreSQL). Grafana provisionne automatiquement la datasource Prometheus et deux dashboards (`taskira-runtime` : JVM, CPU, débit HTTP, latence p95, connexions HikariCP; `taskira-business` : tickets/projets par statut, utilisateurs actifs par rôle, débit de tentatives de connexion).

Valider health/readiness/liveness, target Prometheus `UP`, datasource Grafana et dashboards versionnés — vérifié réellement contre la stack en cours d'exécution (target `up` via `/api/v1/targets`, dashboards et datasource confirmés via `/api/search`/`/api/datasources`), pas seulement configuré. Mesures couvertes : disponibilité, débit, latence, 4xx/5xx, JVM, CPU, mémoire, HikariCP et métriques métier ci-dessus.

Loki reste un lab optionnel P20 après la stack métriques. Voir [ADR-0012](../adr/0012-observability-stack.md) pour le détail complet, y compris les découvertes réelles sur l'API Spring Boot 4.1 et la convention de nommage Prometheus de Micrometer.
