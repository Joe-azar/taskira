package com.joe.taskira.attachments.dto;

import com.joe.taskira.attachments.entity.Attachment;
import com.joe.taskira.user.dto.UserSummaryResponse;

import java.time.Instant;

public record AttachmentResponse(
        Long id,
        Long ticketId,
        UserSummaryResponse uploader,
        String originalFilename,
        String contentType,
        long sizeBytes,
        String sha256,
        Instant createdAt
) {
    public static AttachmentResponse from(Attachment attachment) {
        return new AttachmentResponse(
                attachment.getId(),
                attachment.getTicket().getId(),
                UserSummaryResponse.from(attachment.getUploader()),
                attachment.getOriginalFilename(),
                attachment.getContentType(),
                attachment.getSizeBytes(),
                attachment.getSha256(),
                attachment.getCreatedAt()
        );
    }
}
