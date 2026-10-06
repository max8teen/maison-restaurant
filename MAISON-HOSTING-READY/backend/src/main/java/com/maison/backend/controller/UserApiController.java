package com.maison.backend.controller;

import com.maison.backend.dto.*;
import com.maison.backend.entity.*;
import com.maison.backend.repository.*;
import com.maison.backend.service.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class UserApiController {

    private final UserRepository userRepository;
    private final UserTokenRepository userTokenRepository;
    private final OtpTokenRepository otpTokenRepository;
    private final OtpAttemptRepository otpAttemptRepository;
    private final ReservationRepository reservationRepository;
    private final ReviewRepository reviewRepository;
    private final WaitlistRepository waitlistRepository;
    private final KvCacheRepository kvCacheRepository;
    private final AuthService authService;
    private final EmailService emailService;
    private final BookingService bookingService;

    public UserApiController(UserRepository userRepository,
                             UserTokenRepository userTokenRepository,
                             OtpTokenRepository otpTokenRepository,
                             OtpAttemptRepository otpAttemptRepository,
                             ReservationRepository reservationRepository,
                             ReviewRepository reviewRepository,
                             WaitlistRepository waitlistRepository,
                             KvCacheRepository kvCacheRepository,
                             AuthService authService,
                             EmailService emailService,
                             BookingService bookingService) {
        this.userRepository = userRepository;
        this.userTokenRepository = userTokenRepository;
        this.otpTokenRepository = otpTokenRepository;
        this.otpAttemptRepository = otpAttemptRepository;
        this.reservationRepository = reservationRepository;
        this.reviewRepository = reviewRepository;
        this.waitlistRepository = waitlistRepository;
        this.kvCacheRepository = kvCacheRepository;
        this.authService = authService;
        this.emailService = emailService;
        this.bookingService = bookingService;
    }

    // ── Login ──────────────────────────────────────────────────────────────────
    @PostMapping(value = "/login")
    public ApiResponse<?> login(@RequestBody LoginRequest req) {
        if (req.getEmail() == null || req.getEmail().trim().isEmpty() ||
            req.getPassword() == null || req.getPassword().isEmpty()) {
            return ApiResponse.error("Email and password are required");
        }

        Optional<User> uOpt = userRepository.findByEmail(req.getEmail().trim());
        if (uOpt.isEmpty()) {
            return ApiResponse.error("Invalid email or password");
        }

        User user = uOpt.get();
        if (!authService.verifyPassword(req.getPassword(), user.getPassword())) {
            return ApiResponse.error("Invalid email or password");
        }

        String token = authService.createTokenForUser(user.getId());

        Map<String, Object> userData = new HashMap<>();
        userData.put("id", user.getId());
        userData.put("full_name", user.getFullName());
        userData.put("email", user.getEmail());
        userData.put("phone", user.getPhone());
        userData.put("role", user.getRole());

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("user", userData);

        return ApiResponse.ok("Login successful", data);
    }

    // ── Send OTP ───────────────────────────────────────────────────────────────
    @PostMapping(value = "/send-otp")
    public ApiResponse<?> sendOtp(@RequestBody SendOtpRequest req) {
        String email = req.getEmail() != null ? req.getEmail().trim() : "";
        String purpose = req.getPurpose() != null ? req.getPurpose().trim() : "register";

        if (email.isEmpty()) return ApiResponse.error("Email is required");
        if (!purpose.equals("register") && !purpose.equals("reset_password")) {
            return ApiResponse.error("Invalid purpose");
        }

        int rateCount = otpTokenRepository.countByEmailAndPurposeAndUsedAndCreatedAtAfter(
                email, purpose, 0, LocalDateTime.now().minusMinutes(10));
        if (rateCount >= 3) {
            return ApiResponse.error("Too many OTP requests. Please wait 10 minutes before trying again.");
        }

        Optional<User> uOpt = userRepository.findByEmail(email);
        boolean exists = uOpt.isPresent();

        if (purpose.equals("register") && exists) {
            return ApiResponse.error("An account with this email already exists. Please login instead.");
        }
        if (purpose.equals("reset_password") && !exists) {
            return ApiResponse.error("No account found with this email.");
        }

        otpTokenRepository.invalidatePreviousOtps(email, purpose);

        String otp = String.format("%06d", new java.security.SecureRandom().nextInt(1000000));
        LocalDateTime expires = LocalDateTime.now().plusMinutes(3);

        OtpToken otpToken = new OtpToken(email, otp, purpose, expires);
        otpTokenRepository.save(otpToken);

        String name = uOpt.map(User::getFullName)
                .orElse(req.getFullName() != null && !req.getFullName().trim().isEmpty() ? req.getFullName().trim() : "Guest");

        boolean emailSent;
        if (purpose.equals("register")) {
            emailSent = emailService.sendWelcomeOTP(email, name, otp);
        } else {
            emailSent = emailService.sendPasswordResetOTP(email, name, otp);
        }

        if (!emailSent) {
            // Do not tell the browser that the OTP was sent when SMTP failed.
            otpTokenRepository.delete(otpToken);
            return ApiResponse.error("We could not send the OTP email. Please check the email configuration and try again.");
        }

        return ApiResponse.ok("OTP sent to your email");
    }

    // ── Verify OTP & Register ──────────────────────────────────────────────────
    @PostMapping(value = "/verify-otp")
    public ApiResponse<?> verifyOtp(@RequestBody VerifyOtpRequest req) {
        String email = req.getEmail() != null ? req.getEmail().trim() : "";
        String otp = req.getOtp() != null ? req.getOtp().trim() : "";
        String purpose = req.getPurpose() != null ? req.getPurpose().trim() : "register";

        if (email.isEmpty() || otp.isEmpty()) return ApiResponse.error("Email and OTP are required");

        int attempts = otpAttemptRepository.countByEmailAndCreatedAtAfter(email, LocalDateTime.now().minusMinutes(15));
        if (attempts >= 5) {
            return ApiResponse.error("Too many incorrect attempts. Please wait 15 minutes and try again.");
        }

        Optional<OtpToken> otpOpt = otpTokenRepository.findFirstByEmailAndCodeAndPurposeAndUsedAndExpiresAtAfter(
                email, otp, purpose, 0, LocalDateTime.now());

        if (otpOpt.isEmpty()) {
            otpAttemptRepository.save(new OtpAttempt(email));
            return ApiResponse.error("Invalid or expired OTP. Please try again.");
        }

        OtpToken otpToken = otpOpt.get();
        otpToken.setUsed(1);
        otpTokenRepository.save(otpToken);

        if (purpose.equals("register")) {
            String name = req.getFullName() != null ? req.getFullName().trim() : "";
            String phone = req.getPhone() != null ? req.getPhone().trim() : "";
            String password = req.getPassword();

            if (name.isEmpty() || password == null || password.isEmpty()) {
                return ApiResponse.error("Name and password are required");
            }
            if (password.length() < 6) {
                return ApiResponse.error("Password must be at least 6 characters");
            }

            if (userRepository.existsByEmail(email)) {
                return ApiResponse.error("Failed to create account. Email may already exist.");
            }

            User user = new User();
            user.setFullName(name);
            user.setEmail(email);
            user.setPhone(phone);
            user.setPassword(authService.hashPassword(password));
            user.setRole("customer");

            user = userRepository.save(user);

            String token = authService.createTokenForUser(user.getId());

            Map<String, Object> userData = new HashMap<>();
            userData.put("id", user.getId());
            userData.put("full_name", user.getFullName());
            userData.put("email", user.getEmail());
            userData.put("phone", user.getPhone());
            userData.put("role", user.getRole());

            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("user", userData);

            return ApiResponse.ok("Account created successfully", data);
        }

        if (purpose.equals("reset_password")) {
            return ApiResponse.ok("OTP verified successfully", Map.of("email", email));
        }

        return ApiResponse.error("Invalid purpose");
    }

    // ── Reset Password ─────────────────────────────────────────────────────────
    @PostMapping(value = "/reset-password")
    public ApiResponse<?> resetPassword(@RequestBody ResetPasswordRequest req) {
        String email = req.getEmail() != null ? req.getEmail().trim() : "";
        String password = req.getPassword();

        if (email.isEmpty() || password == null || password.isEmpty()) {
            return ApiResponse.error("Email and password are required");
        }
        if (password.length() < 6) {
            return ApiResponse.error("Password must be at least 6 characters");
        }

        // Verify that an OTP was recently verified for this email (within 10 minutes)
        int verifiedOtps = otpTokenRepository.countByEmailAndPurposeAndUsedAndCreatedAtAfter(
                email, "reset_password", 1, LocalDateTime.now().minusMinutes(10));
        if (verifiedOtps <= 0) {
            return ApiResponse.error("Password reset requires a verified OTP. Please verify your OTP first.");
        }

        Optional<User> uOpt = userRepository.findByEmail(email);
        if (uOpt.isEmpty()) return ApiResponse.error("Account not found");

        User user = uOpt.get();
        user.setPassword(authService.hashPassword(password));
        userRepository.save(user);

        return ApiResponse.ok("Password reset successfully");
    }

    // ── Get Profile ────────────────────────────────────────────────────────────
    @RequestMapping(value = "/get-profile", method = {RequestMethod.GET, RequestMethod.POST})
    public ApiResponse<?> getProfile(HttpServletRequest request, @RequestBody(required = false) Map<String, Object> body) {
        String bodyToken = body != null ? (String) body.get("_token") : null;
        Optional<User> userOpt = authService.getAuthUser(request, bodyToken);
        if (userOpt.isEmpty()) return ApiResponse.error("Unauthorized");

        User u = userOpt.get();
        Map<String, Object> map = new HashMap<>();
        map.put("id", u.getId());
        map.put("full_name", u.getFullName());
        map.put("email", u.getEmail());
        map.put("phone", u.getPhone());
        map.put("role", u.getRole());
        map.put("gender", u.getGender());
        map.put("dob", u.getDob());
        return ApiResponse.ok("Profile loaded", map);
    }

    // ── Update Profile ─────────────────────────────────────────────────────────
    @PostMapping(value = "/update-profile")
    public ApiResponse<?> updateProfile(HttpServletRequest request, @RequestBody UpdateProfileRequest req) {
        Optional<User> userOpt = authService.getAuthUser(request, req.getToken());
        if (userOpt.isEmpty()) return ApiResponse.error("Unauthorized");

        User u = userOpt.get();
        if (req.getFullName() != null && !req.getFullName().trim().isEmpty()) {
            u.setFullName(req.getFullName().trim());
        }
        if (req.getPhone() != null) u.setPhone(req.getPhone().trim());
        if (req.getGender() != null) u.setGender(req.getGender().trim());
        if (req.getDob() != null && !req.getDob().trim().isEmpty()) {
            try { u.setDob(LocalDate.parse(req.getDob().trim())); } catch (Exception ignored) {}
        }

        userRepository.save(u);
        return ApiResponse.ok("Profile updated successfully");
    }

    // ── Get Tables ─────────────────────────────────────────────────────────────
    @RequestMapping(value = "/get-tables", method = {RequestMethod.GET, RequestMethod.POST})
    public ApiResponse<?> getTables(@RequestParam(value = "party", defaultValue = "1") int party,
                                     @RequestParam(value = "date", required = false) String date,
                                     @RequestParam(value = "time", required = false) String time) {
        Map<String, Object> tablesData = bookingService.getAvailableTables(party, date, time);
        return ApiResponse.ok("Tables loaded", tablesData);
    }

    // ── Create Booking ─────────────────────────────────────────────────────────
    @PostMapping(value = "/create-booking")
    public ApiResponse<?> createBooking(HttpServletRequest request, @RequestBody CreateBookingRequest req) {
        Optional<User> userOpt = authService.getAuthUser(request, req.getToken());
        if (userOpt.isEmpty()) return ApiResponse.error("Please login first");

        try {
            Map<String, Object> bookingData = bookingService.createBooking(
                    userOpt.get(),
                    req.getGuestName(),
                    req.getGuestEmail(),
                    req.getGuestPhone(),
                    req.getReservationDate(),
                    req.getReservationTime(),
                    req.getPartySize() != null ? req.getPartySize() : 2,
                    req.getOccasion(),
                    req.getSpecialRequests(),
                    req.getTableId()
            );
            return ApiResponse.ok("Booking confirmed", bookingData);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("Booking failed. Please try again.");
        }
    }

    // ── Get Bookings ───────────────────────────────────────────────────────────
    @RequestMapping(value = "/get-bookings", method = {RequestMethod.GET, RequestMethod.POST})
    public ApiResponse<?> getBookings(HttpServletRequest request, @RequestBody(required = false) Map<String, Object> body) {
        String bodyToken = body != null ? (String) body.get("_token") : null;
        Optional<User> userOpt = authService.getAuthUser(request, bodyToken);
        if (userOpt.isEmpty()) return ApiResponse.error("Please login first");

        List<Reservation> list = reservationRepository.findByUserIdOrderByReservationDateDescReservationTimeDesc(userOpt.get().getId());
        return ApiResponse.ok("Bookings loaded", Map.of("bookings", list));
    }

    // ── Cancel Booking ─────────────────────────────────────────────────────────
    @PostMapping(value = "/cancel-booking")
    public ApiResponse<?> cancelBooking(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        String bodyToken = (String) body.get("_token");
        Optional<User> userOpt = authService.getAuthUser(request, bodyToken);
        if (userOpt.isEmpty()) return ApiResponse.error("Please login first");

        Object bIdObj = body.get("booking_id");
        Long bookingId = bIdObj != null ? Long.parseLong(bIdObj.toString()) : 0L;
        if (bookingId <= 0) return ApiResponse.error("Invalid booking");

        try {
            bookingService.cancelBooking(userOpt.get(), bookingId);
            return ApiResponse.ok("Booking cancelled");
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("Failed to cancel booking");
        }
    }

    // ── Submit Review ──────────────────────────────────────────────────────────
    @PostMapping(value = "/submit-review")
    public ApiResponse<?> submitReview(HttpServletRequest request, @RequestBody SubmitReviewRequest req) {
        Optional<User> userOpt = authService.getAuthUser(request, req.getToken());
        if (userOpt.isEmpty()) return ApiResponse.error("Unauthorized");

        User user = userOpt.get();
        if (req.getBookingId() == null || req.getBookingId() <= 0) return ApiResponse.error("Invalid booking");
        if (req.getRating() == null || req.getRating() < 1 || req.getRating() > 5) {
            return ApiResponse.error("Invalid overall rating");
        }
        if (req.getComment() != null && req.getComment().length() > 300) {
            return ApiResponse.error("Comment must be under 300 characters");
        }

        Optional<Reservation> resOpt = reservationRepository.findByIdAndUserId(req.getBookingId(), user.getId());
        if (resOpt.isEmpty()) return ApiResponse.error("Booking not found");
        if (!"completed".equalsIgnoreCase(resOpt.get().getStatus())) {
            return ApiResponse.error("You can only review a completed dining experience");
        }

        Optional<Review> existingReview = reviewRepository.findByBookingIdAndUserId(req.getBookingId(), user.getId());
        if (existingReview.isPresent()) return ApiResponse.error("You have already reviewed this booking");

        Review review = new Review();
        review.setUserId(user.getId());
        review.setBookingId(req.getBookingId());
        review.setRating(req.getRating());
        review.setFoodRating(req.getFoodRating());
        review.setServiceRating(req.getServiceRating());
        review.setAmbienceRating(req.getAmbienceRating());
        review.setComment(req.getComment());
        review.setStatus("pending");

        reviewRepository.save(review);
        return ApiResponse.ok("Review submitted! It will appear after admin approval.");
    }

    // ── Get Reviews ────────────────────────────────────────────────────────────
    @GetMapping(value = "/get-reviews")
    public ApiResponse<?> getReviews() {
        List<Review> approved = reviewRepository.findByStatusOrderByCreatedAtDesc("approved");
        return ApiResponse.ok("Reviews loaded", Map.of("reviews", approved));
    }

    // ── Waitlist Operations ────────────────────────────────────────────────────
    @PostMapping(value = "/waitlist")
    public ApiResponse<?> waitlist(HttpServletRequest request, @RequestBody WaitlistRequest req) {
        Optional<User> userOpt = authService.getAuthUser(request, req.getToken());
        if (userOpt.isEmpty()) return ApiResponse.error("Please login first");

        User user = userOpt.get();
        String action = req.getAction() != null ? req.getAction().trim() : "join";

        if ("join".equalsIgnoreCase(action)) {
            if (req.getDate() == null || req.getTime() == null) return ApiResponse.error("Date and time are required");
            LocalDate date = LocalDate.parse(req.getDate().trim());
            String time = req.getTime().trim();

            Optional<Waitlist> dup = waitlistRepository.findFirstByUserIdAndReservationDateAndReservationTimeAndNotified(
                    user.getId(), date, time, 0);
            if (dup.isPresent()) return ApiResponse.error("You are already on the waitlist for this slot.");

            Waitlist wait = new Waitlist();
            wait.setUserId(user.getId());
            wait.setName(user.getFullName());
            wait.setEmail(user.getEmail());
            wait.setReservationDate(date);
            wait.setReservationTime(time);
            wait.setPartySize(req.getPartySize() != null ? req.getPartySize() : 2);
            wait.setNotified(0);

            wait = waitlistRepository.save(wait);

            long pos = waitlistRepository.countPositionInQueue(date, time, wait.getId());
            return ApiResponse.ok("Added to waitlist", Map.of("waitlist_id", wait.getId(), "position", pos));
        }

        if ("check".equalsIgnoreCase(action)) {
            if (req.getDate() == null || req.getTime() == null) return ApiResponse.error("Date and time required");
            LocalDate date = LocalDate.parse(req.getDate().trim());
            String time = req.getTime().trim();

            Optional<Waitlist> waitOpt = waitlistRepository.findFirstByUserIdAndReservationDateAndReservationTimeOrderByCreatedAtDesc(
                    user.getId(), date, time);
            if (waitOpt.isEmpty()) return ApiResponse.error("Not on waitlist for this slot");

            Waitlist w = waitOpt.get();
            long pos = waitlistRepository.countPositionInQueue(date, time, w.getId());
            return ApiResponse.ok("Waitlist status", Map.of("position", pos, "notified", w.getNotified() == 1));
        }

        if ("leave".equalsIgnoreCase(action)) {
            if (req.getId() != null) {
                Optional<Waitlist> wOpt = waitlistRepository.findByIdAndUserId(req.getId(), user.getId());
                wOpt.ifPresent(waitlistRepository::delete);
            }
            return ApiResponse.ok("Removed from waitlist");
        }

        return ApiResponse.error("Unknown action");
    }
}
