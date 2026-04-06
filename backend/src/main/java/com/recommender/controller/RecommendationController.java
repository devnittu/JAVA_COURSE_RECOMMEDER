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
     * Returns scored and sorted course recommendations.
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
     * Returns all courses without scoring.
     */
    @GetMapping("/courses")
    public ResponseEntity<List<CourseDTO>> getAllCourses() {
        return ResponseEntity.ok(recommendationService.getAllCourses());
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
