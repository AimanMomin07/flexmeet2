import { Room, RoomEvent, Track } from "https://cdn.jsdelivr.net/npm/livekit-client@2.25.0/+esm";

const API_BASE = (localStorage.getItem("flexmeetApiBase") || window.FLEXMEET_API_BASE || `${location.origin}/api`).replace(/\/+$/, "");
const ID_STORAGE = "flexmeetParticipantIdentity";
const room = new Room({ adaptiveStream: true, dynacast: true });
let activeMeeting = null;
let localIdentity = localStorage.getItem(ID_STORAGE) || crypto.randomUUID();
localStorage.setItem(ID_STORAGE, localIdentity);
let isHost = false;
let handQueue = [];
let toastTimer;
let authMode = "login";
let pendingCreate = false;

const $ = (id) => document.getElementById(id);
const homeScreen = $("home-screen");
const meetingScreen = $("meeting-screen");
const videoGrid = $("video-grid");

function showToast(message) {
  const element = $("toast");
  element.textContent = message;
  element.classList.add("show");
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => element.classList.remove("show"), 3600);
}

function setConnectionStatus(connected, label) {
  const status = $("connection-status");
  status.innerHTML = `<i></i>${label}`;
  status.classList.toggle("online", connected);
}

async function api(path, options = {}) {
  const token = localStorage.getItem("flexmeetAuthToken");
  const headers = { "Content-Type": "application/json", ...(token ? { Authorization: "Bearer " + token } : {}), ...(options.headers || {}) };
  const response = await fetch(`${API_BASE}${path}`, { ...options, headers });
  const data = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(data.message || `Request failed (${response.status})`);
  return data;
}

function setScreen(screen) {
  homeScreen.hidden = screen !== "home";
  meetingScreen.hidden = screen !== "meeting";
}

function parseRoomCode(value) {
  const text = (value || "").trim();
  if (!text) return "";
  try {
    const url = new URL(text);
    const parts = url.pathname.split("/").filter(Boolean);
    return (parts.at(-1) || "").toUpperCase();
  } catch (_) {
    return text.toUpperCase();
  }
}

function goToJoin() {
  $("join-card").hidden = false;
  $("join-code").focus();
  $("join-card").scrollIntoView({ behavior: "smooth", block: "center" });
}

function makeTile(identity, name, isLocal = false, host = false) {
  const id = `tile-${identity.replace(/[^a-zA-Z0-9_-]/g, "-")}`;
  let tile = document.getElementById(id);
  if (tile) return tile;
  tile = document.createElement("article");
  tile.id = id;
  tile.className = `video-tile${isLocal ? " local" : ""}`;
  tile.dataset.identity = identity;
  const placeholder = document.createElement("div");
  placeholder.className = "tile-placeholder";
  placeholder.textContent = (name || "G").trim().charAt(0).toUpperCase();
  const label = document.createElement("div");
  label.className = "tile-label";
  label.append(document.createTextNode(name || "Guest"));
  if (host) {
    const badge = document.createElement("span");
    badge.className = "host-badge";
    badge.textContent = "HOST";
    label.append(badge);
  }
  const video = document.createElement("video");
  video.autoplay = true;
  video.playsInline = true;
  video.muted = isLocal;
  const mic = document.createElement("span");
  mic.className = "tile-mic";
  mic.setAttribute("aria-hidden", "true");
  mic.textContent = "🎙";
  tile.append(placeholder, video, label, mic);
  videoGrid.append(tile);
  $("empty-stage").hidden = true;
  return tile;
}

function removeTile(identity) {
  document.getElementById(`tile-${identity.replace(/[^a-zA-Z0-9_-]/g, "-")}`)?.remove();
  if (videoGrid.querySelectorAll(".video-tile").length === 0) $("empty-stage").hidden = false;
}

function attachTrack(track, participant) {
  const isLocal = participant === room.localParticipant;
  const name = participant.name || participant.identity;
  const tile = makeTile(participant.identity, name, isLocal, participant.identity === activeMeeting?.hostIdentity);
  if (track.kind === Track.Kind.Video) {
    const video = tile.querySelector("video");
    track.attach(video);
    tile.querySelector(".tile-placeholder").hidden = true;
  } else if (track.kind === Track.Kind.Audio) {
    const audio = document.createElement("audio");
    audio.autoplay = true;
    audio.playsInline = true;
    tile.append(track.attach(audio));
  }
  updateParticipantCount();
}

