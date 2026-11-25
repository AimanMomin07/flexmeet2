package com.flexmeet.activities

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.GridLayout
import android.widget.HorizontalScrollView
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.flexmeet.firebase.FirebaseManager
import com.google.android.material.card.MaterialCardView
import io.livekit.android.LiveKit
import io.livekit.android.events.RoomEvent
import io.livekit.android.renderer.SurfaceViewRenderer
import io.livekit.android.room.Room
import io.livekit.android.room.track.VideoTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import io.livekit.android.events.collect
import kotlinx.coroutines.withContext
import org.json.JSONObject

class LiveKitCallActivity : AppCompatActivity() {
    private lateinit var roomCode: String
    private lateinit var title: String
    private lateinit var status: TextView
    private lateinit var videoGrid: GridLayout
    private val renderers = mutableMapOf<String, SurfaceViewRenderer>()
    private val tiles = mutableMapOf<String, MaterialCardView>()
    private lateinit var micButton: Button
    private lateinit var cameraButton: Button
    private lateinit var manageButton: Button
    private lateinit var room: Room
    private var micOn = false
    private var cameraOn = false
    private var isHost = false
    private var canAudio = true
    private var canVideo = true
    private var joining = false
    private var connected = false
    private var pendingJoin: JSONObject? = null

