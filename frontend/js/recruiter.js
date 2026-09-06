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

      // Populate Conversion Funnel
      const totalApps = statsRes.data.totalApplications || 0;
      const shortlisted = statsRes.data.shortlistedCandidates || 0;
      const selected = statsRes.data.selectedCandidates || 0;

      const elIntake = document.getElementById('funnel-intake');
      const elScreened = document.getElementById('funnel-screened');
      const elInterviewing = document.getElementById('funnel-interviewing');
      const elHired = document.getElementById('funnel-hired');

      if (elIntake) elIntake.textContent = totalApps;
      if (elScreened) elScreened.textContent = shortlisted;
      if (elInterviewing) elInterviewing.textContent = Math.max(0, shortlisted - selected);
      if (elHired) elHired.textContent = selected;
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
// 2. Post Job with Dual-Skills Decomposition
// ==========================================
let postJobSkills = [
  { name: 'Java', category: 'Backend', isMandatory: true, minYearsRequired: 2 },
  { name: 'MySQL', category: 'Database', isMandatory: true, minYearsRequired: 1 }
];

async function initPostJobPage() {
  const form = document.getElementById('post-job-form');
  if (form) {
    form.addEventListener('submit', handlePostJobSubmit);
  }

  // Load registered companies for autocomplete
  try {
    const compRes = await authFetch('/api/companies');
    if (compRes.success && Array.isArray(compRes.data)) {
      const dl = document.getElementById('companies-list');
      if (dl) {
        dl.innerHTML = compRes.data.map(c => `<option value="${c.name}">`).join('');
      }
    }
  } catch (ignored) {}

  // Load master skills taxonomy
  try {
    const skillsRes = await authFetch('/api/candidate/skills');
    if (skillsRes.success && Array.isArray(skillsRes.data)) {
      const dl = document.getElementById('master-skills-datalist');
      if (dl) {
        dl.innerHTML = skillsRes.data.map(s => `<option value="${s.name}">`).join('');
      }
    }
  } catch (ignored) {}

  renderPostJobSkills();
}

function addSkillToPostList() {
  const nameEl = document.getElementById('skill-input-name');
  const catEl = document.getElementById('skill-input-category');
  const mandEl = document.getElementById('skill-input-mandatory');
  const yearsEl = document.getElementById('skill-input-years');

  const name = (nameEl.value || '').trim();
  if (!name) {
    showToast('Please enter a skill name.', 'error');
    return;
  }

  const category = catEl.value || 'Backend';
  const isMandatory = mandEl.value === 'true';
  const minYears = parseInt(yearsEl.value) || 1;

  // Avoid duplicates
  const existing = postJobSkills.find(s => s.name.toLowerCase() === name.toLowerCase());
  if (existing) {
    showToast(`Skill "${name}" is already added.`, 'error');
    return;
  }

  postJobSkills.push({ name, category, isMandatory, minYearsRequired: minYears });
  nameEl.value = '';
  renderPostJobSkills();
  showToast(`Added ${isMandatory ? 'mandatory' : 'preferred'} skill: ${name}`, 'success');
}

function removeSkillFromPostList(idx) {
  if (idx >= 0 && idx < postJobSkills.length) {
    postJobSkills.splice(idx, 1);
    renderPostJobSkills();
  }
}

