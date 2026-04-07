import React, { useState, useEffect, useCallback } from 'react';
import { getCourseDetail } from '../services/api';

const PLATFORM_ICONS = {
  Udemy: '🎓', YouTube: '▶️', Coursera: '📚',
  edX: '🎓', freeCodeCamp: '🔥', Pluralsight: '🔷', Udacity: '⚡',
};

const CourseDetailModal = ({ course, onClose }) => {
  const [detail, setDetail]   = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError]     = useState(false);

  const fetchDetail = useCallback(async () => {
    if (!course?.url) return;
    setLoading(true);
    setError(false);
    try {
      const data = await getCourseDetail(course.url);
      setDetail(data);
    } catch {
      setError(true);
    } finally {
      setLoading(false);
    }
  }, [course?.url]);

  useEffect(() => { fetchDetail(); }, [fetchDetail]);

  // Close on Escape key
  useEffect(() => {
    const handler = (e) => { if (e.key === 'Escape') onClose(); };
    document.addEventListener('keydown', handler);
    document.body.style.overflow = 'hidden';
    return () => {
      document.removeEventListener('keydown', handler);
      document.body.style.overflow = '';
    };
  }, [onClose]);

  const icon = PLATFORM_ICONS[detail?.platform || course?.platform] || '📖';
  const thumbnail = detail?.thumbnail || course?.thumbnail;

  return (
    <div className="modal-overlay" onClick={onClose} id="course-detail-modal-overlay">
      <div className="modal-panel" onClick={e => e.stopPropagation()} id="course-detail-modal">

        {/* Header */}
        <div className="modal-header">
          <div className="modal-platform-row">
            <span className="modal-platform-icon">{icon}</span>
            <span className="modal-platform-name">{detail?.platform || course?.platform}</span>
            {detail?.price && (
              <span className={`modal-price-badge ${detail.price === 'Free' || detail.price === '0' ? 'free' : ''}`}>
                {detail.price === '0' ? 'Free' : detail.price}
              </span>
            )}
          </div>
          <button className="modal-close-btn" onClick={onClose} id="modal-close-btn">✕</button>
        </div>

        {/* Scrollable content */}
        <div className="modal-body">

          {/* Thumbnail */}
          {thumbnail && (
            <div className="modal-thumb-wrap">
              <img
                src={thumbnail}
                alt={detail?.title || course?.title}
                className="modal-thumbnail"
                referrerPolicy="no-referrer"
                onError={e => e.target.style.display = 'none'}
              />
              <div className="modal-thumb-overlay">
                <a
                  href={course?.url}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="modal-play-btn"
                  id="modal-play-btn"
                >
                  ▶ View Course
                </a>
              </div>
            </div>
          )}

          {loading ? (
            <div className="modal-loading">
              <div className="spinner" />
              <p>Fetching course details...</p>
            </div>
          ) : error ? (
            <div className="modal-error">
              <p>⚠️ Couldn't load details. <a href={course?.url} target="_blank" rel="noopener noreferrer">Open course directly →</a></p>
            </div>
          ) : (
            <>
              {/* Title */}
              <h2 className="modal-title">{detail?.title || course?.title}</h2>

              {/* Stats row */}
              <div className="modal-stats">
                {detail?.rating && (
                  <div className="modal-stat">
                    <span className="modal-stat-icon">⭐</span>
                    <span>{detail.rating}</span>
                    {detail.ratingCount && <span className="modal-stat-sub">({detail.ratingCount})</span>}
                  </div>
                )}
                {detail?.students && (
                  <div className="modal-stat">
                    <span className="modal-stat-icon">👥</span>
                    <span>{detail.students}</span>
                  </div>
                )}
                {detail?.duration && (
                  <div className="modal-stat">
                    <span className="modal-stat-icon">⏱</span>
                    <span>{detail.duration}</span>
                  </div>
                )}
                {detail?.level && (
                  <div className="modal-stat">
                    <span className="modal-stat-icon">📊</span>
                    <span>{detail.level}</span>
                  </div>
                )}
              </div>

              {/* Instructor */}
              {detail?.instructor && (
                <div className="modal-instructor">
                  <span className="modal-section-label">Instructor</span>
                  <span className="modal-instructor-name">{detail.instructor}</span>
                </div>
              )}

              {/* Description */}
              {detail?.description && (
                <div className="modal-description-wrap">
                  <div className="modal-section-label">About this course</div>
                  <p className="modal-description">{detail.description}</p>
                </div>
              )}

              {/* What you'll learn */}
              {detail?.whatYouLearn?.length > 0 && (
                <div className="modal-learn-wrap">
                  <div className="modal-section-label">What you'll learn</div>
                  <ul className="modal-learn-list">
                    {detail.whatYouLearn.map((item, i) => (
                      <li key={i} className="modal-learn-item">
                        <span className="modal-check">✓</span>
                        <span>{item}</span>
                      </li>
                    ))}
                  </ul>
                </div>
              )}

              {/* Requirements */}
              {detail?.requirements?.length > 0 && (
                <div className="modal-req-wrap">
                  <div className="modal-section-label">Requirements</div>
                  <ul className="modal-req-list">
                    {detail.requirements.map((item, i) => (
                      <li key={i}>• {item}</li>
                    ))}
                  </ul>
                </div>
              )}

              {!detail?.scraped && (
                <div className="modal-scrape-note">
                  ℹ️ Live details unavailable — visit the course page for full info.
                </div>
              )}
            </>
          )}
        </div>

        {/* Footer CTA */}
        <div className="modal-footer">
          <button className="modal-cancel-btn" onClick={onClose}>← Back</button>
          <a
            href={course?.url}
            target="_blank"
            rel="noopener noreferrer"
            className="modal-cta-btn"
            id="modal-enroll-btn"
          >
            Open on {detail?.platform || course?.platform} →
          </a>
        </div>
      </div>
    </div>
  );
};

export default CourseDetailModal;
