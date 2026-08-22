# Architecture de stockage documentaire

Statut : implémenté et fusionné dans `main` depuis la phase 13 ([ADR-0009](../adr/0009-local-filesystem-first.md), promu `Accepted`; [ADR-0021](../adr/0021-attachments-storage.md)). Module Spring Modulith `attachments`, fermé par défaut.

## Port implémenté

```java
public interface DocumentStorage {
    String store(InputStream content) throws IOException;
    InputStream retrieve(String storageKey) throws IOException;
    void delete(String storageKey) throws IOException;
}
```

Une seule implémentation aujourd'hui : `LocalFileSystemStorage` (`attachments/adapter/`). Un adapter MinIO ou Azure Blob remplacerait cette implémentation sans changer aucun appelant — non ajouté faute de besoin réel, conformément à ADR-0009.

## Entité et métadonnées réelles

`Attachment` (`AuditableEntity`, table `attachments`, Flyway `V9`) : clé de stockage UUID générée côté serveur (jamais dérivée du nom client), nom original conservé comme métadonnée d'affichage uniquement, type MIME réel détecté par Apache Tika, taille, SHA-256, ticket lié, utilisateur uploadeur, horodatage.

## Sécurité réellement appliquée

- Type MIME réel détecté par Apache Tika (`tika-core`), jamais le `Content-Type` déclaré par le client ni l'extension du fichier — liste blanche explicite de types autorisés.
- Clé de stockage UUID générée côté serveur; `LocalFileSystemStorage` revérifie en plus que le chemin résolu reste sous le répertoire de stockage (défense en profondeur, pas seulement la forme de la clé).
- Taille limitée à la fois par Spring (`multipart.max-file-size`) et par le service.
- SHA-256 calculé et stocké pour chaque fichier.
- `Content-Disposition: attachment` (jamais `inline`), nom de fichier encodé RFC 6266.
- Autorisation backend selon l'appartenance au projet du ticket (mêmes règles que les commentaires); upload/suppression bloqués sur un projet archivé.
- Chaque upload et suppression est audité (`ATTACHMENT_CREATED`, `ATTACHMENT_DELETED`).
- ClamAV n'est pas intégré (décision assumée, pas un oubli — voir [ADR-0021](../adr/0021-attachments-storage.md)); aucun quota par projet/utilisateur.

## Endpoints

`GET/POST /api/v1/tickets/{id}/attachments`, `GET /api/v1/attachments/{id}/content` (téléchargement en streaming), `DELETE /api/v1/attachments/{id}`.

## Ce qui reste hors périmètre

Aucune UI frontend pour les pièces jointes (P13 est backend uniquement); l'API existe et est testée mais rien ne la consomme encore dans Angular — voir `ENTERPRISE_MIGRATION_REPORT.md`, section « Problèmes et dettes ouverts ».
