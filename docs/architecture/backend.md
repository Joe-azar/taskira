# Architecture backend

Statut : frontières de module vérifiées mécaniquement par Spring Modulith depuis la phase 5 (critère mécanique) — voir [modules.md](modules.md) et [ADR-0016](../adr/0016-spring-modulith-boundaries.md) pour le graphe de dépendances réel et les interfaces nommées.

## État actuel

Le package racine reste `com.joe.taskira`. Treize modules existent aujourd'hui : `auth`, `user`, `project`, `ticket`, `comment`, `dashboard`, `audit` (P9), `notifications` (P12), `attachments` (P13), `exports` (P14); `security`, `config` et `common` portent les préoccupations transversales (`@ApplicationModule(type = OPEN)`).

Chaque module regroupe contrôleurs, services, DTO, entités et repositories. `ModularityTests` (`backend/src/test/java`) vérifie à chaque `mvn verify` l'absence de cycle et les frontières déclarées via `@NamedInterface` — un module ne peut pas importer arbitrairement les repositories ou entités internes d'un autre module sans que ce sous-package soit explicitement exposé.

## Organisation réelle des modules

```text
com.joe.taskira
├── common/          (OPEN) utilitaires, ProblemDetail, filtres transversaux
├── config/          (OPEN) configuration Spring
├── security/        (OPEN) Spring Security, session, CSRF
├── auth/            login/register/logout
├── user/
├── project/
├── ticket/
├── comment/
├── dashboard/
├── audit/           audit_events, AuditService (P9)
├── notifications/   TicketAssignedEvent/CommentCreatedEvent -> Mailpit (P12)
├── attachments/     DocumentStorage/LocalFileSystemStorage (P13)
└── exports/         Excel/PDF synchrones, export en masse Spring Batch (P14)
```

Chaque module expose une API interne aussi étroite que possible via `@NamedInterface`. Le couplage direct restant à certains repositories/entités d'autres modules est nommé et vérifié (pas éliminé) — dette assumée et documentée dans [ADR-0016](../adr/0016-spring-modulith-boundaries.md), pas un oubli.

Pour une capacité complexe seulement :

```text
api -> application -> domain <- ports <- infrastructure
```

Les ports comme `DocumentStorage`, `NotificationSender` ou `ReportRenderer` ne sont créés qu'avec un cas d'usage concret. Les features simples restent simples.

## Règles Java/Spring

- Java 21, injection par constructeur, aucune injection de champ.
- Contrôleurs limités au contrat HTTP et à la délégation.
- Règles métier et frontières transactionnelles dans la couche application/service.
- SLF4J/Logback; aucun `System.out` ou secret dans les logs.
- Spring MVC reste l'API synchrone; pas de migration générale WebFlux.

## Stack actuelle

Spring Boot 4.1.x, Spring Framework 7.x, Spring Security 7.x, Hibernate 7.x, Spring Modulith, MapStruct, Spring Batch (exports en masse, P14) — voir `backend/pom.xml` pour les versions exactes et `ENTERPRISE_MIGRATION_REPORT.md` pour l'historique des montées de version.
