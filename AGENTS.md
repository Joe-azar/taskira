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

Terminée localement sur `feat/phase9-audit-logging`, pas encore fusionnée dans `main`.

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

Validation locale :

```text
65 tests backend (29 rapides + 36 intégration)
25 tests Vitest
10/10 Playwright
```

Ces nombres sont historiques et augmenteront normalement avec les futures phases.

---

## Phases 10 à 20

Planifiées mais non considérées comme terminées tant que leur implémentation et leur validation ne sont pas réellement présentes.

Roadmap générale :

```text
Phase 10  Actuator + Micrometer + Prometheus + Grafana
Phase 11  Nginx + Docker production + production-like
Phase 12  Notifications + Mailpit
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
