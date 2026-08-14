# Architecture de stockage documentaire

Statut : proposition pour la phase 13; aucun module de pièces jointes n'est actuellement implémenté.

## Port proposé

```java
public interface DocumentStorage {
    StoredDocument store(...);
    InputStream read(...);
    void delete(...);
    boolean exists(...);
}
```

La première implémentation proposée est `LocalFileSystemStorage`, conformément à [ADR-0009](../adr/0009-local-filesystem-first.md). Un adapter MinIO ou Azure Blob ne sera ajouté qu'après stabilisation du port et besoin réel.

## Métadonnées prévues

- UUID et nom technique;
- nom original;
- MIME détecté et extension;
- taille et SHA-256;
- auteur, date, ticket/projet et statut.

## Sécurité prévue

- Taille, extension, double extension et MIME réel détecté avec Tika.
- Protection path traversal et noms techniques générés.
- Autorisation backend selon projet/ticket.
- Aucun contenu ou secret dans les logs.
- ClamAV optionnel derrière une abstraction, jamais requis pour le profil dev minimal.

Les migrations, adapters et tests correspondants n'existent pas encore; ils relèvent de P13.
