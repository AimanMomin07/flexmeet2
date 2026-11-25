package com.flexmeet.service.firebase;

import com.google.firebase.database.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.google.firebase.auth.UserRecord.CreateRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class FirebaseService {

    private final FirebaseApp firebaseApp;
    private final FirebaseAuth firebaseAuth;
    private final FirebaseDatabase firebaseDatabase;

    public FirebaseService() {
        this.firebaseApp = FirebaseApp.getInstance();
        this.firebaseAuth = FirebaseAuth.getInstance(firebaseApp);
        this.firebaseDatabase = FirebaseDatabase.getInstance(firebaseApp);
    }

    /**
     * Create a new user in Firebase Authentication
     */
    public UserRecord createFirebaseUser(String email, String password, String displayName) 
            throws FirebaseAuthException {
        CreateRequest request = new CreateRequest()
                .setEmail(email)
                .setPassword(password)
                .setDisplayName(displayName);

        return firebaseAuth.createUser(request);
    }

    /**
     * Get user by email from Firebase
     */
    public UserRecord getUserByEmail(String email) throws FirebaseAuthException {
        return firebaseAuth.getUserByEmail(email);
    }

    /**
     * Delete user from Firebase
     */
    public void deleteUser(String uid) throws FirebaseAuthException {
        firebaseAuth.deleteUser(uid);
    }

    /**
     * Store meeting data in Firebase Realtime Database
     */
    public CompletableFuture<Void> storeMeetingData(String meetingCode, Map<String, Object> meetingData) {
        DatabaseReference ref = firebaseDatabase.getReference("meetings/" + meetingCode);
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        ref.setValue(meetingData, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError error, DatabaseReference ref) {
                if (error != null) {
                    log.error("Failed to store meeting data: " + error.getMessage());
                    future.completeExceptionally(new Exception(error.getMessage()));
                } else {
                    log.info("Meeting data stored successfully for meeting: " + meetingCode);
                    future.complete(null);
                }
            }
        });
        
        return future;
    }

    /**
     * Get meeting data from Firebase
     */
    public CompletableFuture<DataSnapshot> getMeetingData(String meetingCode) {
        DatabaseReference ref = firebaseDatabase.getReference("meetings/" + meetingCode);
        CompletableFuture<DataSnapshot> future = new CompletableFuture<>();
        
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                future.complete(dataSnapshot);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                log.error("Failed to get meeting data: " + error.getMessage());
                future.completeExceptionally(new Exception(error.getMessage()));
            }
        });
        
        return future;
    }

    /**
     * Store participant data in Firebase
     */
    public CompletableFuture<Void> storeParticipantData(String meetingCode, String participantUUID, 
                                                         Map<String, Object> participantData) {
        DatabaseReference ref = firebaseDatabase.getReference(
            "meetings/" + meetingCode + "/participants/" + participantUUID);
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        ref.setValue(participantData, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError error, DatabaseReference ref) {
                if (error != null) {
                    log.error("Failed to store participant data: " + error.getMessage());
                    future.completeExceptionally(new Exception(error.getMessage()));
                } else {
                    future.complete(null);
                }
            }
        });
        
        return future;
    }

    /**
     * Remove participant from Firebase meeting
     */
    public CompletableFuture<Void> removeParticipant(String meetingCode, String participantUUID) {
        DatabaseReference ref = firebaseDatabase.getReference(
            "meetings/" + meetingCode + "/participants/" + participantUUID);
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        ref.removeValue(new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError error, DatabaseReference ref) {
                if (error != null) {
                    future.completeExceptionally(new Exception(error.getMessage()));
                } else {
                    future.complete(null);
                }
            }
        });
        
        return future;
    }

    /**
     * Store WebRTC signaling data
     */
    public CompletableFuture<Void> storeSignalingData(String meetingCode, String peerId, 
                                                       Map<String, Object> signalingData) {
        DatabaseReference ref = firebaseDatabase.getReference(
            "meetings/" + meetingCode + "/signaling/" + peerId);
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        ref.setValue(signalingData, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError error, DatabaseReference ref) {
                if (error != null) {
                    future.completeExceptionally(new Exception(error.getMessage()));
                } else {
                    future.complete(null);
                }
            }
        });
        
        return future;
    }

    /**
     * Listen to real-time participant changes
     */
    public void listenToParticipantChanges(String meetingCode, 
                                          ValueEventListener listener) {
        DatabaseReference ref = firebaseDatabase.getReference(
            "meetings/" + meetingCode + "/participants");
        ref.addValueEventListener(listener);
    }

    /**
     * Stop listening to participant changes
     */
    public void stopListeningToParticipantChanges(String meetingCode, 
                                                   ValueEventListener listener) {
        DatabaseReference ref = firebaseDatabase.getReference(
            "meetings/" + meetingCode + "/participants");
        ref.removeEventListener(listener);
    }
}
