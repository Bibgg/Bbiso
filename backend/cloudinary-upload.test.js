const test = require("node:test");
const assert = require("node:assert/strict");
const crypto = require("node:crypto");
const { createCloudinarySignature, createUploadTicket, verifyCloudinaryAsset } = require("./cloudinary-upload");

test("Cloudinary signature sorts parameters and excludes empty values", () => {
  const params = { timestamp: 123, folder: "bbiso/users/u1/posts", unused: "", public_id: "asset-1" };
  const expected = crypto.createHash("sha1")
    .update("folder=bbiso/users/u1/posts&public_id=asset-1&timestamp=123secret", "utf8")
    .digest("hex");
  assert.equal(createCloudinarySignature(params, "secret"), expected);
});

test("upload ticket scopes asset path to authenticated UID and requested purpose", () => {
  const ticket = createUploadTicket({
    uid: "firebase-user-123",
    mediaType: "video",
    purpose: "post",
    env: { CLOUDINARY_CLOUD_NAME: "demo-cloud", CLOUDINARY_API_KEY: "12345", CLOUDINARY_API_SECRET: "secret" },
    now: () => 1700000000000,
    uuid: () => "fixed-asset-id"
  });
  assert.equal(ticket.folder, "bbiso/users/firebase-user-123/posts");
  assert.equal(ticket.publicId, "fixed-asset-id");
  assert.equal(ticket.resourceType, "video");
  assert.equal(ticket.maxBytes, 104857600);
  assert.equal(ticket.timestamp, 1700000000);
  assert.ok(!JSON.stringify(ticket).includes("secret"));
});

test("upload ticket rejects invalid media types, purposes and missing credentials", () => {
  const env = { CLOUDINARY_CLOUD_NAME: "demo", CLOUDINARY_API_KEY: "key", CLOUDINARY_API_SECRET: "secret" };
  assert.throws(() => createUploadTicket({ uid: "u1", mediaType: "raw", purpose: "post", env }));
  assert.throws(() => createUploadTicket({ uid: "u1", mediaType: "image", purpose: "profile", env }));
  assert.throws(() => createUploadTicket({ uid: "u1", mediaType: "image", purpose: "post", env: {} }), { code: "CLOUDINARY_NOT_CONFIGURED" });
});

test("story ticket signs a 24-hour public access window and uses the stories folder", () => {
  const ticket = createUploadTicket({
    uid: "firebase-user-123",
    mediaType: "image",
    purpose: "story",
    env: { CLOUDINARY_CLOUD_NAME: "demo-cloud", CLOUDINARY_API_KEY: "12345", CLOUDINARY_API_SECRET: "secret" },
    now: () => 1700000000000,
    uuid: () => "story-asset-id"
  });
  assert.equal(ticket.folder, "bbiso/users/firebase-user-123/stories");
  assert.equal(ticket.expiresAt, 1700086400);
  assert.ok(ticket.accessControl.includes('"access_type":"anonymous"'));
  assert.ok(ticket.accessControl.includes("2023-11-14T22:13:20.000Z"));
  assert.ok(ticket.accessControl.includes("2023-11-15T22:13:20.000Z"));
});

test("Cloudinary verification accepts only the matching authenticated user's uploaded asset", async () => {
  let requestedUrl = "";
  let requestedAuth = "";
  const verified = await verifyCloudinaryAsset({
    uid: "firebase-user-123",
    mediaType: "video",
    purpose: "post",
    publicId: "asset-123",
    secureUrl: "https://res.cloudinary.com/demo/video/upload/asset-123.mp4",
    env: { CLOUDINARY_CLOUD_NAME: "demo", CLOUDINARY_API_KEY: "key", CLOUDINARY_API_SECRET: "secret" },
    fetchImpl: async (url, options) => {
      requestedUrl = url;
      requestedAuth = options.headers.Authorization;
      return {
        status: 200,
        ok: true,
        json: async () => ({
          public_id: "asset-123",
          asset_folder: "bbiso/users/firebase-user-123/posts",
          resource_type: "video",
          secure_url: "https://res.cloudinary.com/demo/video/upload/asset-123.mp4",
          format: "mp4",
          bytes: 1024,
          duration: 12.5,
          width: 720,
          height: 1280
        })
      };
    }
  });
  assert.match(requestedUrl, /resources\/video\/upload\/asset-123$/);
  assert.match(requestedAuth, /^Basic /);
  assert.equal(verified.verified, true);
  assert.equal(verified.bytes, 1024);
  assert.equal(verified.durationSeconds, 12.5);
});

test("Cloudinary verification rejects assets outside the caller's UID folder", async () => {
  await assert.rejects(() => verifyCloudinaryAsset({
    uid: "user-a",
    mediaType: "image",
    purpose: "post",
    publicId: "asset-123",
    secureUrl: "https://res.cloudinary.com/demo/image/upload/asset-123.jpg",
    env: { CLOUDINARY_CLOUD_NAME: "demo", CLOUDINARY_API_KEY: "key", CLOUDINARY_API_SECRET: "secret" },
    fetchImpl: async () => ({
      status: 200,
      ok: true,
      json: async () => ({
        public_id: "asset-123",
        asset_folder: "bbiso/users/user-b/posts",
        resource_type: "image",
        secure_url: "https://res.cloudinary.com/demo/image/upload/asset-123.jpg",
        format: "jpg",
        bytes: 1024
      })
    })
  }), { code: "MEDIA_VALIDATION_FAILED" });
});

test("Cloudinary verification rejects missing and oversized assets", async () => {
  const common = {
    uid: "user-a",
    mediaType: "image",
    purpose: "post",
    publicId: "asset-123",
    secureUrl: "https://res.cloudinary.com/demo/image/upload/asset-123.jpg",
    env: { CLOUDINARY_CLOUD_NAME: "demo", CLOUDINARY_API_KEY: "key", CLOUDINARY_API_SECRET: "secret" }
  };
  await assert.rejects(() => verifyCloudinaryAsset({
    ...common,
    fetchImpl: async () => ({ status: 404, ok: false })
  }), { code: "MEDIA_NOT_FOUND" });
  await assert.rejects(() => verifyCloudinaryAsset({
    ...common,
    fetchImpl: async () => ({
      status: 200,
      ok: true,
      json: async () => ({
        public_id: "asset-123",
        asset_folder: "bbiso/users/user-a/posts",
        resource_type: "image",
        secure_url: common.secureUrl,
        format: "jpg",
        bytes: 10 * 1024 * 1024 + 1
      })
    })
  }), { code: "MEDIA_VALIDATION_FAILED" });
});
