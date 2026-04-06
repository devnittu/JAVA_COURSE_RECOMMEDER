package com.recommender.service;

import com.recommender.dto.CourseDTO;
import com.recommender.model.Course;
import com.recommender.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {

    private final CourseRepository courseRepository;

    /**
     * Recommendation scoring logic:
     *  +10 if category matches
     *  +5  if level matches
     *  +3  if rating > 4.0
     */
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

                    return new CourseDTO(
                            course.getId(),
                            course.getTitle(),
                            course.getPlatform(),
                            course.getUrl(),
                            course.getCategory(),
                            course.getLevel(),
                            course.getRating(),
                            score
                    );
                })
                // Only return courses with score > 0
                .filter(dto -> dto.getScore() > 0)
                .sorted(Comparator.comparingInt(CourseDTO::getScore).reversed())
                .collect(Collectors.toList());

        log.debug("Returning {} recommendations", scored.size());
        return scored;
    }

    public List<CourseDTO> getAllCourses() {
        return courseRepository.findAll().stream()
                .map(course -> new CourseDTO(
                        course.getId(),
                        course.getTitle(),
                        course.getPlatform(),
                        course.getUrl(),
                        course.getCategory(),
                        course.getLevel(),
                        course.getRating(),
                        0
                ))
                .collect(Collectors.toList());
    }
}
