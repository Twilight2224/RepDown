package app.repdawn.android.domain

import app.repdawn.android.data.Grade
import app.repdawn.android.data.SchedulingEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SchedulerTest {
    @Test fun `new card grades produce deliberate increasing intervals`() {
        val now = 1_700_000_000_000L
        val initial = SchedulingEntity(cardId = "a", dueAt = now)
        val again = Scheduler.calculate(initial, Grade.AGAIN, now)
        val good = Scheduler.calculate(initial, Grade.GOOD, now)
        val easy = Scheduler.calculate(initial, Grade.EASY, now)
        assertTrue(again.state.dueAt < good.state.dueAt)
        assertTrue(good.state.dueAt < easy.state.dueAt)
        assertEquals(1, good.state.repetitions)
    }
    @Test fun `lapse increments and never schedules before one minute`() {
        val now = 1_700_000_000_000L
        val prior = SchedulingEntity("a", dueAt = now, stabilityDays = 12.0, repetitions = 4, lastReviewedAt = now - 20L * 86_400_000)
        val result = Scheduler.calculate(prior, Grade.AGAIN, now)
        assertEquals(1, result.state.lapses)
        assertTrue(result.state.dueAt >= now + 60_000L)
    }
    @Test fun `manual interval has a floor`() {
        val now = 1_700_000_000_000L
        assertEquals(now + 60_000L, Scheduler.custom(SchedulingEntity("a"), 0, now).dueAt)
    }
}
