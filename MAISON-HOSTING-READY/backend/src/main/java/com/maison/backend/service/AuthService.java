package com.maison.backend.service;

import com.maison.backend.entity.User;
import com.maison.backend.entity.UserToken;
import com.maison.backend.repository.UserRepository;
import com.maison.backend.repository.UserTokenRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserTokenRepository userTokenRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom;

    public AuthService(UserRepository userRepository, UserTokenRepository userTokenRepository) {
        this.userRepository = userRepository;
        this.userTokenRepository = userTokenRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
        this.secureRandom = new SecureRandom();
    }

    public String hashPassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    public boolean verifyPassword(String rawPassword, String encodedPassword) {
        if (encodedPassword == null || rawPassword == null) return false;
        // Fix for standard PHP $2y$ prefix if present
        if (encodedPassword.startsWith("$2y$")) {
            encodedPassword = "$2a$" + encodedPassword.substring(4);
        }
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    public String generateSecureToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    @Transactional
    public String createTokenForUser(Long userId) {
        userTokenRepository.deleteByUserId(userId);
        String token = generateSecureToken();
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(30);
        UserToken userToken = new UserToken(userId, token, expiresAt);
        userTokenRepository.save(userToken);
        return token;
    }

    public Optional<User> getAuthUser(HttpServletRequest request, String bodyToken) {
        String token = "";

        // 1. ?t= query param
        String paramToken = request.getParameter("t");
        if (paramToken != null && !paramToken.trim().isEmpty()) {
            token = paramToken.trim();
        }

        // 2. bodyToken (_token)
        if (token.isEmpty() && bodyToken != null && !bodyToken.trim().isEmpty()) {
            token = bodyToken.trim();
        }

        // 3. Authorization header
        if (token.isEmpty()) {
            String header = request.getHeader("Authorization");
            if (header != null && header.toLowerCase().startsWith("bearer ")) {
                token = header.substring(7).trim();
            }
        }

        if (token.isEmpty()) {
            return Optional.empty();
        }

        Optional<UserToken> tokenOpt = userTokenRepository.findByTokenAndExpiresAtAfter(token, LocalDateTime.now());
        if (tokenOpt.isEmpty()) {
            return Optional.empty();
        }

        return userRepository.findById(tokenOpt.get().getUserId());
    }
}
