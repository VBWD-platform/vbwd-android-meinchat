# MeinChat — UI walkthrough

Screenshots from the running Android app (`vbwd-android-app-example` host, signed
in as a UX-test user against the `vbwd.cc` backend). They document the ported
meinchat feature set and its modern Compose UI.

| # | Screen | What it shows |
|---|--------|---------------|
| 1 | [Inbox + tabs](01-inbox-chats-tabs.png) | Conversation cards (colour-keyed avatars, previews, unread badges), the **Chats/Rooms** segmented toggle, and the **+ New chat** entry. |
| 2 | [Conversation](02-conversation-bubbles.png) | Message bubbles — outgoing (blue, right) vs incoming (grey, left) with sender names and a top bar. |
| 3 | [Token-transfer cards](03-token-transfer-cards.png) | In-chat token transfers rendered as **"Sent N tokens to X"** cards (with note) instead of raw JSON. |
| 4 | [Image attachment](04-image-attachment.png) | A message image attachment (webp) loaded via Coil. |
| 5 | [Rooms list](05-rooms-list.png) | Group rooms with member counts and unread badges. |
| 6 | [Room view](06-room-view.png) | A room thread reusing the bubble UI, with the room name + member count. |
| 7 | [New-chat search](07-new-chat-search.png) | Searching users by nickname to start a conversation. |
| 8 | [Nickname section](08-nickname-section.png) | The profile section to view/change the meinchat nickname. |

Automated coverage of these screens lives in
`meinchat/src/test/.../ui/MeinChatScreenTest.kt` (Robolectric Compose tests).
