# Règles de travail Taskira

Ce fichier définit les règles permanentes applicables à l'ensemble du dépôt Taskira.

Il constitue la référence principale pour les agents de développement tels que Codex, Claude Code ou tout autre assistant disposant d'un accès au repository.

Des consignes placées dans un sous-répertoire peuvent préciser ces règles pour une partie du projet, mais ne doivent jamais affaiblir les exigences de sécurité, de qualité, de migration, de tests ou de protection Git définies ici.

---

# 1. Source de vérité et environnement canonique

Le repository officiel est :

```text
https://github.com/Joe-azar/taskira.git
```

Le repository local canonique sous Windows est :

```text
D:\ALL DATA\France\Taskira
```

Cet emplacement est la source locale de référence.

Un ancien repository de récupération a existé sous :

```text
C:\Users\joeaz\Taskira-enterprise-recovery
```

Ce chemin est obsolète.

Ne jamais :

* recréer volontairement le projet dans cet ancien emplacement ;
* configurer un bind mount Docker vers cet ancien emplacement ;
* restaurer un script ou une configuration contenant ce chemin ;
* considérer cet ancien repository comme source de vérité.

Lorsqu'un chemin absolu est réellement nécessaire, vérifier qu'il correspond au repository actuel sur `D:`.

Éviter autant que possible les chemins absolus dans les fichiers versionnés.

---

# 2. Principe général du projet

Taskira est une plateforme web de gestion :

* d'utilisateurs ;
* de projets ;
* de membres de projets ;
* de tickets ;
* de tâches ;
* d'anomalies ;
* d'affectations ;
* de statuts ;
* de priorités ;
* de commentaires ;
* d'historique ;
* de tableaux de bord ;
* de Kanban.

L'objectif technique est de maintenir un environnement proche d'une application Java moderne utilisée en entreprise.

Architecture cible :

```text
Modular Monolith
+
Feature-Based Architecture
+
Lightweight Hexagonal Architecture
```

Runtime principal :

```text
Angular
Nginx
Spring Boot
PostgreSQL
```

Infrastructure et qualité principales :

```text
Docker
Docker Compose
GitHub Actions
SonarQube
CodeQL
Trivy
Prometheus
Grafana
```

Tests principaux :

```text
JUnit 5
Mockito
AssertJ
MockMvc
Testcontainers
Vitest
Playwright
```

Taskira reste un **monolithe modulaire**.

Ne pas transformer Taskira en microservices sans besoin réel, mesure, justification et ADR explicite.

---

# 3. Documents de référence

Avant toute modification substantielle, consulter les documents nécessaires parmi :

```text
AGENTS.md
ENTERPRISE_MIGRATION_REPORT.md
MIGRATION_MATRIX.md
docs/migration-matrix.md
docs/testing-strategy.md
docs/architecture.md
docs/architecture/
docs/adr/
```

Rôle de chaque document :

* `AGENTS.md`

  * règles permanentes de développement ;
  * contraintes d'architecture ;
  * contraintes Git ;
  * contraintes de sécurité ;
  * stratégie générale de validation.

* `ENTERPRISE_MIGRATION_REPORT.md`

  * état vérifié de la migration ;
  * versions réellement installées ;
  * résultats de tests ;
  * problèmes rencontrés ;
  * validations effectuées ;
  * dette restante.

* `MIGRATION_MATRIX.md`

  * état des modules et capacités principales.

* `docs/migration-matrix.md`

  * feuille de route des phases restantes.

* `docs/testing-strategy.md`

  * stratégie détaillée de tests et commandes de validation.

* `docs/architecture.md` et `docs/architecture/`

  * architecture actuelle et cible.

* `docs/adr/`

  * décisions structurantes et difficilement réversibles.

Le repository et ces documents constituent ensemble la source de vérité.

Ne jamais se fier uniquement à une ancienne conversation, à une ancienne consigne ou à une hypothèse si le repository actuel permet de vérifier l'information.

---

# 4. Règles de reprise du travail

Avant toute intervention substantielle :

```bash
git status
git branch --show-current
git remote -v
git log -10 --oneline
```

Inspecter ensuite les fichiers concernés avant de modifier quoi que ce soit.

Toujours déterminer :

1. la branche actuelle ;
2. l'état du worktree ;
3. la dernière phase réellement terminée ;
4. les changements non commités existants ;
5. les tests applicables ;
6. l'éventuelle présence de travaux utilisateur ou d'un autre agent.

Ne jamais recommencer une phase déjà terminée uniquement parce qu'elle apparaît dans une ancienne consigne.

Continuer depuis le premier travail réellement incomplet.

Lorsque les documents et le code divergent :

1. inspecter le code ;
2. inspecter Git ;
3. inspecter les tests ;
4. inspecter les workflows GitHub si nécessaire ;
5. déterminer l'état réel ;
6. corriger ensuite la documentation devenue obsolète.

Ne jamais présenter une fonctionnalité, une technologie ou une validation comme terminée sans preuve dans le repository, les tests ou les workflows correspondants.

---

# 5. État de référence et ordre des phases

## Phase 0 — Baseline Git

Terminée.

Baseline récupérable au commit :

```text
fd84c54
```

Tag de sécurité :

```text
pre-enterprise-migration
```

Branche historique de migration :

```text
feat/enterprise-platform-migration
```

---

## Phase 1 — Documentation et architecture

Terminée.

Commit historique principal :

```text
cccf2ee
```

Livrables notamment présents :

* règles de travail ;
* matrices ;
* documentation d'architecture ;
* ADR ;
* rapport cumulatif de migration.

---

## Phase 2 — Filet de sécurité

Terminée.

Ont notamment été introduits et validés :

```text
JUnit 5
Mockito
AssertJ
MockMvc
Testcontainers PostgreSQL
JaCoCo
Vitest
Playwright
Angular ESLint
```

La suite E2E utilise une stack Docker dédiée et éphémère et détruit ses ressources en fin d'exécution.

Les nombres de tests indiqués dans les différentes phases sont des **résultats historiques au moment où la phase a été validée**.

Ils ne constituent pas un nombre fixe attendu pour les futures exécutions.

Toujours déterminer le nombre actuel de tests à partir du dernier run réel.

---

## Phase 3 — GitHub Actions et protection de `main`

Terminée.

`main` est protégée depuis le 15 août 2026.

Configuration vérifiée :

```text
Pull Request obligatoire
required_approving_review_count = 0
CI Gate obligatoire
strict = true
force-push interdit
suppression de main interdite
enforce_admins = false
```

Le projet étant actuellement développé principalement par une seule personne, aucune revue humaine obligatoire n'est imposée.

Les validations automatiques restent obligatoires.

---

## Phase 4 — Qualité et sécurité

Terminée.

Outils réellement intégrés :

```text
SonarQube Community Build
GitHub CodeQL
Trivy
Dependabot
```

Workflows GitHub associés notamment :

```text
quality.yml
codeql.yml
security.yml
```

SonarQube a produit une Quality Gate `OK` sur un run GitHub réel.

CodeQL et Trivy ont également été exécutés réellement sur GitHub.

Les nombres de CVE constatés historiquement pendant cette phase ne doivent pas être considérés automatiquement comme dette actuelle.

Consulter la section de sécurité la plus récente de :

```text
ENTERPRISE_MIGRATION_REPORT.md
```

avant d'affirmer le nombre courant de vulnérabilités.

---

## Phase 5 — Modular Monolith et Spring Modulith

Terminée pour son critère mécanique de frontières modulaires.

Spring Modulith est intégré.

Le test :

```text
ModularityTests
```

vérifie :

* les frontières de modules ;
* les dépendances ;
* l'absence de cycle architectural.

Modules transversaux actuellement ouverts :

```text
common
config
security
```

Les modules :

```text
project
ticket
user
```

exposent explicitement certains sous-packages via `@NamedInterface`.

Un cycle réel :

```text
project -> ticket -> project
```

a été détecté puis supprimé avec inversion de dépendance grâce au port :

```text
ProjectMemberAssignmentCheck
```

défini dans `project` et implémenté dans `ticket`.

Références :

```text
docs/architecture/modules.md
docs/adr/0016-spring-modulith-boundaries.md
```

Attention :

la présence de Spring Modulith ne signifie pas que toute l'architecture hexagonale est terminée.

Restent notamment des améliorations possibles :

