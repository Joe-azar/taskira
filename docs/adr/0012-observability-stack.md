# ADR-0012 — Utiliser Actuator, Prometheus et Grafana

- Statut : Proposed
- Date : 2026-08-15

## Contexte

La baseline ne fournit ni endpoint métriques ni collecte/dashboard versionné.

## Proposition

En P10, exposer de façon sécurisée health/info/prometheus via Actuator/Micrometer, collecter avec Prometheus et provisionner Grafana. Mesurer disponibilité, débit, latence, erreurs, JVM, CPU, mémoire, HikariCP et quelques métriques métier.

Les request IDs et logs structurés sont préparés en P9. Loki reste optionnel en P20.

## Critères d'acceptation

- Health/readiness/liveness pertinents.
- Target Prometheus `UP`.
- Datasource et dashboards Grafana versionnés et vérifiés.
- Aucun endpoint sensible exposé publiquement.

## État d'implémentation

Non implémenté.
