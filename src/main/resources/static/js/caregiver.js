/**
 * Aegis Ambient — Caregiver Dashboard Script
 * Real-time STOMP status receiver, incident intelligence renderer, and timeline manager.
 */

(function() {
  const PATIENT_ID = 1;

  // DOM Elements
  const lblLastUpdate = document.getElementById('lblLastUpdate');
  const mainStatusBanner = document.getElementById('mainStatusBanner');
  const statusHeadline = document.getElementById('statusHeadline');
  const statusSubtext = document.getElementById('statusSubtext');
  const statusIconBubble = document.getElementById('statusIconBubble');
  const statusActionCol = document.getElementById('statusActionCol');
  const btnResolveIncident = document.getElementById('btnResolveIncident');

  // Metrics
  const valPatientStatus = document.getElementById('valPatientStatus');
  const valActiveAlerts = document.getElementById('valActiveAlerts');
  const valTotalIncidents = document.getElementById('valTotalIncidents');
  const valBackendLink = document.getElementById('valBackendLink');

  // Insight elements
  const insightExplanationText = document.getElementById('insightExplanationText');
  const insightSeverityBadge = document.getElementById('insightSeverityBadge');
  const insightRoom = document.getElementById('insightRoom');
  const insightType = document.getElementById('insightType');
  const insightTime = document.getElementById('insightTime');

  // Timeline
  const incidentTimelineList = document.getElementById('incidentTimelineList');

  // Responder info
  const resName = document.getElementById('resName');
  const resAge = document.getElementById('resAge');
  const resBlood = document.getElementById('resBlood');
  const resAllergies = document.getElementById('resAllergies');
  const resCondition = document.getElementById('resCondition');
  const resAddress = document.getElementById('resAddress');
  const resContact = document.getElementById('resContact');

  // Controls
  const btnToggleSound = document.getElementById('btnToggleSound');
  const lblSoundStatus = document.getElementById('lblSoundStatus');
  const btnRefreshDashboard = document.getElementById('btnRefreshDashboard');
  const btnUnlockDoor = document.getElementById('btnUnlockDoor');
  const btnTurnOnLights = document.getElementById('btnTurnOnLights');

  let isSoundMuted = false;
  let activeIncidentId = null;

  // 1. Initialize STOMP Connection & Status Topic Listener
  Aegis.initWebSocket(onConnected, {
    [`/topic/caregiver/${PATIENT_ID}/status`]: handleCaregiverStatusUpdate
  });

  function onConnected() {
    valBackendLink.textContent = 'ONLINE (STOMP)';
    valBackendLink.style.color = 'var(--color-green)';
    loadInitialData();
  }

  // 2. Handle Status Update from Server State Machine
  function handleCaregiverStatusUpdate(data) {
    console.log('Received caregiver status push:', data);
    lblLastUpdate.textContent = new Date().toLocaleTimeString();

    activeIncidentId = data.incidentId || activeIncidentId;

    // Update Status Banner
    updateStatusBanner(data);

    // Update Incident Insight Card
    updateInsightCard(data);

    // Refresh Incident History from H2
    loadIncidents();
  }

  function updateStatusBanner(data) {
    const status = data.status || 'NORMAL';
    const countdown = data.countdownRemaining || 0;

    mainStatusBanner.className = `status-banner ${status.toLowerCase()}`;

    if (status === 'NORMAL') {
      statusHeadline.textContent = 'All Clear';
      statusSubtext.textContent = data.summary || 'Ambient sensors active and observing locally. No abnormal stillness or fall events reported.';
      statusIconBubble.innerHTML = `<svg fill="none" stroke="#15803D" stroke-width="2.5" viewBox="0 0 24 24"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>`;
      statusActionCol.style.display = 'none';

      valPatientStatus.textContent = 'NORMAL';
      valPatientStatus.style.color = 'var(--color-green)';
      valActiveAlerts.textContent = '0';

    } else if (status === 'CHECK_IN' || status === 'COUNTDOWN') {
      const remainingText = countdown > 0 ? ` (${countdown}s remaining)` : '';
      statusHeadline.textContent = `Checking on Patient${remainingText}`;
      statusSubtext.textContent = data.summary || 'Verifying patient status via voice check-in and on-screen countdown.';
      statusIconBubble.innerHTML = `<svg fill="none" stroke="#D97706" stroke-width="2.5" viewBox="0 0 24 24"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 14 14"/></svg>`;
      statusActionCol.style.display = 'none';

      valPatientStatus.textContent = status;
      valPatientStatus.style.color = 'var(--color-amber)';
      valActiveAlerts.textContent = '1';

      if (!isSoundMuted && status === 'COUNTDOWN' && countdown % 5 === 0) {
        Aegis.playAlertChime(false);
      }

    } else if (status === 'ALERT') {
      statusHeadline.textContent = 'CRITICAL ALERT — ATTENTION REQUIRED';
      statusSubtext.textContent = data.summary || 'Emergency detected and verified. Patient did not respond within countdown window.';
      statusIconBubble.innerHTML = `<svg fill="none" stroke="#DC2626" stroke-width="2.5" viewBox="0 0 24 24"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>`;
      statusActionCol.style.display = 'block';

      valPatientStatus.textContent = 'ALERT';
      valPatientStatus.style.color = 'var(--color-red)';
      valActiveAlerts.textContent = '1';

      if (!isSoundMuted) {
        Aegis.playAlertChime(true);
      }
    }
  }

  function updateInsightCard(data) {
    if (data.summary) {
      insightExplanationText.textContent = data.summary;
    }
    if (data.severity) {
      insightSeverityBadge.textContent = data.severity;
      if (data.severity === 'CRITICAL') {
        insightSeverityBadge.className = 'badge badge-danger';
      } else if (data.severity === 'WARNING') {
        insightSeverityBadge.className = 'badge badge-warning';
      } else {
        insightSeverityBadge.className = 'badge badge-info';
      }
    }
    if (data.room) {
      insightRoom.textContent = data.room;
    }
    if (data.incidentType) {
      insightType.textContent = data.incidentType.replace(/_/g, ' ');
    }
    if (data.timestamp) {
      const dt = new Date(data.timestamp);
      insightTime.textContent = dt.toLocaleTimeString();
    }
  }

  // 3. Load Initial REST Data
  async function loadInitialData() {
    try {
      // 1. Patient Profile
      const patient = await Aegis.fetchPatient(PATIENT_ID);
      if (patient) {
        resName.textContent = patient.name;
        resAge.textContent = patient.age;
        resBlood.textContent = patient.bloodType;
        resAllergies.textContent = patient.allergies;
        resCondition.textContent = patient.condition;
        resAddress.textContent = patient.address;
        resContact.textContent = `${patient.emergencyContactName} — ${patient.emergencyContactPhone}`;
      }

      // 2. Incident History
      await loadIncidents();

    } catch (e) {
      console.warn('Initial REST load error:', e);
    }
  }

  async function loadIncidents() {
    try {
      const incidents = await Aegis.fetchIncidents(PATIENT_ID);
      valTotalIncidents.textContent = incidents.length;
      renderTimeline(incidents);
    } catch (e) {
      console.error('Failed to load incidents:', e);
      incidentTimelineList.innerHTML = `<li style="padding:1rem;color:var(--color-red);">Error connecting to incident database.</li>`;
    }
  }

  function renderTimeline(incidents) {
    if (!incidents || incidents.length === 0) {
      incidentTimelineList.innerHTML = `
        <li style="text-align:center; padding:2rem; color:var(--color-text-muted); font-size:0.9375rem;">
          No incidents recorded yet. All quiet.
        </li>
      `;
      return;
    }

    incidentTimelineList.innerHTML = incidents.map(inc => {
      const timeStr = new Date(inc.detectedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
      let badgeClass = 'badge-info';
      let outcomeText = inc.status;

      if (inc.status === 'ALERT') {
        badgeClass = 'badge-danger';
        outcomeText = 'ESCALATED';
      } else if (inc.status === 'CANCELLED') {
        badgeClass = 'badge-demo';
        outcomeText = 'CANCELLED';
      } else if (inc.status === 'RESOLVED') {
        badgeClass = 'badge-success';
        outcomeText = 'RESOLVED';
      } else if (inc.status === 'CHECK_IN' || inc.status === 'COUNTDOWN') {
        badgeClass = 'badge-warning';
        outcomeText = 'VERIFYING';
      }

      const typeFormatted = (inc.type || 'INCIDENT').replace(/_/g, ' ');

      return `
        <li class="timeline-item">
          <div class="timeline-badge-col">
            <span class="badge ${badgeClass}">${outcomeText}</span>
            <span class="timeline-time">${timeStr}</span>
          </div>
          <div class="timeline-content-col">
            <div class="timeline-title">${escapeHtml(typeFormatted)} <span style="font-size:0.75rem;font-weight:400;color:var(--color-text-muted);">in ${escapeHtml(inc.room || 'home')}</span></div>
            <div class="timeline-desc">${escapeHtml(inc.summary || 'Safety check initiated.')}</div>
            ${inc.notes ? `<div style="font-size:0.75rem;color:var(--color-teal);margin-top:0.25rem;">Note: ${escapeHtml(inc.notes)}</div>` : ''}
          </div>
        </li>
      `;
    }).join('');
  }

  function escapeHtml(str) {
    return Aegis.escapeHtml(str);
  }

  // 4. Resolve Incident Action
  btnResolveIncident.addEventListener('click', async () => {
    btnResolveIncident.disabled = true;
    btnResolveIncident.textContent = 'Resolving...';

    try {
      const res = await Aegis.resolveIncident(activeIncidentId || 1);
      Aegis.showToast(res.message || 'Incident marked as resolved.', 'success');
      activeIncidentId = null;
      btnResolveIncident.disabled = false;
      btnResolveIncident.textContent = 'Resolve Alert';
      // Reload incidents
      await loadIncidents();
    } catch (e) {
      Aegis.showToast('Failed to resolve incident', 'error');
      btnResolveIncident.disabled = false;
      btnResolveIncident.textContent = 'Resolve Alert';
    }
  });

  // 5. Mute Audio Toggle
  btnToggleSound.addEventListener('click', () => {
    isSoundMuted = !isSoundMuted;
    if (isSoundMuted) {
      lblSoundStatus.textContent = 'Sound: MUTED';
      btnToggleSound.classList.add('btn-outline-danger');
      Aegis.showToast('Critical alert audio muted', 'warning');
    } else {
      lblSoundStatus.textContent = 'Sound: ON';
      btnToggleSound.classList.remove('btn-outline-danger');
      Aegis.showToast('Critical alert audio enabled', 'info');
      Aegis.playAlertChime(false);
    }
  });

  // 6. Refresh Button
  btnRefreshDashboard.addEventListener('click', async () => {
    Aegis.showToast('Refreshing dashboard data...', 'info');
    await loadInitialData();
  });

  // 7. Mock Smart Home Controls
  btnUnlockDoor.addEventListener('click', () => {
    Aegis.showToast('Mock Action: Front door smart lock UNLOCKED for first responders. (Simulated Demo Action)', 'success');
    console.log('[MOCK SMART HOME] Sent UNLOCK command to front door lock.');
  });

  btnTurnOnLights.addEventListener('click', () => {
    Aegis.showToast('Mock Action: Entry porch & hallway lighting turned ON. (Simulated Demo Action)', 'success');
    console.log('[MOCK SMART HOME] Sent LIGHTS_ON command to entry porch.');
  });

})();
