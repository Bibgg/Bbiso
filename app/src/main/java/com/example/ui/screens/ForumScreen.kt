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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.data.model.Community
import com.example.data.model.FeedPost
import com.example.data.model.PostFormat
import com.example.ui.components.cards.ForumPostCard
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceMedium
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.OledBlack
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.UpvoteOrange

@Composable
fun ForumScreen(
    posts: List<FeedPost>,
    communities: List<Community>,
    currentSort: String,
    onSortChange: (String) -> Unit,
    onVote: (String, Int) -> Unit,
    onCommentClick: (String) -> Unit,
    onBookmarkClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val forumPosts = posts.filter { it.format == PostFormat.FORUM }
    var selectedCommunitySlug by remember { mutableStateOf<String?>(null) } // null means "c/all"

    val sortOptions = listOf("Hot 🔥", "Top 🏆", "New ⚡")

    val communityFilteredPosts = if (selectedCommunitySlug != null) {
        forumPosts.filter { it.community?.slug == selectedCommunitySlug }
    } else {
        forumPosts
    }

    // Sort posts according to active sort
    val sortedPosts = when {
        currentSort.contains("Top") -> communityFilteredPosts.sortedByDescending { it.upvoteCount - it.downvoteCount }
        currentSort.contains("New") -> communityFilteredPosts.reversed()
        else -> communityFilteredPosts // Hot
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(OledBlack),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Community Chips Bar
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
            ) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // "c/all" chip
                    item {
                        val isAllSelected = selectedCommunitySlug == null
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isAllSelected) UpvoteOrange else DarkSurfaceMedium)
                                .clickable { selectedCommunitySlug = null }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = "c/all",
                                color = if (isAllSelected) OledBlack else TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Individual Communities
                    items(communities) { comm ->
                        val isSelected = selectedCommunitySlug == comm.slug
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) UpvoteOrange else DarkSurfaceMedium)
                                .clickable { selectedCommunitySlug = comm.slug }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            AsyncImage(
                                model = comm.iconUrl,
                                contentDescription = comm.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = comm.slug,
                                color = if (isSelected) OledBlack else TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Sort Bar (Hot, Top, New)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    sortOptions.forEach { sort ->
                        val isSelected = currentSort.startsWith(sort.take(3))
                        Text(
                            text = sort,
                            color = if (isSelected) UpvoteOrange else TextMuted,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier
                                .clickable { onSortChange(sort) }
                                .padding(vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .background(DarkBorder)
                )
            }
        }

        // Thread List
        if (sortedPosts.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "No threads found in this community", color = TextSecondary)
                }
            }
        } else {
            items(sortedPosts, key = { it.id }) { post ->
                ForumPostCard(
                    post = post,
                    onVote = { dir -> onVote(post.id, dir) },
                    onCommentClick = { onCommentClick(post.id) },
                    onBookmarkClick = { onBookmarkClick(post.id) }
                )
            }
        }
    }
}
