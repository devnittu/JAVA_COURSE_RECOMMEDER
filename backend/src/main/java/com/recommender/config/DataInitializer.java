package com.recommender.config;

import com.recommender.model.Course;
import com.recommender.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final CourseRepository courseRepository;

    @Override
    public void run(String... args) throws Exception {
        if (courseRepository.count() == 0) {
            log.info("Database is empty. Populating initial courses...");

            List<Course> initialCourses = List.of(
                    new Course(null, "The Complete Web Development Bootcamp", "Udemy", "https://udemy.com", "Web Development", "Beginner", 4.7),
                    new Course(null, "Advanced React and Next.js", "Coursera", "https://coursera.org", "Web Development", "Advanced", 4.8),
                    new Course(null, "Machine Learning A-Z", "Udemy", "https://udemy.com", "Artificial Intelligence", "Beginner", 4.5),
                    new Course(null, "Deep Learning Specialization", "Coursera", "https://coursera.org", "Artificial Intelligence", "Advanced", 4.9),
                    new Course(null, "Python for Data Science and Machine Learning", "Udemy", "https://udemy.com", "Data Science", "Intermediate", 4.6),
                    new Course(null, "Data Science crash course", "YouTube", "https://youtube.com", "Data Science", "Beginner", 4.3),
                    new Course(null, "Spring Boot 3, Spring 6 & Hibernate", "Udemy", "https://udemy.com", "Web Development", "Intermediate", 4.7),
                    new Course(null, "Full Stack Java Developer", "Coursera", "https://coursera.org", "Web Development", "Beginner", 4.5),
                    new Course(null, "Generative AI with Large Language Models", "Coursera", "https://coursera.org", "Artificial Intelligence", "Intermediate", 4.8),
                    new Course(null, "Practical Data Science", "Udacity", "https://udacity.com", "Data Science", "Advanced", 4.6),
                    new Course(null, "React - The Complete Guide (incl Hooks, React Router, Redux)", "Udemy", "https://udemy.com", "Web Development", "Beginner", 4.7),
                    new Course(null, "Artificial Intelligence for Business", "Udemy", "https://udemy.com", "Artificial Intelligence", "Beginner", 4.4),
                    new Course(null, "Advanced SQL for Data Scientists", "Coursera", "https://coursera.org", "Data Science", "Advanced", 4.8),
                    new Course(null, "Mastering Data Structures & Algorithms", "Udemy", "https://udemy.com", "Computer Science", "Intermediate", 4.7),
                    new Course(null, "CS50: Introduction to Computer Science", "edX", "https://edx.org", "Computer Science", "Beginner", 4.9)
            );

            courseRepository.saveAll(initialCourses);
            log.info("Successfully populated 15 initial courses into the database.");
        } else {
            log.info("Database already contains courses. No initialization needed.");
        }
    }
}
