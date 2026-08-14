# ADR-0006 — Migrer vers une session cookie sécurisée

- Statut : Proposed
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

Non implémenté; le JWT actuel reste actif.
