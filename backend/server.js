require("dotenv").config();
const express = require("express");
const mongoose = require("mongoose");
const bcrypt = require("bcryptjs");
const jwt = require("jsonwebtoken");
const helmet = require("helmet");
const cors = require("cors");
const rateLimit = require("express-rate-limit");
const { z } = require("zod");
const { requireFirebaseAuth } = require("./firebase-auth");
const { createUploadTicket, verifyCloudinaryAsset } = require("./cloudinary-upload");

const app = express();
app.disable("x-powered-by");
// Render terminates TLS and forwards requests through one trusted proxy hop.
app.set("trust proxy", 1);
app.use(helmet());
app.use(cors({ origin: process.env.CORS_ORIGIN ? process.env.CORS_ORIGIN.split(",") : true }));
app.use(express.json({ limit: "1mb" }));
app.use("/api/auth", rateLimit({ windowMs: 15 * 60 * 1000, limit: 30, standardHeaders: true, legacyHeaders: false }));
app.use("/api/media/upload-signature", rateLimit({ windowMs: 60 * 60 * 1000, limit: 30, standardHeaders: true, legacyHeaders: false }));

const userSchema = new mongoose.Schema({
  name: { type: String, required: true, trim: true, maxlength: 60 },
  email: { type: String, required: true, unique: true, lowercase: true, trim: true },
  passwordHash: { type: String, required: true, select: false },
  handle: { type: String, required: true, unique: true, lowercase: true, trim: true },
  avatarUrl: { type: String, default: "" },
  createdAt: { type: Date, default: Date.now }
});
const User = mongoose.model("User", userSchema);

const replySchema = new mongoose.Schema({
  authorId: { type: mongoose.Schema.Types.ObjectId, ref: "User", required: true },
  authorName: { type: String, required: true },
  body: { type: String, required: true, maxlength: 5000 },
  createdAt: { type: Date, default: Date.now }
}, { _id: true });
const forumSchema = new mongoose.Schema({
  authorId: { type: mongoose.Schema.Types.ObjectId, ref: "User", required: true, index: true },
  authorName: { type: String, required: true },
  community: { type: String, required: true, enum: ["Technology","Education","Entertainment","General"] },
  title: { type: String, required: true, trim: true, maxlength: 180 },
  body: { type: String, required: true, trim: true, maxlength: 10000 },
  tags: [{ type: String, trim: true, maxlength: 30 }],
  replies: [replySchema],
  createdAt: { type: Date, default: Date.now, index: true }
});
const ForumPost = mongoose.model("ForumPost", forumSchema);

const mediaPostSchema = new mongoose.Schema({
  authorId: { type: mongoose.Schema.Types.ObjectId, ref: "User", required: true, index: true },
  kind: { type: String, enum: ["tube","snap"], required: true },
  caption: { type: String, maxlength: 2000, default: "" },
  mediaUrl: { type: String, required: true },
  mimeType: { type: String, default: "" },
  createdAt: { type: Date, default: Date.now, index: true }
});
const MediaPost = mongoose.model("MediaPost", mediaPostSchema);

function signToken(user) {
  return jwt.sign({ sub: user._id.toString(), email: user.email }, process.env.JWT_SECRET, { expiresIn: "7d", issuer: "bbiso-api", audience: "bbiso-android" });
}
async function requireAuth(req, res, next) {
  try {
    const header = req.headers.authorization || "";
    const token = header.startsWith("Bearer ") ? header.slice(7) : "";
    if (!token) return res.status(401).json({ error: "Authentication required" });
    const payload = jwt.verify(token, process.env.JWT_SECRET, { issuer: "bbiso-api", audience: "bbiso-android" });
    const user = await User.findById(payload.sub);
    if (!user) return res.status(401).json({ error: "Account not found" });
    req.user = user;
    next();
  } catch { return res.status(401).json({ error: "Invalid or expired token" }); }
}
const registerSchema = z.object({
  name: z.string().trim().min(2).max(60),
  email: z.string().trim().email().max(254),
  password: z.string().min(10).refine(value => Buffer.byteLength(value, "utf8") <= 72, "Password must be at most 72 UTF-8 bytes")
});
const loginSchema = z.object({
  email: z.string().trim().email().max(254),
  password: z.string().min(1).refine(value => Buffer.byteLength(value, "utf8") <= 72)
});
app.get("/health", (_req, res) => res.json({ ok: true, service: "bbiso-api", database: mongoose.connection.readyState === 1 ? "connected" : "disconnected" }));

