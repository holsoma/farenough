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
import io.github.holsoma.fairenough.AppModel
import io.github.holsoma.fairenough.core.*
import java.time.YearMonth

@Composable fun Insights(model: AppModel, snapshot: Snapshot, pass: PassPeriod?, onEditTrip: (Trip) -> Unit) {
    var calendar by rememberSaveable { mutableStateOf(false) }
    var monthText by rememberSaveable { mutableStateOf(YearMonth.from(singaporeToday()).toString()) }
    var view by rememberSaveable { mutableStateOf("Charts") }
    var selectedDate by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedRoute by rememberSaveable { mutableStateOf<String?>(null) }
    val month = YearMonth.parse(monthText)
    val start = if (!calendar && pass != null) pass.start else month.atDay(1).toString()
    val end = if (!calendar && pass != null) pass.end else month.atEndOfMonth().toString()
    val trips = snapshot.trips.filter { it.date in start..end }
    val days = remember(trips, start, end) { dailyStatistics(trips, start, end) }
    val routes = remember(trips) { routeStatistics(trips) }
    val priced = trips.count { it.effectiveFare != null }
    val selected = routes.find { "${it.origin}|${it.destination}" == selectedRoute }
    LazyColumn(contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 100.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item { PageTitle("Your travels, in view", "${readableDate(start)} to ${readableDate(end)}") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(!calendar, { calendar = false; selectedDate = null; selectedRoute = null }, label = { Text("Pass period") }, enabled = pass != null)
                FilterChip(calendar || pass == null, { calendar = true; selectedDate = null; selectedRoute = null }, label = { Text("Calendar month") })
            }
            if (calendar || pass == null) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                TextButton(onClick = { monthText = month.minusMonths(1).toString(); selectedDate = null; selectedRoute = null }) { Text("Previous") }
                Text(monthText, fontWeight = FontWeight.Medium)
                TextButton(onClick = { monthText = month.plusMonths(1).toString(); selectedDate = null; selectedRoute = null }) { Text("Next") }
            }
        }
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("Charts", "Map", "Routes").forEach { label -> FilterChip(view == label, { view = label }, label = { Text(label) }) } } }
        if (view != "Map") item { InsightsSummary(trips) }
        if (trips.isEmpty()) item { Notice("No recorded journeys in this period. Try another month or add a journey.") }
        else when (view) {
            "Charts" -> {
                item { FareChart(days, if (!calendar) pass?.priceCents else null, days.indexOfFirst { it.date == selectedDate }.takeIf { it >= 0 }) { selectedDate = days[it].date } }
                item { CountChart(days) { selectedDate = days[it].date } }
                if (selectedDate != null) {
                    item { Text("Journeys on ${readableDate(selectedDate!!)}", style = MaterialTheme.typography.titleMedium) }
                    val dayTrips = trips.filter { it.date == selectedDate }
                    if (dayTrips.isEmpty()) item { Text("No journeys recorded on this day.") }
                    items(dayTrips, key = { "selected-${it.id}" }) { TripRow(model, it) { onEditTrip(it) } }
                }
                item { Text("Daily values", style = MaterialTheme.typography.titleLarge) }
                items(days, key = { it.date }) { day ->
                    ListItem(headlineContent = { Text(readableDate(day.date)) }, supportingContent = { Text("${day.trips} trips · ${money(day.cents)} recorded fares") }, trailingContent = { Text(money(day.cumulativeCents)) }, modifier = Modifier.clickable { selectedDate = day.date })
                }
                if (snapshot.passes.size > 1) {
                    item { Text("Pass comparisons", style = MaterialTheme.typography.titleLarge) }
                    items(snapshot.passes.sortedByDescending { it.start }) { p -> val c = compare(p, snapshot.trips); ListItem(headlineContent = { Text(periodLabel(p)) }, supportingContent = { Text("${c.tripCount} trips; ${c.pricedCount} priced. Pass ${money(p.priceCents)}") }, trailingContent = { Text(money(c.totalCents)) }) }
                }
            }
            else -> {
                if (view == "Map") {
                    item { TravelMap(model.stations, model.repository.coastline, routes, selectedRoute) { selectedRoute = "${it.origin}|${it.destination}" } }
                    item { InsightsSummary(trips) }
                    val unmapped = trips.count { t -> listOf(t.origin, t.destination).any { model.station(it).latitude == null } }
                    if (unmapped > 0) item { Notice("$unmapped journeys have an unavailable station location. They remain in the totals and route list.") }
                }
                selected?.let { r ->
                    item {
                        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.medium) {
                            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("${model.name(r.origin)} → ${model.name(r.destination)}", style = MaterialTheme.typography.titleLarge)
                                Text("${r.count} trips · ${money(r.totalCents)} from ${r.pricedCount} priced trips")
                                Text("${r.trips.map { it.date }.distinct().size} recorded travel days")
                            }
                        }
                    }
                    items(r.trips, key = { "route-${it.id}" }) { t -> Column { Text(readableDate(t.date), style = MaterialTheme.typography.labelSmall); TripRow(model, t) { onEditTrip(t) } } }
                }
                item { Text("Most travelled routes", style = MaterialTheme.typography.titleLarge); Text("Each direction is counted separately. Equal counts share a rank.", style = MaterialTheme.typography.bodySmall) }
                items(routes, key = { "${it.origin}|${it.destination}" }) { r ->
                    val rank = 1 + routes.count { it.count > r.count }
                    ListItem(leadingContent = { Text(rank.toString(), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary) }, headlineContent = { Text("${model.name(r.origin)} → ${model.name(r.destination)}") }, supportingContent = { Text("${money(r.totalCents)} · ${r.pricedCount}/${r.count} priced") }, trailingContent = { Text("${r.count} trips") }, modifier = Modifier.clickable { selectedRoute = "${r.origin}|${r.destination}" })
                }
            }
        }
    }
}

@Composable private fun InsightsSummary(trips: List<Trip>) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Metric(trips.size.toString(), "Trips", Modifier.weight(1f))
            Metric(trips.map { it.date }.distinct().size.toString(), "Travel days", Modifier.weight(1f))
            Metric(distinctEndpoints(trips).toString(), "Start/end stations", Modifier.weight(1f))
        }
        Text("${money(trips.sumOf { it.effectiveFare?.toLong() ?: 0 })} in recorded fares. Based on ${trips.count { it.effectiveFare != null }} of ${trips.size} trips.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
