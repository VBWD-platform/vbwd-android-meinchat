package com.vbwd.plugin.meinchat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import kotlin.math.abs

// --- spacing / sizing tokens (extracted so detekt's MagicNumber stays happy) ---
private val SCREEN_PADDING = 16.dp
private val ROW_SPACING = 8.dp
private val BUBBLE_SPACING = 4.dp
private val CONTENT_SPACING = 6.dp
private val BUBBLE_MAX_WIDTH = 300.dp
private val BUBBLE_CORNER = 18.dp
private val BUBBLE_TAIL = 4.dp
private val BUBBLE_PADDING_H = 14.dp
private val BUBBLE_PADDING_V = 10.dp
private val CARD_CORNER = 18.dp
private val CARD_PADDING = 12.dp
private val AVATAR_SIZE = 46.dp
private val BADGE_SIZE = 22.dp
private val SEND_SIZE = 46.dp
private val INPUT_CORNER = 24.dp
private val CHIP_CORNER = 8.dp
private val CHIP_PADDING_H = 8.dp
private val CHIP_PADDING_V = 2.dp
private val LIST_ELEVATION = 2.dp
private val BAR_ELEVATION = 3.dp
private const val FULL_WEIGHT = 1f
private const val INITIALS_MAX = 2

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

    /** A message is "mine" (right-aligned) when its sender is me — bot/system stays left. */
    fun isMine(message: ChatMessage): Boolean {
        val me = _myNickname.value
        return !message.isSystemMessage && me != null && message.senderNickname == me
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
) {
    val state by inboxViewModel.uiState.collectAsState()
    var openConversation by remember { mutableStateOf<ConversationViewModel?>(null) }
    LaunchedEffect(Unit) { inboxViewModel.load() }

    val open = openConversation
    if (open != null) {
        ConversationView(open, onBack = { openConversation = null })
        return
    }

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
                    ConversationCard(conversation) { openConversation = conversationFactory(conversation) }
                }
            }
    }
}

@Composable
private fun ConversationCard(
    conversation: Conversation,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(CARD_CORNER),
        tonalElevation = LIST_ELEVATION,
        modifier = Modifier.fillMaxWidth().testTag("meinchat_conversation_${conversation.id}"),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(CARD_PADDING),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CARD_PADDING),
        ) {
            val title = conversation.peerNickname ?: conversation.id
            Avatar(title)
            Column(modifier = Modifier.weight(FULL_WEIGHT)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(CONTENT_SPACING),
                ) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(FULL_WEIGHT, fill = false),
                    )
                    if (conversation.isE2E) E2ELabel()
                }
                Text(
                    conversation.lastMessagePreview ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (conversation.unreadCount > 0) UnreadBadge(conversation.unreadCount)
        }
    }
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
    // Keep the newest message in view as the thread loads / grows.
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Column(modifier = Modifier.fillMaxSize().testTag("meinchat_conversation")) {
        val convo = viewModel.conversation
        ChatTopBar(title = convo.peerNickname ?: convo.id, isE2E = convo.isE2E, onBack = onBack)
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
                if (message.isSystemMessage) {
                    SystemNote(message.body ?: message.systemKind.orEmpty())
                } else {
                    MessageBubble(
                        message = message,
                        isMine = viewModel.isMine(message),
                        onChoiceTap = { choice -> scope.launch { viewModel.tapChoice(choice) } },
                        onCartCheckout = { cart -> viewModel.addCartToCart(cart) },
                    )
                }
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

@Composable
private fun MessageBubble(
    message: ChatMessage,
    isMine: Boolean,
    onChoiceTap: (BotChoice) -> Unit,
    onCartCheckout: (BotCart) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val background = if (isMine) scheme.primary else scheme.surfaceVariant
    val foreground = if (isMine) scheme.onPrimary else scheme.onSurfaceVariant
    val shape =
        RoundedCornerShape(
            topStart = BUBBLE_CORNER,
            topEnd = BUBBLE_CORNER,
            bottomStart = if (isMine) BUBBLE_CORNER else BUBBLE_TAIL,
            bottomEnd = if (isMine) BUBBLE_TAIL else BUBBLE_CORNER,
        )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
    ) {
        Surface(color = background, shape = shape, modifier = Modifier.widthIn(max = BUBBLE_MAX_WIDTH)) {
            Column(
                modifier = Modifier.padding(horizontal = BUBBLE_PADDING_H, vertical = BUBBLE_PADDING_V),
                verticalArrangement = Arrangement.spacedBy(CONTENT_SPACING),
            ) {
                message.body?.takeIf { it.isNotEmpty() }?.let {
                    Text(it, color = foreground, style = MaterialTheme.typography.bodyLarge)
                }
                message.meta?.let { meta ->
                    BotMetaContent(
                        meta = meta,
                        onChoiceTap = onChoiceTap,
                        onCartCheckout = onCartCheckout,
                        contentColor = foreground,
                    )
                }
            }
        }
    }
}

