# ADR-0016 — Spring Modulith pour la vérification des frontières de modules

- Statut : Accepted
- Date : 2026-08-15

## Contexte

Taskira est déjà organisé par fonctionnalité (`auth`, `comment`, `dashboard`, `project`, `ticket`, `user`, plus `common`/`config`/`security` transversaux). ADR-0001 acte le monolithe modulaire, mais rien ne vérifiait mécaniquement le respect des frontières avant la phase 5.

Une première exécution de `ApplicationModules.of(TaskiraApiApplication.class).verify()`, avec la configuration par défaut de Spring Modulith, échoue massivement : par défaut, seuls les types placés directement dans le package racine d'un module sont considérés comme son API; tout ce qui est dans un sous-package (`entity/`, `repository/`, `dto/`, `enums/`) est considéré interne. Or Taskira place systématiquement son code dans ces sous-packages, donc rien n'était exposé par défaut.

L'audit du code a confirmé un couplage réel et déjà existant :

- `ticket` et `comment` injectent directement `ProjectRepository`/`ProjectMemberRepository` et lisent `Project`/`ProjectMember`.
- `comment` et `dashboard` injectent directement `TicketRepository` et lisent `Ticket`.
- `auth`, `project`, `ticket`, `comment` injectent directement `UserRepository` et lisent `User`.
- `TicketService` modifie `Project.ticketSequence` dans la même transaction que la création du ticket (compteur de séquence protégé par verrou pessimiste `findByIdForUpdate`).
- Les enums (`ProjectStatus`, `ProjectRole`, `TicketStatus`, `TicketPriority`, `TicketType`, `GlobalRole`) et certains DTO de réponse (`UserSummaryResponse`, `TicketSummaryResponse`) sont réutilisés tels quels par d'autres modules.

## Décision

1. Ajouter Spring Modulith (`spring-modulith-bom` 1.4.1, compatible Spring Boot 3.5.x) en dépendance de test uniquement pour l'instant : `spring-modulith-starter-test` (vérification) et `spring-modulith-docs` (documentation PlantUML générée dans `target/spring-modulith-docs`).
2. `common`, `config` et `security` sont déclarés modules `OPEN` (`@ApplicationModule(type = Type.OPEN)`) : ce sont un socle transversal partagé, pas des contextes métier, conformément à la règle « garder `common`/`shared` strictement transversal » d'AGENTS.md.
3. Pour les modules métier, exposer explicitement via `@NamedInterface` sur `package-info.java` uniquement les sous-packages réellement consommés ailleurs aujourd'hui : `project.entity`, `project.repository`, `project.enums`, `ticket.entity`, `ticket.repository`, `ticket.enums`, `ticket.dto`, `user.entity`, `user.repository`, `user.enums`, `user.dto`. Rien d'autre n'est ouvert : `comment`, `dashboard` et `auth` ne sont consommés par personne et restent fermés; les couches `service`/`controller`/`specification` de chaque module restent internes partout.
4. `ModularityTests` (`backend/src/test/java/com/joe/taskira/ModularityTests.java`) fait tourner `verify()` à chaque build et échoue si une NOUVELLE dépendance non déclarée ou un cycle apparaît, plus un test qui régénère la documentation des modules.

Ce choix documente honnêtement le couplage existant au lieu de le masquer, tout en bloquant toute dégradation future. Ce n'est **pas** une déclaration que Taskira a atteint une architecture hexagonale complète avec API applicatives étroites : c'est explicitement acté comme non fait.

## Conséquences

- `mvn verify` échoue désormais si un module accède à un type non exposé d'un autre module ou si un cycle de modules apparaît; c'est un filet de régression, pas une réécriture.
- Le couplage direct aux repositories/entités d'autres modules reste en place et est nommé, pas éliminé. Le remplacer par de vraies façades applicatives (ex. `ProjectModuleApi.reserveNextTicketSequence(projectId)` au lieu de `TicketService` mutant directement `Project.ticketSequence` sous verrou pessimiste) est un refactor à part entière, plus risqué, qui n'est pas tenté dans ce lot pour ne pas mélanger gouvernance d'architecture et changement de comportement transactionnel — conformément à la règle AGENTS.md « ne pas mélanger mise à niveau, refactorisation structurelle et évolution métier dans un même lot ».
- Les futurs modules (`notifications`, `attachments`, `exports`, `audit`) devront démarrer avec des frontières fermées par défaut et n'exposer que ce qui est réellement nécessaire, plutôt que de reproduire le couplage actuel.
- `spring-modulith-events-api`/`spring-modulith-starter-jpa` (registre d'événements persistant) ne sont pas ajoutés dans ce lot : aucun événement métier (`TicketCreatedEvent`, etc.) n'est introduit tant qu'un cas d'usage concret ne le justifie, conformément à la section 13 du plan de migration.

## Alternatives rejetées

- **Restreindre `allowedDependencies` immédiatement pour forcer l'isolation complète** : rejeté maintenant, provoquerait soit un refactor massif et risqué en un seul lot, soit des suppressions de test/contournements interdits par AGENTS.md.
- **Ignorer Spring Modulith et documenter les frontières uniquement en Markdown** : rejeté, un README ne détecte pas une régression; l'objectif de la phase 5 est une vérification mécanique.
