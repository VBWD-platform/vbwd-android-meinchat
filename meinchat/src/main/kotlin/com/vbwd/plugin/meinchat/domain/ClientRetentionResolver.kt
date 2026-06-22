package com.vbwd.plugin.meinchat.domain

/** Persistence seam for the user's chosen retention (DIP — testable). */
interface RetentionStore {
    var userRetentionDays: Int?
}

class InMemoryRetentionStore : RetentionStore {
    override var userRetentionDays: Int? = null
}

/**
 * Single home for "what TTL applies?" (DRY). Port of the iOS
 * `ClientRetentionResolver`: effective days = min(user-chosen, server-suggested);
 * the user can shorten retention but never extend past the server's guarantee.
 */
class ClientRetentionResolver(
    private val limits: MeinChatLimitsService,
    private val store: RetentionStore = InMemoryRetentionStore(),
) {
    fun serverSuggested(): Int =
        limits.current?.messagesRetentionDaysClientSuggested ?: DEFAULT_DAYS

    fun effectiveDays(): Int {
        val suggested = serverSuggested()
        val user = store.userRetentionDays ?: suggested
        return minOf(user, suggested)
    }

    fun ttlSeconds(): Long = effectiveDays().toLong() * SECONDS_PER_DAY

    /** Writes the user's chosen retention, clamped to `[0, serverSuggested]`. */
    fun setUserSetting(days: Int) {
        store.userRetentionDays = days.coerceIn(0, serverSuggested())
    }

    private companion object {
        const val DEFAULT_DAYS = 10
        const val SECONDS_PER_DAY = 86_400L
    }
}