function renderExistingTracks() {
  const local = room.localParticipant;
  for (const publication of local.trackPublications.values()) {
    if (publication.track) attachTrack(publication.track, local);
  }
  for (const participant of room.remoteParticipants.values()) {
    for (const publication of participant.trackPublications.values()) {
      if (publication.track) attachTrack(publication.track, participant);
    }
  }
}

function updateParticipantCount() {
  const count = 1 + room.remoteParticipants.size;
  $("people-count").textContent = String(count);
  for (const tile of videoGrid.querySelectorAll(".video-tile")) {
    const participant = tile.dataset.identity === room.localParticipant.identity
      ? room.localParticipant
      : room.remoteParticipants.get(tile.dataset.identity);
    const mic = tile.querySelector(".tile-mic");
    if (participant && mic) mic.textContent = participant.isMicrophoneEnabled ? "🎙" : "🔇";
  }
}

function renderPeople() {
  const list = $("participant-list");
  list.replaceChildren();
  const local = room.localParticipant;
  const participants = [{ participant: local, local: true }, ...Array.from(room.remoteParticipants.values()).map(p => ({ participant: p, local: false }))];
  $("people-count").textContent = String(participants.length);

  participants.forEach(({ participant, local: isLocal }) => {
    const row = document.createElement("div");
    row.className = "participant-row";
    const avatar = document.createElement("span");
    avatar.className = "participant-avatar";
    avatar.textContent = (participant.name || "G").trim().charAt(0).toUpperCase();
    const meta = document.createElement("span");
    meta.className = "participant-meta";
    const name = document.createElement("b");
    name.textContent = participant.name || participant.identity;
    const role = document.createElement("small");
    role.textContent = participant.identity === activeMeeting?.hostIdentity ? "Host" : (isLocal ? "You" : "Guest");
    meta.append(name, role);
    row.append(avatar, meta);

    if (isHost && !isLocal) {
      const tools = document.createElement("span");
      tools.className = "participant-tools";
      const audio = document.createElement("button");
      audio.title = participant.isMicrophoneEnabled ? "Mute microphone" : "Allow microphone";
      audio.textContent = participant.isMicrophoneEnabled ? "🎙" : "🔇";
      audio.addEventListener("click", () => setGuestPermission(participant.identity, !participant.isMicrophoneEnabled, participant.isCameraEnabled));
      const video = document.createElement("button");
      video.title = participant.isCameraEnabled ? "Turn off camera" : "Allow camera";
      video.textContent = participant.isCameraEnabled ? "▣" : "◻";
      video.addEventListener("click", () => setGuestPermission(participant.identity, participant.isMicrophoneEnabled, !participant.isCameraEnabled));
      const kick = document.createElement("button");
      kick.title = "Remove participant";
      kick.textContent = "×";
      kick.addEventListener("click", () => kickGuest(participant.identity));
      tools.append(audio, video, kick);
      row.append(tools);
    }
    list.append(row);
  });
}

function updateHostControls() {
  document.querySelectorAll(".admin-only").forEach(el => { el.hidden = !isHost; });
  $("room-mode").value = activeMeeting?.mode || "OPEN";
}

async function joinRoom(code, requestedName = null) {
  setConnectionStatus(false, "Connecting…");
  const name = requestedName || localStorage.getItem("flexmeetDisplayName") || "Guest";
  const session = await api(`/rooms/${encodeURIComponent(code)}/join`, {
    method: "POST",
    body: JSON.stringify({ participantName: name, identity: localIdentity })
  });
  activeMeeting = {
    room: session.room,
    title: session.title || "FlexMeet room",
    mode: session.mode || "OPEN",
    hostIdentity: session.hostIdentity || (session.isHost ? session.identity : null),
    identity: session.identity,
    isHost: Boolean(session.isHost)
  };
  isHost = activeMeeting.isHost;
  localIdentity = session.identity;
  localStorage.setItem(ID_STORAGE, localIdentity);
  await room.connect(session.url, session.token);
  setScreen("meeting");
  $("meeting-title-display").textContent = activeMeeting.title;
  $("meeting-room-code").textContent = `ROOM · ${activeMeeting.room}`;
  updateHostControls();
  setupRoomEvents();
  renderPeople();
  setConnectionStatus(true, "Connected");
  if (session.canPublishAudio) await room.localParticipant.setMicrophoneEnabled(true);
  else $("mic-toggle").disabled = true;
  if (session.canPublishVideo) await room.localParticipant.setCameraEnabled(true);
  else $("camera-toggle").disabled = true;
  renderExistingTracks();
  updateParticipantCount();
}

