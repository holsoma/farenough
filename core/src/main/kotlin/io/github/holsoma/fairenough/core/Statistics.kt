package io.github.holsoma.fairenough.core

import java.time.LocalDate

data class Comparison(val totalCents: Long, val passCents: Int, val tripCount: Int, val pricedCount: Int) {
    val difference: Long get() = totalCents - passCents
    val remaining: Long get() = (passCents - totalCents).coerceAtLeast(0)
    val complete: Boolean get() = pricedCount == tripCount
    val coverage: Float get() = (totalCents.toDouble() / passCents).toFloat()
    val averageCents: Double? get() = if (pricedCount > 0) totalCents.toDouble() / pricedCount else null
    val averageTripsNeeded: Long? get() = when {
        remaining == 0L -> 0L
        totalCents == 0L -> null
        else -> ceilDivide(remaining * pricedCount, totalCents)
    }
    fun tripsAt(cents: Int): Long? = when { remaining == 0L -> 0; cents <= 0 -> null; else -> ceilDivide(remaining, cents.toLong()) }
}

fun ceilDivide(a: Long, b: Long): Long = a / b + if (a % b > 0) 1 else 0
fun compare(pass: PassPeriod, trips: List<Trip>): Comparison {
    val selected = trips.filter { pass.contains(it.date) }
    return Comparison(selected.sumOf { it.effectiveFare?.toLong() ?: 0 }, pass.priceCents, selected.size, selected.count { it.effectiveFare != null })
}

data class RouteStats(val origin: String, val destination: String, val count: Int, val totalCents: Long, val pricedCount: Int, val trips: List<Trip>)
fun routeStatistics(trips: List<Trip>): List<RouteStats> = trips.groupBy { it.origin to it.destination }.map { (pair, items) ->
    RouteStats(pair.first, pair.second, items.size, items.sumOf { it.effectiveFare?.toLong() ?: 0 }, items.count { it.effectiveFare != null }, items.sortedByDescending { it.date })
}.sortedWith(compareByDescending<RouteStats> { it.count }.thenBy { it.origin }.thenBy { it.destination })

data class DayStats(val date: String, val trips: Int, val cents: Long, val cumulativeCents: Long)
fun dailyStatistics(trips: List<Trip>, start: String, end: String): List<DayStats> {
    val groups = trips.filter { it.date in start..end }.groupBy { it.date }
    val result = mutableListOf<DayStats>()
    var day = LocalDate.parse(start)
    val last = LocalDate.parse(end)
    require(!last.isBefore(day) && java.time.temporal.ChronoUnit.DAYS.between(day, last) <= 3660) { "Choose a period of at most ten years." }
    var sum = 0L
    while (!day.isAfter(last)) {
        val rows = groups[day.toString()].orEmpty()
        val cents = rows.sumOf { it.effectiveFare?.toLong() ?: 0 }
        sum += cents
        result += DayStats(day.toString(), rows.size, cents, sum)
        day = day.plusDays(1)
    }
    return result
}

fun distinctEndpoints(trips: List<Trip>): Int = trips.flatMap { listOf(it.origin, it.destination) }.toSet().size
