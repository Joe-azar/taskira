# ADR-0012 — Utiliser Actuator, Prometheus et Grafana

- Statut : Accepted
- Date : 2026-08-15

## Contexte

La baseline ne fournit ni endpoint métriques ni collecte/dashboard versionné.

## Décision

En P10, exposer de façon sécurisée health/info/prometheus via Actuator/Micrometer, collecter avec Prometheus et provisionner Grafana. Mesurer disponibilité, débit, latence, erreurs, JVM, CPU, mémoire, HikariCP et quelques métriques métier.

Les request IDs et logs structurés sont préparés en P9 (terminé — voir [ADR-0018](0018-audit-request-correlation.md)). Loki reste optionnel en P20.

Isolation retenue : un port de gestion séparé (`management.server.port`, jamais publié à l'hôte dans `infra/docker-compose.yml`) plutôt qu'un identifiant/mot de passe applicatif — Prometheus n'a pas de mécanisme d'authentification machine-à-machine simple à lui fournir dans une stack Docker Compose (pas de Kubernetes, voir [ADR-0011](0011-kubernetes-training-only.md)), et un port non publié est une frontière plus difficile à contourner par erreur qu'un identifiant qui finirait committé quelque part. La séparation de port ne suffit toutefois pas seule : la correspondance de Spring Security est fondée sur le chemin, pas sur le port, donc `EndpointRequest.toAnyEndpoint().permitAll()` reste nécessaire dans `SecurityConfig` pour que les requêtes sur le port de gestion ne soient pas elles aussi bloquées par `anyRequest().authenticated()`.

## Critères d'acceptation

- Health/readiness/liveness pertinents.
- Target Prometheus `UP`.
- Datasource et dashboards Grafana versionnés et vérifiés.
- Aucun endpoint sensible exposé publiquement.

## État d'implémentation

Terminée localement sur `feat/phase10-observability`, pas encore fusionnée dans `main`. Détail complet dans [ENTERPRISE_MIGRATION_REPORT.md](../../ENTERPRISE_MIGRATION_REPORT.md) (section « Résultats de la phase 10 »).

Vérifié réellement, pas seulement configuré : `docker exec taskira-backend wget -qO- http://localhost:9091/actuator/health` répond `UP` depuis l'intérieur du conteneur alors que le même port refuse la connexion depuis l'hôte; Prometheus (`v3.13.2`, épinglé par digest) affiche la cible `taskira-backend` à l'état `up` via `/api/v1/targets`; Grafana (`13.1.3`, épinglé par digest) provisionne la datasource Prometheus et les deux dashboards (`taskira-runtime`, `taskira-business`), confirmés via `/api/datasources` et `/api/search`. `management.endpoints.web.exposure.include` est une liste explicite (`health,info,prometheus`) — `env`/`beans`/`heapdump`/`configprops`/`mappings`/`threaddump`/`shutdown` restent non mappés même sur le port isolé.

Deux découvertes réelles sur l'API Spring Boot 4.1, confirmées par inspection directe du jar résolu (`jar tf`) après que les hypothèses initiales se sont révélées fausses : `ManagementWebSecurityAutoConfiguration` (mécanisme classique Boot 2.x/3.x de mot de passe généré pour le port de gestion) n'existe plus du tout dans `spring-boot-actuator-autoconfigure-4.1.0.jar`; `EndpointRequest` a déménagé vers un nouveau module Maven dédié (`spring-boot-security`, package `org.springframework.boot.security.autoconfigure.actuate.web.servlet`), déjà résolu transitivement via `spring-boot-starter-security`.

Une découverte Micrometer réelle, trouvée uniquement par vérification manuelle contre la vraie stack en cours d'exécution (aucun test automatisé ne l'aurait révélée à l'époque) : `PrometheusNamingConvention` réserve le suffixe `_total` aux compteurs et le retire silencieusement d'un nom de jauge qui s'y termine déjà — les jauges nommées `taskira.tickets.total`/`taskira.projects.total` s'exposaient donc réellement comme `taskira_tickets`/`taskira_projects`, pas `..._total`. Corrigé en renommant côté Java pour faire correspondre le nom réellement exposé; un nouveau test d'intégration (`ActuatorSecurityIT`) vérifie désormais les noms exacts scrapés pour empêcher toute régression silencieuse de ce type.