function renderPostJobSkills() {
  const mandContainer = document.getElementById('mandatory-skills-chips');
  const prefContainer = document.getElementById('preferred-skills-chips');

  const mandatory = postJobSkills.filter(s => s.isMandatory);
  const preferred = postJobSkills.filter(s => !s.isMandatory);

  if (mandContainer) {
    if (mandatory.length === 0) {
      mandContainer.innerHTML = '<span class="text-muted" style="font-size: 0.82rem;">⚠️ No mandatory skills added yet. At least one required.</span>';
    } else {
      mandContainer.innerHTML = mandatory.map(s => {
        const globalIdx = postJobSkills.indexOf(s);
        return `
          <span class="skill-badge-item mandatory">
            <span>🔴 <strong>${s.name}</strong> (${s.minYearsRequired}y+ req)</span>
            <span class="skill-badge-remove" onclick="removeSkillFromPostList(${globalIdx})" title="Remove skill">&times;</span>
          </span>
        `;
      }).join('');
    }
  }

  if (prefContainer) {
    if (preferred.length === 0) {
      prefContainer.innerHTML = '<span class="text-muted" style="font-size: 0.82rem;">No preferred skills added (optional).</span>';
    } else {
      prefContainer.innerHTML = preferred.map(s => {
        const globalIdx = postJobSkills.indexOf(s);
        return `
          <span class="skill-badge-item preferred">
            <span>🔵 ${s.name} (${s.category})</span>
            <span class="skill-badge-remove" onclick="removeSkillFromPostList(${globalIdx})" title="Remove skill">&times;</span>
          </span>
        `;
      }).join('');
    }
  }
}

