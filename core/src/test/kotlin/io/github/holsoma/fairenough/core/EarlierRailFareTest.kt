package io.github.holsoma.fairenough.core

import java.io.File
import kotlin.test.*

class EarlierRailFareTest {
    private val directory = File(checkNotNull(System.getProperty("rail.data.directory")))
    private val data = ExchangeJson.decodeFromString<RailFareDataset>(File(directory, "rail-fares.json").readText())
    private val table = RailFareTable(data)
    private val engine = FareEngine(automatic = table)
    private fun trip(a: String = "NE18", b: String = "DT14", date: String = "2026-06-02") = Trip(date = date, origin = a, destination = b)

    @Test fun `reported June journeys receive fares and retain original records on upgrade`() {
        val first = trip().copy(note = "Original note", sourceText = "2 June Punggol Coast to Bugis", importBatchId = "batch")
        val second = trip("DT14", "BP6")
        val old = Snapshot(passes = listOf(PassPeriod(start = "2026-06-01", end = "2026-06-30")), trips = listOf(first, second))
        val updated = priceMissingTrips(old, engine)
        assertEquals(listOf(211, 211), updated.trips.map { it.fareCents })
        assertEquals(listOf(first.id, second.id), updated.trips.map { it.id })
        assertEquals(first.copy(fareCents = 211, fareSource = EarlierRailFares.source, fareVersion = EarlierRailFares.version), updated.trips.first())
        assertEquals(422L, compare(old.passes.single(), updated.trips).totalCents)
        assertEquals(RailDistanceTotal(31_100, 2, 2), railDistanceTotal(updated.trips, table))
        assertEquals(updated, priceMissingTrips(updated, engine))
    }

    @Test fun `earlier coverage applies across the network in both directions with bounded dates`() {
        assertEquals("2025-03-01", table.earlierValidFrom)
        assertEquals(16_106, table.earlierPairCount)
        for (pair in data.pairs) {
            val early = table.quote(trip(pair.origin, pair.destination))
            assertEquals(early, table.quote(trip(pair.destination, pair.origin)))
            assertEquals(early, table.quote(trip(pair.origin, pair.destination, "2025-12-27")))
            assertEquals(early, table.quote(trip(pair.origin, pair.destination, "2026-07-11")))
            val previous = table.quote(trip(pair.origin, pair.destination, "2025-12-26"))
            assertEquals(early == null, previous == null)
            assertEquals(previous, table.quote(trip(pair.origin, pair.destination, "2025-03-01")))
            if (previous != null) assertEquals(EarlierRailFares.previousVersion, previous.version)
            assertNull(table.quote(trip(pair.origin, pair.destination, "2025-02-28")))
            assertNull(table.quote(trip(pair.origin, pair.destination, "2026-12-26")))
            val current = assertNotNull(table.quote(trip(pair.origin, pair.destination, "2026-07-12")))
            assertEquals(pair.cents, current.cents)
            assertEquals(pair.distanceMetres, current.distanceMetres)
            if (early != null) {
                assertEquals(current.cents, early.cents)
                assertEquals(current.distanceMetres, early.distanceMetres)
                assertNotEquals(current.version, early.version)
            }
        }
    }

    @Test fun `earlier tariff changes on the correct day instead of reusing 2026 fares`() {
        val dec26 = assertNotNull(table.quote(trip(date = "2025-12-26")))
        val dec27 = assertNotNull(table.quote(trip(date = "2025-12-27")))
        assertEquals(202, dec26.cents)
        assertEquals(211, dec27.cents)
        assertEquals(dec26.distanceMetres, dec27.distanceMetres)
        for ((metres, cents) in listOf(3200 to 119, 3300 to 129, 16_200 to 202, 16_300 to 206,
            17_200 to 206, 17_300 to 210, 40_200 to 246, 40_300 to 247)) {
            assertEquals(cents, EarlierRailFares.bandFare(metres, "2025-09-01"))
        }
    }

    @Test fun `PTC examples of changed fares and unopened stations stay unpriced before July`() {
        // PTC's published before/after examples independently confirm these routes changed.
        // https://ask.gov.sg/ptc/questions/cmq0mmuip003j4f01c53tmnco
        val changed = listOf(Triple("CC29", "CC34", 149), Triple("CC26", "CC33", 175),
            Triple("CC27", "TE22", 175), Triple("CC28", "TE26", 202))
        for ((a, b, currentCents) in changed) {
            assertNull(table.quote(trip(a, b)))
            assertEquals(currentCents, table.quote(trip(a, b, "2026-07-12"))?.cents)
        }
        for (station in listOf("CC30", "CC31", "CC32")) for (other in data.stationIds) {
            assertNull(table.quote(trip(station, other)))
        }
        // Calculator fare differs from the general band: do not infer its earlier fare.
        assertNull(table.quote(trip("CC15", "NS19")))
        assertNull(table.quote(trip(date = "2026-02-30")))
        assertNull(table.quote(trip("NE18", "NE18")))
    }

    @Test fun `new snapshot versions cannot inherit the historical inference`() {
        assertNull(RailFareTable(data.copy(id = "future-table")).quote(trip()))
        assertNull(RailFareTable(data.copy(retrieved = "2026-11-01")).quote(trip()))
        assertEquals(0, RailFareTable(data.copy(validFrom = "2026-08-01")).earlierPairCount)
    }

    @Test fun `PTC bands and rounding margin reject uncertain inferences`() {
        for ((metres, cents) in listOf(1 to 128, 3200 to 128, 3300 to 138, 5200 to 149,
            5300 to 159, 15_200 to 207, 15_300 to 211, 16_200 to 211, 16_300 to 215,
            40_200 to 256, 40_300 to 257, 50_000 to 257)) assertEquals(cents, EarlierRailFares.bandFare(metres))
        val pair = RailFarePair("A", "B", 2000, 128)
        // Three lower bounds of 1 km leave exactly the 1 km margin, which must be rejected.
        assertFalse(EarlierRailFares.eligible(pair) { _, _ -> 1000 })
        assertTrue(EarlierRailFares.eligible(pair) { _, _ -> 1100 })
        assertFalse(EarlierRailFares.eligible(pair.copy(cents = 127)) { _, _ -> 1100 })
    }

    @Test fun `June imports are priced and existing manual fares and corrections are retained`() {
        val stations = ExchangeJson.decodeFromString<List<Station>>(File(directory, "stations.json").readText())
        val input = """{"format":"farenough-trips","version":1,"trips":[{"date":"2026-06-02","origin":"Punggol Coast","destination":"Bugis"},{"date":"2026-06-02","origin":"Bugis","destination":"BP6 / DT1"}]}"""
        val review = reviewImport(input, stations, Snapshot(), engine)
        assertEquals(2, review.validCount)
        assertEquals(listOf(211, 211), review.rows.map { it.trip?.fareCents })
        val priced = trip().copy(fareCents = 177, fareSource = "Checked by user", fareVersion = "manual")
        val corrected = trip().copy(correctionCents = 50, correctionReason = "Earlier discount")
        val saved = RouteFare(origin = "DT14", destination = "BP6", start = "2026-01-01", end = "2026-12-01", cents = 199)
        val updated = priceMissingTrips(Snapshot(trips = listOf(priced, corrected, trip("DT14", "BP6")), routeFares = listOf(saved)), engine)
        assertEquals(priced, updated.trips[0])
        assertEquals(50, updated.trips[1].effectiveFare)
        assertEquals("Earlier discount", updated.trips[1].correctionReason)
        assertEquals(199, updated.trips[2].fareCents)
    }
}
