import React from 'react';
import HomePage from './pages/HomePage';
import './index.css';

function App() {
  return (
    <div className="app">
      <nav className="navbar">
        <div className="nav-logo">
          <span className="nav-dot"></span>
          CourseRec
        </div>
        <div className="nav-links">
          <a href="#!" className="nav-link">Explore</a>
          <a href="#!" className="nav-link">About</a>
          <a
            href="http://localhost:8080/api/health"
            target="_blank"
            rel="noopener noreferrer"
            className="nav-btn"
          >
            API Status
          </a>
        </div>
      </nav>

      <main className="main-content">
        <HomePage />
      </main>

      <footer className="footer">
        <p>© 2024 CourseRec · Built with Spring Boot + React · Powered by curiosity.</p>
      </footer>
    </div>
  );
}

export default App;
