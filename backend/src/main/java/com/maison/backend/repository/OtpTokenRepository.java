package com.maison.backend.repository;

import com.maison.backend.entity.OtpToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {
    int countByEmailAndPurposeAndUsedAndCreatedAtAfter(String email, String purpose, Integer used, LocalDateTime after);

    Optional<OtpToken> findFirstByEmailAndCodeAndPurposeAndUsedAndExpiresAtAfter(
            String email, String code, String purpose, Integer used, LocalDateTime now
    );

    @Transactional
    @Modifying
    @Query("UPDATE OtpToken o SET o.used = 1 WHERE o.email = ?1 AND o.purpose = ?2 AND o.used = 0")
    void invalidatePreviousOtps(String email, String purpose);
}
