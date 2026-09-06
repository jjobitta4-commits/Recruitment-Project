/**
 * Core Innovation 4: Adaptive Online Assessment Engine Frontend Logic.
 * Manages real-time test steering, live difficulty animations, and score report modals.
 */

let activeSession = null;
let currentQuestionId = null;
let selectedOptionValue = null;
let timerInterval = null;
let remainingSeconds = 900; // 15 minutes

document.addEventListener('DOMContentLoaded', () => {
  if (typeof renderNavbar === 'function') {
    renderNavbar('assessments');
  }
  loadAssessmentsPage();
});

async function loadAssessmentsPage() {
  await Promise.all([
    fetchAvailableAssessments(),
    fetchPastAttempts()
  ]);
}

async function fetchAvailableAssessments() {
  const container = document.getElementById('assessments-catalog-grid');
  if (!container) return;

  try {
    const res = await authFetch('/api/assessments');
    if (res.success && Array.isArray(res.data)) {
      const tests = res.data;
      document.getElementById('stat-assess-available').textContent = tests.length;

      if (tests.length === 0) {
        container.innerHTML = '<div class="text-center text-muted" style="grid-column: 1 / -1; padding: 2rem;">No assessments currently published.</div>';
        return;
      }

      container.innerHTML = tests.map(t => `
        <div class="card job-card" style="display: flex; flex-direction: column; justify-content: space-between;">
          <div>
            <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 0.5rem;">
              <h4 style="margin: 0; color: #0f172a; font-size: 1.1rem;">${t.title}</h4>
              <span class="badge" style="background: #eff6ff; color: #2563eb; font-weight: 700;">Adaptive</span>
            </div>
            ${t.jobTitle ? `<div style="font-size: 0.85rem; color: #64748b; margin-bottom: 0.5rem;">💼 Linked to: <strong>${t.jobTitle}</strong> (${t.companyName || 'Enterprise'})</div>` : ''}
            <p class="text-muted" style="font-size: 0.88rem; line-height: 1.5; margin-bottom: 1rem;">${t.description || 'Comprehensive evaluation covering system design, relational queries, and algorithms.'}</p>

            <div style="display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 1rem;">
              <span class="badge" style="background: #f1f5f9; color: #334155;">⏱️ ${t.timeLimitMinutes || 20} Mins</span>
              <span class="badge" style="background: #f1f5f9; color: #334155;">🎯 Pass: ${t.passingScore || 60}%</span>
              <span class="badge" style="background: #f1f5f9; color: #334155;">❓ 5 Adaptive Questions</span>
            </div>

            <div style="background: #f8fafc; padding: 0.5rem 0.75rem; border-radius: 6px; border: 1px solid #e2e8f0; font-size: 0.78rem; margin-bottom: 1rem;">
              <span style="font-weight: 700; color: #475569;">Multi-Tier Pool:</span>
              <span style="color: #059669; margin-left: 6px;">🟢 ${t.easyQuestionsCount || 0} Easy</span>
              <span style="color: #d97706; margin-left: 6px;">🟡 ${t.mediumQuestionsCount || 0} Med</span>
              <span style="color: #dc2626; margin-left: 6px;">🔴 ${t.hardQuestionsCount || 0} Hard</span>
            </div>
          </div>

          <button onclick="startAdaptiveTest(${t.assessmentId}, '${encodeURIComponent(t.title)}')" class="btn btn-primary" style="width: 100%;">
            ⚡ Start Adaptive Test
          </button>
        </div>
      `).join('');
    }
  } catch (err) {
    container.innerHTML = '<div class="text-center text-danger" style="grid-column: 1 / -1;">Failed to load assessments.</div>';
  }
}

