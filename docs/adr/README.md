# Index des décisions d'architecture

Un statut `Accepted` signifie que la décision guide le projet; il ne signifie pas que toutes ses étapes sont déjà implémentées. `Proposed` désigne une décision à confirmer pendant sa phase avant modification du runtime.

| ADR | Décision | Statut | Implémentation |
| --- | --- | --- | --- |
| [0001](0001-modular-monolith.md) | Monolithe modulaire | Accepted | Organisation progressive P5 |
| [0002](0002-java-21.md) | Java 21 LTS | Accepted | Déjà utilisé |
| [0003](0003-angular-22.md) | Migration Angular 22 | Proposed | P6 |
| [0004](0004-postgresql.md) | PostgreSQL | Accepted | PostgreSQL 16; upgrade P6 |
| [0005](0005-flyway-only.md) | Flyway seul pour le schéma | Accepted | Déjà appliqué |
| [0006](0006-session-cookie-auth.md) | Session cookie sécurisée | Proposed | P8 |
| [0007](0007-github-actions.md) | GitHub Actions | Proposed | P3 partielle localement |
| [0008](0008-docker-compose.md) | Docker Compose runtime principal | Accepted | Dev actuel; profils futurs |
| [0009](0009-local-filesystem-first.md) | Filesystem avant stockage objet | Proposed | P13 |
| [0010](0010-no-kafka-rabbitmq-main-runtime.md) | Pas de Kafka/RabbitMQ principal | Accepted | Contrainte active |
| [0011](0011-kubernetes-training-only.md) | Kubernetes réservé au lab | Accepted | Lab P17 |
| [0012](0012-observability-stack.md) | Prometheus/Grafana | Proposed | P10 |
| [0013](0013-versioning-strategy.md) | Semantic Versioning | Proposed | P15 |
| [0014](0014-incremental-test-first-migration.md) | Migration incrémentale test-first | Accepted | Appliquée depuis P2 |

Toute nouvelle décision structurante ou modification substantielle d'un ADR accepté exige un nouvel ADR ou une supersession explicite.
