package com.maison.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "kv_cache")
public class KvCache {

    @Id
    @Column(name = "cache_key", nullable = false, length = 100)
    private String cacheKey;

    @Column(columnDefinition = "MEDIUMTEXT", nullable = false)
    private String value;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public KvCache() {}

    public KvCache(String cacheKey, String value) {
        this.cacheKey = cacheKey;
        this.value = value;
        this.updatedAt = LocalDateTime.now();
    }

    public String getCacheKey() { return cacheKey; }
    public void setCacheKey(String cacheKey) { this.cacheKey = cacheKey; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
