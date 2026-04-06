import axios from 'axios';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 10000,
});

/**
 * Fetch course recommendations based on category and level.
 * @param {string} category - e.g. "Java", "AI", "Web Dev"
 * @param {string} level    - e.g. "Beginner", "Intermediate", "Advanced"
 */
export const getRecommendations = async (category, level) => {
  const params = {};
  if (category) params.category = category;
  if (level) params.level = level;

  const response = await api.get('/recommend', { params });
  return response.data;
};

/**
 * Fetch all courses without scoring.
 */
export const getAllCourses = async () => {
  const response = await api.get('/courses');
  return response.data;
};

export default api;
