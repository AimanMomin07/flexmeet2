# FlexMeet Android - Quick Reference Guide

A quick lookup guide for common tasks, code snippets, and important information while developing FlexMeet.

---

## 🚀 Quick Commands

### Build Commands
```bash
# Clean build
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# Build debug and install
./gradlew installDebug

# Build fresh (clean + build)
./gradlew clean build

# Build without cache
./gradlew build --no-build-cache

# Run on device
./gradlew runDebug
```

### Emulator Commands
```bash
# List emulators
emulator -list-avds

# Start emulator
emulator -avd Pixel_6_API_33

# List connected devices
adb devices

# Clear app data
adb shell pm clear com.flexmeet

# Reinstall app
adb uninstall com.flexmeet && ./gradlew installDebug

# View logs
adb logcat
```

---

## 📁 File Locations Quick Reference

| Component | Location |
|-----------|----------|
| Main Application | `src/main/java/com/flexmeet/FlexMeetApplication.java` |
| Models | `src/main/java/com/flexmeet/models/` |
| Firebase Manager | `src/main/java/com/flexmeet/firebase/FirebaseManager.java` |
| Permissions | `src/main/java/com/flexmeet/utils/PermissionManager.java` |
| Constants | `src/main/java/com/flexmeet/constants/AppConstants.java` |
| Strings | `src/main/res/values/strings.xml` |
| Colors | `src/main/res/values/colors.xml` |
| Styles | `src/main/res/values/styles.xml` |
| Layouts | `src/main/res/layout/` |
| Manifest | `src/main/AndroidManifest.xml` |
| Gradle Config | `build.gradle` (app level) |
| Firebase Config | `google-services.json` |

---

## 🔥 Firebase Quick Snippets

### Get Firebase Manager Instance
```java
FirebaseManager firebaseManager = FirebaseManager.getInstance();
```

### Register User
```java
FirebaseManager.getInstance().registerUser(
    email, 
    password, 
    displayName,
    task -> {
        if (task.isSuccessful()) {
            Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Registration failed: " + task.getException().getMessage(), 
                         Toast.LENGTH_SHORT).show();
        }
    });
```

### Login User
```java
FirebaseManager.getInstance().loginUser(
    email,
    password,
    task -> {
        if (task.isSuccessful()) {
            startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
            finish();
        } else {
            Toast.makeText(this, "Login failed", Toast.LENGTH_SHORT).show();
        }
    });
```

### Save User Data
```java
User user = new User();
user.setUid(userId);
user.setEmail(email);
user.setDisplayName(displayName);

FirebaseManager.getInstance().saveUser(user, task -> {
    if (task.isSuccessful()) {
        Timber.d("User saved successfully");
    } else {
        Timber.e(task.getException(), "Failed to save user");
    }
});
```

### Create Meeting
```java
Meeting meeting = new Meeting();
meeting.setMeetingId(UUID.randomUUID().toString());
meeting.setTitle("Quick Meeting");
meeting.setHostId(userId);
meeting.setHostName(userName);

FirebaseManager.getInstance().createMeeting(meeting, task -> {
    if (task.isSuccessful()) {
        Timber.d("Meeting created: " + meeting.getMeetingId());
    }
});
```

### Send Message
```java
Message message = new Message();
message.setMessageId(UUID.randomUUID().toString());
message.setMeetingId(meetingId);
message.setSenderId(userId);
message.setSenderName(userName);
message.setText(messageText);
message.setTimestamp(System.currentTimeMillis());

FirebaseManager.getInstance().sendMessage(meetingId, message, task -> {
    if (task.isSuccessful()) {
        Timber.d("Message sent");
    }
});
```

### Upload File
```java
Uri fileUri = Uri.parse(filePath);
FirebaseManager.getInstance().uploadFile(fileUri, fileName, task -> {
    if (task.isSuccessful()) {
        String downloadUrl = task.getResult().toString();
        Timber.d("File uploaded: " + downloadUrl);
    } else {
        Timber.e(task.getException(), "Upload failed");
    }
});
```

---

## 🔐 Permission Quick Snippets

### Check Single Permission
```java
if (PermissionManager.hasCameraPermission()) {
    // Camera permission granted
    startCamera();
} else {
    PermissionManager.requestCameraPermission(this);
}
```

