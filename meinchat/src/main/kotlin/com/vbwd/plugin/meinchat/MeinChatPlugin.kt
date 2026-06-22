package com.vbwd.plugin.meinchat

import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import com.vbwd.core.events.AppEvents
import com.vbwd.core.events.Unsubscribe
import com.vbwd.core.plugins.PlatformSdk
import com.vbwd.core.plugins.Plugin
import com.vbwd.core.plugins.PluginMetadata
import com.vbwd.core.plugins.PluginRoute
import com.vbwd.core.plugins.SemanticVersion
import com.vbwd.core.plugins.registries.MenuItem
import com.vbwd.plugin.meinchat.domain.ClientRetentionResolver
import com.vbwd.plugin.meinchat.domain.DefaultMeinChatLimitsService
import com.vbwd.plugin.meinchat.domain.DefaultMeinChatService
import com.vbwd.plugin.meinchat.domain.InMemoryMessageCache
import com.vbwd.plugin.meinchat.notifications.MeinChatTokenSink
import com.vbwd.plugin.meinchat.ui.ConversationViewModel
import com.vbwd.plugin.meinchat.ui.MeinChatInboxViewModel
import com.vbwd.plugin.meinchat.ui.MeinChatScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Peer-to-peer messaging with bot rich-choice rendering. Port of the iOS
 * `MeinChatPlugin` (backbone). Registers `/meinchat` (prefix-matched), the
 * profile nickname section, menu item, translations, the limits/retention/cache
 * stores, and the auth-aware FCM token sink. The E2E crypto seam is provided by
 * `meinchat-plus` (A10) under `MeinChatSecureMessagingStoreId`.
 */
class MeinChatPlugin : Plugin {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val subscriptions = mutableListOf<Unsubscribe>()

    override val metadata = PluginMetadata(
        name = "meinchat",
        version = SemanticVersion(1, 1, 0),
        description = "Peer-to-peer messaging with image attachments and token transfers.",
        author = "VBWD",
        keywords = listOf("chat", "messaging", "meinchat"),
        translations = mapOf("en" to TRANSLATIONS),
    )

    @Suppress("LongMethod")
    override suspend fun install(sdk: PlatformSdk) {
        val service = DefaultMeinChatService(sdk.api)
        val limits = DefaultMeinChatLimitsService(sdk.api)
        val retention = ClientRetentionResolver(limits)
        val cache = InMemoryMessageCache()

        sdk.createStore("meinchatLimits", limits)
        sdk.createStore("meinchatRetention", retention)
        sdk.createStore("meinchatCache", cache)

        sdk.addRoute(
            PluginRoute(path = "/meinchat", name = "meinchat", requiresAuth = true, matchPrefix = true) {
                val inbox = remember { MeinChatInboxViewModel(service) }
                MeinChatScreen(
                    inboxViewModel = inbox,
                    conversationFactory = { id -> ConversationViewModel(service, sdk.cart, id) },
                )
            },
        )

        sdk.addComponent("ProfileMeinChatNickname") { Text("MeinChat nickname") }

        sdk.addMenuItem(
            MenuItem(
                id = "meinchat",
                icon = "chat",
                title = "MeinChat",
                routePath = "/meinchat",
                order = MENU_ORDER,
                section = "top",
            ),
        )
        sdk.addTranslations("en", TRANSLATIONS)

        val tokenSink = MeinChatTokenSink(service)
        sdk.notifications.registerSink(tokenSink)
        subscriptions += sdk.events.on(AppEvents.AUTH_LOGIN) {
            scope.launch {
                limits.refresh()
                tokenSink.handleLogin()
            }
        }
        subscriptions += sdk.events.on(AppEvents.AUTH_LOGOUT) {
            scope.launch { tokenSink.handleLogout() }
        }
    }

    override suspend fun uninstall() {
        subscriptions.forEach { it() }
        subscriptions.clear()
        scope.cancel()
    }

    private companion object {
        const val MENU_ORDER = 40

        val TRANSLATIONS = mapOf(
            "nav.meinchat" to "MeinChat",
            "meinchat.title" to "MeinChat",
            "meinchat.inbox" to "Inbox",
            "meinchat.send" to "Send",
        )
    }
}