@Composable
private fun SystemNote(text: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        Surface(
            shape = RoundedCornerShape(BUBBLE_CORNER),
            color = MaterialTheme.colorScheme.secondaryContainer,
        ) {
            Text(
                text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(horizontal = BUBBLE_PADDING_H, vertical = CHIP_PADDING_V),
            )
        }
    }
}

@Composable
private fun ChatTopBar(
    title: String,
    isE2E: Boolean,
    onBack: () -> Unit,
) {
    Surface(tonalElevation = BAR_ELEVATION, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = ROW_SPACING, vertical = ROW_SPACING),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ROW_SPACING),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(AVATAR_SIZE)
                        .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Text("←", style = MaterialTheme.typography.headlineSmall)
            }
            Avatar(title)
            Column(modifier = Modifier.weight(FULL_WEIGHT)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (isE2E) "End-to-end encrypted" else "Online",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (isE2E) E2ELabel()
        }
    }
}

@Composable
private fun MessageInput(
    draft: String,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    Surface(tonalElevation = BAR_ELEVATION, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(ROW_SPACING),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ROW_SPACING),
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = onDraftChange,
                modifier = Modifier.weight(FULL_WEIGHT).testTag("meinchat_input"),
                placeholder = { Text("Message") },
                shape = RoundedCornerShape(INPUT_CORNER),
                maxLines = 5,
            )
            SendButton(enabled = draft.isNotBlank(), onClick = onSend)
        }
    }
}

@Composable
private fun SendButton(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val background = if (enabled) scheme.primary else scheme.surfaceVariant
    val foreground = if (enabled) scheme.onPrimary else scheme.onSurfaceVariant
    Box(
        modifier =
            Modifier
                .size(SEND_SIZE)
                .background(background, CircleShape)
                .clickable(enabled = enabled, onClick = onClick)
                .testTag("meinchat_send"),
        contentAlignment = Alignment.Center,
    ) {
        Text("↑", style = MaterialTheme.typography.titleLarge, color = foreground)
    }
}

@Composable
private fun Avatar(name: String) {
    val scheme = MaterialTheme.colorScheme
    val palette =
        listOf(
            scheme.primaryContainer to scheme.onPrimaryContainer,
            scheme.secondaryContainer to scheme.onSecondaryContainer,
            scheme.tertiaryContainer to scheme.onTertiaryContainer,
        )
    val (background, foreground) = palette[abs(name.hashCode()) % palette.size]
    Box(
        modifier = Modifier.size(AVATAR_SIZE).background(background, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(initials(name), style = MaterialTheme.typography.titleMedium, color = foreground)
    }
}

@Composable
private fun UnreadBadge(count: Int) {
    Box(
        modifier = Modifier.size(BADGE_SIZE).background(MaterialTheme.colorScheme.primary, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

@Composable
private fun E2ELabel() {
    Surface(
        shape = RoundedCornerShape(CHIP_CORNER),
        color = MaterialTheme.colorScheme.tertiaryContainer,
    ) {
        Text(
            "E2E",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = CHIP_PADDING_H, vertical = CHIP_PADDING_V),
        )
    }
}

@Composable
private fun CenteredBox(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().padding(SCREEN_PADDING),
        contentAlignment = Alignment.Center,
        content = { content() },
    )
}

private fun initials(name: String): String {
    val parts = name.trim().split(" ").filter { it.isNotEmpty() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].take(INITIALS_MAX).uppercase()
        else -> (parts[0].take(1) + parts[1].take(1)).uppercase()
    }
}