* réduction du couplage direct entre modules ;
* APIs applicatives plus étroites ;
* remplacement progressif de certains accès directs aux repositories d'autres modules ;
* utilisation raisonnée d'événements métier ;
* séparation `api/application/domain/infrastructure` uniquement lorsqu'elle apporte une vraie valeur.

Ne pas présenter ces éléments comme terminés tant qu'ils ne le sont pas réellement.

---

## Phase 6 — Upgrade technologique

Terminée et fusionnée dans `main`.

Pull Request :

```text
#28
```

Commit de fusion :

```text
a7463afce0b9ce454519ae35ce493faaa2cffed5
```

Stack validée à la sortie de phase :

### Backend

```text
Java 21 LTS
Spring Boot 4.1.0
Spring Framework 7.0.8
Spring Security 7.1.0
Hibernate 7.4.1
Spring Modulith 2.1.0
Springdoc 3.1.0
MapStruct 1.6.3
Jackson 3
```

### Frontend

```text
Angular 22.1.2
Angular CLI 22.1.2
Angular Material 22.1.2
TypeScript 6.0.3
Node.js 24.19.0 LTS
npm 12.0.2
```

### Database

```text
PostgreSQL 18.6
```

PostgreSQL utilise désormais le point de montage correspondant à PostgreSQL 18 :

```text
/var/lib/postgresql
```

La migration PostgreSQL a été effectuée par sauvegarde/restauration réelle tout en conservant l'ancien volume intact au moment de la migration.

Référence :

```text
docs/adr/0017-postgresql-18-migration.md
```

---

## Phase 7 — API, profils, erreurs et concurrence

Terminée et fusionnée dans `main`.

Pull Request :

```text
#30
```

Éléments terminés :

* API versionnée sous `/api/v1` ;
* profils Spring `dev`, `test`, `prod` ;
* différences réelles selon les profils ;
* Springdoc désactivé en production ;
* erreurs standardisées avec `ProblemDetail` ;
* transactions ajoutées aux services appropriés ;
* optimistic locking avec `@Version` ;
* migration Flyway `V7` ;
* réponse HTTP `409 Conflict` lors des conflits de verrouillage optimiste ;
* validation sur PostgreSQL réel.

---

## Phase 8 — Session Spring Security et CSRF

Terminée et fusionnée dans `main`.

Pull Request :

```text
#31
```

Commit de fusion :

```text
4b983a3e05faaa60c2c3cc5dcabab8ba6e6d81a3
```

L'authentification a été migrée de :

```text
JWT stateless
+
localStorage
```

vers :

```text
Spring Security server-side session
+
cookie TASKIRA_SESSION
```

Propriétés principales du cookie de session :

```text
HttpOnly
SameSite=Lax
Secure en production
```

Protection CSRF :

```text
CookieCsrfTokenRepository
CsrfTokenRequestAttributeHandler
CsrfCookieFilter
```

Le frontend et le backend sont désormais en mode session.

Ne pas réintroduire :

```text
JWT d'authentification
token d'authentification dans localStorage
token d'authentification dans sessionStorage
```

Un bootstrap ADMIN idempotent limité au profil `dev` est également présent.

Validation historique à la sortie de phase :

```text
39 tests backend
25 tests Vitest
9/9 Playwright
smoke test réel de la stack Docker dev
```

Ces nombres sont historiques et augmenteront normalement avec les futures phases.

---

## Phase 9 — Audit, request ID et logs

Terminée et fusionnée dans `main`.

Pull Request :

```text
#32
```

Commit de fusion :

```text
1e6170b
```

Module `audit` (Spring Modulith, fermé par défaut, dépend uniquement de `common`/`security`) :

```text
AuditEvent (table audit_events, Flyway V8)
AuditService.record(actorId, actorEmail, entityType, entityId, action, detail)
GET /api/v1/audit/events (ADMIN uniquement, paginé)
```

Déclencheurs réels : connexion réussie/échouée, déconnexion, création de ticket et changement de statut, création/archivage de projet, ajout/retrait de membre, création d'utilisateur, changement de rôle et de statut actif. Rien sur les commentaires (déjà couverts par `ticket_history`).

Corrélation des requêtes :

```text
X-Request-ID (accepté si sûr, sinon UUID généré)
MDC (clé requestId)
ProblemDetails.of(...) — point unique de construction des erreurs, propriété requestId
```

Logs structurés JSON en profil `prod` (`logging.structured.format.console: logstash`, natif Spring Boot 4.1, aucune dépendance ajoutée); pattern lisible avec `%X{requestId}` en `dev`/`test`.

Voir [ADR-0018](docs/adr/0018-audit-request-correlation.md) pour le détail complet des décisions.

Validation historique à la sortie de phase :

```text
65 tests backend (29 rapides + 36 intégration)
25 tests Vitest
10/10 Playwright
```

Ces nombres sont historiques et augmenteront normalement avec les futures phases.

---

## Phase 10 — Observabilité (Actuator, Micrometer, Prometheus, Grafana)

Terminée et fusionnée dans `main`.

Pull Request :

```text
#33
```

Commit de fusion :

```text
8f560fe
```

Actuator exposé sur un port de gestion isolé, jamais publié à l'hôte :

```text
management.server.port = 9091 (surchargeable via MANAGEMENT_SERVER_PORT)
management.endpoints.web.exposure.include = health,info,prometheus
groupe readiness (readinessState, db) et liveness (livenessState)
```

`EndpointRequest.toAnyEndpoint().permitAll()` dans `SecurityConfig` autorise ce port anonymement : la séparation de port ne suffit pas seule, la correspondance de Spring Security est fondée sur le chemin et non sur le port, donc une requête sur le port de gestion traverse la même chaîne de filtres que le port applicatif principal.

Métriques métier (`config.BusinessMetricsBinder`, `MeterBinder`) : `taskira_tickets`/`taskira_projects` (jauges par statut), `taskira_users_active` (jauge par rôle), `taskira_auth_login_attempts_total` (compteur par résultat, incrémenté dans `AuthService`).

Prometheus (`v3.13.2`, épinglé par digest) scrute `backend:9091/actuator/prometheus` toutes les 15 s. Grafana (`13.1.3`, épinglé par digest) provisionne automatiquement la datasource Prometheus et deux dashboards (`taskira-runtime`, `taskira-business`).

Voir [ADR-0012](docs/adr/0012-observability-stack.md) pour le détail complet des décisions et découvertes.

Validation historique à la sortie de phase :

```text
76 tests backend (33 rapides + 43 intégration)
25 tests Vitest (inchangé, aucun fichier frontend modifié)
10/10 Playwright
```

Ces nombres sont historiques et augmenteront normalement avec les futures phases.

---

## Phase 11 — Nginx et runtime production-like

Terminée et fusionnée dans `main`.

Pull Request :

```text
#34
```

Commit de fusion :

```text
ec22ad6
```

Runtime `production-like` séparé du développement, pas une vraie production (HTTP local, pas de certificat — TLS réel en P15+) :

```text
Client -> Nginx :8080 (non-root, publié) -> /api/* -> Spring Boot (non-root, interne) -> PostgreSQL (interne)
```

`infra/docker-compose.prodlike.yml`, fichier séparé de `infra/docker-compose.yml` avec un nom de projet Compose explicite (`name: taskira-prodlike`) — trois réseaux Docker (`app_net`, `db_net`, `observability_net`) au lieu d'un seul : le frontend ne peut jamais joindre PostgreSQL même en cas de mauvaise configuration Nginx. Seul `frontend` publie un port hôte. Prometheus/Grafana restent derrière un profil Compose optionnel `observability`, jamais publiés même actifs.

