package io.github.holsoma.fairenough.core

import java.io.File
import kotlin.test.*

class BundledRailFareTest {
    private val directory = File(checkNotNull(System.getProperty("rail.data.directory")))
    private val data = ExchangeJson.decodeFromString<RailFareDataset>(File(directory, "rail-fares.json").readText())
    private val table = RailFareTable(data)

    @Test fun `bundled coverage matches every canonical app station and every pair`() {
        val stations = ExchangeJson.decodeFromString<List<Station>>(File(directory, "stations.json").readText())
        assertEquals(stations.map { it.id }.toSet(), data.stationIds.toSet())
        assertEquals(184, stations.size)
        assertEquals(16_836, data.pairs.size)
        for (pair in data.pairs) {
            val forward = table.quote(Trip(date = "2026-10-29", origin = pair.origin, destination = pair.destination))
            val reverse = table.quote(Trip(date = "2026-10-29", origin = pair.destination, destination = pair.origin))
            assertNotNull(forward)
            assertEquals(forward, reverse)
            assertTrue(forward.cents in 124..257)
        }
    }

    @Test fun `selected official quotes retain their exact distance and fare`() {
        // Independently recorded LTA calculator responses, including a rounded-distance boundary
        // and a result whose returned fare differs from applying the generic distance band.
        val examples = listOf(
            RailFarePair("NS19", "DT14", 5300, 159),
            RailFarePair("BP6", "CC4", 16200, 211),
            RailFarePair("DT10", "CC4", 5200, 149),
            RailFarePair("CC29", "CC34", 4900, 149),
            RailFarePair("EW18", "CC15", 11500, 190),
            RailFarePair("CC15", "NS19", 2100, 124),
            RailFarePair("EW33", "CG2", 47700, 257),
        )
        for (example in examples) {
            val quote = table.quote(Trip(date = "2026-10-29", origin = example.origin, destination = example.destination))!!
            assertEquals(example.distanceMetres, quote.distanceMetres, "${example.origin} → ${example.destination}")
            assertEquals(example.cents, quote.cents, "${example.origin} → ${example.destination}")
        }
    }
}
