package app.repdawn.android.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Database(entities = [FolderEntity::class, CardEntity::class, TagEntity::class, CardTagEntity::class, SchedulingEntity::class, ReviewLogEntity::class, GeneratedSentenceEntity::class, SentenceCardLinkEntity::class, IdiomEntity::class, IdiomReviewEntity::class, MixtureWordEntity::class, GenerationJobEntity::class, SyncOutboxEntity::class, TombstoneEntity::class, AppSettingEntity::class], version = 1, exportSchema = true)
abstract class RepDawnDatabase : RoomDatabase() {
    abstract fun dao(): RepDawnDao
    companion object {
        fun open(context: Context) = Room.databaseBuilder(context, RepDawnDatabase::class.java, "repdawn.db").build()
    }
}

@Dao
interface RepDawnDao {
    @Query("SELECT * FROM cards WHERE deletedAt IS NULL ORDER BY updatedAt DESC") fun observeCards(): Flow<List<CardEntity>>
    @Query("SELECT * FROM cards WHERE id = :id") suspend fun card(id: String): CardEntity?
    @Query("SELECT * FROM cards WHERE deletedAt IS NULL AND (normalizedGerman LIKE '%' || :query || '%' OR lower(COALESCE(english,'')) LIKE '%' || :query || '%') ORDER BY german") fun search(query: String): Flow<List<CardEntity>>
    @Query("SELECT c.* FROM cards c JOIN scheduling s ON c.id=s.cardId WHERE c.deletedAt IS NULL AND s.dueAt <= :now ORDER BY s.dueAt LIMIT :limit") suspend fun dueCards(now: Long, limit: Int): List<CardEntity>
    @Query("SELECT * FROM scheduling WHERE cardId = :cardId") suspend fun schedule(cardId: String): SchedulingEntity?
    @Query("SELECT * FROM scheduling WHERE cardId = :cardId") fun observeSchedule(cardId: String): Flow<SchedulingEntity?>
    @Query("SELECT COUNT(*) FROM cards c JOIN scheduling s ON c.id=s.cardId WHERE c.deletedAt IS NULL AND s.dueAt <= :now") fun dueCount(now: Long): Flow<Int>
    @Query("SELECT * FROM cards WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC") fun trash(): Flow<List<CardEntity>>
    @Upsert suspend fun upsertCard(card: CardEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertSchedule(state: SchedulingEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertLog(log: ReviewLogEntity)
    @Query("SELECT * FROM review_logs WHERE cardId = :cardId ORDER BY reviewedAt DESC") suspend fun logs(cardId: String): List<ReviewLogEntity>
    @Query("SELECT * FROM review_logs ORDER BY reviewedAt") suspend fun allLogs(): List<ReviewLogEntity>
    @Query("DELETE FROM review_logs WHERE id = :id") suspend fun deleteLog(id: String)
    @Query("SELECT * FROM folders WHERE deletedAt IS NULL ORDER BY parentId, position, name") fun observeFolders(): Flow<List<FolderEntity>>
    @Upsert suspend fun upsertFolder(folder: FolderEntity)
    @Query("SELECT * FROM folders WHERE id = :id") suspend fun folder(id: String): FolderEntity?
    @Query("UPDATE folders SET name=:name, updatedAt=:at, revision=revision+1 WHERE id=:id") suspend fun renameFolder(id: String, name: String, at: Long)
    @Query("UPDATE folders SET parentId=:parentId, updatedAt=:at, revision=revision+1 WHERE id=:id") suspend fun moveFolder(id: String, parentId: String?, at: Long)
    @Query("UPDATE cards SET folderId=:toFolderId, updatedAt=:at, revision=revision+1 WHERE folderId=:fromFolderId") suspend fun moveCardsFromFolder(fromFolderId: String, toFolderId: String?, at: Long)
    @Query("UPDATE folders SET deletedAt=:at, updatedAt=:at, revision=revision+1 WHERE id=:id") suspend fun trashFolder(id: String, at: Long)
    @Query("UPDATE cards SET deletedAt=:at, updatedAt=:at, revision=revision+1 WHERE id IN (:ids)") suspend fun trashCards(ids: List<String>, at: Long)
    @Query("UPDATE cards SET deletedAt=NULL, updatedAt=:at, revision=revision+1 WHERE id IN (:ids)") suspend fun restoreCards(ids: List<String>, at: Long)
    @Query("DELETE FROM cards WHERE id=:id") suspend fun permanentlyDeleteCard(id: String)
    @Query("DELETE FROM scheduling WHERE cardId=:id") suspend fun deleteSchedule(id: String)
    @Query("DELETE FROM review_logs WHERE cardId=:id") suspend fun deleteLogs(id: String)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertTombstone(tombstone: TombstoneEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertMixture(word: MixtureWordEntity)
    @Query("SELECT * FROM mixture_words WHERE deletedAt IS NULL ORDER BY createdAt DESC") fun observeMixture(): Flow<List<MixtureWordEntity>>
    @Upsert suspend fun upsertSentence(sentence: GeneratedSentenceEntity)
    @Query("SELECT * FROM generated_sentences WHERE deletedAt IS NULL ORDER BY createdAt DESC") fun observeSentences(): Flow<List<GeneratedSentenceEntity>>
    @Upsert suspend fun upsertIdiom(idiom: IdiomEntity)
    @Query("SELECT * FROM idioms WHERE deletedAt IS NULL ORDER BY createdLocalDate DESC") fun observeIdioms(): Flow<List<IdiomEntity>>
    @Query("SELECT * FROM cards") suspend fun backupCards(): List<CardEntity>
    @Query("SELECT * FROM folders") suspend fun backupFolders(): List<FolderEntity>
    @Query("SELECT * FROM scheduling") suspend fun backupSchedules(): List<SchedulingEntity>
}
