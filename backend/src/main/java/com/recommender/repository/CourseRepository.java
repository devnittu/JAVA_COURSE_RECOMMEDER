package com.recommender.repository;

import com.recommender.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findByCategory(String category);

    List<Course> findByCategoryAndLevel(String category, String level);

    List<Course> findByLevel(String level);
}
