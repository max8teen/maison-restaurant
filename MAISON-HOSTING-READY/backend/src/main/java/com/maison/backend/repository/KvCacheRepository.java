package com.maison.backend.repository;

import com.maison.backend.entity.KvCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KvCacheRepository extends JpaRepository<KvCache, String> {
}
