package com.joe.taskira.exports.util;

import com.joe.taskira.ticket.entity.Ticket;
import com.joe.taskira.user.entity.User;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

/**
 * Shared between the synchronous per-project export (TicketExcelExportService) and the
 * bulk export batch writer (BulkExportItemWriter) - one place defines what a ticket row
 * looks like in a workbook, not two independently-maintained copies.
 */
public final class TicketExcelRowWriter {

    public static final String[] HEADERS = {
            "Reference", "Title", "Type", "Status", "Priority", "Creator", "Assignee", "Due date"
    };

    private TicketExcelRowWriter() {
    }

    public static void writeHeaderRow(Sheet sheet) {
        Workbook workbook = sheet.getWorkbook();
        CellStyle headerStyle = workbook.createCellStyle();
        Font boldFont = workbook.createFont();
        boldFont.setBold(true);
        headerStyle.setFont(boldFont);

        Row header = sheet.createRow(0);
        for (int i = 0; i < HEADERS.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(HEADERS[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    public static void writeTicketRow(Sheet sheet, int rowIndex, Ticket ticket) {
        Row row = sheet.createRow(rowIndex);
        row.createCell(0).setCellValue(ticket.getReference());
        row.createCell(1).setCellValue(ticket.getTitle());
        row.createCell(2).setCellValue(ticket.getType().name());
        row.createCell(3).setCellValue(ticket.getStatus().name());
        row.createCell(4).setCellValue(ticket.getPriority().name());
        row.createCell(5).setCellValue(displayName(ticket.getCreator()));
        row.createCell(6).setCellValue(ticket.getAssignee() != null ? displayName(ticket.getAssignee()) : "");
        row.createCell(7).setCellValue(ticket.getDueDate() != null ? ticket.getDueDate().toString() : "");
    }

    private static String displayName(User user) {
        return user.getFullName() + " <" + user.getEmail() + ">";
    }
}
