import React, { useState } from 'react';
import CourseDetailModal from './CourseDetailModal';

const PLATFORM_ICONS = {
  Udemy: '🎓', YouTube: '▶️', Coursera: '📚',
  Pluralsight: '🔷', 'Helsinki University': '🏛️', edX: '🎓', Udacity: '⚡',
};

const FREE_PLATFORMS = ['YouTube'];

const CourseCard = ({ course, index, isSaved, onToggleSave }) => {
  const [imgError, setImgError]     = useState(false);
  const [showModal, setShowModal]   = useState(false);

  const icon         = PLATFORM_ICONS[course?.platform] || '📖';
  const isRecommended = course?.score >= 15;
  const isFree       = FREE_PLATFORMS.includes(course?.platform);
  const hasThumbnail = course?.thumbnail && !imgError;

  const handleSaveClick = (e) => {
    e.preventDefault(); e.stopPropagation();
    if (onToggleSave) onToggleSave(course.id);
  };

  const handleCardClick = (e) => {
    e.preventDefault();
    setShowModal(true);
  };

  return (
    <>
      <div
        className="course-card"
        id={`course-card-${course.id}`}
        style={{ animationDelay: `${index * 0.05}s`, cursor: 'pointer' }}
        data-platform={course.platform}
        onClick={handleCardClick}
        role="button"
        tabIndex={0}
        onKeyDown={e => e.key === 'Enter' && setShowModal(true)}
      >
        {isFree && <span className="card-free-badge">Free</span>}

        {hasThumbnail ? (
          <img
            src={course.thumbnail}
            alt={course.title}
            className="card-thumbnail"
            onError={() => setImgError(true)}
            loading="lazy"
            referrerPolicy="no-referrer"
          />
        ) : (
          <div className="card-thumbnail-placeholder">
            <span>{icon}</span>
          </div>
        )}

        <div className="card-header">
          <span className="card-platform-icon">{icon}</span>
          <span className="card-platform" data-p={course.platform}>{course.platform}</span>
          {course.score > 0 && <span className="card-score">Score {course.score}</span>}
        </div>

        {isRecommended && <div className="card-tag-recommended">✓ Top Pick</div>}

        <h3 className="card-title">{course.title}</h3>

        {course.instructor && (
          <p className="card-instructor">👨‍🏫 {course.instructor}</p>
        )}

        <div className="card-footer">
          {course.category && <span className="card-category">{course.category}</span>}
          {course.level    && <span className="card-level">{course.level}</span>}
          {course.rating   && <span className="card-rating">⭐ {parseFloat(course.rating).toFixed(1)}</span>}
        </div>

        <div className="card-actions">
          <span className="card-cta">View Details →</span>
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
      </div>

      {showModal && (
        <CourseDetailModal
          course={course}
          onClose={() => setShowModal(false)}
        />
      )}
    </>
  );
};

export default CourseCard;
