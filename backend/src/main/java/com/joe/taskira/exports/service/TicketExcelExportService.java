package com.joe.taskira.exports.service;

import com.joe.taskira.common.exception.ForbiddenException;
import com.joe.taskira.common.exception.ResourceNotFoundException;
import com.joe.taskira.common.util.SecurityUtils;
import com.joe.taskira.exports.util.TicketExcelRowWriter;
import com.joe.taskira.project.entity.Project;
import com.joe.taskira.project.repository.ProjectMemberRepository;
import com.joe.taskira.project.repository.ProjectRepository;
import com.joe.taskira.security.model.AuthenticatedUser;
import com.joe.taskira.ticket.entity.Ticket;
import com.joe.taskira.ticket.repository.TicketRepository;
import com.joe.taskira.user.enums.GlobalRole;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Synchronous, single-project export - bounded by definition (one project's tickets), so
 * a plain in-memory XSSFWorkbook is enough; contrast with the bulk export's streaming
 * SXSSFWorkbook (see ADR-0022, BulkExportItemWriter).
 */
@Service
@RequiredArgsConstructor
public class TicketExcelExportService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final TicketRepository ticketRepository;

    public record ExcelExport(String filename, byte[] content) {
    }

    public ExcelExport exportProjectTickets(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        assertCanAccessProject(project);

        List<Ticket> tickets = ticketRepository.findByProjectIdWithRelations(projectId);

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(project.getCode());
            TicketExcelRowWriter.writeHeaderRow(sheet);

            int rowIndex = 1;
            for (Ticket ticket : tickets) {
                TicketExcelRowWriter.writeTicketRow(sheet, rowIndex++, ticket);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return new ExcelExport(project.getCode() + "-tickets.xlsx", out.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate ticket export", e);
        }
    }

    private void assertCanAccessProject(Project project) {
        AuthenticatedUser currentUser = SecurityUtils.getCurrentUser();

        if (currentUser.getUser().getGlobalRole() == GlobalRole.ADMIN) {
            return;
        }

        if (project.getOwner().getId().equals(currentUser.getId())) {
            return;
        }

        boolean isMember = projectMemberRepository.existsByProjectIdAndUserId(project.getId(), currentUser.getId());

        if (!isMember) {
            throw new ForbiddenException("You are not allowed to access this project");
        }
    }
}
