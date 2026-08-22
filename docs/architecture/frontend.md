# Architecture frontend

Statut : Angular 22.1.2 (P6), authentification session/cookie sans jeton client (P8). Design system Angular Material intégré progressivement depuis P6.

## État actuel

Le frontend utilise Angular 22.1.2, TypeScript 6.0.3, RxJS, Reactive Forms, HttpClient, des composants standalone et des routes lazy-loaded.

```text
frontend/src/app/
├── core/       auth (session, pas de jeton), guards, intercepteur, services et composants globaux
├── layout/     coque applicative
└── features/   auth, comments, dashboard, projects, tickets, users
```

Aucun jeton d'authentification n'est stocké côté client (`localStorage`/`sessionStorage`) : l'intercepteur HTTP lit le cookie CSRF (`document.cookie`) et attache `X-XSRF-TOKEN` sur les requêtes mutantes; la session elle-même est un cookie `HttpOnly` que le JavaScript ne peut pas lire — voir [security.md](security.md) et [ADR-0006](../adr/0006-session-cookie-auth.md).

## Règles cible

- Une feature possède ses pages, services, modèles et état local.
- `core` contient uniquement les singletons et préoccupations globales.
- Un futur `shared` regroupe des éléments UI génériques, pas la logique métier.
- Signals pour l'état UI local/dérivé; RxJS pour HTTP et flux asynchrones.
- Reactive Forms pour formulaires et validation.
- Lazy loading pour les capacités principales.
- Les guards améliorent l'UX mais ne remplacent aucune autorisation backend.

## Tests

Vitest couvre l'authentification (session, pas de jeton), les guards, l'intercepteur et le rendu asynchrone du login; Playwright couvre les parcours critiques (auth, projets, membres, tickets, commentaires) sur la stack E2E isolée. Voir [testing-strategy.md](../testing-strategy.md) pour la stratégie et `ENTERPRISE_MIGRATION_REPORT.md` pour les nombres du run le plus récent — ne jamais réutiliser un ancien chiffre ici.

Le lint repose sur `angular-eslint`/ESLint/`typescript-eslint` (versions dans `frontend/package.json`); une dette de typage `any` reste visible et diminue progressivement sans désactiver la règle.

## Runtime de production

Depuis P11, le build Angular de production est servi par un Nginx non-root dédié (`frontend/Dockerfile.prod`, `frontend/nginx/default.conf`) qui proxifie `/api/*` vers le backend sur la même origine — `ng serve` reste réservé au développement local.
