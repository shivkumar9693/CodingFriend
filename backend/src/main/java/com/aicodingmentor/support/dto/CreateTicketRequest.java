package com.aicodingmentor.support.dto;

import com.aicodingmentor.support.domain.TicketCategory;
import com.aicodingmentor.support.domain.TicketPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTicketRequest(
        @NotNull(message = "Category is required")
        TicketCategory category,

        TicketPriority priority,

        @NotBlank(message = "Subject cannot be blank")
        @Size(max = 255, message = "Subject cannot exceed 255 characters")
        String subject,

        @NotBlank(message = "Description cannot be blank")
        String description,

        String problemId,

        String problemTitle,

        String codeSnippet
) {
}
