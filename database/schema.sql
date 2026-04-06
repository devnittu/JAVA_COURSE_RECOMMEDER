-- Course Recommender Database Schema
-- Run: mysql -u root < database/schema.sql

CREATE DATABASE IF NOT EXISTS course_recommender;
USE course_recommender;

-- Courses table
CREATE TABLE IF NOT EXISTS courses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    platform VARCHAR(100) NOT NULL,
    url VARCHAR(500) NOT NULL,
    category VARCHAR(100) NOT NULL,
    level VARCHAR(50) NOT NULL,
    rating DECIMAL(3,1) NOT NULL
);

-- Interests table
CREATE TABLE IF NOT EXISTS interests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

-- Users table (Google OAuth)
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    google_id VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    picture VARCHAR(500),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Saved courses table
CREATE TABLE IF NOT EXISTS saved_courses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    saved_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_user_course (user_id, course_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE
);

-- Clear and insert fresh sample data
TRUNCATE TABLE courses;

INSERT INTO courses (title, platform, url, category, level, rating) VALUES
-- Java Courses
('Java Programming Masterclass', 'Udemy', 'https://www.udemy.com/course/java-the-complete-java-developer-course/', 'Java', 'Beginner', 4.6),
('Spring Boot Full Course', 'YouTube', 'https://www.youtube.com/watch?v=9SGDpanrc8U', 'Java', 'Intermediate', 4.5),
('Effective Java - Advanced Patterns', 'Pluralsight', 'https://www.pluralsight.com/courses/java-patterns', 'Java', 'Advanced', 4.7),
('Java for Beginners', 'YouTube', 'https://www.youtube.com/watch?v=eIrMbAQSU34', 'Java', 'Beginner', 4.4),

-- AI / Machine Learning Courses
('Machine Learning Specialization', 'Coursera', 'https://www.coursera.org/specializations/machine-learning-introduction', 'AI', 'Intermediate', 4.9),
('Deep Learning with Python', 'Udemy', 'https://www.udemy.com/course/deep-learning-with-keras/', 'AI', 'Advanced', 4.6),
('AI For Everyone', 'Coursera', 'https://www.coursera.org/learn/ai-for-everyone', 'AI', 'Beginner', 4.8),
('TensorFlow Developer Certificate', 'Coursera', 'https://www.coursera.org/professional-certificates/tensorflow-in-practice', 'AI', 'Intermediate', 4.7),

-- Web Development Courses
('The Web Developer Bootcamp 2024', 'Udemy', 'https://www.udemy.com/course/the-web-developer-bootcamp/', 'Web Dev', 'Beginner', 4.7),
('React - The Complete Guide', 'Udemy', 'https://www.udemy.com/course/react-the-complete-guide-incl-hooks-react-router-redux/', 'Web Dev', 'Intermediate', 4.6),
('Full Stack Open 2024', 'Helsinki University', 'https://fullstackopen.com/en/', 'Web Dev', 'Intermediate', 4.9),
('CSS Grid & Flexbox Master Course', 'YouTube', 'https://www.youtube.com/watch?v=3elGSZSWTbM', 'Web Dev', 'Beginner', 4.3);

-- Sample interests
INSERT IGNORE INTO interests (name) VALUES ('Java'), ('AI'), ('Web Dev'), ('Python'), ('DevOps');


INSERT INTO courses (title, platform, url, category, level, rating) VALUES
-- Java Courses
('Java Programming Masterclass', 'Udemy', 'https://www.udemy.com/course/java-the-complete-java-developer-course/', 'Java', 'Beginner', 4.6),
('Spring Boot Full Course', 'YouTube', 'https://www.youtube.com/watch?v=9SGDpanrc8U', 'Java', 'Intermediate', 4.5),
('Effective Java - Advanced Patterns', 'Pluralsight', 'https://www.pluralsight.com/courses/java-patterns', 'Java', 'Advanced', 4.7),
('Java for Beginners', 'YouTube', 'https://www.youtube.com/watch?v=eIrMbAQSU34', 'Java', 'Beginner', 4.4),

-- AI / Machine Learning Courses
('Machine Learning Specialization', 'Coursera', 'https://www.coursera.org/specializations/machine-learning-introduction', 'AI', 'Intermediate', 4.9),
('Deep Learning with Python', 'Udemy', 'https://www.udemy.com/course/deep-learning-with-keras/', 'AI', 'Advanced', 4.6),
('AI For Everyone', 'Coursera', 'https://www.coursera.org/learn/ai-for-everyone', 'AI', 'Beginner', 4.8),
('TensorFlow Developer Certificate', 'Coursera', 'https://www.coursera.org/professional-certificates/tensorflow-in-practice', 'AI', 'Intermediate', 4.7),

-- Web Development Courses
('The Web Developer Bootcamp 2024', 'Udemy', 'https://www.udemy.com/course/the-web-developer-bootcamp/', 'Web Dev', 'Beginner', 4.7),
('React - The Complete Guide', 'Udemy', 'https://www.udemy.com/course/react-the-complete-guide-incl-hooks-react-router-redux/', 'Web Dev', 'Intermediate', 4.6),
('Full Stack Open 2024', 'Helsinki University', 'https://fullstackopen.com/en/', 'Web Dev', 'Intermediate', 4.9),
('CSS Grid & Flexbox Master Course', 'YouTube', 'https://www.youtube.com/watch?v=3elGSZSWTbM', 'Web Dev', 'Beginner', 4.3);

-- Sample interests
INSERT IGNORE INTO interests (name) VALUES ('Java'), ('AI'), ('Web Dev'), ('Python'), ('DevOps');
