package com.joe.taskira.exports.service;

import com.joe.taskira.common.exception.ForbiddenException;
import com.joe.taskira.common.exception.ResourceNotFoundException;
import com.joe.taskira.project.entity.Project;
import com.joe.taskira.project.enums.ProjectStatus;
import com.joe.taskira.project.repository.ProjectMemberRepository;
import com.joe.taskira.project.repository.ProjectRepository;
import com.joe.taskira.security.model.AuthenticatedUser;
import com.joe.taskira.ticket.entity.Ticket;
import com.joe.taskira.ticket.enums.TicketPriority;
import com.joe.taskira.ticket.enums.TicketStatus;
import com.joe.taskira.ticket.enums.TicketType;
import com.joe.taskira.ticket.repository.TicketRepository;
import com.joe.taskira.user.entity.User;
import com.joe.taskira.user.enums.GlobalRole;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketExcelExportServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private TicketRepository ticketRepository;

    private TicketExcelExportService service;

    private User member;
    private Project project;
    private Ticket ticket;

    @BeforeEach
    void setUp() {
        service = new TicketExcelExportService(projectRepository, projectMemberRepository, ticketRepository);

        member = User.builder().firstName("A").lastName("B").email("member@taskira.test")
                .passwordHash("x").globalRole(GlobalRole.USER).active(true).build();
        ReflectionTestUtils.setField(member, "id", 1L);

        User owner = User.builder().firstName("O").lastName("W").email("owner@taskira.test")
                .passwordHash("x").globalRole(GlobalRole.USER).active(true).build();
        ReflectionTestUtils.setField(owner, "id", 2L);

        project = Project.builder().code("EXP").name("Export Project").owner(owner)
                .status(ProjectStatus.ACTIVE).ticketSequence(0).build();
        ReflectionTestUtils.setField(project, "id", 100L);

        ticket = Ticket.builder().reference("EXP-1").project(project).title("Sample ticket")
                .type(TicketType.TASK).status(TicketStatus.OPEN).priority(TicketPriority.HIGH)
                .creator(owner).build();
        ReflectionTestUtils.setField(ticket, "id", 10L);

        AuthenticatedUser principal = new AuthenticatedUser(member);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void exportProducesARealWorkbookWithTheProjectsTicketsAsRows() throws Exception {
        when(projectRepository.findById(100L)).thenReturn(Optional.of(project));
        when(projectMemberRepository.existsByProjectIdAndUserId(100L, 1L)).thenReturn(true);
        when(ticketRepository.findByProjectIdWithRelations(100L)).thenReturn(List.of(ticket));

        TicketExcelExportService.ExcelExport export = service.exportProjectTickets(100L);

        assertThat(export.filename()).isEqualTo("EXP-tickets.xlsx");

        // Read the actual bytes back with POI, not just check they're non-empty - proves
        // a real, valid workbook with the expected sheet/rows was produced.
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(export.content()))) {
            Sheet sheet = workbook.getSheet("EXP");
            assertThat(sheet).isNotNull();

            Row header = sheet.getRow(0);
            assertThat(header.getCell(0).getStringCellValue()).isEqualTo("Reference");

            Row dataRow = sheet.getRow(1);
            assertThat(dataRow.getCell(0).getStringCellValue()).isEqualTo("EXP-1");
            assertThat(dataRow.getCell(1).getStringCellValue()).isEqualTo("Sample ticket");
            assertThat(dataRow.getCell(3).getStringCellValue()).isEqualTo("OPEN");
        }
    }

    @Test
    void exportRejectsANonMemberOfTheProject() {
        when(projectRepository.findById(100L)).thenReturn(Optional.of(project));
        when(projectMemberRepository.existsByProjectIdAndUserId(100L, 1L)).thenReturn(false);

        assertThatThrownBy(() -> service.exportProjectTickets(100L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void exportRejectsAnUnknownProject() {
        when(projectRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.exportProjectTickets(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
