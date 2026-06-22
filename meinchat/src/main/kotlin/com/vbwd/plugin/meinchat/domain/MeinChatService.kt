package com.vbwd.plugin.meinchat.domain

import com.vbwd.core.networking.ApiClient
import com.vbwd.core.networking.EmptyResponse
import com.vbwd.core.networking.delete
import com.vbwd.core.networking.get
import com.vbwd.core.networking.post
import com.vbwd.core.networking.put
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** meinchat API operations (DIP — testable). Port of the iOS protocol (focused subset). */
interface MeinChatService {
    suspend fun fetchConversations(): List<Conversation>
    suspend fun openConversation(peerNickname: String): Conversation
    suspend fun fetchMessages(convId: String, before: String? = null, limit: Int = DEFAULT_LIMIT): List<ChatMessage>
    suspend fun sendText(convId: String, body: String, meta: MessageMeta? = null): ChatMessage
    suspend fun markRead(convId: String)
    suspend fun searchNickname(query: String): List<NicknameSearchHit>
    suspend fun transferTokens(toNickname: String, amount: Int, note: String? = null): TokenTransferResult
    suspend fun fetchMyNickname(): String?
    suspend fun setNickname(nickname: String): NicknameRecord
    suspend fun fetchRooms(): List<Room>
    suspend fun fetchRoomMessages(roomId: String, before: String? = null, limit: Int = DEFAULT_LIMIT): List<ChatMessage>
    suspend fun sendRoomText(roomId: String, body: String, meta: MessageMeta? = null): ChatMessage
    suspend fun registerDeviceToken(tokenHex: String, app: String)
    suspend fun unregisterDeviceToken(tokenHex: String)

    companion object {
        const val DEFAULT_LIMIT = 50
    }
}

class DefaultMeinChatService(private val api: ApiClient) : MeinChatService {

    override suspend fun fetchConversations(): List<Conversation> =
        api.get<ConversationsResponse>(MeinChatEndpoints.CONVERSATIONS).items ?: emptyList()

    override suspend fun openConversation(peerNickname: String): Conversation =
        api.post(MeinChatEndpoints.CONVERSATIONS, OpenBody(peerNickname))

    override suspend fun fetchMessages(convId: String, before: String?, limit: Int): List<ChatMessage> {
        val path = if (before != null) {
            MeinChatEndpoints.messagesPage(convId, before, limit)
        } else {
            MeinChatEndpoints.messages(convId)
        }
        return api.get<MessagesResponse>(path).items ?: emptyList()
    }

    override suspend fun sendText(convId: String, body: String, meta: MessageMeta?): ChatMessage =
        api.post(MeinChatEndpoints.messages(convId), SendBody(body, meta))

    override suspend fun markRead(convId: String) {
        api.post<EmptyBody, EmptyResponse>(MeinChatEndpoints.markRead(convId), EmptyBody())
    }

    override suspend fun searchNickname(query: String): List<NicknameSearchHit> =
        api.get<NicknameSearchResponse>(MeinChatEndpoints.searchNickname(query)).items ?: emptyList()

    override suspend fun transferTokens(toNickname: String, amount: Int, note: String?): TokenTransferResult =
        api.post(MeinChatEndpoints.TOKEN_TRANSFER, TransferBody(toNickname, amount, note))

    override suspend fun fetchMyNickname(): String? =
        api.get<MyNicknameResponse>(MeinChatEndpoints.MY_NICKNAME).nickname

    override suspend fun setNickname(nickname: String): NicknameRecord =
        api.put(MeinChatEndpoints.MY_NICKNAME, NickBody(nickname))

    override suspend fun fetchRooms(): List<Room> =
        api.get<RoomsResponse>(MeinChatEndpoints.ROOMS).items ?: emptyList()

    override suspend fun fetchRoomMessages(roomId: String, before: String?, limit: Int): List<ChatMessage> =
        api.get<MessagesResponse>(MeinChatEndpoints.roomMessages(roomId)).items ?: emptyList()

    override suspend fun sendRoomText(roomId: String, body: String, meta: MessageMeta?): ChatMessage =
        api.post(MeinChatEndpoints.roomMessages(roomId), SendBody(body, meta))

    override suspend fun registerDeviceToken(tokenHex: String, app: String) {
        api.post<RegisterBody, EmptyResponse>(MeinChatEndpoints.DEVICE_REGISTER, RegisterBody(tokenHex, app))
    }

    override suspend fun unregisterDeviceToken(tokenHex: String) {
        api.delete<EmptyResponse>(MeinChatEndpoints.deviceUnregister(tokenHex))
    }

    @Serializable private class EmptyBody
    @Serializable private data class OpenBody(@SerialName("peer_nickname") val peerNickname: String)
    @Serializable private data class SendBody(val body: String, val meta: MessageMeta? = null)
    @Serializable private data class NickBody(val nickname: String)
    @Serializable private data class TransferBody(
        @SerialName("recipient_nickname") val recipientNickname: String,
        val amount: Int,
        val note: String? = null,
    )
    @Serializable private data class RegisterBody(val token: String, val app: String)
}

@Serializable
internal data class NicknameSearchResponse(val items: List<NicknameSearchHit>? = null)
