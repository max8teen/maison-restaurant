package com.maison.backend.repository;

import com.maison.backend.entity.Waitlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface WaitlistRepository extends JpaRepository<Waitlist, Long> {

    Optional<Waitlist> findFirstByUserIdAndReservationDateAndReservationTimeAndNotified(
            Long userId, LocalDate date, String time, Integer notified
    );

    Optional<Waitlist> findFirstByUserIdAndReservationDateAndReservationTimeOrderByCreatedAtDesc(
            Long userId, LocalDate date, String time
    );

    Optional<Waitlist> findFirstByReservationDateAndReservationTimeAndNotifiedOrderByCreatedAtAsc(
            LocalDate date, String time, Integer notified
    );

    @Query("SELECT COUNT(w) FROM Waitlist w WHERE w.reservationDate = ?1 AND w.reservationTime = ?2 AND w.notified = 0 AND w.id <= ?3")
    long countPositionInQueue(LocalDate date, String time, Long waitlistId);

    Optional<Waitlist> findByIdAndUserId(Long id, Long userId);

    List<Waitlist> findAllByOrderByCreatedAtDesc();
}