async function fetchPastAttempts() {
  const tbody = document.getElementById('attempts-table-tbody');
  if (!tbody) return;

  try {
    const res = await authFetch('/api/assessments/attempts');
    if (res.success && Array.isArray(res.data)) {
      const attempts = res.data;
      document.getElementById('stat-assess-completed').textContent = attempts.length;

      if (attempts.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" class="text-center text-muted" style="padding: 2rem;">No test attempts recorded yet. Take a test above to activate your assessment score!</td></tr>';
        return;
      }

      // Calculate stats
      let totalPct = 0;
      let hasHard = false;
      let hasMed = false;

      attempts.forEach(at => {
        totalPct += (at.percentageScore || 0);
        if (at.difficultyReached === 'Hard') hasHard = true;
        if (at.difficultyReached === 'Medium') hasMed = true;
      });

      const avg = Math.round(totalPct / attempts.length);
      document.getElementById('stat-assess-avg-score').textContent = avg + '%';
      document.getElementById('stat-assess-peak-tier').textContent = hasHard ? 'Hard 🔴' : (hasMed ? 'Medium 🟡' : 'Easy 🟢');

      tbody.innerHTML = attempts.map(at => {
        const isPass = at.passed || (at.percentageScore >= at.passingScore);
        let tierClass = 'diff-easy';
        if (at.difficultyReached === 'Hard') tierClass = 'diff-hard';
        else if (at.difficultyReached === 'Medium') tierClass = 'diff-medium';

        return `
          <tr>
            <td>
              <strong>${at.assessmentTitle}</strong>
              <div style="font-size: 0.78rem; color: #64748b;">Passing requirement: ${at.passingScore}%</div>
            </td>
            <td>${formatDate(at.completedAt)}</td>
            <td>
              <div style="font-weight: 700; font-size: 1rem; color: ${isPass ? '#15803d' : '#b91c1c'};">
                ${at.percentageScore}%
              </div>
              <div class="progress-bar-bg" style="height: 5px; width: 90px; margin-top: 3px;">
                <div class="progress-bar-fill" style="width: ${at.percentageScore}%; background: ${isPass ? '#22c55e' : '#ef4444'};"></div>
              </div>
            </td>
            <td>
              <span class="diff-badge ${tierClass}">
                ${at.difficultyReached === 'Hard' ? '🔴' : (at.difficultyReached === 'Medium' ? '🟡' : '🟢')}
                ${at.difficultyReached} Tier
              </span>
            </td>
            <td>
              <span class="badge" style="background: ${isPass ? '#ecfdf5' : '#fef2f2'}; color: ${isPass ? '#065f46' : '#991b1b'}; font-weight: 700; border: 1px solid ${isPass ? '#a7f3d0' : '#fecaca'};">
                ${isPass ? '✓ PASSED' : '✕ NEEDS IMPROVEMENT'}
              </span>
            </td>
            <td>
              <button onclick="viewAttemptBreakdown(${at.attemptId})" class="btn btn-secondary btn-sm">
                🔍 Review
              </button>
            </td>
          </tr>
        `;
      }).join('');
    }
  } catch (err) {
    tbody.innerHTML = '<tr><td colspan="6" class="text-center text-danger">Failed to load attempts history.</td></tr>';
  }
}

async function startAdaptiveTest(assessmentId, encTitle) {
  const title = decodeURIComponent(encTitle);
  if (!confirm(`Start Adaptive Online Assessment: "${title}"?\n\nThe engine will steer difficulty in real-time between Easy, Medium, and Hard.`)) {
    return;
  }

  try {
    const res = await authFetch('/api/assessments/start', {
      method: 'POST',
      body: JSON.stringify({ assessmentId })
    });

    if (res.success && res.data) {
      activeSession = res.data;
      openModal('adaptive-test-modal');
      startTestTimer(activeSession.totalQuestionsLimit * 3 * 60); // 3 mins per question
      renderAdaptiveQuestion(activeSession);
    } else {
      alert(res.message || 'Failed to start test session.');
    }
  } catch (err) {
    alert('Error initiating test: ' + err.message);
  }
}

