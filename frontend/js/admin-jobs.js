/**
 * Administrator Job Moderation & Verification Script
 */

let allAdminJobs = [];
let currentAdminFilter = 'pending';

document.addEventListener('DOMContentLoaded', () => {
  if (typeof Auth !== 'undefined' && !Auth.requireAuth('admin')) return;

  const user = Auth.getUser();
  const greetingEl = document.getElementById('admin-user-greeting');
  if (greetingEl && user) {
    greetingEl.textContent = user.name || user.email || 'Admin';
  }

  loadAdminStats();
  loadAdminJobs();
});

async function loadAdminStats() {
  try {
    const res = await authFetch('/api/admin/stats');
    if (res.success && res.data) {
      const d = res.data;
      const totalEl = document.getElementById('stat-mod-total');
      const pendingEl = document.getElementById('stat-mod-pending');
      const approvedEl = document.getElementById('stat-mod-approved');
      const rejectedEl = document.getElementById('stat-mod-rejected');

      if (totalEl) totalEl.textContent = d.totalJobs || 0;
      if (pendingEl) pendingEl.textContent = d.pendingJobs || 0;
      if (approvedEl) approvedEl.textContent = d.approvedJobs || 0;
      if (rejectedEl) rejectedEl.textContent = Math.max(0, (d.totalJobs || 0) - (d.pendingJobs || 0) - (d.approvedJobs || 0));
    }
  } catch (err) {
    console.error('Failed to load admin stats:', err);
  }
}

async function loadAdminJobs() {
  const tbody = document.getElementById('admin-jobs-tbody');
  if (!tbody) return;

  tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted" style="padding: 2.5rem;">Loading job postings...</td></tr>';

  try {
    const res = await authFetch(`/api/admin/jobs?filter=${encodeURIComponent(currentAdminFilter)}`);
    if (res.success && Array.isArray(res.data)) {
      allAdminJobs = res.data;
      renderAdminJobsTable(allAdminJobs);
    } else {
      tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted">No vacancies found.</td></tr>';
    }
  } catch (err) {
    console.error('Failed to load admin jobs:', err);
    tbody.innerHTML = '<tr><td colspan="7" class="text-center text-danger">Error loading job postings.</td></tr>';
  }
}

function setAdminJobFilter(filter) {
  currentAdminFilter = filter;
  const buttons = document.querySelectorAll('#filter-tabs .filter-tab-btn');
  buttons.forEach(btn => {
    if (btn.getAttribute('data-filter') === filter) {
      btn.classList.add('active');
    } else {
      btn.classList.remove('active');
    }
  });
  loadAdminJobs();
}

function filterAdminJobsTable() {
  const query = (document.getElementById('admin-job-search').value || '').trim().toLowerCase();
  if (!query) {
    renderAdminJobsTable(allAdminJobs);
    return;
  }
  const filtered = allAdminJobs.filter(j => 
    (j.title && j.title.toLowerCase().includes(query)) ||
    (j.company && j.company.toLowerCase().includes(query)) ||
    (j.recruiterName && j.recruiterName.toLowerCase().includes(query)) ||
    (j.skillsRequired && j.skillsRequired.toLowerCase().includes(query))
  );
  renderAdminJobsTable(filtered);
}

