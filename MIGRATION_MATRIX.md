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
| Auth | Présent : JWT stateless | Présent : login, guards, intercepteur | Partiel : sécurité backend et 20 Vitest partagés | Partiel : démarrage Spring/Flyway, pas de scénario auth dédié | Couvert : page publique, login/logout, login invalide et guard anonyme | Terminé localement P4 | Planifié P15 | Caractériser les rôles, puis migrer session/CSRF en P8. |
| Users | Présent | Présent : administration | Absent dédié | Absent dédié | Partiel : un `USER` est refusé sur l'API et la route admin | Terminé localement P4 | Planifié P15 | Couvrir activation/désactivation et matrice complète des rôles. |
| Projects | Présent | Présent : liste, détail, board | Présent : service et MockMvc ciblés | Présent : repository/Flyway via Testcontainers | Couvert : create/update/archive | Terminé localement P4 | Planifié P15 | Ajouter l'endpoint de désarchivage et son E2E en P7; ne pas le simuler. |
| Project Members | Présent | Présent dans les vues projet | Partiel via tests projet | Partiel via requêtes d'accès projet | Couvert : add/remove | Terminé localement P4 | Planifié P15 | Étendre aux cas owner/manager/member/non-member. |
| Tickets | Présent | Présent : liste et détail | Absent dédié | Absent dédié | Couvert : create/update/status/assign | Terminé localement P4 | Planifié P15 | Ajouter l'endpoint de suppression et son E2E en P7; couvrir aussi pagination et concurrence. |
| Comments | Présent | Présent dans le détail ticket | Absent dédié | Absent dédié | Couvert : create/update/delete | Terminé localement P4 | Planifié P15 | Étendre les tests de permissions. |
| Dashboard | Présent | Présent | Absent dédié | Absent dédié | Absent | Terminé localement P4 | Planifié P15 | Tester agrégats, requêtes et performance. |
| Audit | Partiel : audit JPA et historique ticket | Absent dédié | Absent dédié | Partiel : schéma historique existant | Absent | Terminé localement P4 | Planifié P15 | Créer le module et `audit_events` en P9. |
| Notifications | Absent | Absent | Absent | Absent | Absent | Planifié P4 après création | Planifié P15 | Ajouter notifications/Mailpit en P12 si cas métier validé. |
| Attachments | Absent | Absent | Absent | Absent | Absent | Planifié P4 après création | Planifié P15 | Ajouter stockage filesystem/Tika/sécurité upload en P13. |
| Exports | Absent | Absent | Absent | Absent | Absent | Planifié P4 après création | Planifié P15 | Ajouter POI/PDF/ZXing/Spring Batch en P14. |

## Socle transversal

| Domaine | État validé | Dette ou prochaine sortie |
| --- | --- | --- |
| Architecture | Feature-first existant; décision monolithe modulaire acceptée | Frontières publiques et Spring Modulith en P5. |
| Base de données | PostgreSQL 16, Flyway `V1`–`V6`, `ddl-auto=validate` | Upgrade PostgreSQL en P6; toute évolution par `V7+`. |
| Tests backend | 11 rapides + 3 intégration Testcontainers/Flyway; JaCoCo lignes 20,17 %, seuil 19 % vert | Étendre la couverture métier pour accompagner les prochaines phases. |
| Tests frontend | 20 Vitest; couverture lignes 11,84 %, branches 11,27 %, fonctions 11,78 %, seuils verts; lint 0 erreur/41 avertissements `any` | Étendre aux features au-delà de l'authentification et réduire la dette `any`. |
| Tests navigateur | Playwright 1.62.1 : 9/9 en 1,3 min sur la stack isolée `e2e/playwright/compose.e2e.yml` | Désarchivage projet et suppression ticket après création des endpoints P7; maintenir le nettoyage intégral. |
| CI/CD | Partiel : PR draft #1, run GitHub #3 vert sur `6db6115` avec Backend, Frontend lint/tests/build, Containers and E2E et CI Gate | Activer la protection de `main`; GHCR/staging/release restent en P15. |
| Qualité et scans | Terminé localement P4 : Quality Gate SonarQube `OK` (0 bug, 0 vulnérabilité, 0 hotspot, 24 code smells, couverture 13,0 %); Trivy dépendances/images et CodeQL exécutés | 21 CVE backend (17 HIGH/4 CRITICAL) et 8 CVE HIGH frontend restent ouvertes, corrigibles en P6; run GitHub distant non vérifié faute d'authentification. |
| Sécurité | Backend autoritaire; JWT/localStorage actuel | Session HttpOnly, CSRF, CORS credentials et bootstrap dev en P8. |
| Observabilité | Logs Spring standards | Request ID/logs en P9; Actuator/Micrometer/Prometheus/Grafana en P10. |
| Déploiement | Compose de développement | Nginx/images production en P11; staging en P15. |
| Kubernetes/Helm/Azure | Absents | Labs P17, P18 et P19 uniquement. |
