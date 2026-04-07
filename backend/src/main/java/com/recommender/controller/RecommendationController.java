package com.recommender.controller;

import com.recommender.dto.CourseDTO;
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

    /**
     * GET /api/recommend?category=Java&level=Beginner
     * Returns scored and sorted course recommendations (existing).
     */
    @GetMapping("/recommend")
    public ResponseEntity<List<CourseDTO>> getRecommendations(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String level) {
        List<CourseDTO> recommendations = recommendationService.getRecommendations(category, level);
        return ResponseEntity.ok(recommendations);
    }

    /**
     * GET /api/courses
     * Returns all courses sorted by rating.
     */
    @GetMapping("/courses")
    public ResponseEntity<List<CourseDTO>> getAllCourses() {
        return ResponseEntity.ok(recommendationService.getAllCourses());
    }

    /**
     * GET /api/courses/search?q=java spring boot
     * Smart keyword search — scores by title, category, platform relevance.
     */
    @GetMapping("/courses/search")
    public ResponseEntity<List<CourseDTO>> searchCourses(
            @RequestParam(required = false, defaultValue = "") String q) {
        List<CourseDTO> results = recommendationService.searchCourses(q);
        return ResponseEntity.ok(results);
    }

    /**
     * GET /api/courses/trending
     * Returns top-rated courses from popular categories for homepage auto-load.
     */
    @GetMapping("/courses/trending")
    public ResponseEntity<List<CourseDTO>> getTrendingCourses() {
        return ResponseEntity.ok(recommendationService.getTrendingCourses());
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
