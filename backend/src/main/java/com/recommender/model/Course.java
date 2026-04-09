package com.recommender.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "courses", indexes = {
    @Index(name = "idx_category", columnList = "category"),
    @Index(name = "idx_platform", columnList = "platform"),
    @Index(name = "idx_rating", columnList = "rating DESC"),
    @Index(name = "idx_created_at", columnList = "created_at DESC")
})
@Data
@NoArgsConstructor
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String platform;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String level;

    @Column(nullable = false)
    private Double rating;

    @Column(length = 600)
    private String thumbnail;

    // ─── New Fields (Phase 2) ───
    @Column(length = 255)
    private String instructor;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 50)
    private String duration;

    @Column(length = 50)
    private String students;

    @Column(length = 50)
    private String price;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    // ─────────────────────────────────────────────────────────────
    //  Custom Constructor for DataInitializer (backward compatible)
    //  Signature: (id, title, platform, url, category, level, rating, thumbnail)
    // ─────────────────────────────────────────────────────────────
    public Course(Long id, String title, String platform, String url, String category, 
                  String level, Double rating, String thumbnail) {
        this.id = id;
        this.title = title;
        this.platform = platform;
        this.url = url;
        this.category = category;
        this.level = level;
        this.rating = rating;
        this.thumbnail = thumbnail;
        // Default values for new fields
        this.instructor = platform != null ? platform + " Team" : "Unknown";
        this.price = "Free";
        this.duration = "Variable";
        this.students = "N/A";
    }
}
