package com.vbwd.plugin.meinchat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextAlign
import com.vbwd.core.cart.Cart
import com.vbwd.core.cart.CartItem
import com.vbwd.core.networking.ApiError
import com.vbwd.plugin.meinchat.domain.BotCart
import com.vbwd.plugin.meinchat.domain.BotChoice
import com.vbwd.plugin.meinchat.domain.ChatMessage
import com.vbwd.plugin.meinchat.domain.Conversation
import com.vbwd.plugin.meinchat.domain.MeinChatService
import com.vbwd.plugin.meinchat.domain.MessageMeta
import com.vbwd.plugin.meinchat.domain.Room
import com.vbwd.plugin.meinchat.domain.TokenTransfer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** The two meinchat surfaces — direct conversations and group rooms. */
enum class ChatTab(val label: String) {
    CHATS("Chats"),
    ROOMS("Rooms"),
}

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
    val conversation: Conversation,
) {
    val conversationId: String get() = conversation.id

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _myNickname = MutableStateFlow<String?>(null)
    val myNickname: StateFlow<String?> = _myNickname.asStateFlow()

    suspend fun load() {
        if (_myNickname.value == null) {
            _myNickname.value = runCatching { service.fetchMyNickname() }.getOrNull()
        }
        // The API does not guarantee order (it pages newest-first); sort oldest →
        // newest by sent time so the thread reads top-to-bottom. ISO-8601 strings
        // sort lexicographically in timestamp order; ties fall back to id.
        _messages.value =
            runCatching { service.fetchMessages(conversationId) }
                .getOrDefault(emptyList())
                .sortedWith(compareBy({ it.sentAt ?: "" }, { it.id }))
    }

    /** "Mine" (right-aligned) when I sent it — for transfers, when I am the sender. */
    fun isMine(message: ChatMessage): Boolean {
        val me = _myNickname.value ?: return false
        val transfer = TokenTransfer.parseOrNull(message.body)
        return if (transfer != null) {
            transfer.fromNickname == me
        } else {
            !message.isSystemMessage && message.senderNickname == me
        }
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
    conversationFactory: (Conversation) -> ConversationViewModel,
    roomsViewModel: MeinChatRoomsViewModel,
    roomFactory: (Room) -> RoomViewModel,
) {
    var openConversation by remember { mutableStateOf<ConversationViewModel?>(null) }
    var openRoom by remember { mutableStateOf<RoomViewModel?>(null) }
    var tab by remember { mutableStateOf(ChatTab.CHATS) }

    val convo = openConversation
    val room = openRoom
    when {
        convo != null -> ConversationView(convo, onBack = { openConversation = null })
        room != null -> RoomView(room, onBack = { openRoom = null })
        else ->
            Column(modifier = Modifier.fillMaxSize()) {
                ChatTabs(selected = tab, onSelect = { tab = it })
                when (tab) {
                    ChatTab.CHATS -> DirectInbox(inboxViewModel) { openConversation = conversationFactory(it) }
                    ChatTab.ROOMS -> RoomsList(roomsViewModel) { openRoom = roomFactory(it) }
                }
            }
    }
}

@Composable
private fun ChatTabs(
    selected: ChatTab,
    onSelect: (ChatTab) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = SCREEN_PADDING, vertical = ROW_SPACING),
        horizontalArrangement = Arrangement.spacedBy(ROW_SPACING),
    ) {
        ChatTab.entries.forEach { entry ->
            val active = entry == selected
            Surface(
                onClick = { onSelect(entry) },
                shape = RoundedCornerShape(INPUT_CORNER),
                color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.weight(FULL_WEIGHT).testTag("meinchat_tab_${entry.name.lowercase()}"),
            ) {
                Text(
                    entry.label,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelLarge,
                    color =
                        if (active) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    modifier = Modifier.fillMaxWidth().padding(vertical = ROW_SPACING),
                )
            }
        }
    }
}

@Composable
private fun DirectInbox(
    inboxViewModel: MeinChatInboxViewModel,
    onOpen: (Conversation) -> Unit,
) {
    val state by inboxViewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { inboxViewModel.load() }

    when {
        state.isLoading && state.conversations.isEmpty() -> CenteredBox { CircularProgressIndicator() }
        state.conversations.isEmpty() ->
            CenteredBox {
                Text(
                    state.errorMessage ?: "No conversations yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        else ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(SCREEN_PADDING).testTag("meinchat_inbox"),
                verticalArrangement = Arrangement.spacedBy(ROW_SPACING),
            ) {
                items(state.conversations, key = { it.id }) { conversation ->
                    ConversationCard(conversation) { onOpen(conversation) }
                }
            }
    }
}

@Composable
private fun ConversationCard(
    conversation: Conversation,
    onClick: () -> Unit,
) {
    val title = conversation.peerNickname ?: conversation.id
    ChatListCard(
        title = title,
        subtitle = conversation.lastMessagePreview ?: "",
        unreadCount = conversation.unreadCount,
        showE2EChip = conversation.isE2E,
        testTag = "meinchat_conversation_${conversation.id}",
        onClick = onClick,
    )
}

@Composable
private fun ConversationView(
    viewModel: ConversationViewModel,
    onBack: () -> Unit,
) {
    val messages by viewModel.messages.collectAsState()
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var draft by remember { mutableStateOf("") }
    LaunchedEffect(viewModel.conversationId) { viewModel.load() }
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Column(modifier = Modifier.fillMaxSize().testTag("meinchat_conversation")) {
        val convo = viewModel.conversation
        ChatTopBar(
            title = convo.peerNickname ?: convo.id,
            subtitle = if (convo.isE2E) "End-to-end encrypted" else "Online",
            showE2EChip = convo.isE2E,
            onBack = onBack,
        )
        LazyColumn(
            state = listState,
            modifier =
                Modifier
                    .weight(FULL_WEIGHT)
                    .fillMaxWidth()
                    .padding(horizontal = SCREEN_PADDING, vertical = ROW_SPACING),
            verticalArrangement = Arrangement.spacedBy(BUBBLE_SPACING),
        ) {
            items(messages, key = { it.id }) { message ->
                MessageRow(
                    message = message,
                    isMine = viewModel.isMine(message),
                    onChoiceTap = { choice -> scope.launch { viewModel.tapChoice(choice) } },
                    onCartCheckout = { cart -> viewModel.addCartToCart(cart) },
                )
            }
        }
        MessageInput(
            draft = draft,
            onDraftChange = { draft = it },
            onSend = {
                val text = draft
                draft = ""
                scope.launch { viewModel.send(text) }
            },
        )
    }
}
