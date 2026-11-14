const test = require("node:test");
const assert = require("node:assert/strict");
const crypto = require("node:crypto");
const { createCloudinarySignature, createUploadTicket } = require("./cloudinary-upload");

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
