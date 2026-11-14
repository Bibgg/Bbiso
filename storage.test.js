const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { ref, uploadBytes } = require("firebase/storage");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const [host, portText] = (process.env.FIREBASE_STORAGE_EMULATOR_HOST || "127.0.0.1:9199").split(":");

before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    storage: {
      rules: fs.readFileSync("./storage.rules", "utf8"),
      host,
      port: Number(portText)
    }
  });
});

after(async () => {
  if (testEnv) await testEnv.cleanup();
});

beforeEach(async () => {
  if (testEnv) await testEnv.clearStorage();
});

test("Unauthenticated user cannot upload media", async () => {
  const storage = testEnv.unauthenticatedContext().storage();
  await assertFails(uploadBytes(ref(storage, "users/alice_123/photos/a.jpg"), new Uint8Array([1, 2, 3]), {
    contentType: "image/jpeg"
  }));
});

test("User can upload an image inside their own folder", async () => {
  const storage = testEnv.authenticatedContext(ALICE_UID).storage();
  await assertSucceeds(uploadBytes(ref(storage, "users/alice_123/photos/a.jpg"), new Uint8Array([1, 2, 3]), {
    contentType: "image/jpeg"
  }));
});

test("User cannot upload into another user's folder", async () => {
  const storage = testEnv.authenticatedContext(ALICE_UID).storage();
  await assertFails(uploadBytes(ref(storage, "users/bob_456/photos/a.jpg"), new Uint8Array([1, 2, 3]), {
    contentType: "image/jpeg"
  }));
});

test("Image folder rejects non-image content", async () => {
  const storage = testEnv.authenticatedContext(ALICE_UID).storage();
  await assertFails(uploadBytes(ref(storage, "users/alice_123/photos/a.jpg"), new Uint8Array([1, 2, 3]), {
    contentType: "application/octet-stream"
  }));
});
