package io.github.holsoma.fairenough.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.holsoma.fairenough.AppModel
import io.github.holsoma.fairenough.core.*

@Composable fun ImportScreen(model: AppModel, onOpenFile: () -> Unit, onDone: () -> Unit) {
    val context = LocalContext.current
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val review = model.importReview
    var selection by remember(review) { mutableStateOf(review?.rows?.filter { it.trip != null && it.matchesExisting == 0 }?.map { it.index }?.toSet().orEmpty()) }
    var confirmation by remember { mutableStateOf(false) }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { PageTitle("Bring your records", "Use any LLM to clarify your notes and produce the import format. Your records are only shared when you choose to paste them elsewhere.") }
        item { OutlinedButton(onClick = { clipboard.setPrimaryClip(ClipData.newPlainText("farenough formatting prompt", formattingPrompt(model.stations))); model.message = "Formatting prompt copied." }, modifier = Modifier.fillMaxWidth()) { Text("Copy formatting prompt") } }
        if (review == null) {
            item { Notice("Give the prompt and your old notes to an LLM. Confirm abbreviations and dates, then paste its JSON here. Each endpoint should use one station code, such as BP6 for Bukit Panjang.") }
            item { OutlinedTextField(model.importText, { model.importText = it }, Modifier.fillMaxWidth().heightIn(min = 200.dp, max = 320.dp), label = { Text("Normalised JSON") }, placeholder = { Text("{\"format\":\"farenough-trips\", ...}") }) }
            item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { TextButton(onClick = { model.importText = clipboard.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty() }) { Text("Paste") }; TextButton(onClick = onOpenFile) { Text("Open JSON file") } } }
            item { Button(onClick = { model.review(model.importText) }, enabled = model.importText.isNotBlank() && !model.working, modifier = Modifier.fillMaxWidth()) { Text("Review journeys") } }
        } else {
            item { Notice("${review.rows.size} rows · ${review.validCount} valid · ${selection.size} selected. Nothing is saved until you confirm.") }
            val unpriced = review.rows.count { row -> row.trip?.let { it.effectiveFare == null } == true }
            if (unpriced > 0) item { Notice("$unpriced valid ${if (unpriced == 1) "journey has" else "journeys have"} no fare. Automatic coverage: ${readableDate(model.fareData.validFrom)} to ${readableDate(model.fareData.validUntil)}. Keep the correct travel dates; you can import these journeys and add checked fares for their period.") }
            if (review.alreadyImported) item { Notice("This complete batch has already been imported. It will not be added again.") }
            if (review.rows.any { it.errors.isNotEmpty() }) item { Notice("Rows with errors cannot be imported. Edit the JSON or return to the LLM to correct them. You can still explicitly import the valid selection.") }
            if (review.rows.any { it.matchesExisting > 0 }) item { Notice("Potential duplicates start unselected. Select one only if it is another journey you actually took. Repeated journeys within this file are preserved.") }
            items(review.rows, key = { it.index }) { row ->
                Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainer) {
                    Row(Modifier.fillMaxWidth().clickable(enabled = row.trip != null && !review.alreadyImported) { selection = if (row.index in selection) selection - row.index else selection + row.index }.padding(12.dp)) {
                        Checkbox(checked = row.index in selection, enabled = row.trip != null && !review.alreadyImported, onCheckedChange = { checked -> selection = if (checked) selection + row.index else selection - row.index })
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text("${row.index + 1}. ${row.input.date}", style = MaterialTheme.typography.labelLarge)
                            Text("${row.input.origin} → ${row.input.destination}")
                            row.trip?.let { Text(it.effectiveFare?.let(::money) ?: "Fare needed", color = MaterialTheme.colorScheme.primary) }
                            row.errors.forEach { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                            if (row.matchesExisting > 0) Text("${row.matchesExisting} matching saved journeys", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                            if (row.input.sourceText.isNotBlank()) Text(row.input.sourceText, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            item { Button(onClick = { confirmation = true }, enabled = selection.isNotEmpty() && !review.alreadyImported && !model.working, modifier = Modifier.fillMaxWidth()) { Text("Import ${selection.size} journeys") } }
            item { TextButton(onClick = { model.importReview = null }, modifier = Modifier.fillMaxWidth()) { Text("Edit JSON") } }
        }
    }
    if (confirmation) Confirm("Import ${selection.size} journeys?", "The selected records will be saved together. Unpriced journeys will remain visible until you add their fares. You can undo this batch from More.", "Import", { confirmation = false }) { confirmation = false; model.import(selection, onDone) }
}
