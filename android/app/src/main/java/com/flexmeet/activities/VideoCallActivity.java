package com.flexmeet.activities;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import com.flexmeet.R;
import com.flexmeet.adapters.VideoParticipantAdapter;
import com.flexmeet.firebase.FirebaseManager;
import com.flexmeet.webRTC.SignalingClient;
import com.flexmeet.databinding.ActivityVideoCallBinding;
import com.flexmeet.fragments.NotesFragment;
import com.flexmeet.fragments.ChatFragment;
import com.flexmeet.fragments.ParticipantsFragment;
import com.flexmeet.services.ScreenRecordService;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import org.webrtc.*;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class VideoCallActivity extends AppCompatActivity {

    private static final String TAG = "VideoCallActivity";
    private static final int SCREEN_CAPTURE_REQUEST_CODE = 1001;
    
    private ActivityVideoCallBinding binding;
    private EglBase eglBase;
    private PeerConnectionFactory peerConnectionFactory;
    private VideoTrack localVideoTrack;
    private AudioTrack localAudioTrack;
    private VideoCapturer videoCapturer;
    private SurfaceViewRenderer localVideoView;
    private AudioManager audioManager;

    private String meetingId;
    private boolean isMuted = false;
    private boolean isVideoOn = true;
    private boolean isRecording = false;
    private boolean isCasting = false;
    private DatabaseReference participantsRef;
    private ValueEventListener participantsListener;
    private SignalingClient signalingClient;
    private VideoParticipantAdapter participantAdapter;
    private final Map<String, PeerConnection> peerConnections = new HashMap<>();
    private final Map<String, VideoTrack> remoteVideoTracks = new HashMap<>();
    private final Map<String, List<IceCandidate>> pendingIceCandidates = new HashMap<>();
    private final List<VideoParticipantAdapter.Participant> remoteParticipants = new ArrayList<>();

    private MediaProjectionManager mediaProjectionManager;
    private int captureType = 0; // 0: None, 1: Record, 2: Cast

    private Handler timerHandler = new Handler(Looper.getMainLooper());
    private long startTime = 0L;
    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            long millis = System.currentTimeMillis() - startTime;
            int seconds = (int) (millis / 1000);
            int minutes = seconds / 60;
            int hours = minutes / 60;
            seconds = seconds % 60;
            minutes = minutes % 60;
            binding.recordingTimerText.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds));
            timerHandler.postDelayed(this, 1000);
        }
    };

    // Drag variables
    private float dX, dY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVideoCallBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        setupSpeaker();

        localVideoView = binding.localVideoView;

        meetingId = getIntent().getStringExtra("meeting_id");
        if (meetingId == null) {
            meetingId = "FLEX-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase(Locale.ROOT);
        }

        initializeWebRTC();
        setupControls();
        setupDraggablePreview();
        initializeSignaling();
        if (!isFinishing()) listenForParticipants();
    }

    private void setupSpeaker() {
        if (audioManager != null) {
            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
            audioManager.setSpeakerphoneOn(true);
        }
    }

    private void initializeWebRTC() {
        eglBase = EglBase.create();

        PeerConnectionFactory.InitializationOptions initializationOptions =
                PeerConnectionFactory.InitializationOptions.builder(this)
                        .createInitializationOptions();
        PeerConnectionFactory.initialize(initializationOptions);

        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        
        DefaultVideoEncoderFactory defaultVideoEncoderFactory = new DefaultVideoEncoderFactory(
                eglBase.getEglBaseContext(), true, true);
        DefaultVideoDecoderFactory defaultVideoDecoderFactory = new DefaultVideoDecoderFactory(
                eglBase.getEglBaseContext());

        peerConnectionFactory = PeerConnectionFactory.builder()
                .setOptions(options)
                .setVideoEncoderFactory(defaultVideoEncoderFactory)
                .setVideoDecoderFactory(defaultVideoDecoderFactory)
                .createPeerConnectionFactory();

        localVideoView.init(eglBase.getEglBaseContext(), null);
        localVideoView.setEnableHardwareScaler(true);
        localVideoView.setMirror(true);

        startLocalStream();
    }

    private void startLocalStream() {
        videoCapturer = createVideoCapturer();
        if (videoCapturer == null) {
            Toast.makeText(this, "Failed to open camera", Toast.LENGTH_SHORT).show();
            return;
        }

        VideoSource videoSource = peerConnectionFactory.createVideoSource(false);
        SurfaceTextureHelper surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.getEglBaseContext());
        videoCapturer.initialize(surfaceTextureHelper, this, videoSource.getCapturerObserver());
        videoCapturer.startCapture(1280, 720, 30);

        localVideoTrack = peerConnectionFactory.createVideoTrack("101", videoSource);
        localVideoTrack.setEnabled(true);
        localVideoTrack.addSink(localVideoView);

        AudioSource audioSource = peerConnectionFactory.createAudioSource(new MediaConstraints());
        localAudioTrack = peerConnectionFactory.createAudioTrack("102", audioSource);
        localAudioTrack.setEnabled(true);
    }

    private VideoCapturer createVideoCapturer() {
        CameraEnumerator enumerator;
        if (Camera2Enumerator.isSupported(this)) {
            enumerator = new Camera2Enumerator(this);
        } else {
            enumerator = new Camera1Enumerator(false);
        }

        final String[] deviceNames = enumerator.getDeviceNames();

        for (String deviceName : deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                VideoCapturer capturer = enumerator.createCapturer(deviceName, null);
                if (capturer != null) return capturer;
            }
        }

        for (String deviceName : deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                VideoCapturer capturer = enumerator.createCapturer(deviceName, null);
                if (capturer != null) return capturer;
            }
        }

        return null;
    }

    private void initializeSignaling() {
        String selfId = FirebaseManager.getInstance().getCurrentUserId();
        if (selfId == null || selfId.trim().isEmpty()) {
            Toast.makeText(this, "Sign in before joining a meeting", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        participantAdapter = new VideoParticipantAdapter(eglBase.getEglBaseContext());
        binding.videoGridRecycler.setLayoutManager(new GridLayoutManager(this, 2));
        binding.videoGridRecycler.setAdapter(participantAdapter);
        signalingClient = new SignalingClient(meetingId, new SignalingClient.SignalingInterface() {
            @Override public void onRemoteUserJoined(String remoteId) {
                runOnUiThread(() -> {
                    if (selfId.compareTo(remoteId) > 0) createOffer(remoteId);
                });
            }
            @Override public void onRemoteUserLeft(String remoteId) {
                runOnUiThread(() -> removePeer(remoteId));
            }
            @Override public void onOfferReceived(String remoteId, SessionDescription sdp) {
                runOnUiThread(() -> acceptOffer(remoteId, sdp));
            }
            @Override public void onAnswerReceived(String remoteId, SessionDescription sdp) {
                PeerConnection peer = peerConnections.get(remoteId);
                if (peer != null) peer.setRemoteDescription(new SdpObserver() {
                    @Override public void onSetSuccess() { flushPendingIceCandidates(remoteId, peer); }
                    @Override public void onCreateSuccess(SessionDescription value) {}
                    @Override public void onCreateFailure(String error) { Log.e(TAG, error); }
                    @Override public void onSetFailure(String error) { Log.e(TAG, error); }
                }, sdp);
            }
            @Override public void onIceCandidateReceived(String remoteId, IceCandidate candidate) {
                PeerConnection peer = peerConnections.get(remoteId);
                if (peer == null) {
                    pendingIceCandidates.computeIfAbsent(remoteId, key -> new ArrayList<>()).add(candidate);
                } else {
                    List<IceCandidate> queued = pendingIceCandidates.get(remoteId);
                    if (queued == null) peer.addIceCandidate(candidate);
                    else queued.add(candidate);
                }
            }
        });
    }

    private PeerConnection getOrCreatePeer(String remoteId) {
        PeerConnection existing = peerConnections.get(remoteId);
        if (existing != null) return existing;
        List<PeerConnection.IceServer> iceServers = new ArrayList<>();
        iceServers.add(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer());
        PeerConnection.RTCConfiguration configuration = new PeerConnection.RTCConfiguration(iceServers);
        pendingIceCandidates.putIfAbsent(remoteId, new ArrayList<>());
        PeerConnection peer = peerConnectionFactory.createPeerConnection(configuration, new PeerConnection.Observer() {
            @Override public void onSignalingChange(PeerConnection.SignalingState state) {}
            @Override public void onIceConnectionChange(PeerConnection.IceConnectionState state) {
                if (state == PeerConnection.IceConnectionState.FAILED || state == PeerConnection.IceConnectionState.DISCONNECTED) {
                    runOnUiThread(() -> Toast.makeText(VideoCallActivity.this, "Connection interrupted. Check your network.", Toast.LENGTH_SHORT).show());
                }
            }
            @Override public void onIceConnectionReceivingChange(boolean receiving) {}
            @Override public void onIceGatheringChange(PeerConnection.IceGatheringState state) {}
            @Override public void onIceCandidate(IceCandidate candidate) {
                if (signalingClient != null) signalingClient.sendIceCandidate(remoteId, candidate);
            }
            @Override public void onIceCandidatesRemoved(IceCandidate[] candidates) {}
            @Override public void onAddStream(MediaStream stream) {
                if (!stream.videoTracks.isEmpty()) showRemoteVideo(remoteId, stream.videoTracks.get(0));
            }
            @Override public void onRemoveStream(MediaStream stream) { runOnUiThread(() -> removeRemoteVideo(remoteId)); }
            @Override public void onDataChannel(DataChannel channel) {}
            @Override public void onRenegotiationNeeded() {}
            @Override public void onAddTrack(RtpReceiver receiver, MediaStream[] streams) {
                MediaStreamTrack track = receiver.track();
                if (track instanceof VideoTrack) showRemoteVideo(remoteId, (VideoTrack) track);
            }
        });
        if (peer == null) {
            Toast.makeText(this, "Unable to create a call connection", Toast.LENGTH_LONG).show();
            return null;
        }
        if (localAudioTrack != null) peer.addTrack(localAudioTrack, java.util.Collections.singletonList("flexmeet"));
        if (localVideoTrack != null) peer.addTrack(localVideoTrack, java.util.Collections.singletonList("flexmeet"));
        peerConnections.put(remoteId, peer);
        return peer;
    }

    private void createOffer(String remoteId) {
        PeerConnection peer = getOrCreatePeer(remoteId);
        if (peer == null) return;
        peer.createOffer(new SdpObserver() {
            @Override public void onCreateSuccess(SessionDescription description) {
                peer.setLocalDescription(new SdpObserver() {
                    @Override public void onSetSuccess() { if (signalingClient != null) signalingClient.sendOffer(remoteId, description); }
                    @Override public void onSetFailure(String error) { Log.e(TAG, "Set local offer failed: " + error); }
                    @Override public void onCreateSuccess(SessionDescription value) {}
                    @Override public void onCreateFailure(String error) {}
                }, description);
            }
            @Override public void onSetSuccess() {}
            @Override public void onCreateFailure(String error) { Log.e(TAG, "Create offer failed: " + error); }
            @Override public void onSetFailure(String error) {}
        }, new MediaConstraints());
    }

    private void acceptOffer(String remoteId, SessionDescription offer) {
        PeerConnection peer = getOrCreatePeer(remoteId);
        if (peer == null) return;
        peer.setRemoteDescription(new SdpObserver() {
            @Override public void onSetSuccess() {
                flushPendingIceCandidates(remoteId, peer);
                peer.createAnswer(new SdpObserver() {
                    @Override public void onCreateSuccess(SessionDescription answer) {
                        peer.setLocalDescription(new SdpObserver() {
                            @Override public void onSetSuccess() { if (signalingClient != null) signalingClient.sendAnswer(remoteId, answer); }
                            @Override public void onSetFailure(String error) { Log.e(TAG, "Set local answer failed: " + error); }
                            @Override public void onCreateSuccess(SessionDescription value) {}
                            @Override public void onCreateFailure(String error) {}
                        }, answer);
                    }
                    @Override public void onSetSuccess() {}
                    @Override public void onCreateFailure(String error) { Log.e(TAG, "Create answer failed: " + error); }
                    @Override public void onSetFailure(String error) {}
                }, new MediaConstraints());
            }
            @Override public void onSetFailure(String error) { Log.e(TAG, "Set remote offer failed: " + error); }
            @Override public void onCreateSuccess(SessionDescription value) {}
            @Override public void onCreateFailure(String error) {}
        }, offer);
    }

    private SdpObserver noopSdpObserver() {
        return new SdpObserver() {
            @Override public void onCreateSuccess(SessionDescription value) {}
            @Override public void onSetSuccess() {}
            @Override public void onCreateFailure(String error) { Log.e(TAG, error); }
            @Override public void onSetFailure(String error) { Log.e(TAG, error); }
        };
    }

    private void showRemoteVideo(String remoteId, VideoTrack track) {
        runOnUiThread(() -> {
            remoteVideoTracks.put(remoteId, track);
            boolean found = false;
            for (int i = 0; i < remoteParticipants.size(); i++) {
                VideoParticipantAdapter.Participant participant = remoteParticipants.get(i);
                if (participant.id.equals(remoteId)) {
                    remoteParticipants.set(i, new VideoParticipantAdapter.Participant(remoteId, "Guest " + remoteId.substring(0, Math.min(4, remoteId.length())), null, track));
                    found = true;
                    break;
                }
            }
            if (!found) remoteParticipants.add(new VideoParticipantAdapter.Participant(remoteId, "Guest " + remoteId.substring(0, Math.min(4, remoteId.length())), null, track));
            if (participantAdapter != null) participantAdapter.setParticipants(new ArrayList<>(remoteParticipants));
        });
    }

    private void flushPendingIceCandidates(String remoteId, PeerConnection peer) {
        List<IceCandidate> queued = pendingIceCandidates.remove(remoteId);
        if (queued != null) for (IceCandidate candidate : queued) peer.addIceCandidate(candidate);
    }

    private void removeRemoteVideo(String remoteId) {
        remoteVideoTracks.remove(remoteId);
        remoteParticipants.removeIf(participant -> participant.id.equals(remoteId));
        if (participantAdapter != null) participantAdapter.setParticipants(new ArrayList<>(remoteParticipants));
    }

    private void removePeer(String remoteId) {
        PeerConnection peer = peerConnections.remove(remoteId);
        if (peer != null) { peer.close(); peer.dispose(); }
        removeRemoteVideo(remoteId);
    }

    private void setupControls() {
        binding.btnMic.setOnClickListener(v -> {
            isMuted = !isMuted;
            if (localAudioTrack != null) {
                localAudioTrack.setEnabled(!isMuted);
            }
            binding.btnMic.setImageResource(isMuted ? android.R.drawable.ic_lock_silent_mode : android.R.drawable.ic_btn_speak_now);
            binding.btnMic.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    isMuted ? getResources().getColor(R.color.error_red) : getResources().getColor(R.color.dark_gray)
            ));
            Toast.makeText(this, isMuted ? "Muted successfully" : "Unmuted successfully", Toast.LENGTH_SHORT).show();
        });

        binding.btnVideo.setOnClickListener(v -> {
            isVideoOn = !isVideoOn;
            if (localVideoTrack != null) {
                localVideoTrack.setEnabled(isVideoOn);
            }
            binding.localPreviewCard.setVisibility(isVideoOn ? View.VISIBLE : View.GONE);
            binding.btnVideo.setImageResource(isVideoOn ? android.R.drawable.ic_menu_camera : android.R.drawable.ic_menu_close_clear_cancel);
            binding.btnVideo.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    isVideoOn ? getResources().getColor(R.color.dark_gray) : getResources().getColor(R.color.error_red)
            ));
        });

        binding.btnEndCall.setOnClickListener(v -> finish());

        binding.btnNotes.setOnClickListener(v -> toggleFragment(NotesFragment.newInstance(meetingId)));
        binding.btnChat.setOnClickListener(v -> toggleFragment(ChatFragment.newInstance(meetingId)));
        
        binding.btnFlipCamera.setOnClickListener(v -> {
            if (videoCapturer instanceof CameraVideoCapturer) {
                ((CameraVideoCapturer) videoCapturer).switchCamera(null);
            }
        });

        binding.btnShareScreen.setOnClickListener(v -> {
            shareMeetingLink();
        });

        binding.btnRecord.setOnClickListener(v -> {
            if (!isRecording && !isCasting) {
                showRecordOrCastDialog();
            } else {
                if (isRecording) stopRecording();
                if (isCasting) stopCasting();
            }
        });
    }

    private void showRecordOrCastDialog() {
        String[] options = {"Record Screen", "Screen Share (Cast)"};
        new AlertDialog.Builder(this)
                .setTitle("Select Option")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        captureType = 1; // Record
                    } else {
                        captureType = 2; // Cast
                    }
                    startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), SCREEN_CAPTURE_REQUEST_CODE);
                })
                .show();
    }

    private void shareMeetingLink() {
        String shareLink = "https://flexmeet-aiman-3859.vercel.app/join/" + Uri.encode(meetingId);
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, "Join my FlexMeet: " + shareLink);
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, "Share meeting via"));
    }

    private void stopRecording() {
        Intent serviceIntent = new Intent(this, ScreenRecordService.class);
        serviceIntent.setAction(ScreenRecordService.ACTION_STOP);
        startService(serviceIntent);
        
        isRecording = false;
        timerHandler.removeCallbacks(timerRunnable);
        binding.recordingBadge.setVisibility(View.GONE);
        binding.btnRecord.setImageResource(android.R.drawable.presence_video_busy);
        binding.btnRecord.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.dark_gray)));
        
        Toast.makeText(this, "Recording saved to Gallery & Documents", Toast.LENGTH_LONG).show();
    }

    private void stopCasting() {
        isCasting = false;
        binding.btnRecord.setImageResource(android.R.drawable.presence_video_busy);
        binding.btnRecord.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.dark_gray)));
        Toast.makeText(this, "Casting stopped", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SCREEN_CAPTURE_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            if (captureType == 1) {
                startRecordingService(resultCode, data);
            } else if (captureType == 2) {
                startCasting(data);
            }
        }
        captureType = 0; // Reset
    }

    private void startRecordingService(int resultCode, Intent data) {
        isRecording = true;
        
        Intent serviceIntent = new Intent(this, ScreenRecordService.class);
        serviceIntent.setAction(ScreenRecordService.ACTION_START);
        serviceIntent.putExtra(ScreenRecordService.EXTRA_RESULT_CODE, resultCode);
        serviceIntent.putExtra(ScreenRecordService.EXTRA_DATA, data);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        binding.btnRecord.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        binding.btnRecord.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.error_red)));
        binding.recordingBadge.setVisibility(View.VISIBLE);
        
        startTime = System.currentTimeMillis();
        timerHandler.postDelayed(timerRunnable, 0);
        
        Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show();
    }

    private void startCasting(Intent data) {
        isCasting = true;
        binding.btnRecord.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        binding.btnRecord.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.success_green)));
        Toast.makeText(this, "Screen sharing active", Toast.LENGTH_SHORT).show();
    }

    private void setupDraggablePreview() {
        binding.dragHandle.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    dX = binding.localPreviewCard.getX() - event.getRawX();
                    dY = binding.localPreviewCard.getY() - event.getRawY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    binding.localPreviewCard.animate()
                            .x(event.getRawX() + dX)
                            .y(event.getRawY() + dY)
                            .setDuration(0)
                            .start();
                    break;
                case MotionEvent.ACTION_UP:
                    v.performClick();
                    break;
                default:
                    return false;
            }
            return true;
        });
    }

    private void listenForParticipants() {
        participantsRef = com.flexmeet.firebase.FirebaseManager.getInstance().getFirebaseDatabase()
                .getReference("meetings").child(meetingId).child("participants");

        String myId = FirebaseManager.getInstance().getCurrentUserId();
        if (myId == null) return;
        participantsRef.child(myId).setValue(true);
        participantsRef.child(myId).onDisconnect().removeValue();

        participantsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (binding == null) return;
                long count = snapshot.getChildrenCount();
                if (count > 1) {
                    binding.waitingScreen.setVisibility(View.GONE);
                    binding.videoGridRecycler.setVisibility(View.VISIBLE);
                } else {
                    binding.waitingScreen.setVisibility(View.VISIBLE);
                    binding.videoGridRecycler.setVisibility(View.GONE);
                    binding.participantCountText.setText("You are the only one here (" + count + ")");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        participantsRef.addValueEventListener(participantsListener);
    }

    private void toggleFragment(Fragment fragment) {
        if (binding.fragmentContainer.getVisibility() == View.VISIBLE) {
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            if (currentFragment != null && currentFragment.getClass().equals(fragment.getClass())) {
                binding.fragmentContainer.setVisibility(View.GONE);
                getSupportFragmentManager().beginTransaction().remove(currentFragment).commit();
            } else {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .commit();
            }
        } else {
            binding.fragmentContainer.setVisibility(View.VISIBLE);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isRecording) stopRecording();
        if (signalingClient != null) signalingClient.dispose();
        String userId = FirebaseManager.getInstance().getCurrentUserId();
        if (participantsRef != null) {
            if (participantsListener != null) participantsRef.removeEventListener(participantsListener);
            if (userId != null) participantsRef.child(userId).removeValue();
        }
        for (PeerConnection peer : peerConnections.values()) { peer.close(); peer.dispose(); }
        peerConnections.clear();
        if (participantAdapter != null) participantAdapter.setParticipants(new ArrayList<>());
        if (binding != null) binding.videoGridRecycler.setAdapter(null);
        if (audioManager != null) {
            audioManager.setMode(AudioManager.MODE_NORMAL);
            audioManager.setSpeakerphoneOn(false);
        }
        if (videoCapturer != null) {
            try {
                videoCapturer.stopCapture();
            } catch (InterruptedException e) {
                Log.e(TAG, "Error stopping capture", e);
            }
            videoCapturer.dispose();
        }
        if (localVideoView != null) {
            localVideoView.release();
        }
        if (eglBase != null) {
            eglBase.release();
        }
    }
}
