package com.vbwd.plugin.meinchat.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.vbwd.core.cart.Cart
import com.vbwd.core.cart.CartItem
import com.vbwd.core.networking.ApiError
import com.vbwd.plugin.meinchat.domain.BotCart
import com.vbwd.plugin.meinchat.domain.BotChoice
import com.vbwd.plugin.meinchat.domain.ChatMessage
import com.vbwd.plugin.meinchat.domain.Conversation
import com.vbwd.plugin.meinchat.domain.MeinChatService
import com.vbwd.plugin.meinchat.domain.MessageMeta
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private val PADDING = 12.dp
private const val FULL_WEIGHT = 1f

/** Inbox (conversation list). Port of the iOS `InboxViewModel`. */
class MeinChatInboxViewModel(private val service: MeinChatService) {
    data class UiState(
        val isLoading: Boolean = false,
        val conversations: List<Conversation> = emptyList(),
        val errorMessage: String? = null,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    suspend fun load() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        try {
            _uiState.value = UiState(isLoading = false, conversations = service.fetchConversations())
        } catch (error: ApiError) {
            _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = error.message)
        }
    }
}

/** A single conversation. Port of the iOS `ConversationViewModel`. */
class ConversationViewModel(
    private val service: MeinChatService,
    private val cart: Cart,
    val conversationId: String,
) {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    suspend fun load() {
        _messages.value = runCatching { service.fetchMessages(conversationId) }.getOrDefault(emptyList())
    }

    suspend fun send(text: String) {
        if (text.isBlank()) return
        runCatching { service.sendText(conversationId, text) }
        load()
    }

    suspend fun tapChoice(choice: BotChoice) {
        runCatching { service.sendText(conversationId, choice.label, MessageMeta.BotAction(choice.actionData)) }
        load()
    }

    fun addCartToCart(botCart: BotCart) {
        botCart.items.forEach { item ->
            cart.add(
                CartItem(
                    type = "shop_product",
                    id = item.name,
                    name = item.name,
                    price = item.unitPrice,
                    quantity = item.quantity.toInt().coerceAtLeast(1),
                    currency = botCart.currency,
                ),
            )
        }
    }
}

@Composable
fun MeinChatScreen(
    inboxViewModel: MeinChatInboxViewModel,
    conversationFactory: (String) -> ConversationViewModel,
) {
    val state by inboxViewModel.uiState.collectAsState()
    var openConversation by remember { mutableStateOf<ConversationViewModel?>(null) }
    LaunchedEffect(Unit) { inboxViewModel.load() }

    val open = openConversation
    if (open != null) {
        ConversationView(open, onBack = { openConversation = null })
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(PADDING).testTag("meinchat_inbox"),
        verticalArrangement = Arrangement.spacedBy(PADDING),
    ) {
        items(state.conversations, key = { it.id }) { conversation ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { openConversation = conversationFactory(conversation.id) },
            ) {
                Text(conversation.peerNickname ?: conversation.id, style = MaterialTheme.typography.titleSmall)
                Text(conversation.lastMessagePreview ?: "", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ConversationView(viewModel: ConversationViewModel, onBack: () -> Unit) {
    val messages by viewModel.messages.collectAsState()
    val scope = rememberCoroutineScope()
    var draft by remember { mutableStateOf("") }
    LaunchedEffect(viewModel.conversationId) { viewModel.load() }

    Column(modifier = Modifier.fillMaxSize().padding(PADDING).testTag("meinchat_conversation")) {
        Text("Back", modifier = Modifier.clickable { onBack() }.padding(bottom = PADDING))
        LazyColumn(modifier = Modifier.weight(FULL_WEIGHT), verticalArrangement = Arrangement.spacedBy(PADDING)) {
            items(messages, key = { it.id }) { message ->
                message.body?.takeIf { it.isNotEmpty() }?.let { Text(it) }
                message.meta?.let { meta ->
                    BotMetaContent(
                        meta = meta,
                        onChoiceTap = { choice -> scope.launch { viewModel.tapChoice(choice) } },
                        onCartCheckout = { cart -> viewModel.addCartToCart(cart) },
                    )
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(value = draft, onValueChange = { draft = it }, modifier = Modifier.weight(FULL_WEIGHT))
            Button(onClick = {
                val text = draft
                draft = ""
                scope.launch { viewModel.send(text) }
            }) {
                Text("Send")
            }
        }
    }
}
