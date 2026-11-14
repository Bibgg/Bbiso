# OmniBox: Unified Social Media Architecture & Development Plan

OmniBox is a single-ecosystem Android application and backend architecture that bridges the mechanics of YouTube ("Tube"), Instagram ("Snap"), X ("Chatter"), and Reddit ("Forum") into an adaptive, multi-format social platform. This blueprint defines the end-to-end engineering specification, database schemas, GraphQL API contracts, Jetpack Compose component hierarchy, and offline-first state management strategy.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The architectural direction integrates user confirmations from the initial design interview:
> - **Unified Multi-Format Prototype**: The prototype implements the full 5-tab ecosystem (Home Mixed Feed, Tube short/long video, Snap photo stories/carousels, Chatter microblogging threads, and Forum community discussions) plus the central Create Action Modal.
> - **Adaptive Feed Layout with Quick Filter Chips**: The Mixed Home Feed serves an algorithmic blended stream of all media types while providing instant filter chips (`All`, `Tube`, `Snap`, `Chatter`, `Forum`) for user convenience without context switching.
> - **Viewport-Aware Auto-Play with Tap-to-Unmute**: Videos in the feed auto-play muted when >60% visible in the viewport, conserving memory and battery via a pooled ExoPlayer manager, with one-tap unmute and full-screen expansion.

---

## 1. Overview & Core Concept

OmniBox eliminates fragmented app-hopping by synthesizing four distinct social interaction paradigms into a unified, high-performance Android experience:

```
┌────────────────────────────────────────────────────────────────────────┐
│                                OMNIBOX                                 │
├───────────────┬────────────────┬───────────────────┬───────────────────┤
│  MODULE A     │  MODULE B      │  MODULE C         │  MODULE D         │
│  "Tube"       │  "Snap"        │  "Chatter"        │  "Forum"          │
│  (YouTube)    │  (Instagram)   │  (X / Twitter)    │  (Reddit)         │
├───────────────┼────────────────┼───────────────────┼───────────────────┤
│ • Vertical &  │ • Ephemeral    │ • Text-first      │ • Sub-communities │
│   long video  │   24h Stories  │   microblogging   │ • Up/down voting  │
│ • ExoPlayer   │ • Multi-photo  │ • Repost / quote  │ • Nested tree     │
│   pip & scrub │   carousels    │ • Character tiers │   comments        │
│ • Video chaps │ • Double-tap   │ • Live trending   │ • Sorting algos   │
│   & overlay   │   heart ripple │   hashtag radar   │   (Hot/Top/New)   │
└───────────────┴────────────────┴───────────────────┴───────────────────┘
```

- **Target Persona**: Digital natives, content creators, and social enthusiasts who switch between video bingeing, aesthetic photography, breaking news, and in-depth niche discussions.
- **Key Value**: One identity, one notification hub, one unified bookmarking system, and a blended algorithmic feed tailored to real-time intent.

---

## 2. User Experience & Visual Design

### Visual Identity & Theme Tokens
- **Background**: True OLED Black (`#000000`) for battery efficiency on AMOLED screens and high-contrast media presentation.
- **Surface Elevation**: `#0D0D11` (Surface Low), `#16161F` (Surface Medium), `#222230` (Surface High / Card stroke).
- **Primary Accent**: Neon Purple (`#B026FF`) representing creativity, stream, and multimedia.
- **Secondary Accent**: Cyber Cyan (`#00FFFF`) representing live chatter, real-time updates, and active links.
- **Feedback & Karma**: Neon Tangerine (`#FF5722` / Reddit Orange-Red for upvotes), Cyber Azure (`#2979FF` for downvotes), Hot Magenta (`#FF1493` for double-tap likes).
- **Typography Pairing**:
  - Display & Headings: `Outfit` / `Cabinet Grotesk` (bold, geometric, modern social personality).
  - Body & Micro-copy: `Plus Jakarta Sans` / `Inter` (high-legibility at 12–16sp, optimized for mobile screens).

### Navigation & Screen Structure
1. **Top Bar**:
   - OmniBox dynamic branding with animated status glyph.
   - Filter Chips Row: `All` | `Tube` | `Snap` | `Chatter` | `Forum`.
   - Direct Messages & Notification bell with unread badge counter.
2. **Body (Mixed Home Feed)**:
   - Top 24-hour Story Bar (`SnapStoryRow`) featuring circular creator avatars with neon gradient border rings.
   - Algorithmic `LazyColumn` interleaving Tube videos, Snap carousels, Chatter threads, and Forum cards.
