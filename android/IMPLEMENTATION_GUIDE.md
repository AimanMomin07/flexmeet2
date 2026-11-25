# FlexMeet Android Application - Complete Implementation Guide

## Project Overview

FlexMeet is a comprehensive Android video conferencing application similar to Google Meet, built with Core Java and Firebase as backend. It includes advanced features like live meeting notes, file sharing, location sharing, scheduling, and real-time chat.

---

## Table of Contents

1. [Project Architecture](#project-architecture)
2. [Folder Structure](#folder-structure)
3. [Module Overview](#module-overview)
4. [Setup Instructions](#setup-instructions)
5. [Firebase Configuration](#firebase-configuration)
6. [Dependency Management](#dependency-management)
7. [Feature Modules](#feature-modules)
8. [Database Schema](#database-schema)
9. [Permissions Handling](#permissions-handling)
10. [API Integration](#api-integration)
11. [WebRTC Implementation](#webrtc-implementation)
12. [Best Practices](#best-practices)

---

## Project Architecture

FlexMeet uses **Clean Architecture** with **MVC (Model-View-Controller)** pattern:

```
┌─────────────────────────────────────────┐
│         Presentation Layer (UI)         │
│   Activities, Fragments, Adapters       │
└────────────────┬────────────────────────┘
                 │
┌────────────────┴────────────────────────┐
│      Business Logic Layer (Managers)    │
│  FirebaseManager, LocationManager, etc  │
└────────────────┬────────────────────────┘
                 │
┌────────────────┴────────────────────────┐
│        Data Layer (Repositories)        │
│    Firebase, SharedPreferences, etc     │
└─────────────────────────────────────────┘
```

---

## Folder Structure

```
FlexMeet/
├── android/
│   ├── app/
│   │   ├── src/
│   │   │   ├── main/
│   │   │   │   ├── AndroidManifest.xml
│   │   │   │   ├── java/com/flexmeet/
│   │   │   │   │   ├── activities/              # All Activities
│   │   │   │   │   │   ├── SplashActivity.java
│   │   │   │   │   │   ├── LoginActivity.java
│   │   │   │   │   │   ├── RegisterActivity.java
│   │   │   │   │   │   ├── DashboardActivity.java
│   │   │   │   │   │   ├── VideoCallActivity.java
│   │   │   │   │   │   ├── ScheduleMeetingActivity.java
│   │   │   │   │   │   ├── ProfileActivity.java
│   │   │   │   │   │   ├── LocationSharingActivity.java
│   │   │   │   │   │   └── FileTransferActivity.java
│   │   │   │   │   │
│   │   │   │   │   ├── fragments/               # All Fragments
│   │   │   │   │   │   ├── ChatFragment.java
│   │   │   │   │   │   ├── NotesFragment.java
│   │   │   │   │   │   ├── ParticipantsFragment.java
│   │   │   │   │   │   ├── FileShareFragment.java
│   │   │   │   │   │   └── LocationShareFragment.java
│   │   │   │   │   │
│   │   │   │   │   ├── models/                  # Data Models
│   │   │   │   │   │   ├── User.java
│   │   │   │   │   │   ├── Meeting.java
│   │   │   │   │   │   ├── Message.java
│   │   │   │   │   │   ├── MeetingNotes.java
│   │   │   │   │   │   ├── File.java
│   │   │   │   │   │   └── LocationData.java
│   │   │   │   │   │
│   │   │   │   │   ├── firebase/                # Firebase Management
│   │   │   │   │   │   ├── FirebaseManager.java
│   │   │   │   │   │   └── FirebaseMessagingService.java
│   │   │   │   │   │
│   │   │   │   │   ├── services/                # Background Services
│   │   │   │   │   │   ├── CallService.java
│   │   │   │   │   │   ├── NotificationService.java
│   │   │   │   │   │   ├── LocationService.java
│   │   │   │   │   │   └── FirebaseMessagingService.java
│   │   │   │   │   │
│   │   │   │   │   ├── utils/                   # Utility Classes
│   │   │   │   │   │   ├── PermissionManager.java
│   │   │   │   │   │   ├── SharedPrefManager.java
│   │   │   │   │   │   ├── DateTimeUtils.java
│   │   │   │   │   │   ├── ValidationUtils.java
│   │   │   │   │   │   ├── NetworkUtils.java
│   │   │   │   │   │   ├── FileUtils.java
│   │   │   │   │   │   └── LocationUtils.java
│   │   │   │   │   │
│   │   │   │   │   ├── adapters/                # RecyclerView Adapters
│   │   │   │   │   │   ├── MessageAdapter.java
│   │   │   │   │   │   ├── ParticipantAdapter.java
│   │   │   │   │   │   ├── MeetingAdapter.java
│   │   │   │   │   │   └── FileAdapter.java
│   │   │   │   │   │
│   │   │   │   │   ├── interfaces/              # Callbacks & Interfaces
│   │   │   │   │   │   ├── CallCallback.java
│   │   │   │   │   │   ├── MessageCallback.java
│   │   │   │   │   │   └── LocationCallback.java
│   │   │   │   │   │
│   │   │   │   │   ├── constants/               # App Constants
│   │   │   │   │   │   └── AppConstants.java
│   │   │   │   │   │
│   │   │   │   │   └── FlexMeetApplication.java # App Entry Point
│   │   │   │   │
│   │   │   │   └── res/
│   │   │   │       ├── layout/                  # XML Layouts
│   │   │   │       │   ├── activity_login.xml
│   │   │   │       │   ├── activity_register.xml
│   │   │   │       │   ├── activity_dashboard.xml
│   │   │   │       │   ├── activity_video_call.xml
│   │   │   │       │   ├── activity_schedule_meeting.xml
│   │   │   │       │   ├── fragment_chat.xml
│   │   │   │       │   ├── fragment_notes.xml
│   │   │   │       │   ├── fragment_location.xml
│   │   │   │       │   └── item_message.xml
│   │   │   │       │
│   │   │   │       ├── drawable/                # Drawable Resources
│   │   │   │       │   ├── ic_launcher_foreground.xml
│   │   │   │       │   ├── rounded_button_blue.xml
│   │   │   │       │   ├── ic_camera.png
│   │   │   │       │   ├── ic_microphone.png
│   │   │   │       │   ├── ic_call_end.png
│   │   │   │       │   └── ic_location.png
│   │   │   │       │
│   │   │   │       ├── values/                  # Value Resources
│   │   │   │       │   ├── strings.xml
│   │   │   │       │   ├── colors.xml
│   │   │   │       │   ├── styles.xml
│   │   │   │       │   └── dimens.xml
│   │   │   │       │
│   │   │   │       ├── menu/                    # Menu Resources
│   │   │   │       │   └── menu_main.xml
│   │   │   │       │
│   │   │   │       └── mipmap/                  # Icon Resources
│   │   │   │           └── ic_launcher.png
│   │   │   │
│   │   │   └── test/                            # Unit Tests
│   │   │       └── java/com/flexmeet/
│   │   │
│   │   ├── build.gradle                         # Module build file
│   │   ├── proguard-rules.pro                  # Proguard rules
│   │   └── google-services.json                 # Firebase configuration
│   │
│   ├── build.gradle                             # Project build file
│   └── gradle.properties                        # Gradle properties
│
├── FIREBASE_SCHEMA.md                           # Firebase database schema
├── IMPLEMENTATION_GUIDE.md                      # This file
└── README.md                                    # Project README
```

---

## Module Overview

### 1. **Activities (User Interface Screens)**

Each Activity represents a complete screen in the application:

#### SplashActivity
- Entry point of the application
- Checks user authentication status
- Initializes Firebase

#### LoginActivity
- Email and password login
- Forgot password functionality
- Sign-up navigation

#### RegisterActivity
- User registration with email and password
- Validation of inputs
- Profile creation

#### DashboardActivity
- Main menu after login
- Quick action buttons (Start/Join/Schedule Meeting)
- List of upcoming and recent meetings

#### VideoCallActivity
- Main video calling screen
- WebRTC peer connection handling
- Real-time communication controls
- Hosts Chat, Notes, Participants, Location, and File fragments

#### ScheduleMeetingActivity
- Date and time selection
- Participant management
- Meeting configuration

#### ProfileActivity
- User profile display and editing
- Settings management
- Logout functionality

#### LocationSharingActivity
- Live location display using Google Maps
- Real-time location updates

#### FileTransferActivity
- File upload and download management

---

### 2. **Fragments (Reusable UI Components)**

Fragments are used within VideoCallActivity:

#### ChatFragment
- Real-time message display
- Message input field
- Auto-scroll to latest messages

#### NotesFragment
- Meeting notes editor
- Auto-save to Firebase
- Edit notes during meeting

#### ParticipantsFragment
- List of active participants
- Participant status indicators
- Mute/Remove functionality

#### FileShareFragment
- File upload interface
- Downloaded files list
- File sharing with other participants

#### LocationShareFragment
- Share current location
- View other participants' locations
- Display distance and directions

---

### 3. **Models (Data Classes)**

Data classes representing business entities:

- **User**: User profile and authentication
- **Meeting**: Meeting details and metadata
- **Message**: Chat messages
- **MeetingNotes**: Notes taken during meeting
- **File**: Shared files metadata
- **LocationData**: Location coordinates and metadata

---

### 4. **Firebase Management**

#### FirebaseManager
Centralized manager for all Firebase operations:
- User authentication
- User data storage
- Meeting management
- Message storage
- File metadata
- Location data handling

#### FirebaseMessagingService
Handles push notifications:
- Incoming call notifications
- Meeting reminders
- Message notifications

---

### 5. **Services (Background Tasks)**

#### CallService
- Manages call lifecycle
- Handles audio routing
- Keeps call active in background

#### NotificationService
- Displays call notifications
- Notification actions (Answer/Reject)

#### LocationService
- Continuous location tracking
- Periodic updates to Firebase
- Battery-efficient location handling

---

### 6. **Utilities (Helper Classes)**

#### PermissionManager
- Runtime permission handling
- Permission checking methods
- Permission request builders

#### SharedPrefManager
- Local data persistence
- User preferences storage
- Session management

#### DateTimeUtils
- Date/time formatting
- Timestamp conversion
- Duration calculation

#### ValidationUtils
- Email validation
- Password strength checking
- Input validation

#### NetworkUtils
- Network connectivity checking
- API call handling
- Error handling

#### FileUtils
- File operations
- MIME type detection
- File size formatting

#### LocationUtils
- Geocoding operations
- Distance calculations
- Map interface handling

---

### 7. **Adapters (RecyclerView Adapters)**

- **MessageAdapter**: Display chat messages
- **ParticipantAdapter**: Display meeting participants
- **MeetingAdapter**: Display meetings list
- **FileAdapter**: Display shared files

---

## Setup Instructions

### Prerequisites

1. **Android Studio** (Latest version)
2. **Android SDK** (API 24 and above)
3. **Java 11** or higher
4. **Firebase Project** created at https://console.firebase.google.com
5. **Google Maps API Key**

### Step 1: Clone/Create Project

```bash
mkdir FlexMeet
cd FlexMeet
```

### Step 2: Create Android Project in Android Studio

1. Open Android Studio
2. Create New Project > Empty Activity
3. Name: FlexMeet
4. Package: com.flexmeet
5. Language: Java
6. API Level: 24
7. Build system: Gradle (Kotlin DSL)

### Step 3: Add Dependencies

1. Update `build.gradle` (Project level) with the provided content
2. Update `build.gradle` (App level) with the provided content
3. Sync Gradle

### Step 4: Firebase Setup

1. Go to [Firebase Console](https://console.firebase.google.com)
2. Create a new project named "FlexMeet"
3. Enable Authentication (Email/Password, Google)
4. Create Firestore Database
5. Create Realtime Database
6. Enable Cloud Storage
7. Enable Cloud Messaging
8. Download `google-services.json`:
   - In Firebase Console: Project Settings > Service Accounts
   - Generate New Private Key
   - Save as `google-services.json` in `android/app/`

### Step 5: Google Maps API

1. Go to Google Cloud Console
2. Create API Key
3. Add to AndroidManifest.xml:

```xml
<meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="YOUR_API_KEY" />
```

### Step 6: Update Configuration

Update `AppConstants.java`:

```java
public static final String MAPS_API_KEY = "YOUR_MAPS_API_KEY";
public static final String FIREBASE_DATABASE_URL = "YOUR_FIREBASE_DATABASE_URL";
```

### Step 7: Build and Run

```bash
./gradlew build
./gradlew installDebug
```

Or run from Android Studio: `Run > Run 'app'`

---

## Firebase Configuration

### Firestore Rules

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Allow read/write for users' own documents
    match /users/{userId} {
      allow read, write: if request.auth.uid == userId;
    }

    // Allow read for all meetings
    match /meetings/{meetingId} {
      allow read: if true;
      allow create: if request.auth != null;
      allow update, delete: if resource.data.hostId == request.auth.uid;

      match /{document=**} {
        allow read, write: if request.auth != null;
      }
    }
  }
}
```

### Realtime Database Rules

```json
{
  "rules": {
    "meetings": {
      "{meetingId}": {
        "locations": {
          "{userId}": {
            ".read": true,
            ".write": "root.child('meetings').child($meetingId).child('participants').hasChild(auth.uid)"
          }
        }
      }
    }
  }
}
```

---

## Dependency Management

All dependencies are managed in `build.gradle`. Key dependencies:

- **Firebase Admin SDK**: For backend authentication
- **Material Components**: For UI elements
- **WebRTC**: For video/audio communication
- **Google Maps API**: For location services
- **Glide**: For image loading
- **Gson**: For JSON serialization
- **OkHttp**: For network requests
- **Timber**: For logging

---

## Feature Modules

### 1. Authentication Module

**Flow:**
1. User opens app → SplashActivity checks login status
2. If not logged in → LoginActivity
3. User enters credentials → FirebaseManager.loginUser()
4. Firebase authenticates → SharedPrefManager saves session
5. Navigate to DashboardActivity

**Key Files:**
- `LoginActivity.java`
- `RegisterActivity.java`
- `FirebaseManager.java` (auth methods)

### 2. Meeting Management Module

**Flow:**
1. User taps "Start Meeting" → DashboardActivity
2. Create meeting → create() method generates meeting ID
3. Meeting data → saved to Firestore
4. Meeting code generated → shared with users
5. Participants join → added to participants subcollection

**Key Files:**
- `DashboardActivity.java`
- `ScheduleMeetingActivity.java`
- `FirebaseManager.java` (meeting methods)

### 3. Video Call Module

**Flow:**
1. User joins meeting → enters VideoCallActivity
2. WebRTC initialization → creates peer connections
3. Local stream → captured from camera
4. Signaling → messages exchanged via Firebase
5. Remote stream → received and displayed

**Key Files:**
- `VideoCallActivity.java`
- `CallService.java`
- WebRTC helper classes (to be implemented)

### 4. Chat Module

**Flow:**
1. User opens ChatFragment during call
2. Types message → sends via Firebase
3. Message stored in Firestore → messages subcollection
4. Realtime listener → receives new messages
5. Display in ChatFragment

**Key Files:**
- `ChatFragment.java`
- `MessageAdapter.java`
- `FirebaseManager.java` (message methods)

### 5. File Sharing Module

**Flow:**
1. User selects file → picks from device storage
2. File uploaded → to Firebase Storage
3. File metadata → saved to Firestore
4. Other participants → see file in FileShareFragment
5. Download file → via download link in Firebase Storage

**Key Files:**
- `FileShareFragment.java`
- `FileTransferActivity.java`
- `FirebaseManager.java` (file methods)

### 6. Location Sharing Module

**Flow:**
1. User enables location sharing
2. LocationService → starts location updates
3. Periodic updates → saved to Realtime Database
4. Real-time listeners → fetch other users' locations
5. Display on map → LocationSharingActivity

**Key Files:**
- `LocationSharingActivity.java`
- `LocationShareFragment.java`
- `LocationService.java`
- `FirebaseManager.java` (location methods)

### 7. Notes Module

**Flow:**
1. User opens NotesFragment during call
2. Types notes → auto-saves to Firestore
3. Real-time sync → across all participants
4. Edit notes → update timestamp
5. Save after meeting → for reference

**Key Files:**
- `NotesFragment.java`
- `FirebaseManager.java` (notes methods)

---

## Permissions Handling

### AndroidManifest.xml Permissions

```xml
<!-- Dangerous Permissions (require runtime requests from Android 6.0+) -->
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />

<!-- Normal Permissions (granted automatically) -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

### Runtime Permission Requests

In Activities:

```java
@Override
public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    
    if (requestCode == PermissionManager.ALL_PERMISSIONS_CODE) {
        if (PermissionManager.handlePermissionResult(grantResults)) {
            // All permissions granted
            startVideoCall();
        } else {
            // Some permissions denied
            Toast.makeText(this, "Permissions denied", Toast.LENGTH_SHORT).show();
        }
    }
}
```

---

## API Integration

### Firebase REST API (Optional)

For backend operations not covered by SDK:

```java
public void callFirebaseAPI(String endpoint, Object data) {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl("https://[YOUR-PROJECT].firebaseio.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build();
    
    // Define API service interface
    // Make API calls
}
```

---

## WebRTC Implementation

### Peer Connection Setup

```java
private void initPeerConnection(String peerId) {
    PeerConnectionFactory.InitializationOptions options = 
        PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(true)
            .createInitializationOptions();
    
    PeerConnectionFactory.initialize(options);
    
    peerConnectionFactory = PeerConnectionFactory.builder()
        .setVideoEncoderFactory(new DefaultVideoEncoderFactory(...))
        .setVideoDecoderFactory(new DefaultVideoDecoderFactory(...))
        .createPeerConnectionFactory();
    
    // Create peer connection with STUN/TURN servers
    PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(
        Arrays.asList(
            new PeerConnection.IceServer("stun:stun.l.google.com:19302")
        )
    );
    
    PeerConnection peerConnection = peerConnectionFactory.createPeerConnection(
        rtcConfig, 
        new PeerConnectionObserver()
    );
}
```

---

## Best Practices

### Code Quality

1. **Use Timber for Logging**
   ```java
   Timber.d("Debug message");
   Timber.e(exception, "Error message");
   ```

2. **Handle Exceptions Gracefully**
   ```java
   try {
       // Operation
   } catch (Exception e) {
       Timber.e(e, "Operation failed");
       showErrorMessage("An error occurred");
   }
   ```

3. **Use Constants**
   - All hardcoded values in `AppConstants.java`
   - All strings in `strings.xml`
   - All colors in `colors.xml`

### Performance Optimization

1. **Use RecyclerView** for lists
2. **Lazy load images** with Glide
3. **Manage lifecycle** properly
4. **Close resources** in onDestroy()
5. **Use background threads** for heavy operations

### Security

1. **Validate all inputs** before processing
2. **Use HTTPS** for all network calls
3. **Never expose sensitive data** in logs
4. **Implement proper authentication**
5. **Use Firebase Security Rules**

### Testing

```java
// Unit Test Example
@Test
public void testUserValidation() {
    assertTrue(ValidationUtils.isValidEmail("test@example.com"));
    assertFalse(ValidationUtils.isValidEmail("invalid"));
}
```

---

## Development Workflow

1. **Create Layout** (`res/layout/activity_*.xml`)
2. **Create Activity/Fragment** (`activities/` or `fragments/`)
3. **Implement UI Logic** in Activity/Fragment
4. **Add Firebase Operations** in FirebaseManager
5. **Handle Permissions** in Activity
6. **Test Thoroughly** before deployment

---

## Troubleshooting

### Build Errors

- **Gradle sync failed**: Clean and rebuild
  ```bash
  ./gradlew clean
  ./gradlew build
  ```

### Runtime Errors

- **Firebase not initialized**: Check `google-services.json` placement
- **Permission denied**: Request permissions before accessing resources
- **WebRTC not working**: Verify STUN server availability

### Firebase Issues

- **Firestore rules error**: Update Security Rules in Firebase Console
- **Storage quota exceeded**: Check file upload size limits
- **Authentication failed**: Verify Firebase Authentication methods

---

## Next Steps

1. Implement remaining Activities mentioned in folder structure
2. Create XML layout files for all screens
3. Implement WebRTC signaling using Firebase Realtime Database
4. Add WebRTC audio/video streaming
5. Implement location tracking using Google Maps API
6. Add push notifications using Firebase Cloud Messaging
7. Comprehensive testing and bug fixes
8. Deploy to Google Play Store

---

## Support & Resources

- [Firebase Documentation](https://firebase.google.com/docs)
- [Android Developers](https://developer.android.com)
- [WebRTC Documentation](https://webrtc.org/getting-started/overview)
- [Google Maps API](https://developers.google.com/maps/documentation)

---

## License

This project is licensed under the MIT License.

---

## Authors

FlexMeet Development Team

---

**Last Updated**: March 4, 2026
