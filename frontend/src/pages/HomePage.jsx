import React, { useState, useEffect, useCallback, useRef } from 'react';
import CourseCard from '../components/CourseCard';
import LoadingSpinner from '../components/LoadingSpinner';
import {
  getRecommendations,
  getSavedCourseIds,
  saveCourse,
  unsaveCourse,
  searchCourses,
  getTrendingCourses,
} from '../services/api';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const CATEGORIES = [
  { label: 'All',        value: '' },
  { label: '☕ Java',    value: 'Java' },
  { label: '🐍 Python',  value: 'Python' },
  { label: '🌐 Web Dev', value: 'Web Dev' },
  { label: '🤖 AI / ML', value: 'AI' },
  { label: '📊 Data',    value: 'Data Science' },
  { label: '💻 CS',      value: 'Computer Science' },
  { label: '☁️ DevOps',  value: 'DevOps' },
  { label: '📱 Mobile',  value: 'Mobile' },
  { label: '🧩 DSA',     value: 'DSA' },
];

const SUGGESTIONS = [
  'Java Spring Boot',
  'React for beginners',
  'Machine Learning',
  'Python data science',
  'Docker Kubernetes',
];

const HomePage = () => {
  const { user } = useAuth();
  const navigate  = useNavigate();

  const [query, setQuery]           = useState('');
  const [activeCategory, setActiveCategory] = useState('');
  const [courses, setCourses]       = useState([]);
  const [trending, setTrending]     = useState([]);
  const [loading, setLoading]       = useState(false);
  const [trendingLoading, setTrendingLoading] = useState(true);
  const [error, setError]           = useState('');
  const [searched, setSearched]     = useState(false);
  const [savedIds, setSavedIds]     = useState([]);
  const [offset, setOffset]         = useState(0);
  const [hasMore, setHasMore]       = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);

  const debounceRef = useRef(null);

  // Redirect if not logged in
  useEffect(() => {
    if (!user) navigate('/');
  }, [user, navigate]);

  // Fetch saved IDs + trending on mount
  const fetchSavedIds = useCallback(async () => {
    try {
      const ids = await getSavedCourseIds();
      setSavedIds(ids);
    } catch (_) {}
  }, []);

  const fetchTrending = useCallback(async () => {
    setTrendingLoading(true);
    try {
      const data = await getTrendingCourses();
      // Handle both array and paginated response
      const courses = Array.isArray(data) ? data : data.content || [];
      setTrending(courses);
    } catch (_) {
      // fallback silently — trending is optional
    } finally {
      setTrendingLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchSavedIds();
    fetchTrending();
  }, [fetchSavedIds, fetchTrending]);

  // ─── Search handler ──────────────────────────────────
  const doSearch = useCallback(async (q, cat) => {
    const trimmed = q?.trim();
    const hasCat  = cat && cat !== '';

    if (!trimmed && !hasCat) {
      setSearched(false);
      setCourses([]);
      setError('');
      return;
    }

    setLoading(true);
    setError('');
    setCourses([]);
    setSearched(false);

    try {
      let data;
      if (trimmed) {
        data = await searchCourses(trimmed);
      } else {
        data = await getRecommendations(cat, '');
      }
      // Handle both array and paginated response
      const courseList = Array.isArray(data) ? data : data.content || [];
      setCourses(courseList);
      setSearched(true);
      if (courseList.length === 0) setError('No courses found. Try a different keyword!');
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to fetch courses. Make sure the backend is running.');
    } finally {
      setLoading(false);
    }
  }, []);

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    doSearch(query, activeCategory);
  };

  const handleCategoryClick = (cat) => {
    setActiveCategory(cat);
    setQuery('');
    doSearch('', cat);
  };

  const handleSuggestionClick = (s) => {
    setQuery(s);
    doSearch(s, '');
  };

  const handleInputChange = (e) => {
    const val = e.target.value;
    setQuery(val);
    // debounce auto-search after 600ms
    clearTimeout(debounceRef.current);
    if (val.trim().length >= 3) {
      debounceRef.current = setTimeout(() => {
        doSearch(val, activeCategory);
      }, 600);
    } else if (val.trim() === '') {
      setSearched(false);
      setCourses([]);
      setError('');
    }
  };

  const handleToggleSave = async (courseId) => {
    if (!courseId) return; // Guard against missing courseId
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

  const handleLoadMore = async () => {
    if (!hasMore || loadingMore) return; // Prevent duplicate requests
    
    setLoadingMore(true);
    try {
      let data;
      const newOffset = offset + 20;

      if (searched && query.trim()) {
        data = await searchCourses(query.trim(), 20, newOffset);
      } else if (activeCategory) {
        data = await getRecommendations(activeCategory, '', 20, newOffset);
      } else {
        data = await getTrendingCourses(20, newOffset);
      }

      // Handle both array and paginated response
      const newCourses = Array.isArray(data) ? data : data.content || [];
      
      if (newCourses.length > 0) {
        setCourses(prev => [...prev, ...newCourses]);
        setOffset(newOffset);
      }
      
      // Check if there are more courses
      if (Array.isArray(data)) {
        setHasMore(newCourses.length === 20); // Simple heuristic
      } else {
        setHasMore(data.hasMore !== false && newCourses.length > 0);
      }
    } catch (err) {
      console.error('Load more error:', err);
      setHasMore(false);
    } finally {
      setLoadingMore(false);
    }
  };

  const displayCourses = searched && courses.length > 0 ? courses : (trending && trending.length > 0 ? trending : []);

  return (
    <div className="page">
      {/* ── HERO ── */}
      <header className="hero">
        <div className="hero-badge">✦ AI-Powered Course Discovery</div>

        <h1 className="hero-title">
          Find Your<br />
          <span className="hero-title-accent">Perfect Course</span>
        </h1>

        <p className="hero-subtitle">
          Discover top-rated courses from Udemy, Coursera, YouTube and more — matched to your goals.
        </p>

        {/* Search Bar */}
        <form className="hero-search-wrapper" onSubmit={handleSearchSubmit} id="hero-search-form">
          <div className="hero-search-box">
            <span className="hero-search-icon">🔍</span>
            <input
              id="hero-search-input"
              className="hero-search-input"
              type="text"
              value={query}
              onChange={handleInputChange}
              placeholder="Search any topic — Java, React, ML..."
              autoComplete="off"
            />
            <button
              type="submit"
              className="hero-search-btn"
              disabled={loading}
              id="hero-search-btn"
            >
              {loading ? '...' : 'Search'}
            </button>
          </div>
          <div className="hero-search-hint">
            Try:{' '}
            {SUGGESTIONS.map(s => (
              <span key={s} onClick={() => handleSuggestionClick(s)}>{s}</span>
            ))}
          </div>
        </form>

        {/* Hero Stats Row */}
        <div className="hero-stats">
          <div className="hero-stat">
            <span className="hero-stat-value">56+</span>
            <span className="hero-stat-label">Courses</span>
          </div>
          <div className="hero-stat-divider" />
          <div className="hero-stat">
            <span className="hero-stat-value">6</span>
            <span className="hero-stat-label">Platforms</span>
          </div>
          <div className="hero-stat-divider" />
          <div className="hero-stat">
            <span className="hero-stat-value">9</span>
            <span className="hero-stat-label">Categories</span>
          </div>
          <div className="hero-stat-divider" />
          <div className="hero-stat">
            <span className="hero-stat-value">Free</span>
            <span className="hero-stat-label">&amp; Paid</span>
          </div>
        </div>
      </header>

      {/* ── CATEGORY PILLS ── */}
      <section aria-label="Filter by category">
        <div className="category-pills">
          {CATEGORIES.map(cat => (
            <button
              key={cat.value}
              className={`category-pill ${activeCategory === cat.value ? 'active' : ''}`}
              onClick={() => handleCategoryClick(cat.value)}
              id={`cat-pill-${cat.value || 'all'}`}
            >
              {cat.label}
            </button>
          ))}
        </div>
      </section>

      {/* ── RESULTS / TRENDING ── */}
      <section className="results-section">
        {loading && <LoadingSpinner />}

        {error && !loading && (
          <div className="error-banner" role="alert">⚠️ {error}</div>
        )}

        {!loading && !error && (
          <>
            {/* Section Label */}
            <div className="section-label">
              <span className="section-label-text">
                {searched ? `Results` : '🔥 Trending Now'}
              </span>
              <span className="section-label-line" />
              {searched && courses.length > 0 && (
                <span className="section-label-badge">{courses.length} courses</span>
              )}
              {!searched && (
                <span className="section-label-badge">Popular</span>
              )}
            </div>

            {/* Trending skeleton loading */}
            {!searched && trendingLoading && (
              <div className="skeleton-grid">
                {[...Array(6)].map((_, i) => (
                  <div key={i} className="skeleton-card" style={{ animationDelay: `${i * 0.1}s` }} />
                ))}
              </div>
            )}

            {/* Course grid */}
            {!trendingLoading && displayCourses.length > 0 && (
              <>
                {searched && (
                  <div className="results-header">
                    <span className="results-count">
                      {courses.length} course{courses.length !== 1 ? 's' : ''} found
                    </span>
                    <span className="results-filters">
                      {query && <span className="results-tag">"{query}"</span>}
                      {activeCategory && <span className="results-tag">{activeCategory}</span>}
                    </span>
                  </div>
                )}

                <div className="courses-grid">
                  {displayCourses.map((course, index) => (
                    <CourseCard
                      key={course.id}
                      course={course}
                      index={index}
                      isSaved={savedIds.includes(course.id)}
                      onToggleSave={handleToggleSave}
                    />
                  ))}
                </div>

                {/* Load More Button */}
                {hasMore && displayCourses.length > 0 && (
                  <div className="load-more-container">
                    <button
                      onClick={handleLoadMore}
                      disabled={loadingMore}
                      className="btn-load-more"
                      id="load-more-btn"
                    >
                      {loadingMore ? '⏳ Loading...' : '📖 Load More Courses'}
                    </button>
                  </div>
                )}
              </>
            )}

            {/* Empty state when search returned nothing */}
            {searched && !loading && courses.length === 0 && !error && (
              <div className="empty-state">
                <div className="empty-icon">🎯</div>
                <p>No courses found for <strong>"{query}"</strong>. Try a different keyword.</p>
              </div>
            )}

            {/* Initial empty state (no trending loaded) */}
            {!searched && !trendingLoading && trending.length === 0 && (
              <div className="empty-state">
                <div className="empty-icon">🔍</div>
                <p style={{ color: 'var(--text-secondary)', fontWeight: 500 }}>
                  Search for a topic above to discover courses.
                </p>
              </div>
            )}
          </>
        )}
      </section>
    </div>
  );
};

export default HomePage;
