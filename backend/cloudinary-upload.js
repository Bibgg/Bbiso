const crypto = require("node:crypto");

const VIDEO_FORMATS = "mp4,mov,m4v,webm";
const IMAGE_FORMATS = "jpg,jpeg,png,webp";
const VIDEO_MAX_BYTES = 100 * 1024 * 1024;
const IMAGE_MAX_BYTES = 10 * 1024 * 1024;

function createCloudinarySignature(parameters, apiSecret) {
  if (!apiSecret) throw new Error("CLOUDINARY_API_SECRET is not configured");
  const canonical = Object.keys(parameters)
    .filter((key) => parameters[key] !== undefined && parameters[key] !== null && parameters[key] !== "")
    .sort()
    .map((key) => `${key}=${parameters[key]}`)
    .join("&");
  return crypto.createHash("sha1").update(canonical + apiSecret, "utf8").digest("hex");
}

function createUploadTicket({ uid, mediaType, purpose, env = process.env, now = Date.now, uuid = crypto.randomUUID }) {
  const cloudName = env.CLOUDINARY_CLOUD_NAME;
  const apiKey = env.CLOUDINARY_API_KEY;
  const apiSecret = env.CLOUDINARY_API_SECRET;
  if (!cloudName || !apiKey || !apiSecret) {
    const error = new Error("Cloudinary credentials are not configured");
    error.code = "CLOUDINARY_NOT_CONFIGURED";
    throw error;
  }
  if (!uid || typeof uid !== "string" || uid.length > 128) throw new Error("Invalid user identifier");
  if (!["video", "image"].includes(mediaType)) throw new Error("mediaType must be video or image");
  if (!["post", "story"].includes(purpose)) throw new Error("purpose must be post or story");

  const nowMs = now();
  const timestamp = Math.floor(nowMs / 1000);
  const folder = `bbiso/users/${uid}/${purpose === "story" ? "stories" : "posts"}`;
  const publicId = uuid();
  const allowedFormats = mediaType === "video" ? VIDEO_FORMATS : IMAGE_FORMATS;
  const resourceType = mediaType === "video" ? "video" : "image";
  const accessControl = purpose === "story"
    ? JSON.stringify([{ access_type: "anonymous", start: new Date(nowMs).toISOString(), end: new Date(nowMs + 24 * 60 * 60 * 1000).toISOString() }])
    : "";
  const parameters = { folder, public_id: publicId, timestamp, allowed_formats: allowedFormats };
  if (accessControl) parameters.access_control = accessControl;

  return {
    cloudName,
    apiKey,
    timestamp,
    signature: createCloudinarySignature(parameters, apiSecret),
    folder,
    publicId,
    resourceType,
    allowedFormats,
    accessControl,
    expiresAt: purpose === "story" ? timestamp + 24 * 60 * 60 : null,
    maxBytes: mediaType === "video" ? VIDEO_MAX_BYTES : IMAGE_MAX_BYTES
  };
}

/**
 * Verify the uploaded asset with Cloudinary's authenticated Admin API before
 * the Android client is allowed to publish its Firestore metadata.
 */
async function verifyCloudinaryAsset({
  uid, mediaType, purpose, publicId, secureUrl,
  env = process.env, fetchImpl = globalThis.fetch
}) {
  const cloudName = env.CLOUDINARY_CLOUD_NAME;
  const apiKey = env.CLOUDINARY_API_KEY;
  const apiSecret = env.CLOUDINARY_API_SECRET;
  if (!cloudName || !apiKey || !apiSecret) {
    const error = new Error("Cloudinary credentials are not configured");
    error.code = "CLOUDINARY_NOT_CONFIGURED";
    throw error;
  }
  if (!uid || typeof uid !== "string" || uid.length > 128) throw new Error("Invalid user identifier");
  if (!["video", "image"].includes(mediaType) || !["post", "story"].includes(purpose)) {
    throw new Error("Invalid media type or purpose");
  }
  if (typeof publicId !== "string" || !/^[A-Za-z0-9_-]{1,80}$/.test(publicId)) {
    throw new Error("Invalid Cloudinary public ID");
  }
  if (typeof secureUrl !== "string" || !secureUrl.startsWith("https://")) {
    throw new Error("Invalid media URL");
  }
  if (typeof fetchImpl !== "function") throw new Error("Fetch is unavailable");

  const resourceType = mediaType === "video" ? "video" : "image";
  const folder = `bbiso/users/${uid}/${purpose === "story" ? "stories" : "posts"}`;
  const endpoint = `https://api.cloudinary.com/v1_1/${encodeURIComponent(cloudName)}/resources/${resourceType}/upload/${encodeURIComponent(publicId)}`;
  const credentials = Buffer.from(`${apiKey}:${apiSecret}`).toString("base64");
  const response = await fetchImpl(endpoint, {
    method: "GET",
    headers: { Authorization: `Basic ${credentials}`, Accept: "application/json" }
  });
  if (response.status === 404) {
    const error = new Error("Uploaded media was not found in Cloudinary");
    error.code = "MEDIA_NOT_FOUND";
    throw error;
  }
  if (!response.ok) throw new Error("Cloudinary could not verify the uploaded media");

  const asset = await response.json();
  const correctPublicId = asset.public_id === publicId || asset.public_id === `${folder}/${publicId}`;
  const correctFolder = asset.asset_folder === folder || asset.public_id === `${folder}/${publicId}`;
  const maxBytes = mediaType === "video" ? VIDEO_MAX_BYTES : IMAGE_MAX_BYTES;
  const allowedFormats = (mediaType === "video" ? VIDEO_FORMATS : IMAGE_FORMATS).split(",");
  if (
    asset.resource_type !== resourceType ||
    !correctPublicId ||
    !correctFolder ||
    typeof asset.secure_url !== "string" ||
    asset.secure_url !== secureUrl ||
    !Number.isFinite(asset.bytes) ||
    asset.bytes <= 0 ||
    asset.bytes > maxBytes ||
    !allowedFormats.includes(String(asset.format || "").toLowerCase())
  ) {
    const error = new Error("Cloudinary asset does not match the signed upload ticket");
    error.code = "MEDIA_VALIDATION_FAILED";
    throw error;
  }

  return {
    verified: true,
    publicId: asset.public_id,
    secureUrl: asset.secure_url,
    resourceType: asset.resource_type,
    format: asset.format,
    bytes: asset.bytes,
    durationSeconds: Number.isFinite(asset.duration) ? asset.duration : null,
    width: Number.isFinite(asset.width) ? asset.width : null,
    height: Number.isFinite(asset.height) ? asset.height : null
  };
}

module.exports = {
  createCloudinarySignature,
  createUploadTicket,
  verifyCloudinaryAsset,
  VIDEO_FORMATS,
  IMAGE_FORMATS,
  VIDEO_MAX_BYTES,
  IMAGE_MAX_BYTES
};