function setupRoomEvents() {
  room.on(RoomEvent.TrackSubscribed, (track, publication, participant) => {
    attachTrack(track, participant);
    renderPeople();
  });
  room.on(RoomEvent.TrackUnsubscribed, (track, publication, participant) => {
    track.detach().forEach(element => element.remove());
    if (track.kind === Track.Kind.Video && !participant.getTrackPublication(Track.Source.Camera)?.track) {
      const tile = document.getElementById(`tile-${participant.identity.replace(/[^a-zA-Z0-9_-]/g, "-")}`);
      if (tile) tile.querySelector(".tile-placeholder").hidden = false;
    }
    renderPeople();
  });
  room.on(RoomEvent.ParticipantConnected, () => { renderPeople(); updateParticipantCount(); });
  room.on(RoomEvent.ParticipantDisconnected, participant => {
    removeTile(participant.identity);
    renderPeople();
    updateParticipantCount();
  });
  room.on(RoomEvent.TrackMuted, () => renderPeople());
  room.on(RoomEvent.TrackUnmuted, () => renderPeople());
  room.on(RoomEvent.ParticipantPermissionsChanged, () => {
    const permissions = room.localParticipant.permissions;
    $("mic-toggle").disabled = !permissions?.canPublish;
    $("camera-toggle").disabled = !permissions?.canPublish;
    renderPeople();
  });
  room.on(RoomEvent.DataReceived, (payload, participant, kind, topic) => {
    let message;
    try { message = JSON.parse(new TextDecoder().decode(payload)); } catch (_) { return; }
    if (topic === "flexmeet.chat" && message.type === "chat") addChatMessage(message.name, message.text);
    if (topic === "flexmeet.stage" && message.type === "hand") updateHand(message.identity, message.name, message.raised);
  });
  room.on(RoomEvent.Disconnected, () => {
    setConnectionStatus(false, "Disconnected");
    showToast("Your connection ended.");
  });
}

async function publishControl(topic, message) {
  if (!room.localParticipant) return;
  await room.localParticipant.publishData(
    new TextEncoder().encode(JSON.stringify(message)),
    { reliable: true, topic }
  );
}

function updateHand(identity, name, raised) {
  handQueue = handQueue.filter(item => item.identity !== identity);
  if (raised) handQueue.push({ identity, name });
  renderHandQueue();
}

function renderHandQueue() {
  const queue = $("hand-queue");
  queue.replaceChildren();
  if (!handQueue.length) {
    const empty = document.createElement("span");
    empty.className = "muted";
    empty.textContent = "No hands raised";
    queue.append(empty);
    return;
  }
  handQueue.forEach(item => {
    const row = document.createElement("div");
    row.className = "queue-person";
    const label = document.createElement("span");
    label.textContent = `✋ ${item.name}`;
    row.append(label);
    if (isHost) {
      const stage = document.createElement("button");
      stage.textContent = "Invite to stage";
      stage.addEventListener("click", async () => {
        await setGuestPermission(item.identity, true, true);
        await publishControl("flexmeet.stage", { type: "hand", identity: item.identity, name: item.name, raised: false });
        showToast(`${item.name} can now use mic and camera.`);
      });
      row.append(stage);
    }
    queue.append(row);
  });
}

function addChatMessage(name, text) {
  const container = $("chat-messages");
  const bubble = document.createElement("div");
  bubble.className = "chat-bubble";
  const author = document.createElement("b");
  author.textContent = name || "Guest";
  const content = document.createElement("p");
  content.textContent = text;
  bubble.append(author, content);
  container.append(bubble);
  container.scrollTop = container.scrollHeight;
}

async function createRoom() {
  const title = $("room-name").value.trim() || "FlexMeet room";
  const mode = $("room-start-mode").value;
  const result = await api("/rooms", {
    method: "POST",
    body: JSON.stringify({ title, mode })
  });
  localStorage.setItem("flexmeetDisplayName", "Host");
  history.pushState({}, "", `/join/${result.room}`);
  await joinRoom(result.room, "Host");
}

async function setGuestPermission(identity, audio, video) {
  try {
    await api(`/rooms/${encodeURIComponent(activeMeeting.room)}/participants/${encodeURIComponent(identity)}/permissions`, {
      method: "POST",
      body: JSON.stringify({ audio, video })
    });
    showToast("Participant permissions updated.");
  } catch (error) { showToast(error.message); }
}

async function kickGuest(identity) {
  if (!confirm("Remove this person from the room?")) return;
  try {
    await api(`/rooms/${encodeURIComponent(activeMeeting.room)}/participants/${encodeURIComponent(identity)}/kick`, {
      method: "POST",
    });
    showToast("Participant removed.");
  } catch (error) { showToast(error.message); }
}

