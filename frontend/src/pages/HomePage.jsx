import React, { useState } from 'react';
import FilterForm from '../components/FilterForm';
import CourseCard from '../components/CourseCard';
import LoadingSpinner from '../components/LoadingSpinner';
import { getRecommendations } from '../services/api';

const HomePage = () => {
  const [category, setCategory] = useState('');
  const [level, setLevel] = useState('');
  const [courses, setCourses] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [searched, setSearched] = useState(false);

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
      if (data.length === 0) {
        setError('No courses found for the selected filters. Try a different combination!');
      }
    } catch (err) {
      console.error('API error:', err);
      setError(
        err.response?.data?.message ||
        'Failed to connect to the server. Make sure the backend is running on port 8080.'
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="page">
      {/* Hero Section */}
      <header className="hero">
        <div className="hero-badge">AI-Powered</div>
        <h1 className="hero-title">
          Course<br />
          <span className="hero-title-accent">Recommender</span>
        </h1>
        <p className="hero-subtitle">
          Discover the best courses matched to your goals — curated from top platforms worldwide.
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
          <div className="error-banner" role="alert">
            ⚠️ {error}
          </div>
        )}

        {!loading && !error && searched && courses.length > 0 && (
          <>
            <div className="results-header">
              <span className="results-count">{courses.length} courses found</span>
              <span className="results-filters">
                {category && <span className="results-tag">{category}</span>}
                {level && <span className="results-tag">{level}</span>}
              </span>
            </div>

            <div className="courses-grid">
              {courses.map((course, index) => (
                <CourseCard key={course.id} course={course} index={index} />
              ))}
            </div>
          </>
        )}

        {!loading && !searched && (
          <div className="empty-state">
            <div className="empty-icon">🎯</div>
            <p>Select a category and level above to get personalized recommendations.</p>
          </div>
        )}
      </section>
    </div>
  );
};

export default HomePage;
