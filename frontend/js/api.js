/**
 * Centralized REST API Service Module
 * Pure Vanilla JavaScript (No external frameworks)
 * 
 * Provides structured promise-based HTTP methods, session header injection,
 * error handling, and server health telemetry.
 */

const API = {
  baseUrl: (function() {
    if (window.location.protocol === 'file:') {
      return 'http://localhost:8080';
    }
    return '';
  })(),

  /**
   * Builds normalized absolute/relative URL
   */
  endpoint(path) {
    if (!path.startsWith('/')) path = '/' + path;
    return this.baseUrl + path;
  },

  /**
   * Retrieves active session token
   */
  getToken() {
    return localStorage.getItem('recruit_token') || sessionStorage.getItem('recruit_token');
  },

  /**
   * Core request dispatcher with automatic JSON parsing and auth headers
   */
  async request(path, options = {}) {
    const url = this.endpoint(path);
    const token = this.getToken();

    const headers = {
      'Accept': 'application/json',
      ...(options.headers || {})
    };

    if (token) {
      headers['X-Session-Token'] = token;
      headers['Authorization'] = 'Bearer ' + token;
    }

    if (options.body && !(options.body instanceof FormData) && !headers['Content-Type']) {
      headers['Content-Type'] = 'application/json; charset=UTF-8';
    }

    const fetchConfig = {
      method: options.method || 'GET',
      headers: headers,
      ...options
    };

    try {
      const response = await fetch(url, fetchConfig);

      if (response.status === 401) {
        localStorage.removeItem('recruit_token');
        localStorage.removeItem('recruit_user');
        console.warn('[API] Authentication expired or invalid.');
      }

      const contentType = response.headers.get('content-type') || '';
      if (contentType.includes('application/json')) {
        const json = await response.json();
        return {
          ok: response.ok,
          status: response.status,
          ...json
        };
      } else {
        const text = await response.text();
        return {
          ok: response.ok,
          status: response.status,
          data: text
        };
      }
    } catch (err) {
      console.error('[API Error] ' + url + ':', err);
      return {
        ok: false,
        status: 0,
        success: false,
        message: 'Unable to connect to server. Please check your network or server status.'
      };
    }
  },

  // HTTP Verb wrappers
  async get(path, params = null) {
    let url = path;
    if (params) {
      const query = new URLSearchParams(params).toString();
      url += (url.includes('?') ? '&' : '?') + query;
    }
    return this.request(url, { method: 'GET' });
  },

  async post(path, body = {}) {
    const payload = (body instanceof FormData) ? body : JSON.stringify(body);
    return this.request(path, { method: 'POST', body: payload });
  },

  async put(path, body = {}) {
    const payload = (body instanceof FormData) ? body : JSON.stringify(body);
    return this.request(path, { method: 'PUT', body: payload });
  },

  async delete(path) {
    return this.request(path, { method: 'DELETE' });
  },

  /**
   * Health Check Telemetry
   * Calls GET /api/health
   */
  async checkHealth() {
    return this.get('/api/health');
  }
};

// Expose API globally
window.API = API;
