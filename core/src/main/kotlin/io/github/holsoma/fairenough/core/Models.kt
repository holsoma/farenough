package io.github.holsoma.fairenough.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.util.UUID

val ExchangeJson = Json { prettyPrint = true; encodeDefaults = true; ignoreUnknownKeys = false }
fun newId(): String = UUID.randomUUID().toString()

@Serializable
data class Station(val id: String, val name: String, val codes: List<String>, val latitude: Double? = null, val longitude: Double? = null)

@Serializable
data class PassPeriod(val id: String = newId(), val name: String = "Undergraduate train pass", val start: String, val end: String, val priceCents: Int = 4800) {
    fun contains(date: String): Boolean = date >= start && date <= end
}

@Serializable
data class Trip(
    val id: String = newId(), val date: String, val origin: String, val destination: String,
    val fareCents: Int? = null, val fareSource: String = "Fare needed", val fareVersion: String? = null,
    val correctionCents: Int? = null, val correctionReason: String = "", val note: String = "",
    val sourceText: String = "", val importBatchId: String? = null, val edited: Boolean = false,
) {
    val effectiveFare: Int? get() = correctionCents ?: fareCents
    val routeKey: String get() = "$origin|$destination"
}

@Serializable
data class RouteFare(val id: String = newId(), val origin: String, val destination: String, val start: String, val end: String, val cents: Int, val source: String = "User checked with LTA") {
    fun matches(trip: Trip): Boolean = trip.origin == origin && trip.destination == destination && trip.date in start..end
}

@Serializable
data class ImportBatch(val id: String, val fingerprint: String, val createdAt: String, val tripIds: List<String>)

@Serializable
data class Preferences(val appearance: String = "system", val favourites: List<String> = emptyList())

@Serializable
data class Snapshot(val passes: List<PassPeriod> = emptyList(), val trips: List<Trip> = emptyList(), val routeFares: List<RouteFare> = emptyList(), val batches: List<ImportBatch> = emptyList(), val preferences: Preferences = Preferences())

@Serializable
data class FareQuote(val cents: Int, val source: String, val version: String, val distanceMetres: Int? = null)

/** No distance guessed from coordinates. A missing quote stays null. */
class FareEngine(private val bundled: List<RouteFare> = emptyList(), private val automatic: RailFareTable? = null) {
    fun quote(trip: Trip, saved: List<RouteFare>): FareQuote? {
        if (trip.origin == trip.destination || !validDate(trip.date)) return null
        val official = automatic?.quote(trip)
        val custom = saved.singleOrNull { it.matches(trip) }
        val fare = custom ?: bundled.singleOrNull { it.matches(trip) } ?: return official
        return FareQuote(fare.cents, fare.source, "${fare.start}/${fare.end}", official?.distanceMetres)
    }
    fun assess(trip: Trip, saved: List<RouteFare>): Trip {
        val q = quote(trip, saved)
        return trip.copy(fareCents = q?.cents, fareSource = q?.source ?: "Fare needed", fareVersion = q?.version)
    }
}

/** Fill missing regular fares on upgrade or restore without changing saved prices or corrections. */
fun priceMissingTrips(snapshot: Snapshot, engine: FareEngine): Snapshot = snapshot.copy(
    trips = snapshot.trips.map { if (it.fareCents == null && engine.quote(it, snapshot.routeFares) != null) engine.assess(it, snapshot.routeFares) else it },
)

fun validDate(value: String): Boolean = runCatching { LocalDate.parse(value).toString() == value }.getOrDefault(false)
fun validatePass(pass: PassPeriod, existing: List<PassPeriod>): String? = when {
    !validDate(pass.start) || !validDate(pass.end) -> "Use dates in YYYY-MM-DD format."
    pass.end < pass.start -> "The end date must follow the start date."
    java.time.temporal.ChronoUnit.DAYS.between(LocalDate.parse(pass.start), LocalDate.parse(pass.end)) > 365 -> "A pass period cannot exceed one year. Check the dates."
    pass.priceCents !in 1..1_000_000 -> "Enter a pass price between S$0.01 and S$10,000."
    existing.any { it.id != pass.id && it.start <= pass.end && pass.start <= it.end } -> "This pass overlaps an existing pass."
    else -> null
}

fun validateRouteFare(fare: RouteFare, existing: List<RouteFare>, stations: List<Station>): String? = when {
    stations.none { it.id == fare.origin } || stations.none { it.id == fare.destination } -> "Choose two recognised stations."
    fare.origin == fare.destination -> "Choose different start and end stations."
    !validDate(fare.start) || !validDate(fare.end) || fare.end < fare.start -> "Enter a valid fare period."
    fare.cents !in 0..1_000_000 -> "Enter a fare between S$0 and S$10,000."
    existing.any { it.id != fare.id && it.origin == fare.origin && it.destination == fare.destination && it.start <= fare.end && fare.start <= it.end } -> "A saved fare for this route overlaps these dates. Edit that fare instead."
    else -> null
}

fun money(cents: Long): String = "S$" + String.format(java.util.Locale.UK, "%.2f", cents / 100.0)
fun money(cents: Int): String = money(cents.toLong())
fun parseMoney(value: String): Int? = runCatching {
    val n = value.trim().toBigDecimal().movePointRight(2).intValueExact()
    n.takeIf { it in 0..1_000_000 }
}.getOrNull()
