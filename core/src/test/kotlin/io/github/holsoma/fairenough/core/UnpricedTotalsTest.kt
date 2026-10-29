package io.github.holsoma.fairenough.core

import kotlin.test.*

class UnpricedTotalsTest {
    private val pass = PassPeriod(start = "2026-06-01", end = "2026-06-30")
    private fun trip(date: String = "2026-06-02", cents: Int? = null) = Trip(date = date, origin = "A", destination = "B", fareCents = cents)

    @Test fun `two unpriced journeys do not present a zero fare total`() {
        val comparison = compare(pass, listOf(trip(), trip()))
        assertTrue(comparison.needsFares)
        assertFalse(comparison.complete)
        assertEquals(2, comparison.tripCount)
        assertEquals("Not priced", fareTotalText(comparison.totalCents, comparison.pricedCount, comparison.tripCount))
        assertNull(comparison.averageCents)
        assertNull(comparison.averageTripsNeeded)
    }

    @Test fun `confirmed zero fares and an empty period remain different from missing fares`() {
        val free = compare(pass, listOf(trip().copy(correctionCents = 0, correctionReason = "Confirmed free journey")))
        assertFalse(free.needsFares)
        assertTrue(free.complete)
        assertEquals("S$0.00", fareTotalText(free.totalCents, free.pricedCount, free.tripCount))
        assertFalse(compare(pass, emptyList()).needsFares)
        assertEquals("S$0.00", fareTotalText(0, 0, 0))
    }

    @Test fun `daily and cumulative coverage distinguish missing zero and paid amounts`() {
        val days = dailyStatistics(listOf(trip(), trip(), trip("2026-06-03", 0), trip("2026-06-04", 128)), "2026-06-01", "2026-06-04")
        assertEquals(listOf(0, 0, 1, 1), days.map { it.pricedCount })
        assertEquals(listOf(0, 2, 3, 4), days.map { it.cumulativeTrips })
        assertEquals(listOf(0, 0, 1, 2), days.map { it.cumulativePricedCount })
        assertEquals(listOf("S$0.00", "Not priced", "S$0.00", "S$1.28"), days.map { fareTotalText(it.cumulativeCents, it.cumulativePricedCount, it.cumulativeTrips) })
    }
}
