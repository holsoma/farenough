package io.github.holsoma.fairenough.core

import kotlin.test.*

class MonthStatisticsTest {
    private fun trip(date: String, cents: Int?, correction: Int? = null) = Trip(date = date, origin = "A", destination = "B", fareCents = cents, correctionCents = correction)
    @Test fun `calendar totals cross years count repeated rides and use corrections`() {
        val months = monthlyStatistics(listOf(trip("2025-12-31", 200), trip("2026-01-01", 200), trip("2026-01-01", 200, 50), trip("2026-01-02", 0)))
        assertEquals(listOf("2026-01", "2025-12"), months.map { it.month })
        assertEquals(MonthStats("2026-01", 3, 3, 2, 250), months.first())
        assertEquals(83L, months.first().averageFare)
        assertEquals(50L, monthlyFareChange(months[0], months[1]))
    }
    @Test fun `missing fares prevent monetary comparisons without removing trip counts`() {
        val months = monthlyStatistics(listOf(trip("2026-03-01", null), trip("2026-03-02", 200), trip("2026-02-01", 0)))
        assertEquals(2, months.first().trips)
        assertEquals(1, months.first().pricedTrips)
        assertEquals(200L, months.first().averageFare)
        assertNull(monthlyFareChange(months[0], months[1]))
        assertEquals(0L, months[1].averageFare)
        assertTrue(months[1].complete)
    }
    @Test fun `empty and entirely unpriced months retain their meaning`() {
        assertTrue(monthlyStatistics(emptyList()).isEmpty())
        val month = monthlyStatistics(listOf(trip("2025-03-01", null))).single()
        assertNull(month.averageFare)
        assertFalse(month.complete)
        assertEquals("Not priced", fareTotalText(month.fareCents, month.pricedTrips, month.trips))
    }
}