function renderAdminJobsTable(jobs) {
  const tbody = document.getElementById('admin-jobs-tbody');
  if (!tbody) return;

  if (jobs.length === 0) {
    tbody.innerHTML = `<tr><td colspan="7" class="text-center text-muted" style="padding: 3rem;">
      No job postings found for filter <strong>${currentAdminFilter}</strong>.
    </td></tr>`;
    return;
  }

  tbody.innerHTML = jobs.map(j => {
    // Render mandatory vs preferred skills chips
    let skillsHtml = '';
    if (j.jobSkills && j.jobSkills.length > 0) {
      skillsHtml = j.jobSkills.map(s => {
        const cls = s.mandatory ? 'skill-mini-chip mandatory' : 'skill-mini-chip preferred';
        const label = s.mandatory ? `${s.skillName} (${s.minYearsRequired}y+)` : `${s.skillName} (bonus)`;
        return `<span class="${cls}">${label}</span>`;
      }).join('');
    } else if (j.skillsRequired) {
      skillsHtml = j.skillsRequired.split(',').map(s => `<span class="skill-mini-chip mandatory">${s.trim()}</span>`).join('');
    } else {
      skillsHtml = '<span class="text-muted" style="font-size: 0.8rem;">No skills defined</span>';
    }

    // Moderation badge
    let modBadge = '';
    const appr = (j.approvalStatus || 'approved').toLowerCase();
    if (appr === 'pending') {
      modBadge = '<span class="status-badge-pending">⏳ Pending Review</span>';
    } else if (appr === 'approved') {
      modBadge = '<span class="status-badge-approved">✅ Approved &amp; Live</span>';
    } else {
      modBadge = '<span class="status-badge-rejected">🚫 Rejected</span>';
    }

    // Action buttons
    let actionsHtml = '';
    if (appr === 'pending') {
      actionsHtml = `
        <div class="flex gap-1">
          <button class="btn btn-primary btn-sm" onclick="approveJob(${j.jobId})" style="background: #059669; border-color: #059669;" title="Approve and publish vacancy">✓ Approve</button>
          <button class="btn btn-danger btn-sm" onclick="rejectJob(${j.jobId})" title="Reject vacancy">✕ Reject</button>
        </div>
      `;
    } else if (appr === 'approved') {
      actionsHtml = `
        <div class="flex gap-1">
          <button class="btn btn-danger btn-sm" onclick="rejectJob(${j.jobId})" title="Revoke and reject vacancy">Revoke / Reject</button>
        </div>
      `;
    } else {
      actionsHtml = `
        <div class="flex gap-1">
          <button class="btn btn-primary btn-sm" onclick="approveJob(${j.jobId})" style="background: #059669; border-color: #059669;" title="Re-approve vacancy">✓ Re-Approve</button>
        </div>
      `;
    }

    return `
      <tr>
        <td style="font-weight: 700; color: #64748b;">#${j.jobId}</td>
        <td>
          <strong style="color: #0f172a; font-size: 0.95rem;">${j.title}</strong>
          <br>
          <span style="font-size: 0.85rem; color: #475569;">🏢 ${j.company || 'Enterprise'} &bull; 📍 ${j.location || 'Remote'}</span>
        </td>
        <td>
          <strong>${j.recruiterName || 'Talent Lead'}</strong>
          <br>
          <span style="font-size: 0.78rem; color: #94a3b8;">Recruiter ID #${j.recruiterId}</span>
        </td>
        <td style="max-width: 280px;">
          ${skillsHtml}
        </td>
        <td>
          <span class="badge" style="background: #f1f5f9; color: #334155;">${j.jobType || 'Full Time'}</span>
          <br>
          <small style="color: #64748b;">${j.experienceRequired || (j.minExperienceYears + '+ Yrs')}</small>
        </td>
        <td>${modBadge}</td>
        <td>${actionsHtml}</td>
      </tr>
    `;
  }).join('');
}

async function approveJob(jobId) {
  if (!confirm(`Are you sure you want to approve Job #${jobId}? It will immediately become visible to all candidates.`)) {
    return;
  }

  try {
    const res = await authFetch('/api/admin/jobs/approve', {
      method: 'POST',
      body: JSON.stringify({ jobId })
    });
    if (res.success) {
      showToast(`Job #${jobId} approved and is now live!`, 'success');
      loadAdminStats();
      loadAdminJobs();
    } else {
      showToast(res.message || 'Failed to approve job', 'error');
    }
  } catch (err) {
    showToast('Error communicating with server', 'error');
  }
}

async function rejectJob(jobId) {
  if (!confirm(`Are you sure you want to reject Job #${jobId}? It will be hidden from candidate search.`)) {
    return;
  }

  try {
    const res = await authFetch('/api/admin/jobs/reject', {
      method: 'POST',
      body: JSON.stringify({ jobId })
    });
    if (res.success) {
      showToast(`Job #${jobId} has been rejected.`, 'info');
      loadAdminStats();
      loadAdminJobs();
    } else {
      showToast(res.message || 'Failed to reject job', 'error');
    }
  } catch (err) {
    showToast('Error communicating with server', 'error');
  }
}
