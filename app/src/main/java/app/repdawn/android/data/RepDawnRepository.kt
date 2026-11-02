package app.repdawn.android.data

import androidx.room.withTransaction
import app.repdawn.android.domain.Scheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.Normalizer

class RepDawnRepository(private val db: RepDawnDatabase) {
    private val dao = db.dao()
    private val gradingLock = Mutex()
    fun cards(): Flow<List<CardEntity>> = dao.observeCards()
    fun folders(): Flow<List<FolderEntity>> = dao.observeFolders()
    fun dueCount(): Flow<Int> = dao.dueCount(System.currentTimeMillis())
    fun trash(): Flow<List<CardEntity>> = dao.trash()
    fun mixture(): Flow<List<MixtureWordEntity>> = dao.observeMixture()
    fun sentences(): Flow<List<GeneratedSentenceEntity>> = dao.observeSentences()
    fun idioms(): Flow<List<IdiomEntity>> = dao.observeIdioms()
    suspend fun due(limit: Int = 100): List<CardEntity> = dao.dueCards(System.currentTimeMillis(), limit)
    suspend fun card(id: String) = dao.card(id)
    suspend fun schedule(id: String) = dao.schedule(id)

    suspend fun save(draft: CardDraft, id: String? = null): String = db.withTransaction {
        require(draft.german.trim().isNotEmpty()) { "German text is required." }
        val prior = id?.let { dao.card(it) }
        val card = CardEntity(id = id ?: newId(), german = draft.german.trim(), normalizedGerman = normalize(draft.german), hint = draft.hint.blank(), english = draft.english.blank(), definition = draft.definition.blank(), exampleGerman1 = draft.exampleGerman1.blank(), exampleEnglish1 = draft.exampleEnglish1.blank(), exampleGerman2 = draft.exampleGerman2.blank(), exampleEnglish2 = draft.exampleEnglish2.blank(), notes = draft.notes.blank(), article = draft.article.blank(), plural = draft.plural.blank(), partOfSpeech = draft.partOfSpeech.blank(), conjugation = draft.conjugation.blank(), folderId = draft.folderId, createdAt = prior?.createdAt ?: System.currentTimeMillis(), updatedAt = System.currentTimeMillis(), revision = (prior?.revision ?: 0) + 1)
        dao.upsertCard(card)
        if (prior == null) dao.upsertSchedule(SchedulingEntity(card.id))
        card.id
    }
    suspend fun duplicate(id: String, folderId: String? = null): String = db.withTransaction {
        val source = requireNotNull(dao.card(id))
        val copy = source.copy(id = newId(), folderId = folderId ?: source.folderId, createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis(), deletedAt = null, revision = 1)
        dao.upsertCard(copy); dao.upsertSchedule(SchedulingEntity(copy.id)); copy.id
    }
    suspend fun grade(cardId: String, grade: Grade, token: String = newId()): SchedulingEntity = gradingLock.withLock {
        db.withTransaction {
            val state = requireNotNull(dao.schedule(cardId))
            check(state.lastGradeToken != token) { "This review was already recorded." }
            val next = Scheduler.calculate(state, grade, System.currentTimeMillis())
            dao.upsertSchedule(next.state.copy(lastGradeToken = token))
            dao.insertLog(ReviewLogEntity(cardId = cardId, grade = grade.score, reviewedAt = System.currentTimeMillis(), previousDueAt = state.dueAt, nextDueAt = next.state.dueAt, elapsedDays = state.lastReviewedAt?.let { (System.currentTimeMillis() - it).toDouble() / 86_400_000 } ?: 0.0, intervalDays = next.intervalDays))
            next.state
        }
    }
    suspend fun customGrade(cardId: String, durationMs: Long): SchedulingEntity = gradingLock.withLock { db.withTransaction {
        val state = requireNotNull(dao.schedule(cardId)); val next = Scheduler.custom(state, durationMs, System.currentTimeMillis())
        dao.upsertSchedule(next); dao.insertLog(ReviewLogEntity(cardId = cardId, grade = 0, reviewedAt = System.currentTimeMillis(), previousDueAt = state.dueAt, nextDueAt = next.dueAt, elapsedDays = 0.0, intervalDays = durationMs.toDouble() / 86_400_000)); next
    } }
    suspend fun undoLast(cardId: String): Boolean = db.withTransaction {
        val last = dao.logs(cardId).firstOrNull() ?: return@withTransaction false
        val state = requireNotNull(dao.schedule(cardId))
        dao.upsertSchedule(state.copy(dueAt = last.previousDueAt, repetitions = (state.repetitions - 1).coerceAtLeast(0), lastGradeToken = null))
        dao.deleteLog(last.id); true
    }
    suspend fun trash(ids: List<String>) = db.withTransaction { val now = System.currentTimeMillis(); dao.trashCards(ids, now); ids.forEach { dao.upsertTombstone(TombstoneEntity(entityType = "card", entityId = it, revision = now)) } }
    suspend fun restore(ids: List<String>) = dao.restoreCards(ids, System.currentTimeMillis())
    suspend fun permanentDelete(id: String) = db.withTransaction { dao.deleteLogs(id); dao.deleteSchedule(id); dao.permanentlyDeleteCard(id) }
    suspend fun folder(name: String, parentId: String? = null) { require(name.isNotBlank()); dao.upsertFolder(FolderEntity(name = name.trim(), parentId = parentId)) }
    suspend fun renameFolder(id: String, name: String) { require(name.isNotBlank()); dao.renameFolder(id, name.trim(), System.currentTimeMillis()) }
    suspend fun moveFolder(id: String, parentId: String?) { require(id != parentId) { "A folder cannot contain itself." }; dao.moveFolder(id, parentId, System.currentTimeMillis()) }
    suspend fun deleteFolderMoveCards(id: String, destination: String? = null) = db.withTransaction { require(id != destination); val now = System.currentTimeMillis(); dao.moveCardsFromFolder(id, destination, now); dao.trashFolder(id, now); dao.upsertTombstone(TombstoneEntity(entityType = "folder", entityId = id, revision = now)) }
    suspend fun queueMixture(lines: List<String>) = db.withTransaction { lines.map { it.trim() }.filter { it.isNotBlank() }.distinctBy(::normalize).forEach { dao.upsertMixture(MixtureWordEntity(german = it, normalizedGerman = normalize(it))) } }
    private fun normalize(value: String) = Normalizer.normalize(value.trim(), Normalizer.Form.NFD).replace("\\p{M}".toRegex(), "").lowercase()
}
private fun String.blank(): String? = trim().ifBlank { null }
