/**
 * Public Job Directory and Application Script
 */

let allJobs = [];
let currentJobIdToApply = null;

document.addEventListener('DOMContentLoaded', () => {
  renderNavbar('jobs');
  fetchJobs();

  // Filter Listeners
  const searchInput = document.getElementById('search-keyword');
  const typeFilter = document.getElementById('filter-type');
  const expFilter = document.getElementById('filter-exp');
  const skillFilter = document.getElementById('filter-skill');
  const resetBtn = document.getElementById('btn-reset-filters');

  if (searchInput) searchInput.addEventListener('input', debounce(applyFilters, 300));
  if (typeFilter) typeFilter.addEventListener('change', applyFilters);
  if (expFilter) expFilter.addEventListener('change', applyFilters);
  if (skillFilter) skillFilter.addEventListener('input', debounce(applyFilters, 300));
  if (resetBtn) resetBtn.addEventListener('click', resetFilters);

  // Resume File Selection in Apply Modal
  const applyResumeInput = document.getElementById('apply-resume-file');
  const applyResumeText = document.getElementById('apply-resume-label');
  if (applyResumeInput && applyResumeText) {
    applyResumeInput.addEventListener('change', (e) => {
      if (e.target.files && e.target.files[0]) {
        const file = e.target.files[0];
        if (!file.name.toLowerCase().endsWith('.pdf')) {
          showToast('Only PDF files are allowed.', 'error');
          e.target.value = '';
          applyResumeText.textContent = 'Upload New Resume (PDF)...';
          return;
        }
        applyResumeText.textContent = `Selected: ${file.name}`;
      } else {
        applyResumeText.textContent = 'Upload New Resume (PDF)...';
      }
    });
  }

  // Handle Apply Form Submission
  const applyForm = document.getElementById('job-apply-form');
  if (applyForm) {
    applyForm.addEventListener('submit', handleApplySubmit);
  }
});

async function fetchJobs() {
  const container = document.getElementById('jobs-grid-container');
  if (container) {
    container.innerHTML = '<div style="grid-column: 1/-1; text-align: center; padding: 3rem;"><p class="text-muted">Loading open vacancies...</p></div>';
  }

  try {
    const jobsUrl = typeof apiEndpoint === 'function' ? apiEndpoint('/api/jobs') : '/api/jobs';
    const headers = {};
    if (typeof Auth !== 'undefined' && Auth.isLoggedIn && Auth.isLoggedIn()) {
      headers['Authorization'] = 'Bearer ' + Auth.getToken();
      headers['X-Session-Token'] = Auth.getToken();
    }
    const res = await fetch(jobsUrl, { headers });
    const data = await res.json();
    if (data.success && Array.isArray(data.data)) {
      allJobs = data.data;
      renderJobs(allJobs);
    } else {
      if (container) container.innerHTML = '<p class="text-muted text-center">No active jobs found.</p>';
    }
  } catch (err) {
    console.error('Failed to fetch jobs:', err);
    if (container) container.innerHTML = '<p class="text-danger text-center">Failed to load jobs. Please try again.</p>';
  }
}

function renderJobs(jobs) {
  const container = document.getElementById('jobs-grid-container');
  const countEl = document.getElementById('job-count-text');

  if (!container) return;

  if (countEl) {
    countEl.textContent = `${jobs.length} Job${jobs.length === 1 ? '' : 's'} Available`;
  }

  if (jobs.length === 0) {
    container.innerHTML = `
      <div style="grid-column: 1/-1; text-align: center; padding: 3rem; background: #fff; border-radius: 10px; border: 1px solid #e2e8f0;">
        <h3>No matching jobs found</h3>
        <p class="text-muted">Try adjusting your search criteria or resetting filters.</p>
        <button onclick="resetFilters()" class="btn btn-secondary btn-sm mt-2">Reset All Filters</button>
      </div>
    `;
    return;
  }

  container.innerHTML = jobs.map(j => {
    const skillsList = j.skillsRequired ? j.skillsRequired.split(',').map(s => `<span class="skill-tag">${s.trim()}</span>`).join('') : '';
    const deadlineText = j.deadline ? `Deadline: ${formatDate(j.deadline)}` : 'Open until filled';
    
    return `
      <div class="job-card">
        <div>
          <div class="flex justify-between items-center mb-1">
            <h3 class="job-title">${j.title}</h3>
            ${getJobTypeBadge(j.jobType)}
          </div>
          <div class="job-company">🏢 ${j.company}</div>
          
          ${j.matchScore ? `
            <div style="margin: 0.5rem 0;">
              <span class="match-badge ${(j.matchLevel || 'MODERATE').toLowerCase()}">
                <span class="match-badge-dot"></span>
                ${j.matchScore}% Match for Your Profile
              </span>
            </div>
          ` : ''}

          <div class="job-meta">
            <span class="job-meta-item">📍 ${j.location || 'Remote'}</span>
            <span class="job-meta-item">💼 ${j.experienceRequired || 'Any Exp'}</span>
            <span class="job-meta-item">👥 ${j.vacancies} ${j.vacancies === 1 ? 'Opening' : 'Openings'}</span>
          </div>

          <p class="text-muted mb-2" style="font-size: 0.9rem; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;">
            ${j.description}
          </p>

          <div class="job-skills">
            ${skillsList}
          </div>
        </div>

        <div>
          <div class="job-footer">
            <div>
              <div class="job-salary">${j.salaryRange || 'Competitive Salary'}</div>
              <div style="font-size: 0.75rem; color: #94a3b8;">${deadlineText}</div>
            </div>
            <div class="flex gap-1" style="flex-wrap: wrap;">
              <button onclick="openSkillGapModal(${j.jobId})" class="btn btn-outline btn-sm" style="border-color: #8b5cf6; color: #7c3aed;" title="Analyze Skill Gap & Learning Roadmap">📊 Skill Gap</button>
              <button onclick="viewJobDetails(${j.jobId})" class="btn btn-secondary btn-sm">Details</button>
              <button onclick="openApplyModal(${j.jobId})" class="btn btn-primary btn-sm">Apply Now</button>
            </div>
          </div>
        </div>
      </div>
    `;
  }).join('');
}

