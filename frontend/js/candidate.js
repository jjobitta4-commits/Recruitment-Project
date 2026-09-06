/**
 * Candidate Profile & Skill Management Script
 * Pure Vanilla JavaScript
 */

let candidateData = null;
let masterSkillsList = [];
let detectedSkillsCache = [];

document.addEventListener('DOMContentLoaded', () => {
  Auth.requireAuth('candidate');
  setupTabs();
  loadFullCandidateProfile();
  loadMasterSkillsTaxonomy();
  setupEventListeners();
});

function setupTabs() {
  const tabs = document.querySelectorAll('.profile-nav-tab');
  tabs.forEach(tab => {
    tab.addEventListener('click', () => {
      tabs.forEach(t => t.classList.remove('active'));
      document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));

      tab.classList.add('active');
      const targetId = tab.getAttribute('data-tab');
      const target = document.getElementById(targetId);
      if (target) target.classList.add('active');
    });
  });
}

async function loadFullCandidateProfile() {
  try {
    const res = await API.get('/api/candidate/profile');
    if (res && res.success && res.data) {
      candidateData = res.data;
      renderPersonalInfo(candidateData);
      renderDeclaredSkills(candidateData.skills || []);
      renderEducationList(candidateData.education || []);
      renderExperienceList(candidateData.experience || []);
      renderProjectsList(candidateData.projects || []);
      updateCompletenessMeter(candidateData.profileCompletion || 0);
    }
  } catch (err) {
    console.error('Error loading candidate profile:', err);
  }
}

function updateCompletenessMeter(pct) {
  const text = document.getElementById('profile-pct-text');
  const bar = document.getElementById('profile-progress-bar');
  if (text) text.textContent = pct + '%';
  if (bar) bar.style.width = pct + '%';
}

function renderPersonalInfo(c) {
  const setVal = (id, val) => {
    const el = document.getElementById(id);
    if (el) el.value = val || '';
  };
  setVal('cand-fullName', c.fullName);
  setVal('cand-email', c.email);
  setVal('cand-phone', c.phone);
  setVal('cand-dob', c.dob ? c.dob.substring(0, 10) : '');
  setVal('cand-gender', c.gender);
  setVal('cand-city', c.city);
  setVal('cand-country', c.country);
  setVal('cand-address', c.address);
  setVal('cand-bio', c.bio);
}

async function loadMasterSkillsTaxonomy() {
  try {
    const res = await API.get('/api/candidate/skills');
    if (res && res.success && res.data) {
      masterSkillsList = res.data.masterSkills || [];
      populateSkillSelector(masterSkillsList);
      if (res.data.candidateSkills) {
        renderDeclaredSkills(res.data.candidateSkills);
      }
    }
  } catch (err) {
    console.error(err);
  }
}

function populateSkillSelector(skills) {
  const selector = document.getElementById('skill-selector');
  if (!selector) return;

  selector.innerHTML = '<option value="">-- Choose Skill --</option>';
  const groups = {};
  skills.forEach(s => {
    const cat = s.category || 'General';
    if (!groups[cat]) groups[cat] = [];
    groups[cat].push(s);
  });

  Object.keys(groups).sort().forEach(cat => {
    const optgroup = document.createElement('optgroup');
    optgroup.label = cat;
    groups[cat].forEach(s => {
      const opt = document.createElement('option');
      opt.value = s.skillId;
      opt.textContent = s.name;
      optgroup.appendChild(opt);
    });
    selector.appendChild(optgroup);
  });
}

