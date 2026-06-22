package com.vbwd.plugin.meinchat.domain

import com.vbwd.core.networking.ApiJson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test

class MeinChatModelsTest {
    private val json = ApiJson.instance

    private fun message(metaJson: String) =
        json.decodeFromString(ChatMessage.serializer(), """{"id":"m1","body":"x","meta":$metaJson}""")

    @Test
    fun `bot_choices meta decodes into typed choices`() {
        val m = message("""{"kind":"bot_choices","choices":[{"label":"Yes","action_data":"y"}]}""")
        val meta = assertInstanceOf(MessageMeta.BotChoices::class.java, m.meta)
        assertEquals("Yes", meta.choices.single().label)
        assertEquals("y", meta.choices.single().actionData)
    }

    @Test
    fun `bot_cart meta decodes items, total and currency`() {
        val m = message(
            """{"kind":"bot_cart","items":[{"name":"Apple","quantity":2.0,"unit_price":1.5,"line_total":3.0}],
               "total":3.0,"currency":"EUR"}""",
        )
        val meta = assertInstanceOf(MessageMeta.Cart::class.java, m.meta)
        assertEquals("Apple", meta.cart.items.single().name)
        assertEquals("EUR", meta.cart.currency)
        assertEquals(3.0, meta.cart.total)
    }

    @Test
    fun `an unknown meta kind degrades to Unknown (plain body fallback)`() {
        val m = message("""{"kind":"bot_future_thing"}""")
        assertEquals(MessageMeta.Unknown, m.meta)
    }
}
