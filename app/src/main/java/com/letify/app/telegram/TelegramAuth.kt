package com.letify.app.telegram

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.random.Random

/**
 * Lightweight Telegram bot login flow that does not require a backend.
 *
 * Flow:
 *  1. The bindings screen calls [newSessionToken] to mint a short, random
 *     8-character session key.
 *  2. [buildBotDeepLink] turns that key into a `https://t.me/<bot>?start=<key>`
 *     URL — Telegram intercepts the link, opens the bot's chat with a single
 *     "Start" button that, when tapped, sends `/start <key>` back to the bot.
 *  3. [pollOnce] long-polls `getUpdates` against the Bot API directly from the
 *     device and returns the [TelegramUser] whose `/start <key>` payload
 *     matched our session. The bindings screen calls it in a loop while the
 *     user is in the "waiting" state.
 *
 * Notes on security: embedding a bot token in a client app is not best
 * practice — anyone decompiling the APK can read it. The user explicitly
 * asked for the auth to happen "from the app, no backend", so we accept this
 * tradeoff. A production version should put the polling on a backend the bot
 * webhooks at, and have the device authenticate against that backend.
 */
object TelegramAuth {

    /** Bot token issued by @BotFather for @letifybot — pinned by the user. */
    const val BOT_TOKEN = "8987686050:AAGdzAkysPvmM5EXuvHVEwsCDOo20yhO2ao"

    /** Public username of the bot, used to build the deep link. */
    const val BOT_USERNAME = "letifybot"

    private const val API_BASE = "https://api.telegram.org/bot$BOT_TOKEN"
    private const val TAG = "TelegramAuth"

    /**
     * A real Telegram user. [photoUrl] is filled in via a follow-up call to
     * [fetchProfilePhotoUrl] once we know the user id — Telegram does not
     * include profile photos on the `message.from` object, so we need a
     * separate round trip. Null means the user has no profile photo (or
     * the fetch failed, which we treat the same way).
     */
    data class TelegramUser(
        val id: Long,
        val firstName: String,
        val lastName: String?,
        val username: String?,
        val photoUrl: String? = null,
    ) {
        val displayName: String
            get() = listOfNotNull(firstName.takeIf { it.isNotEmpty() }, lastName)
                .joinToString(" ")
                .ifEmpty { username?.let { "@$it" } ?: "Telegram пользователь" }
    }

    /**
     * Mint a fresh session token. Short alphanumeric so it embeds cleanly into
     * the `start=` query param without URL-encoding noise. 8 characters from a
     * 62-char alphabet ≈ 2.18e14 combinations — way more than enough entropy
     * for the few-minutes window in which a single binding attempt is alive.
     */
    fun newSessionToken(): String {
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return buildString(8) {
            repeat(8) { append(alphabet[Random.nextInt(alphabet.length)]) }
        }
    }

    /** Public deep link the user is sent to to authorise the binding. */
    fun buildBotDeepLink(sessionToken: String): String =
        "https://t.me/$BOT_USERNAME?start=$sessionToken"

    /**
     * Launch Telegram (or fall back to the web client) on the bot's chat with
     * the start payload prefilled. Returns `true` if an activity was actually
     * resolved and started. The screen treats `false` as "device has no
     * Telegram or browser" and falls back to a copyable link.
     */
    fun openBot(context: Context, sessionToken: String): Boolean {
        val link = buildBotDeepLink(sessionToken)
        // Try the tg:// scheme first — that's the explicit handoff into the
        // Telegram app without going through the browser's intent picker. If
        // Telegram isn't installed, the https URL will resolve through the
        // user's browser instead, which still works (web Telegram).
        val tgIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("tg://resolve?domain=$BOT_USERNAME&start=$sessionToken"),
        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        if (tgIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(tgIntent)
            return true
        }
        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(link)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (webIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(webIntent)
            return true
        }
        return false
    }

    /**
     * Result of a single `getUpdates` poll cycle. `Found` carries both the
     * authenticated user and the next offset to use for subsequent polls so
     * we never re-scan the same update twice.
     */
    sealed interface PollResult {
        data class Found(val user: TelegramUser, val nextOffset: Long) : PollResult
        data class NotFound(val nextOffset: Long) : PollResult
        data class Error(val message: String) : PollResult
    }

