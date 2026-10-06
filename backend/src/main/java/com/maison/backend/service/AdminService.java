package com.maison.backend.service;

import com.maison.backend.entity.*;
import com.maison.backend.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class AdminService {

    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final CustomerNoteRepository customerNoteRepository;
    private final RestaurantTableRepository restaurantTableRepository;
    private final ReviewRepository reviewRepository;
    private final WaitlistRepository waitlistRepository;
    private final EmailService emailService;

    @Value("${restaurant.no-show-block-limit:3}")
    private int noShowBlockLimit;

    @Value("${restaurant.url:http://localhost:8080}")
    private String restaurantUrl;

    public AdminService(ReservationRepository reservationRepository,
                        UserRepository userRepository,
                        CustomerNoteRepository customerNoteRepository,
                        RestaurantTableRepository restaurantTableRepository,
                        ReviewRepository reviewRepository,
                        WaitlistRepository waitlistRepository,
                        EmailService emailService) {
        this.reservationRepository = reservationRepository;
        this.userRepository = userRepository;
        this.customerNoteRepository = customerNoteRepository;
        this.restaurantTableRepository = restaurantTableRepository;
        this.reviewRepository = reviewRepository;
        this.waitlistRepository = waitlistRepository;
        this.emailService = emailService;
    }

    public Map<String, Object> getStats() {
        LocalDate today = LocalDate.now();

        long todayBookings = reservationRepository.countByReservationDate(today);
        long todayGuests = reservationRepository.sumPartySizeByReservationDate(today);
        long pending = reservationRepository.countByStatus("pending");
        long totalBookings = reservationRepository.count();

        Map<String, Long> statusCounts = new HashMap<>();
        for (Object[] row : reservationRepository.countGroupedByStatus()) {
            statusCounts.put((String) row[0], (Long) row[1]);
        }

        List<Reservation> recentList = reservationRepository.findTop5ByOrderByCreatedAtDesc();
        List<Map<String, Object>> recent = new ArrayList<>();
        for (Reservation r : recentList) {
            Map<String, Object> m = new HashMap<>();
            m.put("booking_ref", r.getBookingRef());
            m.put("guest_name", r.getGuestName());
            m.put("reservation_date", r.getReservationDate());
            m.put("reservation_time", r.getReservationTime());
            m.put("party_size", r.getPartySize());
            m.put("status", r.getStatus());
            recent.add(m);
        }

        List<Map<String, Object>> monthlyTrend = new ArrayList<>();
        DateTimeFormatter monthLabelFormatter = DateTimeFormatter.ofPattern("MMM");

        for (int i = 5; i >= 0; i--) {
            LocalDate d = today.minusMonths(i);
            String label = d.format(monthLabelFormatter);
            long count = reservationRepository.countBookingsByYearAndMonth(d.getYear(), d.getMonthValue());

            Map<String, Object> m = new HashMap<>();
            m.put("month", label);
            m.put("count", count);
            monthlyTrend.add(m);
        }

        List<Map<String, Object>> timeSlots = new ArrayList<>();
        for (Object[] row : reservationRepository.findPeakTimeSlots()) {
            Map<String, Object> m = new HashMap<>();
            m.put("time", row[0]);
            m.put("count", row[1]);
            timeSlots.add(m);
            if (timeSlots.size() >= 6) break;
        }

        Map<String, Object> res = new HashMap<>();
        res.put("today_bookings", todayBookings);
        res.put("today_guests", todayGuests);
        res.put("pending", pending);
        res.put("total_bookings", totalBookings);
        res.put("status_counts", statusCounts);
        res.put("recent", recent);
        res.put("monthly_trend", monthlyTrend);
        res.put("time_slots", timeSlots);

        return res;
    }

    @Transactional
    public Map<String, Object> updateReservationStatus(Long id, String status) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));

        reservation.setStatus(status);
        reservationRepository.save(reservation);

        boolean userBlocked = false;
        int noShowCount = 0;

        if ("no_show".equalsIgnoreCase(status) && reservation.getUserId() != null) {
            Optional<User> uOpt = userRepository.findById(reservation.getUserId());
            if (uOpt.isPresent()) {
                User user = uOpt.get();
                noShowCount = (user.getNoShowCount() != null ? user.getNoShowCount() : 0) + 1;
                user.setNoShowCount(noShowCount);
                if (noShowCount >= noShowBlockLimit) {
                    user.setIsBlocked(1);
                    userBlocked = true;
                }
                userRepository.save(user);
            }
        }

        if ("cancelled".equalsIgnoreCase(status)) {
            Optional<Waitlist> waitlistOpt = waitlistRepository
                    .findFirstByReservationDateAndReservationTimeAndNotifiedOrderByCreatedAtAsc(
                            reservation.getReservationDate(), reservation.getReservationTime(), 0);

            if (waitlistOpt.isPresent()) {
                Waitlist wait = waitlistOpt.get();
                wait.setNotified(1);
                wait.setNotifiedAt(LocalDateTime.now());
                waitlistRepository.save(wait);

                String bookUrl = restaurantUrl + "/book.html?date=" + URLEncoder.encode(reservation.getReservationDate().toString(), StandardCharsets.UTF_8)
                        + "&time=" + URLEncoder.encode(reservation.getReservationTime(), StandardCharsets.UTF_8);
                emailService.sendWaitlistNotification(wait.getEmail(), wait.getName(), wait.getReservationDate(), wait.getReservationTime(), wait.getPartySize(), bookUrl);
            }
        }

        if (reservation.getGuestEmail() != null && !"seated".equalsIgnoreCase(status)) {
            emailService.sendStatusChangeEmail(reservation.getGuestEmail(), reservation.getGuestName(),
                    reservation.getBookingRef(), reservation.getReservationDate(), reservation.getReservationTime(), status);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("user_blocked", userBlocked);
        result.put("no_show_count", noShowCount);
        return result;
    }

    public List<Map<String, Object>> getCustomers() {
        List<User> users = userRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();
        for (User u : users) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", u.getId());
            map.put("full_name", u.getFullName());
            map.put("email", u.getEmail());
            map.put("phone", u.getPhone());
            map.put("role", u.getRole());
            map.put("no_show_count", u.getNoShowCount());
            map.put("is_blocked", u.getIsBlocked());
            map.put("created_at", u.getCreatedAt());

            List<Reservation> bookings = reservationRepository.findByUserIdOrderByReservationDateDescReservationTimeDesc(u.getId());
            map.put("total_bookings", bookings.size());
            map.put("last_visit", bookings.isEmpty() ? null : bookings.get(0).getReservationDate());

            result.add(map);
        }
        return result;
    }

    @Transactional
    public void saveNotes(Long userId, String noteText) {
        Optional<CustomerNote> noteOpt = customerNoteRepository.findByUserId(userId);
        CustomerNote note;
        if (noteOpt.isPresent()) {
            note = noteOpt.get();
            note.setNote(noteText);
            note.setUpdatedAt(LocalDateTime.now());
        } else {
            note = new CustomerNote(userId, noteText);
        }
        customerNoteRepository.save(note);
    }
}
