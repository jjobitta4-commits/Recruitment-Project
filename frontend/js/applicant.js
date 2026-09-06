/**
 * Applicant Portal Management Script
 */

document.addEventListener('DOMContentLoaded', () => {
  if (!Auth.requireAuth('applicant')) return;

  const path = window.location.pathname;

  if (path.includes('dashboard')) {
    renderNavbar('dashboard');
    loadApplicantDashboard();
  } else if (path.includes('profile')) {
    renderNavbar('profile');
    loadApplicantProfile();
  } else if (path.includes('applications')) {
    renderNavbar('applications');
    loadApplicantApplications();
  } else if (path.includes('interviews')) {
    renderNavbar('interviews');
    loadApplicantInterviews();
  } else if (path.includes('notifications')) {
    renderNavbar('notifications');
    loadApplicantNotifications();
  }
});

// ==========================================
// 1. Dashboard Overview
// ==========================================
async function loadApplicantDashboard() {
  const user = Auth.getUser();
  const welcomeEl = document.getElementById('applicant-welcome-name');
  if (welcomeEl && user) {
    welcomeEl.textContent = user.name || user.email;
  }

  // Fetch all widgets concurrently using Promise.allSettled so an issue in one never blocks the others
  const [statsSettled, appsSettled, notifsSettled] = await Promise.allSettled([
    authFetch('/api/applicant/stats'),
    authFetch('/api/applications'),
    authFetch('/api/notifications')
  ]);

  // 1. Overview Stats
  if (statsSettled.status === 'fulfilled' && statsSettled.value && statsSettled.value.success && statsSettled.value.data) {
    const s = statsSettled.value.data;
    const totalEl = document.getElementById('stat-total-apps');
    const shortEl = document.getElementById('stat-shortlisted');
    const selEl = document.getElementById('stat-selected');
    const ivsEl = document.getElementById('stat-interviews');
    if (totalEl) totalEl.textContent = s.totalApplications ?? 0;
    if (shortEl) shortEl.textContent = s.shortlistedCount ?? 0;
    if (selEl) selEl.textContent = s.selectedCount ?? 0;
    if (ivsEl) ivsEl.textContent = s.interviewsCount ?? 0;
  }

  // 2. Recent Applications
  if (appsSettled.status === 'fulfilled' && appsSettled.value && appsSettled.value.success && Array.isArray(appsSettled.value.data)) {
    renderRecentApplications(appsSettled.value.data.slice(0, 5));
  } else {
    const tbody = document.getElementById('recent-applications-tbody');
    if (tbody) {
      tbody.innerHTML = '<tr><td colspan="5" class="text-center text-muted" style="padding: 1.5rem;">You have not applied for any jobs yet. <a href="../jobs.html" class="btn btn-outline btn-sm" style="margin-left: 0.5rem;">Browse Jobs</a></td></tr>';
    }
  }

  // 3. Recent Notifications
  if (notifsSettled.status === 'fulfilled' && notifsSettled.value && notifsSettled.value.success && notifsSettled.value.data) {
    renderRecentNotifications(notifsSettled.value.data.notifications ? notifsSettled.value.data.notifications.slice(0, 4) : []);
  } else {
    const notifsContainer = document.getElementById('recent-notifications-list');
    if (notifsContainer) {
      notifsContainer.innerHTML = '<p class="text-muted text-center" style="padding: 1rem;">No recent notifications.</p>';
    }
  }

  // 4. Smart Matched Vacancies
  loadRecommendedMatches();
}

