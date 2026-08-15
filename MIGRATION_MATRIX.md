# Matrice de migration Taskira

Dernière mise à jour : 2026-08-15.

Cette matrice suit l'avancement par capacité métier. Elle complète la [feuille de route des phases](docs/migration-matrix.md) et doit être mise à jour après chaque phase validée.

## Légende

- `Présent` : comportement applicatif disponible dans la baseline.
- `Couvert` : comportement exercé par le niveau de test indiqué.
- `Partiel` : couverture ou architecture amorcée, mais critère enterprise incomplet.
- `Absent` : aucun livrable correspondant dans le dépôt.
- `Planifié Pn` : attendu pendant la phase `n`, sans prétendre qu'il existe déjà.

## Capacités métier

| Module | Backend | Frontend | Tests rapides | Intégration PostgreSQL | E2E | Sonar | Staging | État et prochain critère |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Auth | Présent : session Spring Security, cookie `HttpOnly`/`SameSite=Lax`, CSRF double-soumission (P8) | Présent : login, guards, intercepteur, aucun jeton en `localStorage`/`sessionStorage` | Couvert : 12 tests session/CSRF dédiés (`SessionAuthenticationIT`) et 25 Vitest partagés | Couvert : session/CSRF/logout contre PostgreSQL réel via `RestTestClient` | Couvert : page publique, login/logout, login invalide, guard anonyme, refus USER sur admin | Terminé localement P4 | Planifié P15 | Rôles au-delà de USER/ADMIN restent à caractériser plus finement. |
| Users | Présent | Présent : administration | Absent dédié | Absent dédié | Partiel : un `USER` est refusé sur l'API et la route admin | Terminé localement P4 | Planifié P15 | Couvrir activation/désactivation et matrice complète des rôles. |
| Projects | Présent, sous `/api/v1` depuis P7 | Présent : liste, détail, board | Présent : service et MockMvc ciblés | Présent : repository/Flyway via Testcontainers | Couvert : create/update/archive | Terminé localement P4 | Planifié P15 | L'endpoint de désarchivage n'était pas dans le périmètre réel de P7 (API versioning/profils/ProblemDetail/transactions/verrouillage optimiste, voir la feuille de route des phases) et reste un gap sans phase assignée; ne pas le simuler. |
| Project Members | Présent | Présent dans les vues projet | Partiel via tests projet | Partiel via requêtes d'accès projet | Couvert : add/remove | Terminé localement P4 | Planifié P15 | Étendre aux cas owner/manager/member/non-member. |
| Tickets | Présent, sous `/api/v1` depuis P7; `@Version` protège les mises à jour concurrentes | Présent : liste et détail | Absent dédié | Absent dédié | Couvert : create/update/status/assign | Terminé localement P4 | Planifié P15 | L'endpoint de suppression n'était pas dans le périmètre réel de P7 et reste un gap sans phase assignée; couvrir aussi pagination et concurrence (verrouillage optimiste en place, E2E dédié au conflit 409 encore à écrire). |
| Comments | Présent | Présent dans le détail ticket | Absent dédié | Absent dédié | Couvert : create/update/delete | Terminé localement P4 | Planifié P15 | Étendre les tests de permissions. |
| Dashboard | Présent | Présent | Absent dédié | Absent dédié | Absent | Terminé localement P4 | Planifié P15 | Tester agrégats, requêtes et performance. |
| Audit | Partiel : audit JPA et historique ticket | Absent dédié | Absent dédié | Partiel : schéma historique existant | Absent | Terminé localement P4 | Planifié P15 | Créer le module et `audit_events` en P9. |
| Notifications | Absent | Absent | Absent | Absent | Absent | Planifié P4 après création | Planifié P15 | Ajouter notifications/Mailpit en P12 si cas métier validé. |
| Attachments | Absent | Absent | Absent | Absent | Absent | Planifié P4 après création | Planifié P15 | Ajouter stockage filesystem/Tika/sécurité upload en P13. |
| Exports | Absent | Absent | Absent | Absent | Absent | Planifié P4 après création | Planifié P15 | Ajouter POI/PDF/ZXing/Spring Batch en P14. |

## Socle transversal

