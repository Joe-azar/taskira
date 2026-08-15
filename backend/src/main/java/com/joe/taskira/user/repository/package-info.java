/**
 * Named interface: User's Spring Data repository, injected directly by auth, project,
 * ticket and comment for lookups. Tracked as debt to replace with a narrower
 * application-level API; see ADR-0016.
 */
@org.springframework.modulith.NamedInterface
package com.joe.taskira.user.repository;