    /**
     * Single long-poll pass against `getUpdates`. Returns when either:
     *  - a matching `/start <sessionToken>` message arrives,
     *  - the long-poll times out without a match (HTTP returns 200 with an
     *    empty `result` array), or
     *  - the request itself errors.
     *
     * `offset` should be 0 on the first call, then the `nextOffset` from the
     * previous result. The Bot API uses `offset` to confirm-and-drop updates
     * up to that id, so passing the running max+1 keeps the queue clean.
     *
     * The `timeoutSeconds` parameter maps to Telegram's `timeout=` query —
     * we keep it short (8s) so the user sees a UI tick if anything stalls.
     */
    suspend fun pollOnce(
        sessionToken: String,
        offset: Long,
        timeoutSeconds: Int = 8,
    ): PollResult = withContext(Dispatchers.IO) {
        val urlStr = "$API_BASE/getUpdates" +
            "?offset=$offset" +
            "&timeout=$timeoutSeconds" +
            "&allowed_updates=" + URLEncoder.encode("[\"message\"]", "UTF-8")
        val conn = try {
            (URL(urlStr).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = (timeoutSeconds + 5) * 1000
                requestMethod = "GET"
            }
        } catch (t: Throwable) {
            Log.w(TAG, "getUpdates connect failed", t)
            return@withContext PollResult.Error("Нет подключения")
        }
        try {
            val code = conn.responseCode
            val body = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()
                ?.use { it.readText() }
                .orEmpty()
            if (code !in 200..299) {
                Log.w(TAG, "getUpdates http $code: $body")
                // Conflict (409) typically means there's a webhook set on the
                // bot. We can't unconditionally drop it (would interfere with
                // anyone running a real server), but the user should at least
                // see a coherent message.
                val message = when (code) {
                    409 -> "Бот занят другим клиентом"
                    401 -> "Неверный токен бота"
                    else -> "Ошибка $code"
                }
                return@withContext PollResult.Error(message)
            }
            val json = JSONObject(body)
            if (!json.optBoolean("ok")) {
                return@withContext PollResult.Error(
                    json.optString("description").ifEmpty { "Неизвестная ошибка" }
                )
            }
            val updates = json.optJSONArray("result")
            var nextOffset = offset
            if (updates != null) {
                for (i in 0 until updates.length()) {
                    val update = updates.getJSONObject(i)
                    val id = update.optLong("update_id", 0L)
                    if (id >= nextOffset) nextOffset = id + 1
                    val message = update.optJSONObject("message") ?: continue
                    val text = message.optString("text", "")
                    if (text == "/start $sessionToken") {
                        val from = message.optJSONObject("from") ?: continue
                        val user = TelegramUser(
                            id = from.optLong("id"),
                            firstName = from.optString("first_name", ""),
                            lastName = from.optString("last_name").takeIf { it.isNotEmpty() },
                            username = from.optString("username").takeIf { it.isNotEmpty() },
                        )
                        // Best-effort confirmation message back to the user —
                        // ignore failures, the binding is already complete on
                        // our side.
                        try {
                            sendConfirmation(user.id, user.firstName)
                        } catch (t: Throwable) {
                            Log.w(TAG, "sendMessage confirm failed", t)
                        }
                        return@withContext PollResult.Found(user, nextOffset)
                    }
                }
            }
            return@withContext PollResult.NotFound(nextOffset)
        } catch (t: Throwable) {
            Log.w(TAG, "getUpdates parse failed", t)
            return@withContext PollResult.Error("Не удалось прочитать ответ")
        } finally {
            conn.disconnect()
        }
    }

    private fun sendConfirmation(chatId: Long, @Suppress("UNUSED_PARAMETER") firstName: String) {
        // Plain message with no inline button per the latest spec. The
        // first letter is intentionally lowercase — the user asked for it
        // verbatim that way.
        val text = "тг успешно привязан к летифай, вернитесь в приложение"
        val url = URL(
            "$API_BASE/sendMessage" +
                "?chat_id=$chatId" +
                "&text=" + URLEncoder.encode(text, "UTF-8")
        )
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        try {
            conn.responseCode // execute & discard
        } finally {
            conn.disconnect()
        }
    }

