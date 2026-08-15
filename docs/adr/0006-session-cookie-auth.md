# ADR-0006 — Migrer vers une session cookie sécurisée

- Statut : Accepted
- Date : 2026-08-15

## Contexte

Le modèle actuel est stateless avec JWT dans `localStorage` et CSRF désactivé. Le référentiel cible une architecture first-party réduisant l'exposition du jeton au JavaScript.

## Proposition

Après tests de caractérisation, migrer en P8 vers une session serveur et un cookie d'authentification `HttpOnly`, `SameSite` et `Secure` en staging/production. Activer CSRF compatible Angular, CORS credentials, protection de fixation, expiration et logout.

## Critères d'acceptation

- Login/logout, rôles, `401`/`403`, requêtes avec/sans CSRF testés.
- Aucun token d'authentification durable dans `localStorage` après bascule.
- Compatibilité locale documentée; aucun secret versionné.

## État d'implémentation

Implémenté et vérifié en phase 8, sur `feat/phase8-session-security`. Session Spring Security (`HttpSessionSecurityContextRepository`, `SessionCreationPolicy.IF_REQUIRED`), cookie `TASKIRA_SESSION` (`HttpOnly`, `SameSite=Lax`, `Secure` uniquement en profil `prod`). CSRF via `CookieCsrfTokenRepository.withHttpOnlyFalse()` avec le `CsrfTokenRequestAttributeHandler` explicite (le handler par défaut, `XorCsrfTokenRequestAttributeHandler`, masque le jeton pour la protection BREACH des vues rendues côté serveur et casse le pattern double-soumission qu'un client REST attend) et un `CsrfCookieFilter` dédié pour forcer la résolution du jeton et l'émission du cookie `XSRF-TOKEN` (autrement paresseuse et jamais déclenchée par un backend purement REST). Migration en 4 incréments atomiques, JWT retiré uniquement une fois le remplacement fonctionnel et testé (voir la section phase 8 du rapport cumulatif) : aucun jeton d'authentification en `localStorage`/`sessionStorage`, `AuthService`/guards/intercepteur Angular réécrits en conséquence, bootstrap admin dev idempotent ajouté. 39 tests backend, 25 Vitest, 9/9 Playwright verts; smoke test manuel de la vraie stack de développement confirmant `HttpOnly`/`SameSite=Lax`/`Secure=false` en local et l'invalidation de session au logout.
