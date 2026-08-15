/**
 * Named interface: Project's Spring Data repositories, currently injected directly by
 * the ticket and comment modules. Tracked as debt to replace with a narrower
 * application-level API; see ADR-0016.
 */
@org.springframework.modulith.NamedInterface
package com.joe.taskira.project.repository;
