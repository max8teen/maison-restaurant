package com.maison.backend.repository;

import com.maison.backend.entity.OtpAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface OtpAttemptRepository extends JpaRepository<OtpAttempt, Long> {
    int countByEmailAndCreatedAtAfter(String email, LocalDateTime after);
}