async function loadRecommendedMatches() {
  const tbody = document.getElementById('recommended-matches-tbody');
  if (!tbody) return;

  try {
    const res = await authFetch('/api/match?action=ranked-jobs');
    if (res && res.success && Array.isArray(res.data)) {
      const jobs = res.data.slice(0, 5);
      if (jobs.length === 0) {
        tbody.innerHTML = '<tr><td colspan="4" class="text-center text-muted" style="padding: 1.5rem;">No recommendations generated yet. Complete your profile and skills to boost algorithmic matches! <a href="profile.html" class="btn btn-outline btn-sm" style="margin-left: 0.5rem;">Update Profile</a></td></tr>';
        return;
      }

      tbody.innerHTML = jobs.map(j => {
        const score = j.matchScore || 0;
        let badgeStyle = 'background: #dcfce7; color: #166534;';
        if (score < 50) badgeStyle = 'background: #fee2e2; color: #991b1b;';
        else if (score < 70) badgeStyle = 'background: #fef3c7; color: #92400e;';
        else if (score < 85) badgeStyle = 'background: #e0f2fe; color: #0369a1;';

        return `
          <tr>
            <td>
              <strong style="font-size: 0.95rem; color: #0f172a;">${j.title}</strong><br>
              <span style="font-size: 0.8rem; color: #64748b;">🏢 ${j.company}</span>
            </td>
            <td>
              <span class="badge" style="${badgeStyle} font-weight: 800; font-size: 0.85rem;">
                ⭐ ${score}% &bull; ${j.matchLevel || 'MATCH'}
              </span>
            </td>
            <td>
              📍 ${j.location || 'Remote'}<br>
              <small style="color: #64748b;">💼 ${j.jobType || 'Full Time'}</small>
            </td>
            <td>
              <div class="flex gap-1">
                <button onclick="openSkillGapModal(${j.jobId})" class="btn btn-outline btn-sm" style="border-color: #8b5cf6; color: #7c3aed;">
                  📊 Skill Gap &amp; Roadmap
                </button>
                <a href="../jobs.html" class="btn btn-primary btn-sm">Apply</a>
              </div>
            </td>
          </tr>
        `;
      }).join('');
    } else {
      tbody.innerHTML = '<tr><td colspan="4" class="text-center text-muted" style="padding: 1.5rem;">Explore all available vacancies in the jobs catalog. <a href="../jobs.html" class="btn btn-primary btn-sm" style="margin-left: 0.5rem;">Explore Jobs</a></td></tr>';
    }
  } catch (err) {
    console.error('Failed to load recommended matches:', err);
    tbody.innerHTML = '<tr><td colspan="4" class="text-center text-muted" style="padding: 1.5rem;">Explore all available vacancies in the jobs catalog. <a href="../jobs.html" class="btn btn-primary btn-sm" style="margin-left: 0.5rem;">Explore Jobs</a></td></tr>';
  }
}

function renderRecentApplications(apps) {
  const tbody = document.getElementById('recent-applications-tbody');
  if (!tbody) return;

  if (!apps || apps.length === 0) {
    tbody.innerHTML = '<tr><td colspan="5" class="text-center text-muted" style="padding: 1.5rem;">You have not applied for any jobs yet. <a href="../jobs.html" class="btn btn-outline btn-sm" style="margin-left: 0.5rem;">Browse Jobs</a></td></tr>';
    return;
  }

  tbody.innerHTML = apps.map(a => `
    <tr>
      <td><strong>${a.jobTitle}</strong></td>
      <td>🏢 ${a.company}</td>
      <td>${formatDate(a.appliedDate)}</td>
      <td>${getStatusBadge(a.status)}</td>
      <td>
        <a href="applications.html" class="btn btn-secondary btn-sm">View Details</a>
      </td>
    </tr>
  `).join('');
}

function renderRecentNotifications(notifs) {
  const container = document.getElementById('recent-notifications-list');
  if (!container) return;

  if (notifs.length === 0) {
    container.innerHTML = '<p class="text-muted text-center" style="padding: 1rem;">No recent notifications.</p>';
    return;
  }

  container.innerHTML = notifs.map(n => `
    <div style="padding: 0.85rem; border-bottom: 1px solid #f1f5f9; display: flex; justify-content: space-between; align-items: start;">
      <div>
        <div style="font-weight: 600; font-size: 0.92rem; color: #0f172a;">${n.title}</div>
        <div style="font-size: 0.85rem; color: #475569; margin-top: 0.2rem;">${n.message}</div>
      </div>
      <span style="font-size: 0.75rem; color: #94a3b8; white-space: nowrap; margin-left: 0.75rem;">${formatDate(n.createdAt)}</span>
    </div>
  `).join('');
}

