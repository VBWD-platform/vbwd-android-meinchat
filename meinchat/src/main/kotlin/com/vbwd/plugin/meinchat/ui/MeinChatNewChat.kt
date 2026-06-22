package com.vbwd.plugin.meinchat.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.vbwd.plugin.meinchat.domain.Conversation
import com.vbwd.plugin.meinchat.domain.MeinChatService
import com.vbwd.plugin.meinchat.domain.NicknameSearchHit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val MIN_QUERY = 2

/** Find a user by nickname and open (or create) a conversation with them. */
class NewChatViewModel(private val service: MeinChatService) {
    data class UiState(
        val query: String = "",
        val results: List<NicknameSearchHit> = emptyList(),
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun onQueryChange(value: String) {
        _state.value = _state.value.copy(query = value)
    }

    suspend fun search() {
        val query = _state.value.query.trim()
        if (query.length < MIN_QUERY) {
            _state.value = _state.value.copy(results = emptyList())
            return
        }
        val hits = runCatching { service.searchNickname(query) }.getOrDefault(emptyList())
        _state.value = _state.value.copy(results = hits)
    }

    suspend fun open(nickname: String): Conversation? = runCatching { service.openConversation(nickname) }.getOrNull()
}

@Composable
internal fun NewChatSearch(
    viewModel: NewChatViewModel,
    onBack: () -> Unit,
    onOpen: (Conversation) -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()
    LaunchedEffect(state.query) { viewModel.search() }

    Column(modifier = Modifier.fillMaxSize().testTag("meinchat_new_chat")) {
        Surface(tonalElevation = BAR_ELEVATION, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(ROW_SPACING),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ROW_SPACING),
            ) {
                Box(
                    modifier = Modifier.size(AVATAR_SIZE).clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("←", style = MaterialTheme.typography.headlineSmall)
                }
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::onQueryChange,
                    modifier = Modifier.weight(FULL_WEIGHT).testTag("meinchat_new_chat_input"),
                    placeholder = { Text("Search a nickname") },
                    singleLine = true,
                    shape = RoundedCornerShape(INPUT_CORNER),
                )
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(SCREEN_PADDING),
            verticalArrangement = Arrangement.spacedBy(ROW_SPACING),
        ) {
            items(state.results, key = { it.nickname }) { hit ->
                ChatListCard(
                    title = hit.nickname,
                    subtitle = "Tap to start chatting",
                    unreadCount = 0,
                    onClick = {
                        scope.launch { viewModel.open(hit.nickname)?.let(onOpen) }
                    },
                    testTag = "meinchat_hit_${hit.nickname}",
                )
            }
        }
    }
}
