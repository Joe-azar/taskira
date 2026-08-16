# ADR-0022 — Module exports : POI, OpenHTMLtoPDF, PDFBox, ZXing, Spring Batch

- Statut : Accepted
- Date : 2026-08-16

## Contexte

La feuille de route (P14) prévoit un module `exports` couvrant trois besoins réels distincts, pas une démonstration de technologies pour elles-mêmes :

1. Un chef de projet veut extraire la liste des tickets d'un projet dans un tableur pour le partager ou le retravailler hors de l'application.
2. Une personne veut un compte-rendu imprimable d'un ticket précis (description, statut, commentaires) avec un moyen rapide de revenir à sa version en ligne.
3. Un administrateur veut un export complet, tous projets confondus, sans bloquer une requête HTTP le temps de le générer - un vrai cas d'usage de volume et d'asynchronicité, pas juste une liste de tickets légèrement plus longue.

## Décision

### Export Excel synchrone (Apache POI)

`GET /api/v1/projects/{projectId}/tickets/export.xlsx` génère un classeur `.xlsx` à la demande avec `poi-ooxml` (`5.5.1`, dernière version stable confirmée via l'API de recherche Maven Central) pour les tickets d'un seul projet. Mêmes règles d'accès que `GET /api/v1/projects/{projectId}/tickets` (membre du projet, propriétaire ou administrateur) : l'export ne doit jamais exposer plus que ce que l'API JSON expose déjà. Pas de filtrage additionnel dans cette première version - l'export livre l'intégralité des tickets du projet, la fonction de recherche/filtre restant le rôle de l'API JSON existante.

### Export PDF synchrone (OpenHTMLtoPDF + ZXing + PDFBox)

`GET /api/v1/tickets/{ticketId}/export.pdf` génère un rapport PDF pour un ticket unique (référence, titre, description, statut, priorité, créateur, assigné, échéance, commentaires). Le HTML est construit directement en Java (text blocks), pas via un moteur de templating - un besoin à une seule vue fixe ne justifie pas d'ajouter Thymeleaf à un backend qui n'a par ailleurs aucune vue serveur. Rendu en PDF par `openhtmltopdf-pdfbox` (`1.0.10`, dernière version stable) plutôt que directement par PDFBox : OpenHTMLtoPDF prend en charge la mise en page HTML/CSS, ce que PDFBox seul ne fait pas.

Le rapport inclut un QR code généré par ZXing (`core`/`javase` `3.5.3`) encodant l'URL de la version web du ticket (`${app.frontend-url}/projects/{id}/tickets/{ticketId}`), embarqué directement dans le HTML via une image `data:image/png;base64,...` - aucun fichier temporaire, aucun aller-retour par le stockage pour une image générée à la volée et jetée immédiatement après rendu.

`openhtmltopdf-pdfbox` dépend en interne de `org.apache.pdfbox:pdfbox` **2.0.24** (vérifié en lisant directement son POM Maven, pas supposé) : PDFBox est donc déjà présent sur le classpath par transitivité, pas ajouté comme une dépendance de test isolée. La ligne `2.0.x` est épinglée explicitement à `2.0.36` (dernier correctif de cette ligne au 16 août 2026) plutôt que de laisser la version transitive `2.0.24` non patchée s'imposer silencieusement - même logique que l'épinglage du pilote JDBC PostgreSQL en P6. PDFBox a alors un rôle réel en production, pas seulement en test : `PDDocumentInformation` pour poser les métadonnées du PDF généré (titre, auteur, sujet) après le rendu OpenHTMLtoPDF - et un rôle de vérification en test, `PDFTextStripper` pour extraire le texte réellement rendu et `PDFRenderer` pour rastériser la page et redécoder le QR code avec ZXing, prouvant que la composition HTML → PDF n'a pas corrompu l'image embarquée.

### Export en masse asynchrone (Spring Batch)

`ticketsBulkExportJob`, réservé aux administrateurs (`@PreAuthorize("hasRole('ADMIN')")`, même style que `AuditController`), exporte l'intégralité des tickets de tous les projets dans un seul classeur `.xlsx`, un onglet par code de projet. C'est le seul cas de ce module qui justifie réellement Spring Batch : un volume non borné (tous les tickets, tous projets confondus) traité par lots plutôt que chargé entièrement en mémoire d'un coup, avec un état de job persistant et interrogeable - pas un habillage sur une simple boucle.

```text
POST   /api/v1/exports/tickets/batch              lance le job, retourne son jobExecutionId
GET    /api/v1/exports/tickets/batch/{id}          état du job (STARTING/STARTED/COMPLETED/FAILED)
GET    /api/v1/exports/tickets/batch/{id}/download  télécharge le résultat une fois COMPLETED
```

- **Lecture** : un `ItemReader<Ticket>` paginé maison (`TicketRepository.findAll(Pageable)`), pas `spring-batch-data`/`RepositoryItemReader` - une dépendance de plus pour un besoin que Spring Data JPA couvre déjà nativement.
- **Écriture** : un `SXSSFWorkbook` (variante *streaming* de POI, pas `XSSFWorkbook`) partagé sur toute la durée du job, alimenté ligne par ligne par chaque *chunk*, un onglet créé à la demande par code de projet - contrairement à l'export synchrone mono-projet, dont le volume borné ne justifie pas le mode streaming.
- **Lancement asynchrone** : le `JobLauncher` par défaut de Spring Boot est synchrone (`SyncTaskExecutor`); un bean `JobLauncher` dédié avec un `ThreadPoolTaskExecutor` borné (cœur 1, max 2 - ce job reste rare et déclenché par un administrateur, pas un besoin de parallélisme élevé) permet à l'appel HTTP de retourner immédiatement l'identifiant du job pendant qu'il s'exécute en arrière-plan.
- **Résultat** : à la fin du job (`JobExecutionListener.afterJob`), le classeur fini est stocké via le port `DocumentStorage` déjà défini par le module `attachments` (P13) et exposé via `@NamedInterface` - pas un second mécanisme de stockage de fichiers. La clé de stockage est écrite dans le `ExecutionContext` du `JobExecution`, lisible ensuite par l'API de statut sans table de correspondance supplémentaire.
- **Schéma** : les tables `BATCH_*` de Spring Batch (`6.0.4`, résolu par le BOM Spring Boot 4.1.0) sont créées par une migration Flyway dédiée (`V10`), avec `spring.batch.jdbc.initialize-schema: never` pour empêcher Spring Boot de les créer lui-même au démarrage - même règle que pour Hibernate (`ddl-auto=validate`), le schéma ne doit avoir qu'une seule source de vérité. `spring.batch.job.enabled: false` empêche par ailleurs Spring Boot de lancer automatiquement `ticketsBulkExportJob` au démarrage de l'application : ce job ne doit s'exécuter que sur déclenchement HTTP explicite d'un administrateur.

### Audit

Un seul nouveau type d'événement, `EXPORT_GENERATED` (`AuditEntityType.EXPORT`), déclenché uniquement à la fin réussie du job en masse - les deux exports synchrones sont des lectures à la volée sans effet persistant, cohérent avec le principe déjà appliqué ailleurs dans ce dépôt de ne pas auditer de simples consultations.

## Conséquences

- Aucune interface frontend dans cette première version (cohérent avec P12/P13) : l'export synchrone est directement téléchargeable par URL, l'export en masse nécessite un client capable de lancer/interroger/télécharger - une UI d'administration dédiée reste à évaluer si un besoin concret apparaît.
- Le classeur en masse n'est pas purgé automatiquement du stockage après téléchargement; aucune politique de rétention n'est définie dans cette première version.
- Le `ThreadPoolTaskExecutor` dédié au lancement de job est un pool en mémoire de l'instance applicative - un redémarrage pendant un job en cours abandonnerait son exécution (`JobExecution` resterait à `STARTED`); acceptable pour ce cas d'usage rare et manuel, pas pour un ordonnancement de production à fort volume.
