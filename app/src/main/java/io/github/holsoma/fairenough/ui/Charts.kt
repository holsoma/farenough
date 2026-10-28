package io.github.holsoma.fairenough.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.holsoma.fairenough.core.*
import kotlin.math.*

@Composable fun FareChart(days: List<DayStats>, passPrice: Int?, selected: Int?, onSelect: (Int) -> Unit) {
    val ink = MaterialTheme.colorScheme.primary
    val rule = MaterialTheme.colorScheme.outline.copy(alpha = .35f)
    val target = MaterialTheme.colorScheme.secondary
    val maximum = max(days.maxOfOrNull { it.cumulativeCents } ?: 0, passPrice?.toLong() ?: 0).coerceAtLeast(1)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Cumulative fares", style = MaterialTheme.typography.titleMedium); Text(money(maximum), style = MaterialTheme.typography.labelMedium) }
        Canvas(Modifier.fillMaxWidth().height(190.dp).semantics { contentDescription = "Cumulative recorded fare chart. Use the day list below for values." }.pointerInput(days) { detectTapGestures { p -> if (days.isNotEmpty()) onSelect(((p.x / size.width) * (days.size - 1)).roundToInt().coerceIn(days.indices)) } }) {
            val margin = 12.dp.toPx()
            val h = size.height - margin * 2
            fun x(i: Int) = margin + i * (size.width - 2 * margin) / (days.size - 1).coerceAtLeast(1)
            fun y(cents: Long) = size.height - margin - (cents.toFloat() / maximum) * h
            repeat(4) { i -> val yy = margin + h * i / 3; drawLine(rule, Offset(0f, yy), Offset(size.width, yy), 1.dp.toPx()) }
            passPrice?.let { drawLine(target, Offset(0f, y(it.toLong())), Offset(size.width, y(it.toLong())), 2.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 8f))) }
            if (days.isNotEmpty()) {
                val path = Path().apply { moveTo(x(0), y(days[0].cumulativeCents)); days.forEachIndexed { i, d -> if (i > 0) { lineTo(x(i), y(days[i - 1].cumulativeCents)); lineTo(x(i), y(d.cumulativeCents)) } } }
                drawPath(path, ink, style = Stroke(3.dp.toPx()))
                selected?.takeIf { it in days.indices }?.let { drawCircle(ink, 6.dp.toPx(), Offset(x(it), y(days[it].cumulativeCents))) }
            }
        }
        Text(if (passPrice != null) "Solid: recorded fares. Dashed: ${money(passPrice)} pass price." else "Tap a point or choose a day below.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable fun CountChart(days: List<DayStats>, onSelect: (Int) -> Unit) {
    val colour = MaterialTheme.colorScheme.primary
    val max = (days.maxOfOrNull { it.trips } ?: 1).coerceAtLeast(1)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Journeys by day", style = MaterialTheme.typography.titleMedium)
        Canvas(Modifier.fillMaxWidth().height(110.dp).semantics { contentDescription = "Daily trip counts. Use the day list below for values." }.pointerInput(days) { detectTapGestures { p -> if (days.isNotEmpty()) onSelect((p.x / size.width * days.size).toInt().coerceIn(days.indices)) } }) {
            val slot = size.width / days.size.coerceAtLeast(1)
            days.forEachIndexed { i, d -> val height = size.height * d.trips / max; drawRect(colour, Offset(i * slot + slot * .12f, size.height - height), androidx.compose.ui.geometry.Size(slot * .76f, height)) }
        }
        Text("Highest daily count: $max. Tap a bar to see its journeys.", style = MaterialTheme.typography.bodySmall)
    }
}

private fun quadratic(a: Offset, b: Offset, control: Offset, t: Float): Offset = a * ((1 - t) * (1 - t)) + control * (2 * (1 - t) * t) + b * (t * t)

