const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

// 1. Unauthenticated checks
test("Unauthenticated user: cannot read users", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").get());
});

test("Unauthenticated user: cannot write posts", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("posts").doc("post_1").set({
    postId: "post_1",
    uid: ALICE_UID,
    format: "TUBE"
  }));
});

// 2. User profiles
test("Authenticated user: can create own profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set({
    uid: ALICE_UID,
    email: "alice@example.com",
    username: "alice_crypto",
    full_name: "Alice Wonderland"
  }));
});

test("Authenticated user: cannot update someone else's profile", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(BOB_UID).set({
      uid: BOB_UID,
      email: "bob@example.com",
      username: "bob_ross"
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).update({
    full_name: "Hacked by Alice"
  }));
});

// 3. Unique Username Reservation
test("Authenticated user: can reserve valid handle for self", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("usernames").doc("alice_crypto").set({
    uid: ALICE_UID,
    username: "alice_crypto"
  }));
});

test("Authenticated user: cannot reserve handle under someone else's uid", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("usernames").doc("bob_handle").set({
    uid: BOB_UID,
    username: "bob_handle"
  }));
});

// 4. Posts
test("Authenticated user: can create post with own UID", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("posts").doc("post_alice_1").set({
    postId: "post_alice_1",
    uid: ALICE_UID,
    format: "TUBE",
    caption: "My first tube video!"
  }));
});

test("Authenticated user: cannot spoof another user's post creation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("posts").doc("post_bob_1").set({
    postId: "post_bob_1",
    uid: BOB_UID,
    format: "CHATTER",
    caption: "Impersonating Bob"
  }));
});

// 5. Chats and Messaging
test("Chat participants: can read and send messages", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("chats").doc("chat_12").set({
      chatId: "chat_12",
      participants: [ALICE_UID, BOB_UID],
      lastMessage: "Hi"
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("chats").doc("chat_12").collection("messages").doc("msg_1").set({
    messageId: "msg_1",
    chatId: "chat_12",
    senderUid: ALICE_UID,
    text: "Hello Bob!"
  }));
});

test("Non-participant: cannot read or write to private chat", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("chats").doc("chat_12").set({
      chatId: "chat_12",
      participants: [ALICE_UID, BOB_UID],
      lastMessage: "Secret"
    });
  });

  const eveDb = testEnv.authenticatedContext("eve_999").firestore();
  await assertFails(eveDb.collection("chats").doc("chat_12").get());
  await assertFails(eveDb.collection("chats").doc("chat_12").collection("messages").doc("msg_evil").set({
    messageId: "msg_evil",
    chatId: "chat_12",
    senderUid: "eve_999",
    text: "Eavesdropping"
  }));
});
