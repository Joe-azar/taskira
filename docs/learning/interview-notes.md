# Notes pour entretien — Taskira

Points clés et questions types, construits progressivement au fil de la formation.

## Partie 1 — Docker

**5 points à retenir :**
1. Image = modèle figé et réutilisable ; container = instance en cours d'exécution de ce modèle.
2. Docker Compose démarre plusieurs containers ensemble, dans le bon ordre, sur le même réseau,
   depuis un seul fichier YAML.
3. Un volume nommé survit à la suppression d'un container — c'est la vraie garantie de persistance
   des données, pas le container lui-même.
4. `docker compose down` (sans `-v`) est sûr ; `down -v` supprime aussi les volumes, donc les
   données, de façon définitive.
5. Un healthcheck vérifie qu'un service répond correctement, pas seulement qu'il est démarré — c'est
   ce qui permet à `depends_on: condition: service_healthy` d'attendre intelligemment.

**Question type :** *"Pourquoi utiliser un volume Docker plutôt que de laisser les données dans le
container PostgreSQL lui-même ?"*

**Réponse attendue :** Le système de fichiers d'un container est éphémère par nature — dès qu'on
supprime ou recrée le container (mise à jour d'image, `docker compose down`, etc.), tout ce qui a été
écrit à l'intérieur disparaît avec lui. Un volume nommé est un espace de stockage géré séparément par
Docker, monté dans le container mais qui existe indépendamment de son cycle de vie : on peut détruire
et recréer le container PostgreSQL autant de fois que nécessaire (mise à jour de version, correction
de configuration...), tant que le volume `taskira_postgres_data_pg18` existe, les données réelles
(utilisateurs, projets, tickets) reviennent intactes au redémarrage.

## Partie 2 — Backend + PostgreSQL + Flyway

**5 points à retenir :**
1. Controller → Service → Repository → Entity : chaque couche a une seule responsabilité.
2. Un DTO (`CreateTicketRequest`, `TicketResponse`...) n'est jamais l'Entity elle-même exposée
   directement au client.
3. Flyway possède le schéma ; `ddl-auto=validate` empêche Hibernate de le modifier tout seul.
4. Une migration Flyway déjà appliquée est immuable — toute évolution est un nouveau fichier.
5. `@Version` (verrouillage optimiste) évite qu'une modification concurrente écrase silencieusement
   celle d'un autre utilisateur — réponse `409 Conflict` à la place.

**Question type :** *"Que se passe-t-il si deux utilisateurs modifient le même ticket en même temps
dans Taskira ?"*

**Réponse attendue :** Chaque entité hérite de `AuditableEntity`, qui porte une colonne `version`
annotée `@Version` (verrouillage optimiste JPA). Quand un utilisateur charge un ticket, il reçoit
aussi sa version courante. À la sauvegarde, Hibernate génère un `UPDATE ... WHERE id = ? AND
version = ?` : si un autre utilisateur a déjà modifié (et donc incrémenté) la version entre-temps,
cette clause ne correspond à aucune ligne, Hibernate lève une `OptimisticLockingFailureException`,
et `GlobalExceptionHandler` la traduit en réponse HTTP `409 Conflict` plutôt que de laisser la
seconde sauvegarde écraser silencieusement la première.

## Partie 3 — Frontend Angular

**5 points à retenir :**
1. Une SPA (Single Page Application) ne recharge jamais la page entière pour naviguer.
2. Guard = autorise/bloque une navigation ; Interceptor = agit sur toutes les requêtes HTTP.
3. Le lazy loading télécharge le code d'une page seulement au moment d'y naviguer.
4. `environment.ts`/`environment.development.ts` séparent la config prod (URL relative, même
   origine que Nginx) de la config dev (URL absolue, ports différents).
5. Aucun jeton d'authentification n'est stocké côté client (`localStorage`/`sessionStorage`) — la
   session vit dans un cookie `HttpOnly` que JavaScript ne peut même pas lire.

**Question type :** *"Comment l'intercepteur Angular sait-il qu'il doit ajouter un en-tête CSRF, et
pourquoi ne peut-il pas simplement laisser le navigateur le faire automatiquement ?"*

**Réponse attendue :** L'intercepteur lit directement `document.cookie` pour trouver le cookie
`XSRF-TOKEN` (déposé par le backend, lisible en JavaScript contrairement au cookie de session) et
l'attache manuellement à l'en-tête `X-XSRF-TOKEN` de chaque requête sortante. Le mécanisme intégré
d'Angular (`withXsrfConfiguration()`) retient volontairement cet en-tête pour les requêtes
cross-origin par mesure de sécurité par défaut — mais frontend et backend tournent sur des origines
différentes dans tous les environnements de ce projet (ports différents en dev, protocole/hôte
distincts possibles ailleurs), donc ce mécanisme intégré ne ferait silencieusement rien. Lire le
cookie et poser l'en-tête à la main fonctionne quelle que soit l'origine.

## Partie 4 — Authentification et sécurité

**5 points à retenir :**
1. Taskira utilise une session serveur + cookie `TASKIRA_SESSION` - JWT a été entièrement retiré en
   Phase 8, aucun jeton n'est jamais stocké côté client.
2. `HttpOnly` protège le cookie de session contre le vol par JavaScript (XSS) ; `SameSite=Lax` +
   double cookie CSRF protègent contre les requêtes forgées par un site tiers.
3. Le backend est la seule vraie autorité de sécurité - tout guard Angular n'est qu'une amélioration
   d'expérience utilisateur.
4. Modèle "tout fermé par défaut" : seules `/auth/**`, Swagger et Actuator sont publics dans
   `SecurityConfig`.
5. BCrypt transforme chaque mot de passe en empreinte irréversible - même Taskira ne peut jamais
   retrouver un mot de passe en clair.

**Question type :** *"Pourquoi Taskira est-il passé de JWT à une session cookie ? Quel problème
réel cela résolvait-il ?"*

**Réponse attendue :** Avec le modèle JWT initial, le jeton était stocké dans `localStorage`,
accessible en JavaScript - donc vulnérable à un vol via une faille XSS, et CSRF était désactivé
puisqu'un jeton bearer n'est jamais envoyé automatiquement par le navigateur comme un cookie l'est.
Le modèle session + cookie `HttpOnly` retire complètement le jeton d'authentification de portée du
JavaScript (impossible à voler même via XSS), au prix de devoir réintroduire une vraie protection
CSRF (puisque cette fois le cookie de session, lui, est envoyé automatiquement par le navigateur à
chaque requête) - ce compromis est documenté dans ADR-0006.

## Partie 5 — Tests

**5 points à retenir :**
1. `*Test.java` (Surefire, rapide) vs `*IT.java` (Failsafe, intégration) - une convention stricte du
   projet, pas un détail cosmétique.
2. Mockito isole une classe en simulant ses dépendances ; Testcontainers teste avec une vraie base de
   données, jetable et isolée, jamais avec un simulateur.
3. Testcontainers ne teste pas seulement "avec une base" - il prouve que les migrations Flyway
   fonctionnent réellement depuis zéro, sur une base neuve, à chaque run.
4. Un pourcentage de couverture JaCoCo mesure ce qui est *exécuté*, jamais ce qui est *bien vérifié*.
5. Playwright est le seul niveau de test qui exerce la chaîne complète (Angular réel, backend réel,
   PostgreSQL réel) via un vrai navigateur.

**Question type :** *"Pourquoi ne pas simplement utiliser H2 (une base en mémoire) à la place de
Testcontainers pour aller plus vite ?"*

**Réponse attendue :** H2 n'est pas PostgreSQL - certaines fonctionnalités, certains types de
données, certaines contraintes ou certains comportements de requêtes diffèrent subtilement entre les
deux moteurs. Un test vert avec H2 ne garantit pas qu'il le resterait sur le vrai moteur utilisé en
production. Testcontainers démarre le *même* PostgreSQL (même image, même version) que celui utilisé
partout ailleurs dans le projet - un test qui passe avec Testcontainers est une preuve nettement plus
fiable qu'avec un simulateur. C'est une règle explicite du projet (AGENTS.md §15) : ne jamais utiliser
H2 pour simuler PostgreSQL.

## Partie 6 — GitHub Actions + SonarQube + CodeQL + Trivy

**5 points à retenir :**
1. Workflow (quand) / job (groupe d'étapes) / step (une action) - tout tourne sur une machine
   temporaire (runner) détruite après coup.
2. `CI Gate` est le seul check directement exigé par la protection de `main` ; il agrège Backend,
   Frontend et Containers-and-E2E avec `if: always()` pour échouer visiblement si l'un des trois a
   échoué plutôt que d'être silencieusement sauté.
3. SonarQube (qualité générale : bugs, code smells, duplication, couverture) et CodeQL (motifs de
   vulnérabilité précis : injection SQL, XSS...) sont complémentaires, pas redondants.
4. Trivy peut signaler une CVE qui ne vient pas du code Java/TypeScript de Taskira lui-même - une
   image Docker peut être vulnérable à cause d'une bibliothèque tierce ou d'un composant du système
   d'exploitation de l'image de base, même avec un code applicatif irréprochable.
5. `codeql.yml`, `security.yml` et `backup-restore-drill.yml` se déclenchent aussi chaque semaine,
   sans aucun changement de code - pour détecter une nouvelle CVE publiée entre-temps, pas seulement
   une régression introduite par un commit.

**Question type :** *"Pourquoi Taskira a-t-il plusieurs workflows GitHub Actions séparés
(`ci.yml`, `quality.yml`, `codeql.yml`, `security.yml`) plutôt qu'un seul fichier qui fait tout ?"*

**Réponse attendue :** Ces workflows n'ont ni le même déclencheur, ni le même objectif, ni la même
urgence. `ci.yml` doit tourner sur chaque push/PR et bloquer la fusion via `CI Gate` - il doit être
rapide et fiable. `quality.yml` (SonarQube) et `codeql.yml`/`security.yml` (CodeQL, Trivy) sont des
analyses de qualité et de sécurité plus lentes, qui tournent aussi sur un rythme hebdomadaire
indépendant du code (pour détecter une CVE nouvellement publiée), et qui ne sont volontairement pas
des checks obligatoires de fusion - un Quality Gate SonarQube rouge ou une CVE Trivy ne doit pas
bloquer un correctif urgent de la même façon qu'un test cassé. Séparer les fichiers permet aussi de
relire, faire échouer et corriger chaque préoccupation indépendamment, sans qu'un problème de
sécurité fasse échouer la compilation ou inversement.

## Partie 7 — Observabilité

**5 points à retenir :**
1. Actuator/Micrometer exposent des métriques ; Prometheus vient les scraper (chercher) toutes les
   15 secondes ; Grafana les affiche - Taskira ne pousse jamais rien vers eux.
2. Le port 9091 (Actuator) est séparé du port 8080 (API) et jamais publié à l'hôte - c'est la vraie
   frontière de sécurité, pas un mot de passe applicatif.
3. Spring Security raisonne par chemin d'URL, jamais par port - une règle explicite
   (`EndpointRequest.toAnyEndpoint().permitAll()`) est nécessaire même avec un port séparé, sinon
   même une requête interne au réseau Docker recevrait un `401`.
4. `readiness` ("peut recevoir du trafic maintenant ?", inclut la connexion PostgreSQL) et
   `liveness` ("le processus est-il vivant ?") répondent à deux questions différentes.
5. Les jauges métier (`taskira_tickets`, `taskira_projects`, `taskira_users_active`) ré-interrogent
   réellement la base à chaque scrape - jamais de valeur en cache ou périmée.

**Question type :** *"Pourquoi Actuator est-il exposé sur un port séparé (9091) plutôt que sur le
même port que l'API (8080) ?"*

**Réponse attendue :** Les endpoints Actuator (santé, métriques Prometheus) sont des informations
techniques internes destinées à l'infrastructure de monitoring, pas à un client applicatif classique.
Les séparer sur un port dédié permet de ne jamais publier ce port vers l'hôte dans
`infra/docker-compose.yml` - seul un autre container du même réseau Docker (comme `prometheus`) peut
l'atteindre. C'est cette segmentation réseau qui protège réellement ces endpoints, pas une
authentification applicative : Taskira tourne en Docker Compose, où il n'existe pas de mécanisme
d'authentification service-à-service standard à donner à Prometheus. Un piège réel rencontré en
Phase 10 : Spring Security évalue ses règles par chemin d'URL, jamais par port, donc une requête sur
le port 9091 traverse la même chaîne de filtres de sécurité que sur 8080 - sans la règle
`EndpointRequest.toAnyEndpoint().permitAll()` ajoutée explicitement, même une requête interne
légitime depuis Prometheus recevrait un `401`.

## Partie 8 — Nginx + production-like + GHCR + release/staging

**5 points à retenir :**
1. Nginx sert l'Angular *compilé* en production-like/staging, jamais le serveur de développement
   `ng serve` - `frontend/Dockerfile.prod` est un fichier distinct de `frontend/Dockerfile`.
2. Un multi-stage build (`FROM ... AS build` puis un second `FROM` propre) ne livre jamais le code
   source ni les outils de compilation dans l'image finale.
3. Le backend tourne en utilisateur non-root dédié (`taskira`, uid 10001), jamais en `root` -
   limite les dégâts possibles en cas de faille exploitée dans l'application.
4. Chaque image publiée sur GHCR porte deux tags - une version lisible (`v0.1.0`) ET le SHA du
   commit exact - jamais `latest` seul, pour rester traçable même si le tag de version est redéplacé.
5. Une GitHub Release n'apparaît que si `staging-smoke-test` (déploiement réel + suite Playwright
   complète contre ce déploiement) a réellement réussi - jamais sur la seule force d'un build vert.

**Question type :** *"Explique tout le chemin parcouru entre le moment où tu pousses un tag `v0.1.0`
et le moment où une GitHub Release apparaît."*

**Réponse attendue :** `release.yml` se déclenche uniquement sur un tag `v*.*.*`, jamais sur un push
normal. Le job `build-and-push` construit les images backend et frontend (`frontend/Dockerfile.prod`,
pas le Dockerfile de dev) et les pousse vers GHCR sous deux tags (version + SHA). Le job
`staging-smoke-test`, qui dépend du précédent, déploie *réellement* `docker-compose.staging.yml` en
utilisant ces images tout juste publiées (ce fichier ne construit jamais rien localement), attend que
les healthchecks passent à `healthy`, puis exécute la suite Playwright complète contre ce déploiement
réel - pas un mock, pas la stack E2E isolée habituelle. Seulement si ce job réussit entièrement, le
job `publish-release` s'exécute et crée une vraie GitHub Release avec notes auto-générées. À aucun
moment une Release n'est publiée sur la seule preuve qu'un build a compilé sans erreur.

## Partie 9 — Backup / restore

**5 points à retenir :**
1. Sauvegarder (`backup-postgres.ps1`, contre n'importe quelle base réelle) et restaurer
   (`restore-postgres.ps1`, toujours dans un container jetable séparé) sont deux scripts distincts -
   jamais une restauration directe dans le container `taskira-postgres` réel.
2. Une sauvegarde n'est fiable que si elle a déjà été restaurée avec succès au moins une fois - un
   fichier `.dump` jamais rechargé n'est qu'une hypothèse.
3. L'image officielle PostgreSQL démarre un serveur temporaire (pour l'initialisation) avant le vrai
   serveur - `pg_isready` seul peut répondre "prêt" contre ce serveur temporaire juste avant son
   arrêt ; une vraie requête SQL répétée traverse cette fenêtre au lieu d'entrer en course avec elle.
4. `Write-Host` écrit uniquement sur la console, jamais sur le flux de sortie de succès de
   PowerShell capturable par un appelant (`$x = & script.ps1`) - une expression nue en fin de script
   est la vraie valeur de retour.
5. Aucune base de production persistante n'existe encore chez Taskira - le workflow hebdomadaire ne
   protège pas de vraies données, il prouve en continu que le mécanisme fonctionne toujours.

**Question type :** *"Un bug a été trouvé uniquement par le tout premier run réel du workflow de
sauvegarde sur GitHub Actions, jamais en local avant. Comment est-ce possible si le cycle complet
avait déjà été testé à la main avant la fusion ?"*

**Réponse attendue :** Le workflow utilise `workflow_dispatch` comme déclencheur manuel en plus du
planning hebdomadaire - mais `workflow_dispatch` ne peut déclencher qu'un workflow qui existe déjà
sur la branche par défaut (`main`). Avant la fusion de la Pull Request qui l'introduit, ce fichier
n'existe nulle part sur `main`, donc il est impossible de le déclencher réellement sur GitHub Actions
avant fusion - seule une répétition manuelle des mêmes étapes en local était possible. Cette
répétition locale a testé la logique des scripts avec un texte de log synthétique reproduisant le
format attendu, mais pas la sémantique réelle de capture PowerShell (`Write-Host` vs valeur de
retour) telle qu'elle se produit vraiment quand un workflow GitHub Actions exécute
`$result = & backup-postgres.ps1`. C'est cette différence précise, invisible en répétition manuelle,
qui n'est apparue que lors du tout premier run réel sur le vrai runner GitHub Actions.

## Partie 10 — Kubernetes + kind

**5 points à retenir :**
1. Kubernetes orchestre des containers *à travers plusieurs machines* ; Docker Compose ne le fait que
   sur *une seule* - `kind` simule un cluster complet dans de simples containers Docker pour
   apprendre sans coût ni matériel dédié.
2. Pod (unité de base) / Deployment (combien de Pods + quelle image) / Service (adresse réseau
   stable) / Ingress (porte d'entrée HTTP publique) sont les briques que Docker Compose n'a pas.
3. Le DNS intégré de Docker (`127.0.0.11`, utilisé par `frontend/nginx/default.conf`) n'existe
   simplement pas dans l'espace réseau d'un Pod Kubernetes.
4. Même corrigé vers le DNS du cluster, le résolveur intégré de Nginx n'applique jamais la liste
   `search` de `/etc/resolv.conf` contrairement à `wget`/`curl`/la JVM - un nom de service doit être
   pleinement qualifié (`backend.taskira.svc.cluster.local`) pour lui, jamais un nom court.
5. PostgreSQL utilise un `Deployment` en stratégie `Recreate` (jamais `RollingUpdate`, qui bloquerait
   un second Pod incapable de monter un PVC `ReadWriteOnce` déjà tenu par le premier) - pas un
   `StatefulSet`, dont les garanties (identité stable, scaling ordonné) ne servent à rien pour une
   seule replica.

**Question type :** *"Le fichier `frontend/nginx/default.conf` fonctionne très bien dans Docker
Compose. Pourquoi a-t-il fallu le modifier pour Kubernetes, et comment cette modification a-t-elle
été faite sans casser les trois runtimes Compose qui l'utilisent déjà ?"*

**Réponse attendue :** Deux bugs réels, trouvés uniquement en démarrant réellement le cluster et en
envoyant une vraie requête à travers l'Ingress - aucune relecture de manifeste ne les aurait révélés.
D'abord, `resolver 127.0.0.11 valid=10s;` pointe vers le DNS intégré de Docker, absent de l'espace
réseau d'un Pod Kubernetes - `502` sur toute requête `/api/`. Ensuite, même en pointant vers le vrai
DNS du cluster, le nom court `backend` restait introuvable : le résolveur intégré de Nginx interroge
le DNS avec le nom littéral configuré, sans jamais appliquer la liste `search` du pod - contrairement
à des outils basés sur libc (`wget`, `curl`, la JVM), qui étendent automatiquement un nom court en
nom pleinement qualifié avant d'interroger le DNS. Plutôt que de modifier
`frontend/nginx/default.conf` lui-même (partagé et déjà validé par trois runtimes Docker Compose
distincts, un fichier que ce lab ne devait pas devenir une raison de toucher), un `ConfigMap`
Kubernetes remplace uniquement ce fichier *à l'intérieur du Pod* - identique au vrai fichier à deux
lignes près (le résolveur et le nom pleinement qualifié dans `proxy_pass`).

## Partie 11 — Helm

**5 points à retenir :**
1. Helm empaquette des manifestes Kubernetes paramétrables dans un Chart réutilisable - construit
   après avoir déjà maîtrisé les manifestes bruts de la Partie 10, jamais à leur place.
2. `helm lint`/`helm template` vérifient un Chart sans jamais toucher un vrai cluster ;
   `helm upgrade --install` installe ou met à jour en une seule commande.
3. La fonction Helm `required` fait échouer explicitement le rendu d'un Chart si une valeur sensible
   (le mot de passe PostgreSQL) manque - jamais de valeur par défaut codée en dur dans `values.yaml`.
4. `helm upgrade --install --wait` soumet tous les templates du Chart ensemble à l'API server,
   contrairement à des manifestes bruts appliqués un par un dans un ordre choisi - une vraie course
   de démarrage (backend avant que PostgreSQL ne soit prêt) est apparue à la toute première
   installation réelle.
5. Un `initContainer`, qui réutilise l'image PostgreSQL déjà déclarée et le même `pg_isready` que ses
   propres probes, bloque le Pod backend jusqu'à ce que PostgreSQL accepte réellement les connexions -
   élimine le crash-loop plutôt que de le tolérer.

**Question type :** *"Les manifestes Kubernetes bruts de la Partie 10 appliquaient PostgreSQL avant
le backend, sans problème. Pourquoi le même déploiement, une fois empaqueté en Chart Helm, a-t-il
révélé un vrai bug de démarrage ?"*

**Réponse attendue :** Les manifestes bruts sont appliqués explicitement dans un ordre choisi
(`postgres` avant `backend`) par le script qui les orchestre. `helm upgrade --install --wait`
fonctionne différemment : il soumet **tous** les templates du Chart à l'API server Kubernetes en
même temps, sans garantie d'ordre entre eux, et n'attend la disponibilité générale qu'à la toute fin.
La toute première installation réelle du Chart a donc fait crash-looper le Pod backend deux fois
(`Connection to postgres:5432 refused`) avant que le redémarrage automatique de Kubernetes ne finisse
par réussir une fois PostgreSQL prêt - un vrai bug, seulement visible en installant réellement le
Chart, jamais par une simple relecture de template. Corrigé avec un `initContainer` sur le Pod
backend : il réutilise l'image PostgreSQL déjà pinnée dans `values.yaml` et le même `pg_isready`
déjà utilisé par les probes de PostgreSQL lui-même pour bloquer le démarrage du container principal
tant que PostgreSQL n'accepte pas réellement les connexions - la façon Kubernetes-native d'exprimer
une dépendance de démarrage, qui élimine le crash-loop au lieu de simplement compter sur le
redémarrage automatique pour le masquer.

## Partie 12 — Terraform + Azure

**5 points à retenir :**
1. Terraform décrit l'infrastructure en code (IaC) - `terraform init`/`fmt`/`validate` sont
   entièrement hors ligne et sans coût ; `plan`/`apply` contactent le vrai cloud et peuvent créer de
   vraies ressources payantes.
2. L'architecture Azure envisagée (Application Gateway → Container Apps internes → PostgreSQL
   Flexible Server privé) reprend un principe déjà appliqué ailleurs dans Taskira : un seul point
   d'entrée public, tout le reste strictement interne.
3. Un seul nom d'hôte public n'est pas une préférence esthétique - le cookie de session
   `SameSite=Lax` de Taskira n'est jamais envoyé sur une requête cross-origin, donc frontend et
   backend ne peuvent pas vivre sur deux noms d'hôte Azure distincts sans casser l'authentification.
4. Azure Container Apps exige des identifiants explicites pour tirer une image GHCR même publique -
   une vraie contrainte documentaire découverte via Microsoft Learn avant d'écrire le moindre
   Terraform, pas supposée après coup.
5. **Aucune ressource Azure réelle n'a jamais été créée** pour ce lab - c'est le critère de sortie
   explicite de cette phase, respecté à la lettre (AGENTS.md §38 : jamais de dépense cloud
   automatique).

**Question type :** *"Tu dis avoir 'terminé' le lab Azure sans jamais avoir déployé quoi que ce soit
sur un vrai compte Azure. Comment peux-tu prouver que le code Terraform fonctionne réellement, alors ?"*

**Réponse attendue :** Trois commandes, toutes strictement hors ligne, ont été utilisées pour
vérifier ce code sans jamais contacter Azure ni créer de ressource : `terraform init` (télécharge le
provider public `azurerm` depuis le Registry Terraform, aucun identifiant Azure requis),
`terraform fmt` (vérifie le style) et surtout `terraform validate`, qui vérifie la cohérence
syntaxique et de schéma de chaque ressource déclarée. Cette dernière commande a d'ailleurs
réellement trouvé un vrai bug - `azurerm_private_dns_zone_virtual_network_link` exigeait
`private_dns_zone_id` sur la version du provider utilisée, pas la paire
`resource_group_name`/`private_dns_zone_name` supposée à tort au premier jet - la preuve concrète
que `validate` attrape de vraies erreurs sans jamais toucher au cloud. Ce que ces trois commandes ne
prouvent *pas*, et qui est documenté explicitement plutôt que passé sous silence (ADR-0026) : le
comportement réel de l'Application Gateway routant vers les FQDN internes des Container Apps n'a
jamais été observé contre un environnement réel - seules la cohérence syntaxique et la cohérence de
conception avec la documentation Microsoft consultée sont établies. `terraform plan`/`apply`
resteraient nécessaires pour une vérification complète, mais exigent un abonnement Azure réel et un
accord explicite du propriétaire du dépôt, qui n'a jamais été donné pour cette phase.

## Partie 13 — Diagnostic

**5 points à retenir :**
1. Face à un problème inconnu : d'abord "qu'est-ce qui a changé récemment ?" (`git status`,
   `git log`) - la cause la plus probable est presque toujours la dernière modification.
