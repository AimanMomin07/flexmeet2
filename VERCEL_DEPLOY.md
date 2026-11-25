# Deploy FlexMeet web on Vercel

The Vercel site at https://flexmeet-aiman-3859.vercel.app is the HTTPS web client and invite-link host. The app shell is in `src/main/resources/static`; `vercel.json` sets that output directory and rewrites `/join/<room-code>` to the PWA entry point.

Connect this repository to Vercel with the repository root as the project root. The deployed client supports iOS Safari/PWA and desktop/mobile browsers. Android opens the same client in an in-app browser and asks Android for camera or microphone access when the browser first uses those devices.

## Required services

Vercel serves static files. It does not host the Spring API or LiveKit media server. Configure a publicly reachable Spring API (HTTPS) and set its environment variables:

- `LIVEKIT_URL`: LiveKit WebSocket URL, for example `wss://your-project.livekit.cloud`
- `LIVEKIT_API_KEY`
- `LIVEKIT_API_SECRET`
- `FLEXMEET_WEB_ORIGIN=https://flexmeet-aiman-3859.vercel.app`
- A production `JWT_SECRET`/JWT signing secret with sufficient entropy (the checked-in development default must be replaced)

The URL supplied for this project is a Vercel web origin. It is not itself a LiveKit media URL; the API needs the separate LiveKit WebSocket URL and credentials. Never place LiveKit credentials in Vercel client files, the APK, or a public repository.

Open the settings gear on the web app and enter the Spring API base URL ending in `/api`. It is saved in that browser's local storage. The configured API must allow CORS from the exact Vercel origin. The room REST endpoints and LiveKit token minting are implemented in the Spring service.

## Current server limitations

The room owner/mode/permission registry is in memory. Rooms disappear when the API restarts and multiple API instances will not share room state. Before a public launch, move this state to durable shared storage, replace the in-memory H2 database with a managed database, configure rate limiting and account recovery, and validate deployment secrets. Participant capacity depends on your LiveKit plan and server resources; no service can promise unlimited participants.

## Android app links

The static client includes `/.well-known/assetlinks.json` for the local Android debug signing certificate. Vercel must redeploy this file before Android will verify invite links for that debug build. Before publishing, add the Play App Signing release certificate fingerprint to the same file. iOS users can open invite links directly in Safari or add the PWA to their home screen.
