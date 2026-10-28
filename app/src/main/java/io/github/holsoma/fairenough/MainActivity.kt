package io.github.holsoma.fairenough

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.holsoma.fairenough.core.*
import io.github.holsoma.fairenough.ui.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.io.ByteArrayOutputStream

class MainActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); enableEdgeToEdge(); setContent { FairenoughApp() } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun FairenoughApp(model: AppModel = viewModel()) {
    val snapshot by model.state.collectAsStateWithLifecycle()
    val selectedPass by model.selectedPass.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var page by rememberSaveable { mutableStateOf("overview") }
    var tab by rememberSaveable { mutableStateOf("overview") }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var presetOrigin by rememberSaveable { mutableStateOf("") }
    var presetDestination by rememberSaveable { mutableStateOf("") }
    var draftFare by remember { mutableStateOf<RouteFare?>(null) }
    var exportAction by rememberSaveable { mutableStateOf("backup") }
    var passMenu by remember { mutableStateOf(false) }
    val pageStates = rememberSaveableStateHolder()
    val s = snapshot ?: Snapshot()
    val pass = s.passes.find { it.id == selectedPass } ?: s.passes.find { it.contains(singaporeToday().toString()) } ?: s.passes.maxByOrNull { it.start }
    fun goTrip(trip: Trip? = null, from: String = "", to: String = "") { pageStates.removeState("trip"); editingId = trip?.id; presetOrigin = from; presetDestination = to; page = "trip" }
    fun goPass(p: PassPeriod?) { pageStates.removeState("pass"); editingId = p?.id; page = "pass" }
    fun goFare(fare: RouteFare?) { pageStates.removeState("fare"); draftFare = fare; page = "fare" }
    fun finish() { page = tab }
    fun readFile(uri: Uri, limit: Int): String {
        return context.contentResolver.openInputStream(uri)?.use { stream ->
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) { val n = stream.read(buffer); if (n < 0) break; require(out.size() + n <= limit) { "This file is too large." }; out.write(buffer, 0, n) }
            out.toString("UTF-8")
        } ?: error("Cannot open this file.")
    }
    val createJson = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) { if (exportAction == "before-restore") model.message = "Restore cancelled. Your data has not changed." }
        else model.run {
            val before = model.repository.current()
            val data = encodeBackup(before, Instant.now().toString())
            context.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use { it.write(data) } ?: error("Cannot write the backup. Your data has not changed.")
            if (exportAction == "before-restore") {
                val restored = model.restoreData ?: error("Choose the backup again.")
                model.repository.update { restored }
                withContext(Dispatchers.Main) { model.restoreData = null; model.message = "Backup restored. A copy of the previous records was exported." }
            } else withContext(Dispatchers.Main) { model.message = "Full backup saved." }
        }
    }
    val createCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) model.run {
            val data = exportCsv(model.repository.current().trips, model.stations)
            context.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use { it.write(data) } ?: error("Cannot write this file.")
            withContext(Dispatchers.Main) { model.message = "CSV exported." }
        }
    }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) model.run {
            val data = decodeBackup(readFile(uri, 20_000_000), model.stations)
            withContext(Dispatchers.Main) { model.restoreData = data }
        }
    }
    val importFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) model.run {
            val text = readFile(uri, 2_000_000)
            withContext(Dispatchers.Main) { model.importText = text; model.importReview = null }
        }
    }
    BackHandler(page != tab) { finish() }
    FairenoughTheme(s.preferences.appearance) {
        if (snapshot == null) { Surface(Modifier.fillMaxSize()) { Box(contentAlignment = Alignment.Center) { CircularProgressIndicator() } }; return@FairenoughTheme }
        Scaffold(
            topBar = {
                TopAppBar(title = { Text("fairenough") }, navigationIcon = { if (page != tab) IconButton(onClick = ::finish) { Icon(Icons.Outlined.ArrowBack, "Back") } }, actions = {
                    if (page == tab) {
                        if (tab != "more") IconButton(onClick = { goTrip() }) { Icon(Icons.Outlined.Add, "Record a journey") }
                        Box {
                            IconButton(onClick = { passMenu = true }) { Icon(Icons.Outlined.ConfirmationNumber, "Choose pass period") }
                            DropdownMenu(expanded = passMenu, onDismissRequest = { passMenu = false }) {
                                s.passes.sortedByDescending { it.start }.forEach { p -> DropdownMenuItem(text = { Text(periodLabel(p)) }, onClick = { model.selectPass(p.id); passMenu = false }) }
                                DropdownMenuItem(text = { Text("Add a pass") }, onClick = { passMenu = false; goPass(null) })
                            }
                        }
                    }
                })
            },
            bottomBar = { if (page == tab) NavigationBar {
                listOf(Triple("overview", "Overview", Icons.Outlined.Home), Triple("trips", "Trips", Icons.Outlined.ReceiptLong), Triple("insights", "Insights", Icons.Outlined.BarChart), Triple("more", "More", Icons.Outlined.MoreHoriz)).forEach { (key, label, icon) ->
                    NavigationBarItem(selected = tab == key, onClick = { tab = key; page = key }, icon = { Icon(icon, null) }, label = { Text(label) })
                }
            } },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                pageStates.SaveableStateProvider(page) { when (page) {
                    "overview" -> Overview(model, s, pass, { goPass(null) }, { page = "target" }, { page = "import" }, { page = "fares" }, { from, to -> goTrip(from = from, to = to) })
                    "trips" -> TripList(model, s, pass, { goTrip(it) }, { page = "import" })
                    "insights" -> Insights(model, s, pass) { goTrip(it) }
                    "more" -> More(model, s, ::goPass, { page = "import" }, ::goFare, { page = "fares" }, { exportAction = "backup"; createJson.launch("fairenough-backup-${singaporeToday()}.json") }, { createCsv.launch("fairenough-journeys-${singaporeToday()}.csv") }, { restore.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) })
                    "pass" -> PassForm(model, s.passes.find { it.id == editingId }, ::finish)
                    "trip" -> TripForm(model, s, s.trips.find { it.id == editingId }, presetOrigin, presetDestination, ::finish)
                    "fare" -> FareForm(model, s, draftFare) { page = "fares" }
                    "fares" -> FareList(model, s, ::goFare)
                    "target" -> Target(model, s, pass)
                    "import" -> ImportScreen(model, { importFile.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }) { tab = "trips"; page = "trips" }
                } }
                if (model.working) LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter))
            }
        }
        model.message?.let { message -> AlertDialog(onDismissRequest = { model.message = null }, title = { Text("fairenough") }, text = { Text(message) }, confirmButton = { TextButton(onClick = { model.message = null }) { Text("OK") } }) }
        model.restoreData?.let { data ->
            AlertDialog(onDismissRequest = { model.restoreData = null }, title = { Text("Restore this backup?") }, text = { Text("Replace your records with ${data.trips.size} journeys, ${data.passes.size} passes and ${data.routeFares.size} saved fares. First choose where to export your current records. Replacement only starts after that export succeeds.") }, confirmButton = {
                TextButton(onClick = { exportAction = "before-restore"; createJson.launch("fairenough-before-restore-${singaporeToday()}.json") }, enabled = !model.working) { Text("Export current data and restore") }
            }, dismissButton = { TextButton(onClick = { model.restoreData = null }) { Text("Cancel") } })
        }
    }
}