3. **Floating "Create" FAB**:
   - Central dock button emitting an ambient neon glow.
   - Triggers a bottom sheet modal allowing creation of Video, Story/Post, Chatter, or Forum Thread.
4. **Bottom Navigation Bar (5 Destinations)**:
   - `Home`: Blended algorithmic feed.
   - `Tube`: Fullscreen vertical video reels with gestures & horizontal watch screen.
   - `Snap`: Curated photo grid & fullscreen story viewer.
   - `Chatter`: Fast microblog stream with character counter and reposting.
   - `Forum`: Sub-community drawer, thread sorting, and collapsible tree comments.

---

## 3. Database Schema (PostgreSQL Relational + ScyllaDB Timeline)

To balance strict ACID integrity for users/finances/follows with high-throughput timeline writes (fan-out on write), OmniBox utilizes a hybrid storage architecture:

### 3.1 Relational Core (PostgreSQL)

```sql
-- 1. Users Table
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(32) UNIQUE NOT NULL,
    display_name VARCHAR(64) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    avatar_url TEXT,
    bio VARCHAR(280),
    is_verified BOOLEAN DEFAULT FALSE,
    is_premium BOOLEAN DEFAULT FALSE, -- Unlocks extended 4,000 char chatter & 4K video
    karma_score BIGINT DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 2. Communities Table (Forum Sub-ecosystems)
CREATE TABLE communities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    slug VARCHAR(50) UNIQUE NOT NULL, -- e.g., 'c/technology'
    name VARCHAR(100) NOT NULL,
    description TEXT,
    icon_url TEXT,
    banner_url TEXT,
    creator_id UUID REFERENCES users(id),
    rules JSONB DEFAULT '[]'::jsonb,
    subscriber_count INT DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 3. Posts Unified Table
CREATE TYPE post_format AS ENUM ('TUBE_VIDEO', 'SNAP_CAROUSEL', 'CHATTER_TEXT', 'FORUM_THREAD');

CREATE TABLE posts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    author_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    community_id UUID REFERENCES communities(id) ON DELETE SET NULL, -- Nullable if personal post
    format post_format NOT NULL,
    title VARCHAR(300), -- Used for Forum & Tube
    body TEXT,          -- Chatter text, Forum body, or Tube video description
    media_metadata JSONB DEFAULT '{}'::jsonb, -- Video URLs, chapters, aspect ratio, carousel images
    like_count INT DEFAULT 0,
    upvote_count INT DEFAULT 0,
    downvote_count INT DEFAULT 0,
    comment_count INT DEFAULT 0,
    repost_count INT DEFAULT 0,
    view_count BIGINT DEFAULT 0,
    is_pinned BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 4. Nested Comments Tree (Path-based or ltree for O(1) subtree retrieval)
CREATE TABLE comments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    author_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    parent_comment_id UUID REFERENCES comments(id) ON DELETE CASCADE,
    path TEXT NOT NULL, -- e.g. "root.parent_id.child_id" for hierarchical nesting
    content TEXT NOT NULL,
    media_url TEXT,
    upvotes INT DEFAULT 0,
    downvotes INT DEFAULT 0,
    depth INT DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 5. Social Graph (Follows & Subscriptions)
CREATE TABLE follows (
    follower_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    following_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (follower_id, following_id)
);

CREATE TABLE community_memberships (
    community_id UUID NOT NULL REFERENCES communities(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(20) DEFAULT 'MEMBER', -- 'ADMIN', 'MODERATOR', 'MEMBER'
    joined_at TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (community_id, user_id)
);

-- 6. Votes & Reactions
CREATE TABLE post_votes (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    post_id UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    vote_type SMALLINT NOT NULL, -- +1 for upvote/like, -1 for downvote, 0 for neutral
    PRIMARY KEY (user_id, post_id)
);
```

### 3.2 High-Throughput Timeline Store (ScyllaDB / Cassandra)

```cql
-- User Feed Fan-Out Table (Pre-materialized feed for instant retrieval)
CREATE TABLE user_timelines (
    user_id uuid,
    bucket int, -- partition split by week (YYYYWW)
    published_at timestamp,
    post_id uuid,
    author_id uuid,
    format text, -- 'TUBE', 'SNAP', 'CHATTER', 'FORUM'
    score double, -- algorithmic relevance score
    PRIMARY KEY ((user_id, bucket), score, published_at, post_id)
) WITH CLUSTERING ORDER BY (score DESC, published_at DESC);
```