2. Identifier le niveau qui échoue avant de chercher une solution : container qui ne démarre pas ?
   qui tourne mais ne répond pas ? qui répond avec une erreur HTTP ? qui répond 200 avec un résultat
   faux ?
3. La contention de ressources (plusieurs stacks Docker tournant en même temps) a réellement produit
   des faux échecs pendant la migration de Taskira à plusieurs reprises - toujours à écarter avant de
   chercher un vrai bug de code.
4. Seul l'environnement de développement publie tous ses ports à l'hôte ; tous les environnements
   proches de la production (prod-like, staging, K8s, Helm) n'exposent jamais qu'un seul point
   d'entrée HTTP.
5. Le port 9091 (Actuator) ne doit jamais répondre depuis l'hôte, dans aucun environnement - un
   signal d'anomalie immédiat et fiable, indépendant de la partie de l'application concernée.

**Question type :** *"Le port 9091 répond depuis ta machine Windows alors que ça ne devrait jamais
arriver. Comment diagnostiques-tu ça, étape par étape ?"*

**Réponse attendue :** D'abord confirmer que c'est réellement anormal (Partie 7) : `9091` sert
Actuator, jamais publié à l'hôte dans `infra/docker-compose.yml` par conception - seul un autre
container du même réseau Docker devrait pouvoir l'atteindre. Étape suivante : "qu'est-ce qui a
changé récemment ?" (`git status`/`git diff` sur `infra/docker-compose.yml`) - la cause la plus
probable est une modification accidentelle de la section `ports:` du service `backend`, ajoutant une
publication `9091:9091` qui ne devrait pas exister. Vérifier ensuite avec
`docker inspect taskira-backend` ou en relisant directement le fichier Compose actif. Si le fichier
lui-même est intact, vérifier qu'aucun autre service (un ancien container jamais nettoyé, par
exemple) ne publie accidentellement ce même port sur l'hôte. La correction elle-même est simple une
fois la cause confirmée - retirer la publication du port puis `docker compose up -d` pour appliquer
le changement - mais la valeur de la démarche est de confirmer la cause avant d'agir, plutôt que de
supposer.

