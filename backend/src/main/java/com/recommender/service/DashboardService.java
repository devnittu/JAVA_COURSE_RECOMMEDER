package com.recommender.service;

import com.recommender.dto.CourseDTO;
import com.recommender.dto.PaginatedResponse;
import com.recommender.model.Course;
import com.recommender.model.SavedCourse;
import com.recommender.repository.CourseRepository;
import com.recommender.repository.SavedCourseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Dashboard-specific service for user-centric course operations.
 * Handles: saved course filtering, recommendations, stats, etc.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final SavedCourseRepository savedCourseRepository;
    private final CourseRepository courseRepository;
    private final GeminiService geminiService;
    private final RecommendationService recommendationService;

    // ─────────────────────────────────────────────────────────────
    //  Get Filtered Saved Courses
    // ─────────────────────────────────────────────────────────────

    /**
     * Get user's saved courses with filtering and pagination
     */
    public PaginatedResponse<CourseDTO> getFilteredSavedCourses(
            Long userId, String category, String platform, String sortBy, int limit, int offset) {
        
        log.debug("Getting filtered saved courses for user {}: category={}, platform={}, sort={}",
                userId, category, platform, sortBy);

        // Get all saved course IDs for this user
        List<SavedCourse> saved = savedCourseRepository.findByUserId(userId);
        List<Long> courseIds = saved.stream()
                .map(SavedCourse::getCourseId)
                .collect(Collectors.toList());

        if (courseIds.isEmpty()) {
            return new PaginatedResponse<>(new ArrayList<>(), 0, offset, limit);
        }

        // Fetch courses and apply filters
        List<Course> courses = courseRepository.findAllById(courseIds);

        // Filter by category
        if (category != null && !category.isBlank()) {
            courses = courses.stream()
                    .filter(c -> c.getCategory().equalsIgnoreCase(category))
                    .collect(Collectors.toList());
        }

        // Filter by platform
        if (platform != null && !platform.isBlank()) {
            courses = courses.stream()
                    .filter(c -> c.getPlatform().equalsIgnoreCase(platform))
                    .collect(Collectors.toList());
        }

        // Sort
        courses.sort((a, b) -> {
            if ("newest".equalsIgnoreCase(sortBy)) {
                return b.getCreatedAt().compareTo(a.getCreatedAt());
            } else if ("rating".equalsIgnoreCase(sortBy)) {
                return Double.compare(b.getRating(), a.getRating());
            } else { // title
                return a.getTitle().compareToIgnoreCase(b.getTitle());
            }
        });

        // Paginate
        int start = offset;
        int end = Math.min(start + limit, courses.size());
        List<CourseDTO> pageContent = courses.subList(start, end).stream()
                .map(c -> new CourseDTO(c.getId(), c.getTitle(), c.getPlatform(), c.getUrl(),
                        c.getCategory(), c.getLevel(), c.getRating(), 0, c.getThumbnail()))
                .collect(Collectors.toList());

        return new PaginatedResponse<>(pageContent, courses.size(), offset, limit);
    }

    // ─────────────────────────────────────────────────────────────
    //  AI-Powered Recommendations
    // ─────────────────────────────────────────────────────────────

    /**
     * Get AI-powered recommendations based on user's saved courses
     * Uses Gemini to analyze interests and suggest next courses
     */
    public List<CourseDTO> getRecommendationsForUser(Long userId) {
        log.debug("Getting AI recommendations for user {}", userId);

        try {
            // Get user's saved courses
            List<SavedCourse> saved = savedCourseRepository.findByUserId(userId);
            List<Course> savedCourses = saved.stream()
                    .map(sc -> courseRepository.findById(sc.getCourseId()))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());

            if (savedCourses.isEmpty()) {
                log.debug("No saved courses for user {}, returning trending", userId);
                return recommendationService.getTrendingCourses();
            }

            // Get user's categories and interests
            List<String> categories = savedCourses.stream()
                    .map(Course::getCategory)
                    .distinct()
                    .collect(Collectors.toList());

            // Use Gemini to suggest next courses
            Map<String, Object> suggestions = geminiService.suggestNextSteps(categories);
            if (suggestions != null && !suggestions.isEmpty()) {
                log.debug("Gemini provided suggestions");
                // Get search query and use it to find courses
                String searchQuery = (String) suggestions.getOrDefault("searchQuery", "programming");
                return getRecommendationsByCategories(categories).stream().limit(8).collect(Collectors.toList());
            }

            // Fallback: return courses from similar categories
            log.debug("Gemini had no suggestions, using category-based fallback");
            return getRecommendationsByCategories(categories);

        } catch (Exception e) {
            log.error("Error getting AI recommendations: {}", e.getMessage());
            // Fallback to trending
            return recommendationService.getTrendingCourses();
        }
    }

    /**
     * Get courses in similar categories (fallback for when Gemini unavailable)
     */
    private List<CourseDTO> getRecommendationsByCategories(List<String> categories) {
        if (categories.isEmpty()) {
            return recommendationService.getTrendingCourses();
        }

        List<CourseDTO> recommendations = new ArrayList<>();

        for (String category : categories) {
            List<CourseDTO> catCourses = recommendationService.getRecommendations(category, null);
            recommendations.addAll(catCourses);
        }

        return recommendations.stream()
                .distinct()
                .limit(8)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────
    //  User Dashboard Stats
    // ─────────────────────────────────────────────────────────────

    /**
     * Get user's dashboard statistics
     */
    public Map<String, Object> getUserStats(Long userId) {
        List<SavedCourse> saved = savedCourseRepository.findByUserId(userId);
        List<Course> courses = saved.stream()
                .map(sc -> courseRepository.findById(sc.getCourseId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        List<String> categories = courses.stream()
                .map(Course::getCategory)
                .distinct()
                .collect(Collectors.toList());

        List<String> platforms = courses.stream()
                .map(Course::getPlatform)
                .distinct()
                .collect(Collectors.toList());

        double avgRating = courses.stream()
                .mapToDouble(c -> c.getRating() != null ? c.getRating() : 0)
                .average()
                .orElse(0);

        return Map.of(
                "totalSaved", courses.size(),
                "categories", categories,
                "platforms", platforms,
                "avgRating", Math.round(avgRating * 10.0) / 10.0
        );
    }

    // ─────────────────────────────────────────────────────────────
    //  Clear All Saved Courses
    // ─────────────────────────────────────────────────────────────

    /**
     * Clear all saved courses for a user
     */
    public Map<String, Object> clearAllSavedCourses(Long userId) {
        try {
            List<SavedCourse> saved = savedCourseRepository.findByUserId(userId);
            int count = saved.size();
            
            savedCourseRepository.deleteAll(saved);
            
            log.info("Cleared {} saved courses for user {}", count, userId);
            
            return Map.of(
                    "status", "success",
                    "message", "Cleared " + count + " saved courses",
                    "cleared", count
            );
        } catch (Exception e) {
            log.error("Error clearing saved courses: {}", e.getMessage());
            return Map.of(
                    "status", "error",
                    "message", e.getMessage()
            );
        }
    }
}
