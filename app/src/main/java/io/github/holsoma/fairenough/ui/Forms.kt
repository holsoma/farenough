package io.github.holsoma.fairenough.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import io.github.holsoma.fairenough.AppModel
import io.github.holsoma.fairenough.core.*
import java.time.LocalDate
import java.time.ZoneId

fun singaporeToday(): LocalDate = LocalDate.now(ZoneId.of("Asia/Singapore"))
@Composable private fun FormColumn(footer: @Composable ColumnScope.() -> Unit = {}, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().imePadding()) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
        footer()
    }
}

@Composable fun PassForm(model: AppModel, initial: PassPeriod?, onDone: () -> Unit) {
    val today = singaporeToday()
    var start by rememberSaveable { mutableStateOf(initial?.start ?: today.toString()) }
    var end by rememberSaveable { mutableStateOf(initial?.end ?: today.plusMonths(1).minusDays(1).toString()) }
    var price by rememberSaveable { mutableStateOf(initial?.priceCents?.let { "%.2f".format(java.util.Locale.UK, it / 100.0) } ?: "48.00") }
    FormColumn {
        PageTitle(if (initial == null) "Set up your pass" else "Edit your pass", "Undergraduate train pass, compared with adult card fares.")
        Notice("Use the actual dates shown on your pass. The suggested end date is only a starting point.")
        DateField("First valid day", start) { start = it }
        DateField("Last valid day", end) { end = it }
        OutlinedTextField(price, { price = it }, Modifier.fillMaxWidth(), label = { Text("Price paid, S$") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
        Button(onClick = {
            val cents = parseMoney(price)
            if (cents == null || cents == 0) model.message = "Enter the pass price in dollars and cents."
            else model.savePass(PassPeriod(id = initial?.id ?: newId(), start = start, end = end, priceCents = cents), onDone)
        }, enabled = !model.working, modifier = Modifier.fillMaxWidth()) { Text("Save pass") }
    }
}

@Composable fun TripForm(model: AppModel, snapshot: Snapshot, initial: Trip?, presetOrigin: String, presetDestination: String, onDone: () -> Unit) {
    var date by rememberSaveable { mutableStateOf(initial?.date ?: singaporeToday().toString()) }
    var origin by rememberSaveable { mutableStateOf(initial?.origin ?: presetOrigin) }
    var destination by rememberSaveable { mutableStateOf(initial?.destination ?: presetDestination) }
    var note by rememberSaveable { mutableStateOf(initial?.note.orEmpty()) }
    var correction by rememberSaveable { mutableStateOf(initial?.correctionCents?.let { "%.2f".format(java.util.Locale.UK, it / 100.0) }.orEmpty()) }
    var reason by rememberSaveable { mutableStateOf(initial?.correctionReason.orEmpty()) }
    var delete by remember { mutableStateOf(false) }
    val quote = remember(origin, destination, date, snapshot.routeFares, initial) {
        val current = if (origin.isNotEmpty() && destination.isNotEmpty() && validDate(date)) model.engine.quote(Trip(date = date, origin = origin, destination = destination), snapshot.routeFares) else null
        val recordedCents = initial?.fareCents
        if (recordedCents != null && initial.origin == origin && initial.destination == destination && initial.date == date) {
            FareQuote(recordedCents, initial.fareSource, initial.fareVersion ?: "recorded", current?.distanceMetres)
        } else current
    }
    FormColumn(footer = {
        HorizontalDivider()
        Button(onClick = {
            val corrected = if (correction.isBlank()) null else parseMoney(correction)
            if (correction.isNotBlank() && corrected == null) model.message = "Enter a valid corrected fare."
            else if (correction.isNotBlank() && reason.isBlank()) model.message = "Add a short reason for the fare correction."
            else model.saveTrip((initial ?: Trip(date = date, origin = origin, destination = destination)).copy(date = date, origin = origin, destination = destination, note = note, correctionCents = corrected, correctionReason = reason), onDone)
        }, enabled = !model.working && validDate(date) && origin.isNotBlank() && destination.isNotBlank() && origin != destination, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) { Text("Save journey") }
    }) {
        PageTitle(if (initial == null) "Record a journey" else "Edit journey", "One entry and exit. Line changes inside the paid area are part of this journey.")
        DateField("Travel date", date) { date = it }
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                StationField("From", origin, model.stations) { origin = it }
                StationField("To", destination, model.stations) { destination = it }
            }
            TextButton(onClick = { val temp = origin; origin = destination; destination = temp }, modifier = Modifier.width(64.dp).semantics { contentDescription = "Swap stations" }, contentPadding = PaddingValues(4.dp)) {
                Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) { Icon(Icons.Outlined.SwapVert, null); Text("Swap", style = MaterialTheme.typography.labelMedium) }
            }
        }
        Notice(quote?.let {
            buildString {
                append("Regular adult fare: ${money(it.cents)}")
                it.distanceMetres?.let { distance -> append("\nRail fare distance: ${railDistance(distance)}") }
                append("\n${it.source}.")
            }
        } ?: when {
            origin.isEmpty() || destination.isEmpty() -> "Choose two stations to see the rail distance and regular fare."
            origin == destination -> "Choose different start and end stations."
            !validDate(date) -> "Enter a valid travel date to see the fare."
            else -> "Automatic fares are unavailable for this route and travel date. ${fareCoverage(model.repository.railFares)} Keep the correct date. You can save the journey and add a checked fare or an individual correction."
        })
        OutlinedTextField(correction, { correction = it }, Modifier.fillMaxWidth(), label = { Text("This journey's corrected fare, S$ (optional)") }, supportingText = { Text("For example, a discounted ride. Blank uses the regular fare.") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
        if (correction.isNotBlank()) OutlinedTextField(reason, { reason = it.take(500) }, Modifier.fillMaxWidth(), label = { Text("Reason for correction") })
        OutlinedTextField(note, { note = it.take(4000) }, Modifier.fillMaxWidth(), label = { Text("Note (optional)") }, minLines = 2)
        if (origin.isNotBlank() && destination.isNotBlank() && origin != destination) TextButton(onClick = { model.favourite(origin, destination) }) { Text(if ("$origin|$destination" in snapshot.preferences.favourites) "Remove from favourite routes" else "Save as a favourite route") }

        if (initial != null) TextButton(onClick = { delete = true }, modifier = Modifier.fillMaxWidth()) { Text("Delete journey", color = MaterialTheme.colorScheme.error) }
    }
    if (delete && initial != null) Confirm("Delete this journey?", "This removes it from your recorded totals and map.", "Delete", { delete = false }) { model.deleteTrip(initial.id, onDone) }
}

@Composable fun FareForm(model: AppModel, snapshot: Snapshot, initial: RouteFare?, onDone: () -> Unit) {
    val isExisting = initial != null && snapshot.routeFares.any { it.id == initial.id }
    var origin by rememberSaveable { mutableStateOf(initial?.origin.orEmpty()) }
    var destination by rememberSaveable { mutableStateOf(initial?.destination.orEmpty()) }
    var start by rememberSaveable { mutableStateOf(initial?.start ?: "2025-12-27") }
    var end by rememberSaveable { mutableStateOf(initial?.end ?: "2026-12-25") }
    var price by rememberSaveable { mutableStateOf(if (isExisting) "%.2f".format(java.util.Locale.UK, initial!!.cents / 100.0) else "") }
    var confirm by remember { mutableStateOf<RouteFare?>(null) }
    var removing by remember { mutableStateOf(false) }
    FormColumn {
        PageTitle(if (!isExisting) "Save a route fare" else "Edit route fare", "Override the automatic fare or cover a date outside the bundled data. This fare applies in this direction only.")
        ExternalLink("Open LTA fare calculator", "https://www.lta.gov.sg/content/ltagov/en/map/fare-calculator.html")
        StationField("From", origin, model.stations) { origin = it }
        StationField("To", destination, model.stations) { destination = it }
        OutlinedTextField(price, { price = it }, Modifier.fillMaxWidth(), label = { Text("Regular adult fare, S$") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
        DateField("Valid from", start) { start = it }
        DateField("Valid until", end) { end = it }
        Notice("Fare periods are separate from pass periods. Do not apply today's fare to older records without checking. Individual trip corrections are retained.")
        Button(onClick = {
            val cents = parseMoney(price)
            if (cents == null) model.message = "Enter the checked fare in dollars and cents."
            else {
                val fare = RouteFare(id = initial?.id ?: newId(), origin = origin, destination = destination, start = start, end = end, cents = cents)
                val problem = validateRouteFare(fare, snapshot.routeFares, model.stations)
                if (problem != null) model.message = problem else confirm = fare
            }
        }, enabled = !model.working, modifier = Modifier.fillMaxWidth()) { Text("Review affected journeys") }
        if (isExisting) TextButton(onClick = { removing = true }, modifier = Modifier.fillMaxWidth()) { Text("Remove saved fare") }
    }
    confirm?.let { fare ->
        val affected = snapshot.trips.count { fare.matches(it) || initial?.matches(it) == true }
        Confirm("Save ${money(fare.cents)} for this route?", "$affected recorded journeys will have their regular fare recalculated. Corrections to individual trips will remain.", "Save fare", { confirm = null }) { model.saveFare(fare, onDone) }
    }
    if (removing && initial != null) {
        val affected = snapshot.trips.filter(initial::matches)
        val remaining = snapshot.routeFares.filterNot { it.id == initial.id }
        val unpriced = affected.count { model.engine.quote(it, remaining) == null && it.correctionCents == null }
        Confirm("Remove this saved fare?", "${affected.size} recorded journeys will use automatic fares where available. $unpriced journeys will be unpriced. Individual corrections will remain.", "Remove fare", { removing = false }) {
            model.removeFare(initial, onDone)
        }
    }
}
