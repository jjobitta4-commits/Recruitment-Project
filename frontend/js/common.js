/**
 * Common JavaScript Utilities for Online Recruitment Management System
 * Pure Vanilla JavaScript (No frameworks)
 */

// Helper to determine relative base path for root vs subfolder
function getBasePath() {
  const path = window.location.pathname.replace(/\\/g, '/');
  if (path.includes('/applicant/') || path.includes('/recruiter/')) {
    return '../';
  }
  return '';
}

// ==========================================
// 1. Session Storage & Auth State
// ==========================================
const Auth = {
  getToken() {
    return localStorage.getItem('recruit_token');
  },
  getUser() {
    const raw = localStorage.getItem('recruit_user');
    try {
      return raw ? JSON.parse(raw) : null;
    } catch (e) {
      return null;
    }
  },
  setSession(token, user) {
    localStorage.setItem('recruit_token', token);
    localStorage.setItem('recruit_user', JSON.stringify(user));
  },
  clearSession() {
    localStorage.removeItem('recruit_token');
    localStorage.removeItem('recruit_user');
  },
  isLoggedIn() {
    return !!this.getToken();
  },
  isApplicant() {
    const user = this.getUser();
    return user && user.role && user.role.toLowerCase() === 'applicant';
  },
  isRecruiter() {
    const user = this.getUser();
    return user && user.role && user.role.toLowerCase() === 'recruiter';
  },
  requireAuth(expectedRole = null) {
    const base = getBasePath();
    if (!this.isLoggedIn()) {
      window.location.href = base + 'login.html?redirect=' + encodeURIComponent(window.location.pathname);
      return false;
    }
    if (expectedRole && this.getUser().role.toLowerCase() !== expectedRole.toLowerCase()) {
      if (this.isApplicant()) window.location.href = base + 'applicant/dashboard.html';
      else if (this.isRecruiter()) window.location.href = base + 'recruiter/dashboard.html';
      else window.location.href = base + 'login.html';
      return false;
    }
    return true;
  },
  logout() {
    const token = this.getToken();
    if (token) {
      fetch('/api/logout', {
        method: 'POST',
        headers: { 'X-Session-Token': token }
      }).catch(() => {});
    }
    this.clearSession();
    showToast('You have been logged out.', 'info');
    setTimeout(() => {
      window.location.href = getBasePath() + 'login.html';
    }, 500);
  }
};

// ==========================================
// 2. Fetch API Helper with Auth Headers
// ==========================================
async function authFetch(url, options = {}) {
  const token = Auth.getToken();
  const headers = options.headers || {};

  if (token) {
    headers['X-Session-Token'] = token;
    headers['Authorization'] = 'Bearer ' + token;
  }

  // If body is not FormData, set Content-Type JSON if not already set
  if (options.body && !(options.body instanceof FormData) && !headers['Content-Type']) {
    headers['Content-Type'] = 'application/json';
  }

  options.headers = headers;

  try {
    const res = await fetch(url, options);
    if (res.status === 401) {
      Auth.clearSession();
      showToast('Session expired. Please log in again.', 'error');
      setTimeout(() => {
        window.location.href = getBasePath() + 'login.html';
      }, 1000);
      throw new Error('Unauthorized');
    }
    const data = await res.json();
    return data;
  } catch (err) {
    if (err.message !== 'Unauthorized') {
      console.error('API Error:', err);
    }
    throw err;
  }
}

// ==========================================
// 3. Toast Notifications
// ==========================================
function showToast(message, type = 'success') {
  let container = document.getElementById('toast-container');
  if (!container) {
    container = document.createElement('div');
    container.id = 'toast-container';
    document.body.appendChild(container);
  }

  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;
  
  const icon = type === 'success' ? '✓' : (type === 'error' ? '✕' : 'ℹ');
  toast.innerHTML = `<span style="font-weight: bold; font-size: 1.1rem;">${icon}</span> <span>${message}</span>`;
  
  container.appendChild(toast);

  setTimeout(() => {
    toast.style.opacity = '0';
    toast.style.transform = 'translateX(50px)';
    toast.style.transition = 'all 0.3s ease';
    setTimeout(() => toast.remove(), 300);
  }, 4000);
}

// ==========================================
// 4. Modal Dialog Helpers
// ==========================================
function openModal(modalId) {
  const modal = document.getElementById(modalId);
  if (modal) {
    modal.classList.add('active');
  }
}

function closeModal(modalId) {
  const modal = document.getElementById(modalId);
  if (modal) {
    modal.classList.remove('active');
  }
}

