package com.recommender.controller;

import com.recommender.dto.CourseDTO;
import com.recommender.dto.PaginatedResponse;
import com.recommender.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RecommendationController {

    private final RecommendationService recommendationService;

    // ─────────────────────────────────────────────────────────────
    //  EXISTING ENDPOINTS (Backward Compatible)
    // ─────────────────────────────────────────────────────────────

    /**
     * GET /api/recommend?category=Java&level=Beginner
     * Returns scored and sorted course recommendations (existing).
     */
    @GetMapping("/recommend")
    public ResponseEntity<?> getRecommendations(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String level,
            @RequestParam(required = false, defaultValue = "20") int limit,
            @RequestParam(required = false, defaultValue = "0") int offset) {
        
        // If pagination params are provided, use paginated version
        if (limit > 0) {
            PaginatedResponse<CourseDTO> response = recommendationService.getRecommendationsPage(
                    category, level, limit, offset, "rating");
            return ResponseEntity.ok(response);
        }
        
        // Otherwise, use legacy list version
        List<CourseDTO> recommendations = recommendationService.getRecommendations(category, level);
        return ResponseEntity.ok(recommendations);
    }

    /**
     * GET /api/courses
     * Returns all courses sorted by rating.
     */
    @GetMapping("/courses")
    public ResponseEntity<?> getAllCourses(
            @RequestParam(required = false, defaultValue = "20") int limit,
            @RequestParam(required = false, defaultValue = "0") int offset,
            @RequestParam(required = false) String sortBy) {
        
        // If pagination params, return paginated
        if (limit > 0) {
            PaginatedResponse<CourseDTO> response = recommendationService.getAllCoursesPage(
                    limit, offset, sortBy);
            return ResponseEntity.ok(response);
        }
        
        // Legacy: return all
        return ResponseEntity.ok(recommendationService.getAllCourses());
    }

    /**
     * GET /api/courses/search?q=java spring boot&limit=20&offset=0
     * Smart keyword search — scores by title, category, platform relevance.
     */
    @GetMapping("/courses/search")
    public ResponseEntity<?> searchCourses(
            @RequestParam(required = false, defaultValue = "") String q,
            @RequestParam(required = false, defaultValue = "20") int limit,
            @RequestParam(required = false, defaultValue = "0") int offset,
            @RequestParam(required = false) String sortBy) {
        
        // If pagination params, return paginated
        if (limit > 0) {
            PaginatedResponse<CourseDTO> response = recommendationService.searchCoursesPage(
                    q, limit, offset, sortBy);
            return ResponseEntity.ok(response);
        }
        
        // Legacy: return list
        List<CourseDTO> results = recommendationService.searchCourses(q);
        return ResponseEntity.ok(results);
    }

    /**
     * GET /api/courses/trending?limit=20&offset=0
     * Returns top-rated courses from popular categories for homepage.
     */
    @GetMapping("/courses/trending")
    public ResponseEntity<?> getTrendingCourses(
            @RequestParam(required = false, defaultValue = "20") int limit,
            @RequestParam(required = false, defaultValue = "0") int offset) {
        
        // If pagination params, return paginated
        if (limit > 0) {
            PaginatedResponse<CourseDTO> response = recommendationService.getTrendingCoursesPage(
                    limit, offset);
            return ResponseEntity.ok(response);
        }
        
        // Legacy: return list
        List<CourseDTO> trending = recommendationService.getTrendingCourses();
        return ResponseEntity.ok(trending);
    }

    // ─────────────────────────────────────────────────────────────
    //  NEW ENDPOINTS (Phase 2)
    // ─────────────────────────────────────────────────────────────

    /**
     * GET /api/courses/advanced?category=&platform=&level=&minRating=4.0&sortBy=rating&limit=20&offset=0
     * Advanced search with multiple filters
     */
    @GetMapping("/courses/advanced")
    public ResponseEntity<PaginatedResponse<CourseDTO>> advancedSearch(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false, defaultValue = "20") int limit,
            @RequestParam(required = false, defaultValue = "0") int offset,
            @RequestParam(required = false, defaultValue = "rating") String sortBy) {
        
        PaginatedResponse<CourseDTO> response = recommendationService.advancedSearch(
                category, platform, level, minRating, limit, offset, sortBy);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/health
     * Health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Course Recommender API is running!");
    }
}