function renderDeclaredSkills(skills) {
  const container = document.getElementById('candidate-skills-container');
  const countBadge = document.getElementById('skills-count-badge');
  if (!container) return;

  if (countBadge) countBadge.textContent = (skills ? skills.length : 0) + ' Skills';

  if (!skills || skills.length === 0) {
    container.innerHTML = '<p class="text-muted">No technical skills declared yet. Select from the taxonomy above or extract from resume.</p>';
    return;
  }

  container.innerHTML = skills.map(s => `
    <span class="skill-tag-pill">
      <strong>${s.skillName}</strong>
      <span style="font-size: 0.75rem; color: #2563eb; background: #eff6ff; padding: 2px 6px; border-radius: 999px;">${s.proficiencyLevel || 'Intermediate'} &bull; ${s.yearsExperience || 1}y</span>
      <span class="remove-btn" title="Remove" onclick="removeSkill(${s.skillId})">&times;</span>
    </span>
  `).join('');
}

async function removeSkill(skillId) {
  try {
    const res = await API.delete('/api/candidate/skills?skillId=' + skillId);
    if (res && res.success) {
      showToast('Skill removed', 'info');
      loadFullCandidateProfile();
    }
  } catch (err) {
    showToast('Failed to remove skill', 'error');
  }
}

function renderEducationList(list) {
  const tbody = document.getElementById('education-table-body');
  if (!tbody) return;

  if (!list || list.length === 0) {
    tbody.innerHTML = '<tr><td colspan="6" class="text-center text-muted">No education records added yet.</td></tr>';
    return;
  }

  tbody.innerHTML = list.map(e => `
    <tr>
      <td><strong>${e.degree}</strong></td>
      <td>${e.institution}</td>
      <td>${e.fieldOfStudy || '-'}</td>
      <td>${(e.startYear || '') + (e.endYear ? ' - ' + e.endYear : '')}</td>
      <td><span class="badge badge-secondary">${e.gradeOrGpa || '-'}</span></td>
      <td>
        <button class="btn btn-sm btn-danger" style="padding: 2px 8px; font-size: 0.78rem;" onclick="deleteEducation(${e.educationId})">Delete</button>
      </td>
    </tr>
  `).join('');
}

async function deleteEducation(id) {
  if (!confirm('Are you sure you want to delete this education entry?')) return;
  try {
    await API.delete('/api/candidate/education?educationId=' + id);
    showToast('Education deleted', 'info');
    loadFullCandidateProfile();
  } catch (e) {
    showToast('Failed to delete', 'error');
  }
}

function renderExperienceList(list) {
  const tbody = document.getElementById('experience-table-body');
  if (!tbody) return;

  if (!list || list.length === 0) {
    tbody.innerHTML = '<tr><td colspan="5" class="text-center text-muted">No experience entries recorded.</td></tr>';
    return;
  }

  tbody.innerHTML = list.map(exp => `
    <tr>
      <td><strong>${exp.jobTitle}</strong></td>
      <td>🏢 ${exp.companyName}</td>
      <td>${exp.startDate || ''} &rarr; ${exp.current ? '<span class="badge badge-success">Current</span>' : (exp.endDate || 'Present')}</td>
      <td style="font-size: 0.88rem;">${exp.description || '-'}</td>
      <td>
        <button class="btn btn-sm btn-danger" style="padding: 2px 8px; font-size: 0.78rem;" onclick="deleteExperience(${exp.experienceId})">Delete</button>
      </td>
    </tr>
  `).join('');
}

async function deleteExperience(id) {
  if (!confirm('Delete this experience entry?')) return;
  try {
    await API.delete('/api/candidate/experience?experienceId=' + id);
    showToast('Experience deleted', 'info');
    loadFullCandidateProfile();
  } catch (e) {
    showToast('Failed to delete', 'error');
  }
}

