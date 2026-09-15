const $ = (selector) => document.querySelector(selector);
const dialog = $('#profile-dialog');
const form = $('#profile-form');
const messages = $('#messages');
const identityDialog = $('#identity-dialog');
// Do not restore the prior browser user: every fresh page load requires a face login or profile creation.
let profile = null;
let recorder;
let recording = false;
let cameraStream;
let cameraPurpose;

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
function bubble(text, who) { const node = document.createElement('article'); node.className = `bubble ${who}`; node.textContent = text; messages.append(node); messages.scrollTop = messages.scrollHeight; return node; }
async function speak(text) {
  try {
    const response = await fetch('/api/voice/speech', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ text }) });
    if (!response.ok) throw new Error();
    const audio = new Audio(URL.createObjectURL(await response.blob()));
    audio.onended = () => URL.revokeObjectURL(audio.src);
    await audio.play();
  } catch { /* Voice provider is optional; text remains available. */ }
}
$('#settings').onclick = () => dialog.showModal();
$('#switch-user').onclick = () => identityDialog.showModal();
$('#recognize-login').onclick = () => { identityDialog.close(); openFaceCamera('login'); };
$('#create-profile').onclick = () => { identityDialog.close(); dialog.showModal(); };
form.addEventListener('submit', async (event) => {
  event.preventDefault(); if (event.submitter?.value === 'cancel') return;
  const values = new FormData(form);
  try {
    profile = await api('/profiles', { displayName: values.get('displayName'), personality: values.get('personality'), interests: values.get('interests'), timezone: Intl.DateTimeFormat().resolvedOptions().timeZone, location: values.get('location') });
    localStorage.setItem('fin-profile', JSON.stringify(profile)); updateProfile(); dialog.close();
  } catch (error) { alert(error.message); }
});
$('#composer').addEventListener('submit', async (event) => {
  event.preventDefault(); const input = $('#message'); const text = input.value.trim(); if (!text) return; if (!profile) return dialog.showModal();
  bubble(text, 'user'); input.value = ''; const pending = bubble('Thinking…', 'companion');
  try { const reply = await api('/chat', { profileId: profile.id, message: text }); pending.textContent = reply.message; speak(reply.message); } catch (error) { pending.textContent = `I’m having trouble connecting: ${error.message}`; }
});
$('#mic').onclick = async () => {
  if (recording) { recorder.stop(); return; }
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true }); const chunks = [];
    recorder = new MediaRecorder(stream); recorder.ondataavailable = (event) => chunks.push(event.data);
    recorder.onstop = async () => {
      stream.getTracks().forEach((track) => track.stop()); recording = false; $('#mic').textContent = '◉';
      const body = new FormData(); body.append('audio', new Blob(chunks, { type: recorder.mimeType }), 'voice.webm');
      try { const result = await responseJson(await fetch('/api/voice/transcriptions', { method: 'POST', body })); $('#message').value = result.text; } catch (error) { alert(`Transcription unavailable: ${error.message}`); }
    };
    recorder.start(); recording = true; $('#mic').textContent = '■';
  } catch (error) { alert(`Microphone unavailable: ${error.message}`); }
};
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
    if (cameraPurpose === 'enroll') { profile = await api(`/profiles/${profile.id}/face`, { descriptor }); localStorage.setItem('fin-profile', JSON.stringify(profile)); updateProfile(); $('#camera-status').textContent = 'Enrollment complete.'; }
    else if (cameraPurpose === 'login') { const result = await api('/profiles/recognize', { descriptor }); if (!result.recognized) { $('#camera-status').textContent = 'No enrolled profile matched. Try again or create a new profile.'; capture.disabled = false; return; } profile = result.profile; localStorage.setItem('fin-profile', JSON.stringify(profile)); updateProfile(); $('#camera-status').textContent = `Welcome back, ${profile.displayName}! Similarity: ${result.confidence}%`; }
    else { const result = await api(`/profiles/${profile.id}/face/verify`, { descriptor }); $('#camera-status').textContent = result.recognized ? `Welcome back! Face similarity: ${result.confidence}%` : `No match (${result.confidence}%). Keep centered and capture again.`; if (!result.recognized) { capture.disabled = false; return; } }
    setTimeout(closeFaceCamera, 900);
  } catch (error) { $('#camera-status').textContent = error.message; capture.disabled = false; }
};
updateProfile(); if (!profile) identityDialog.showModal();
