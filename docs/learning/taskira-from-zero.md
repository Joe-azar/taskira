# Taskira, de zéro — mon manuel personnel

Ce document est un manuel d'apprentissage écrit pour Joe, propriétaire de Taskira, afin de comprendre
en profondeur tout ce qui a été construit dans ce dépôt. Il est différent de `docs/architecture/` :
- `docs/architecture/` décrit l'architecture pour quelqu'un qui la connaît déjà.
- `docs/learning/` explique tout depuis zéro, avec des mots simples, pour apprendre.

Construit progressivement, partie par partie, avec Claude Code.

## Sommaire

- [x] Partie 0 — Vue générale (voir l'audit d'inventaire de la session)
- [x] Partie 1 — Lancer Taskira et comprendre Docker
- [x] Partie 2 — Backend + PostgreSQL + Flyway
- [x] Partie 3 — Frontend Angular
- [x] Partie 4 — Authentification et sécurité
- [x] Partie 5 — Tests
- [x] Partie 6 — GitHub Actions + SonarQube + CodeQL + Trivy
- [x] Partie 7 — Observabilité
- [x] Partie 8 — Nginx + production-like + GHCR + release/staging
- [x] Partie 9 — Backup / restore
- [x] Partie 10 — Kubernetes + kind
- [x] Partie 11 — Helm
- [x] Partie 12 — Terraform + Azure
- [x] Partie 13 — Diagnostic
- [x] Partie 14 — Préparation entretien

---

## Partie 1 — Lancer Taskira et comprendre Docker

### A. Docker expliqué depuis zéro

**Image** — Un modèle figé, en lecture seule, qui contient tout ce qu'il faut pour faire tourner un
programme : le code, les bibliothèques, le système de fichiers minimal. Comme un CD-ROM
d'installation : on ne le modifie jamais, on l'utilise pour installer quelque chose.
→ `postgres:18.6-alpine3.23` est une image. Elle contient PostgreSQL 18.6 déjà installé sur un
mini-Linux (Alpine).

**Container** — Une instance *en cours d'exécution* d'une image. Si l'image est le CD-ROM, le
container est l'installation réellement lancée, avec de la mémoire, un processus qui tourne, des
fichiers qui peuvent changer.
→ `taskira-postgres` est le container qui tourne actuellement, créé à partir de cette image.

**Dockerfile** — Une recette écrite, étape par étape, qui explique comment *construire* une image à
partir de zéro.
→ `backend/Dockerfile` explique comment fabriquer l'image du backend Spring Boot.

**Docker Compose** — Un chef d'orchestre qui lit un seul fichier (`docker-compose.yml`) et
démarre/arrête *plusieurs* containers ensemble, dans le bon ordre, avec le bon réseau entre eux.
→ `infra/docker-compose.yml` démarre 6 containers (`postgres`, `backend`, `frontend`, `mailpit`,
`prometheus`, `grafana`) d'un coup.

**Volume** — Un espace de stockage géré par Docker, qui *survit* même si on supprime le container.
→ `taskira_postgres_data_pg18` est le volume où vivent réellement les utilisateurs, projets et
tickets.

**Réseau (network)** — Une bulle virtuelle dans laquelle plusieurs containers peuvent se parler par
leur *nom* (pas par une IP à retenir).
→ Tous les containers de `infra/docker-compose.yml` sont sur `infra_default`. Le backend joint
PostgreSQL en écrivant simplement `postgres` dans son URL de connexion.

**Port** — Une "porte numérotée" par laquelle un programme accepte des connexions réseau. `8080:8080`
veut dire : "le port 8080 de Windows est relié au port 8080 à l'intérieur du container."

**Bind mount** — Un type de volume qui relie un dossier réel du disque à un dossier dans le
container, en direct, dans les deux sens.
→ `../frontend:/app` dans le service `frontend` — modifier un fichier `.ts` sur le disque est vu
*instantanément* par le container (rechargement automatique).

**Healthcheck** — Une commande que Docker répète automatiquement pour savoir si un container est
"vivant" ET "sain" (il répond correctement, pas juste allumé).
→ Le healthcheck de `postgres` exécute `pg_isready -U taskira -d taskira` toutes les 5 secondes.

**Environnement (variables d'environnement)** — Des informations de configuration injectées dans un
container au démarrage, sans modifier le code.
→ `SPRING_PROFILES_ACTIVE: dev` dit au backend "démarre en mode développement" sans toucher au Java.

**Registry** — Un entrepôt en ligne où des images Docker toutes prêtes sont stockées.
→ `ghcr.io` héberge les images officielles publiées de Taskira (détail en Partie 8).

**Tag** — Une étiquette texte sur une image (`18.6-alpine3.23`, `v0.1.0`). Un tag peut changer de
contenu avec le temps.

**Digest** — Une empreinte cryptographique unique (`sha256:...`) qui identifie le contenu *exact*
d'une image, garantie d'immuabilité que le tag seul n'offre pas.
→ Partout dans ce dépôt : `postgres:18.6-alpine3.23@sha256:f65cfc...` — le tag pour la lisibilité
humaine, le digest pour la garantie technique.

**Multi-stage build** — Un Dockerfile avec *plusieurs* `FROM`, où seules les dernières étapes finissent
dans l'image finale. Permet d'utiliser de gros outils de compilation puis de les jeter.
→ `backend/Dockerfile` : `FROM ...jdk AS build` (compile avec Maven) puis `FROM ...jre` (image
finale, juste le `.jar` et un runtime Java minimal).

**Utilisateur non-root** — Par défaut, un processus dans un container tourne en `root`. Faire tourner
l'application avec un utilisateur normal limite les dégâts en cas de faille exploitée.
→ `backend/Dockerfile` crée l'utilisateur `taskira` (uid 10001) et termine par `USER taskira`.

### B. Les fichiers Docker de Taskira

| Fichier | Rôle |
|---|---|
| `infra/docker-compose.yml` | Orchestre les 6 containers du développement local |
| `backend/Dockerfile` | Construit l'image du backend (multi-stage, non-root) |
| `frontend/Dockerfile` | Image frontend en mode **développement** (hot reload) |
| `frontend/Dockerfile.prod` | Image frontend en mode **production** (Angular compilé + Nginx) — Partie 8 |
| `backend/.dockerignore`, `frontend/.dockerignore` | Ce qu'il ne faut jamais copier dans l'image |

`infra/docker-compose.prodlike.yml` et `infra/docker-compose.staging.yml` existent aussi — vus en
Partie 8. Cette partie ne couvre que `infra/docker-compose.yml`, celui du quotidien.

### C. Que se passe-t-il avec `docker compose up -d` ?

```powershell
docker compose -f infra/docker-compose.yml up -d
```

1. Compose lit le fichier, repère 6 services.
2. Il crée le réseau `infra_default` s'il n'existe pas.
3. Il crée les volumes nommés s'ils n'existent pas — **s'ils existent déjà, ils sont réutilisés, les
   données ne sont jamais effacées par un simple `up`.**
4. Pour `backend`/`frontend` (qui ont un `build:`), Compose construit l'image si besoin.
5. `postgres` démarre en premier ; `backend` attend qu'il soit *healthy* (`depends_on: condition:
   service_healthy`), pas juste démarré.
6. Puis `frontend`, puis `prometheus` (dépend de `backend`), puis `grafana` (dépend de `prometheus`).
7. Le `-d` ("detached") rend la main immédiatement ; les containers continuent en arrière-plan.

### D. Container par container

**`taskira-postgres`** — image `postgres:18.6-alpine3.23` · port `5432:5432` · réseau `infra_default`
· volume `taskira_postgres_data_pg18` sur `/var/lib/postgresql` (les vraies données) · aucune
dépendance · healthcheck `pg_isready`.
- Logs : `docker logs taskira-postgres`
- Entrer (SQL) : `docker exec -it taskira-postgres psql -U taskira -d taskira`
- Redémarrer : `docker restart taskira-postgres`
- Santé : `docker inspect --format='{{.State.Health.Status}}' taskira-postgres`

**`taskira-backend`** — construit depuis `backend/Dockerfile`, taguée `infra-backend` · port `8080`
publié, **`9091` (Actuator) jamais publié** · volume `taskira_attachments_data` · dépend de `postgres`
healthy · healthcheck `wget` sur `/actuator/health/readiness`.
- Logs : `docker logs -f taskira-backend`
- Entrer : `docker exec -it taskira-backend sh` (pas de bash dans cette image)
- Redémarrer : `docker restart taskira-backend`

**`taskira-frontend`** — construit depuis `frontend/Dockerfile` (dev, pas `.prod`) · port `4200` ·
bind mount `../frontend:/app` (rechargement automatique) · dépend de `backend` (existe, pas
forcément healthy) · **pas de healthcheck défini**.
- Logs : `docker logs -f taskira-frontend`
- Entrer : `docker exec -it taskira-frontend sh`

**`taskira-mailpit`** — `axllent/mailpit:v1.30.7` · port `8025` (interface web) publié, `1025` (SMTP)
non publié · healthcheck `/mailpit readyz`.

**`taskira-prometheus`** — `prom/prometheus:v3.13.2` · port `9090` · bind mount
`./prometheus/prometheus.yml:...:ro` (lecture seule) · volume `taskira_prometheus_data` · dépend de
`backend` · pas de healthcheck défini.

**`taskira-grafana`** — `grafana/grafana:13.1.3` · port `3000` · identifiants par défaut
`admin`/`taskira` · dépend de `prometheus`.

### E. Commandes Docker à connaître

| Commande | Ce qu'elle fait |
|---|---|
| `docker ps` | Containers en cours d'exécution |
| `docker ps -a` | Tous les containers, même arrêtés |
| `docker images` | Toutes les images sur la machine |
| `docker volume ls` | Liste les volumes |
| `docker network ls` | Liste les réseaux |
| `docker logs <nom>` | Logs d'un container |
| `docker logs -f <nom>` | Logs en direct |
| `docker exec -it <nom> <commande>` | Exécute une commande dans un container démarré |
| `docker inspect <nom>` | Configuration technique complète (JSON) |
| `docker compose ps` | `docker ps` limité aux services du fichier Compose |
| `docker compose logs` | Logs de tous les services Compose |
| `docker compose stop` | Arrête sans supprimer |
| `docker compose start` | Redémarre ce qui a été arrêté |
| `docker compose down` | Supprime containers + réseau — **garde les volumes** |
| `docker compose down -v` | Supprime containers + réseau + **volumes** ⚠️ |

### F. `down` vs `down -v` — règle à ne jamais oublier

- `docker compose down` → supprime les containers. Les volumes (`taskira_postgres_data_pg18` etc.)
  survivent. Un `up -d` juste après retrouve exactement les mêmes données.
- `docker compose down -v` → supprime aussi les volumes. **Efface définitivement** toutes les
  données de développement. Pas de corbeille, pas d'annulation.

**Règle : ne jamais ajouter `-v` sauf décision consciente de repartir d'une base 100 % vide.**

### G. Erreurs possibles

| Erreur | Cause probable |
|---|---|
| `port is already allocated` | Un autre programme (ou un vieux container Taskira) utilise déjà ce port |
| Service bloqué sur `starting` | Son healthcheck échoue en boucle — regarder `docker logs` |
| `Cannot connect to the Docker daemon` | Docker Desktop n'est pas lancé |
| Le frontend ne recharge pas | Bind mount ou polling en souci (rare) |
| `volume already exists but was not created by Docker Compose` | Avertissement normal, pas une erreur |

### Résumé à retenir

1. Image = modèle figé ; container = instance en cours d'exécution.
2. Docker Compose démarre plusieurs containers ensemble, dans l'ordre, sur le même réseau.
3. Les volumes conservent les données même si les containers sont supprimés.
4. `down` est sûr ; `down -v` efface les données de développement pour de bon.
5. Un healthcheck dit si un container est vraiment prêt, pas juste allumé.

### Quiz Partie 1

1. Différence entre une image et un container ?
2. Pourquoi le port `9091` du backend n'est-il jamais publié vers Windows ?
3. Après `docker compose down` puis `up -d`, les tickets créés hier sont-ils toujours là ? Pourquoi ?
4. À quoi sert `AS build` dans `backend/Dockerfile` ?
5. Pourquoi créer un utilisateur `taskira` plutôt que de laisser tourner l'app en `root` ?

Réponses : voir la conversation avec Claude Code de cette session, ou raisonner à partir des
explications ci-dessus.

---

## Partie 2 — Backend Spring Boot + PostgreSQL + Flyway

### A. Spring Boot expliqué depuis zéro

**Application Spring Boot** — Un programme Java qui démarre lui-même un serveur web intégré (pas
besoin d'installer Tomcat à part). Un seul fichier `.jar` exécutable contient tout.
→ `java -jar app.jar` (l'`ENTRYPOINT` de `backend/Dockerfile`) lance toute l'API.

**Controller** — La porte d'entrée HTTP. Reçoit une requête, vérifie sa forme, délègue le vrai
travail à un Service. Ne doit jamais contenir de logique métier (AGENTS.md §9).
→ `TicketController.createTicket` ne fait qu'appeler `ticketService.createTicket(request)`.

**Service** — Là où vit la logique métier et les transactions.
→ `TicketService` porte `@Transactional` sur la classe entière.

**Repository** — Parle à la base de données sans écrire le SQL basique soi-même.
→ `TicketRepository extends JpaRepository<Ticket, Long>` : Spring Data JPA génère `save`,
`findById`, `deleteById`... automatiquement.

**Entity** — Une classe Java qui représente une ligne d'une table SQL.
→ `Ticket.java`, `@Entity @Table(name = "tickets")`.

**DTO (Data Transfer Object)** — Transporte des données entre frontend et backend en HTTP, jamais
l'Entity elle-même.
→ `CreateTicketRequest` (entrée) ≠ `Ticket` (stockage) ≠ `TicketResponse` (sortie).

**Bean** — Un objet dont la création est déléguée à Spring plutôt que fait à la main partout.

**Dependency Injection** — Les dépendances sont données à une classe de l'extérieur, via le
constructeur (jamais `@Autowired` sur un champ, interdit par AGENTS.md §9).
→ `TicketController` reçoit `TicketService` via un constructeur généré par Lombok
(`@RequiredArgsConstructor`).

**Configuration / Profile** — Des réglages qui changent selon l'environnement sans dupliquer le
code.
→ `application.yaml` (commun) + `application-dev.yaml`/`-prod.yaml`/`-test.yaml` (par profil).

**Transaction** — Un groupe d'opérations en base qui réussissent ou échouent toutes ensemble.

**JPA / Hibernate** — JPA est la spécification ; Hibernate est l'implémentation réellement utilisée.

**Exception / ProblemDetail** — Format d'erreur HTTP standardisé (RFC 9457) avec `status`, `title`,
`detail`, et un `requestId` de corrélation - jamais un texte d'erreur brut.

**Actuator** — Endpoints techniques ajoutés par Spring Boot (`/actuator/health`,
`/actuator/prometheus`...) - détaillé en Partie 7.

**Port 8080 vs port 9091** — 8080 sert l'API métier ; 9091 sert uniquement les endpoints
techniques, jamais publié à l'extérieur du réseau Docker.

### B. Fichiers concernés

| Fichier | Rôle |
|---|---|
| `backend/src/main/resources/application.yaml` | Réglages communs |
| `backend/src/main/resources/application-dev.yaml` | Dev (logs DEBUG, SQL affiché, bootstrap admin) |
| `backend/src/main/resources/application-prod.yaml` | Prod (cookie Secure, logs JSON, Swagger off) |
| `backend/src/main/resources/application-test.yaml` | Tests (logs silencieux) |
| `backend/src/main/resources/db/migration/V*.sql` | Les 10 migrations Flyway |
| `backend/src/main/java/com/joe/taskira/<module>/` | `controller/`, `service/`, `repository/`, `entity/`, `dto/` par module |

### C. Exemple réel : l'entité `Ticket`

```java
@Entity
@Table(name = "tickets", uniqueConstraints = {
        @UniqueConstraint(name = "uk_tickets_reference", columnNames = "reference")
})
public class Ticket extends AuditableEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private TicketStatus status;
}
```

- `@ManyToOne` : un ticket appartient à un projet ; un projet a plusieurs tickets.
- `fetch = FetchType.LAZY` : le projet lié n'est chargé que si le code y accède réellement.
- `@Enumerated(EnumType.STRING)` : le statut est stocké en texte lisible, pas en numéro.

`AuditableEntity` (`common/audit/AuditableEntity.java`) donne à toutes les entités :
```java
@Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
@CreatedDate private Instant createdAt;
@LastModifiedDate private Instant updatedAt;
@Version private Integer version;   // verrouillage optimiste
```

### D. Verrouillage optimiste — `@Version`

Deux personnes modifient le même ticket en même temps : sans protection, la deuxième sauvegarde
écraserait silencieusement la première. La colonne `version` (compteur entier) vérifie à chaque
`UPDATE` qu'elle n'a pas changé depuis la lecture ; sinon Taskira répond `409 Conflict`. Introduit en
P7 (`V7__add_optimistic_locking.sql`).

### E. PostgreSQL : où sont vraiment les données

Jamais dans le container `taskira-postgres` lui-même - dans le volume `taskira_postgres_data_pg18`.
Le container peut être détruit/recréé sans perte tant que ce volume existe.

```powershell
docker exec -it taskira-postgres psql -U taskira -d taskira
# \dt                              liste les tables
# \d tickets                       décrit une table
# SELECT * FROM users LIMIT 5;
# \q

