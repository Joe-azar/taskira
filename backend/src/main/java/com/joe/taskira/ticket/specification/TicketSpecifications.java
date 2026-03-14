package com.joe.taskira.ticket.specification;

import com.joe.taskira.ticket.entity.Ticket;
import com.joe.taskira.ticket.enums.TicketPriority;
import com.joe.taskira.ticket.enums.TicketStatus;
import com.joe.taskira.ticket.enums.TicketType;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collection;

public final class TicketSpecifications {

    private TicketSpecifications() {
    }

    public static Specification<Ticket> hasProjectIds(Collection<Long> projectIds) {
        return (root, query, cb) -> root.get("project").get("id").in(projectIds);
    }

    public static Specification<Ticket> hasProjectId(Long projectId) {
        if (projectId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("project").get("id"), projectId);
    }

    public static Specification<Ticket> hasStatus(TicketStatus status) {
        if (status == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Ticket> hasPriority(TicketPriority priority) {
        if (priority == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("priority"), priority);
    }

    public static Specification<Ticket> hasType(TicketType type) {
        if (type == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("type"), type);
    }

    public static Specification<Ticket> hasCreatorId(Long creatorId) {
        if (creatorId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("creator").get("id"), creatorId);
    }

    public static Specification<Ticket> hasAssigneeId(Long assigneeId) {
        if (assigneeId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("assignee").get("id"), assigneeId);
    }

    public static Specification<Ticket> isUnassigned(Boolean unassigned) {
        if (unassigned == null || !unassigned) {
            return null;
        }
        return (root, query, cb) -> cb.isNull(root.get("assignee"));
    }

    public static Specification<Ticket> matchesKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }

        String pattern = "%" + keyword.trim().toLowerCase() + "%";

        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("reference")), pattern),
                cb.like(cb.lower(root.get("title")), pattern),
                cb.like(cb.lower(root.get("description")), pattern)
        );
    }
}
