package io.github.holsoma.fairenough.core

import java.io.File
import kotlin.test.*
import kotlinx.serialization.encodeToString

class StationImportTest {
    private val directory = File(checkNotNull(System.getProperty("rail.data.directory")))
    private val stations = ExchangeJson.decodeFromString<List<Station>>(File(directory, "stations.json").readText())
    private val engine = FareEngine(automatic = RailFareTable(ExchangeJson.decodeFromString(File(directory, "rail-fares.json").readText())))
    private fun json(origin: String, destination: String) = ExchangeJson.encodeToString(
        TripExchange("farenough-trips", 1, listOf(ExchangeTrip("2026-10-29", origin, destination))))

    @Test fun `reported Bukit Panjang combined origin resolves and receives its fare`() {
        val review = reviewImport(json("BP6 / DT1", "CC4 / DT15"), stations, Snapshot(), engine)
        assertEquals(1, review.validCount)
        val trip = assertNotNull(review.rows.single().trip)
        assertEquals("BP6", trip.origin)
        assertEquals("CC4", trip.destination)
        assertEquals(211, trip.fareCents)
    }

    @Test fun `every interchange label is accepted only as its canonical station`() {
        for (station in stations) {
            for (token in listOf(station.id, station.name, station.codes.joinToString(" / "), "${station.name} (${station.codes.joinToString("/")})", "${station.name}: ${station.codes.joinToString(" / ")}")) {
                assertEquals(station.id, resolveStation(token, stations)?.id, token)
            }
        }
        assertEquals("BP6", resolveStation(" bp6\u00a0/\u00a0dt1 ", stations)?.id)
        assertEquals("NS19", resolveStation("Toa   Payoh", stations)?.id)
    }

    @Test fun `mixed stations unknown codes and shorthand are not silently accepted`() {
        for (token in listOf("BP6 / CC4", "BP6 / XX99", "BP6 /", "/ DT1", "Bukit Panjang (CC4)", "Bukit Panjang: BP6 / CC4", "unknown (BP6)", "Stevens / Promenade", "BP", "SV", "PC")) {
            assertNull(resolveStation(token, stations), token)
            assertEquals(0, reviewImport(json(token, "CC4"), stations, Snapshot(), engine).validCount, token)
        }
    }

    @Test fun `equivalent formats preserve duplicate detection and same-station rejection`() {
        val initial = reviewImport(json("Bukit Panjang", "Promenade"), stations, Snapshot(), engine)
        val saved = commitImport(initial, setOf(0), Snapshot(), "now")
        val repeated = reviewImport(json("BP6 / DT1", "Promenade (DT15 / CC4)"), stations, saved, engine)
        assertTrue(repeated.alreadyImported)
        assertEquals(1, repeated.rows.single().matchesExisting)
        assertEquals(0, reviewImport(json("BP6 / DT1", "DT1"), stations, Snapshot(), engine).validCount)
    }

    @Test fun `prompt provides one valid unique output code per catalogue row`() {
        val prompt = formattingPrompt(stations)
        val rows = prompt.substringAfter("Output code | Official name | Other codes for reference only\n").substringBefore("\n\nMy records follow:").lines()
        assertEquals(184, rows.size)
        val codes = rows.map { it.substringBefore(" | ") }
        assertEquals(codes.size, codes.toSet().size)
        for ((row, station) in rows.zip(stations)) {
            assertEquals(station.id, row.substringBefore(" | "))
            assertTrue(station.id in station.codes)
            assertEquals(station.id, resolveStation(row.substringBefore(" | "), stations)?.id)
        }
        assertContains(prompt, "BP6 | Bukit Panjang | DT1")
        assertContains(prompt, "copy exactly ONE output code")
        assertContains(prompt, "ask me and wait rather than omitting that journey")
        assertFalse(prompt.contains("BP6 / DT1"))
    }
}