### Request All Video Call Permissions
```java
if (!PermissionManager.hasVideoCallPermissions()) {
    PermissionManager.requestVideoCallPermissions(this);
} else {
    startVideoCall();
}
```

### Handle Permission Result
```java
@Override
public void onRequestPermissionsResult(int requestCode, 
        @NonNull String[] permissions, 
        @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    
    if (PermissionManager.handlePermissionResult(grantResults)) {
        // Permissions granted
        proceedWithFeature();
    } else {
        // Permissions denied
        Toast.makeText(this, "Permissions required for this feature", Toast.LENGTH_SHORT).show();
    }
}
```

---

## 🎨 UI/Layout Quick Snippets

### Material Button
```xml
<com.google.android.material.button.MaterialButton
    android:id="@+id/buttonLogin"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:text="@string/button_login"
    app:cornerRadius="8dp"
    android:layout_margin="16dp" />
```

### Material Text Input
```xml
<com.google.android.material.textfield.TextInputLayout
    android:id="@+id/emailInput"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:hint="@string/hint_email"
    android:layout_margin="16dp">
    
    <com.google.android.material.textfield.TextInputEditText
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:inputType="textEmailAddress" />
</com.google.android.material.textfield.TextInputLayout>
```

### Material Card View
```xml
<com.google.android.material.card.MaterialCardView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_margin="8dp"
    app:cardCornerRadius="12dp"
    app:cardElevation="4dp">
    
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="16dp">
        
        <!-- Content here -->
    </LinearLayout>
</com.google.android.material.card.MaterialCardView>
```

### RecyclerView
```xml
<androidx.recyclerview.widget.RecyclerView
    android:id="@+id/recyclerMessages"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    app:layoutManager="androidx.recyclerview.widget.LinearLayoutManager" />
```

---

## 🎬 Fragment Quick Snippets

### Create Fragment Instance
```java
ChatFragment fragment = new ChatFragment();
Bundle args = new Bundle();
args.putString("meetingId", meetingId);
fragment.setArguments(args);
```

### Add Fragment to Activity
```java
getSupportFragmentManager()
    .beginTransaction()
    .add(R.id.fragmentContainer, fragment)
    .addToBackStack(null)
    .commit();
```

### Replace Fragment
```java
getSupportFragmentManager()
    .beginTransaction()
    .replace(R.id.fragmentContainer, newFragment)
    .addToBackStack(null)
    .commit();
```

### Get Bundle Arguments
```java
@Override
public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
    if (getArguments() != null) {
        String meetingId = getArguments().getString("meetingId");
    }
}
```

---

## 📱 Activity Quick Snippets

### Start Another Activity
```java
Intent intent = new Intent(this, VideoCallActivity.class);
intent.putExtra("meetingId", meetingId);
intent.putExtra("meetingCode", meetingCode);
startActivity(intent);
```

### Get Intent Extras
```java
Intent intent = getIntent();
String meetingId = intent.getStringExtra("meetingId");
int participantCount = intent.getIntExtra("participantCount", 0);
```

### Send Result Back to Parent Activity
```java
Intent resultIntent = new Intent();
resultIntent.putExtra("resultKey", resultValue);
setResult(RESULT_OK, resultIntent);
finish();
```

### Request Result from Activity
```java
startActivityForResult(intent, REQUEST_CODE_CAMERA);

@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    
    if (requestCode == REQUEST_CODE_CAMERA && resultCode == RESULT_OK) {
        String result = data.getStringExtra("resultKey");
    }
}
```

---

## 🛠️ Logging Quick Snippets

### Debug Log
```java
Timber.d("Debug message: %s", variableName);
```

### Error Log
```java
Timber.e(exception, "Error occurred: %s", errorMessage);
```

### Warning Log
```java
Timber.w("Warning: %s", warningMessage);
```

### Info Log
```java
Timber.i("Info message: %s", info);
```

### Log with Tag
```java
Timber.tag("MyTag").d("Tagged debug message");
```

---

## 🎯 Important Constants

