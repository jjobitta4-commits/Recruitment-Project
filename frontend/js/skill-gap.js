/**
 * Core Innovation 2: Skill Gap Analyzer & Learning Roadmap Generator
 * Standalone Client Module
 */

async function openSkillGapModal(jobId) {
  if (typeof Auth !== 'undefined' && !Auth.isLoggedIn()) {
    showToast('Please log in as a candidate to view your personalized Skill Gap Analysis.', 'info');
    setTimeout(() => {
      window.location.href = 'login.html?redirect=' + encodeURIComponent(window.location.pathname + window.location.search);
    }, 1200);
    return;
  }

  const modal = document.getElementById('skill-gap-modal');
  if (!modal) return;

  const contentEl = document.getElementById('skill-gap-modal-content');
  if (contentEl) {
    contentEl.innerHTML = `
      <div style="text-align: center; padding: 3rem;">
        <div class="spinner" style="margin: 0 auto 1rem auto; width: 40px; height: 40px; border: 4px solid #f3f3f3; border-top: 4px solid #3b82f6; border-radius: 50%; animation: spin 1s linear infinite;"></div>
        <h3 style="color: #0f172a; margin-bottom: 0.5rem;">Synthesizing 5-Factor Skill Gap Audit...</h3>
        <p class="text-muted" style="font-size: 0.9rem;">Comparing technical skills, experience depth, degree taxonomy, project codebases, and test performance...</p>
      </div>
    `;
  }
  openModal('skill-gap-modal');

  try {
    const res = await authFetch(`/api/skill-gap?jobId=${jobId}`);
    if (res.success && res.data) {
      renderSkillGapReport(res.data);
    } else {
      if (contentEl) {
        contentEl.innerHTML = `
          <div style="text-align: center; padding: 2.5rem;">
            <p class="text-danger font-bold">${res.message || 'Failed to generate skill gap report.'}</p>
            <button class="btn btn-secondary btn-sm mt-2" onclick="closeModal('skill-gap-modal')">Close</button>
          </div>
        `;
      }
    }
  } catch (err) {
    console.error('Skill gap fetch error:', err);
    if (contentEl) {
      contentEl.innerHTML = '<p class="text-danger text-center" style="padding: 2rem;">Error connecting to Skill Gap Engine.</p>';
    }
  }
}