// ==========================================
// 2. Profile Management
// ==========================================
async function loadApplicantProfile() {
  try {
    const res = await authFetch('/api/applicant/profile');
    if (res.success && res.data) {
      const p = res.data;
      document.getElementById('prof-fullName').value = p.fullName || '';
      document.getElementById('prof-email').value = p.email || '';
      document.getElementById('prof-phone').value = p.phone || '';
      if (p.dob) document.getElementById('prof-dob').value = p.dob;
      document.getElementById('prof-gender').value = p.gender || '';
      document.getElementById('prof-address').value = p.address || '';
      document.getElementById('prof-city').value = p.city || '';
      document.getElementById('prof-country').value = p.country || '';
      document.getElementById('prof-education').value = p.education || '';
      document.getElementById('prof-university').value = p.university || '';
      if (p.graduationYear) document.getElementById('prof-gradYear').value = p.graduationYear;
      document.getElementById('prof-skills').value = p.skills || '';
      document.getElementById('prof-exp').value = p.experienceYears || 0;
      document.getElementById('prof-salary').value = p.expectedSalary || '';
      document.getElementById('prof-linkedin').value = p.linkedinUrl || '';
      document.getElementById('prof-github').value = p.githubUrl || '';
      document.getElementById('prof-leetcode').value = p.leetcodeUrl || '';

      // Resume display
      const resumeContainer = document.getElementById('current-resume-display');
      if (resumeContainer) {
        if (p.resumeFile) {
          resumeContainer.innerHTML = `
            <div style="background: #eff6ff; border: 1px solid #bfdbfe; border-radius: 8px; padding: 0.85rem 1.25rem; display: flex; align-items: center; justify-content: space-between;">
              <div style="display: flex; align-items: center; gap: 0.6rem;">
                <span style="font-size: 1.4rem;">📄</span>
                <div>
                  <div style="font-weight: 600; color: #1e40af;">${p.resumeFile}</div>
                  <div style="font-size: 0.78rem; color: #3b82f6;">PDF Resume on File</div>
                </div>
              </div>
              <a href="/uploads/resumes/${p.resumeFile}" target="_blank" class="btn btn-outline btn-sm">Preview / Download PDF</a>
            </div>
          `;
        } else {
          resumeContainer.innerHTML = '<p class="text-muted" style="font-size: 0.9rem;">No resume uploaded yet. Upload a PDF below.</p>';
        }
      }
    }

    const form = document.getElementById('applicant-profile-form');
    if (form) {
      form.addEventListener('submit', handleUpdateProfile);
    }

    const resumeForm = document.getElementById('applicant-resume-form');
    if (resumeForm) {
      resumeForm.addEventListener('submit', handleUploadResume);
    }

  } catch (err) {
    console.error('Failed to load profile:', err);
  }
}

async function handleUpdateProfile(e) {
  e.preventDefault();
  const form = e.target;
  const submitBtn = form.querySelector('button[type="submit"]');

  const payload = {
    fullName: document.getElementById('prof-fullName').value.trim(),
    phone: document.getElementById('prof-phone').value.trim(),
    dob: document.getElementById('prof-dob').value || null,
    gender: document.getElementById('prof-gender').value,
    address: document.getElementById('prof-address').value.trim(),
    city: document.getElementById('prof-city').value.trim(),
    country: document.getElementById('prof-country').value.trim(),
    education: document.getElementById('prof-education').value.trim(),
    university: document.getElementById('prof-university').value.trim(),
    graduationYear: document.getElementById('prof-gradYear').value ? parseInt(document.getElementById('prof-gradYear').value) : null,
    skills: document.getElementById('prof-skills').value.trim(),
    experienceYears: document.getElementById('prof-exp').value ? parseInt(document.getElementById('prof-exp').value) : 0,
    expectedSalary: document.getElementById('prof-salary').value.trim(),
    linkedinUrl: document.getElementById('prof-linkedin').value.trim(),
    githubUrl: document.getElementById('prof-github').value.trim(),
    leetcodeUrl: document.getElementById('prof-leetcode').value.trim()
  };

  if (submitBtn) {
    submitBtn.disabled = true;
    submitBtn.textContent = 'Saving Changes...';
  }

  try {
    const res = await authFetch('/api/applicant/profile', {
      method: 'PUT',
      body: JSON.stringify(payload)
    });

    if (res.success) {
      showToast('Profile updated successfully!', 'success');
      // Update session storage name
      const user = Auth.getUser();
      user.name = payload.fullName;
      localStorage.setItem('recruit_user', JSON.stringify(user));
      renderNavbar('profile');
    } else {
      showToast(res.message || 'Failed to update profile.', 'error');
    }
  } catch (err) {
    showToast('Server error while saving profile.', 'error');
  } finally {
    if (submitBtn) {
      submitBtn.disabled = false;
      submitBtn.textContent = 'Save Profile Changes';
    }
  }
}

