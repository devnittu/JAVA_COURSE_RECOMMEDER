import React, { useState } from 'react';

const PLATFORM_ICONS = {
  Udemy:               '🎓',
  YouTube:             '▶️',
  Coursera:            '📚',
  Pluralsight:         '🔷',
  'Helsinki University':'🏛️',
  edX:                 '🎓',
  Udacity:             '⚡',
};

const PLATFORM_LABELS = {
  Udemy:    'Udemy',
  YouTube:  'YouTube',
  Coursera: 'Coursera',
  edX:      'edX',
  Udacity:  'Udacity',
  Pluralsight: 'Pluralsight',
};

const FREE_PLATFORMS = ['YouTube'];

const CourseCard = ({ course, index, isSaved, onToggleSave }) => {
  const [imgError, setImgError] = useState(false);
  const icon = PLATFORM_ICONS[course.platform] || '📖';
  const isRecommended = course.score >= 15;
  const isFree = FREE_PLATFORMS.includes(course.platform);

  const handleSaveClick = (e) => {
    e.preventDefault();
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
      style={{ animationDelay: `${index * 0.06}s` }}
      data-platform={course.platform}
    >
      {/* Free badge */}
      {isFree && <span className="card-free-badge">Free</span>}

      {/* Thumbnail */}
      {course.thumbnail && !imgError ? (
        <img
          src={course.thumbnail}
          alt={course.title}
          className="card-thumbnail"
          onError={() => setImgError(true)}
          loading="lazy"
        />
      ) : (
        <div className="card-thumbnail-placeholder">
          {icon}
        </div>
      )}

      {/* Platform + Score */}
      <div className="card-header">
        <span className="card-platform-icon">{icon}</span>
        <span className="card-platform" data-p={course.platform}>
          {PLATFORM_LABELS[course.platform] || course.platform}
        </span>
        {course.score > 0 && (
          <span className="card-score">Score {course.score}</span>
        )}
      </div>

      {/* Recommended badge */}
      {isRecommended && (
        <div className="card-tag-recommended">✓ Top Pick</div>
      )}

      {/* Title */}
      <h3 className="card-title">{course.title}</h3>

      {/* Meta tags */}
      <div className="card-footer">
        {course.category && (
          <span className="card-category">{course.category}</span>
        )}
        {course.level && (
          <span className={`card-level`}>{course.level}</span>
        )}
        {course.rating && (
          <span className="card-rating">⭐ {course.rating}</span>
        )}
      </div>

      {/* Actions */}
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
