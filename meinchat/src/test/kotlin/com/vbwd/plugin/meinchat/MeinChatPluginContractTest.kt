package com.vbwd.plugin.meinchat

import com.vbwd.core.events.DefaultEventBus
import com.vbwd.core.networking.ApiClient
import com.vbwd.core.networking.ApiClientConfig
import com.vbwd.core.networking.ApiEvent
import com.vbwd.core.networking.EmptyResponse
import com.vbwd.core.networking.HttpMethod
import com.vbwd.core.plugins.DefaultPlatformSdk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.DeserializationStrategy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private class FakeApi : ApiClient {
    @Suppress("UNCHECKED_CAST")
    override suspend fun <T> request(
        method: HttpMethod,
        path: String,
        jsonBody: String?,
        deserializer: DeserializationStrategy<T>,
    ): T = EmptyResponse() as T
    override fun setToken(token: String?) = Unit
    override fun on(event: ApiEvent, handler: () -> Unit) = Unit
}

class MeinChatPluginContractTest {
    private fun sdk() = DefaultPlatformSdk(FakeApi(), ApiClientConfig("http://x"), DefaultEventBus(FakeApi()))

    @Test
    fun `install registers the route, profile section, menu, translations and stores`() = runTest {
        val platform = sdk()
        val plugin = MeinChatPlugin()
        plugin.install(platform)

        assertEquals(listOf("/meinchat"), platform.getRoutes().map { it.path })
        assertTrue(platform.getRoutes().single().matchPrefix)
        assertTrue(platform.getComponents().containsKey("ProfileMeinChatNickname"))
        assertEquals(listOf("meinchat"), platform.getMenuItems().map { it.id })
        assertEquals("MeinChat", platform.getTranslations()["en"]?.get("meinchat.title"))
        assertTrue(platform.getStores().keys.containsAll(setOf("meinchatLimits", "meinchatRetention", "meinchatCache")))

        plugin.uninstall()
    }
}
