package com.tinyurl.stats.repository;

import com.tinyurl.stats.entity.UrlStatistics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UrlStatisticsRepository extends JpaRepository<UrlStatistics, Long> {
    
    Optional<UrlStatistics> findByShortCode(String shortCode);
}

