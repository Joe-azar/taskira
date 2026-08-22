# Lab Azure

Voir [ADR-0026](adr/0026-azure-lab.md) pour les décisions complètes.

**Ce lab n'a jamais déployé de ressource Azure réelle.** Aucune commande créant une ressource
(`terraform apply`, `terraform plan` contre un abonnement réel) n'a été exécutée — seules
`terraform init` (télécharge le provider public, aucun identifiant Azure requis),
`terraform fmt` et `terraform validate` (entièrement hors ligne) ont tourné. Voir ADR-0026, section « Ce
qui reste explicitement non vérifié », pour ce qu'un déploiement réel devra confirmer.

## Architecture

```text
Internet
   |
Application Gateway v2 (Standard_v2, IP publique unique, routage par chemin)
   +-- /api/*  -> backend  (Container App, ingress interne uniquement)
   +-- /*      -> frontend (Container App, ingress interne uniquement)
                     |
                 Container Apps Environment (intégré au VNet)
                     |
                 PostgreSQL Flexible Server 18 (accès privé uniquement)
```

Un seul nom d'hôte public : contrainte réelle, pas une préférence — le cookie de session `SameSite=Lax`
de Taskira (P8) n'est jamais envoyé sur une requête `fetch`/XHR cross-origin, donc frontend et backend
ne peuvent pas vivre sur deux noms d'hôte distincts sans casser l'authentification sur toute requête
mutante.

## Fichiers

```text
labs/azure/
  versions.tf                  provider azurerm ~> 5.1
  variables.tf                 aucune valeur par défaut pour les secrets
  main.tf                      resource group
  network.tf                   VNet, 3 sous-réseaux (appgw/container-apps/postgres)
  database.tf                  PostgreSQL Flexible Server 18, accès privé uniquement
  container_apps.tf            Log Analytics, Container Apps Environment, backend/frontend
  application_gateway.tf       routage par chemin /api/* -> backend, /* -> frontend
  outputs.tf
  terraform.tfvars.example     gabarit, jamais de vraie valeur
```

## Valider (sans identifiants Azure, sans dépense)

```powershell
cd labs\azure
terraform init      # télécharge le provider public, aucun identifiant Azure requis
terraform fmt -check
terraform validate
```

## Déployer réellement (jamais fait automatiquement)

Nécessite un abonnement Azure réel, une authentification (`az login` ou équivalent) et l'accord explicite
du propriétaire du dépôt (AGENTS.md §38) :

```powershell
cp terraform.tfvars.example terraform.tfvars   # remplir avec de vraies valeurs, jamais commiter
terraform plan   -var-file=terraform.tfvars
terraform apply  -var-file=terraform.tfvars    # crée de vraies ressources facturées
```

## Une vraie découverte documentaire, pas supposée

Recherchée avant d'écrire le moindre Terraform : contrairement à d'autres registres, Azure Container
Apps exige des identifiants explicites pour tirer une image depuis GHCR même si l'image est publique
([Microsoft Learn](https://learn.microsoft.com/en-us/azure/container-apps/github-actions)) — modélisé
comme la variable sensible `ghcr_pull_token`, sans valeur par défaut.

## Vérification réelle effectuée

`terraform fmt -check` (0 problème après une correction d'alignement) et `terraform validate` (0 erreur,
0 avertissement) — un vrai bug de schéma trouvé et corrigé au passage :
`azurerm_private_dns_zone_virtual_network_link` en provider `azurerm` 5.x utilise `private_dns_zone_id`,
pas la paire `resource_group_name`/`private_dns_zone_name` d'une génération plus ancienne du provider.