docker exec -it taskira-postgres psql -U taskira -d taskira -c "SELECT count(*) FROM tickets;"
```

### F. Flyway — pourquoi V1, V2, V3...

Flyway garde une table `flyway_schema_history` listant les migrations déjà appliquées. Au démarrage,
il compare à `db/migration/` et applique uniquement les nouvelles, dans l'ordre.

| Migration | Contenu |
|---|---|
| V1 | `users` |
| V2 | `projects` |
| V3 | `project_members` |
| V4 | `tickets` |
| V5 | `comments` |
| V6 | `ticket_history` |
| V7 | colonne `version` (verrouillage optimiste) |
| V8 | `audit_events` |
| V9 | `attachments` |
| V10 | tables `BATCH_*` (Spring Batch) |

**Règle absolue (AGENTS.md §13) : on ne modifie jamais une migration déjà appliquée.** Flyway calcule
une empreinte (checksum) de chaque fichier ; la modifier après coup fait échouer le démarrage au
prochain lancement. Toute évolution = un nouveau fichier `Vn+1`.

`spring.jpa.hibernate.ddl-auto=validate` : Hibernate ne crée/modifie jamais le schéma tout seul - il
vérifie seulement la correspondance et refuse de démarrer sinon. Flyway est l'unique source de
vérité du schéma.

### G. Exercice pratique

```powershell
docker exec -it taskira-postgres psql -U taskira -d taskira -c "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;"
docker exec -it taskira-postgres psql -U taskira -d taskira -c "SELECT status, count(*) FROM tickets GROUP BY status;"
docker logs taskira-backend 2>&1 | Select-String "Flyway"
docker logs taskira-backend 2>&1 | Select-String "The following"
```

### H. Ce que tu dois observer

- 10 lignes dans `flyway_schema_history`, toutes `success = t`.
- `Successfully validated 10 migrations` dans les logs.
- `The following 1 profile is active: "dev"`.

### I. Erreurs possibles

| Erreur | Cause probable |
|---|---|
| `Validate failed: Migrations have failed validation` | Un fichier déjà appliqué a été modifié après coup |
| `Schema-validation: missing column` | Une entité référence une colonne jamais créée par Flyway |
| `relation "xxx" does not exist` | Faute de frappe, ou migration pas encore appliquée |

### Résumé à retenir

1. Controller (HTTP) → Service (logique + transactions) → Repository (BDD) → Entity (une ligne).
2. Un DTO n'est jamais l'Entity elle-même.
3. Flyway possède le schéma ; Hibernate le valide seulement (`ddl-auto=validate`).
4. Une migration appliquée est immuable ; toute évolution est un nouveau fichier `Vn+1`.
5. `@Version` protège contre deux modifications simultanées (409 au lieu d'écrasement silencieux).

### Quiz Partie 2

1. Pourquoi un Controller ne doit-il jamais contenir de logique métier ?
2. Pourquoi un DTO plutôt que l'Entity directement ?
3. Que se passe-t-il si on modifie `V3` après qu'elle a déjà tourné ?
4. Que `ddl-auto=validate` empêche-t-il concrètement ?
5. À quoi sert la colonne `version` de `V7` ?

---

## Partie 3 — Frontend Angular

### A. Angular expliqué depuis zéro

**SPA (Single Page Application)** — Une seule page HTML est chargée une fois ; tout le reste se fait
en JavaScript dans le navigateur, sans recharger la page depuis le serveur.

**Component** — Classe TypeScript (logique) + template HTML (affichage) + SCSS (style).
→ `LoginPage` (`login.page.ts`/`.html`/`.scss`).

**Standalone component** — Un component déclare directement ses dépendances (`imports: [...]`) sans
`NgModule` parent séparé.

**Service** — Logique réutilisable sans affichage, le plus souvent des appels HTTP.
→ `TicketService` (`@Injectable({ providedIn: 'root' })`).

**Routing** — Associe une URL à un component.
→ `app.routes.ts` : `{ path: 'tickets/:id', loadComponent: () => import(...) }`.

**Guard** — Décide si une navigation est autorisée avant qu'elle ait lieu.
→ `authGuard` (connecté ?), `adminGuard` (`/users` réservé aux admins).

**Interceptor** — S'insère automatiquement sur chaque requête HTTP sortante et sa réponse.
→ `authInterceptor` ajoute le cookie/en-tête CSRF et gère les `401`.

**Observable / RxJS** — Un flux de valeurs qui arrivent dans le temps.
→ `authService.login(...).pipe(finalize(...)).subscribe({ next, error })`.

**Reactive Forms** — L'état d'un formulaire est représenté par des objets TypeScript.
→ `this.fb.nonNullable.group({ email: [...], password: [...] })`.

**Lazy loading** — Le code d'une page n'est téléchargé qu'au moment d'y naviguer.

**Environment** — Config différente selon dev/prod.
→ dev : `apiUrl: 'http://localhost:8080/api/v1'` (URL absolue, ports différents). Prod :
`apiUrl: '/api/v1'` (URL relative, même origine que Nginx — Partie 8).

**Signal** — Une valeur réactive ; quand elle change, tout ce qui l'affiche se met à jour seul.
→ `readonly loading = signal(false);`.

### B. Fichiers concernés

| Fichier/dossier | Rôle |
|---|---|
| `frontend/src/app/app.routes.ts` | Toutes les routes |
| `frontend/src/app/core/guards/` | `auth.guard.ts`, `guest.guard.ts`, `admin.guard.ts` |
| `frontend/src/app/core/interceptors/auth.interceptor.ts` | Cookie + CSRF + gestion des 401 |
| `frontend/src/app/core/auth/auth.service.ts` | Connexion, déconnexion, utilisateur courant |
| `frontend/src/app/features/<domaine>/pages/` | Une page par écran |
| `frontend/src/app/features/<domaine>/services/` | Un service HTTP par domaine |
| `frontend/src/environments/environment*.ts` | Config selon build prod/dev |

### C. Structure réelle

```
frontend/src/app/
├── core/           singletons (auth, guards, intercepteur, badges partagés)
├── layout/         coque applicative (AppShellComponent)
└── features/       auth, comments, dashboard, projects, tickets, users
```

### D. Ce qui se passe vraiment

**Connexion** : `LoginPage.submit()` → `POST /api/v1/auth/login` → `authInterceptor` ajoute
`X-XSRF-TOKEN` → le backend répond avec `Set-Cookie: TASKIRA_SESSION=...` (géré par le navigateur,
Angular n'y touche jamais) → navigation vers `/dashboard`.

**Ouvrir un projet** : route `projects/:id` charge `ProjectDetailPage` en lazy loading → `GET
/api/v1/projects/{id}` via `HttpClient` → l'intercepteur ajoute cookie+CSRF automatiquement.

**Créer un ticket** : Reactive Forms valide avant l'envoi → `POST /api/v1/tickets` → mise à jour
locale de l'écran, **jamais** `window.location.reload()` (AGENTS.md §24).

### E. Exercice pratique

Ouvrir `http://localhost:4200`, F12 → onglet Network, recharger la page de login, inspecter une
requête vers `/api/v1/auth/me` : en-tête `X-XSRF-TOKEN` ajouté par l'intercepteur, cookie
`TASKIRA_SESSION` marqué `HttpOnly`.

### F. Erreurs possibles

| Erreur | Cause probable |
|---|---|
| Page blanche après connexion | Boucle guard/intercepteur (déjà rencontrée et corrigée en P8) |
| `Cannot GET /tickets/42` en rechargeant l'URL directement | Pas une erreur Angular - le serveur doit rediriger vers `index.html` (Partie 8) |
| 403 sur `POST`/`PUT`/`DELETE` | Cookie CSRF absent ou expiré (Partie 4) |
| Le formulaire ne s'envoie jamais | `form.invalid` reste vrai, un `Validator` bloque |

### Résumé à retenir

1. Une SPA change d'écran en JavaScript, sans recharger la page entière.
2. Component (affichage) / Service (logique+HTTP) / Guard (autorise une navigation) / Interceptor
   (agit sur toutes les requêtes).
3. Le lazy loading ne télécharge le code d'une page qu'à la navigation.
4. `environment.ts` change selon dev (URL absolue) ou prod (URL relative, même origine).
5. Taskira ne fait jamais `window.location.reload()` pour se mettre à jour (règle UX explicite).

### Quiz Partie 3

1. Pourquoi `environment.development.ts` utilise une URL absolue et `environment.ts` une URL relative ?
2. Différence de rôle entre un Guard et un Interceptor ?
3. Pourquoi `authGuard` appelle parfois `authService.fetchMe()` plutôt que lire une variable locale ?
4. Qu'est-ce que le lazy loading évite au premier chargement ?
5. Pourquoi éviter `window.location.reload()` après une création de ticket ?

---

## Partie 4 — Authentification et sécurité

**Taskira n'utilise plus JWT.** Retiré entièrement en Phase 8, remplacé par session serveur + cookie.

### A. Les concepts

**Session HTTP côté serveur** — Après connexion, le serveur note en mémoire "cet appareil = utilisateur
X" et donne un identifiant au navigateur (le cookie) pour le retrouver à chaque requête.

**Cookie `TASKIRA_SESSION`** — Renvoyé automatiquement par le navigateur à chaque requête vers Taskira.

**`HttpOnly`** — Interdit à tout JavaScript de lire le cookie - protection contre le vol de session
par une faille XSS.

**`SameSite=Lax`** — Le cookie n'est envoyé que depuis une navigation normale sur le site, pas depuis
une requête déclenchée par un site tiers.

**`Secure`** — Interdit d'envoyer le cookie hors HTTPS. Désactivé en dev/test (HTTP local), activé en
`prod`.

**CSRF** — Un site malveillant fait exécuter une action à ton insu sur un site où tu es déjà connecté.
Le navigateur enverrait le cookie de session automatiquement ; sans protection supplémentaire le
serveur ne peut pas distinguer une vraie action d'une action forgée.

**Cookie `XSRF-TOKEN` + en-tête `X-XSRF-TOKEN`** — Protection *double soumission* : un deuxième
cookie, lisible en JavaScript, que le frontend doit lire et recopier dans un en-tête sur toute
requête mutante. Un site tiers ne peut pas lire ce cookie d'un autre domaine pour forger l'en-tête.

**`SecurityFilterChain`** — La chaîne de règles Spring Security qui autorise ou non chaque requête
avant même d'atteindre un Controller.

**`AuthenticationManager` / BCrypt** — Vérifie email/mot de passe sans jamais comparer le mot de
passe en clair - BCrypt transforme chaque mot de passe en empreinte irréversible.

**Rôles `ADMIN`/`USER`** — Déterminent les droits de chaque compte.

**CORS** — Le navigateur bloque par défaut un appel JavaScript vers une autre origine ; le backend
doit explicitement autoriser l'origine du frontend.

**401 vs 403** — 401 = "je ne sais pas qui tu es" ; 403 = "je te connais, tu n'as pas le droit".

**Le backend est l'autorité de sécurité** — Un guard Angular n'est jamais une vraie protection :
n'importe qui peut appeler l'API directement (`curl`) en contournant le frontend (AGENTS.md §11).

### B. Schéma d'un LOGIN

