package com.vbwd.plugin.meinchat.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

private class FakeLimits(suggested: Int) : MeinChatLimitsService {
    override val current = MessagingLimits.DEFAULT.copy(messagesRetentionDaysClientSuggested = suggested)
    override val serverCapabilities = listOf("plain")
    override val myCapabilities = listOf("plain")
    override suspend fun refresh() = current
}

class ClientRetentionResolverTest {
    @Test
    fun `effective days defaults to the server-suggested value`() {
        val resolver = ClientRetentionResolver(FakeLimits(suggested = 7), InMemoryRetentionStore())
        assertEquals(7, resolver.effectiveDays())
    }

    @Test
    fun `the user can shorten but never extend past the server suggestion`() {
        val resolver = ClientRetentionResolver(FakeLimits(suggested = 7), InMemoryRetentionStore())
        resolver.setUserSetting(3)
        assertEquals(3, resolver.effectiveDays())
        resolver.setUserSetting(100)
        assertEquals(7, resolver.effectiveDays())
        resolver.setUserSetting(-5)
        assertEquals(0, resolver.effectiveDays())
    }
}
