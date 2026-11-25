package com.flexmeet.webRTC;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.flexmeet.firebase.FirebaseManager;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;

import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

import java.util.HashMap;
import java.util.Map;

public class SignalingClient {

    private final String selfId;
    private final DatabaseReference dbRef;
    private final SignalingInterface callback;
    private final Map<String, ChildEventListener> messageListeners = new HashMap<>();
    private final ChildEventListener participantsListener;

    public interface SignalingInterface {
        void onRemoteUserJoined(String remoteId);
        void onRemoteUserLeft(String remoteId);
        void onOfferReceived(String remoteId, SessionDescription sdp);
        void onAnswerReceived(String remoteId, SessionDescription sdp);
        void onIceCandidateReceived(String remoteId, IceCandidate candidate);
    }

    public SignalingClient(String meetingId, SignalingInterface callback) {
        this.callback = callback;
        this.selfId = FirebaseManager.getInstance().getCurrentUserId();
        if (selfId == null || selfId.trim().isEmpty()) {
            throw new IllegalStateException("A signed-in Firebase user is required for meeting signaling");
        }
        this.dbRef = FirebaseManager.getInstance().getFirebaseDatabase()
                .getReference("meetings").child(meetingId).child("signaling");

        participantsListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                String remoteId = snapshot.getKey();
                if (remoteId == null || remoteId.equals(selfId)) return;
                listenToUserSignaling(remoteId);
                callback.onRemoteUserJoined(remoteId);
            }

            @Override public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                String remoteId = snapshot.getKey();
                if (remoteId != null && !remoteId.equals(selfId)) {
                    removeMessageListener(remoteId);
                    callback.onRemoteUserLeft(remoteId);
                }
            }
            @Override public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };

        dbRef.addChildEventListener(participantsListener);
        dbRef.child(selfId).onDisconnect().removeValue();
        dbRef.child(selfId).child("joinedAt").setValue(System.currentTimeMillis());
    }

    private void listenToUserSignaling(String remoteId) {
        if (messageListeners.containsKey(remoteId)) return;
        DatabaseReference remoteRef = dbRef.child(remoteId).child("messages").child(selfId);
        ChildEventListener listener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                String type = snapshot.child("type").getValue(String.class);
                if (type == null) return;
                if ("offer".equals(type)) {
                    String sdp = snapshot.child("sdp").getValue(String.class);
                    if (sdp != null) callback.onOfferReceived(remoteId,
                            new SessionDescription(SessionDescription.Type.OFFER, sdp));
                } else if ("answer".equals(type)) {
                    String sdp = snapshot.child("sdp").getValue(String.class);
                    if (sdp != null) callback.onAnswerReceived(remoteId,
                            new SessionDescription(SessionDescription.Type.ANSWER, sdp));
                } else if ("candidate".equals(type)) {
                    String sdpMid = snapshot.child("sdpMid").getValue(String.class);
                    Integer lineIndex = snapshot.child("sdpMLineIndex").getValue(Integer.class);
                    String sdp = snapshot.child("sdp").getValue(String.class);
                    if (sdpMid != null && lineIndex != null && sdp != null) {
                        callback.onIceCandidateReceived(remoteId,
                                new IceCandidate(sdpMid, lineIndex, sdp));
                    }
                }
                snapshot.getRef().removeValue();
            }
            @Override public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
            @Override public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };
        messageListeners.put(remoteId, listener);
        remoteRef.addChildEventListener(listener);
    }

    public void sendOffer(String remoteId, SessionDescription sdp) {
        sendMessage(remoteId, "offer", "sdp", sdp.description);
    }

    public void sendAnswer(String remoteId, SessionDescription sdp) {
        sendMessage(remoteId, "answer", "sdp", sdp.description);
    }

    public void sendIceCandidate(String remoteId, IceCandidate candidate) {
        Map<String, Object> data = new HashMap<>();
        data.put("type", "candidate");
        data.put("sdpMid", candidate.sdpMid);
        data.put("sdpMLineIndex", candidate.sdpMLineIndex);
        data.put("sdp", candidate.sdp);
        dbRef.child(selfId).child("messages").child(remoteId).push().setValue(data);
    }

    private void sendMessage(String remoteId, String type, String key, String value) {
        Map<String, Object> data = new HashMap<>();
        data.put("type", type);
        data.put(key, value);
        dbRef.child(selfId).child("messages").child(remoteId).push().setValue(data);
    }

    private void removeMessageListener(String remoteId) {
        ChildEventListener listener = messageListeners.remove(remoteId);
        if (listener != null) {
            dbRef.child(remoteId).child("messages").child(selfId).removeEventListener(listener);
        }
    }

    public void dispose() {
        dbRef.removeEventListener(participantsListener);
        for (String remoteId : new java.util.ArrayList<>(messageListeners.keySet())) {
            removeMessageListener(remoteId);
        }
        dbRef.child(selfId).removeValue();
    }
}
