# Architecture de sécurité

Statut : session/cookie implémentée et fusionnée dans `main` depuis la phase 8 ([ADR-0006](../adr/0006-session-cookie-auth.md), `Accepted`). Le modèle JWT décrit dans une version antérieure de ce document a été entièrement retiré — voir « Retiré » ci-dessous.

## État actuel

```text
Angular -> cookie de session (TASKIRA_SESSION) -> Spring Security -> session serveur
```

- Session Spring Security côté serveur; aucun jeton d'authentification n'est émis ni stocké côté client.
- Cookie `TASKIRA_SESSION` : `HttpOnly`, `SameSite=Lax`, `Secure` en profil `prod` (`false` en `dev`/`test`, HTTP local).
- BCrypt pour les mots de passe.
- Protection CSRF active (`CookieCsrfTokenRepository`, `CsrfTokenRequestAttributeHandler`, `CsrfCookieFilter`) : toute requête mutante exige l'en-tête `X-XSRF-TOKEN` correspondant au cookie `XSRF-TOKEN`.
- CORS autorise l'origine configurée (`APP_CORS_ALLOWED_ORIGINS`) avec `credentials: true`.
- Logout serveur explicite (`POST /api/v1/auth/logout`) : invalide la session et efface le contexte de sécurité.
- Bootstrap admin dev idempotent (`DevAdminBootstrap`, profil `dev` uniquement, jamais actif en `prod`/`test`) — voir [README.md](../../README.md).
- Autorisations HTTP et méthode côté backend (`@PreAuthorize`, règles `SecurityConfig`); les guards Angular n'accordent ni ne retirent aucun droit serveur — ils cachent seulement une navigation.
- Actuator exposé sur un port de gestion isolé (`management.server.port`, `9091` par défaut, jamais publié à l'hôte) — `EndpointRequest.toAnyEndpoint().permitAll()` documente explicitement que la séparation de port seule ne suffit pas (la correspondance Spring Security est fondée sur le chemin, pas sur le port).
- Réponses d'erreur standardisées `ProblemDetail` (`ProblemDetails.of(...)`), y compris `401`/`403`, avec `requestId` de corrélation.

Le backend reste l'autorité de sécurité. Un bouton caché ou un guard Angular n'accorde et ne retire aucun droit serveur.

## Retiré : authentification JWT

Une version antérieure de ce document décrivait un modèle JWT stateless (`localStorage`, CSRF désactivé) comme état courant. Ce modèle a été entièrement retiré en phase 8 : `JwtAuthenticationFilter`, `JwtService`, `JwtProperties`, la dépendance `io.jsonwebtoken` et tout jeton côté client ont été supprimés. Ne jamais les réintroduire (AGENTS.md §11) — voir [ADR-0006](../adr/0006-session-cookie-auth.md) pour le détail complet de la migration.

## Modules avec surface de sécurité propre

Chaque module ajouté depuis P8 applique les mêmes règles d'accès (session + CSRF + autorisation par rôle/appartenance) sans mécanisme parallèle :

- `attachments` (P13) : upload/téléchargement/suppression réservés aux membres du projet; type de fichier réellement détecté (Apache Tika), jamais le `Content-Type` déclaré.
- `exports` (P14) : export en masse réservé à `ADMIN`; exports synchrones soumis aux mêmes règles que les endpoints JSON qu'ils reflètent.
- `audit` (P9) : lecture des événements d'audit réservée à `ADMIN`.

## Règles permanentes

- Aucun secret, mot de passe ou jeton dans Git, les fixtures, logs ou rapports.
- Aucun mot de passe, cookie de session, jeton CSRF ou chaîne de connexion DB dans les logs.
- Tester ADMIN, USER, propriétaire, membre et non-membre côté backend pour toute nouvelle règle d'autorisation.
- Revoir Swagger, Actuator, uploads, erreurs et proxy à chaque phase qui les concerne.
