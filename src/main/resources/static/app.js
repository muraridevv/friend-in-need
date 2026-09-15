const $ = (selector) => document.querySelector(selector);
const dialog = $('#profile-dialog');
const form = $('#profile-form');
const messages = $('#messages');
const identityDialog = $('#identity-dialog');
const authForm = $('#auth-form');
let jwt;
const nativeFetch = window.fetch.bind(window);
window.fetch = (input, init = {}) => { const headers = new Headers(init.headers || {}); if (jwt && !headers.has("Authorization")) headers.set("Authorization", `Bearer ${jwt}`); return nativeFetch(input, { ...init, headers }); };
// Do not restore the prior browser user: every fresh page load requires a face login or profile creation.
let profile = null;
let notifications;
let recorder;
let recording = false;
let cameraStream;
let cameraPurpose;
const PRESENCE_ENABLED = true; let presenceStream, presenceCanvas, lastPresenceFrame, absentSince, presenceInterval;
let voiceSocket, handsFree = false, audioQueue = [], playingAudio, speechStartedAt;
const latency = {};
let simulateOffline = false;
function routedHeaders(headers = {}) { return simulateOffline ? { ...headers, "X-Simulate-Offline": "true" } : headers; }
async function updateSystemStatus() {
  try { const status = await responseJson(await fetch('/api/system/status', { headers: routedHeaders() }));
    const values = [status.chat, status.stt, status.tts]; const allCloud = values.every(value => value === 'cloud');
    const allOffline = values.every(value => value === 'local'); const bar = $('#system-status');
    bar.className = `system-status ${allCloud ? 'connected' : allOffline ? 'offline' : 'local'}`;
    bar.querySelector('span').textContent = allCloud ? 'Connected' : allOffline ? 'Offline mode' : 'Local mode (limited)';
  } catch { const bar = $('#system-status'); bar.className = 'system-status offline'; bar.querySelector('span').textContent = 'Offline mode'; }
}

function showLatency() { const node = $("#latency-values"); if (node) node.textContent = Object.entries(latency).map(([k,v]) => `${k}: ${Math.round(v)}ms`).join("\n"); }
function checkWakeWord(audioBuffer) { /* Replace with Porcupine.js or a custom wake-word model */ return true; }
class SentenceChunker { constructor(onSentence) { this.value = ""; this.onSentence = onSentence; } push(token) { this.value += token; const parts = this.value.split(/(?<=[.!?])\s+|\n/); this.value = parts.pop(); parts.filter(Boolean).forEach(this.onSentence); } finish() { if (this.value.trim()) this.onSentence(this.value); this.value = ""; } }
function stopTalking() { if (playingAudio) { playingAudio.pause(); playingAudio = null; } audioQueue.forEach(url => URL.revokeObjectURL(url)); audioQueue = []; document.querySelectorAll(".bubble.companion").forEach(b => { if (!b.textContent.includes("(interrupted)")) b.append(" (interrupted)"); }); }
async function queueSentence(sentence) { const started = performance.now(); const response = await fetch("/api/voice/speech", { method:"POST", headers:routedHeaders({"Content-Type":"application/json"}), body:JSON.stringify({text:sentence}) }); if (!response.ok) return; const url = URL.createObjectURL(await response.blob()); audioQueue.push(url); if (!playingAudio) playNext(started); }
function playNext(started) { const url = audioQueue.shift(); if (!url) { playingAudio = null; return; } playingAudio = new Audio(url); playingAudio.onplay=()=>{latency["TTS first sentence"] = performance.now()-started; showLatency();}; playingAudio.onended=()=>{URL.revokeObjectURL(url); playNext(started);}; playingAudio.play(); }
function startVoiceTurn() { stopTalking(); speechStartedAt=performance.now(); voiceSocket = new WebSocket(`${location.protocol === "https:" ? "wss" : "ws"}://${location.host}/ws/voice`); voiceSocket.onmessage=(event)=>{const data=JSON.parse(event.data); if(data.final){ $("#message").value=data.transcript; latency["STT total"]=performance.now()-speechStartedAt; showLatency(); $("#composer").requestSubmit(); }}; }
function endVoiceTurn() { if (voiceSocket?.readyState === WebSocket.OPEN) voiceSocket.send("end"); }
async function setupVad() { if (!window.vad) return; try { const vad = await vad.MicVAD.new({ onSpeechStart:()=>{ avatar.setExpression('LISTENING'); avatar.nod(); $("#voice-dot").classList.add("active"); if (!voiceSocket && checkWakeWord()) startVoiceTurn(); }, onSpeechEnd:(audio)=>{ avatar.setExpression('THINKING'); $("#voice-dot").classList.remove("active"); if (voiceSocket?.readyState === WebSocket.OPEN) { const pcm=new Int16Array(audio.length); audio.forEach((v,i)=>pcm[i]=Math.max(-1,Math.min(1,v))*32767); voiceSocket.send(pcm.buffer); endVoiceTurn(); voiceSocket=null; } } }); if(handsFree) vad.start(); window.voiceVad=vad; } catch(error) { console.warn("VAD unavailable", error); } }

