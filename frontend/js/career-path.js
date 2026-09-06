/**
 * Career Path Recommendation Engine (Core Innovation 5) Client Controller
 * Pure Vanilla JavaScript (No frameworks)
 */

let careerRecommendations = [];
let allTracks = [];
let selectedTrackIndex = 0;

document.addEventListener('DOMContentLoaded', async () => {
  if (!Auth.isLoggedIn()) {
    window.location.href = '../login.html';
    return;
  }

  await loadCareerTrajectories();
});

async function loadCareerTrajectories() {
  const container = document.getElementById('track-tabs-container');
  try {
    const res = await API.request('/api/career-paths');
    if (!res || !res.success) {
      if (container) {
        container.innerHTML = '<div style="color:#ef4444;">Failed to load career paths. Please retry.</div>';
      }
      return;
    }

    const data = res.data;
    if (data && data.recommendations && data.recommendations.length > 0) {
      careerRecommendations = data.recommendations;
      renderTrackTabs();
      selectTrack(0);
    } else if (Array.isArray(data) && data.length > 0) {
      // Unauthenticated or general view fallback
      allTracks = data;
      renderFallbackTracks();
      selectFallbackTrack(0);
    } else {
      if (container) {
        container.innerHTML = '<div style="color:#64748b;">No career paths available.</div>';
      }
    }
  } catch (err) {
    console.error('[CareerPath] Error loading trajectories:', err);
    if (container) {
      container.innerHTML = '<div style="color:#ef4444;">Network error while loading career trajectories.</div>';
    }
  }
}

function renderTrackTabs() {
  const container = document.getElementById('track-tabs-container');
  if (!container) return;

  container.innerHTML = careerRecommendations.map((rec, idx) => {
    const path = rec.careerPath;
    const rating = rec.readinessRating || 'EXPLORATORY';
    let pillClass = 'affinity-exploratory';
    let label = rec.readinessLabel || 'Exploratory';

    if (rating === 'HIGH_AFFINITY') {
      pillClass = 'affinity-high';
    } else if (rating === 'MODERATE_AFFINITY') {
      pillClass = 'affinity-moderate';
    }

    return `
      <button class="track-tab-btn ${idx === 0 ? 'active' : ''}" onclick="selectTrack(${idx})" id="tab-btn-${idx}">
        <div class="track-tab-title">${escapeHtml(path.title)}</div>
        <div style="display:flex; align-items:center; gap:8px; margin-top:4px;">
          <span class="affinity-pill ${pillClass}">${rec.matchScore}% Match</span>
          <span style="font-size:0.75rem; color:#64748b; font-weight:600;">L${rec.currentMilestoneLevel || 1}</span>
        </div>
      </button>
    `;
  }).join('');

  // Update Hero Top Match Banner
  const topMatch = careerRecommendations[0];
  if (topMatch) {
    const scoreEl = document.getElementById('hero-match-score');
    const trackEl = document.getElementById('hero-track-name');
    if (scoreEl) scoreEl.textContent = topMatch.matchScore + '%';
    if (trackEl) trackEl.textContent = topMatch.careerPath.title;
  }
}

function selectTrack(index) {
  selectedTrackIndex = index;
  const rec = careerRecommendations[index];
  if (!rec) return;

  // Update tab styles
  document.querySelectorAll('.track-tab-btn').forEach((btn, idx) => {
    if (idx === index) btn.classList.add('active');
    else btn.classList.remove('active');
  });

  const path = rec.careerPath;

  // 1. Overview Card
  const titleEl = document.getElementById('track-title');
  const catEl = document.getElementById('track-category-badge');
  const salEl = document.getElementById('track-salary');
  const descEl = document.getElementById('track-description');

  if (titleEl) titleEl.textContent = path.title;
  if (catEl) catEl.textContent = path.category;
  if (salEl) salEl.textContent = path.averageMarketSalary || '$100,000+';
  if (descEl) descEl.textContent = path.description;

  // Core Technical Competencies
  const skillsContainer = document.getElementById('track-core-skills-list');
  if (skillsContainer && path.requiredCoreSkills) {
    const skills = path.requiredCoreSkills.split(',');
    skillsContainer.innerHTML = skills.map(s => {
      const trimmed = s.trim();
      const acquired = (rec.acquiredSkills || []).some(as => as.toLowerCase() === trimmed.toLowerCase());
      return acquired
        ? `<span class="skill-chip skill-chip-acquired">✓ ${escapeHtml(trimmed)}</span>`
        : `<span class="skill-chip skill-chip-target">○ ${escapeHtml(trimmed)}</span>`;
    }).join('');
  }

  // 2. Candidate Trajectory Breakdown
  const scoreEl = document.getElementById('eval-match-score');
  const affinityEl = document.getElementById('eval-affinity-pill');
  if (scoreEl) scoreEl.textContent = rec.matchScore + '%';

  if (affinityEl) {
    const rating = rec.readinessRating || 'EXPLORATORY';
    affinityEl.textContent = rec.readinessLabel || 'Exploratory';
    affinityEl.className = 'affinity-pill';
    if (rating === 'HIGH_AFFINITY') affinityEl.classList.add('affinity-high');
    else if (rating === 'MODERATE_AFFINITY') affinityEl.classList.add('affinity-moderate');
    else affinityEl.classList.add('affinity-exploratory');
  }

  // Breakdown Bars
  setBar('factor-skills', 'bar-skills', rec.skillsScore || 0);
  setBar('factor-exp', 'bar-exp', rec.experienceScore || 0);
  setBar('factor-proj', 'bar-proj', rec.projectScore || 0);
  setBar('factor-assess', 'bar-assess', rec.assessmentScore || 0);
  setBar('factor-edu', 'bar-edu', rec.educationScore || 0);

  const summaryEl = document.getElementById('eval-summary');
  if (summaryEl) summaryEl.textContent = rec.trajectorySummary || 'Evaluation complete.';

  // 3. Milestone Ladder (Levels 1 to 4)
  renderMilestoneLadder(path.milestones || [], rec);

  // 4. Next Frontier Skills
  renderNextFrontierSkills(rec);

  // 5. Stepping-Stone Jobs
  renderSteppingStones(rec.matchingJobs || []);
}

