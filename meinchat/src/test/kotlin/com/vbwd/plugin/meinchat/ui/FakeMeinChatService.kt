package com.vbwd.plugin.meinchat.ui

import com.vbwd.plugin.meinchat.domain.ChatMessage
import com.vbwd.plugin.meinchat.domain.Conversation
import com.vbwd.plugin.meinchat.domain.MeinChatService
import com.vbwd.plugin.meinchat.domain.MessageMeta
import com.vbwd.plugin.meinchat.domain.NicknameRecord
import com.vbwd.plugin.meinchat.domain.NicknameSearchHit
import com.vbwd.plugin.meinchat.domain.Room
import com.vbwd.plugin.meinchat.domain.TokenTransferResult

/** In-memory [MeinChatService] for UI tests — canned data, no network. */
class FakeMeinChatService(
    private val conversations: List<Conversation> = emptyList(),
    private val rooms: List<Room> = emptyList(),
    private val messages: List<ChatMessage> = emptyList(),
    private val nickname: String? = null,
    private val searchHits: List<NicknameSearchHit> = emptyList(),
) : MeinChatService {
    override suspend fun fetchConversations(): List<Conversation> = conversations

    override suspend fun openConversation(peerNickname: String): Conversation =
        Conversation(id = "conv-$peerNickname", peerNickname = peerNickname)

    override suspend fun fetchMessages(
        convId: String,
        before: String?,
        limit: Int,
    ): List<ChatMessage> = messages

    override suspend fun sendText(
        convId: String,
        body: String,
        meta: MessageMeta?,
    ): ChatMessage = ChatMessage(id = "sent", body = body)

    override suspend fun markRead(convId: String) = Unit

    override suspend fun searchNickname(query: String): List<NicknameSearchHit> = searchHits

    override suspend fun transferTokens(
        toNickname: String,
        amount: Int,
        note: String?,
    ): TokenTransferResult = TokenTransferResult(amount = amount)

    override suspend fun fetchMyNickname(): String? = nickname

    override suspend fun setNickname(nickname: String): NicknameRecord = NicknameRecord(id = "n1", nickname = nickname)

    override suspend fun fetchRooms(): List<Room> = rooms

    override suspend fun fetchRoomMessages(
        roomId: String,
        before: String?,
        limit: Int,
    ): List<ChatMessage> = messages

    override suspend fun sendRoomText(
        roomId: String,
        body: String,
        meta: MessageMeta?,
    ): ChatMessage = ChatMessage(id = "sent", body = body)

    override suspend fun registerDeviceToken(
        tokenHex: String,
        app: String,
    ) = Unit

    override suspend fun unregisterDeviceToken(tokenHex: String) = Unit
}
