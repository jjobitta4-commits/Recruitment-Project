/**
 * Login Page Script
 */

document.addEventListener('DOMContentLoaded', () => {
  renderNavbar('login');

  // If already logged in, redirect to dashboard
  if (Auth.isLoggedIn()) {
    const base = typeof getBasePath === 'function' ? getBasePath() : '';
    if (Auth.isApplicant()) {
      window.location.href = base + 'applicant/dashboard.html';
    } else if (Auth.isRecruiter()) {
      window.location.href = base + 'recruiter/dashboard.html';
    }
  }

  const loginForm = document.getElementById('login-form');
  if (loginForm) {
    loginForm.addEventListener('submit', handleLogin);
  }
});

function fillDemoAccount(email, password) {
  const emailInput = document.getElementById('email');
  const passInput = document.getElementById('password');
  if (emailInput && passInput) {
    emailInput.value = email;
    passInput.value = password;
    showToast(`Loaded demo credentials for: ${email}`, 'info');
  }
}

async function handleLogin(e) {
  e.preventDefault();
  const email = document.getElementById('email').value.trim();
  const password = document.getElementById('password').value.trim();
  const submitBtn = document.getElementById('login-submit-btn');

  if (!email || !password) {
    showToast('Please enter both email and password.', 'error');
    return;
  }

  if (submitBtn) {
    submitBtn.disabled = true;
    submitBtn.textContent = 'Logging in...';
  }

  try {
    const apiUrl = (window.location.protocol === 'file:') ? 'http://localhost:8080/api/login' : '/api/login';
    const res = await fetch(apiUrl, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password })
    });

    const data = await res.json();

    if (res.ok && data.success) {
      showToast(data.message || 'Login successful! Redirecting...', 'success');
      Auth.setSession(data.data.token, data.data);

      const params = new URLSearchParams(window.location.search);
      const redirectUrl = params.get('redirect');

      setTimeout(() => {
        const base = typeof getBasePath === 'function' ? getBasePath() : '';
        if (redirectUrl) {
          window.location.href = redirectUrl;
        } else if (data.data.role.toLowerCase() === 'applicant' || data.data.role.toLowerCase() === 'candidate') {
          window.location.href = base + 'applicant/dashboard.html';
        } else if (data.data.role.toLowerCase() === 'recruiter') {
          window.location.href = base + 'recruiter/dashboard.html';
        } else if (data.data.role.toLowerCase() === 'admin') {
          window.location.href = base + 'admin/dashboard.html';
        } else {
          window.location.href = base + 'index.html';
        }
      }, 700);

    } else {
      showToast(data.message || 'Invalid credentials.', 'error');
    }
  } catch (err) {
    console.error(err);
    showToast('Network error or server unreachable.', 'error');
  } finally {
    if (submitBtn) {
      submitBtn.disabled = false;
      submitBtn.textContent = 'Log In';
    }
  }
}