function setBar(labelId, barId, score) {
  const lbl = document.getElementById(labelId);
  const bar = document.getElementById(barId);
  if (lbl) lbl.textContent = score + '%';
  if (bar) bar.style.width = Math.min(100, Math.max(0, score)) + '%';
}

function renderMilestoneLadder(milestones, rec) {
  const container = document.getElementById('milestones-container');
  if (!container) return;

  if (!milestones || milestones.length === 0) {
    container.innerHTML = '<div style="color:#64748b;">No milestones defined for this track.</div>';
    return;
  }

  container.innerHTML = milestones.map(m => {
    let cardClass = '';
    let statusBadge = '';

    if (m.completed) {
      cardClass = 'milestone-completed';
      statusBadge = '<span class="badge" style="background:#dcfce7;color:#15803d;">COMPLETED ✓</span>';
    } else if (m.current) {
      cardClass = 'milestone-current';
      statusBadge = '<span class="badge" style="background:#dbeafe;color:#1d4ed8;font-weight:700;">CURRENT STANDING 📍</span>';
    } else if (m.target) {
      cardClass = 'milestone-target';
      statusBadge = '<span class="badge" style="background:#f3e8ff;color:#7e22ce;font-weight:700;">NEXT FRONTIER TARGET 🎯</span>';
    } else {
      statusBadge = '<span class="badge" style="background:#f1f5f9;color:#64748b;">UPCOMING MILESTONE 🔒</span>';
    }

    // Parse milestone skills
    let reqSkills = m.skillsRequired ? m.skillsRequired.split(',').map(s => s.trim()) : [];
    let skillChips = reqSkills.map(s => {
      const isAcquired = (m.acquiredSkills || []).some(as => as.toLowerCase() === s.toLowerCase()) ||
                         (rec.acquiredSkills || []).some(as => as.toLowerCase() === s.toLowerCase());
      return isAcquired
        ? `<span class="skill-chip skill-chip-acquired">✓ ${escapeHtml(s)}</span>`
        : `<span class="skill-chip skill-chip-missing">⚡ ${escapeHtml(s)}</span>`;
    }).join('');

    return `
      <div class="milestone-card ${cardClass}">
        <div class="milestone-badge-col">
          <div class="level-circle">L${m.levelOrder}</div>
        </div>
        <div class="milestone-content-col">
          <div class="milestone-header">
            <div class="milestone-title">${escapeHtml(m.levelName)}</div>
            <div>${statusBadge}</div>
          </div>
          <div class="milestone-meta">
            <span class="meta-tag">⏱️ Experience: ${escapeHtml(m.experienceYearsRange || '0+ Years')}</span>
            <span class="meta-tag" style="color:#047857;">💰 Salary: ${escapeHtml(m.salaryRange || 'Competitive')}</span>
          </div>
          <div style="color:#475569; font-size:0.88rem; line-height:1.45; margin-bottom:12px;">
            ${escapeHtml(m.milestoneDescription || '')}
          </div>
          <div style="margin-bottom:8px;">
            <div style="font-size:0.78rem; font-weight:700; color:#64748b; text-transform:uppercase; margin-bottom:6px;">
              Required Milestone Competencies:
            </div>
            <div>${skillChips}</div>
          </div>
          ${m.recommendedAction ? `
            <div class="action-guidance-box">
              <strong>💡 Actionable Career Guidance:</strong> ${escapeHtml(m.recommendedAction)}
            </div>
          ` : ''}
        </div>
      </div>
    `;
  }).join('');
}

