const jwt = require("jsonwebtoken");

const CERTS_URL = "https://www.googleapis.com/robot/v1/metadata/x509/securetoken@system.gserviceaccount.com";
let cachedCerts = null;
let cachedCertsUntil = 0;

async function getFirebaseSigningCerts() {
  if (cachedCerts && Date.now() < cachedCertsUntil) return cachedCerts;
  const response = await fetch(CERTS_URL, { headers: { accept: "application/json" } });
  if (!response.ok) throw new Error("Unable to retrieve Firebase signing certificates");
  const certs = await response.json();
  const cacheControl = response.headers.get("cache-control") || "";
  const maxAge = Number((cacheControl.match(/max-age=(\d+)/i) || [])[1] || 300);
  cachedCerts = certs;
  cachedCertsUntil = Date.now() + Math.max(60, Math.min(maxAge, 3600)) * 1000;
  return certs;
}

async function verifyFirebaseIdToken(token, projectId) {
  if (!projectId) throw new Error("FIREBASE_PROJECT_ID is not configured");
  const decoded = jwt.decode(token, { complete: true });
  const kid = decoded && decoded.header && decoded.header.kid;
  if (!kid || decoded.header.alg !== "RS256") throw new Error("Invalid Firebase token header");
  const certs = await getFirebaseSigningCerts();
  const certificate = certs[kid];
  if (!certificate) {
    cachedCerts = null;
    cachedCertsUntil = 0;
    throw new Error("Unknown Firebase token signing key");
  }
  const payload = jwt.verify(token, certificate, {
    algorithms: ["RS256"],
    audience: projectId,
    issuer: `https://securetoken.google.com/${projectId}`
  });
  if (!payload.sub || typeof payload.sub !== "string" || payload.sub.length > 128) throw new Error("Invalid Firebase user identifier");
  return payload;
}

function requireFirebaseAuth(projectId) {
  return async function firebaseAuthMiddleware(req, res, next) {
    try {
      const header = req.headers.authorization || "";
      const token = header.startsWith("Bearer ") ? header.slice(7) : "";
      if (!token) return res.status(401).json({ error: "Firebase authentication required" });
      const payload = await verifyFirebaseIdToken(token, projectId);
      req.firebaseUser = { uid: payload.sub, email: payload.email || "" };
      return next();
    } catch (_error) {
      return res.status(401).json({ error: "Invalid or expired Firebase ID token" });
    }
  };
}

module.exports = { verifyFirebaseIdToken, requireFirebaseAuth };
