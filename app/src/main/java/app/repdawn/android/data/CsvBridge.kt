package app.repdawn.android.data

import android.content.ContentResolver
import android.net.Uri
import androidx.room.withTransaction
import java.text.Normalizer
import java.util.Locale

/** CSV interchange bridge for editing vocabulary on a laptop without a cloud service. */
class CsvBridge(private val db: RepDawnDatabase) {
    private val dao = db.dao()
    private val columns = listOf(
        "id", "german", "english", "definition", "hint", "exampleGerman", "exampleEnglish",
        "dueAt", "intervalDays", "repetitions", "lapses", "lastReviewedAt", "createdAt", "updatedAt", "folderName"
    )

    suspend fun export(resolver: ContentResolver, uri: Uri) {
        val folders = dao.backupFolders().associateBy { it.id }
        val schedules = dao.backupSchedules().associateBy { it.cardId }
        val rows = dao.backupCards().filter { it.deletedAt == null }.map { card ->
            val schedule = schedules[card.id]
            listOf(
                card.id, card.german, card.english.orEmpty(), card.definition.orEmpty(), card.hint.orEmpty(),
                card.exampleGerman1.orEmpty(), card.exampleEnglish1.orEmpty(), schedule?.dueAt?.toString().orEmpty(),
                schedule?.stabilityDays?.toString().orEmpty(), schedule?.repetitions?.toString().orEmpty(),
                schedule?.lapses?.toString().orEmpty(), schedule?.lastReviewedAt?.toString().orEmpty(),
                card.createdAt.toString(), card.updatedAt.toString(), folders[card.folderId]?.name.orEmpty()
            )
        }
        val csv = buildString {
            append(columns.joinToString(",") { it.csv() }).append("\r\n")
            rows.forEach { row -> append(row.joinToString(",") { it.csv() }).append("\r\n") }
        }
        resolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { it.write("\uFEFF"); it.write(csv) }
            ?: error("The selected file could not be opened.")
    }

