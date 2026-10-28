package io.github.holsoma.fairenough.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

@Serializable
data class Backup(val format: String = "fairenough-backup", val version: Int = 1, val createdAt: String, val data: Snapshot)

fun encodeBackup(data: Snapshot, time: String): String = ExchangeJson.encodeToString(Backup(createdAt = time, data = data))

fun decodeBackup(text: String, stations: List<Station>): Snapshot {
    require(text.length <= 20_000_000) { "Backup exceeds 20 MB." }
    val backup = ExchangeJson.decodeFromString<Backup>(text)
    require(backup.format == "fairenough-backup" && backup.version == 1) { "Unsupported backup format or version." }
    val s = backup.data
    require(s.passes.map { it.id }.distinct().size == s.passes.size && s.trips.map { it.id }.distinct().size == s.trips.size && s.routeFares.map { it.id }.distinct().size == s.routeFares.size && s.batches.map { it.id }.distinct().size == s.batches.size) { "The backup contains duplicate record IDs." }
    s.passes.forEach { require(validatePass(it, s.passes) == null) { "Invalid or overlapping pass periods." } }
    s.routeFares.forEach { require(validateRouteFare(it, s.routeFares, stations) == null) { "Invalid or overlapping saved fares." } }
    val ids = stations.map { it.id }.toSet()
    s.trips.forEach {
        require(validDate(it.date) && it.origin in ids && it.destination in ids && it.origin != it.destination) { "A journey has an invalid date or station." }
        require(it.fareCents == null || it.fareCents in 0..1_000_000) { "Invalid fare amount." }
        require(it.correctionCents == null || it.correctionCents in 0..1_000_000) { "Invalid correction amount." }
        require(it.importBatchId == null || s.batches.any { b -> b.id == it.importBatchId && it.id in b.tripIds }) { "A journey refers to an invalid import batch." }
    }
    require(s.preferences.appearance in listOf("system", "light", "dark")) { "Invalid appearance preference." }
    require(s.preferences.favourites.all { key -> val p = key.split('|'); p.size == 2 && p[0] in ids && p[1] in ids && p[0] != p[1] }) { "A favourite has an invalid station." }
    return s
}

fun exportCsv(trips: List<Trip>, stations: List<Station>): String {
    val names = stations.associate { it.id to it.name }
    fun cell(s: String): String {
        // Prevent spreadsheet formula execution when a note is opened in Excel.
        val safe = if (s.trimStart().firstOrNull() in listOf('=', '+', '-', '@', '\t', '\r')) "'$s" else s
        return "\"${safe.replace("\"", "\"\"")}\""
    }
    return "date,origin,destination,fare_cents,source,correction_cents,note\r\n" + trips.sortedBy { it.date }.joinToString("\r\n") {
        listOf(it.date, names[it.origin] ?: it.origin, names[it.destination] ?: it.destination, it.fareCents?.toString().orEmpty(), it.fareSource, it.correctionCents?.toString().orEmpty(), it.note).joinToString(",", transform = ::cell)
    }
}
