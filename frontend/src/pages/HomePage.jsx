import React, { useState, useEffect, useCallback } from 'react';
import FilterForm from '../components/FilterForm';
import CourseCard from '../components/CourseCard';
import LoadingSpinner from '../components/LoadingSpinner';
import { getRecommendations, getSavedCourseIds, saveCourse, unsaveCourse } from '../services/api';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const HomePage = () => {
  const { user } = useAuth();
  const navigate  = useNavigate();

  const [category, setCategory] = useState('');
  const [level, setLevel]       = useState('');
  const [courses, setCourses]   = useState([]);
  const [loading, setLoading]   = useState(false);
  const [error, setError]       = useState('');
  const [searched, setSearched] = useState(false);
  const [savedIds, setSavedIds] = useState([]);

  // Redirect if not logged in
  useEffect(() => {
    if (!user) navigate('/');
  }, [user, navigate]);

  // Fetch already-saved course IDs on mount
  const fetchSavedIds = useCallback(async () => {
    try {
      const ids = await getSavedCourseIds();
      setSavedIds(ids);
    } catch (_) { /* ignore */ }
  }, []);

  useEffect(() => { fetchSavedIds(); }, [fetchSavedIds]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!category && !level) {
      setError('Please select at least a category or a level.');
      return;
    }
    setLoading(true);
    setError('');
    setCourses([]);
    setSearched(false);

    try {
      const data = await getRecommendations(category, level);
      setCourses(data);
      setSearched(true);
      if (data.length === 0) setError('No courses found. Try a different combination!');
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to connect to the server. Make sure the backend is running.');
    } finally {
      setLoading(false);
    }
  };

  const handleToggleSave = async (courseId) => {
    const isSaved = savedIds.includes(courseId);
    try {
      if (isSaved) {
        await unsaveCourse(courseId);
        setSavedIds(prev => prev.filter(id => id !== courseId));
      } else {
        await saveCourse(courseId);
        setSavedIds(prev => [...prev, courseId]);
      }
    } catch (err) {
      console.error('Save error:', err);
    }
  };

  return (
    <div className="page">
      {/* Hero */}
      <header className="hero">
        <div className="hero-badge">✨ AI-Powered</div>
        <h1 className="hero-title">
          Find Your<br />
          <span className="hero-title-accent">Perfect Course</span>
        </h1>
        <p className="hero-subtitle">
          Discover the best courses matched to your goals — curated from Udemy, Coursera, YouTube and more.
        </p>
      </header>

      {/* Filter Form */}
      <section className="filter-section">
        <FilterForm
          category={category}
          level={level}
          onCategoryChange={setCategory}
          onLevelChange={setLevel}
          onSubmit={handleSubmit}
          loading={loading}
        />
      </section>

      {/* Results */}
      <section className="results-section">
        {loading && <LoadingSpinner />}

        {error && !loading && (
          <div className="error-banner" role="alert">⚠️ {error}</div>
        )}

        {!loading && !error && searched && courses.length > 0 && (
          <>
            <div className="results-header">
              <span className="results-count">{courses.length} course{courses.length !== 1 ? 's' : ''} found</span>
              <span className="results-filters">
                {category && <span className="results-tag">{category}</span>}
                {level && <span className="results-tag">{level}</span>}
              </span>
            </div>

            <div className="courses-grid">
              {courses.map((course, index) => (
                <CourseCard
                  key={course.id}
                  course={course}
                  index={index}
                  isSaved={savedIds.includes(course.id)}
                  onToggleSave={handleToggleSave}
                />
              ))}
            </div>
          </>
        )}

        {!loading && !searched && (
          <div className="empty-state">
            <div className="empty-icon">🎯</div>
            <p style={{ color: 'var(--text-secondary)', fontWeight: 500 }}>
              Select a category and level above to get personalized recommendations.
            </p>
          </div>
        )}
      </section>
    </div>
  );
};

export default HomePage;