@Composable fun TravelMap(stations: List<Station>, coastline: List<List<List<Double>>>, routes: List<RouteStats>, selectedRoute: String?, onSelect: (RouteStats) -> Unit) {
    var zoom by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    val land = MaterialTheme.colorScheme.surfaceContainer
    val border = MaterialTheme.colorScheme.outline.copy(alpha = .35f)
    val routeColour = MaterialTheme.colorScheme.primary
    val highlight = MaterialTheme.colorScheme.secondary
    val stationLookup = remember(stations) { stations.associateBy { it.id } }
    val maxCount = routes.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
    var canvasSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }
    fun project(lon: Double, lat: Double, size: androidx.compose.ui.geometry.Size): Offset {
        val spanX = .48f; val spanY = .34f
        val fit = min(size.width / spanX, size.height / spanY) * .94f
        return Offset(size.width / 2 + ((lon - 103.84) * fit * zoom).toFloat(), size.height / 2 - ((lat - 1.33) * fit * zoom).toFloat()) + pan
    }
    fun points(route: RouteStats, size: androidx.compose.ui.geometry.Size): Triple<Offset, Offset, Offset>? {
        val a = stationLookup[route.origin] ?: return null; val b = stationLookup[route.destination] ?: return null
        val ax = a.longitude ?: return null; val ay = a.latitude ?: return null
        val bx = b.longitude ?: return null; val by = b.latitude ?: return null
        val p = project(ax, ay, size); val q = project(bx, by, size)
        val delta = q - p
        val normal = Offset(-delta.y, delta.x) / delta.getDistance().coerceAtLeast(1f)
        return Triple(p, q, (p + q) / 2f + normal * min(24f * zoom, delta.getDistance() * .16f))
    }
    Column {
        Canvas(Modifier.fillMaxWidth().height(320.dp).clipToBounds().semantics { contentDescription = "Singapore map with ${routes.size} directional connections. The route list below provides accessible details." }
            .pointerInput(Unit) { detectTransformGestures { _, drag, change, _ -> zoom = (zoom * change).coerceIn(1f, 5f); pan = if (zoom == 1f) Offset.Zero else pan + drag } }
            .pointerInput(routes, zoom, pan) { detectTapGestures { tap ->
                val nearest = routes.mapNotNull { r -> points(r, canvasSize)?.let { (a, b, c) -> r to (0..24).minOf { t -> (quadratic(a, b, c, t / 24f) - tap).getDistance() } } }.minByOrNull { it.second }
                if (nearest != null && nearest.second < 32f) onSelect(nearest.first)
            } }) {
            canvasSize = size
            coastline.forEach { ring ->
                if (ring.size > 2) {
                    val path = Path().apply { ring.forEachIndexed { i, xy -> val p = project(xy[0], xy[1], size); if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) }; close() }
                    drawPath(path, land); drawPath(path, border, style = Stroke(1f))
                }
            }
            routes.sortedBy { it.count }.forEach { r ->
                points(r, size)?.let { (a, b, c) ->
                    val colour = if (selectedRoute == "${r.origin}|${r.destination}") highlight else routeColour
                    val width = (1.5f + r.count.toFloat() / maxCount * 4f).dp.toPx()
                    val path = Path().apply { moveTo(a.x, a.y); quadraticTo(c.x, c.y, b.x, b.y) }
                    drawPath(path, colour.copy(alpha = .72f), style = Stroke(width))
                    val mid = quadratic(a, b, c, .58f); val before = quadratic(a, b, c, .54f)
                    val direction = (mid - before) / (mid - before).getDistance().coerceAtLeast(1f)
                    val normal = Offset(-direction.y, direction.x)
                    drawLine(colour, mid, mid - direction * 12f + normal * 6f, 2.dp.toPx())
                    drawLine(colour, mid, mid - direction * 12f - normal * 6f, 2.dp.toPx())
                    drawCircle(colour, 3.dp.toPx(), a); drawCircle(colour, 3.dp.toPx(), b)
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Line weight: 1 to $maxCount trips", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f)); TextButton(onClick = { zoom = 1f; pan = Offset.Zero }) { Text("Reset map") } }
        Text("Connections show start and end stations, not the train path. Pinch to zoom. Select a connection or a route below.", style = MaterialTheme.typography.bodySmall)
    }
}
