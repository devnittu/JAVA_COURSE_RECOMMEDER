package com.recommender.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseDetailDTO {
    private String title;
    private String description;
    private String thumbnail;
    private String instructor;
    private String rating;
    private String ratingCount;
    private String students;
    private String price;
    private String duration;
    private String level;
    private String language;
    private String platform;
    private String url;
    private List<String> whatYouLearn;
    private List<String> requirements;
    private String lastUpdated;
    private boolean scraped; // true = real data, false = fallback
}
