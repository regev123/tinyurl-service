package com.tinyurl.stats.repository;

import com.tinyurl.stats.entity.UrlClickEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UrlClickEventRepository extends JpaRepository<UrlClickEvent, Long> {
    
    List<UrlClickEvent> findByShortCodeOrderByClickedAtDesc(String shortCode);
    
    @Query("SELECT COUNT(e) FROM UrlClickEvent e WHERE e.shortCode = :shortCode")
    Long countByShortCode(@Param("shortCode") String shortCode);
    
    @Query("SELECT COUNT(e) FROM UrlClickEvent e WHERE e.shortCode = :shortCode AND e.clickedAt >= :startTime")
    Long countByShortCodeAndClickedAtAfter(@Param("shortCode") String shortCode, @Param("startTime") LocalDateTime startTime);
    
    @Query("SELECT e.country, COUNT(e) as clicks FROM UrlClickEvent e WHERE e.shortCode = :shortCode GROUP BY e.country ORDER BY clicks DESC")
    List<Object[]> findTopCountriesByShortCode(@Param("shortCode") String shortCode);
    
    @Query(value = "SELECT DATE(e.clicked_at) as click_date, COUNT(e) as click_count FROM url_click_events e WHERE e.short_code = :shortCode AND e.clicked_at >= :startTime GROUP BY DATE(e.clicked_at) ORDER BY DATE(e.clicked_at)", nativeQuery = true)
    List<Object[]> findClickTimelineByShortCode(@Param("shortCode") String shortCode, @Param("startTime") LocalDateTime startTime);
}