function renderNextFrontierSkills(rec) {
  const targetMilestoneNameEl = document.getElementById('target-milestone-name');
  const container = document.getElementById('next-frontier-skills-list');
  if (!container) return;

  const targetM = rec.nextMilestone || rec.currentMilestone;
  if (targetMilestoneNameEl && targetM) {
    targetMilestoneNameEl.textContent = targetM.levelName;
  }

  const skills = rec.nextFrontierSkills || [];
  if (skills.length === 0) {
    container.innerHTML = `
      <div style="color:#059669; font-weight:600; font-size:0.9rem;">
        🎉 Outstanding! You have met all key technical competencies for this milestone level.
      </div>
    `;
    return;
  }

  container.innerHTML = skills.map(s => `
    <span class="skill-chip skill-chip-target" style="font-size:0.85rem; padding:6px 14px;">
      🎯 Master <strong>${escapeHtml(s)}</strong>
    </span>
  `).join('');
}

function renderSteppingStones(jobs) {
  const container = document.getElementById('stepping-stones-list');
  if (!container) return;

  if (!jobs || jobs.length === 0) {
    container.innerHTML = `
      <div style="grid-column: 1 / -1; padding: 20px; text-align:center; color:#64748b; background:#f8fafc; border-radius:10px;">
        No active stepping-stone postings found right now. Check back soon or browse all open vacancies.
      </div>
    `;
    return;
  }

  container.innerHTML = jobs.map(j => `
    <div class="card" style="border:1.5px solid #e2e8f0; display:flex; flex-direction:column; justify-content:space-between; padding:16px;">
      <div>
        <div style="display:flex; justify-content:space-between; align-items:flex-start; gap:8px;">
          <h4 style="margin:0; font-size:1.05rem; color:#0f172a;">${escapeHtml(j.title)}</h4>
          <span class="badge badge-info" style="font-size:0.75rem;">${escapeHtml(j.jobType || 'Full Time')}</span>
        </div>
        <div style="font-size:0.82rem; color:#475569; margin-top:4px; font-weight:600;">
          🏢 ${escapeHtml(j.company || 'Enterprise Partner')}
        </div>
        <div style="font-size:0.8rem; color:#64748b; margin-top:4px;">
          📍 ${escapeHtml(j.location || 'Remote')} &nbsp;|&nbsp; 💰 ${escapeHtml(j.salaryRange || 'Competitive')}
        </div>
        <div style="font-size:0.82rem; color:#334155; margin-top:8px; line-height:1.4; display:-webkit-box; -webkit-line-clamp:2; -webkit-box-orient:vertical; overflow:hidden;">
          ${escapeHtml(j.description || '')}
        </div>
      </div>
      <div style="margin-top:14px; padding-top:10px; border-top:1px solid #f1f5f9; display:flex; justify-content:flex-end;">
        <a href="../jobs.html?jobId=${j.jobId}" class="btn btn-primary btn-sm">View & Apply →</a>
      </div>
    </div>
  `).join('');
}

function renderFallbackTracks() {
  const container = document.getElementById('track-tabs-container');
  if (!container) return;

  container.innerHTML = allTracks.map((path, idx) => `
    <button class="track-tab-btn ${idx === 0 ? 'active' : ''}" onclick="selectFallbackTrack(${idx})">
      <div class="track-tab-title">${escapeHtml(path.title)}</div>
      <div style="font-size:0.75rem; color:#64748b; margin-top:4px;">${escapeHtml(path.category)}</div>
    </button>
  `).join('');
}

function selectFallbackTrack(index) {
  const path = allTracks[index];
  if (!path) return;

  document.querySelectorAll('.track-tab-btn').forEach((btn, idx) => {
    if (idx === index) btn.classList.add('active');
    else btn.classList.remove('active');
  });

  const titleEl = document.getElementById('track-title');
  const catEl = document.getElementById('track-category-badge');
  const salEl = document.getElementById('track-salary');
  const descEl = document.getElementById('track-description');

  if (titleEl) titleEl.textContent = path.title;
  if (catEl) catEl.textContent = path.category;
  if (salEl) salEl.textContent = path.averageMarketSalary || '$100,000+';
  if (descEl) descEl.textContent = path.description;

  renderMilestoneLadder(path.milestones || [], {});
}

function escapeHtml(str) {
  if (!str) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}
