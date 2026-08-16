package com.joe.taskira.exports.service;

import com.joe.taskira.comment.entity.Comment;
import com.joe.taskira.comment.repository.CommentRepository;
import com.joe.taskira.common.exception.ForbiddenException;
import com.joe.taskira.common.exception.ResourceNotFoundException;
import com.joe.taskira.common.util.SecurityUtils;
import com.joe.taskira.exports.util.QrCodeGenerator;
import com.joe.taskira.project.entity.Project;
import com.joe.taskira.project.repository.ProjectMemberRepository;
import com.joe.taskira.security.model.AuthenticatedUser;
import com.joe.taskira.ticket.entity.Ticket;
import com.joe.taskira.ticket.repository.TicketRepository;
import com.joe.taskira.user.entity.User;
import com.joe.taskira.user.enums.GlobalRole;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Single-ticket PDF report: reference, fields, description, comments, and a QR code
 * (QrCodeGenerator) linking back to the ticket's web page - see ADR-0022. HTML is built
 * directly in Java (text blocks), not a templating engine: a REST-only backend with no
 * other server-rendered views doesn't need one for a single fixed report layout.
 */
@Service
@RequiredArgsConstructor
public class TicketPdfExportService {

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneOffset.UTC);

    private final TicketRepository ticketRepository;
    private final CommentRepository commentRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final QrCodeGenerator qrCodeGenerator;

    @Value("${app.exports.frontend-url}")
    private String frontendUrl;

    public record PdfExport(String filename, byte[] content) {
    }

    public PdfExport exportTicket(Long ticketId) {
        Ticket ticket = ticketRepository.findByIdWithRelations(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));

        assertCanAccessProject(ticket.getProject());

        List<Comment> comments = commentRepository.findByTicketIdWithRelations(ticketId);

        String ticketUrl = frontendUrl + "/projects/" + ticket.getProject().getId() + "/tickets/" + ticket.getId();
        String qrDataUri = qrCodeGenerator.generateDataUri(ticketUrl);

        String html = buildHtml(ticket, comments, qrDataUri);
        byte[] renderedPdf = renderToPdf(html);
        byte[] finalPdf = withMetadata(renderedPdf, ticket);

        return new PdfExport(ticket.getReference() + ".pdf", finalPdf);
    }

    private String buildHtml(Ticket ticket, List<Comment> comments, String qrDataUri) {
        StringBuilder commentsHtml = new StringBuilder();
        if (comments.isEmpty()) {
            commentsHtml.append("<p class=\"muted\">No comments.</p>");
        } else {
            for (Comment comment : comments) {
                // &#183; (numeric XML character reference), not the named HTML entity
                // &middot; - OpenHTMLtoPDF parses this as strict XHTML, and named
                // entities beyond amp/lt/gt/quot/apos require a DTD it doesn't have.
                commentsHtml.append("""
                        <div class="comment">
                          <div class="comment-meta">%s &#183; %s</div>
                          <div class="comment-content">%s</div>
                        </div>
                        """.formatted(
                        HtmlUtils.htmlEscape(displayName(comment.getUser())),
                        TIMESTAMP_FORMAT.format(comment.getCreatedAt()),
                        HtmlUtils.htmlEscape(comment.getContent()).replace("\n", "<br/>")
                ));
            }
        }

        return """
                <html>
                <head>
                <style>
                  body { font-family: sans-serif; font-size: 11pt; color: #1a1a1a; }
                  h1 { font-size: 18pt; margin-bottom: 0; }
                  .reference { color: #666; font-size: 10pt; }
                  table.fields { width: 100%%; border-collapse: collapse; margin: 16px 0; }
                  table.fields td { padding: 4px 8px; border: 1px solid #ddd; vertical-align: top; }
                  table.fields td.label { font-weight: bold; width: 120px; background: #f5f5f5; }
                  .description { white-space: pre-wrap; margin: 12px 0; }
                  .qr { text-align: right; }
                  .qr img { width: 100px; height: 100px; }
                  .comment { border-top: 1px solid #eee; padding: 8px 0; }
                  .comment-meta { color: #666; font-size: 9pt; }
                  .muted { color: #999; }
                </style>
                </head>
                <body>
                  <table style="width: 100%%; border: none;">
                    <tr>
                      <td style="border: none;">
                        <h1>%s</h1>
                        <div class="reference">%s</div>
                      </td>
                      <td class="qr" style="border: none;">
                        <img src="%s" alt="QR code" />
                      </td>
                    </tr>
                  </table>

                  <table class="fields">
                    <tr><td class="label">Type</td><td>%s</td></tr>
                    <tr><td class="label">Status</td><td>%s</td></tr>
                    <tr><td class="label">Priority</td><td>%s</td></tr>
                    <tr><td class="label">Creator</td><td>%s</td></tr>
                    <tr><td class="label">Assignee</td><td>%s</td></tr>
                    <tr><td class="label">Due date</td><td>%s</td></tr>
                  </table>

                  <div class="description">%s</div>

                  <h3>Comments</h3>
                  %s
                </body>
                </html>
                """.formatted(
                HtmlUtils.htmlEscape(ticket.getTitle()),
                HtmlUtils.htmlEscape(ticket.getReference()),
                qrDataUri,
                HtmlUtils.htmlEscape(ticket.getType().name()),
                HtmlUtils.htmlEscape(ticket.getStatus().name()),
                HtmlUtils.htmlEscape(ticket.getPriority().name()),
                HtmlUtils.htmlEscape(displayName(ticket.getCreator())),
                ticket.getAssignee() != null ? HtmlUtils.htmlEscape(displayName(ticket.getAssignee())) : "-",
                ticket.getDueDate() != null ? ticket.getDueDate().toString() : "-",
                ticket.getDescription() != null
                        ? HtmlUtils.htmlEscape(ticket.getDescription()).replace("\n", "<br/>")
                        : "",
                commentsHtml
        );
    }

    private byte[] renderToPdf(String html) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withProducer("Taskira");
            // No relative resources to resolve - the QR code is already a self-contained
            // data URI - so an empty base URI is safe here.
            builder.withHtmlContent(html, "");
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to render ticket PDF", e);
        }
    }

    // PDFBox's one production role in this module (see ADR-0022) - everything else it
    // does here is post-render metadata, not page layout, which OpenHTMLtoPDF already
    // handled.
    private byte[] withMetadata(byte[] pdfBytes, Ticket ticket) {
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(pdfBytes))) {
            PDDocumentInformation info = document.getDocumentInformation();
            info.setTitle(ticket.getReference() + " - " + ticket.getTitle());
            info.setAuthor("Taskira");
            info.setSubject("Ticket export");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to set ticket PDF metadata", e);
        }
    }

    private String displayName(User user) {
        return user.getFullName() + " <" + user.getEmail() + ">";
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
