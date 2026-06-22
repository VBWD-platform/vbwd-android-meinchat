package com.vbwd.plugin.meinchat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vbwd.plugin.meinchat.domain.BotCart
import com.vbwd.plugin.meinchat.domain.BotChoice
import com.vbwd.plugin.meinchat.domain.MessageMeta

private val SPACING = 8.dp
private val CARD_PADDING = 14.dp
private val CARD_CORNER = 16.dp
private val CARD_ELEVATION = 3.dp
private val CHIP_SHAPE = RoundedCornerShape(SPACING)
private const val FULL_WEIGHT = 1f
private const val MUTED_ALPHA = 0.7f

/**
 * Native rendering of the bot `meta` vocab (S69/S70): choices / menu / cart.
 * Unknown/Unknown-kind degrades to nothing here — the caller renders the plain
 * `body` (Liskov). Port of the iOS `BotChoiceCards`/`BotMenuList`/`BotCartCard`.
 *
 * [contentColor] tints the plain-text parts so the meta reads correctly on top
 * of whichever bubble colour hosts it (incoming vs outgoing).
 */
@Composable
fun BotMetaContent(
    meta: MessageMeta,
    onChoiceTap: (BotChoice) -> Unit,
    onCartCheckout: (BotCart) -> Unit,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    when (meta) {
        is MessageMeta.BotChoices -> Column(
            modifier = Modifier.fillMaxWidth().testTag("bot_choices"),
            verticalArrangement = Arrangement.spacedBy(SPACING),
        ) {
            meta.choices.forEach { choice ->
                FilledTonalButton(
                    onClick = { onChoiceTap(choice) },
                    shape = CHIP_SHAPE,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(choice.label)
                }
            }
        }

        is MessageMeta.BotMenu -> Column(
            modifier = Modifier.testTag("bot_menu"),
            verticalArrangement = Arrangement.spacedBy(SPACING),
        ) {
            meta.commands.forEach { command ->
                Column {
                    Text(
                        command.command,
                        style = MaterialTheme.typography.labelLarge,
                        color = contentColor,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        command.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor.copy(alpha = MUTED_ALPHA),
                    )
                }
            }
        }

        is MessageMeta.Cart -> Surface(
            shape = RoundedCornerShape(CARD_CORNER),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = CARD_ELEVATION,
            modifier = Modifier.fillMaxWidth().testTag("bot_cart"),
        ) {
            Column(
                modifier = Modifier.padding(CARD_PADDING),
                verticalArrangement = Arrangement.spacedBy(SPACING),
            ) {
                meta.cart.items.forEach { item ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            item.name,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(FULL_WEIGHT),
                        )
                        Text(
                            "${item.quantity.toInt()} × ${item.unitPrice}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                HorizontalDivider()
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Total",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(FULL_WEIGHT),
                    )
                    Text(
                        "${meta.cart.currency} ${meta.cart.total}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Button(
                    onClick = { onCartCheckout(meta.cart) },
                    shape = CHIP_SHAPE,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Checkout")
                }
            }
        }

        is MessageMeta.BotAction, MessageMeta.Unknown -> Unit
    }
}
