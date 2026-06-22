package com.vbwd.plugin.meinchat.notifications

import com.vbwd.plugin.meinchat.domain.ChatMessage
import com.vbwd.plugin.meinchat.domain.Conversation
import com.vbwd.plugin.meinchat.domain.MeinChatService
import com.vbwd.plugin.meinchat.domain.MessageMeta
import com.vbwd.plugin.meinchat.domain.NicknameRecord
import com.vbwd.plugin.meinchat.domain.NicknameSearchHit
import com.vbwd.plugin.meinchat.domain.Room
import com.vbwd.plugin.meinchat.domain.TokenTransferResult
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

private class RecordingService : MeinChatService {
    val registered = mutableListOf<String>()
    var unregisterCount = 0
    override suspend fun registerDeviceToken(tokenHex: String, app: String) { registered.add(tokenHex) }
    override suspend fun unregisterDeviceToken(tokenHex: String) { unregisterCount++ }

    // unused for this test
    override suspend fun fetchConversations(): List<Conversation> = emptyList()
    override suspend fun openConversation(peerNickname: String) = Conversation(id = "c")
    override suspend fun fetchMessages(convId: String, before: String?, limit: Int): List<ChatMessage> = emptyList()
    override suspend fun sendText(convId: String, body: String, meta: MessageMeta?) =
        ChatMessage(id = "m")
    override suspend fun markRead(convId: String) = Unit
    override suspend fun searchNickname(query: String): List<NicknameSearchHit> = emptyList()
    override suspend fun transferTokens(toNickname: String, amount: Int, note: String?) =
        TokenTransferResult(amount = amount)
    override suspend fun fetchMyNickname(): String? = null
    override suspend fun setNickname(nickname: String) = NicknameRecord(id = "n", nickname = nickname)
    override suspend fun fetchRooms(): List<Room> = emptyList()
    override suspend fun fetchRoomMessages(roomId: String, before: String?, limit: Int): List<ChatMessage> = emptyList()
    override suspend fun sendRoomText(roomId: String, body: String, meta: MessageMeta?) =
        ChatMessage(id = "m")
}

class MeinChatTokenSinkTest {
    @Test
    fun `the token is buffered until login, then registered`() = runTest {
        val service = RecordingService()
        val sink = MeinChatTokenSink(service)

        sink.handleDeviceToken("TOK")
        assertEquals(emptyList<String>(), service.registered) // not authenticated yet

        sink.handleLogin()
        assertEquals(listOf("TOK"), service.registered)

        sink.handleLogout()
        assertEquals(1, service.unregisterCount)
    }
}