async function leaveRoom() {
  await room.disconnect();
  for (const tile of videoGrid.querySelectorAll(".video-tile")) tile.remove();
  $("empty-stage").hidden = false;
  setScreen("home");
  activeMeeting = null;
  isHost = false;
  history.pushState({}, "", "/");
  setConnectionStatus(false, "Ready when you are");
}

async function copyInvite() {
  const invite = `${location.origin}/join/${activeMeeting.room}`;
  try {
    await navigator.clipboard.writeText(invite);
    showToast("Invite link copied. Send it anywhere.");
  } catch (_) {
    window.prompt("Copy this meeting link", invite);
  }
}

function setPressed(button, pressed) {
  button.setAttribute("aria-pressed", String(pressed));
}

async function toggleMic() {
  const enabled = !room.localParticipant.isMicrophoneEnabled;
  await room.localParticipant.setMicrophoneEnabled(enabled);
  setPressed($("mic-toggle"), enabled);
}

async function toggleCamera() {
  const enabled = !room.localParticipant.isCameraEnabled;
  await room.localParticipant.setCameraEnabled(enabled);
  setPressed($("camera-toggle"), enabled);
}

async function toggleScreenShare() {
  if (!navigator.mediaDevices?.getDisplayMedia) {
    showToast("Screen sharing is not available in this browser.");
    return;
  }
  const publication = [...room.localParticipant.trackPublications.values()]
    .find(item => item.source === Track.Source.ScreenShare);
  await room.localParticipant.setScreenShareEnabled(!publication);
}

async function sendChat(event) {
  event.preventDefault();
  const input = $("chat-input");
  const text = input.value.trim();
  if (!text) return;
  const message = { type: "chat", name: room.localParticipant.name || "Guest", text, at: Date.now() };
  await publishControl("flexmeet.chat", message);
  addChatMessage(message.name, text);
  input.value = "";
}

async function raiseHand() {
  const raised = $("raise-hand").getAttribute("aria-pressed") !== "true";
  setPressed($("raise-hand"), raised);
  await publishControl("flexmeet.stage", {
    type: "hand",
    identity: room.localParticipant.identity,
    name: room.localParticipant.name || "Guest",
    raised
  });
  showToast(raised ? "You’re in the stage queue." : "Hand lowered.");
}

async function updateRoomMode() {
  try {
    const mode = $("room-mode").value;
    await api(`/rooms/${encodeURIComponent(activeMeeting.room)}/mode`, {
      method: "POST",
      body: JSON.stringify({ mode })
    });
    activeMeeting.mode = mode;
    showToast(mode === "HOST_ONLY" ? "Host-led stage is on." : "Everyone can use mic and camera.");
  } catch (error) { showToast(error.message); }
}

async function muteEveryone() {
  try {
    await api(`/rooms/${encodeURIComponent(activeMeeting.room)}/mute-all`, {
      method: "POST",
    });
    showToast("Guests are muted. You can invite people back to the stage.");
  } catch (error) { showToast(error.message); }
}

function changeTab(tab) {
  document.querySelectorAll(".tab").forEach(button => button.classList.toggle("active", button.dataset.tab === tab));
  $("people-view").hidden = tab !== "people";
  $("chat-view").hidden = tab !== "chat";
}

function showApiError(error) {
  console.error(error);
  if (error.message.includes("Failed to fetch")) {
    showToast("Could not reach the FlexMeet API. Open connection settings and enter your deployed API URL.");
  } else {
    showToast(error.message);
  }
}

function openSettings() {
  $("api-address").value = API_BASE;
  $("settings-dialog").showModal();
}

async function loadFromLink() {
  const parts = location.pathname.split("/").filter(Boolean);
  if (parts[0] !== "join" || !parts[1]) return;
  const roomCode = parseRoomCode(parts[1]);
  const savedName = localStorage.getItem("flexmeetDisplayName") || "";
  const name = window.prompt("What name should people see?", savedName || "Guest");
  if (name === null) return;
  localStorage.setItem("flexmeetDisplayName", name.trim() || "Guest");
  try {
    await joinRoom(roomCode, name.trim() || "Guest");
  } catch (error) {
    showApiError(error);
    setScreen("home");
  }
}

