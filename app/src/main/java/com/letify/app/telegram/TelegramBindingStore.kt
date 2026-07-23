package com.letify.app.telegram

import android.content.Context

/**
 * Lightweight SharedPreferences-backed persistence for the Telegram binding.
 *
 * We store only the few primitive fields surfaced in the bound-state UI so
 * a) restoring on next launch is instant and b) there is no risk of leaking
 * a more sensitive snapshot of the user's Telegram profile.
 */
class TelegramBindingStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("letify.tg", Context.MODE_PRIVATE)

    fun load(): TelegramAuth.TelegramUser? {
        if (!prefs.getBoolean(KEY_BOUND, false)) return null
        val id = prefs.getLong(KEY_ID, 0L)
        if (id == 0L) return null
        return TelegramAuth.TelegramUser(
            id = id,
            firstName = prefs.getString(KEY_FIRST, "").orEmpty(),
            lastName = prefs.getString(KEY_LAST, null),
            username = prefs.getString(KEY_USERNAME, null),
            photoUrl = prefs.getString(KEY_PHOTO_URL, null),
        )
    }

    fun save(user: TelegramAuth.TelegramUser) {
        prefs.edit()
            .putBoolean(KEY_BOUND, true)
            .putLong(KEY_ID, user.id)
            .putString(KEY_FIRST, user.firstName)
            .putString(KEY_LAST, user.lastName)
            .putString(KEY_USERNAME, user.username)
            .putString(KEY_PHOTO_URL, user.photoUrl)
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val KEY_BOUND = "bound"
        const val KEY_ID = "id"
        const val KEY_FIRST = "first_name"
        const val KEY_LAST = "last_name"
        const val KEY_USERNAME = "username"
        const val KEY_PHOTO_URL = "photo_url"
    }
}
