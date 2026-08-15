/**
 * Named interface: Project's JPA entities, currently read/written directly by the
 * ticket and comment modules for membership checks and relationship mapping.
 * Tracked as debt to replace with a narrower application-level API; see ADR-0016.
 */
@org.springframework.modulith.NamedInterface
package com.joe.taskira.project.entity;
