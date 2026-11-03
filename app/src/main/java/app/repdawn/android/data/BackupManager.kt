package app.repdawn.android.data

import android.content.ContentResolver
import android.net.Uri
import androidx.room.withTransaction
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** Versioned, offline-only ZIP backup. Import only merges after the whole JSON is validated. */
class BackupManager(private val db: RepDawnDatabase) {
    private val dao = db.dao()
    suspend fun export(resolver: ContentResolver, uri: Uri) {
        val data = JSONObject().apply {
            put("formatVersion", 1); put("exportedAt", System.currentTimeMillis())
            put("folders", JSONArray(dao.backupFolders().map { it.json() }))
            put("cards", JSONArray(dao.backupCards().map { it.json() }))
            put("scheduling", JSONArray(dao.backupSchedules().map { it.json() }))
            put("reviewLogs", JSONArray(dao.allLogs().map { it.json() }))
        }.toString()
        val checksum = MessageDigest.getInstance("SHA-256").digest(data.toByteArray()).joinToString("") { "%02x".format(it) }
        resolver.openOutputStream(uri)?.use { output -> ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry("data.json")); zip.write(data.toByteArray()); zip.closeEntry()
            zip.putNextEntry(ZipEntry("manifest.json")); zip.write(JSONObject(mapOf("formatVersion" to 1, "sha256" to checksum)).toString().toByteArray()); zip.closeEntry()
        } } ?: error("The selected file could not be opened.")
    }
    suspend fun importMerge(resolver: ContentResolver, uri: Uri) {
        val files = mutableMapOf<String, String>()
        resolver.openInputStream(uri)?.use { ZipInputStream(it).use { zip -> var entry = zip.nextEntry; while (entry != null) { files[entry.name] = zip.readBytes().decodeToString(); entry = zip.nextEntry } } } ?: error("The selected file could not be opened.")
        val dataText = requireNotNull(files["data.json"]) { "Backup has no data.json." }; val manifest = JSONObject(requireNotNull(files["manifest.json"]) { "Backup has no manifest." })
        require(manifest.getInt("formatVersion") == 1) { "Unsupported backup format." }
        val actual = MessageDigest.getInstance("SHA-256").digest(dataText.toByteArray()).joinToString("") { "%02x".format(it) }
        require(actual == manifest.getString("sha256")) { "Backup checksum does not match." }
        val root = JSONObject(dataText); require(root.getInt("formatVersion") == 1) { "Unsupported backup data." }
        val folders = root.getJSONArray("folders"); val cards = root.getJSONArray("cards"); val schedules = root.getJSONArray("scheduling"); val logs = root.getJSONArray("reviewLogs")
        // Parse first: an invalid row never leaves a partial import.
        val parsedFolders = (0 until folders.length()).map { folders.getJSONObject(it).folder() }
        val parsedCards = (0 until cards.length()).map { cards.getJSONObject(it).card() }
        val parsedSchedules = (0 until schedules.length()).map { schedules.getJSONObject(it).schedule() }
        val parsedLogs = (0 until logs.length()).map { logs.getJSONObject(it).log() }
        db.withTransaction {
            for (folder in parsedFolders) {
                dao.upsertFolder(folder)
            }
            for (card in parsedCards) {
                dao.upsertCard(card)
            }
            for (schedule in parsedSchedules) {
                dao.upsertSchedule(schedule)
            }
            for (log in parsedLogs) {
                dao.insertLog(log)
            }
        }
    }
}
private fun FolderEntity.json() = JSONObject(mapOf("id" to id, "name" to name, "parentId" to parentId, "position" to position, "createdAt" to createdAt, "updatedAt" to updatedAt, "deletedAt" to deletedAt, "revision" to revision))
private fun CardEntity.json() = JSONObject(mapOf("id" to id, "german" to german, "normalizedGerman" to normalizedGerman, "hint" to hint, "english" to english, "definition" to definition, "exampleGerman1" to exampleGerman1, "exampleEnglish1" to exampleEnglish1, "exampleGerman2" to exampleGerman2, "exampleEnglish2" to exampleEnglish2, "notes" to notes, "article" to article, "plural" to plural, "partOfSpeech" to partOfSpeech, "conjugation" to conjugation, "folderId" to folderId, "isMachineGenerated" to isMachineGenerated, "createdAt" to createdAt, "updatedAt" to updatedAt, "deletedAt" to deletedAt, "revision" to revision))
private fun SchedulingEntity.json() = JSONObject(mapOf("cardId" to cardId, "dueAt" to dueAt, "stabilityDays" to stabilityDays, "difficulty" to difficulty, "repetitions" to repetitions, "lapses" to lapses, "lastReviewedAt" to lastReviewedAt, "manualDueAt" to manualDueAt, "lastGradeToken" to lastGradeToken))
private fun ReviewLogEntity.json() = JSONObject(mapOf("id" to id, "cardId" to cardId, "grade" to grade, "reviewedAt" to reviewedAt, "previousDueAt" to previousDueAt, "nextDueAt" to nextDueAt, "elapsedDays" to elapsedDays, "intervalDays" to intervalDays, "undoOf" to undoOf))
private fun JSONObject.stringOrNull(key: String) = if (isNull(key)) null else getString(key)
private fun JSONObject.longOrNull(key: String) = if (isNull(key)) null else getLong(key)
private fun JSONObject.folder() = FolderEntity(getString("id"), getString("name"), stringOrNull("parentId"), getInt("position"), getLong("createdAt"), getLong("updatedAt"), longOrNull("deletedAt"), getLong("revision"))
private fun JSONObject.card() = CardEntity(getString("id"), getString("german"), getString("normalizedGerman"), stringOrNull("hint"), stringOrNull("english"), stringOrNull("definition"), stringOrNull("exampleGerman1"), stringOrNull("exampleEnglish1"), stringOrNull("exampleGerman2"), stringOrNull("exampleEnglish2"), stringOrNull("notes"), stringOrNull("article"), stringOrNull("plural"), stringOrNull("partOfSpeech"), stringOrNull("conjugation"), stringOrNull("folderId"), getBoolean("isMachineGenerated"), getLong("createdAt"), getLong("updatedAt"), longOrNull("deletedAt"), getLong("revision"))
private fun JSONObject.schedule() = SchedulingEntity(getString("cardId"), getLong("dueAt"), getDouble("stabilityDays"), getDouble("difficulty"), getInt("repetitions"), getInt("lapses"), longOrNull("lastReviewedAt"), longOrNull("manualDueAt"), stringOrNull("lastGradeToken"))
private fun JSONObject.log() = ReviewLogEntity(getString("id"), getString("cardId"), getInt("grade"), getLong("reviewedAt"), getLong("previousDueAt"), getLong("nextDueAt"), getDouble("elapsedDays"), getDouble("intervalDays"), stringOrNull("undoOf"))
