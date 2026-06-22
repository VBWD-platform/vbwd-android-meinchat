package com.vbwd.plugin.meinchat.domain

/**
 * At-rest message cache seam (S28.6). Port of the iOS `MessageCacheProtocol`.
 *
 * The encrypted-at-rest persistent store (iOS CoreData + ChaChaPoly under a
 * Keystore KEK) is **deferred** to a follow-up: it needs a Room/SQLCipher +
 * Android-Keystore KEK that must be built and verified with the SDK present.
 * This in-memory impl carries the testable upsert/list/remove/evict semantics;
 * the plugin degrades to the no-cache path if a persistent store is absent.
 */
interface MessageCache {
    fun upsert(rows: List<ChatMessage>, atMillis: Long)
    fun list(conversationId: String, limit: Int? = null): List<ChatMessage>
    fun remove(conversationId: String)
    /** Removes rows cached before [olderThanMillis]; returns the number evicted. */
    fun evict(olderThanMillis: Long): Int
}

class InMemoryMessageCache : MessageCache {
    private data class Entry(val message: ChatMessage, val atMillis: Long)

    private val rows = mutableListOf<Entry>()

    override fun upsert(rows: List<ChatMessage>, atMillis: Long) {
        rows.forEach { message ->
            this.rows.removeAll { it.message.id == message.id }
            this.rows.add(Entry(message, atMillis))
        }
    }

    override fun list(conversationId: String, limit: Int?): List<ChatMessage> {
        val matching = rows.filter { it.message.conversationId == conversationId }.map { it.message }
        return if (limit != null) matching.takeLast(limit) else matching
    }

    override fun remove(conversationId: String) {
        rows.removeAll { it.message.conversationId == conversationId }
    }

    override fun evict(olderThanMillis: Long): Int {
        val before = rows.size
        rows.removeAll { it.atMillis < olderThanMillis }
        return before - rows.size
    }
}
