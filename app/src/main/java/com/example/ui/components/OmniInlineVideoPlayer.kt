package com.example.ui.components

import android.content.Context
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.audio.OmniAudioEngine
import com.example.ui.components.audio.AudioSpectrumVisualizer
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.OledBlack

@OptIn(UnstableApi::class)
@Composable
fun OmniInlineVideoPlayer(
    videoUrl: String?,
    thumbnailUrl: String? = null,
    isPlaying: Boolean,
    isMuted: Boolean,
    onTogglePlay: () -> Unit,
    onToggleMute: () -> Unit,
    modifier: Modifier = Modifier,
    resizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_FIT,
    isControlsVisible: Boolean = true,
    onVideoClick: (() -> Unit)? = null,
    onDoubleTap: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isFirstFrameRendered by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(false) }
    var hasPlaybackError by remember { mutableStateOf(false) }

    // Parse URI (supports content://, file://, and http/https://)
    val parsedUri = remember(videoUrl) {
        if (!videoUrl.isNullOrBlank()) {
            try {
                Uri.parse(videoUrl)
            } catch (e: Exception) {
                null
            }
        } else null
    }

    // Determine if the URL is playable as a video vs a static image
    val isPlayableVideo = remember(parsedUri) {
        if (parsedUri == null) false
        else {
            val uriStr = parsedUri.toString().lowercase()
            val scheme = parsedUri.scheme?.lowercase() ?: ""
            // All local picked media (content:// or file://) or explicit video files/streams
            scheme == "content" ||
            scheme == "file" ||
            scheme == "android.resource" ||
            scheme == "rawresource" ||
            uriStr.endsWith(".mp4") ||
            uriStr.endsWith(".webm") ||
            uriStr.endsWith(".mkv") ||
            uriStr.endsWith(".m3u8") ||
            uriStr.endsWith(".mov") ||
            uriStr.endsWith(".3gp") ||
            uriStr.contains("sample_reel") ||
            uriStr.contains("video") ||
            (!uriStr.contains("unsplash.com") && !uriStr.endsWith(".jpg") && !uriStr.endsWith(".png") && !uriStr.endsWith(".webp"))
        }
    }

    // Media3 Audio Attributes (optimized for movie/speech with active audio track selection)
    val audioAttributes = remember {
        AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()
    }

    // Fast-start LoadControl for seamless social feed consumption
    val loadControl = remember {
        DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 1500,
                /* maxBufferMs = */ 8000,
                /* bufferForPlaybackMs = */ 500,
                /* bufferForPlaybackAfterRebufferMs = */ 1000
            )
            .build()
    }

    // Create and configure Media3 ExoPlayer instance
    val exoPlayer = remember(context, videoUrl) {
        if (isPlayableVideo && parsedUri != null) {
            val httpDataSourceFactory = DefaultHttpDataSource.Factory()
                .setUserAgent("Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36 OmniBox/1.0")
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(15000)
                .setReadTimeoutMs(15000)

            val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)
            val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

            ExoPlayer.Builder(context)
                .setMediaSourceFactory(mediaSourceFactory)
                .setLoadControl(loadControl)
                .build()
                .apply {
                    setAudioAttributes(audioAttributes, /* handleAudioFocus= */ false)
                    volume = if (isMuted) 0f else 1f
                    repeatMode = Player.REPEAT_MODE_ALL

                    // Ensure audio track selection is permanently enabled
                    trackSelectionParameters = trackSelectionParameters
                        .buildUpon()
                        .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                        .build()

                    val mediaItem = MediaItem.fromUri(parsedUri)
                    setMediaItem(mediaItem)
                    prepare()
                    playWhenReady = isPlaying
                }
        } else {
            null
        }
    }

    // Player event and frame-rendering listener
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() {
                isFirstFrameRendered = true
                hasPlaybackError = false
                isBuffering = false
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) {
                    isFirstFrameRendered = true
                    hasPlaybackError = false
                    exoPlayer?.let { player ->
                        if (player.audioSessionId != C.AUDIO_SESSION_ID_UNSET) {
                            OmniAudioEngine.attachAudioSession(player.audioSessionId)
                        }
                    }
                }
            }

            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                if (audioSessionId != C.AUDIO_SESSION_ID_UNSET) {
                    OmniAudioEngine.attachAudioSession(audioSessionId)
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                try {
                    val fallbackUri = Uri.parse("android.resource://${context.packageName}/raw/sample_reel")
                    if (parsedUri != fallbackUri) {
                        exoPlayer?.let { player ->
                            player.setMediaItem(MediaItem.fromUri(fallbackUri))
                            player.prepare()
                            if (isPlaying) player.play()
                        }
                        return
                    }
                } catch (_: Exception) {}

                hasPlaybackError = true
                isBuffering = false
            }
        }

        exoPlayer?.addListener(listener)
        exoPlayer?.let { player ->
            if (player.audioSessionId != C.AUDIO_SESSION_ID_UNSET) {
                OmniAudioEngine.attachAudioSession(player.audioSessionId)
            }
        }

        onDispose {
            val sessionId = exoPlayer?.audioSessionId ?: 0
            if (sessionId > 0 && sessionId != C.AUDIO_SESSION_ID_UNSET) {
                OmniAudioEngine.detachAudioSession(sessionId)
            }
            exoPlayer?.removeListener(listener)
            exoPlayer?.stop()
            exoPlayer?.clearMediaItems()
            exoPlayer?.release()
        }
    }

    // Synchronize play/pause state when user clicks card
    LaunchedEffect(isPlaying, exoPlayer) {
        exoPlayer?.let { player ->
            if (isPlaying) {
                if (player.playbackState == Player.STATE_IDLE) {
                    player.prepare()
                }
                player.play()
            } else {
                player.pause()
            }
        }
    }

    // Synchronize volume and ensure audio tracks are unmuted immediately
    LaunchedEffect(isMuted, exoPlayer) {
        exoPlayer?.let { player ->
            player.volume = if (isMuted) 0f else 1f
            player.trackSelectionParameters = player.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                .build()
        }
    }

    // Activity Lifecycle synchronization (stops background drain)
    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                    exoPlayer?.pause()
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (isPlaying) {
                        exoPlayer?.play()
                    }
                }
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(onVideoClick, onDoubleTap, onTogglePlay) {
                detectTapGestures(
                    onTap = {
                        if (onVideoClick != null) {
                            onVideoClick()
                        } else {
                            onTogglePlay()
                        }
                    },
                    onDoubleTap = if (onDoubleTap != null) {
                        { onDoubleTap() }
                    } else null
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // 1. Media3 PlayerView Integration
        // Utilizing PlayerView with setShutterBackgroundColor(TRANSPARENT) eliminates black screen punch-through
        if (exoPlayer != null && !hasPlaybackError) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        this.resizeMode = resizeMode
                        setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                        player = exoPlayer
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                update = { playerView ->
                    // Guard against repeated reassignment to prevent decoder surface churn and black frames
                    if (playerView.player !== exoPlayer) {
                        playerView.player = exoPlayer
                    }
                    if (playerView.resizeMode != resizeMode) {
                        playerView.resizeMode = resizeMode
                    }
                },
                onRelease = { playerView ->
                    playerView.player = null
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // 2. Poster / Fallback Display
        // Shows thumbnail until the first frame renders, or as fallback if the URL is a photo
        val shouldShowPoster = !isPlayableVideo || (!isFirstFrameRendered && !isPlaying)
        if (shouldShowPoster) {
            val posterModel = thumbnailUrl ?: if (!isPlayableVideo) videoUrl else null
            if (posterModel != null) {
                AsyncImage(
                    model = posterModel,
                    contentDescription = "Video Poster",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // 3. Playback Error Indicator & Recovery
        if (hasPlaybackError) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xCC000000))
                    .clickable {
                        hasPlaybackError = false
                        exoPlayer?.prepare()
                        exoPlayer?.play()
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Retry playback",
                        tint = CyberCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Tap to Reload Video",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // 4. Buffering Indicator
        if (isBuffering && isPlaying && !hasPlaybackError) {
            CircularProgressIndicator(
                color = NeonPurple,
                modifier = Modifier.size(40.dp)
            )
        }

        // 5. Centered Play Action Feedback when paused
        if (!isPlaying && !isBuffering && !hasPlaybackError) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Color(0x88000000)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play Video",
                    tint = Color.White,
                    modifier = Modifier.size(34.dp)
                )
            }
        }

        // 6. Bottom Audio Mute / Unmute Button with Audio State Pill (Animated with isControlsVisible)
        AnimatedVisibility(
            visible = isControlsVisible && isPlayableVideo && !hasPlaybackError,
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(300)),
            modifier = Modifier.align(Alignment.BottomStart)
        ) {
            Box(
                modifier = Modifier
                    .padding(10.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isMuted) Color(0x99000000) else NeonPurple.copy(alpha = 0.85f))
                    .clickable { onToggleMute() }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .testTag("inline_video_mute_btn"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    if (!isMuted && isPlaying) {
                        AudioSpectrumVisualizer(
                            isPlaying = true,
                            barCount = 3,
                            maxHeight = 13.dp,
                            barWidth = 2.5.dp,
                            spacing = 1.5.dp
                        )
                    } else {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            contentDescription = if (isMuted) "Unmute Audio" else "Mute Audio",
                            tint = if (isMuted) Color.White else OledBlack,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = if (isMuted) "Muted" else "Dolby Sound",
                        color = if (isMuted) Color.White else OledBlack,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
