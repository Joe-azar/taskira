# Index des décisions d'architecture

Un statut `Accepted` signifie que la décision guide le projet; il ne signifie pas que toutes ses étapes sont déjà implémentées. `Proposed` désigne une décision à confirmer pendant sa phase avant modification du runtime.

| ADR | Décision | Statut | Implémentation |
| --- | --- | --- | --- |
| [0001](0001-modular-monolith.md) | Monolithe modulaire | Accepted | Organisation progressive P5 |
| [0002](0002-java-21.md) | Java 21 LTS | Accepted | Déjà utilisé |
| [0003](0003-angular-22.md) | Migration Angular 22 | Accepted | Terminée P6 |
| [0004](0004-postgresql.md) | PostgreSQL | Accepted | PostgreSQL 18 depuis P6 |
| [0005](0005-flyway-only.md) | Flyway seul pour le schéma | Accepted | Déjà appliqué |
| [0006](0006-session-cookie-auth.md) | Session cookie sécurisée | Accepted | Terminée et fusionnée P8 (PR #31) |
| [0007](0007-github-actions.md) | GitHub Actions | Accepted | Run distant vert; protection `main` activée |
| [0008](0008-docker-compose.md) | Docker Compose runtime principal | Accepted | Dev actuel; profils futurs |
| [0009](0009-local-filesystem-first.md) | Filesystem avant stockage objet | Accepted | Terminée et fusionnée P13 (PR #36) |
| [0010](0010-no-kafka-rabbitmq-main-runtime.md) | Pas de Kafka/RabbitMQ principal | Accepted | Contrainte active |
| [0011](0011-kubernetes-training-only.md) | Kubernetes réservé au lab | Accepted | Lab P17 |
| [0012](0012-observability-stack.md) | Prometheus/Grafana | Accepted | Terminée et fusionnée P10 (PR #33) |
| [0013](0013-versioning-strategy.md) | Semantic Versioning et images immuables | Accepted | Terminée et fusionnée P15 (PR #46) |
| [0014](0014-incremental-test-first-migration.md) | Migration incrémentale test-first | Accepted | Appliquée depuis P2 |
| [0015](0015-sonarqube-quality-gate.md) | SonarQube Community Build local et éphémère | Accepted | Validée localement P4 |
| [0016](0016-spring-modulith-boundaries.md) | Spring Modulith pour la vérification des frontières | Accepted | Validée localement P5; cycle project/ticket détecté et corrigé |
| [0017](0017-postgresql-18-migration.md) | Migration PostgreSQL 16 vers 18 | Accepted | Validée localement P6 : Flyway, restauration et Hibernate testés avant bascule |
| [0018](0018-audit-request-correlation.md) | Module `audit`, `audit_events` et corrélation par request id | Accepted | Terminée et fusionnée P9 (PR #32) |
| [0019](0019-production-runtime.md) | Runtime production-like Nginx/non-root | Accepted | Terminée et fusionnée P11 (PR #34) |
| [0020](0020-notifications-mailpit.md) | Module notifications avec Mailpit | Accepted | Terminée et fusionnée P12 (PR #35) |
| [0021](0021-attachments-storage.md) | Module attachments : port `DocumentStorage`, filesystem, Tika | Accepted | Terminée et fusionnée P13 (PR #36) |
| [0022](0022-exports-batch.md) | Module exports : POI, OpenHTMLtoPDF, PDFBox, ZXing, Spring Batch | Accepted | Terminée et fusionnée P14 (PR #37) |

Toute nouvelle décision structurante ou modification substantielle d'un ADR accepté exige un nouvel ADR ou une supersession explicite.
