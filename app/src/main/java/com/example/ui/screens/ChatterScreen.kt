package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingUp
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FeedPost
import com.example.data.model.PostFormat
import com.example.ui.components.cards.ChatterPostCard
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceMedium
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.OledBlack
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ChatterScreen(
    posts: List<FeedPost>,
    onLikeClick: (String) -> Unit,
    onCommentClick: (String) -> Unit,
    onRepostClick: (String) -> Unit,
    onBookmarkClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val chatterPosts = posts.filter { it.format == PostFormat.CHATTER }

    val trendingTopics = listOf(
        "#AI" to "242K Chatters",
        "#EdgeComputing" to "89K Chatters",
        "#ComposeMultiplatform" to "56K Chatters",
        "#SciFiReality" to "114K Chatters",
        "#OmniBox" to "18K Chatters"
    )

    var activeTrend by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(OledBlack),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Trending Radar Section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = "Trending",
                        tint = CyberCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = " Live Trending Radar",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(top = 6.dp)
                ) {
                    items(trendingTopics) { (tag, count) ->
                        val isSelected = activeTrend == tag
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) NeonPurple else DarkSurfaceMedium)
                                .clickable { activeTrend = if (isSelected) null else tag }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Column {
                                Text(
                                    text = tag,
                                    color = if (isSelected) OledBlack else CyberCyan,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = count,
                                    color = if (isSelected) OledBlack else TextMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .background(DarkBorder)
                )
            }
        }

        // Microblog stream
        val displayedPosts = if (activeTrend != null) {
            chatterPosts.filter { post ->
                post.tags.any { "#$it".equals(activeTrend, ignoreCase = true) } ||
                post.body.contains(activeTrend!!, ignoreCase = true)
            }
        } else {
            chatterPosts
        }

        if (displayedPosts.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "No chatters under $activeTrend", color = TextSecondary)
                }
            }
        } else {
            items(displayedPosts, key = { it.id }) { post ->
                ChatterPostCard(
                    post = post,
                    onLikeClick = { onLikeClick(post.id) },
                    onCommentClick = { onCommentClick(post.id) },
                    onRepostClick = { onRepostClick(post.id) },
                    onBookmarkClick = { onBookmarkClick(post.id) }
                )
            }
        }
    }
}
