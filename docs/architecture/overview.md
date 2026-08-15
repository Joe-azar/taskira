# Vue d'ensemble de l'architecture

Statut : état courant documenté; cible progressive. Mise à jour : 2026-08-15.

## Architecture actuelle

```text
Navigateur
   |
   | :4200, REST JSON + bearer JWT vers :8080/api
   v
Angular 21 / TypeScript 5.9 / composants standalone
   |
   v
Spring Boot 3.5.11 / Java 21 / Spring MVC / Spring Security
   |
   | JPA/Hibernate, ddl-auto=validate
   v
PostgreSQL 16
   ^
   |
Flyway V1 à V6
```

Docker Compose fournit `frontend`, `backend` et `postgres` pour le développement. Le frontend utilise `ng serve` avec hot reload; il n'existe pas encore d'image frontend Nginx de production.

Le backend et le frontend sont déjà organisés par fonctionnalités. Les frontières backend restent conventionnelles : Spring Modulith et les tests de modules appartiennent à la phase 5.

## État de la migration

- Phase 0 : baseline Git terminée.
- Phase 1 : documentation terminée localement.
- Phase 2 : tests, seuils de couverture et 9/9 parcours E2E isolés validés; phase terminée localement.
- Phase 3 : run GitHub #3 vert sur la PR draft #1 au commit `6db6115`; seule la protection de `main` reste à activer.
- Phases 4 à 20 : planifiées.

Voir la [feuille de route](../migration-matrix.md) et le [rapport cumulatif](../../ENTERPRISE_MIGRATION_REPORT.md).

## Cible

```text
                         HTTPS
Navigateur ----------------|
                           v
                         Nginx
                    /                \
           Angular compilé          /api/v1
                                        |
                                        v
                    Monolithe modulaire Spring Boot
              identity | users | projects | tickets
              comments | dashboard | capacités futures
                                        |
                                        v
                                   PostgreSQL
                                        ^
                                        |
                                      Flyway
```

Autour du runtime principal : GitHub Actions, SonarQube et scans, Prometheus/Grafana, GHCR et staging. Kubernetes, Helm, Azure et les technologies conditionnelles restent des labs.

## Principes invariants

- Monolithe modulaire, feature-first; pas de microservices par défaut.
- API publique de module; aucun accès opportuniste au repository interne d'un autre module.
- Architecture hexagonale légère uniquement là où les ports/adapters réduisent réellement le couplage.
- Backend autorité de sécurité; frontend limité à l'expérience utilisateur.
- PostgreSQL et migrations Flyway immuables; Hibernate valide seulement.
- Migration incrémentale protégée par des tests et des critères de sortie observables.

## Documents spécialisés

- [Backend](backend.md)
- [Frontend](frontend.md)
- [Base de données](database.md)
- [Sécurité](security.md)
- [Stockage](storage.md)
- [Tests](testing.md)
- [CI/CD](ci-cd.md)
- [Observabilité](observability.md)
- [Déploiement](deployment.md)