### Collection Names (in Firebase)
```java
AppConstants.COLLECTION_USERS      // "users"
AppConstants.COLLECTION_MEETINGS   // "meetings"
AppConstants.COLLECTION_MESSAGES   // "messages"
AppConstants.COLLECTION_NOTES      // "notes"
AppConstants.COLLECTION_FILES      // "files"
```

### Status Values
```java
AppConstants.STATUS_SCHEDULED      // "scheduled"
AppConstants.STATUS_ONGOING        // "ongoing"
AppConstants.STATUS_ENDED          // "ended"

AppConstants.USER_STATUS_ONLINE    // "online"
AppConstants.USER_STATUS_OFFLINE   // "offline"
AppConstants.USER_STATUS_BUSY      // "busy"
AppConstants.USER_STATUS_AWAY      // "away"
```

### Message Types
```java
AppConstants.MESSAGE_TYPE_TEXT     // "text"
AppConstants.MESSAGE_TYPE_IMAGE    // "image"
AppConstants.MESSAGE_TYPE_FILE     // "file"
AppConstants.MESSAGE_TYPE_LOCATION // "location"
```

### WebRTC Settings
```java
AppConstants.MAX_VIDEO_WIDTH       // 720
AppConstants.MAX_VIDEO_HEIGHT      // 1280
AppConstants.VIDEO_FPS             // 30
```

### Request Codes
```java
AppConstants.REQUEST_CODE_CAMERA           // 101
AppConstants.REQUEST_CODE_MICROPHONE       // 102
AppConstants.REQUEST_CODE_LOCATION         // 103
AppConstants.REQUEST_CODE_STORAGE          // 104
AppConstants.REQUEST_CODE_ALL_PERMISSIONS  // 105
```

---

## 🎨 Color Reference

| Color | Hex Code | Usage |
|-------|----------|-------|
| Primary Blue | #1A73E8 | Buttons, headers, active states |
| Secondary Gray | #5F6368 | Text, icons, secondary elements |
| Light Gray | #F8F9FA | Backgrounds, cards |
| Dark Gray | #202124 | Dark text, dark theme |
| Success Green | #34A853 | Success messages, positive states |
| Error Red | #D33B27 | Error messages, warnings |
| Call End Red | #DC3545 | End call button |
| Mute Gray | #6C757D | Muted state |

Access in code:
```java
int color = ContextCompat.getColor(this, R.color.primary_blue);
```

---

## 📊 SharedPreferences Quick Snippets

### Save Data
```java
SharedPreferences pref = getSharedPreferences("app_pref", MODE_PRIVATE);
SharedPreferences.Editor editor = pref.edit();
editor.putString("user_id", userId);
editor.putBoolean("is_logged_in", true);
editor.apply();
```

### Retrieve Data
```java
SharedPreferences pref = getSharedPreferences("app_pref", MODE_PRIVATE);
String userId = pref.getString("user_id", "");
boolean isLoggedIn = pref.getBoolean("is_logged_in", false);
```

### Clear Data
```java
SharedPreferences pref = getSharedPreferences("app_pref", MODE_PRIVATE);
pref.edit().clear().apply();
```

---

## 🔔 Notification Quick Snippets

### Create Notification Builder
```java
NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "CHANNEL_ID")
    .setSmallIcon(R.drawable.ic_notification)
    .setContentTitle("Title")
    .setContentText("Text")
    .setPriority(NotificationCompat.PRIORITY_HIGH)
    .setAutoCancel(true);
```

### Show Notification
```java
NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
notificationManager.notify(notificationId, builder.build());
```

### Create Notification Channel (Android 8+)
```java
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    NotificationChannel channel = new NotificationChannel(
        channelId,
        "Channel Name",
        NotificationManager.IMPORTANCE_HIGH
    );
    channel.setDescription("Channel description");
    
    NotificationManager manager = getSystemService(NotificationManager.class);
    manager.createNotificationChannel(channel);
}
```

---

## 🌐 Network Quick Snippets

### Check Internet Connection
```java
ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
```

