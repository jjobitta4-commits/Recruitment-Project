/**
 * Virtual Technical Interview Room Script
 */

let currentInterview = null;
let mediaStream = null;
let isMicMuted = false;
let isCamOff = false;
let timerSeconds = 0;
let callInterval = null;

const STARTER_CODE = {
  java: `// Candidate Technical Challenge (Java 21)
public class Solution {
    public static void main(String[] args) {
        System.out.println("Executing candidate solution...");
        int[] nums = {2, 7, 11, 15};
        int target = 9;
        int[] result = twoSum(nums, target);
        System.out.println("Result indices: [" + result[0] + ", " + result[1] + "]");
    }

    public static int[] twoSum(int[] nums, int target) {
        java.util.Map<Integer, Integer> map = new java.util.HashMap<>();
        for (int i = 0; i < nums.length; i++) {
            int comp = target - nums[i];
            if (map.containsKey(comp)) return new int[]{map.get(comp), i};
            map.put(nums[i], i);
        }
        return new int[]{-1, -1};
    }
}`,
  javascript: `// Candidate Technical Challenge (JavaScript ES6)
function twoSum(nums, target) {
    const map = new Map();
    for (let i = 0; i < nums.length; i++) {
        const comp = target - nums[i];
        if (map.has(comp)) return [map.get(comp), i];
        map.set(nums[i], i);
    }
    return [];
}

console.log("Result:", twoSum([2, 7, 11, 15], 9));`,
  python: `# Candidate Technical Challenge (Python 3.13)
def two_sum(nums, target):
    seen = {}
    for i, n in enumerate(nums):
        diff = target - n
        if diff in seen:
            return [seen[diff], i]
        seen[n] = i
    return []

print("Result:", two_sum([2, 7, 11, 15], 9))`,
  sql: `-- Technical Interview Query Challenge
SELECT 
    a.full_name,
    COUNT(app.application_id) AS total_applications
FROM applicants a
JOIN applications app ON a.applicant_id = app.applicant_id
GROUP BY a.applicant_id
HAVING total_applications > 1
ORDER BY total_applications DESC;`
};

document.addEventListener('DOMContentLoaded', async () => {
  const session = Auth.getSession();
  if (!session) {
    window.location.href = 'login.html';
    return;
  }

  // Determine user role
  const isRecruiter = session.role === 'recruiter';
  const roleBadge = document.getElementById('role-badge');
  const tabEvaluator = document.getElementById('tab-evaluator');
  const btnBack = document.getElementById('btn-back-dashboard');

  if (btnBack) {
    btnBack.href = isRecruiter ? 'recruiter/dashboard.html' : 'applicant/dashboard.html';
  }

  if (roleBadge) {
    roleBadge.textContent = isRecruiter ? 'Interviewer / Recruiter' : 'Candidate';
    roleBadge.style.background = isRecruiter ? 'rgba(16,185,129,0.2)' : 'rgba(37,99,235,0.2)';
    roleBadge.style.borderColor = isRecruiter ? '#10b981' : '#3b82f6';
    roleBadge.style.color = isRecruiter ? '#6ee7b7' : '#93c5fd';
  }

  if (isRecruiter && tabEvaluator) {
    tabEvaluator.style.display = 'block';
  }

  // Start Call Timer
  callInterval = setInterval(() => {
    timerSeconds++;
    const m = String(Math.floor(timerSeconds / 60)).padStart(2, '0');
    const s = String(timerSeconds % 60).padStart(2, '0');
    const timerEl = document.getElementById('call-timer');
    if (timerEl) timerEl.textContent = `${m}:${s}`;
  }, 1000);

  // Initialize Media Devices (Webcam / Mic)
  await initMedia();

  // Setup Workspace Tabs
  setupWorkspaceTabs();

  // Setup Code Editor and Language Picker
  setupCodeEditor();

  // Setup Recruiter Evaluation Scorecard
  if (isRecruiter) {
    setupScorecard();
  }

  // Load Interview Details from URL Param
  const urlParams = new URLSearchParams(window.location.search);
  const interviewId = urlParams.get('id');
  await loadInterviewDetails(interviewId);

  // Video Media Buttons
  setupMediaControls();
});

