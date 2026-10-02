package com.aicodingmentor.support.dto;

import com.aicodingmentor.support.domain.TicketStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateTicketStatusRequest(
        @NotNull(message = "Status is required")
        TicketStatus status,

        String adminNotes
) {
}
