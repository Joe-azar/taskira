package com.joe.taskira.exports.service;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.joe.taskira.comment.entity.Comment;
import com.joe.taskira.comment.repository.CommentRepository;
import com.joe.taskira.common.exception.ForbiddenException;
import com.joe.taskira.exports.util.QrCodeGenerator;
import com.joe.taskira.project.entity.Project;
import com.joe.taskira.project.enums.ProjectStatus;
import com.joe.taskira.project.repository.ProjectMemberRepository;
import com.joe.taskira.security.model.AuthenticatedUser;
import com.joe.taskira.ticket.entity.Ticket;
import com.joe.taskira.ticket.enums.TicketPriority;
import com.joe.taskira.ticket.enums.TicketStatus;
import com.joe.taskira.ticket.enums.TicketType;
import com.joe.taskira.ticket.repository.TicketRepository;
import com.joe.taskira.user.entity.User;
import com.joe.taskira.user.enums.GlobalRole;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketPdfExportServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    private TicketPdfExportService service;

    private User member;
    private Project project;
    private Ticket ticket;

    @BeforeEach
    void setUp() {
        service = new TicketPdfExportService(ticketRepository, commentRepository, projectMemberRepository, new QrCodeGenerator());
        ReflectionTestUtils.setField(service, "frontendUrl", "https://taskira.test");

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
                .description("A description with <angle brackets> & an ampersand")
                .type(TicketType.TASK).status(TicketStatus.OPEN).priority(TicketPriority.HIGH)
                .creator(owner).build();
        ReflectionTestUtils.setField(ticket, "id", 42L);

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
    void exportProducesARealPdfWithTicketContentAndAScannableQrCode() throws Exception {
        when(ticketRepository.findByIdWithRelations(42L)).thenReturn(Optional.of(ticket));
        when(projectMemberRepository.existsByProjectIdAndUserId(100L, 1L)).thenReturn(true);

        Comment comment = Comment.builder().ticket(ticket).user(member).content("A real comment body").build();
        ReflectionTestUtils.setField(comment, "id", 500L);
        ReflectionTestUtils.setField(comment, "createdAt", Instant.now());
        when(commentRepository.findByTicketIdWithRelations(42L)).thenReturn(List.of(comment));

        TicketPdfExportService.PdfExport export = service.exportTicket(42L);

        assertThat(export.filename()).isEqualTo("EXP-1.pdf");

        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(export.content()))) {
            // Real rendered text, not just "a PDF was produced" - proves HTML-escaping
            // preserved the actual words (only the angle brackets/ampersand themselves
            // are escaped, not the surrounding text).
            String text = new PDFTextStripper().getText(document);
            assertThat(text)
                    .contains("EXP-1")
                    .contains("Sample ticket")
                    .contains("angle brackets")
                    .contains("A real comment body");

            assertThat(document.getDocumentInformation().getTitle()).contains("EXP-1");

            // Rasterize the actual rendered page and decode it with ZXing - proves the
            // embedded QR data URI survived the HTML -> PDF pipeline as a real, scannable
            // code, not just that QrCodeGenerator works in isolation.
            BufferedImage pageImage = new PDFRenderer(document).renderImageWithDPI(0, 150);
            Result decoded = new MultiFormatReader().decode(
                    new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(pageImage)))
            );
            assertThat(decoded.getText()).isEqualTo("https://taskira.test/projects/100/tickets/42");
        }
    }

    @Test
    void exportRejectsANonMemberOfTheProject() {
        when(ticketRepository.findByIdWithRelations(42L)).thenReturn(Optional.of(ticket));
        when(projectMemberRepository.existsByProjectIdAndUserId(100L, 1L)).thenReturn(false);

        assertThatThrownBy(() -> service.exportTicket(42L))
                .isInstanceOf(ForbiddenException.class);
    }
}
