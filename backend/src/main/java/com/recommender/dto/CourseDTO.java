package com.recommender.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseDTO {
    private Long id;
    private String title;
    private String platform;
    private String url;
    private String category;
    private String level;
    private Double rating;
    private int score;
    private String thumbnail;
    
    // Phase 2 fields
    private String instructor;
    private String description;
    private String duration;
    private String students;
    private String price;
}