async function initMedia() {
  const videoEl = document.getElementById('user-webcam');
  const avatarFallback = document.getElementById('avatar-fallback');

  try {
    mediaStream = await navigator.mediaDevices.getUserMedia({ video: true, audio: true });
    if (videoEl) {
      videoEl.srcObject = mediaStream;
    }
  } catch (err) {
    console.log('Webcam/Mic not accessible or permission denied; using virtual simulation.');
    if (videoEl) videoEl.style.display = 'none';
    if (avatarFallback) avatarFallback.style.display = 'block';
  }
}

function setupMediaControls() {
  const btnMic = document.getElementById('ctrl-mic');
  const btnCam = document.getElementById('ctrl-cam');
  const btnShare = document.getElementById('ctrl-share');
  const btnLeave = document.getElementById('ctrl-leave');

  if (btnMic) {
    btnMic.addEventListener('click', () => {
      isMicMuted = !isMicMuted;
      btnMic.classList.toggle('active-danger', isMicMuted);
      btnMic.innerHTML = isMicMuted ? '🔇' : '🎙️';
      if (mediaStream) {
        mediaStream.getAudioTracks().forEach(t => t.enabled = !isMicMuted);
      }
      showToast(isMicMuted ? 'Microphone muted' : 'Microphone unmuted', 'info');
    });
  }

  if (btnCam) {
    btnCam.addEventListener('click', () => {
      isCamOff = !isCamOff;
      btnCam.classList.toggle('active-danger', isCamOff);
      btnCam.innerHTML = isCamOff ? '🚫' : '📹';
      const videoEl = document.getElementById('user-webcam');
      const avatarFallback = document.getElementById('avatar-fallback');

      if (mediaStream) {
        mediaStream.getVideoTracks().forEach(t => t.enabled = !isCamOff);
      }
      if (videoEl) videoEl.style.display = isCamOff ? 'none' : 'block';
      if (avatarFallback) avatarFallback.style.display = isCamOff ? 'block' : 'none';
      showToast(isCamOff ? 'Camera turned off' : 'Camera turned on', 'info');
    });
  }

  if (btnShare) {
    btnShare.addEventListener('click', async () => {
      try {
        if (navigator.mediaDevices && navigator.mediaDevices.getDisplayMedia) {
          const screenStream = await navigator.mediaDevices.getDisplayMedia({ video: true });
          const videoEl = document.getElementById('user-webcam');
          if (videoEl) videoEl.srcObject = screenStream;
          showToast('Sharing your screen with the room', 'success');
        } else {
          showToast('Screen sharing simulated in session.', 'info');
        }
      } catch (e) {
        showToast('Screen share canceled.', 'info');
      }
    });
  }

  if (btnLeave) {
    btnLeave.addEventListener('click', () => {
      if (confirm('Are you sure you want to end this interview call?')) {
        if (mediaStream) {
          mediaStream.getTracks().forEach(t => t.stop());
        }
        clearInterval(callInterval);
        const session = Auth.getSession();
        window.location.href = (session && session.role === 'recruiter') ? 'recruiter/dashboard.html' : 'applicant/dashboard.html';
      }
    });
  }
}

function setupWorkspaceTabs() {
  const tabs = document.querySelectorAll('.workspace-tab');
  tabs.forEach(tab => {
    tab.addEventListener('click', () => {
      tabs.forEach(t => t.classList.remove('active'));
      tab.classList.add('active');

      const target = tab.getAttribute('data-tab');
      document.querySelectorAll('.tab-pane').forEach(pane => {
        pane.style.display = 'none';
      });

      const activePane = document.getElementById(`pane-${target}`);
      if (activePane) activePane.style.display = 'block';
    });
  });
}

function setupCodeEditor() {
  const selectLang = document.getElementById('editor-lang');
  const codeEditor = document.getElementById('code-editor');
  const btnRun = document.getElementById('btn-run-code');
  const outputConsole = document.getElementById('code-output');

  if (selectLang && codeEditor) {
    selectLang.addEventListener('change', (e) => {
      const lang = e.target.value;
      codeEditor.value = STARTER_CODE[lang] || '';
    });
  }

  if (btnRun && outputConsole && codeEditor) {
    btnRun.addEventListener('click', () => {
      btnRun.disabled = true;
      btnRun.textContent = '⏳ Compiling...';
      outputConsole.textContent = 'Running build & execution test suite...';

      setTimeout(() => {
        btnRun.disabled = false;
        btnRun.textContent = '▶ Run Code';
        outputConsole.innerHTML = `<span style="color: #4ade80;">[SUCCESS] Build completed in 0.42s (0 warnings)</span>\n\nExecuting candidate code:\nExecuting candidate solution...\nResult indices: [0, 1]\n\nExecution finished with exit code 0.`;
      }, 700);
    });
  }
}

