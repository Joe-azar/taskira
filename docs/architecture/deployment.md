# Architecture de déploiement

Statut : développement Compose opérationnel; modes production-like, staging et labs planifiés.

## Développement actuel

```text
Angular dev server :4200
Spring Boot       :8080
PostgreSQL        :5432
```

Docker Compose reste l'environnement de référence. Angular conserve le hot reload; le backend utilise une image multi-stage Java 21.

## Production-like P11

```text
HTTPS -> Nginx -> Angular statique
              -> /api/v1 -> Spring Boot -> PostgreSQL
```

Le frontend de production sera construit avec Node puis servi par Nginx. Les ports backend et PostgreSQL ne seront pas exposés publiquement. Les images devront utiliser runtime minimal, utilisateur non-root, healthchecks et aucun secret embarqué.

## Livraison P15/P16

- Images GHCR versionnées par SemVer/SHA.
- Staging Ubuntu/Compose/Nginx et Playwright post-déploiement.
- Production manuelle, versionnée et rollbackable.
- Backups PostgreSQL avec restauration testée, pas seulement `pg_dump` produit.

## Labs

- P17 Kubernetes, P18 Helm et P19 Azure.
- Aucun de ces labs ne remplace le runtime principal ni ne déclenche une dépense cloud sans contexte explicite.

Voir [ADR-0008](../adr/0008-docker-compose.md) et [ADR-0011](../adr/0011-kubernetes-training-only.md).
