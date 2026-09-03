/**
 * Login Page Script
 */

document.addEventListener('DOMContentLoaded', () => {
  renderNavbar('login');

  // If already logged in, redirect to dashboard
  if (Auth.isLoggedIn()) {
    if (Auth.isApplicant()) {
      window.location.href = '/applicant/dashboard.html';
    } else if (Auth.isRecruiter()) {
      window.location.href = '/recruiter/dashboard.html';
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
    const res = await fetch('/api/login', {
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
        if (redirectUrl) {
          window.location.href = redirectUrl;
        } else if (data.data.role.toLowerCase() === 'applicant') {
          window.location.href = '/applicant/dashboard.html';
        } else if (data.data.role.toLowerCase() === 'recruiter') {
          window.location.href = '/recruiter/dashboard.html';
        } else {
          window.location.href = '/index.html';
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