`nginxinc/nginx-unprivileged:1.30.4-alpine3.24` (branche stable, épinglée par digest) sert le build Angular statique et proxifie `/api/*` vers `backend:8080`. `backend/Dockerfile` (partagé avec le développement et l'E2E) tourne désormais en utilisateur explicite non-root (`taskira`, uid/gid 10001).

`application-prod.yaml` fixe `Secure=true` sur le cookie de session, incompatible avec ce runtime HTTP local; résolu par `SERVER_SERVLET_SESSION_COOKIE_SECURE=false` en variable d'environnement uniquement dans `infra/docker-compose.prodlike.yml`, sans toucher au défaut réel de production. Voir [ADR-0019](docs/adr/0019-production-runtime.md) pour le détail complet, y compris plusieurs bugs réels trouvés uniquement en démarrant réellement la stack complète (jamais par simple `docker compose config`) : collision de nom de projet Compose ayant détruit les conteneurs de développement et écrasé son image frontend en cache, résolution DNS Nginx figée au démarrage, écoute IPv6 manquante sur le healthcheck.

Validation historique à la sortie de phase :

```text
76 tests backend (33 rapides + 43 intégration)
25 tests Vitest, lint 0 erreur/36 avertissements `any`, build Angular de production réussi
10/10 Playwright (stack de développement isolée)
Smoke test Playwright réel supplémentaire contre la stack production-like démarrée pour de vrai :
inscription, connexion, cookie de session (HttpOnly/SameSite=Lax/Secure=false), navigation
authentifiée et lien profond SPA, tous via un vrai navigateur à travers le reverse proxy Nginx réel
```

Ces nombres sont historiques et augmenteront normalement avec les futures phases.

---

## Phase 12 — Notifications et Mailpit

Terminée et fusionnée dans `main`.

Pull Request :

```text
#35
```

Commit de fusion :

```text
40e791e
```

Deux déclencheurs seulement (`TicketAssignedEvent`, `CommentCreatedEvent`) : ceux qui ont un destinataire non ambigu et un effet direct sur ce que la personne doit faire ensuite. Voir [ADR-0020](docs/adr/0020-notifications-mailpit.md) — premiers événements métier réels du projet (Spring Application Events, anticipés sans usage concret depuis AGENTS.md §36).

`ticket`/`comment` publient les événements (types exposés via `@NamedInterface`), le module `notifications` (fermé par défaut) les écoute via `@TransactionalEventListener(phase = AFTER_COMMIT)` — jamais un `@EventListener` simple, pour qu'une transaction annulée ne déclenche jamais un email pour une assignation qui n'a in fine pas eu lieu. Envoi best-effort : toute `MailException` est loguée et jamais propagée, un Mailpit indisponible ne doit jamais faire échouer l'opération métier qui a déclenché la notification.

Bug réel trouvé par la suite de tests complète, pas supposé : ajouter `spring-boot-starter-mail` active automatiquement un indicateur de santé Actuator pour le courrier, qui fait passer `/actuator/health` à 503 dès que le serveur SMTP n'est pas joignable — directement contraire à la philosophie « best-effort » de ce module. Corrigé avec `management.health.mail.enabled: false`.

`axllent/mailpit:v1.30.7`, épinglé par digest, uniquement dans `infra/docker-compose.yml` (développement) — jamais dans le runtime production-like de P11, qui n'a pas vocation à simuler l'envoi d'email.

Validation historique à la sortie de phase :

```text
83 tests backend (38 rapides + 45 intégration, dont NotificationWiringIT : un vrai conteneur
Mailpit via Testcontainers, un vrai contexte Spring, l'API HTTP réelle de Mailpit pour lire
l'email réellement reçu)
25 Vitest, lint et build Angular inchangés (aucun fichier frontend modifié)
Vérifié en plus contre la vraie stack de développement : inscription, création de projet/ticket,
assignation via l'API réelle, email réellement reçu dans Mailpit avec le bon sujet
```

Ces nombres sont historiques et augmenteront normalement avec les futures phases.

---

## Phase 13 — Pièces jointes (Tika, stockage local, sécurité upload)

Terminée et fusionnée dans `main`.

Pull Request :

```text
#36
```

Commit de fusion :

```text
0e535d2
```

Module `attachments` (Spring Modulith, fermé par défaut, aucune autre partie du code ne le consomme) : entité `Attachment` (`AuditableEntity`, liée à `Ticket` et à l'utilisateur uploadeur), table `attachments` (Flyway `V9`), port `DocumentStorage` avec une seule implémentation `LocalFileSystemStorage` — exactement ce qu'annonçait ADR-0009, désormais livré et promu `Accepted`. Voir [ADR-0021](docs/adr/0021-attachments-storage.md) pour le détail complet des décisions de sécurité.

Aucune entrée client n'est jamais approuvée directement :

```text
type MIME réel détecté par Apache Tika (tika-core 3.3.1), jamais le Content-Type déclaré ni l'extension
liste blanche explicite de types autorisés, tout le reste rejeté (409)
clé de stockage = UUID généré côté serveur, jamais dérivé du nom de fichier client
chemin résolu re-vérifié contre le répertoire de stockage (défense en profondeur, pas seulement le regex du UUID)
taille limitée à la fois par Spring (multipart.max-file-size) et par le service
SHA-256 calculé et stocké pour chaque fichier
Content-Disposition: attachment (jamais inline), nom de fichier encodé RFC 6266
```

`GET/POST /api/v1/tickets/{id}/attachments`, `GET /api/v1/attachments/{id}/content` (téléchargement en streaming), `DELETE /api/v1/attachments/{id}`. Mêmes règles d'accès que les commentaires/tickets (membre du projet, upload/suppression bloqués sur un projet archivé). Chaque upload et suppression est audité (`ATTACHMENT_CREATED`, `ATTACHMENT_DELETED`).

Deux bugs réels trouvés et corrigés pendant l'écriture des tests, pas supposés :

1. Le SHA-256 était calculé en enveloppant le flux passé à `DocumentStorage.store(...)` dans un `DigestInputStream` — un mock qui ne lit jamais réellement le flux (cas d'un test unitaire) laisse alors le digest dans son état initial et produit silencieusement le hash d'un contenu vide. Corrigé en lisant le fichier une seule fois en mémoire et en calculant le SHA-256 directement sur ce tableau d'octets, indépendamment de ce que fait l'adaptateur de stockage.
2. Le service résolvait l'utilisateur courant avant de vérifier le type MIME réel, ce qui remontait une erreur trompeuse (« utilisateur introuvable ») sur un test qui n'avait volontairement pas stubé cette dépendance pour un fichier de toute façon destiné au rejet. Corrigé en vérifiant le type de contenu avant de résoudre l'utilisateur — plus correct aussi bien pour le test que pour la conception : un fichier refusé ne doit pas payer le coût d'une résolution utilisateur inutile.

Un troisième bug, découvert uniquement en démarrant réellement la stack de développement (pas par les tests automatisés) : le backend non-root introduit en P11 n'a pas la permission d'écrire dans `/var/lib/taskira`, répertoire jamais créé ni monté par un volume. `LocalFileSystemStorage` échouait au démarrage avec `AccessDeniedException`. Corrigé en pré-créant et chownant le répertoire dans `backend/Dockerfile` avant de basculer vers `USER taskira`, et en montant un volume nommé à ce chemin dans `infra/docker-compose.yml` et `infra/docker-compose.prodlike.yml` — le mécanisme de copy-up de Docker sur un volume nommé préserve alors la propriété déjà posée dans l'image.

Vérifié réellement contre la vraie stack de développement après ce correctif, pas seulement en test automatisé : upload d'un vrai PNG (201, SHA-256 correct), téléchargement identique octet pour octet avec les bons en-têtes, rejet réel d'un script shell déguisé en `.png` (409, type détecté `application/x-sh`), suppression (204) avec disparition confirmée du fichier physique sur disque et de la ligne en base, et les deux événements d'audit correctement enregistrés.

Un quatrième bug, trouvé uniquement par le vrai run GitHub Actions de la Pull Request (jamais reproduit localement avant cela) : le `mvn verify` local avait toujours été exécuté dans un conteneur Maven générique tournant en `root`, qui peut écrire n'importe où — masquant que la valeur par défaut de `app.attachments.storage-path` (`/var/lib/taskira/attachments`) n'est réellement écrivable que grâce au `chown` explicite de l'image Docker de ce dépôt, pas par défaut sur une machine quelconque. Sur le runner GitHub Actions (utilisateur non privilégié), `ProdProfileTest` (le seul test Surefire à démarrer un contexte Spring complet) échouait avec `BeanCreationException` → `AccessDeniedException`, faisant aussi échouer le check `SonarQube analysis and quality gate` qui exécute le même `mvn verify` avant l'analyse. Corrigé en changeant la valeur par défaut vers un chemin relatif au répertoire de travail (`./data/attachments`) — écrivable sans provisioning particulier partout où l'application démarre directement (CI, poste de développement, tout `*Test`/`*IT`) — et en alignant `backend/Dockerfile` et les deux fichiers Compose sur `/app/data/attachments` (sous le `WORKDIR` de l'image) pour préserver le comportement de persistance Docker. Revérifié par un `mvn verify` propre (100 tests toujours verts), un nouveau smoke test réel contre la stack de développement reconstruite, et un nouveau 10/10 Playwright, avant repush.

PR [#36](https://github.com/Joe-azar/taskira/pull/36) vérifiée verte sur GitHub avant fusion — `CI Gate`, `Backend`, `SonarQube analysis and quality gate`, `Containers and E2E`, CodeQL et Trivy tous passés après ce correctif.

Validation historique à la sortie de phase :

```text
100 tests backend (51 rapides + 49 intégration, dont AttachmentWiringIT : upload/téléchargement/
suppression réels via HTTP et PostgreSQL, pas AttachmentService appelé directement)
25 Vitest, lint et build Angular inchangés (aucun fichier frontend modifié - P13 est backend uniquement)
10/10 Playwright (stack de développement isolée, aucun nouveau scénario - aucune UI d'attachments
n'existe encore côté frontend)
```

Ces nombres sont historiques et augmenteront normalement avec les futures phases.

---

## Phase 14 — Exports (POI, OpenHTMLtoPDF, PDFBox, ZXing, Spring Batch)

Terminée et fusionnée dans `main`.

Pull Request :

```text
#37
```

Commit de fusion :

```text
2f84d5b
```

Module `exports` (Spring Modulith, fermé par défaut) couvre trois cas réels distincts, pas une simple vitrine technologique — voir [ADR-0022](docs/adr/0022-exports-batch.md) pour le détail complet des décisions.

Deux exports synchrones, bornés par construction, mêmes règles d'accès que les endpoints JSON qu'ils reflètent :

```text
GET /api/v1/projects/{id}/tickets/export.xlsx  Apache POI (poi-ooxml 5.5.1), un seul projet
GET /api/v1/tickets/{id}/export.pdf            OpenHTMLtoPDF (1.0.10), un seul ticket,
                                                QR code ZXing intégré (lien vers le ticket web),
                                                métadonnées PDF posées par PDFBox après rendu
```

Un export en masse asynchrone, seul cas qui justifie réellement Spring Batch (volume non borné, tous projets confondus) :

```text
POST /api/v1/exports/tickets/batch              ADMIN uniquement, lance ticketsBulkExportJob
GET  /api/v1/exports/tickets/batch/{id}          état du job (STARTING/STARTED/COMPLETED/FAILED)
GET  /api/v1/exports/tickets/batch/{id}/download téléchargement une fois COMPLETED
```

`ticketsBulkExportJob` pagine tous les tickets (`ItemReader` maison, pas de dépendance `spring-batch-data` supplémentaire) dans un `SXSSFWorkbook` streaming partagé sur toute la durée du job, un onglet par code de projet. Le fichier fini est stocké via le port `DocumentStorage` déjà défini par `attachments` (P13, désormais exposé via `@NamedInterface`) plutôt qu'un second mécanisme de stockage. Schéma `BATCH_*` de Spring Batch 6.0.4 (résolu par le BOM Spring Boot 4.1.0) créé par Flyway `V10`, jamais par l'initialiseur intégré de Spring Boot (`spring.batch.jdbc.initialize-schema: never`, même règle que `ddl-auto: validate` pour Hibernate); `spring.batch.job.enabled: false` empêche tout lancement automatique au démarrage.

Trois bugs réels trouvés et corrigés, tous par des tests qui vérifient de vrais états, pas des suppositions :

1. **Entité XML non déclarée** : `&middot;` (entité HTML nommée) fait échouer le rendu OpenHTMLtoPDF, qui exige du XHTML strict (seules `amp`/`lt`/`gt`/`quot`/`apos` sont valides sans DTD). Corrigé avec la référence numérique `&#183;`.
2. **`Specification.where(null)`** : cette version de Spring Data JPA lève `IllegalArgumentException` plutôt que de traiter `null` comme « aucune restriction » (ambiguïté de surcharge entre `Specification<T>` et `PredicateSpecification<T>` en plus). Corrigé avec une spécification explicitement toujours vraie (`(root, query, cb) -> cb.conjunction()`).
3. **Persistance de l'`ExecutionContext` après complétion du job** : Spring Batch 6.0 `StepBuilder.chunk(int)` bascule silencieusement vers un `ResourcelessTransactionManager` si aucun n'est fourni explicitement (vérifié dans le code source du framework) — corrigé en câblant le vrai `PlatformTransactionManager` JPA. Plus sérieux : `JobRepository.update(JobExecution)` — le seul appel de persistance qu'`AbstractJob` effectue autour de la complétion d'un job — ne persiste jamais l'`ExecutionContext` (vérifié dans le code source : il n'appelle que le DAO d'exécution de job). La logique de finalisation vivait initialement dans un `JobExecutionListener.afterJob(...)`, qui s'exécute en plus *après* que le statut terminal du job soit déjà visible depuis l'extérieur — un appelant qui sonde uniquement le statut pouvait observer `COMPLETED` avant même que le listener ait fini. Déplacée vers un `StepExecutionListener.afterStep(...)` (s'exécute dans le cadre de la complétion du step lui-même, strictement avant la détermination du statut global du job) avec un appel explicite à `jobRepository.updateExecutionContext(...)`, que rien d'autre dans le framework n'effectue sur ce chemin.

Vérifié réellement contre la vraie stack de développement reconstruite, pas seulement en test automatisé : export Excel synchrone (`.xlsx` réel, ouvrable), export PDF synchrone (`.pdf` réel, 1 page), lancement du job en masse par l'admin dev bootstrapé (`202`, `jobExecutionId`), sondage jusqu'à `COMPLETED` (quasi instantané sur ce volume de données), téléchargement du classeur réel — les onglets couvrent bien tous les projets existants de la base réelle, y compris ceux créés par d'autres sessions de test — événement d'audit `EXPORT_GENERATED` confirmé en base, et un utilisateur non-admin refusé (`403`) sur le lancement.

Validation historique à la sortie de phase :

```text
115 tests backend (59 rapides + 56 intégration, dont BulkExportJobIT — vrais états de job
Spring Batch via le vrai JobLauncher asynchrone de l'application — et BulkExportWiringIT/
ExportWiringIT — flux HTTP complets, fichiers réels relus avec POI/PDFBox, QR code
redécodé avec ZXing depuis la page PDF rendue)
25 Vitest, lint et build Angular inchangés (aucun fichier frontend modifié - P14 est
backend uniquement, comme P12 et P13)
10/10 Playwright (stack de développement isolée, aucun nouveau scénario)
```

Ces nombres sont historiques et augmenteront normalement avec les futures phases.

---

## Phase 15 — Registry, release et staging (GHCR, SemVer, rollback)

Terminée et fusionnée dans `main`.

Pull Request :

```text
#46
```

Commit de fusion :

```text
e2072bc
```

`.github/workflows/release.yml` (déclenché sur `push` d'un tag `v*.*.*`) publie `ghcr.io/joe-azar/taskira-{backend,frontend}` sous deux tags à chaque fois — la version et le SHA du commit, jamais `latest` seul (ADR-0013) — puis déploie réellement la version tout juste publiée via `infra/docker-compose.staging.yml` (le seul fichier Compose du dépôt qui ne construit jamais d'image localement : `image: ghcr.io/...:${VERSION:?...}`) et exécute la suite Playwright complète contre ce déploiement réel avant de publier une `GitHub Release` formelle (notes auto-générées) — la release n'apparaît que si le déploiement et les tests ont réellement réussi, jamais sur la seule force d'un build vert.

`infra/docker-compose.staging.yml` reprend la topologie à trois réseaux de `docker-compose.prodlike.yml` (P11) sans Prometheus/Grafana (déjà validés en P10/P11; staging se concentre sur la vérification de déploiement). Le rollback n'est pas une procédure séparée : redéployer une version antérieure consiste à relancer ce même fichier avec un `VERSION` différent — exactement le chemin de code déjà emprunté par le workflow de release lui-même.

Première version réellement publiée : `v0.1.0`, pas `v1.0.0` — SemVer `0.x` signale explicitement « pré-1.0, en évolution », cohérent avec le fait que les phases 15 à 20 ne sont pas terminées. Le numéro de version n'est pas indexé sur le numéro de phase.

Deux bugs réels trouvés par le premier run réel déclenché par tag (pas supposés, corrigés avant le second run) :

1. **`POSTGRES_PASSWORD`/`VERSION` en `env:` de step, pas de job** : chaque invocation `docker compose -f docker-compose.staging.yml` réinterpole tout le fichier quelle que soit la sous-commande — l'étape de démontage (`down`) échouait avec la même erreur de variable requise absente que le fichier est censé lever volontairement en cas d'absence réelle. Corrigé en déplaçant ces variables au niveau du job.
2. **`npm ci` exécuté dans `frontend/`** : `e2e/playwright/playwright.config.ts` résout son propre `import ... from '@playwright/test'` relativement à l'emplacement du fichier de configuration (un répertoire frère de `frontend/`, jamais un ancêtre), pas relativement au répertoire depuis lequel `npx` est invoqué — `Cannot find module '@playwright/test'` malgré une résolution `npx` elle-même correcte. Corrigé en installant à la racine du dépôt, reproduisant exactement l'approche déjà utilisée par `e2e/playwright/Dockerfile`.

Second run réel entièrement vert : images GHCR construites et poussées, stack staging démarrée et saine, 10/10 Playwright contre le déploiement réel, `GitHub Release v0.1.0` publiée avec notes auto-générées (<https://github.com/Joe-azar/taskira/releases/tag/v0.1.0>). Rollback vérifié réellement en plus, indépendamment du run CI : `v0.1.0` retiré (`docker pull`) et redéployé localement via `infra/docker-compose.staging.yml`, stack saine, `/api/v1/auth/me` (401 via le proxy Nginx), `/healthz` (200) et le SPA (200) tous confirmés fonctionnels avant démontage propre.

Validé localement avant tout push : `actionlint 1.7.12` (0 problème), `docker compose config` (cas valide et cas de rejet de variable requise absente).

---

## Phase 16 — Sauvegarde et restauration testée

Terminée et fusionnée dans `main` (PR #47, commit de fusion `3eacaaf`; correctif de suivi PR #48, commit de fusion `e03c51f`).

Deux mécanismes distincts (ADR-0023, [`docs/architecture/backup.md`](docs/architecture/backup.md)), pas un seul mal défini :

```text
scripts/backup/backup-postgres.ps1     à la demande, contre n'importe quelle base réelle
scripts/restore/restore-postgres.ps1   restauration + vérification, jamais sur la base active
.github/workflows/backup-restore-drill.yml   hebdomadaire, prouve que le mécanisme fonctionne toujours
```

Aucune base de données de production persistante n'existe encore pour qu'un job planifié la sauvegarde réellement (le staging de P15 est déployé et détruit à la demande, la vraie production reste hors périmètre avant P19) — l'ADR le dit explicitement plutôt que de prétendre le contraire. Le rôle du workflow planifié n'est donc pas de protéger des données réelles, mais de démarrer un vrai PostgreSQL et le vrai backend (toutes les migrations Flyway réellement appliquées), semer de vraies données via l'API HTTP réelle, sauvegarder, restaurer dans un conteneur jetable, puis vérifier que ces données sont réellement revenues — pas seulement que `pg_restore` a rendu un code de sortie zéro.

Deux bugs réels trouvés en exécutant réellement le cycle complet avant fusion, pas supposés :

1. **`FATAL: the database system is shutting down`** : le point d'entrée officiel de l'image PostgreSQL démarre un serveur temporaire (socket Unix seulement) pour les scripts d'initialisation, l'arrête, puis démarre le serveur réel — `pg_isready` seul pouvait réussir contre ce serveur temporaire juste avant son arrêt. Corrigé en remplaçant la vérification par une boucle de nouvelle tentative autour d'une vraie requête `psql -c "SELECT 1"`, plus une redirection `stderr` retirée (PowerShell 5.1 la transforme en erreur bloquante sous `$ErrorActionPreference = "Stop"`).
2. **Extraction du chemin de sauvegarde cassée par un chemin contenant des espaces** : trouvé en répétant localement les étapes du workflow avant fusion (`workflow_dispatch` ne peut pas tester un tout nouveau fichier de workflow avant qu'il n'existe sur la branche par défaut) — le chemin de ce dépôt lui-même (« ...ALL DATA\France\Taskira... ») contient un espace, cassant une regex `\S+`. Corrigé avec une regex gourmande ancrée sur le suffixe fixe du message.

Validation locale réelle avant fusion : cycle complet semer → sauvegarder → restaurer → vérifier exécuté à la main contre la base de développement réelle (10 migrations, données accumulées réelles), y compris les requêtes de vérification exactes utilisées par le workflow.

**Un troisième bug réel, trouvé uniquement par le tout premier run réel du workflow sur GitHub Actions**, après fusion : `workflow_dispatch` ne pouvait littéralement pas être testé avant que le fichier n'existe sur `main`, donc ce run était la première exécution jamais faite dans le vrai environnement GitHub Actions. Il a échoué à l'étape « Back up the seeded database » avec `Cannot index into a null array` — `backup-postgres.ps1` ne rapporte son chemin de sauvegarde que via `Write-Host`, qui écrit directement sur la console et n'atteint jamais le flux de sortie de succès de PowerShell; `$result = & backup-postgres.ps1` capturait donc `$null`, et la répétition locale avant fusion avait testé la regex contre du texte synthétique, pas contre cette sémantique de capture réelle. Corrigé à la racine (PR #48) : le script se termine désormais par une expression nue (`$hostPath`) qui devient sa vraie valeur de retour, capturable directement (`$dumpPath = & backup-postgres.ps1 | Select-Object -Last 1`) sans plus jamais analyser du texte de log. Un second run réel déclenché après ce correctif ([32567218685](https://github.com/Joe-azar/taskira/actions/runs/32567218685)) est passé intégralement au vert, confirmant le mécanisme sur GitHub Actions et pas seulement en local.

---

## Phases 17 à 20

Planifiées mais non considérées comme terminées tant que leur implémentation et leur validation ne sont pas réellement présentes.

Roadmap générale :

```text
Phase 13  Attachments + Tika + storage + sécurité uploads
Phase 14  Exports + POI + OpenHTMLtoPDF + PDFBox + ZXing + Spring Batch
Phase 15  GHCR + release + staging + rollback
Phase 16  Backup + restore testé
Phase 17  Kubernetes Lab
Phase 18  Helm Lab
Phase 19  Azure Lab
Phase 20  Technologies optionnelles / labs
```

Ne jamais présenter une technologie d'une phase future comme installée uniquement parce qu'elle apparaît dans la roadmap.

---

# 6. Versions technologiques actuelles

Depuis la Phase 6, la baseline technologique est notamment :

```text
Java 21 LTS
Spring Boot 4.1.x
Spring Framework 7.x
Spring Security 7.x
Angular 22.1.x
TypeScript 6.x compatible Angular
Node.js 24 LTS
PostgreSQL 18.6
```

Java 21 LTS est la version Java cible actuelle de Taskira.

Ne pas migrer opportunément vers :

```text
Java 25
nouvelle major Spring Boot
nouvelle major Angular
nouvelle major PostgreSQL
```

sans phase dédiée, analyse de compatibilité, tests et ADR si la décision est structurante.

Privilégier les patchs compatibles lorsqu'ils sont nécessaires pour la sécurité ou la stabilité.

---

# 7. Principes d'architecture

Taskira reste :

```text
Angular
   ↓
REST /api/v1
   ↓
Spring Boot modular monolith
   ↓
PostgreSQL
```

Organiser le code d'abord par fonctionnalité métier.

Backend actuel à faire évoluer progressivement :

```text
auth
user
project
ticket
comment
dashboard
audit
notifications
attachments
exports
```

Frontend :

```text
core/
shared/
features/
```

Chaque module backend doit exposer une API interne aussi étroite que possible.

Un module ne doit pas accéder arbitrairement :

* aux repositories internes d'un autre module ;
* aux détails d'infrastructure d'un autre module ;
* aux classes internes qui ne font pas partie de son contrat public.

Préférer selon le besoin :

```text
application service
public module API
port
event
```

Ne pas créer d'interfaces ou de couches uniquement pour satisfaire artificiellement un diagramme.

Les couches :

```text
api
application
domain
infrastructure
```

doivent être introduites lorsqu'elles clarifient réellement un domaine suffisamment complexe.

Garder `common` et `shared` strictement transversaux.

Ne pas transformer un besoin métier en classe générique placée dans `common` simplement pour éviter de choisir son propriétaire.

Toute dépendance entre modules doit rester compatible avec :

```text
ModularityTests
```

Aucun nouveau cycle n'est acceptable.

---

# 8. Technologies à ne pas introduire dans le runtime principal sans justification

Ne pas ajouter au runtime principal uniquement pour enrichir la stack :

```text
microservices
Kafka
RabbitMQ
Redis
Elasticsearch
OpenSearch
Event Sourcing
CQRS complet
service mesh
GraphQL
WebFlux généralisé
```

Priorité pour Taskira :

```text
Spring Application Events
Spring Scheduler
Spring Batch
PostgreSQL
REST
Spring MVC
```

Les technologies plus avancées peuvent être étudiées dans :

```text
labs/
```

sans devenir des dépendances obligatoires de l'application.

---

# 9. Règles backend Java / Spring

Utiliser Java 21.

Injection de dépendances :

```text
constructor injection only
```

Interdit dans le code applicatif :

```java
@Autowired
private SomeService service;
```

Les contrôleurs :

* valident les entrées HTTP ;
* traduisent HTTP vers les use cases ;
* délèguent ;
* construisent la réponse HTTP.

Ils ne doivent pas contenir :

* de logique métier importante ;
* de logique de persistance ;
* de transactions ;
* d'orchestration complexe.

Les transactions doivent être placées principalement dans la couche application/service portant le cas d'usage.

Utiliser :

```java
@Transactional
```

seulement lorsque la frontière transactionnelle est claire.

Ne jamais utiliser :

```java
System.out.println()
System.err.println()
```

dans le code applicatif.

Utiliser :

```text
SLF4J
Logback
```

Ne jamais logger :

* mot de passe ;
* session cookie ;
* token CSRF ;
* secret ;
* credential DB ;
* clé privée ;
* donnée personnelle inutile.

---

# 10. API REST

Toutes les routes applicatives sont versionnées sous :

```text
/api/v1
```

Référence centrale :

```text
common.web.ApiVersion
```

Ne pas créer de nouvelle route applicative non versionnée.

Lors d'une modification de préfixe ou de route, vérifier simultanément :

* controller backend ;
* `SecurityConfig` ;
* services frontend ;
* interceptors ;
* tests backend ;
* Vitest ;
* Playwright ;
* documentation OpenAPI.

Utiliser les codes HTTP de manière cohérente :

```text
200 OK
201 Created
204 No Content
400 Bad Request
401 Unauthorized
403 Forbidden
404 Not Found
409 Conflict
422 Unprocessable Entity uniquement si réellement justifié
500 Internal Server Error
```

Les erreurs REST utilisent le format `ProblemDetail` lorsqu'il est applicable.

---

# 11. Authentification et sécurité

Le backend est l'autorité de sécurité.

Le frontend peut :

* cacher un bouton ;
* désactiver une action ;
* afficher une navigation différente ;
* utiliser un guard.

Mais cela ne constitue jamais une protection de sécurité.

Toute règle d'autorisation doit exister côté serveur.

Authentification actuelle :

```text
Spring Security
server-side session
TASKIRA_SESSION
```

Le cookie de session :

```text
HttpOnly
SameSite=Lax
Secure en prod
```

La protection CSRF doit rester active.

Ne jamais rendre un test vert en :

```text
désactivant Spring Security
désactivant CSRF
ouvrant temporairement tous les endpoints
```

sans justification explicite et uniquement dans un environnement de test réellement isolé.

Toute évolution importante de :

```text
authentication
authorization
CORS
CSRF
session
cookies
```

doit inclure des tests de sécurité.

Si elle modifie le modèle de confiance, créer ou mettre à jour un ADR.

---

# 12. Bootstrap ADMIN

Le bootstrap admin est autorisé uniquement dans les environnements prévus, notamment `dev`.

Configuration par variables d'environnement.

Aucun mot de passe réel ne doit apparaître dans Git.

Le bootstrap doit rester :

```text
idempotent
désactivable
non actif en production
```

---

# 13. Base de données et Flyway

Toute évolution de schéma passe exclusivement par :

```text
Flyway
```

Répertoire :

```text
backend/src/main/resources/db/migration/
```

Règle absolue :

```properties
spring.jpa.hibernate.ddl-auto=validate
```

Hibernate ne doit jamais créer ou modifier automatiquement le schéma.

Ne jamais modifier une migration Flyway déjà appliquée.

Avant de créer une nouvelle migration :

1. inspecter le répertoire des migrations ;
2. identifier la dernière version existante ;
3. vérifier si elle est déjà appliquée ;
4. créer la version suivante.

Exemple :

```text
V7 existe
→ nouvelle migration = V8__description.sql
```

Ne jamais coder une règle supposant que `V6`, `V7` ou une autre version restera éternellement la dernière migration.

Tester les migrations sur un vrai PostgreSQL avec Testcontainers.

Ne pas utiliser H2 pour simuler PostgreSQL.

Une migration destructive exige avant intégration :

* justification ;
* stratégie de sauvegarde ;
* stratégie de compatibilité ;
* stratégie de retour arrière ou d'évolution forward-only documentée ;
* tests appropriés.

---

# 14. PostgreSQL

Version actuelle de référence :

```text
PostgreSQL 18.6
```

Ne pas simplement changer la major PostgreSQL tout en réutilisant aveuglément un volume d'une ancienne major.

Toute future montée de version majeure exige notamment :

```text
backup
nouveau volume
restore ou pg_upgrade approprié
Flyway validation
Hibernate validation
tests
```

Un backup ne doit pas être considéré comme fiable s'il n'a jamais été restauré avec succès.

---

# 15. Tests et qualité

Tout changement de comportement doit inclure ses tests dans le même lot lorsque raisonnablement possible.

Toute correction de bug doit idéalement commencer par un test de régression démontrant le problème.

Utiliser selon le type de test :

### Logique métier

```text
JUnit 5
Mockito
AssertJ
```

### Contrats HTTP et sécurité

```text
MockMvc
Spring Security Test
```

### Persistance et migrations

```text
Testcontainers
PostgreSQL réel
Flyway
```

### Frontend

```text
Vitest
Angular testing
```

### Parcours critiques

```text
Playwright
```

Ne jamais introduire H2 uniquement pour rendre les tests plus simples.

Ne jamais désactiver un test valide uniquement pour obtenir un pipeline vert.

Ne jamais réduire artificiellement un seuil de qualité sans comprendre la cause.

---

# 16. Nombres de tests

Les nombres de tests mentionnés dans l'historique des phases représentent uniquement l'état lors de leur validation.

Ils ne sont pas des constantes.

Avant d'annoncer :

```text
X tests backend
Y Vitest
Z Playwright
```

exécuter ou consulter le dernier run réel.

Le nombre de tests est normalement appelé à augmenter.

---

# 17. Playwright

La suite Playwright de référence peut être exécutée depuis la racine avec :

```powershell
& .\e2e\playwright\run.ps1
```

La suite doit :

* utiliser des données isolées ;
* utiliser des identités de test ;
* éviter de dépendre d'un état manuel de la DB ;
* nettoyer ses conteneurs ;
* nettoyer son réseau ;
* nettoyer son stockage temporaire ;
* produire ses rapports dans des chemins ignorés par Git.

Les tests ne doivent pas dépendre les uns des autres.

Lorsqu'une nouvelle API métier critique existe réellement, ajouter progressivement son parcours E2E.

Ne jamais simuler un endpoint inexistant uniquement pour compléter une checklist.

---

# 18. Validation d'un lot

Un lot applicatif n'est terminé que lorsque les validations pertinentes sont vertes.

Selon le type de changement, exécuter notamment :

### Backend

```text
test
verify
integration tests
ModularityTests
```

### Frontend

```text
npm ci
tests Vitest
lint
production build
```

### Infrastructure

```bash
docker compose -f infra/docker-compose.yml config
```

et si nécessaire :

```text
Docker build
Docker startup
health checks
smoke tests
```

### E2E

```text
Playwright
```

Pour une modification purement documentaire :

```text
git diff --check
vérification des liens relatifs
```

Un rebuild complet applicatif n'est pas requis si aucun code ou fichier de configuration exécutable n'a changé.

---

# 19. SonarQube et qualité

Les analyses Sonar doivent privilégier :

```text
bugs
vulnerabilities
security hotspots
maintainability sévère
duplication significative
```

Ne pas gaspiller des heures à éliminer des micro-code-smells sans impact si cela bloque injustement la progression.

Toute modification volontaire d'un Quality Gate, d'un seuil de couverture ou d'une exclusion doit être expliquée.

Ne jamais exclure artificiellement du code important uniquement pour améliorer les métriques.

---

# 20. Security scanning

Les scans de sécurité comprennent notamment :

```text
Dependabot
CodeQL
Trivy
```

Selon le contexte, vérifier :

* dépendances Maven ;
* dépendances npm ;
* code source ;
* Docker images ;
* secrets potentiels.

Ne jamais lancer aveuglément :

```bash
npm audit fix --force
```

ou une mise à jour majeure automatique sans analyser les breaking changes.

Une vulnérabilité doit être :

1. identifiée ;
2. qualifiée ;
3. corrigée ou documentée ;
4. rescannée.

---

# 21. Développement Docker-first

Taskira est Docker-first.

Le projet ne doit pas dépendre obligatoirement d'installations globales de :

```text
Java
Maven
Node
npm
Angular CLI
PostgreSQL
```

Utiliser les wrappers et versions du repository.

Environnement local de référence :

```text
infra/docker-compose.yml
```

La stack doit rester exécutable via Docker.

Le développement frontend doit conserver une expérience fluide avec hot reload lorsque le profil dev est utilisé.

Ne pas remplacer Docker par des installations locales.

Les installations locales peuvent servir au confort du développeur mais ne doivent pas devenir une dépendance nécessaire au projet.

---

# 22. Docker

Toute image ou dépendance Docker ajoutée doit être :

* justifiée ;
* compatible ;
* reproductible ;
* versionnée raisonnablement ;
* analysée pour la sécurité.

Éviter les tags flottants comme seule référence pour les composants critiques.

Les images applicatives de production doivent progressivement respecter :

```text
multi-stage build
runtime minimal
non-root user
no secrets in image
.dockerignore
healthcheck approprié
```

Ne jamais stocker un secret dans une image Docker.

---

# 23. Frontend Angular

Architecture générale à conserver :

```text
frontend/src/app/
├── core/
├── shared/
└── features/
```

Utiliser :

```text
standalone components
lazy loading
Reactive Forms
HttpClient
RxJS
Signals
```

Signals :

```text
state local
state UI
derived state
loading state simple
```

RxJS :

```text
HTTP
streams asynchrones
combinaisons
events
orchestration réactive
```

Ne pas remplacer RxJS par Signals partout uniquement pour utiliser une technologie plus récente.

Ne pas détruire le design existant uniquement pour Angular Material.

Utiliser Material lorsqu'il améliore réellement :

```text
dialogs
menus
forms
buttons
tooltips
snackbars
tables/pagination
```

---

# 24. UX / SPA

Taskira doit rester une vraie SPA fluide.

Éviter :

```javascript
window.location.reload()
```

pour synchroniser l'interface après :

* création ;
* modification ;
* suppression ;
* archivage ;
* changement de statut ;
* affectation ;
* commentaire.

Préférer :

```text
mise à jour d'état
RxJS
Signals
refresh ciblé de données
optimistic/pessimistic UI selon le cas
loading states
error states
toast/snackbar
dialog de confirmation
```

Les transitions doivent être rapides, compréhensibles et user-friendly.

---

# 25. Git — règles générales

Inspecter l'état Git avant et après chaque intervention.

Toujours préserver :

* les changements utilisateur ;
* les modifications d'un autre agent ;
* les fichiers non liés à la tâche actuelle.

Ne pas reformater massivement des fichiers non concernés.

Ne pas mélanger sans nécessité :

```text
upgrade technologique
refactor architectural
nouvelle feature métier
formatage massif
```

dans un même commit.

Ne jamais utiliser une commande Git destructive pour « nettoyer » le travail d'autrui.

Interdits sans besoin exceptionnel et autorisation explicite :

```text
git reset --hard
git clean -fd
git push --force
git push --force-with-lease
réécriture arbitraire de l'historique
```

---

# 26. Git — branches, commits et Pull Requests

`main` est protégée.

Ne jamais développer directement une phase substantielle sur `main`.

Pour une phase ou une évolution importante, créer une branche dédiée, par exemple :

```text
feat/phase9-audit-logging
feat/project-unarchive
fix/session-expiration
chore/dependency-update
```

Les agents sont autorisés, lorsqu'ils disposent des permissions nécessaires et que la tâche demande une exécution autonome, à :

```text
créer une branche
modifier le code
exécuter les tests
corriger les erreurs
créer des commits atomiques
push la branche
ouvrir une Pull Request
mettre à jour la Pull Request
```

Ne jamais pousser directement sur `main`.

Les commits doivent être atomiques et porter une intention claire.

Exemples :

```text
feat(audit): add audit event persistence
feat(logging): add request correlation IDs
test(audit): cover project audit events
docs: record phase 9 audit architecture
```

Avant chaque commit :

```bash
git status
git diff
```

Les tests pertinents doivent être verts.

---

# 27. Merge des Pull Requests

Ne jamais contourner la protection de `main`.

Ne jamais utiliser l'option GitHub permettant de bypass les checks uniquement pour accélérer une migration.

Une Pull Request ne doit être fusionnée qu'après réussite des checks obligatoires applicables.

Le merge final dans `main` reste manuel par défaut.

Un agent ne fusionne lui-même une Pull Request que si le propriétaire le demande explicitement dans la tâche en cours.

Après fusion :

1. synchroniser `main` local ;
2. vérifier le commit de merge ;
3. supprimer éventuellement la branche devenue inutile ;
4. mettre à jour les documents de migration si nécessaire ;
5. passer à la phase suivante seulement après validation réelle.

---

# 28. GitHub Actions

GitHub Actions est la CI principale.

Les workflows existants doivent rester cohérents et éviter les builds inutilement dupliqués.

Les checks critiques comprennent notamment :

```text
backend tests
frontend tests
lint
integration tests
Testcontainers
Docker builds
Playwright
SonarQube
CodeQL
Trivy
CI Gate
```

Lorsqu'un workflow distant échoue :

1. lire le job ;
2. lire les logs ;
3. identifier la cause ;
4. reproduire localement si pertinent ;
5. corriger ;
6. push le correctif ;
7. attendre/revérifier le workflow.

Ne jamais contourner un check critique sans avoir compris son échec.

---

# 29. Runners GitHub

Le repository Taskira peut être public.

Ne jamais transformer le poste Windows personnel en runner GitHub Actions persistant accessible aux Pull Requests publiques.

Utiliser en priorité :

```text
GitHub-hosted runners
```

Un self-hosted runner éventuel doit rester :

```text
isolé
éphémère
lab uniquement
sans accès sensible au poste hôte
```

---

# 30. Secrets

Interdit dans Git :

```text
password
DB secret
SMTP password
API token
GitHub token
private key
Azure secret
session secret
credentials personnels
```

Utiliser :

```text
environment variables
.env locaux ignorés
GitHub Secrets
configuration example sans secret réel
```

Ne jamais mettre un vrai secret dans :

* fixture ;
* test ;
* log ;
* README ;
* ADR ;
* Dockerfile ;
* Compose versionné ;
* capture d'exemple ;
* rapport.

---

# 31. Logs

Utiliser :

```text
SLF4J
Logback
```

Les logs applicatifs doivent progressivement inclure selon le contexte :

```text
timestamp
level
service
requestId
logger
action
user identifier non sensible si pertinent
message
exception
```

À partir de la Phase 9, les requêtes doivent pouvoir être corrélées via un request ID.

Ne jamais logger :

```text
password
TASKIRA_SESSION
CSRF token
Authorization secret
DB password
private key
données personnelles inutiles
```

---

# 32. Audit métier

L'audit métier doit être séparé des logs techniques.

Les événements d'audit peuvent notamment concerner :

```text
USER_CREATED
USER_DISABLED

PROJECT_CREATED
PROJECT_UPDATED
PROJECT_ARCHIVED
PROJECT_UNARCHIVED
PROJECT_DELETED

PROJECT_MEMBER_ADDED
PROJECT_MEMBER_REMOVED

TICKET_CREATED
TICKET_UPDATED
TICKET_ASSIGNED
TICKET_STATUS_CHANGED
TICKET_DELETED

COMMENT_CREATED
COMMENT_UPDATED
COMMENT_DELETED
```

Ne jamais stocker de secret dans les métadonnées d'audit.

L'audit doit rester testable, exploitable et indépendant du simple niveau de log.

---

# 33. Request / Correlation ID

À partir de la Phase 9, utiliser un identifiant de corrélation pour les requêtes HTTP.

Convention prévue :

```text
X-Request-ID
```

Si le client n'en fournit pas :

```text
générer un UUID
```

Le request ID doit pouvoir être ajouté au MDC Logback et, si prévu par l'implémentation, renvoyé dans la réponse.

Ne jamais faire confiance à une valeur client sans validation raisonnable si elle est utilisée dans les logs.

---

# 34. Nouvelles fonctionnalités métier

Après stabilisation des phases techniques nécessaires, les fonctionnalités métier manquantes comprennent notamment :

```text
désarchivage projet
suppression projet
suppression ticket
désactivation utilisateur
réactivation utilisateur
suppression utilisateur selon règles historiques
permissions avancées
UX confirmations
```

Ne pas forcer ces fonctionnalités dans une phase technique qui ne les concerne pas.

Pour une nouvelle fonctionnalité métier importante, appliquer autant que possible :

```text
règle métier
↓
tests
↓
backend
↓
integration
↓
frontend
↓
Vitest
↓
Playwright
↓
CI
↓
Sonar / sécurité selon pertinence
↓
PR
```

---

# 35. Suppressions et historique

Ne jamais utiliser aveuglément :

```text
CascadeType.REMOVE
ON DELETE CASCADE
```

sur un agrégat complexe uniquement pour faciliter une suppression.

Avant toute suppression :

1. inspecter les FK ;
2. inspecter les relations JPA ;
3. déterminer les données historiques à conserver ;
4. définir les règles métier ;
5. écrire les tests ;
6. implémenter explicitement la stratégie.

Pour les utilisateurs, préférer une désactivation lorsque l'utilisateur possède un historique métier devant être conservé.

Suppression définitive uniquement selon une règle claire et testée.

---

# 36. Events métier

Utiliser Spring Application Events uniquement lorsqu'ils réduisent réellement le couplage.

Exemples possibles :

```text
ProjectCreatedEvent
ProjectArchivedEvent
ProjectUnarchivedEvent
ProjectDeletedEvent

TicketCreatedEvent
TicketAssignedEvent
TicketStatusChangedEvent
TicketDeletedEvent

CommentCreatedEvent

UserCreatedEvent
UserDisabledEvent
```

Ne pas transformer tous les appels de services en événements.

Les événements internes ne justifient pas l'introduction de Kafka ou RabbitMQ.

---

# 37. Infrastructure future

Les technologies prévues dans les phases suivantes doivent être introduites progressivement.

Ordre général :

```text
Actuator
Micrometer
Prometheus
Grafana
Nginx production
Mailpit
Attachments
Tika
Storage abstraction
Exports
Spring Batch
GHCR
Staging
Backup / Restore
Kubernetes Lab
Helm Lab
Azure Lab
Optional labs
```

Chaque outil doit répondre à un besoin réel ou à un objectif pédagogique clairement isolé.

Ne pas transformer Taskira en démonstrateur artificiel contenant toutes les technologies du marché dans son runtime principal.

---

# 38. Kubernetes, Helm et Azure

Kubernetes, Helm et Azure sont principalement des environnements de formation et de démonstration pour Taskira.

Ils ne remplacent pas le runtime Docker Compose principal sans décision explicite.

Kubernetes :

```text
lab réel
kind / k3d / minikube selon besoin
manifests
probes
services
ingress
PVC
rolling update
rollback
```

Helm :

```text
chart
values
helm lint
helm template
déploiement local
```

Azure :

```text
architecture
scripts
configuration
documentation
```

Ne jamais créer automatiquement une infrastructure cloud payante importante sans autorisation explicite et credentials disponibles.

Ne jamais exécuter automatiquement :

```text
terraform apply
```

créant des ressources payantes sans demande explicite.

---

# 39. Gestion des erreurs pendant le travail

Lorsqu'une commande, un build, un test, Docker ou un workflow échoue :

1. lire les logs ;
2. déterminer la cause réelle ;
3. reproduire si nécessaire ;
4. corriger ;
5. relancer ;
6. retester ;
7. documenter si le problème est significatif ;
8. continuer lorsque la validation est verte.

Ne pas demander au propriétaire d'exécuter une commande qu'un agent peut exécuter lui-même.

Une intervention humaine ne doit être demandée que lorsqu'elle est réellement nécessaire, par exemple :

* authentification interactive ;
* MFA ;
* validation GitHub manuelle nécessaire ;
* création de ressource payante ;
* achat de domaine ;
* redémarrage Windows ;
* secret inaccessible à l'agent ;
* permission externe manquante.

Dans ce cas, continuer tout le travail indépendant possible.

---

# 40. Règle d'autonomie

Lorsqu'une tâche demande explicitement à l'agent de continuer une phase complète ou plusieurs phases :

```text
inspect
implement
test
fix
commit
push
PR
continue
```

sans demander systématiquement :

> Voulez-vous que je continue ?

Une phase verte peut être suivie automatiquement par le travail suivant autorisé.

Toutefois :

* ne jamais contourner `main` protégée ;
* ne jamais fusionner automatiquement une PR sans autorisation explicite ;
* ne jamais créer de dépense cloud ;
* ne jamais effectuer une opération destructive irréversible sans nécessité et validation.

---

# 41. Interdiction des faux succès

Il est interdit de rendre artificiellement une migration verte en :

```text
désactivant Spring Security
désactivant CSRF
désactivant Flyway
passant ddl-auto à create/update
supprimant un test valide
ignorant les erreurs TypeScript
utilisant --force aveuglément
abaissant artificiellement les seuils
ignorant un workflow distant rouge
```

Une validation doit correspondre à un comportement réellement fonctionnel.

---

# 42. Critère de fin d'une phase

Une phase n'est marquée terminée que lorsque les éléments applicables ont été réellement :

```text
implémentés
testés
corrigés
retestés
documentés
commités
poussés
validés par CI si applicable
fusionnés si la phase est annoncée comme présente dans main
```

Le rapport de migration doit faire la différence entre :

```text
planifié
implémenté localement
pushé
PR ouverte
CI verte
fusionné dans main
```

Ne jamais confondre ces états.

---

# 43. Mise à jour documentaire

Après une phase importante, mettre à jour si nécessaire :

```text
AGENTS.md
ENTERPRISE_MIGRATION_REPORT.md
MIGRATION_MATRIX.md
docs/migration-matrix.md
docs/architecture/
docs/adr/
README.md
```

Ne pas surcharger `AGENTS.md` avec tous les détails historiques.

Les détails longs, incidents, résultats de scans et statistiques de tests appartiennent principalement à :

```text
ENTERPRISE_MIGRATION_REPORT.md
```

`AGENTS.md` doit rester la référence opérationnelle durable.

---

# 44. Documentation de référence finale

Architecture :

```text
docs/architecture.md
docs/architecture/
```

Stratégie de tests :

```text
docs/testing-strategy.md
```

Matrice fonctionnelle / modules :

```text
MIGRATION_MATRIX.md
```

Feuille de route des phases :

```text
docs/migration-matrix.md
```

Journal cumulatif et état vérifié :

```text
ENTERPRISE_MIGRATION_REPORT.md
```

Décisions structurantes :

```text
docs/adr/
```

---

# 45. Règle finale

Le but n'est pas de cocher artificiellement des technologies.

Chaque outil doit avoir :

```text
un rôle
une justification
des tests ou une validation
une documentation adaptée
```

Le runtime principal doit rester simple et cohérent :

```text
Angular
Nginx
Spring Boot
PostgreSQL
```

avec l'écosystème principal :

```text
Docker
GitHub Actions
SonarQube
CodeQL
Trivy
Prometheus
Grafana
```

et les tests :

```text
JUnit
Mockito
AssertJ
MockMvc
Testcontainers
Vitest
Playwright
```

Les technologies telles que :

```text
Kubernetes
Helm
Azure
OAuth2/OIDC
Entra ID
Loki
Redis
RabbitMQ
Kafka
Elasticsearch/OpenSearch
Terraform
```

doivent rester des évolutions justifiées ou des labs tant qu'aucun besoin réel ne justifie leur présence dans le runtime principal.

Le résultat attendu est un projet :

```text
moderne
modulaire
testable
observable
sécurisé
documenté
containerisé
déployable
versionné
rollbackable
CI/CD
enterprise-like
```

Toujours privilégier :

```text
simplicité
cohérence
sécurité
tests
maintenabilité
réversibilité
valeur métier
```

plutôt que la complexité technique pour elle-même.
