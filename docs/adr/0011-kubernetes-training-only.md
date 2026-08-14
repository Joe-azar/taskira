# ADR-0011 — Réserver Kubernetes au laboratoire de formation

- Statut : Accepted
- Date : 2026-08-15

## Contexte

L'objectif inclut l'apprentissage Kubernetes, mais la taille et le mode de déploiement Taskira ne justifient pas d'en faire le runtime de production principal.

## Décision

Créer en P17 un lab kind/k3d avec manifests, Ingress, probes, ConfigMaps, Secrets, PVC, scaling et rollback. Les images sont construites avant les pods. Compose reste le runtime principal.

Helm arrive seulement après validation des manifests bruts.

## Conséquences

- Apprentissage réaliste sans complexifier l'exploitation courante.
- Lab destructible et sans secret réel.
- Pas de promesse de production Kubernetes.

## État d'implémentation

Décision de portée acceptée; le lab P17 n'est pas créé.
