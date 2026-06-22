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
import com.vbwd.plugin.meinchat.domain.BotMenuCommand
import com.vbwd.plugin.meinchat.domain.MessageMeta

// CART_* tokens are local to the bot cart card; FULL_WEIGHT comes from ChatComponents.
private val SPACING = 8.dp
private val CART_PADDING = 14.dp
private val CART_CORNER = 16.dp
private val CARD_ELEVATION = 3.dp
private val CHIP_SHAPE = RoundedCornerShape(SPACING)
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
        is MessageMeta.BotChoices -> BotChoiceButtons(meta.choices, onChoiceTap)
        is MessageMeta.BotMenu -> BotMenuList(meta.commands, contentColor)
        is MessageMeta.Cart -> BotCartCard(meta.cart, onCartCheckout)
        is MessageMeta.BotAction, MessageMeta.Unknown -> Unit
    }
}

@Composable
private fun BotChoiceButtons(
    choices: List<BotChoice>,
    onChoiceTap: (BotChoice) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().testTag("bot_choices"),
        verticalArrangement = Arrangement.spacedBy(SPACING),
    ) {
        choices.forEach { choice ->
            FilledTonalButton(
                onClick = { onChoiceTap(choice) },
                shape = CHIP_SHAPE,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(choice.label)
            }
        }
    }
}

@Composable
private fun BotMenuList(
    commands: List<BotMenuCommand>,
    contentColor: Color,
) {
    Column(
        modifier = Modifier.testTag("bot_menu"),
        verticalArrangement = Arrangement.spacedBy(SPACING),
    ) {
        commands.forEach { command ->
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
}

@Composable
private fun BotCartCard(
    cart: BotCart,
    onCartCheckout: (BotCart) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(CART_CORNER),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = CARD_ELEVATION,
        modifier = Modifier.fillMaxWidth().testTag("bot_cart"),
    ) {
        Column(
            modifier = Modifier.padding(CART_PADDING),
            verticalArrangement = Arrangement.spacedBy(SPACING),
        ) {
            cart.items.forEach { item ->
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
                    "${cart.currency} ${cart.total}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            Button(
                onClick = { onCartCheckout(cart) },
                shape = CHIP_SHAPE,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Checkout")
            }
        }
    }
}
