package com.maison.backend.controller;

import com.maison.backend.dto.*;
import com.maison.backend.entity.*;
import com.maison.backend.repository.*;
import com.maison.backend.service.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
@RequestMapping("/admin/api")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class AdminApiController {

    private final ReservationRepository reservationRepository;
    private final RestaurantTableRepository restaurantTableRepository;
    private final ReviewRepository reviewRepository;
    private final WaitlistRepository waitlistRepository;
    private final AuthService authService;
    private final AdminService adminService;
    private final EmailService emailService;

    public AdminApiController(ReservationRepository reservationRepository,
                              RestaurantTableRepository restaurantTableRepository,
                              ReviewRepository reviewRepository,
                              WaitlistRepository waitlistRepository,
                              AuthService authService,
                              AdminService adminService,
                              EmailService emailService) {
        this.reservationRepository = reservationRepository;
        this.restaurantTableRepository = restaurantTableRepository;
        this.reviewRepository = reviewRepository;
        this.waitlistRepository = waitlistRepository;
        this.authService = authService;
        this.adminService = adminService;
        this.emailService = emailService;
    }

    private boolean checkAdmin(HttpServletRequest request, String bodyToken) {
        Optional<User> userOpt = authService.getAuthUser(request, bodyToken);
        return userOpt.isPresent() && "admin".equalsIgnoreCase(userOpt.get().getRole());
    }

    // ── Get Stats ──────────────────────────────────────────────────────────────
    @RequestMapping(value = "/get-stats", method = {RequestMethod.GET, RequestMethod.POST})
    public ApiResponse<?> getStats(HttpServletRequest request, @RequestBody(required = false) Map<String, Object> body) {
        String bodyToken = body != null ? (String) body.get("_token") : null;
        if (!checkAdmin(request, bodyToken)) return ApiResponse.error("Unauthorized");

        Map<String, Object> stats = adminService.getStats();
        return ApiResponse.ok("Stats loaded", stats);
    }

    // ── Get All Reservations ───────────────────────────────────────────────────
    @RequestMapping(value = "/get-all-reservations", method = {RequestMethod.GET, RequestMethod.POST})
    public ApiResponse<?> getAllReservations(HttpServletRequest request, @RequestBody(required = false) Map<String, Object> body) {
        String bodyToken = body != null ? (String) body.get("_token") : null;
        if (!checkAdmin(request, bodyToken)) return ApiResponse.error("Unauthorized");

        List<Reservation> list = reservationRepository.findAllByOrderByReservationDateDescReservationTimeDesc();
        return ApiResponse.ok("Reservations loaded", Map.of("reservations", list));
    }

    // ── Update Reservation Status ──────────────────────────────────────────────
    @PostMapping(value = "/update-reservation")
    public ApiResponse<?> updateReservation(HttpServletRequest request, @RequestBody UpdateReservationRequest req) {
        if (!checkAdmin(request, req.getToken())) return ApiResponse.error("Unauthorized");
        if (req.getId() == null || req.getStatus() == null || req.getStatus().trim().isEmpty()) {
            return ApiResponse.error("ID and status required");
        }

        List<String> allowed = Arrays.asList("pending", "confirmed", "seated", "completed", "cancelled", "no_show");
        if (!allowed.contains(req.getStatus().trim().toLowerCase())) {
            return ApiResponse.error("Invalid status");
        }

        try {
            Map<String, Object> data = adminService.updateReservationStatus(req.getId(), req.getStatus().trim().toLowerCase());
            return ApiResponse.ok("Status updated", data);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // ── Get Customers ──────────────────────────────────────────────────────────
    @RequestMapping(value = "/get-customers", method = {RequestMethod.GET, RequestMethod.POST})
    public ApiResponse<?> getCustomers(HttpServletRequest request, @RequestBody(required = false) Map<String, Object> body) {
        String bodyToken = body != null ? (String) body.get("_token") : null;
        if (!checkAdmin(request, bodyToken)) return ApiResponse.error("Unauthorized");

        List<Map<String, Object>> customers = adminService.getCustomers();
        return ApiResponse.ok("Customers loaded", Map.of("customers", customers));
    }

    // ── Save Customer Notes ────────────────────────────────────────────────────
    @PostMapping(value = "/save-notes")
    public ApiResponse<?> saveNotes(HttpServletRequest request, @RequestBody SaveNotesRequest req) {
        if (!checkAdmin(request, req.getToken())) return ApiResponse.error("Unauthorized");
        if (req.getUserId() == null || req.getUserId() <= 0) return ApiResponse.error("User ID required");

        adminService.saveNotes(req.getUserId(), req.getNote() != null ? req.getNote().trim() : "");
        return ApiResponse.ok("Notes saved");
    }

    // ── Tables Management ──────────────────────────────────────────────────────
    @RequestMapping(value = "/get-all-tables", method = {RequestMethod.GET, RequestMethod.POST})
    public ApiResponse<?> getAllTables(HttpServletRequest request, @RequestBody(required = false) Map<String, Object> body) {
        String bodyToken = body != null ? (String) body.get("_token") : null;
        if (!checkAdmin(request, bodyToken)) return ApiResponse.error("Unauthorized");

        List<RestaurantTable> tables = restaurantTableRepository.findAllByOrderByCapacityAsc();
        return ApiResponse.ok("Tables loaded", Map.of("tables", tables));
    }

    @PostMapping(value = "/add-table")
    public ApiResponse<?> addTable(HttpServletRequest request, @RequestBody TableRequest req) {
        if (!checkAdmin(request, req.getToken())) return ApiResponse.error("Unauthorized");

        RestaurantTable table = new RestaurantTable();
        table.setTableNumber(req.getTableNumber() != null ? req.getTableNumber().trim() : "T-NEW");
        table.setCapacity(req.getCapacity() != null ? req.getCapacity() : 2);
        table.setLocation(req.getLocation() != null ? req.getLocation().trim() : "indoor");
        table.setStatus(req.getStatus() != null ? req.getStatus().trim() : "available");

        table = restaurantTableRepository.save(table);
        return ApiResponse.ok("Table added successfully", Map.of("table", table));
    }

    @PostMapping(value = "/update-table")
    public ApiResponse<?> updateTable(HttpServletRequest request, @RequestBody TableRequest req) {
        if (!checkAdmin(request, req.getToken())) return ApiResponse.error("Unauthorized");
        if (req.getId() == null) return ApiResponse.error("Table ID required");

        Optional<RestaurantTable> tOpt = restaurantTableRepository.findById(req.getId());
        if (tOpt.isEmpty()) return ApiResponse.error("Table not found");

        RestaurantTable table = tOpt.get();
        if (req.getTableNumber() != null) table.setTableNumber(req.getTableNumber().trim());
        if (req.getCapacity() != null) table.setCapacity(req.getCapacity());
        if (req.getLocation() != null) table.setLocation(req.getLocation().trim());
        if (req.getStatus() != null) table.setStatus(req.getStatus().trim());

        restaurantTableRepository.save(table);
        return ApiResponse.ok("Table updated successfully");
    }

    @PostMapping(value = "/delete-table")
    public ApiResponse<?> deleteTable(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        String bodyToken = (String) body.get("_token");
        if (!checkAdmin(request, bodyToken)) return ApiResponse.error("Unauthorized");

        Object idObj = body.get("id");
        Long id = idObj != null ? Long.parseLong(idObj.toString()) : 0L;
        if (id <= 0) return ApiResponse.error("Table ID required");

        restaurantTableRepository.deleteById(id);
        return ApiResponse.ok("Table deleted successfully");
    }

    // ── Reviews Moderation ─────────────────────────────────────────────────────
    @RequestMapping(value = "/get-all-reviews", method = {RequestMethod.GET, RequestMethod.POST})
    public ApiResponse<?> getAllReviews(HttpServletRequest request, @RequestBody(required = false) Map<String, Object> body) {
        String bodyToken = body != null ? (String) body.get("_token") : null;
        if (!checkAdmin(request, bodyToken)) return ApiResponse.error("Unauthorized");

        List<Review> reviews = reviewRepository.findAllByOrderByCreatedAtDesc();
        return ApiResponse.ok("Reviews loaded", Map.of("reviews", reviews));
    }

    @PostMapping(value = "/update-review")
    public ApiResponse<?> updateReview(HttpServletRequest request, @RequestBody UpdateReviewRequest req) {
        if (!checkAdmin(request, req.getToken())) return ApiResponse.error("Unauthorized");
        if (req.getId() == null || req.getStatus() == null) return ApiResponse.error("Review ID and status required");

        Optional<Review> rOpt = reviewRepository.findById(req.getId());
        if (rOpt.isEmpty()) return ApiResponse.error("Review not found");

        Review review = rOpt.get();
        review.setStatus(req.getStatus().trim().toLowerCase());
        reviewRepository.save(review);

        return ApiResponse.ok("Review status updated");
    }

    // ── Waitlist Management ────────────────────────────────────────────────────
    @RequestMapping(value = "/get-waitlist", method = {RequestMethod.GET, RequestMethod.POST})
    public ApiResponse<?> getWaitlist(HttpServletRequest request, @RequestBody(required = false) Map<String, Object> body) {
        String bodyToken = body != null ? (String) body.get("_token") : null;
        if (!checkAdmin(request, bodyToken)) return ApiResponse.error("Unauthorized");

        List<Waitlist> list = waitlistRepository.findAllByOrderByCreatedAtDesc();
        return ApiResponse.ok("Waitlist loaded", Map.of("waitlist", list));
    }

    @PostMapping(value = "/notify-waitlist")
    public ApiResponse<?> notifyWaitlist(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        String bodyToken = (String) body.get("_token");
        if (!checkAdmin(request, bodyToken)) return ApiResponse.error("Unauthorized");

        Object idObj = body.get("id");
        Long id = idObj != null ? Long.parseLong(idObj.toString()) : 0L;
        if (id <= 0) return ApiResponse.error("Waitlist ID required");

        Optional<Waitlist> wOpt = waitlistRepository.findById(id);
        if (wOpt.isEmpty()) return ApiResponse.error("Waitlist entry not found");

        Waitlist wait = wOpt.get();
        wait.setNotified(1);
        wait.setNotifiedAt(java.time.LocalDateTime.now());
        waitlistRepository.save(wait);

        String bookUrl = "http://localhost:8080/book.html?date=" + URLEncoder.encode(wait.getReservationDate().toString(), StandardCharsets.UTF_8)
                + "&time=" + URLEncoder.encode(wait.getReservationTime(), StandardCharsets.UTF_8);

        emailService.sendWaitlistNotification(wait.getEmail(), wait.getName(), wait.getReservationDate(), wait.getReservationTime(), wait.getPartySize(), bookUrl);
        return ApiResponse.ok("Customer notified");
    }
}
