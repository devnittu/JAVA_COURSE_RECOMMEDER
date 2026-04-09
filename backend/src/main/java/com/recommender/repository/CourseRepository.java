package com.recommender.repository;

import com.recommender.model.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    // ─── Existing queries ───
    List<Course> findByCategory(String category);
    List<Course> findByCategoryAndLevel(String category, String level);
    List<Course> findByLevel(String level);

    // ─── New queries for Phase 2 ───
    Optional<Course> findByUrl(String url);
    List<Course> findByPlatform(String platform);
    
    // Pagination queries
    Page<Course> findAll(Pageable pageable);
    Page<Course> findByCategory(String category, Pageable pageable);
    Page<Course> findByPlatform(String platform, Pageable pageable);
    Page<Course> findByLevel(String level, Pageable pageable);
    Page<Course> findByCategoryAndLevel(String category, String level, Pageable pageable);
    
    // Search by title (case-insensitive)
    @Query("SELECT c FROM Course c WHERE LOWER(c.title) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Course> findByTitleContainingIgnoreCase(@Param("query") String query, Pageable pageable);
    
    // Combined filters
    @Query("SELECT c FROM Course c WHERE " +
           "(:category IS NULL OR c.category = :category) AND " +
           "(:platform IS NULL OR c.platform = :platform) AND " +
           "(:level IS NULL OR c.level = :level) AND " +
           "(:minRating IS NULL OR c.rating >= :minRating)")
    Page<Course> findWithFilters(
        @Param("category") String category,
        @Param("platform") String platform,
        @Param("level") String level,
        @Param("minRating") Double minRating,
        Pageable pageable
    );
    
    // Find top rated or by created date
    @Query("SELECT c FROM Course c ORDER BY c.rating DESC")
    Page<Course> findTopRated(Pageable pageable);
    
    @Query("SELECT c FROM Course c ORDER BY c.createdAt DESC")
    Page<Course> findNewest(Pageable pageable);
}
