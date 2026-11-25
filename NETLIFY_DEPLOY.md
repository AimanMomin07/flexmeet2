# FlexMeet deployment

The current web deployment target is Vercel. Follow [VERCEL_DEPLOY.md](VERCEL_DEPLOY.md) for the Vercel static site, API, and LiveKit configuration.

The repository still includes `netlify.toml` for teams that choose Netlify instead. The client remains in `src/main/resources/static`; shared links use the deployed site origin and `/join/<room-code>`.

The Spring room API and LiveKit token endpoints are implemented. They require a separately hosted HTTPS API, valid Firebase-backed sign-in, a production JWT signing secret, LiveKit server credentials, and an exact CORS origin. The room registry currently uses in-memory state, so it is a development integration and must move to durable shared storage before production.
