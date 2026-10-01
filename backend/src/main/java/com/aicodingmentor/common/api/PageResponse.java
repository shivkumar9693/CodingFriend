package com.aicodingmentor.common.api;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

@Schema(description = "Generic paginated response wrapper")
public record PageResponse<T>(
        @Schema(description = "List of items on the current page")
        List<T> content,

        @Schema(description = "Zero-based page index", example = "0")
        int pageNumber,

        @Schema(description = "Page size", example = "10")
        int pageSize,

        @Schema(description = "Total number of items matching filter", example = "42")
        long totalElements,

        @Schema(description = "Total number of pages available", example = "5")
        int totalPages,

        @Schema(description = "Whether current page is the first page", example = "true")
        boolean isFirst,

        @Schema(description = "Whether current page is the last page", example = "false")
        boolean isLast
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
