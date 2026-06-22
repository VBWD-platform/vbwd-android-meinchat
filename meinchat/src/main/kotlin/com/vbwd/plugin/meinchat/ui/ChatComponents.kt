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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.vbwd.plugin.meinchat.domain.BotCart
import com.vbwd.plugin.meinchat.domain.BotChoice
import com.vbwd.plugin.meinchat.domain.ChatMessage
import com.vbwd.plugin.meinchat.domain.MessageAttachment
import com.vbwd.plugin.meinchat.domain.TokenTransfer
import kotlin.math.abs

// --- shared spacing / sizing tokens (internal: used by MeinChat + MeinChatRooms;
// extracted so detekt's MagicNumber stays happy) ---
internal val SCREEN_PADDING = 16.dp
internal val ROW_SPACING = 8.dp
internal val BUBBLE_SPACING = 4.dp
internal val CONTENT_SPACING = 6.dp
internal val BUBBLE_MAX_WIDTH = 300.dp
internal val BUBBLE_CORNER = 18.dp
internal val BUBBLE_TAIL = 4.dp
internal val BUBBLE_PADDING_H = 14.dp
internal val BUBBLE_PADDING_V = 10.dp
internal val CARD_CORNER = 18.dp
internal val CARD_PADDING = 12.dp
internal val AVATAR_SIZE = 46.dp
internal val BADGE_SIZE = 22.dp
internal val SEND_SIZE = 46.dp
internal val INPUT_CORNER = 24.dp
internal val CHIP_CORNER = 8.dp
internal val CHIP_PADDING_H = 8.dp
internal val CHIP_PADDING_V = 2.dp
internal val LIST_ELEVATION = 2.dp
internal val BAR_ELEVATION = 3.dp
internal val COIN_SIZE = 32.dp
internal val IMAGE_MAX_WIDTH = 240.dp
internal const val FULL_WEIGHT = 1f
private const val INITIALS_MAX = 2
private const val ICON_BG_ALPHA = 0.15f

/**
 * Routes a message to the right surface: a token-transfer card, a centered
 * system note, or a normal bubble. Shared by the conversation and room views.
 */
@Composable
internal fun MessageRow(
    message: ChatMessage,
    isMine: Boolean,
    onChoiceTap: (BotChoice) -> Unit,
    onCartCheckout: (BotCart) -> Unit,
) {
    val isTransfer = TokenTransfer.parseOrNull(message.body) != null
    if (message.isSystemMessage && !isTransfer) {
        SystemNote(message.body ?: message.systemKind.orEmpty())
    } else {
        MessageBubble(message, isMine, onChoiceTap, onCartCheckout)
    }
}

/** A list row (conversation or room): avatar, title + preview, optional unread badge / E2E chip. */
@Composable
internal fun ChatListCard(
    title: String,
    subtitle: String,
    unreadCount: Int,
    onClick: () -> Unit,
    showE2EChip: Boolean = false,
    testTag: String = "",
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(CARD_CORNER),
        tonalElevation = LIST_ELEVATION,
        modifier = Modifier.fillMaxWidth().testTag(testTag),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(CARD_PADDING),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CARD_PADDING),
        ) {
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
                    if (showE2EChip) E2ELabel()
                }
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (unreadCount > 0) UnreadBadge(unreadCount)
        }
    }
}

/** A chat message bubble: outgoing (right, primary) vs incoming (left, surfaceVariant). */
@Composable
internal fun MessageBubble(
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
                val transfer = TokenTransfer.parseOrNull(message.body)
                if (transfer == null && message.senderNickname != null && !isMine) {
                    Text(
                        message.senderNickname,
                        style = MaterialTheme.typography.labelMedium,
                        color = foreground.copy(alpha = SENDER_ALPHA),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                if (transfer != null) {
                    TokenTransferContent(transfer = transfer, isMine = isMine, contentColor = foreground)
                } else {
                    message.body?.takeIf { it.isNotEmpty() }?.let {
                        Text(it, color = foreground, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                AttachmentImages(message.attachments.orEmpty())
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

/** Origin (scheme+host) used to absolutise relative `/uploads/...` attachment urls. */
internal val LocalMediaOrigin = staticCompositionLocalOf { "" }

/** Renders image attachments (one per logical image — skips the duplicate thumbnail). */
@Composable
private fun AttachmentImages(attachments: List<MessageAttachment>) {
    val origin = LocalMediaOrigin.current
    attachments
        .filter { it.kind != "thumb" && it.mime?.startsWith("image/") == true && !it.storageUrl.isNullOrEmpty() }
        .forEach { attachment ->
            AsyncImage(
                model = origin + attachment.storageUrl,
                contentDescription = "image attachment",
                contentScale = ContentScale.FillWidth,
                modifier =
                    Modifier
                        .widthIn(max = IMAGE_MAX_WIDTH)
                        .clip(RoundedCornerShape(BUBBLE_TAIL)),
            )
        }
}

/** A readable token-transfer card (replaces the raw `{"amount":…}` body). */
@Composable
private fun TokenTransferContent(
    transfer: TokenTransfer,
    isMine: Boolean,
    contentColor: Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ROW_SPACING),
    ) {
        Box(
            modifier = Modifier.size(COIN_SIZE).background(contentColor.copy(alpha = ICON_BG_ALPHA), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(if (isMine) "↑" else "↓", color = contentColor, style = MaterialTheme.typography.titleMedium)
        }
        Column {
            Text(
                "${if (isMine) "Sent" else "Received"} ${transfer.amount} tokens",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
            )
            Text(
                if (isMine) "to ${transfer.toNickname}" else "from ${transfer.fromNickname}",
                style = MaterialTheme.typography.labelMedium,
                color = contentColor.copy(alpha = SENDER_ALPHA),
            )
            transfer.note?.takeIf { it.isNotEmpty() }?.let {
                Text(
                    "“$it”",
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = SENDER_ALPHA),
                )
            }
        }
    }
}

/** A centered "system" note pill (joins, reads, etc.). */
@Composable
internal fun SystemNote(text: String) {
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

/** Top bar for a conversation/room: back, avatar, title + [subtitle], optional E2E chip. */
@Composable
internal fun ChatTopBar(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    showE2EChip: Boolean = false,
) {
    Surface(tonalElevation = BAR_ELEVATION, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = ROW_SPACING, vertical = ROW_SPACING),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ROW_SPACING),
        ) {
            Box(
                modifier = Modifier.size(AVATAR_SIZE).clickable(onClick = onBack).testTag("meinchat_back"),
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
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (showE2EChip) E2ELabel()
        }
    }
}

/** Rounded message input + circular send button. */
@Composable
internal fun MessageInput(
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

/** Circular initials avatar, colour-keyed from [name]. */
@Composable
internal fun Avatar(name: String) {
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
internal fun UnreadBadge(count: Int) {
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
internal fun E2ELabel() {
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
internal fun CenteredBox(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().padding(SCREEN_PADDING),
        contentAlignment = Alignment.Center,
        content = { content() },
    )
}

private const val SENDER_ALPHA = 0.8f

private fun initials(name: String): String {
    val parts = name.trim().split(" ").filter { it.isNotEmpty() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].take(INITIALS_MAX).uppercase()
        else -> (parts[0].take(1) + parts[1].take(1)).uppercase()
    }
}
