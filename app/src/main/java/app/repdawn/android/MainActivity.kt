package app.repdawn.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.repdawn.android.data.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); enableEdgeToEdge(); setContent { RepDawnApp() } }
}

private enum class Destination(val label: String) { HOME("Home"), STUDY("Study"), FOLDERS("Folders"), PRACTICE("Practice"), MIXTURE("Mixture"), SETTINGS("Settings"), ADD("Add card"), TRASH("Trash") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun RepDawnApp(vm: AppViewModel = viewModel(factory = AppViewModel.Factory(androidx.compose.ui.platform.LocalContext.current))) {
    var destination by rememberSaveable { mutableStateOf(Destination.HOME) }
    var editId by rememberSaveable { mutableStateOf<String?>(null) }
    val notice by vm.notice.collectAsState()
    MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF9B4010), secondary = Color(0xFF6E5B00), tertiary = Color(0xFF3D6655))) {
        Scaffold(topBar = { TopAppBar(title = { Text("RepDawn", fontWeight = FontWeight.Bold) }, actions = { TextButton(onClick = { destination = Destination.SETTINGS }) { Text("Settings") } }) }, bottomBar = { NavigationBar { listOf(Destination.HOME, Destination.STUDY, Destination.FOLDERS, Destination.PRACTICE, Destination.MIXTURE).forEach { d -> NavigationBarItem(selected = destination == d, onClick = { destination = d; if (d == Destination.STUDY) vm.loadStudy() }, icon = { Text(d.label.take(1)) }, label = { Text(d.label) }) } } }) { padding ->
            Box(Modifier.padding(padding).fillMaxSize()) {
                when (destination) {
                    Destination.HOME -> Home(vm, { editId = null; destination = Destination.ADD }, { destination = Destination.STUDY; vm.loadStudy() }, { destination = Destination.TRASH }, { editId = it; destination = Destination.ADD })
                    Destination.STUDY -> Study(vm)
                    Destination.FOLDERS -> Folders(vm, { destination = Destination.ADD })
                    Destination.PRACTICE -> Practice(vm)
                    Destination.MIXTURE -> Mixture(vm)
                    Destination.SETTINGS -> Settings(vm)
                    Destination.ADD -> Editor(vm, editId, onDone = { destination = Destination.HOME })
                    Destination.TRASH -> Trash(vm)
                }
                notice?.let { Snackbar(Modifier.align(Alignment.BottomCenter).padding(16.dp), action = { TextButton(onClick = vm::dismissNotice) { Text("OK") } }) { Text(it) } }
            }
        }
    }
}

