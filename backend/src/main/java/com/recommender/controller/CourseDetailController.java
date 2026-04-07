package com.recommender.controller;

import com.recommender.dto.CourseDetailDTO;
import com.recommender.service.CourseScraperService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class CourseDetailController {

    private final CourseScraperService scraperService;

    /**
     * GET /api/courses/detail?url=https%3A%2F%2Fwww.udemy.com%2Fcourse%2F...
     *
     * Scrapes the course URL and returns rich course details:
     * title, description, instructor, rating, price, what-you-learn, etc.
     */
    @GetMapping("/detail")
    public ResponseEntity<CourseDetailDTO> getCourseDetail(@RequestParam String url) {
        try {
            String decoded = URLDecoder.decode(url, StandardCharsets.UTF_8);
            log.info("Scraping course detail for: {}", decoded);

            CourseDetailDTO detail = scraperService.scrape(decoded);
            return ResponseEntity.ok(detail);

        } catch (Exception e) {
            log.error("CourseDetail endpoint error: {}", e.getMessage());
            CourseDetailDTO error = new CourseDetailDTO();
            error.setTitle("Unable to fetch details");
            error.setDescription("Could not load course details. Please visit the course page directly.");
            error.setUrl(url);
            error.setScraped(false);
            return ResponseEntity.ok(error);
        }
    }
}
