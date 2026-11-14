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

  const timestamp = Math.floor(now() / 1000);
  const folder = `bbiso/users/${uid}/${purpose}s`;
  const publicId = uuid();
  const allowedFormats = mediaType === "video" ? VIDEO_FORMATS : IMAGE_FORMATS;
  const resourceType = mediaType === "video" ? "video" : "image";
  const parameters = { folder, public_id: publicId, timestamp, allowed_formats: allowedFormats };

  return {
    cloudName,
    apiKey,
    timestamp,
    signature: createCloudinarySignature(parameters, apiSecret),
    folder,
    publicId,
    resourceType,
    allowedFormats,
    maxBytes: mediaType === "video" ? VIDEO_MAX_BYTES : IMAGE_MAX_BYTES
  };
}

module.exports = { createCloudinarySignature, createUploadTicket, VIDEO_FORMATS, IMAGE_FORMATS, VIDEO_MAX_BYTES, IMAGE_MAX_BYTES };
