package com.maison.backend.repository;

import com.maison.backend.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByUserIdOrderByReservationDateDescReservationTimeDesc(Long userId);

    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.userId = ?1 AND r.status IN ('confirmed','pending','seated') AND r.reservationDate >= ?2")
    long countActiveBookingsForUser(Long userId, LocalDate today);

    Optional<Reservation> findByIdAndUserId(Long id, Long userId);

    List<Reservation> findAllByOrderByReservationDateDescReservationTimeDesc();

    long countByReservationDate(LocalDate date);

    @Query("SELECT COALESCE(SUM(r.partySize), 0) FROM Reservation r WHERE r.reservationDate = ?1")
    long sumPartySizeByReservationDate(LocalDate date);

    long countByStatus(String status);

    @Query("SELECT r.status, COUNT(r) FROM Reservation r GROUP BY r.status")
    List<Object[]> countGroupedByStatus();

    List<Reservation> findTop5ByOrderByCreatedAtDesc();

    @Query("SELECT r.reservationTime, COUNT(r) FROM Reservation r GROUP BY r.reservationTime ORDER BY COUNT(r) DESC")
    List<Object[]> findPeakTimeSlots();

    @Query("SELECT COUNT(r) FROM Reservation r WHERE YEAR(r.reservationDate) = ?1 AND MONTH(r.reservationDate) = ?2")
    long countBookingsByYearAndMonth(int year, int month);

    @Query("SELECT r FROM Reservation r WHERE r.tableId = ?1 AND r.reservationDate = ?2 AND r.status NOT IN ('cancelled', 'no_show')")
    List<Reservation> findActiveReservationsForTableAndDate(Long tableId, LocalDate date);
}
