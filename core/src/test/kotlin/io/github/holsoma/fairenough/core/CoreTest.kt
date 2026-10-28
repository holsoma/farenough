package io.github.holsoma.fairenough.core

import kotlin.test.*
import kotlinx.serialization.encodeToString

class CoreTest {
    private val stations = listOf(Station("A", "Alpha", listOf("AA1")), Station("B", "Beta", listOf("BB2")), Station("C", "Gamma", listOf("CC3")))
    private val pass = PassPeriod(id = "p", start = "2026-09-28", end = "2026-10-27")
    private fun trip(cents: Int? = 200, date: String = "2026-10-01", origin: String = "A", destination: String = "B") = Trip(date = date, origin = origin, destination = destination, fareCents = cents)
    private fun json(rows: List<ExchangeTrip>) = ExchangeJson.encodeToString(TripExchange("fairenough-trips", 1, rows))

    @Test fun `inclusive dates and exact break even`() {
        val trips = List(22) { trip() } + trip(date = pass.start) + trip(date = pass.end) + trip(date = "2026-09-27") + trip(date = "2026-10-28")
        val c = compare(pass, trips)
        assertEquals(24, c.tripCount); assertEquals(4800, c.totalCents); assertEquals(0, c.remaining); assertEquals(0, c.averageTripsNeeded)
    }
    @Test fun `missing is not zero and target is a scenario`() {
        val c = compare(pass, listOf(trip(), trip(null), trip(0)))
        assertFalse(c.complete); assertEquals(2, c.pricedCount); assertEquals(100.0, c.averageCents)
        assertEquals(46, c.averageTripsNeeded); assertEquals(23, c.tripsAt(200)); assertNull(c.tripsAt(0))
    }
    @Test fun `empty data has no average`() { assertNull(compare(pass, emptyList()).averageTripsNeeded) }
    @Test fun `correction only affects one trip`() {
        val original = trip()
        assertEquals(350, compare(pass, listOf(original, original.copy(id = newId(), correctionCents = 150))).totalCents)
    }
    @Test fun `opposite directions rank separately and ties survive`() {
        val stats = routeStatistics(listOf(trip(), trip(), trip(origin = "B", destination = "A"), trip(origin = "C", destination = "A")))
        assertEquals(3, stats.size); assertEquals(2, stats.first().count); assertEquals(2, stats.count { it.count == 1 })
    }
    @Test fun `pass overlap rejected but consecutive passes accepted`() {
        assertNotNull(validatePass(pass.copy(id = "q", start = "2026-10-27", end = "2026-11-26"), listOf(pass)))
        assertNull(validatePass(pass.copy(id = "q", start = "2026-10-28", end = "2026-11-27"), listOf(pass)))
    }
    @Test fun `unknown years and invalid calendar dates rejected`() {
        val review = reviewImport(json(listOf(ExchangeTrip("28 SEP", "Alpha", "Beta"), ExchangeTrip("2026-02-30", "Alpha", "Beta"))), stations, Snapshot())
        assertEquals(0, review.validCount)
    }
    @Test fun `personal aliases are not guessed`() {
        assertNull(resolveStation("AL", stations)); assertEquals("A", resolveStation(" aa1 ", stations)?.id)
        assertNull(resolveStation("Alpha", stations + Station("D", "Alpha", listOf("DD1"))))
    }
    @Test fun `same station and unsupported version rejected`() {
        assertEquals(0, reviewImport(json(listOf(ExchangeTrip("2026-10-01", "Alpha", "AA1"))), stations, Snapshot()).validCount)
        assertFailsWith<ImportProblem> { reviewImport("{\"format\":\"fairenough-trips\",\"version\":2,\"trips\":[]}", stations, Snapshot()) }
    }
    @Test fun `repeated journeys within a batch are preserved and exact reimport detected`() {
        val text = json(List(2) { ExchangeTrip("2026-10-01", "Alpha", "Beta") })
        val review = reviewImport(text, stations, Snapshot())
        val saved = commitImport(review, setOf(0, 1), Snapshot(), "2026-10-28T00:00:00Z")
        assertEquals(2, saved.trips.size)
        val again = reviewImport(text, stations, saved)
        assertTrue(again.alreadyImported); assertEquals(2, again.rows.first().matchesExisting)
        assertFails { commitImport(again, setOf(0), saved, "now") }
    }
    @Test fun `partial import does not mark complete source as imported`() {
        val text = json(listOf(ExchangeTrip("2026-10-01", "A", "B"), ExchangeTrip("2026-10-02", "B", "A")))
        val saved = commitImport(reviewImport(text, stations, Snapshot()), setOf(0), Snapshot(), "now")
        assertFalse(reviewImport(text, stations, saved).alreadyImported)
    }
    @Test fun `invalid rows cannot be committed and undo only removes its batch`() {
        val base = Snapshot(trips = listOf(trip()))
        val review = reviewImport(json(listOf(ExchangeTrip("bad", "A", "B"), ExchangeTrip("2026-10-02", "B", "A"))), stations, base)
        assertFails { commitImport(review, setOf(0), base, "now") }
        val saved = commitImport(review, setOf(1), base, "now")
        assertEquals(base, undoImport(saved, saved.batches.single().id))
    }
    @Test fun `fare lookup respects date and direction`() {
        val fare = RouteFare(origin = "A", destination = "B", start = "2025-12-27", end = "2026-12-25", cents = 200)
        val e = FareEngine()
        assertEquals(200, e.quote(trip(), listOf(fare))?.cents)
        assertNull(e.quote(trip(date = "2026-12-26"), listOf(fare)))
        assertNull(e.quote(trip(origin = "B", destination = "A"), listOf(fare)))
        assertEquals(150, e.assess(trip().copy(correctionCents = 150), listOf(fare)).effectiveFare)
    }
    @Test fun `overlapping route fares rejected`() {
        val fare = RouteFare(origin = "A", destination = "B", start = "2026-01-01", end = "2026-12-31", cents = 200)
        assertNotNull(validateRouteFare(fare.copy(id = newId()), listOf(fare), stations))
    }
    @Test fun `backup round trip and invalid duplicate identity`() {
        val snapshot = Snapshot(passes = listOf(pass), trips = listOf(trip()), preferences = Preferences("dark", listOf("A|B")))
        assertEquals(snapshot, decodeBackup(encodeBackup(snapshot, "now"), stations))
        assertFails { decodeBackup(encodeBackup(snapshot.copy(trips = snapshot.trips + snapshot.trips), "now"), stations) }
    }
    @Test fun `csv quotes data and protects formulas`() {
        val csv = exportCsv(listOf(trip().copy(note = "=HYPERLINK(\"bad\")")), stations)
        assertTrue(csv.contains("\"'=HYPERLINK(\"\"bad\"\")\""))
    }
    @Test fun `money uses exact cents`() {
        assertEquals(128, parseMoney("1.28")); assertNull(parseMoney("1.285")); assertNull(parseMoney("-2")); assertEquals("S$48.00", money(4800))
    }
    @Test fun `sample keeps all dates and trips`() {
        // Synthetic dates preserve the acceptance totals without publishing private history.
        val counts = (28..30).map { "09-$it" to 2 } + (1..20).map { "10-%02d".format(it) to if (it == 20) 1 else 2 }
        val rows = counts.flatMap { (day, count) -> List(count) { ExchangeTrip("2026-$day", "A", "B") } }
        val review = reviewImport(json(rows), stations, Snapshot())
        val trips = commitImport(review, rows.indices.toSet(), Snapshot(), "now").trips
        assertEquals(45, trips.size); assertEquals(23, trips.map { it.date }.distinct().size)
        assertEquals(6, trips.count { it.date.startsWith("2026-09") }); assertEquals(39, trips.count { it.date.startsWith("2026-10") })
        val daily = dailyStatistics(trips, pass.start, pass.end)
        assertEquals(30, daily.size); assertEquals(45, daily.sumOf { it.trips }); assertEquals(0, daily.last().cumulativeCents)
    }
}
