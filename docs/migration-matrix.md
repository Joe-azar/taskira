# Feuille de route de migration enterprise

Dernière mise à jour : 2026-08-16.

Cette feuille de route suit les phases 0 à 20 du référentiel. La [matrice racine](../MIGRATION_MATRIX.md) suit séparément les capacités métier.

Statuts : `Terminée`, `Terminée localement`, `Partielle`, `Planifiée`. Une phase future n'est jamais considérée comme installée parce qu'elle est documentée.

| Phase | Portée | Priorité | Statut | Critère de sortie |
| ---: | --- | --- | --- | --- |
| 0 | Baseline Git | Critique | Terminée | État fonctionnel validé, commit `fd84c54`, tag `pre-enterprise-migration` et branche de migration présents. |
| 1 | Documentation | Critique | Terminée localement | `AGENTS.md`, matrices, rapport, architecture et ADR complets; liens, cohérence et diff vérifiés; commit local `cccf2ee`. |
| 2 | Filet de sécurité | Critique | Terminée localement | 14 backend, 20 Vitest et seuils JaCoCo/V8 sont verts; 9/9 Playwright couvrent auth, autorisation et workflows métier sur une stack isolée détruite après le run. |
| 3 | GitHub Actions CI | Critique | Terminée | `main` est protégée (`protected: true`, vérifié via l'API GitHub) : PR + `CI Gate` obligatoires (`strict: true`), pas de revue humaine requise, force-push et suppression interdits, admin non bloqué. `ci.yml` vert sur GitHub. |
| 4 | SonarQube et scans | Haute | Terminée | Quality Gate SonarQube `OK`, CodeQL et Trivy tous vérifiés verts sur un run GitHub distant réel (PR #28, `quality.yml`/`security.yml`/`codeql.yml`), pas seulement localement. |
| 5 | Architecture modulaire | Haute | Terminée localement (critère mécanique) | Spring Modulith vérifie les frontières et l'absence de cycle à chaque build (`ModularityTests`); un cycle réel `project`/`ticket` a été détecté et corrigé par port/adapter. L'architecture hexagonale légère complète et les événements métier restent à faire. |
| 6 | Montées technologiques | Haute | Terminée et fusionnée | Java 21 conservé; Spring Boot 4.1.0/Spring 7.0.8, Angular 22.1.2, Node 24.19.0, Material 22.1.2 et PostgreSQL 18.6. 18 tests backend, 20 Vitest, 9/9 Playwright; `CI Gate`, Quality Gate SonarQube, CodeQL et Trivy tous vérifiés verts sur GitHub avant fusion (PR #28, commit `a7463af`). |
| 7 | API et robustesse applicative | Haute | Terminée | `/api/v1` versionne les six contrôleurs; profils Spring `dev`/`test`/`prod` avec différences réelles (logs, springdoc désactivé en `prod`, vérifié par test); erreurs migrées vers `ProblemDetail` RFC 7807 (backend et ~25 sites frontend); `AuthService`/`UserService`/`DashboardService` couverts par `@Transactional`; verrouillage optimiste (`@Version`, Flyway `V7`) avec 409 sur conflit, prouvé contre PostgreSQL réel. 22 tests backend, 20 Vitest, 9/9 Playwright verts sur `feat/phase7-api-concurrency`. |
| 8 | Authentification sécurisée | Haute | Terminée localement | Session `HttpOnly`/`SameSite=Lax`/`Secure` (`prod`), CSRF double-soumission Angular/Spring, CORS credentials, logout serveur et bootstrap admin dev idempotent (profil `dev` uniquement) sont testés; JWT et `localStorage`/`sessionStorage` retirés. 39 tests backend, 25 Vitest, 9/9 Playwright sur `feat/phase8-session-security`, pas encore fusionnée. |
| 9 | Audit et journalisation | Haute | Terminée localement | Module `audit`/`audit_events` (Flyway `V8`), `RequestIdFilter`/MDC/`ProblemDetails.of(...)` et logs structurés (JSON en `prod`) sont validés; aucun secret dans `audit_events` ni dans les logs. 29 tests backend rapides, 36 intégration, 10/10 Playwright sur `feat/phase9-audit-logging`, pas encore fusionnée. |
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