@Composable private fun Home(vm: AppViewModel, add: () -> Unit, study: () -> Unit, trash: () -> Unit, edit: (String) -> Unit) {
    val due by vm.dueCount.collectAsState(); val cards by vm.cards.collectAsState()
    LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Learn German, locally first", style = MaterialTheme.typography.headlineSmall); Text("Your cards stay usable in airplane mode.") }
        item { ElevatedCard { Row(Modifier.padding(18.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Column { Text("Due cards", style = MaterialTheme.typography.labelLarge); Text("$due", style = MaterialTheme.typography.displaySmall) }; Button(onClick = study) { Text("Study now") } } } }
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { Button(onClick = add) { Text("Add card") }; OutlinedButton(onClick = trash) { Text("Trash") } } }
        item { Text("Recent cards", style = MaterialTheme.typography.titleMedium) }
        items(cards.take(8), key = { it.id }) { card -> ElevatedCard(Modifier.clickable { edit(card.id) }) { Column(Modifier.padding(14.dp)) { Text(card.german, style = MaterialTheme.typography.titleMedium); Text(card.english ?: "Draft — translation missing", color = if (card.complete) LocalContentColor.current else MaterialTheme.colorScheme.error) } } }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable private fun Study(vm: AppViewModel) {
    val card by vm.current.collectAsState(); val queue by vm.queue.collectAsState(); val index by vm.index.collectAsState()
    var face by remember(card?.id) { mutableIntStateOf(0) }; var extra by remember(card?.id) { mutableStateOf(false) }; var reverse by rememberSaveable { mutableStateOf(false) }; var custom by remember { mutableStateOf(10) }; var customUnit by remember { mutableStateOf("minutes") }
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(if (queue.isEmpty()) "Study session" else "Card ${index + 1} of ${queue.size}", style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(onClick = { reverse = false; face = 0; extra = false }, label = { Text("German → meaning") })
            AssistChip(onClick = { reverse = true; face = 0; extra = false }, label = { Text("Meaning → German") })
        }
        if (card == null) { Text("No due cards. You can still add cards and return when they are due."); Button(onClick = vm::loadStudy) { Text("Refresh") }; return@Column }
        ElevatedCard(Modifier.fillMaxWidth().weight(1f).combinedClickable(onClick = { extra = !extra }, onDoubleClick = { face = (face + 1) % 3; extra = false })) { Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            when (face) {
                0 -> { Text(if (reverse) card!!.english ?: card!!.definition ?: "Add a translation to practise this direction" else card!!.german, style = MaterialTheme.typography.headlineMedium); if (extra && !card!!.hint.isNullOrBlank()) Text(card!!.hint!!, style = MaterialTheme.typography.titleMedium); Text(if (reverse) "Think of the German word" else "Think of the meaning", style = MaterialTheme.typography.labelMedium) }
                1 -> { Text(if (reverse) card!!.german else card!!.english ?: "Draft: no translation yet", style = MaterialTheme.typography.headlineSmall); card!!.exampleGerman1?.let { Text(it, style = MaterialTheme.typography.titleMedium) }; card!!.exampleEnglish1?.let { Text(it) }; if (extra) Details(card!!) }
                else -> { Text(if (reverse) card!!.german else card!!.definition ?: "No definition yet", style = MaterialTheme.typography.titleLarge); card!!.exampleGerman2?.let { Text(it, style = MaterialTheme.typography.titleMedium) }; card!!.exampleEnglish2?.let { Text(it) }; if (extra) Details(card!!) }
            }
        } }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton(onClick = { extra = !extra }) { Text("Hint / details") }; OutlinedButton(onClick = { face = (face + 1) % 3 }) { Text(if (face == 0) "Reveal" else "Definition") }; OutlinedButton(onClick = vm::previous) { Text("Previous") }; OutlinedButton(onClick = vm::next) { Text("Next") } }
        if (face > 0) { Text("Schedule deliberately", style = MaterialTheme.typography.titleSmall); Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { Grade.entries.forEach { g -> Button(onClick = { vm.grade(g) }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(4.dp)) { Text("${g.name.lowercase().replaceFirstChar { it.uppercase() }}\n${vm.suggested(g)}") } } }; Row(verticalAlignment = Alignment.CenterVertically) { OutlinedTextField(value = custom.toString(), onValueChange = { custom = it.toIntOrNull() ?: 0 }, label = { Text("Custom") }, modifier = Modifier.width(100.dp), singleLine = true); Column { Row { listOf("minutes", "hours").forEach { unit -> AssistChip(onClick = { customUnit = unit }, label = { Text(unit.take(1)) }) } }; Row { listOf("days", "weeks").forEach { unit -> AssistChip(onClick = { customUnit = unit }, label = { Text(unit.take(1)) }) } } }; TextButton(onClick = { val factor = when(customUnit) { "hours" -> 60L; "days" -> 1440L; "weeks" -> 10080L; else -> 1L }; vm.custom(custom.toLong() * factor) }) { Text("Set") }; TextButton(onClick = vm::undo) { Text("Undo") } } }
    }
}
@Composable private fun Details(card: CardEntity) { listOfNotNull(card.article?.let { "Article: $it" }, card.plural?.let { "Plural: $it" }, card.partOfSpeech?.let { "Part of speech: $it" }, card.conjugation?.let { "Conjugation: $it" }, card.notes?.let { "Notes: $it" }).forEach { Text(it) } }


