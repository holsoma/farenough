package io.github.holsoma.fairenough.core

import kotlin.test.*

class RailFareTest {
    private val data = RailFareDataset(1, "test-2026", "2026-07-12", "2026-12-25", "2026-10-29", "LTA adult card fare",
        listOf("A", "B", "C"), listOf(RailFarePair("A", "B", 3200, 128), RailFarePair("B", "C", 2100, 128), RailFarePair("A", "C", 5200, 149)))
    private val table = RailFareTable(data)
    private val engine = FareEngine(automatic = table)
    private fun trip(date: String = "2026-10-29", origin: String = "A", destination: String = "C") = Trip(date = date, origin = origin, destination = destination)

    @Test fun `whole journey quote takes precedence over adding rounded adjacent distances`() {
        val quote = engine.quote(trip(), emptyList())!!
        assertEquals(149, quote.cents)
        assertEquals(5200, quote.distanceMetres)
        assertNotEquals(3200 + 2100, quote.distanceMetres)
        assertEquals("5.2 km", railDistance(quote.distanceMetres!!))
        assertEquals(quote, engine.quote(trip(origin = "C", destination = "A"), emptyList()))
    }

    @Test fun `coverage dates are inclusive and do not apply current rates to other periods`() {
        assertNotNull(engine.quote(trip(data.validFrom), emptyList()))
        assertNotNull(engine.quote(trip(data.validUntil), emptyList()))
        assertNull(engine.quote(trip("2026-07-11"), emptyList()))
        assertNull(engine.quote(trip("2026-12-26"), emptyList()))
        assertNull(engine.quote(trip("2026-02-30"), emptyList()))
        assertNull(engine.quote(trip(origin = "unknown"), emptyList()))
        assertNull(engine.quote(trip(destination = "A"), emptyList()))
    }

    @Test fun `saved fare overrides remain directional and individual corrections take precedence`() {
        val saved = RouteFare(origin = "A", destination = "C", start = "2026-01-01", end = "2026-12-31", cents = 180)
        val quote = engine.quote(trip(), listOf(saved))!!
        assertEquals(180, quote.cents)
        assertEquals(5200, quote.distanceMetres)
        assertEquals(149, engine.quote(trip(origin = "C", destination = "A"), listOf(saved))?.cents)
        val corrected = engine.assess(trip().copy(correctionCents = 99, correctionReason = "Discount"), listOf(saved))
        assertEquals(180, corrected.fareCents)
        assertEquals(99, corrected.effectiveFare)
        assertEquals("Discount", corrected.correctionReason)
        val outside = engine.quote(trip("2026-07-01"), listOf(saved))!!
        assertEquals(180, outside.cents)
        assertNull(outside.distanceMetres)
    }

    @Test fun `upgrade fills unpriced journeys without changing identifiers saved fares or corrections`() {
        val missing = trip().copy(note = "Original note", correctionCents = 90, correctionReason = "Known discount", edited = true)
        val priced = trip().copy(fareCents = 177, fareSource = "Earlier checked fare", fareVersion = "earlier")
        val outside = trip("2025-10-01").copy(fareSource = "Previous import awaiting fare")
        val original = Snapshot(trips = listOf(missing, priced, outside), preferences = Preferences("dark", listOf("A|C")))
        val updated = priceMissingTrips(original, engine)
        assertEquals(missing.copy(fareCents = 149, fareSource = data.source, fareVersion = data.id), updated.trips[0])
        assertEquals(priced, updated.trips[1])
        assertEquals(outside, updated.trips[2])
        assertEquals(original.preferences, updated.preferences)
        assertEquals(updated, priceMissingTrips(updated, engine))
    }

    @Test fun `imported journeys receive automatic fares while unknown dates stay explicit`() {
        val stations = data.stationIds.map { Station(it, it, listOf(it)) }
        val text = """{"format":"farenough-trips","version":1,"trips":[{"date":"2026-10-29","origin":"A","destination":"C"},{"date":"2025-10-29","origin":"C","destination":"A"}]}"""
        val review = reviewImport(text, stations, Snapshot(), engine)
        assertEquals(149, review.rows[0].trip?.fareCents)
        assertNull(review.rows[1].trip?.fareCents)
        val imported = commitImport(review, setOf(0,1), Snapshot(), "now")
        assertEquals(1, compare(PassPeriod(start="2026-10-01",end="2026-10-31"), imported.trips).pricedCount)
    }

    @Test fun `incomplete duplicate or malformed fare data is rejected`() {
        assertFails { RailFareTable(data.copy(pairs = data.pairs.dropLast(1))) }
        assertFails { RailFareTable(data.copy(pairs = data.pairs + data.pairs.first().copy(origin = "B", destination = "A"))) }
        assertFails { RailFareTable(data.copy(pairs = data.pairs.map { it.copy(distanceMetres = -1) })) }
        assertFails { RailFareTable(data.copy(stationIds = data.stationIds + "A")) }
        assertFails { RailFareTable(data.copy(validUntil = "2026-01-01")) }
    }

    @Test fun `period distance counts repeated journeys and reports incomplete date coverage`() {
        val trips = listOf(trip(), trip(origin = "C", destination = "A"), trip("2025-10-29"))
        assertEquals(RailDistanceTotal(10400, 2, 3), railDistanceTotal(trips, table))
        assertEquals("10.4 km", railDistance(railDistanceTotal(trips, table).metres))
        assertEquals(RailDistanceTotal(0, 0, 0), railDistanceTotal(emptyList(), table))
    }
}
