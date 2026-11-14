# Bbiso Backend

Node.js/Express API for the Bbiso Android app.

## Environment
- `MONGO_URI`: MongoDB Atlas connection string for database `bbiso`.
- `JWT_SECRET`: long random secret (32+ characters).
- `CORS_ORIGIN`: optional comma-separated allowed origins.

## Run
```bash
npm install
npm start
```

Health check: `GET /health`

Auth: `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/auth/me` (Bearer token).
Forum: `GET /api/forum`, `POST /api/forum`, `POST /api/forum/:id/replies`.
Media metadata: `GET /api/media`, `POST /api/media`.