app.post("/api/auth/register", async (req, res, next) => {
  try {
    const parsed = registerSchema.safeParse(req.body);
    if (!parsed.success) return res.status(400).json({ error: "Enter a valid name, email and password (10-72 characters)." });
    const email = parsed.data.email.toLowerCase();
    if (await User.exists({ email })) return res.status(409).json({ error: "Email is already registered" });
    const base = parsed.data.name.toLowerCase().replace(/[^a-z0-9]/g, "").slice(0, 18) || "member";
    let handle = base, suffix = 1;
    while (await User.exists({ handle })) handle = base + (++suffix);
    const user = await User.create({ name: parsed.data.name, email, handle, passwordHash: await bcrypt.hash(parsed.data.password, 12) });
    res.status(201).json({ token: signToken(user), user: { id: user.id, name: user.name, email: user.email, handle: user.handle, avatarUrl: user.avatarUrl } });
  } catch (e) { next(e); }
});
app.post("/api/auth/login", async (req, res, next) => {
  try {
    const parsed = loginSchema.safeParse(req.body || {});
    if (!parsed.success) return res.status(400).json({ error: "Enter a valid email and password" });
    const email = parsed.data.email.toLowerCase();
    const password = parsed.data.password;
    const user = await User.findOne({ email }).select("+passwordHash");
    if (!user || !(await bcrypt.compare(password, user.passwordHash))) return res.status(401).json({ error: "Email or password is incorrect" });
    res.json({ token: signToken(user), user: { id: user.id, name: user.name, email: user.email, handle: user.handle, avatarUrl: user.avatarUrl } });
  } catch (e) { next(e); }
});
app.get("/api/auth/me", requireAuth, (req, res) => res.json({ user: { id: req.user.id, name: req.user.name, email: req.user.email, handle: req.user.handle, avatarUrl: req.user.avatarUrl } }));

app.get("/api/forum", async (req, res, next) => {
  try {
    const page = Math.max(1, Math.min(10000, Number(req.query.page) || 1));
    const limit = Math.max(1, Math.min(50, Number(req.query.limit) || 20));
    const filter = req.query.community ? { community: String(req.query.community) } : {};
    const [items, total] = await Promise.all([
      ForumPost.find(filter).sort({ createdAt: -1 }).skip((page - 1) * limit).limit(limit).lean(),
      ForumPost.countDocuments(filter)
    ]);
    res.json({ items: items.map(p => ({ ...p, id: p._id.toString(), replyCount: p.replies.length })), page, limit, total });
  } catch (e) { next(e); }
});
app.post("/api/forum", requireAuth, async (req, res, next) => {
  try {
    const schema = z.object({
      community: z.enum(["Technology","Education","Entertainment","General"]),
      title: z.string().trim().min(5).max(180),
      body: z.string().trim().min(10).max(10000),
      tags: z.array(z.string().trim().min(1).max(30)).max(10).default([])
    });
    const parsed = schema.safeParse(req.body);
    if (!parsed.success) return res.status(400).json({ error: "Check community, title, description and tags" });
    const post = await ForumPost.create({ ...parsed.data, authorId: req.user._id, authorName: req.user.name });
    res.status(201).json({ item: post });
  } catch (e) { next(e); }
});
app.post("/api/forum/:id/replies", requireAuth, async (req, res, next) => {
  try {
    const body = String(req.body.body || "").trim();
    if (body.length < 2 || body.length > 5000) return res.status(400).json({ error: "Reply must be 2-5000 characters" });
    const post = await ForumPost.findOneAndUpdate(
      { _id: req.params.id, $expr: { $lt: [{ $size: { $ifNull: ["$replies", []] } }, 100] } },
      { $push: { replies: { authorId: req.user._id, authorName: req.user.name, body } } },
      { new: true, runValidators: true }
    );
    if (!post) {
      const exists = await ForumPost.exists({ _id: req.params.id });
      return res.status(exists ? 409 : 404).json({ error: exists ? "This discussion has reached its 100-reply limit" : "Forum post not found" });
    }
    res.status(201).json({ item: post });
  } catch (e) { next(e); }
});
// Firebase ID tokens are verified against Google's published signing certificates.
app.post("/api/media/upload-signature", requireFirebaseAuth(process.env.FIREBASE_PROJECT_ID), (req, res) => {
  const schema = z.object({ mediaType: z.enum(["video", "image"]), purpose: z.enum(["post", "story"]) }).strict();
  const parsed = schema.safeParse(req.body);
  if (!parsed.success) return res.status(400).json({ error: "Choose a valid media type and upload purpose" });
  try {
    const ticket = createUploadTicket({ uid: req.firebaseUser.uid, mediaType: parsed.data.mediaType, purpose: parsed.data.purpose });
    return res.json(ticket);
  } catch (error) {
    if (error.code === "CLOUDINARY_NOT_CONFIGURED") return res.status(503).json({ error: "Media uploads are temporarily unavailable" });
    console.error("Cloudinary signing failed:", error.message);
    return res.status(500).json({ error: "Could not prepare media upload" });
  }
});

