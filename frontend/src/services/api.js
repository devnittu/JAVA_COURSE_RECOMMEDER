import axios from 'axios';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: { 'Content-Type': 'application/json' },
  timeout: 60000,
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

export const getRecommendations = async (category, level, limit = 20, offset = 0) => {
  const params = { limit, offset };
  if (category) params.category = category;
  if (level)    params.level    = level;
  const response = await api.get('/recommend', { params });
  return response.data;
};

export const getAllCourses = async (limit = 20, offset = 0, sortBy = 'rating') => {
  const response = await api.get('/courses', { params: { limit, offset, sortBy } });
  return response.data;
};

// Paginated search courses by keyword
export const searchCourses = async (query, limit = 20, offset = 0, sortBy = 'relevance') => {
  const response = await api.get('/courses/search', { 
    params: { q: query, limit, offset, sortBy } 
  });
  return response.data;
};

// ── PHASE 2: Paginated versions ──
export const getPaginatedCourses = async (limit = 20, offset = 0, sortBy = 'rating') => {
  const response = await api.get('/courses', { params: { limit, offset, sortBy } });
  return response.data;
};

export const getTrendingCourses = async (limit = 20, offset = 0) => {
  const response = await api.get('/courses/trending', { params: { limit, offset } });
  return response.data;
};

// Advanced search with multiple filters
export const advancedSearch = async (filters) => {
  const {
    query = '',
    category = null,
    platform = null,
    level = null,
    minRating = null,
    sortBy = 'relevance',
    limit = 20,
    offset = 0
  } = filters;

  const params = { limit, offset, sortBy };
  if (query) params.q = query;
  if (category) params.category = category;
  if (platform) params.platform = platform;
  if (level) params.level = level;
  if (minRating) params.minRating = minRating;

  const response = await api.get('/courses/advanced', { params });
  return response.data;
};

// ════════════════════════════
//  Dashboard (saved courses)
// ════════════════════════════

export const getSavedCourses = async () => {
  try {
    const response = await api.get('/dashboard/saved');
    return response.data || [];
  } catch (error) {
    console.error('Error fetching saved courses:', error);
    return [];
  }
};

export const getSavedCourseIds = async () => {
  try {
    const response = await api.get('/dashboard/saved-ids');
    return Array.isArray(response.data) ? response.data : [];
  } catch (error) {
    console.error('Error fetching saved course IDs:', error);
    return [];
  }
};

export const saveCourse = async (courseId) => {
  const response = await api.post(`/dashboard/save/${courseId}`);
  return response.data;
};

export const unsaveCourse = async (courseId) => {
  const response = await api.delete(`/dashboard/unsave/${courseId}`);
  return response.data;
};

// ── PHASE 2: Dashboard with filtering ──
export const getFilteredSavedCourses = async (category = null, platform = null, sortBy = 'newest', limit = 20, offset = 0) => {
  const params = { sortBy, limit, offset };
  if (category) params.category = category;
  if (platform) params.platform = platform;
  const response = await api.get('/dashboard/saved/filtered', { params });
  return response.data;
};

export const getDashboardRecommendations = async () => {
  const response = await api.get('/dashboard/recommendations');
  return response.data;
};

export const getDashboardStats = async () => {
  const response = await api.get('/dashboard/stats');
  return response.data;
};

export const clearAllSavedCourses = async () => {
  const response = await api.post('/dashboard/clear-all');
  return response.data;
};

// ════════════════════════════
//  AI (Gemini)
// ════════════════════════════

export const aiChat = async (message) => {
  const response = await api.post('/ai/chat', { message });
  return response.data;
};

export const aiRecommend = async (savedCategories) => {
  const response = await api.post('/ai/recommend', { savedCategories });
  return response.data;
};

export const aiStatus = async () => {
  const response = await api.get('/ai/status');
  return response.data;
};

// ════════════════════════════
//  Course Detail (Web Scraper)
// ════════════════════════════

export const getCourseDetail = async (courseUrl) => {
  const response = await api.get('/courses/detail', {
    params: { url: courseUrl }
  });
  return response.data;
};

// ════════════════════════════
//  Admin (Seeding)
// ════════════════════════════

export const seedDatabase = async (coursesPerPlatform = 200) => {
  const response = await api.post('/admin/seed', {}, {
    params: { coursesPerPlatform }
  });
  return response.data;
};

export default api;
