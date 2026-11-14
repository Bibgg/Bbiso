package com.example.data.repository

import com.example.data.model.Comment
import com.example.data.model.Community
import com.example.data.model.FeedPost
import com.example.data.model.PostFormat
import com.example.data.model.StoryItem
import com.example.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class OmniRepository {

    private val currentUser = User(
        id = "user_me",
        username = "alex_cyber",
        displayName = "Alex Rivera",
        avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop&q=80",
        isVerified = true,
        karmaScore = 4850
    )

    private val initialCommunities = listOf(
        Community("c1", "c/androiddev", "Android Architecture & Compose", "https://images.unsplash.com/photo-1607252650355-f7fd0460ccdb?w=100&auto=format&fit=crop&q=80", "340k"),
        Community("c2", "c/futuretech", "Artificial Intelligence & Quantum", "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=100&auto=format&fit=crop&q=80", "890k"),
        Community("c3", "c/cyberpunk", "Neon Aesthetic & Urban Future", "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=100&auto=format&fit=crop&q=80", "150k"),
        Community("c4", "c/gaming", "Next-Gen Engine & Modding", "https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=100&auto=format&fit=crop&q=80", "1.2M")
    )

    private val initialStories = listOf(
        StoryItem(
            id = "s1",
            user = User("u1", "elena_lens", "Elena Rostova", "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150&auto=format&fit=crop&q=80", true),
            previewImage = "https://images.unsplash.com/photo-1514565131-fce0801e5785?w=600&auto=format&fit=crop&q=80",
            caption = "Tokyo midnight street lights 🏙️",
            hasUnseen = true,
            timestamp = "1h ago"
        ),
        StoryItem(
            id = "s2",
            user = User("u2", "dev_marcus", "Marcus Vance", "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150&auto=format&fit=crop&q=80", true),
            previewImage = "https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?w=600&auto=format&fit=crop&q=80",
            caption = "Building real-time WebSockets on Kotlin Flow ⚡",
            hasUnseen = true,
            timestamp = "2h ago"
        ),
        StoryItem(
            id = "s3",
            user = User("u3", "synth_wave", "Kora Nova", "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=150&auto=format&fit=crop&q=80", false),
            previewImage = "https://images.unsplash.com/photo-1508739773434-c26b3d09e071?w=600&auto=format&fit=crop&q=80",
            caption = "Late studio jam session with modular racks 🎛️",
            hasUnseen = true,
            timestamp = "4h ago"
        ),
        StoryItem(
            id = "s4",
            user = User("u4", "cyber_drifter", "Ren Tanaka", "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150&auto=format&fit=crop&q=80", true),
            previewImage = "https://images.unsplash.com/photo-1519501025264-65ba15a82390?w=600&auto=format&fit=crop&q=80",
            caption = "Cyberpunk rain reflections in Shibuya 🌧️",
            hasUnseen = false,
            timestamp = "6h ago"
        )
    )

    private val initialPosts = listOf(
        // 1. TUBE (Video Card - YouTube style)
        FeedPost(
            id = "post_tube_1",
            author = User("u_tube1", "unreal_creator", "Apex Virtual Studios", "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150&auto=format&fit=crop&q=80", true),
            format = PostFormat.TUBE,
            title = "Building a Full Open-World City in Unreal Engine 5.5 with Nanite & Lumen",
            body = "In this deep dive, we walk through building 12 square kilometers of procedurally generated sci-fi cityscape with real-time global illumination and 60 FPS performance on mobile GPUs.",
            mediaUrls = listOf("android.resource://com.aistudio.omnisocialbox.elmlpx/raw/sample_reel"),
            videoDuration = "14:28",
            tags = listOf("GameDev", "UnrealEngine", "Graphics", "OmniTube"),
            likeCount = 8420,
            commentCount = 612,
            viewCount = "142K",
            isPlaying = true,
            isMuted = false,
            createdAt = "3h ago",
            comments = listOf(
                Comment("c1_1", User("u_c1", "pixel_master", "Ray Tracer", "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100"), "The lighting at 04:12 is incredible. Are you using custom HLSL shaders?", "2h ago", upvotes = 142),
                Comment("c1_2", User("u_c2", "shader_nerd", "Kari Lin", "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=100"), "Great optimization tips on memory pooling!", "1h ago", upvotes = 56)
            )
        ),

        // 2. SNAP (Photo Carousel - Instagram style)
        FeedPost(
            id = "post_snap_1",
            author = User("u_snap1", "astral_design", "Astral Architecture", "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150&auto=format&fit=crop&q=80", true),
            format = PostFormat.SNAP,
            body = "Biophilic skyscraper concepts for 2035. Glass atrium gardens integrated with passive atmospheric water harvesters. Which terrace design do you prefer? 🌿🏢✨",
            mediaUrls = listOf(
                "https://images.unsplash.com/photo-1512917774080-9991f1c4c750?w=1080&auto=format&fit=crop&q=80",
                "https://images.unsplash.com/photo-1600585154340-be6161a56a0c?w=1080&auto=format&fit=crop&q=80",
                "https://images.unsplash.com/photo-1600596542815-ffad4c1539a9?w=1080&auto=format&fit=crop&q=80"
            ),
            tags = listOf("Architecture", "DesignInspo", "Biophilic", "FutureLiving"),
            likeCount = 19420,
            commentCount = 428,
            createdAt = "4h ago",
            comments = listOf(
                Comment("c2_1", User("u_c3", "eco_arch", "Sora Green", "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=100"), "Slide 2 is breathtaking. The hanging vertical moss insulation is genius.", "3h ago", upvotes = 89)
            )
        ),

        // 3. CHATTER (Microblog - X/Twitter style)
        FeedPost(
            id = "post_chatter_1",
            author = User("u_chatter1", "neural_dave", "David Miller", "https://images.unsplash.com/photo-1522075469751-3a6694fb2f61?w=150&auto=format&fit=crop&q=80", true),
            format = PostFormat.CHATTER,
            body = "Multi-modal foundation models running locally on client silicon with zero cloud latency changes the entire software paradigm.\n\nEverything from real-time visual translation to edge query synthesis is now accessible offline.\n\nThe edge is the new cloud. #AI #EdgeComputing #TechTrends",
            tags = listOf("AI", "EdgeComputing", "TechTrends"),
            likeCount = 4120,
            repostCount = 890,
            commentCount = 312,
            viewCount = "89.2K",
            createdAt = "1h ago",
            comments = listOf(
                Comment("c3_1", User("u_c4", "sam_codes", "Sam Wu", "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=100"), "Battery draw is still the biggest hurdle, but NPU efficiency is doubling yearly.", "45m ago", upvotes = 74)
            )
        ),

        // 4. FORUM (Discussion Thread - Reddit style)
        FeedPost(
            id = "post_forum_1",
            author = User("u_forum1", "compose_architect", "Devin Vance", "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150&auto=format&fit=crop&q=80", false),
            community = Community("c1", "c/androiddev", "Android Architecture & Compose", "https://images.unsplash.com/photo-1607252650355-f7fd0460ccdb?w=100&auto=format&fit=crop&q=80", "340k"),
            format = PostFormat.FORUM,
            title = "[Discussion] Is MVI with immutable StateFlow always superior to traditional MVVM with multiple LiveData streams?",
            body = "Over the last 3 enterprise projects, migrating from scattered LiveData states to a single unified `StateFlow<UiState>` has drastically reduced race conditions, especially during rapid config changes and process death.\n\nHowever, state copy overhead on high-frequency streams (like sensor/audio data) can become noticeable. How are you handling partial recomposition boundaries?",
            upvoteCount = 1840,
            downvoteCount = 42,
            commentCount = 289,
            createdAt = "5h ago",
            comments = listOf(
                Comment(
                    id = "c4_1",
                    author = User("u_f1", "torvalds_fan", "Linus K.", "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100"),
                    text = "A single StateFlow is king for UI consistency. For high-frequency telemetry, keep those hot flows isolated to dedicated composables with derivedStateOf rather than churning the global state object.",
                    timestamp = "4h ago",
                    upvotes = 342,
                    depth = 0,
                    replies = listOf(
                        Comment(
                            id = "c4_1_1",
                            author = User("u_f2", "kotlin_ninja", "Elena K.", "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=100"),
                            text = "100% agree. derivedStateOf prevents unnecessary recompositions even when the source flow is emitting at 60Hz.",
                            timestamp = "3h ago",
                            upvotes = 98,
                            depth = 1
                        )
                    )
                ),
                Comment(
                    id = "c4_2",
                    author = User("u_f3", "clean_coder", "Maya Patel", "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=100"),
                    text = "The biggest win of MVI is time-travel debugging and deterministic test fixtures. You can mock any state snapshot with one line.",
                    timestamp = "2h ago",
                    upvotes = 118,
                    depth = 0
                )
            )
        ),

        // 5. TUBE SHORT (Vertical Video Reel)
        FeedPost(
            id = "post_tube_2",
            author = User("u_tube2", "cyber_pilot", "Neon Runner Studio", "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop&q=80", true),
            format = PostFormat.TUBE,
            title = "First flight test of the autonomous hoverbike prototype! 🚀",
            body = "Testing the dual-ducted turbine stabilization at dawn. The gyroscopic balancing algorithm responded in under 4ms.",
            mediaUrls = listOf("android.resource://com.aistudio.omnisocialbox.elmlpx/raw/sample_reel"),
            videoDuration = "0:45",
            tags = listOf("Aviation", "Hoverbike", "SciFiReality", "Shorts"),
            likeCount = 38900,
            commentCount = 1240,
            viewCount = "520K",
            isPlaying = true,
            isMuted = false,
            createdAt = "6h ago"
        )
    )

    private val _postsFlow = MutableStateFlow(initialPosts)
    val postsFlow: StateFlow<List<FeedPost>> = _postsFlow.asStateFlow()

    private val _storiesFlow = MutableStateFlow(initialStories)
    val storiesFlow: StateFlow<List<StoryItem>> = _storiesFlow.asStateFlow()

    fun getCurrentUser(): User = currentUser
    fun getCommunities(): List<Community> = initialCommunities

    fun toggleLike(postId: String) {
        _postsFlow.value = _postsFlow.value.map { post ->
            if (post.id == postId) {
                val newLiked = !post.isLiked
                val newCount = if (newLiked) post.likeCount + 1 else (post.likeCount - 1).coerceAtLeast(0)
                post.copy(isLiked = newLiked, likeCount = newCount)
            } else post
        }
    }

    fun castVote(postId: String, direction: Int) {
        _postsFlow.value = _postsFlow.value.map { post ->
            if (post.id == postId) {
                val currentVote = post.userVote
                val newVote = if (currentVote == direction) 0 else direction
                val upvoteDiff = when {
                    newVote == 1 && currentVote != 1 -> 1
                    newVote != 1 && currentVote == 1 -> -1
                    else -> 0
                }
                val downvoteDiff = when {
                    newVote == -1 && currentVote != -1 -> 1
                    newVote != -1 && currentVote == -1 -> -1
                    else -> 0
                }
                post.copy(
                    userVote = newVote,
                    upvoteCount = (post.upvoteCount + upvoteDiff).coerceAtLeast(0),
                    downvoteCount = (post.downvoteCount + downvoteDiff).coerceAtLeast(0)
                )
            } else post
        }
    }

    fun toggleBookmark(postId: String) {
        _postsFlow.value = _postsFlow.value.map { post ->
            if (post.id == postId) {
                post.copy(isBookmarked = !post.isBookmarked)
            } else post
        }
    }

    fun toggleVideoMute(postId: String) {
        _postsFlow.value = _postsFlow.value.map { post ->
            if (post.id == postId) {
                post.copy(isMuted = !post.isMuted)
            } else post
        }
    }

    fun toggleVideoPlay(postId: String) {
        _postsFlow.value = _postsFlow.value.map { post ->
            if (post.id == postId) {
                post.copy(isPlaying = !post.isPlaying)
            } else {
                // Pause other videos so only one plays at a time
                if (post.isPlaying) post.copy(isPlaying = false) else post
            }
        }
    }

    fun syncUserInteractions(likedIds: Set<String>, bookmarkedIds: Set<String>) {
        _postsFlow.value = _postsFlow.value.map { post ->
            post.copy(
                isLiked = likedIds.contains(post.id) || post.isLiked,
                isBookmarked = bookmarkedIds.contains(post.id) || post.isBookmarked
            )
        }
    }

    fun repost(postId: String) {
        _postsFlow.value = _postsFlow.value.map { post ->
            if (post.id == postId) {
                post.copy(repostCount = post.repostCount + 1)
            } else post
        }
    }

    fun addComment(postId: String, parentId: String?, content: String) {
        val newComment = Comment(
            id = "c_${UUID.randomUUID().toString().take(6)}",
            author = currentUser,
            text = content,
            timestamp = "Just now",
            upvotes = 1,
            userVote = 1,
            depth = if (parentId != null) 1 else 0
        )

        _postsFlow.value = _postsFlow.value.map { post ->
            if (post.id == postId) {
                val updatedComments = post.comments + newComment
                post.copy(
                    comments = updatedComments,
                    commentCount = post.commentCount + 1
                )
            } else post
        }
    }

    fun createPost(
        format: PostFormat,
        title: String?,
        body: String,
        mediaUrls: List<String>,
        community: Community?,
        tags: List<String>
    ): FeedPost {
        val newPost = FeedPost(
            id = "post_${format.name.lowercase()}_${UUID.randomUUID().toString().take(6)}",
            author = currentUser,
            format = format,
            community = community,
            title = title,
            body = body,
            mediaUrls = mediaUrls,
            videoDuration = if (format == PostFormat.TUBE) "1:15" else null,
            tags = tags,
            likeCount = 1,
            isLiked = true,
            userVote = if (format == PostFormat.FORUM) 1 else 0,
            upvoteCount = if (format == PostFormat.FORUM) 1 else 0,
            isPlaying = if (format == PostFormat.TUBE) true else false,
            isMuted = false,
            createdAt = "Just now"
        )

        _postsFlow.value = listOf(newPost) + _postsFlow.value
        return newPost
    }
}
