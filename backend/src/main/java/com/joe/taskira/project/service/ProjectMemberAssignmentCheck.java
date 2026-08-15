package com.joe.taskira.project.service;

/**
 * Port owned by the project module: asks "does this user still have work assigned to
 * them in this project" without the project module depending on the ticket module's
 * internals. Implemented in the ticket module (which already legitimately depends on
 * project, not the other way around) to avoid a project &lt;-&gt; ticket module cycle.
 * See ADR-0016.
 */
@org.springframework.modulith.NamedInterface
public interface ProjectMemberAssignmentCheck {

    long countAssignedTickets(Long projectId, Long userId);
}