### Retrofit API Call
```java
Retrofit retrofit = new Retrofit.Builder()
    .baseUrl(BASE_URL)
    .addConverterFactory(GsonConverterFactory.create())
    .build();

ApiService service = retrofit.create(ApiService.class);
Call<ResponseObject> call = service.getEndpoint();
call.enqueue(new Callback<ResponseObject>() {
    @Override
    public void onResponse(Call<ResponseObject> call, Response<ResponseObject> response) {
        if (response.isSuccessful()) {
            ResponseObject data = response.body();
            // Handle success
        }
    }
    
    @Override
    public void onFailure(Call<ResponseObject> call, Throwable t) {
        Timber.e(t, "API call failed");
    }
});
```

---

## 🚨 Common Errors & Solutions

| Error | Solution |
|-------|----------|
| "google-services.json is missing" | Place `google-services.json` in `android/app/` directory |
| "Gradle sync failed" | Run `./gradlew clean`, restart Android Studio |
| "Firebase not initialized" | Ensure `FlexMeetApplication` extends `Application` class |
| "Permission denied" | Check `AndroidManifest.xml` has permission declared |
| "Activity not declared" | Add activity declaration to `AndroidManifest.xml` |
| "Cannot find symbol" | Check import statements, run `./gradlew clean` |
| "API key invalid" | Verify Google Maps API key in `AndroidManifest.xml` |
| "Firestore rules error" | Update database rules in Firebase Console |

---

## 📚 Documentation Links in Project

| Document | Location | Purpose |
|----------|----------|---------|
| Getting Started | [GETTING_STARTED.md](GETTING_STARTED.md) | Quick setup (5-10 min) |
| Implementation Guide | [android/IMPLEMENTATION_GUIDE.md](android/IMPLEMENTATION_GUIDE.md) | Comprehensive guide |
| Firebase Schema | [android/FIREBASE_SCHEMA.md](android/FIREBASE_SCHEMA.md) | Database structure |
| Project Summary | [PROJECT_SUMMARY.md](PROJECT_SUMMARY.md) | Project overview |
| Developer Checklist | [DEVELOPER_CHECKLIST.md](DEVELOPER_CHECKLIST.md) | Setup checklist |
| README | [README.md](README.md) | Project readme |

---

## 🎯 Development Tips

### Use Timber Everywhere
```java
// Bad
System.out.println("Debug info");

// Good
Timber.d("Debug info");
```

### Use Constants, Not Hardcoded Strings
```java
// Bad
startActivity(new Intent(this, "VideoCallActivity"));

// Good
Intent intent = new Intent(this, VideoCallActivity.class);
startActivity(intent);
```

### Handle Exceptions Properly
```java
// Bad
task.getResult(); // Might crash

// Good
if (task.isSuccessful()) {
    Object result = task.getResult();
} else {
    Timber.e(task.getException(), "Operation failed");
}
```

### Use ContextCompat for Colors
```java
// Deprecated
int color = getResources().getColor(R.color.primary_blue);

// Current
int color = ContextCompat.getColor(this, R.color.primary_blue);
```

### Always Check Null
```java
// Bad
String value = data.getValue(); // Might be null
int length = value.length();    // Crash!

// Good
String value = data.getValue();
if (value != null) {
    int length = value.length();
}
```

---

## 📞 Quick Links

- **Firebase Console**: https://console.firebase.google.com
- **Google Cloud Console**: https://console.cloud.google.com
- **Android Docs**: https://developer.android.com
- **Firebase Android**: https://firebase.google.com/docs/android
- **Material Design**: https://material.io
- **Android API Reference**: https://developer.android.com/reference

---

## ✨ Code Snippets Cheat Sheet

**File Structure Template:**
```java
package com.flexmeet.features;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import timber.log.Timber;

public class MyActivity extends AppCompatActivity {
    
    private static final String TAG = "MyActivity";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);
        
        Timber.d("onCreate called");
    }
}
```

**Fragment Template:**
```java
public class MyFragment extends Fragment {
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my, container, false);
    }
    
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Setup UI here
    }
}
```

---

## 🎓 Learning Resources

1. **Android Official Training**: https://developer.android.com/training
2. **Firebase Codelabs**: https://codelabs.developers.google.com
3. **Android Architecture Patterns**: https://developer.android.com/topic/architecture
4. **WebRTC Documentation**: https://webrtc.org/getting-started

---

**This Quick Reference Guide is optimized for quick lookups while coding.**

**Current Version**: 1.0  
**Last Updated**: March 4, 2026  
**For**: FlexMeet Android Developers
