/**
 * Recruiter Portal Management Script
 */

let recruiterJobs = [];
let allApplications = [];

document.addEventListener('DOMContentLoaded', () => {
  if (!Auth.requireAuth('recruiter')) return;

  const path = window.location.pathname;

  if (path.includes('dashboard')) {
    renderNavbar('dashboard');
    loadRecruiterDashboard();
  } else if (path.includes('post-job')) {
    renderNavbar('post-job');
    initPostJobPage();
  } else if (path.includes('manage-jobs')) {
    renderNavbar('manage-jobs');
    loadManageJobs();
  } else if (path.includes('candidates')) {
    renderNavbar('candidates');
    loadCandidatesPage();
  } else if (path.includes('interviews')) {
    renderNavbar('interviews');
    loadRecruiterInterviews();
  } else if (path.includes('notifications')) {
    renderNavbar('notifications');
    loadRecruiterNotifications();
  }
});

// ==========================================
// 1. Recruiter Dashboard
// ==========================================
async function loadRecruiterDashboard() {
  const user = Auth.getUser();
  const welcomeEl = document.getElementById('recruiter-welcome-name');
  if (welcomeEl && user) {
    welcomeEl.textContent = user.name || user.email;
  }

  try {
    const [statsRes, appsRes, notifsRes] = await Promise.all([
      authFetch('/api/recruiter/stats'),
      authFetch('/api/applications'),
      authFetch('/api/notifications')
    ]);

    if (statsRes.success && statsRes.data) {
      document.getElementById('stat-rec-total-jobs').textContent = statsRes.data.totalJobs || 0;
      document.getElementById('stat-rec-active-jobs').textContent = statsRes.data.activeJobs || 0;
      document.getElementById('stat-rec-total-apps').textContent = statsRes.data.totalApplications || 0;
      document.getElementById('stat-rec-shortlisted').textContent = statsRes.data.shortlistedCandidates || 0;
      document.getElementById('stat-rec-selected').textContent = statsRes.data.selectedCandidates || 0;
    }

    if (appsRes.success && Array.isArray(appsRes.data)) {
      renderRecentCandidates(appsRes.data.slice(0, 5));
    }

    if (notifsRes.success && notifsRes.data) {
      renderRecentRecruiterNotifs(notifsRes.data.notifications ? notifsRes.data.notifications.slice(0, 4) : []);
    }
  } catch (err) {
    console.error('Failed to load recruiter dashboard:', err);
  }
}

function renderRecentCandidates(apps) {
  const tbody = document.getElementById('recent-candidates-tbody');
  if (!tbody) return;

  if (apps.length === 0) {
    tbody.innerHTML = '<tr><td colspan="5" class="text-center text-muted">No candidate applications received yet. <a href="/recruiter/post-job.html">Post a Job</a></td></tr>';
    return;
  }

  tbody.innerHTML = apps.map(a => `
    <tr>
      <td><strong>${a.applicantName}</strong><br><span style="font-size: 0.8rem; color: #64748b;">${a.applicantEmail}</span></td>
      <td>💼 ${a.jobTitle}</td>
      <td>${formatDate(a.appliedDate)}</td>
      <td>${getStatusBadge(a.status)}</td>
      <td>
        <a href="/recruiter/candidates.html" class="btn btn-secondary btn-sm">Review</a>
      </td>
    </tr>
  `).join('');
}

