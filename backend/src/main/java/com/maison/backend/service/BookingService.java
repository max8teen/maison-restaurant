package com.maison.backend.service;

import com.maison.backend.entity.*;
import com.maison.backend.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class BookingService {

    private final ReservationRepository reservationRepository;
    private final RestaurantTableRepository restaurantTableRepository;
    private final BookingRefSeqRepository bookingRefSeqRepository;
    private final WaitlistRepository waitlistRepository;
    private final EmailService emailService;

    @Value("${restaurant.max-active-bookings:2}")
    private int maxActiveBookings;

    @Value("${restaurant.table-block-hours:2}")
    private int tableBlockHours;

    @Value("${restaurant.wa-enabled:true}")
    private boolean waEnabled;

    @Value("${restaurant.wa-business-number:917972666151}")
    private String waBusinessNumber;

    @Value("${restaurant.name:Maison Fine Dining}")
    private String restaurantName;

    @Value("${restaurant.url:http://localhost:8080}")
    private String restaurantUrl;

    public BookingService(ReservationRepository reservationRepository,
                          RestaurantTableRepository restaurantTableRepository,
                          BookingRefSeqRepository bookingRefSeqRepository,
                          WaitlistRepository waitlistRepository,
                          EmailService emailService) {
        this.reservationRepository = reservationRepository;
        this.restaurantTableRepository = restaurantTableRepository;
        this.bookingRefSeqRepository = bookingRefSeqRepository;
        this.waitlistRepository = waitlistRepository;
        this.emailService = emailService;
    }

    @Transactional
    public String generateBookingRef() {
        BookingRefSeq seq = bookingRefSeqRepository.save(new BookingRefSeq());
        long seqId = seq.getId();
        return String.format("MR-%04d", seqId);
    }

    public Map<String, Object> getAvailableTables(int party, String dateStr, String timeStr) {
        if (dateStr == null || dateStr.trim().isEmpty() || timeStr == null || timeStr.trim().isEmpty()) {
            List<RestaurantTable> allTables = restaurantTableRepository.findByCapacityGreaterThanEqualAndStatusNotOrderByCapacityAsc(party, "maintenance");
            List<Map<String, Object>> list = new ArrayList<>();
            for (RestaurantTable t : allTables) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", t.getId());
                map.put("table_number", t.getTableNumber());
                map.put("capacity", t.getCapacity());
                map.put("location", t.getLocation());
                map.put("status", t.getStatus());
                list.add(map);
            }
            return Map.of("tables", list);
        }

        LocalDate date = LocalDate.parse(dateStr.trim());
        LocalTime time = parseTime(timeStr.trim());

        List<RestaurantTable> allTables = restaurantTableRepository.findByCapacityGreaterThanEqualAndStatusNotOrderByCapacityAsc(party, "maintenance");
        List<Map<String, Object>> tableList = new ArrayList<>();
        int availableCount = 0;

        for (RestaurantTable table : allTables) {
            List<Reservation> reservations = reservationRepository.findActiveReservationsForTableAndDate(table.getId(), date);
            boolean isConflict = false;
            for (Reservation r : reservations) {
                LocalTime rTime = parseTime(r.getReservationTime());
                long diffMinutes = Math.abs(ChronoUnit.MINUTES.between(rTime, time));
                if (diffMinutes < (tableBlockHours * 60)) {
                    isConflict = true;
                    break;
                }
            }

            String realStatus = isConflict ? "booked" : "available";
            if (!isConflict) availableCount++;

            Map<String, Object> map = new HashMap<>();
            map.put("id", table.getId());
            map.put("table_number", table.getTableNumber());
            map.put("capacity", table.getCapacity());
            map.put("location", table.getLocation());
            map.put("status", realStatus);
            tableList.add(map);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("tables", tableList);
        result.put("available_count", availableCount);
        return result;
    }

    @Transactional
    public Map<String, Object> createBooking(User user, String name, String email, String phone,
                                            String dateStr, String timeStr, int partySize,
                                            String occasion, String requests, Long tableId) {

        if (user.getIsBlocked() != null && user.getIsBlocked() == 1) {
            throw new IllegalArgumentException("Your account has been suspended due to repeated no-shows. Please contact us.");
        }

        long activeCount = reservationRepository.countActiveBookingsForUser(user.getId(), LocalDate.now());
        if (activeCount >= maxActiveBookings) {
            throw new IllegalArgumentException("You already have " + maxActiveBookings + " active bookings. Please cancel one before making a new reservation.");
        }

        LocalDate date = LocalDate.parse(dateStr.trim());
        String tableNumber = null;
        String tableLocation = null;

        if (tableId != null && tableId > 0) {
            Optional<RestaurantTable> tOpt = restaurantTableRepository.findById(tableId);
            if (tOpt.isPresent()) {
                RestaurantTable table = tOpt.get();
                tableNumber = table.getTableNumber();
                tableLocation = table.getLocation();

                LocalTime targetTime = parseTime(timeStr.trim());
                List<Reservation> existing = reservationRepository.findActiveReservationsForTableAndDate(tableId, date);
                for (Reservation r : existing) {
                    LocalTime rTime = parseTime(r.getReservationTime());
                    long diffMinutes = Math.abs(ChronoUnit.MINUTES.between(rTime, targetTime));
                    if (diffMinutes < (tableBlockHours * 60)) {
                        throw new IllegalArgumentException("Sorry, that table was just booked. Please go back and select another.");
                    }
                }
            }
        }

        String ref = generateBookingRef();

        Reservation reservation = new Reservation();
        reservation.setBookingRef(ref);
        reservation.setUserId(user.getId());
        reservation.setGuestName(name);
        reservation.setGuestEmail(email);
        reservation.setGuestPhone(phone);
        reservation.setReservationDate(date);
        reservation.setReservationTime(timeStr);
        reservation.setPartySize(partySize);
        reservation.setOccasion(occasion != null ? occasion : "none");
        reservation.setSpecialRequests(requests);
        reservation.setStatus("confirmed");
        reservation.setTableId(tableId);
        reservation.setSource("online");

        reservationRepository.save(reservation);

        String waLink = null;
        if (waEnabled && waBusinessNumber != null && !waBusinessNumber.isEmpty()) {
            String formattedDate = date.format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
            String msg = "Hi " + restaurantName + ", my booking ref is " + ref + " for " + formattedDate + " at " + timeStr + ".";
            waLink = "https://wa.me/" + waBusinessNumber + "?text=" + URLEncoder.encode(msg, StandardCharsets.UTF_8);
        }

        emailService.sendBookingConfirmation(email, name, ref, date, timeStr, partySize, occasion, tableNumber, tableLocation);

        Map<String, Object> data = new HashMap<>();
        data.put("booking_ref", ref);
        data.put("table_number", tableNumber);
        data.put("table_location", tableLocation);
        data.put("whatsapp_link", waLink);
        return data;
    }

    @Transactional
    public void cancelBooking(User user, Long bookingId) {
        Reservation reservation = reservationRepository.findByIdAndUserId(bookingId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        if ("cancelled".equalsIgnoreCase(reservation.getStatus())) {
            throw new IllegalArgumentException("Booking is already cancelled");
        }

        reservation.setStatus("cancelled");
        reservationRepository.save(reservation);

        // Auto promote waitlist
        Optional<Waitlist> waitlistOpt = waitlistRepository
                .findFirstByReservationDateAndReservationTimeAndNotifiedOrderByCreatedAtAsc(
                        reservation.getReservationDate(), reservation.getReservationTime(), 0);

        if (waitlistOpt.isPresent()) {
            Waitlist wait = waitlistOpt.get();
            wait.setNotified(1);
            wait.setNotifiedAt(java.time.LocalDateTime.now());
            waitlistRepository.save(wait);

            String bookUrl = restaurantUrl + "/book.html?date=" + URLEncoder.encode(reservation.getReservationDate().toString(), StandardCharsets.UTF_8)
                    + "&time=" + URLEncoder.encode(reservation.getReservationTime(), StandardCharsets.UTF_8);
            emailService.sendWaitlistNotification(wait.getEmail(), wait.getName(), wait.getReservationDate(), wait.getReservationTime(), wait.getPartySize(), bookUrl);
        }

        emailService.sendCancellationEmail(reservation.getGuestEmail(), reservation.getGuestName(),
                reservation.getBookingRef(), reservation.getReservationDate(),
                reservation.getReservationTime(), reservation.getPartySize());
    }

    private LocalTime parseTime(String timeStr) {
        timeStr = timeStr.trim();
        try {
            if (timeStr.length() == 5) return LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm"));
            if (timeStr.length() == 8) return LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm:ss"));
            return LocalTime.parse(timeStr);
        } catch (Exception e) {
            return LocalTime.of(19, 0);
        }
    }
}
