# Architecture d'observabilité

Statut : proposée; la stack d'observabilité n'est pas installée.

## P9 — Corrélation et logs

- Accepter ou générer `X-Request-ID`.
- Placer l'identifiant dans MDC et éventuellement dans la réponse.
- Logs locaux lisibles; logs structurés staging/production.
- Aucun mot de passe, cookie, token ou secret.

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
