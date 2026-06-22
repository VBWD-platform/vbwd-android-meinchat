package com.vbwd.plugin.meinchat.notifications

import com.vbwd.core.notifications.DeviceTokenSink
import com.vbwd.plugin.meinchat.domain.MeinChatService
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Ships the FCM device token to the meinchat backend (S67.2). Auth-aware: buffers
 * the token until login (the register POST needs a JWT) and re-posts on each
 * later login; logout best-effort unregisters. Port of the iOS `MeinChatTokenSink`.
 */
class MeinChatTokenSink(
    private val service: MeinChatService,
    private val app: String = "meinchat",
) : DeviceTokenSink {
    private val mutex = Mutex()
    private var lastTokenHex: String? = null
    private var isAuthenticated = false

    override suspend fun handleDeviceToken(tokenHex: String) {
        val send = mutex.withLock {
            lastTokenHex = tokenHex
            isAuthenticated
        }
        if (send) runCatching { service.registerDeviceToken(tokenHex, app) }
    }

    suspend fun handleLogin() {
        val token = mutex.withLock {
            isAuthenticated = true
            lastTokenHex
        }
        token?.let { runCatching { service.registerDeviceToken(it, app) } }
    }

    suspend fun handleLogout() {
        val token = mutex.withLock {
            isAuthenticated = false
            lastTokenHex
        }
        token?.let { runCatching { service.unregisterDeviceToken(it) } }
    }
}