function renderProjectsList(list) {
  const container = document.getElementById('projects-container');
  if (!container) return;

  if (!list || list.length === 0) {
    container.innerHTML = '<p class="text-muted">No projects added yet.</p>';
    return;
  }

  container.innerHTML = list.map(p => `
    <div style="padding: 1rem; background: var(--surface-alt); border-radius: var(--radius); border: 1px solid var(--border); margin-bottom: 0.75rem;">
      <div class="flex justify-between items-center mb-1">
        <h4 style="margin: 0;">${p.title}</h4>
        <button class="btn btn-sm btn-danger" style="padding: 2px 8px; font-size: 0.75rem;" onclick="deleteProject(${p.projectId})">Delete</button>
      </div>
      <div class="text-muted mb-1" style="font-size: 0.85rem;"><strong>Tech:</strong> ${p.technologiesUsed || 'N/A'}</div>
      <p style="font-size: 0.9rem; margin-bottom: 0.5rem;">${p.description || ''}</p>
      ${p.projectUrl ? `<a href="${p.projectUrl}" target="_blank" class="btn btn-outline btn-sm" style="font-size: 0.78rem;">🔗 View Project</a>` : ''}
    </div>
  `).join('');
}

async function deleteProject(id) {
  if (!confirm('Delete this project?')) return;
  try {
    await API.delete('/api/candidate/projects?projectId=' + id);
    showToast('Project deleted', 'info');
    loadFullCandidateProfile();
  } catch (e) {
    showToast('Failed to delete', 'error');
  }
}

