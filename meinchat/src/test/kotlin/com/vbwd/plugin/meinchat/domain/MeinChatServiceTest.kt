package com.vbwd.plugin.meinchat.domain

import com.vbwd.core.networking.ApiClient
import com.vbwd.core.networking.HttpMethod
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MeinChatServiceTest {
    private val client = mockk<ApiClient>(relaxed = true)
    private val service = DefaultMeinChatService(client)

    @Test
    fun `fetchConversations unwraps the items`() = runTest {
        coEvery {
            client.request<ConversationsResponse>(HttpMethod.GET, "/messaging/conversations", any(), any())
        } returns ConversationsResponse(items = listOf(Conversation(id = "c1")))
        assertEquals(listOf("c1"), service.fetchConversations().map { it.id })
    }

    @Test
    fun `sendText posts to the messages endpoint`() = runTest {
        coEvery {
            client.request<ChatMessage>(HttpMethod.POST, "/messaging/conversations/c1/messages", any(), any())
        } returns ChatMessage(id = "m1", body = "hi")
        assertEquals("m1", service.sendText("c1", "hi").id)
    }

    @Test
    fun `transferTokens posts and returns the result`() = runTest {
        coEvery { client.request<TokenTransferResult>(HttpMethod.POST, "/token-transfer", any(), any()) } returns
            TokenTransferResult(amount = 10, newBalance = 90)
        assertEquals(90, service.transferTokens("alice", 10).newBalance)
    }
}
