package com.maison.backend.repository;

import com.maison.backend.entity.UserToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserTokenRepository extends JpaRepository<UserToken, Long> {
    Optional<UserToken> findByTokenAndExpiresAtAfter(String token, LocalDateTime now);
    void deleteByUserId(Long userId);
}
