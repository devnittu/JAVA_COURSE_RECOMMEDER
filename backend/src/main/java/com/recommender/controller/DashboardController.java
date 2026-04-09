package com.recommender.controller;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.recommender.config.JwtUtil;
import com.recommender.dto.CourseDTO;
import com.recommender.dto.PaginatedResponse;
import com.recommender.model.SavedCourse;
import com.recommender.repository.CourseRepository;
import com.recommender.repository.SavedCourseRepository;
import com.recommender.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DashboardController {

    private final SavedCourseRepository savedCourseRepository;
    private final CourseRepository courseRepository;
    private final DashboardService dashboardService;
    private final JwtUtil jwtUtil;

    private Long extractUserId(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new JWTVerificationException("No token provided");
        }
        return jwtUtil.getUserId(authHeader.substring(7));
    }

    /**
     * GET /api/dashboard/saved
     * Returns all courses saved by the authenticated user.
     */
    @GetMapping("/saved")
    public ResponseEntity<?> getSavedCourses(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            Long userId = extractUserId(authHeader);
            List<SavedCourse> savedCourses = savedCourseRepository.findByUserId(userId);

            List<CourseDTO> courses = savedCourses.stream()
                    .map(sc -> courseRepository.findById(sc.getCourseId()))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .map(c -> new CourseDTO(c.getId(), c.getTitle(), c.getPlatform(),
                            c.getUrl(), c.getCategory(), c.getLevel(), c.getRating(), 0, c.getThumbnail(),
                            c.getInstructor(), c.getDescription(), c.getDuration(), c.getStudents(), c.getPrice()))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(courses);
        } catch (JWTVerificationException e) {
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Error fetching saved courses: " + e.getMessage()));
        }
    }

    /**
     * POST /api/dashboard/save/{courseId}
     * Saves a course for the authenticated user.
     */
    @PostMapping("/save/{courseId}")
    public ResponseEntity<?> saveCourse(
            @PathVariable Long courseId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            Long userId = extractUserId(authHeader);

            if (savedCourseRepository.existsByUserIdAndCourseId(userId, courseId)) {
                return ResponseEntity.ok(Map.of("message", "Course already saved", "saved", true));
            }

            SavedCourse saved = new SavedCourse();
            saved.setUserId(userId);
            saved.setCourseId(courseId);
            savedCourseRepository.save(saved);

            return ResponseEntity.ok(Map.of("message", "Course saved successfully", "saved", true));
        } catch (JWTVerificationException e) {
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Error saving course: " + e.getMessage()));
        }
    }

    /**
     * DELETE /api/dashboard/unsave/{courseId}
     * Removes a saved course for the authenticated user.
     */
    @DeleteMapping("/unsave/{courseId}")
    public ResponseEntity<?> unsaveCourse(
            @PathVariable Long courseId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            Long userId = extractUserId(authHeader);
            
            if (userId == null || courseId == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Invalid user or course ID"));
            }
            
            savedCourseRepository.deleteByUserIdAndCourseId(userId, courseId);
            return ResponseEntity.ok(Map.of("message", "Course removed from saved", "saved", false));
        } catch (JWTVerificationException e) {
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Error removing course: " + e.getMessage()));
        }
    }

    /**
     * GET /api/dashboard/saved-ids
     * Returns only the IDs of courses saved by the user (for quick UI state).
     */
    @GetMapping("/saved-ids")
    public ResponseEntity<?> getSavedCourseIds(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            Long userId = extractUserId(authHeader);
            List<Long> ids = savedCourseRepository.findByUserId(userId)
                    .stream().map(SavedCourse::getCourseId).collect(Collectors.toList());
            return ResponseEntity.ok(ids);
        } catch (JWTVerificationException e) {
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Error fetching saved course IDs: " + e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  PHASE 2: New Dashboard Endpoints
    // ─────────────────────────────────────────────────────────────

    /**
     * GET /api/dashboard/saved/filtered?category=&platform=&sortBy=&limit=20&offset=0
     * Get saved courses with filtering and pagination
     */
    @GetMapping("/saved/filtered")
    public ResponseEntity<?> getFilteredSavedCourses(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String platform,
            @RequestParam(required = false, defaultValue = "newest") String sortBy,
            @RequestParam(required = false, defaultValue = "20") int limit,
            @RequestParam(required = false, defaultValue = "0") int offset) {
        try {
            Long userId = extractUserId(authHeader);
            PaginatedResponse<CourseDTO> response = dashboardService.getFilteredSavedCourses(
                    userId, category, platform, sortBy, limit, offset);
            return ResponseEntity.ok(response);
        } catch (JWTVerificationException e) {
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Error filtering saved courses: " + e.getMessage()));
        }
    }

    /**
     * GET /api/dashboard/recommendations
     * Get AI-powered course recommendations based on user's saved courses
     */
    @GetMapping("/recommendations")
    public ResponseEntity<?> getRecommendations(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            Long userId = extractUserId(authHeader);
            List<CourseDTO> recommendations = dashboardService.getRecommendationsForUser(userId);
            return ResponseEntity.ok(recommendations);
        } catch (JWTVerificationException e) {
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Error fetching recommendations: " + e.getMessage()));
        }
    }

    /**
     * GET /api/dashboard/stats
     * Get user's dashboard statistics (total saved, categories, platforms, avg rating)
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getStats(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            Long userId = extractUserId(authHeader);
            Map<String, Object> stats = dashboardService.getUserStats(userId);
            return ResponseEntity.ok(stats);
        } catch (JWTVerificationException e) {
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Error fetching stats: " + e.getMessage()));
        }
    }

    /**
     * POST /api/dashboard/clear-all
     * Clear all saved courses for the user
     */
    @PostMapping("/clear-all")
    public ResponseEntity<?> clearAllSaved(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            Long userId = extractUserId(authHeader);
            Map<String, Object> result = dashboardService.clearAllSavedCourses(userId);
            return ResponseEntity.ok(result);
        } catch (JWTVerificationException e) {
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Error clearing saved courses: " + e.getMessage()));
        }
    }
}
