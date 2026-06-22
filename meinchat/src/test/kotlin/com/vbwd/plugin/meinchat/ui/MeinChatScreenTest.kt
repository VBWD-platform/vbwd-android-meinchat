package com.vbwd.plugin.meinchat.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.vbwd.core.cart.Cart
import com.vbwd.plugin.meinchat.domain.Conversation
import com.vbwd.plugin.meinchat.domain.Room
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private const val WAIT_MS = 5_000L

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel5)
class MeinChatScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private fun showScreen(service: FakeMeinChatService) {
        rule.setContent {
            MeinChatScreen(
                inboxViewModel = MeinChatInboxViewModel(service),
                conversationFactory = { ConversationViewModel(service, Cart(), it) },
                roomsViewModel = MeinChatRoomsViewModel(service),
                roomFactory = { RoomViewModel(service, it) },
                newChatViewModel = NewChatViewModel(service),
            )
        }
    }

    @Test
    fun `inbox shows the chats and rooms tabs plus the new-chat entry`() {
        showScreen(FakeMeinChatService(conversations = listOf(Conversation(id = "c1", peerNickname = "alice"))))

        rule.onNodeWithTag("meinchat_tab_chats").assertExists()
        rule.onNodeWithTag("meinchat_tab_rooms").assertExists()
        rule.onNodeWithTag("meinchat_new_chat_button").assertExists()
    }

    @Test
    fun `tapping the rooms tab shows the rooms list`() {
        showScreen(FakeMeinChatService(rooms = listOf(Room(id = "r1", name = "team", memberCount = 3))))

        rule.onNodeWithTag("meinchat_tab_rooms").performClick()
        rule.waitUntil(WAIT_MS) {
            rule.onAllNodesWithTag("meinchat_rooms").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithTag("meinchat_room_r1").assertExists()
    }

    @Test
    fun `the nickname section shows the current nickname`() {
        rule.setContent { NicknameSection(NicknameViewModel(FakeMeinChatService(nickname = "bob"))) }

        rule.onNodeWithText("MeinChat nickname").assertExists()
        rule.waitUntil(WAIT_MS) {
            rule.onAllNodesWithText("Others find you as “bob”").fetchSemanticsNodes().isNotEmpty()
        }
    }
}
