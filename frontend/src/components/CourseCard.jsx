import React from 'react';

const PLATFORM_ICONS = {
  Udemy: '🎓',
  YouTube: '▶️',
  Coursera: '📚',
  Pluralsight: '🔷',
  'Helsinki University': '🏛️',
};

const CourseCard = ({ course, index, isSaved, onToggleSave }) => {
  const icon = PLATFORM_ICONS[course.platform] || '📖';
  const isRecommended = course.score >= 15;

  const handleSaveClick = (e) => {
    e.preventDefault();   // don't follow the card link
    e.stopPropagation();
    if (onToggleSave) onToggleSave(course.id);
  };

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

      {isRecommended && (
        <div className="card-tag-recommended">✓ Recommended</div>
      )}

      <h3 className="card-title">{course.title}</h3>

      <div className="card-footer">
        <span className="card-category">{course.category}</span>
        <span className={`card-level ${course.level}`}>{course.level}</span>
        <span className="card-rating">⭐ {course.rating}</span>
      </div>

      <div className="card-actions">
        <span className="card-cta">View Course →</span>
        {onToggleSave && (
          <button
            className={`card-save-btn ${isSaved ? 'saved' : ''}`}
            onClick={handleSaveClick}
            id={`save-btn-${course.id}`}
          >
            {isSaved ? '✓ Saved' : '+ Save'}
          </button>
        )}
      </div>
    </a>
  );
};

export default CourseCard;
