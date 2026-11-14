package com.example

import com.example.data.model.PostFormat
import com.example.data.repository.OmniRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OmniRepositoryTest {

    private lateinit var repository: OmniRepository

    @Before
    fun setUp() {
        repository = OmniRepository()
    }

    @Test
    fun testInitialPostsNotEmpty() {
        val posts = repository.postsFlow.value
        assertTrue("Initial posts should not be empty", posts.isNotEmpty())
        assertTrue("Should contain TUBE post", posts.any { it.format == PostFormat.TUBE })
        assertTrue("Should contain SNAP post", posts.any { it.format == PostFormat.SNAP })
        assertTrue("Should contain CHATTER post", posts.any { it.format == PostFormat.CHATTER })
        assertTrue("Should contain FORUM post", posts.any { it.format == PostFormat.FORUM })
    }

    @Test
    fun testToggleLike() {
        val targetPost = repository.postsFlow.value.first()
        val initialLiked = targetPost.isLiked
        val initialCount = targetPost.likeCount

        repository.toggleLike(targetPost.id)
        val updatedPost = repository.postsFlow.value.first { it.id == targetPost.id }

        assertEquals(!initialLiked, updatedPost.isLiked)
        if (!initialLiked) {
            assertEquals(initialCount + 1, updatedPost.likeCount)
        }
    }

    @Test
    fun testCastVote() {
        val forumPost = repository.postsFlow.value.first { it.format == PostFormat.FORUM }
        val initialScore = forumPost.upvoteCount

        repository.castVote(forumPost.id, 1)
        val updated = repository.postsFlow.value.first { it.id == forumPost.id }
        assertEquals(1, updated.userVote)
    }

    @Test
    fun testCreatePost() {
        val initialSize = repository.postsFlow.value.size
        val newPost = repository.createPost(
            format = PostFormat.CHATTER,
            title = null,
            body = "Testing new chatter post on OmniBox!",
            mediaUrls = emptyList(),
            community = null,
            tags = listOf("Test")
        )

        assertNotNull(newPost)
        assertEquals(initialSize + 1, repository.postsFlow.value.size)
        assertEquals("Testing new chatter post on OmniBox!", repository.postsFlow.value.first().body)
    }

    @Test
    fun testOmniViewModelHasApplicationConstructor() {
        val constructor = com.example.ui.viewmodel.OmniViewModel::class.java.getConstructor(android.app.Application::class.java)
        assertNotNull("OmniViewModel must have a public (Application) constructor for ViewModelProvider", constructor)
    }
}

