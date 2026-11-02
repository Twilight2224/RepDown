package app.repdawn.android.network

/**
 * Boundary for Supabase sync and the generation proxy. Configuration is deliberately
 * absent from the APK: the owner supplies only public Supabase values in settings and
 * secrets remain in the Edge Function. Failures are returned to optional screens only.
 */
interface OptionalServices {
    suspend fun syncNow(): Result<Unit>
    suspend fun processGenerationBatch(): Result<Int>
}
class DisabledOptionalServices : OptionalServices {
    override suspend fun syncNow() = Result.failure<Unit>(IllegalStateException("Cloud sync is not configured."))
    override suspend fun processGenerationBatch() = Result.failure<Int>(IllegalStateException("AI generation is not configured."))
}
