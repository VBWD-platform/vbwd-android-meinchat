package com.vbwd.plugin.meinchat.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MessageCacheTest {
    private fun msg(id: String, conv: String) = ChatMessage(id = id, conversationId = conv)

    @Test
    fun `upsert then list by conversation`() {
        val cache = InMemoryMessageCache()
        cache.upsert(listOf(msg("a", "c1"), msg("b", "c2")), atMillis = 1000)
        assertEquals(listOf("a"), cache.list("c1").map { it.id })
    }

    @Test
    fun `remove drops a conversation`() {
        val cache = InMemoryMessageCache()
        cache.upsert(listOf(msg("a", "c1")), atMillis = 1000)
        cache.remove("c1")
        assertTrue(cache.list("c1").isEmpty())
    }

    @Test
    fun `evict removes rows older than the cutoff`() {
        val cache = InMemoryMessageCache()
        cache.upsert(listOf(msg("old", "c1")), atMillis = 1000)
        cache.upsert(listOf(msg("new", "c1")), atMillis = 5000)
        assertEquals(1, cache.evict(olderThanMillis = 3000))
        assertEquals(listOf("new"), cache.list("c1").map { it.id })
    }
}
