package io.github.holsoma.fairenough.core

import kotlinx.serialization.Serializable

@Serializable
data class RailFareDataset(
    val schemaVersion: Int,
    val id: String,
    val validFrom: String,
    val validUntil: String,
    val retrieved: String,
    val source: String,
    val stationIds: List<String>,
    val pairs: List<RailFarePair>,
)

@Serializable
data class RailFarePair(val origin: String, val destination: String, val distanceMetres: Int, val cents: Int)

/** Exact station-pair observations. Rounded adjacent distances must not be added together. */
class RailFareTable(val data: RailFareDataset) {
    private val stations = data.stationIds.toSet()
    private val pairs: Map<String, RailFarePair>

    init {
        require(data.schemaVersion == 1) { "Unsupported rail fare data version." }
        require(data.id.isNotBlank() && data.source.isNotBlank()) { "Rail fare data needs its version and source." }
        require(validDate(data.validFrom) && validDate(data.validUntil) && data.validFrom <= data.validUntil && validDate(data.retrieved)) { "Invalid rail fare data dates." }
        require(stations.size >= 2 && stations.size == data.stationIds.size && stations.none { it.isBlank() }) { "Invalid rail station catalogue." }
        val indexed = mutableMapOf<String, RailFarePair>()
        for (pair in data.pairs) {
            require(pair.origin in stations && pair.destination in stations && pair.origin != pair.destination) { "Invalid rail fare station pair." }
            require(pair.distanceMetres in 1..200_000 && pair.cents in 1..100_000) { "Invalid rail distance or fare." }
            require(indexed.put(key(pair.origin, pair.destination), pair) == null) { "Duplicate rail fare station pair." }
        }
        require(indexed.size.toLong() == stations.size.toLong() * (stations.size - 1) / 2) { "Rail fare data must cover every station pair." }
        pairs = indexed.toMap()
    }

    fun quote(trip: Trip): FareQuote? {
        if (!validDate(trip.date) || trip.date !in data.validFrom..data.validUntil) return null
        val pair = pairs[key(trip.origin, trip.destination)] ?: return null
        return FareQuote(pair.cents, data.source, data.id, pair.distanceMetres)
    }

    private fun key(a: String, b: String): String = if (a < b) "$a|$b" else "$b|$a"
}

fun railDistance(metres: Long): String = String.format(java.util.Locale.UK, "%.1f km", metres / 1000.0)
fun railDistance(metres: Int): String = railDistance(metres.toLong())

data class RailDistanceTotal(val metres: Long, val coveredTrips: Int, val totalTrips: Int)

fun railDistanceTotal(trips: List<Trip>, table: RailFareTable): RailDistanceTotal {
    val distances = trips.mapNotNull { table.quote(it)?.distanceMetres }
    return RailDistanceTotal(distances.sumOf { it.toLong() }, distances.size, trips.size)
}
