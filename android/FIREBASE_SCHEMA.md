/**
 * FIREBASE SCHEMA DOCUMENTATION
 * ============================
 * 
 * This document describes the structure of data in Firebase (Firestore and Realtime Database)
 * 
 * 1. FIRESTORE DATABASE STRUCTURE
 * ===============================
 * 
 * Collection: users
 * └── Document: {uid}
 *     ├── uid (String) - User ID from Firebase Authentication
 *     ├── email (String) - User email
 *     ├── displayName (String) - User's full name
 *     ├── photoUrl (String) - URL to user's profile picture
 *     ├── phoneNumber (String) - User's phone number
 *     ├── status (String) - User status (online, offline, busy, away)
 *     ├── fcmToken (String) - Firebase Cloud Messaging token
 *     ├── isVerified (Boolean) - Email verification status
 *     ├── createdAt (Number) - Account creation timestamp
 *     └── updatedAt (Number) - Last update timestamp
 * 
 * Collection: meetings
 * └── Document: {meetingId}
 *     ├── meetingId (String) - Unique meeting ID
 *     ├── title (String) - Meeting title
 *     ├── description (String) - Meeting description
 *     ├── hostId (String) - Host's user ID
 *     ├── hostName (String) - Host's display name
 *     ├── hostPhotoUrl (String) - Host's profile picture
 *     ├── startTime (Number) - Meeting start time
 *     ├── endTime (Number) - Meeting end time
 *     ├── scheduledTime (Number) - When meeting is scheduled to start
 *     ├── maxParticipants (Number) - Maximum participant limit
 *     ├── status (String) - scheduled, ongoing, ended, cancelled
 *     ├── isGroupCall (Boolean) - Whether it's a group or one-to-one call
 *     ├── meetingCode (String) - Unique code to join meeting
 *     ├── password (String) - Optional password protection
 *     ├── participants (Map) - {userId: joinedStatus}
 *     ├── createdAt (Number) - Meeting creation timestamp
 *     ├── updatedAt (Number) - Last update timestamp
 *     │
 *     ├── Subcollection: participants
 *     │   └── Document: {userId}
 *     │       ├── joinedAt (Number) - Participant join time
 *     │       ├── leftAt (Number) - Participant leave time (optional)
 *     │       ├── role (String) - participant, host, moderator
 *     │       └── duration (Number) - Time spent in meeting
 *     │
 *     ├── Subcollection: messages
 *     │   └── Document: {messageId}
 *     │       ├── messageId (String) - Unique message ID
 *     │       ├── senderId (String) - Sender's user ID
 *     │       ├── senderName (String) - Sender's display name
 *     │       ├── senderPhotoUrl (String) - Sender's avatar
 *     │       ├── text (String) - Message content
 *     │       ├── imageUrl (String) - Attached image URL (optional)
 *     │       ├── messageType (String) - text, image, file, location
 *     │       ├── timestamp (Number) - Message timestamp
 *     │       └── isRead (Boolean) - Read status
 *     │
 *     ├── Subcollection: notes
 *     │   └── Document: {noteId}
 *     │       ├── noteId (String) - Unique note ID
 *     │       ├── userId (String) - Note creator's ID
 *     │       ├── content (String) - Note content
 *     │       ├── createdAt (Number) - Creation timestamp
 *     │       ├── updatedAt (Number) - Last update timestamp
 *     │       └── lastEditedBy (String) - Last editor's ID
 *     │
 *     └── Subcollection: files
 *         └── Document: {fileId}
 *             ├── fileId (String) - Unique file ID
 *             ├── fileName (String) - File name
 *             ├── fileUrl (String) - Download URL
 *             ├── fileType (String) - pdf, image, document, video, audio
 *             ├── fileSize (Number) - File size in bytes
 *             ├── uploadedBy (String) - Uploader's user ID
 *             ├── uploaderName (String) - Uploader's display name
 *             ├── uploadTime (Number) - Upload timestamp
 *             └── storagePath (String) - Storage path in Firebase Storage
 * 
 * 
 * 2. FIREBASE REALTIME DATABASE STRUCTURE
 * ========================================
 * 
 * Root
 * └── meetings
 *     └── {meetingId}
 *         └── locations
 *             └── {userId}
 *                 ├── userId (String) - User ID
 *                 ├── userName (String) - User's display name
 *                 ├── latitude (Number) - Location latitude
 *                 ├── longitude (Number) - Location longitude
 *                 ├── accuracy (Number) - Location accuracy in meters
 *                 ├── timestamp (Number) - Last update timestamp
 *                 ├── displayAddress (String) - Human-readable address
 *                 └── isActive (Boolean) - Whether location sharing is active
 * 
 * 
 * 3. FIREBASE STORAGE STRUCTURE
 * =============================
 * 
 * /meetings
 * └── {meetingId}
 *     ├── /files
 *     │   ├── file1.pdf
 *     │   ├── image1.jpg
 *     │   └── document1.docx
 *     │
 *     └── /recordings (optional)
 *         └── recording1.mp4
 * 
 * /users
 * └── {userId}
 *     └── profile_picture.jpg
 * 
 * 
 * 4. FIREBASE AUTHENTICATION
 * ==========================
 * 
 * Each user is authenticated via Firebase Authentication using:
 * - Email & Password
 * - Google Sign-In (optional)
 * - Phone Authentication (optional)
 * 
 * 
 * 5. SECURITY RULES (FIRESTORE)
 * =============================
 * 
 * rules_version = '2';
 * service cloud.firestore {
 *   match /databases/{database}/documents {
 *     // Users can only read/write their own documents
 *     match /users/{userId} {
 *       allow read, write: if request.auth.uid == userId;
 *     }
 *     
 *     // Anyone can read meetings
 *     match /meetings/{meetingId} {
 *       allow read: if true;
 *       allow create: if request.auth != null;
 *       allow update, delete: if resource.data.hostId == request.auth.uid;
 *       
 *       // Subcollections
 *       match /{document=**} {
 *         allow read: if true;
 *         allow write: if request.auth != null;
 *       }
 *     }
 *   }
 * }
 * 
 * 
 * 6. INDEXES
 * ==========
 * 
 * The following composite indexes should be created:
 * 
 * Collection: meetings
 * Indexes:
 * - fields: hostId, scheduledTime (DESC)
 * - fields: status, startTime (DESC)
 * - fields: participantIds, updatedAt (DESC)
 * 
 * 
 * 7. DATA TYPES REFERENCE
 * ======================
 * 
 * String: Text data
 * Number: Integer or floating-point numbers
 * Boolean: true or false
 * Timestamp: Firestore Timestamp (auto-converted to milliseconds)
 * Map: Object with key-value pairs
 * Array: List of values
 * Reference: Document reference
 * 
 */
