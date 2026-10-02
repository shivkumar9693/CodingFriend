package com.aicodingmentor.support.repository;

import com.aicodingmentor.support.domain.SupportTicket;
import com.aicodingmentor.support.domain.TicketCategory;
import com.aicodingmentor.support.domain.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, UUID> {

    List<SupportTicket> findByUserIdOrderByCreatedAtDesc(UUID userId);

    @Query("SELECT t FROM SupportTicket t WHERE " +
           "(:status IS NULL OR t.status = :status) AND " +
           "(:category IS NULL OR t.category = :category) AND " +
           "(:search IS NULL OR LOWER(t.subject) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(t.userFullName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(t.userEmail) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(COALESCE(t.problemTitle, '')) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<SupportTicket> searchTickets(
            @Param("status") TicketStatus status,
            @Param("category") TicketCategory category,
            @Param("search") String search,
            Pageable pageable
    );

    long countByStatus(TicketStatus status);
}