async function handleUploadResume(e) {
  e.preventDefault();
  const input = document.getElementById('prof-resume-input');
  if (!input || !input.files || !input.files[0]) {
    showToast('Please select a PDF file first.', 'error');
    return;
  }

  const file = input.files[0];
  if (!file.name.toLowerCase().endsWith('.pdf')) {
    showToast('Only PDF files are supported.', 'error');
    return;
  }

  const formData = new FormData();
  formData.append('resume', file);

  const btn = e.target.querySelector('button[type="submit"]');
  if (btn) {
    btn.disabled = true;
    btn.textContent = 'Uploading...';
  }

  try {
    const resumeUrl = typeof apiEndpoint === 'function' ? apiEndpoint('/api/applicant/resume') : '/api/applicant/resume';
    const res = await fetch(resumeUrl, {
      method: 'POST',
      headers: {
        'X-Session-Token': Auth.getToken(),
        'Authorization': 'Bearer ' + Auth.getToken()
      },
      body: formData
    });
    const data = await res.json();
    if (res.ok && data.success) {
      showToast('Resume PDF uploaded successfully!', 'success');
      loadApplicantProfile();
    } else {
      showToast(data.message || 'Upload failed.', 'error');
    }
  } catch (err) {
    showToast('Error uploading resume.', 'error');
  } finally {
    if (btn) {
      btn.disabled = false;
      btn.textContent = 'Upload New Resume';
    }
  }
}

