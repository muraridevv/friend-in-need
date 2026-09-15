const $ = (selector) => document.querySelector(selector);
const dialog = $('#profile-dialog');
const form = $('#profile-form');
const messages = $('#messages');
let profile = JSON.parse(localStorage.getItem('fin-profile') || 'null');
let recorder;
let recording = false;

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
async function faceDescriptor() {
  const stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'user' } }); const video = $('#camera'); video.srcObject = stream;
  await new Promise((resolve) => { video.onloadeddata = resolve; });
  let crop = { x: 0, y: 0, width: video.videoWidth, height: video.videoHeight };
  if ('FaceDetector' in window) { const face = (await new FaceDetector({ fastMode: false, maxDetectedFaces: 1 }).detect(video))[0]; if (!face) { stream.getTracks().forEach((track) => track.stop()); throw new Error('No face detected. Center your face and try again.'); } crop = face.boundingBox; }
  const canvas = $('#canvas'); const context = canvas.getContext('2d', { willReadFrequently: true }); canvas.width = 32; canvas.height = 32;
  context.drawImage(video, crop.x, crop.y, crop.width, crop.height, 0, 0, 32, 32); stream.getTracks().forEach((track) => track.stop());
  const pixels = context.getImageData(0, 0, 32, 32).data; const shades = []; for (let index = 0; index < pixels.length; index += 4) shades.push((pixels[index] * 0.299) + (pixels[index + 1] * 0.587) + (pixels[index + 2] * 0.114));
  const mean = shades.reduce((sum, shade) => sum + shade, 0) / shades.length; return shades.map((shade) => shade >= mean ? '1' : '0').join('');
}
$('#face').onclick = async () => { if (!profile) return dialog.showModal(); try { profile = await api(`/profiles/${profile.id}/face`, { descriptor: await faceDescriptor() }); localStorage.setItem('fin-profile', JSON.stringify(profile)); updateProfile(); alert('Face template enrolled. Use similar lighting for recognition.'); } catch (error) { alert(error.message); } };
$('#briefing').onclick = async () => { if (!profile) return dialog.showModal(); try { const response = await fetch(`/api/profiles/${profile.id}/briefing`); const briefing = await responseJson(response); const events = briefing.events.length ? briefing.events.map((event) => `${event.title} at ${new Date(event.startsAt).toLocaleString()}`).join('; ') : 'No events in the next seven days.'; const text = `${briefing.weather} ${events}`; bubble(text, 'companion'); speak(text); } catch (error) { alert(error.message); } };
$('#verify').onclick = async () => { if (!profile) return dialog.showModal(); try { const result = await api(`/profiles/${profile.id}/face/verify`, { descriptor: await faceDescriptor() }); alert(result.recognized ? 'Welcome back!' : 'I could not recognize you. Try enrolling again in similar lighting.'); } catch (error) { alert(error.message); } };
updateProfile(); if (!profile) dialog.showModal();