function emotionDot(emotion) { const dot=document.createElement("i"); dot.className=`emotion-dot ${String(emotion || "NEUTRAL").toLowerCase()}`; return dot; }
async function loadMood() { if (!profile) return; try { const values=await responseJson(await fetch(`/api/profiles/${profile.id}/emotions?limit=10`)); const strip=$("#mood-strip"); strip.replaceChildren(...values.reverse().map(value=>emotionDot(value.emotion))); } catch {} }
async function startPresence() { if (!PRESENCE_ENABLED || !profile || presenceStream || !window.FaceDetector) return; try { presenceStream = await navigator.mediaDevices.getUserMedia({video:{width:320,height:240},audio:false}); const video=document.createElement("video"); video.srcObject=presenceStream; await video.play(); presenceCanvas=document.createElement("canvas"); presenceCanvas.width=320; presenceCanvas.height=240; const detector=window.FaceDetector ? new FaceDetector({fastMode:true,maxDetectedFaces:4}) : null; presenceInterval=setInterval(async()=>{ if(!profile) return; const ctx=presenceCanvas.getContext("2d");ctx.drawImage(video,0,0,320,240);let faces=[];try{faces=detector?await detector.detect(presenceCanvas):[];}catch{} const present=faces.length>0; if(present) absentSince=null; else absentSince??=Date.now(); const away=!present&&Date.now()-absentSince>=30000; const result=await responseJson(await fetch(`/api/profiles/${profile.id}/presence`,{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({status:away?"AWAY":"PRESENT",faceCount:faces.length})})); if(result.multiplePeople) bubble("I notice someone else is here. Would they like to say hi?", "companion proactive", "Nova checked in"); if(result.changed&&result.newStatus==="AWAY") $("#messages").dataset.presence="away"; if(result.changed&&result.newStatus==="PRESENT") { $("#messages").dataset.presence="present"; if(result.awayDurationMinutes) bubble(`Welcome back! You were away for about ${result.awayDurationMinutes} minutes. How’s it going?`,"companion proactive","Nova checked in"); } },2000); }catch(error){console.warn("Presence detection unavailable",error);} }
function updateProfile() {
  $('#profile-name').textContent = profile ? profile.displayName : 'Getting to know you';
  $('#profile-detail').textContent = profile ? `${profile.faceEnrolled ? 'Face enrolled · ' : ''}${profile.personality}` : 'Set up your companion below.';
}
async function responseJson(response) {
  if (response.ok) return response.json();
  const error = await response.json().catch(() => ({ error: 'Something went wrong' }));
  throw new Error(error.error || 'Something went wrong');
}
async function api(path, body) { return responseJson(await fetch(`/api${path}`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) })); }
function bubble(text, who, label) { const node = document.createElement('article'); node.className = `bubble ${who}`; if (label) { const tag = document.createElement('small'); tag.className = 'bubble-label'; tag.textContent = label; node.append(tag); } node.append(document.createTextNode(text)); messages.append(node); messages.scrollTop = messages.scrollHeight; return node; }
function openNotifications() { if (notifications) notifications.close(); if (!profile) return; notifications = new EventSource(`/api/profiles/${profile.id}/notifications`); notifications.addEventListener('proactive', (event) => bubble(event.data, 'companion proactive', 'Nova checked in')); }
async function speak(text) {
  try {
    const response = await fetch('/api/voice/speech', { method: 'POST', headers: routedHeaders({ 'Content-Type': 'application/json' }), body: JSON.stringify({ text }) });
    if (!response.ok) throw new Error();
    const audio = new Audio(URL.createObjectURL(await response.blob()));
    audio.onended = () => URL.revokeObjectURL(audio.src);
    await audio.play();
  } catch { /* Voice provider is optional; text remains available. */ }
}
authForm.addEventListener('submit', async (event) => { event.preventDefault(); const values = new FormData(authForm); try { const response = await nativeFetch('/api/auth/login', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ username: values.get('username'), password: values.get('password') }) }); jwt = (await responseJson(response)).token; $('#auth-error').textContent = ''; $('#recognize-login').focus(); } catch (error) { $('#auth-error').textContent = error.message; } });
$('#register').onclick = async () => { const values = new FormData(authForm); try { const response = await nativeFetch('/api/auth/register', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ username: values.get('username'), password: values.get('password') }) }); jwt = (await responseJson(response)).token; $('#auth-error').textContent = ''; $('#recognize-login').focus(); } catch (error) { $('#auth-error').textContent = error.message; } };
$('#show').onclick = async () => { if (!presenceCanvas) return; presenceCanvas.toBlob(async blob => { const form = new FormData(); form.append('image', blob, 'show.jpg'); form.append('prompt', 'Describe what you see'); const result = await responseJson(await fetch('/api/vision/describe', { method:'POST', body:form })); bubble(result.description, 'companion proactive', 'What Nova sees'); $('#message').value = `I'm showing you something: ${result.description}`; $('#composer').requestSubmit(); }, 'image/jpeg'); };
$('#hands-free').onclick = () => { handsFree = !handsFree; $('#hands-free').classList.toggle('active', handsFree); if (handsFree) setupVad(); };
$('#mic').onclick = () => { startVoiceTurn(); setupVad(); window.voiceVad?.start(); };
$('#menu').onclick = () => document.querySelector('aside').classList.toggle('open');
$('#settings').onclick = () => dialog.showModal();
$('#switch-user').onclick = () => { if (presenceInterval) clearInterval(presenceInterval); presenceStream?.getTracks().forEach(track => track.stop()); presenceStream=undefined; notifications?.close(); notifications=undefined; profile=null; identityDialog.showModal(); };
$('#recognize-login').onclick = () => { if (!jwt) { $('#auth-error').textContent = 'Sign in first.'; return; } identityDialog.close(); openFaceCamera('login'); };
$('#create-profile').onclick = () => { if (!jwt) { $('#auth-error').textContent = 'Sign in first.'; return; } identityDialog.close(); dialog.showModal(); };
form.addEventListener('submit', async (event) => {
  event.preventDefault(); if (event.submitter?.value === 'cancel') return;
  const values = new FormData(form);
  try {
    profile = await api('/profiles', { displayName: values.get('displayName'), personality: values.get('personality'), interests: values.get('interests'), timezone: Intl.DateTimeFormat().resolvedOptions().timeZone, location: values.get('location') });
    for (const [type, field] of [['CALENDAR_SYNC', 'consent-calendar'], ['NEWS', 'consent-news'], ['SMART_HOME', 'consent-smart-home']]) if (values.get(field) === 'on') await responseJson(await fetch(`/api/profiles/${profile.id}/consents/${type}`, { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ enabled: true }) }));
    if (values.get('proactiveEnabled') === 'on') profile = await responseJson(await fetch(`/api/profiles/${profile.id}/settings`, { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ proactiveEnabled: true }) }));
    updateProfile(); loadMood(); openNotifications(); startPresence(); dialog.close();
  } catch (error) { alert(error.message); }
});
$('#composer').addEventListener('submit', async (event) => {
  event.preventDefault(); const input = $('#message'); const text = input.value.trim(); if (!text) return; if (!profile) return dialog.showModal();
  bubble(text, 'user'); input.value = ''; const pending = bubble('Thinking…', 'companion');
  try { const response = await fetch(`/api/chat/stream?profileId=${encodeURIComponent(profile.id)}&message=${encodeURIComponent(text)}`, { headers: routedHeaders() }); if (!response.ok) throw new Error('Streaming failed'); const reader = response.body.getReader(); const decoder = new TextDecoder(); let sseBuffer = ''; let full = ''; const chunker = new SentenceChunker(queueSentence); const llmStarted = performance.now(); while (true) { const { value, done } = await reader.read(); if (done) break; sseBuffer += decoder.decode(value, { stream: true }); const lines=sseBuffer.split('\n'); sseBuffer=lines.pop(); for (const line of lines) if (line.startsWith('data: ')) { const event = JSON.parse(line.slice(6)); if (event.token) { if (!full) { avatar.setExpression('SPEAKING'); } full += event.token; pending.textContent = full; chunker.push(event.token); if (!latency['LLM first token']) { latency['LLM first token'] = performance.now()-llmStarted; showLatency(); } } if (event.done) { avatar.setExpression('NEUTRAL'); if (event.fullMessage.includes('?')) avatar.tilt(); pending.textContent = event.fullMessage; chunker.finish(); loadMood(); } } } } catch (error) { pending.textContent = `I’m having trouble connecting: ${error.message}`; }
});
async function openFaceCamera(purpose) {
  cameraPurpose = purpose; const modal = $('#camera-dialog'); const video = $('#camera');
  $('#camera-mode').textContent = purpose === 'enroll' ? 'FACE ENROLLMENT' : purpose === 'login' ? 'FACE LOGIN' : 'FACE RECOGNITION';
  $('#camera-status').textContent = 'Keep your face centered in the live preview, then select Capture face.';
  $('#camera-capture').textContent = purpose === 'enroll' ? 'Save 3 templates' : purpose === 'login' ? 'Sign in with face' : 'Recognize me';
  try { cameraStream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'user' } }); video.srcObject = cameraStream; await new Promise((resolve) => { video.onloadeddata = resolve; }); modal.showModal(); } catch (error) { alert(`Camera unavailable: ${error.message}`); }
}
function closeFaceCamera() { if (cameraStream) cameraStream.getTracks().forEach((track) => track.stop()); cameraStream = undefined; $('#camera').srcObject = null; $('#camera-dialog').close(); }
async function captureFaceTemplates(samples) {
  const video = $('#camera'); const templates = [];
  for (let sample = 0; sample < samples; sample += 1) {
    $('#camera-status').textContent = `Capturing template ${sample + 1} of ${samples}. Keep still…`;
    let crop = { x: 0, y: 0, width: video.videoWidth, height: video.videoHeight };
    if ('FaceDetector' in window) { const face = (await new FaceDetector({ fastMode: false, maxDetectedFaces: 1 }).detect(video))[0]; if (!face) throw new Error('No face detected. Stay in the center of the preview and try again.'); crop = face.boundingBox; }
    const canvas = $('#canvas'); const context = canvas.getContext('2d', { willReadFrequently: true }); canvas.width = 32; canvas.height = 32;
    context.drawImage(video, crop.x, crop.y, crop.width, crop.height, 0, 0, 32, 32);
    const pixels = context.getImageData(0, 0, 32, 32).data; const shades = [];
    for (let index = 0; index < pixels.length; index += 4) shades.push((pixels[index] * 0.299) + (pixels[index + 1] * 0.587) + (pixels[index + 2] * 0.114));
    const mean = shades.reduce((sum, shade) => sum + shade, 0) / shades.length; templates.push(shades.map((shade) => shade >= mean ? '1' : '0').join(''));
    if (sample + 1 < samples) await new Promise((resolve) => setTimeout(resolve, 350));
  }
  return templates.join('|');
}
$('#camera-cancel').onclick = closeFaceCamera;
$('#camera-dialog').addEventListener('cancel', (event) => { event.preventDefault(); closeFaceCamera(); });
$('#face').onclick = () => { if (!profile) return dialog.showModal(); openFaceCamera('enroll'); };
$('#verify').onclick = () => openFaceCamera(profile ? 'verify' : 'login');
$('#camera-capture').onclick = async () => {
  const capture = $('#camera-capture'); capture.disabled = true;
  try {
    const descriptor = await captureFaceTemplates(cameraPurpose === 'enroll' ? 3 : 2);
    if (cameraPurpose === 'enroll') { profile = await api(`/profiles/${profile.id}/face`, { descriptor }); updateProfile(); $('#camera-status').textContent = 'Enrollment complete.'; }
    else if (cameraPurpose === 'login') { const result = await api('/profiles/recognize', { descriptor }); if (!result.recognized) { $('#camera-status').textContent = 'No enrolled profile matched. Try again or create a new profile.'; capture.disabled = false; return; } profile = result.profile; updateProfile(); openNotifications(); $('#camera-status').textContent = `Welcome back, ${profile.displayName}! Similarity: ${result.confidence}%`; }
    else { const result = await api(`/profiles/${profile.id}/face/verify`, { descriptor }); $('#camera-status').textContent = result.recognized ? `Welcome back! Face similarity: ${result.confidence}%` : `No match (${result.confidence}%). Keep centered and capture again.`; if (!result.recognized) { capture.disabled = false; return; } }
    setTimeout(closeFaceCamera, 900);
  } catch (error) { $('#camera-status').textContent = error.message; capture.disabled = false; }
};
$('#offline-toggle input').onchange = (event) => { simulateOffline = event.target.checked; updateSystemStatus(); };
updateSystemStatus(); setInterval(updateSystemStatus, 10000);
updateProfile(); openNotifications(); if (!profile) identityDialog.showModal();
