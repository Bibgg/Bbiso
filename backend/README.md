# Bbiso Backend

Node.js/Express API for the Bbiso Android app.

## Environment
- `MONGO_URI`: MongoDB Atlas connection string for database `bbiso`.
- `JWT_SECRET`: long random secret (32+ characters) for legacy backend sessions.
- `CORS_ORIGIN`: optional comma-separated allowed origins.
- `FIREBASE_PROJECT_ID`: Firebase project ID used to verify Android Firebase ID tokens.
- `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET`: Cloudinary credentials. The API secret must exist only in Render environment variables, never in the Android app or Git repository.

## Run
```bash
npm install
npm start
npm test
```

Health check: `GET /health`

Auth: `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/auth/me` (legacy backend Bearer token).

Forum: `GET /api/forum`, `POST /api/forum`, `POST /api/forum/:id/replies`.

## Signed media upload
`POST /api/media/upload-signature` requires a Firebase ID token in the Bearer header. JSON body:

```json
{ "mediaType": "video", "purpose": "post" }
```

The response contains a signature scoped to the authenticated Firebase UID and either the `posts` or `stories` folder. Android uploads media bytes directly to Cloudinary; Render does not proxy the video file. The Android client must enforce `maxBytes`, use the returned `resourceType`, and publish post/story metadata only after Cloudinary confirms upload success.

The connected Cloudinary Free account currently reports a 100 MiB video upload maximum and a 10 MiB image upload maximum. Account quotas and limits may change.
