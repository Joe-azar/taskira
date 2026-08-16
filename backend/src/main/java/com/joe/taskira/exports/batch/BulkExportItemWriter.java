package com.joe.taskira.exports.batch;

import com.joe.taskira.exports.util.TicketExcelRowWriter;
import com.joe.taskira.ticket.entity.Ticket;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;

import java.util.HashMap;
import java.util.Map;

/**
 * Appends tickets into the job-scoped workbook (see BulkExportJobConfig), one sheet per
 * project code, created on demand as a new code is first seen. Row indices are tracked
 * here rather than read back from the sheet: SXSSFWorkbook flushes older rows to a temp
 * file once its in-memory window is exceeded, and Sheet.getLastRowNum() is documented as
 * unreliable once that has happened.
 */
public class BulkExportItemWriter implements ItemWriter<Ticket> {

    private final Workbook workbook;
    private final Map<String, Sheet> sheetsByProjectCode = new HashMap<>();
    private final Map<String, Integer> nextRowByProjectCode = new HashMap<>();

    public BulkExportItemWriter(Workbook workbook) {
        this.workbook = workbook;
    }

    @Override
    public void write(Chunk<? extends Ticket> chunk) {
        for (Ticket ticket : chunk) {
            String projectCode = ticket.getProject().getCode();
            Sheet sheet = sheetsByProjectCode.computeIfAbsent(projectCode, code -> {
                Sheet newSheet = workbook.createSheet(code);
                TicketExcelRowWriter.writeHeaderRow(newSheet);
                nextRowByProjectCode.put(code, 1);
                return newSheet;
            });
            int rowIndex = nextRowByProjectCode.get(projectCode);
            TicketExcelRowWriter.writeTicketRow(sheet, rowIndex, ticket);
            nextRowByProjectCode.put(projectCode, rowIndex + 1);
        }
    }
}
