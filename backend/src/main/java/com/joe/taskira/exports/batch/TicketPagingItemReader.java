package com.joe.taskira.exports.batch;

import com.joe.taskira.ticket.entity.Ticket;
import com.joe.taskira.ticket.repository.TicketRepository;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.Iterator;
import java.util.List;

/**
 * Pages through every ticket in the database, all projects included - the bulk export's
 * whole reason to be chunk-oriented rather than a single findAll(). Uses
 * TicketRepository.findAll(Specification, Pageable) with an empty specification purely to
 * reuse its existing @EntityGraph(project, creator, assignee) eager fetch, not because any
 * filtering is needed here.
 */
public class TicketPagingItemReader implements ItemReader<Ticket> {

    private final TicketRepository ticketRepository;
    private final int pageSize;

    private int currentPage = 0;
    private Iterator<Ticket> currentPageIterator = List.<Ticket>of().iterator();

    public TicketPagingItemReader(TicketRepository ticketRepository, int pageSize) {
        this.ticketRepository = ticketRepository;
        this.pageSize = pageSize;
    }

    @Override
    public Ticket read() {
        if (!currentPageIterator.hasNext()) {
            // cb.conjunction() (always-true), not Specification.where(null) - this
            // Spring Data JPA version rejects a null Specification with
            // IllegalArgumentException rather than treating it as "no restriction".
            Page<Ticket> page = ticketRepository.findAll(
                    (root, query, cb) -> cb.conjunction(),
                    PageRequest.of(currentPage, pageSize, Sort.by("id"))
            );
            if (page.isEmpty()) {
                return null;
            }
            currentPageIterator = page.iterator();
            currentPage++;
        }
        return currentPageIterator.hasNext() ? currentPageIterator.next() : null;
    }
}
