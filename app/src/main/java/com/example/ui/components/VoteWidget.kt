package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceMedium
import com.example.ui.theme.DownvoteBlue
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.UpvoteOrange

@Composable
fun VoteWidget(
    score: Int,
    userVote: Int, // +1, -1, 0
    onVote: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val upvoteColor by animateColorAsState(
        targetValue = if (userVote == 1) UpvoteOrange else TextMuted,
        label = "upvoteColor"
    )
    val downvoteColor by animateColorAsState(
        targetValue = if (userVote == -1) DownvoteBlue else TextMuted,
        label = "downvoteColor"
    )
    val scoreColor by animateColorAsState(
        targetValue = when (userVote) {
            1 -> UpvoteOrange
            -1 -> DownvoteBlue
            else -> TextPrimary
        },
        label = "scoreColor"
    )

    val scaleUp by animateFloatAsState(
        targetValue = if (userVote == 1) 1.25f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "upScale"
    )
    val scaleDown by animateFloatAsState(
        targetValue = if (userVote == -1) 1.25f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "downScale"
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(DarkSurfaceMedium)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Upvote button
        Icon(
            imageVector = Icons.Default.ArrowUpward,
            contentDescription = "Upvote",
            tint = upvoteColor,
            modifier = Modifier
                .size(20.dp)
                .scale(scaleUp)
                .clickable { onVote(1) }
                .testTag("vote_up_btn")
        )

        // Net score
        Text(
            text = formatScore(score),
            color = scoreColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 2.dp)
        )

        // Downvote button
        Icon(
            imageVector = Icons.Default.ArrowDownward,
            contentDescription = "Downvote",
            tint = downvoteColor,
            modifier = Modifier
                .size(20.dp)
                .scale(scaleDown)
                .clickable { onVote(-1) }
                .testTag("vote_down_btn")
        )
    }
}

private fun formatScore(score: Int): String {
    return when {
        score >= 10000 -> String.format("%.1fk", score / 1000.0)
        score >= 1000 -> String.format("%.1fk", score / 1000.0)
        else -> score.toString()
    }
}
