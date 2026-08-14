# Feuille de route de migration enterprise

Dernière mise à jour : 2026-08-15.

Cette feuille de route suit les phases 0 à 20 du référentiel. La [matrice racine](../MIGRATION_MATRIX.md) suit séparément les capacités métier.

Statuts : `Terminée`, `Terminée localement`, `Partielle`, `Planifiée`. Une phase future n'est jamais considérée comme installée parce qu'elle est documentée.

| Phase | Portée | Priorité | Statut | Critère de sortie |
| ---: | --- | --- | --- | --- |
| 0 | Baseline Git | Critique | Terminée | État fonctionnel validé, commit `fd84c54`, tag `pre-enterprise-migration` et branche de migration présents. |
| 1 | Documentation | Critique | Terminée localement | `AGENTS.md`, matrices, rapport, architecture et ADR complets; liens, cohérence et diff vérifiés; commit local `cccf2ee`. |
| 2 | Filet de sécurité | Critique | Terminée localement | 14 backend, 20 Vitest et seuils JaCoCo/V8 sont verts; 9/9 Playwright couvrent auth, autorisation et workflows métier sur une stack isolée détruite après le run. |
| 3 | GitHub Actions CI | Critique | Partielle | `ci.yml` et `actionlint` sont validés localement; jobs tests/coverage/build et E2E isolé sont définis. Run distant, lint et checks requis manquent. |
| 4 | SonarQube et scans | Haute | Planifiée | Quality Gate, couverture, CodeQL, dépendances, secrets et images sont analysés automatiquement et les résultats triés. |
| 5 | Architecture modulaire | Haute | Planifiée | Frontières feature-first publiques, architecture hexagonale légère et tests Spring Modulith sans cycles. |
| 6 | Montées technologiques | Haute | Planifiée | Java 21 conservé; Spring Boot 4/Spring 7, Angular 22, Node 24, Material et PostgreSQL 17/18 passent la non-régression. |
| 7 | API et robustesse applicative | Haute | Planifiée | `/api/v1`, profils Spring, ProblemDetail, transactions applicatives et optimistic locking sont documentés et testés. |
| 8 | Authentification sécurisée | Haute | Planifiée | Session HttpOnly/SameSite/Secure, CSRF Angular/Spring, CORS credentials, logout et bootstrap admin dev idempotent sont testés. |
| 9 | Audit et journalisation | Haute | Planifiée | Module audit/Flyway, request ID, MDC et logs structurés sans secrets sont validés. |
| 10 | Observabilité | Haute | Planifiée | Actuator/Micrometer, Prometheus target UP, Grafana provisionné et métriques techniques/métier vérifiées. |
| 11 | Runtime production-like | Haute | Planifiée | Nginx sert Angular compilé et proxifie l'API; images durcies et Compose production-like passent les smoke tests. |
| 12 | Notifications | Moyenne | Planifiée | Module notifications et Mailpit fournissent un cas métier réel sans envoi externe en développement. |
| 13 | Pièces jointes | Moyenne | Planifiée | Port `DocumentStorage`, filesystem, métadonnées, Tika, SHA-256 et sécurité upload sont testés; ClamAV/MinIO restent optionnels. |
| 14 | Exports | Moyenne | Planifiée | Export Excel/PDF/QR et job Spring Batch répondent à des cas réels avec tests de fichiers et états de job. |
| 15 | Registry, release et staging | Haute | Planifiée | Images versionnées GHCR, release par tag, staging, Playwright post-déploiement et rollback documenté fonctionnent. |
| 16 | Sauvegarde et restauration | Haute | Planifiée | `pg_dump` automatisé et restauration réellement testée sur une base temporaire. |
| 17 | Kubernetes Lab | Moyenne | Planifiée | kind/k3d déploie manifests, Ingress, probes, ConfigMaps, Secrets et PVC; scaling/update/rollback démontrés. |
| 18 | Helm Lab | Moyenne | Planifiée | Chart créé après les manifests bruts; `helm lint`, `helm template` et déploiement local réussissent. |
| 19 | Azure Lab | Moyenne | Planifiée | Architecture/scripts validés sans dépense implicite; déploiement réel uniquement avec authentification et accord adaptés. |
| 20 | Technologies conditionnelles | Conditionnelle | Planifiée | OAuth2/OIDC, Entra ID, Loki, Redis, RabbitMQ, Kafka, recherche et Terraform restent des labs sauf besoin et ADR. |

La sortie de chaque phase exige documentation à jour, tests applicables verts, absence de secret versionné et mise à jour du [rapport cumulatif](../ENTERPRISE_MIGRATION_REPORT.md).
