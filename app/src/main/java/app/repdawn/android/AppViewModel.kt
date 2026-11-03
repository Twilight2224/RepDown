package app.repdawn.android

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.repdawn.android.data.*
import app.repdawn.android.domain.Scheduler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AppViewModel(context: Context) : ViewModel() {
    private val db = RepDawnDatabase.open(context.applicationContext)
    private val repo = RepDawnRepository(db)
    private val backup = BackupManager(db)
    private val csv = CsvBridge(db)
    val cards = repo.cards().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val folders = repo.folders().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val dueCount = repo.dueCount().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    val trash = repo.trash().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val mixture = repo.mixture().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val sentences = repo.sentences().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val idioms = repo.idioms().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val _queue = MutableStateFlow<List<CardEntity>>(emptyList())
    val queue = _queue.asStateFlow()
    private val _index = MutableStateFlow(0)
    val index = _index.asStateFlow()
    private val _currentSchedule = MutableStateFlow<SchedulingEntity?>(null)
    private val _notice = MutableStateFlow<String?>(null)
    val notice = _notice.asStateFlow()
    val current = combine(_queue, _index) { q, i -> q.getOrNull(i) }.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    fun dismissNotice() { _notice.value = null }
    fun loadStudy() = viewModelScope.launch { _queue.value = repo.due(); _index.value = 0; _currentSchedule.value = _queue.value.firstOrNull()?.let { repo.schedule(it.id) }; if (_queue.value.isEmpty()) _notice.value = "No cards are due. Add a card or come back later." }
    fun next() { if (_index.value + 1 < _queue.value.size) viewModelScope.launch { _index.value++; _currentSchedule.value = repo.schedule(_queue.value[_index.value].id) } }
    fun previous() { if (_index.value > 0) viewModelScope.launch { _index.value--; _currentSchedule.value = repo.schedule(_queue.value[_index.value].id) } }
    fun save(draft: CardDraft, id: String? = null, done: (() -> Unit)? = null) = viewModelScope.launch { runCatching { repo.save(draft, id) }.onSuccess { _notice.value = "Card saved locally."; done?.invoke() }.onFailure { _notice.value = it.message ?: "Could not save card." } }
    fun grade(grade: Grade) = viewModelScope.launch { val card = current.value ?: return@launch; runCatching { repo.grade(card.id, grade) }.onSuccess { _notice.value = "Scheduled for ${relative(it.dueAt)}."; loadStudy() }.onFailure { _notice.value = it.message ?: "Review was not saved." } }
    fun custom(minutes: Long) = viewModelScope.launch { val card = current.value ?: return@launch; if (minutes < 1) { _notice.value = "Custom interval must be at least one minute."; return@launch }; repo.customGrade(card.id, minutes * 60_000); loadStudy() }
    fun undo() = viewModelScope.launch { val card = current.value ?: return@launch; _notice.value = if (repo.undoLast(card.id)) "Last review undone." else "Nothing to undo." }
    fun trash(id: String) = viewModelScope.launch { repo.trash(listOf(id)); _notice.value = "Moved to Trash." }
    fun restore(id: String) = viewModelScope.launch { repo.restore(listOf(id)); _notice.value = "Restored." }
    fun permanent(id: String) = viewModelScope.launch { repo.permanentDelete(id); _notice.value = "Permanently deleted." }
    fun addFolder(name: String, parentId: String? = null) = viewModelScope.launch { runCatching { repo.folder(name, parentId) }.onSuccess { _notice.value = "Folder created." }.onFailure { _notice.value = it.message } }
    fun renameFolder(id: String, name: String) = viewModelScope.launch { runCatching { repo.renameFolder(id, name) }.onSuccess { _notice.value = "Folder renamed." }.onFailure { _notice.value = it.message } }
    fun deleteFolderMoveCards(id: String, destination: String? = null) = viewModelScope.launch { repo.deleteFolderMoveCards(id, destination); _notice.value = "Folder sent to Trash; its cards were moved safely." }
    fun addMixture(text: String) = viewModelScope.launch { repo.queueMixture(text.lines()); _notice.value = "Words added to Mixture Box." }
    fun exportBackup(resolver: android.content.ContentResolver, uri: android.net.Uri) = viewModelScope.launch { runCatching { backup.export(resolver, uri) }.onSuccess { _notice.value = "Backup exported." }.onFailure { _notice.value = "Backup failed: ${it.message}" } }
    fun importBackup(resolver: android.content.ContentResolver, uri: android.net.Uri) = viewModelScope.launch { runCatching { backup.importMerge(resolver, uri) }.onSuccess { _notice.value = "Backup merged safely." }.onFailure { _notice.value = "Import rejected: ${it.message}" } }
    fun exportCsv(resolver: android.content.ContentResolver, uri: android.net.Uri) = viewModelScope.launch { runCatching { csv.export(resolver, uri) }.onSuccess { _notice.value = "CSV exported. Open it on your laptop to edit cards." }.onFailure { _notice.value = "CSV export failed: ${it.message}" } }
    fun importCsv(resolver: android.content.ContentResolver, uri: android.net.Uri) = viewModelScope.launch { runCatching { csv.import(resolver, uri) }.onSuccess { _notice.value = "CSV imported and merged." }.onFailure { _notice.value = "CSV import rejected: ${it.message}" } }
    fun duplicate(id: String) = viewModelScope.launch { repo.duplicate(id); _notice.value = "Independent copy created." }
    fun suggested(grade: Grade): String = _currentSchedule.value?.let { relative(Scheduler.estimate(it, grade)) } ?: "—"
    private fun relative(due: Long): String { val diff = due - System.currentTimeMillis(); return when { diff < 3_600_000 -> "${(diff / 60_000).coerceAtLeast(1)} min"; diff < 86_400_000 -> "${diff / 3_600_000} h"; else -> "${diff / 86_400_000} d" } }
    class Factory(private val context: Context) : ViewModelProvider.Factory { @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = AppViewModel(context) as T }
}
