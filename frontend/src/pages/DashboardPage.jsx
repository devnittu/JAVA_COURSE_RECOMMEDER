import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { getSavedCourses, unsaveCourse } from '../services/api';

const PLATFORM_ICONS = {
  Udemy: '🎓', YouTube: '▶️', Coursera: '📚',
  Pluralsight: '🔷', edX: '🎓', Udacity: '⚡',
};

const PLATFORM_COLORS = {
  YouTube:  'rgba(255,60,60,0.15)',
  Udemy:    'rgba(168,117,241,0.15)',
  Coursera: 'rgba(80,140,255,0.15)',
  edX:      'rgba(160,160,160,0.12)',
};

const DashboardPage = () => {
  const { user } = useAuth();
  const navigate  = useNavigate();
  const [savedCourses, setSavedCourses] = useState([]);
  const [loading, setLoading]           = useState(true);
  const [removing, setRemoving]         = useState(null);
  const [imgErrors, setImgErrors]       = useState({});

  useEffect(() => { if (!user) navigate('/'); }, [user, navigate]);

  const fetchSaved = useCallback(async () => {
    setLoading(true);
    try {
      const data = await getSavedCourses();
      setSavedCourses(data);
    } catch (e) { console.error(e); }
    finally { setLoading(false); }
  }, []);

  useEffect(() => { fetchSaved(); }, [fetchSaved]);

  const handleRemove = async (courseId) => {
    // ── Layer 1: Optimistic Update (instant UI feedback) ──
    const removedCourse = savedCourses.find(c => c.id === courseId);
    setSavedCourses(prev => prev.filter(c => c.id !== courseId));
    setRemoving(courseId);

    // ── Layer 2: Send request to backend ──
    try {
      await unsaveCourse(courseId);
      // Success: UI already updated optimistically, nothing to do
    } catch (error) {
      // ── Layer 3: Rollback on failure ──
      if (removedCourse) {
        setSavedCourses(prev => [...prev, removedCourse]);
      }
      const errorMsg = error.response?.data?.message || error.message || 'Failed to remove course';
      console.error('Remove error:', errorMsg);
      alert(`❌ Could not remove: ${errorMsg}`);
    } finally {
      setRemoving(null);
    }
  };

  const handleImgError = (id) => setImgErrors(prev => ({ ...prev, [id]: true }));

  const categories = [...new Set(savedCourses.map(c => c.category).filter(Boolean))];
  const platforms  = [...new Set(savedCourses.map(c => c.platform).filter(Boolean))];
  const avgRating  = savedCourses.length
    ? (savedCourses.reduce((s, c) => s + (c.rating || 0), 0) / savedCourses.filter(c => c.rating).length || 0).toFixed(1)
    : '—';

  const joinDate = new Date().toLocaleString('en-US', { month: 'long', year: 'numeric' });

  return (
    <div className="dash-root">

      {/* ── PROFILE BANNER ── */}
      <div className="dash-banner">
        <div className="dash-banner-glow" />
        <div className="dash-banner-inner">
          <div className="dash-avatar-wrap">
            {user?.picture ? (
              <img
                src={user.picture}
                alt={user?.name}
                className="dash-avatar"
                referrerPolicy="no-referrer"
                onError={e => { e.target.style.display = 'none'; }}
              />
            ) : (
              <div className="dash-avatar-fallback">{user?.name?.[0]?.toUpperCase() || '?'}</div>
            )}
            <div className="dash-avatar-ring" />
          </div>

          <div className="dash-profile-info">
            <div className="dash-welcome">Welcome back</div>
            <h1 className="dash-username">{user?.name}</h1>
            <div className="dash-useremail">{user?.email}</div>
            <div className="dash-membersince">Member since {joinDate}</div>
          </div>

          <button className="dash-explore-btn" onClick={() => navigate('/home')} id="explore-btn">
            <span>＋</span> Explore Courses
          </button>
        </div>
      </div>

      {/* ── STATS ROW ── */}
      <div className="dash-stats-row">
        {[
          { icon: '📚', val: savedCourses.length, label: 'Saved' },
          { icon: '🗂️', val: categories.length,   label: 'Categories' },
          { icon: '🌐', val: platforms.length,    label: 'Platforms' },
          { icon: '⭐', val: avgRating,            label: 'Avg Rating' },
        ].map(s => (
          <div className="dash-stat" key={s.label}>
            <span className="dash-stat-icon">{s.icon}</span>
            <span className="dash-stat-val">{s.val}</span>
            <span className="dash-stat-label">{s.label}</span>
          </div>
        ))}
      </div>

      {/* ── SAVED COURSES ── */}
      <div className="dash-section">
        <div className="dash-section-head">
          <h2 className="dash-section-title">My Library</h2>
          <span className="dash-section-count">{savedCourses.length} courses</span>
        </div>

        {loading ? (
          <div className="dash-skeleton-grid">
            {[...Array(4)].map((_, i) => (
              <div key={i} className="dash-skeleton-card" style={{ animationDelay: `${i * 0.1}s` }} />
            ))}
          </div>
        ) : savedCourses.length === 0 ? (
          <div className="dash-empty">
            <div className="dash-empty-illustration">
              <span>📭</span>
            </div>
            <h3 className="dash-empty-title">Your library is empty</h3>
            <p className="dash-empty-sub">Save courses from the explore page to track them here.</p>
            <button className="btn-primary" onClick={() => navigate('/home')}>
              🔍 Explore Courses
            </button>
          </div>
        ) : (
          <div className="dash-courses-grid">
            {savedCourses.map((course, i) => {
              const icon = PLATFORM_ICONS[course.platform] || '📖';
              const platformColor = PLATFORM_COLORS[course.platform] || 'rgba(255,255,255,0.08)';
              const hasThumbnail = course.thumbnail && !imgErrors[course.id];
              const isFree = course.platform === 'YouTube';

              return (
                <div
                  key={course.id}
                  className="dash-course-card"
                  id={`saved-course-${course.id}`}
                  style={{ animationDelay: `${i * 0.07}s` }}
                >
                  {/* Thumbnail */}
                  <div className="dash-course-thumb">
                    {hasThumbnail ? (
                      <img
                        src={course.thumbnail}
                        alt={course.title}
                        className="dash-thumb-img"
                        onError={() => handleImgError(course.id)}
                        loading="lazy"
                        referrerPolicy="no-referrer"
                      />
                    ) : (
                      <div className="dash-thumb-placeholder" style={{ background: platformColor }}>
                        <span className="dash-thumb-icon">{icon}</span>
                      </div>
                    )}
                    {isFree && <span className="dash-free-chip">FREE</span>}
                  </div>

                  {/* Card Body */}
                  <div className="dash-course-body">
                    <div className="dash-course-platform">
                      <span className="dash-platform-dot" data-p={course.platform}>{icon}</span>
                      <span className="dash-platform-name">{course.platform}</span>
                      {course.rating && (
                        <span className="dash-course-rating">⭐ {course.rating}</span>
                      )}
                    </div>

                    <h3 className="dash-course-title">{course.title}</h3>

                    <div className="dash-course-tags">
                      {course.category && <span className="dash-tag">{course.category}</span>}
                      {course.level    && <span className="dash-tag">{course.level}</span>}
                    </div>

                    <div className="dash-course-actions">
                      <a
                        href={course.url}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="dash-btn-view"
                      >
                        View Course →
                      </a>
                      <button
                        className="dash-btn-remove"
                        onClick={() => handleRemove(course.id)}
                        disabled={removing === course.id}
                        id={`remove-course-${course.id}`}
                      >
                        {removing === course.id ? '…' : '✕'}
                      </button>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
};

export default DashboardPage;
