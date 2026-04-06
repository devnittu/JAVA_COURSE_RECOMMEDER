import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { getSavedCourses, unsaveCourse } from '../services/api';
import LoadingSpinner from '../components/LoadingSpinner';

const PLATFORM_ICONS = {
  Udemy: '🎓', YouTube: '▶️', Coursera: '📚',
  Pluralsight: '🔷', 'Helsinki University': '🏛️',
};

const DashboardPage = () => {
  const { user } = useAuth();
  const navigate  = useNavigate();
  const [savedCourses, setSavedCourses] = useState([]);
  const [loading, setLoading]           = useState(true);
  const [removing, setRemoving]         = useState(null);

  useEffect(() => {
    if (!user) navigate('/');
  }, [user, navigate]);

  const fetchSaved = useCallback(async () => {
    setLoading(true);
    try {
      const data = await getSavedCourses();
      setSavedCourses(data);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetchSaved(); }, [fetchSaved]);

  const handleRemove = async (courseId) => {
    setRemoving(courseId);
    try {
      await unsaveCourse(courseId);
      setSavedCourses(prev => prev.filter(c => c.id !== courseId));
    } catch (e) {
      console.error(e);
    } finally {
      setRemoving(null);
    }
  };

  // Compute stats
  const categories = [...new Set(savedCourses.map(c => c.category))];
  const platforms  = [...new Set(savedCourses.map(c => c.platform))];

  return (
    <div className="page" style={{ paddingTop: '2rem' }}>
      {/* Hero / Profile Banner */}
      <div className="dashboard-hero">
        {user?.picture ? (
          <img src={user.picture} alt={user.name} className="dashboard-avatar" referrerPolicy="no-referrer" />
        ) : (
          <div className="dashboard-avatar-placeholder">{user?.name?.[0] || '?'}</div>
        )}
        <div className="dashboard-user-info">
          <div className="dashboard-greeting">Welcome back 👋</div>
          <div className="dashboard-name">{user?.name}</div>
          <div className="dashboard-email">{user?.email}</div>
        </div>
      </div>

      {/* Stats Row */}
      <div className="dashboard-stats">
        <div className="stat-card">
          <div className="stat-icon blue">📚</div>
          <div className="stat-value">{savedCourses.length}</div>
          <div className="stat-label">Saved Courses</div>
        </div>
        <div className="stat-card">
          <div className="stat-icon green">🗂️</div>
          <div className="stat-value">{categories.length}</div>
          <div className="stat-label">Categories</div>
        </div>
        <div className="stat-card">
          <div className="stat-icon purple">🌐</div>
          <div className="stat-value">{platforms.length}</div>
          <div className="stat-label">Platforms</div>
        </div>
      </div>

      {/* Saved Courses */}
      <div className="dashboard-section">
        <div className="dashboard-section-header">
          <h2 className="dashboard-section-title">📌 My Saved Courses</h2>
          <button className="btn-primary" id="explore-btn" onClick={() => navigate('/home')}>
            + Explore More
          </button>
        </div>

        {loading ? (
          <LoadingSpinner />
        ) : savedCourses.length === 0 ? (
          <div className="dashboard-empty">
            <div className="dashboard-empty-icon">📭</div>
            <p style={{ color: 'var(--text-secondary)', fontWeight: 500 }}>
              You haven't saved any courses yet.
            </p>
            <button className="btn-primary" onClick={() => navigate('/home')}>
              🔍 Explore Courses
            </button>
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.85rem' }}>
            {savedCourses.map((course, i) => (
              <div
                key={course.id}
                className="saved-course-card"
                style={{ animationDelay: `${i * 0.05}s` }}
                id={`saved-course-${course.id}`}
              >
                <div className="saved-course-icon">
                  {PLATFORM_ICONS[course.platform] || '📖'}
                </div>
                <div className="saved-course-info">
                  <div className="saved-course-title">{course.title}</div>
                  <div className="saved-course-meta">
                    <span className="saved-course-tag">{course.platform}</span>
                    <span className="saved-course-tag">{course.category}</span>
                    <span className="saved-course-tag">{course.level}</span>
                    <span className="saved-course-tag">⭐ {course.rating}</span>
                  </div>
                </div>
                <div className="saved-course-actions">
                  <a href={course.url} target="_blank" rel="noopener noreferrer" className="btn-view">
                    View →
                  </a>
                  <button
                    className="btn-remove"
                    onClick={() => handleRemove(course.id)}
                    disabled={removing === course.id}
                    id={`remove-course-${course.id}`}
                  >
                    {removing === course.id ? '…' : '✕ Remove'}
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default DashboardPage;
