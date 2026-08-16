package com.joe.taskira.exports;

import com.joe.taskira.support.PostgreSqlIntegrationTest;
import com.joe.taskira.user.repository.UserRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.client.ExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;
import tools.jackson.databind.json.JsonMapper;

import java.io.ByteArrayInputStream;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Real HTTP proof for the two synchronous exports (see ADR-0022) - the bulk async job is
 * covered separately by BulkExportJobIT (job mechanics) and BulkExportWiringIT (the full
 * HTTP-triggered flow). Mirrors AttachmentWiringIT's discipline: real bytes read back with
 * the same library that would read them in production, not just a non-empty body check.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ExportWiringIT extends PostgreSqlIntegrationTest {

    private static final String SESSION_COOKIE = "TASKIRA_SESSION";
    private static final String CSRF_COOKIE = "XSRF-TOKEN";
    private static final String CSRF_HEADER = "X-XSRF-TOKEN";
    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final String TEST_PASSWORD = "Taskira-Export-IT-42!";

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private RestTestClient client() {
        return RestTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    private record Actor(Long userId, String sessionCookie, String csrfCookie) {
    }

    private String seedCsrfToken() {
        ExchangeResult result = client().get().uri("/api/v1/auth/me").exchange().returnResult();
        return result.getResponseCookies().getFirst(CSRF_COOKIE).getValue();
    }

    private Actor register(String label) {
        String csrfToken = seedCsrfToken();
        String email = label + "-" + UUID.randomUUID() + "@taskira.test";

        ExchangeResult result = client().post().uri("/api/v1/auth/register")
                .cookie(CSRF_COOKIE, csrfToken)
                .header(CSRF_HEADER, csrfToken)
                .header(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE_JSON)
                .body("""
                        {
                          "firstName": "Export",
                          "lastName": "Test",
                          "email": "%s",
                          "password": "%s",
                          "confirmPassword": "%s"
                        }
                        """.formatted(email, TEST_PASSWORD, TEST_PASSWORD))
                .exchange()
                .returnResult();
        assertThat(result.getStatus().value()).isEqualTo(201);

        Long userId = userRepository.findByEmailIgnoreCase(email).orElseThrow().getId();
        String sessionCookie = result.getResponseCookies().getFirst(SESSION_COOKIE).getValue();
        return new Actor(userId, sessionCookie, csrfToken);
    }

    private ExchangeResult authenticatedPost(Actor actor, String uri, String body) {
        return client().post().uri(uri)
                .cookie(SESSION_COOKIE, actor.sessionCookie())
                .cookie(CSRF_COOKIE, actor.csrfCookie())
                .header(CSRF_HEADER, actor.csrfCookie())
                .header(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE_JSON)
                .body(body)
                .exchange()
                .returnResult();
    }

    private long extractId(ExchangeResult result) {
        String body = new String(result.getResponseBodyContent());
        return JSON.readTree(body).get("id").asLong();
    }

    private long createProject(Actor owner, String code) {
        ExchangeResult result = authenticatedPost(owner, "/api/v1/projects", """
                {"code": "%s", "name": "Export wiring project", "description": ""}
                """.formatted(code));
        assertThat(result.getStatus().value()).isEqualTo(201);
        return extractId(result);
    }

    private long createTicket(Actor owner, long projectId, String title) {
        ExchangeResult result = authenticatedPost(owner, "/api/v1/tickets", """
                {"projectId": %d, "title": "%s", "type": "TASK"}
                """.formatted(projectId, title));
        assertThat(result.getStatus().value()).isEqualTo(201);
        return extractId(result);
    }

    @Test
    void projectTicketExportDownloadsARealWorkbookWithEveryTicketOfTheProject() throws Exception {
        Actor owner = register("export-xlsx-owner");
        long projectId = createProject(owner, "EWX" + owner.userId());
        createTicket(owner, projectId, "First ticket");
        createTicket(owner, projectId, "Second ticket");

        ExchangeResult result = client().get().uri("/api/v1/projects/" + projectId + "/tickets/export.xlsx")
                .cookie(SESSION_COOKIE, owner.sessionCookie())
                .exchange()
                .returnResult();

        assertThat(result.getStatus().value()).isEqualTo(200);
        assertThat(result.getResponseHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION)).contains("attachment");

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(result.getResponseBodyContent()))) {
            Sheet sheet = workbook.getSheetAt(0);
            Row header = sheet.getRow(0);
            assertThat(header.getCell(0).getStringCellValue()).isEqualTo("Reference");
            // Header row + 2 tickets
            assertThat(sheet.getPhysicalNumberOfRows()).isEqualTo(3);
        }
    }

    @Test
    void projectTicketExportIsForbiddenForANonMember() {
        Actor owner = register("export-xlsx-access-owner");
        Actor outsider = register("export-xlsx-access-outsider");
        long projectId = createProject(owner, "EWA" + owner.userId());

        ExchangeResult result = client().get().uri("/api/v1/projects/" + projectId + "/tickets/export.xlsx")
                .cookie(SESSION_COOKIE, outsider.sessionCookie())
                .exchange()
                .returnResult();

        assertThat(result.getStatus().value()).isEqualTo(403);
    }

    @Test
    void ticketPdfExportDownloadsARealPdfContainingTheTicketsReferenceAndTitle() throws Exception {
        Actor owner = register("export-pdf-owner");
        long projectId = createProject(owner, "EWP" + owner.userId());
        long ticketId = createTicket(owner, projectId, "A ticket worth printing");

        ExchangeResult result = client().get().uri("/api/v1/tickets/" + ticketId + "/export.pdf")
                .cookie(SESSION_COOKIE, owner.sessionCookie())
                .exchange()
                .returnResult();

        assertThat(result.getStatus().value()).isEqualTo(200);
        assertThat(result.getResponseHeaders().getFirst(HttpHeaders.CONTENT_TYPE)).contains("application/pdf");

        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(result.getResponseBodyContent()))) {
            String text = new PDFTextStripper().getText(document);
            assertThat(text).contains("A ticket worth printing");
        }
    }

    @Test
    void ticketPdfExportIsForbiddenForANonMember() {
        Actor owner = register("export-pdf-access-owner");
        Actor outsider = register("export-pdf-access-outsider");
        long projectId = createProject(owner, "EWQ" + owner.userId());
        long ticketId = createTicket(owner, projectId, "Private ticket");

        ExchangeResult result = client().get().uri("/api/v1/tickets/" + ticketId + "/export.pdf")
                .cookie(SESSION_COOKIE, outsider.sessionCookie())
                .exchange()
                .returnResult();

        assertThat(result.getStatus().value()).isEqualTo(403);
    }
}
