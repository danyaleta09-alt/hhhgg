package com.letify.app.ui.state

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.letify.app.data.LetifyDataStore
import com.letify.app.telegram.TelegramAuth
import com.letify.app.telegram.TelegramBindingStore
import com.letify.app.ui.theme.ThemeMode
import com.letify.app.ui.theme.LetifyColors
import java.time.DayOfWeek
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

enum class Tab(val key: String) { Home("home"), Nutrition("nutrition"), Plan("plan"), Profile("profile") }

/** Resolve a persisted [Tab.key] back to the enum (null if unknown/absent). */
fun tabFromKey(key: String?): Tab? = when (key) {
    Tab.Home.key -> Tab.Home
    Tab.Nutrition.key -> Tab.Nutrition
    Tab.Plan.key -> Tab.Plan
    Tab.Profile.key -> Tab.Profile
    else -> null
}

/**
 * Screen-transition animation the user picks in Оформление → Анимация.
 *  - [Push]  — the new single-canvas push: both screens move full width as one
 *              strip at the same speed (Material "Shared Axis X" / iOS push).
 *  - [Cover] — the classic "наплыв": the screen underneath shifts a little to the
 *              left while the new screen slides over it from the right with its
 *              corners smoothly rounding (the r52 look).
 */
enum class TransitionStyle(val key: String) { Push("push"), Cover("cover") }

enum class Gender(val title: String) { Male("Мужской"), Female("Женский"), Other("Не указан") }

// ── Habits ────────────────────────────────────────────────────────────────
//
// `days` is the set of ISO weekdays (1=Mon … 7=Sun) on which the habit is
// scheduled. `log` is the per-day progress count keyed by ISO-8601 date.
//
// Progress is intentionally a per-date map rather than a single counter so
// "did you actually do it today" stays truthful when the day rolls over —
// yesterday's progress doesn't bleed into today.

data class Habit(
    val id: Int,
    val name: String,
    val icon: String,
    val color: Color,
    val target: Int,
    val unit: String,
    val days: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7),
    val remind: Boolean = false,
    val remindAt: String? = null,            // "HH:mm" or null
    val log: Map<String, Int> = emptyMap(),  // dateKey -> progress count
) {
    /** Target floored at 1 — a 0/negative target is meaningless and would
     *  otherwise make the habit count as "done" forever (progress >= 0). */
    val effectiveTarget: Int get() = target.coerceAtLeast(1)

    /** Progress count recorded for [dateKey]. 0 if untouched. */
    fun progressOn(dateKey: String): Int = log[dateKey] ?: 0

    /** Done if today's progress reached the target (target=1 → simple toggle). */
    fun isDoneOn(dateKey: String): Boolean = progressOn(dateKey) >= effectiveTarget

    /** Whether the habit is scheduled on the given ISO weekday (1..7). */
    fun isScheduledOn(isoDow: Int): Boolean = isoDow in days

    /** Compact human-readable schedule like "Ежедневно", "Будни", "Пн Ср Пт". */
    fun scheduleText(): String = scheduleTextFor(days)
}

fun scheduleTextFor(days: Set<Int>): String {
    if (days.isEmpty()) return "—"
    if (days.size == 7) return "Ежедневно"
    if (days == setOf(1, 2, 3, 4, 5)) return "Будни"
    if (days == setOf(6, 7)) return "Выходные"
    val short = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
    return days.sorted().joinToString(" ") { short[it - 1] }
}

// ── Other entities ────────────────────────────────────────────────────────

