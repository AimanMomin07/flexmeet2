# FlexMeet Android - Getting Started Guide

## ⚡ Quick Setup (5 minutes)

This guide walks you through the essential steps to get FlexMeet running on your device.

---

## 1. Prerequisites Check

Before you start, make sure you have:

- ✅ **Android Studio** installed (version 2024.1 or later)
- ✅ **Android SDK** API 24+ (download via SDK Manager in Android Studio)
- ✅ **Java 11+** (Android Studio bundles this)
- ✅ **Firebase Account** (free at https://console.firebase.google.com)
- ✅ **Google Cloud Account** (free at https://console.cloud.google.com)

---

## 2. Firebase Project Setup (10 minutes)

### Create Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com)
2. Click **Create Project** → Name it "FlexMeet"
3. Follow the prompts (disable Analytics for now)
4. Click **Create Project**

### Enable Required Services

Once your project is created:

1. **Enable Authentication**
   - Navigate to **Authentication** (left sidebar)
   - Click **Get Started**
   - Enable **Email/Password** provider
   - Enable **Google** provider (optional)
   - Save

2. **Create Firestore Database**
   - Click **Firestore Database** (left sidebar)
   - Click **Create Database**
   - Select **Start in test mode** (for development)
   - Choose region closest to you
   - Click **Enable**

3. **Create Realtime Database**
   - Click **Realtime Database** (left sidebar)
   - Click **Create Database**
   - Select same region as Firestore
   - Choose **Start in test mode**
   - Click **Enable**

4. **Enable Cloud Storage**
   - Click **Storage** (left sidebar)
   - Click **Get Started**
   - Choose same region
   - Click **Done**

5. **Enable Cloud Messaging (Optional)**
   - Click **Cloud Messaging** (left sidebar)
   - Note your Server API Key (you'll need this later)

---

## 3. Download google-services.json

1. In Firebase Console, go to **Project Settings** (⚙️ icon, top-right)
2. Click **Your Apps** section
3. Click **Add App** → Select **Android**
4. Enter Package Name: `com.flexmeet`
5. Click **Register App**
6. Click **Download google-services.json**
7. **Important**: Save this file to `android/app/` directory in the project

---

## 4. Get Google Maps API Key (Optional, for location features)

1. Go to [Google Cloud Console](https://console.cloud.google.com)
2. Create a new project or use existing one
3. Go to **APIs & Services** → **Library**
4. Search for **Maps SDK for Android**
5. Click → **Enable**
6. Go to **Credentials** → **Create Credentials** → **API Key**
7. Copy your API Key
8. Open `android/app/src/main/AndroidManifest.xml`
9. Find this line:
   ```xml
   <meta-data
       android:name="com.google.android.geo.API_KEY"
       android:value="YOUR_API_KEY" />
   ```
10. Replace `YOUR_API_KEY` with your actual key

---

## 5. Open Project in Android Studio

1. Open **Android Studio**
2. Click **Open** → Navigate to `flexmeet/android` folder
3. Open the `android` folder (not the root `flexmeet`)
4. Click **Open**
5. Wait for Gradle sync to complete (⏳ may take 2-3 minutes on first run)

---

## 6. Configure Emulator or Device

### Option A: Use Android Emulator (Recommended for beginners)

1. In Android Studio, click **Device Manager** (right sidebar)
2. Click **Create Device**
3. Select **Phone** → **Pixel 6**
4. Select **API 33** (or higher)
5. Click **Next** → **Finish**
6. Click the Play icon to start emulator

### Option B: Use Physical Android Device

1. Enable **Developer Mode** on your phone:
   - Go to **Settings** → **About Phone**
   - Tap **Build Number** 7 times
2. Return to **Settings** → **Developer Options**
3. Enable **USB Debugging**
4. Connect phone to computer via USB
5. Click **Trust** on your phone
6. Android Studio should detect your device

---

## 7. Build & Run the App

1. In Android Studio, click **Run** → **Run 'app'**
2. Or press `Shift + F10` (Windows) / `⌃ + R` (Mac)
3. Select your device/emulator
4. Click **OK**
5. Wait for compilation and deployment (⏳ 2-3 minutes first time)
6. App should launch on your device!

---

## 8. First Time Setup in App

Once the app opens:

1. **Register**: Create account with email/password
   - Example: test@example.com / password123

2. **Login**: Use the credentials you just created

3. **Grant Permissions**: When asked:
   - ✅ Allow Camera
   - ✅ Allow Microphone
   - ✅ Allow Location (for location sharing)
   - ✅ Allow Storage (for file sharing)

4. **Test Features**:
   - Click **Start Meeting** to create a video call
   - Click **Join Meeting** to join (on another device)
   - Try Chat, Notes, Files, Location sharing

---

## 9. Troubleshooting

### Gradle Sync Failed
```bash
# In Android Studio terminal:
./gradlew clean
./gradlew build
```

### Emulator Won't Start
- Check if you have enough disk space
- Try: **Device Manager** → Delete device → Create new one
- Restart Android Studio

### Firebase Not Connecting
- Verify `google-services.json` is in `android/app/` (not anywhere else)
- Check Firebase Console → Firestore → Rules (should say "allow read, write: if true" in test mode)

### App Crashes on Startup
- Check Android Studio's **Logcat** tab
- Look for red error messages
- Search error message in README or IMPLEMENTATION_GUIDE

### Camera/Microphone Not Working
- Check permissions in device **Settings** → **Apps** → **FlexMeet**
- Ensure permissions are **Allowed**
- Restart the app

### Can't Join Another Device's Meeting
- Both devices must have strong internet connection
- Use meeting code from first device
- Verify both devices have granted permissions

---

## 10. Next Steps

1. **Read the Full Docs**: Check [IMPLEMENTATION_GUIDE.md](android/IMPLEMENTATION_GUIDE.md)
2. **Explore the Code**: Open files in `android/app/src/main/java/com/flexmeet/`
3. **Customize the App**:
   - Change app name in `strings.xml`
   - Change colors in `colors.xml`
   - Modify layouts in `android/app/src/main/res/layout/`

4. **Submit to Play Store** (when ready):
   - Generate signed APK: **Build** → **Generate Signed Bundle/APK**
   - Upload to [Google Play Console](https://play.google.com/console)

---

## 11. Useful Commands

### Clean Build
```bash
cd android
./gradlew clean
./gradlew build
```

### Run on Device
```bash
./gradlew installDebug
```

### View Logs
```bash
# Android Studio Logcat tab (bottom of screen)
# Or use adb:
adb logcat
```

### Debug App
- In Android Studio: **Run** → **Debug 'app'** (or `Shift + F9`)
- Set breakpoints by clicking left of line numbers
- Use **Step Over** (F8), **Step Into** (F7), **Resume** (F9)

---

## 12. Important Project Files

| File | Purpose |
|------|---------|
| `android/app/src/main/AndroidManifest.xml` | App permissions & activities |
| `android/app/google-services.json` | Firebase configuration ⚠️ CRITICAL |
| `android/app/build.gradle` | Dependencies & build config |
| `android/build.gradle` | Project-level config |
| `android/app/src/main/res/values/strings.xml` | All UI text |
| `android/app/src/main/res/values/colors.xml` | App colors |
| `android/app/src/main/res/layout/` | UI layouts |

---

## 13. Firebase Security Rules (Important!)

**⚠️ Test Mode Rules ⚠️**

The default test rules allow anyone to read/write. For production, update these rules in Firebase Console:

**Firestore Rules:**
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Only allow authenticated users to read their own data
    match /users/{userId} {
      allow read, write: if request.auth.uid == userId;
    }
    
    // Allow read all meetings, write only if host
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

---

## 14. Need Help?

- 📖 Read: [IMPLEMENTATION_GUIDE.md](android/IMPLEMENTATION_GUIDE.md)
- 📊 Schema: [FIREBASE_SCHEMA.md](android/FIREBASE_SCHEMA.md)
- 🔍 Code: Check `android/app/src/main/java/com/flexmeet/`
- 🐛 Issues: Create issue on GitHub

---

## Summary Checklist

- ✅ Android Studio installed
- ✅ Firebase project created
- ✅ google-services.json downloaded & placed
- ✅ Google Maps API key (optional) configured
- ✅ Project opened in Android Studio
- ✅ Emulator/device ready
- ✅ App built and running
- ✅ All permissions granted in app
- ✅ Firebase rules configured

**You're all set! 🎉**

Now start exploring the app and building awesome features!

---

**Questions?** Check [IMPLEMENTATION_GUIDE.md](android/IMPLEMENTATION_GUIDE.md) for comprehensive documentation.

**Last Updated**: March 4, 2026
