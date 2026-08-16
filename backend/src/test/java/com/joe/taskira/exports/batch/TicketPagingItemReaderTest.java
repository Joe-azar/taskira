package com.joe.taskira.exports.batch;

import com.joe.taskira.ticket.entity.Ticket;
import com.joe.taskira.ticket.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketPagingItemReaderTest {

    @Mock
    private TicketRepository ticketRepository;

    @Test
    void pagesThroughEveryTicketAcrossMultiplePagesThenReturnsNull() {
        Ticket t1 = ticket(1L);
        Ticket t2 = ticket(2L);
        Ticket t3 = ticket(3L);

        List<Page<Ticket>> pages = List.of(
                new PageImpl<>(List.of(t1, t2), PageRequest.of(0, 2), 3),
                new PageImpl<>(List.of(t3), PageRequest.of(1, 2), 3)
        );

        when(ticketRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenAnswer(invocation -> {
                    Pageable pageable = invocation.getArgument(1);
                    int pageNumber = pageable.getPageNumber();
                    return pageNumber < pages.size() ? pages.get(pageNumber) : Page.empty(pageable);
                });

        TicketPagingItemReader reader = new TicketPagingItemReader(ticketRepository, 2);

        assertThat(reader.read()).isEqualTo(t1);
        assertThat(reader.read()).isEqualTo(t2);
        assertThat(reader.read()).isEqualTo(t3);
        assertThat(reader.read()).isNull();
    }

    private static Ticket ticket(Long id) {
        Ticket ticket = new Ticket();
        ReflectionTestUtils.setField(ticket, "id", id);
        return ticket;
    }
}