function renderAdaptiveQuestion(session) {
  const q = session.currentQuestion;
  if (!q) return;

  currentQuestionId = q.questionId;
  selectedOptionValue = null;

  // Header & Counter
  document.getElementById('test-modal-title').textContent = session.assessmentTitle || 'Adaptive Assessment';
  document.getElementById('test-q-counter').textContent = `Question ${session.currentQuestionNumber} of ${session.totalQuestionsLimit}`;

  // Difficulty Badge
  const diffBadge = document.getElementById('test-current-diff-badge');
  const diff = q.difficulty || session.currentDifficulty || 'Medium';
  if (diff === 'Hard') {
    diffBadge.className = 'diff-badge diff-hard';
    diffBadge.innerHTML = '🔴 Hard Tier (+20 pts)';
  } else if (diff === 'Medium') {
    diffBadge.className = 'diff-badge diff-medium';
    diffBadge.innerHTML = '🟡 Medium Tier (+15 pts)';
  } else {
    diffBadge.className = 'diff-badge diff-easy';
    diffBadge.innerHTML = '🟢 Easy Tier (+10 pts)';
  }

  // Trajectory Path
  const trajContainer = document.getElementById('test-trajectory-container');
  const trajectory = session.difficultyTrajectory || [];
  if (trajectory.length === 0) {
    trajContainer.innerHTML = '<span class="trajectory-step">Start ➔ Medium</span>';
  } else {
    trajContainer.innerHTML = trajectory.map((t, idx) => {
      const icon = t === 'Hard' ? '🔴' : (t === 'Medium' ? '🟡' : '🟢');
      return `<span class="trajectory-step">${icon} Q${idx + 1}: ${t}</span>`;
    }).join(' <span style="color: #94a3b8;">➔</span> ');
  }

  // Steering Alert
  const alertEl = document.getElementById('test-steering-alert');
  if (session.feedbackMessage) {
    alertEl.style.display = 'block';
    if (session.feedbackMessage.includes('Correct')) {
      alertEl.style.background = '#ecfdf5';
      alertEl.style.color = '#065f46';
      alertEl.style.border = '1px solid #a7f3d0';
    } else if (session.feedbackMessage.includes('Incorrect')) {
      alertEl.style.background = '#fef2f2';
      alertEl.style.color = '#991b1b';
      alertEl.style.border = '1px solid #fecaca';
    } else {
      alertEl.style.background = '#eff6ff';
      alertEl.style.color = '#1e40af';
      alertEl.style.border = '1px solid #bfdbfe';
    }
    alertEl.textContent = session.feedbackMessage;
  } else {
    alertEl.style.display = 'none';
  }

  // Question & Options
  document.getElementById('test-question-text').textContent = q.questionText;
  document.getElementById('test-opt-a-text').textContent = q.optionA;
  document.getElementById('test-opt-b-text').textContent = q.optionB;
  document.getElementById('test-opt-c-text').textContent = q.optionC;
  document.getElementById('test-opt-d-text').textContent = q.optionD;

  // Reset radio states
  ['a', 'b', 'c', 'd'].forEach(letter => {
    const card = document.getElementById(`opt-label-${letter}`);
    if (card) {
      card.classList.remove('selected');
      const radio = card.querySelector('input[type="radio"]');
      if (radio) radio.checked = false;
    }
  });

  const submitBtn = document.getElementById('btn-submit-answer');
  submitBtn.disabled = true;
  submitBtn.textContent = (session.currentQuestionNumber >= session.totalQuestionsLimit) ? 'Submit & Finish Test ➔' : 'Submit Answer & Next ➔';
}

function selectOption(letter) {
  selectedOptionValue = letter;
  ['a', 'b', 'c', 'd'].forEach(l => {
    const card = document.getElementById(`opt-label-${l}`);
    if (card) {
      if (l.toUpperCase() === letter) {
        card.classList.add('selected');
        const r = card.querySelector('input[type="radio"]');
        if (r) r.checked = true;
      } else {
        card.classList.remove('selected');
      }
    }
  });

  document.getElementById('btn-submit-answer').disabled = false;
}