// Close modal on overlay click
document.addEventListener('click', (e) => {
  if (e.target.classList.contains('modal-overlay')) {
    e.target.classList.remove('active');
  }
});

// ==========================================
// 5. Dynamic Navigation Bar Renderer
// ==========================================
function renderNavbar(activePage = '') {
  const navContainer = document.getElementById('global-navbar');
  if (!navContainer) return;

  const base = getBasePath();
  const user = Auth.getUser();
  const isLoggedIn = Auth.isLoggedIn();

  let navLinksHtml = `
    <li><a href="${base}index.html" class="${activePage === 'home' ? 'active' : ''}">Home</a></li>
    <li><a href="${base}jobs.html" class="${activePage === 'jobs' ? 'active' : ''}">Find Jobs</a></li>
  `;

  let userActionHtml = '';

  if (isLoggedIn && user) {
    const isApp = user.role.toLowerCase() === 'applicant';
    const notifPage = isApp ? `${base}applicant/notifications.html` : `${base}recruiter/notifications.html`;
    const dashPage = isApp ? `${base}applicant/dashboard.html` : `${base}recruiter/dashboard.html`;

    navLinksHtml += `
      <li><a href="${dashPage}" class="${activePage === 'dashboard' ? 'active' : ''}">Dashboard</a></li>
    `;

    if (isApp) {
      navLinksHtml += `
        <li><a href="${base}applicant/applications.html" class="${activePage === 'applications' ? 'active' : ''}">My Applications</a></li>
        <li><a href="${base}applicant/interviews.html" class="${activePage === 'interviews' ? 'active' : ''}">Interviews</a></li>
      `;
    } else {
      navLinksHtml += `
        <li><a href="${base}recruiter/post-job.html" class="${activePage === 'post-job' ? 'active' : ''}">Post Job</a></li>
        <li><a href="${base}recruiter/manage-jobs.html" class="${activePage === 'manage-jobs' ? 'active' : ''}">Manage Jobs</a></li>
        <li><a href="${base}recruiter/candidates.html" class="${activePage === 'candidates' ? 'active' : ''}">Candidates</a></li>
      `;
    }

    userActionHtml = `
      <a href="${notifPage}" class="notif-badge-btn" title="Notifications">
        🔔
        <span id="nav-notif-count" class="notif-count" style="display: none;">0</span>
      </a>
      <div class="user-badge">
        <span>👤 ${user.name || user.email}</span>
        <span style="font-size: 0.75rem; text-transform: uppercase; opacity: 0.8;">(${user.role})</span>
      </div>
      <button onclick="Auth.logout()" class="btn btn-secondary btn-sm" title="Log Out">Logout</button>
    `;

    // Fetch unread notifications count
    fetchUnreadNotificationsCount();
  } else {
    userActionHtml = `
      <a href="${base}login.html" class="btn btn-secondary btn-sm">Log In</a>
      <a href="${base}register.html" class="btn btn-primary btn-sm">Sign Up</a>
    `;
  }

  navContainer.innerHTML = `
    <nav class="navbar">
      <div class="container nav-container">
        <a href="${base}index.html" class="nav-brand">
          <div class="brand-badge">R</div>
          <span>RecruitHub</span>
        </a>
        <ul class="nav-links">
          ${navLinksHtml}
        </ul>
        <div class="nav-actions">
          ${userActionHtml}
        </div>
      </div>
    </nav>
  `;
}

// Fetch unread count for badge
async function fetchUnreadNotificationsCount() {
  if (!Auth.isLoggedIn()) return;
  try {
    const res = await authFetch('/api/notifications');
    if (res && res.success && res.data) {
      const badge = document.getElementById('nav-notif-count');
      const count = res.data.unreadCount || 0;
      if (badge) {
        if (count > 0) {
          badge.textContent = count > 99 ? '99+' : count;
          badge.style.display = 'flex';
        } else {
          badge.style.display = 'none';
        }
      }
    }
  } catch (e) {}
}

// ==========================================
// 6. Formatting Helpers & Badges
// ==========================================
function getStatusBadge(status) {
  if (!status) return '<span class="badge">Unknown</span>';
  const s = status.toLowerCase().replace(/\s+/g, '-');
  return `<span class="badge badge-${s}">${status}</span>`;
}

function getJobTypeBadge(type) {
  if (!type) return '<span class="badge">Full Time</span>';
  const t = type.toLowerCase().replace(/\s+/g, '-');
  return `<span class="badge badge-${t}">${type}</span>`;
}

function formatDate(dateStr) {
  if (!dateStr) return 'N/A';
  try {
    const d = new Date(dateStr);
    return d.toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' });
  } catch (e) {
    return dateStr;
  }
}