```
1. Navigateur -> GET /api/v1/auth/me au premier chargement -> 401, mais un cookie
   XSRF-TOKEN est déposé même sur cet échec.
2. L'intercepteur Angular lit XSRF-TOKEN, l'ajoute en en-tête X-XSRF-TOKEN.
3. Navigateur -> POST /api/v1/auth/login + en-tête X-XSRF-TOKEN
   -> Spring Security vérifie le CSRF -> AuthenticationManager vérifie email+mot de
   passe (BCrypt) -> SecurityContext créé -> AuthController persiste la session
   -> réponse : Set-Cookie TASKIRA_SESSION (HttpOnly, SameSite=Lax, Secure selon profil)
4. Requêtes suivantes : le navigateur renvoie automatiquement TASKIRA_SESSION.
```

### C. Fichiers concernés

| Fichier | Rôle |
|---|---|
| `backend/.../security/config/SecurityConfig.java` | La chaîne de règles complète |
| `backend/.../config/CorsConfig.java` | Origines autorisées |
| `backend/.../auth/controller/AuthController.java` | `/auth/register`, `/auth/login`, `/auth/me` |
| `backend/.../security/service/CustomUserDetailsService.java` | Charge un utilisateur pour Spring Security |
| `frontend/.../interceptors/auth.interceptor.ts` | Pose l'en-tête CSRF sur chaque requête |
| `frontend/.../guards/*.ts` | `authGuard`, `guestGuard`, `adminGuard` |

### D. Ce que révèle `SecurityConfig.java`

```java
.authorizeHttpRequests(auth -> auth
        .requestMatchers(EndpointRequest.toAnyEndpoint()).permitAll()   // Actuator, port isolé
        .requestMatchers(ApiVersion.V1 + "/auth/**", "/v3/api-docs/**",
                          "/swagger-ui/**", "/swagger-ui.html").permitAll()
        .anyRequest().authenticated()   // tout le reste exige une session valide
)
```

Exactement 3 zones publiques (auth, Swagger, Actuator) ; tout le reste ferme par défaut (*deny by
default*).

### E. Exercice pratique

```powershell
curl.exe -i http://localhost:8080/api/v1/tickets                      # 401 attendu
curl.exe -i -c cookies.txt http://localhost:8080/api/v1/auth/me       # dépose XSRF-TOKEN
Get-Content cookies.txt
curl.exe -i -b cookies.txt -X POST http://localhost:8080/api/v1/auth/login `
  -H "Content-Type: application/json" `
  -d '{\"email\":\"admin@taskira.test\",\"password\":\"Taskira-Admin-42!\"}'  # 403 sans en-tête CSRF
```

### F. Erreurs possibles

| Erreur | Cause probable |
|---|---|
| `403` sur une requête mutante | En-tête `X-XSRF-TOKEN` manquant ou différent du cookie |
| `401` après connexion | Cookie `TASKIRA_SESSION` non envoyé (outil qui ne gère pas les cookies) |
| Connexion refusée, mot de passe correct | Compte désactivé (`active = false`) |
| `CORS error` en console | Origine appelante absente de `app.cors.allowed-origins` |

### Résumé à retenir

1. Session serveur + cookie, jamais de JWT - retiré entièrement en P8.
2. `HttpOnly` protège contre le JavaScript malveillant ; CSRF double-cookie protège contre les
   requêtes forgées par un tiers.
3. Le backend est la seule vraie autorité de sécurité.
4. Modèle "tout fermé par défaut" - seules `/auth/**`, Swagger et Actuator sont publics.
5. 401 = identité inconnue ; 403 = identité connue, droits insuffisants.

### Quiz Partie 4

1. Pourquoi le cookie de session est `HttpOnly` mais pas le cookie CSRF ?
2. Que se passerait-il sans protection CSRF si un attaquant forçait une requête `DELETE` pendant que
   tu es connecté ?
3. Pourquoi `Secure` est-il désactivé en développement local ?
4. Un guard Angular bloque `/users` visuellement pour un `USER` - est-ce suffisant côté sécurité ?
5. Différence entre 401 et 403 ?

---

## Partie 5 — Tests

### A. Les outils, un par un

**JUnit 5** — Fait tourner les tests Java (`@Test`).

**Mockito** — Crée de fausses dépendances (`@Mock`) pour tester une classe isolément.
→ `ProjectServiceTest` : `@Mock` simule `ProjectRepository`, `@InjectMocks` construit un vrai
`ProjectService` avec ces faux objets dedans.

**Spring Boot Test / MockMvc** — Simule une vraie requête HTTP sans démarrer de vrai serveur réseau.

**Testcontainers** — Démarre un vrai PostgreSQL dans un vrai container Docker, juste pour la durée
d'un test, puis le détruit.

**JaCoCo** — Mesure quel pourcentage du code a été exécuté par les tests (la couverture) - ne prouve
pas que les tests sont bons.

**Vitest** — L'équivalent JUnit côté frontend.

**Playwright** — Pilote un vrai navigateur pour simuler un vrai utilisateur, de bout en bout.

### B. Pourquoi Testcontainers alors que PostgreSQL tourne déjà dans docker-compose ?

