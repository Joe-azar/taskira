package com.joe.taskira.ticket.repository;

import com.joe.taskira.ticket.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    @Query("""
            select t
            from Ticket t
            join fetch t.project p
            join fetch t.creator c
            left join fetch t.assignee a
            where p.id = :projectId
            order by t.createdAt desc
            """)
    List<Ticket> findByProjectIdWithRelations(Long projectId);

    @Query("""
            select t
            from Ticket t
            join fetch t.project p
            join fetch t.creator c
            left join fetch t.assignee a
            where t.id = :ticketId
            """)
    Optional<Ticket> findByIdWithRelations(Long ticketId);
}