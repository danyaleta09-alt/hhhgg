package com.letify.app.data

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.letify.app.ui.state.Habit
import com.letify.app.ui.state.SleepEntry
import com.letify.app.ui.state.Subtask
import com.letify.app.ui.state.TaskItem
import com.letify.app.ui.state.WaterEntry
import com.letify.app.ui.state.WeightEntry

/**
 * SharedPreferences-backed persistence for the user's REAL logged data —
 * weigh-ins and sleep records — plus the weight goal scalars. Mirrors the
 * lightweight approach of [com.letify.app.telegram.TelegramBindingStore].
 *
 * Logs are encoded as flat strings (record separator ';', field separator
 * '|') to avoid pulling in a JSON/serialization dependency. The date keys
 * are ISO "yyyy-MM-dd" so they never contain the separators.
 */
class LetifyDataStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("letify.data", Context.MODE_PRIVATE)

    // ── Weight scalars ────────────────────────────────────────────────
    fun loadWeight(default: Float): Float = prefs.getFloat(KEY_WEIGHT, default)
    fun loadWeightGoal(default: Float): Float = prefs.getFloat(KEY_WEIGHT_GOAL, default)
    fun loadWeightStart(default: Float): Float = prefs.getFloat(KEY_WEIGHT_START, default)

    fun saveWeightScalars(weight: Float, goal: Float, start: Float) {
        prefs.edit()
            .putFloat(KEY_WEIGHT, weight)
            .putFloat(KEY_WEIGHT_GOAL, goal)
            .putFloat(KEY_WEIGHT_START, start)
            .apply()
    }

    fun loadSleepGoal(default: Int): Int = prefs.getInt(KEY_SLEEP_GOAL, default)

    // ── Nutrition goals (water + calories) — persisted ────────────────
    fun loadWaterTarget(default: Int): Int = prefs.getInt(KEY_WATER_TARGET, default)
    fun saveWaterTarget(value: Int) { prefs.edit().putInt(KEY_WATER_TARGET, value).apply() }
    fun loadKcalTarget(default: Int): Int = prefs.getInt(KEY_KCAL_TARGET, default)
    fun saveKcalTarget(value: Int) { prefs.edit().putInt(KEY_KCAL_TARGET, value).apply() }

    // ── Navbar default landing tab — persisted ────────────────────────
    fun loadDefaultTab(default: String): String =
        prefs.getString(KEY_DEFAULT_TAB, default) ?: default
    fun saveDefaultTab(key: String) { prefs.edit().putString(KEY_DEFAULT_TAB, key).apply() }

    // ── Profile (persisted so name/age/gender survive a restart) ──────
    // Previously these lived only in memory, so every relaunch reset the
    // name back to the hard-coded default ("Иван"). Now they round-trip
    // through prefs via AppState's custom setters.
    fun loadUserName(default: String): String =
        prefs.getString(KEY_USER_NAME, default) ?: default

    fun loadAge(default: Int): Int = prefs.getInt(KEY_AGE, default)

    fun loadGender(default: String): String =
        prefs.getString(KEY_GENDER, default) ?: default

    fun saveProfile(name: String, age: Int, gender: String) {
        prefs.edit()
            .putString(KEY_USER_NAME, name)
            .putInt(KEY_AGE, age)
            .putString(KEY_GENDER, gender)
            .apply()
    }

    // ── UI / motion preferences ───────────────────────────────────────
    fun loadTransitionStyle(default: String): String =
        prefs.getString(KEY_TRANSITION_STYLE, default) ?: default

    fun saveTransitionStyle(key: String) {
        prefs.edit().putString(KEY_TRANSITION_STYLE, key).apply()
    }

    fun loadSwipeBackEnabled(default: Boolean): Boolean =
        prefs.getBoolean(KEY_SWIPE_BACK, default)

    fun saveSwipeBackEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SWIPE_BACK, enabled).apply()
    }

    fun loadDividersEnabled(default: Boolean): Boolean =
        prefs.getBoolean(KEY_DIVIDERS, default)

    fun saveDividersEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DIVIDERS, enabled).apply()
    }

    fun loadAppIcon(default: String): String =
        prefs.getString(KEY_APP_ICON, default) ?: default

    fun saveAppIcon(key: String) {
        prefs.edit().putString(KEY_APP_ICON, key).apply()
    }

    // ── Theme (persisted) ─────────────────────────────────────────────
    // themeMode stored as the enum name; accent stored as a packed ARGB int.
    fun loadThemeMode(default: String): String =
        prefs.getString(KEY_THEME_MODE, default) ?: default

    fun saveThemeMode(name: String) {
        prefs.edit().putString(KEY_THEME_MODE, name).apply()
    }

    fun loadAccent(default: Color): Color {
        if (!prefs.contains(KEY_ACCENT)) return default
        return Color(prefs.getInt(KEY_ACCENT, default.toArgb()))
    }

    fun saveAccent(color: Color) {
        prefs.edit().putInt(KEY_ACCENT, color.toArgb()).apply()
    }

    // ── Weight log ────────────────────────────────────────────────────
    fun loadWeightLog(): List<WeightEntry> {
        val raw = prefs.getString(KEY_WEIGHT_LOG, null) ?: return emptyList()
        return raw.split(RECORD_SEP).mapNotNull { token ->
            if (token.isBlank()) return@mapNotNull null
            val p = token.split(FIELD_SEP)
            if (p.size != 2) return@mapNotNull null
            val kg = p[1].toFloatOrNull() ?: return@mapNotNull null
            WeightEntry(p[0], kg)
        }
    }

    fun saveWeightLog(entries: List<WeightEntry>) {
        val raw = entries.joinToString(RECORD_SEP) { "${it.dateKey}$FIELD_SEP${it.kg}" }
        prefs.edit().putString(KEY_WEIGHT_LOG, raw).apply()
    }

    // ── Sleep log ─────────────────────────────────────────────────────
    fun loadSleepLog(): List<SleepEntry> {
        val raw = prefs.getString(KEY_SLEEP_LOG, null) ?: return emptyList()
        return raw.split(RECORD_SEP).mapNotNull { token ->
            if (token.isBlank()) return@mapNotNull null
            val p = token.split(FIELD_SEP)
            if (p.size != 4) return@mapNotNull null
            val from = p[1].toIntOrNull() ?: return@mapNotNull null
            val to = p[2].toIntOrNull() ?: return@mapNotNull null
            val q = p[3].toIntOrNull() ?: return@mapNotNull null
            SleepEntry(p[0], from, to, q)
        }
    }

    fun saveSleepLog(entries: List<SleepEntry>) {
        val raw = entries.joinToString(RECORD_SEP) {
            "${it.dateKey}$FIELD_SEP${it.fromMinutes}$FIELD_SEP${it.toMinutes}$FIELD_SEP${it.quality}"
        }
        prefs.edit().putString(KEY_SLEEP_LOG, raw).apply()
    }

    // ── Water log ─────────────────────────────────────────────────────
    // Every logged intake, across all days (not just today) — the history
    // screen groups these by dateKey for the daily list + chart. Fields:
    // dateKey|ml|time|label|icon. label/icon come from a fixed set of app
    // strings (no '|' or ';'), so the flat encoding is safe.
    fun loadWaterLog(): List<WaterEntry> {
        val raw = prefs.getString(KEY_WATER_LOG, null) ?: return emptyList()
        return raw.split(RECORD_SEP).mapNotNull { token ->
            if (token.isBlank()) return@mapNotNull null
            val p = token.split(FIELD_SEP)
            if (p.size != 5) return@mapNotNull null
            val ml = p[1].toIntOrNull() ?: return@mapNotNull null
            WaterEntry(ml = ml, time = p[2], label = p[3], icon = p[4], dateKey = p[0])
        }
    }

    fun saveWaterLog(entries: List<WaterEntry>) {
        val raw = entries.joinToString(RECORD_SEP) {
            "${it.dateKey}$FIELD_SEP${it.ml}$FIELD_SEP${it.time}$FIELD_SEP${it.label}$FIELD_SEP${it.icon}"
        }
        prefs.edit().putString(KEY_WATER_LOG, raw).apply()
    }

    // ── Tasks (schedule) ──────────────────────────────────────────────
    // Persisted so user-created tasks survive app restarts (previously the
    // list was re-seeded with demos every launch and new tasks were lost).
    // A null return means "never saved" → caller seeds the demo list once.
    //
    // Fields are separated by U+0001 (never present in user text) and records
    // by newline, so task names can safely contain '|' or ';'.
    fun loadTasks(): List<TaskItem>? {
        val raw = prefs.getString(KEY_TASKS, null) ?: return null
        return raw.split('\n').mapNotNull { token ->
            if (token.isBlank()) return@mapNotNull null
            val p = token.split('\u0001')
            if (p.size < 10) return@mapNotNull null
            runCatching {
                // Record layout: fields 0..9 are the core task. Subtasks (and their
                // per-day completions) live in the last two fields.
                //   • Current format → 12 fields: subtasks at 10/11.
                //   • Legacy format  → 14 fields: a now-removed feature occupied
                //     slots 10/11, so subtasks sat at 12/13. We still read those so
                //     tasks saved by older builds keep their subtasks intact.
                val subIdx = if (p.size >= 14) 12 else 10
                TaskItem(
                    id = p[0].toInt(),
                    name = p[1],
                    icon = p[2],
                    color = Color(p[3].toInt()),
                    startMinutes = p[4].toInt(),
                    endMinutes = p[5].toInt(),
                    days = p[6].splitToInts(),
                    remind = p[7] == "1",
                    remindMinutesBefore = p[8].toInt(),
                    completions = p[9].splitToStrings(),
                    subtasks = if (p.size > subIdx) p[subIdx].parseSubtasks() else emptyList(),
                    subtaskCompletions = if (p.size > subIdx + 1) p[subIdx + 1].parseSubCompletions() else emptyMap(),
                )
            }.getOrNull()
        }
    }

    fun saveTasks(tasks: List<TaskItem>) {
        val raw = tasks.joinToString("\n") { t ->
            listOf(
                t.id.toString(),
                t.name,
                t.icon,
                t.color.toArgb().toString(),
                t.startMinutes.toString(),
                t.endMinutes.toString(),
                t.days.sorted().joinToString(","),
                if (t.remind) "1" else "0",
                t.remindMinutesBefore.toString(),
                t.completions.joinToString(","),
                // Field 10: subtasks as id\u0002title pairs joined by \u0003.
                t.subtasks.joinToString("\u0003") { "${it.id}\u0002${it.title}" },
                // Field 11: per-day completed ids as dateKey\u0002id,id groups by \u0003.
                t.subtaskCompletions.entries.joinToString("\u0003") { (k, ids) ->
                    "$k\u0002${ids.joinToString(",")}"
                },
            ).joinToString("\u0001")
        }
        prefs.edit().putString(KEY_TASKS, raw).apply()
    }

    // ── Habits ────────────────────────────────────────────────────────
    // Persisted so user-created habits (and deletions of the old demo seed)
    // survive app restarts. Previously habits lived only in memory and the
    // demo list was re-seeded on every launch, so deleting/editing a habit
    // never stuck. A null return means "never saved" → caller starts empty.
    //
    // Fields separated by U+0001, records by newline (same scheme as tasks),
    // so habit names/units can safely contain '|' ';' ','.
    //   0:id 1:name 2:icon 3:color(argb) 4:target 5:unit 6:days(csv)
    //   7:remind(0/1) 8:remindAt(or empty) 9:log(dateKey\u0002count joined by \u0003)
    fun loadHabits(): List<Habit>? {
        val raw = prefs.getString(KEY_HABITS, null) ?: return null
        return raw.split('\n').mapNotNull { token ->
            if (token.isBlank()) return@mapNotNull null
            val p = token.split('\u0001')
            if (p.size < 9) return@mapNotNull null
            runCatching {
                Habit(
                    id = p[0].toInt(),
                    name = p[1],
                    icon = p[2],
                    color = Color(p[3].toInt()),
                    target = p[4].toInt(),
                    unit = p[5],
                    days = p[6].splitToInts(),
                    remind = p[7] == "1",
                    remindAt = p[8].ifBlank { null },
                    log = if (p.size > 9) p[9].parseHabitLog() else emptyMap(),
                )
            }.getOrNull()
        }
    }

    fun saveHabits(habits: List<Habit>) {
        val raw = habits.joinToString("\n") { h ->
            listOf(
                h.id.toString(),
                h.name,
                h.icon,
                h.color.toArgb().toString(),
                h.target.toString(),
                h.unit,
                h.days.sorted().joinToString(","),
                if (h.remind) "1" else "0",
                h.remindAt ?: "",
                h.log.entries.joinToString("\u0003") { (k, c) -> "$k\u0002$c" },
            ).joinToString("\u0001")
        }
        prefs.edit().putString(KEY_HABITS, raw).apply()
    }

    private fun String.parseHabitLog(): Map<String, Int> =
        if (isBlank()) emptyMap() else split('\u0003').mapNotNull { tok ->
            if (tok.isBlank()) return@mapNotNull null
            val parts = tok.split('\u0002')
            if (parts.size != 2) return@mapNotNull null
            val c = parts[1].toIntOrNull() ?: return@mapNotNull null
            parts[0] to c
        }.toMap()

    private fun String.parseSubtasks(): List<Subtask> =
        if (isBlank()) emptyList() else split('\u0003').mapNotNull { tok ->
            if (tok.isBlank()) return@mapNotNull null
            val parts = tok.split('\u0002')
            if (parts.size != 2) return@mapNotNull null
            val id = parts[0].toIntOrNull() ?: return@mapNotNull null
            Subtask(id, parts[1])
        }

    private fun String.parseSubCompletions(): Map<String, Set<Int>> =
        if (isBlank()) emptyMap() else split('\u0003').mapNotNull { tok ->
            if (tok.isBlank()) return@mapNotNull null
            val parts = tok.split('\u0002')
            if (parts.size != 2) return@mapNotNull null
            val ids = parts[1].split(",").mapNotNull { it.toIntOrNull() }.toSet()
            if (ids.isEmpty()) null else parts[0] to ids
        }.toMap()

    private fun String.splitToInts(): Set<Int> =
        if (isBlank()) emptySet() else split(",").mapNotNull { it.toIntOrNull() }.toSet()

    private fun String.splitToStrings(): Set<String> =
        if (isBlank()) emptySet() else split(",").filter { it.isNotBlank() }.toSet()

    private companion object {
        const val RECORD_SEP = ";"
        const val FIELD_SEP = "|"
        const val KEY_TASKS = "tasks_v1"
        const val KEY_WEIGHT = "weight"
        const val KEY_WEIGHT_GOAL = "weight_goal"
        const val KEY_WEIGHT_START = "weight_start"
        const val KEY_SLEEP_GOAL = "sleep_goal"
        const val KEY_WEIGHT_LOG = "weight_log"
        const val KEY_SLEEP_LOG = "sleep_log"
        const val KEY_TRANSITION_STYLE = "transition_style"
        const val KEY_SWIPE_BACK = "swipe_back_enabled"
        const val KEY_DIVIDERS = "dividers_enabled"
        const val KEY_APP_ICON = "app_icon"
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_ACCENT = "accent_argb"
        const val KEY_HABITS = "habits_v1"
        const val KEY_USER_NAME = "user_name"
        const val KEY_AGE = "user_age"
        const val KEY_GENDER = "user_gender"
        const val KEY_WATER_TARGET = "water_target"
        const val KEY_WATER_LOG = "water_log"
        const val KEY_KCAL_TARGET = "kcal_target"
        const val KEY_DEFAULT_TAB = "default_tab"
    }
}
