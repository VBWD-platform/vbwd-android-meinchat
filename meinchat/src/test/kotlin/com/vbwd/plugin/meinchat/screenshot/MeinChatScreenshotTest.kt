package com.vbwd.plugin.meinchat.screenshot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.vbwd.core.theme.InMemoryThemeStore
import com.vbwd.core.theme.ThemeManager
import com.vbwd.core.theme.ThemeRegistry
import com.vbwd.core.theme.VbwdTheme
import com.vbwd.plugin.meinchat.domain.BotCart
import com.vbwd.plugin.meinchat.domain.BotCartItem
import com.vbwd.plugin.meinchat.domain.BotChoice
import com.vbwd.plugin.meinchat.domain.MessageMeta
import com.vbwd.plugin.meinchat.ui.BotMetaContent
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel5)
class MeinChatScreenshotTest {
    private val theme = ThemeManager(ThemeRegistry(), InMemoryThemeStore())

    @Test
    fun botCards() {
        val choices = MessageMeta.BotChoices(
            listOf(BotChoice("Track my order", "track"), BotChoice("Talk to support", "support")),
        )
        val cart = MessageMeta.Cart(
            BotCart(
                items = listOf(
                    BotCartItem("Apples", quantity = 2.0, unitPrice = 1.5, lineTotal = 3.0),
                    BotCartItem("Sourdough bread", quantity = 1.0, unitPrice = 2.0, lineTotal = 2.0),
                ),
                total = 5.0,
                currency = "EUR",
            ),
        )
        captureRoboImage("screenshots/bot_cards.png") {
            VbwdTheme(theme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text("How can I help?")
                        BotMetaContent(choices, onChoiceTap = {}, onCartCheckout = {})
                        BotMetaContent(cart, onChoiceTap = {}, onCartCheckout = {})
                    }
                }
            }
        }
    }
}
