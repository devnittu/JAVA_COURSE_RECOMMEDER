package com.recommender.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiService {

    @Value("${gemini.api.key:}")
    private String apiKey;

    private final RestTemplate restTemplate;

    private static final String GEMINI_URL =
    "https://generativelanguage.googleapis.com/v1/models/gemini-1.5-flash-latest:generateContent?key=";

    public boolean isAvailable() {
        return apiKey != null && !apiKey.isBlank();
    }

    // ─────────────────────────────────────────────────────────────
    //  1. AI Chat — answers learning questions + returns search hint
    // ─────────────────────────────────────────────────────────────
    public Map<String, Object> chat(String userMessage) {
        if (!isAvailable()) {
            return Map.of("reply", "AI is not configured yet.", "searchQuery", userMessage);
        }

        String prompt = """
            You are a helpful online course advisor. The user is asking about learning something.
            
            User message: "%s"
            
            Respond in JSON with exactly this structure:
            {
              "reply": "A helpful 2-3 sentence response about what they should learn and why",
              "searchQuery": "the best search query to find courses for this (1-4 words)",
              "category": "one of: Java, Python, Web Dev, AI, Data Science, DevOps, Mobile, DSA, Computer Science",
              "level": "one of: Beginner, Intermediate, Advanced, All Levels",
              "suggestedTopics": ["topic1", "topic2", "topic3"]
            }
            
            Only respond with valid JSON, nothing else.
            """.formatted(userMessage);

        String raw = callGemini(prompt);
        if (raw == null) return Map.of("reply", "Sorry, AI is temporarily unavailable.", "searchQuery", userMessage);

        try {
            return parseJsonResponse(raw);
        } catch (Exception e) {
            log.warn("Could not parse Gemini JSON response: {}", e.getMessage());
            return Map.of("reply", raw, "searchQuery", userMessage);
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  2. Smart Search — enhances user query for better results
    // ─────────────────────────────────────────────────────────────
    public Map<String, String> enhanceSearch(String query) {
        if (!isAvailable()) return Map.of("searchQuery", query, "category", "");

        String prompt = """
            Convert this user search query into a structured course search.
            
            User query: "%s"
            
            Respond ONLY with valid JSON:
            {
              "searchQuery": "optimized search terms (1-5 words)",
              "category": "one of: Java, Python, Web Dev, AI, Data Science, DevOps, Mobile, DSA, Computer Science, or empty string if unclear",
              "level": "one of: Beginner, Intermediate, Advanced, or empty string"
            }
            """.formatted(query);

        String raw = callGemini(prompt);
        if (raw == null) return Map.of("searchQuery", query, "category", "");

        try {
            return parseJsonResponse(raw);
        } catch (Exception e) {
            return Map.of("searchQuery", query, "category", "");
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  3. Course Summary — 1-line AI summary for a course card
    // ─────────────────────────────────────────────────────────────
    public String generateSummary(String title, String platform, Double rating) {
        if (!isAvailable()) return null;

        String prompt = """
            Write a single compelling sentence (max 15 words) describing why someone should take this course.
            Course: "%s" on %s (rating: %s/5)
            Only respond with that one sentence, no quotes, no extra text.
            """.formatted(title, platform, rating != null ? rating : "unknown");

        return callGemini(prompt);
    }

    // ─────────────────────────────────────────────────────────────
    //  4. Learning Path — suggest what to learn next
    // ─────────────────────────────────────────────────────────────
    public Map<String, Object> suggestNextSteps(List<String> savedCategories) {
        if (!isAvailable() || savedCategories == null || savedCategories.isEmpty()) {
            return Map.of("reply", "Save some courses first to get personalized suggestions!", "searchQuery", "programming");
        }

        String prompt = """
            A user has been studying: %s
            
            Suggest what they should learn next to advance their career.
            Respond in JSON:
            {
              "reply": "2-3 sentence personalized learning advice",
              "searchQuery": "best next topic to search for (1-4 words)",
              "nextTopics": ["topic1", "topic2", "topic3"]
            }
            Only respond with valid JSON.
            """.formatted(String.join(", ", savedCategories));

        String raw = callGemini(prompt);
        if (raw == null) return Map.of("reply", "Keep exploring courses!", "searchQuery", "programming");

        try {
            return parseJsonResponse(raw);
        } catch (Exception e) {
            return Map.of("reply", raw, "searchQuery", "programming");
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  Core: Call Gemini REST API
    // ─────────────────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    private String callGemini(String prompt) {
        try {
            log.info("Gemini API key present: {}", apiKey != null && !apiKey.isBlank());
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> part    = Map.of("text", prompt);
            Map<String, Object> content = Map.of("parts", List.of(part));
            Map<String, Object> body    = Map.of("contents", List.of(content));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            String url = GEMINI_URL + apiKey;

            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            if (response.getBody() == null) return null;

            List<Map<String, Object>> candidates =
                (List<Map<String, Object>>) response.getBody().get("candidates");
            if (candidates == null || candidates.isEmpty()) return null;

            Map<String, Object> firstCandidate = candidates.get(0);
            Map<String, Object> responseContent = (Map<String, Object>) firstCandidate.get("content");
            List<Map<String, Object>> parts = (List<Map<String, Object>>) responseContent.get("parts");
            if (parts == null || parts.isEmpty()) return null;

            String text = (String) parts.get(0).get("text");

            // Strip markdown code fences if present
            if (text != null) {
                text = text.strip()
                    .replaceAll("^```json\\s*", "")
                    .replaceAll("^```\\s*", "")
                    .replaceAll("\\s*```$", "")
                    .strip();
            }

            return text;

        } catch (Exception e) {
            log.error("Gemini API call failed", e);
            return null;
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  Parse JSON string into Map
    // ─────────────────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    private <T> T parseJsonResponse(String json) {
        // Use Jackson via RestTemplate's message converter trick
        // Simple approach: use a lightweight parser
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                new com.fasterxml.jackson.databind.ObjectMapper();
            return (T) mapper.readValue(json, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse: " + json, e);
        }
    }
}
