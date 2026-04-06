package com.recommender.controller;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.recommender.config.JwtUtil;
import com.recommender.dto.CourseDTO;

import com.recommender.model.SavedCourse;
import com.recommender.repository.CourseRepository;
import com.recommender.repository.SavedCourseRepository;
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
                            c.getUrl(), c.getCategory(), c.getLevel(), c.getRating(), 0))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(courses);
        } catch (JWTVerificationException e) {
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
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
            savedCourseRepository.deleteByUserIdAndCourseId(userId, courseId);
            return ResponseEntity.ok(Map.of("message", "Course removed from saved", "saved", false));
        } catch (JWTVerificationException e) {
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
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
        }
    }
}
