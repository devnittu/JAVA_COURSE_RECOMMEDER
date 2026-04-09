package com.recommender.service;

import com.recommender.dto.CourseDTO;
import com.recommender.dto.PaginatedResponse;
import com.recommender.model.Course;
import com.recommender.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {

    private final CourseRepository courseRepository;
    private final RestTemplate restTemplate;

    @Value("${youtube.api.key:}")
    private String youtubeApiKey;

    // Categories shown on the homepage trending carousel (by priority)
    private static final List<String> TRENDING_CATEGORIES = List.of(
            "Web Dev", "AI", "Python", "Java", "Data Science"
    );

    // ─────────────────────────────────────────────────────────────
    //  Filtered Recommendations — /api/recommend?category=Java
    //  FIX: strictly filter by category first, THEN score within it
    // ─────────────────────────────────────────────────────────────
    public List<CourseDTO> getRecommendations(String category, String level) {
        log.debug("Getting recommendations for category={}, level={}", category, level);

        List<Course> allCourses = courseRepository.findAll();

        // ── STRICT category filter FIRST (was bug: scoring all courses) ──
        if (category != null && !category.isBlank()) {
            allCourses = allCourses.stream()
                    .filter(c -> c.getCategory().equalsIgnoreCase(category))
                    .collect(Collectors.toList());
        }

        List<CourseDTO> scored = allCourses.stream()
                .map(course -> {
                    int score = 0;

                    if (category != null && !category.isBlank()) score += 10; // all passed the filter

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
                .sorted(Comparator.comparingInt(CourseDTO::getScore).reversed()
                        .thenComparingDouble(d -> d.getRating() != null ? -d.getRating() : 0))
                .collect(Collectors.toList());

        // If DB has no results for this category, try APIs fallback
        if (scored.isEmpty() && category != null && !category.isBlank()) {
            log.debug("No DB results for category '{}', fetching from APIs", category);
            List<CourseDTO> fallback = new ArrayList<>();
            fallback.addAll(searchYouTube(category + " programming course"));
            return fallback;
        }

        log.debug("Returning {} recommendations", scored.size());
        return scored;
    }

    // ─────────────────────────────────────────────────────────────
    //  Smart Keyword Search — /api/courses/search?q=...
    //  Merges DB results + live YouTube results
    // ─────────────────────────────────────────────────────────────
    public List<CourseDTO> searchCourses(String query) {
        if (query == null || query.isBlank()) {
            return getTrendingCourses();
        }

        String q = query.trim().toLowerCase();
        String[] terms = q.split("\\s+");

        log.debug("Searching courses for query='{}'", query);

        List<CourseDTO> results = new ArrayList<>();

        // ── 1. Query the database ──
        List<Course> allCourses = courseRepository.findAll();
        List<CourseDTO> dbResults = allCourses.stream()
                .map(course -> {
                    int score = computeSearchScore(course, terms);
                    return toDTO(course, score);
                })
                .filter(dto -> dto.getScore() > 0)
                .sorted(Comparator.comparingInt(CourseDTO::getScore).reversed())
                .collect(Collectors.toList());

        results.addAll(dbResults);

        // ── 2. Fetch live from YouTube ──
        List<CourseDTO> apiResults = new ArrayList<>();
        apiResults.addAll(searchYouTube(query + " course tutorial"));
        
        // Merge: skip API results whose title closely matches a DB result
        Set<String> dbTitlesLower = dbResults.stream()
                .map(d -> d.getTitle().toLowerCase())
                .collect(Collectors.toSet());

        for (CourseDTO apiRes : apiResults) {
            boolean duplicate = dbTitlesLower.stream()
                    .anyMatch(t -> similarity(t, apiRes.getTitle().toLowerCase()) > 0.7);
            if (!duplicate) results.add(apiRes);
        }

        // If still nothing, return top-rated DB courses
        if (results.isEmpty()) {
            log.debug("No results for '{}', returning top-rated fallback", query);
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
    // ─────────────────────────────────────────────────────────────
    public List<CourseDTO> getTrendingCourses() {
        log.debug("Fetching trending courses");

        List<Course> allCourses = courseRepository.findAll();

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

        // If DB is nearly empty, supplement with API trending
        if (trending.size() < 6) {
            log.debug("Few DB trending courses, supplementing with YouTube API");
            List<CourseDTO> xtraTrending = new ArrayList<>();
            xtraTrending.addAll(searchYouTube("programming course 2024"));
            
            xtraTrending.stream()
                    .limit(6 - trending.size())
                    .forEach(trending::add);
        }

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
    //  YouTube Data API v3 — live course fetching
    // ─────────────────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    public List<CourseDTO> searchYouTube(String query) {
        if (youtubeApiKey == null || youtubeApiKey.isBlank()) {
            log.debug("YouTube API key not configured, skipping live fetch");
            return Collections.emptyList();
        }

        try {
            String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = "https://www.googleapis.com/youtube/v3/search"
                    + "?part=snippet"
                    + "&q=" + encoded
                    + "&type=video"
                    + "&maxResults=12"
                    + "&relevanceLanguage=en"
                    + "&videoDuration=long"
                    + "&key=" + youtubeApiKey;

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response == null || !response.containsKey("items")) return Collections.emptyList();

            List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");
            List<CourseDTO> ytCourses = new ArrayList<>();

            for (Map<String, Object> item : items) {
                try {
                    Map<String, Object> idObj      = (Map<String, Object>) item.get("id");
                    Map<String, Object> snippet    = (Map<String, Object>) item.get("snippet");
                    Map<String, Object> thumbnails = (Map<String, Object>) snippet.get("thumbnails");
                    Map<String, Object> highThumb  = (Map<String, Object>) thumbnails.get("high");

                    String videoId    = (String) idObj.get("videoId");
                    String title      = (String) snippet.get("title");
                    String thumbUrl   = highThumb != null ? (String) highThumb.get("url") : null;

                    if (videoId == null || title == null) continue;

                    // Detect category from query
                    String category = detectCategory(query);

                    CourseDTO dto = new CourseDTO();
                    dto.setId(Math.abs((long) videoId.hashCode()));
                    dto.setTitle(cleanTitle(title));
                    dto.setPlatform("YouTube");
                    dto.setUrl("https://www.youtube.com/watch?v=" + videoId);
                    dto.setCategory(category);
                    dto.setLevel("All Levels");
                    dto.setRating(null);
                    dto.setScore(8); // moderate score for YouTube results
                    dto.setThumbnail(thumbUrl);

                    ytCourses.add(dto);
                } catch (Exception e) {
                    log.warn("Error parsing YouTube item: {}", e.getMessage());
                }
            }

            log.debug("YouTube returned {} results for '{}'", ytCourses.size(), query);
            return ytCourses;

        } catch (Exception e) {
            log.error("YouTube API call failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────────────────────

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

        if (course.getRating() != null) {
            if (course.getRating() >= 4.8) score += 5;
            else if (course.getRating() >= 4.5) score += 3;
            else if (course.getRating() >= 4.0) score += 1;
        }

        return score;
    }

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

    /** Detect likely course category from the search query string */
    private String detectCategory(String query) {
        String q = query.toLowerCase();
        if (q.contains("java"))           return "Java";
        if (q.contains("python"))         return "Python";
        if (q.contains("react") || q.contains("javascript") || q.contains("web")) return "Web Dev";
        if (q.contains("machine learning") || q.contains("ai") || q.contains("deep learning")) return "AI";
        if (q.contains("data science") || q.contains("pandas") || q.contains("sql")) return "Data Science";
        if (q.contains("docker") || q.contains("kubernetes") || q.contains("devops")) return "DevOps";
        if (q.contains("flutter") || q.contains("android") || q.contains("ios") || q.contains("mobile")) return "Mobile";
        if (q.contains("algorithm") || q.contains("dsa") || q.contains("leetcode")) return "DSA";
        if (q.contains("computer science") || q.contains("cs50")) return "Computer Science";
        return "General";
    }

    /** Strip HTML entities and emoji from YouTube titles */
    private String cleanTitle(String title) {
        return title.replaceAll("&amp;", "&")
                    .replaceAll("&lt;", "<")
                    .replaceAll("&gt;", ">")
                    .replaceAll("&#39;", "'")
                    .replaceAll("&quot;", "\"")
                    .trim();
    }

    /** Simple title similarity check to avoid duplicates (Jaccard on words) */
    private double similarity(String a, String b) {
        Set<String> wordsA = new HashSet<>(Arrays.asList(a.split("\\s+")));
        Set<String> wordsB = new HashSet<>(Arrays.asList(b.split("\\s+")));
        if (wordsA.isEmpty() || wordsB.isEmpty()) return 0;
        Set<String> intersection = new HashSet<>(wordsA);
        intersection.retainAll(wordsB);
        Set<String> union = new HashSet<>(wordsA);
        union.addAll(wordsB);
        return (double) intersection.size() / union.size();
    }

    // ─────────────────────────────────────────────────────────────
    //  PHASE 2: Pagination Methods (NEW)
    // ─────────────────────────────────────────────────────────────

    /**
     * GET /api/recommend?category=Java&level=Beginner&limit=20&offset=0
     * Paginated recommendations with category + level filtering
     */
    public PaginatedResponse<CourseDTO> getRecommendationsPage(
            String category, String level, int limit, int offset, String sortBy) {
        log.debug("Getting paginated recommendations: category={}, level={}, limit={}, offset={}, sort={}",
                category, level, limit, offset, sortBy);

        Pageable pageable = createPageable(offset, limit, sortBy != null ? sortBy : "rating");
        Page<Course> page;

        if (category != null && !category.isBlank() && level != null && !level.isBlank()) {
            page = courseRepository.findByCategoryAndLevel(category, level, pageable);
        } else if (category != null && !category.isBlank()) {
            page = courseRepository.findByCategory(category, pageable);
        } else if (level != null && !level.isBlank()) {
            page = courseRepository.findByLevel(level, pageable);
        } else {
            page = courseRepository.findTopRated(pageable);
        }

        List<CourseDTO> dtos = page.getContent().stream()
                .map(c -> toDTO(c, 0))
                .collect(Collectors.toList());

        return new PaginatedResponse<>(dtos, (int) page.getTotalElements(), offset, limit);
    }

    /**
     * GET /api/courses?limit=20&offset=0&sortBy=rating
     * Paginated all courses with sorting
     */
    public PaginatedResponse<CourseDTO> getAllCoursesPage(int limit, int offset, String sortBy) {
        log.debug("Getting all courses paginated: limit={}, offset={}, sort={}", limit, offset, sortBy);

        Pageable pageable = createPageable(offset, limit, sortBy != null ? sortBy : "rating");
        Page<Course> page;

        if ("newest".equals(sortBy)) {
            page = courseRepository.findNewest(pageable);
        } else {
            page = courseRepository.findTopRated(pageable);
        }

        List<CourseDTO> dtos = page.getContent().stream()
                .map(c -> toDTO(c, 0))
                .collect(Collectors.toList());

        return new PaginatedResponse<>(dtos, (int) page.getTotalElements(), offset, limit);
    }

    /**
     * GET /api/courses/search?q=java&limit=20&offset=0&sortBy=relevance
     * Paginated search with sorting
     */
    public PaginatedResponse<CourseDTO> searchCoursesPage(String query, int limit, int offset, String sortBy) {
        log.debug("Searching paginated: q={}, limit={}, offset={}, sort={}", query, limit, offset, sortBy);

        if (query == null || query.isBlank()) {
            return getTrendingCoursesPage(limit, offset);
        }

        Pageable pageable = createPageable(offset, limit, sortBy != null ? sortBy : "relevance");
        Page<Course> page = courseRepository.findByTitleContainingIgnoreCase(query, pageable);

        List<CourseDTO> dtos = page.getContent().stream()
                .map(c -> {
                    int score = computeSearchScore(c, query.split("\\s+"));
                    return toDTO(c, score);
                })
                .collect(Collectors.toList());

        return new PaginatedResponse<>(dtos, (int) page.getTotalElements(), offset, limit);
    }

    /**
     * GET /api/courses/trending?limit=20&offset=0
     * Paginated trending courses
     */
    public PaginatedResponse<CourseDTO> getTrendingCoursesPage(int limit, int offset) {
        log.debug("Getting trending paginated: limit={}, offset={}", limit, offset);

        Pageable pageable = PageRequest.of(offset / limit, limit, Sort.by(Sort.Direction.DESC, "rating"));
        Page<Course> page = courseRepository.findTopRated(pageable);

        List<CourseDTO> dtos = page.getContent().stream()
                .map(c -> toDTO(c, computeTrendingScore(c)))
                .collect(Collectors.toList());

        return new PaginatedResponse<>(dtos, (int) page.getTotalElements(), offset, limit);
    }

    /**
     * GET /api/courses/advanced?category=&platform=&level=&minRating=4.0&sortBy=rating&limit=20&offset=0
     * Advanced search with multiple filters
     */
    public PaginatedResponse<CourseDTO> advancedSearch(
            String category, String platform, String level, Double minRating,
            int limit, int offset, String sortBy) {
        log.debug("Advanced search: category={}, platform={}, level={}, minRating={}, sort={}",
                category, platform, level, minRating, sortBy);

        Pageable pageable = createPageable(offset, limit, sortBy != null ? sortBy : "rating");
        Page<Course> page = courseRepository.findWithFilters(category, platform, level, minRating, pageable);

        List<CourseDTO> dtos = page.getContent().stream()
                .map(c -> toDTO(c, 0))
                .collect(Collectors.toList());

        return new PaginatedResponse<>(dtos, (int) page.getTotalElements(), offset, limit);
    }

    // ─────────────────────────────────────────────────────────────
    //  Helper: Create Pageable with sorting
    // ─────────────────────────────────────────────────────────────
    private Pageable createPageable(int offset, int limit, String sortBy) {
        Sort sort;

        switch (sortBy.toLowerCase()) {
            case "newest":
                sort = Sort.by(Sort.Direction.DESC, "createdAt");
                break;
            case "rating":
            case "relevance":
                sort = Sort.by(Sort.Direction.DESC, "rating");
                break;
            case "title":
                sort = Sort.by(Sort.Direction.ASC, "title");
                break;
            case "trending":
                sort = Sort.by(Sort.Direction.DESC, "rating", "createdAt");
                break;
            default:
                sort = Sort.by(Sort.Direction.DESC, "rating");
        }

        return PageRequest.of(offset / limit, limit, sort);
    }
}