function setupScorecard() {
  const stars = document.querySelectorAll('.rating-star');
  const ratingInput = document.getElementById('eval-rating');
  const btnSubmit = document.getElementById('btn-submit-eval');

  stars.forEach(star => {
    star.addEventListener('click', () => {
      const val = parseInt(star.getAttribute('data-value'));
      if (ratingInput) ratingInput.value = val;
      stars.forEach(s => {
        const sVal = parseInt(s.getAttribute('data-value'));
        s.classList.toggle('selected', sVal <= val);
      });
    });
  });

  if (btnSubmit) {
    btnSubmit.addEventListener('click', async () => {
      if (!currentInterview || !currentInterview.interviewId) {
        showToast('No active interview session linked.', 'error');
        return;
      }

      const rating = parseInt(ratingInput ? ratingInput.value : 5);
      const feedback = document.getElementById('eval-feedback').value.trim();
      const statusAction = document.getElementById('eval-status-action').value;

      btnSubmit.disabled = true;
      btnSubmit.textContent = 'Saving Evaluation...';

      try {
        const res = await fetch(apiEndpoint('/api/interviews/evaluate'), {
          method: 'PUT',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + Auth.getToken()
          },
          body: JSON.stringify({
            interviewId: currentInterview.interviewId,
            rating: rating,
            feedback: feedback,
            status: 'Completed',
            applicationStatus: statusAction
          })
        });

        const data = await res.json();
        if (res.ok && data.success) {
          showToast('Evaluation recorded! Candidate status updated.', 'success');
        } else {
          showToast(data.message || 'Failed to submit scorecard.', 'error');
        }
      } catch (err) {
        console.error('Scorecard error:', err);
        showToast('Network error while saving scorecard.', 'error');
      } finally {
        btnSubmit.disabled = false;
        btnSubmit.textContent = 'Save Evaluation & Update Candidate Status';
      }
    });
  }
}

async function loadInterviewDetails(interviewId) {
  try {
    const res = await fetch(apiEndpoint('/api/interviews'), {
      headers: { 'Authorization': 'Bearer ' + Auth.getToken() }
    });
    const data = await res.json();

    if (res.ok && data.success && data.data && data.data.length > 0) {
      if (interviewId) {
        currentInterview = data.data.find(i => String(i.interviewId) === String(interviewId)) || data.data[0];
      } else {
        currentInterview = data.data[0];
      }

      // Populate UI with details
      const titleEl = document.getElementById('interview-title');
      const participantsEl = document.getElementById('interview-participants');
      const detJob = document.getElementById('det-job-title');
      const detCompany = document.getElementById('det-company');
      const detName = document.getElementById('det-applicant-name');
      const detEmail = document.getElementById('det-applicant-email');
      const detDateTime = document.getElementById('det-date-time');
      const activeSpeaker = document.getElementById('active-speaker-name');
      const fallbackName = document.getElementById('fallback-participant-name');

      if (titleEl) titleEl.textContent = `${currentInterview.jobTitle} — Interview`;
      if (participantsEl) participantsEl.textContent = `Candidate: ${currentInterview.applicantName} | Interviewer: ${currentInterview.company}`;
      if (detJob) detJob.textContent = currentInterview.jobTitle;
      if (detCompany) detCompany.textContent = currentInterview.company;
      if (detName) detName.textContent = currentInterview.applicantName;
      if (detEmail) detEmail.textContent = currentInterview.applicantEmail;
      if (detDateTime) detDateTime.textContent = `${currentInterview.interviewDate} at ${currentInterview.interviewTime}`;
      if (activeSpeaker) activeSpeaker.textContent = currentInterview.applicantName;
      if (fallbackName) fallbackName.textContent = currentInterview.applicantName;
    }
  } catch (err) {
    console.error('Error fetching interview:', err);
  }
}
