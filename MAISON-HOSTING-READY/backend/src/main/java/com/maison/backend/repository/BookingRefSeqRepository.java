package com.maison.backend.repository;

import com.maison.backend.entity.BookingRefSeq;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingRefSeqRepository extends JpaRepository<BookingRefSeq, Long> {
}
