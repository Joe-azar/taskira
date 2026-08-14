# ADR-0014 — Migrer incrémentalement avec un filet de tests préalable

- Statut : Accepted
- Date : 2026-08-15

## Contexte

La cible cumule architecture, upgrades, sécurité et infrastructure. Une transformation en une passe rendrait les régressions et leur cause difficiles à isoler.

## Décision

Suivre les phases 0–20, écrire les tests de caractérisation avant les changements risqués et exiger le critère de sortie avant de déclarer une phase terminée. Une correction commence par un test de régression. Un lot ne mélange pas évolution métier, refactor majeur et upgrade sans nécessité démontrée.

Les migrations de schéma restent additives et testées sur PostgreSQL réel.

## Conséquences

- Diagnostic plus précis et rollback plus sûr.
- Certaines phases restent honnêtement `Partielle` malgré des tests verts si tous les critères ne sont pas livrés.
- La CI et les Quality Gates automatiseront progressivement cette discipline.

## État d'implémentation

Décision appliquée; la phase 2 a été déclarée terminée localement seulement après validation des tests rapides, intégration, couverture et 9 parcours E2E isolés. La phase 3 reste partielle tant que ses critères distants ne sont pas atteints.
