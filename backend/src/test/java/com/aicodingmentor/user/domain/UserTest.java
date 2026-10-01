package com.aicodingmentor.user.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    @DisplayName("User entity constructor initializes fields with correct defaults")
    void testUserInitialization() {
        User user = new User("dev@example.com", "hash123", "Dev User", UserRole.LEARNER);

        assertEquals("dev@example.com", user.getEmail());
        assertEquals("hash123", user.getPasswordHash());
        assertEquals("Dev User", user.getFullName());
        assertEquals(UserRole.LEARNER, user.getRole());
        assertNotNull(user.getCreatedAt());
        assertNotNull(user.getUpdatedAt());
    }

    @Test
    @DisplayName("User full constructor initializes profile fields correctly")
    void testUserFullConstructor() {
        User user = new User("dev@example.com", "hash123", "Dev User", "Coding enthusiast", "devuser", UserRole.USER);

        assertEquals("dev@example.com", user.getEmail());
        assertEquals("Dev User", user.getFullName());
        assertEquals("Coding enthusiast", user.getBio());
        assertEquals("devuser", user.getGithubUsername());
        assertEquals(UserRole.USER, user.getRole());
    }

    @Test
    @DisplayName("User onCreate callback populates timestamps and default role if missing")
    void testUserPrePersist() {
        User user = new User();
        user.setEmail("admin@example.com");
        user.setPasswordHash("admin_hash");
        user.setFullName("Admin User");
        user.setRole(null);
        user.setCreatedAt(null);
        user.setUpdatedAt(null);

        user.onCreate();

        assertEquals(UserRole.LEARNER, user.getRole());
        assertNotNull(user.getCreatedAt());
        assertNotNull(user.getUpdatedAt());
    }

    @Test
    @DisplayName("User equality is consistent based on UUID and email")
    void testUserEquality() {
        UUID id = UUID.randomUUID();
        User user1 = new User("user@example.com", "hash", "User One", UserRole.LEARNER);
        user1.setId(id);

        User user2 = new User("user@example.com", "hash2", "User Two", UserRole.ADMIN);
        user2.setId(id);

        assertEquals(user1, user2);
        assertEquals(user1.hashCode(), user2.hashCode());
    }
}
