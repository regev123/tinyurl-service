package com.tinyurl.repository;

import com.tinyurl.entity.UrlMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UrlMappingRepository extends JpaRepository<UrlMapping, Long> {
    
    Optional<UrlMapping> findByShortUrl(String shortUrl);
    
    boolean existsByShortUrl(String shortUrl);
    
    Optional<UrlMapping> findByOriginalUrl(String originalUrl);
}
