# Aegis Ambient — Privacy-First Home Safety Prototype

> **PromptWars 2026 Hackathon Demonstration Prototype**  
> *Tagline: Privacy-first care for hidden and silent emergencies at home.*

A cohesive, modern web application and edge sensing prototype built with **Spring Boot 3**, **Spring WebSocket with STOMP**, **Spring Data JPA**, **H2 Database**, **Vanilla HTML5/CSS3/JavaScript**, and browser-side **MediaPipe Pose Landmarker**.

---

## 1. Safety and Medical Disclaimer

> **IMPORTANT DISCLAIMER**  
> **Prototype for demonstration purposes only. Not a certified medical device.**  
> In a genuine medical or personal safety emergency, always contact local emergency services immediately (911 or local equivalent).  
> Aegis Ambient detects movement indicators and sound signals; it **does not** diagnose strokes, seizures, cardiac events, or any clinical conditions.

---

## 2. The Four Safety Scenarios

Aegis Ambient addresses four common home safety emergencies that traditional cameras or push-button panic pendants often miss:

| Scenario | Detection Strategy | Prototype vs. Production Hardware |
|---|---|---|
| **1. Hidden Fall** (e.g. bathroom, behind sofa) | Audio impact spike (`IMPACT_SOUND`) followed by sudden loss of camera visibility or low posture within a 10-second window. | *Prototype:* Browser microphone level + camera bounding disappearance.<br>*Production:* mmWave radar and floor vibration sensors. |
| **2. Prolonged Stillness** (silent emergency) | MediaPipe pose tracking calculates center-of-mass sway variance over a sliding window. Upright/sitting stillness with sway below `0.020` for $>15$s in demo triggers check-in. | *Prototype:* Optical skeleton sway tracking in browser.<br>*Production:* mmWave micro-doppler radar (respiration & chest wall motion). |
| **3. Night-time Wandering** (dementia care) | Rule engine checks configurable quiet hours (`22:00` to `06:00`).<br>• *Tier 1:* Bed exit triggers a gentle voice prompt.<br>• *Tier 2:* Wandering past baseline into hallways triggers a soft caregiver notification.<br>• *Tier 3:* Exterior door opening triggers an immediate critical alert. | *Prototype:* Browser event stream + "Pretend It Is 3 AM" toggle.<br>*Production:* Under-mattress pressure sensor, passive infrared (PIR) hallway motion, and reed door contacts. |
| **4. Verified Alerts** (false-alarm prevention) | Two-stage verification: state machine enters `CHECK_IN` (speaks voice prompt via Web Speech API), then `COUNTDOWN` (30s timer). The person can speak *"Yes"* or click the prominent **"I'm OK"** button to cancel before caregiver dispatch. | *Prototype & Production:* Voice recognition + tactile wall/wearable button with server-side timer enforcement. |

---

## 3. Architecture & Privacy Guarantee

```
+-------------------------------------------------------+
|                 BROWSER CLIENTS                       |
|                                                       |
|  [ Patient Page: /patient.html ]                      |
|   • MediaPipe Pose Landmarker (local WebGL/CPU)       |
|   • Skeleton Canvas (green vector overlay)            |
|   • Web Audio Analyser (loudness / impact spike)      |
|   • Web Speech API (synthesis & recognition)          |
|   • "I'm OK" Big Action Button                        |
|   • Interactive Scenario Simulation Controls          |
|                       |                               |
|        STOMP /app/event (Metadata JSON only)          |
|                       v                               |
+-------------------------------------------------------+
|             SPRING BOOT 3 BACKEND SERVER              |
|                                                       |
|   • EventController (@MessageMapping("/event"))       |
|   • RuleEngine (Fall, HiddenFall, Stillness, Wander)  |
|   • IncidentStateMachine (Java server-side timers)    |
|   • H2 In-Memory Database (Spring Data JPA)           |
|   • LogNotificationService (Simulated SMS Dispatch)   |
|   • TwilioNotificationService (Optional, disabled)    |
|                       |                               |
|        STOMP /topic/caregiver/{id}/status             |
|                       v                               |
+-------------------------------------------------------+
|  [ Caregiver Dashboard: /caregiver.html ]             |
|   • Live Status Banner (NORMAL / CHECK_IN / ALERT)    |
|   • Plain-English Incident Intelligence Insight Card   |
|   • Real-Time Countdown Ring & Audio Chime            |
|   • Incident Audit History Timeline (REST API)        |
|   • First-Responder Card (John Doe, 78, Penicillin)   |
|   • Mock Smart-Home Controls (Lock / Porch Lights)    |
+-------------------------------------------------------+
```

