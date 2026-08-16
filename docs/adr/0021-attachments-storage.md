# ADR-0021 — Module attachments : port DocumentStorage, filesystem local, Tika

- Statut : Accepted
- Date : 2026-08-16

## Contexte

ADR-0009 avait déjà anticipé ce choix avant que le module n'existe : commencer le stockage de pièces jointes par le filesystem local via un port `DocumentStorage`, pas directement MinIO ou un stockage cloud. P13 est la phase qui livre réellement ce module.

Un upload de fichier est une surface d'attaque directe : nom de fichier contrôlé par le client, type MIME déclaré par le client (jamais fiable), taille arbitraire, contenu arbitraire. Aucune de ces valeurs ne doit être approuvée sans vérification côté serveur.

## Décision

### Port `DocumentStorage`, adapter filesystem

Interface `DocumentStorage` (module `attachments`) avec une seule implémentation pour l'instant, `LocalFileSystemStorage` — exactement le port/adapter qu'ADR-0009 annonçait. MinIO ou un stockage objet resteront des adapters alternatifs activables par configuration si un besoin réel apparaît, pas une réécriture.

### Ne jamais faire confiance à l'entrée client

- **Type MIME réel, pas déclaré** : Apache Tika (`tika-core` 3.3.1, dernière version stable — 4.0.0 n'existe qu'en alpha, écartée) détecte le type réel par analyse du contenu (magic bytes), jamais le `Content-Type` envoyé par le navigateur ni l'extension du nom de fichier. Une liste blanche explicite de types autorisés (images courantes, PDF, texte brut, quelques formats bureautiques) rejette tout le reste — en particulier tout exécutable ou script, qu'il soit correctement ou incorrectement étiqueté par le client.
- **Nom de fichier jamais utilisé comme chemin** : la clé de stockage réelle est un identifiant technique généré côté serveur (UUID), jamais dérivé du nom fourni par le client — élimine toute possibilité de traversée de répertoire (`../../etc/passwd`) par construction, pas par validation a posteriori. Le nom d'origine est conservé uniquement comme métadonnée, réinjecté au téléchargement via `Content-Disposition: attachment` (jamais `inline` : un SVG ou HTML malveillant ne doit jamais s'exécuter dans le contexte de l'application).
- **Taille limitée** à la fois au niveau Spring (`spring.servlet.multipart.max-file-size`) et au niveau service, pour qu'une requête surdimensionnée soit rejetée avant même d'être entièrement lue en mémoire.
- **SHA-256 calculé et stocké** pour chaque fichier, à des fins d'intégrité et de traçabilité — pas de déduplication automatique dans cette première version, qui ajouterait de la complexité sans besoin métier démontré.

### ClamAV reste optionnel

Conforme à la feuille de route (« ClamAV/MinIO restent optionnels ») : pas d'antivirus dans cette première version. La combinaison liste blanche de types MIME réels + détection de contenu + absence totale d'exécution ou de rendu serveur des fichiers stockés constitue la ligne de base de sécurité retenue. Un vrai besoin (upload public non authentifié, par exemple) justifierait de reconsidérer ce choix dans un ADR dédié.

### Contrôle d'accès

Mêmes règles que les commentaires/tickets : accès réservé aux membres du projet (ou propriétaire/administrateur), upload et suppression bloqués sur un projet archivé — cohérent avec les règles déjà appliquées ailleurs plutôt qu'une nouvelle politique parallèle.

## Conséquences

- Le filesystem local suppose un stockage persistant partagé entre instances si l'application devait un jour tourner en plusieurs répliques — un problème pour une éventuelle bascule Kubernetes (P17), pas pour Docker Compose aujourd'hui (ADR-0011).
- Aucun quota par projet ni par utilisateur dans cette première version; à ajouter si un besoin réel de limitation d'espace disque apparaît.
- La liste blanche de types MIME devra être révisée si un besoin métier réel demande d'autres formats — décision volontairement restrictive au départ plutôt que permissive par défaut.