async function handlePostJobSubmit(e) {
  e.preventDefault();
  const form = e.target;
  const submitBtn = form.querySelector('button[type="submit"]');

  const mandatory = postJobSkills.filter(s => s.isMandatory);
  if (mandatory.length === 0) {
    showToast('Please add at least one Mandatory Skill requirement.', 'error');
    return;
  }

  const payload = {
    title: document.getElementById('job-title').value.trim(),
    company: document.getElementById('job-company').value.trim(),
    jobType: document.getElementById('job-type').value,
    vacancies: parseInt(document.getElementById('job-vacancies').value) || 1,
    location: document.getElementById('job-location').value.trim(),
    country: document.getElementById('job-country').value.trim(),
    salaryRange: document.getElementById('job-salary').value.trim(),
    minExperienceYears: parseInt(document.getElementById('job-min-experience').value) || 0,
    educationRequired: document.getElementById('job-education').value,
    deadline: document.getElementById('job-deadline').value || null,
    description: document.getElementById('job-description').value.trim(),
    skills: postJobSkills,
    skillsRequired: postJobSkills.map(s => s.name).join(', '),
    approvalStatus: 'approved'
  };

  if (!payload.title || !payload.company || !payload.description) {
    showToast('Job Title, Hiring Company, and Description are required.', 'error');
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
        const base = typeof getBasePath === 'function' ? getBasePath() : '';
        window.location.href = base + 'recruiter/manage-jobs.html';
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
// 3. Manage Jobs with Approval & Dual Skills
// ==========================================
async function loadManageJobs() {
  const tbody = document.getElementById('manage-jobs-tbody');
  if (!tbody) return;

  try {
    const res = await authFetch('/api/jobs/recruiter');
    if (res.success && Array.isArray(res.data)) {
      recruiterJobs = res.data;

      if (recruiterJobs.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted" style="padding: 2.5rem;">You have not posted any jobs yet. <a href="post-job.html">Post your first job</a></td></tr>';
        return;
      }

      tbody.innerHTML = recruiterJobs.map(j => {
        // Moderation badge
        let apprBadge = '';
        const appr = (j.approvalStatus || 'approved').toLowerCase();
        if (appr === 'pending') {
          apprBadge = '<span class="badge badge-warning" style="background: #fef3c7; color: #92400e; font-weight: 700;">⏳ Pending Review</span>';
        } else if (appr === 'approved') {
          apprBadge = '<span class="badge badge-success" style="background: #dcfce7; color: #166534; font-weight: 700;">✅ Approved &amp; Live</span>';
        } else {
          apprBadge = '<span class="badge badge-danger" style="background: #fee2e2; color: #991b1b; font-weight: 700;">🚫 Rejected</span>';
        }

        return `
          <tr>
            <td>
              <strong style="font-size: 0.95rem; color: #0f172a;">${j.title}</strong><br>
              <span style="font-size: 0.8rem; color: #64748b;">🏢 ${j.company} &bull; 📍 ${j.location || 'Remote'}</span>
            </td>
            <td>
              ${getJobTypeBadge(j.jobType)}<br>
              <small style="color: #64748b;">💼 ${j.experienceRequired || (j.minExperienceYears + '+ Yrs')}</small>
            </td>
            <td>
              <span class="badge" style="background: #e0f2fe; color: #0369a1; font-weight: bold; font-size: 0.85rem;">
                👥 ${j.applicationCount || 0} Apps
              </span>
            </td>
            <td>${apprBadge}</td>
            <td>
              <button onclick="toggleJobStatus(${j.jobId}, '${j.status}')" class="badge ${j.status === 'Active' ? 'badge-active' : 'badge-closed'}" style="cursor: pointer; border: none;" title="Click to Toggle Status">
                ${j.status} 🔄
              </button>
            </td>
            <td style="white-space: nowrap; font-size: 0.85rem; color: #64748b;">
              ${formatDate(j.postedDate)}
            </td>
            <td>
              <div class="flex gap-1">
                <button onclick="viewJobOpeningModal(${j.jobId})" class="btn btn-secondary btn-sm" title="View Requirements">🔍 Details</button>
                <button onclick="openEditJobModal(${j.jobId})" class="btn btn-secondary btn-sm" title="Edit Job Details">✏️</button>
                <button onclick="handleDeleteJob(${j.jobId})" class="btn btn-danger btn-sm" title="Delete Job">🗑️</button>
              </div>
            </td>
          </tr>
        `;
      }).join('');
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

function viewJobOpeningModal(jobId) {
  const job = recruiterJobs.find(j => j.jobId === jobId);
  if (!job) return;

  const titleEl = document.getElementById('view-job-title');
  const bodyEl = document.getElementById('view-job-body');
  if (titleEl) titleEl.textContent = `${job.title} — ${job.company}`;

  let mandatoryHtml = '';
  let preferredHtml = '';

  if (job.jobSkills && job.jobSkills.length > 0) {
    const mand = job.jobSkills.filter(s => s.mandatory);
    const pref = job.jobSkills.filter(s => !s.mandatory);

    mandatoryHtml = mand.length > 0 
      ? mand.map(s => `<span class="badge" style="background: #fee2e2; color: #991b1b; margin: 3px;">🔴 ${s.skillName} (${s.minYearsRequired}y+ req)</span>`).join('')
      : '<span class="text-muted">None specified</span>';

    preferredHtml = pref.length > 0
      ? pref.map(s => `<span class="badge" style="background: #e0f2fe; color: #0369a1; margin: 3px;">🔵 ${s.skillName}</span>`).join('')
      : '<span class="text-muted">None specified</span>';
  } else if (job.skillsRequired) {
    mandatoryHtml = job.skillsRequired.split(',').map(s => `<span class="badge" style="background: #fee2e2; color: #991b1b; margin: 3px;">${s.trim()}</span>`).join('');
    preferredHtml = '<span class="text-muted">None</span>';
  }

  if (bodyEl) {
    bodyEl.innerHTML = `
      <div style="margin-bottom: 1rem;">
        <div style="display: flex; gap: 0.5rem; margin-bottom: 0.75rem;">
          <span class="badge" style="background: #f1f5f9; color: #334155;">📍 ${job.location || 'Remote'}</span>
          <span class="badge" style="background: #f1f5f9; color: #334155;">💼 ${job.jobType || 'Full Time'}</span>
          <span class="badge" style="background: #f1f5f9; color: #334155;">💰 ${job.salaryRange || 'Competitive'}</span>
          <span class="badge" style="background: #f1f5f9; color: #334155;">⏳ Min Exp: ${job.minExperienceYears || 0} Yrs</span>
        </div>
        <p style="font-size: 0.9rem; color: #334155; line-height: 1.5; white-space: pre-line;">${job.description}</p>
      </div>
      <div style="margin-top: 1rem; padding-top: 1rem; border-top: 1px solid #e2e8f0;">
        <h4 style="font-size: 0.9rem; color: #991b1b; margin-bottom: 0.4rem;">Mandatory Skill Prerequisites:</h4>
        <div>${mandatoryHtml}</div>
      </div>
      <div style="margin-top: 0.75rem;">
        <h4 style="font-size: 0.9rem; color: #0369a1; margin-bottom: 0.4rem;">Preferred / Bonus Skills:</h4>
        <div>${preferredHtml}</div>
      </div>
    `;
  }

  openModal('view-job-modal');
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
    const sortSelect = document.getElementById('candidate-sort-match');
    if (sortSelect) sortSelect.addEventListener('change', fetchAndRenderCandidates);

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
  const sortOrder = document.getElementById('candidate-sort-match')?.value || 'match';

  let query = [];
  if (jobId) query.push(`jobId=${jobId}`);
  if (status) query.push(`status=${encodeURIComponent(status)}`);
  if (sortOrder) query.push(`sortBy=${encodeURIComponent(sortOrder)}`);
  const queryString = query.length > 0 ? '?' + query.join('&') : '';

  try {
    const res = await authFetch('/api/applications' + queryString);
    if (res.success && Array.isArray(res.data)) {
      allApplications = res.data;

      if (allApplications.length === 0) {
        tbody.innerHTML = '<tr><td colspan="8" class="text-center text-muted" style="padding: 2.5rem;">No candidates matching criteria.</td></tr>';
        return;
      }

      tbody.innerHTML = allApplications.map((a, idx) => {
        const rank = a.rank || (idx + 1);
        let rankBadge = `<span style="display: inline-flex; align-items: center; justify-content: center; width: 32px; height: 32px; border-radius: 50%; background: #e2e8f0; color: #475569; font-weight: 700; font-size: 0.88rem;">#${rank}</span>`;
        if (rank === 1) {
          rankBadge = `<span style="display: inline-flex; align-items: center; justify-content: center; width: 36px; height: 36px; border-radius: 50%; background: linear-gradient(135deg, #fbbf24, #d97706); color: #fff; font-weight: 800; font-size: 0.95rem; box-shadow: 0 2px 8px rgba(217,119,6,0.4);" title="Top Candidate">🥇1</span>`;
        } else if (rank === 2) {
          rankBadge = `<span style="display: inline-flex; align-items: center; justify-content: center; width: 34px; height: 34px; border-radius: 50%; background: linear-gradient(135deg, #94a3b8, #64748b); color: #fff; font-weight: 800; font-size: 0.9rem; box-shadow: 0 2px 6px rgba(100,116,139,0.3);" title="Rank #2">🥈2</span>`;
        } else if (rank === 3) {
          rankBadge = `<span style="display: inline-flex; align-items: center; justify-content: center; width: 34px; height: 34px; border-radius: 50%; background: linear-gradient(135deg, #d97706, #b45309); color: #fff; font-weight: 800; font-size: 0.9rem; box-shadow: 0 2px 6px rgba(180,83,9,0.3);" title="Rank #3">🥉3</span>`;
        }

        return `
        <tr style="${rank === 1 ? 'background: #fffbeb;' : ''}">
          <td style="text-align: center; vertical-align: middle;">
            ${rankBadge}
          </td>
          <td>
            <strong>${a.candidateName || a.applicantName}</strong><br>
            <span style="font-size: 0.8rem; color: #64748b;">${a.candidateEmail || a.applicantEmail}</span><br>
            <span style="font-size: 0.78rem; color: #94a3b8;">📞 ${a.candidatePhone || a.applicantPhone || 'N/A'}</span>
            ${a.candidateCity ? `<div style="font-size: 0.75rem; color: #64748b;">📍 ${a.candidateCity}, ${a.candidateCountry || ''}</div>` : ''}
          </td>
          <td>
            <strong>${a.jobTitle}</strong><br>
            <span style="font-size: 0.8rem; color: #64748b;">${a.companyName || a.company || 'Enterprise'}</span>
          </td>
          <td>
            <div style="margin-bottom: 4px;">
              ${renderMatchBadge(a.matchScore, a.matchLevel)}
            </div>
            ${a.rankingInsight ? `
              <div style="font-size: 0.76rem; color: #1e3a8a; font-weight: 600; margin-bottom: 4px; background: #eff6ff; padding: 2px 6px; border-radius: 4px;">
                ${a.rankingInsight}
              </div>
            ` : ''}
            <div style="font-size: 0.82rem; max-width: 250px;">
              <div><strong>Exp:</strong> ${a.candidateExperienceYears !== undefined ? a.candidateExperienceYears : (a.applicantExperience || 0)} Yrs</div>
              ${a.matchedSkills && a.matchedSkills.length > 0 ? `
                <div style="margin-top: 3px;">
                  ${a.matchedSkills.slice(0, 3).map(s => `<span class="badge skill-tag-matched" style="font-size: 0.72rem; padding: 1px 6px; margin-right: 2px;">✓ ${s}</span>`).join('')}
                </div>
              ` : ''}
              ${a.missingMandatorySkills && a.missingMandatorySkills.length > 0 ? `
                <div style="margin-top: 2px;">
                  ${a.missingMandatorySkills.slice(0, 2).map(s => `<span class="badge skill-tag-missing" style="font-size: 0.72rem; padding: 1px 6px; margin-right: 2px; background: #fef2f2; color: #dc2626;">✕ ${s}</span>`).join('')}
                </div>
              ` : ''}
            </div>
          </td>
          <td>${formatDate(a.appliedAt || a.appliedDate)}</td>
          <td>
            <select onchange="updateCandidateStatus(${a.applicationId}, this.value)" class="form-control" style="padding: 0.35rem 0.5rem; font-size: 0.85rem; font-weight: 600; width: auto;">
              <option value="Applied" ${(a.status === 'Applied') ? 'selected' : ''}>Applied</option>
              <option value="Under Review" ${(a.status === 'Under Review' || a.status === 'Under_Review') ? 'selected' : ''}>Under Review</option>
              <option value="Shortlisted" ${(a.status === 'Shortlisted') ? 'selected' : ''}>Shortlisted</option>
              <option value="Interview Scheduled" ${(a.status === 'Interview Scheduled' || a.status === 'Interview_Scheduled') ? 'selected' : ''}>Interview Scheduled</option>
              <option value="Selected" ${(a.status === 'Selected') ? 'selected' : ''}>Selected</option>
              <option value="Rejected" ${(a.status === 'Rejected') ? 'selected' : ''}>Rejected</option>
            </select>
          </td>
          <td>
            ${(a.resumePath || a.resumeFileName) ? `
              <a href="/uploads/resumes/${a.resumePath || a.resumeFileName}" target="_blank" class="btn btn-outline btn-sm">
                📄 Resume
              </a>
            ` : '<span class="text-muted" style="font-size: 0.8rem;">No file</span>'}
          </td>
          <td>
            <div class="flex gap-1" style="flex-wrap: wrap;">
              <button onclick="openCandidateProfileModal(${a.candidateId || a.applicantId}, ${a.applicationId})" class="btn btn-secondary btn-sm" title="View Full Candidate Profile">Profile</button>
              ${a.status !== 'Shortlisted' ? `
                <button onclick="quickUpdateStatus(${a.applicationId}, 'Shortlisted')" class="btn btn-sm" style="background: #2563eb; color: #fff;" title="1-Click Shortlist">⭐ Shortlist</button>
              ` : ''}
              ${(a.status === 'Interview Scheduled' || a.status === 'Interview_Scheduled') ? `
                <a href="../interview-room.html" class="btn btn-sm btn-primary" style="background: #10b981; border-color: #10b981;" title="Open Virtual Interview Room">
                  💻 Room
                </a>
              ` : `
                <button onclick="openScheduleModal(${a.applicationId}, '${encodeURIComponent(a.candidateName || a.applicantName)}', '${encodeURIComponent(a.jobTitle)}')" class="btn btn-primary btn-sm" title="Schedule Interview">📅 Interview</button>
              `}
            </div>
          </td>
        </tr>
      `}).join('');
    }
  } catch (err) {
    tbody.innerHTML = '<tr><td colspan="8" class="text-center text-danger">Failed to fetch candidates: ' + err.message + '</td></tr>';
  }
}

async function quickUpdateStatus(applicationId, newStatus) {
  await updateCandidateStatus(applicationId, newStatus);
  await fetchAndRenderCandidates();
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

        ${app ? `
          <div class="mb-3" style="background: #f0fdf4; border: 1px solid #bbf7d0; padding: 1rem; border-radius: 8px;">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.5rem;">
              <h4 style="margin: 0; color: #166534;">Smart Match Compatibility Breakdown</h4>
              <span style="font-size: 1.1rem; font-weight: 800; color: #15803d;">${app.matchScore || 0}% (${app.matchLevel || 'MODERATE'})</span>
            </div>
            <div style="font-size: 0.85rem; color: #334155; margin-bottom: 0.5rem;">
              ${app.rankingInsight || ''}
            </div>
            <div class="stats-grid" style="grid-template-columns: repeat(5, 1fr); gap: 0.5rem; text-align: center;">
              <div style="background: #fff; padding: 0.5rem; border-radius: 6px; border: 1px solid #dcfce7;">
                <div style="font-size: 0.72rem; color: #64748b;">Skills (45%)</div>
                <div style="font-weight: 700; color: #0f172a;">${app.skillScore || 0}%</div>
              </div>
              <div style="background: #fff; padding: 0.5rem; border-radius: 6px; border: 1px solid #dcfce7;">
                <div style="font-size: 0.72rem; color: #64748b;">Exp (20%)</div>
                <div style="font-weight: 700; color: #0f172a;">${app.experienceScore || 0}%</div>
              </div>
              <div style="background: #fff; padding: 0.5rem; border-radius: 6px; border: 1px solid #dcfce7;">
                <div style="font-size: 0.72rem; color: #64748b;">Edu (10%)</div>
                <div style="font-weight: 700; color: #0f172a;">${app.educationScore || 0}%</div>
              </div>
              <div style="background: #fff; padding: 0.5rem; border-radius: 6px; border: 1px solid #dcfce7;">
                <div style="font-size: 0.72rem; color: #64748b;">Proj (10%)</div>
                <div style="font-weight: 700; color: #0f172a;">${app.projectScore || 0}%</div>
              </div>
              <div style="background: #fff; padding: 0.5rem; border-radius: 6px; border: 1px solid #dcfce7;">
                <div style="font-size: 0.72rem; color: #64748b;">Assess (15%)</div>
                <div style="font-weight: 700; color: #0f172a;">${app.assessmentScore || 0}%</div>
              </div>
            </div>
            ${app.missingMandatorySkills && app.missingMandatorySkills.length > 0 ? `
              <div style="margin-top: 0.5rem; font-size: 0.8rem; color: #b91c1c;">
                <strong>Missing Mandatory Prerequisites:</strong> ${app.missingMandatorySkills.join(', ')}
              </div>
            ` : '<div style="margin-top: 0.5rem; font-size: 0.8rem; color: #15803d;">✓ All mandatory job requirements satisfied</div>'}
          </div>
        ` : ''}

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

function renderMatchBadge(score, level) {
  if (score === undefined || score === null) return '';
  const lvl = (level || 'MODERATE').toLowerCase();
  return `
    <span class="match-badge ${lvl}">
      <span class="match-badge-dot"></span>
      ${score}% Match (${level || 'Good'})
    </span>
  `;
}
