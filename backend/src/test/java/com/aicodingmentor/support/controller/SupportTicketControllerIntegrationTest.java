package com.aicodingmentor.support.controller;

import com.aicodingmentor.support.domain.TicketCategory;
import com.aicodingmentor.support.domain.TicketPriority;
import com.aicodingmentor.support.domain.TicketStatus;
import com.aicodingmentor.support.dto.CreateTicketRequest;
import com.aicodingmentor.support.dto.UpdateTicketStatusRequest;
import com.aicodingmentor.support.repository.SupportTicketRepository;
import com.aicodingmentor.user.domain.User;
import com.aicodingmentor.user.domain.UserRole;
import com.aicodingmentor.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("SupportTicketController and AdminSupportTicketController Integration Tests")
class SupportTicketControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SupportTicketRepository ticketRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        ticketRepository.deleteAll();
        testUser = userRepository.findByEmail("learner@example.com")
                .orElseGet(() -> userRepository.save(new User("learner@example.com", "hash", "Alex Mercer", UserRole.LEARNER)));
    }

    @Test
    @WithMockUser(username = "learner@example.com", roles = {"USER", "LEARNER"})
    @DisplayName("Should create support ticket successfully")
    void shouldCreateTicket() throws Exception {
        CreateTicketRequest request = new CreateTicketRequest(
                TicketCategory.BUG_REPORT,
                TicketPriority.HIGH,
                "Editor syntax highlighting issue",
                "Line numbers disappear when resizing window",
                "two-sum",
                "Two Sum",
                "int a = 10;"
        );

        mockMvc.perform(post("/api/support/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.subject").value("Editor syntax highlighting issue"))
                .andExpect(jsonPath("$.category").value("BUG_REPORT"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    @WithMockUser(username = "learner@example.com", roles = {"USER", "LEARNER"})
    @DisplayName("Should get user submitted tickets")
    void shouldGetMyTickets() throws Exception {
        CreateTicketRequest request = new CreateTicketRequest(
                TicketCategory.PROBLEM_ISSUE,
                TicketPriority.MEDIUM,
                "Test case edge case question",
                "Is negative target allowed?",
                null,
                null,
                null
        );

        mockMvc.perform(post("/api/support/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/support/my-tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].subject").value("Test case edge case question"));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    @DisplayName("Admin can search tickets and update resolution status")
    void adminCanSearchAndUpdateTickets() throws Exception {
        CreateTicketRequest request = new CreateTicketRequest(
                TicketCategory.BUG_REPORT,
                TicketPriority.CRITICAL,
                "Docker container memory leak",
                "Sandbox memory limit exceeded on large loop",
                null,
                null,
                null
        );

        String responseJson = mockMvc.perform(post("/api/support/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String ticketId = objectMapper.readTree(responseJson).get("id").asText();

        // Admin search tickets
        mockMvc.perform(get("/api/admin/support/tickets")
                        .param("status", "OPEN")
                        .param("search", "memory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(ticketId));

        // Admin update status to RESOLVED
        UpdateTicketStatusRequest updateReq = new UpdateTicketStatusRequest(
                TicketStatus.RESOLVED,
                "Increased container cgroup memory limits to 256MB"
        );

        mockMvc.perform(patch("/api/admin/support/tickets/" + ticketId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.adminNotes").value("Increased container cgroup memory limits to 256MB"));
    }

    @Test
    @DisplayName("Should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/support/my-tickets"))
                .andExpect(status().isUnauthorized());
    }
}
