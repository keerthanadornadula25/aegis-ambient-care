/**
 * Aegis Ambient — Patient Edge Monitoring Script
 * Real-time MediaPipe Pose Landmarker, Web Audio meter, Web Speech API, and STOMP event dispatch.
 */

(function() {
  const PATIENT_ID = 1;

  // DOM Elements
  const btnStartMonitoring = document.getElementById('btnStartMonitoring');
  const permissionCard = document.getElementById('permissionCard');
  const cameraVideo = document.getElementById('cameraVideo');
  const skeletonCanvas = document.getElementById('skeletonCanvas');
  const canvasPlaceholder = document.getElementById('canvasPlaceholder');
  const chkShowCamera = document.getElementById('chkShowCamera');
  const chkPretend3am = document.getElementById('chkPretend3am');
  const btnImOk = document.getElementById('btnImOk');
  const countdownBox = document.getElementById('countdownBox');
  const countdownSecondsNum = document.getElementById('countdownSecondsNum');
  const voicePromptBanner = document.getElementById('voicePromptBanner');
  const voicePromptText = document.getElementById('voicePromptText');

  // HUD elements
  const hudPosture = document.getElementById('hudPosture');
  const hudSway = document.getElementById('hudSway');
  const hudStill = document.getElementById('hudStill');
  const hudSound = document.getElementById('hudSound');

  // Simulation buttons
  const btnSimHiddenFall = document.getElementById('btnSimHiddenFall');
  const btnSimFreeze = document.getElementById('btnSimFreeze');
  const btnSimBedExit = document.getElementById('btnSimBedExit');
  const btnSimWalkingToDoor = document.getElementById('btnSimWalkingToDoor');
  const btnSimDoorOpening = document.getElementById('btnSimDoorOpening');
  const btnResetDemo = document.getElementById('btnResetDemo');

  // State Variables
  let poseLandmarker = null;
  let videoStream = null;
  let audioContext = null;
  let audioAnalyser = null;
  let audioDataArray = null;
  let currentSoundLevel = 0.0;

  let isMonitoringActive = false;
  let currentPosture = 'UPRIGHT';
  let currentSwayScore = 0.024;
  let stillSeconds = 0;
  let lastSwayPositions = [];
  let lastEventDispatchTime = 0;
  let countdownTimerId = null;
  let speechRecognizer = null;

  // 1. Initialize STOMP Connection & Command Subscriptions
  Aegis.initWebSocket(null, {
    [`/topic/patient/${PATIENT_ID}/commands`]: handleServerCommand
  });

  function handleServerCommand(cmd) {
    console.log('Received patient command from server:', cmd);

    if (cmd.command === 'SPEAK') {
      showVoicePrompt(cmd.text);
      speakText(cmd.text);
      startSpeechRecognition();
    } else if (cmd.command === 'START_COUNTDOWN') {
      startCountdown(cmd.seconds || 30, cmd.text);
    } else if (cmd.command === 'CANCEL') {
      stopCountdown();
      hideVoicePrompt();
      speakText("Glad you are okay. All clear.");
      Aegis.showToast('Incident cancelled: Returned to all clear', 'success');
      Aegis.playAlertChime(false);
    } else if (cmd.command === 'CLEAR') {
      stopCountdown();
      hideVoicePrompt();
    }
  }

  // 2. Speech Synthesis & Recognition (Web Speech API)
  function speakText(text) {
    if (!('speechSynthesis' in window) || !text) return;
    window.speechSynthesis.cancel();
    const utterance = new SpeechSynthesisUtterance(text);
    utterance.rate = 0.95;
    utterance.pitch = 1.0;
    window.speechSynthesis.speak(utterance);
  }

  function startSpeechRecognition() {
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (!SpeechRecognition) {
      console.log('SpeechRecognition API not available in this browser. Fallback button ready.');
      return;
    }

    try {
      if (speechRecognizer) {
        speechRecognizer.abort();
      }
      speechRecognizer = new SpeechRecognition();
      speechRecognizer.continuous = false;
      speechRecognizer.interimResults = false;
      speechRecognizer.lang = 'en-US';

      speechRecognizer.onresult = (event) => {
        const transcript = event.results[0][0].transcript.toLowerCase();
        console.log('Patient speech recognized:', transcript);
        if (transcript.includes('yes') || transcript.includes('ok') || transcript.includes('fine') || transcript.includes('cancel')) {
          Aegis.showToast(`Voice detected: "${transcript}" -> Cancelled alert`, 'success');
          sendUserOk();
        }
      };

      speechRecognizer.onerror = (e) => {
        console.warn('Speech recognition status:', e.error);
      };

      speechRecognizer.start();
    } catch (err) {
      console.warn('Speech recognition could not be started:', err);
    }
  }

  function showVoicePrompt(text) {
    if (voicePromptBanner && voicePromptText) {
      voicePromptText.textContent = `"${text}"`;
      voicePromptBanner.classList.add('active');
    }
  }

  function hideVoicePrompt() {
    if (voicePromptBanner) {
      voicePromptBanner.classList.remove('active');
    }
    if (speechRecognizer) {
      try { speechRecognizer.abort(); } catch (e) {}
    }
  }

  // 3. Countdown Display Logic
  function startCountdown(seconds, text) {
    stopCountdown();
    Aegis.playAlertChime(true);
    if (countdownBox) countdownBox.classList.add('active');

    let remaining = seconds;
    if (countdownSecondsNum) countdownSecondsNum.textContent = remaining;

    countdownTimerId = setInterval(() => {
      remaining--;
      if (countdownSecondsNum) countdownSecondsNum.textContent = remaining;
      if (remaining <= 0) {
        stopCountdown();
      }
    }, 1000);
  }

  function stopCountdown() {
    if (countdownTimerId) {
      clearInterval(countdownTimerId);
      countdownTimerId = null;
    }
    if (countdownBox) countdownBox.classList.remove('active');
  }

  function sendUserOk() {
    stopCountdown();
    hideVoicePrompt();
    Aegis.sendEvent({
      patientId: PATIENT_ID,
      type: 'USER_OK',
      posture: currentPosture,
      swayScore: currentSwayScore,
      stillSeconds: stillSeconds,
      room: 'bedroom',
      simulated: false,
      pretend3am: chkPretend3am ? chkPretend3am.checked : false,
      timestamp: new Date().toISOString()
    });
    Aegis.showToast("Confirmed: I'm OK", 'success');
  }

  if (btnImOk) {
    btnImOk.addEventListener('click', sendUserOk);
  }

  // 4. Start Monitoring (Explicit User Action)
  btnStartMonitoring.addEventListener('click', async () => {
    btnStartMonitoring.disabled = true;
    btnStartMonitoring.textContent = 'Initializing Camera...';

    try {
      // 1. Request camera and microphone
      videoStream = await navigator.mediaDevices.getUserMedia({
        video: { width: 640, height: 480, facingMode: 'user' },
        audio: true
      });

      cameraVideo.srcObject = videoStream;
      await new Promise(resolve => cameraVideo.onloadedmetadata = resolve);
      cameraVideo.play();

      // 2. Setup Web Audio Analyser for loudness/impact monitoring
      setupAudioMonitoring(videoStream);

      // 3. Hide setup notice and placeholder
      permissionCard.style.display = 'none';
      canvasPlaceholder.style.display = 'none';
      isMonitoringActive = true;

      Aegis.showToast('Camera and local edge processing activated.', 'success');

      // 4. Load MediaPipe Pose Landmarker
      await loadMediaPipe();

      // 5. Start frame processing loop
      requestAnimationFrame(processVideoFrame);

    } catch (err) {
      console.warn('Could not initialize webcam/mic:', err);
      permissionCard.style.display = 'none';
      canvasPlaceholder.style.display = 'block';
      canvasPlaceholder.innerHTML = `
        <h4 style="color:#FCA5A5; margin-bottom:0.5rem;">Camera Permission Notice</h4>
        <p style="font-size:0.875rem; color:#CBD5E1;">
          Camera/Mic access was denied or not available. Aegis Ambient is operating in <strong>Simulated Edge Mode</strong>. You can fully test all safety scenarios using the Demo Controls on the right!
        </p>
      `;
      Aegis.showToast('Operating in Simulated Sensor Mode', 'warning');
      isMonitoringActive = true;
      startSimulatedSwayLoop();
    }
  });

  // Audio loudness / impact calculation
  function setupAudioMonitoring(stream) {
    try {
      const AudioCtx = window.AudioContext || window.webkitAudioContext;
      audioContext = new AudioCtx();
      const source = audioContext.createMediaStreamSource(stream);
      audioAnalyser = audioContext.createAnalyser();
      audioAnalyser.fftSize = 256;
      source.connect(audioAnalyser);
      audioDataArray = new Uint8Array(audioAnalyser.frequencyBinCount);

      setInterval(() => {
        if (!isMonitoringActive || !audioAnalyser) return;
        audioAnalyser.getByteFrequencyData(audioDataArray);
        let sum = 0;
        for (let i = 0; i < audioDataArray.length; i++) {
          sum += audioDataArray[i];
        }
        let avg = sum / audioDataArray.length;
        currentSoundLevel = Math.min(1.0, +(avg / 128.0).toFixed(2));
        if (hudSound) hudSound.textContent = currentSoundLevel.toFixed(2);

        // Impact detection spike
        if (currentSoundLevel > 0.85) {
          console.log('Loud impact sound spike detected locally:', currentSoundLevel);
          Aegis.sendEvent({
            patientId: PATIENT_ID,
            type: 'IMPACT_SOUND',
            soundLevel: currentSoundLevel,
            room: 'bedroom',
            simulated: false,
            pretend3am: chkPretend3am ? chkPretend3am.checked : false,
            timestamp: new Date().toISOString()
          });
        }
      }, 500);
    } catch (e) {
      console.warn('Audio analyser could not be attached:', e);
    }
  }

  // Load MediaPipe Pose Landmarker from vision bundle
  async function loadMediaPipe() {
    try {
      if (typeof vision === 'undefined' || !vision.PoseLandmarker) {
        console.warn('MediaPipe vision script not loaded from CDN, falling back to simulated pose loop.');
        startSimulatedSwayLoop();
        return;
      }
      const filesetResolver = await vision.FilesetResolver.forVisionTasks(
        "https://cdn.jsdelivr.net/npm/@mediapipe/tasks-vision@0.10.14/wasm"
      );
      poseLandmarker = await vision.PoseLandmarker.createFromOptions(filesetResolver, {
        baseOptions: {
          modelAssetPath: "https://storage.googleapis.com/mediapipe-models/pose_landmarker/pose_landmarker_lite/float16/1/pose_landmarker_lite.task",
          delegate: "GPU"
        },
        runningMode: "VIDEO",
        numPoses: 1
      });
      console.log('MediaPipe PoseLandmarker loaded successfully.');
    } catch (e) {
      console.warn('MediaPipe model load error, fallback active:', e);
      startSimulatedSwayLoop();
    }
  }

  // 5. Video Processing & Skeleton Drawing Loop
  let lastVideoTime = -1;
  const ctx = skeletonCanvas.getContext('2d');

  function processVideoFrame() {
    if (!isMonitoringActive) return;

    if (cameraVideo.readyState >= 2 && cameraVideo.currentTime !== lastVideoTime) {
      lastVideoTime = cameraVideo.currentTime;

      // Adjust canvas size to match video element
      if (skeletonCanvas.width !== cameraVideo.videoWidth || skeletonCanvas.height !== cameraVideo.videoHeight) {
        skeletonCanvas.width = cameraVideo.videoWidth || 640;
        skeletonCanvas.height = cameraVideo.videoHeight || 480;
      }

      ctx.clearRect(0, 0, skeletonCanvas.width, skeletonCanvas.height);

      if (poseLandmarker) {
        const startTimeMs = performance.now();
        const results = poseLandmarker.detectForVideo(cameraVideo, startTimeMs);

        if (results.landmarks && results.landmarks.length > 0) {
          const landmarks = results.landmarks[0];
          drawSkeleton(landmarks, ctx, skeletonCanvas.width, skeletonCanvas.height);
          analyzePose(landmarks);
        } else {
          // No person visible
          handlePersonLost();
        }
      }
    }

    requestAnimationFrame(processVideoFrame);
  }

  // Analyze Pose Landmarks: Posture & Sway Score
  function analyzePose(landmarks) {
    // MediaPipe landmarks:
    // 0: nose, 11: left_shoulder, 12: right_shoulder, 23: left_hip, 24: right_hip, 27: left_ankle, 28: right_ankle
    const nose = landmarks[0];
    const leftShoulder = landmarks[11];
    const rightShoulder = landmarks[12];
    const leftHip = landmarks[23];
    const rightHip = landmarks[24];
    const leftAnkle = landmarks[27];
    const rightAnkle = landmarks[28];

    const shoulderY = (leftShoulder.y + rightShoulder.y) / 2;
    const hipY = (leftHip.y + rightHip.y) / 2;
    const shoulderX = (leftShoulder.x + rightShoulder.x) / 2;
    const hipX = (leftHip.x + rightHip.x) / 2;

    const torsoHeight = Math.abs(hipY - shoulderY);
    const torsoWidth = Math.abs(leftShoulder.x - rightShoulder.x);

    // Determine Posture
    // If shoulder and hip are nearly at the same vertical level or very low in frame -> HORIZONTAL
    if (torsoHeight < 0.10 || (shoulderY > 0.75 && hipY > 0.75)) {
      currentPosture = 'HORIZONTAL';
    } else if (torsoHeight < 0.22) {
      currentPosture = 'SITTING';
    } else {
      currentPosture = 'UPRIGHT';
    }

    // Sway calculation (sliding window variance of torso center)
    const currentCenter = { x: (shoulderX + hipX) / 2, y: (shoulderY + hipY) / 2 };
    lastSwayPositions.push(currentCenter);
    if (lastSwayPositions.length > 15) {
      lastSwayPositions.shift();
    }

    // Compute standard deviation of positions
    let meanX = 0, meanY = 0;
    for (const p of lastSwayPositions) {
      meanX += p.x;
      meanY += p.y;
    }
    meanX /= lastSwayPositions.length;
    meanY /= lastSwayPositions.length;

    let variance = 0;
    for (const p of lastSwayPositions) {
      variance += Math.hypot(p.x - meanX, p.y - meanY);
    }
    currentSwayScore = +(variance / lastSwayPositions.length).toFixed(4);

    // Stillness tracking: if sway < 0.015 while sitting or standing
    if (currentSwayScore < 0.018 && (currentPosture === 'UPRIGHT' || currentPosture === 'SITTING')) {
      stillSeconds += 1;
    } else {
      stillSeconds = Math.max(0, stillSeconds - 1);
    }

    // Update HUD
    if (hudPosture) hudPosture.textContent = currentPosture;
    if (hudSway) hudSway.textContent = currentSwayScore.toFixed(3);
    if (hudStill) hudStill.textContent = stillSeconds + 's';

    // Dispatch periodic event to backend (at most every 1.5 seconds)
    const now = Date.now();
    if (now - lastEventDispatchTime > 1500) {
      lastEventDispatchTime = now;
      dispatchPoseEvent();
    }
  }

  function handlePersonLost() {
    currentPosture = 'UNKNOWN';
    if (hudPosture) hudPosture.textContent = 'NONE';
  }

  function dispatchPoseEvent() {
    Aegis.sendEvent({
      patientId: PATIENT_ID,
      type: 'POSE_STATE',
      posture: currentPosture,
      swayScore: currentSwayScore,
      stillSeconds: stillSeconds,
      soundLevel: currentSoundLevel,
      room: 'bedroom',
      simulated: false,
      pretend3am: chkPretend3am ? chkPretend3am.checked : false,
      timestamp: new Date().toISOString()
    });
  }

  // Draw Green Skeleton on Canvas
  const POSE_CONNECTIONS = [
    [11, 12], [11, 13], [13, 15], [12, 14], [14, 16], // Upper body
    [11, 23], [12, 24], [23, 24],                      // Torso
    [23, 25], [25, 27], [24, 26], [26, 28]             // Lower body
  ];

  function drawSkeleton(landmarks, context, w, h) {
    context.save();
    context.strokeStyle = '#22C55E';
    context.fillStyle = '#22C55E';
    context.lineWidth = 3;
    context.shadowColor = 'rgba(34, 197, 94, 0.6)';
    context.shadowBlur = 8;

    // Draw lines
    for (const [i, j] of POSE_CONNECTIONS) {
      const p1 = landmarks[i];
      const p2 = landmarks[j];
      if (p1 && p2 && p1.visibility > 0.5 && p2.visibility > 0.5) {
        context.beginPath();
        context.moveTo(p1.x * w, p1.y * h);
        context.lineTo(p2.x * w, p2.y * h);
        context.stroke();
      }
    }

    // Draw landmark points
    for (let i = 0; i < landmarks.length; i++) {
      const p = landmarks[i];
      if (p.visibility > 0.5) {
        context.beginPath();
        context.arc(p.x * w, p.y * h, 4, 0, 2 * Math.PI);
        context.fill();
      }
    }
    context.restore();
  }

  // Fallback simulated pose loop (for devices with no webcam)
  function startSimulatedSwayLoop() {
    setInterval(() => {
      if (!isMonitoringActive) return;
      currentSwayScore = +(0.020 + (Math.random() * 0.008)).toFixed(3);
      if (hudSway) hudSway.textContent = currentSwayScore.toFixed(3);
      if (hudPosture) hudPosture.textContent = currentPosture;
    }, 2000);
  }

  // 6. Camera Mirror Toggle
  if (chkShowCamera) {
    chkShowCamera.addEventListener('change', (e) => {
      if (e.target.checked) {
        cameraVideo.classList.add('visible');
      } else {
        cameraVideo.classList.remove('visible');
      }
    });
  }

  // 7. Interactive Simulation Controls
  btnSimHiddenFall.addEventListener('click', () => {
    Aegis.showToast('DEMO: Simulating Hidden Fall (Impact + Disappearance)', 'warning');
    Aegis.sendEvent({
      patientId: PATIENT_ID,
      type: 'SIMULATED_FALL',
      posture: 'HORIZONTAL',
      soundLevel: 0.95,
      room: 'bathroom',
      simulated: true,
      pretend3am: chkPretend3am.checked,
      timestamp: new Date().toISOString()
    });
  });

  btnSimFreeze.addEventListener('click', () => {
    Aegis.showToast('DEMO: Simulating Freeze (Stillness >15s)', 'warning');
    stillSeconds = 16;
    currentSwayScore = 0.005;
    Aegis.sendEvent({
      patientId: PATIENT_ID,
      type: 'SIMULATED_FREEZE',
      posture: 'SITTING',
      swayScore: 0.005,
      stillSeconds: 16,
      room: 'living room',
      simulated: true,
      pretend3am: chkPretend3am.checked,
      timestamp: new Date().toISOString()
    });
  });

  btnSimBedExit.addEventListener('click', () => {
    const is3am = chkPretend3am.checked;
    Aegis.showToast(`DEMO: Simulating Bed Exit (3 AM: ${is3am ? 'ON' : 'OFF'})`, 'info');
    Aegis.sendEvent({
      patientId: PATIENT_ID,
      type: 'BED_EXIT',
      posture: 'UPRIGHT',
      room: 'bedroom',
      simulated: true,
      pretend3am: is3am,
      timestamp: new Date().toISOString()
    });
  });

  btnSimWalkingToDoor.addEventListener('click', () => {
    const is3am = chkPretend3am.checked;
    Aegis.showToast('DEMO: Simulating Walking to Exit Door', 'warning');
    Aegis.sendEvent({
      patientId: PATIENT_ID,
      type: 'ROOM_CHANGE',
      room: 'hallway',
      simulated: true,
      pretend3am: is3am,
      timestamp: new Date().toISOString()
    });
  });

  btnSimDoorOpening.addEventListener('click', () => {
    const is3am = chkPretend3am.checked;
    Aegis.showToast(`DEMO: Simulating Exterior Door Opening (3 AM: ${is3am ? 'ON' : 'OFF'})`, 'error');
    Aegis.sendEvent({
      patientId: PATIENT_ID,
      type: 'DOOR_OPEN',
      room: 'front door',
      simulated: true,
      pretend3am: is3am,
      timestamp: new Date().toISOString()
    });
  });

  btnResetDemo.addEventListener('click', async () => {
    try {
      await Aegis.resetDemo();
      stopCountdown();
      hideVoicePrompt();
      stillSeconds = 0;
      currentPosture = 'UPRIGHT';
      Aegis.showToast('Demo state reset to NORMAL', 'success');
    } catch (e) {
      Aegis.showToast('Error resetting demo', 'error');
    }
  });

})();
