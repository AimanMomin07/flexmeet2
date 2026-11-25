# FlexMeet deployment checklist

## Android meetings

The Android client now calls the Spring API directly and gets a short-lived LiveKit participant token from it. The APK must never contain the LiveKit API secret.

Set these server environment values before deploying the Spring service:

- `LIVEKIT_URL`: the LiveKit WebSocket URL (for example, `wss://your-livekit-host`)
- `LIVEKIT_API_KEY` and `LIVEKIT_API_SECRET`: credentials from the LiveKit project
- `FIREBASE_DATABASE_URL`: the exact Realtime Database URL shown in Firebase console
- `FIREBASE_SERVICE_ACCOUNT_PATH`: path to the server-only Firebase Admin service account JSON, or configure Google Application Default Credentials

Deploy the Spring service at a stable public HTTPS URL. On first host or join, enter that service URL in the app. Enter the origin with or without its `/api` context path; the client normalizes it.

Room state is currently held in the Spring process memory. Run one backend instance for this version; restarting it clears room metadata. A production multi-instance deployment needs a shared room store and expiry policy.

## Android App Links and web fallback

Redeploy the Vercel site after publishing `src/main/resources/static/.well-known/assetlinks.json`. Replace/add the SHA-256 fingerprint for the Play signing certificate (and any internal-test signing certificate). Android links open the native call screen when the app is installed. Without the app, the browser route must remain publicly accessible.

Disable Vercel Deployment Protection for the public invite route or configure an access policy that permits guests. The app cannot make a protected deployment public.

The current repository has no iOS app target. On iPhone, invite links can open the web experience; a native iOS app requires a separate iOS project and the LiveKit iOS SDK.

## Friend chat

User profiles, friend requests, accepted contacts, and messages use Firebase Realtime Database. On registration and login, FlexMeet writes a minimal public directory profile. Existing users need to sign in once after this update before they can be found by email.

Merge the paths and rules in `firebase-chat.rules.example.json` with your existing Realtime Database rules. Do not replace other meeting, signaling, or profile rules without merging them. The rules limit directory searches to exact email lookups and require reciprocal accepted contacts to read or write direct messages.

The Explore tab shows community rooms only when a host selects “Show this room in Live now”. It is a live room directory, not a third-party events/news feed.
