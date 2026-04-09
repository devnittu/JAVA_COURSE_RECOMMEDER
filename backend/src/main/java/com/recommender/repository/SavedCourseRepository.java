package com.recommender.repository;

import com.recommender.model.SavedCourse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface SavedCourseRepository extends JpaRepository<SavedCourse, Long> {
    List<SavedCourse> findByUserId(Long userId);
    Optional<SavedCourse> findByUserIdAndCourseId(Long userId, Long courseId);
    boolean existsByUserIdAndCourseId(Long userId, Long courseId);
    
    @Transactional
    @Modifying
    void deleteByUserIdAndCourseId(Long userId, Long courseId);
}