function applyFilters() {
  const kw = (document.getElementById('search-keyword')?.value || '').toLowerCase().trim();
  const type = (document.getElementById('filter-type')?.value || '').toLowerCase().trim();
  const exp = (document.getElementById('filter-exp')?.value || '').toLowerCase().trim();
  const skill = (document.getElementById('filter-skill')?.value || '').toLowerCase().trim();

  const filtered = allJobs.filter(j => {
    const matchKw = !kw || 
      (j.title && j.title.toLowerCase().includes(kw)) ||
      (j.company && j.company.toLowerCase().includes(kw)) ||
      (j.location && j.location.toLowerCase().includes(kw)) ||
      (j.description && j.description.toLowerCase().includes(kw));

    const matchType = !type || type === 'all' || (j.jobType && j.jobType.toLowerCase() === type);
    const matchExp = !exp || exp === 'all' || (j.experienceRequired && j.experienceRequired.toLowerCase().includes(exp));
    const matchSkill = !skill || (j.skillsRequired && j.skillsRequired.toLowerCase().includes(skill));

    return matchKw && matchType && matchExp && matchSkill;
  });

  renderJobs(filtered);
}

function resetFilters() {
  if (document.getElementById('search-keyword')) document.getElementById('search-keyword').value = '';
  if (document.getElementById('filter-type')) document.getElementById('filter-type').value = '';
  if (document.getElementById('filter-exp')) document.getElementById('filter-exp').value = '';
  if (document.getElementById('filter-skill')) document.getElementById('filter-skill').value = '';
  renderJobs(allJobs);
}

