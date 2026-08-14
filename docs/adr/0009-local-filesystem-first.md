# ADR-0009 — Commencer le stockage par le filesystem local

- Statut : Proposed
- Date : 2026-08-15

## Contexte

Le module de pièces jointes n'existe pas encore. Introduire immédiatement MinIO ou un cloud ajouterait une dépendance avant stabilisation du modèle et des règles de sécurité.

## Proposition

En P13, définir le port `DocumentStorage` puis livrer `LocalFileSystemStorage` en premier. Stocker les métadonnées en base, générer les noms techniques et contrôler MIME réel, taille, hash, chemin et droits.

MinIO et Azure Blob seront des adapters ultérieurs activés par configuration. ClamAV reste optionnel.

## Conséquences

- Développement simple et port testable.
- Nécessité de gérer permissions filesystem, atomicité et nettoyage.
- Migration vers un stockage objet possible sans changer les cas d'usage.

## État d'implémentation

Non implémenté; aucun module attachments n'est présent.
