import React from 'react';

const LoadingSpinner = () => (
  <div className="spinner-container">
    <div className="spinner" aria-label="Loading recommendations..."></div>
    <p className="spinner-text">Finding the best courses for you...</p>
  </div>
);

export default LoadingSpinner;
