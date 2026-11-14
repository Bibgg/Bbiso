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


// 6. Chat membership and message integrity
test("Chat participant cannot add an outsider to an existing chat", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("chats").doc("chat_locked").set({
      chatId: "chat_locked",
      participants: [ALICE_UID, BOB_UID],
      lastMessage: "Hi"
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("chats").doc("chat_locked").update({
    participants: [ALICE_UID, BOB_UID, "eve_999"]
  }));
});

test("Chat participant cannot remove another participant", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("chats").doc("chat_locked").set({
      chatId: "chat_locked",
      participants: [ALICE_UID, BOB_UID],
      lastMessage: "Hi"
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("chats").doc("chat_locked").update({
    participants: [ALICE_UID]
  }));
});

test("Message sender cannot transfer message ownership", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("chats").doc("chat_locked").set({
      chatId: "chat_locked",
      participants: [ALICE_UID, BOB_UID]
    });
    await context.firestore().collection("chats").doc("chat_locked")
      .collection("messages").doc("msg_alice").set({
        messageId: "msg_alice",
        chatId: "chat_locked",
        senderUid: ALICE_UID,
        text: "Original"
      });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("chats").doc("chat_locked")
    .collection("messages").doc("msg_alice").update({ senderUid: BOB_UID }));
});

test("Chat participant can update last-message preview without changing membership", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("chats").doc("chat_locked").set({
      chatId: "chat_locked",
      participants: [ALICE_UID, BOB_UID],
      lastMessage: "Old"
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("chats").doc("chat_locked").update({
    lastMessage: "New message"
  }));
});


// 7. Story expiry and viewer records
test("Authenticated user can publish own story with expiry within 24 hours", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("stories").doc("story_alice").set({
    storyId: "story_alice",
    uid: ALICE_UID,
    mediaUrl: "https://res.cloudinary.com/demo/video/upload/story.mp4",
    mediaType: "video",
    caption: "A short story",
    expiresAt: new Date(Date.now() + 23 * 60 * 60 * 1000)
  }));
});

test("User cannot publish a story under another user's UID", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("stories").doc("story_spoof").set({
    storyId: "story_spoof",
    uid: BOB_UID,
    mediaUrl: "https://res.cloudinary.com/demo/image/upload/story.jpg",
    mediaType: "photo",
    expiresAt: new Date(Date.now() + 60 * 60 * 1000)
  }));
});

test("Expired stories cannot be read", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("stories").doc("story_expired").set({
      storyId: "story_expired",
      uid: ALICE_UID,
      mediaUrl: "https://res.cloudinary.com/demo/image/upload/story.jpg",
      mediaType: "photo",
      expiresAt: new Date(Date.now() - 60 * 1000)
    });
  });
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("stories").doc("story_expired").get());
});

test("A signed-in viewer can record one view for an active story", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("stories").doc("story_active").set({
      storyId: "story_active",
      uid: ALICE_UID,
      mediaUrl: "https://res.cloudinary.com/demo/image/upload/story.jpg",
      mediaType: "photo",
      expiresAt: new Date(Date.now() + 60 * 60 * 1000)
    });
  });
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(bobDb.collection("stories").doc("story_active").collection("views").doc(BOB_UID).set({
    viewerUid: BOB_UID,
    viewedAt: new Date()
  }));
  await assertFails(bobDb.collection("stories").doc("story_active").collection("views").doc(ALICE_UID).set({
    viewerUid: ALICE_UID,
    viewedAt: new Date()
  }));
});
