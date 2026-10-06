package com.maison.backend.repository;

import com.maison.backend.entity.CustomerNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerNoteRepository extends JpaRepository<CustomerNote, Long> {
    Optional<CustomerNote> findByUserId(Long userId);
}
