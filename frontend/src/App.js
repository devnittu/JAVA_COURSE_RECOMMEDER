import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { GoogleOAuthProvider } from '@react-oauth/google';
import { AuthProvider, useAuth } from './context/AuthContext';
import Navbar from './components/Navbar';
import LoginPage from './pages/LoginPage';
import HomePage from './pages/HomePage';
import DashboardPage from './pages/DashboardPage';
import './index.css';

const GOOGLE_CLIENT_ID = process.env.REACT_APP_GOOGLE_CLIENT_ID ||
  '396278717491-u2r9lbc1psc9oqm8qc08s2uttp0a3jq4.apps.googleusercontent.com';

// Protected route wrapper
const ProtectedRoute = ({ children }) => {
  const { user } = useAuth();
  return user ? children : <Navigate to="/" replace />;
};

// Public route — redirect logged-in users away from login
const PublicRoute = ({ children }) => {
  const { user } = useAuth();
  return !user ? children : <Navigate to="/home" replace />;
};

const AppRoutes = () => {
  const { user } = useAuth();

  return (
    <>
      {/* Navbar is shown on all pages except the login page */}
      {user && <Navbar />}

      <main className="main-content">
        <Routes>
          <Route path="/" element={
            <PublicRoute><LoginPage /></PublicRoute>
          } />
          <Route path="/home" element={
            <ProtectedRoute><HomePage /></ProtectedRoute>
          } />
          <Route path="/dashboard" element={
            <ProtectedRoute><DashboardPage /></ProtectedRoute>
          } />
          {/* Catch-all */}
          <Route path="*" element={<Navigate to={user ? '/home' : '/'} replace />} />
        </Routes>
      </main>

      {user && (
        <footer className="footer">
          <p>© 2024 CourseRec · Built with Spring Boot + React · Powered by curiosity 🚀</p>
        </footer>
      )}
    </>
  );
};

function App() {
  return (
    <GoogleOAuthProvider clientId={GOOGLE_CLIENT_ID}>
      <AuthProvider>
        <BrowserRouter>
          <div className="app">
            <AppRoutes />
          </div>
        </BrowserRouter>
      </AuthProvider>
    </GoogleOAuthProvider>
  );
}

export default App;
