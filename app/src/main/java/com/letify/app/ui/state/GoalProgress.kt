package com.letify.app.ui.state

import java.time.LocalDate

/**
 * Aggregate goal progress.
 *
 * `overall` is a weighted average of the three sub-goals — 60% weight,
 * 25% habits, 15% tasks — re-normalised across whichever sub-goals are
 * actually present. That re-normalisation is important: if a user has no
 * scheduled habits today, we don't want their entire "overall" number
 * to ceiling at 75%; we drop habits from the average and re-weight
 * weight↔tasks.
 *
 * `null` for a sub-goal means "user hasn't engaged with this category".
 * The detail screen renders the card greyed-out for `null` so the user
 * understands why it's excluded from the headline.
 */
data class GoalBreakdown(
    val overall: Float,            // 0..1
    val weight: WeightProgress?,   // null if no goal set / no movement
    val habits: HabitsProgress?,   // null if no habits scheduled
    val tasks: TasksProgress?,     // null if no tasks scheduled
)

data class WeightProgress(
    val progress: Float,           // 0..1
    val startKg: Float,
    val currentKg: Float,
    val goalKg: Float,
) {
    val deltaKg: Float get() = startKg - currentKg
    val totalKg: Float get() = startKg - goalKg
}

data class HabitsProgress(
    val progress: Float,           // 0..1
    val done: Int,
    val total: Int,
)

data class TasksProgress(
    val progress: Float,           // 0..1
    val done: Int,
    val total: Int,
)

/**
 * Compute the breakdown from the live [AppState].
 *
 * Weight: classic burn-down — `(start - current) / (start - goal)`,
 * clamped to 0..1 so over-shooting the goal still reads as 100%.
 * Returns null when the user is heading away from the goal so far
 * that the formula would underflow (start ≤ goal but losing weight,
 * or start ≥ goal but gaining weight — both legitimate flips we don't
 * try to second-guess; we just hide that card with no progress.)
 *
 * Habits: average completion ratio across the last 7 days, counting
 * only days on which a given habit was scheduled. A habit with target=3
 * recorded once in the daily log counts as 1/3. A day with no scheduled
 * habits is skipped so the user doesn't get penalised for rest days.
 *
 * Tasks: completion ratio across the last 7 days. Past days where the
 * task was scheduled but not marked done count toward "total"; today
 * counts too if the time slot has already passed (statusAt() at end-
 * of-day always reports Done either via the completion set or via
 * "now > endMinutes"). For simplicity we count `completions ∩ last-7`
 * vs the number of scheduled task-day pairs in that window.
 */
fun calculateGoalProgress(state: AppState, today: LocalDate = LocalDate.now()): GoalBreakdown {
    val weight = computeWeightProgress(state)
    val habits = computeHabitsProgress(state, today)
    val tasks = computeTasksProgress(state, today)

    val parts = mutableListOf<Pair<Float, Float>>()  // weight → progress
    weight?.let { parts += 0.60f to it.progress }
    habits?.let { parts += 0.25f to it.progress }
    tasks?.let { parts += 0.15f to it.progress }

    val totalWeight = parts.sumOf { it.first.toDouble() }.toFloat()
    val overall = if (totalWeight == 0f) 0f
                  else parts.sumOf { (it.first * it.second).toDouble() }.toFloat() / totalWeight

    return GoalBreakdown(overall.coerceIn(0f, 1f), weight, habits, tasks)
}

private fun computeWeightProgress(state: AppState): WeightProgress? {
    val start = state.weightStart
    val current = state.weight
    val goal = state.weightGoal
    if (start == current && current == goal) return null
    val direction = goal - start
    if (direction == 0f) return null
    // Linear progress toward the goal, capped at 0..1. Works for both
    // weight-loss (goal < start) and weight-gain (goal > start).
    val raw = (current - start) / direction
    val p = raw.coerceIn(0f, 1f)
    return WeightProgress(progress = p, startKg = start, currentKg = current, goalKg = goal)
}

private fun computeHabitsProgress(state: AppState, today: LocalDate): HabitsProgress? {
    // Look back 7 days inclusive of today. For each (day, habit) pair
    // where the habit is scheduled on that ISO weekday, add target to
    // the denominator and the recorded progress (clamped to target) to
    // the numerator. Returns null if nothing was scheduled at all in
    // the window — keeps the card honest instead of showing "0 of 0".
    var done = 0
    var total = 0
    for (offset in 0..6) {
        val date = today.minusDays(offset.toLong())
        val dow = date.dayOfWeek.value
        val key = date.toString()
        for (habit in state.habits) {
            if (!habit.isScheduledOn(dow)) continue
            total += habit.target
            done += habit.progressOn(key).coerceAtMost(habit.target)
        }
    }
    if (total == 0) return null
    val p = (done.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    return HabitsProgress(progress = p, done = done, total = total)
}

private fun computeTasksProgress(state: AppState, today: LocalDate): TasksProgress? {
    // Window: the past 6 full days plus today. For past days we count a
    // task as "done" only if it's in the completions set (the schedule
    // already passed — uncompleted = missed). For today we use the same
    // strict rule so the headline number doesn't jump up just because
    // the time slot ended; that matches the prototype's "% завершённых
    // задач за 7 дней" subtitle.
    var done = 0
    var total = 0
    for (offset in 0..6) {
        val date = today.minusDays(offset.toLong())
        val dow = date.dayOfWeek.value
        val key = date.toString()
        for (task in state.tasks) {
            if (!task.isScheduledOn(dow)) continue
            total += 1
            if (task.isCompletedOn(key)) done += 1
        }
    }
    if (total == 0) return null
    val p = (done.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    return TasksProgress(progress = p, done = done, total = total)
}