data class MediaItem(
    val id: String,
    val uri: String,
    val isVideo: Boolean,
    val aspectRatio: Float = 3f / 4f,
    val durationLabel: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

data class WaterEntry(
    val ml: Int,
    val time: String,
    val label: String,
    val icon: String,
    val dateKey: String = Dates.todayKey(),
)

data class Meal(val name: String, val title: String, val icon: String, val color: Color, val kcal: Int?, val description: String?)

enum class TaskStatus { Done, Live, Upcoming }

// A single checklist item inside a task ("подзадача"). `id` is stable within
// the parent task so per-day completion can reference it.
data class Subtask(val id: Int, val title: String)

// `startMinutes` / `endMinutes` are minutes from midnight. `completions`
// records the dateKeys on which the user marked the task done — the live
// status is otherwise derived from the current wall-clock minute.

data class TaskItem(
    val id: Int,
    val name: String,
    val icon: String = "alarm-bold-duotone",
    val color: Color = LetifyColors.Purple,
    val startMinutes: Int,
    val endMinutes: Int,
    val days: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7),
    val remind: Boolean = false,
    val remindMinutesBefore: Int = 10,
    val completions: Set<String> = emptySet(),
    // Optional checklist of sub-items ("подзадачи"). When non-empty, the task
    // card shows a ring of progress (done/total) and expands to reveal the
    // items; the task counts as done once every sub-item is checked.
    val subtasks: List<Subtask> = emptyList(),
    // Per-day completed sub-item ids: dateKey → set of checked Subtask.id.
    val subtaskCompletions: Map<String, Set<Int>> = emptyMap(),
) {
    val startTime: String get() = "%02d:%02d".format(startMinutes / 60, startMinutes % 60)
    val endTime: String get() = "%02d:%02d".format(endMinutes / 60, endMinutes % 60)
    /** Total duration in minutes, wrapping past midnight (e.g. 23:00 → 05:00). */
    val durationMinutes: Int
        get() {
            val raw = endMinutes - startMinutes
            return if (raw <= 0) raw + 24 * 60 else raw
        }

    /** Minutes elapsed since start at [nowMinutes], wrapping past midnight. */
    fun elapsedMinutes(nowMinutes: Int): Int = ((nowMinutes - startMinutes) + 24 * 60) % (24 * 60)

    /** Minutes remaining until end at [nowMinutes], wrapping past midnight. */
    fun remainingMinutes(nowMinutes: Int): Int = ((endMinutes - nowMinutes) + 24 * 60) % (24 * 60)

    val durationLabel: String get() = formatDurationLabel(durationMinutes)

    /** Whether this task has a sub-item checklist. */
    val hasSubtasks: Boolean get() = subtasks.isNotEmpty()

    /** Checked sub-item ids on the given day (intersected with current items). */
    fun completedSubtasksOn(dateKey: String): Set<Int> {
        val done = subtaskCompletions[dateKey] ?: return emptySet()
        return subtasks.mapNotNull { it.id.takeIf { id -> id in done } }.toSet()
    }

    /** (done, total) sub-item counts for the given day. */
    fun subtaskProgressOn(dateKey: String): Pair<Int, Int> =
        completedSubtasksOn(dateKey).size to subtasks.size

    /** Whether every sub-item is checked on the given day. */
    fun allSubtasksDoneOn(dateKey: String): Boolean =
        subtasks.isNotEmpty() && completedSubtasksOn(dateKey).size == subtasks.size

    /** Whether the task is scheduled on the given ISO weekday (1..7). */
    fun isScheduledOn(isoDow: Int): Boolean = isoDow in days

    fun isCompletedOn(dateKey: String): Boolean = dateKey in completions

    fun statusAt(nowMinutes: Int, dateKey: String): TaskStatus {
        if (isCompletedOn(dateKey)) return TaskStatus.Done
        val live = if (endMinutes > startMinutes) {
            nowMinutes in startMinutes until endMinutes
        } else {
            // Task wraps past midnight (e.g. 23:00 → 05:00): live late tonight
            // or early tomorrow morning.
            nowMinutes >= startMinutes || nowMinutes < endMinutes
        }
        if (live) return TaskStatus.Live
        return if (endMinutes > startMinutes && nowMinutes >= endMinutes) {
            TaskStatus.Done
        } else {
            TaskStatus.Upcoming
        }
    }

    fun statusTextAt(nowMinutes: Int, dateKey: String): String {
        if (isCompletedOn(dateKey)) return "Выполнено"
        return when (statusAt(nowMinutes, dateKey)) {
            TaskStatus.Done -> "Выполнено"
            TaskStatus.Live -> "Идёт сейчас · до конца ${formatRelative(remainingMinutes(nowMinutes))}"
            TaskStatus.Upcoming -> {
                val left = startMinutes - nowMinutes
                if (left <= 0) "Запланировано" else "Через ${formatRelative(left)}"
            }
        }
    }
}

private fun formatDurationLabel(total: Int): String = when {
    total <= 0 -> "—"
    total < 60 -> "${total}м"
    total % 60 == 0 -> "${total / 60}ч"
    else -> "${total / 60}ч ${total % 60}м"
}

private fun formatRelative(total: Int): String = when {
    total <= 0 -> "сейчас"
    total < 60 -> "${total} мин"
    total % 60 == 0 -> "${total / 60}ч"
    else -> "${total / 60}ч ${total % 60}м"
}

