# Architecture frontend

Statut : Angular 21 opérationnel; upgrade et design system planifiés en phase 6.

## État actuel

Le frontend utilise Angular 21, TypeScript 5.9, RxJS, Reactive Forms, HttpClient, des composants standalone et des routes lazy-loaded.

```text
frontend/src/app/
├── core/       auth, guards, intercepteur, services et composants globaux
├── layout/     coque applicative
└── features/   auth, comments, dashboard, projects, tickets, users
```

Le JWT est actuellement stocké dans `localStorage`; ce comportement doit rester décrit comme transitoire jusqu'à la phase sécurité.

## Règles cible

- Une feature possède ses pages, services, modèles et état local.
- `core` contient uniquement les singletons et préoccupations globales.
- Un futur `shared` regroupe des éléments UI génériques, pas la logique métier.
- Signals pour l'état UI local/dérivé; RxJS pour HTTP et flux asynchrones.
- Reactive Forms pour formulaires et validation.
- Lazy loading pour les capacités principales.
- Les guards améliorent l'UX mais ne remplacent aucune autorisation backend.

## Tests actuels

Vingt tests Vitest couvrent l'authentification, les guards, l'intercepteur et le rendu asynchrone du login. Les 9 tests Playwright couvrent login/logout, login invalide, guard anonyme, refus admin pour `USER`, projet create/update/archive, membre add/remove, ticket create/update/status/assign et commentaire create/update/delete. La couverture unitaire des autres features reste à renforcer.

Le lint repose sur `angular-eslint` 21.4.0, ESLint 10.3.0 et `typescript-eslint` 8.59.2. Le run distant validé passe avec 0 erreur et 41 avertissements `any`; cette dette reste visible et doit diminuer progressivement.

## Évolutions planifiées

- P3 : lint, tests, couverture, build et E2E sont verts dans GitHub Actions; protéger `main` par les checks requis.
- P6 : Angular 22, Node 24, TypeScript compatible et Angular Material progressif.
- P8 : cookies/session, XSRF et suppression du stockage JWT après migration validée.
- P11 : build statique servi par Nginx; `ng serve` reste réservé au développement.