## Partie 14 — Préparation entretien

**5 points à retenir :**
1. Un pitch d'entretien solide couvre l'architecture (Angular → Nginx → Spring Boot → PostgreSQL), la
   sécurité (session cookie, pas JWT), les tests (multi-niveaux, PostgreSQL réel via Testcontainers),
   la CI/CD (GitHub Actions, scans de sécurité) et le déploiement (GHCR, staging réel avant release) -
   en une minute, sans lire de notes.
2. Kubernetes, Helm et Azure/Terraform sont des labs d'apprentissage pratiqués réellement, jamais le
   runtime de production actuel de Taskira - une nuance à énoncer clairement, jamais à laisser
   ambiguë.
3. Chaque décision technique de Taskira a une raison documentée, souvent un vrai bug rencontré et
   corrigé en le construisant (le résolveur DNS de Nginx en Kubernetes, la course PostgreSQL/backend
   sous Helm, la capture `Write-Host` du script de sauvegarde...) - pas une préférence esthétique ou
   une mode technique.
4. `docs/architecture/` documente pour quelqu'un qui connaît déjà le projet ; `docs/learning/`
   (ce document inclus) explique depuis zéro - les deux se complètent sans jamais faire doublon.
5. Face à une question d'entretien sur un point encore flou, la bonne réponse est de le dire
   honnêtement plutôt que d'improviser - exactement la même discipline appliquée tout au long de ce
   projet (ne jamais présenter comme terminé ce qui ne l'est pas réellement, AGENTS.md §41).

**Question type :** *"Décris-moi l'architecture de Taskira, du navigateur jusqu'à la base de
données."*

**Réponse attendue :** Le navigateur charge une application Angular (SPA) - en production, servie en
fichiers statiques compilés par Nginx, pas par le serveur de développement `ng serve`. Toute requête
`/api/*` est proxifiée par ce même Nginx vers le backend Spring Boot, sur la même origine que le
frontend - une contrainte réelle, pas un détail : le cookie de session `SameSite=Lax` de Taskira
n'est jamais envoyé sur une requête cross-origin. Le backend suit une architecture en couches
(Controller → Service → Repository → Entity) organisée par fonctionnalité métier (monolithe
modulaire, frontières vérifiées par Spring Modulith), authentifie via une session côté serveur avec
cookie `HttpOnly` et une protection CSRF à double cookie plutôt qu'un JWT stateless, et persiste dans
PostgreSQL dont le schéma est entièrement possédé par Flyway - Hibernate se contente de le valider,
jamais de le modifier. Autour de ce cœur applicatif : Actuator/Prometheus/Grafana pour
l'observabilité, une CI GitHub Actions avec scans de qualité et de sécurité, un pipeline de release
qui publie des images versionnées sur GHCR et les déploie réellement en staging avant toute
publication officielle, et un mécanisme de sauvegarde/restauration testé régulièrement. Kubernetes,
Helm et Azure existent en plus comme labs d'apprentissage séparés, jamais comme runtime de production
actuel.

---

Formation terminée. Ces notes couvrent les 14 parties de `taskira-from-zero.md` - à relire et
enrichir au fil du temps, pas seulement pendant cette session.
