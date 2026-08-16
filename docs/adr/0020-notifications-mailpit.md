# ADR-0020 — Module notifications avec Mailpit

- Statut : Accepted
- Date : 2026-08-16

## Contexte

Aucune notification n'existe aujourd'hui : un utilisateur assigné à un ticket ou mentionné par un commentaire ne l'apprend qu'en rouvrant l'application. Introduire un envoi d'email nécessite un cas métier réel (pas juste « ajouter Mailpit parce que c'est dans la roadmap ») et un moyen de le prouver sans jamais envoyer un email réel en développement ou en test.

## Décision

### Cas métier retenus

Deux déclencheurs seulement, ceux qui ont un destinataire non ambigu et un effet direct sur ce que la personne doit faire ensuite :

- Un ticket est assigné à quelqu'un (`TicketAssignedEvent`) : le nouvel assigné doit savoir qu'un ticket lui revient.
- Un commentaire est ajouté à un ticket (`CommentCreatedEvent`) : le créateur et l'assigné du ticket (hors l'auteur du commentaire lui-même) doivent savoir qu'une discussion a eu lieu.

Volontairement pas de notification sur chaque champ modifié, chaque changement de statut ou chaque membre de projet ajouté — AGENTS.md §36 est explicite : « Ne pas transformer tous les appels de services en événements. » Ces deux cas couvrent le besoin réel (« je dois agir » / « il y a du nouveau sur mon ticket ») sans multiplier les emails.

### Spring Application Events, pas d'appel direct

`TicketAssignedEvent`/`CommentCreatedEvent` sont définis dans les modules qui les déclenchent (`ticket`, `comment`), exposés via `@NamedInterface`, et publiés avec `ApplicationEventPublisher` depuis `TicketService.updateAssignee(...)`/`CommentService.createComment(...)`. Le nouveau module `notifications` (fermé par défaut) les écoute sans que `ticket`/`comment` n'aient besoin de connaître son existence — c'est exactement le cas d'usage qu'AGENTS.md §36 anticipait déjà sans qu'aucun événement métier n'ait encore été introduit dans le code.

`@TransactionalEventListener(phase = AFTER_COMMIT)` plutôt que `@EventListener` simple : un ticket assigné puis la transaction annulée (erreur ailleurs dans la même requête) ne doit jamais déclencher un email pour une assignation qui n'a in fine jamais eu lieu.

### Best-effort, jamais bloquant

L'envoi d'email ne doit jamais faire échouer l'opération métier qui l'a déclenché. `NotificationService` capture toute exception d'envoi et logue en `WARN`, ne la propage jamais — un Mailpit temporairement indisponible ne doit pas empêcher d'assigner un ticket ou de poster un commentaire. Pas de file d'attente ni de retry pour cette première version : le volume (déclenché uniquement par une action humaine explicite, jamais en masse) ne le justifie pas encore.

### Mailpit

`axllent/mailpit:v1.30.7`, épinglé par digest, vérifié identique au tag flottant `latest` au moment du pull. SMTP sur `1025`, interface web sur `8025`, tous deux uniquement dans `infra/docker-compose.yml` (développement) — jamais dans `infra/docker-compose.prodlike.yml`, qui n'a pas vocation à simuler l'envoi d'email (une vraie production nécessiterait un relais SMTP réel, hors périmètre de P12). C'est directement le cas d'usage visé par la feuille de route : « sans envoi externe en développement ».

### Vérification

Un test unitaire (`JavaMailSender` simulé) ne prouve que la construction du message, pas qu'il atteint réellement un serveur SMTP. Un test d'intégration démarre un vrai conteneur Mailpit via Testcontainers, publie un événement réel à travers un contexte Spring réel, puis interroge l'API HTTP de Mailpit (`GET /api/v1/messages`) pour lire l'email réellement reçu (destinataire, sujet, contenu) — la même discipline de preuve contre une infrastructure réelle que le reste du projet.

## Conséquences

- Deux nouveaux types d'événement (`TicketAssignedEvent`, `CommentCreatedEvent`) sont les premiers événements métier réels du projet — leur usage doit rester justifié par un besoin de notification concret, pas étendu par défaut à chaque futur changement.
- Un relais SMTP réel pour une vraie production reste à décider en P15+; Mailpit ne doit jamais être présenté comme une solution de production.
- Pas de préférences de notification par utilisateur ni de désactivation dans cette première version — si un besoin réel apparaît, une évolution ultérieure documentée en ADR.

## État d'implémentation

Terminée localement sur `feat/phase12-notifications`, pas encore fusionnée dans `main`. Détail complet dans [ENTERPRISE_MIGRATION_REPORT.md](../../ENTERPRISE_MIGRATION_REPORT.md) (section « Résultats de la phase 12 »).

Un bug réel trouvé par la suite de tests complète, pas supposé : ajouter `spring-boot-starter-mail` active automatiquement un indicateur de santé Actuator pour le courrier qui fait passer l'endpoint agrégé `/actuator/health` à `503` dès que le serveur SMTP n'est pas joignable — révélé par un échec inattendu d'`ActuatorSecurityIT` (phase 10), directement contraire à la philosophie best-effort ci-dessus. Corrigé avec `management.health.mail.enabled: false`.

Vérifié réellement, de bout en bout, à deux niveaux : `NotificationWiringIT` (vrai conteneur Mailpit via Testcontainers, vraies requêtes HTTP, email réellement lu depuis l'API Mailpit) et un smoke test manuel contre la vraie stack de développement reconstruite (inscription, projet, ticket, assignation via l'API réelle, email reçu dans la vraie instance Mailpit avec le sujet attendu).
