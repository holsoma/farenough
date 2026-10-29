package io.github.holsoma.fairenough.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.holsoma.fairenough.core.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

fun readableDate(date: String): String = runCatching { LocalDate.parse(date).format(DateTimeFormatter.ofPattern("d MMM yyyy", java.util.Locale.UK)) }.getOrDefault(date)
fun periodLabel(pass: PassPeriod): String = "${readableDate(pass.start)} to ${readableDate(pass.end)}"

@Composable fun PageTitle(title: String, description: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
        description?.let { Text(it, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable fun Notice(text: String, modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.medium) {
        Text(text, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable fun Metric(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(value, fontSize = 27.sp, fontWeight = FontWeight.SemiBold)
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun DateField(label: String, value: String, onChange: (String) -> Unit) {
    var showing by remember { mutableStateOf(false) }
    OutlinedTextField(value, onChange, Modifier.fillMaxWidth(), label = { Text(label) }, singleLine = true,
        supportingText = { Text("YYYY-MM-DD") }, trailingIcon = { IconButton(onClick = { showing = true }) { Icon(Icons.Outlined.CalendarToday, "Choose $label") } })
    if (showing) {
        val dateState = rememberDatePickerState(initialSelectedDateMillis = runCatching { LocalDate.parse(value).toEpochDay() * 86_400_000 }.getOrNull())
        DatePickerDialog(onDismissRequest = { showing = false }, confirmButton = {
            TextButton(onClick = { dateState.selectedDateMillis?.let { onChange(LocalDate.ofEpochDay(it / 86_400_000).toString()) }; showing = false }) { Text("Choose date") }
        }, dismissButton = { TextButton(onClick = { showing = false }) { Text("Cancel") } }) { DatePicker(state = dateState) }
    }
}

@Composable fun StationField(label: String, value: String, stations: List<Station>, onChange: (String) -> Unit) {
    var showing by remember { mutableStateOf(false) }
    val station = stations.find { it.id == value }
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    OutlinedButton(onClick = { focus.clearFocus(); keyboard?.hide(); showing = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp), shape = MaterialTheme.shapes.small, contentPadding = PaddingValues(16.dp)) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(station?.name ?: "Choose a station", style = MaterialTheme.typography.bodyLarge)
            station?.let { Text(it.codes.joinToString(" / "), style = MaterialTheme.typography.labelSmall) }
        }
    }
    if (showing) {
        var search by rememberSaveable { mutableStateOf("") }
        AlertDialog(onDismissRequest = { showing = false }, title = { Text(label) }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(search, { search = it }, Modifier.fillMaxWidth(), label = { Text("Station name or code") }, singleLine = true)
                val filtered = stations.filter { it.name.contains(search.trim(), true) || it.codes.any { c -> c.contains(search.trim(), true) } }
                LazyColumn(Modifier.heightIn(max = 380.dp)) {
                    if (filtered.isEmpty()) item { Text("No matching stations.", Modifier.padding(16.dp)) }
                    items(filtered, key = { it.id }) { s ->
                        ListItem(headlineContent = { Text(s.name) }, supportingContent = { Text(s.codes.joinToString(" / ")) }, modifier = Modifier.clickable { keyboard?.hide(); onChange(s.id); showing = false })
                    }
                }
            }
        }, confirmButton = { TextButton(onClick = { showing = false }) { Text("Close") } })
    }
}

@Composable fun Confirm(title: String, text: String, button: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { Text(text) }, confirmButton = { TextButton(onClick = onConfirm) { Text(button) } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}
