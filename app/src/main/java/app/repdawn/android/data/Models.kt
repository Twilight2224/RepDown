package app.repdawn.android.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

fun newId(): String = UUID.randomUUID().toString()

@Entity(tableName = "folders", indices = [Index("parentId"), Index("deletedAt")])
data class FolderEntity(
    @PrimaryKey val id: String = newId(),
    val name: String,
    val parentId: String? = null,
    val position: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val revision: Long = 1
)

@Entity(tableName = "cards", foreignKeys = [ForeignKey(entity = FolderEntity::class, parentColumns = ["id"], childColumns = ["folderId"], onDelete = ForeignKey.SET_NULL)], indices = [Index("folderId"), Index("deletedAt"), Index("normalizedGerman")])
data class CardEntity(
    @PrimaryKey val id: String = newId(),
    val german: String,
    val normalizedGerman: String = german.lowercase(),
    val hint: String? = null,
    val english: String? = null,
    val definition: String? = null,
    val exampleGerman1: String? = null,
    val exampleEnglish1: String? = null,
    val exampleGerman2: String? = null,
    val exampleEnglish2: String? = null,
    val notes: String? = null,
    val article: String? = null,
    val plural: String? = null,
    val partOfSpeech: String? = null,
    val conjugation: String? = null,
    val folderId: String? = null,
    val isMachineGenerated: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val revision: Long = 1
)

val CardEntity.complete: Boolean get() = !english.isNullOrBlank() && !definition.isNullOrBlank()

@Entity(tableName = "tags") data class TagEntity(@PrimaryKey val id: String = newId(), val name: String, val normalizedName: String = name.lowercase())
@Entity(tableName = "card_tags", primaryKeys = ["cardId", "tagId"], foreignKeys = [ForeignKey(entity = CardEntity::class, parentColumns = ["id"], childColumns = ["cardId"], onDelete = ForeignKey.CASCADE), ForeignKey(entity = TagEntity::class, parentColumns = ["id"], childColumns = ["tagId"], onDelete = ForeignKey.CASCADE)], indices = [Index("tagId")]) data class CardTagEntity(val cardId: String, val tagId: String)

@Entity(tableName = "scheduling", foreignKeys = [ForeignKey(entity = CardEntity::class, parentColumns = ["id"], childColumns = ["cardId"], onDelete = ForeignKey.CASCADE)], indices = [Index("dueAt")])
data class SchedulingEntity(
    @PrimaryKey val cardId: String,
    val dueAt: Long = System.currentTimeMillis(),
    val stabilityDays: Double = 0.0,
    val difficulty: Double = 5.0,
    val repetitions: Int = 0,
    val lapses: Int = 0,
    val lastReviewedAt: Long? = null,
    val manualDueAt: Long? = null,
    val lastGradeToken: String? = null
)

@Entity(tableName = "review_logs", foreignKeys = [ForeignKey(entity = CardEntity::class, parentColumns = ["id"], childColumns = ["cardId"], onDelete = ForeignKey.CASCADE)], indices = [Index("cardId"), Index("reviewedAt")])
data class ReviewLogEntity(
    @PrimaryKey val id: String = newId(),
    val cardId: String,
    val grade: Int,
    val reviewedAt: Long,
    val previousDueAt: Long,
    val nextDueAt: Long,
    val elapsedDays: Double,
    val intervalDays: Double,
    val undoOf: String? = null
)

@Entity(tableName = "generated_sentences", indices = [Index("usedAt")])
data class GeneratedSentenceEntity(@PrimaryKey val id: String = newId(), val english: String, val german: String, val grammarExplanation: String? = null, val difficulty: String = "A1", val vocabularySnapshot: String = "", val createdAt: Long = System.currentTimeMillis(), val usedAt: Long? = null, val deletedAt: Long? = null)
@Entity(tableName = "sentence_card_links", primaryKeys = ["sentenceId", "cardId"], foreignKeys = [ForeignKey(entity = GeneratedSentenceEntity::class, parentColumns = ["id"], childColumns = ["sentenceId"], onDelete = ForeignKey.CASCADE), ForeignKey(entity = CardEntity::class, parentColumns = ["id"], childColumns = ["cardId"], onDelete = ForeignKey.CASCADE)], indices = [Index("cardId")]) data class SentenceCardLinkEntity(val sentenceId: String, val cardId: String, val word: String)

@Entity(tableName = "idioms", indices = [Index("createdLocalDate"), Index("deletedAt")])
data class IdiomEntity(@PrimaryKey val id: String = newId(), val german: String, val literalEnglish: String? = null, val naturalEnglish: String, val explanation: String, val exampleGerman: String, val exampleEnglish: String, val register: String? = null, val region: String? = null, val createdLocalDate: String, val isMachineGenerated: Boolean = true, val deletedAt: Long? = null)
@Entity(tableName = "idiom_reviews", foreignKeys = [ForeignKey(entity = IdiomEntity::class, parentColumns = ["id"], childColumns = ["idiomId"], onDelete = ForeignKey.CASCADE)], indices = [Index("idiomId"), Index("dueAt")]) data class IdiomReviewEntity(@PrimaryKey val idiomId: String, val dueAt: Long = System.currentTimeMillis(), val repetitions: Int = 0)

@Entity(tableName = "mixture_words", indices = [Index("status"), Index("normalizedGerman")])
data class MixtureWordEntity(@PrimaryKey val id: String = newId(), val german: String, val normalizedGerman: String = german.lowercase(), val status: String = "PENDING", val cardJson: String? = null, val error: String? = null, val createdAt: Long = System.currentTimeMillis(), val updatedAt: Long = System.currentTimeMillis(), val deletedAt: Long? = null)
@Entity(tableName = "generation_jobs", indices = [Index("status"), Index("kind")]) data class GenerationJobEntity(@PrimaryKey val id: String = newId(), val kind: String, val payload: String, val status: String = "PENDING", val attempts: Int = 0, val error: String? = null, val createdAt: Long = System.currentTimeMillis(), val updatedAt: Long = System.currentTimeMillis())
@Entity(tableName = "sync_outbox", indices = [Index("status"), Index("createdAt")]) data class SyncOutboxEntity(@PrimaryKey val id: String = newId(), val kind: String, val entityId: String, val payload: String, val status: String = "PENDING", val attempts: Int = 0, val createdAt: Long = System.currentTimeMillis())
@Entity(tableName = "tombstones", indices = [Index("entityType"), Index("deletedAt")]) data class TombstoneEntity(@PrimaryKey val id: String = newId(), val entityType: String, val entityId: String, val revision: Long, val deletedAt: Long = System.currentTimeMillis())
@Entity(tableName = "app_settings") data class AppSettingEntity(@PrimaryKey val key: String, val value: String, val updatedAt: Long = System.currentTimeMillis())

enum class Grade(val score: Int) { AGAIN(1), HARD(2), GOOD(3), EASY(4) }
data class CardDraft(val german: String = "", val hint: String = "", val english: String = "", val definition: String = "", val exampleGerman1: String = "", val exampleEnglish1: String = "", val exampleGerman2: String = "", val exampleEnglish2: String = "", val notes: String = "", val article: String = "", val plural: String = "", val partOfSpeech: String = "", val conjugation: String = "", val folderId: String? = null)
