package com.aicodingmentor.support.dto;

import com.aicodingmentor.support.domain.SupportTicket;
import com.aicodingmentor.support.domain.TicketCategory;
import com.aicodingmentor.support.domain.TicketPriority;
import com.aicodingmentor.support.domain.TicketStatus;

import java.time.Instant;
import java.util.UUID;

public record SupportTicketResponse(
        UUID id,
        UUID userId,
        String userEmail,
        String userFullName,
        TicketCategory category,
        TicketPriority priority,
        TicketStatus status,
        String subject,
        String description,
        String problemId,
        String problemTitle,
        String codeSnippet,
        String adminNotes,
        Instant createdAt,
        Instant updatedAt
) {
    public static SupportTicketResponse from(SupportTicket t) {
        if (t == null) return null;
        return new SupportTicketResponse(
                t.getId(),
                t.getUserId(),
                t.getUserEmail(),
                t.getUserFullName(),
                t.getCategory(),
                t.getPriority(),
                t.getStatus(),
                t.getSubject(),
                t.getDescription(),
                t.getProblemId(),
                t.getProblemTitle(),
                t.getCodeSnippet(),
                t.getAdminNotes(),
                t.getCreatedAt(),
                t.getUpdatedAt()
        );
    }
}
