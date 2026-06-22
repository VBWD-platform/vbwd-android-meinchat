package com.vbwd.plugin.meinchat.domain

import com.vbwd.core.networking.ApiClient
import com.vbwd.core.networking.get
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Read-model for `GET /messaging/limits` (S28.0). Port of the iOS `MessagingLimits`. */
@Serializable
data class MessagingLimits(
    @SerialName("messages_retention_days_server") val messagesRetentionDaysServer: Int,
    @SerialName("messages_retention_days_client_suggested") val messagesRetentionDaysClientSuggested: Int,
    @SerialName("attachments_retention_days_server") val attachmentsRetentionDaysServer: Int,
    @SerialName("ciphertext_max_bytes") val ciphertextMaxBytes: Int,
) {
    companion object {
        val DEFAULT = MessagingLimits(
            messagesRetentionDaysServer = 2,
            messagesRetentionDaysClientSuggested = 10,
            attachmentsRetentionDaysServer = 2,
            ciphertextMaxBytes = 65_536,
        )
    }
}

@Serializable
data class MessagingCapabilities(val server: List<String>, val me: List<String>? = null)

/** Limits + capabilities. Port of the iOS `MeinChatLimitsService`. */
interface MeinChatLimitsService {
    val current: MessagingLimits?
    val serverCapabilities: List<String>
    val myCapabilities: List<String>
    suspend fun refresh(): MessagingLimits
}

class DefaultMeinChatLimitsService(
    private val api: ApiClient,
    private val cacheTtlMillis: Long = DEFAULT_TTL_MILLIS,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) : MeinChatLimitsService {

    override var current: MessagingLimits? = null
        private set
    override var serverCapabilities: List<String> = listOf("plain")
        private set
    override var myCapabilities: List<String> = listOf("plain")
        private set

    private var lastFetchedAt: Long? = null

    override suspend fun refresh(): MessagingLimits {
        val cached = current
        val at = lastFetchedAt
        if (cached != null && at != null && nowMillis() - at < cacheTtlMillis) return cached

        runCatching { api.get<MessagingLimits>(MeinChatEndpoints.LIMITS) }.getOrNull()?.let { current = it }
            ?: run { if (current == null) current = MessagingLimits.DEFAULT }

        runCatching { api.get<MessagingCapabilities>(MeinChatEndpoints.CAPABILITIES) }.getOrNull()?.let { caps ->
            serverCapabilities = caps.server
            myCapabilities = caps.me ?: caps.server
        }

        lastFetchedAt = nowMillis()
        return current ?: MessagingLimits.DEFAULT
    }

    private companion object {
        const val DEFAULT_TTL_MILLIS = 24L * 60 * 60 * 1000
    }
}
