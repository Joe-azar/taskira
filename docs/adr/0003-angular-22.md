# ADR-0003 — Migrer vers Angular 22

- Statut : Proposed
- Date : 2026-08-15

## Contexte

La baseline utilise Angular 21.2.x, TypeScript 5.9 et Node 22. Le référentiel cible Angular 22, Node 24 LTS et Angular Material, après mise en place du filet de tests et de la CI.

## Proposition

En P6, migrer par les outils Angular officiels vers les derniers patchs compatibles d'Angular/CLI 22, choisir la version TypeScript supportée et Node 24 LTS, puis introduire Material progressivement sans réécrire l'UI.

## Critères d'acceptation

- Vitest, Playwright et build production verts.
- Aucun `--force` aveugle ni erreur TypeScript ignorée.
- Bundle, lazy loading, formulaires et comportement visuel revus.

## État d'implémentation

Non implémenté; Angular 21 reste la version active.