async function submitCurrentAnswer() {
  if (!activeSession || !currentQuestionId || !selectedOptionValue) return;

  const submitBtn = document.getElementById('btn-submit-answer');
  submitBtn.disabled = true;
  submitBtn.textContent = 'Submitting...';

  try {
    const res = await authFetch('/api/assessments/answer', {
      method: 'POST',
      body: JSON.stringify({
        attemptId: activeSession.attemptId,
        questionId: currentQuestionId,
        selectedOption: selectedOptionValue
      })
    });

    if (res.success && res.data) {
      activeSession = res.data;

      if (activeSession.completed) {
        // Test Completed!
        stopTestTimer();
        closeModal('adaptive-test-modal');
        displayTestReport(activeSession);
      } else {
        renderAdaptiveQuestion(activeSession);
      }
    } else {
      alert(res.message || 'Error submitting answer');
      submitBtn.disabled = false;
    }
  } catch (err) {
    alert('Submission failure: ' + err.message);
    submitBtn.disabled = false;
  }
}

function displayTestReport(session) {
  const percentage = session.percentageScore || 0;
  const isPass = session.passed;

  document.getElementById('results-score-title').textContent = `${percentage}% Score`;
  const icon = document.getElementById('results-badge-icon');
  const pill = document.getElementById('results-status-pill');

  if (isPass) {
    icon.textContent = '🏆';
    pill.textContent = 'PASSED';
    pill.style.background = '#ecfdf5';
    pill.style.color = '#065f46';
    pill.style.border = '1px solid #a7f3d0';
  } else {
    icon.textContent = '📚';
    pill.textContent = 'NEEDS IMPROVEMENT';
    pill.style.background = '#fef2f2';
    pill.style.color = '#991b1b';
    pill.style.border = '1px solid #fecaca';
  }

  document.getElementById('results-summary-text').textContent = session.feedbackMessage ||
    `You answered ${session.totalQuestionsLimit} questions in this session. Your score has been recorded.`;

  document.getElementById('results-peak-tier').textContent = session.currentDifficulty || 'Medium';
  document.getElementById('results-questions-count').textContent = `${session.answeredQuestionIds?.length || 5} / ${session.totalQuestionsLimit}`;

  openModal('test-results-modal');
}

function startTestTimer(seconds) {
  remainingSeconds = seconds;
  const timerEl = document.getElementById('test-timer');

  if (timerInterval) clearInterval(timerInterval);

  timerInterval = setInterval(() => {
    remainingSeconds--;
    if (remainingSeconds <= 0) {
      clearInterval(timerInterval);
      alert('Time has expired! Submitting assessment.');
      finishTestEarly();
      return;
    }

    const mins = Math.floor(remainingSeconds / 60);
    const secs = remainingSeconds % 60;
    timerEl.textContent = `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  }, 1000);
}

function stopTestTimer() {
  if (timerInterval) {
    clearInterval(timerInterval);
    timerInterval = null;
  }
}

async function finishTestEarly() {
  if (!activeSession) return;
  stopTestTimer();

  try {
    const res = await authFetch('/api/assessments/finish', {
      method: 'POST',
      body: JSON.stringify({ attemptId: activeSession.attemptId })
    });
    if (res.success && res.data) {
      closeModal('adaptive-test-modal');
      displayTestReport(res.data);
    }
  } catch (err) {
    console.error(err);
  }
}

async function viewAttemptBreakdown(attemptId) {
  try {
    const res = await authFetch(`/api/assessments/attempts/${attemptId}`);
    if (res.success && Array.isArray(res.data)) {
      const answers = res.data;
      let text = `Test Attempt #${attemptId} Breakdown:\n\n`;
      answers.forEach((a, i) => {
        text += `Q${i + 1} [${a.difficulty} - ${a.pointsAwarded} pts]: ${a.questionText}\n`;
        text += `Selected: ${a.selectedOption} | Correct: ${a.correctOption} (${a.correct ? '✓ Correct' : '✕ Incorrect'})\n\n`;
      });
      alert(text);
    }
  } catch (err) {
    alert('Failed to load attempt details');
  }
}
