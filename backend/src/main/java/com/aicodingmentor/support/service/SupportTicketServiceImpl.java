package com.aicodingmentor.support.service;

import com.aicodingmentor.common.error.ResourceNotFoundException;
import com.aicodingmentor.support.domain.SupportTicket;
import com.aicodingmentor.support.domain.TicketCategory;
import com.aicodingmentor.support.domain.TicketStatus;
import com.aicodingmentor.support.dto.CreateTicketRequest;
import com.aicodingmentor.support.dto.SupportTicketResponse;
import com.aicodingmentor.support.dto.UpdateTicketStatusRequest;
import com.aicodingmentor.support.repository.SupportTicketRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class SupportTicketServiceImpl implements SupportTicketService {

    private final SupportTicketRepository ticketRepository;

    public SupportTicketServiceImpl(SupportTicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    @Override
    public SupportTicketResponse createTicket(UUID userId, String userEmail, String userFullName, CreateTicketRequest request) {
        SupportTicket ticket = new SupportTicket(
                userId,
                userEmail,
                userFullName,
                request.category(),
                request.priority(),
                request.subject(),
                request.description(),
                request.problemId(),
                request.problemTitle(),
                request.codeSnippet()
        );

        SupportTicket saved = ticketRepository.save(ticket);
        return SupportTicketResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupportTicketResponse> getUserTickets(UUID userId) {
        return ticketRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(SupportTicketResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SupportTicketResponse> searchTickets(TicketStatus status, TicketCategory category, String search, Pageable pageable) {
        String cleanSearch = (search != null && !search.isBlank()) ? search.trim() : null;
        return ticketRepository.searchTickets(status, category, cleanSearch, pageable)
                .map(SupportTicketResponse::from);
    }

    @Override
    public SupportTicketResponse updateTicketStatus(UUID ticketId, UpdateTicketStatusRequest request) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Support ticket not found with id: " + ticketId));

        ticket.setStatus(request.status());
        if (request.adminNotes() != null) {
            ticket.setAdminNotes(request.adminNotes());
        }

        SupportTicket updated = ticketRepository.save(ticket);
        return SupportTicketResponse.from(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public long countOpenTickets() {
        return ticketRepository.countByStatus(TicketStatus.OPEN);
    }
}
