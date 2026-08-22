# Vue d'ensemble de l'architecture

Statut : reflète l'état réellement fusionné dans `main` (phases 0 à 19). Pour l'historique complet phase par phase, voir la [feuille de route](../migration-matrix.md) et le [rapport cumulatif](../../ENTERPRISE_MIGRATION_REPORT.md) — ce document ne duplique pas leur détail.

## Runtime de développement

```text
Navigateur
   |
   | :4200, REST JSON + cookie de session (TASKIRA_SESSION) + CSRF vers :8080/api/v1
   v
Angular 22.1.2 / TypeScript 6.0.3 / composants standalone, ng serve avec hot reload
   |
   v
Spring Boot 4.1.x / Java 21 / Spring MVC / Spring Security (session, pas de JWT)
   |
   | JPA/Hibernate 7.x, ddl-auto=validate
   v
PostgreSQL 18.6
   ^
   |
Flyway V1 à V10
```

`infra/docker-compose.yml` fournit `frontend`, `backend`, `postgres` et (profil `observability` ou service dédié selon le fichier) Mailpit/Prometheus/Grafana pour le développement local.

## Runtime production-like, staging et release

```text
Navigateur
   | HTTPS (staging/prod) ou HTTP local (prodlike)
   v
Nginx non-root (image de production, sert Angular compilé + proxifie /api/*)
   |
   v
Backend Spring Boot non-root, ingress interne uniquement
   |
   v
PostgreSQL (accès interne uniquement)
```

`infra/docker-compose.prodlike.yml` (P11) et `infra/docker-compose.staging.yml` (P15) partagent cette topologie à réseaux Docker segmentés (le frontend ne peut jamais joindre PostgreSQL). `staging` ne construit jamais d'image localement : il déploie une image déjà publiée sur GHCR (`ghcr.io/joe-azar/taskira-{backend,frontend}`) par `.github/workflows/release.yml`, déclenché sur un tag `v*.*.*` — voir [deployment.md](deployment.md).

Le backend est organisé en modules Spring Modulith (`auth`, `user`, `project`, `ticket`, `comment`, `dashboard`, `audit`, `notifications`, `attachments`, `exports`; `common`/`config`/`security` transversaux) — voir [backend.md](backend.md) et [modules.md](modules.md).

## Labs (hors runtime principal)

`labs/kubernetes/`, `labs/helm/` et `labs/azure/` démontrent des chemins de déploiement alternatifs (Kubernetes local, chart Helm, architecture Azure en Terraform) sans jamais devenir des dépendances du runtime principal ni de la CI — voir [kubernetes-lab.md](kubernetes-lab.md), [helm-lab.md](helm-lab.md) et [azure-lab.md](azure-lab.md).

## Principes invariants

- Monolithe modulaire, feature-first; pas de microservices par défaut.
- API publique de module (`@NamedInterface`); aucun accès opportuniste au repository interne d'un autre module sans qu'il soit explicitement exposé.
- Architecture hexagonale légère uniquement là où les ports/adapters réduisent réellement le couplage (ex. `DocumentStorage`).
- Backend autorité de sécurité; frontend limité à l'expérience utilisateur — un guard Angular n'accorde ni ne retire aucun droit serveur.
- PostgreSQL et migrations Flyway immuables une fois appliquées; Hibernate valide seulement (`ddl-auto=validate`).
- Migration incrémentale protégée par des tests et des critères de sortie observables — jamais une phase déclarée terminée sans preuve dans le dépôt, les tests ou les workflows.

## Documents spécialisés

- [Backend](backend.md)
- [Frontend](frontend.md)
- [Base de données](database.md)
- [Sécurité](security.md)
- [Stockage documentaire](storage.md)
- [Tests](../testing-strategy.md)
- [CI/CD](ci-cd.md)
- [Observabilité](observability.md)
- [Déploiement](deployment.md)
- [Sauvegarde et restauration](backup.md)
- [Lab Kubernetes](kubernetes-lab.md)
- [Lab Helm](helm-lab.md)
- [Lab Azure](azure-lab.md)
