/**
 * Named interface: User's JPA entity, read directly by nearly every other module
 * (auth, project, ticket, comment, security, exports). This is the closest thing
 * Taskira has to a shared identity/kernel type; see ADR-0016.
 */
@org.springframework.modulith.NamedInterface
package com.joe.taskira.user.entity;
