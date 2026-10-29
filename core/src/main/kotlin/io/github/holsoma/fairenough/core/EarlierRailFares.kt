package io.github.holsoma.fairenough.core

/** Conservative historical inference, not a dated calculator observation. See DATA_SOURCES.md. */
internal object EarlierRailFares {
    // Start after Hume opened, so all 181 earlier endpoints were in service.
    const val validFrom = "2025-03-01"
    const val tariffChange = "2025-12-27"
    const val validUntil = "2026-07-11"
    const val source = "Adult fare derived for the pre-July network"
    const val version = "pre-ccl6-2025-12-27-v1"
    const val previousVersion = "pre-ccl6-2024-12-28-v1"
    private val newStations = setOf("CC30", "CC31", "CC32")

    // PTC adult card fares, all other timings, effective 27 December 2025.
    // https://data.gov.sg/datasets/d_2ca29f673b89e0cfddbb7c74516fa4d3/view
    private val bandCents = listOf(128, 138, 149, 159, 168, 175, 182, 186, 190, 194, 198, 202, 207,
        211, 215, 220, 224, 227, 230, 233, 236, 238, 240, 242, 243, 244, 245, 246, 247, 248,
        249, 250, 251, 252, 253, 254, 255, 256, 257)

    // Government Gazette No. 4168 (27 December 2024), Part 2, pages 7-8, all other timings.
    private val previousBandCents = listOf(119, 129, 140, 150, 159, 166, 173, 177, 181, 185, 189, 193, 198,
        202, 206, 210, 214, 217, 220, 223, 226, 228, 230, 232, 233, 234, 235, 236, 237, 238,
        239, 240, 241, 242, 243, 244, 245, 246, 247)

    fun bandFare(metres: Int, date: String = tariffChange): Int {
        require(metres > 0)
        val index = if (metres <= 3200) 0 else ((metres - 3201) / 1000 + 1).coerceAtMost(bandCents.lastIndex)
        return (if (date < tariffChange) previousBandCents else bandCents)[index]
    }

    // A future network/table must be reviewed separately. Never extend a new snapshot implicitly.
    fun supports(data: RailFareDataset): Boolean = data.id == "lta-2026-10-29-adult" &&
        data.validFrom == "2026-07-12" && data.validUntil == "2026-12-25" && data.retrieved == "2026-10-29" &&
        data.stationIds.size == 184 && data.stationIds.containsAll(newStations + setOf("CC29", "CC33")) &&
        data.pairs.all { it.distanceMetres % 100 == 0 }

    fun eligible(pair: RailFarePair, distance: (String, String) -> Int): Boolean {
        if (pair.origin in newStations || pair.destination in newStations) return false
        // Do not extrapolate the eight observed departures from the general fare bands.
        if (pair.cents != bandFare(pair.distanceMetres)) return false
        val a = pair.origin
        val b = pair.destination
        val u = "CC29" // HarbourFront
        val v = "CC33" // Marina Bay
        // Any path using CCL6 between old stations must traverse both ends. Current shortest
        // distances are lower bounds for its three segments, even if a segment uses CCL6 too.
        val viaExtension = minOf(distance(a, u) + distance(u, v) + distance(v, b),
            distance(a, v) + distance(v, u) + distance(u, b))
        // Four displayed values have 100 m precision. A 1 km exclusion margin is deliberately
        // larger than their combined rounding uncertainty. These sums never become fare distances.
        return viaExtension - pair.distanceMetres > 1000
    }
}
