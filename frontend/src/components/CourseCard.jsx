import React from 'react';

const PLATFORM_ICONS = {
  Udemy: '🎓',
  YouTube: '▶️',
  Coursera: '📚',
  Pluralsight: '🔷',
  'Helsinki University': '🏛️',
};

const LEVEL_COLORS = {
  Beginner: '#4ade80',
  Intermediate: '#facc15',
  Advanced: '#f87171',
};

const CourseCard = ({ course, index }) => {
  const icon = PLATFORM_ICONS[course.platform] || '📖';
  const levelColor = LEVEL_COLORS[course.level] || '#aaaaaa';

  return (
    <a
      href={course.url}
      target="_blank"
      rel="noopener noreferrer"
      className="course-card"
      id={`course-card-${course.id}`}
      style={{ animationDelay: `${index * 0.07}s` }}
    >
      <div className="card-header">
        <span className="card-platform-icon">{icon}</span>
        <span className="card-platform">{course.platform}</span>
        <span className="card-score">Score: {course.score}</span>
      </div>

      <h3 className="card-title">{course.title}</h3>

      <div className="card-footer">
        <span className="card-category">{course.category}</span>
        <span className="card-level" style={{ color: levelColor }}>
          {course.level}
        </span>
        <span className="card-rating">⭐ {course.rating}</span>
      </div>

      <div className="card-cta">View Course →</div>
    </a>
  );
};

export default CourseCard;
