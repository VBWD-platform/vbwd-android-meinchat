package com.vbwd.plugin.meinchat.domain

import java.net.URLEncoder

/** API endpoint paths for the meinchat plugin. Port of the iOS `MeinChatEndpoints`. */
internal object MeinChatEndpoints {
    const val MY_NICKNAME = "/nickname/me"
    fun searchNickname(query: String): String =
        "/nickname/search?q=${URLEncoder.encode(query, Charsets.UTF_8.name())}"

    const val CONVERSATIONS = "/messaging/conversations"
    fun messages(convId: String): String = "/messaging/conversations/$convId/messages"
    fun messagesPage(convId: String, before: String, limit: Int): String =
        "/messaging/conversations/$convId/messages?before=$before&limit=$limit"
    fun markRead(convId: String): String = "/messaging/conversations/$convId/read"

    const val TOKEN_TRANSFER = "/token-transfer"
    const val LIMITS = "/messaging/limits"
    const val CAPABILITIES = "/messaging/capabilities?me=true"
    const val BOT_STYLE_ACTIVE = "/bot-conversation-style/active"

    const val DEVICE_REGISTER = "/devices/register"
    fun deviceUnregister(token: String): String = "/devices/$token"

    const val ROOMS = "/messaging/rooms"
    fun roomMessages(roomId: String): String = "/messaging/rooms/$roomId/messages"
    fun roomRead(roomId: String): String = "/messaging/rooms/$roomId/read"
}
