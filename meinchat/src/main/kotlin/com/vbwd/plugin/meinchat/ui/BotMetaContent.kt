package com.vbwd.plugin.meinchat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.vbwd.plugin.meinchat.domain.BotCart
import com.vbwd.plugin.meinchat.domain.BotChoice
import com.vbwd.plugin.meinchat.domain.MessageMeta

private val SPACING = 8.dp
private const val FULL_WEIGHT = 1f

/**
 * Native rendering of the bot `meta` vocab (S69/S70): choices / menu / cart.
 * Unknown/Unknown-kind degrades to nothing here — the caller renders the plain
 * `body` (Liskov). Port of the iOS `BotChoiceCards`/`BotMenuList`/`BotCartCard`.
 */
@Composable
fun BotMetaContent(
    meta: MessageMeta,
    onChoiceTap: (BotChoice) -> Unit,
    onCartCheckout: (BotCart) -> Unit,
) {
    when (meta) {
        is MessageMeta.BotChoices -> Column(
            modifier = Modifier.testTag("bot_choices"),
            verticalArrangement = Arrangement.spacedBy(SPACING),
        ) {
            meta.choices.forEach { choice ->
                OutlinedButton(onClick = { onChoiceTap(choice) }, modifier = Modifier.fillMaxWidth()) {
                    Text(choice.label)
                }
            }
        }
        is MessageMeta.BotMenu -> Column(modifier = Modifier.testTag("bot_menu")) {
            meta.commands.forEach { command ->
                Text("${command.command} — ${command.description}", style = MaterialTheme.typography.bodySmall)
            }
        }
        is MessageMeta.Cart -> Card(modifier = Modifier.fillMaxWidth().testTag("bot_cart")) {
            Column(modifier = Modifier.padding(SPACING), verticalArrangement = Arrangement.spacedBy(SPACING)) {
                meta.cart.items.forEach { item ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(item.name, modifier = Modifier.weight(FULL_WEIGHT))
                        Text("${item.quantity} × ${item.unitPrice}")
                    }
                }
                Text("Total: ${meta.cart.currency} ${meta.cart.total}")
                Button(onClick = { onCartCheckout(meta.cart) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Checkout")
                }
            }
        }
        is MessageMeta.BotAction, MessageMeta.Unknown -> Unit
    }
}
