package io.github.holsoma.fairenough.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.holsoma.fairenough.core.*
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

fun monthLabel(month: String): String = YearMonth.parse(month).format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.UK))

@Composable fun MonthPicker(months: List<MonthStats>, title: String = "Choose a recorded month", exclude: String? = null, onChoose: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = {
        LazyColumn(Modifier.heightIn(max = 400.dp)) {
            items(months, key = { it.month }) { month ->
                ListItem(headlineContent = { Text(monthLabel(month.month)) }, supportingContent = { Text("${month.trips} journeys · ${month.pricedTrips} priced") }, trailingContent = { if (month.month == exclude) Text("In use", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.clickable(enabled = month.month != exclude) { onChoose(month.month) })
            }
            if (months.isEmpty()) item { Text("No recorded months yet. Use Previous and Next to browse.") }
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } })
}

@Composable fun MonthComparison(trips: List<Trip>, onOpenMonth: (String) -> Unit) {
    val months = remember(trips) { monthlyStatistics(trips) }
    if (months.isEmpty()) { Notice("Record or import journeys to compare your months."); return }
    var selectedMonth by rememberSaveable { mutableStateOf(months.first().month) }
    var referenceMonth by rememberSaveable { mutableStateOf(months.getOrNull(1)?.month) }
    var picker by rememberSaveable { mutableStateOf<String?>(null) }
    var chartMetric by rememberSaveable { mutableStateOf("Journeys") }
    val selected = months.firstOrNull { it.month == selectedMonth } ?: months.first()
    val reference = months.firstOrNull { it.month == referenceMonth && it.month != selected.month }
    val fareChange = reference?.let { monthlyFareChange(selected, it) }
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { picker = "month" }, modifier = Modifier.weight(1f)) { Text(monthLabel(selected.month)) }
            OutlinedButton(onClick = { picker = "reference" }, enabled = months.size > 1, modifier = Modifier.weight(1f)) { Text(reference?.let { monthLabel(it.month) } ?: "Compare with") }
        }
        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.large) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("${monthLabel(selected.month)} recorded fares", style = MaterialTheme.typography.labelLarge)
                Text(fareTotalText(selected.fareCents, selected.pricedTrips, selected.trips), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.SemiBold)
                Text("${selected.pricedTrips} of ${selected.trips} journeys priced", style = MaterialTheme.typography.bodyMedium)
                reference?.let {
                    HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = .2f))
                    Text(when {
                        fareChange == null -> "Fare comparison incomplete. Add the missing fares in either month."
                        fareChange == 0L -> "The same fare total as ${monthLabel(it.month)}."
                        fareChange > 0 -> "${money(fareChange)} more than ${monthLabel(it.month)}."
                        else -> "${money(-fareChange)} less than ${monthLabel(it.month)}."
                    }, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        if (reference == null) Notice("Choose another recorded month to compare. A single month still shows its totals below.")
        if (LocalDensity.current.fontScale > 1.2f) Text("Month totals", style = MaterialTheme.typography.titleSmall)
        else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Month totals", Modifier.weight(1.2f), style = MaterialTheme.typography.titleSmall)
            Text(monthLabel(selected.month), Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
            Text(reference?.let { monthLabel(it.month) } ?: "", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
        }
        ComparisonRow("Journeys", selected.trips.toString(), reference?.trips?.toString(), selected.month, reference?.month)
        ComparisonRow("Travel days", selected.travelDays.toString(), reference?.travelDays?.toString(), selected.month, reference?.month)
        ComparisonRow("Average fare", selected.averageFare?.let(::money) ?: "Not priced", reference?.averageFare?.let(::money), selected.month, reference?.month)
        Text("Average fares use priced journeys. Calendar totals reflect the records you entered; a month still in progress or an incomplete import can look lower. Fare changes can also reflect a new tariff.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(onClick = { onOpenMonth(selected.month) }, modifier = Modifier.fillMaxWidth()) { Text("Explore ${monthLabel(selected.month)}") }
        HorizontalDivider()
        Text("Month by month", style = MaterialTheme.typography.titleLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Journeys", "Fares").forEach { metric -> FilterChip(chartMetric == metric, { chartMetric = metric }, label = { Text(metric) }) }
        }
        Text("Tap a month to select it above. A dot marks the comparison month.", style = MaterialTheme.typography.bodySmall)
        val maximum = months.maxOf { if (chartMetric == "Journeys") it.trips.toLong() else it.fareCents }.coerceAtLeast(1)
        months.forEach { month ->
            val value = if (chartMetric == "Journeys") month.trips.toLong() else month.fareCents
            val chosen = month.month == selected.month
            Surface(color = if (chosen) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth().clickable {
                    if (month.month == referenceMonth) referenceMonth = selected.month
                    selectedMonth = month.month
                }) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${if (month.month == reference?.month) "• " else ""}${monthLabel(month.month)}", fontWeight = if (chosen) FontWeight.Bold else FontWeight.Medium)
                        Text(if (chartMetric == "Journeys") "${month.trips} trips" else fareTotalText(month.fareCents, month.pricedTrips, month.trips))
                    }
                    if (chartMetric == "Journeys" || month.pricedTrips > 0) LinearProgressIndicator(progress = { value.toFloat() / maximum }, modifier = Modifier.fillMaxWidth().height(6.dp), color = if (chosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary)
                    if (chartMetric == "Fares" && !month.complete) Text("${month.pricedTrips}/${month.trips} priced", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        Text("Pass comparisons use each pass's actual dates. Calendar-month fare totals are not pass savings.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    picker?.let { target ->
        MonthPicker(months, if (target == "reference") "Compare with a month" else "Choose a month", if (target == "reference") selected.month else reference?.month,
            onChoose = { if (target == "reference") referenceMonth = it else selectedMonth = it; picker = null }, onDismiss = { picker = null })
    }
}

@Composable private fun ComparisonRow(label: String, selected: String, reference: String?, selectedMonth: String, referenceMonth: String?) {
    if (LocalDensity.current.fontScale > 1.2f) Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.titleSmall)
        Text("${monthLabel(selectedMonth)}: $selected", fontWeight = FontWeight.SemiBold)
        referenceMonth?.let { Text("${monthLabel(it)}: ${reference ?: "Not priced"}", color = MaterialTheme.colorScheme.onSurfaceVariant) }
    } else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, Modifier.weight(1.2f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(selected, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
        Text(reference ?: "Not available", Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