// ==========================================
// 3. Applications Tracker
// ==========================================
async function loadApplicantApplications() {
  const tbody = document.getElementById('applications-table-tbody');
  if (!tbody) return;

  try {
    const res = await authFetch('/api/applications');
    if (res.success && Array.isArray(res.data)) {
      if (res.data.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" class="text-center text-muted" style="padding: 2.5rem;">No submitted applications yet. <a href="../jobs.html" class="btn btn-outline btn-sm" style="margin-left: 0.5rem;">Search Jobs</a></td></tr>';
        return;
      }

      tbody.innerHTML = res.data.map(a => {
        const isInterview = a.status === 'Interview Scheduled' || a.status === 'Interview_Scheduled';
        const canWithdraw = a.status === 'Applied' || a.status === 'Under Review' || a.status === 'Under_Review';
        return `
        <tr>
          <td>
            <strong>${a.jobTitle}</strong>
            ${a.matchScore ? `<div style="margin-top: 4px;">${renderApplicantMatchBadge(a.matchScore, a.matchLevel)}</div>` : ''}
          </td>
          <td>🏢 ${a.company}</td>
          <td>📍 ${a.jobLocation || 'Remote'}</td>
          <td>${formatDate(a.appliedDate)}</td>
          <td>${getStatusBadge(a.status)}</td>
          <td>
            <div class="flex gap-1">
              ${isInterview ? `
                <a href="../interview-room.html" class="btn btn-sm btn-primary" style="background: #10b981; border-color: #10b981;" title="Join Virtual Technical Room">
                  💻 Room
                </a>
              ` : ''}
              ${canWithdraw ? `
                <button onclick="withdrawApplication(${a.applicationId}, '${encodeURIComponent(a.jobTitle)}')" class="btn btn-sm" style="background: #fff; color: #ef4444; border: 1px solid #fca5a5;" title="Withdraw Application">✕ Withdraw</button>
              ` : ''}
              ${a.resumePath ? `<a href="/uploads/resumes/${a.resumePath}" target="_blank" class="btn btn-outline btn-sm">Resume</a>` : ''}
              <button onclick="viewApplicationCoverLetter('${encodeURIComponent(a.coverLetter || '')}', '${encodeURIComponent(a.jobTitle)}', '${encodeURIComponent(a.company)}')" class="btn btn-secondary btn-sm">Cover Letter</button>
            </div>
          </td>
        </tr>
        <tr style="background: #f8fafc;">
          <td colspan="6" style="padding: 0.85rem 1.5rem 1.25rem; border-bottom: 2px solid #e2e8f0;">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.25rem;">
              <span style="font-weight: 700; font-size: 0.78rem; color: #475569; text-transform: uppercase; letter-spacing: 0.5px;">Hiring Pipeline Status</span>
              ${a.matchedSkills && a.matchedSkills.length > 0 ? `
                <span style="font-size: 0.8rem; color: #065f46;">
                  Skill Match: ${a.matchedSkills.slice(0, 4).map(s => `<span class="badge skill-tag-matched" style="font-size: 0.72rem; padding: 1px 6px; margin-left: 3px;">✓ ${s}</span>`).join('')}
                </span>
              ` : ''}
            </div>
            ${renderPipelineStepper(a.status)}
          </td>
        </tr>
      `;
      }).join('');
    } else {
      tbody.innerHTML = '<tr><td colspan="6" class="text-center text-muted" style="padding: 2.5rem;">No applications found. <a href="../jobs.html" class="btn btn-outline btn-sm" style="margin-left: 0.5rem;">Browse Jobs</a></td></tr>';
    }
  } catch (err) {
    tbody.innerHTML = '<tr><td colspan="6" class="text-center text-muted" style="padding: 2.5rem;">Could not load applications. <a href="../jobs.html" class="btn btn-outline btn-sm" style="margin-left: 0.5rem;">Browse Jobs</a></td></tr>';
  }
}

async function withdrawApplication(applicationId, jobTitle) {
  const decodedTitle = decodeURIComponent(jobTitle);
  if (!confirm(`Are you sure you want to withdraw your application for "${decodedTitle}"? This action cannot be undone.`)) {
    return;
  }
  try {
    const res = await authFetch(`/api/applications?applicationId=${applicationId}`, {
      method: 'DELETE'
    });
    if (res.success) {
      if (typeof showToast === 'function') {
        showToast('Application withdrawn successfully', 'success');
      } else {
        alert('Application withdrawn successfully');
      }
      loadApplicantApplications();
    } else {
      alert(res.message || 'Failed to withdraw application');
    }
  } catch (err) {
    alert('Error withdrawing application: ' + err.message);
  }
}

function viewApplicationCoverLetter(encLetter, encTitle, encCompany) {
  const letter = decodeURIComponent(encLetter);
  const title = decodeURIComponent(encTitle);
  const comp = decodeURIComponent(encCompany);

  const titleEl = document.getElementById('cl-modal-title');
  const bodyEl = document.getElementById('cl-modal-body');
  if (titleEl) titleEl.textContent = `Cover Letter: ${title} (${comp})`;
  if (bodyEl) bodyEl.innerHTML = letter ? `<p style="white-space: pre-line;">${letter}</p>` : '<p class="text-muted">No cover letter was submitted with this application.</p>';

  openModal('cover-letter-modal');
}

