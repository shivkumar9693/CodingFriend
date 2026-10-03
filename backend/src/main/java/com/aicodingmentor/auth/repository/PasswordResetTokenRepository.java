package com.aicodingmentor.auth.repository;

import com.aicodingmentor.auth.domain.PasswordResetToken;
import com.aicodingmentor.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {
    Optional<PasswordResetToken> findByToken(String token);
    void deleteByUser(User user);
    void deleteByToken(String token);
}
