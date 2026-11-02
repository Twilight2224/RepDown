package app.repdawn.android.domain

import app.repdawn.android.data.Grade
import app.repdawn.android.data.SchedulingEntity
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

/**
 * A transparent, local FSRS-inspired scheduler. It stores stability and difficulty per
 * card and uses the same deterministic calculation on every device. It intentionally
 * does not claim byte-for-byte compatibility with upstream FSRS; see KNOWN_LIMITATIONS.
 */
object Scheduler {
    private const val MINUTE = 60_000L
    private const val DAY = 86_400_000L
    data class Result(val state: SchedulingEntity, val intervalDays: Double)

    fun estimate(state: SchedulingEntity, grade: Grade): Long = calculate(state, grade, System.currentTimeMillis()).state.dueAt

    fun calculate(state: SchedulingEntity, grade: Grade, now: Long): Result {
        val elapsed = state.lastReviewedAt?.let { max(0.0, (now - it).toDouble() / DAY) } ?: 0.0
        val initial = when (grade) { Grade.AGAIN -> 1.0 / 1440; Grade.HARD -> 1.0 / 24; Grade.GOOD -> 1.0; Grade.EASY -> 4.0 }
        val retrievability = if (state.stabilityDays <= 0.0) 0.0 else exp(-elapsed / state.stabilityDays)
        val nextDifficulty = min(10.0, max(1.0, state.difficulty + when (grade) { Grade.AGAIN -> 1.4; Grade.HARD -> .35; Grade.GOOD -> -.2; Grade.EASY -> -.65 }))
        val nextStability = if (state.repetitions == 0) initial else when (grade) {
            Grade.AGAIN -> max(1.0 / 1440, state.stabilityDays * .35)
            Grade.HARD -> state.stabilityDays * (1.08 + (1.0 - retrievability) * .15)
            Grade.GOOD -> state.stabilityDays * (1.75 + (1.0 - retrievability) * .35) * (11.0 - nextDifficulty) / 7.0
            Grade.EASY -> state.stabilityDays * (2.35 + (1.0 - retrievability) * .45) * (11.0 - nextDifficulty) / 7.0
        }
        val interval = when (grade) {
            Grade.AGAIN -> 1.0 / 1440
            Grade.HARD -> min(nextStability, max(1.0 / 24, state.stabilityDays * .5))
            Grade.GOOD -> min(36500.0, nextStability)
            Grade.EASY -> min(36500.0, nextStability * 1.15)
        }
        val due = now + max(MINUTE, (interval * DAY).toLong())
        return Result(state.copy(dueAt = due, stabilityDays = nextStability, difficulty = nextDifficulty, repetitions = state.repetitions + 1, lapses = state.lapses + if (grade == Grade.AGAIN) 1 else 0, lastReviewedAt = now, manualDueAt = null), interval)
    }
    fun custom(state: SchedulingEntity, durationMs: Long, now: Long) = state.copy(dueAt = now + max(MINUTE, durationMs), manualDueAt = now + max(MINUTE, durationMs), lastReviewedAt = now)
}
