/**
 * 1. WEBCAM INITIALIZATION
 * Used by both Admin (Registration) and Voter (Verification)
 */
function initWebcam(selector) {
    Webcam.set({
        width: 320,
        height: 240,
        image_format: 'jpeg',
        jpeg_quality: 90
    });
    Webcam.attach(selector);
}

/**
 * 2. ADMIN: LOGIN LOGIC
 */
async function adminLogin() {
    const user = document.getElementById('user').value;
    const pass = document.getElementById('pass').value;

    const res = await fetch('/api/admin/login', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({ username: user, password: pass })
    });

    if (await res.text() === "SUCCESS") {
        sessionStorage.setItem("adminLoggedIn", "true");
        window.location.href = "dashboard.html";
    } else {
        alert("Invalid Admin Credentials");
    }
}

/**
 * 3. ADMIN: HANDOVER & LOCK SYSTEM
 * This destroys the Admin session and forces the browser into Voter Mode.
 */
function lockAndOpenPortal() {
    const proceed = confirm("⚠️ SYSTEM LOCK ALERT:\n\nOnce you open the Voter Portal, you cannot return to the dashboard without the Admin Password.\n\nProceed to Handover?");
    if (proceed) {
        sessionStorage.removeItem("adminLoggedIn"); // Security: Remove Admin access
        sessionStorage.setItem("voterModeActive", "true"); // Flag: System is in Voter Mode
        window.location.href = "voterLogin.html";
    }
}

/**
 * 4. VOTER: BIOMETRIC VERIFICATION
 */
async function verifyVoter() {
    const id = document.getElementById('vid').value;
    if (!id) return alert("Please enter Voter ID");

    Webcam.snap(async (data_uri) => {
        const res = await fetch('/api/voter/verify', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({ voterId: id, faceData: data_uri })
        });

        const status = await res.text();
        if (status === "MATCH") {
            localStorage.setItem("currentVoterId", id);
            window.location.href = "vote.html";
        } else {
            alert("Verification Failed: " + status);
        }
    });
}

/**
 * 5. VOTER: LOAD CANDIDATES & CAST VOTE
 */
async function loadCandidates() {
    const res = await fetch('/api/candidates');
    const data = await res.json();
    let html = '';
    data.forEach(c => {
        html += `
            <div class="candidate-item" style="margin-bottom:15px; border-bottom:1px solid #eee; padding:10px;">
                <b>${c.name}</b> (${c.partySymbol})<br>
                <button onclick="submitVote(${c.id})" style="width:auto; margin-top:5px;">Vote</button>
            </div>`;
    });
    document.getElementById('candidate-list').innerHTML = html;
}

async function submitVote(candidateId) {
    if(!confirm("Confirm your vote? This action cannot be undone.")) return;
    
    const voterId = localStorage.getItem("currentVoterId");
    const res = await fetch(`/api/voter/vote?voterId=${voterId}&candidateId=${candidateId}`, {
        method: 'POST'
    });

    if (await res.text() === "SUCCESS") {
        alert("Vote Cast Successfully!");
        localStorage.removeItem("currentVoterId");
        window.location.href = "voterLogin.html"; // Returns to the LOCKED portal login
    } else {
        alert("Voting Error. Please contact the administrator.");
    }
}

/**
 * 6. ADMIN: SECRET UNLOCK
 * Used to return to the Dashboard after the election is over.
 */
async function adminUnlock() {
    const pass = prompt("SYSTEM LOCKED. Enter Admin Password to return to Dashboard:");
    if (!pass) return;

    const res = await fetch('/api/admin/login', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({ username: 'admin', password: pass })
    });

    if (await res.text() === "SUCCESS") {
        sessionStorage.setItem("adminLoggedIn", "true");
        sessionStorage.removeItem("voterModeActive");
        window.location.href = "dashboard.html";
    } else {
        alert("Invalid Password!");
    }
}
let modelsLoaded = false;

// Load models on page start
async function loadFaceModels() {
    await faceapi.nets.tinyFaceDetector.loadFromUri('https://raw.githubusercontent.com/justadudewhohacks/face-api.js/master/weights');
    await faceapi.nets.faceLandmark68Net.loadFromUri('https://raw.githubusercontent.com/justadudewhohacks/face-api.js/master/weights');
    modelsLoaded = true;
}

async function verify() {
    if(!modelsLoaded) return alert("AI Models loading, please wait...");

    const video = document.querySelector('video');
    const detections = await faceapi.detectSingleFace(video, new faceapi.TinyFaceDetectorOptions()).withFaceLandmarks();

    if (detections) {
        const landmarks = detections.landmarks;
        const leftEye = landmarks.getLeftEye();
        const rightEye = landmarks.getRightEye();

        // Simple Blink Detection (Check if eyes are narrow)
        const eyeDist = Math.abs(leftEye[0].y - leftEye[3].y);
        
        if (eyeDist < 3) { // Threshold for blink
            alert("Liveness Confirmed! Processing face match...");
            processAWSVerification(); // Call your existing AWS function here
        } else {
            alert("Please BLINK your eyes to prove you are a real person!");
        }
    }
}