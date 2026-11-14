package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.ViewDay
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.FeedPost
import com.example.data.model.PostFormat
import com.example.data.model.StoryItem
import com.example.data.model.User
import com.example.ui.components.StoryReelRow
import com.example.ui.components.cards.SnapPostCard
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceMedium
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.OledBlack
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SnapScreen(
    posts: List<FeedPost>,
    stories: List<StoryItem>,
    currentUser: User?,
    onStoryClick: (StoryItem) -> Unit,
    onAddStoryClick: () -> Unit,
    onLikeClick: (String) -> Unit,
    onCommentClick: (String) -> Unit,
    onBookmarkClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val snapPosts = posts.filter { it.format == PostFormat.SNAP }
    var isGridView by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(OledBlack)
    ) {
        // Stories Header
        StoryReelRow(
            currentUser = currentUser,
            stories = stories,
            onStoryClick = onStoryClick,
            onAddStoryClick = onAddStoryClick
        )

        // View toggle bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Snap Feed",
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurfaceMedium)
                    .padding(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (!isGridView) NeonPurple else DarkSurfaceMedium)
                        .clickable { isGridView = false }
                        .padding(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ViewDay,
                        contentDescription = "Feed View",
                        tint = if (!isGridView) OledBlack else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isGridView) NeonPurple else DarkSurfaceMedium)
                        .clickable { isGridView = true }
                        .padding(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.GridOn,
                        contentDescription = "Grid View",
                        tint = if (isGridView) OledBlack else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        if (isGridView) {
            // Instagram Explore Grid
            val allMedia = snapPosts.flatMap { post ->
                post.mediaUrls.map { url -> post to url }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(allMedia) { (post, url) ->
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clickable { onCommentClick(post.id) }
                    ) {
                        AsyncImage(
                            model = url,
                            contentDescription = "Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        } else {
            // Full Snap Card Feed
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(snapPosts, key = { it.id }) { post ->
                    SnapPostCard(
                        post = post,
                        onLikeClick = { onLikeClick(post.id) },
                        onCommentClick = { onCommentClick(post.id) },
                        onBookmarkClick = { onBookmarkClick(post.id) }
                    )
                }
            }
        }
    }
}
