# Architecture backend

Statut : organisation actuelle décrite; modularisation détaillée planifiée en phase 5.

## État actuel

Le package racine reste `com.joe.taskira`. Les capacités existantes sont `auth`, `user`, `project`, `ticket`, `comment` et `dashboard`; `security`, `config` et `common` portent les préoccupations transversales.

Chaque capacité regroupe actuellement contrôleurs, services, DTO, entités et repositories. Cette organisation feature-first est conservée, mais aucune règle automatique ne bloque encore les dépendances inter-modules.

## Direction modulaire

```text
com.joe.taskira
├── shared/          configuration, sécurité, erreurs, audit, utilitaires
├── identity/
├── users/
├── projects/
├── tickets/
├── comments/
├── dashboard/
├── notifications/  futur
├── attachments/    futur
├── exports/        futur
└── audit/          futur
```

Chaque module doit exposer un service/application API public étroit. Les autres modules ne doivent pas importer directement ses repositories, entités internes ou adapters.

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

## Évolutions planifiées

- P5 : frontières, événements internes ciblés et Spring Modulith.
- P6 : Spring Boot 4/Spring 7 après non-régression.
- P7 : `/api/v1`, ProblemDetail, profils, transactions et optimistic locking.
- P9 : audit métier et request IDs.

Spring Modulith, MapStruct, Spring Batch et les modules futurs ne sont pas encore déclarés livrés.
