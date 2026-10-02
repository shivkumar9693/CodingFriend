package com.aicodingmentor.support.controller;

import com.aicodingmentor.support.domain.TicketCategory;
import com.aicodingmentor.support.domain.TicketStatus;
import com.aicodingmentor.support.dto.SupportTicketResponse;
import com.aicodingmentor.support.dto.UpdateTicketStatusRequest;
import com.aicodingmentor.support.service.SupportTicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/support")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Support Desk", description = "Endpoints for administrators to triage and manage student support tickets")
public class AdminSupportTicketController {

    private final SupportTicketService supportTicketService;

    public AdminSupportTicketController(SupportTicketService supportTicketService) {
        this.supportTicketService = supportTicketService;
    }

    @GetMapping("/tickets")
    @Operation(summary = "Search and list all student support tickets")
    public ResponseEntity<Page<SupportTicketResponse>> searchTickets(
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) TicketCategory category,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<SupportTicketResponse> page = supportTicketService.searchTickets(status, category, search, pageable);
        return ResponseEntity.ok(page);
    }

    @PatchMapping("/tickets/{id}/status")
    @Operation(summary = "Update ticket status and add resolution notes")
    public ResponseEntity<SupportTicketResponse> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTicketStatusRequest request
    ) {
        SupportTicketResponse response = supportTicketService.updateTicketStatus(id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats")
    @Operation(summary = "Get support ticket summary statistics")
    public ResponseEntity<Map<String, Object>> getStats() {
        long openCount = supportTicketService.countOpenTickets();
        return ResponseEntity.ok(Map.of("openTickets", openCount));
    }
}