function renderRecentRecruiterNotifs(notifs) {
  const container = document.getElementById('recent-recruiter-notifs');
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
// 2. Post Job
// ==========================================
function initPostJobPage() {
  const form = document.getElementById('post-job-form');
  if (form) {
    form.addEventListener('submit', handlePostJobSubmit);
  }
}

async function handlePostJobSubmit(e) {
  e.preventDefault();
  const form = e.target;
  const submitBtn = form.querySelector('button[type="submit"]');

  const payload = {
    title: document.getElementById('job-title').value.trim(),
    company: document.getElementById('job-company').value.trim(),
    jobType: document.getElementById('job-type').value,
    vacancies: parseInt(document.getElementById('job-vacancies').value) || 1,
    location: document.getElementById('job-location').value.trim(),
    country: document.getElementById('job-country').value.trim(),
    salaryRange: document.getElementById('job-salary').value.trim(),
    experienceRequired: document.getElementById('job-experience').value.trim(),
    educationRequired: document.getElementById('job-education').value.trim(),
    deadline: document.getElementById('job-deadline').value || null,
    skillsRequired: document.getElementById('job-skills').value.trim(),
    description: document.getElementById('job-description').value.trim()
  };

  if (!payload.title || !payload.description || !payload.skillsRequired) {
    showToast('Job Title, Skills, and Description are required.', 'error');
    return;
  }

  if (submitBtn) {
    submitBtn.disabled = true;
    submitBtn.textContent = 'Publishing Opening...';
  }

  try {
    const res = await authFetch('/api/jobs', {
      method: 'POST',
      body: JSON.stringify(payload)
    });

    if (res.success) {
      showToast('🎉 Job posted successfully! Candidates can now apply.', 'success');
      setTimeout(() => {
        window.location.href = '/recruiter/manage-jobs.html';
      }, 800);
    } else {
      showToast(res.message || 'Failed to post job.', 'error');
    }
  } catch (err) {
    showToast('Error communicating with server.', 'error');
  } finally {
    if (submitBtn) {
      submitBtn.disabled = false;
      submitBtn.textContent = 'Publish Job Vacancy';
    }
  }
}

// ==========================================
// 3. Manage Jobs
// ==========================================
async function loadManageJobs() {
  const tbody = document.getElementById('manage-jobs-tbody');
  if (!tbody) return;

  try {
    const res = await authFetch('/api/jobs/recruiter');
    if (res.success && Array.isArray(res.data)) {
      recruiterJobs = res.data;

      if (recruiterJobs.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted" style="padding: 2.5rem;">You have not posted any jobs yet. <a href="/recruiter/post-job.html">Post your first job</a></td></tr>';
        return;
      }

      tbody.innerHTML = recruiterJobs.map(j => `
        <tr>
          <td><strong>${j.title}</strong><br><span style="font-size: 0.8rem; color: #64748b;">${j.company}</span></td>
          <td>${getJobTypeBadge(j.jobType)}</td>
          <td>📍 ${j.location || 'Remote'}</td>
          <td><span class="badge" style="background: #e0f2fe; color: #0369a1; font-weight: bold;">👥 ${j.applicationCount || 0} Apps</span></td>
          <td>${formatDate(j.postedDate)}</td>
          <td>
            <button onclick="toggleJobStatus(${j.jobId}, '${j.status}')" class="badge ${j.status === 'Active' ? 'badge-active' : 'badge-closed'}" style="cursor: pointer; border: none;" title="Click to Toggle Status">
              ${j.status} 🔄
            </button>
          </td>
          <td>
            <div class="flex gap-1">
              <button onclick="openEditJobModal(${j.jobId})" class="btn btn-secondary btn-sm" title="Edit Job Details">✏️ Edit</button>
              <button onclick="handleDeleteJob(${j.jobId})" class="btn btn-danger btn-sm" title="Delete Job">🗑️</button>
            </div>
          </td>
        </tr>
      `).join('');
    }
  } catch (err) {
    tbody.innerHTML = '<tr><td colspan="7" class="text-center text-danger">Failed to load jobs.</td></tr>';
  }

  // Hook edit form submit
  const editForm = document.getElementById('edit-job-form');
  if (editForm) {
    editForm.addEventListener('submit', handleUpdateJobSubmit);
  }
}

async function toggleJobStatus(jobId, currentStatus) {
  const newStatus = currentStatus === 'Active' ? 'Closed' : 'Active';
  try {
    const res = await authFetch('/api/jobs/status', {
      method: 'PUT',
      body: JSON.stringify({ jobId, status: newStatus })
    });
    if (res.success) {
      showToast(`Job marked as ${newStatus}`, 'success');
      loadManageJobs();
    } else {
      showToast(res.message || 'Failed to update status', 'error');
    }
  } catch (err) {
    showToast('Error updating job status', 'error');
  }
}

async function handleDeleteJob(jobId) {
  if (!confirm('Are you sure you want to delete this job posting? All related candidate applications will also be removed.')) {
    return;
  }

  try {
    const res = await authFetch(`/api/jobs?id=${jobId}`, {
      method: 'DELETE'
    });
    if (res.success) {
      showToast('Job deleted successfully', 'success');
      loadManageJobs();
    } else {
      showToast(res.message || 'Failed to delete job', 'error');
    }
  } catch (err) {
    showToast('Error deleting job', 'error');
  }
}

function openEditJobModal(jobId) {
  const job = recruiterJobs.find(j => j.jobId === jobId);
  if (!job) return;

  document.getElementById('edit-job-id').value = job.jobId;
  document.getElementById('edit-job-title').value = job.title;
  document.getElementById('edit-job-company').value = job.company;
  document.getElementById('edit-job-type').value = job.jobType;
  document.getElementById('edit-job-vacancies').value = job.vacancies;
  document.getElementById('edit-job-location').value = job.location || '';
  document.getElementById('edit-job-country').value = job.country || '';
  document.getElementById('edit-job-salary').value = job.salaryRange || '';
  document.getElementById('edit-job-experience').value = job.experienceRequired || '';
  document.getElementById('edit-job-education').value = job.educationRequired || '';
  if (job.deadline) document.getElementById('edit-job-deadline').value = job.deadline;
  document.getElementById('edit-job-skills').value = job.skillsRequired || '';
  document.getElementById('edit-job-description').value = job.description || '';
  document.getElementById('edit-job-status').value = job.status || 'Active';

  openModal('edit-job-modal');
}

async function handleUpdateJobSubmit(e) {
  e.preventDefault();
  const payload = {
    jobId: parseInt(document.getElementById('edit-job-id').value),
    title: document.getElementById('edit-job-title').value.trim(),
    company: document.getElementById('edit-job-company').value.trim(),
    jobType: document.getElementById('edit-job-type').value,
    vacancies: parseInt(document.getElementById('edit-job-vacancies').value) || 1,
    location: document.getElementById('edit-job-location').value.trim(),
    country: document.getElementById('edit-job-country').value.trim(),
    salaryRange: document.getElementById('edit-job-salary').value.trim(),
    experienceRequired: document.getElementById('edit-job-experience').value.trim(),
    educationRequired: document.getElementById('edit-job-education').value.trim(),
    deadline: document.getElementById('edit-job-deadline').value || null,
    skillsRequired: document.getElementById('edit-job-skills').value.trim(),
    description: document.getElementById('edit-job-description').value.trim(),
    status: document.getElementById('edit-job-status').value
  };

  try {
    const res = await authFetch('/api/jobs', {
      method: 'PUT',
      body: JSON.stringify(payload)
    });
    if (res.success) {
      closeModal('edit-job-modal');
      showToast('Job updated successfully!', 'success');
      loadManageJobs();
    } else {
      showToast(res.message || 'Failed to update job', 'error');
    }
  } catch (err) {
    showToast('Error updating job', 'error');
  }
}

// ==========================================
// 4. Candidates & Applications Review
// ==========================================
async function loadCandidatesPage() {
  const tbody = document.getElementById('candidates-table-tbody');
  const jobFilterSelect = document.getElementById('candidate-filter-job');
  const statusFilterSelect = document.getElementById('candidate-filter-status');

  try {
    // Populate jobs dropdown for filter
    const jobsRes = await authFetch('/api/jobs/recruiter');
    if (jobsRes.success && Array.isArray(jobsRes.data) && jobFilterSelect) {
      jobFilterSelect.innerHTML = '<option value="">All Jobs</option>' + 
        jobsRes.data.map(j => `<option value="${j.jobId}">${j.title}</option>`).join('');
    }

    await fetchAndRenderCandidates();

    if (jobFilterSelect) jobFilterSelect.addEventListener('change', fetchAndRenderCandidates);
    if (statusFilterSelect) statusFilterSelect.addEventListener('change', fetchAndRenderCandidates);

    // Schedule Interview Form Listener
    const interviewForm = document.getElementById('schedule-interview-form');
    if (interviewForm) {
      interviewForm.addEventListener('submit', handleScheduleInterviewSubmit);
    }

  } catch (err) {
    console.error('Failed to load candidate page:', err);
  }
}

async function fetchAndRenderCandidates() {
  const tbody = document.getElementById('candidates-table-tbody');
  if (!tbody) return;

  const jobId = document.getElementById('candidate-filter-job')?.value || '';
  const status = document.getElementById('candidate-filter-status')?.value || '';

  let query = [];
  if (jobId) query.push(`jobId=${jobId}`);
  if (status) query.push(`status=${encodeURIComponent(status)}`);
  const queryString = query.length > 0 ? '?' + query.join('&') : '';

  try {
    const res = await authFetch('/api/applications' + queryString);
    if (res.success && Array.isArray(res.data)) {
      allApplications = res.data;

      if (allApplications.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted" style="padding: 2.5rem;">No candidates matching criteria.</td></tr>';
        return;
      }

      tbody.innerHTML = allApplications.map(a => `
        <tr>
          <td>
            <strong>${a.applicantName}</strong><br>
            <span style="font-size: 0.8rem; color: #64748b;">${a.applicantEmail}</span><br>
            <span style="font-size: 0.78rem; color: #94a3b8;">📞 ${a.applicantPhone || 'N/A'}</span>
          </td>
          <td><strong>${a.jobTitle}</strong></td>
          <td>
            <div style="font-size: 0.85rem; max-width: 200px;">
              <div><strong>Exp:</strong> ${a.applicantExperience || 0} Yrs</div>
              <div class="text-muted" style="font-size: 0.8rem; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;" title="${a.applicantSkills || ''}">
                ${a.applicantSkills || 'None listed'}
              </div>
            </div>
          </td>
          <td>${formatDate(a.appliedDate)}</td>
          <td>
            <select onchange="updateCandidateStatus(${a.applicationId}, this.value)" class="form-control" style="padding: 0.35rem 0.5rem; font-size: 0.85rem; font-weight: 600; width: auto;">
              <option value="Applied" ${a.status === 'Applied' ? 'selected' : ''}>Applied</option>
              <option value="Under Review" ${a.status === 'Under Review' ? 'selected' : ''}>Under Review</option>
              <option value="Shortlisted" ${a.status === 'Shortlisted' ? 'selected' : ''}>Shortlisted</option>
              <option value="Interview Scheduled" ${a.status === 'Interview Scheduled' ? 'selected' : ''}>Interview Scheduled</option>
              <option value="Selected" ${a.status === 'Selected' ? 'selected' : ''}>Selected</option>
              <option value="Rejected" ${a.status === 'Rejected' ? 'selected' : ''}>Rejected</option>
            </select>
          </td>
          <td>
            ${a.resumePath ? `
              <a href="/uploads/resumes/${a.resumePath}" target="_blank" class="btn btn-outline btn-sm">
                📄 Resume
              </a>
            ` : '<span class="text-muted" style="font-size: 0.8rem;">No file</span>'}
          </td>
          <td>
            <div class="flex gap-1">
              <button onclick="openCandidateProfileModal(${a.applicantId}, ${a.applicationId})" class="btn btn-secondary btn-sm">Profile</button>
              <button onclick="openScheduleModal(${a.applicationId}, '${encodeURIComponent(a.applicantName)}', '${encodeURIComponent(a.jobTitle)}')" class="btn btn-primary btn-sm" title="Schedule Interview">📅 Interview</button>
            </div>
          </td>
        </tr>
      `).join('');
    }
  } catch (err) {
    tbody.innerHTML = '<tr><td colspan="7" class="text-center text-danger">Failed to fetch candidates.</td></tr>';
  }
}

async function updateCandidateStatus(applicationId, newStatus) {
  try {
    const res = await authFetch('/api/applications/status', {
      method: 'PUT',
      body: JSON.stringify({ applicationId, status: newStatus })
    });
    if (res.success) {
      showToast(`Candidate status updated to: ${newStatus}`, 'success');
    } else {
      showToast(res.message || 'Failed to update status', 'error');
    }
  } catch (err) {
    showToast('Error updating candidate status', 'error');
  }
}

async function openCandidateProfileModal(applicantId, applicationId) {
  try {
    const res = await authFetch(`/api/recruiter/candidates?id=${applicantId}`);
    if (res.success && res.data) {
      const c = res.data;
      const app = allApplications.find(a => a.applicationId === applicationId);
      const skills = c.skills ? c.skills.split(',').map(s => `<span class="skill-tag">${s.trim()}</span>`).join(' ') : 'None listed';

      document.getElementById('cand-modal-title').textContent = `${c.fullName} - Candidate Profile`;
      document.getElementById('cand-modal-content').innerHTML = `
        <div class="stats-grid mb-3" style="grid-template-columns: repeat(3, 1fr);">
          <div style="background: #f8fafc; padding: 0.75rem; border-radius: 8px; border: 1px solid #e2e8f0;">
            <div class="text-muted" style="font-size: 0.8rem;">Email & Phone</div>
            <div class="font-bold">${c.email}</div>
            <div style="font-size: 0.85rem; color: #64748b;">${c.phone || 'N/A'}</div>
          </div>
          <div style="background: #f8fafc; padding: 0.75rem; border-radius: 8px; border: 1px solid #e2e8f0;">
            <div class="text-muted" style="font-size: 0.8rem;">Location</div>
            <div class="font-bold">${c.city || 'City'}, ${c.country || 'Country'}</div>
            <div style="font-size: 0.85rem; color: #64748b;">${c.address || ''}</div>
          </div>
          <div style="background: #f8fafc; padding: 0.75rem; border-radius: 8px; border: 1px solid #e2e8f0;">
            <div class="text-muted" style="font-size: 0.8rem;">Experience & Salary</div>
            <div class="font-bold">${c.experienceYears} Years</div>
            <div style="font-size: 0.85rem; color: #16a34a;">${c.expectedSalary || 'Negotiable'}</div>
          </div>
        </div>

        <div class="mb-3">
          <h4 class="mb-1">Education & Background</h4>
          <p><strong>${c.education || 'N/A'}</strong> — ${c.university || 'N/A'} ${c.graduationYear ? `(${c.graduationYear})` : ''}</p>
        </div>

        <div class="mb-3">
          <h4 class="mb-1">Skills</h4>
          <div class="job-skills">${skills}</div>
        </div>

        ${app && app.coverLetter ? `
          <div class="mb-3" style="background: #f8fafc; padding: 1rem; border-radius: 8px; border: 1px solid #e2e8f0;">
            <h4 class="mb-1">Submitted Cover Letter</h4>
            <p style="white-space: pre-line; font-size: 0.92rem; color: #334155;">${app.coverLetter}</p>
          </div>
        ` : ''}

        <div class="flex gap-2 items-center mt-2">
          ${c.linkedinUrl ? `<a href="${c.linkedinUrl}" target="_blank" class="btn btn-outline btn-sm">🔗 LinkedIn</a>` : ''}
          ${c.githubUrl ? `<a href="${c.githubUrl}" target="_blank" class="btn btn-outline btn-sm">🐙 GitHub</a>` : ''}
          ${c.leetcodeUrl ? `<a href="${c.leetcodeUrl}" target="_blank" class="btn btn-outline btn-sm">💻 LeetCode</a>` : ''}
          ${c.resumeFile ? `<a href="/uploads/resumes/${c.resumeFile}" target="_blank" class="btn btn-primary btn-sm">📄 View PDF Resume</a>` : ''}
        </div>
      `;

      openModal('candidate-profile-modal');
    }
  } catch (err) {
    showToast('Failed to load candidate details', 'error');
  }
}

function openScheduleModal(applicationId, encName, encTitle) {
  const name = decodeURIComponent(encName);
  const title = decodeURIComponent(encTitle);

  document.getElementById('sched-app-id').value = applicationId;
  document.getElementById('sched-modal-title').textContent = `Schedule Interview with ${name} (${title})`;
  
  // Set default tomorrow date
  const tomorrow = new Date();
  tomorrow.setDate(tomorrow.getDate() + 1);
  document.getElementById('sched-date').value = tomorrow.toISOString().split('T')[0];
  document.getElementById('sched-time').value = '14:00 EST';
  document.getElementById('sched-link').value = 'https://meet.google.com/' + Math.random().toString(36).substring(2, 5) + '-' + Math.random().toString(36).substring(2, 6) + '-' + Math.random().toString(36).substring(2, 5);

  openModal('schedule-interview-modal');
}

async function handleScheduleInterviewSubmit(e) {
  e.preventDefault();
  const form = e.target;
  const submitBtn = form.querySelector('button[type="submit"]');

  const payload = {
    applicationId: parseInt(document.getElementById('sched-app-id').value),
    interviewDate: document.getElementById('sched-date').value,
    interviewTime: document.getElementById('sched-time').value.trim(),
    interviewType: document.getElementById('sched-type').value,
    meetingLink: document.getElementById('sched-link').value.trim(),
    notes: document.getElementById('sched-notes').value.trim()
  };

  if (!payload.interviewDate || !payload.interviewTime) {
    showToast('Interview Date and Time are required.', 'error');
    return;
  }

  if (submitBtn) {
    submitBtn.disabled = true;
    submitBtn.textContent = 'Scheduling Interview...';
  }

  try {
    const res = await authFetch('/api/interviews', {
      method: 'POST',
      body: JSON.stringify(payload)
    });

    if (res.success) {
      closeModal('schedule-interview-modal');
      showToast('🎉 Interview scheduled and notification sent to candidate!', 'success');
      fetchAndRenderCandidates();
    } else {
      showToast(res.message || 'Failed to schedule interview', 'error');
    }
  } catch (err) {
    showToast('Server error while scheduling interview', 'error');
  } finally {
    if (submitBtn) {
      submitBtn.disabled = false;
      submitBtn.textContent = 'Confirm & Schedule';
    }
  }
}

// ==========================================
// 5. Recruiter Interviews Management
// ==========================================
async function loadRecruiterInterviews() {
  const tbody = document.getElementById('recruiter-interviews-tbody');
  if (!tbody) return;

  try {
    const res = await authFetch('/api/interviews');
    if (res.success && Array.isArray(res.data)) {
      if (res.data.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted" style="padding: 2.5rem;">No scheduled interviews found. Schedule interviews from the <a href="/recruiter/candidates.html">Candidates Page</a>.</td></tr>';
        return;
      }

      tbody.innerHTML = res.data.map(iv => `
        <tr>
          <td><strong>${iv.applicantName}</strong><br><span style="font-size: 0.8rem; color: #64748b;">${iv.applicantEmail}</span></td>
          <td><strong>${iv.jobTitle}</strong></td>
          <td>📅 ${formatDate(iv.interviewDate)}</td>
          <td>⏰ ${iv.interviewTime}</td>
          <td>${iv.interviewType === 'Online' ? '🌐 Online' : '🏢 Offline'}</td>
          <td>
            <select onchange="updateInterviewStatus(${iv.interviewId}, this.value)" class="form-control" style="padding: 0.35rem 0.5rem; font-size: 0.85rem; font-weight: 600; width: auto;">
              <option value="Scheduled" ${iv.status === 'Scheduled' ? 'selected' : ''}>Scheduled</option>
              <option value="Completed" ${iv.status === 'Completed' ? 'selected' : ''}>Completed</option>
              <option value="Cancelled" ${iv.status === 'Cancelled' ? 'selected' : ''}>Cancelled</option>
            </select>
          </td>
          <td>
            ${iv.meetingLink ? `
              <a href="${iv.meetingLink}" target="_blank" class="btn btn-primary btn-sm">
                🔗 Join
              </a>
            ` : '<span class="text-muted" style="font-size: 0.8rem;">No link</span>'}
          </td>
        </tr>
      `).join('');
    }
  } catch (err) {
    tbody.innerHTML = '<tr><td colspan="7" class="text-center text-danger">Failed to load interviews.</td></tr>';
  }
}

async function updateInterviewStatus(interviewId, newStatus) {
  try {
    const res = await authFetch('/api/interviews/status', {
      method: 'PUT',
      body: JSON.stringify({ interviewId, status: newStatus })
    });
    if (res.success) {
      showToast(`Interview marked as: ${newStatus}`, 'success');
    } else {
      showToast(res.message || 'Failed to update interview status', 'error');
    }
  } catch (err) {
    showToast('Error updating interview status', 'error');
  }
}

// ==========================================
// 6. Recruiter Notifications
// ==========================================
async function loadRecruiterNotifications() {
  const container = document.getElementById('recruiter-notifications-container');
  if (!container) return;

  try {
    const res = await authFetch('/api/notifications');
    if (res.success && res.data) {
      const list = res.data.notifications || [];
      if (list.length === 0) {
        container.innerHTML = '<div class="card text-center text-muted" style="padding: 3rem;"><h3>No Notifications</h3><p>You have no recent alerts.</p></div>';
        return;
      }

      container.innerHTML = list.map(n => `
        <div class="card mb-2" style="background: ${n.isRead ? '#ffffff' : '#f0f9ff'}; border-left: 4px solid ${n.isRead ? '#cbd5e1' : 'var(--primary)'};">
          <div class="flex justify-between items-center">
            <h4 style="color: ${n.isRead ? '#334155' : 'var(--primary-dark)'};">${n.title}</h4>
            <div class="flex items-center gap-2">
              <span style="font-size: 0.8rem; color: #94a3b8;">${formatDate(n.createdAt)}</span>
              ${!n.isRead ? `<button onclick="markRecruiterNotifRead(${n.notificationId})" class="btn btn-secondary btn-sm" style="font-size: 0.75rem; padding: 0.2rem 0.6rem;">Mark Read</button>` : '<span style="font-size: 0.75rem; color: #64748b;">Read ✓</span>'}
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

async function markRecruiterNotifRead(id) {
  try {
    await authFetch('/api/notifications/read', {
      method: 'PUT',
      body: JSON.stringify({ notificationId: id })
    });
    loadRecruiterNotifications();
    fetchUnreadNotificationsCount();
  } catch (err) {}
}

async function markAllRecruiterNotifsRead() {
  try {
    await authFetch('/api/notifications/read-all', { method: 'PUT' });
    showToast('All notifications marked as read', 'success');
    loadRecruiterNotifications();
    fetchUnreadNotificationsCount();
  } catch (err) {}
}