---

## 4. API Contract (GraphQL Queries, Mutations & Subscriptions)

```graphql
enum PostFormat {
  TUBE_VIDEO
  SNAP_CAROUSEL
  CHATTER_TEXT
  FORUM_THREAD
}

enum FeedSortOrder {
  ALGORITHMIC_HOT
  CHRONOLOGICAL_LATEST
  TOP_RATED
  TRENDING
}

type MediaItem {
  url: String!
  thumbnailUrl: String
  mimeType: String!
  width: Int
  height: Int
  durationSeconds: Float
}

type UserSummary {
  id: ID!
  username: String!
  displayName: String!
  avatarUrl: String
  isVerified: Boolean!
  karmaScore: Int!
}

type Post {
  id: ID!
  author: UserSummary!
  community: CommunitySummary
  format: PostFormat!
  title: String
  body: String
  mediaItems: [MediaItem!]!
  likeCount: Int!
  upvoteCount: Int!
  downvoteCount: Int!
  commentCount: Int!
  repostCount: Int!
  userVote: Int # +1, -1, or 0
  isBookmarked: Boolean!
  createdAt: String!
}

type FeedPage {
  items: [Post!]!
  nextCursor: String
  hasMore: Boolean!
}

type Query {
  # Mixed Algorithmic Home Feed with format filtering
  getHomeFeed(
    filterFormat: PostFormat
    sortOrder: FeedSortOrder = ALGORITHMIC_HOT
    cursor: String
    limit: Int = 20
  ): FeedPage!

  # Dedicated Pillar Feeds
  getTubeVideos(cursor: String, limit: Int = 10): FeedPage!
  getStories: [StoryGroup!]!
  getChatterStream(cursor: String, limit: Int = 25): FeedPage!
  getForumFeed(communitySlug: String, sort: FeedSortOrder, cursor: String): FeedPage!
  
  # Nested Comments Thread
  getThreadComments(postId: ID!, parentId: ID, depthLimit: Int = 5): [CommentNode!]!
}

type Mutation {
  createPost(input: CreatePostInput!): Post!
  castVote(postId: ID!, vote: Int!): VoteResult!
  toggleBookmark(postId: ID!): Boolean!
  repostChatter(postId: ID!, quoteText: String): Post!
  postComment(postId: ID!, parentCommentId: ID, text: String!): CommentNode!
}

type Subscription {
  # Real-time live comments and upvote ticker
  onPostMetricsUpdated(postId: ID!): PostMetrics!
  onNewChatterLive(hashtag: String!): Post!
}
```

---

## 5. Component Architecture (Jetpack Compose UI)

The Android UI uses modular, composable widgets bound together by a unified sealed state:

```
┌────────────────────────────────────────────────────────────────────────┐
│                          OmniBoxApp (Scaffold)                         │
│                                                                        │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │ OmniBoxTopBar: Dynamic branding + unread notifications + DM      │  │
│  ├──────────────────────────────────────────────────────────────────┤  │
│  │ FilterChipsRow: [ All | Tube 🎥 | Snap 📸 | Chatter 💬 | Forum 🌐 ]│  │
│  └──────────────────────────────────────────────────────────────────┘  │
│                                                                        │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │ Main Screen Container (Animated NavHost or Switcher)             │  │
│  │                                                                  │  │
│  │  [ HomeFeedScreen ]                                              │  │
│  │    ├─ StoriesRail (Horizontal Carousel with neon active rings)   │  │
│  │    └─ LazyColumn (Virtual list with key-based recycling)         │  │
│  │        ├─ TubeCardItem (Inline ExoPlayer preview + duration chip)│  │
│  │        ├─ SnapCardItem (Pager photo carousel + double-tap heart) │  │
│  │        ├─ ChatterCardItem (Microblog thread + repost indicator)  │  │
│  │        └─ ForumCardItem (VoteWidget + sub-community badge)       │  │
│  │                                                                  │  │
│  │  [ TubeScreen ] -> Vertical Pager of full-screen ExoPlayer reels │  │
│  │  [ SnapScreen ] -> Staggered visual explore grid + story modal   │  │
│  │  [ ChatterScreen ] -> Microblog stream with trending hashtags    │  │
│  │  [ ForumScreen ] -> Community selector + Top/Hot/New sort bar    │  │
│  └──────────────────────────────────────────────────────────────────┘  │
│                                                                        │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │ BottomBar: [Home] [Tube] [ (+) CREATE FAB ] [Chatter] [Forum]    │  │
│  └──────────────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────────┘
```