app.post("/api/media/verify-upload", rateLimit({ windowMs: 60 * 60 * 1000, limit: 60, standardHeaders: true, legacyHeaders: false }), requireFirebaseAuth(process.env.FIREBASE_PROJECT_ID), async (req, res) => {
  const schema = z.object({
    mediaType: z.enum(["video", "image"]),
    purpose: z.enum(["post", "story"]),
    publicId: z.string().regex(/^[A-Za-z0-9_-]{1,80}$/),
    secureUrl: z.string().url().max(2048)
  }).strict();
  const parsed = schema.safeParse(req.body);
  if (!parsed.success || new URL(parsed.success ? parsed.data.secureUrl : "https://invalid.local").protocol !== "https:") {
    return res.status(400).json({ error: "Valid uploaded media details are required" });
  }
  try {
    const verified = await verifyCloudinaryAsset({
      uid: req.firebaseUser.uid,
      mediaType: parsed.data.mediaType,
      purpose: parsed.data.purpose,
      publicId: parsed.data.publicId,
      secureUrl: parsed.data.secureUrl
    });
    return res.json(verified);
  } catch (error) {
    if (error.code === "CLOUDINARY_NOT_CONFIGURED") return res.status(503).json({ error: "Media verification is temporarily unavailable" });
    if (error.code === "MEDIA_NOT_FOUND" || error.code === "MEDIA_VALIDATION_FAILED") {
      return res.status(422).json({ error: "The uploaded media could not be verified" });
    }
    console.error("Cloudinary media verification failed:", error.message);
    return res.status(502).json({ error: "Could not verify uploaded media" });
  }
});

app.get("/api/media", async (req, res, next) => {
  try {
    const kind = ["tube","snap"].includes(String(req.query.kind)) ? String(req.query.kind) : null;
    const filter = kind ? { kind } : {};
    const items = await MediaPost.find(filter).sort({ createdAt: -1 }).limit(50).lean();
    res.json({ items });
  } catch (e) { next(e); }
});
app.post("/api/media", requireAuth, async (req, res, next) => {
  try {
    const schema = z.object({
      kind: z.enum(["tube","snap"]),
      caption: z.string().trim().max(2000).optional().default(""),
      mediaUrl: z.string().url().max(2048).refine(value => new URL(value).protocol === "https:", "Media URL must use HTTPS"),
      mimeType: z.string().max(120).optional().default("")
    });
    const parsed = schema.safeParse(req.body);
    if (!parsed.success) return res.status(400).json({ error: "Valid kind and media URL are required" });
    const item = await MediaPost.create({ ...parsed.data, authorId: req.user._id });
    res.status(201).json({ item });
  } catch (e) { next(e); }
});

app.use((err, _req, res, _next) => {
  if (err && err.type === "entity.too.large") return res.status(413).json({ error: "Request body is too large" });
  if (err instanceof SyntaxError && err.status === 400 && "body" in err) return res.status(400).json({ error: "Malformed JSON request body" });
  if (err && err.code === 11000) return res.status(409).json({ error: "An account with these details already exists" });
  console.error("API error:", err.message);
  res.status(500).json({ error: "Internal server error" });
});
const port = Number(process.env.PORT || 10000);
async function start() {
  if (!process.env.MONGO_URI || !process.env.JWT_SECRET || process.env.JWT_SECRET.length < 32) {
    throw new Error("MONGO_URI and a JWT_SECRET of at least 32 characters are required");
  }
  await mongoose.connect(process.env.MONGO_URI, { serverSelectionTimeoutMS: 15000 });
  app.listen(port, "0.0.0.0", () => console.log("Bbiso API listening on", port));
}
start().catch(err => { console.error("Startup failed:", err.message); process.exit(1); });