@Composable
private fun Folders(vm: AppViewModel, add: () -> Unit) {
    val folders by vm.folders.collectAsState()
    var name by remember { mutableStateOf("") }
    var parent by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("Folders", style = MaterialTheme.typography.headlineSmall)
            Text("Create nested folders or move cards out before deleting a folder.")
        }

        item {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("New folder") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AssistChip(
                    onClick = { parent = null },
                    label = { Text("Top level") }
                )

                folders.take(4).forEach { folder ->
                    AssistChip(
                        onClick = { parent = folder.id },
                        label = { Text("Inside ${folder.name}") }
                    )
                }

                Button(onClick = {
                    vm.addFolder(name, parent)
                    name = ""
                }) {
                    Text("Add")
                }
            }
        }

        items(folders, key = { it.id }) { folder ->
            var editing by remember(folder.id) {
                mutableStateOf(false)
            }
            var renamed by remember(folder.id) {
                mutableStateOf(folder.name)
            }

            ElevatedCard {
                if (editing) {
                    Row(Modifier.padding(10.dp)) {
                        OutlinedTextField(
                            value = renamed,
                            onValueChange = { renamed = it },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        TextButton(onClick = {
                            vm.renameFolder(folder.id, renamed)
                            editing = false
                        }) {
                            Text("Save")
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "${if (folder.parentId != null) "↳ " else ""}${folder.name}"
                        )

                        Row {
                            TextButton(onClick = {
                                editing = true
                            }) {
                                Text("Rename")
                            }

                            TextButton(onClick = {
                                vm.deleteFolderMoveCards(
                                    folder.id,
                                    folder.parentId
                                )
                            }) {
                                Text("Delete")
                            }
                        }
                    }
                }
            }
        }

        item {
            Button(onClick = add) {
                Text("Add a card")
            }
        }
    }
}
@Composable private fun Editor(vm: AppViewModel, editId: String?, onDone: () -> Unit) { val cards by vm.cards.collectAsState(); val existing = cards.firstOrNull { it.id == editId }; var draft by remember(editId, existing?.updatedAt) { mutableStateOf(existing?.toDraft() ?: CardDraft()) }; val folders by vm.folders.collectAsState(); LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { item { Text(if (editId == null) "New flashcard" else "Edit flashcard", style = MaterialTheme.typography.headlineSmall); Text("German is required. Incomplete cards are saved as drafts.") }; item { Field("German *", draft.german) { draft = draft.copy(german = it) } }; item { Field("Hint / synonym", draft.hint) { draft = draft.copy(hint = it) } }; item { Field("English translation", draft.english) { draft = draft.copy(english = it) } }; item { Field("English definition", draft.definition, multi = true) { draft = draft.copy(definition = it) } }; item { Field("Example 1 — German", draft.exampleGerman1, true) { draft = draft.copy(exampleGerman1 = it) } }; item { Field("Example 1 — English", draft.exampleEnglish1, true) { draft = draft.copy(exampleEnglish1 = it) } }; item { Field("Example 2 — German", draft.exampleGerman2, true) { draft = draft.copy(exampleGerman2 = it) } }; item { Field("Example 2 — English", draft.exampleEnglish2, true) { draft = draft.copy(exampleEnglish2 = it) } }; item { Field("Notes", draft.notes, true) { draft = draft.copy(notes = it) } }; item { Field("Article", draft.article) { draft = draft.copy(article = it) } }; item { Field("Plural / inflection", draft.plural) { draft = draft.copy(plural = it) } }; item { Field("Part of speech", draft.partOfSpeech) { draft = draft.copy(partOfSpeech = it) } }; item { Field("Conjugation", draft.conjugation) { draft = draft.copy(conjugation = it) } }; item { Text("Folder: ${folders.firstOrNull { it.id == draft.folderId }?.name ?: "None"}"); Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { folders.take(4).forEach { f -> AssistChip(onClick = { draft = draft.copy(folderId = f.id) }, label = { Text(f.name) }) } } }; item { Button(onClick = { vm.save(draft, editId, onDone) }, enabled = draft.german.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Save locally") } }; if(editId != null) item { OutlinedButton(onClick = { vm.trash(editId); onDone() }, modifier = Modifier.fillMaxWidth()) { Text("Move to Trash") } } } }
@Composable private fun Field(label: String, value: String, multi: Boolean = false, update: (String) -> Unit) = OutlinedTextField(value = value, onValueChange = update, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), minLines = if (multi) 2 else 1, maxLines = if (multi) 5 else 1)
@Composable private fun Practice(vm: AppViewModel) { val sentences by vm.sentences.collectAsState(); val idioms by vm.idioms.collectAsState(); LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { item { Text("Practice", style = MaterialTheme.typography.headlineSmall); Text("Cached sentence and idiom exercises work offline. Generation requires configured Wi‑Fi service.") }; item { Text("Sentences: ${sentences.size}", style = MaterialTheme.typography.titleMedium) }; items(sentences.take(5)) { Text(it.english) }; item { Text("Idioms: ${idioms.size}", style = MaterialTheme.typography.titleMedium) }; items(idioms.take(5)) { ElevatedCard { Column(Modifier.padding(12.dp)) { Text(it.german, style = MaterialTheme.typography.titleMedium); Text(it.naturalEnglish) } } } } }
@Composable private fun Mixture(vm: AppViewModel) { val words by vm.mixture.collectAsState(); var input by remember { mutableStateOf("") }; LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { item { Text("Mixture Box", style = MaterialTheme.typography.headlineSmall); Text("Independent queue. Copying to main folders will create independent cards after enrichment.") }; item { OutlinedTextField(value = input, onValueChange = { input = it }, label = { Text("One German word or phrase per line") }, minLines = 5, modifier = Modifier.fillMaxWidth()) }; item { Button(onClick = { vm.addMixture(input); input = "" }) { Text("Add to local queue") } }; items(words, key = { it.id }) { w -> ElevatedCard { Row(Modifier.padding(14.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(w.german); Text(w.status) } } } } }
@Composable private fun Trash(vm: AppViewModel) { val cards by vm.trash.collectAsState(); LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { item { Text("Trash", style = MaterialTheme.typography.headlineSmall); Text("Restoring keeps the review history. Permanent delete cannot be undone.") }; items(cards, key = { it.id }) { c -> ElevatedCard { Row(Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(c.german); Row { TextButton(onClick = { vm.restore(c.id) }) { Text("Restore") }; TextButton(onClick = { vm.permanent(c.id) }) { Text("Delete") } } } } } } }
@Composable private fun Settings(vm: AppViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { it?.let { vm.exportBackup(context.contentResolver, it) } }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let { vm.importBackup(context.contentResolver, it) } }
    val exportCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { it?.let { vm.exportCsv(context.contentResolver, it) } }
    val importCsv = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let { vm.importCsv(context.contentResolver, it) } }
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall)
        Text("RepDawn is local-first. Flashcards, study, and repetition scheduling work without Wi‑Fi or an account. No automatic cloud sync is configured in this build.")
        Text("Full backup (cards + review history)", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { export.launch("repdawn-backup.zip") }) { Text("Export ZIP") }
            OutlinedButton(onClick = { import.launch(arrayOf("application/zip", "application/octet-stream")) }) { Text("Import ZIP") }
        }
        Divider()
        Text("Laptop transfer (vocabulary CSV)", style = MaterialTheme.typography.titleMedium)
        Text("Export the CSV to edit vocabulary on your laptop. Import the edited CSV here to merge your changes. Existing cards keep their IDs and repetition schedules when possible.")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { exportCsv.launch("repdawn-vocabulary.csv") }) { Text("Export CSV") }
            OutlinedButton(onClick = { importCsv.launch(arrayOf("text/csv", "text/*", "application/vnd.ms-excel", "application/octet-stream", "*/*")) }) { Text("Import CSV") }
        }
        Text("For automatic online sync, a cloud backend must be configured separately. The app never needs internet for core study.", style = MaterialTheme.typography.bodySmall)
    }
}

private fun CardEntity.toDraft() = CardDraft(german, hint.orEmpty(), english.orEmpty(), definition.orEmpty(), exampleGerman1.orEmpty(), exampleEnglish1.orEmpty(), exampleGerman2.orEmpty(), exampleEnglish2.orEmpty(), notes.orEmpty(), article.orEmpty(), plural.orEmpty(), partOfSpeech.orEmpty(), conjugation.orEmpty(), folderId)
