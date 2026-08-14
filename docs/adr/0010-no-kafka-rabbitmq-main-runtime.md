# ADR-0010 — Exclure Kafka et RabbitMQ du runtime principal

- Statut : Accepted
- Date : 2026-08-15

## Contexte

Les workflows Taskira actuels ne justifient ni broker distribué ni découpage microservices.

## Décision

Prioriser appels synchrones internes, événements Spring ciblés, Scheduler et Spring Batch. Kafka et RabbitMQ ne sont pas des dépendances du runtime principal.

Ils peuvent apparaître dans des labs P20 isolés. Une intégration réelle exige besoin mesuré, analyse opérationnelle et nouvel ADR.

## Conséquences

- Moins de services et de modes de panne.
- Transactionnalité plus simple.
- L'apprentissage messaging reste possible sans coupler Taskira.

## État d'implémentation

Contrainte respectée; aucun broker obligatoire n'est présent.
