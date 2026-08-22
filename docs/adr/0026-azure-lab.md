# ADR-0026 — Lab Azure : architecture et Terraform, jamais déployé automatiquement

- Statut : Accepted
- Date : 2026-08-22

## Contexte

AGENTS.md §38 et §45 sont explicites : Azure reste « architecture, scripts, configuration,
documentation », jamais une dépense cloud automatique. « Ne jamais exécuter automatiquement
`terraform apply` créant des ressources payantes sans demande explicite. » La feuille de route
(`docs/migration-matrix.md`, P19) fixe le critère de sortie : « Architecture/scripts validés sans
dépense implicite; déploiement réel uniquement avec authentification et accord adaptés. »

**Ce lab ne déploie rien de réel.** Aucune commande créant une ressource Azure (`terraform apply`,
`terraform plan` contre un abonnement réel, `az deployment ...`) n'a été exécutée. Le code Terraform a
été validé uniquement avec `terraform validate` et `terraform fmt`, qui fonctionnent entièrement hors
ligne, sans identifiants Azure. Toute affirmation de comportement à l'exécution qui n'a pas pu être
vérifiée par ces moyens hors-ligne est explicitement marquée comme non vérifiée ci-dessous — conformément
à AGENTS.md §41 (interdiction des faux succès), il serait malhonnête de prétendre le contraire.

## Décision

### Architecture retenue

```text
Internet
   |
Application Gateway v2 (Standard_v2, IP publique unique, routage par chemin)
   | VNet, sous-réseau dédié
   +-- /api/*  -> backend  (Container App, ingress interne uniquement)
   +-- /*      -> frontend (Container App, ingress interne uniquement)
                     |
                 Container Apps Environment (intégré au VNet)
                     |
                 PostgreSQL Flexible Server (accès privé uniquement, sous-réseau délégué)
```

Un seul point d'entrée public, exactement comme `infra/docker-compose.prodlike.yml` (P11) et
l'`Ingress` unique des labs Kubernetes/Helm (P17/P18) : Application Gateway effectue le routage par
chemin (`/api/*` vers le backend, tout le reste vers le frontend) sous un **seul nom d'hôte public** —
ni le backend ni le frontend ne sont exposés directement à Internet.

### Pourquoi un seul nom d'hôte public est une contrainte réelle, pas une préférence

Le cookie de session de Taskira est `SameSite=Lax` depuis P8 ([ADR-0006](0006-session-cookie-auth.md)).
Un cookie `SameSite=Lax` n'est **jamais** envoyé sur une requête `fetch`/XHR cross-origin (seules les
navigations de premier niveau et les requêtes « sûres » en bénéficient) — déployer frontend et backend
sur deux noms d'hôte Azure distincts casserait silencieusement l'authentification sur toute requête
mutante (login, création, etc.). Ce n'est pas une hypothèse : c'est le même comportement de navigateur
qui a motivé la conception Nginx à origine unique de P11 dès le départ. Application Gateway resitue
cette même contrainte à la frontière Azure plutôt que dans un conteneur Nginx.

### Application Gateway plutôt que Front Door

Application Gateway (v2, régional, intégré au VNet) route directement vers les FQDN internes des
Container Apps sur le réseau privé, sans exposition publique d'aucun des deux services applicatifs et
sans avoir besoin de Private Link (réservé au tier Premium de Front Door, plus coûteux) ni de
restriction par plage d'IP (le contournement nécessaire avec le tier Standard de Front Door). Cohérent
avec la segmentation réseau déjà appliquée à `infra/docker-compose.prodlike.yml` (P11, trois réseaux
Docker) : tout reste interne sauf le point d'entrée unique.

### Container Apps (consumption), pas AKS

Azure Container Apps (plan consumption, sans serveur, facturé à l'usage — proche de zéro au repos) est
préféré à un AKS géré : Taskira reste un monolithe modulaire (AGENTS.md, principe général), pas une
architecture microservices justifiant la gestion d'un cluster complet. Les labs Kubernetes (P17) et Helm
(P18) restent les environnements dédiés à l'apprentissage de Kubernetes lui-même — ce lab Azure ne les
duplique pas, il modélise plutôt le déploiement le plus adapté à l'architecture réelle de Taskira.

### PostgreSQL Flexible Server 18, accès privé uniquement

Version 18 (confirmée disponible sur Azure au 22 août 2026, [Microsoft
Learn](https://learn.microsoft.com/en-us/azure/postgresql/configure-maintain/concepts-supported-versions))
— aligné sur la version 18.6 réellement utilisée depuis P6 ([ADR-0017](0017-postgresql-18-migration.md)).
Tier `Burstable` (`B1ms`), le moins coûteux, cohérent avec un lab jamais destiné à un trafic réel. Accès
réseau privé uniquement (intégration VNet, sous-réseau délégué) : jamais d'adresse IP publique, même
principe que `db_net` dans `infra/docker-compose.prodlike.yml` — aucun service autre que le backend ne
peut jamais l'atteindre.

### Identifiants GHCR requis même pour une image publique — une découverte réelle, pas supposée

Recherche documentaire avant d'écrire le moindre Terraform (pas une hypothèse) : contrairement à d'autres
registres, Azure Container Apps exige des identifiants explicites pour tirer une image depuis GHCR même
si l'image est publique ([Microsoft
Learn](https://learn.microsoft.com/en-us/azure/container-apps/github-actions)) — un jeton d'accès
personnel GitHub classique avec le scope `read:packages` est nécessaire. Modélisé comme une variable
Terraform sensible (`ghcr_pull_token`), sans valeur par défaut, jamais commitée — même discipline que
`postgres_password` et que `-PostgresPassword` de `scripts/up.ps1` (P17).

### Un vrai bug de schéma trouvé par `terraform validate`, pas supposé

`terraform init`/`terraform fmt`/`terraform validate` ont réellement tourné (aucun n'exige d'identifiant
Azure) et ont révélé une vraie erreur : `azurerm_private_dns_zone_virtual_network_link` sur le provider
`azurerm` 5.2.0 exige `private_dns_zone_id`, pas la paire `resource_group_name`/`private_dns_zone_name`
d'une génération plus ancienne du provider supposée à tort au premier jet. Corrigé, `terraform validate`
confirme désormais 0 erreur et 0 avertissement.

### Ce qui reste explicitement non vérifié

Aucune commande Terraform créant une ressource réelle n'a été exécutée (voir Contexte). En particulier,
le comportement réel d'Application Gateway routant vers les FQDN internes des Container Apps n'a **pas**
été observé contre un environnement réel — seule sa cohérence syntaxique (`terraform validate`) et sa
cohérence de conception avec la documentation Microsoft consultée sont établies. Un futur déploiement
réel devra le confirmer avant toute promotion de ce lab vers un statut « vérifié ».

## Conséquences

- Aucune ressource Azure n'existe ni n'est créée par ce travail — un abonnement Azure réel et un accord
  explicite du propriétaire du dépôt restent nécessaires avant tout `terraform apply` (AGENTS.md §38).
- Le jeton GHCR nécessaire au tirage d'image n'est jamais commité; comme pour les autres labs, un futur
  déploiement réel devra le fournir via une variable d'environnement ou un backend de secrets Azure
  (Key Vault), non modélisé plus avant ici pour rester dans le périmètre « architecture, pas
  implémentation complète de la gestion de secrets » de ce lab.
- Ce lab ne remplace pas et ne duplique pas les labs Kubernetes/Helm (P17/P18) : il modélise le
  déploiement Azure le plus adapté à l'architecture réelle de Taskira, pas un exercice Kubernetes
  supplémentaire.
