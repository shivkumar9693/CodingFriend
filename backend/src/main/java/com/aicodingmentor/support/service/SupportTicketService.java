package com.aicodingmentor.support.service;

import com.aicodingmentor.support.domain.TicketCategory;
import com.aicodingmentor.support.domain.TicketStatus;
import com.aicodingmentor.support.dto.CreateTicketRequest;
import com.aicodingmentor.support.dto.SupportTicketResponse;
import com.aicodingmentor.support.dto.UpdateTicketStatusRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface SupportTicketService {

    SupportTicketResponse createTicket(UUID userId, String userEmail, String userFullName, CreateTicketRequest request);

    List<SupportTicketResponse> getUserTickets(UUID userId);

    Page<SupportTicketResponse> searchTickets(TicketStatus status, TicketCategory category, String search, Pageable pageable);

    SupportTicketResponse updateTicketStatus(UUID ticketId, UpdateTicketStatusRequest request);

    long countOpenTickets();
}
