package com.joe.taskira.exports.controller;

import com.joe.taskira.common.web.ApiVersion;
import com.joe.taskira.exports.service.TicketExcelExportService;
import com.joe.taskira.exports.service.TicketPdfExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

/**
 * Synchronous exports (see ADR-0022): both are bounded by a single project or a single
 * ticket, generated and returned within one request - the bulk, unbounded case is
 * BulkExportController's asynchronous Spring Batch job.
 */
@RestController
@RequestMapping(ApiVersion.V1)
@RequiredArgsConstructor
public class ExportController {

    private static final String XLSX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final TicketExcelExportService ticketExcelExportService;
    private final TicketPdfExportService ticketPdfExportService;

    @GetMapping("/projects/{projectId}/tickets/export.xlsx")
    public ResponseEntity<byte[]> exportProjectTicketsAsExcel(@PathVariable Long projectId) {
        TicketExcelExportService.ExcelExport export = ticketExcelExportService.exportProjectTickets(projectId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition(export.filename()))
                .contentType(MediaType.parseMediaType(XLSX_CONTENT_TYPE))
                .body(export.content());
    }

    @GetMapping("/tickets/{ticketId}/export.pdf")
    public ResponseEntity<byte[]> exportTicketAsPdf(@PathVariable Long ticketId) {
        TicketPdfExportService.PdfExport export = ticketPdfExportService.exportTicket(ticketId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition(export.filename()))
                .contentType(MediaType.APPLICATION_PDF)
                .body(export.content());
    }

    private String contentDisposition(String filename) {
        return "attachment; filename*=UTF-8''" + java.net.URLEncoder.encode(filename, StandardCharsets.UTF_8);
    }
}
