package com.vbwd.plugin.meinchat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.vbwd.plugin.meinchat.domain.MeinChatService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val SAVE_LABEL = "Save"
private const val SAVED_LABEL = "Saved"

/** Set/change the signed-in user's meinchat nickname (over fetch/setNickname). */
class NicknameViewModel(private val service: MeinChatService) {
    data class UiState(
        val current: String? = null,
        val draft: String = "",
        val isSaving: Boolean = false,
        val saved: Boolean = false,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    suspend fun load() {
        val nick = runCatching { service.fetchMyNickname() }.getOrNull()
        _state.value = _state.value.copy(current = nick, draft = _state.value.draft.ifEmpty { nick.orEmpty() })
    }

    fun onDraftChange(value: String) {
        _state.value = _state.value.copy(draft = value, saved = false)
    }

    suspend fun save() {
        val draft = _state.value.draft.trim()
        if (draft.isEmpty() || draft == _state.value.current) return
        _state.value = _state.value.copy(isSaving = true, saved = false)
        val record = runCatching { service.setNickname(draft) }.getOrNull()
        _state.value = _state.value.copy(isSaving = false, current = record?.nickname ?: draft, saved = record != null)
    }
}

/** Profile section to view + change the meinchat nickname. */
@Composable
internal fun NicknameSection(viewModel: NicknameViewModel) {
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { viewModel.load() }

    Column(
        modifier = Modifier.fillMaxWidth().testTag("meinchat_nickname"),
        verticalArrangement = Arrangement.spacedBy(ROW_SPACING),
    ) {
        Text("MeinChat nickname", style = MaterialTheme.typography.titleSmall)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ROW_SPACING),
        ) {
            OutlinedTextField(
                value = state.draft,
                onValueChange = viewModel::onDraftChange,
                modifier = Modifier.weight(FULL_WEIGHT).testTag("meinchat_nickname_input"),
                placeholder = { Text("nickname") },
                singleLine = true,
                shape = RoundedCornerShape(INPUT_CORNER),
            )
            Button(
                onClick = { scope.launch { viewModel.save() } },
                enabled = !state.isSaving && state.draft.isNotBlank() && state.draft.trim() != state.current,
            ) {
                Text(if (state.saved) SAVED_LABEL else SAVE_LABEL)
            }
        }
        state.current?.takeIf { it.isNotEmpty() }?.let {
            Text(
                "Others find you as “$it”",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
