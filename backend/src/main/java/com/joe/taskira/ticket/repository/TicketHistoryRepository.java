package com.joe.taskira.ticket.repository;

import com.joe.taskira.ticket.entity.TicketHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TicketHistoryRepository extends JpaRepository<TicketHistory, Long> {

    @Query("""
            select th
            from TicketHistory th
            join fetch th.ticket t
            join fetch th.changedBy u
            where t.id = :ticketId
            order by th.changedAt desc, th.id desc
            """)
    List<TicketHistory> findByTicketIdWithRelations(Long ticketId);
}