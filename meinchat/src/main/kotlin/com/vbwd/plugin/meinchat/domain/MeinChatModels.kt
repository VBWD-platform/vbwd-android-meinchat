package com.vbwd.plugin.meinchat.domain

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

@Serializable
data class Conversation(
    val id: String,
    @SerialName("peer_user_id") val peerUserId: String? = null,
    @SerialName("peer_nickname") val peerNickname: String? = null,
    @SerialName("last_message_at") val lastMessageAt: String? = null,
    @SerialName("last_message_preview") val lastMessagePreview: String? = null,
    @SerialName("unread_count") val unreadCount: Int = 0,
    val protocol: String? = null,
) {
    val isE2E: Boolean get() = protocol == "e2e_v1"
}

@Serializable
data class BotChoice(
    val label: String,
    @SerialName("action_data") val actionData: String,
    val hint: String? = null,
)

@Serializable
data class BotMenuCommand(val command: String, val description: String)

@Serializable
data class BotCartItem(
    val name: String,
    val quantity: Double,
    @SerialName("unit_price") val unitPrice: Double,
    @SerialName("line_total") val lineTotal: Double,
)

@Serializable
data class BotCart(val items: List<BotCartItem>, val total: Double, val currency: String)

/** Typed projection of a message's `meta` JSON (S69/S70). Unknown ⇒ [Unknown] so
 *  the bubble degrades to the plain `body` (Liskov). */
@Serializable(with = MessageMetaSerializer::class)
sealed interface MessageMeta {
    data class BotChoices(val choices: List<BotChoice>) : MessageMeta
    data class BotAction(val actionData: String) : MessageMeta
    data class BotMenu(val commands: List<BotMenuCommand>) : MessageMeta
    data class Cart(val cart: BotCart) : MessageMeta
    data object Unknown : MessageMeta
}

internal object MessageMetaSerializer : KSerializer<MessageMeta> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("MessageMeta")

    override fun deserialize(decoder: Decoder): MessageMeta {
        val input = decoder as? JsonDecoder ?: return MessageMeta.Unknown
        val obj = input.decodeJsonElement().jsonObject
        val json = input.json
        return when (obj["kind"]?.jsonPrimitive?.contentOrNull) {
            "bot_choices" -> MessageMeta.BotChoices(
                obj["choices"]?.let { json.decodeFromJsonElement(ListSerializer(BotChoice.serializer()), it) }
                    ?: emptyList(),
            )
            "bot_action" -> MessageMeta.BotAction(obj["action_data"]?.jsonPrimitive?.contentOrNull ?: "")
            "bot_menu" -> MessageMeta.BotMenu(
                obj["commands"]?.let { json.decodeFromJsonElement(ListSerializer(BotMenuCommand.serializer()), it) }
                    ?: emptyList(),
            )
            "bot_cart" -> MessageMeta.Cart(
                BotCart(
                    items = obj["items"]
                        ?.let { json.decodeFromJsonElement(ListSerializer(BotCartItem.serializer()), it) }
                        ?: emptyList(),
                    total = obj["total"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                    currency = obj["currency"]?.jsonPrimitive?.contentOrNull ?: "",
                ),
            )
            else -> MessageMeta.Unknown
        }
    }

    override fun serialize(encoder: Encoder, value: MessageMeta) {
        val output = encoder as? JsonEncoder ?: return
        val json = output.json
        val element: JsonObject = when (value) {
            is MessageMeta.BotChoices -> buildJsonObject {
                put("kind", "bot_choices")
                put("choices", json.encodeToJsonElement(ListSerializer(BotChoice.serializer()), value.choices))
            }
            is MessageMeta.BotAction -> buildJsonObject {
                put("kind", "bot_action")
                put("action_data", value.actionData)
            }
            is MessageMeta.BotMenu -> buildJsonObject {
                put("kind", "bot_menu")
                put("commands", json.encodeToJsonElement(ListSerializer(BotMenuCommand.serializer()), value.commands))
            }
            is MessageMeta.Cart -> buildJsonObject {
                put("kind", "bot_cart")
                put("items", json.encodeToJsonElement(ListSerializer(BotCartItem.serializer()), value.cart.items))
                put("total", value.cart.total)
                put("currency", value.cart.currency)
            }
            MessageMeta.Unknown -> buildJsonObject { }
        }
        output.encodeJsonElement(element)
    }
}

@Serializable
data class MessageAttachment(
    val id: String,
    val kind: String,
    val mime: String? = null,
    @SerialName("storage_url") val storageUrl: String? = null,
    @SerialName("width_px") val widthPx: Int? = null,
    @SerialName("height_px") val heightPx: Int? = null,
    @SerialName("bytes_count") val bytesCount: Int? = null,
    val protocol: String? = null,
)

@Serializable
data class ChatMessage(
    val id: String,
    @SerialName("conversation_id") val conversationId: String? = null,
    @SerialName("sender_id") val senderId: String? = null,
    @SerialName("sender_nickname") val senderNickname: String? = null,
    val body: String? = null,
    val attachments: List<MessageAttachment>? = null,
    @SerialName("sent_at") val sentAt: String? = null,
    @SerialName("delivered_at") val deliveredAt: String? = null,
    @SerialName("read_at") val readAt: String? = null,
    @SerialName("system_kind") val systemKind: String? = null,
    val protocol: String? = null,
    @SerialName("envelope_b64") val envelopeB64: String? = null,
    @SerialName("sender_device_id") val senderDeviceId: String? = null,
    val meta: MessageMeta? = null,
) {
    val isE2E: Boolean get() = protocol == "e2e_v1"
    val isSystemMessage: Boolean get() = systemKind != null
}

@Serializable
data class Room(
    val id: String,
    val name: String? = null,
    @SerialName("member_count") val memberCount: Int? = null,
    @SerialName("last_message_at") val lastMessageAt: String? = null,
    @SerialName("unread_count") val unreadCount: Int = 0,
)

@Serializable
data class RoomMember(
    @SerialName("user_id") val userId: String,
    val nickname: String? = null,
)

@Serializable
data class NicknameSearchHit(val nickname: String, @SerialName("user_id") val userId: String? = null)

@Serializable
data class NicknameRecord(
    val id: String,
    @SerialName("user_id") val userId: String? = null,
    val nickname: String,
    val banned: Boolean? = null,
)

@Serializable
internal data class MyNicknameResponse(val nickname: String? = null)

@Serializable
data class TokenTransferResult(
    val amount: Int,
    @SerialName("transfer_id") val transferId: String? = null,
    @SerialName("recipient_nickname") val recipientNickname: String? = null,
    @SerialName("new_balance") val newBalance: Int? = null,
)

@Serializable
data class BotConversationStyleDTO(val name: String, val tokens: Map<String, String> = emptyMap())

// --- response wrappers ---

@Serializable
internal data class ConversationsResponse(val items: List<Conversation>? = null)

@Serializable
internal data class MessagesResponse(val items: List<ChatMessage>? = null)

@Serializable
internal data class RoomsResponse(val items: List<Room>? = null)
