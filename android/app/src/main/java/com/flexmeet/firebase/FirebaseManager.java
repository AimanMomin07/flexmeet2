package com.flexmeet.firebase;

import android.net.Uri;
import androidx.annotation.NonNull;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.flexmeet.models.User;
import com.flexmeet.models.Meeting;
import com.flexmeet.models.Message;
import com.flexmeet.models.MeetingNotes;
import com.flexmeet.models.File;
import com.flexmeet.models.LocationData;
import timber.log.Timber;
import java.util.HashMap;
import java.util.Map;

/**
 * FirebaseManager
 * Centralized manager for all Firebase operations
 */
public class FirebaseManager {

    private static FirebaseManager instance;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firebaseFirestore;
    private FirebaseDatabase firebaseDatabase;
    private FirebaseStorage firebaseStorage;

    private FirebaseManager() {
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.firebaseFirestore = FirebaseFirestore.getInstance();
        this.firebaseDatabase = FirebaseDatabase.getInstance();
        this.firebaseStorage = FirebaseStorage.getInstance();

        // Realtime Database persistence is enabled once during Application startup.
        // Calling setPersistenceEnabled again here can fail after another reference was opened.
    }

    public static synchronized FirebaseManager getInstance() {
        if (instance == null) {
            instance = new FirebaseManager();
        }
        return instance;
    }

    // ==================== AUTHENTICATION METHODS ====================

    /**
     * Register a new user
     */
    public Task<AuthResult> registerUser(String email, String password) {
        return firebaseAuth.createUserWithEmailAndPassword(email, password);
    }

    /**
     * Login user
     */
    public Task<AuthResult> loginUser(String email, String password) {
        return firebaseAuth.signInWithEmailAndPassword(email, password);
    }

    /**
     * Logout user
     */
    public void logoutUser() {
        firebaseAuth.signOut();
    }

