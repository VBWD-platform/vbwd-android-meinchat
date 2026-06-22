package com.vbwd.plugin.meinchat.domain

/**
 * Cross-plugin contract implemented by `meinchat-plus` (A10) and consumed by
 * `meinchat` for `protocol == "e2e_v1"` conversations. The plus plugin registers
 * a concrete impl under [MeinChatSecureMessagingStoreId]; meinchat looks it up
 * at send/read time. Port of the iOS `MeinChatSecureMessaging` protocol.
 *
 * **Fail-closed contract:** implementations MUST throw rather than ever transmit
 * plaintext for an `e2e_v1` row.
 */
interface MeinChatSecureMessaging {
    /** True once the device is paired + keys minted. */
    suspend fun isReady(): Boolean

    /** Encrypts + fans out + posts plaintext for an `e2e_v1` conversation. */
    suspend fun sendSecure(plaintext: String, conversation: Conversation): ChatMessage

    /** Decrypts an incoming `e2e_v1` row, returning plaintext. */
    suspend fun decryptIncoming(message: ChatMessage): String

    /** True when the peer has at least one active device key. */
    suspend fun peerCanReceiveE2E(userId: String): Boolean
}

/** Store id under which `meinchat-plus` registers its [MeinChatSecureMessaging]. */
const val MeinChatSecureMessagingStoreId = "meinchatSecureMessaging"
