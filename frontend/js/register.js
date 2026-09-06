/**
 * Registration Page Script with Automated 6-Digit Email Verification
 */

let currentRole = 'applicant';
let pendingSession = null;
let currentRegisteredEmail = '';
let resendTimerInterval = null;

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

  // Setup OTP Modal Interactions
  setupOtpModal();
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
    const res = await fetch(apiEndpoint('/api/register'), {
      method: 'POST',
      body: formData // multipart/form-data
    });

    const data = await res.json();

    if (res.ok && data.success) {
      pendingSession = data.data;
      currentRegisteredEmail = email;
      showToast('Account created! Verification code sent to your email.', 'success');
      
      // Request verification code dispatch
      await requestOtpCode(email, 'REGISTRATION');
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
    const res = await fetch(apiEndpoint('/api/register'), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });

    const data = await res.json();

    if (res.ok && data.success) {
      pendingSession = data.data;
      currentRegisteredEmail = email;
      showToast('Account created! Verification code sent to your email.', 'success');
      
      // Request verification code dispatch
      await requestOtpCode(email, 'REGISTRATION');
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

/* =========================================================
   Automated 6-Digit OTP Verification Functions
   ========================================================= */

async function requestOtpCode(email, purpose, isResend = false) {
  try {
    const endpoint = isResend ? '/api/verify-email/resend' : '/api/verify-email/send';
    const res = await fetch(apiEndpoint(endpoint), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, purpose })
    });

    const data = await res.json();
    if (res.ok && data.success) {
      openOtpModal(email, data.data ? data.data.peekCode : null);
      startResendCooldown(data.data ? data.data.cooldownSeconds : 60);
    } else {
      showToast(data.message || 'Could not send verification code.', 'error');
      // Still open modal so user can input code if previously generated
      openOtpModal(email, null);
    }
  } catch (err) {
    console.error('OTP request error:', err);
    openOtpModal(email, null);
  }
}

function openOtpModal(email, peekCode) {
  const modal = document.getElementById('otp-modal-backdrop');
  const targetEmailEl = document.getElementById('otp-target-email');
  const demoBanner = document.getElementById('otp-demo-banner');
  const demoCodeEl = document.getElementById('otp-demo-code');

  if (targetEmailEl) targetEmailEl.textContent = email;

  if (peekCode) {
    if (demoBanner) demoBanner.style.display = 'block';
    if (demoCodeEl) demoCodeEl.textContent = peekCode;
  }

  if (modal) {
    modal.style.display = 'flex';
    clearOtpInputs();
    const firstInput = modal.querySelector('.otp-input[data-index="0"]');
    if (firstInput) setTimeout(() => firstInput.focus(), 100);
  }
}

function clearOtpInputs() {
  document.querySelectorAll('.otp-input').forEach(inp => {
    inp.value = '';
    inp.classList.remove('filled');
  });
}

