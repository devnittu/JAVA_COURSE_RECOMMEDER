package com.recommender.controller;

import com.recommender.dto.CourseDTO;
import com.recommender.service.GeminiService;
import com.recommender.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class AiController {

    private final GeminiService geminiService;
    private final RecommendationService recommendationService;

    /**
     * POST /api/ai/chat
     * Body: { "message": "I want to learn machine learning" }
     * Returns: AI reply + matching course cards
     */
    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(@RequestBody Map<String, String> body) {
        String message = body.getOrDefault("message", "").trim();
        if (message.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "message is required"));
        }

        log.info("AI chat request: '{}'", message);

        // 1. Ask Gemini to understand the request
        Map<String, Object> aiResponse = geminiService.chat(message);

        // 2. Use the AI-extracted search query to find real courses
        String searchQuery = (String) aiResponse.getOrDefault("searchQuery", message);
        String category    = (String) aiResponse.getOrDefault("category", "");

        List<CourseDTO> courses = new ArrayList<>();
        try {
            if (!searchQuery.isBlank()) {
                courses = recommendationService.searchCourses(searchQuery);
            }
            if (courses.isEmpty() && !category.isBlank()) {
                courses = recommendationService.getRecommendations(category, "");
            }
        } catch (Exception e) {
            log.warn("Course fetch failed for AI chat: {}", e.getMessage());
        }

        // Limit to top 6 cards
        List<CourseDTO> topCourses = courses.stream().limit(6).toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("reply",          aiResponse.getOrDefault("reply", "Here are some courses I found:"));
        result.put("suggestedTopics",aiResponse.getOrDefault("suggestedTopics", List.of()));
        result.put("searchQuery",    searchQuery);
        result.put("courses",        topCourses);
        result.put("aiEnabled",      geminiService.isAvailable());

        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/ai/recommend
     * Body: { "savedCategories": ["Java", "Python"] }
     * Returns: personalized next-step suggestion + courses
     */
    @PostMapping("/recommend")
    public ResponseEntity<Map<String, Object>> recommend(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> savedCategories = (List<String>) body.getOrDefault("savedCategories", List.of());

        log.info("AI personalized recommend for categories: {}", savedCategories);

        Map<String, Object> aiResponse = geminiService.suggestNextSteps(savedCategories);
        String searchQuery = (String) aiResponse.getOrDefault("searchQuery", "programming");

        List<CourseDTO> courses = new ArrayList<>();
        try {
            courses = recommendationService.searchCourses(searchQuery);
        } catch (Exception e) {
            log.warn("Course fetch failed for AI recommend: {}", e.getMessage());
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("reply",       aiResponse.getOrDefault("reply", "Here's what to study next:"));
        result.put("nextTopics",  aiResponse.getOrDefault("nextTopics", List.of()));
        result.put("courses",     courses.stream().limit(6).toList());
        result.put("aiEnabled",   geminiService.isAvailable());

        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/ai/status
     * Returns whether AI is configured
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        return ResponseEntity.ok(Map.of("aiEnabled", geminiService.isAvailable()));
    }
}
