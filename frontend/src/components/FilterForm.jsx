import React from 'react';

const CATEGORIES = ['Java', 'AI', 'Web Dev'];
const LEVELS = ['Beginner', 'Intermediate', 'Advanced'];

const FilterForm = ({ category, level, onCategoryChange, onLevelChange, onSubmit, loading }) => {
  return (
    <form className="filter-form" onSubmit={onSubmit}>
      <div className="form-row">
        <div className="form-group">
          <label htmlFor="category-select" className="form-label">
            Category
          </label>
          <select
            id="category-select"
            className="form-select"
            value={category}
            onChange={(e) => onCategoryChange(e.target.value)}
          >
            <option value="">— Select Category —</option>
            {CATEGORIES.map((cat) => (
              <option key={cat} value={cat}>
                {cat}
              </option>
            ))}
          </select>
        </div>

        <div className="form-group">
          <label htmlFor="level-select" className="form-label">
            Level
          </label>
          <select
            id="level-select"
            className="form-select"
            value={level}
            onChange={(e) => onLevelChange(e.target.value)}
          >
            <option value="">— Select Level —</option>
            {LEVELS.map((lvl) => (
              <option key={lvl} value={lvl}>
                {lvl}
              </option>
            ))}
          </select>
        </div>
      </div>

      <button
        id="recommend-btn"
        type="submit"
        className={`btn-recommend ${loading ? 'btn-loading' : ''}`}
        disabled={loading}
      >
        {loading ? (
          <span className="btn-spinner" />
        ) : (
          '⚡ Get Recommendations'
        )}
      </button>
    </form>
  );
};

export default FilterForm;