    private val permissionRequest = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        if (grants[Manifest.permission.RECORD_AUDIO] == false) canAudio = false
        if (grants[Manifest.permission.CAMERA] == false) canVideo = false
        pendingJoin?.let { connectToRoom(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        roomCode = intent.getStringExtra(EXTRA_ROOM).orEmpty()
        title = intent.getStringExtra(EXTRA_TITLE) ?: "FlexMeet"
        if (roomCode.isBlank()) { finish(); return }
        isHost = intent.getBooleanExtra(EXTRA_HOST, false)
        buildUi()
        requestJoinToken()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.rgb(13, 19, 34)); setPadding(18, 24, 18, 18) }
        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val names = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0, -2, 1f) }
        names.addView(TextView(this).apply { text = title; textSize = 21f; setTextColor(Color.WHITE); setTypeface(null, android.graphics.Typeface.BOLD) })
        status = TextView(this).apply { text = "Getting secure meeting access…"; textSize = 14f; setTextColor(Color.LTGRAY); setPadding(0, 6, 0, 0) }
        names.addView(status)
        header.addView(names)
        header.addView(Button(this).apply { text = "Share"; setOnClickListener { shareInvite() } })
        root.addView(header)
        val card = MaterialCardView(this).apply { radius = 28f; cardElevation = 0f; setCardBackgroundColor(Color.rgb(27, 36, 56)); layoutParams = LinearLayout.LayoutParams(-1, 0, 1f).apply { topMargin = 22; bottomMargin = 22 } }
        val scroll = ScrollView(this)
        videoGrid = GridLayout(this).apply { columnCount = 2; setPadding(6, 6, 6, 6) }
        scroll.addView(videoGrid, ViewGroup.LayoutParams(-1, -2))
        card.addView(scroll, LinearLayout.LayoutParams(-1, -1))
        root.addView(card)
        val controls = LinearLayout(this).apply { gravity = Gravity.CENTER; orientation = LinearLayout.HORIZONTAL }
        micButton = Button(this).apply { text = "Mic"; setOnClickListener { toggleMic() } }
        cameraButton = Button(this).apply { text = "Camera"; setOnClickListener { toggleCamera() } }
        manageButton = Button(this).apply { text = "Manage"; visibility = android.view.View.GONE; setOnClickListener { manageParticipants() } }
        controls.addView(micButton)
        controls.addView(cameraButton)
        controls.addView(manageButton)
        controls.addView(Button(this).apply { text = "Leave"; setOnClickListener { finish() } })
        val controlScroll = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false }
        controlScroll.addView(controls, ViewGroup.LayoutParams(-2, -2))
        root.addView(controlScroll)
        setContentView(root)
        room = LiveKit.create(applicationContext)
    }

    private fun requestJoinToken() {
        if (joining || connected) return
        joining = true
        lifecycleScope.launch {
            try {
                val profile = FirebaseManager.getInstance().getCurrentRealtimeUser()
                val isSignedIn = FirebaseManager.getInstance().isUserLoggedIn()
                val body = JSONObject().put("participantName", profile?.displayName ?: "Guest")
                val result = withContext(Dispatchers.IO) {
                    MeetingsApi.post(this@LiveKitCallActivity, "/rooms/" + roomCode + "/join", body, isSignedIn)
                }
                isHost = result.optBoolean("isHost", false)
                canAudio = result.optBoolean("canPublishAudio", true)
                canVideo = result.optBoolean("canPublishVideo", true)
                manageButton.visibility = if (isHost) android.view.View.VISIBLE else android.view.View.GONE
                pendingJoin = result
                status.text = "Allow the media you want to use"
                val missing = mutableListOf<String>()
                if (canAudio && ContextCompat.checkSelfPermission(this@LiveKitCallActivity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) missing.add(Manifest.permission.RECORD_AUDIO)
                if (canVideo && ContextCompat.checkSelfPermission(this@LiveKitCallActivity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) missing.add(Manifest.permission.CAMERA)
                if (missing.isEmpty()) connectToRoom(result) else permissionRequest.launch(missing.toTypedArray())
            } catch (error: Exception) {
                joining = false
                status.text = "Could not join meeting"
                Toast.makeText(this@LiveKitCallActivity, error.message ?: "Unable to join meeting", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun connectToRoom(join: JSONObject) {
        if (connected) return
        pendingJoin = null
        lifecycleScope.launch {
            try {
                status.text = "Connecting to " + roomCode + "…"
                launch {
                    room.events.collect { event ->
                        if (event is RoomEvent.TrackSubscribed && event.track is VideoTrack) {
                            val identity = event.participant.identity?.toString() ?: "guest"
                            val displayName = event.participant.name ?: identity
                            runOnUiThread {
                                attachVideo(identity, displayName, event.track as VideoTrack)
                                status.text = "Connected · " + identity
                            }
                        } else if (event is RoomEvent.ParticipantConnected) {
                            val identity = event.participant.identity?.toString() ?: "guest"
                            val displayName = event.participant.name ?: identity
                            runOnUiThread {
                                ensureParticipantTile(identity, displayName)
                                status.text = "Connected · " + identity + " joined"
                            }
                        } else if (event is RoomEvent.ParticipantDisconnected) {
                            val identity = event.participant.identity?.toString() ?: "guest"
                            runOnUiThread { removeParticipantTile(identity); status.text = "A participant left" }
                        }
                    }
                }
                room.connect(join.getString("url"), join.getString("token"))
                connected = true
                joining = false
                if (canAudio) { micOn = room.localParticipant.setMicrophoneEnabled(true); micButton.text = if (micOn) "Mute" else "Unmute" }
                if (canVideo) {
                    cameraOn = room.localParticipant.setCameraEnabled(true)
                    cameraButton.text = if (cameraOn) "Stop video" else "Start video"
                    val localTrack = room.localParticipant.videoTrackPublications.firstOrNull()?.second as? VideoTrack
                    if (localTrack != null) runOnUiThread { attachVideo("local", "You", localTrack) }
                }
                status.text = if (canAudio || canVideo) "Connected · share the invite to bring people in" else "Connected · host has restricted publishing"
            } catch (error: Exception) {
                joining = false
                status.text = "Could not connect"
                Toast.makeText(this@LiveKitCallActivity, error.message ?: "Unable to connect", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun ensureParticipantTile(identity: String, displayName: String) {
        if (tiles.containsKey(identity)) return
        val tile = MaterialCardView(this).apply {
            radius = 18f
            cardElevation = 0f
            setCardBackgroundColor(Color.rgb(30, 40, 60))
            strokeWidth = 1
            strokeColor = Color.rgb(58, 72, 98)
        }
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(TextView(this).apply {
            text = displayName
            setTextColor(Color.WHITE)
            textSize = 14f
            setPadding(12, 10, 12, 10)
            maxLines = 1
        })
        tile.addView(content)
        val cellWidth = resources.displayMetrics.widthPixels / 2 - dp(32)
        val params = GridLayout.LayoutParams(
            GridLayout.spec(GridLayout.UNDEFINED),
            GridLayout.spec(GridLayout.UNDEFINED)
        ).apply {
            width = cellWidth
            height = dp(220)
            setMargins(dp(4), dp(4), dp(4), dp(4))
        }
        videoGrid.addView(tile, params)
        tiles[identity] = tile
    }

    private fun attachVideo(identity: String, displayName: String, track: VideoTrack) {
        ensureParticipantTile(identity, displayName)
        var view = renderers[identity]
        if (view == null) {
            view = SurfaceViewRenderer(this)
            room.initVideoRenderer(view)
            val content = tiles[identity]?.getChildAt(0) as? LinearLayout ?: return
            content.addView(view, LinearLayout.LayoutParams(-1, 0, 1f))
            renderers[identity] = view
        }
        track.addRenderer(view)
    }

    private fun removeParticipantTile(identity: String) {
        tiles.remove(identity)?.let { videoGrid.removeView(it) }
        renderers.remove(identity)?.release()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun toggleMic() {
        if (!connected || !canAudio) return
        lifecycleScope.launch { micOn = room.localParticipant.setMicrophoneEnabled(!micOn); micButton.text = if (micOn) "Mute" else "Unmute" }
    }
    private fun toggleCamera() {
        if (!connected || !canVideo) return
        lifecycleScope.launch { cameraOn = room.localParticipant.setCameraEnabled(!cameraOn); cameraButton.text = if (cameraOn) "Stop video" else "Start video" }
    }

    private fun shareInvite() {
        val url = "https://flexmeet-aiman-3859.vercel.app/join/" + roomCode
        val share = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, "Join my FlexMeet meeting: " + url)
        startActivity(Intent.createChooser(share, "Share meeting invite"))
    }

    private fun manageParticipants() {
        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) { MeetingsApi.getArray(this@LiveKitCallActivity, "/rooms/" + roomCode + "/participants", true) }
                val participants = result
                val labels = mutableListOf<String>()
                val identities = mutableListOf<String>()
                for (i in 0 until participants.length()) {
                    val participant = participants.optJSONObject(i) ?: continue
                    val identity = participant.optString("identity")
                    if (identity.isBlank() || identity == FirebaseManager.getInstance().getCurrentUserId()) continue
                    identities.add(identity)
                    labels.add(participant.optString("name", identity))
                }
                if (labels.isEmpty()) { Toast.makeText(this@LiveKitCallActivity, "No other participants yet", Toast.LENGTH_SHORT).show(); return@launch }
                AlertDialog.Builder(this@LiveKitCallActivity).setTitle("Manage participant")
                    .setItems(labels.toTypedArray()) { _, which -> showParticipantActions(identities[which]) }.show()
            } catch (e: Exception) { Toast.makeText(this@LiveKitCallActivity, e.message, Toast.LENGTH_LONG).show() }
        }
    }

    private fun showParticipantActions(identity: String) {
        val actions = arrayOf("Allow mic and camera", "Allow mic only", "Stop mic and camera", "Remove from meeting")
        AlertDialog.Builder(this).setTitle(identity).setItems(actions) { _, which ->
            when (which) {
                0 -> changeParticipantPermission(identity, true, true)
                1 -> changeParticipantPermission(identity, true, false)
                2 -> changeParticipantPermission(identity, false, false)
                3 -> moderate("/participants/" + identity + "/kick", JSONObject())
            }
        }.show()
    }

    private fun changeParticipantPermission(identity: String, audio: Boolean, video: Boolean) {
        moderate("/participants/" + identity + "/permissions", JSONObject().put("audio", audio).put("video", video))
    }

    private fun moderate(path: String, body: JSONObject?) {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) { MeetingsApi.post(this@LiveKitCallActivity, "/rooms/" + roomCode + path, body, true) }
                Toast.makeText(this@LiveKitCallActivity, "Meeting updated", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) { Toast.makeText(this@LiveKitCallActivity, e.message, Toast.LENGTH_LONG).show() }
        }
    }

    override fun onDestroy() {
        if (::room.isInitialized) {
            if (isHost) FirebaseManager.getInstance().getFirebaseDatabase().getReference("public_rooms").child(roomCode).removeValue()
            lifecycleScope.launch { try { room.disconnect() } catch (_: Exception) {} }
            renderers.values.forEach { it.release() }
            renderers.clear()
            tiles.clear()
        }
        super.onDestroy()
    }

    companion object {
        const val EXTRA_ROOM = "room"
        const val EXTRA_TITLE = "title"
        const val EXTRA_HOST = "host"
    }
}