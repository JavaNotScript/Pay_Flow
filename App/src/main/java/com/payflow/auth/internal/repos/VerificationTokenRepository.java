package com.payflow.auth.internal.repos;

import com.payflow.auth.internal.domain.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken,UUID> {
    Optional<VerificationToken> findByTokenHash(String verificationToken);
}
