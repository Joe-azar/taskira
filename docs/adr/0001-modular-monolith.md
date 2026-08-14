# ADR-0001 — Adopter un monolithe modulaire

- Statut : Accepted
- Date : 2026-08-15

## Contexte

Taskira est une application cohérente de gestion de projets et tickets, livrée avec une API, un frontend et une base. Son échelle actuelle ne justifie pas les coûts réseau, données et exploitation des microservices.

## Décision

Conserver une seule unité de déploiement backend et une base relationnelle, organisées par capacités métier. Chaque module expose une API interne publique; aucun module ne consomme directement le repository interne d'un autre. Les couches hexagonales ne sont introduites que pour les domaines complexes ou les adapters externes.

Spring Modulith vérifiera les frontières en P5. Une extraction future exige besoin mesuré et nouvel ADR.

## Conséquences

- Transactions et déploiement simples.
- Modularisation progressive sans réécriture.
- Discipline nécessaire pour éviter cycles et dossier `shared` fourre-tout.

## État d'implémentation

Le code est feature-first, mais les frontières ne sont pas encore automatisées; P5 reste planifiée.