function renderSkillGapReport(report) {
  const contentEl = document.getElementById('skill-gap-modal-content');
  if (!contentEl) return;

  const score = report.overallScore || 0;
  let colorTheme = '#10b981'; // Green
  let badgeCls = 'badge-success';
  if (score < 50) {
    colorTheme = '#ef4444'; // Red
    badgeCls = 'badge-danger';
  } else if (score < 70) {
    colorTheme = '#f59e0b'; // Amber
    badgeCls = 'badge-warning';
  } else if (score < 85) {
    colorTheme = '#3b82f6'; // Blue
    badgeCls = 'badge-info';
  }

  // Mandatory Missing Chips
  let mandatoryMissingHtml = '';
  if (report.missingMandatorySkills && report.missingMandatorySkills.length > 0) {
    mandatoryMissingHtml = report.missingMandatorySkills.map(s => `
      <span class="skill-gap-pill critical">
        ⚠️ <strong>${s.skillName}</strong> (${s.minYearsRequired}y+ req)
      </span>
    `).join('');
  } else {
    mandatoryMissingHtml = '<span style="color: #10b981; font-weight: 600; font-size: 0.85rem;">✅ All mandatory prerequisites met!</span>';
  }

  // Preferred Missing Chips
  let preferredMissingHtml = '';
  if (report.missingPreferredSkills && report.missingPreferredSkills.length > 0) {
    preferredMissingHtml = report.missingPreferredSkills.map(s => `
      <span class="skill-gap-pill optional">
        🔵 ${s.skillName} (${s.category || 'Bonus'})
      </span>
    `).join('');
  } else {
    preferredMissingHtml = '<span class="text-muted" style="font-size: 0.85rem;">No preferred skill gaps.</span>';
  }

  // Proficiency Gaps
  let profGapsHtml = '';
  if (report.proficiencyGaps && report.proficiencyGaps.length > 0) {
    profGapsHtml = report.proficiencyGaps.map(g => `
      <div style="background: #fffbeb; border: 1px solid #fde68a; padding: 0.5rem 0.75rem; border-radius: 6px; margin-bottom: 0.4rem; font-size: 0.85rem; color: #92400e;">
        <strong>${g.skillName}:</strong> ${g.gapDescription}
      </div>
    `).join('');
  }

  // Acquired Skills Chips
  let acquiredHtml = '';
  if (report.acquiredSkills && report.acquiredSkills.length > 0) {
    acquiredHtml = report.acquiredSkills.map(s => `
      <span class="skill-gap-pill acquired">
        ✓ ${s.skillName}
      </span>
    `).join('');
  } else {
    acquiredHtml = '<span class="text-muted" style="font-size: 0.85rem;">None</span>';
  }

  // Roadmap Timeline Steps
  let roadmapHtml = '';
  if (report.learningRoadmap && report.learningRoadmap.length > 0) {
    roadmapHtml = report.learningRoadmap.map(step => {
      let urgencyBadge = '';
      if (step.urgency === 'CRITICAL') {
        urgencyBadge = '<span class="badge badge-danger" style="font-size: 0.72rem; padding: 0.2rem 0.5rem;">CRITICAL PREREQUISITE</span>';
      } else if (step.urgency === 'RECOMMENDED') {
        urgencyBadge = '<span class="badge badge-warning" style="font-size: 0.72rem; padding: 0.2rem 0.5rem;">EXPERIENCE DEPTH</span>';
      } else {
        urgencyBadge = '<span class="badge badge-info" style="font-size: 0.72rem; padding: 0.2rem 0.5rem;">BONUS ENHANCEMENT</span>';
      }

      const resourceLinks = step.resources && step.resources.length > 0
        ? step.resources.map(r => `<a href="${r.url}" target="_blank" style="color: #2563eb; text-decoration: underline; font-size: 0.8rem; margin-right: 0.75rem;">📖 ${r.title}</a>`).join('')
        : '';

      return `
        <div class="roadmap-timeline-step">
          <div class="roadmap-step-circle">${step.stepNumber}</div>
          <div class="roadmap-step-content">
            <div class="flex justify-between items-center mb-1" style="flex-wrap: wrap; gap: 0.4rem;">
              <h4 style="margin: 0; font-size: 0.95rem; color: #0f172a;">${step.actionTitle}</h4>
              <div class="flex items-center gap-1">
                ${urgencyBadge}
                <span style="font-size: 0.75rem; color: #64748b; font-weight: 600;">⏱️ ${step.estimatedTime}</span>
              </div>
            </div>
            <p style="font-size: 0.85rem; color: #475569; margin-bottom: 0.5rem; line-height: 1.45;">
              ${step.actionDescription}
            </p>
            ${step.suggestedProject ? `
              <div style="background: #f8fafc; border-left: 3px solid #3b82f6; padding: 0.5rem 0.75rem; border-radius: 4px; font-size: 0.82rem; margin-bottom: 0.5rem; color: #1e293b;">
                <strong>🛠️ Milestone Project:</strong> ${step.suggestedProject}
              </div>
            ` : ''}
            <div style="margin-top: 0.3rem;">${resourceLinks}</div>
          </div>
        </div>
      `;
    }).join('');
  } else {
    roadmapHtml = '<p class="text-muted" style="padding: 1rem; text-align: center;">No learning steps required. You already meet 100% of this role\'s requirements!</p>';
  }

  contentEl.innerHTML = `
    <!-- Header Summary -->
    <div style="background: linear-gradient(135deg, #f8fafc 0%, #edf2f7 100%); border-radius: 10px; padding: 1.25rem; border: 1px solid #e2e8f0; margin-bottom: 1.25rem;">
      <div class="flex justify-between items-center" style="flex-wrap: wrap; gap: 1rem;">
        <div>
          <span style="font-size: 0.75rem; text-transform: uppercase; font-weight: 700; color: #64748b; letter-spacing: 0.05em;">Target Vacancy</span>
          <h2 style="margin: 0.2rem 0; font-size: 1.35rem; color: #0f172a;">${report.jobTitle}</h2>
          <span style="font-size: 0.9rem; color: #475569;">🏢 ${report.companyName}</span>
        </div>
        <div style="text-align: right; display: flex; align-items: center; gap: 1rem;">
          <div>
            <div style="font-size: 2.4rem; font-weight: 800; color: ${colorTheme}; line-height: 1;">${score}%</div>
            <span class="badge ${badgeCls}" style="margin-top: 0.2rem; font-weight: 700;">${report.matchLevel} MATCH</span>
          </div>
        </div>
      </div>
      <p style="font-size: 0.85rem; color: #475569; margin: 0.75rem 0 0 0; line-height: 1.5; border-top: 1px solid #e2e8f0; padding-top: 0.6rem;">
        ${report.executiveSummary}
      </p>
    </div>

    <!-- 5-Factor Weighted Compatibility Breakdown -->
    <div class="card mb-3" style="padding: 1rem; border: 1px solid #e2e8f0; background: #fff;">
      <h4 style="font-size: 0.9rem; color: #0f172a; margin-bottom: 0.75rem; text-transform: uppercase; letter-spacing: 0.04em;">
        📊 5-Factor Weighted Compatibility Formula Breakdown
      </h4>
      <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 0.75rem;">
        <div>
          <div class="flex justify-between text-muted" style="font-size: 0.78rem; margin-bottom: 0.25rem;">
            <span>Technical Skills (45%)</span>
            <strong>${report.skillScore}%</strong>
          </div>
          <div class="metric-progress-bar"><div class="metric-progress-fill" style="width: ${report.skillScore}%; background: #3b82f6;"></div></div>
        </div>
        <div>
          <div class="flex justify-between text-muted" style="font-size: 0.78rem; margin-bottom: 0.25rem;">
            <span>Experience Depth (20%)</span>
            <strong>${report.experienceScore}%</strong>
          </div>
          <div class="metric-progress-bar"><div class="metric-progress-fill" style="width: ${report.experienceScore}%; background: #10b981;"></div></div>
        </div>
        <div>
          <div class="flex justify-between text-muted" style="font-size: 0.78rem; margin-bottom: 0.25rem;">
            <span>Education Level (10%)</span>
            <strong>${report.educationScore}%</strong>
          </div>
          <div class="metric-progress-bar"><div class="metric-progress-fill" style="width: ${report.educationScore}%; background: #8b5cf6;"></div></div>
        </div>
        <div>
          <div class="flex justify-between text-muted" style="font-size: 0.78rem; margin-bottom: 0.25rem;">
            <span>Practical Projects (10%)</span>
            <strong>${report.projectScore}%</strong>
          </div>
          <div class="metric-progress-bar"><div class="metric-progress-fill" style="width: ${report.projectScore}%; background: #f59e0b;"></div></div>
        </div>
        <div>
          <div class="flex justify-between text-muted" style="font-size: 0.78rem; margin-bottom: 0.25rem;">
            <span>Assessment Score (15%)</span>
            <strong>${report.assessmentScore}%</strong>
          </div>
          <div class="metric-progress-bar"><div class="metric-progress-fill" style="width: ${report.assessmentScore}%; background: #06b6d4;"></div></div>
        </div>
      </div>
    </div>

    <!-- Deficiency Breakdown Grid -->
    <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; margin-bottom: 1.25rem;">
      <div class="card" style="padding: 1rem; border: 1px solid #fee2e2; background: #fffaf0;">
        <h4 style="font-size: 0.85rem; color: #991b1b; margin-bottom: 0.5rem; text-transform: uppercase;">
          🔴 Missing Mandatory Prerequisites (${report.missingMandatorySkills ? report.missingMandatorySkills.length : 0})
        </h4>
        <div style="display: flex; flex-wrap: wrap; gap: 0.4rem;">${mandatoryMissingHtml}</div>
        ${profGapsHtml ? `<div style="margin-top: 0.75rem;"><strong style="font-size: 0.78rem; color: #92400e;">Experience Depth Shortfalls:</strong>${profGapsHtml}</div>` : ''}
      </div>

      <div class="card" style="padding: 1rem; border: 1px solid #e0f2fe; background: #f8fafc;">
        <h4 style="font-size: 0.85rem; color: #0369a1; margin-bottom: 0.5rem; text-transform: uppercase;">
          🔵 Preferred / Bonus Competencies (${report.missingPreferredSkills ? report.missingPreferredSkills.length : 0})
        </h4>
        <div style="display: flex; flex-wrap: wrap; gap: 0.4rem; margin-bottom: 0.75rem;">${preferredMissingHtml}</div>
        
        <h4 style="font-size: 0.85rem; color: #166534; margin-bottom: 0.4rem; text-transform: uppercase;">
          🟢 Acquired &amp; Validated Skills (${report.acquiredSkills ? report.acquiredSkills.length : 0})
        </h4>
        <div style="display: flex; flex-wrap: wrap; gap: 0.35rem;">${acquiredHtml}</div>
      </div>
    </div>

    <!-- Personalized Learning Roadmap Timeline -->
    <div class="card" style="padding: 1.25rem; border: 1px solid #e2e8f0;">
      <div class="flex justify-between items-center mb-3">
        <div>
          <h3 style="font-size: 1.05rem; color: #0f172a; margin: 0;">🗺️ Personalized Step-by-Step Learning Roadmap</h3>
          <p class="text-muted" style="font-size: 0.82rem; margin: 0.2rem 0 0 0;">
            Estimated time to bridge all technical gaps: <strong>~${report.estimatedWeeksTotal} Weeks</strong>
          </p>
        </div>
        <span class="badge" style="background: #e0e7ff; color: #3730a3; font-weight: bold;">
          ${report.learningRoadmap ? report.learningRoadmap.length : 0} Action Milestones
        </span>
      </div>

      <div class="roadmap-timeline">
        ${roadmapHtml}
      </div>
    </div>
  `;
}
