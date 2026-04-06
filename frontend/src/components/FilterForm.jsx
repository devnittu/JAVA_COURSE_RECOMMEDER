import React from 'react';

const FilterForm = ({ category, level, onCategoryChange, onLevelChange, onSubmit, loading }) => (
  <form className="filter-form" onSubmit={onSubmit} id="filter-form">
    <div className="form-row">
      <div className="form-group">
        <label className="form-label" htmlFor="category-select">Category</label>
        <select
          id="category-select"
          className="form-select"
          value={category}
          onChange={e => onCategoryChange(e.target.value)}
        >
          <option value="">All Categories</option>
          <option value="Java">☕ Java</option>
          <option value="AI">🤖 AI / Machine Learning</option>
          <option value="Web Dev">🌐 Web Development</option>
        </select>
      </div>

      <div className="form-group">
        <label className="form-label" htmlFor="level-select">Skill Level</label>
        <select
          id="level-select"
          className="form-select"
          value={level}
          onChange={e => onLevelChange(e.target.value)}
        >
          <option value="">All Levels</option>
          <option value="Beginner">🌱 Beginner</option>
          <option value="Intermediate">🚀 Intermediate</option>
          <option value="Advanced">⚡ Advanced</option>
        </select>
      </div>
    </div>

    <button type="submit" className="btn-recommend" disabled={loading} id="get-recommendations-btn">
      {loading ? (
        <>
          <span className="btn-spinner" />
          Finding Courses…
        </>
      ) : (
        '🔍 Get Recommendations'
      )}
    </button>
  </form>
);

export default FilterForm;