### Key Reusable Jetpack Compose Components:
1. `VoteWidget(upvotes, downvotes, currentVote, onVoteChange)`:
   - Vertically or horizontally stacked Reddit-style score indicator with spring bounce animation on vote change.
2. `DoubleTapLikeOverlay(isLiked, onDoubleTap)`:
   - Instagram-style bursting heart particle overlay with alpha scale animation.
3. `InlineVideoPlayer(videoUrl, isVisible, isMuted, onToggleMute)`:
   - Lightweight Compose wrapper around Media3 `PlayerView` with thumbnail poster fallback and playback state sync.
4. `StoryRingAvatar(avatarUrl, hasUnseenStory, onClick)`:
   - Gradient circular border (`Brush.sweepGradient` from Neon Purple to Cyan) indicating unwatched stories.
5. `ChatterEngagementBar(replies, reposts, likes, views, onAction)`:
   - Microblog metric actions with optimistic counter increment.
6. `CreateActionSheet`:
   - Bottom sheet modal displaying post creation options with dedicated creation flows.

---

## 6. State Management & Offline Strategy

```
┌─────────────────┐       ┌─────────────────┐       ┌─────────────────┐
│ User UI Action  │ ───>  │  OmniViewModel  │ ───>  │ Optimistic State │
└─────────────────┘       └────────┬────────┘       └────────┬────────┘
                                   │                         │
                                   ▼                         ▼
                          ┌─────────────────┐       ┌─────────────────┐
                          │ OmniRepository  │ <───> │ Room DB (Cache) │
                          └────────┬────────┘       └─────────────────┘
                                   │
                                   ▼
                          ┌─────────────────┐
                          │ GraphQL / REST  │
                          └─────────────────┘
```

- **MVI Architecture**:
  - `OmniIntent`: Sealed class of user actions (`SelectFilter`, `LikePost`, `UpvotePost`, `ToggleMute`, `PostCreated`, `RefreshFeed`).
  - `OmniUiState`: Immutable data class containing `feedItems`, `activeFilter`, `stories`, `isRefreshing`, and `currentPlayingPostId`.
  - `OmniEffect`: One-time events via `SharedFlow` (`ShowSnackbar`, `LaunchFullscreenVideo`, `OpenCommentsSheet`).
- **Optimistic Updates**:
  - When the user likes a Snap or upvotes a Forum post, the UI updates its local `StateFlow` immediately.
  - If the background network request fails, the local state rolls back with an animated shake and notification.
- **ExoPlayer Lifecycle & Memory Management**:
  - Maintain a maximum pool of 2 active player instances.
  - Listen to `LazyListState.layoutInfo.visibleItemsInfo` to calculate visibility percentage.
  - Pause playback when visible area drops below 50% or when the app is backgrounded.

---

## 7. Implementation Plan for Android Codebase

1. **Project Dependencies & Styling**:
   - Verify Coil, Material 3 icons, and Jetpack Compose dependencies.
   - Configure OLED dark theme with Neon Purple (`#B026FF`) and Cyan (`#00FFFF`) tokens in `Theme.kt`.
2. **Domain Models & Seed Data Engine**:
   - Create unified `FeedPost` hierarchy (`TubePost`, `SnapPost`, `ChatterPost`, `ForumPost`) with rich media, creators, and community tags.
   - Build a sample social graph data source for interactive exploration.
3. **Core Interactive Widgets**:
   - `VoteWidget`, `LikeWidget`, `StoryReel`, `MediaCard`, `CommentOverlaySheet`.
4. **Feed Pillars & Screens**:
   - `HomeFeedScreen` with multi-format card rendering and quick filter chips.
   - `TubeScreen` with vertical swipe reels.
   - `SnapScreen` with story views and photo grid.
   - `ChatterScreen` with microblog threads and repost mechanics.
   - `ForumScreen` with community topics and vote sorting.
5. **Interactive Creation Modal**:
   - Create modal allowing users to post stories, videos, chatter, or forum topics.
6. **Verification & Build**:
   - Compile and verify with `compile_applet`.