function setupOtpModal() {
  const inputs = Array.from(document.querySelectorAll('.otp-input'));
  const btnVerify = document.getElementById('btn-verify-otp');
  const btnResend = document.getElementById('btn-resend-otp');
  const btnAutofill = document.getElementById('btn-autofill-otp');

  inputs.forEach((input, index) => {
    // Input navigation
    input.addEventListener('input', (e) => {
      const val = e.target.value.replace(/[^0-9]/g, '');
      e.target.value = val ? val[val.length - 1] : '';

      if (e.target.value) {
        input.classList.add('filled');
        if (index < inputs.length - 1) {
          inputs[index + 1].focus();
        }
      } else {
        input.classList.remove('filled');
      }

      // If all filled, auto-trigger verify
      checkAllFilled();
    });

    // Backspace navigation
    input.addEventListener('keydown', (e) => {
      if (e.key === 'Backspace' && !input.value && index > 0) {
        inputs[index - 1].focus();
      }
    });

    // Paste handling (auto fills 6 digits)
    input.addEventListener('paste', (e) => {
      e.preventDefault();
      const pasteData = (e.clipboardData || window.clipboardData).getData('text');
      const digits = pasteData.replace(/[^0-9]/g, '').slice(0, 6);
      if (digits) {
        digits.split('').forEach((d, i) => {
          if (inputs[i]) {
            inputs[i].value = d;
            inputs[i].classList.add('filled');
          }
        });
        const nextIndex = Math.min(digits.length, inputs.length - 1);
        inputs[nextIndex].focus();
        checkAllFilled();
      }
    });
  });

  if (btnAutofill) {
    btnAutofill.addEventListener('click', () => {
      const demoCodeEl = document.getElementById('otp-demo-code');
      if (demoCodeEl && demoCodeEl.textContent) {
        const code = demoCodeEl.textContent.trim();
        code.split('').forEach((digit, i) => {
          if (inputs[i]) {
            inputs[i].value = digit;
            inputs[i].classList.add('filled');
          }
        });
        checkAllFilled();
      }
    });
  }

  if (btnVerify) {
    btnVerify.addEventListener('click', submitOtpVerification);
  }

  if (btnResend) {
    btnResend.addEventListener('click', () => {
      if (btnResend.disabled) return;
      requestOtpCode(currentRegisteredEmail, 'REGISTRATION', true);
    });
  }

  function checkAllFilled() {
    const code = inputs.map(i => i.value).join('');
    if (code.length === 6) {
      submitOtpVerification();
    }
  }
}

async function submitOtpVerification() {
  const inputs = Array.from(document.querySelectorAll('.otp-input'));
  const code = inputs.map(i => i.value.trim()).join('');
  const btnVerify = document.getElementById('btn-verify-otp');

  if (code.length < 6) {
    showToast('Please enter the full 6-digit verification code.', 'error');
    return;
  }

  if (btnVerify) {
    btnVerify.disabled = true;
    btnVerify.textContent = 'Verifying...';
  }

  try {
    const res = await fetch(apiEndpoint('/api/verify-email/verify'), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        email: currentRegisteredEmail,
        code: code,
        purpose: 'REGISTRATION'
      })
    });

    const data = await res.json();

    if (res.ok && data.success) {
      showToast('Email verified successfully! Logging you in...', 'success');
      
      // Complete login session
      if (pendingSession && pendingSession.token) {
        Auth.setSession(pendingSession.token, pendingSession);
      }

      setTimeout(() => {
        const base = typeof getBasePath === 'function' ? getBasePath() : '';
        const role = (pendingSession && pendingSession.role) ? pendingSession.role : currentRole;
        if (role === 'recruiter') {
          window.location.href = base + 'recruiter/dashboard.html';
        } else {
          window.location.href = base + 'applicant/dashboard.html';
        }
      }, 1000);

    } else {
      showToast(data.message || 'Invalid or expired code. Please try again.', 'error');
      inputs.forEach(i => {
        i.value = '';
        i.classList.remove('filled');
      });
      if (inputs[0]) inputs[0].focus();
    }
  } catch (err) {
    console.error('Verification error:', err);
    showToast('Server error while verifying. Please try again.', 'error');
  } finally {
    if (btnVerify) {
      btnVerify.disabled = false;
      btnVerify.textContent = 'Verify & Continue';
    }
  }
}

function startResendCooldown(seconds) {
  const btnResend = document.getElementById('btn-resend-otp');
  const countdownEl = document.getElementById('resend-countdown');
  const secondsEl = document.getElementById('resend-seconds');

  if (!btnResend || !countdownEl || !secondsEl) return;

  clearInterval(resendTimerInterval);
  let remaining = seconds || 60;

  btnResend.disabled = true;
  btnResend.style.opacity = '0.5';
  btnResend.style.cursor = 'not-allowed';
  countdownEl.style.display = 'inline';
  secondsEl.textContent = remaining;

  resendTimerInterval = setInterval(() => {
    remaining--;
    secondsEl.textContent = remaining;
    if (remaining <= 0) {
      clearInterval(resendTimerInterval);
      btnResend.disabled = false;
      btnResend.style.opacity = '1';
      btnResend.style.cursor = 'pointer';
      countdownEl.style.display = 'none';
    }
  }, 1000);
}