    suspend fun importCsv(resolver: ContentResolver, uri: Uri) {
        val raw = resolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            ?: error("The selected file could not be opened.")
        val rows = parseCsv(raw.removePrefix("\uFEFF"))
        require(rows.size >= 2) { "The CSV contains no card rows." }
        val headers = rows.first().map { it.trim().removePrefix("\uFEFF").lowercase(Locale.ROOT) }
        require("german" in headers) { "CSV needs a 'german' column." }
        val hasSchedule = "dueat" in headers
        val now = System.currentTimeMillis()
        val parsed = rows.drop(1).filter { row -> row.any { cell -> cell.isNotBlank() } }.map { row ->
            fun get(key: String): String = headers.indexOf(key).takeIf { it >= 0 }?.let { row.getOrNull(it).orEmpty().trim() }.orEmpty()
            fun nullable(key: String): String? = get(key).ifBlank { null }
            fun longOrNull(key: String): Long? = get(key).toLongOrNull()
            fun doubleOrNull(key: String): Double? = get(key).toDoubleOrNull()
            val german = get("german")
            require(german.isNotBlank()) { "A card has an empty German field." }
            ImportedCard(
                id = get("id").ifBlank { newId() }, german = german, english = nullable("english"),
                definition = nullable("definition"), hint = nullable("hint"), exampleGerman = nullable("examplegerman") ?: nullable("examplegerman1"),
                exampleEnglish = nullable("exampleenglish") ?: nullable("exampleenglish1"), article = nullable("article"),
                plural = nullable("plural"), partOfSpeech = nullable("partofspeech"), conjugation = nullable("conjugation"),
                notes = nullable("notes"), folderName = nullable("foldername"), dueAt = longOrNull("dueat"),
                stabilityDays = doubleOrNull("intervaldays") ?: doubleOrNull("stabilitydays"),
                repetitions = longOrNull("repetitions")?.toInt(), lapses = longOrNull("lapses")?.toInt(),
                lastReviewedAt = longOrNull("lastreviewedat"), createdAt = longOrNull("createdat")
            )
        }
        db.withTransaction {
            val foldersByName = dao.backupFolders().filter { it.deletedAt == null }.associateBy { it.name.trim().lowercase(Locale.ROOT) }.toMutableMap()
            parsed.forEach { item ->
                val existingById = dao.card(item.id)
                val existing = existingById ?: dao.backupCards().firstOrNull { it.normalizedGerman == normalize(item.german) && it.deletedAt == null }
                val targetId = existing?.id ?: item.id
                val folderId = item.folderName?.takeIf { it.isNotBlank() }?.let { name ->
                    val key = name.trim().lowercase(Locale.ROOT)
                    foldersByName[key]?.id ?: FolderEntity(name = name.trim()).also { dao.upsertFolder(it); foldersByName[key] = it }.id
                } ?: existing?.folderId
                val card = CardEntity(
                    id = targetId, german = item.german, normalizedGerman = normalize(item.german), hint = item.hint,
                    english = item.english, definition = item.definition, exampleGerman1 = item.exampleGerman,
                    exampleEnglish1 = item.exampleEnglish, notes = item.notes, article = item.article, plural = item.plural,
                    partOfSpeech = item.partOfSpeech, conjugation = item.conjugation, folderId = folderId,
                    createdAt = existing?.createdAt ?: item.createdAt ?: now, updatedAt = now,
                    deletedAt = null, revision = (existing?.revision ?: 0) + 1
                )
                dao.upsertCard(card)
                val oldSchedule = dao.schedule(targetId)
                if (oldSchedule == null) {
                    dao.upsertSchedule(SchedulingEntity(cardId = targetId, dueAt = item.dueAt ?: now,
                        stabilityDays = item.stabilityDays ?: 0.0, repetitions = item.repetitions ?: 0,
                        lapses = item.lapses ?: 0, lastReviewedAt = item.lastReviewedAt))
                } else if (hasSchedule && item.dueAt != null) {
                    dao.upsertSchedule(oldSchedule.copy(dueAt = item.dueAt, stabilityDays = item.stabilityDays ?: oldSchedule.stabilityDays,
                        repetitions = item.repetitions ?: oldSchedule.repetitions, lapses = item.lapses ?: oldSchedule.lapses,
                        lastReviewedAt = item.lastReviewedAt ?: oldSchedule.lastReviewedAt))
                }
            }
        }
    }

    private fun normalize(value: String) = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
        .replace("\\p{M}".toRegex(), "").lowercase(Locale.ROOT)

    private data class ImportedCard(
        val id: String, val german: String, val english: String?, val definition: String?, val hint: String?,
        val exampleGerman: String?, val exampleEnglish: String?, val article: String?, val plural: String?,
        val partOfSpeech: String?, val conjugation: String?, val notes: String?, val folderName: String?,
        val dueAt: Long?, val stabilityDays: Double?, val repetitions: Int?, val lapses: Int?,
        val lastReviewedAt: Long?, val createdAt: Long?
    )

    private fun String.csv(): String = if (any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"${replace("\"", "\"\"")}\"" else this

    private fun parseCsv(text: String): List<List<String>> {
        val result = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var quoted = false
        var index = 0
        while (index < text.length) {
            val char = text[index]
            if (quoted) {
                if (char == '"' && text.getOrNull(index + 1) == '"') { field.append('"'); index++ }
                else if (char == '"') quoted = false
                else field.append(char)
            } else when (char) {
                '"' -> quoted = true
                ',' -> { row.add(field.toString()); field.setLength(0) }
                '\n' -> { row.add(field.toString().removeSuffix("\r")); field.setLength(0); result.add(row); row = mutableListOf() }
                else -> field.append(char)
            }
            index++
        }
        if (field.isNotEmpty() || row.isNotEmpty()) { row.add(field.toString().removeSuffix("\r")); result.add(row) }
        return result
    }
}
