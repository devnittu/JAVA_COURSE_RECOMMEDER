package com.recommender.service;

import com.recommender.dto.CourseDTO;
import com.recommender.model.Course;
import com.recommender.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {

    private final CourseRepository courseRepository;

    // Categories shown on the homepage trending carousel (by priority)
    private static final List<String> TRENDING_CATEGORIES = List.of(
            "Web Dev", "AI", "Python", "Java", "Data Science"
    );

    // ─────────────────────────────────────────────────────────────
    //  Filtered Recommendations (existing endpoint /api/recommend)
    // ─────────────────────────────────────────────────────────────
    public List<CourseDTO> getRecommendations(String category, String level) {
        log.debug("Getting recommendations for category={}, level={}", category, level);

        List<Course> allCourses = courseRepository.findAll();

        List<CourseDTO> scored = allCourses.stream()
                .map(course -> {
                    int score = 0;

                    if (category != null && !category.isBlank()
                            && course.getCategory().equalsIgnoreCase(category)) {
                        score += 10;
                    }

                    if (level != null && !level.isBlank()
                            && course.getLevel().equalsIgnoreCase(level)) {
                        score += 5;
                    }

                    if (course.getRating() != null && course.getRating() > 4.0) {
                        score += 3;
                    }

                    return toDTO(course, score);
                })
                .filter(dto -> dto.getScore() > 0)
                .sorted(Comparator.comparingInt(CourseDTO::getScore).reversed())
                .collect(Collectors.toList());

        log.debug("Returning {} recommendations", scored.size());
        return scored;
    }

    // ─────────────────────────────────────────────────────────────
    //  Smart Keyword Search — /api/courses/search?q=...
    // ─────────────────────────────────────────────────────────────
    public List<CourseDTO> searchCourses(String query) {
        if (query == null || query.isBlank()) {
            return getTrendingCourses();
        }

        String q = query.trim().toLowerCase();
        String[] terms = q.split("\\s+");

        log.debug("Searching courses for query='{}'", query);

        List<Course> allCourses = courseRepository.findAll();

        List<CourseDTO> results = allCourses.stream()
                .map(course -> {
                    int score = computeSearchScore(course, terms);
                    return toDTO(course, score);
                })
                .filter(dto -> dto.getScore() > 0)
                .sorted(Comparator.comparingInt(CourseDTO::getScore).reversed())
                .collect(Collectors.toList());

        // If no keyword match, fall back to returning all courses sorted by rating
        if (results.isEmpty()) {
            log.debug("No keyword match for '{}', returning top-rated courses", query);
            results = allCourses.stream()
                    .sorted(Comparator.comparingDouble(
                            c -> c.getRating() != null ? -c.getRating() : 0))
                    .map(c -> toDTO(c, 0))
                    .limit(12)
                    .collect(Collectors.toList());
        }

        log.debug("Search returned {} results for '{}'", results.size(), query);
        return results;
    }

    // ─────────────────────────────────────────────────────────────
    //  Trending Courses — /api/courses/trending
    //  Returns top-rated courses from each trending category
    // ─────────────────────────────────────────────────────────────
    public List<CourseDTO> getTrendingCourses() {
        log.debug("Fetching trending courses");

        List<Course> allCourses = courseRepository.findAll();

        // Build a pool: top 2 from each trending category, sorted by rating desc
        List<CourseDTO> trending = new ArrayList<>();

        for (String cat : TRENDING_CATEGORIES) {
            allCourses.stream()
                    .filter(c -> cat.equalsIgnoreCase(c.getCategory()))
                    .sorted(Comparator.comparingDouble(
                            c -> c.getRating() != null ? -c.getRating() : 0))
                    .limit(2)
                    .map(c -> toDTO(c, computeTrendingScore(c)))
                    .forEach(trending::add);
        }

        // Fill with any remaining high-rated courses not yet included (up to 12 total)
        Set<Long> included = trending.stream()
                .map(CourseDTO::getId)
                .collect(Collectors.toSet());

        allCourses.stream()
                .filter(c -> !included.contains(c.getId()))
                .sorted(Comparator.comparingDouble(
                        c -> c.getRating() != null ? -c.getRating() : 0))
                .limit(Math.max(0, 12 - trending.size()))
                .map(c -> toDTO(c, computeTrendingScore(c)))
                .forEach(trending::add);

        log.debug("Returning {} trending courses", trending.size());
        return trending;
    }

    // ─────────────────────────────────────────────────────────────
    //  All Courses — /api/courses
    // ─────────────────────────────────────────────────────────────
    public List<CourseDTO> getAllCourses() {
        return courseRepository.findAll().stream()
                .sorted(Comparator.comparingDouble(
                        c -> c.getRating() != null ? -c.getRating() : 0))
                .map(c -> toDTO(c, 0))
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────────────────────

    /** Score a course against a list of search terms */
    private int computeSearchScore(Course course, String[] terms) {
        int score = 0;
        String title    = course.getTitle()    != null ? course.getTitle().toLowerCase()    : "";
        String category = course.getCategory() != null ? course.getCategory().toLowerCase() : "";
        String platform = course.getPlatform() != null ? course.getPlatform().toLowerCase() : "";
        String level    = course.getLevel()    != null ? course.getLevel().toLowerCase()    : "";

        for (String term : terms) {
            if (title.contains(term))    score += 8;
            if (category.contains(term)) score += 6;
            if (platform.contains(term)) score += 2;
            if (level.contains(term))    score += 2;
        }

        // Boost for high rating
        if (course.getRating() != null) {
            if (course.getRating() >= 4.8) score += 5;
            else if (course.getRating() >= 4.5) score += 3;
            else if (course.getRating() >= 4.0) score += 1;
        }

        return score;
    }

    /** Trending score based purely on rating */
    private int computeTrendingScore(Course course) {
        if (course.getRating() == null) return 0;
        return (int) Math.round(course.getRating() * 2);
    }

    private CourseDTO toDTO(Course course, int score) {
        return new CourseDTO(
                course.getId(),
                course.getTitle(),
                course.getPlatform(),
                course.getUrl(),
                course.getCategory(),
                course.getLevel(),
                course.getRating(),
                score,
                course.getThumbnail()
        );
    }
}
