import React, { useState, useRef, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const Navbar = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const dropdownRef = useRef(null);

  const isActive = (path) => location.pathname === path ? 'nav-link active' : 'nav-link';

  // Close dropdown when clicking outside
  useEffect(() => {
    const handler = (e) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target)) {
        setDropdownOpen(false);
      }
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  const handleLogout = () => {
    logout();
    navigate('/');
    setDropdownOpen(false);
  };

  return (
    <nav className="navbar">
      {/* Logo */}
      <div className="nav-logo" style={{ cursor: 'pointer' }} onClick={() => navigate(user ? '/home' : '/')}>
        <div className="nav-logo-icon">🎯</div>
        CourseRec
      </div>

      {/* Center Links */}
      {user && (
        <div className="nav-links">
          <span className={isActive('/home')} style={{ cursor: 'pointer' }} onClick={() => navigate('/home')}>
            Explore
          </span>
          <span className={isActive('/dashboard')} style={{ cursor: 'pointer' }} onClick={() => navigate('/dashboard')}>
            Dashboard
          </span>
        </div>
      )}

      {/* Right side */}
      <div className="nav-right">
        {user ? (
          <div className="nav-user-btn" ref={dropdownRef} onClick={() => setDropdownOpen(o => !o)} id="nav-user-menu">
            {user.picture ? (
              <img src={user.picture} alt={user.name} className="nav-avatar" referrerPolicy="no-referrer" />
            ) : (
              <div className="nav-avatar-placeholder">{user.name?.[0] || '?'}</div>
            )}
            <span className="nav-user-name">{user.name?.split(' ')[0]}</span>
            <span style={{ color: '#94A3B8', fontSize: '0.65rem' }}>▾</span>

            {dropdownOpen && (
              <div className="nav-dropdown" onClick={e => e.stopPropagation()}>
                <div className="nav-dropdown-header">
                  <div className="nav-dropdown-name">{user.name}</div>
                  <div className="nav-dropdown-email">{user.email}</div>
                </div>
                <button className="nav-dropdown-item" onClick={() => { navigate('/dashboard'); setDropdownOpen(false); }}>
                  📊 Dashboard
                </button>
                <button className="nav-dropdown-item" onClick={() => { navigate('/home'); setDropdownOpen(false); }}>
                  🔍 Explore Courses
                </button>
                <div className="nav-dropdown-divider" />
                <button className="nav-dropdown-item danger" onClick={handleLogout} id="logout-btn">
                  🚪 Sign Out
                </button>
              </div>
            )}
          </div>
        ) : (
          <button className="btn-primary" onClick={() => navigate('/')} style={{ fontSize: '0.85rem', padding: '7px 16px' }}>
            Sign In
          </button>
        )}
      </div>
    </nav>
  );
};

export default Navbar;
