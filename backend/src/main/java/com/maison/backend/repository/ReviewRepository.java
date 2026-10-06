package com.maison.backend.repository;

import com.maison.backend.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByStatusOrderByCreatedAtDesc(String status);
    List<Review> findAllByOrderByCreatedAtDesc();
    Optional<Review> findByBookingIdAndUserId(Long bookingId, Long userId);
}
