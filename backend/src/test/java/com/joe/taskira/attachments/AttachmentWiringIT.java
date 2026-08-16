package com.joe.taskira.attachments;

import com.joe.taskira.support.PostgreSqlIntegrationTest;
import com.joe.taskira.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.ExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end proof through the real HTTP endpoints - real multipart upload, real Tika
 * content detection deciding whether the upload is accepted, real download bytes, real
 * deletion - not AttachmentService/LocalFileSystemStorage exercised directly (see
 * AttachmentServiceTest/LocalFileSystemStorageTest for those). Mirrors AuditWiringIT/
 * NotificationWiringIT's discipline.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AttachmentWiringIT extends PostgreSqlIntegrationTest {

    private static final String SESSION_COOKIE = "TASKIRA_SESSION";
    private static final String CSRF_COOKIE = "XSRF-TOKEN";
    private static final String CSRF_HEADER = "X-XSRF-TOKEN";
    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final String TEST_PASSWORD = "Taskira-Attach-IT-42!";

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private RestTestClient client() {
        return RestTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    private record Actor(Long userId, String email, String sessionCookie, String csrfCookie) {
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
                          "firstName": "Attach",
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
        return new Actor(userId, email, sessionCookie, csrfToken);
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
                {"code": "%s", "name": "Attachment wiring project", "description": ""}
                """.formatted(code));
        assertThat(result.getStatus().value()).isEqualTo(201);
        return extractId(result);
    }

    private long createTicket(Actor owner, long projectId) {
        ExchangeResult result = authenticatedPost(owner, "/api/v1/tickets", """
                {"projectId": %d, "title": "Attachment wiring ticket", "type": "TASK"}
                """.formatted(projectId));
        assertThat(result.getStatus().value()).isEqualTo(201);
        return extractId(result);
    }

    private static class NamedByteArrayResource extends ByteArrayResource {
        private final String filename;

        NamedByteArrayResource(byte[] content, String filename) {
            super(content);
            this.filename = filename;
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }

    private ExchangeResult uploadFile(Actor actor, long ticketId, String filename, String declaredContentType, byte[] content) {
        MultiValueMap<String, Object> multipart = new LinkedMultiValueMap<>();
        multipart.add("file", new NamedByteArrayResource(content, filename));

        return client().post().uri("/api/v1/tickets/" + ticketId + "/attachments")
                .cookie(SESSION_COOKIE, actor.sessionCookie())
                .cookie(CSRF_COOKIE, actor.csrfCookie())
                .header(CSRF_HEADER, actor.csrfCookie())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(multipart)
                .exchange()
                .returnResult();
    }

    // A real, minimal 1x1 PNG - Tika must detect image/png from these actual bytes, not
    // from the .png extension (the rejection test below sends the exact same extension
    // with executable-script bytes to prove that).
    private static final byte[] REAL_PNG_BYTES = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
            0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
            0x08, 0x02, 0x00, 0x00, 0x00, (byte) 0x90, 0x77, 0x53,
            (byte) 0xDE, 0x00, 0x00, 0x00, 0x0C, 0x49, 0x44, 0x41,
            0x54, 0x08, (byte) 0xD7, 0x63, (byte) 0xF8, (byte) 0xCF, (byte) 0xC0, 0x00,
            0x00, 0x00, 0x03, 0x00, 0x01, 0x71, 0x35, 0x37,
            (byte) 0xE8, 0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E,
            0x44, (byte) 0xAE, 0x42, 0x60, (byte) 0x82
    };

    @Test
    void uploadingARealPngIsAcceptedAndCanBeDownloadedBackByteForByte() {
        Actor owner = register("attach-png-owner");
        long projectId = createProject(owner, "AWU" + owner.userId());
        long ticketId = createTicket(owner, projectId);

        ExchangeResult uploadResult = uploadFile(owner, ticketId, "photo.png", "image/png", REAL_PNG_BYTES);
        assertThat(uploadResult.getStatus().value()).isEqualTo(201);

        String body = new String(uploadResult.getResponseBodyContent());
        long attachmentId = JSON.readTree(body).get("id").asLong();
        assertThat(JSON.readTree(body).get("contentType").asText()).isEqualTo("image/png");

        ExchangeResult downloadResult = client().get().uri("/api/v1/attachments/" + attachmentId + "/content")
                .cookie(SESSION_COOKIE, owner.sessionCookie())
                .exchange()
                .returnResult();
        assertThat(downloadResult.getStatus().value()).isEqualTo(200);
        assertThat(downloadResult.getResponseHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .contains("attachment").doesNotContain("inline");
        assertThat(downloadResult.getResponseBodyContent()).isEqualTo(REAL_PNG_BYTES);
    }

    @Test
    void aScriptDisguisedWithAPngExtensionAndDeclaredContentTypeIsRejected() {
        Actor owner = register("attach-disguised-owner");
        long projectId = createProject(owner, "AWD" + owner.userId());
        long ticketId = createTicket(owner, projectId);

        byte[] shellScript = "#!/bin/sh\necho pwned\n".getBytes(StandardCharsets.UTF_8);

        ExchangeResult result = uploadFile(owner, ticketId, "totally-a-photo.png", "image/png", shellScript);

        assertThat(result.getStatus().value()).isEqualTo(409);
    }

    @Test
    void aNonMemberOfTheProjectCannotUploadOrDownload() {
        Actor owner = register("attach-access-owner");
        Actor outsider = register("attach-access-outsider");
        long projectId = createProject(owner, "AWO" + owner.userId());
        long ticketId = createTicket(owner, projectId);

        ExchangeResult uploadAttempt = uploadFile(outsider, ticketId, "notes.txt", "text/plain", "hello".getBytes());
        assertThat(uploadAttempt.getStatus().value()).isEqualTo(403);

        ExchangeResult ownerUpload = uploadFile(owner, ticketId, "notes.txt", "text/plain", "hello".getBytes());
        long attachmentId = JSON.readTree(new String(ownerUpload.getResponseBodyContent())).get("id").asLong();

        ExchangeResult downloadAttempt = client().get().uri("/api/v1/attachments/" + attachmentId + "/content")
                .cookie(SESSION_COOKIE, outsider.sessionCookie())
                .exchange()
                .returnResult();
        assertThat(downloadAttempt.getStatus().value()).isEqualTo(403);
    }

    @Test
    void deletingAnAttachmentRemovesItFromTheListingAndMakesDownloadFail() {
        Actor owner = register("attach-delete-owner");
        long projectId = createProject(owner, "AWX" + owner.userId());
        long ticketId = createTicket(owner, projectId);

        ExchangeResult uploadResult = uploadFile(owner, ticketId, "notes.txt", "text/plain", "delete me".getBytes());
        long attachmentId = JSON.readTree(new String(uploadResult.getResponseBodyContent())).get("id").asLong();

        ExchangeResult deleteResult = client().delete().uri("/api/v1/attachments/" + attachmentId)
                .cookie(SESSION_COOKIE, owner.sessionCookie())
                .cookie(CSRF_COOKIE, owner.csrfCookie())
                .header(CSRF_HEADER, owner.csrfCookie())
                .exchange()
                .returnResult();
        assertThat(deleteResult.getStatus().value()).isEqualTo(204);

        ExchangeResult listResult = client().get().uri("/api/v1/tickets/" + ticketId + "/attachments")
                .cookie(SESSION_COOKIE, owner.sessionCookie())
                .exchange()
                .returnResult();
        assertThat(new String(listResult.getResponseBodyContent())).doesNotContain("\"id\":" + attachmentId);
    }
}