$("create-room").addEventListener("click", () => {
  if (!localStorage.getItem("flexmeetAuthToken")) {
    pendingCreate = true;
    openAuth();
  } else {
    $("create-dialog").showModal();
  }
});
$("create-form").addEventListener("submit", async event => {
  event.preventDefault();
  $("create-dialog").close();
  try { await createRoom(); } catch (error) { showApiError(error); }
});
$("join-open").addEventListener("click", goToJoin);
$("join-form").addEventListener("submit", async event => {
  event.preventDefault();
  const code = parseRoomCode($("join-code").value);
  if (!code) return showToast("Enter a meeting code or invite link.");
  try {
    await joinRoom(code);
  } catch (error) { showApiError(error); }
});
$("mic-toggle").addEventListener("click", () => toggleMic().catch(showApiError));
$("camera-toggle").addEventListener("click", () => toggleCamera().catch(showApiError));
$("screen-share").addEventListener("click", () => toggleScreenShare().catch(showApiError));
$("raise-hand").addEventListener("click", () => raiseHand().catch(showApiError));
$("share-invite").addEventListener("click", () => copyInvite().catch(showApiError));
$("leave-room").addEventListener("click", () => leaveRoom().catch(showApiError));
$("room-mode").addEventListener("change", () => updateRoomMode().catch(showApiError));
$("admin-mute-all").addEventListener("click", () => muteEveryone().catch(showApiError));
$("chat-form").addEventListener("submit", event => sendChat(event).catch(showApiError));
$("settings-open").addEventListener("click", openSettings);
$("account-open").addEventListener("click", () => {
  if (localStorage.getItem("flexmeetAuthToken")) {
    if (confirm("Sign out of FlexMeet?")) {
      localStorage.removeItem("flexmeetAuthToken");
      localStorage.removeItem("flexmeetUser");
      updateAccountButton();
      showToast("You are signed out.");
    }
  } else openAuth();
});
$("auth-form").addEventListener("submit", event => authenticate(event).catch(showApiError));
$("auth-toggle").addEventListener("click", toggleAuthMode);
$("settings-form").addEventListener("submit", event => {
  event.preventDefault();
  const value = $("api-address").value.trim().replace(/\/+$/, "");
  if (!/^https?:\/\//i.test(value)) return showToast("Enter a full API address beginning with https://.");
  localStorage.setItem("flexmeetApiBase", value);
  $("settings-dialog").close();
  showToast("API address saved. Reload the page to connect.");
});
document.querySelectorAll(".tab").forEach(button => button.addEventListener("click", () => changeTab(button.dataset.tab)));
window.addEventListener("popstate", () => { if (location.pathname === "/") setScreen("home"); else loadFromLink(); });
function updateAccountButton() {
  const user = JSON.parse(localStorage.getItem("flexmeetUser") || "null");
  $("account-open").textContent = user?.fullName ? user.fullName.split(" ")[0] : "Sign in";
}

function openAuth() {
  $("auth-message").textContent = "";
  $("auth-dialog").showModal();
}

function toggleAuthMode() {
  authMode = authMode === "login" ? "register" : "login";
  const registering = authMode === "register";
  $("auth-title").textContent = registering ? "Create your account" : "Welcome back";
  $("auth-submit").textContent = registering ? "Create account" : "Sign in";
  $("auth-toggle").textContent = registering ? "I already have an account" : "Create an account";
  $("auth-name-label").hidden = !registering;
  $("auth-name").hidden = !registering;
  $("auth-name").required = registering;
  $("auth-password").autocomplete = registering ? "new-password" : "current-password";
}

async function authenticate(event) {
  event.preventDefault();
  const registering = authMode === "register";
  const data = await api(registering ? "/auth/register" : "/auth/login", {
    method: "POST",
    headers: { Authorization: "" },
    body: JSON.stringify({
      email: $("auth-email").value.trim(),
      password: $("auth-password").value,
      fullName: registering ? $("auth-name").value.trim() : null,
      isRegistration: registering
    })
  });
  const result = data.data;
  if (!data.success || !result?.success || !result.token) {
    $("auth-message").textContent = result?.message || data.message || "Could not sign in.";
    return;
  }
  localStorage.setItem("flexmeetAuthToken", result.token);
  localStorage.setItem("flexmeetUser", JSON.stringify(result.user));
  localStorage.setItem("flexmeetDisplayName", result.user?.fullName || "Host");
  $("auth-dialog").close();
  updateAccountButton();
  showToast(registering ? "Your account is ready." : "Welcome back.");
  if (pendingCreate) {
    pendingCreate = false;
    $("create-dialog").showModal();
  }
}

updateAccountButton();
loadFromLink().catch(showApiError);
