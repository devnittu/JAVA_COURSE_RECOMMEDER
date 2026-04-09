package com.recommender.controller;

import com.recommender.service.CourseSeederService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Admin-only endpoint for database seeding.
 * POST /api/admin/seed?coursesPerPlatform=200
 * 
 * Populates database with 1000+ courses from multiple platforms.
 * Works once (idempotent due to URL deduplication).
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class AdminController {

    private final CourseSeederService courseSeederService;

    /**
     * POST /api/admin/seed?coursesPerPlatform=200
     * 
     * Seed database with multi-platform courses
     * 
     * Params:
     *   - coursesPerPlatform: Number of courses per platform (default 200)
     * 
     * Response:
     * {
     *   "status": "success",
     *   "totalAdded": 1047,
     *   "totalInDb": 1047,
     *   "breakdown": {
     *     "youtube": 400,
     *     "coursera": 250,
     *     "edx": 150,
     *     "udacity": 80,
     *     "freecodecamp": 80,
     *     "pluralsight": 40
     *   },
     *   "timestamp": "2026-04-09T10:30:00"
     * }
     */
    @PostMapping("/seed")
    public ResponseEntity<Map<String, Object>> seedDatabase(
            @RequestParam(defaultValue = "200") int coursesPerPlatform) {
        
        log.info("🌱 Starting database seeding with {} courses per platform", coursesPerPlatform);
        
        Map<String, Object> result = courseSeederService.seedAllPlatforms(coursesPerPlatform);
        
        if ("success".equals(result.get("status"))) {
            log.info("✅ Seeding completed successfully");
            return ResponseEntity.ok(result);
        } else {
            log.error("❌ Seeding failed: {}", result.get("message"));
            return ResponseEntity.status(500).body(result);
        }
    }
}