`PostgreSqlIntegrationTest.java` (classe de base de tous les tests d'intégration) démarre un
PostgreSQL séparé (`taskira_test`), à chaque run. Trois raisons réelles :
1. **Isolation** : ne jamais toucher aux vraies données de dev.
2. **Reproductibilité** : sur GitHub Actions, aucun `docker-compose up` ne tourne déjà - le même
   test fonctionne identiquement partout.
3. **Migrations vérifiées pour de vrai** : base totalement vide, les 10 migrations Flyway
   s'appliquent depuis zéro, Hibernate valide le résultat - preuve réelle que `V1` à `V10`
   fonctionnent dans l'ordre, pas seulement sur une base de dev déjà peuplée progressivement.

### C. Fichiers et exemples réels

| Outil | Fichier exemple | Ce qu'il teste |
|---|---|---|
| JUnit + Mockito | `project/service/ProjectServiceTest.java` | Logique isolée |
| MockMvc | `project/controller/ProjectControllerTest.java` | Contrat HTTP |
| Testcontainers | `support/PostgreSqlIntegrationTest.java`, `TicketRepositoryIT.java`... | Vraie base, vrai contexte Spring |
| ModularityTests | `ModularityTests.java` | Frontières entre modules |
| Vitest | `core/auth/auth.service.spec.ts` | Service d'auth frontend |
| Playwright | `e2e/playwright/tests/*.spec.ts` | Parcours utilisateur réels |

Convention stricte : `*Test.java` (rapide, Surefire) vs `*IT.java` (intégration, Failsafe) - Surefire
exclut explicitement `**/*IT.java` dans `backend/pom.xml`.

### D. Lancer les tests

```powershell
docker build --target build -t taskira-backend-build backend
docker run --rm `
  -e TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal `
  -v /var/run/docker.sock:/var/run/docker.sock `
  -v taskira_maven_cache:/root/.m2 `
  -v "${PWD}\backend:/workspace" `
  -w /workspace `
  taskira-backend-build `
  ./mvnw verify

docker build -f frontend/Dockerfile -t taskira-frontend-tests frontend
docker run --rm taskira-frontend-tests npm run lint
docker run --rm taskira-frontend-tests npm run test:coverage
docker run --rm taskira-frontend-tests npm run build

& .\e2e\playwright\run.ps1
```

### E. Erreurs possibles

| Erreur | Cause probable |
|---|---|
| `Could not find a valid Docker environment` | Socket Docker pas monté (`-v /var/run/docker.sock:...`) |
| Un test échoue seulement parfois | Contention de ressources (plusieurs stacks Docker en même temps) |
| Playwright timeout générique | La stack de dev manuelle tourne en même temps - `docker compose stop` d'abord |

### Résumé à retenir

1. `*Test.java` (Surefire) vs `*IT.java` (Failsafe) - convention stricte, pas un détail.
2. Mockito isole une classe ; Testcontainers teste avec une vraie base jetable et isolée.
3. Testcontainers résout un vrai problème : isolation, reproductibilité, migrations vérifiées.
4. Un pourcentage de couverture mesure ce qui est exécuté, pas ce qui est bien testé.
5. Ne jamais citer un ancien nombre de tests - toujours relancer réellement.

### Quiz Partie 5

1. Pourquoi Testcontainers démarre une base différente plutôt que de réutiliser `taskira-postgres` ?
2. Différence entre `*Test.java` et `*IT.java` ?
3. Pourquoi un mock permet de tester `ProjectService` sans toucher une vraie base ?
4. 95% de couverture JaCoCo prouve-t-il qu'une classe est bien testée ?
5. Que vérifie Playwright que Vitest ne peut pas vérifier ?

---

## Partie 6 — GitHub Actions + SonarQube + CodeQL + Trivy

### A. Concepts

**GitHub Actions** — Automatisation intégrée à GitHub : à chaque `push`/PR, exécute des étapes sur
une machine temporaire (le runner).

**Workflow** — Fichier YAML (`.github/workflows/*.yml`) : quand se déclencher, quoi faire.

**Job** — Groupe d'étapes sur une machine virtuelle temporaire ; plusieurs jobs tournent en parallèle
sauf dépendance explicite (`needs`).

**Step** — Une seule action à l'intérieur d'un job.

**Runner** — La machine temporaire, détruite après chaque run.

**`checkout`** — Première étape quasi systématique : télécharger le code sur le runner.

**Artifact** — Un fichier produit pendant un run (rapport de test...), conservé après coup.

**Branch protection / required check** — `CI Gate` doit réussir avant de fusionner sur `main` -
techniquement impossible à contourner sans changer la configuration.

**Dependabot** — Robot qui ouvre une PR automatiquement quand une dépendance a une nouvelle version.

**`GITHUB_TOKEN`** — Jeton temporaire généré automatiquement par run, jamais un vrai mot de passe stocké.

### B. Les 6 workflows réels

| Fichier | Déclencheur | Rôle |
|---|---|---|
| `ci.yml` | push/PR + manuel | Compile, teste - check obligatoire pour fusionner |
| `quality.yml` | push/PR + manuel | SonarQube - qualité et dette technique |
| `codeql.yml` | push/PR + chaque lundi 3h17 + manuel | Analyse statique de sécurité |
| `security.yml` | push/PR + chaque lundi 4h42 + manuel | Trivy - CVE connues |
| `release.yml` | push d'un tag `v*.*.*` uniquement | Publie sur GHCR, déploie en staging, teste, publie une Release |
| `backup-restore-drill.yml` | chaque lundi 5h17 + manuel | Sauvegarde/restauration PostgreSQL testée |

### C. `ci.yml` en détail

```
push/PR
  ├── Backend    -> ./mvnw verify
  ├── Frontend   -> lint + Vitest + coverage + build Angular
  ├── Containers and E2E -> construit la stack E2E, Playwright, la détruit
  ▼ (les 3 en parallèle)
CI Gate (if: always(), needs: [backend, frontend, containers-and-e2e])
  -> réussit seulement si les 3 précédents ont réussi
```

`if: always()` : s'exécute même si un job précédent a échoué, pour signaler clairement l'échec
global plutôt que d'être silencieusement sauté.

### D. SonarQube

Cherche : **Bug** (erreur probable), **Vulnerability** (faille de sécurité), **Code smell** (signe de
code difficile à maintenir), **Duplication**, **Security hotspot** (à vérifier manuellement),
**Quality Gate** (seuils agrégés - un seul dépassé = rouge).

Pas de serveur permanent : `quality.yml` démarre un vrai SonarQube en Docker *le temps du run*,
analyse, lit le résultat, détruit tout. Pas de dashboard toujours accessible pour Taskira - seulement
les résultats de chaque run GitHub Actions.

### E. CodeQL

Outil propre à GitHub, spécialisé sur des schémas de vulnérabilités connus (injection SQL, XSS,
désérialisation dangereuse). Complémentaire à SonarQube, pas redondant. Résultats visibles dans
l'onglet Security → Code scanning alerts de GitHub, jamais en local.

### F. Trivy

**CVE** — Identifiant public standardisé d'une faille déjà connue et documentée.

Compare chaque dépendance et chaque couche d'image Docker à une base de CVE connues. **Une image peut
être vulnérable même si le code Java est parfait** - la faille peut venir d'une bibliothèque tierce
ou même d'un composant du système d'exploitation de l'image de base.

### G. Exercice pratique

```powershell
# Valider la syntaxe de tous les workflows sans rien exécuter
docker run --rm -v "${PWD}:/repo" -w /repo rhysd/actionlint:1.7.12
```

Sur GitHub : onglet Actions pour voir les runs récents, onglet Security pour les alertes
CodeQL/Trivy.

### H. Erreurs possibles

| Erreur | Cause probable |
|---|---|
| `CI Gate` rouge alors que Backend/Frontend sont verts | "Containers and E2E" a probablement échoué |
| Job rouge sur GitHub mais vert en local | Différence d'environnement (version d'outil, secret manquant) |
| SonarQube Quality Gate rouge | Nouveau bug/vulnérabilité/seuil de couverture non atteint |
| Trivy signale une CVE | Vérifier si elle concerne une dépendance directe ou un composant hors périmètre |

### Résumé à retenir

1. Workflow (quand) / job (groupe d'étapes) / step (une action) - sur une machine temporaire détruite
   après coup.
2. `CI Gate` est le seul check directement exigé par la protection de `main`.
3. SonarQube (qualité générale) et CodeQL (motifs de vulnérabilité précis) sont complémentaires.
4. Trivy peut signaler une CVE qui ne vient pas du code de Taskira lui-même.
5. Les scans de sécurité tournent aussi chaque semaine, sans changement de code.

### Quiz Partie 6

1. Pourquoi `CI Gate` a `if: always()` ?
2. Différence de rôle entre SonarQube et CodeQL ?
3. Pourquoi une image Docker peut être vulnérable même avec un code Java irréprochable ?
4. Pourquoi CodeQL/Trivy se déclenchent aussi chaque semaine ?
5. `release.yml` se déclenche-t-il à chaque push sur `main` ?

---

## Partie 7 — Observabilité

### A. Concepts

**Observabilité** — Pouvoir répondre à "que fait l'application maintenant, et va-t-elle bien ?" sans
lire le code ni se connecter en SSH - juste en regardant des chiffres exposés par l'application
elle-même.

**Actuator** — Module Spring Boot qui expose des endpoints techniques tout faits (`/health`, `/info`,
`/prometheus`...) sans que Taskira n'ait eu à les coder à la main.

**Micrometer** — Bibliothèque qui collecte des métriques (compteurs, jauges, temps de réponse) à
l'intérieur de l'application, dans un format neutre que plusieurs outils de monitoring savent lire.

**Métrique** — Un chiffre mesuré dans le temps. Deux formes utilisées par Taskira :
- **Compteur** (`Counter`) — ne fait qu'augmenter (ex. nombre total de tentatives de connexion).
- **Jauge** (`Gauge`) — monte et descend, reflète un état à l'instant présent (ex. nombre de tickets
  actuellement "EN COURS").

**Prometheus** — Un programme qui vient interroger ("scraper") régulièrement une URL de métriques
(ici toutes les 15 secondes) et garde l'historique dans le temps.

**Scraping** — L'action de Prometheus d'aller lire `/actuator/prometheus` à intervalle régulier - pas
Taskira qui pousse les données vers Prometheus, c'est l'inverse.

**Grafana** — Outil de tableaux de bord qui lit les données stockées par Prometheus et les affiche en
graphiques.

**Healthcheck / readiness / liveness** — Trois questions différentes :
- *Liveness* : "le processus est-il encore vivant ?" (sinon, le redémarrer).
- *Readiness* : "peut-il accepter du trafic maintenant ?" (ex. la connexion à PostgreSQL fonctionne).
- Un simple `healthcheck` Docker vérifie qu'une commande répond correctement, pas seulement que le
  container existe.

### B. Pourquoi un port séparé (9091) pour l'observabilité ?

Le port `8080` sert l'API métier (`/api/v1/...`). Le port `9091` (configurable via
`MANAGEMENT_SERVER_PORT`) sert uniquement Actuator. Cette séparation existe pour une raison de
sécurité concrète et documentée directement dans `application.yaml` :

```yaml
management:
  server:
    port: ${MANAGEMENT_SERVER_PORT:9091}
```

`infra/docker-compose.yml` ne publie **jamais** ce port vers l'hôte (contrairement à `8080`) - seul
un autre container du même réseau Docker (comme `prometheus`) peut l'atteindre. C'est la vraie
frontière de sécurité ici, pas un mot de passe applicatif : Taskira tourne en Docker Compose, pas en
Kubernetes, donc il n'existe pas de mécanisme d'authentification service-à-service à donner à
Prometheus - la séparation réseau fait ce travail à la place.

Piège réel rencontré pendant la Phase 10 (documenté dans `SecurityConfig.java`) : Spring Security ne
raisonne jamais par port, seulement par chemin d'URL. Une requête sur le port 9091 traverse la même
chaîne de sécurité que sur le port 8080 - sans une règle explicite, elle serait bloquée en `401`
même depuis l'intérieur du réseau Docker. D'où cette ligne, en tête des règles d'autorisation :

```java
.requestMatchers(EndpointRequest.toAnyEndpoint()).permitAll()
```

### C. Ce qui est exposé — et ce qui ne l'est pas

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
```

Liste blanche explicite, pas `*` (tout exposer). Des endpoints potentiellement dangereux comme `env`
(variables d'environnement, pourrait contenir un secret), `heapdump`, `shutdown` ou `threaddump`
restent invisibles même sur le port isolé.

`/actuator/health` distingue deux groupes :
```yaml
group:
  readiness:
    include: readinessState,db
  liveness:
    include: livenessState
```
`readiness` vérifie aussi la connexion PostgreSQL (`db`) - un backend démarré mais qui ne peut pas
parler à la base ne doit jamais être annoncé comme prêt à recevoir du trafic.

Un vrai bug trouvé en Phase 12 (documenté dans `application.yaml`) : ajouter `spring-boot-starter-mail`
active automatiquement un indicateur de santé pour le courrier, qui faisait passer `/actuator/health`
à `503` dès que Mailpit était injoignable - contraire à la philosophie "best-effort, jamais bloquant"
des notifications. Corrigé avec `management.health.mail.enabled: false`.

### D. Les métriques métier de Taskira

En plus de ce qu'Actuator/Micrometer fournissent automatiquement (mémoire JVM, temps de réponse HTTP,
threads...), Taskira expose ses propres métriques métier via `BusinessMetricsBinder`
(`backend/src/main/java/com/joe/taskira/config/BusinessMetricsBinder.java`) :

```java
Gauge.builder("taskira.tickets", ticketRepository, repo -> repo.countByStatus(status))
        .tag("status", status.name())
        .register(registry);
```

Une jauge par valeur d'énum : `taskira_tickets{status="..."}`, `taskira_projects{status="..."}`,
`taskira_users_active{role="..."}` - chacune ré-interroge réellement la base de données à chaque
scrape Prometheus (pas de cache), donc jamais périmée. `AuthService` incrémente aussi un compteur
`taskira_auth_login_attempts_total{result="success|failure"}` à chaque tentative de connexion.

Détail réel qui a piégé la Phase 10 : Micrometer retire automatiquement le suffixe `_total` d'un nom
de jauge (il est réservé aux compteurs) - nommer la jauge Java `taskira.tickets.total` aurait donné le
même nom scrapé (`taskira_tickets`) que `taskira.tickets`, mais de façon trompeuse pour qui lit le
code Java. D'où le nom Java sans suffixe, qui correspond exactement à ce qui est réellement scrapé.

### E. Fichiers concernés

| Fichier | Rôle |
|---|---|
| `backend/src/main/resources/application.yaml` | Config Actuator (port, endpoints exposés, groupes santé) |
| `backend/src/main/java/com/joe/taskira/config/BusinessMetricsBinder.java` | Jauges métier |
| `backend/src/main/java/com/joe/taskira/security/config/SecurityConfig.java` | `EndpointRequest.toAnyEndpoint().permitAll()` |
| `infra/docker-compose.yml` | Port 9091 non publié, healthcheck, services `prometheus`/`grafana` |
| `infra/prometheus/prometheus.yml` | Cible à scraper (`backend:9091`, toutes les 15s) |
| `infra/grafana/provisioning/` | Datasource + 2 dashboards provisionnés automatiquement |

### F. Comment voir tout ça fonctionner

```powershell
# Le endpoint brut, depuis l'intérieur du réseau Docker (jamais depuis l'hôte)
docker exec taskira-backend wget -qO- http://localhost:9091/actuator/health
docker exec taskira-backend wget -qO- http://localhost:9091/actuator/prometheus | Select-String "taskira_"

# Depuis l'hôte : le port 9091 doit être injoignable (preuve de la séparation réseau)
curl.exe http://localhost:9091/actuator/health   # doit échouer en connexion refusée

# Prometheus a-t-il bien trouvé le backend ?
start http://localhost:9090/targets

# Grafana (identifiants par défaut admin/taskira, surchargeables via variables d'env)
start http://localhost:3000
```

Dans Grafana : dossier "Taskira", deux dashboards déjà provisionnés (`taskira-runtime`,
`taskira-business`) - rien à créer à la main.

### G. Exercice pratique

1. Ouvre `http://localhost:9090/targets` et vérifie que la cible `taskira-backend` est à l'état `UP`.
2. Dans Prometheus, requête `taskira_tickets` (onglet Graph) et observe une valeur par statut.
3. Crée un ticket dans l'interface Taskira, puis relance la même requête 15-20 secondes après :
   la jauge du statut correspondant doit avoir changé.
4. Dans Grafana, ouvre le dashboard `taskira-business` et retrouve le même chiffre affiché en graphique.

### H. Ce que tu dois observer

- La cible Prometheus `taskira-backend` en `UP`, jamais `DOWN`.
- Le port 9091 accessible depuis `docker exec` mais refusé depuis l'hôte Windows directement.
- Les jauges business qui bougent en quasi temps réel après une action réelle dans l'app.

### I. Erreurs possibles

| Erreur | Cause probable |
|---|---|
| Cible Prometheus à `DOWN` | Le backend n'est pas démarré, ou `MANAGEMENT_SERVER_PORT` a été changé sans mettre à jour `prometheus.yml` |
| `curl http://localhost:9091/...` répond depuis l'hôte | Le port a été accidentellement publié dans `docker-compose.yml` - vérifier qu'il n'y a pas de section `ports:` sur `backend` pour 9091 |
| `/actuator/health` renvoie `503` | Vérifier `readiness` (souvent la connexion PostgreSQL) via `show-details` en dev, ou les logs du backend |
| Grafana affiche "No data" | Vérifier d'abord que la cible Prometheus est `UP`, puis que la plage de temps du dashboard couvre le présent |

### Résumé à retenir

1. Actuator/Micrometer exposent des métriques ; Prometheus les scrape (les vient chercher) ; Grafana
   les affiche.
2. Le port 9091 est séparé du port applicatif 8080 et jamais publié à l'hôte - c'est la vraie
   frontière de sécurité, pas un mot de passe.
3. Spring Security raisonne par chemin, pas par port - `EndpointRequest.toAnyEndpoint().permitAll()`
   est nécessaire même avec un port séparé.
4. `readiness` (peut recevoir du trafic) et `liveness` (le processus est vivant) sont deux questions
   différentes.
5. Les jauges métier de Taskira interrogent la vraie base à chaque scrape - jamais de cache périmé.

### Quiz Partie 7

1. Pourquoi Actuator vit-il sur un port différent de l'API (9091 vs 8080) ?
2. Pourquoi `EndpointRequest.toAnyEndpoint().permitAll()` est-il nécessaire même avec un port séparé ?
3. Quelle est la différence entre `readiness` et `liveness` ?
4. Pourquoi les jauges de `BusinessMetricsBinder` ne mettent-elles jamais en cache leur résultat ?
5. Pourquoi `management.health.mail.enabled: false` a-t-il été ajouté ?

---

## Partie 8 — Nginx + production-like + GHCR + release/staging

### A. Concepts

**Reverse proxy** — Un serveur placé devant l'application, qui reçoit toutes les requêtes du
navigateur en premier et les redirige vers le bon endroit interne. Le navigateur ne parle jamais
directement au backend.

**Multi-stage build** — Un `Dockerfile` en plusieurs étapes (`FROM ... AS build`, puis un second
`FROM` propre) : la première compile, la seconde ne garde que le résultat fini - le compilateur, le
code source, les outils de build ne finissent jamais dans l'image livrée.

**Utilisateur non-root** — Par défaut un container tourne en `root` à l'intérieur. Si quelqu'un
exploite une faille dans l'application, il hérite des droits du processus - un utilisateur dédié
sans privilège limite les dégâts possibles.

**Image immuable versionnée** — Une image construite une fois, taguée avec un numéro de version
précis, jamais reconstruite pour "corriger juste ce petit truc" - toute correction produit une
nouvelle version.

**Registry (GHCR)** — Un entrepôt où sont stockées les images Docker déjà construites, pour que
n'importe quelle machine autorisée puisse les télécharger sans jamais reconstruire le code source.
Taskira utilise GHCR (GitHub Container Registry).

**Tag `latest` vs tag de version** — `latest` change de sens dans le temps (pointe vers "la image la
plus récente publiée sous ce nom"), un tag `v0.1.0` ne change jamais : c'est exactement la même image
demain que aujourd'hui.

**Staging** — Un environnement qui ressemble le plus possible à la production réelle, utilisé pour
vérifier qu'un déploiement fonctionne *avant* de l'annoncer officiellement.

**Rollback** — Revenir à une version antérieure qui fonctionnait, après avoir découvert un problème
avec la nouvelle.

**SemVer (Semantic Versioning)** — Convention `MAJOR.MINOR.PATCH` (`v0.1.0`) : `0.x` signale "encore
en évolution, pas encore stable/complet".

### B. Pourquoi Nginx sert le frontend en production-like (et pas `ng serve`)

En développement (`infra/docker-compose.yml`), Angular tourne avec son propre serveur de
développement (hot reload). Ce serveur n'est jamais fait pour être exposé publiquement - il n'est pas
optimisé, pas sécurisé pour ça. `frontend/Dockerfile.prod` construit plutôt le vrai Angular compilé
(fichiers HTML/CSS/JS statiques, minifiés, avec des noms de fichiers hashés) puis les fait servir par
Nginx (`nginxinc/nginx-unprivileged`, déjà non-root par défaut).

Extrait réel de `frontend/nginx/default.conf`, avec les décisions qui ont chacune coûté un vrai bug
en Phase 11 :

```nginx
resolver 127.0.0.11 valid=10s;

location ^~ /api/ {
    set $backend_upstream http://backend:8080;
    proxy_pass $backend_upstream;
    ...
}

location / {
    try_files $uri $uri/ /index.html;
}
```

- **`resolver 127.0.0.11`** : le DNS intégré de Docker. Sans passer `proxy_pass` par une variable
  (`set $backend_upstream ...`), Nginx résout `backend` une seule fois *au démarrage* et refuse même
  de démarrer si ce nom ne résout pas encore à ce moment précis - bug réel rencontré en démarrant
  `frontend` avant que `backend` n'existe sur le réseau.
- **`try_files $uri $uri/ /index.html;`** (fallback SPA) : Angular gère lui-même le routage
  côté client (`/tickets/42` n'est pas un vrai fichier sur le disque). Sans cette ligne, recharger
  directement une URL profonde donnerait un `404` de Nginx au lieu de laisser Angular afficher la
  bonne page.
- **`/api/` proxifié vers `backend:8080`** : le navigateur ne contacte jamais le backend directement -
  toute requête passe par Nginx en premier, sur la même origine que le frontend
  (`environment.ts` de production utilise une URL *relative* `/api/v1`, justement pour ça, voir
  Partie 3).

### C. Le multi-stage build backend - non-root en pratique

`backend/Dockerfile` (le même fichier utilisé en dev, en test E2E et en production) :

```dockerfile
FROM eclipse-temurin:21...-jdk AS build
...
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:21...-jre
RUN groupadd --gid 10001 taskira \
    && useradd --uid 10001 --gid taskira --shell /usr/sbin/nologin --no-create-home taskira
COPY --from=build --chown=taskira:taskira /app/target/*.jar app.jar
USER taskira
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Deux étapes : la première (`AS build`) a Maven, le JDK complet, le code source - tout ce qu'il faut
pour compiler. La seconde repart d'une image `jre` (Java *Runtime* seulement, plus légère, pas de
compilateur) et ne récupère que le `.jar` déjà construit via `COPY --from=build`. Résultat : l'image
livrée n'a ni le code source Java, ni Maven, ni le JDK complet.

`USER taskira` (uid 10001, pas 1000 - un vrai conflit avec un utilisateur `ubuntu` déjà présent dans
l'image de base a forcé ce choix) fait tourner le processus Java sans privilège root à l'intérieur du
container.

### D. GHCR et le versionnage des images

`.github/workflows/release.yml` ne se déclenche **jamais** sur un push normal - seulement sur un tag
`v*.*.*` :

```yaml
on:
  push:
    tags:
      - "v*.*.*"
```

Chaque image est poussée sous **deux** tags à la fois, jamais `latest` tout seul :

```yaml
-t ghcr.io/joe-azar/taskira-backend:${{ github.ref_name }}   # ex. v0.1.0
-t ghcr.io/joe-azar/taskira-backend:${{ github.sha }}        # le commit exact
```

Le tag de version (`v0.1.0`) est ce que `docker-compose.staging.yml` référence pour déployer ; le tag
SHA garantit qu'on peut toujours retrouver *exactement* quel commit a produit une image donnée, même
si le tag de version venait à être redéplacé sur un autre commit plus tard.

### E. Le pipeline complet de `release.yml`

```
push d'un tag v0.1.0
  │
  ▼
build-and-push (backend + frontend, en parallèle)
  │  construit avec frontend/Dockerfile.prod / backend/Dockerfile
  │  pousse vers GHCR sous 2 tags
  ▼
staging-smoke-test (needs: build-and-push)
  │  déploie RÉELLEMENT infra/docker-compose.staging.yml
  │  avec les images tout juste publiées (jamais reconstruites localement)
  │  attend que les healthchecks passent à "healthy"
  │  lance TOUTE la suite Playwright contre ce déploiement réel
  ▼
publish-release (needs: staging-smoke-test)
     ne s'exécute QUE si le déploiement + les tests ont réellement réussi
     publie une vraie GitHub Release avec notes auto-générées
```

Le point clé : une Release GitHub n'apparaît jamais sur la seule force d'un build vert - elle exige
un déploiement réel et des tests E2E réels contre ce déploiement.

`infra/docker-compose.staging.yml` est le seul fichier Compose du dépôt qui ne construit **jamais**
d'image localement - chaque service référence directement `image: ghcr.io/...:${VERSION:?...}`.

### F. Le rollback

Pas de procédure séparée à apprendre : un rollback, c'est redéployer le même
`docker-compose.staging.yml` avec un `VERSION` différent (une version antérieure déjà publiée sur
GHCR). Exactement le même chemin de code que le workflow de release emprunte déjà pour son propre
déploiement de vérification.

### G. Fichiers concernés

| Fichier | Rôle |
|---|---|
| `frontend/nginx/default.conf` | Config Nginx réelle : proxy `/api/`, fallback SPA, cache |
| `frontend/Dockerfile.prod` | Build Angular compilé + Nginx (jamais `ng serve`) |
| `backend/Dockerfile` | Multi-stage, utilisateur non-root `taskira` (uid 10001) |
| `infra/docker-compose.prodlike.yml` | Runtime HTTP local proche production (3 réseaux séparés) |
| `infra/docker-compose.staging.yml` | Déploie une image GHCR déjà publiée, jamais de build local |
| `.github/workflows/release.yml` | Build, push GHCR, déploie staging, teste, publie la Release |

### H. Exercice pratique

```powershell
# Voir la config Nginx réelle utilisée en production-like
docker exec taskira-frontend-prodlike cat /etc/nginx/conf.d/default.conf
```

Sur GitHub : onglet Releases pour voir `v0.1.0` déjà publiée ; onglet Packages (ou
`https://github.com/Joe-azar/taskira/pkgs/container/taskira-backend`) pour voir les images GHCR
réellement publiées avec leurs deux tags.

### I. Ce que tu dois observer

- `frontend/Dockerfile.prod` (production) est un fichier différent de `frontend/Dockerfile` (dev/E2E) -
  ne pas les confondre.
- Chaque image GHCR porte bien deux tags : une version lisible et un SHA de commit.
- `publish-release` n'apparaît dans l'historique du workflow que si `staging-smoke-test` a réussi.

### J. Erreurs possibles

| Erreur | Cause probable |
|---|---|
| `502 Bad Gateway` sur `/api/...` en production-like | Backend pas encore démarré, ou nom de service Docker incorrect dans `proxy_pass` |
| `Cannot GET /tickets/42` en rechargeant une page directement | Le fallback SPA (`try_files ... /index.html`) est absent ou mal configuré |
| Image poussée mais jamais de Release GitHub | `staging-smoke-test` a échoué - `publish-release` ne s'exécute jamais dans ce cas |
| `docker-compose.staging.yml` refuse de démarrer sans variable | Comportement voulu (`${VERSION:?...}`) - jamais de valeur par défaut silencieuse |

### Résumé à retenir

1. Nginx sert l'Angular compilé en production-like, jamais le serveur de développement `ng serve`.
2. Un multi-stage build ne livre jamais le code source ni les outils de compilation dans l'image
   finale - seulement le résultat.
3. Une image tourne en utilisateur non-root dédié, jamais en `root` par défaut.
4. Chaque image publiée porte un tag de version ET un tag de SHA de commit - jamais `latest` seul.
5. Une GitHub Release n'apparaît que si le déploiement staging réel et ses tests E2E ont réellement
   réussi - jamais sur la seule force d'un build vert.

### Quiz Partie 8

1. Pourquoi `frontend/Dockerfile.prod` ne fait-il jamais tourner `ng serve` ?
2. À quoi sert `try_files $uri $uri/ /index.html;` dans la config Nginx ?
3. Pourquoi le backend tourne-t-il avec `USER taskira` (uid 10001) plutôt qu'en `root` ?
4. Pourquoi chaque image GHCR a-t-elle deux tags (version + SHA) plutôt qu'un seul ?
5. Qu'est-ce qui empêche `publish-release` de s'exécuter si le déploiement staging échoue ?

---

## Partie 9 — Backup / restore

### A. Concepts

**`pg_dump`** — Outil officiel PostgreSQL qui produit une copie complète et cohérente d'une base de
données, même pendant qu'elle continue de recevoir des écritures (une "photo" cohérente à un instant
donné, pas un blocage de la base).

**`pg_restore`** — L'outil inverse : recharge un fichier produit par `pg_dump` dans une base
(généralement vide au départ).

**Format personnalisé (`-F custom`)** — Un des formats de sortie de `pg_dump`, compressé et
compatible avec `pg_restore` (contrairement au format SQL brut, plus simple mais plus volumineux).

**Sauvegarde jamais fiable tant qu'elle n'a jamais été restaurée** — Une règle de bon sens que
`ENTERPRISE_MIGRATION_REPORT.md` rappelle explicitement : un fichier de sauvegarde qui n'a jamais été
rechargé avec succès n'est qu'une hypothèse, pas une garantie.

**Conteneur jetable ("disposable")** — Un container créé uniquement pour un test, puis détruit -
jamais la base réelle de développement.

**Cron / planification** — Une tâche qui s'exécute automatiquement selon un horaire, sans action
humaine (ici, chaque lundi).

**Drill (exercice de sauvegarde)** — Le fait de tester réellement le mécanisme de sauvegarde/
restauration régulièrement, pas seulement de faire confiance à ce qu'il "devrait marcher".

### B. Pourquoi deux scripts distincts, pas un seul "backup-and-restore.ps1"

`scripts/backup/backup-postgres.ps1` sauvegarde n'importe quelle base réellement en cours
d'exécution. `scripts/restore/restore-postgres.ps1` restaure **toujours dans un container jetable
séparé**, jamais dans le container `taskira-postgres` réel :

```powershell
docker run -d --name taskira-postgres-restore-check `
    -e "POSTGRES_DB=$Database" -e "POSTGRES_USER=$User" -e "POSTGRES_PASSWORD=$Password" `
    $Image
```

Cette séparation est volontaire : tester une restauration ne doit jamais risquer d'écraser la vraie
base de développement. Le container de vérification reste ensuite allumé pour inspection manuelle -
il n'est pas auto-détruit, pour qu'on puisse vraiment aller regarder les données restaurées avant de
faire confiance à la sauvegarde.

### C. Un vrai bug de course avec le démarrage de PostgreSQL

`restore-postgres.ps1` n'utilise pas seulement `pg_isready` pour attendre que la base soit prête -
il boucle sur une vraie requête SQL :

```powershell
docker exec $ContainerName psql -U $User -d $Database -c "SELECT 1" | Out-Null
if ($LASTEXITCODE -eq 0) { break }
```

Pourquoi : l'image officielle PostgreSQL démarre d'abord un serveur **temporaire** (pour exécuter les
scripts d'initialisation), l'arrête, puis démarre le serveur réel. `pg_isready` seul pouvait répondre
"prêt" contre ce serveur temporaire, juste avant qu'il ne s'arrête - et `pg_restore` échouait ensuite
avec `the database system is shutting down`. Une vraie requête `SELECT 1`, répétée jusqu'à
réussir, traverse cette fenêtre transitoire au lieu d'entrer en course avec elle.

### D. Un vrai bug de capture de valeur PowerShell

`backup-postgres.ps1` se termine par une ligne isolée : `$hostPath`. Ce n'est pas un oubli - c'est
la vraie valeur de retour du script, capturable par un appelant :

```powershell
$dumpPath = & backup-postgres.ps1 | Select-Object -Last 1
```

Le premier run réel du workflow planifié sur GitHub Actions (impossible à tester avant que le fichier
n'existe sur la branche par défaut) a échoué avec `Cannot index into a null array` : le script ne
communiquait son chemin de sauvegarde que via `Write-Host` (qui écrit sur la console, jamais sur le
flux de sortie de succès de PowerShell) - donc `$result = & backup-postgres.ps1` capturait
systématiquement `$null`. Corrigé en ajoutant cette expression nue comme vraie valeur de retour.

### E. Le workflow planifié - une preuve continue, pas une sauvegarde de production réelle

Taskira n'a, à ce jour, aucune base de données de production persistante à sauvegarder pour de vrai
(le staging de la Partie 8 est déployé et détruit à la demande). Le rôle réel de
`.github/workflows/backup-restore-drill.yml` (chaque lundi, plus déclenchable manuellement) est donc
différent : prouver en continu que le mécanisme ne se dégrade jamais silencieusement.

```
Démarre un vrai PostgreSQL + le vrai backend (toutes les migrations Flyway appliquées)
  ▼
Sème de vraies données via l'API HTTP réelle (utilisateur, projet, ticket)
  ▼
Sauvegarde (backup-postgres.ps1)
  ▼
Restaure dans un container jetable (restore-postgres.ps1)
  ▼
Vérifie que les données semées sont réellement revenues (SELECT count(*) sur users/projects/tickets)
```

Pas seulement "`pg_restore` a rendu un code de sortie zéro" - une vraie vérification du contenu.

### F. Fichiers concernés

| Fichier | Rôle |
|---|---|
| `scripts/backup/backup-postgres.ps1` | Sauvegarde à la demande (`pg_dump`, format custom) |
| `scripts/restore/restore-postgres.ps1` | Restauration + vérification, jamais sur la base active |
| `.github/workflows/backup-restore-drill.yml` | Cycle complet hebdomadaire semer→sauvegarder→restaurer→vérifier |
| `backups/` | Dossier local des sauvegardes produites (ignoré par Git) |

### G. Exercice pratique

```powershell
# Sauvegarder la vraie base de développement
& .\scripts\backup\backup-postgres.ps1

# Restaurer cette sauvegarde dans un container jetable et voir les comptes de lignes
& .\scripts\restore\restore-postgres.ps1 -DumpFile ".\backups\taskira-<le-nom-affiché>.dump"

# Nettoyer le container de vérification une fois fini
docker rm -f taskira-postgres-restore-check
```

### H. Ce que tu dois observer

- Le fichier `.dump` créé sous `backups/`, avec sa taille en octets affichée.
- Les comptes de lignes affichés par `restore-postgres.ps1` doivent correspondre à ce qu'il y a
  réellement dans la base de développement au moment de la sauvegarde.
- Le container `taskira-postgres-restore-check` reste allumé après le script - à supprimer soi-même.

### I. Erreurs possibles

| Erreur | Cause probable |
|---|---|
| `the database system is shutting down` pendant la restauration | Rare si le script est utilisé tel quel (déjà corrigé) - mais reproductible avec `pg_isready` seul |
| `Container '...' already exists` | Un ancien container de vérification n'a pas été supprimé | `docker rm -f taskira-postgres-restore-check` |
| Comptes de lignes à 0 après restauration | Le fichier `.dump` ne correspond pas à la base attendue, ou la sauvegarde a échoué silencieusement |

### Résumé à retenir

1. Sauvegarder (n'importe quelle base réelle) et restaurer (toujours dans un container jetable
   séparé) sont deux scripts distincts, jamais mélangés.
2. Une sauvegarde n'est fiable que si elle a déjà été restaurée avec succès au moins une fois.
3. L'image officielle PostgreSQL démarre un serveur temporaire avant le vrai - `pg_isready` seul peut
   mentir sur cette fenêtre transitoire, une vraie requête répétée est plus sûre.
4. `Write-Host` n'atteint jamais le flux de sortie capturable de PowerShell - une expression nue en
   fin de script est la vraie valeur de retour.
5. Le workflow hebdomadaire ne protège aucune donnée de production réelle aujourd'hui - il prouve que
   le mécanisme fonctionne encore, en continu.

### Quiz Partie 9

1. Pourquoi `restore-postgres.ps1` ne restaure-t-il jamais dans le container `taskira-postgres` réel ?
2. Pourquoi `pg_isready` seul n'était-il pas suffisant pour attendre que PostgreSQL soit prêt ?
3. Pourquoi `Write-Host` ne suffit-il pas à faire remonter une valeur à un script appelant ?
4. Le workflow planifié sauvegarde-t-il une vraie base de production aujourd'hui ?
5. Que vérifie concrètement le workflow, au-delà du code de sortie de `pg_restore` ?

---

## Partie 10 — Kubernetes + kind

⚠️ Important : Kubernetes est un **lab d'apprentissage** dans `labs/kubernetes/`, complètement
séparé du runtime principal de Taskira. Docker Compose reste la vraie façon de faire tourner
l'application au quotidien - ce lab existe pour comprendre Kubernetes en le pratiquant sur de vraies
images GHCR déjà publiées, pas pour remplacer Compose.

### A. Concepts

**Kubernetes (K8s)** — Un système qui fait tourner et surveille des containers *à travers plusieurs
machines*, en redémarrant automatiquement ce qui plante, en répartissant la charge, en permettant de
monter en charge (scaling) sans intervention manuelle. Docker Compose orchestre des containers sur
*une seule* machine ; Kubernetes est fait pour en orchestrer sur *plusieurs*.

**`kind` (Kubernetes IN Docker)** — Fait tourner un cluster Kubernetes complet où chaque "nœud" est
en réalité... un simple container Docker. Permet de pratiquer Kubernetes sans machine dédiée ni
service cloud payant.

**Pod** — La plus petite unité déployable de Kubernetes. Contient un ou plusieurs containers qui
partagent le même réseau. Dans Taskira, chaque Pod ne contient qu'un seul container.

**Deployment** — Décrit *combien* de Pods identiques doivent tourner et *quelle image* ils utilisent.
Kubernetes les recrée automatiquement s'ils meurent.

**Service** — Une adresse réseau stable qui pointe toujours vers les bons Pods, même quand ceux-ci
sont recréés (et donc changent d'adresse IP interne).

**Namespace** — Un espace de noms qui isole un groupe de ressources des autres (Taskira utilise
`taskira`).

**ConfigMap / Secret** — Deux façons d'injecter de la configuration dans un Pod sans la coder en dur
dans l'image. Un Secret est destiné aux données sensibles (mot de passe) ; un ConfigMap au reste.

**PersistentVolumeClaim (PVC)** — Une demande de stockage durable qui survit à la destruction d'un
Pod - l'équivalent conceptuel d'un volume nommé Docker, mais standardisé pour Kubernetes.

**StorageClass** — Définit *comment* Kubernetes doit fournir le stockage demandé par un PVC. `kind`
en fournit une par défaut (`standard`), aucune configuration manuelle nécessaire pour ce lab.

**Ingress** — Le point d'entrée HTTP public du cluster, qui route les requêtes vers le bon Service
selon le chemin/domaine demandé - le rôle que joue Nginx en dehors de Kubernetes.

**Probe (readiness/liveness)** — Exactement le même concept que les healthchecks Docker (Partie 7),
mais formalisé par Kubernetes : `readinessProbe` (peut recevoir du trafic ?), `livenessProbe`
(faut-il redémarrer ce Pod ?).

**`kubectl`** — L'outil en ligne de commande pour parler à un cluster Kubernetes.

**Rolling update / Rollback** — Remplacer progressivement les anciens Pods par de nouveaux (sans
interruption de service), ou revenir à la version précédente si la nouvelle pose problème.

### B. La plus grande leçon de ce lab : le DNS n'est pas le même qu'en Docker Compose

`frontend/nginx/default.conf` (le vrai fichier utilisé partout ailleurs dans Taskira, voir Partie 8)
contient `resolver 127.0.0.11 valid=10s;` - le DNS intégré de Docker. Ça fonctionne dans
`infra/docker-compose.yml`, `docker-compose.prodlike.yml` et `docker-compose.staging.yml` sans
jamais y penser. **Ça ne fonctionne pas du tout dans Kubernetes** - deux bugs réels trouvés
uniquement en démarrant réellement le cluster et en envoyant une vraie requête à travers l'Ingress :

1. `127.0.0.11` n'existe simplement pas dans l'espace réseau d'un Pod Kubernetes -> `502` sur toute
   requête `/api/`.
2. Une fois corrigé vers le vrai DNS du cluster (`kube-dns.kube-system.svc.cluster.local`), le nom
   court `backend` restait **encore** introuvable. Cause plus subtile : le résolveur intégré de
   Nginx interroge le DNS avec le nom *littéral* configuré, sans jamais appliquer la liste `search`
   du fichier `/etc/resolv.conf` du Pod - contrairement à `wget`/`curl`/la JVM, qui eux étendent
   automatiquement `backend` en `backend.taskira.svc.cluster.local` avant d'interroger le DNS.

Solution retenue (`labs/kubernetes/manifests/05-frontend.yaml`) : **ne jamais toucher**
`frontend/nginx/default.conf` lui-même (il est partagé et déjà validé par trois runtimes Docker
Compose différents) - un `ConfigMap` remplace juste ce fichier *à l'intérieur du Pod*, identique à
deux lignes près :

```nginx
resolver kube-dns.kube-system.svc.cluster.local valid=10s;
...
set $backend_upstream http://backend.taskira.svc.cluster.local:8080;
```

Nom de service pleinement qualifié (`backend.taskira.svc.cluster.local`), jamais le nom court.

### C. PostgreSQL en Kubernetes : `Deployment` + PVC, pas `StatefulSet`

```yaml
strategy:
  type: Recreate
```

Une seule instance PostgreSQL, un seul PVC en mode `ReadWriteOnce` (un seul Pod à la fois peut
l'utiliser en écriture). Avec la stratégie par défaut (`RollingUpdate`), Kubernetes démarrerait un
second Pod *avant* d'arrêter le premier - ce second Pod resterait bloqué indéfiniment, incapable de
monter un volume déjà tenu par le premier. `Recreate` arrête d'abord l'ancien Pod, exactement ce
qu'il faut pour une seule instance sur ce type de stockage. Un `StatefulSet` (identité stable,
scaling ordonné) n'est volontairement pas utilisé : aucune de ces garanties n'est nécessaire pour une
seule replica - exactement comme `infra/docker-compose*.yml` le fait déjà.

### D. Fichiers concernés

| Fichier | Rôle |
|---|---|
| `labs/kubernetes/cluster/kind-config.yaml` | Définition du cluster `kind` |
| `labs/kubernetes/manifests/00-namespace.yaml` | Namespace `taskira` |
| `labs/kubernetes/manifests/02-postgres-secret.example.yaml` | Modèle - le vrai Secret est créé de façon impérative, jamais commité |
| `labs/kubernetes/manifests/03-postgres.yaml` | PVC + Deployment `Recreate` + Service |
| `labs/kubernetes/manifests/04-backend.yaml` | Deployment backend (images GHCR réelles de la Partie 8) |
| `labs/kubernetes/manifests/05-frontend.yaml` | ConfigMap DNS corrigé + Deployment + Service |
| `labs/kubernetes/manifests/06-ingress.yaml` | Point d'entrée HTTP public du cluster |
| `labs/kubernetes/scripts/up.ps1` / `down.ps1` / `demo-rollout.ps1` | Créer, détruire, démontrer scaling/rollout/rollback |

### E. Comment le voir fonctionner

```powershell
& .\labs\kubernetes\scripts\up.ps1 -PostgresPassword "un-mot-de-passe-de-lab"

curl.exe http://localhost/                    # 200, le vrai index.html Angular
curl.exe http://localhost/healthz              # 200
curl.exe http://localhost/api/v1/auth/me       # 401, un vrai ProblemDetail avec un requestId

kubectl get pods -n taskira
kubectl get deployments -n taskira

& .\labs\kubernetes\scripts\demo-rollout.ps1   # scaling, rolling update, rollback
& .\labs\kubernetes\scripts\down.ps1           # détruit le cluster proprement
```

### F. Exercice pratique

1. Lance `up.ps1` et observe `kubectl get pods -n taskira -w` jusqu'à ce que tous les Pods soient
   `Running`.
2. `kubectl scale deployment/frontend --replicas=3 -n taskira`, puis reconfirme avec
   `kubectl get pods -n taskira`.
3. `kubectl describe pod <un-pod-frontend> -n taskira` et repère la section `Readiness probe`.
4. `kubectl rollout undo deployment/backend -n taskira` puis vérifie l'image redevenue active avec
   `kubectl get deployment/backend -n taskira -o jsonpath="{.spec.template.spec.containers[0].image}"`.

### G. Ce que tu dois observer

- Tous les Pods à l'état `Running`, jamais `CrashLoopBackOff`.
- `curl http://localhost/api/v1/auth/me` répond bien via l'Ingress → Nginx → backend → Spring
  Security réel, avec un `requestId` réel dans le corps `ProblemDetail`.
- Après `kubectl scale`, le nombre réel de Pods `frontend` correspond à la valeur demandée.

### H. Erreurs possibles

| Erreur | Cause probable |
|---|---|
| `502` sur toute requête `/api/` | Résolveur DNS Nginx incompatible avec Kubernetes (voir section B) |
| `backend could not be resolved (2: Server failure)` | Nom de service court utilisé au lieu du nom pleinement qualifié |
| Pod PostgreSQL bloqué en `Pending` | PVC qui n'a pas pu être provisionné, ou ancien Pod encore accroché au volume |
| `kubectl wait` échoue avec "no matching resources found" juste après un `apply` | Appelé trop tôt - le contrôleur n'a pas encore créé l'objet, une nouvelle tentative résout le problème |

### Résumé à retenir

1. Kubernetes orchestre des containers à travers plusieurs machines ; `kind` simule un cluster
   complet dans de simples containers Docker, pour apprendre sans coût ni matériel dédié.
2. Pod (unité de base) / Deployment (combien + quelle image) / Service (adresse stable) / Ingress
   (porte d'entrée HTTP publique) sont les briques de base.
3. Le DNS intégré de Docker Compose (`127.0.0.11`) n'existe pas en Kubernetes - et le résolveur
   Nginx n'applique jamais la liste `search`, contrairement à `wget`/`curl`/la JVM : un nom de
   service doit être pleinement qualifié pour lui.
4. PostgreSQL utilise un `Deployment` en stratégie `Recreate` (pas un `StatefulSet`) : une seule
   replica sur un stockage `ReadWriteOnce` n'a besoin d'aucune des garanties d'un `StatefulSet`.
5. Ce lab utilise les vraies images GHCR déjà publiées par la Partie 8 - pas des images reconstruites
   pour l'occasion.

### Quiz Partie 10

1. Pourquoi le résolveur DNS de `frontend/nginx/default.conf` ne fonctionne-t-il pas tel quel dans
   Kubernetes ?
2. Pourquoi utiliser `backend.taskira.svc.cluster.local` plutôt que juste `backend` dans le `proxy_pass`
   corrigé ?
3. Pourquoi PostgreSQL utilise-t-il `strategy: Recreate` plutôt que la stratégie par défaut
   (`RollingUpdate`) ?
4. Pourquoi ne pas utiliser un `StatefulSet` pour PostgreSQL ici ?
5. `frontend/nginx/default.conf` a-t-il été modifié pour corriger le bug DNS ? Pourquoi (pas) ?

---

## Partie 11 — Helm

⚠️ Toujours un lab (`labs/helm/`), jamais le runtime principal - construit *après* avoir déjà
maîtrisé les manifestes bruts de la Partie 10, pas à leur place.

### A. Concepts

**Helm** — Le "gestionnaire de paquets" de Kubernetes. Plutôt que d'appliquer une pile de fichiers
YAML un par un (Partie 10), on empaquette tout dans un **Chart** réutilisable et paramétrable.

**Chart** — Un paquet Helm : un dossier avec `Chart.yaml` (métadonnées), `values.yaml` (valeurs par
défaut) et `templates/` (les manifestes Kubernetes, mais avec des trous à remplir).

**Template** — Un fichier YAML Kubernetes avec des expressions `{{ .Values.xxx }}` à la place des
valeurs figées - le même manifeste peut ainsi produire des résultats différents selon la
configuration donnée.

**`values.yaml`** — Les valeurs par défaut utilisées pour remplir les templates. Peuvent être
surchargées avec `--set` ou un second fichier `-f`.

**Release** — Une installation d'un Chart, sous un nom donné, dans un namespace donné. On peut avoir
plusieurs Releases du même Chart en parallèle.

**`helm lint`** — Vérifie la syntaxe et les bonnes pratiques d'un Chart, sans rien installer.

**`helm template`** — Affiche ce que les templates *produiraient* comme vrai YAML Kubernetes, sans
rien envoyer au cluster - utile pour vérifier avant d'agir pour de vrai.

**`helm upgrade --install`** — Installe le Chart s'il n'existe pas encore, ou le met à jour s'il
existe déjà - une seule commande pour les deux cas, couramment utilisée en CI/CD.

**`initContainer`** — Un container qui doit terminer *avant* que les containers principaux d'un Pod
ne démarrent - utile pour exprimer une dépendance de démarrage ("attends que telle chose soit prête
avant de commencer").

### B. Pourquoi le Chart a été construit après les manifestes bruts, pas avant

`Chart.yaml` le dit explicitement dans son propre commentaire :

```yaml
description: >-
  Taskira Kubernetes lab chart (P18, ADR-0025) - packages the same manifests already
  validated as raw YAML in labs/kubernetes/ (P17).
```

Comprendre Kubernetes "brut" d'abord (Partie 10) donne les bases nécessaires pour comprendre ce que
Helm automatise réellement - sinon Helm ne serait qu'une boîte noire de plus. Le Chart reprend
exactement le même contenu que les manifestes de la Partie 10, avec les valeurs qui variaient déjà
(tags d'image, nombre de replicas, taille de PVC...) devenues des paramètres de `values.yaml`.

### C. `required` - jamais de mot de passe par défaut

```yaml
stringData:
  password: {{ required "postgres.password is required - ..." .Values.postgres.password | quote }}
```

`values.yaml` ne contient **aucun** mot de passe PostgreSQL par défaut. La fonction Helm `required`
fait échouer le rendu du Chart *explicitement* si la valeur manque, plutôt que de produire
silencieusement un Secret avec une valeur vide ou un mot de passe faible codé en dur - même
discipline que le paramètre `-PostgresPassword` obligatoire du script `up.ps1` de la Partie 10.

### D. Un vrai bug trouvé en installant réellement le Chart : la course PostgreSQL/backend

Les manifestes bruts de la Partie 10 sont appliqués un par un, dans un ordre choisi (`postgres`
avant `backend`). `helm upgrade --install --wait` soumet en revanche **tous** les templates du
Chart ensemble - rien ne garantit que PostgreSQL soit prêt avant que le Pod backend ne démarre. La
toute première installation réelle a fait crash-looper `backend` deux fois
(`FlywaySqlUnableToConnectToDbException: Connection to postgres:5432 refused`) avant que le
redémarrage automatique de Kubernetes ne finisse par réussir.

Corrigé avec un `initContainer`, qui réutilise l'image PostgreSQL déjà présente dans `values.yaml`
et le même `pg_isready` déjà utilisé par les probes de PostgreSQL lui-même :

```yaml
initContainers:
  - name: wait-for-postgres
    image: {{ .Values.postgres.image }}
    command: ["sh", "-c", "until pg_isready -h postgres -p 5432 -U \"$POSTGRES_USER\"; do sleep 2; done"]
```

Le Pod backend ne démarre son container principal qu'une fois cet `initContainer` terminé avec
succès - élimine le crash-loop plutôt que de simplement le tolérer.

### E. Fichiers concernés

| Fichier | Rôle |
|---|---|
| `labs/helm/taskira/Chart.yaml` | Métadonnées du Chart (nom, version) |
| `labs/helm/taskira/values.yaml` | Valeurs par défaut (images, replicas, tailles...) - jamais de mot de passe |
| `labs/helm/taskira/templates/secret.yaml` | `required` empêche un rendu sans mot de passe |
| `labs/helm/taskira/templates/backend.yaml` | `initContainer` qui attend réellement PostgreSQL |
| `labs/helm/scripts/up.ps1` / `down.ps1` | Créer le cluster, installer/désinstaller le Chart |

### F. Comment le voir fonctionner

```powershell
# Vérifier avant d'installer quoi que ce soit
helm lint .\labs\helm\taskira
helm template .\labs\helm\taskira --set postgres.password=un-mot-de-passe-de-lab

# Vérifier que "required" fonctionne réellement (doit échouer)
helm template .\labs\helm\taskira

# Installer pour de vrai
& .\labs\helm\scripts\up.ps1 -PostgresPassword "un-mot-de-passe-de-lab"

helm list -n taskira
helm status taskira -n taskira
kubectl get pods -n taskira -w
```

### G. Exercice pratique

1. Lance `helm template` sans mot de passe et observe le message d'erreur exact produit par
   `required`.
2. Installe le Chart pour de vrai, puis observe `kubectl get pods -n taskira` : combien de
   redémarrages sur chaque Pod ? (attendu : zéro grâce à l'`initContainer`).
3. Modifie `frontend.replicas` dans un fichier `-f` séparé et relance `helm upgrade --install`.

### H. Ce que tu dois observer

- `helm lint` : 0 échec.
- Sans mot de passe : `helm template`/`helm install` échoue explicitement, jamais un rendu silencieux.
- Après installation : `STATUS: deployed`, et surtout **zéro redémarrage** sur les 4 Pods
  (`postgres`, `backend`, 2× `frontend`).

### I. Erreurs possibles

| Erreur | Cause probable |
|---|---|
| `execution error ... postgres.password is required` | Normal si aucun mot de passe n'est fourni - `required` fonctionne comme prévu |
| `backend` en `CrashLoopBackOff` malgré l'`initContainer` | Vérifier que `postgres` a bien démarré : `kubectl get pods -n taskira` |
| `helm upgrade --install --wait` reste bloqué longtemps | Le webhook d'admission ingress-nginx ou l'API server pas encore prêt - patienter/relancer |
| `Error: INSTALLATION FAILED: cannot re-use a name` | Une ancienne Release du même nom existe déjà | `helm uninstall taskira -n taskira` avant de réinstaller |

### Résumé à retenir

1. Helm empaquette des manifestes Kubernetes paramétrables dans un Chart réutilisable - construit ici
   *après* avoir compris les manifestes bruts, pas à leur place.
2. `helm lint`/`helm template` vérifient un Chart sans jamais toucher un vrai cluster ;
   `helm upgrade --install` installe ou met à jour en une seule commande.
3. La fonction `required` fait échouer explicitement le rendu d'un Chart si une valeur sensible
   manque - jamais de mot de passe par défaut codé en dur.
4. `helm upgrade --install --wait` soumet tous les templates ensemble, contrairement à des manifestes
   appliqués un par un dans un ordre choisi - une vraie course de démarrage peut apparaître.
5. Un `initContainer` est la façon Kubernetes-native d'exprimer "attends que telle chose soit prête
   avant de démarrer" - élimine un crash-loop plutôt que de le tolérer.

### Quiz Partie 11

1. Pourquoi le Chart Helm a-t-il été construit après les manifestes bruts de la Partie 10, pas avant ?
2. Que fait concrètement la fonction Helm `required` dans `templates/secret.yaml` ?
3. Pourquoi `helm upgrade --install --wait` a-t-il révélé un bug jamais vu avec les manifestes bruts ?
4. Comment l'`initContainer` de `backend.yaml` élimine-t-il ce bug ?
5. Différence entre `helm template` et `helm upgrade --install` ?

---

## Partie 12 — Terraform + Azure

🚫 Règle absolue, respectée à la lettre pendant cette partie : **aucune commande créant une vraie
ressource Azure n'est jamais exécutée**. Pas de `terraform plan` ni `terraform apply` contre un vrai
abonnement. `labs/azure/` a été validé uniquement hors ligne (`terraform init`/`fmt`/`validate`) -
c'est de l'architecture et du code Terraform *lus et compris*, jamais déployés.

### A. Concepts

**Infrastructure as Code (IaC)** — Décrire l'infrastructure (réseaux, serveurs, bases de données...)
dans des fichiers texte versionnés, plutôt que de cliquer manuellement dans une console web. Le
fichier devient la source de vérité, reproductible et relisible comme du code.

**Terraform** — Un outil d'IaC qui fonctionne avec de nombreux fournisseurs cloud (Azure, AWS,
GCP...) via des "providers".

**Provider** — Le plugin Terraform qui sait parler à un fournisseur cloud précis (`azurerm` pour
Azure).

**Resource** — Un bloc Terraform qui décrit *une* chose à créer (un réseau, une base de données...).

**`terraform init`** — Télécharge les providers nécessaires. Ne crée rien dans le cloud, aucun
identifiant requis pour un provider public.

**`terraform fmt`** — Reformate les fichiers `.tf` selon un style standard, sans rien exécuter.

**`terraform validate`** — Vérifie la cohérence syntaxique et de schéma des fichiers, entièrement
hors ligne, sans identifiant cloud et sans jamais contacter Azure.

**`terraform plan`** — Se connecte au vrai fournisseur cloud pour calculer ce qui *serait* créé/
modifié/détruit - nécessite un abonnement réel et des identifiants. **Jamais exécuté dans ce lab.**

**`terraform apply`** — Crée réellement les ressources dans le cloud, avec un coût réel. **Jamais
exécuté dans ce lab, et ne le sera jamais sans abonnement, identifiants et accord explicite du
propriétaire du dépôt.**

### B. L'architecture Azure envisagée (jamais déployée)

```
Internet
   │
   ▼
Application Gateway v2 (point d'entrée public unique)
   │  routage par chemin : /api/* -> backend, /* -> frontend
   ▼
Container Apps (ingress interne uniquement, pas d'IP publique)
   ├── backend
   └── frontend
   │
   ▼
PostgreSQL Flexible Server 18 (accès privé exclusif, aucune IP publique)
```

Trois sous-réseaux VNet dédiés (`network.tf`) - le même principe de segmentation que les trois
réseaux Docker de `infra/docker-compose.prodlike.yml` (Partie 8) : tout reste interne sauf le seul
point d'entrée public.

**Pourquoi un seul nom d'hôte public, pas un par service ?** Ce n'est pas une préférence esthétique -
une vraie contrainte technique déjà rencontrée dans ce projet : le cookie de session
`SameSite=Lax` de Taskira (Partie 4) n'est jamais envoyé sur une requête cross-origin. Si frontend et
backend vivaient sur deux noms d'hôte Azure distincts, l'authentification casserait silencieusement
sur toute requête qui modifie des données.

**Pourquoi Azure Container Apps, pas AKS (Azure Kubernetes Service) ?** Taskira reste un monolithe
modulaire (AGENTS.md) - les labs Kubernetes/Helm (Parties 10-11) restent les environnements dédiés à
l'apprentissage de Kubernetes lui-même, pas une raison de faire tourner la vraie application dessus.
Container Apps est un service "sans serveur" (pas de nœuds à gérer), suffisant pour deux containers.

### C. Une vraie découverte documentaire, trouvée avant d'écrire le moindre Terraform

`variables.tf` documente une contrainte réelle, vérifiée via Microsoft Learn avant d'écrire du code :
contrairement à d'autres registres, Azure Container Apps exige des identifiants explicites pour tirer
une image depuis GHCR - **même si l'image est publique**. Modélisé comme une variable Terraform
`sensible` sans valeur par défaut :

```hcl
variable "ghcr_pull_token" {
  description = "... Container Apps requires explicit registry credentials for any non-ACR
    registry, even a public one ..."
  type      = string
  sensitive = true
}
```

Même discipline déjà vue en Partie 10 (`-PostgresPassword` obligatoire) et Partie 11 (`required` sur
le mot de passe PostgreSQL) : aucune valeur par défaut pour un secret, jamais commitée.

### D. Un vrai bug de schéma trouvé par `terraform validate`

`terraform validate` a immédiatement révélé une vraie erreur au premier passage : la ressource
`azurerm_private_dns_zone_virtual_network_link`, sur la version du provider `azurerm` utilisée ici,
exige l'argument `private_dns_zone_id` - pas la paire `resource_group_name`/`private_dns_zone_name`
d'une génération plus ancienne du provider, supposée à tort au premier essai. Corrigée, puis
`terraform validate -json` confirme `0 erreur, 0 avertissement`. Preuve concrète que même sans jamais
toucher au cloud réel, `terraform validate` attrape de vraies erreurs de schéma.

### E. Fichiers concernés

| Fichier | Rôle |
|---|---|
| `labs/azure/main.tf` | Point d'entrée, resource group |
| `labs/azure/network.tf` | VNet, 3 sous-réseaux, IP publique de l'Application Gateway |
| `labs/azure/database.tf` | PostgreSQL Flexible Server 18, accès privé |
| `labs/azure/container_apps.tf` | Backend/frontend en ingress interne uniquement |
| `labs/azure/application_gateway.tf` | Point d'entrée public unique, routage par chemin |
| `labs/azure/variables.tf` | Toutes les variables, secrets jamais avec de défaut |
| `labs/azure/terraform.tfvars.example` | Modèle à copier - jamais de vraie valeur commitée |

### F. Comment le vérifier (sans jamais rien déployer)

```powershell
cd labs\azure
terraform init        # télécharge le provider, aucun identifiant Azure requis
terraform fmt -check  # vérifie le style, ne modifie rien si -check
terraform validate    # vérifie la cohérence syntaxique, entièrement hors ligne
```

Ces trois commandes ne contactent jamais Azure et ne créent jamais rien - c'est exactement pour ça
qu'elles peuvent être exécutées librement, contrairement à `plan`/`apply`.

### G. Exercice pratique

1. Ouvre `labs/azure/network.tf` et retrouve les trois sous-réseaux - explique à voix haute à quoi
   sert chacun.
2. Ouvre `labs/azure/variables.tf` et identifie toutes les variables `sensitive = true` - pourquoi
   aucune n'a de valeur par défaut ?
3. Exécute `terraform validate` toi-même et confirme `0 erreur, 0 avertissement`.

### H. Ce que tu dois observer

- `terraform init` télécharge un provider sans jamais demander d'identifiant Azure.
- `terraform validate` répond en quelques secondes, sans connexion réseau vers Azure nécessaire.
- Aucune commande de ce lab ne demande jamais de se connecter à un compte Azure.

### I. Erreurs possibles

| Erreur | Cause probable |
|---|---|
| `terraform validate` échoue sur un argument de ressource | Version du provider différente de celle attendue - vérifier `.terraform.lock.hcl` |
| Une commande demande une connexion Azure | Ne devrait jamais arriver avec `init`/`fmt`/`validate` seuls - vérifier qu'aucun `plan`/`apply` n'a été tapé par erreur |
| `terraform init` échoue | Généralement un problème de réseau vers le Registry Terraform public, pas Azure lui-même |

### Résumé à retenir

1. Terraform décrit l'infrastructure en code (IaC) - `init`/`fmt`/`validate` sont hors ligne et sans
   coût ; `plan`/`apply` contactent le vrai cloud et peuvent créer de vraies ressources payantes.
2. L'architecture envisagée reprend un principe déjà vu ailleurs dans Taskira : un seul point
   d'entrée public, tout le reste strictement interne (comme les 3 réseaux Docker de la Partie 8).
3. Un seul nom d'hôte public n'est pas une préférence - le cookie `SameSite=Lax` de Taskira ne
   survivrait pas à deux noms d'hôte distincts pour frontend et backend.
4. Container Apps exige des identifiants explicites pour tirer une image GHCR même publique - une
   vraie découverte documentaire faite avant d'écrire le moindre code Terraform.
5. Aucune ressource Azure réelle n'a jamais été créée pour ce lab - c'est un critère de sortie
   explicite, pas un oubli.

### Quiz Partie 12

1. Quelle est la différence entre `terraform validate` et `terraform plan` en termes de ce qu'ils
   contactent réellement ?
2. Pourquoi Taskira ne peut-il pas déployer frontend et backend sur deux noms d'hôte Azure distincts ?
3. Pourquoi `ghcr_pull_token` est-il nécessaire même pour une image GHCR publique ?
4. Pourquoi Azure Container Apps a-t-il été choisi plutôt qu'AKS pour ce lab ?
5. Quelle commande a permis de trouver le bug de schéma sur `azurerm_private_dns_zone_virtual_network_link`
   sans jamais contacter Azure ?

---

## Partie 13 — Diagnostic

Cette partie ne présente pas de nouvelle technologie - elle rassemble une méthode générale pour
diagnostiquer un problème inconnu dans Taskira, plus deux fiches de référence transversales
(environnements, ports). Les scénarios de panne détaillés partie par partie sont déjà dans
[`troubleshooting.md`](troubleshooting.md) - plus de 40 au total à ce stade, largement au-delà des
20 initialement visés.

### A. La méthode générale (quand tu ne sais pas par où commencer)

Face à un problème inconnu, suivre cet ordre plutôt que de deviner au hasard :

1. **Qu'est-ce qui a changé récemment ?** `git status`, `git log -5 --oneline`. Un problème apparu
   après une modification est presque toujours lié à cette modification.
2. **Quel niveau échoue ?** Le conteneur ne démarre pas ? (`docker compose ps`) Le conteneur tourne
   mais ne répond pas ? (`docker logs`) Il répond mais avec une erreur HTTP ? (F12 → Network) Il
   répond 200 mais le résultat est faux ? (regarder les données en base).
3. **Lire le message d'erreur en entier**, pas seulement la première ligne - la vraie cause est
   souvent dans la pile d'appel (stack trace) ou juste après le message principal.
4. **Reproduire en isolant** - un seul service à la fois plutôt que toute la stack, quand c'est
   possible.
5. **Vérifier que ce n'est pas de la contention de ressources** (déjà rencontré plusieurs fois
   pendant la vraie migration de Taskira, voir Partie 5) - plusieurs stacks Docker tournant en même
   temps peuvent produire des timeouts qui n'ont rien à voir avec un vrai bug.
6. **Comparer avec un état qui marchait** - `git diff` contre le dernier commit connu-bon, ou
   redémarrer une stack fraîche pour voir si le problème persiste.

### B. Tableau comparatif des environnements

| Environnement | Fichier | Construit localement ? | Ports hôte publiés | Détruit après usage ? |
|---|---|---|---|---|
| Développement | `infra/docker-compose.yml` | Oui | frontend 4200, backend 8080, postgres 5432, mailpit 1025/8025, prometheus 9090, grafana 3000 | Non - persistant |
| Test unitaire/intégration | (pas de Compose - `mvn verify`/Vitest) | N/A | Aucun | Oui - Testcontainers jetable |
| E2E Playwright | `e2e/playwright/compose.e2e.yml` | Oui | Aucun (`tmpfs`, tout interne) | Oui - toujours (`finally`) |
| Production-like | `infra/docker-compose.prodlike.yml` | Oui | frontend seul, 8080 par défaut | Non - persistant tant qu'utilisé |
| Staging (CI) | `infra/docker-compose.staging.yml` | **Non** - images GHCR déjà publiées | frontend seul, 8081 par défaut | Oui - démonté après le smoke test |
| Kubernetes lab | `labs/kubernetes/` (`kind`) | Non - images GHCR | 80/443 (via `kind` → Ingress) | Oui - `scripts/down.ps1` |
| Helm lab | `labs/helm/` (`kind`) | Non - images GHCR | 80/443 (via `kind` → Ingress) | Oui - `scripts/down.ps1` |
| Azure lab | `labs/azure/` (Terraform) | N/A | **Jamais déployé** | N/A |

### C. Fiche des ports

| Port | Service | Où | Publié à l'hôte ? |
|---|---|---|---|
| 4200 | Angular dev server (`ng serve`) | Dev uniquement | Oui |
| 8080 | API Spring Boot | Dev, prod-like (via Nginx) | Dev : oui (direct) ; prod-like/staging : non (derrière Nginx) |
| 9091 | Actuator (santé, métriques) | Tous les environnements Compose | **Jamais** - réseau Docker interne uniquement |
| 5432 | PostgreSQL | Dev uniquement | Oui (E2E/prod-like/staging : non) |
| 1025 / 8025 | Mailpit (SMTP / interface web) | Dev uniquement | Oui |
| 9090 | Interface web Prometheus | Dev uniquement | Oui |
| 3000 | Interface web Grafana | Dev uniquement | Oui |
| 8080 (prod-like) / 8081 (staging) | Nginx (seul point d'entrée) | Prod-like / staging | Oui - le seul port publié dans ces deux environnements |
| 80 / 443 | Ingress-nginx | Kubernetes/Helm lab (via `kind`) | Oui - mappé au nœud `kind` |

Point à retenir : dans **tous** les environnements qui ressemblent à la production (prod-like,
staging, K8s, Helm), un seul point d'entrée HTTP est jamais publié - jamais un accès direct au
backend ou à PostgreSQL depuis l'extérieur. Seul l'environnement de développement expose tout, pour
le confort.

### D. Exercice pratique

Sans relire les réponses, essaie de répondre à ces 5 scénarios avec la méthode de la section A avant
de vérifier dans `troubleshooting.md` :

1. Après `git pull`, le backend refuse de démarrer avec une erreur Flyway. Premier réflexe ?
2. Une page Angular reste blanche après connexion, sans erreur visible à l'écran. Premier réflexe ?
3. Un test Playwright échoue avec un timeout générique, mais un autre test échoue à chaque nouvelle
   tentative. Premier réflexe ?
4. `curl http://localhost:9091/actuator/health` répond depuis l'hôte Windows, ce qui ne devrait
   jamais arriver. Premier réflexe ?
5. Le workflow `CI Gate` est rouge sur GitHub, mais tout est vert en local. Premier réflexe ?

### E. Ce que tu dois observer

- Une méthode reproductible, indépendante de la technologie précise en cause.
- Le tableau des ports permet de repérer immédiatement une anomalie ("ce port ne devrait jamais
  répondre depuis l'hôte").
- Le tableau des environnements permet de savoir en un coup d'œil si un environnement donné construit
  ses propres images ou réutilise des images déjà publiées.

### Résumé à retenir

1. Face à un problème inconnu : qu'est-ce qui a changé ? quel niveau échoue ? lire l'erreur en
   entier ? isoler pour reproduire ? écarter la contention de ressources ? comparer à un état
   connu-bon.
2. Seul l'environnement de développement publie tous ses ports à l'hôte - tous les environnements
   proches de la production n'exposent qu'un seul point d'entrée HTTP.
3. Le port 9091 (Actuator) ne doit **jamais** répondre depuis l'hôte, dans aucun environnement -
   c'est un signal d'anomalie immédiat et fiable.
4. Staging et les labs Kubernetes/Helm ne construisent jamais d'image localement - ils déploient
   toujours des images déjà publiées sur GHCR.
5. Le lab Azure n'a jamais été déployé - il n'a donc pas de ports réels à documenter, seulement une
   architecture prévue.

### Quiz Partie 13

1. Quelle est la toute première question à se poser face à un problème inconnu dans Taskira ?
2. Pourquoi le port 9091 ne devrait-il jamais répondre depuis l'hôte, quel que soit l'environnement ?
3. Quel est le seul port publié à l'hôte en production-like et en staging ?
4. Quels environnements construisent leurs propres images Docker localement, et lesquels réutilisent
   des images GHCR déjà publiées ?
5. Pourquoi le lab Azure n'apparaît-il pas avec de vrais ports dans le tableau de la section C ?

---

## Partie 14 — Préparation entretien

Cette dernière partie ne présente aucune technologie nouvelle - elle relie tout ce qui a été appris
dans les Parties 1 à 13 en un discours cohérent, tel qu'on le tiendrait face à un recruteur technique.
Toutes les questions/réponses détaillées de chaque partie sont déjà rassemblées dans
[`interview-notes.md`](interview-notes.md) ; cette partie sert de plan pour les relier entre elles.

### A. Le pitch en 60 secondes

*"Taskira est une application de gestion de projets/tickets - un monolithe modulaire, pas des
microservices, construit volontairement ainsi pour rester simple et cohérent tant qu'aucun besoin
réel ne justifie plus de complexité. Le runtime principal est Angular (SPA) → Nginx (reverse proxy en
production) → Spring Boot 4 (Java 21) → PostgreSQL. L'authentification repose sur une session côté
serveur avec cookie `HttpOnly`, pas du JWT - un choix de sécurité assumé et documenté. Le projet est
entièrement conteneurisé avec Docker, testé à plusieurs niveaux (unitaire, intégration avec de vrais
PostgreSQL via Testcontainers, et E2E avec Playwright), intégré en continu sur GitHub Actions avec
scans de qualité et de sécurité automatiques (SonarQube, CodeQL, Trivy), et publié sous forme
d'images versionnées sur un registre de containers (GHCR) avec un vrai pipeline de release qui
déploie et teste avant de publier quoi que ce soit officiellement. Kubernetes, Helm et Terraform/Azure
existent en plus comme labs d'apprentissage séparés, pas comme runtime de production actuel."*

### B. Le fil conducteur architecture (à dérouler si on demande "explique-moi l'architecture")

```
Navigateur
   │
   ▼
Angular (SPA, Partie 3)
   │  même origine, URL relative /api/v1 en production
   ▼
Nginx (reverse proxy, Partie 8) - sert l'Angular compilé, proxifie /api/*
   │
   ▼
Spring Boot (Partie 2) - Controller → Service → Repository → Entity
   │  session cookie HttpOnly + CSRF double-soumission (Partie 4)
   │  Flyway possède le schéma, Hibernate le valide seulement (Partie 2)
   ▼
PostgreSQL (Partie 2) - données dans un volume nommé, jamais dans le container
```

Autour de ce cœur : Actuator/Prometheus/Grafana pour l'observabilité (Partie 7), GitHub Actions pour
la CI/CD avec SonarQube/CodeQL/Trivy (Partie 6), un pipeline de release vers GHCR avec déploiement
staging réel avant publication (Partie 8), un mécanisme de sauvegarde/restauration testé
régulièrement (Partie 9), et trois labs d'apprentissage séparés (Kubernetes, Helm, Terraform/Azure -
Parties 10-12) qui n'affectent jamais le runtime principal.

### C. Par thème abordé en entretien → où retrouver la réponse détaillée

| Si on te demande... | Va voir |
|---|---|
| "Pourquoi pas de microservices ?" | AGENTS.md §2/§8 - simplicité et cohérence tant qu'aucun besoin réel ne justifie plus |
| "Comment tu gères l'authentification ?" | Partie 4 - session cookie, jamais JWT depuis la Phase 8 |
| "Comment tu es sûr que le schéma de base est cohérent avec le code ?" | Partie 2 - Flyway + `ddl-auto=validate` |
| "Comment tu testes un vrai comportement PostgreSQL sans base en mémoire ?" | Partie 5 - Testcontainers |
| "Comment ton CI empêche un code cassé d'arriver sur `main` ?" | Partie 6 - `CI Gate` obligatoire |
| "Comment tu déploies une nouvelle version ?" | Partie 8 - GHCR, tag versionné, staging réel, puis Release |
| "Comment tu es sûr que tes sauvegardes fonctionnent ?" | Partie 9 - restauration testée, jamais supposée |
| "Tu connais Kubernetes ?" | Parties 10-11 - lab réel pratiqué, jamais le runtime principal actuel |
| "Tu as de l'expérience cloud ?" | Partie 12 - architecture Azure conçue et validée hors ligne, jamais déployée sans autorisation/budget |
| "Comment tu diagnostiques un problème que tu n'as jamais vu ?" | Partie 13 - méthode générale |

### D. Ce que tu dois pouvoir faire seul, maintenant

- Démarrer/arrêter/redémarrer la stack de développement sans hésitation (Partie 1).
- Expliquer pourquoi les données survivent à la suppression d'un container (Partie 1).
- Lire une entité JPA et une migration Flyway et savoir laquelle des deux "possède" le schéma
  (Partie 2).
- Expliquer la différence entre un guard Angular (confort UX) et une vraie règle de sécurité
  (toujours côté backend) (Partie 3-4).
- Lancer toi-même les tests backend/frontend/E2E et lire un rapport d'échec (Partie 5).
- Lire un run GitHub Actions et savoir dans quel job chercher un échec (Partie 6).
- Vérifier la santé et les métriques réelles du backend (Partie 7).
- Expliquer tout le chemin d'un tag Git jusqu'à une image publiée et testée en staging (Partie 8).
- Sauvegarder et restaurer la base toi-même, et savoir pourquoi la restauration ne se fait jamais sur
  la base active (Partie 9).
- Situer Kubernetes/Helm/Azure comme des labs d'apprentissage, jamais comme le runtime réel
  aujourd'hui (Parties 10-12).
- Face à un problème encore jamais vu, appliquer une méthode plutôt que deviner (Partie 13).

### E. Auto-évaluation finale (30 questions, une par thème)

Ce ne sont pas de nouvelles questions - une sélection représentative parmi les 5 questions de quiz de
chacune des 13 parties précédentes. Si une réponse ne vient pas immédiatement, retourne relire la
partie correspondante avant de continuer.

1. (Partie 1) Pourquoi utiliser un volume Docker plutôt que le système de fichiers du container ?
2. (Partie 2) Que se passe-t-il si on modifie une migration Flyway déjà appliquée ?
3. (Partie 3) Pourquoi l'intercepteur Angular lit-il `document.cookie` à la main pour le CSRF ?
4. (Partie 4) Pourquoi Taskira est-il passé de JWT à une session cookie ?
5. (Partie 5) Pourquoi Testcontainers plutôt que H2 ?
6. (Partie 6) Pourquoi `CI Gate` a-t-il `if: always()` ?
7. (Partie 7) Pourquoi Actuator vit-il sur un port séparé de l'API ?
8. (Partie 8) Pourquoi chaque image GHCR a-t-elle deux tags ?
9. (Partie 9) Pourquoi ne restaure-t-on jamais directement sur la base active ?
10. (Partie 10) Pourquoi le résolveur DNS de Nginx pose-t-il problème en Kubernetes ?
11. (Partie 11) Pourquoi le Chart Helm a-t-il révélé un bug de démarrage jamais vu avec les manifestes bruts ?
12. (Partie 12) Pourquoi aucune ressource Azure réelle n'a-t-elle jamais été créée ?
13. (Partie 13) Quelle est la toute première question à se poser face à un problème inconnu ?

*(Les réponses complètes des 5 questions de chaque partie sont dans les sections "Quiz" correspondantes
de ce document, et les réponses d'entretien détaillées dans `interview-notes.md`.)*

### Résumé à retenir

1. Le pitch en 60 secondes doit couvrir : architecture (Angular → Nginx → Spring Boot → PostgreSQL),
   sécurité (session cookie), tests (multi-niveaux, PostgreSQL réel), CI/CD (GitHub Actions, scans de
   sécurité), déploiement (GHCR, staging réel avant release) et les labs (Kubernetes/Helm/Azure,
   jamais le runtime actuel).
2. Chaque décision technique de Taskira a une raison documentée (souvent un vrai bug rencontré et
   corrigé) - jamais "parce que c'est la technologie à la mode".
3. Ce manuel (`taskira-from-zero.md`) et les fiches associées (`commands-cheatsheet.md`,
   `troubleshooting.md`, `interview-notes.md`) restent des références à consulter après cette
   formation, pas seulement pendant.
4. `docs/architecture/` reste la documentation "pour quelqu'un qui sait déjà" - `docs/learning/`
   explique depuis zéro ; les deux se complètent, jamais en doublon.
5. Comprendre le "pourquoi" derrière chaque choix (pas seulement la commande à taper) est ce qui
   distingue une explication d'entretien solide d'une récitation de vocabulaire technique.

### Quiz Partie 14

1. Résume Taskira en 3 phrases, sans notes.
2. Sur quel document te reposes-tu pour retrouver une réponse d'entretien déjà préparée ?
3. Quelle est la différence de public entre `docs/architecture/` et `docs/learning/` ?
4. Cite un exemple de décision technique de Taskira motivée par un vrai bug plutôt qu'une préférence
   esthétique.
5. Quelle partie de cette formation dois-tu relire en premier si un point te semble encore flou ?

---

## Formation terminée

Les 14 parties de cette formation personnelle sont maintenant écrites. Les quatre documents
(`taskira-from-zero.md`, `commands-cheatsheet.md`, `troubleshooting.md`, `interview-notes.md`) restent
dans `docs/learning/` comme référence permanente, à consulter et enrichir au fil du temps - rien
n'empêche d'y revenir après cette session pour approfondir un point ou noter une nouvelle découverte.