### Privacy Guarantees
1. **Zero Raw Video or Images Uploaded:** All video frames remain exclusively in browser RAM. No camera images, video files, or raw landmark arrays are ever sent across the network or saved to disk.
2. **Metadata-Only Network Payload:** The browser transmits only high-level summary JSON (e.g. `type: "POSE_STATE", posture: "UPRIGHT", swayScore: 0.021, timestamp: "..."`).
3. **Explicit Consent:** Camera and microphone hardware permissions are requested **only** when the user clicks the prominent *"Start Monitoring"* button.

---

## 4. Quick Start & Execution

### Prerequisites
- **Java 17** or higher (Java 17–26 supported).
- Maven (a portable Maven 3.9 launcher `mvn.cmd` and `mvnw.cmd` is pre-configured in this repository).

### Run with One Command
Open PowerShell or your terminal in the project directory:

```powershell
.\mvn.cmd spring-boot:run
```
*(Or `mvn spring-boot:run` if Maven is installed in your global system PATH)*

Once started, open your web browser at:
- **Home Landing Page:** [http://localhost:8080/](http://localhost:8080/)
- **Patient Monitoring Interface:** [http://localhost:8080/patient.html](http://localhost:8080/patient.html)
- **Caregiver Dashboard:** [http://localhost:8080/caregiver.html](http://localhost:8080/caregiver.html)
- **H2 Database Console:** [http://localhost:8080/h2-console](http://localhost:8080/h2-console) (JDBC URL: `jdbc:h2:mem:aegisdb`, Username: `sa`, Password: *empty*)

### Run Unit and Integration Tests

```powershell
.\mvn.cmd test
```
All 20 test cases will execute:
- Sit-down false positive guards (UPRIGHT $\rightarrow$ SITTING does not trigger a fall).
- True positive fall detection (rapid transition to HORIZONTAL sustained $\ge 3$s).
- Impact sound followed by disappearance within window.
- Prolonged stillness low sway detection.
- Night-time 3-tier wandering escalation (gentle prompt, hallway warning, door opening critical alert).
- Two-stage verification and *"I'm OK"* cancellation.
- Caregiver alert resolution REST API.

---

## 5. Five-Minute Hackathon Demo Script

For the best judging demonstration, open **two browser windows side-by-side**:
- **Window A (Left):** [http://localhost:8080/patient.html](http://localhost:8080/patient.html)
- **Window B (Right):** [http://localhost:8080/caregiver.html](http://localhost:8080/caregiver.html)

### Step 1: Normal State Baseline (0:00 - 0:45)
1. Point out the shared navigation bar, brand tagline (*"Privacy-first care at home"*), and green **"Backend Online"** STOMP indicator.
2. Note that on the Caregiver Dashboard, the status banner shows **"All Clear"** (green) and active alerts is `0`.
3. Note the First-Responder info box pre-seeded with demo patient John Doe (78, O+, Penicillin allergy, diabetic).

### Step 2: Test Scenario 1 — Hidden Fall & Verification Cancellation (0:45 - 2:00)
1. In the Patient window under **Demo Controls**, click **"Simulate Hidden Fall"**.
2. **Watch the reaction in both windows:**
   - Patient window announces by voice: *"Hi John, are you okay? Say yes, or press the button."* and displays the countdown box.
   - Caregiver Dashboard banner turns **amber**: *"Checking on Patient"*.
   - Insight card displays: *"Possible hidden fall in the bathroom. A loud impact sound was detected followed by sudden disappearance from view."*
3. In the Patient window, click the large green **"I'M OK — CANCEL ALERT"** button (or speak *"Yes"*).
4. Both windows immediately return to **"All Clear"** (green).
5. Highlight the Caregiver Dashboard's timeline: a record is logged showing `CANCELLED` by patient response.

### Step 3: Test Scenario 2 — Unresponsive Escalation (2:00 - 3:15)
1. Click **"Simulate Freeze"** (or **"Simulate Hidden Fall"**).
2. The verification countdown begins (e.g. 15 seconds in demo mode).
3. **Do not click anything.** Let the timer expire.
4. When countdown reaches 0:
   - Server-side state machine escalates to **CRITICAL ALERT**.
   - Caregiver Dashboard banner flashes **red** with an audible alarm tone.
   - Check the server terminal output: formatted simulated SMS dispatch is logged to `Jane Doe (Daughter) [(555) 234-5678]`.
5. On the Caregiver Dashboard, click the red **"Resolve Alert"** button. The dashboard confirms resolution and returns to **"All Clear"**.

### Step 4: Test Scenario 3 — Night-time Wandering & Door Exit (3:15 - 4:15)
1. In the Patient window, toggle **"Pretend It Is 3 AM"** to `ON`.
2. Click **"Simulate Bed Exit"**:
   - The browser speaks a gentle, calm voice prompt: *"It is night time. Would you like to go to the bathroom? I will turn on the light."*
   - Status remains informative (Tier 1).
3. Click **"Simulate Walking to Door"**:
   - Caregiver dashboard updates with a warning indicating movement toward the exit hallway (Tier 2).
4. Click **"Simulate Door Opening"**:
   - Immediately triggers **CRITICAL ALERT** (Tier 3) without waiting for countdown, because an exterior door opened in the middle of the night.
   - SMS dispatch is logged to terminal.

### Step 5: Test Scenario 4 — Mock Smart Home Entry (4:15 - 5:00)
1. On the Caregiver Dashboard, under **Smart Home Emergency Access**, click **"Unlock Smart Lock"**.
2. A toast appears confirming mock unlocking of the front door for emergency responders.
3. Click **"Reset Demo"** to return all timers, states, and rules back to initial standby.

---

## 6. Configurable Thresholds (`application.yml`)

All timings, thresholds, and quiet hours are configurable in `src/main/resources/application.yml`:

```yaml
aegis:
  # Expedites timers for hackathon presentations (6s check-in, 15s countdown)
  demo-mode: true

  # Production timing defaults
  check-in-seconds: 15
  demo-check-in-seconds: 6
  countdown-seconds: 30
  demo-countdown-seconds: 15
  cooldown-seconds: 60

  # Stillness thresholds
  stillness-threshold-seconds: 120
  demo-stillness-threshold-seconds: 15
  sway-threshold: 0.020

  # Quiet hours configuration
  quiet-hours-start: "22:00"
  quiet-hours-end: "06:00"
  out-of-bed-baseline-seconds: 600
  demo-out-of-bed-baseline-seconds: 30

  # Fall detection guards
  fall-impact-window-seconds: 10
  fall-low-posture-seconds: 3

  # Optional Twilio SMS (Disabled by default, runs in log mode without API keys)
  twilio:
    enabled: false
    account-sid: ${TWILIO_ACCOUNT_SID:}
    auth-token: ${TWILIO_AUTH_TOKEN:}
    from-number: ${TWILIO_FROM_NUMBER:}
    to-number: ${TWILIO_TO_NUMBER:}
```

---

## 7. Limitations and Future Hardware Path

While this software prototype proves the local edge processing paradigm, two-stage verification, and multi-tier state machine, production healthcare deployments would upgrade from optical camera simulation to non-optical sensor hardware:

1. **mmWave FMCW Radar (60 GHz / 77 GHz):**
   - Detects micro-vibrations of the chest wall (heart rate and respiratory rate) through clothing, steam, and darkness without any camera optics.
   - Perfect for high-risk private spaces such as showers and bathrooms.
2. **Piezoelectric Floor Vibration Sensors:**
   - Detects the mechanical shock signature of a human body hitting the floor vs. a dropped phone or book.
3. **Under-Mattress Ballistocardiography (BCG) Strips:**
   - Provides true bed presence, sleep quality, and bed exit detection without wearable devices.
4. **Sub-GHz Long-Range Mesh (Matter / Thread):**
   - Connects edge sensor nodes across large multi-story homes without depending on consumer Wi-Fi uptime.