| Domaine | État validé | Dette ou prochaine sortie |
| --- | --- | --- |
| Architecture | Terminé localement P5 : Spring Modulith vérifie les frontières et l'absence de cycle (`common`/`config`/`security` ouverts, `project`/`ticket`/`user` exposent des interfaces nommées); cycle `project`/`ticket` détecté et corrigé par port/adapter | Couche `api`/`application`/`domain`/`infrastructure` complète et événements métier (`TicketCreatedEvent`, etc.) restent à faire; le couplage direct aux repositories d'autres modules est documenté comme dette, pas éliminé. |
| API applicative | Terminé P7 : toutes les routes versionnées sous `/api/v1`; profils Spring `dev`/`test`/`prod` avec différences réelles vérifiées par test; erreurs `ProblemDetail` RFC 7807/9457 (backend et frontend alignés); verrouillage optimiste (`@Version`) avec 409 sur conflit, prouvé contre PostgreSQL réel | Couche transactionnelle limitée à `@Transactional` par service (pas de découpage applicatif/domaine dédié); pas de gestion différenciée des conflits de verrouillage côté frontend au-delà de l'affichage du message d'erreur. |
| Base de données | Terminé P7 : PostgreSQL 18.6, Flyway `V1`–`V7` (`V7` ajoute `version` à `users`/`projects`/`tickets`/`comments`), `ddl-auto=validate`; migration réelle par sauvegarde/restauration vérifiée (ADR-0017), ancien volume PostgreSQL 16 conservé intact; `V7` revérifié sur la base de développement réelle et déjà peuplée, pas seulement une base vide | Toute évolution de schéma par `V8+`. |
| Tests backend | 39 tests (20 rapides + 19 intégration Testcontainers/Flyway, dont `OptimisticLockingIT`, `SessionAuthenticationIT` et `DevAdminBootstrapIT`) | Étendre la couverture métier pour accompagner les prochaines phases; recalculer la couverture JaCoCo après P8. |
| Tests frontend | 25 Vitest (dont `AuthService`, guards et intercepteur réécrits pour la session/CSRF); lint 0 erreur/36 avertissements `any` | Étendre aux features au-delà de l'authentification et réduire la dette `any`; seuils de couverture recalibrés après P8, à revalider. |
| Tests navigateur | Playwright 1.62.1 : 9/9 sur la stack isolée `e2e/playwright/compose.e2e.yml`, entièrement migré aux cookies de session (aucun jeton bearer) | Désarchivage projet et suppression ticket après création des endpoints P7; maintenir le nettoyage intégral. |
| CI/CD | Terminé P3/P6 : `main` protégée (PR + `CI Gate` obligatoires, force-push/suppression interdits, pas de revue humaine requise); `ci.yml`, `quality.yml`, `security.yml` et `codeql.yml` tous vérifiés verts sur GitHub sur la PR #28 avant fusion (commit `a7463af`) | Rejouer ces checks sur la PR de phase 8 avant fusion. |
| Qualité et scans | Terminé P4/P6 : Quality Gate SonarQube `OK` vérifiée sur GitHub; CodeQL et Trivy verts sur GitHub | CVE post-P6 rescannées : voir la section sécurité post-P6 du rapport cumulatif. L'alerte CodeQL CSRF (`java/spring-disabled-csrf-protection`) attendue résolue par P8, à reconfirmer sur la PR. |
| Sécurité | Terminé P8 : session Spring Security cookie `HttpOnly`/`SameSite=Lax` (`Secure` en `prod`), CSRF double-soumission, CORS credentials, logout serveur, bootstrap admin dev idempotent (profil `dev` uniquement); backend reste autoritaire; plus aucun jeton en `localStorage`/`sessionStorage` ([ADR-0006](docs/adr/0006-session-cookie-auth.md)) | Rôles au-delà de USER/ADMIN à affiner au fil des prochaines phases métier. |
| Observabilité | Logs Spring standards | Request ID/logs en P9; Actuator/Micrometer/Prometheus/Grafana en P10. |
| Déploiement | Compose de développement | Nginx/images production en P11; staging en P15. |
| Kubernetes/Helm/Azure | Absents | Labs P17, P18 et P19 uniquement. |
