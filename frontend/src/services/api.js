import axios from 'axios';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: { 'Content-Type': 'application/json' },
  timeout: 15000,
});

// ── Attach JWT token to every request ──
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('cr_token');
  if (token) config.headers['Authorization'] = `Bearer ${token}`;
  return config;
});

// ════════════════════════════
//  Auth
// ════════════════════════════

/**
 * Exchange a Google access_token for our backend JWT.
 * The backend validates it with Google and upserts the user.
 */
export const googleAuth = async (accessToken) => {
  // We send the access_token; backend calls Google userinfo endpoint
  const response = await api.post('/auth/google', { idToken: accessToken });
  return response.data;
};

export const getMe = async () => {
  const response = await api.get('/auth/me');
  return response.data;
};

// ════════════════════════════
//  Courses / Recommendations
// ════════════════════════════

export const getRecommendations = async (category, level) => {
  const params = {};
  if (category) params.category = category;
  if (level)    params.level    = level;
  const response = await api.get('/recommend', { params });
  return response.data;
};

export const getAllCourses = async () => {
  const response = await api.get('/courses');
  return response.data;
};

// ════════════════════════════
//  Dashboard (saved courses)
// ════════════════════════════

export const getSavedCourses = async () => {
  const response = await api.get('/dashboard/saved');
  return response.data;
};

export const getSavedCourseIds = async () => {
  const response = await api.get('/dashboard/saved-ids');
  return response.data;
};

export const saveCourse = async (courseId) => {
  const response = await api.post(`/dashboard/save/${courseId}`);
  return response.data;
};

export const unsaveCourse = async (courseId) => {
  const response = await api.delete(`/dashboard/unsave/${courseId}`);
  return response.data;
};

export default api;