    /**
     * Get current user
     */
    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }

    /**
     * Update user profile
     */
    public Task<Void> updateUserProfile(String displayName, Uri photoUri) {
        FirebaseUser user = getCurrentUser();
        if (user != null) {
            UserProfileChangeRequest.Builder builder = new UserProfileChangeRequest.Builder();
            if (displayName != null) {
                builder.setDisplayName(displayName);
            }
            if (photoUri != null) {
                builder.setPhotoUri(photoUri);
            }
            return user.updateProfile(builder.build());
        }
        return null;
    }

    /**
     * Send password reset email
     */
    public Task<Void> sendPasswordResetEmail(String email) {
        return firebaseAuth.sendPasswordResetEmail(email);
    }

    // ==================== USER METHODS ====================

    /**
     * Save user to Firestore
     */
    public Task<Void> saveUser(String uid, User user) {
        return firebaseFirestore.collection("users")
                .document(uid)
                .set(user);
    }

    /**
     * Get user from Firestore
     */
    public Task<DocumentSnapshot> getUser(String uid) {
        return firebaseFirestore.collection("users")
                .document(uid)
                .get();
    }

    /**
     * Update user status
     */
    public Task<Void> updateUserStatus(String uid, String status) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);
        updates.put("updatedAt", System.currentTimeMillis());
        return firebaseFirestore.collection("users")
                .document(uid)
                .update(updates);
    }

    // ==================== MEETING METHODS ====================

    /**
     * Create a new meeting with a specific ID
     */
    public Task<Void> saveMeeting(Meeting meeting) {
        return firebaseFirestore.collection("meetings")
                .document(meeting.getMeetingId())
                .set(meeting);
    }

    /**
     * Create a new meeting (random ID)
     */
    public Task<DocumentReference> createMeeting(Meeting meeting) {
        return firebaseFirestore.collection("meetings")
                .add(meeting);
    }

    /**
     * Get meeting by ID
     */
    public Task<DocumentSnapshot> getMeeting(String meetingId) {
        return firebaseFirestore.collection("meetings")
                .document(meetingId)
                .get();
    }

    /**
     * Get user's meetings
     */
    public Task<QuerySnapshot> getUserMeetings(String userId) {
        return firebaseFirestore.collection("meetings")
                .whereEqualTo("hostId", userId)
                .orderBy("scheduledTime", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get();
    }

    /**
     * Update meeting
     */
    public Task<Void> updateMeeting(String meetingId, Meeting meeting) {
        return firebaseFirestore.collection("meetings")
                .document(meetingId)
                .set(meeting);
    }

    /**
     * Add participant to meeting
     */
    public Task<Void> addParticipant(String meetingId, String userId) {
        Map<String, Object> participantData = new HashMap<>();
        participantData.put("joinedAt", System.currentTimeMillis());
        return firebaseFirestore.collection("meetings")
                .document(meetingId)
                .collection("participants")
                .document(userId)
                .set(participantData);
    }

    /**
     * Remove participant from meeting
     */
    public Task<Void> removeParticipant(String meetingId, String userId) {
        return firebaseFirestore.collection("meetings")
                .document(meetingId)
                .collection("participants")
                .document(userId)
                .delete();
    }

    // ==================== MESSAGE METHODS ====================

    /**
     * Send message
     */
    public Task<DocumentReference> sendMessage(String meetingId, Message message) {
        return firebaseFirestore.collection("meetings")
                .document(meetingId)
                .collection("messages")
                .add(message);
    }

    /**
     * Get messages for a meeting
     */
    public Task<QuerySnapshot> getMessages(String meetingId) {
        return firebaseFirestore.collection("meetings")
                .document(meetingId)
                .collection("messages")
                .orderBy("timestamp")
                .get();
    }

    // ==================== MEETING NOTES METHODS ====================

    /**
     * Save meeting notes
     */
    public Task<Void> saveMeetingNotes(String meetingId, MeetingNotes notes) {
        return firebaseFirestore.collection("meetings")
                .document(meetingId)
                .collection("notes")
                .document(notes.getNoteId())
                .set(notes);
    }

    /**
     * Get meeting notes
     */
    public Task<QuerySnapshot> getMeetingNotes(String meetingId) {
        return firebaseFirestore.collection("meetings")
                .document(meetingId)
                .collection("notes")
                .orderBy("updatedAt")
                .get();
    }

    // ==================== FILE UPLOAD/DOWNLOAD METHODS ====================

    /**
     * Upload file to Firebase Storage
     */
    public UploadTask uploadFile(String meetingId, String fileName, Uri fileUri) {
        StorageReference storageRef = firebaseStorage.getReference()
                .child("meetings/" + meetingId + "/files/" + fileName);
        return storageRef.putFile(fileUri);
    }

    /**
     * Get file download URL
     */
    public Task<Uri> getFileDownloadUrl(String meetingId, String fileName) {
        StorageReference storageRef = firebaseStorage.getReference()
                .child("meetings/" + meetingId + "/files/" + fileName);
        return storageRef.getDownloadUrl();
    }

    /**
     * Save file metadata to Firestore
     */
    public Task<Void> saveFileMetadata(String meetingId, File file) {
        return firebaseFirestore.collection("meetings")
                .document(meetingId)
                .collection("files")
                .document(file.getFileId())
                .set(file);
    }

    /**
     * Get files for a meeting
     */
    public Task<QuerySnapshot> getFiles(String meetingId) {
        return firebaseFirestore.collection("meetings")
                .document(meetingId)
                .collection("files")
                .orderBy("uploadTime", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get();
    }

    // ==================== LOCATION SHARING METHODS ====================

    /**
     * Save location data in Realtime Database (for real-time updates)
     */
    public Task<Void> saveLocationData(String meetingId, String userId, LocationData locationData) {
        DatabaseReference ref = firebaseDatabase.getReference()
                .child("meetings")
                .child(meetingId)
                .child("locations")
                .child(userId);
        return ref.setValue(locationData.toMap());
    }

    /**
     * Get location data from Realtime Database
     */
    public DatabaseReference getLocationReference(String meetingId, String userId) {
        return firebaseDatabase.getReference()
                .child("meetings")
                .child(meetingId)
                .child("locations")
                .child(userId);
    }

    // ==================== NOTIFICATION METHODS ====================

    /**
     * Save FCM token
     */
    public Task<Void> saveFCMToken(String uid, String token) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("fcmToken", token);
        return firebaseFirestore.collection("users")
                .document(uid)
                .update(updates);
    }

    // ==================== UTILITY METHODS ====================

    public FirebaseAuth getFirebaseAuth() {
        return firebaseAuth;
    }

    public FirebaseFirestore getFirebaseFirestore() {
        return firebaseFirestore;
    }

    public FirebaseDatabase getFirebaseDatabase() {
        return firebaseDatabase;
    }

    public FirebaseStorage getFirebaseStorage() {
        return firebaseStorage;
    }

    public boolean isUserLoggedIn() {
        return getCurrentUser() != null;
    }

    public String getCurrentUserId() {
        FirebaseUser user = getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    public String getCurrentUserEmail() {
        FirebaseUser user = getCurrentUser();
        return user != null ? user.getEmail() : null;
    }

    /** Publish a minimal profile for email-based friend discovery in Realtime Database. */
    public com.google.android.gms.tasks.Task<Void> saveUserRealtime(String uid, User user) {
        Map<String, Object> profile = new HashMap<>();
        profile.put("uid", uid);
        profile.put("email", user.getEmail() == null ? "" : user.getEmail().trim().toLowerCase(java.util.Locale.ROOT));
        profile.put("displayName", user.getDisplayName() == null ? "" : user.getDisplayName());
        profile.put("photoUrl", user.getPhotoUrl());
        profile.put("updatedAt", System.currentTimeMillis());
        return firebaseDatabase.getReference("users").child(uid).updateChildren(profile);
    }

    public User getCurrentRealtimeUser() {
        FirebaseUser user = getCurrentUser();
        if (user == null) return null;
        String name = user.getDisplayName();
        if (name == null || name.trim().isEmpty()) {
            String email = user.getEmail();
            name = email == null ? "FlexMeet member" : email.split("@")[0];
        }
        User profile = new User(user.getUid(), user.getEmail(), name);
        profile.setPhotoUrl(user.getPhotoUrl() == null ? null : user.getPhotoUrl().toString());
        return profile;
    }
}
