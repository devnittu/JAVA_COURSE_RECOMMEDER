import React from 'react';
import { useGoogleLogin } from '@react-oauth/google';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { googleAuth } from '../services/api';

const FEATURES = [
  { icon: '🎯', text: 'AI-powered recommendations tailored to your level' },
  { icon: '📚', text: 'Courses from Udemy, Coursera, YouTube & more' },
  { icon: '💾', text: 'Save your favourite courses to your dashboard' },
  { icon: '⚡', text: 'Filter by category & skill level in seconds' },
];

const LoginPage = () => {
  const { login } = useAuth();
  const navigate  = useNavigate();
  const [error, setError] = React.useState('');
  const [loading, setLoading] = React.useState(false);

  const handleGoogleSuccess = async (tokenResponse) => {
    setLoading(true);
    setError('');
    try {
      console.log('Token response received:', { hasCode: !!tokenResponse.code, hasIdToken: !!tokenResponse.id_token, hasAccessToken: !!tokenResponse.access_token });
      
      // useGoogleLogin returns 'code' by default, but we need the id_token
      // If you get a code, you'd need to exchange it on the backend
      // For now, ensure we have the correct token
      const token = tokenResponse.id_token || tokenResponse.access_token;
      
      if (!token) {
        throw new Error('No authentication token received from Google');
      }
      
      const data = await googleAuth(token);
      login(data.user, data.token);
      navigate('/home');
    } catch (err) {
      console.error('Login error:', err);
      const errorMessage = err.response?.data?.message || err.message || 'Login failed. Please try again.';
      setError(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  const loginWithGoogle = useGoogleLogin({
    onSuccess: handleGoogleSuccess,
    onError: (error) => {
      console.error('Google OAuth error:', error);
      setError('Google sign-in was cancelled or failed.');
    },
    flow: 'implicit', // Use implicit flow to get id_token directly
  });

  return (
    <div className="login-wrapper">
      {/* Left Panel — Branding */}
      <div className="login-left">
        <div className="login-brand">
          <div className="login-brand-icon">🎯</div>
          <span className="login-brand-name">CourseRec</span>
        </div>

        <h1 className="login-tagline">
          Learn Smarter,<br />
          <span>Not Harder</span>
        </h1>

        <p className="login-desc">
          Get personalized course recommendations from the world's top learning
          platforms — curated just for your goals and skill level.
        </p>

        <div className="login-features">
          {FEATURES.map((f, i) => (
            <div key={i} className="login-feature">
              <div className="login-feature-icon">{f.icon}</div>
              {f.text}
            </div>
          ))}
        </div>
      </div>

      {/* Right Panel — Auth */}
      <div className="login-right">
        <h2 className="login-card-title">Welcome back 👋</h2>
        <p className="login-card-subtitle">Sign in to access your personalized dashboard</p>

        {error && (
          <div className="error-banner" style={{ marginBottom: '1.5rem', width: '100%' }}>
            ⚠️ {error}
          </div>
        )}

        <button
          id="google-signin-btn"
          className="login-google-btn"
          onClick={() => loginWithGoogle()}
          disabled={loading}
          style={{ width: '100%', maxWidth: '340px' }}
        >
          {loading ? (
            <>
              <div className="btn-spinner" />
              Signing in…
            </>
          ) : (
            <>
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
                <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" fill="#ffffff" opacity=".9"/>
                <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#ffffff" opacity=".9"/>
                <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l3.66-2.84z" fill="#ffffff" opacity=".9"/>
                <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" fill="#ffffff" opacity=".9"/>
              </svg>
              Continue with Google
            </>
          )}
        </button>


        <div className="login-divider">or</div>

        <p className="login-terms">
          By signing in, you agree to our Terms of Service and Privacy Policy.<br />
          Your data is protected and never shared.
        </p>
      </div>
    </div>
  );
};

export default LoginPage;
