/**
 * Aegis Ambient — Shared Client Utilities & WebSocket Manager
 */

const Aegis = (function() {
  let stompClient = null;
  let socket = null;
  let isConnected = false;
  let reconnectTimeout = null;
  let audioCtx = null;

  // Sound generator using Web Audio API
  function getAudioContext() {
    if (!audioCtx) {
      const AudioContext = window.AudioContext || window.webkitAudioContext;
      if (AudioContext) {
        audioCtx = new AudioContext();
      }
    }
    if (audioCtx && audioCtx.state === 'suspended') {
      audioCtx.resume();
    }
    return audioCtx;
  }

  function playAlertChime(isCritical = false) {
    try {
      const ctx = getAudioContext();
      if (!ctx) return;

      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      osc.connect(gain);
      gain.connect(ctx.destination);

      const now = ctx.currentTime;
      if (isCritical) {
        // High urgency alternating tone
        osc.type = 'sawtooth';
        osc.frequency.setValueAtTime(880, now);
        osc.frequency.setValueAtTime(660, now + 0.15);
        osc.frequency.setValueAtTime(880, now + 0.3);
        gain.gain.setValueAtTime(0.2, now);
        gain.gain.exponentialRampToValueAtTime(0.01, now + 0.6);
        osc.start(now);
        osc.stop(now + 0.6);
      } else {
        // Calm, gentle two-note chime
        osc.type = 'sine';
        osc.frequency.setValueAtTime(523.25, now); // C5
        osc.frequency.exponentialRampToValueAtTime(659.25, now + 0.15); // E5
        gain.gain.setValueAtTime(0.15, now);
        gain.gain.exponentialRampToValueAtTime(0.01, now + 0.5);
        osc.start(now);
        osc.stop(now + 0.5);
      }
    } catch (e) {
      console.warn('Audio tone could not be played:', e);
    }
  }

  // Toast Notification
  function showToast(message, type = 'info') {
    let container = document.getElementById('toastContainer');
    if (!container) {
      container = document.createElement('div');
      container.id = 'toastContainer';
      container.className = 'toast-container';
      document.body.appendChild(container);
    }

    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    
    // Icon
    let iconSvg = `<svg fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24" style="width:1.25rem;height:1.25rem;flex-shrink:0;"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>`;
    if (type === 'success') {
      iconSvg = `<svg fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24" style="width:1.25rem;height:1.25rem;flex-shrink:0;"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>`;
    } else if (type === 'error') {
      iconSvg = `<svg fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24" style="width:1.25rem;height:1.25rem;flex-shrink:0;"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>`;
    }

    toast.innerHTML = `${iconSvg}<span>${escapeHtml(message)}</span>`;
    container.appendChild(toast);

    setTimeout(() => {
      toast.style.opacity = '0';
      toast.style.transform = 'translateY(10px)';
      toast.style.transition = 'all 0.3s ease';
      setTimeout(() => toast.remove(), 300);
    }, 4000);
  }

  function escapeHtml(text) {
    if (!text) return '';
    return text.replace(/[&<>"']/g, function(m) {
      return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#039;' }[m];
    });
  }

  // Update UI Connection Pills
  function updateConnectionUI(state) {
    const dots = document.querySelectorAll('.status-dot');
    const texts = document.querySelectorAll('.connection-text');

    dots.forEach(dot => {
      dot.className = `status-dot ${state}`;
    });

    texts.forEach(el => {
      if (state === 'connected') el.textContent = 'Backend Online';
      else if (state === 'connecting') el.textContent = 'Connecting...';
      else el.textContent = 'Disconnected';
    });
  }

  // WebSocket / STOMP Connection
  function initWebSocket(onConnectedCallback, onMessageMap = {}) {
    updateConnectionUI('connecting');

    // SockJS or fallback standard WebSocket
    const wsUrl = window.location.origin + '/ws';
    if (typeof SockJS !== 'undefined') {
      socket = new SockJS(wsUrl);
    } else {
      const loc = window.location;
      const proto = loc.protocol === 'https:' ? 'wss:' : 'ws:';
      socket = new WebSocket(proto + '//' + loc.host + '/ws');
    }

    stompClient = Stomp.over(socket);
    // Disable noisy stomp debug logging in production/demo
    stompClient.debug = () => {};

    stompClient.connect({}, function(frame) {
      isConnected = true;
      updateConnectionUI('connected');
      console.log('Connected to Aegis Ambient WebSocket broker.');

      // Register subscription map
      for (const [topic, handler] of Object.entries(onMessageMap)) {
        stompClient.subscribe(topic, function(message) {
          try {
            const data = JSON.parse(message.body);
            handler(data);
          } catch (e) {
            console.error('Error parsing WebSocket message from ' + topic, e);
          }
        });
      }

      if (onConnectedCallback) onConnectedCallback(stompClient);
    }, function(error) {
      isConnected = false;
      updateConnectionUI('disconnected');
      console.warn('STOMP connection lost, scheduling reconnect...', error);
      clearTimeout(reconnectTimeout);
      reconnectTimeout = setTimeout(() => {
        initWebSocket(onConnectedCallback, onMessageMap);
      }, 4000);
    });
  }

  function sendEvent(eventData) {
    if (!stompClient || !isConnected) {
      console.warn('Cannot send event, STOMP not connected.');
      return false;
    }
    stompClient.send('/app/event', {}, JSON.stringify(eventData));
    return true;
  }

  // REST API Helpers
  async function fetchPatient(id = 1) {
    const res = await fetch(`/api/patients/${id}`);
    if (!res.ok) throw new Error('Patient not found');
    return await res.json();
  }

  async function fetchIncidents(patientId = 1) {
    const res = await fetch(`/api/incidents?patientId=${patientId}`);
    if (!res.ok) throw new Error('Failed to load incidents');
    return await res.json();
  }

  async function resolveIncident(incidentId) {
    const res = await fetch(`/api/incidents/${incidentId}/resolve`, { method: 'POST' });
    if (!res.ok) throw new Error('Failed to resolve incident');
    return await res.json();
  }

  async function resetDemo() {
    const res = await fetch('/api/demo/reset', { method: 'POST' });
    if (!res.ok) throw new Error('Failed to reset demo');
    return await res.json();
  }

  // Setup Mobile Nav Toggle & Current Page Highlighting
  document.addEventListener('DOMContentLoaded', () => {
    const toggle = document.querySelector('.mobile-nav-toggle');
    const menu = document.querySelector('.nav-menu');
    if (toggle && menu) {
      toggle.addEventListener('click', () => {
        menu.classList.toggle('open');
      });
    }

    // Highlight current page nav link
    const path = window.location.pathname || '/';
    const links = document.querySelectorAll('.nav-link');
    links.forEach(link => link.classList.remove('active'));
    links.forEach(link => {
      const href = link.getAttribute('href');
      if ((href === '/' && (path === '/' || path === '' || path.endsWith('/index.html'))) ||
          (href !== '/' && path.endsWith(href))) {
        link.classList.add('active');
      }
    });
  });

  return {
    initWebSocket,
    sendEvent,
    showToast,
    playAlertChime,
    fetchPatient,
    fetchIncidents,
    resolveIncident,
    resetDemo,
    escapeHtml
  };
})();