function viewJobDetails(jobId) {
  const job = allJobs.find(j => j.jobId === jobId);
  if (!job) return;

  const contentEl = document.getElementById('job-detail-content');
  if (contentEl) {
    const skillsList = job.skillsRequired ? job.skillsRequired.split(',').map(s => `<span class="skill-tag">${s.trim()}</span>`).join(' ') : 'None specified';
    
    contentEl.innerHTML = `
      <div class="mb-3">
        <div class="flex justify-between items-center mb-1">
          <h2>${job.title}</h2>
          ${getJobTypeBadge(job.jobType)}
        </div>
        <div class="job-company" style="font-size: 1.1rem;">🏢 ${job.company}</div>
      </div>

      <div class="stats-grid mb-3" style="grid-template-columns: repeat(3, 1fr);">
        <div style="background: #f8fafc; padding: 0.75rem; border-radius: 8px; border: 1px solid #e2e8f0;">
          <div class="text-muted" style="font-size: 0.8rem;">Location</div>
          <div class="font-bold">${job.location || 'Remote'} (${job.country || 'Global'})</div>
        </div>
        <div style="background: #f8fafc; padding: 0.75rem; border-radius: 8px; border: 1px solid #e2e8f0;">
          <div class="text-muted" style="font-size: 0.8rem;">Salary</div>
          <div class="font-bold text-primary">${job.salaryRange || 'Competitive'}</div>
        </div>
        <div style="background: #f8fafc; padding: 0.75rem; border-radius: 8px; border: 1px solid #e2e8f0;">
          <div class="text-muted" style="font-size: 0.8rem;">Experience</div>
          <div class="font-bold">${job.experienceRequired || 'Not specified'}</div>
        </div>
      </div>

      <div class="mb-3">
        <h4 class="mb-1">Job Description</h4>
        <p style="white-space: pre-line; color: #334155; line-height: 1.7;">${job.description}</p>
      </div>

      <div class="mb-3">
        <h4 class="mb-1">Required Skills &amp; Competencies</h4>
        ${job.mandatorySkills && job.mandatorySkills.length > 0 ? `
          <div style="margin-bottom: 0.5rem;">
            <strong style="font-size: 0.8rem; color: #991b1b; text-transform: uppercase;">🔴 Mandatory Requirements:</strong>
            <div class="job-skills mt-1 mb-2">
              ${job.mandatorySkills.map(s => `<span class="skill-tag" style="background: #fee2e2; color: #991b1b; border: 1px solid #fca5a5;">${s.skillName} (${s.minYearsRequired}y+ req)</span>`).join(' ')}
            </div>
          </div>
        ` : `<div class="job-skills mb-2">${skillsList}</div>`}

        ${job.preferredSkills && job.preferredSkills.length > 0 ? `
          <div style="margin-bottom: 0.5rem;">
            <strong style="font-size: 0.8rem; color: #0369a1; text-transform: uppercase;">🔵 Preferred / Bonus:</strong>
            <div class="job-skills mt-1 mb-2">
              ${job.preferredSkills.map(s => `<span class="skill-tag" style="background: #e0f2fe; color: #0369a1; border: 1px solid #bae6fd;">${s.skillName}</span>`).join(' ')}
            </div>
          </div>
        ` : ''}
        <p class="text-muted" style="font-size: 0.9rem;">Education: <strong>${job.educationRequired || 'Not specified'}</strong></p>
      </div>

      <div class="mb-2 text-muted" style="font-size: 0.85rem;">
        <span>Vacancies: ${job.vacancies}</span> • 
        <span>Posted: ${formatDate(job.postedDate)}</span> • 
        <span>Deadline: ${formatDate(job.deadline)}</span>
      </div>
    `;
  }

  const applyBtn = document.getElementById('modal-job-apply-trigger');
  if (applyBtn) {
    applyBtn.onclick = () => {
      closeModal('job-detail-modal');
      openApplyModal(jobId);
    };
  }

  const gapBtn = document.getElementById('modal-job-gap-trigger');
  if (gapBtn) {
    gapBtn.onclick = () => {
      closeModal('job-detail-modal');
      openSkillGapModal(jobId);
    };
  }

  openModal('job-detail-modal');
}

function openApplyModal(jobId) {
  if (!Auth.isLoggedIn()) {
    showToast('Please log in as an applicant to apply.', 'info');
    setTimeout(() => {
      const base = typeof getBasePath === 'function' ? getBasePath() : '';
      window.location.href = base + 'login.html?redirect=' + encodeURIComponent(window.location.pathname);
    }, 1000);
    return;
  }

  if (Auth.isRecruiter()) {
    showToast('Recruiters cannot apply for jobs. Please use an applicant account.', 'error');
    return;
  }

  currentJobIdToApply = jobId;
  const job = allJobs.find(j => j.jobId === jobId);
  
  const titleEl = document.getElementById('apply-modal-job-title');
  if (titleEl && job) {
    titleEl.textContent = `Apply for ${job.title} at ${job.company}`;
  }

  // Preload user info
  const user = Auth.getUser();
  const nameInput = document.getElementById('apply-name');
  const emailInput = document.getElementById('apply-email');
  if (nameInput) nameInput.value = user.name || '';
  if (emailInput) emailInput.value = user.email || '';

  openModal('job-apply-modal');
}

async function handleApplySubmit(e) {
  e.preventDefault();
  if (!currentJobIdToApply) return;

  const form = e.target;
  const submitBtn = form.querySelector('button[type="submit"]');
  const formData = new FormData(form);
  formData.append('jobId', currentJobIdToApply);

  if (submitBtn) {
    submitBtn.disabled = true;
    submitBtn.textContent = 'Submitting Application...';
  }

  try {
    const applyUrl = typeof apiEndpoint === 'function' ? apiEndpoint('/api/applications') : '/api/applications';
    const res = await fetch(applyUrl, {
      method: 'POST',
      headers: {
        'X-Session-Token': Auth.getToken(),
        'Authorization': 'Bearer ' + Auth.getToken()
      },
      body: formData
    });

    const data = await res.json();

    if (res.ok && data.success) {
      closeModal('job-apply-modal');
      showToast('🎉 Application submitted successfully! Check your Dashboard to track status.', 'success');
      form.reset();
    } else {
      showToast(data.message || 'Failed to submit application.', 'error');
    }
  } catch (err) {
    console.error('Apply error:', err);
    showToast('Error communicating with server.', 'error');
  } finally {
    if (submitBtn) {
      submitBtn.disabled = false;
      submitBtn.textContent = 'Submit Application';
    }
  }
}

function debounce(func, wait) {
  let timeout;
  return function(...args) {
    clearTimeout(timeout);
    timeout = setTimeout(() => func.apply(this, args), wait);
  };
}
