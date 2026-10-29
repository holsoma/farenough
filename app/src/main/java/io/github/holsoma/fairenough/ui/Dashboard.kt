package io.github.holsoma.fairenough.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.holsoma.fairenough.AppModel
import io.github.holsoma.fairenough.core.*

@Composable fun Overview(model: AppModel, snapshot: Snapshot, pass: PassPeriod?, onPass: () -> Unit, onTarget: () -> Unit, onImport: () -> Unit, onFares: () -> Unit, onAdd: (String, String) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 100.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        item { PageTitle("Have you travelled enough?", pass?.let(::periodLabel) ?: "See when your train pass covers its cost.") }
        if (pass == null) {
            item { Notice("Start with your pass price and its actual validity dates. Then record journeys or bring in your old records.") }
            item { Button(onClick = onPass, modifier = Modifier.fillMaxWidth()) { Text("Set up a pass") } }
            item { OutlinedButton(onClick = onImport, modifier = Modifier.fillMaxWidth()) { Text("Import existing journeys") } }
        } else {
            val c = compare(pass, snapshot.trips)
            item {
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.large) {
                    Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(if (c.difference > 0) "Estimated savings" else if (c.difference == 0L) "Pass cost covered" else "Still to cover", style = MaterialTheme.typography.titleMedium)
                        Text(money(if (c.difference > 0) c.difference else c.remaining), fontSize = 46.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        LinearProgressIndicator(progress = { c.coverage.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(9.dp), color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.surface.copy(alpha = .6f))
                        Text("${money(c.totalCents)} in recorded fares / ${money(pass.priceCents)} pass", style = MaterialTheme.typography.bodyMedium)
                        Text(if (c.complete) "Regular-fare estimate. Correct discounted journeys for a closer comparison." else "Based on ${c.pricedCount} of ${c.tripCount} trips. Add missing fares before drawing a conclusion.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) { Metric(c.tripCount.toString(), "Recorded trips", Modifier.weight(1f)); Metric(snapshot.trips.filter { pass.contains(it.date) }.map { it.date }.distinct().size.toString(), "Travel days", Modifier.weight(1f)) } }
            item {
                OutlinedButton(onClick = onTarget, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("How many more trips?") }
                if (!c.complete) TextButton(onClick = onFares, modifier = Modifier.fillMaxWidth()) { Text("Add missing route fares") }
            }
            if (snapshot.preferences.favourites.isNotEmpty()) {
                item { Text("Your usual journeys", style = MaterialTheme.typography.titleLarge) }
                items(snapshot.preferences.favourites) { key ->
                    val pair = key.split('|')
                    ListItem(headlineContent = { Text("${model.name(pair[0])} → ${model.name(pair[1])}") }, supportingContent = { Text("Tap to record this journey") }, modifier = Modifier.clickable { onAdd(pair[0], pair[1]) })
                }
            }
            item { TextButton(onClick = onImport) { Text("Bring in your existing records") } }
        }
    }
}

@Composable fun TripList(model: AppModel, snapshot: Snapshot, pass: PassPeriod?, onEdit: (Trip) -> Unit, onImport: () -> Unit) {
    var allDates by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    val trips = snapshot.trips.filter { (allDates || pass == null || pass.contains(it.date)) && (model.name(it.origin).contains(query, true) || model.name(it.destination).contains(query, true) || it.note.contains(query, true)) }.sortedByDescending { it.date }
    LazyColumn(contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 100.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { PageTitle("Your journeys", "${trips.size} recorded trips${if (!allDates && pass != null) " in this pass period" else " across all dates"}.") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !allDates, onClick = { allDates = false }, label = { Text("Selected pass") })
                FilterChip(selected = allDates, onClick = { allDates = true }, label = { Text("All dates") })
                TextButton(onClick = onImport) { Text("Import") }
            }
            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), label = { Text("Find a station or note") }, singleLine = true)
        }
        if (trips.isEmpty()) item { Notice("No journeys here yet. Add a trip with the + button, or import records using the formatting prompt.") }
        trips.groupBy { it.date }.forEach { (date, dayTrips) ->
            item(key = "date-$date") { Text(readableDate(date), Modifier.padding(top = 14.dp), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary) }
            items(dayTrips, key = { it.id }) { trip -> TripRow(model, trip) { onEdit(trip) } }
        }
    }
}

@Composable fun TripRow(model: AppModel, trip: Trip, onClick: () -> Unit) {
    ListItem(modifier = Modifier.clickable(onClick = onClick), headlineContent = { Text("${model.name(trip.origin)} → ${model.name(trip.destination)}") }, supportingContent = {
        Column {
            Text(if (trip.correctionCents != null) "Corrected: ${trip.correctionReason}" else if (trip.fareCents == null) "Fare needed" else trip.fareSource)
            model.repository.railFares.quote(trip)?.distanceMetres?.let { Text("${railDistance(it)} rail fare distance") }
        }
    }, trailingContent = { Text(trip.effectiveFare?.let(::money) ?: "Not priced", fontWeight = FontWeight.Medium) })
}

@Composable fun Target(model: AppModel, snapshot: Snapshot, pass: PassPeriod?) {
    var method by rememberSaveable { mutableStateOf("average") }
    var origin by rememberSaveable { mutableStateOf("") }
    var destination by rememberSaveable { mutableStateOf("") }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item { PageTitle("Your next stop: enough", "A trip target depends on what each journey would cost.") }
        if (pass == null) item { Notice("Set up a pass to calculate a trip target.") } else {
            val c = compare(pass, snapshot.trips)
            item { Notice("${money(c.remaining)} remains against a ${money(pass.priceCents)} pass. Based on ${c.pricedCount} of ${c.tripCount} recorded trips${if (c.complete) "." else "; missing fares make this incomplete."}") }
            item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { FilterChip(method == "average", { method = "average" }, label = { Text("My average fare") }); FilterChip(method == "route", { method = "route" }, label = { Text("A specific route") }) } }
            if (method == "route") {
                item { StationField("From", origin, model.stations) { origin = it } }
                item { StationField("To", destination, model.stations) { destination = it } }
            }
            item {
                val date = singaporeToday().toString().coerceIn(pass.start, pass.end)
                val quote = if (method == "route") model.engine.quote(Trip(date = date, origin = origin, destination = destination), snapshot.routeFares) else null
                val needed = if (c.remaining == 0L) 0L else if (method == "average") c.averageTripsNeeded else quote?.let { c.tripsAt(it.cents) }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(needed?.toString() ?: "?", fontSize = 80.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    Text(if (needed == 0L) "No more trips needed to cover the pass at the recorded fares." else "More one-way trips in this scenario", style = MaterialTheme.typography.titleLarge)
                    Text(if (method == "average") c.averageCents?.let { "Using an average of ${money(kotlin.math.round(it).toLong())}, including recorded zero-fare journeys. Future trips can cost a different amount." } ?: "Record at least one priced journey first." else quote?.let { "Using ${money(it.cents)} per trip, at the fare valid on ${readableDate(date)}." } ?: "Choose two stations. Dates outside the automatic fare coverage need a saved route fare.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
