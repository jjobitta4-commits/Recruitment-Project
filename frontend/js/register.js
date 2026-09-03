/**
 * Registration Page Script
 */

let currentRole = 'applicant';

document.addEventListener('DOMContentLoaded', () => {
  renderNavbar('register');

  // Tab switching
  const tabApplicant = document.getElementById('tab-applicant');
  const tabRecruiter = document.getElementById('tab-recruiter');
  const formApplicant = document.getElementById('form-applicant');
  const formRecruiter = document.getElementById('form-recruiter');

  if (tabApplicant && tabRecruiter) {
    tabApplicant.addEventListener('click', () => {
      currentRole = 'applicant';
      tabApplicant.classList.add('active');
      tabRecruiter.classList.remove('active');
      formApplicant.style.display = 'block';
      formRecruiter.style.display = 'none';
    });

    tabRecruiter.addEventListener('click', () => {
      currentRole = 'recruiter';
      tabRecruiter.classList.add('active');
      tabApplicant.classList.remove('active');
      formApplicant.style.display = 'none';
      formRecruiter.style.display = 'block';
    });
  }

  // Resume File Selection Display
  const resumeInput = document.getElementById('app-resume');
  const resumeFileName = document.getElementById('resume-file-name');
  if (resumeInput && resumeFileName) {
    resumeInput.addEventListener('change', (e) => {
      if (e.target.files && e.target.files[0]) {
        const file = e.target.files[0];
        if (!file.name.toLowerCase().endsWith('.pdf')) {
          showToast('Only PDF files are supported.', 'error');
          e.target.value = '';
          resumeFileName.textContent = 'Choose PDF resume file...';
          return;
        }
        if (file.size > 10 * 1024 * 1024) {
          showToast('File size must be under 10MB.', 'error');
          e.target.value = '';
          resumeFileName.textContent = 'Choose PDF resume file...';
          return;
        }
        resumeFileName.textContent = `Selected: ${file.name} (${(file.size / 1024).toFixed(1)} KB)`;
      } else {
        resumeFileName.textContent = 'Choose PDF resume file...';
      }
    });
  }

  // Form Submit Listeners
  if (formApplicant) {
    formApplicant.addEventListener('submit', handleApplicantRegister);
  }
  if (formRecruiter) {
    formRecruiter.addEventListener('submit', handleRecruiterRegister);
  }
});

async function handleApplicantRegister(e) {
  e.preventDefault();
  const form = e.target;
  const submitBtn = form.querySelector('button[type="submit"]');

  const fullName = document.getElementById('app-fullName').value.trim();
  const email = document.getElementById('app-email').value.trim();
  const password = document.getElementById('app-password').value.trim();

  if (!fullName || !email || !password) {
    showToast('Full Name, Email, and Password are required.', 'error');
    return;
  }

  const formData = new FormData(form);
  formData.append('role', 'applicant');

  if (submitBtn) {
    submitBtn.disabled = true;
    submitBtn.textContent = 'Creating Account...';
  }

  try {
    const res = await fetch('/api/register', {
      method: 'POST',
      body: formData // multipart/form-data
    });

    const data = await res.json();

    if (res.ok && data.success) {
      showToast('Registration successful! Logging you in...', 'success');
      Auth.setSession(data.data.token, data.data);
      setTimeout(() => {
        window.location.href = '/applicant/dashboard.html';
      }, 800);
    } else {
      showToast(data.message || 'Registration failed.', 'error');
    }
  } catch (err) {
    console.error(err);
    showToast('Network error or server unreachable.', 'error');
  } finally {
    if (submitBtn) {
      submitBtn.disabled = false;
      submitBtn.textContent = 'Complete Registration';
    }
  }
}

async function handleRecruiterRegister(e) {
  e.preventDefault();
  const form = e.target;
  const submitBtn = form.querySelector('button[type="submit"]');

  const recruiterName = document.getElementById('rec-name').value.trim();
  const companyName = document.getElementById('rec-company').value.trim();
  const email = document.getElementById('rec-email').value.trim();
  const password = document.getElementById('rec-password').value.trim();
  const phone = document.getElementById('rec-phone').value.trim();
  const country = document.getElementById('rec-country').value.trim();
  const companyDescription = document.getElementById('rec-desc').value.trim();

  if (!recruiterName || !companyName || !email || !password) {
    showToast('Recruiter Name, Company Name, Email, and Password are required.', 'error');
    return;
  }

  const payload = {
    role: 'recruiter',
    recruiterName,
    companyName,
    email,
    password,
    phone,
    country,
    companyDescription
  };

  if (submitBtn) {
    submitBtn.disabled = true;
    submitBtn.textContent = 'Creating Company Account...';
  }

  try {
    const res = await fetch('/api/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });

    const data = await res.json();

    if (res.ok && data.success) {
      showToast('Recruiter account created! Welcome aboard.', 'success');
      Auth.setSession(data.data.token, data.data);
      setTimeout(() => {
        window.location.href = '/recruiter/dashboard.html';
      }, 800);
    } else {
      showToast(data.message || 'Registration failed.', 'error');
    }
  } catch (err) {
    console.error(err);
    showToast('Network error or server unreachable.', 'error');
  } finally {
    if (submitBtn) {
      submitBtn.disabled = false;
      submitBtn.textContent = 'Register Recruiter Account';
    }
  }
}
