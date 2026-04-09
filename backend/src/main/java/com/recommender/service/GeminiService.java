package com.recommender.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiService {

    @Value("${gemini.api.key:}")
    private String apiKey;

    private final RestTemplate restTemplate;

    private static final String GEMINI_URL =
        "https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash:generateContent?key=";

    // ─── Rate Limiting (Token Bucket: 10 requests per 60 seconds) ───
    private static final int RATE_LIMIT_REQUESTS = 10;
    private static final long RATE_LIMIT_WINDOW_MS = 60_000;  // 60 seconds
    private static final long REQUEST_TIMEOUT_MS = 15_000;    // 15 seconds
    private static final int MAX_PROMPT_LENGTH = 2000;
    
    private final AtomicInteger requestCount = new AtomicInteger(0);
    private final AtomicLong windowStartTime = new AtomicLong(System.currentTimeMillis());

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
        
        // Null safety: validate inputs
        if (title == null || title.isBlank()) return null;
        if (platform == null || platform.isBlank()) platform = "online";

        String prompt = """         Write a single compelling sentence (max 15 words) describing why someone should take this course.
            Course: "%s" on %s (rating: %s/5)
            Only respond with that one sentence, no quotes, no extra text.
            """.formatted(title.trim(), platform.trim(), rating != null ? String.format("%.1f", rating) : "unknown");

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
    //  Core: Call Gemini REST API with rate limiting & error handling
    // ─────────────────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    private String callGemini(String prompt) {
        try {
            // ─── Rate Limiting (Token Bucket) ───
            if (!checkRateLimit()) {
                log.warn("Gemini rate limit exceeded: {} requests in {} ms", requestCount.get(), RATE_LIMIT_WINDOW_MS);
                return null;
            }

            // ─── Prompt Validation ───
            if (prompt == null || prompt.isBlank()) {
                log.debug("Prompt is empty, skipping Gemini call");
                return null;
            }
            if (prompt.length() > MAX_PROMPT_LENGTH) {
                log.warn("Prompt exceeds max length ({}), truncating", MAX_PROMPT_LENGTH);
                prompt = prompt.substring(0, MAX_PROMPT_LENGTH);
            }

            // ─── API Request ───
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Connection", "close");  // Avoid hanging connections

            Map<String, Object> part    = Map.of("text", prompt);
            Map<String, Object> content = Map.of("parts", List.of(part));
            Map<String, Object> body    = Map.of("contents", List.of(content));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            String url = GEMINI_URL + apiKey;

            long startMs = System.currentTimeMillis();
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            long durationMs = System.currentTimeMillis() - startMs;
            
            log.debug("Gemini API call took {}ms", durationMs);

            // ─── Response Parsing with Null Safety ───
            if (response == null || response.getBody() == null) {
                log.warn("Gemini returned null response");
                return null;
            }

            try {
                Object candidatesObj = response.getBody().get("candidates");
                if (!(candidatesObj instanceof List)) return null;
                
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) candidatesObj;
                if (candidates == null || candidates.isEmpty()) return null;

                Map<String, Object> firstCandidate = candidates.get(0);
                if (firstCandidate == null) return null;
                
                Object contentObj = firstCandidate.get("content");
                if (!(contentObj instanceof Map)) return null;
                
                Map<String, Object> responseContent = (Map<String, Object>) contentObj;
                Object partsObj = responseContent.get("parts");
                if (!(partsObj instanceof List)) return null;
                
                List<Map<String, Object>> parts = (List<Map<String, Object>>) partsObj;
                if (parts == null || parts.isEmpty()) return null;

                Object textObj = parts.get(0).get("text");
                if (!(textObj instanceof String)) return null;
                
                String text = (String) textObj;

                // Strip markdown code fences if present
                if (text != null) {
                    text = text.strip()
                        .replaceAll("^```json\\s*", "")
                        .replaceAll("^```\\s*", "")
                        .replaceAll("\\s*```$", "")
                        .strip();
                }

                return text;
                
            } catch (ClassCastException e) {
                log.error("Unexpected response structure from Gemini: {}", e.getMessage());
                return null;
            }

        } catch (HttpClientErrorException.TooManyRequests e) {
            // 429 — Rate limited by Gemini
            log.warn("Gemini API rate limit hit (429): {}", e.getMessage());
            return null;
        } catch (HttpClientErrorException e) {
            // 4xx errors (401, 403, 404, etc.)
            log.warn("Gemini API client error ({}): {}", e.getStatusCode(), e.getMessage());
            return null;
        } catch (HttpServerErrorException e) {
            // 5xx errors (server down, etc.)
            log.warn("Gemini API server error ({}): {}", e.getStatusCode(), e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("Gemini API call failed: {}", e.getClass().getSimpleName() + " - " + e.getMessage());
            return null;
        }
    }

    /**
     * Token bucket rate limiter: 10 requests per 60 seconds
     */
    private boolean checkRateLimit() {
        long now = System.currentTimeMillis();
        long windowStart = windowStartTime.get();
        long elapsedMs = now - windowStart;

        if (elapsedMs > RATE_LIMIT_WINDOW_MS) {
            // New window: reset counter
            windowStartTime.set(now);
            requestCount.set(1);
            return true;
        }

        // Within window: check if we can add another request
        int currentCount = requestCount.incrementAndGet();
        if (currentCount <= RATE_LIMIT_REQUESTS) {
            log.debug("Gemini request {}/{} in current window", currentCount, RATE_LIMIT_REQUESTS);
            return true;
        }

        // Exceeded limit
        requestCount.decrementAndGet();  // Revert the increment
        return false;
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
