# Architecture d'observabilité

Statut : P9 (corrélation et logs) terminée; la stack de métriques P10 (Actuator/Prometheus/Grafana) n'est pas installée.

## P9 — Corrélation et logs (terminée)

- `RequestIdFilter` (`common.web`) accepte `X-Request-ID` s'il correspond à un format sûr (`^[a-zA-Z0-9-]{1,64}$` — jamais fait confiance tel quel, une valeur non validée atteindrait directement MDC et les logs), sinon en génère un.
- Placé en tout premier dans la chaîne de filtres (`addFilterBefore(..., DisableEncodeUrlFilter.class)`, le tout premier filtre interne de Spring Security), avant CORS/CSRF/authentification — les échecs à ces étapes sont donc eux aussi corrélés, pas seulement les requêtes qui atteignent un contrôleur.
- Identifiant placé dans MDC (clé `requestId`) et dans la réponse (en-tête `X-Request-Id`), sur toute réponse succès ou erreur; `MDC.remove(...)` dans un bloc `finally`.
- Logs locaux lisibles (`logging.pattern.console` avec `%X{requestId}`, dev/test); logs structurés JSON en production (`logging.structured.format.console: logstash`, fonctionnalité native de Spring Boot 4.1, sans nouvelle dépendance — les entrées MDC y apparaissent automatiquement).
- `ProblemDetails.of(...)` (`common.web`) est le point unique de construction des réponses d'erreur (`GlobalExceptionHandler`, `RestAuthenticationEntryPoint`, `RestAccessDeniedHandler`, trois sites auparavant indépendants) et ajoute `requestId` comme propriété d'extension, alignée sur l'en-tête `X-Request-Id`.
- Aucun mot de passe, cookie, token ou secret : `AuditService.record(...)` n'accepte que des identifiants/enums/chaînes courtes en paramètre, jamais une entité, une exception ou un corps de requête brut — un mot de passe ne peut structurellement pas fuiter dans une ligne de log ou une ligne `audit_events`.

Voir [ADR-0018](adr/0018-audit-request-correlation.md) pour le détail complet (module `audit`, table `audit_events`, décision de conception).

## P10 — Métriques

```text
Spring Boot Actuator / Micrometer
                |
                v
            Prometheus
                |
                v
              Grafana
```

Valider health/readiness/liveness, target Prometheus `UP`, datasource Grafana et dashboards versionnés. Mesures prioritaires : disponibilité, débit, latence, 4xx/5xx, JVM, CPU, mémoire, HikariCP et quelques métriques métier.

Loki reste un lab optionnel P20 après la stack métriques. Voir [ADR-0012](../adr/0012-observability-stack.md).
