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

                // ── Web Development ──────────────────────────────────────────
                new Course(null, "The Complete Web Development Bootcamp", "Udemy",
                    "https://www.udemy.com/course/the-complete-web-development-bootcamp/",
                    "Web Dev", "Beginner", 4.7,
                    "https://img-c.udemycdn.com/course/750x422/1565338_e54e_16.jpg"),

                new Course(null, "Advanced React and Next.js", "Coursera",
                    "https://www.coursera.org/learn/react",
                    "Web Dev", "Advanced", 4.8,
                    "https://d3njjcbhbojbot.cloudfront.net/api/utilities/v1/imageproxy/https://coursera-course-photos.s3.amazonaws.com/fb/434a0050aa11e5a1dd9f031d6a3b30/jhep-coursera-course-thumbnail.v2.png?auto=format%2Ccompress&dpr=1&w=330&h=330&fit=fill&q=25"),

                new Course(null, "Spring Boot 3 & Hibernate for Beginners", "Udemy",
                    "https://www.udemy.com/course/spring-hibernate-tutorial/",
                    "Web Dev", "Intermediate", 4.7,
                    "https://img-c.udemycdn.com/course/750x422/1908340_95b6_3.jpg"),

                new Course(null, "React - The Complete Guide (incl Hooks, Redux)", "Udemy",
                    "https://www.udemy.com/course/react-the-complete-guide-incl-redux/",
                    "Web Dev", "Beginner", 4.7,
                    "https://img-c.udemycdn.com/course/750x422/1362070_b9a1_2.jpg"),

                new Course(null, "HTML & CSS Full Course - Beginner to Pro", "YouTube",
                    "https://www.youtube.com/watch?v=mU6anWqZJcc",
                    "Web Dev", "Beginner", 4.6,
                    "https://i.ytimg.com/vi/mU6anWqZJcc/maxresdefault.jpg"),

                new Course(null, "Node.js, Express, MongoDB & More: The Complete Bootcamp", "Udemy",
                    "https://www.udemy.com/course/nodejs-express-mongodb-bootcamp/",
                    "Web Dev", "Intermediate", 4.8,
                    "https://img-c.udemycdn.com/course/750x422/1672410_b320_7.jpg"),

                new Course(null, "Full Stack Web Development with Angular", "Coursera",
                    "https://www.coursera.org/specializations/full-stack-react",
                    "Web Dev", "Intermediate", 4.5,
                    "https://d3njjcbhbojbot.cloudfront.net/api/utilities/v1/imageproxy/https://s3.amazonaws.com/coursera-course-photos/83/e258e0532611e5a5072321239eb4d4/jhep-coursera-course-thumbnail.v2.png?auto=format%2Ccompress&dpr=1&w=330&h=330&fit=fill&q=25"),

                new Course(null, "Vue.js 3 - The Complete Guide", "Udemy",
                    "https://www.udemy.com/course/vuejs-2-the-complete-guide/",
                    "Web Dev", "Intermediate", 4.7,
                    "https://img-c.udemycdn.com/course/750x422/995016_ebf4_4.jpg"),

                new Course(null, "CSS Animation, Transitions & Transforms", "YouTube",
                    "https://www.youtube.com/watch?v=YszONjKpgg4",
                    "Web Dev", "Beginner", 4.5,
                    "https://i.ytimg.com/vi/YszONjKpgg4/maxresdefault.jpg"),

                // ── Java ──────────────────────────────────────────────────────
                new Course(null, "Java Programming Masterclass (Java 17)", "Udemy",
                    "https://www.udemy.com/course/java-the-complete-java-developer-course/",
                    "Java", "Beginner", 4.7,
                    "https://img-c.udemycdn.com/course/750x422/533682_c10c_4.jpg"),

                new Course(null, "Java Programming for Complete Beginners", "Udemy",
                    "https://www.udemy.com/course/java-programming-tutorial-for-beginners/",
                    "Java", "Beginner", 4.5,
                    "https://img-c.udemycdn.com/course/750x422/1778574_9577_3.jpg"),

                new Course(null, "Effective Java in Practice", "Pluralsight",
                    "https://www.pluralsight.com/courses/java-fundamentals-language",
                    "Java", "Intermediate", 4.6,
                    null),

                new Course(null, "Java Full Course for Beginners", "YouTube",
                    "https://www.youtube.com/watch?v=CFD9EFcNZTQ",
                    "Java", "Beginner", 4.5,
                    "https://i.ytimg.com/vi/CFD9EFcNZTQ/maxresdefault.jpg"),

                new Course(null, "Algorithms, Part I (Java)", "Coursera",
                    "https://www.coursera.org/learn/algorithms-part1",
                    "Java", "Intermediate", 4.9,
                    "https://d3njjcbhbojbot.cloudfront.net/api/utilities/v1/imageproxy/https://s3.amazonaws.com/coursera-course-photos/08/33f720502a11e59e72391aa537f5c9/principlescomputerscience.png?auto=format%2Ccompress&dpr=1&w=330&h=330&fit=fill&q=25"),

                new Course(null, "Spring Boot Microservices & Spring Cloud", "Udemy",
                    "https://www.udemy.com/course/microservices-with-spring-boot-and-spring-cloud/",
                    "Java", "Advanced", 4.6,
                    "https://img-c.udemycdn.com/course/750x422/2829332_5e84_2.jpg"),

                new Course(null, "Java Multithreading & Concurrency Masterclass", "Udemy",
                    "https://www.udemy.com/course/java-multithreading-concurrency-performance-optimization/",
                    "Java", "Advanced", 4.7,
                    "https://img-c.udemycdn.com/course/750x422/1206210_4720_2.jpg"),

                new Course(null, "Java Tutorial for Beginners", "YouTube",
                    "https://www.youtube.com/watch?v=eIrMbAQSU34",
                    "Java", "Beginner", 4.6,
                    "https://i.ytimg.com/vi/eIrMbAQSU34/maxresdefault.jpg"),

                // ── AI / Machine Learning ──────────────────────────────────────
                new Course(null, "Machine Learning A-Z: AI, Python & R", "Udemy",
                    "https://www.udemy.com/course/machinelearning/",
                    "AI", "Beginner", 4.5,
                    "https://img-c.udemycdn.com/course/750x422/950390_270f_3.jpg"),

                new Course(null, "Deep Learning Specialization", "Coursera",
                    "https://www.coursera.org/specializations/deep-learning",
                    "AI", "Advanced", 4.9,
                    "https://d3njjcbhbojbot.cloudfront.net/api/utilities/v1/imageproxy/https://s3.amazonaws.com/coursera-course-photos/a6/d363ba52f047bda3e9b3d58fa1c48c/Coursera-DL-Specialization-2.png?auto=format%2Ccompress&dpr=1&w=330&h=330&fit=fill&q=25"),

                new Course(null, "Generative AI with LLMs", "Coursera",
                    "https://www.coursera.org/learn/generative-ai-with-llms",
                    "AI", "Intermediate", 4.8,
                    "https://d3njjcbhbojbot.cloudfront.net/api/utilities/v1/imageproxy/https://coursera-course-photos.s3.amazonaws.com/66/699760e81a11e7b3b5e1d1c9b3c3da/Coursera-Logo-600x600.png?auto=format%2Ccompress&dpr=1&w=330&h=330&fit=fill&q=25"),

                new Course(null, "Machine Learning Crash Course", "YouTube",
                    "https://www.youtube.com/watch?v=yN7ypxC7838",
                    "AI", "Beginner", 4.6,
                    "https://i.ytimg.com/vi/yN7ypxC7838/maxresdefault.jpg"),

                new Course(null, "Fast.ai Practical Deep Learning", "YouTube",
                    "https://www.youtube.com/watch?v=8SF_h3xF3cE",
                    "AI", "Intermediate", 4.7,
                    "https://i.ytimg.com/vi/8SF_h3xF3cE/maxresdefault.jpg"),

                new Course(null, "TensorFlow Developer Certificate", "Coursera",
                    "https://www.coursera.org/professional-certificates/tensorflow-in-practice",
                    "AI", "Intermediate", 4.7,
                    null),

                new Course(null, "ChatGPT Prompt Engineering for Developers", "YouTube",
                    "https://www.youtube.com/watch?v=H4YK_7MAckk",
                    "AI", "Beginner", 4.8,
                    "https://i.ytimg.com/vi/H4YK_7MAckk/maxresdefault.jpg"),

                new Course(null, "Natural Language Processing in Python", "Udemy",
                    "https://www.udemy.com/course/nlp-natural-language-processing-with-python/",
                    "AI", "Intermediate", 4.6,
                    "https://img-c.udemycdn.com/course/750x422/1178170_c1f1_5.jpg"),

                // ── Python ────────────────────────────────────────────────────
                new Course(null, "100 Days of Code: The Complete Python Bootcamp", "Udemy",
                    "https://www.udemy.com/course/100-days-of-code/",
                    "Python", "Beginner", 4.8,
                    "https://img-c.udemycdn.com/course/750x422/2776760_f176_10.jpg"),

                new Course(null, "Python for Everybody Specialization", "Coursera",
                    "https://www.coursera.org/specializations/python",
                    "Python", "Beginner", 4.8,
                    "https://d3njjcbhbojbot.cloudfront.net/api/utilities/v1/imageproxy/https://s3.amazonaws.com/coursera-course-photos/08/33f720502a11e59e72391aa537f5c9/principlescomputerscience.png?auto=format%2Ccompress&dpr=1&w=330&h=330&fit=fill&q=25"),

                new Course(null, "Automate the Boring Stuff with Python", "Udemy",
                    "https://www.udemy.com/course/automate/",
                    "Python", "Intermediate", 4.7,
                    "https://img-c.udemycdn.com/course/750x422/1363092_b0d8_2.jpg"),

                new Course(null, "Python Full Course for Beginners", "YouTube",
                    "https://www.youtube.com/watch?v=_uQrJ0TkZlc",
                    "Python", "Beginner", 4.6,
                    "https://i.ytimg.com/vi/_uQrJ0TkZlc/maxresdefault.jpg"),

                new Course(null, "Python Django Full Stack Web App", "Udemy",
                    "https://www.udemy.com/course/python-and-django-full-stack-web-developer-bootcamp/",
                    "Python", "Intermediate", 4.5,
                    "https://img-c.udemycdn.com/course/750x422/213214_2821_7.jpg"),

                new Course(null, "Python for Data Analysis with Pandas", "YouTube",
                    "https://www.youtube.com/watch?v=vmEHCJofslg",
                    "Python", "Intermediate", 4.5,
                    "https://i.ytimg.com/vi/vmEHCJofslg/maxresdefault.jpg"),

                // ── Data Science ──────────────────────────────────────────────
                new Course(null, "Python for Data Science and ML Bootcamp", "Udemy",
                    "https://www.udemy.com/course/python-for-data-science-and-machine-learning-bootcamp/",
                    "Data Science", "Intermediate", 4.6,
                    "https://img-c.udemycdn.com/course/750x422/903744_8eb2.jpg"),

                new Course(null, "IBM Data Science Professional Certificate", "Coursera",
                    "https://www.coursera.org/professional-certificates/ibm-data-science",
                    "Data Science", "Beginner", 4.6,
                    "https://d3njjcbhbojbot.cloudfront.net/api/utilities/v1/imageproxy/https://s3.amazonaws.com/coursera-course-photos/08/33f720502a11e59e72391aa537f5c9/principlescomputerscience.png?auto=format%2Ccompress&dpr=1&w=330&h=330&fit=fill&q=25"),

                new Course(null, "Advanced SQL for Data Scientists", "Coursera",
                    "https://www.coursera.org/learn/advanced-sql",
                    "Data Science", "Advanced", 4.8,
                    null),

                new Course(null, "Practical Data Science", "Udacity",
                    "https://www.udacity.com/course/practical-data-science--nd02",
                    "Data Science", "Advanced", 4.6,
                    null),

                new Course(null, "Data Science Full Course", "YouTube",
                    "https://www.youtube.com/watch?v=ua-CiDNNj30",
                    "Data Science", "Beginner", 4.3,
                    "https://i.ytimg.com/vi/ua-CiDNNj30/maxresdefault.jpg"),

                new Course(null, "Statistics for Data Science and Business Analysis", "Udemy",
                    "https://www.udemy.com/course/statistics-for-data-science-and-business-analysis/",
                    "Data Science", "Beginner", 4.5,
                    "https://img-c.udemycdn.com/course/750x422/1311522_a09e_3.jpg"),

                new Course(null, "Data Visualization with Python", "YouTube",
                    "https://www.youtube.com/watch?v=9uQd5w3GBb8",
                    "Data Science", "Intermediate", 4.4,
                    "https://i.ytimg.com/vi/9uQd5w3GBb8/maxresdefault.jpg"),

                // ── Computer Science ──────────────────────────────────────────
                new Course(null, "CS50: Introduction to Computer Science", "edX",
                    "https://www.edx.org/learn/computer-science/harvard-university-cs50-s-introduction-to-computer-science",
                    "Computer Science", "Beginner", 4.9,
                    "https://prod-discovery.edx-cdn.org/media/course/image/da1b2400-322b-459b-97b0-0c557f05d017-c352e5aad7a5.small.jpg"),

                new Course(null, "Mastering Data Structures & Algorithms", "Udemy",
                    "https://www.udemy.com/course/datastructurescncpp/",
                    "Computer Science", "Intermediate", 4.7,
                    "https://img-c.udemycdn.com/course/750x422/3108986_6b8d.jpg"),

                new Course(null, "Operating Systems: Three Easy Pieces", "YouTube",
                    "https://www.youtube.com/watch?v=jTSvthW34GU",
                    "Computer Science", "Advanced", 4.5,
                    "https://i.ytimg.com/vi/jTSvthW34GU/maxresdefault.jpg"),

                new Course(null, "Computer Science for Beginners - Full Course", "YouTube",
                    "https://www.youtube.com/watch?v=zOjov-2OZ0E",
                    "Computer Science", "Beginner", 4.4,
                    "https://i.ytimg.com/vi/zOjov-2OZ0E/maxresdefault.jpg"),

                new Course(null, "Introduction to Algorithms (MIT OpenCourseWare)", "YouTube",
                    "https://www.youtube.com/watch?v=HtSuA80QTyo",
                    "Computer Science", "Advanced", 4.8,
                    "https://i.ytimg.com/vi/HtSuA80QTyo/maxresdefault.jpg"),

                new Course(null, "Graph Theory Algorithms", "YouTube",
                    "https://www.youtube.com/watch?v=DgXR2OWQnLc",
                    "Computer Science", "Intermediate", 4.6,
                    "https://i.ytimg.com/vi/DgXR2OWQnLc/maxresdefault.jpg"),

                // ── DevOps / Cloud ────────────────────────────────────────────
                new Course(null, "Docker & Kubernetes: The Practical Guide", "Udemy",
                    "https://www.udemy.com/course/docker-kubernetes-the-practical-guide/",
                    "DevOps", "Intermediate", 4.7,
                    "https://img-c.udemycdn.com/course/750x422/3254009_b971_3.jpg"),

                new Course(null, "AWS Certified Solutions Architect", "Udemy",
                    "https://www.udemy.com/course/aws-certified-solutions-architect-associate-saa-c03/",
                    "DevOps", "Intermediate", 4.7,
                    "https://img-c.udemycdn.com/course/750x422/362328_91f3_10.jpg"),

                new Course(null, "DevOps Bootcamp - Terraform, GitHub Actions, AWS", "YouTube",
                    "https://www.youtube.com/watch?v=j5Zsa_eOXeY",
                    "DevOps", "Intermediate", 4.5,
                    "https://i.ytimg.com/vi/j5Zsa_eOXeY/maxresdefault.jpg"),

                new Course(null, "Kubernetes for the Absolute Beginners", "Udemy",
                    "https://www.udemy.com/course/learn-kubernetes/",
                    "DevOps", "Beginner", 4.7,
                    "https://img-c.udemycdn.com/course/750x422/2955700_d7fb_2.jpg"),

                new Course(null, "CI/CD with GitHub Actions - Full Course", "YouTube",
                    "https://www.youtube.com/watch?v=R8_veQiYBjI",
                    "DevOps", "Intermediate", 4.5,
                    "https://i.ytimg.com/vi/R8_veQiYBjI/maxresdefault.jpg"),

                new Course(null, "Terraform Complete Course", "YouTube",
                    "https://www.youtube.com/watch?v=7xngnjfIlK4",
                    "DevOps", "Intermediate", 4.6,
                    "https://i.ytimg.com/vi/7xngnjfIlK4/maxresdefault.jpg"),

                // ── Mobile Development ────────────────────────────────────────
                new Course(null, "The Complete React Native + Hooks Course", "Udemy",
                    "https://www.udemy.com/course/the-complete-react-native-and-redux-course/",
                    "Mobile", "Intermediate", 4.7,
                    "https://img-c.udemycdn.com/course/750x422/959700_8bf2_3.jpg"),

                new Course(null, "Flutter & Dart - The Complete Guide", "Udemy",
                    "https://www.udemy.com/course/learn-flutter-dart-to-build-ios-android-apps/",
                    "Mobile", "Beginner", 4.7,
                    "https://img-c.udemycdn.com/course/750x422/1708340_7108_4.jpg"),

                new Course(null, "Android Kotlin Development Masterclass", "Udemy",
                    "https://www.udemy.com/course/android-kotlin-developer/",
                    "Mobile", "Beginner", 4.6,
                    "https://img-c.udemycdn.com/course/750x422/2001172_da97_3.jpg"),

                new Course(null, "iOS & Swift - The Complete iOS App Development Bootcamp", "Udemy",
                    "https://www.udemy.com/course/ios-13-app-development-bootcamp/",
                    "Mobile", "Beginner", 4.8,
                    "https://img-c.udemycdn.com/course/750x422/1778502_f4b5_12.jpg"),

                new Course(null, "Flutter Tutorial for Beginners", "YouTube",
                    "https://www.youtube.com/watch?v=VPvVD8t02U8",
                    "Mobile", "Beginner", 4.6,
                    "https://i.ytimg.com/vi/VPvVD8t02U8/maxresdefault.jpg"),

                // ── DSA / Interview Prep ──────────────────────────────────────
                new Course(null, "Data Structures & Algorithms - Java", "YouTube",
                    "https://www.youtube.com/watch?v=RBSGKlAvoiM",
                    "DSA", "Beginner", 4.7,
                    "https://i.ytimg.com/vi/RBSGKlAvoiM/maxresdefault.jpg"),

                new Course(null, "LeetCode + DSA: The Complete Roadmap", "YouTube",
                    "https://www.youtube.com/watch?v=pkYVOmU3MgA",
                    "DSA", "Intermediate", 4.8,
                    "https://i.ytimg.com/vi/pkYVOmU3MgA/maxresdefault.jpg"),

                new Course(null, "Dynamic Programming - Coding Interview Patterns", "YouTube",
                    "https://www.youtube.com/watch?v=oBt53YbR9Kk",
                    "DSA", "Advanced", 4.9,
                    "https://i.ytimg.com/vi/oBt53YbR9Kk/maxresdefault.jpg"),

                new Course(null, "Graph Algorithms for Coding Interviews", "Udemy",
                    "https://www.udemy.com/course/graph-theory-algorithms/",
                    "DSA", "Intermediate", 4.7,
                    "https://img-c.udemycdn.com/course/750x422/3397362_f7bf_2.jpg"),

                new Course(null, "Cracking the Coding Interview — Full Course", "YouTube",
                    "https://www.youtube.com/watch?v=69eDnXHmHZ0",
                    "DSA", "Intermediate", 4.6,
                    "https://i.ytimg.com/vi/69eDnXHmHZ0/maxresdefault.jpg")
            );

            courseRepository.saveAll(initialCourses);
            log.info("Successfully populated {} initial courses into the database.", initialCourses.size());
        } else {
            log.info("Database already contains {} courses. No initialization needed.", courseRepository.count());
        }
    }
}