// ==========================================
// 4. Interviews Tracker
// ==========================================
async function loadApplicantInterviews() {
  const container = document.getElementById('interviews-list-container');
  if (!container) return;

  try {
    const res = await authFetch('/api/interviews');
    if (res.success && Array.isArray(res.data)) {
      if (res.data.length === 0) {
        container.innerHTML = `
          <div style="text-align: center; padding: 3rem; background: #fff; border-radius: 10px; border: 1px solid #e2e8f0;">
            <h3>No Scheduled Interviews</h3>
            <p class="text-muted">When a recruiter shortlists your profile and schedules an interview, it will appear here.</p>
          </div>
        `;
        return;
      }

      container.innerHTML = res.data.map(iv => `
        <div class="card mb-3" style="border-left: 4px solid var(--primary);">
          <div class="flex justify-between items-center mb-2">
            <div>
              <h3 style="margin-bottom: 0.25rem;">${iv.jobTitle}</h3>
              <div class="job-company" style="font-size: 1rem;">🏢 ${iv.company}</div>
            </div>
            <span class="badge ${iv.status === 'Scheduled' ? 'badge-interview-scheduled' : (iv.status === 'Completed' ? 'badge-selected' : 'badge-rejected')}">${iv.status}</span>
          </div>

          <div class="stats-grid mb-3" style="grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));">
            <div style="background: #f8fafc; padding: 0.65rem 1rem; border-radius: 6px; border: 1px solid #e2e8f0;">
              <div class="text-muted" style="font-size: 0.78rem;">Date</div>
              <div class="font-bold">📅 ${formatDate(iv.interviewDate)}</div>
            </div>
            <div style="background: #f8fafc; padding: 0.65rem 1rem; border-radius: 6px; border: 1px solid #e2e8f0;">
              <div class="text-muted" style="font-size: 0.78rem;">Time</div>
              <div class="font-bold">⏰ ${iv.interviewTime}</div>
            </div>
            <div style="background: #f8fafc; padding: 0.65rem 1rem; border-radius: 6px; border: 1px solid #e2e8f0;">
              <div class="text-muted" style="font-size: 0.78rem;">Format</div>
              <div class="font-bold">${iv.interviewType === 'Online' ? '🌐 Virtual Technical Room' : '🏢 In-Person Office'}</div>
            </div>
          </div>

          ${iv.notes ? `
            <div style="background: #fffbeb; border: 1px solid #fde68a; border-radius: 8px; padding: 0.75rem 1rem; margin-bottom: 1rem; font-size: 0.9rem; color: #92400e;">
              <strong>Recruiter Notes:</strong> ${iv.notes}
            </div>
          ` : ''}

          <div class="flex justify-between items-center" style="flex-wrap: wrap; gap: 0.5rem;">
            <div style="font-size: 0.8rem; color: #94a3b8;">Scheduled on ${formatDate(iv.createdAt)}</div>
            <div class="flex gap-1">
              <a href="../interview-room.html?id=${iv.interviewId}" class="btn btn-primary btn-sm" style="background: #10b981; border-color: #10b981;">
                💻 Open Virtual Interview Room
              </a>
              ${iv.meetingLink && !iv.meetingLink.includes('interview-room') ? `
                <a href="${iv.meetingLink}" target="_blank" class="btn btn-outline btn-sm">
                  🔗 External Link
                </a>
              ` : ''}
            </div>
          </div>
        </div>
      `).join('');
    } else {
      container.innerHTML = '<p class="text-muted text-center" style="padding: 2rem;">No scheduled interviews at this time.</p>';
    }
  } catch (err) {
    container.innerHTML = '<p class="text-danger text-center">Failed to load interviews.</p>';
  }
}