    /**
     * Resolve a Telegram user's profile photo to a public HTTPS URL
     * suitable for Coil. Returns null if the user has no profile photo,
     * if the request fails, or if Telegram refuses the file (rare — the
     * file endpoint is open for files owned by the bot's chat partners).
     *
     * Steps:
     *  1. `getUserProfilePhotos(user_id, limit=1)` — take the most recent.
     *  2. Pick the largest size (last entry in the inner array).
     *  3. `getFile(file_id)` — returns a `file_path`.
     *  4. Compose `https://api.telegram.org/file/bot<token>/<file_path>`.
     *
     * Note: the resulting URL embeds the bot token. Treat as sensitive in
     * the same sense as the rest of the client — we don't expose it any
     * more broadly than the token itself already is.
     */
    suspend fun fetchProfilePhotoUrl(userId: Long): String? = withContext(Dispatchers.IO) {
        try {
            // Path 1: getUserProfilePhotos — preferred (full-res profile set).
            resolveFileIdFromProfilePhotos(userId)
                ?.let { fileId -> resolveFileUrl(fileId) }
                ?.also { return@withContext it }

            // Path 2: getChat — works when the user has started the bot and
            // privacy hides the profile-photo list from getUserProfilePhotos
            // (common on some clients). Chat.photo carries small/big file ids.
            resolveFileIdFromChat(userId)
                ?.let { fileId -> resolveFileUrl(fileId) }
                ?.also { return@withContext it }

            null
        } catch (t: Throwable) {
            Log.w(TAG, "fetchProfilePhotoUrl failed", t)
            null
        }
    }

    private fun resolveFileIdFromProfilePhotos(userId: Long): String? {
        val photosBody = httpGet("$API_BASE/getUserProfilePhotos?user_id=$userId&limit=1")
            ?: return null
        val photosJson = JSONObject(photosBody)
        if (!photosJson.optBoolean("ok")) {
            Log.w(TAG, "getUserProfilePhotos not ok: $photosBody")
            return null
        }
        val result = photosJson.optJSONObject("result") ?: return null
        val photos = result.optJSONArray("photos") ?: return null
        if (photos.length() == 0) return null
        val sizes = photos.getJSONArray(0)
        if (sizes.length() == 0) return null
        // Largest is the last element in Telegram's photo size array.
        val largest = sizes.getJSONObject(sizes.length() - 1)
        return largest.optString("file_id").takeIf { it.isNotEmpty() }
    }

    private fun resolveFileIdFromChat(userId: Long): String? {
        val body = httpGet("$API_BASE/getChat?chat_id=$userId") ?: return null
        val json = JSONObject(body)
        if (!json.optBoolean("ok")) {
            Log.w(TAG, "getChat not ok: $body")
            return null
        }
        val photo = json.optJSONObject("result")?.optJSONObject("photo") ?: return null
        // Prefer the big variant when present.
        return photo.optString("big_file_id").takeIf { it.isNotEmpty() }
            ?: photo.optString("small_file_id").takeIf { it.isNotEmpty() }
    }

    private fun resolveFileUrl(fileId: String): String? {
        val fileBody = httpGet(
            "$API_BASE/getFile?file_id=" + URLEncoder.encode(fileId, "UTF-8")
        ) ?: return null
        val fileJson = JSONObject(fileBody)
        if (!fileJson.optBoolean("ok")) return null
        val filePath = fileJson.optJSONObject("result")
            ?.optString("file_path")
            ?.takeIf { it.isNotEmpty() }
            ?: return null
        return "https://api.telegram.org/file/bot$BOT_TOKEN/$filePath"
    }

    /**
     * Download the remote Telegram file into [targetFile] and return a
     * `file://` URI string Coil can load offline. Returns null on any failure.
     * Prefer this over the raw HTTPS bot-token URL so the avatar survives
     * process death and doesn't re-hit Telegram every time the Profile
     * screen recomposes.
     */
    suspend fun downloadProfilePhotoToFile(
        photoUrl: String,
        targetFile: java.io.File,
    ): String? = withContext(Dispatchers.IO) {
        try {
            val conn = (URL(photoUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 15_000
                requestMethod = "GET"
                // Telegram occasionally rejects requests without a UA.
                setRequestProperty("User-Agent", "Letify/1.0")
            }
            try {
                if (conn.responseCode !in 200..299) {
                    Log.w(TAG, "photo download http ${conn.responseCode}")
                    return@withContext null
                }
                conn.inputStream.use { input ->
                    targetFile.outputStream().use { output -> input.copyTo(output) }
                }
                if (targetFile.length() < 64) {
                    targetFile.delete()
                    return@withContext null
                }
                targetFile.toURI().toString()
            } finally {
                conn.disconnect()
            }
        } catch (t: Throwable) {
            Log.w(TAG, "downloadProfilePhotoToFile failed", t)
            null
        }
    }

    private fun httpGet(urlStr: String): String? {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000
            readTimeout = 8000
            requestMethod = "GET"
            setRequestProperty("User-Agent", "Letify/1.0")
        }
        return try {
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            stream?.bufferedReader()?.use { it.readText() }
        } catch (t: Throwable) {
            Log.w(TAG, "httpGet $urlStr failed", t)
            null
        } finally {
            conn.disconnect()
        }
    }
}
