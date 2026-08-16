package com.joe.taskira.attachments.controller;

import com.joe.taskira.attachments.dto.AttachmentResponse;
import com.joe.taskira.attachments.service.AttachmentService;
import com.joe.taskira.common.web.ApiVersion;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping(ApiVersion.V1)
@RequiredArgsConstructor
public class AttachmentController {

    private final AttachmentService attachmentService;

    @GetMapping("/tickets/{ticketId}/attachments")
    public List<AttachmentResponse> listTicketAttachments(@PathVariable Long ticketId) {
        return attachmentService.listTicketAttachments(ticketId);
    }

    @PostMapping("/tickets/{ticketId}/attachments")
    @ResponseStatus(HttpStatus.CREATED)
    public AttachmentResponse uploadAttachment(
            @PathVariable Long ticketId,
            @RequestParam("file") MultipartFile file
    ) {
        return attachmentService.uploadAttachment(ticketId, file);
    }

    @GetMapping("/attachments/{attachmentId}/content")
    public ResponseEntity<StreamingResponseBody> downloadAttachment(@PathVariable Long attachmentId) {
        AttachmentService.AttachmentDownload download = attachmentService.prepareDownload(attachmentId);

        StreamingResponseBody body = outputStream -> attachmentService.streamTo(download.storageKey(), outputStream);

        return ResponseEntity.ok()
                // "attachment", never "inline": a malicious SVG/HTML upload that somehow
                // passed the content-type allowlist must never render inside the app's
                // origin. filename encoded per RFC 6266 - the sanitized name is already
                // free of path separators/control chars, but non-ASCII characters still
                // need percent-encoding to be a valid header value.
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''"
                        + java.net.URLEncoder.encode(download.originalFilename(), StandardCharsets.UTF_8))
                .contentType(MediaType.parseMediaType(download.contentType()))
                .contentLength(download.sizeBytes())
                .body(body);
    }

    @DeleteMapping("/attachments/{attachmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAttachment(@PathVariable Long attachmentId) {
        attachmentService.deleteAttachment(attachmentId);
    }
}
