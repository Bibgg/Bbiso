package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.media3.ui.AspectRatioFrameLayout
import com.example.ui.components.OmniInlineVideoPlayer
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.FeedPost
import com.example.data.model.PostFormat
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceMedium
import com.example.ui.theme.LikeMagenta
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.OledBlack
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun TubeScreen(
    posts: List<FeedPost>,
    onLikeClick: (String) -> Unit,
    onCommentClick: (String) -> Unit,
    onBookmarkClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    onControlsVisibilityChanged: (Boolean) -> Unit = {}
) {
    val tubePosts = posts.filter { it.format == PostFormat.TUBE }
    val pagerState = rememberPagerState(pageCount = { tubePosts.size.coerceAtLeast(1) })
    var isControlsVisible by remember { mutableStateOf(true) }

    // Synchronize visibility with host activity (for bottom navigation bar)
    LaunchedEffect(isControlsVisible) {
        onControlsVisibilityChanged(isControlsVisible)
    }

    // Reset controls to visible when user scrolls to a new video reel
    LaunchedEffect(pagerState.currentPage) {
        isControlsVisible = true
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (tubePosts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "No Tube videos available", color = TextSecondary)
            }
        } else {
            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val post = tubePosts[page]
                val isPageActive = page == pagerState.currentPage
                TubeReelItem(
                    post = post,
                    isPageActive = isPageActive,
                    isControlsVisible = isControlsVisible,
                    onControlsVisibilityChanged = { visible -> isControlsVisible = visible },
                    onLikeClick = { onLikeClick(post.id) },
                    onCommentClick = { onCommentClick(post.id) },
                    onBookmarkClick = { onBookmarkClick(post.id) }
                )
            }
        }

        // Top Header Overlay: "Tube Shorts" title and reels badge (AnimatedVisibility)
        AnimatedVisibility(
            visible = isControlsVisible,
            enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { -it },
            exit = fadeOut(tween(300)) + slideOutVertically(tween(300)) { -it },
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Tube",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(NeonPurple)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "REELS",
                            color = OledBlack,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TubeReelItem(
    post: FeedPost,
    isPageActive: Boolean,
    isControlsVisible: Boolean,
    onControlsVisibilityChanged: (Boolean) -> Unit,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onBookmarkClick: () -> Unit
) {
    var isPlaying by remember { mutableStateOf(isPageActive) }
    var isMuted by remember { mutableStateOf(false) }
    var isSubscribed by remember { mutableStateOf(false) }
    var heartTrigger by remember { androidx.compose.runtime.mutableLongStateOf(0L) }

    // Sync playback state when user scrolls to/from this page in the vertical pager
    LaunchedEffect(isPageActive) {
        isPlaying = isPageActive
    }

    // 3. Auto-Hide on Play: When the video starts playing or resumes,
    // automatically set isControlsVisible to false after a brief delay for uninterrupted immersive view
    LaunchedEffect(isPlaying, isControlsVisible) {
        if (isPlaying && isControlsVisible) {
            delay(3500L)
            onControlsVisibilityChanged(false)
        }
    }

    // 4. Show on Pause: When the video is paused, instantly bring back overlays
    LaunchedEffect(isPlaying) {
        if (!isPlaying) {
            onControlsVisibilityChanged(true)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "discAnim")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "discRotate"
    )

    val progress by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.98f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "reelProgress"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("tube_reel_${post.id}")
    ) {
        // Single-tap toggles playback controls; double-tap triggers bouncy neon heart & like counter
        OmniInlineVideoPlayer(
            videoUrl = post.mediaUrls.firstOrNull(),
            thumbnailUrl = post.mediaUrls.firstOrNull(),
            isPlaying = isPlaying,
            isMuted = isMuted,
            onTogglePlay = {
                isPlaying = !isPlaying
            },
            onToggleMute = { isMuted = !isMuted },
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
            isControlsVisible = isControlsVisible,
            onVideoClick = {
                if (!isPlaying) {
                    isPlaying = true
                    onControlsVisibilityChanged(true)
                } else {
                    onControlsVisibilityChanged(!isControlsVisible)
                }
            },
            onDoubleTap = {
                heartTrigger = System.currentTimeMillis()
                onLikeClick()
            },
            modifier = Modifier
                .fillMaxSize()
                .testTag("tube_video_surface")
        )

        // Bouncy Neon Heart Animation Overlay
        BouncyNeonHeartAnimation(
            trigger = heartTrigger,
            modifier = Modifier.align(Alignment.Center)
        )

        // Gradient overlay for readability of captions & action controls (fades with isControlsVisible)
        AnimatedVisibility(
            visible = isControlsVisible,
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(300))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0x55000000),
                                Color.Transparent,
                                Color(0x88000000),
                                Color(0xDD000000)
                            )
                        )
                    )
            )
        }

        // Play indicator if paused (Always prominent on pause so user knows how to resume)
        AnimatedVisibility(
            visible = !isPlaying,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color(0x99000000))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        isPlaying = true
                        onControlsVisibilityChanged(true)
                    }
                    .testTag("tube_play_indicator"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play Video",
                    tint = Color.White,
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        // Center Pause indicator when playing and controls are visible
        AnimatedVisibility(
            visible = isPlaying && isControlsVisible,
            enter = fadeIn(tween(250)),
            exit = fadeOut(tween(250)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Color(0x66000000))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        isPlaying = false
                        onControlsVisibilityChanged(true)
                    }
                    .testTag("tube_pause_indicator"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Pause,
                    contentDescription = "Pause Video",
                    tint = Color.White,
                    modifier = Modifier.size(34.dp)
                )
            }
        }

        // Bottom scrubbing progress line (fades in/out smoothly)
        AnimatedVisibility(
            visible = isControlsVisible,
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(300)),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp)
                    .padding(bottom = 80.dp),
                color = NeonPurple,
                trackColor = Color(0x33FFFFFF)
            )
        }

        // Right-Side Action Rail: Like, Comment, Bookmark, Share, Audio Disc
        AnimatedVisibility(
            visible = isControlsVisible,
            enter = fadeIn(tween(300)) + slideInHorizontally(tween(300)) { it },
            exit = fadeOut(tween(300)) + slideOutHorizontally(tween(300)) { it },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 90.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp),
                modifier = Modifier.testTag("tube_action_rail")
            ) {
                // Like
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color(0x44000000))
                            .clickable { onLikeClick() }
                            .testTag("tube_like_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (post.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (post.isLiked) LikeMagenta else Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Text(
                        text = "${post.likeCount}",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                // Comment
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color(0x44000000))
                            .clickable { onCommentClick() }
                            .testTag("tube_comment_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChatBubble,
                            contentDescription = "Comment",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        text = "${post.commentCount}",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                // Bookmark
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color(0x44000000))
                        .clickable { onBookmarkClick() }
                        .testTag("tube_bookmark_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (post.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Bookmark",
                        tint = if (post.isBookmarked) CyberCyan else Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Share
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color(0x44000000))
                        .clickable {}
                        .testTag("tube_share_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Spinning Audio Disc
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .rotate(rotation)
                        .clip(CircleShape)
                        .background(Color(0xFF1E1E28))
                        .border(2.dp, NeonPurple, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "Audio track",
                        tint = CyberCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Bottom Left Creator & Caption Overlay
        AnimatedVisibility(
            visible = isControlsVisible,
            enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 2 },
            exit = fadeOut(tween(300)) + slideOutVertically(tween(300)) { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.78f)
                .padding(start = 16.dp, bottom = 90.dp)
        ) {
            Column(modifier = Modifier.testTag("tube_creator_overlay")) {
                // Author info + Subscribe
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AsyncImage(
                        model = post.author.avatarUrl,
                        contentDescription = post.author.displayName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, NeonPurple, CircleShape)
                    )
                    Text(
                        text = post.author.displayName,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    if (post.author.isVerified) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Verified",
                            tint = CyberCyan,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSubscribed) DarkSurfaceMedium else NeonPurple)
                            .clickable { isSubscribed = !isSubscribed }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isSubscribed) "Subscribed" else "Subscribe",
                            color = if (isSubscribed) TextSecondary else OledBlack,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                post.title?.let { title ->
                    Text(
                        text = title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = post.body,
                    color = Color(0xDDFFFFFF),
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Audio track badge
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x66000000))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Original Audio • ${post.author.displayName}",
                        color = Color.White,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun BouncyNeonHeartAnimation(
    trigger: Long,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(false) }
    val scaleAnim = remember { Animatable(0.2f) }
    val alphaAnim = remember { Animatable(0f) }

    LaunchedEffect(trigger) {
        if (trigger > 0) {
            isVisible = true
            scaleAnim.snapTo(0.2f)
            alphaAnim.snapTo(1f)
            // Bouncy pop up to 1.4f with spring physics
            scaleAnim.animateTo(
                targetValue = 1.35f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
            scaleAnim.animateTo(1.0f, tween(150))
            delay(350)
            alphaAnim.animateTo(0f, tween(250))
            isVisible = false
        }
    }

    if (isVisible) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = scaleAnim.value
                        scaleY = scaleAnim.value
                        alpha = alphaAnim.value
                    }
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(Color(0x33FF007A))
                    .border(2.dp, LikeMagenta.copy(alpha = 0.8f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Liked",
                    tint = LikeMagenta,
                    modifier = Modifier.size(72.dp)
                )
            }
        }
    }
}

