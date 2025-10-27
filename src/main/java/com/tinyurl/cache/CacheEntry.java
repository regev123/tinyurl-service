package com.tinyurl.cache;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class CacheEntry<V> {
    private final V value;
    private final LocalDateTime expiryTime;
    
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryTime);
    }
}
