# Architecture de sécurité

Statut : modèle actuel JWT documenté; migration session/cookies proposée pour la phase 8.

## État actuel

- Spring Security stateless avec bearer JWT.
- BCrypt pour les mots de passe.
- Jeton conservé par Angular dans `localStorage`.
- CSRF désactivé pour le modèle bearer actuel.
- CORS autorise l'origine locale configurée.
- Autorisations HTTP et méthode côté backend; guards côté frontend.
- Réponses JSON `401`/`403` couvertes par tests.

Le backend est l'autorité de sécurité. Un bouton caché ou un guard Angular n'accorde et ne retire aucun droit serveur.

## Cible proposée P8

```text
Angular -> cookie de session -> Spring Security -> session serveur
```

- Cookie d'authentification `HttpOnly`, `SameSite`, `Secure` en staging/production.
- Protection CSRF Spring/Angular testée sur les requêtes modifiantes.
- CORS credentials, fixation de session, expiration et logout explicites.
- Aucun JWT conservé dans `localStorage` après migration complète.
- Bootstrap admin local idempotent par variables d'environnement, désactivé hors dev.

Cette cible est `Proposed` dans [ADR-0006](../adr/0006-session-cookie-auth.md); elle n'est pas encore implémentée.

## Règles permanentes

- Aucun secret, mot de passe ou jeton dans Git, les fixtures, logs ou rapports.
- Aucun mot de passe, cookie, token CSRF, secret JWT ou chaîne DB dans les logs.
- Tester ADMIN, USER, owner, membre et non-membre côté backend.
- Revoir Swagger, Actuator, uploads, erreurs et proxy à chaque phase concernée.
