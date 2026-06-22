package com.vbwd.plugin.meinchat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import com.vbwd.core.networking.ApiError
import com.vbwd.plugin.meinchat.domain.ChatMessage
import com.vbwd.plugin.meinchat.domain.MeinChatService
import com.vbwd.plugin.meinchat.domain.Room
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Rooms list. Port of the iOS rooms inbox over the existing `fetchRooms` seam. */
class MeinChatRoomsViewModel(private val service: MeinChatService) {
    data class UiState(
        val isLoading: Boolean = false,
        val rooms: List<Room> = emptyList(),
        val errorMessage: String? = null,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    suspend fun load() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        try {
            _uiState.value = UiState(isLoading = false, rooms = service.fetchRooms())
        } catch (error: ApiError) {
            _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = error.message)
        }
    }
}

/** A single room thread. Mirrors [ConversationViewModel] over the room seams. */
class RoomViewModel(
    private val service: MeinChatService,
    val room: Room,
) {
    val roomId: String get() = room.id

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private var myNickname: String? = null

    suspend fun load() {
        if (myNickname == null) {
            myNickname = runCatching { service.fetchMyNickname() }.getOrNull()
        }
        _messages.value =
            runCatching { service.fetchRoomMessages(roomId) }
                .getOrDefault(emptyList())
                .sortedWith(compareBy({ it.sentAt ?: "" }, { it.id }))
    }

    fun isMine(message: ChatMessage): Boolean {
        val me = myNickname
        return !message.isSystemMessage && me != null && message.senderNickname == me
    }

    suspend fun send(text: String) {
        if (text.isBlank()) return
        runCatching { service.sendRoomText(roomId, text) }
        load()
    }
}

@Composable
internal fun RoomsList(
    viewModel: MeinChatRoomsViewModel,
    onOpen: (Room) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }

    when {
        state.isLoading && state.rooms.isEmpty() -> CenteredBox { CircularProgressIndicator() }
        state.rooms.isEmpty() ->
            CenteredBox {
                Text(
                    state.errorMessage ?: "No rooms yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        else ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(SCREEN_PADDING).testTag("meinchat_rooms"),
                verticalArrangement = Arrangement.spacedBy(ROW_SPACING),
            ) {
                items(state.rooms, key = { it.id }) { room ->
                    ChatListCard(
                        title = room.name ?: room.id,
                        subtitle = memberLabel(room.memberCount),
                        unreadCount = room.unreadCount,
                        onClick = { onOpen(room) },
                        testTag = "meinchat_room_${room.id}",
                    )
                }
            }
    }
}

@Composable
internal fun RoomView(
    viewModel: RoomViewModel,
    onBack: () -> Unit,
) {
    val messages by viewModel.messages.collectAsState()
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var draft by remember { mutableStateOf("") }
    LaunchedEffect(viewModel.roomId) { viewModel.load() }
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Column(modifier = Modifier.fillMaxSize().testTag("meinchat_room")) {
        val room = viewModel.room
        ChatTopBar(
            title = room.name ?: room.id,
            subtitle = memberLabel(room.memberCount),
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
                if (message.isSystemMessage) {
                    SystemNote(message.body ?: message.systemKind.orEmpty())
                } else {
                    MessageBubble(
                        message = message,
                        isMine = viewModel.isMine(message),
                        onChoiceTap = {},
                        onCartCheckout = {},
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

private fun memberLabel(count: Int?): String = "${count ?: 0} members"