function setupEventListeners() {
  // Personal Info Form
  const profileForm = document.getElementById('candidate-profile-form');
  if (profileForm) {
    profileForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const payload = {
        fullName: document.getElementById('cand-fullName').value.trim(),
        phone: document.getElementById('cand-phone').value.trim(),
        dob: document.getElementById('cand-dob').value || null,
        gender: document.getElementById('cand-gender').value,
        city: document.getElementById('cand-city').value.trim(),
        country: document.getElementById('cand-country').value.trim(),
        address: document.getElementById('cand-address').value.trim(),
        bio: document.getElementById('cand-bio').value.trim()
      };

      const res = await API.put('/api/candidate/profile', payload);
      if (res && res.success) {
        showToast('Personal details updated successfully!', 'success');
        loadFullCandidateProfile();
      } else {
        showToast(res ? res.message : 'Error updating profile', 'error');
      }
    });
  }

  // Add Skill Form
  const skillForm = document.getElementById('form-add-skill');
  if (skillForm) {
    skillForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const skillId = document.getElementById('skill-selector').value;
      const level = document.getElementById('skill-proficiency').value;
      const years = document.getElementById('skill-years').value;

      if (!skillId) {
        showToast('Please select a skill', 'error');
        return;
      }

      const res = await API.post('/api/candidate/skills', {
        skillId: parseInt(skillId),
        proficiencyLevel: level,
        yearsExperience: parseInt(years) || 1
      });

      if (res && res.success) {
        showToast('Skill added!', 'success');
        loadFullCandidateProfile();
      }
    });
  }

  // Add Education Form
  const eduForm = document.getElementById('form-add-edu');
  if (eduForm) {
    eduForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const payload = {
        degree: document.getElementById('edu-degree').value.trim(),
        institution: document.getElementById('edu-inst').value.trim(),
        fieldOfStudy: document.getElementById('edu-field').value.trim(),
        startYear: parseInt(document.getElementById('edu-start').value) || null,
        endYear: parseInt(document.getElementById('edu-end').value) || null,
        gradeOrGpa: document.getElementById('edu-grade').value.trim()
      };

      const res = await API.post('/api/candidate/education', payload);
      if (res && res.success) {
        showToast('Education added!', 'success');
        eduForm.reset();
        loadFullCandidateProfile();
      }
    });
  }

  // Add Experience Form
  const expForm = document.getElementById('form-add-exp');
  if (expForm) {
    expForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const payload = {
        companyName: document.getElementById('exp-company').value.trim(),
        jobTitle: document.getElementById('exp-title').value.trim(),
        startDate: document.getElementById('exp-start').value || null,
        endDate: document.getElementById('exp-end').value || null,
        isCurrent: document.getElementById('exp-current').checked,
        description: document.getElementById('exp-desc').value.trim()
      };

      const res = await API.post('/api/candidate/experience', payload);
      if (res && res.success) {
        showToast('Experience added!', 'success');
        expForm.reset();
        loadFullCandidateProfile();
      }
    });
  }

  // Add Project Form
  const projForm = document.getElementById('form-add-proj');
  if (projForm) {
    projForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const payload = {
        title: document.getElementById('proj-title').value.trim(),
        technologiesUsed: document.getElementById('proj-tech').value.trim(),
        projectUrl: document.getElementById('proj-url').value.trim(),
        description: document.getElementById('proj-desc').value.trim()
      };

      const res = await API.post('/api/candidate/projects', payload);
      if (res && res.success) {
        showToast('Project added!', 'success');
        projForm.reset();
        loadFullCandidateProfile();
      }
    });
  }

  // Resume Keyword Extractor: Load Sample Text
  const loadSampleBtn = document.getElementById('btn-load-sample-resume');
  if (loadSampleBtn) {
    loadSampleBtn.addEventListener('click', () => {
      document.getElementById('resume-raw-text').value = 
        `John Doe — Full Stack Java Engineer
Summary: Results-oriented software engineer with 3+ years experience designing high-throughput Core Java microservices, REST APIs, and responsive web platforms.
Skills: Java, MySQL, JDBC, HTML5, CSS3, JavaScript, Git, Docker, Kubernetes, Problem Solving, AWS.
Experience: Backend Developer at Apex Cloud Solutions. Designed JDBC batch pipelines and tuned relational database queries.`;
    });
  }

  // Resume Keyword Extractor: Trigger NLP Extraction
  const extractBtn = document.getElementById('btn-analyze-resume-text');
  if (extractBtn) {
    extractBtn.addEventListener('click', async () => {
      const text = document.getElementById('resume-raw-text').value.trim();
      if (!text) {
        showToast('Please paste resume text to extract keywords.', 'error');
        return;
      }

      extractBtn.disabled = true;
      extractBtn.textContent = 'Extracting...';

      try {
        const res = await API.post('/api/candidate/parse-resume-text', { resumeText: text });
        if (res && res.success && res.data) {
          detectedSkillsCache = res.data.detectedSkills || [];
          renderExtractedSkills(detectedSkillsCache);
        }
      } catch (err) {
        showToast('Failed to extract keywords', 'error');
      } finally {
        extractBtn.disabled = false;
        extractBtn.textContent = '🔍 Extract Keywords';
      }
    });
  }

  // Add all detected skills to profile
  const addAllBtn = document.getElementById('btn-add-all-extracted');
  if (addAllBtn) {
    addAllBtn.addEventListener('click', async () => {
      if (!detectedSkillsCache || detectedSkillsCache.length === 0) return;
      addAllBtn.disabled = true;
      addAllBtn.textContent = 'Adding...';

      for (const s of detectedSkillsCache) {
        await API.post('/api/candidate/skills', {
          skillId: s.skillId,
          proficiencyLevel: 'Advanced',
          yearsExperience: 2
        });
      }

      showToast(`Added ${detectedSkillsCache.length} detected skills to your profile!`, 'success');
      addAllBtn.disabled = false;
      addAllBtn.textContent = '✓ Added to Profile';
      loadFullCandidateProfile();
    });
  }
}

function renderExtractedSkills(skills) {
  const box = document.getElementById('extractor-results-box');
  const countSpan = document.getElementById('extracted-count');
  const chipsContainer = document.getElementById('extracted-skills-chips');
  if (!box || !chipsContainer) return;

  countSpan.textContent = skills.length;
  box.style.display = 'block';

  if (skills.length === 0) {
    chipsContainer.innerHTML = '<p class="text-muted">No recognized skills found in the text.</p>';
    return;
  }

  chipsContainer.innerHTML = skills.map(s => `
    <span class="badge badge-success" style="font-size: 0.85rem; padding: 0.4rem 0.75rem; margin: 0.25rem;">
      ✓ ${s.name} <small style="opacity: 0.8;">(${s.category})</small>
    </span>
  `).join('');
}
