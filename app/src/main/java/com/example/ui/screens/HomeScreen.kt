package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FeedPost
import com.example.data.model.PostFormat
import com.example.data.model.StoryItem
import com.example.data.model.User
import com.example.ui.components.StoryReelRow
import com.example.ui.components.cards.ChatterPostCard
import com.example.ui.components.cards.ForumPostCard
import com.example.ui.components.cards.SnapPostCard
import com.example.ui.components.cards.TubePostCard
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.OledBlack
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary

@Composable
fun HomeScreen(
    posts: List<FeedPost>,
    stories: List<StoryItem>,
    currentUser: User?,
    selectedFilter: PostFormat?,
    onStoryClick: (StoryItem) -> Unit,
    onAddStoryClick: () -> Unit,
    onLikeClick: (String) -> Unit,
    onVoteClick: (String, Int) -> Unit,
    onCommentClick: (String) -> Unit,
    onBookmarkClick: (String) -> Unit,
    onRepostClick: (String) -> Unit,
    onPlayToggle: (String) -> Unit,
    onMuteToggle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val filteredPosts = if (selectedFilter != null) {
        posts.filter { it.format == selectedFilter }
    } else {
        posts
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(OledBlack),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Top Story Reel (Show on All or Snap)
        if (selectedFilter == null || selectedFilter == PostFormat.SNAP) {
            item {
                StoryReelRow(
                    currentUser = currentUser,
                    stories = stories,
                    onStoryClick = onStoryClick,
                    onAddStoryClick = onAddStoryClick
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .background(DarkBorder)
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        // Empty state
        if (filteredPosts.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 60.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No posts found for this format",
                        color = TextSecondary,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tap the (+) button below to create one!",
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Interleaved Cards
        items(filteredPosts, key = { it.id }) { post ->
            when (post.format) {
                PostFormat.TUBE -> TubePostCard(
                    post = post,
                    onLikeClick = { onLikeClick(post.id) },
                    onCommentClick = { onCommentClick(post.id) },
                    onBookmarkClick = { onBookmarkClick(post.id) },
                    onPlayToggle = { onPlayToggle(post.id) },
                    onMuteToggle = { onMuteToggle(post.id) }
                )
                PostFormat.SNAP -> SnapPostCard(
                    post = post,
                    onLikeClick = { onLikeClick(post.id) },
                    onCommentClick = { onCommentClick(post.id) },
                    onBookmarkClick = { onBookmarkClick(post.id) }
                )
                PostFormat.CHATTER -> ChatterPostCard(
                    post = post,
                    onLikeClick = { onLikeClick(post.id) },
                    onCommentClick = { onCommentClick(post.id) },
                    onRepostClick = { onRepostClick(post.id) },
                    onBookmarkClick = { onBookmarkClick(post.id) }
                )
                PostFormat.FORUM -> ForumPostCard(
                    post = post,
                    onVote = { dir -> onVoteClick(post.id, dir) },
                    onCommentClick = { onCommentClick(post.id) },
                    onBookmarkClick = { onBookmarkClick(post.id) }
                )
            }
        }
    }
}