// ==========================================
// 5. Notifications Center
// ==========================================
async function loadApplicantNotifications() {
  const container = document.getElementById('notifications-list-container');
  if (!container) return;

  try {
    const res = await authFetch('/api/notifications');
    if (res.success && res.data) {
      const list = res.data.notifications || [];
      if (list.length === 0) {
        container.innerHTML = '<div class="card text-center text-muted" style="padding: 3rem;"><h3>No Notifications</h3><p>You have no recent alerts or messages.</p></div>';
        return;
      }

      container.innerHTML = list.map(n => `
        <div class="card mb-2" style="background: ${n.isRead ? '#ffffff' : '#f0f9ff'}; border-left: 4px solid ${n.isRead ? '#cbd5e1' : 'var(--primary)'};">
          <div class="flex justify-between items-center">
            <h4 style="color: ${n.isRead ? '#334155' : 'var(--primary-dark)'};">${n.title}</h4>
            <div class="flex items-center gap-2">
              <span style="font-size: 0.8rem; color: #94a3b8;">${formatDate(n.createdAt)}</span>
              ${!n.isRead ? `<button onclick="markNotificationRead(${n.notificationId})" class="btn btn-secondary btn-sm" style="font-size: 0.75rem; padding: 0.2rem 0.6rem;">Mark Read</button>` : '<span style="font-size: 0.75rem; color: #64748b;">Read ✓</span>'}
            </div>
          </div>
          <p style="margin-top: 0.4rem; color: #475569; font-size: 0.95rem;">${n.message}</p>
        </div>
      `).join('');
    }
  } catch (err) {
    container.innerHTML = '<p class="text-danger text-center">Failed to load notifications.</p>';
  }
}

async function markNotificationRead(id) {
  try {
    await authFetch('/api/notifications/read', {
      method: 'PUT',
      body: JSON.stringify({ notificationId: id })
    });
    loadApplicantNotifications();
    fetchUnreadNotificationsCount();
  } catch (err) {
    console.error(err);
  }
}

async function markAllNotificationsRead() {
  try {
    await authFetch('/api/notifications/read-all', {
      method: 'PUT'
    });
    showToast('All notifications marked as read', 'success');
    loadApplicantNotifications();
    fetchUnreadNotificationsCount();
  } catch (err) {
    console.error(err);
  }
}

function renderPipelineStepper(status) {
  const s = (status || 'Applied').toLowerCase();
  
  let step1Class = 'completed';
  let step2Class = '';
  let step3Class = '';
  let step4Class = '';
  let step5Class = '';

  if (s === 'applied') {
    step1Class = 'active';
  } else if (s === 'under review') {
    step1Class = 'completed';
    step2Class = 'active';
  } else if (s === 'shortlisted') {
    step1Class = 'completed';
    step2Class = 'completed';
    step3Class = 'active';
  } else if (s === 'interview scheduled') {
    step1Class = 'completed';
    step2Class = 'completed';
    step3Class = 'active';
  } else if (s === 'selected') {
    step1Class = 'completed';
    step2Class = 'completed';
    step3Class = 'completed';
    step4Class = 'completed';
    step5Class = 'completed';
  } else if (s === 'rejected') {
    step1Class = 'completed';
    step5Class = 'rejected';
  }

  return `
    <div class="pipeline-stepper">
      <div class="pipeline-step ${step1Class}">
        <div class="step-circle">${step1Class === 'completed' ? '✓' : '1'}</div>
        <div class="step-label">Applied</div>
      </div>
      <div class="pipeline-step ${step2Class}">
        <div class="step-circle">${step2Class === 'completed' ? '✓' : '2'}</div>
        <div class="step-label">Screening</div>
      </div>
      <div class="pipeline-step ${step3Class}">
        <div class="step-circle">${step3Class === 'completed' ? '✓' : '3'}</div>
        <div class="step-label">Interview</div>
      </div>
      <div class="pipeline-step ${step4Class}">
        <div class="step-circle">${step4Class === 'completed' ? '✓' : '4'}</div>
        <div class="step-label">Evaluation</div>
      </div>
      <div class="pipeline-step ${step5Class}">
        <div class="step-circle">${step5Class === 'completed' ? '✓' : (step5Class === 'rejected' ? '✕' : '5')}</div>
        <div class="step-label">${step5Class === 'rejected' ? 'Declined' : 'Decision / Offer'}</div>
      </div>
    </div>
  `;
}

function renderApplicantMatchBadge(score, level) {
  if (score === undefined || score === null || score <= 0) return '';
  const lvl = (level || 'MODERATE').toLowerCase();
  return `
    <span class="match-badge ${lvl}">
      <span class="match-badge-dot"></span>
      ${score}% AI Match (${level || 'Good'})
    </span>
  `;
}
