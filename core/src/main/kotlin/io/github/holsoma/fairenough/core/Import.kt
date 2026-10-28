package io.github.holsoma.fairenough.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import java.security.MessageDigest

@Serializable
data class TripExchange(val format: String, val version: Int, val trips: List<ExchangeTrip>)
@Serializable
data class ExchangeTrip(val date: String, val origin: String, val destination: String, val note: String = "", val sourceText: String = "")
data class ImportRow(val index: Int, val input: ExchangeTrip, val trip: Trip?, val errors: List<String>, val matchesExisting: Int)
data class ImportReview(val rows: List<ImportRow>, val fingerprint: String?, val alreadyImported: Boolean) {
    val validCount: Int get() = rows.count { it.trip != null }
}

class ImportProblem(message: String): IllegalArgumentException(message)

fun resolveStation(token: String, stations: List<Station>): Station? {
    val normal = token.trim()
    val candidates = stations.filter { s -> s.id.equals(normal, true) || s.name.equals(normal, true) || s.codes.any { it.equals(normal, true) } }
    return candidates.singleOrNull()
}

fun tripSignature(trip: Trip): String = "${trip.date}|${trip.origin}|${trip.destination}"
fun fingerprint(text: String): String = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

fun reviewImport(text: String, stations: List<Station>, snapshot: Snapshot, engine: FareEngine = FareEngine()): ImportReview {
    if (text.length > 2_000_000) throw ImportProblem("The import is too large. Split it into files below 2 MB.")
    val content = text.trim().let { if (it.startsWith("```json") && it.endsWith("```")) it.removePrefix("```json").removeSuffix("```").trim() else it }
    val exchange = try { ExchangeJson.decodeFromString<TripExchange>(content) } catch (e: Exception) {
        throw ImportProblem("This is not valid farenough JSON. ${e.message?.take(240)}")
    }
    if (exchange.format !in setOf("farenough-trips", "fairenough-trips")) throw ImportProblem("The format must be farenough-trips.")
    if (exchange.version != 1) throw ImportProblem("Import version ${exchange.version} is not supported. Use version 1.")
    if (exchange.trips.isEmpty()) throw ImportProblem("No journeys were found.")
    if (exchange.trips.size > 10_000) throw ImportProblem("Import at most 10,000 journeys at a time.")
    val counts = snapshot.trips.groupingBy(::tripSignature).eachCount()
    val rows = exchange.trips.mapIndexed { index, input ->
        val from = resolveStation(input.origin, stations)
        val to = resolveStation(input.destination, stations)
        val errors = buildList {
            if (!validDate(input.date)) add("Supply a full date in YYYY-MM-DD format.")
            if (from == null) add("Origin '${input.origin}' needs an unambiguous official name or station code.")
            if (to == null) add("Destination '${input.destination}' needs an unambiguous official name or station code.")
            if (from != null && from.id == to?.id) add("Origin and destination must differ.")
            if (input.note.length > 4000 || input.sourceText.length > 4000) add("Notes and source text must be below 4,000 characters.")
        }
        val trip = if (errors.isEmpty()) engine.assess(Trip(date = input.date, origin = from!!.id, destination = to!!.id, note = input.note, sourceText = input.sourceText), snapshot.routeFares) else null
        ImportRow(index, input, trip, errors, trip?.let { counts[tripSignature(it)] } ?: 0)
    }
    val canonical = rows.takeIf { it.all { row -> row.trip != null } }?.map { row -> ExchangeTrip(row.trip!!.date, row.trip.origin, row.trip.destination, row.input.note, row.input.sourceText) }
    val hash = canonical?.let { fingerprint(ExchangeJson.encodeToString(it)) }
    return ImportReview(rows, hash, hash != null && snapshot.batches.any { it.fingerprint == hash })
}

fun commitImport(review: ImportReview, selected: Set<Int>, snapshot: Snapshot, timestamp: String): Snapshot {
    require(!review.alreadyImported) { "This complete batch has already been imported." }
    require(selected.isNotEmpty()) { "Select at least one journey." }
    require(selected.all { index -> review.rows.any { it.index == index && it.trip != null } }) { "Only valid rows can be imported." }
    val id = newId()
    val trips = review.rows.filter { it.index in selected }.map { it.trip!!.copy(importBatchId = id) }
    // A partial import must not claim the complete source batch was imported.
    val hash = if (trips.size == review.rows.size) review.fingerprint!! else fingerprint(ExchangeJson.encodeToString(trips.map { ExchangeTrip(it.date, it.origin, it.destination, it.note, it.sourceText) }))
    val batch = ImportBatch(id, hash, timestamp, trips.map { it.id })
    return snapshot.copy(trips = snapshot.trips + trips, batches = snapshot.batches + batch)
}

fun undoImport(snapshot: Snapshot, batchId: String): Snapshot = snapshot.copy(
    trips = snapshot.trips.filterNot { it.importBatchId == batchId }, batches = snapshot.batches.filterNot { it.id == batchId },
)

fun formattingPrompt(stations: List<Station>): String = """
Convert my travel records to farenough import JSON. Ask clarifying questions before producing the final JSON if any year, date, station abbreviation or journey boundary is unclear. I will paste my records after this prompt.

Each arrow represents a separate rail journey, including line changes inside the paid area. Preserve journey order and every repeated journey. Never invent missing trips, dates, stations, times or fares. Clarify month/year rollover. Do not calculate fares. Resolve personal abbreviations with me; do not pass them to the app. Use an official station code from the catalogue below whenever a station name is ambiguous.

Return only this version 1 structure, with one object per journey:
{"format":"farenough-trips","version":1,"trips":[{"date":"YYYY-MM-DD","origin":"official name or code","destination":"official name or code","note":"optional note","sourceText":"original journey text"}]}

Only date, origin and destination are required. Optional note and sourceText must be strings. Do not add other properties. Do not include a journey until its required information is resolved. Before the final JSON, tell me the total journey count so I can check it.

Station catalogue:
${stations.joinToString("\n") { "${it.name}: ${it.codes.joinToString(" / ")}" }}

My records follow:
""".trimIndent()