// ── Real logged data ──────────────────────────────────────────────────────
//
// These are the user's actual records (no demo seed): a list of weigh-ins
// and a list of sleep nights, both keyed by ISO date. The trackers on the
// Progress-Goals screen derive everything they show from these.

data class WeightEntry(val dateKey: String, val kg: Float)

data class SleepEntry(
    val dateKey: String,
    val fromMinutes: Int,   // bedtime, minutes from midnight
    val toMinutes: Int,     // wake time, minutes from midnight
    val quality: Int,       // 0=плохо 1=так себе 2=норм 3=отлично
) {
    /** Duration in minutes, wrapping past midnight (e.g. 23:30 → 07:00). */
    val durationMinutes: Int
        get() {
            val raw = toMinutes - fromMinutes
            return if (raw <= 0) raw + 24 * 60 else raw
        }
}

// ── Date helpers ──────────────────────────────────────────────────────────

object Dates {
    /** Today's date as "yyyy-MM-dd". */
    fun todayKey(): String = LocalDate.now().toString()

    /** ISO weekday for today: 1=Mon … 7=Sun. */
    fun todayDow(): Int = LocalDate.now().dayOfWeek.value

    fun dowFor(date: LocalDate): Int = date.dayOfWeek.value

    fun dayOfWeekFromIso(iso: Int): DayOfWeek = DayOfWeek.of(iso)
}

// ── AppState ──────────────────────────────────────────────────────────────

