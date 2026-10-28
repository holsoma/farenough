package io.github.holsoma.fairenough.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.holsoma.fairenough.AppModel
import io.github.holsoma.fairenough.BuildConfig
import io.github.holsoma.fairenough.core.*

@Composable fun ExternalLink(label: String, url: String) {
    val context = LocalContext.current
    TextButton(onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }.onFailure { Toast.makeText(context, "No browser is available to open this link.", Toast.LENGTH_LONG).show() } }) { Text(label) }
}

@Composable fun More(model: AppModel, snapshot: Snapshot, onPass: (PassPeriod?) -> Unit, onImport: () -> Unit, onFare: (RouteFare?) -> Unit, onFares: () -> Unit, onBackup: () -> Unit, onCsv: () -> Unit, onRestore: () -> Unit) {
    var undo by remember { mutableStateOf<ImportBatch?>(null) }
    LazyColumn(contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 40.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { PageTitle("Make it yours", "Local records. Clear calculations. No account.") }
        item { Text("Passes", style = MaterialTheme.typography.titleLarge) }
        items(snapshot.passes.sortedByDescending { it.start }) { pass -> ListItem(headlineContent = { Text(periodLabel(pass)) }, supportingContent = { Text("${pass.name} · ${money(pass.priceCents)}") }, modifier = Modifier.clickable { onPass(pass) }) }
        item { OutlinedButton(onClick = { onPass(null) }, modifier = Modifier.fillMaxWidth()) { Text("Add a pass period") } }
        item { HorizontalDivider(); Text("Your data", Modifier.padding(top = 18.dp), style = MaterialTheme.typography.titleLarge) }
        item {
            Column(Modifier.fillMaxWidth()) {
                SettingsRow("Import journeys", "Copy the prompt and review normalised records", onImport)
                SettingsRow("Saved route fares", "${snapshot.routeFares.size} checked fares; used only for their valid dates", onFares)
                SettingsRow("Create full backup", "Passes, journeys, fares, imports and preferences", onBackup)
                SettingsRow("Export journeys as CSV", "A spreadsheet-friendly copy, not a full backup", onCsv)
                SettingsRow("Restore a backup", "Review first, then export current data before replacement", onRestore)
            }
        }
        if (snapshot.batches.isNotEmpty()) {
            item { Text("Recent imports", style = MaterialTheme.typography.titleLarge) }
            items(snapshot.batches.sortedByDescending { it.createdAt }.take(5)) { batch ->
                val trips = snapshot.trips.filter { it.importBatchId == batch.id }
                ListItem(headlineContent = { Text("${trips.size} journeys") }, supportingContent = { Text(batch.createdAt.take(10)) }, trailingContent = { TextButton(onClick = { undo = batch }) { Text("Undo") } })
            }
        }
        item { HorizontalDivider(); Text("Appearance", Modifier.padding(top = 18.dp), style = MaterialTheme.typography.titleLarge) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("system", "light", "dark").forEach { value -> FilterChip(snapshot.preferences.appearance == value, { model.appearance(value) }, label = { Text(value.replaceFirstChar { it.uppercase() }) }) } } }
        item { HorizontalDivider(); Text("About farenough", Modifier.padding(top = 18.dp), style = MaterialTheme.typography.titleLarge); Text("Version ${BuildConfig.VERSION_NAME}", Modifier.padding(top = 8.dp)) }
        item { Notice("The comparison uses regular adult card fares and any corrections you make. Separate records are separate journeys. Automatic transfer and time-based discounts are not applied. Saved route fares are the current pricing source; no complete official fare matrix is bundled.") }
        item { Text("Station positions: LTA DataMall, September 2026. Coastline: SLA National Map Polygon. Adapted under the Singapore Open Data Licence. CCL6 names and code updates are documented in the source manifest.", style = MaterialTheme.typography.bodySmall) }
        item {
            Column {
                ExternalLink("Read the guide", "https://holsoma.github.io/farenough/docs/")
                ExternalLink("Source and releases", "https://github.com/holsoma/farenough")
                ExternalLink("Singapore Open Data Licence", "https://datamall.lta.gov.sg/content/datamall/en/SingaporeOpenDataLicence.html")
                ExternalLink("Check fares with LTA", "https://www.lta.gov.sg/content/ltagov/en/map/fare-calculator.html")
            }
        }
        item { Text("Apache 2.0 code licence. Independent software, not affiliated with LTA or SimplyGo. Backups are unencrypted files in the location you choose. No automatic uploads or analytics.", style = MaterialTheme.typography.bodySmall) }
    }
    undo?.let { batch ->
        val trips = snapshot.trips.filter { it.importBatchId == batch.id }
        Confirm("Undo this import?", "Remove ${trips.size} journeys created by this import, including ${trips.count { it.edited }} subsequently edited journeys. Other journeys remain.", "Remove batch", { undo = null }) { model.undoBatch(batch.id); undo = null }
    }
}

@Composable private fun SettingsRow(title: String, subtitle: String, onClick: () -> Unit) = ListItem(headlineContent = { Text(title) }, supportingContent = { Text(subtitle) }, modifier = Modifier.clickable(onClick = onClick))

@Composable fun FareList(model: AppModel, snapshot: Snapshot, onEdit: (RouteFare?) -> Unit) {
    val missing = routeStatistics(snapshot.trips.filter { it.fareCents == null && it.correctionCents == null })
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { PageTitle("Saved route fares", "Check a regular adult fare once, then reuse it for that route and fare period.") }
        item { Button(onClick = { onEdit(null) }, modifier = Modifier.fillMaxWidth()) { Text("Add a checked fare") } }
        if (missing.isNotEmpty()) item { Text("Routes needing a fare", style = MaterialTheme.typography.titleLarge) }
        items(missing, key = { "missing-${it.origin}-${it.destination}" }) { route ->
            ListItem(headlineContent = { Text("${model.name(route.origin)} → ${model.name(route.destination)}") }, supportingContent = { Text("${route.count} unpriced trips") }, modifier = Modifier.clickable { onEdit(RouteFare(origin = route.origin, destination = route.destination, start = "2025-12-27", end = "2026-12-25", cents = 0)) })
        }
        item { Text("Checked fares", style = MaterialTheme.typography.titleLarge) }
        if (snapshot.routeFares.isEmpty()) item { Notice("No fares saved yet. Missing fares never count as zero in your pass comparison.") }
        items(snapshot.routeFares.sortedWith(compareBy<RouteFare> { model.name(it.origin) }.thenBy { it.start })) { fare ->
            ListItem(headlineContent = { Text("${model.name(fare.origin)} → ${model.name(fare.destination)}") }, supportingContent = { Text("${readableDate(fare.start)} to ${readableDate(fare.end)}") }, trailingContent = { Text(money(fare.cents)) }, modifier = Modifier.clickable { onEdit(fare) })
        }
    }
}
