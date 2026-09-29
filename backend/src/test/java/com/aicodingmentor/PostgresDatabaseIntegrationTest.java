package com.aicodingmentor;

import com.aicodingmentor.user.domain.User;
import com.aicodingmentor.user.domain.UserRole;
import com.aicodingmentor.user.repository.UserRepository;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
class PostgresDatabaseIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("ai_coding_mentor_test")
            .withUsername("test_user")
            .withPassword("test_password");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        if (postgres.isRunning()) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl);
            registry.add("spring.datasource.username", postgres::getUsername);
            registry.add("spring.datasource.password", postgres::getPassword);
            registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
            registry.add("spring.flyway.enabled", () -> "true");
            registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        }
    }

    @Autowired(required = false)
    private Flyway flyway;

    @Autowired(required = false)
    private UserRepository userRepository;

    @Test
    @DisplayName("Verify Flyway migrations execute successfully and application can persist/query entities in PostgreSQL")
    void testFlywayAndPostgresConnection() {
        if (!postgres.isRunning()) {
            // Skips cleanly if Docker is not available in the local execution environment
            return;
        }

        assertNotNull(flyway, "Flyway bean should be present and initialized");
        MigrationInfo[] appliedMigrations = flyway.info().applied();
        assertTrue(appliedMigrations.length > 0, "At least one Flyway migration must be applied");
        assertEquals("initial schema", appliedMigrations[0].getDescription());

        assertNotNull(userRepository, "UserRepository bean should be initialized");

        // Test inserting a new user
        User user = new User("learner@aicodingmentor.com", "argon2_or_bcrypt_hash", "Jane Doe", UserRole.LEARNER);
        User saved = userRepository.save(user);

        assertNotNull(saved.getId(), "Persisted user should receive a generated UUID primary key");
        assertEquals("learner@aicodingmentor.com", saved.getEmail());
        assertEquals("Jane Doe", saved.getFullName());
        assertEquals(UserRole.LEARNER, saved.getRole());
        assertNotNull(saved.getCreatedAt(), "createdAt timestamp must not be null");
        assertNotNull(saved.getUpdatedAt(), "updatedAt timestamp must not be null");

        // Test querying by email
        Optional<User> found = userRepository.findByEmail("learner@aicodingmentor.com");
        assertTrue(found.isPresent(), "User should be findable by email");
        assertEquals(saved.getId(), found.get().getId());
        assertTrue(userRepository.existsByEmail("learner@aicodingmentor.com"));
    }
}