class AppState(
    private val bindingStore: TelegramBindingStore? = null,
    private val dataStore: LetifyDataStore? = null,
) {
    // Theme + accent are PERSISTED (custom setters write to disk immediately,
    // initial values are loaded from disk) — otherwise the user's choice reset
    // to Dark/Purple on every relaunch.
    private val _themeMode = mutableStateOf(
        when (dataStore?.loadThemeMode(ThemeMode.Dark.name)) {
            ThemeMode.Light.name -> ThemeMode.Light
            ThemeMode.System.name -> ThemeMode.System
            else -> ThemeMode.Dark
        }
    )
    var themeMode: ThemeMode
        get() = _themeMode.value
        set(v) { _themeMode.value = v; dataStore?.saveThemeMode(v.name) }

    private val _accent = mutableStateOf(dataStore?.loadAccent(LetifyColors.Purple) ?: LetifyColors.Purple)
    var accent: Color
        get() = _accent.value
        set(v) { _accent.value = v; dataStore?.saveAccent(v) }

    // ── Motion preferences (persisted) ────────────────────────────────────
    // Screen-transition animation + whether the left-edge swipe-back gesture is
    // active. Backed by mutableStateOf but exposed through custom setters so any
    // assignment is written to disk immediately.
    private val _transitionStyle = mutableStateOf(
        when (dataStore?.loadTransitionStyle(TransitionStyle.Push.key)) {
            TransitionStyle.Cover.key -> TransitionStyle.Cover
            else -> TransitionStyle.Push
        }
    )
    var transitionStyle: TransitionStyle
        get() = _transitionStyle.value
        set(v) { _transitionStyle.value = v; dataStore?.saveTransitionStyle(v.key) }

    private val _swipeBackEnabled = mutableStateOf(dataStore?.loadSwipeBackEnabled(true) ?: true)
    var swipeBackEnabled: Boolean
        get() = _swipeBackEnabled.value
        set(v) { _swipeBackEnabled.value = v; dataStore?.saveSwipeBackEnabled(v) }

    // Whether the thin grey separator lines inside settings containers are
    // shown. Persisted; default ON (shown). Toggling off makes every
    // SettingsRowDivider render nothing for a cleaner, line-free look.
    private val _dividersEnabled = mutableStateOf(dataStore?.loadDividersEnabled(true) ?: true)
    var dividersEnabled: Boolean
        get() = _dividersEnabled.value
        set(v) { _dividersEnabled.value = v; dataStore?.saveDividersEnabled(v) }

    // Selected home-screen launcher icon (persisted, key like "coral"). The
    // actual icon swap (enabling the matching manifest alias) is done in the UI
    // layer where a Context is available; here we only remember the choice so
    // the Иконка picker can show which one is active.
    private val _appIcon = mutableStateOf(dataStore?.loadAppIcon("coral") ?: "coral")
    var appIcon: String
        get() = _appIcon.value
        set(v) { _appIcon.value = v; dataStore?.saveAppIcon(v) }

    // Transient (NOT persisted): true while a long-press "peek" context menu is
    // open on the Plan screen. The app shell observes this to blur + dim the
    // bottom navbar in lockstep with the rest of the screen.
    var peekActive by mutableStateOf(false)

    // Transient (NOT persisted): the live 0..1 peek-dim fraction, written every
    // frame by the Plan screen straight from its peek-transition driver. The
    // navbar reads THIS (not its own separate animation) so the bar darkens and
    // lightens in exact lockstep with the background scrim — one common dim,
    // with no lag on open and (the visible bug) none on close.
    var peekDim by mutableFloatStateOf(0f)

    // Telegram binding — restored from disk on construction.
    var telegramUser by mutableStateOf(bindingStore?.load())
        private set

    // Long-lived scope for background work that must survive the caller's
    // screen being popped — e.g. the Telegram profile-photo fetch below.
    // AppState itself lives for the whole Activity (created once in
    // rememberAppState), so this outlives any single screen's own
    // rememberCoroutineScope, which used to get cancelled if the user
    // backed out of "Привязки" right after binding, before the second
    // network round-trip for the photo had finished — the avatar would
    // then never actually get set even though the bind itself succeeded.
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    init {
        // If the user was already bound from a previous session but the
        // photo never landed (fetch cancelled / privacy / old builds that
        // didn't store photoUrl), kick off a retry the moment AppState is
        // constructed so the Profile avatar appears without re-binding.
        val existing = telegramUser
        if (existing != null && existing.photoUrl.isNullOrBlank()) {
            fetchTelegramPhoto(existing.id)
        }
    }

    fun bindTelegram(user: TelegramAuth.TelegramUser) {
        telegramUser = user
        bindingStore?.save(user)
        val tgName = user.displayName.trim()
        if (tgName.isNotEmpty()) {
            userName = tgName
        }
    }

    /** Resolve and store the bound user's Telegram profile photo. Runs on
     *  [appScope] so it completes even if the caller (the bindings screen)
     *  is no longer on screen by the time the two HTTP round-trips finish. */
    fun fetchTelegramPhoto(userId: Long) {
        appScope.launch {
            val photo = TelegramAuth.fetchProfilePhotoUrl(userId)
            if (photo != null) {
                updateTelegramPhoto(photo)
            }
        }
    }

    fun updateTelegramPhoto(photoUrl: String?) {
        val current = telegramUser ?: return
        if (photoUrl.isNullOrBlank()) return
        val patched = current.copy(photoUrl = photoUrl)
        telegramUser = patched
        bindingStore?.save(patched)
    }

    fun unbindTelegram() {
        telegramUser = null
        bindingStore?.clear()
    }

    // Navbar configuration — order of items and which tab opens by default.
    val navbarOrder: SnapshotStateList<Tab> = mutableStateListOf(
        Tab.Home, Tab.Plan, Tab.Profile,
    )
    // Default landing tab — persisted so the user's pick on Оформление → Навбар
    // survives a relaunch (was in-memory only, so it reset to Главная).
    private val _defaultTab = mutableStateOf(tabFromKey(dataStore?.loadDefaultTab(Tab.Home.key)) ?: Tab.Home)
    var defaultTab: Tab
        get() = _defaultTab.value
        set(v) { _defaultTab.value = v; dataStore?.saveDefaultTab(v.key) }
    var currentTab by mutableStateOf(
        _defaultTab.value.let { if (it == Tab.Nutrition) Tab.Home else it },
    )

    /** The factory navbar layout — order + default landing tab. */
    private val defaultNavbarOrder: List<Tab> =
        listOf(Tab.Home, Tab.Plan, Tab.Profile)

    /** Restore the navbar order and default tab to their factory values. */
    fun resetNavbar() {
        navbarOrder.clear()
        navbarOrder.addAll(defaultNavbarOrder)
        defaultTab = Tab.Home
    }

    // True while a scrollable screen is actively being scrolled/flung. The
    // frosted navbar reads this and DROPS its real-time blur for the duration
    // (falling back to its flat translucent tint), then fades the blur back in
    // once scrolling stops. Re-blurring the backdrop on every frame while
    // content slides under the bar is the main cause of scroll jank on this
    // screen — pausing it during the gesture keeps the list buttery, and the
    // frosted look returns the instant the finger lifts. Same mechanism the
    // navbar already uses to drop the blur during a tab-switch slide.
    var contentScrolling by mutableStateOf(false)

    // Water
    //
    // The balance shown on the day's ring is NOT its own stored counter — it's
    // derived from [waterHistory], filtered to today's dateKey. That's what
    // makes the balance start at 0 on a fresh install and reset to 0 every
    // day automatically: once the wall-clock date rolls over, "today" simply
    // has no entries yet, with no separate reset step to forget to run.
    // Every entry ever logged stays in the log (across days) so the history
    // screen can show real day-by-day stats and a chart.
    val waterHistory: SnapshotStateList<WaterEntry> = mutableStateListOf<WaterEntry>().apply {
        dataStore?.loadWaterLog()?.let { addAll(it) }
    }

    /** Today's total, in мл — what the ring on the water screen fills to. */
    val waterMl: Int get() = waterHistory.filter { it.dateKey == Dates.todayKey() }.sumOf { it.ml }

    // Daily water goal (мл) — persisted so it survives a relaunch. Previously a
    // plain in-memory state, which is why a changed goal reset on restart.
    private val _waterTarget = mutableStateOf(dataStore?.loadWaterTarget(2500) ?: 2500)
    var waterTarget: Int
        get() = _waterTarget.value
        set(v) { _waterTarget.value = v; dataStore?.saveWaterTarget(v) }

    // ── Media gallery (photos / videos from in-app camera) ────────────────
    // Files live in filesDir/media/. Call [reloadMedia] once a Context is
    // available (MediaScreen / app start) to hydrate the list from disk.
    val mediaItems: SnapshotStateList<MediaItem> = mutableStateListOf()

    fun reloadMedia(filesDir: java.io.File) {
        val dir = java.io.File(filesDir, "media")
        val loaded = if (dir.isDirectory) {
            dir.listFiles()
                ?.filter { it.isFile && (it.name.endsWith(".jpg") || it.name.endsWith(".mp4")) }
                ?.sortedByDescending { it.lastModified() }
                ?.map { f ->
                    MediaItem(
                        id = f.name,
                        uri = f.toURI().toString(),
                        isVideo = f.name.endsWith(".mp4"),
                        aspectRatio = 3f / 4f,
                        durationLabel = if (f.name.endsWith(".mp4")) "видео" else "",
                        createdAt = f.lastModified(),
                    )
                }
                .orEmpty()
        } else emptyList()
        mediaItems.clear()
        mediaItems.addAll(loaded)
    }

    fun addMedia(
        path: String,
        isVideo: Boolean,
        aspectRatio: Float = 3f / 4f,
        durationLabel: String = "",
    ) {
        val f = java.io.File(path)
        val item = MediaItem(
            id = f.name,
            uri = f.toURI().toString(),
            isVideo = isVideo,
            aspectRatio = aspectRatio,
            durationLabel = durationLabel,
            createdAt = f.lastModified(),
        )
        // De-dupe if already present (e.g. reload raced with capture).
        mediaItems.removeAll { it.id == item.id }
        mediaItems.add(0, item)
    }

    fun removeMedia(item: MediaItem) {
        mediaItems.remove(item)
        runCatching { java.io.File(java.net.URI(item.uri)).delete() }
    }

    /** Log a water intake for right now (today). Persists immediately. */
    fun addWater(ml: Int, label: String, icon: String) {
        val t = java.time.LocalTime.now()
        val time = "%02d:%02d".format(t.hour, t.minute)
        waterHistory.add(0, WaterEntry(ml, time, label, icon, Dates.todayKey()))
        dataStore?.saveWaterLog(waterHistory.toList())
    }

    /** Remove a single logged entry (e.g. undo a mistaken tap) and persist. */
    fun removeWaterEntry(entry: WaterEntry) {
        if (waterHistory.remove(entry)) dataStore?.saveWaterLog(waterHistory.toList())
    }

    /** All entries for one day, most-recent-first (history screen's day detail). */
    fun waterEntriesOn(dateKey: String): List<WaterEntry> =
        waterHistory.filter { it.dateKey == dateKey }

    /**
     * Totals per day for the last [days] days (oldest → newest), including
     * days with 0 мл, so the history chart always shows a full, even axis.
     */
    fun waterDailyTotals(days: Int): List<Pair<String, Int>> {
        val today = LocalDate.now()
        val totals = waterHistory.groupBy { it.dateKey }.mapValues { (_, v) -> v.sumOf { it.ml } }
        return (days - 1 downTo 0).map { offset ->
            val key = today.minusDays(offset.toLong()).toString()
            key to (totals[key] ?: 0)
        }
    }

    // Nutrition / calories
    var kcal by mutableStateOf(1420)
    // Daily calorie goal (ккал) — persisted (was in-memory only before).
    private val _kcalTarget = mutableStateOf(dataStore?.loadKcalTarget(2300) ?: 2300)
    var kcalTarget: Int
        get() = _kcalTarget.value
        set(v) { _kcalTarget.value = v; dataStore?.saveKcalTarget(v) }
    var protein by mutableStateOf(72)
    val proteinTarget = 130
    var fat by mutableStateOf(42)
    val fatTarget = 70
    var carb by mutableStateOf(160)
    val carbTarget = 280

    val meals: SnapshotStateList<Meal> = mutableStateListOf(
        Meal("breakfast", "Завтрак", "sun-outline", LetifyColors.Orange, 420, "Овсянка с ягодами"),
        Meal("lunch", "Обед", "plate-bold-duotone", LetifyColors.Cal, 620, "Курица с рисом"),
        Meal("dinner", "Ужин", "moon-outline", LetifyColors.Purple, null, "Не добавлено"),
        Meal("snack", "Перекусы", "donut-bitten-outline", LetifyColors.Pink, 380, "Яблоко, орехи"),
    )

    // ── Habits (real, day-scheduled, with daily progress log) ─────────────
    //
    // Persisted: loaded from disk on construction, saved after every mutation.
    // No demo seed — the user starts with an empty list and creates their own
    // via «+ Создать» (the old hard-coded habits could be neither edited nor
    // deleted permanently, which is exactly what the user complained about).
    val habits: SnapshotStateList<Habit> = mutableStateListOf<Habit>().apply {
        dataStore?.loadHabits()?.let { addAll(it) }
    }

    private fun persistHabits() { dataStore?.saveHabits(habits.toList()) }

    /** Habits scheduled for today (ISO-DOW filtered). Recomputed on every
     *  read inside @Composable — reads from `habits` snapshot list, so any
     *  mutation triggers recomposition. */
    fun habitsToday(): List<Habit> {
        val dow = Dates.todayDow()
        return habits.filter { it.isScheduledOn(dow) }
    }

    /** Increment today's progress for a habit. For target<=1 toggles between
     *  0 and 1; for larger targets it bumps by 1 and wraps to 0 once the
     *  target is reached (matches the home-screen ring filling up). */
    fun tapHabit(habitId: Int) {
        val idx = habits.indexOfFirst { it.id == habitId }
        if (idx < 0) return
        val h = habits[idx]
        val k = Dates.todayKey()
        val cur = h.progressOn(k)
        val t = h.effectiveTarget
        val next = when {
            t <= 1 -> if (cur >= 1) 0 else 1
            cur >= t -> 0
            else -> cur + 1
        }
        habits[idx] = h.copy(log = h.log + (k to next))
        persistHabits()
    }

    /** Step today's progress for a habit DOWN by one (floored at 0). */
    fun untapHabit(habitId: Int) {
        val idx = habits.indexOfFirst { it.id == habitId }
        if (idx < 0) return
        val h = habits[idx]
        val k = Dates.todayKey()
        val next = (h.progressOn(k) - 1).coerceAtLeast(0)
        habits[idx] = h.copy(log = h.log + (k to next))
        persistHabits()
    }

    /** Reset today's progress for a habit back to 0 (the ring empties). Backs
     *  the «Сбросить» action in the habit long-press menu. */
    fun resetHabitProgress(habitId: Int) {
        val idx = habits.indexOfFirst { it.id == habitId }
        if (idx < 0) return
        val h = habits[idx]
        val k = Dates.todayKey()
        habits[idx] = h.copy(log = h.log - k)
        persistHabits()
    }

    fun addHabit(h: Habit): Habit {
        val id = (habits.maxOfOrNull { it.id } ?: 0) + 1
        val withId = h.copy(id = id)
        habits.add(withId)
        persistHabits()
        return withId
    }

    /** Replace an existing habit's editable fields (keeps id + progress log). */
    fun updateHabit(updated: Habit) {
        val idx = habits.indexOfFirst { it.id == updated.id }
        if (idx < 0) return
        habits[idx] = updated.copy(log = habits[idx].log)
        persistHabits()
    }

    fun deleteHabit(habitId: Int) {
        val idx = habits.indexOfFirst { it.id == habitId }
        if (idx >= 0) { habits.removeAt(idx); persistHabits() }
    }

    // ── Schedule (tasks) ──────────────────────────────────────────────────
    //
    // Persisted: the list is loaded from disk on construction and saved after
    // every mutation. No demo seed — the user starts with an empty schedule and
    // adds their own tasks via «+ Создать». (The old hard-coded demo tasks
    // could be neither edited nor deleted, which the user explicitly disliked.)
    val tasks: SnapshotStateList<TaskItem> = mutableStateListOf<TaskItem>().apply {
        dataStore?.loadTasks()?.let { addAll(it) }
    }

    private fun persistTasks() { dataStore?.saveTasks(tasks.toList()) }

    /** Tasks scheduled for today. */
    fun tasksToday(): List<TaskItem> {
        val dow = Dates.todayDow()
        return tasks.filter { it.isScheduledOn(dow) }
    }

    /** Toggle task completion for today. */
    fun toggleTaskDone(taskId: Int) {
        val idx = tasks.indexOfFirst { it.id == taskId }
        if (idx < 0) return
        val t = tasks[idx]
        val k = Dates.todayKey()
        val next = if (k in t.completions) t.completions - k else t.completions + k
        tasks[idx] = t.copy(completions = next)
        persistTasks()
    }

    /**
     * Toggle a single sub-item for today. Keeps the parent task's completion
     * in sync: the task is marked done for today iff all sub-items are checked.
     */
    fun toggleSubtask(taskId: Int, subtaskId: Int) {
        val idx = tasks.indexOfFirst { it.id == taskId }
        if (idx < 0) return
        val t = tasks[idx]
        if (t.subtasks.none { it.id == subtaskId }) return
        val k = Dates.todayKey()
        val cur = t.subtaskCompletions[k] ?: emptySet()
        val next = if (subtaskId in cur) cur - subtaskId else cur + subtaskId
        val newMap = t.subtaskCompletions.toMutableMap().apply {
            if (next.isEmpty()) remove(k) else put(k, next)
        }
        val allDone = t.subtasks.isNotEmpty() && t.subtasks.all { it.id in next }
        val newCompletions = if (allDone) t.completions + k else t.completions - k
        tasks[idx] = t.copy(subtaskCompletions = newMap, completions = newCompletions)
        persistTasks()
    }

    fun addTask(t: TaskItem): TaskItem {
        val id = (tasks.maxOfOrNull { it.id } ?: 0) + 1
        val withId = t.copy(id = id)
        tasks.add(withId)
        persistTasks()
        return withId
    }

    /** Replace an existing task's editable fields (keeps id + completion
     *  history / per-day subtask state). Backs the «Изменить» menu action. */
    fun updateTask(updated: TaskItem) {
        val idx = tasks.indexOfFirst { it.id == updated.id }
        if (idx < 0) return
        val old = tasks[idx]
        tasks[idx] = updated.copy(
            completions = old.completions,
            subtaskCompletions = old.subtaskCompletions,
        )
        persistTasks()
    }

    /** Delete several tasks at once (multi-select «Удалить»). */
    fun deleteTasks(ids: Collection<Int>) {
        if (ids.isEmpty()) return
        val set = ids.toSet()
        tasks.removeAll { it.id in set }
        persistTasks()
    }

    fun deleteTask(taskId: Int) {
        val idx = tasks.indexOfFirst { it.id == taskId }
        if (idx >= 0) { tasks.removeAt(idx); persistTasks() }
    }

    // Weight
    //
    // `weightStart` is the user's weight when they first set the current
    // goal — the burn-down progress on the profile screen is `(start -
    // current) / (start - goal)`. We seed it slightly higher than the
    // initial `weight` so the bar lands at a non-zero, non-100 % value
    // on a fresh install (matches the prototype's example) and so the
    // user can see the bar respond as soon as they log a weigh-in.
    // Backed by mutableStateOf but exposed through custom setters so any
    // assignment (here or from the profile goal screen) is persisted.
    private val _weight = mutableStateOf(dataStore?.loadWeight(78.4f) ?: 78.4f)
    private val _weightGoal = mutableStateOf(dataStore?.loadWeightGoal(75.0f) ?: 75.0f)
    private val _weightStart = mutableStateOf(dataStore?.loadWeightStart(80.0f) ?: 80.0f)

    var weight: Float
        get() = _weight.value
        set(v) { _weight.value = v; persistWeightScalars() }
    var weightGoal: Float
        get() = _weightGoal.value
        set(v) { _weightGoal.value = v; persistWeightScalars() }
    var weightStart: Float
        get() = _weightStart.value
        set(v) { _weightStart.value = v; persistWeightScalars() }

    // Real weigh-in history (chronological as logged). Drives the weight chart.
    val weightLog: SnapshotStateList<WeightEntry> =
        mutableStateListOf<WeightEntry>().apply {
            dataStore?.loadWeightLog()?.let { addAll(it) }
        }

    // Real sleep history. Drives the sleep card + bars.
    val sleepLog: SnapshotStateList<SleepEntry> =
        mutableStateListOf<SleepEntry>().apply {
            dataStore?.loadSleepLog()?.let { addAll(it) }
        }

    /** Target sleep per night, in minutes (default 8h). */
    val sleepGoalMinutes: Int = dataStore?.loadSleepGoal(8 * 60) ?: (8 * 60)

    /** Record (or overwrite) today's weigh-in and update the current weight. */
    fun logWeight(kg: Float) {
        val k = Dates.todayKey()
        val entry = WeightEntry(k, kg)
        val idx = weightLog.indexOfFirst { it.dateKey == k }
        if (idx >= 0) weightLog[idx] = entry else weightLog.add(entry)
        weight = kg  // also persists scalars
        dataStore?.saveWeightLog(weightLog.toList())
    }

    /** Record (or overwrite) today's sleep night. */
    fun logSleep(fromMinutes: Int, toMinutes: Int, quality: Int) {
        val k = Dates.todayKey()
        val entry = SleepEntry(k, fromMinutes, toMinutes, quality)
        val idx = sleepLog.indexOfFirst { it.dateKey == k }
        if (idx >= 0) sleepLog[idx] = entry else sleepLog.add(entry)
        dataStore?.saveSleepLog(sleepLog.toList())
    }

    private fun persistWeightScalars() {
        dataStore?.saveWeightScalars(_weight.value, _weightGoal.value, _weightStart.value)
    }

    // Profile — persisted so name/age/gender survive a full restart. Backed by
    // mutableStateOf but exposed through custom setters that write to disk on
    // every assignment (from EditProfileScreen or a Telegram bind). Previously
    // these were in-memory only, so the name reset to "Иван" on every relaunch.
    private val _userName = mutableStateOf(dataStore?.loadUserName("Иван") ?: "Иван")
    private val _age = mutableStateOf(dataStore?.loadAge(22) ?: 22)
    private val _gender = mutableStateOf(
        when (dataStore?.loadGender(Gender.Male.name)) {
            Gender.Female.name -> Gender.Female
            Gender.Other.name -> Gender.Other
            else -> Gender.Male
        }
    )
    var userName: String
        get() = _userName.value
        set(v) { _userName.value = v; persistProfile() }
    var age: Int
        get() = _age.value
        set(v) { _age.value = v; persistProfile() }
    var gender: Gender
        get() = _gender.value
        set(v) { _gender.value = v; persistProfile() }

    private fun persistProfile() {
        dataStore?.saveProfile(_userName.value, _age.value, _gender.value.name)
    }

    // Notifications (very lightweight UI-only flags for the screen)
    var notifyMorning by mutableStateOf(true)
    var notifyHabits by mutableStateOf(true)
    var notifyWater by mutableStateOf(false)

    // Legacy aliases retained for callers that still read the immutable goal.
    val waterGoal: Int get() = waterTarget
    val kcalGoal: Int get() = kcalTarget

    fun overallProgress(): Float {
        val w = (waterMl.toFloat() / waterTarget.coerceAtLeast(1)).coerceIn(0f, 1f)
        val k = (kcal.toFloat() / kcalTarget.coerceAtLeast(1)).coerceIn(0f, 1f)
        val today = tasksToday()
        val nowMin = java.time.LocalTime.now().toSecondOfDay() / 60
        val dateKey = Dates.todayKey()
        val planP = if (today.isEmpty()) 0f else
            today.count { it.statusAt(nowMin, dateKey) == TaskStatus.Done }.toFloat() / today.size
        return ((w + k + planP) / 3f).coerceIn(0f, 1f)
    }
}

val LocalAppState = compositionLocalOf<AppState> { error("AppState not provided") }

@Composable
fun rememberAppState(): AppState {
    val context = LocalContext.current.applicationContext
    return remember {
        AppState(
            bindingStore = TelegramBindingStore(context),
            dataStore = LetifyDataStore(context),
        )
    }
}
