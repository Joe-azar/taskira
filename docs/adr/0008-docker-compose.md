# ADR-0008 — Garder Docker Compose comme runtime principal

- Statut : Accepted
- Date : 2026-08-15

## Contexte

Taskira fonctionne localement avec frontend, backend et PostgreSQL sous Docker Compose. L'application ne nécessite pas un orchestrateur distribué pour son runtime principal.

## Décision

Docker Compose reste la référence pour développement, tests intégrés, production-like et staging sur VM. Le profil minimal ne démarre que les services utiles; les outils qualité/observabilité sont séparables.

Kubernetes reste un lab et ne remplace pas Compose pour la production principale actuelle.

## Conséquences

- Onboarding et diagnostic simples.
- Plusieurs fichiers/profils pourront séparer dev, test, qualité et production-like.
- Les secrets de staging/production restent externes aux fichiers versionnés.

## État d'implémentation

Compose de développement est actif; profils production-like et staging restent planifiés.
