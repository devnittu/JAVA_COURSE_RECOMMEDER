# Course Recommender 🎓

A full-stack **Course Recommendation System** built with Spring Boot, React, and MySQL. Users select their learning interests and get ranked course recommendations from top platforms (Udemy, Coursera, YouTube, and more).

![Tech Stack](https://img.shields.io/badge/Java-17-orange?style=flat-square) ![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2-brightgreen?style=flat-square) ![React](https://img.shields.io/badge/React-18-blue?style=flat-square) ![MySQL](https://img.shields.io/badge/MySQL-8.0-lightblue?style=flat-square)

---

## 🧰 Tech Stack

| Layer    | Technology            |
|----------|-----------------------|
| Backend  | Java 17, Spring Boot 3.2 |
| Database | MySQL 8.0            |
| Frontend | React 18, Axios      |
| Styling  | Vanilla CSS (Inter font) |
| ORM      | Spring Data JPA / Hibernate |

---

## 📁 Project Structure

```
course-recommender/
├── backend/                  # Spring Boot app
│   ├── src/main/java/com/recommender/
│   │   ├── controller/       # REST controllers
│   │   ├── service/          # Business logic
│   │   ├── repository/       # Spring Data JPA
│   │   ├── model/            # JPA entities
│   │   ├── dto/              # Data Transfer Objects
│   │   └── config/           # CORS config
│   ├── src/main/resources/
│   │   └── application.properties
│   ├── pom.xml
│   └── Dockerfile
├── frontend/                 # React app
│   ├── src/
│   │   ├── components/       # CourseCard, FilterForm, LoadingSpinner
│   │   ├── pages/            # HomePage
│   │   ├── services/         # axios API service
│   │   ├── App.js
│   │   └── index.css         # Premium black & white theme
│   └── .env
├── database/
│   └── schema.sql            # DB schema + sample data
└── README.md
```

---

## ⚡ Quick Start

### Prerequisites
- Java 17+
- Maven 3.8+
- MySQL 8.0+
- Node.js 18+ (via nvm)

### 1. Database Setup
```bash
mysql -u root -p < database/schema.sql
```

### 2. Backend
```bash
cd backend
mvn spring-boot:run
```
Backend runs at `http://localhost:8080`

### 3. Frontend
```bash
cd frontend
npm install
npm start
```
Frontend runs at `http://localhost:3000`

---

## 🔌 API Endpoints

| Method | Endpoint                                          | Description                |
|--------|---------------------------------------------------|----------------------------|
| GET    | `/api/health`                                     | Health check               |
| GET    | `/api/recommend?category=Java&level=Beginner`     | Get recommendations        |
| GET    | `/api/courses`                                    | List all courses           |
| POST   | `/api/user/register`                              | Register user (placeholder)|
| POST   | `/api/user/login`                                 | Login user (placeholder)   |

### Recommendation Scoring
| Criterion           | Points |
|---------------------|--------|
| Category matches    | +10    |
| Level matches       | +5     |
| Rating > 4.0        | +3     |

---

## 🎨 UI Theme

- **Background**: `#000000` (pure black)
- **Text**: `#FFFFFF` (pure white)
- **Accent**: `#CCCCCC` (light grey)
- **Font**: Inter (Google Fonts)
- Animated card grid, hover effects, loading spinner

---

## 🚀 Deployment

### Backend (Render)
Uses `Dockerfile` in `backend/` for containerized deployment.

### Frontend (Vercel)
Set environment variable:
```
REACT_APP_API_URL=https://your-backend-url.onrender.com/api
```
Then run `npm run build` and deploy the `build/` folder.

---

## 📋 Sample Courses Included

| Category | Platform   | Level        |
|----------|------------|--------------|
| Java     | Udemy      | Beginner     |
| Java     | YouTube    | Intermediate |
| Java     | Pluralsight| Advanced     |
| AI       | Coursera   | Intermediate |
| AI       | Udemy      | Advanced     |
| AI       | Coursera   | Beginner     |
| Web Dev  | Udemy      | Beginner     |
| Web Dev  | Helsinki U | Intermediate |

---

## 🤝 Contributing

Pull requests are welcome. For major changes, please open an issue first.

---

*Built with ❤️ using Spring Boot + React*
