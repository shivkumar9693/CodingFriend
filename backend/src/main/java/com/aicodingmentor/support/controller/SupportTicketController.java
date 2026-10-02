package com.aicodingmentor.support.controller;

import com.aicodingmentor.support.dto.CreateTicketRequest;
import com.aicodingmentor.support.dto.SupportTicketResponse;
import com.aicodingmentor.support.service.SupportTicketService;
import com.aicodingmentor.user.domain.User;
import com.aicodingmentor.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/support")
@Tag(name = "Support & Bug Reports", description = "Endpoints for students to submit tickets and view their support history")
public class SupportTicketController {

    private final SupportTicketService supportTicketService;
    private final UserRepository userRepository;

    public SupportTicketController(SupportTicketService supportTicketService, UserRepository userRepository) {
        this.supportTicketService = supportTicketService;
        this.userRepository = userRepository;
    }

    @PostMapping("/tickets")
    @Operation(summary = "Submit a support or bug ticket")
    public ResponseEntity<SupportTicketResponse> createTicket(
            Authentication authentication,
            @Valid @RequestBody CreateTicketRequest request
    ) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email).orElse(null);
        UUID userId = user != null ? user.getId() : UUID.nameUUIDFromBytes(email.getBytes());
        String fullName = user != null && user.getFullName() != null ? user.getFullName() : email;

        SupportTicketResponse response = supportTicketService.createTicket(
                userId,
                email,
                fullName,
                request
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/my-tickets")
    @Operation(summary = "Get current student's submitted tickets")
    public ResponseEntity<List<SupportTicketResponse>> getMyTickets(
            Authentication authentication
    ) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email).orElse(null);
        UUID userId = user != null ? user.getId() : UUID.nameUUIDFromBytes(email.getBytes());

        List<SupportTicketResponse> responses = supportTicketService.getUserTickets(userId);
        return ResponseEntity.ok(responses);
    }
}
