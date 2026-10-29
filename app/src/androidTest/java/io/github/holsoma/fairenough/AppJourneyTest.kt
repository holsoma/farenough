package io.github.holsoma.fairenough

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.holsoma.fairenough.core.*
import io.github.holsoma.fairenough.data.LocalRepository
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import java.io.File
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class AppJourneyTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val app get() = ApplicationProvider.getApplicationContext<android.content.Context>()

    private fun demo(repo: LocalRepository, dark: Boolean = false): Snapshot {
        fun id(name: String) = repo.stations.first { it.name == name }.id
        val pairs = listOf("Toa Payoh" to "Raffles Place", "Raffles Place" to "Toa Payoh", "Jurong East" to "Tampines", "Tampines" to "Jurong East", "Woodlands" to "Marina Bay", "Marina Bay" to "Woodlands")
        val fares = pairs.map { (a, b) -> RouteFare(origin = id(a), destination = id(b), start = "2025-12-27", end = "2026-12-25", cents = 220, source = "Example fare") }
        val trips = (0 until 26).map { index ->
            val pair = pairs[if (index < 16) index % 2 else index % pairs.size]
            FareEngine().assess(Trip(date = LocalDate.of(2026, 10, 1).plusDays((index / 2).toLong()).toString(), origin = id(pair.first), destination = id(pair.second), note = "Demonstration record"), fares)
        }
        return Snapshot(passes = listOf(PassPeriod(id = "demo", start = "2026-10-01", end = "2026-10-31")), trips = trips, routeFares = fares, preferences = Preferences(if (dark) "dark" else "light", listOf("${id("Toa Payoh")}|${id("Raffles Place")}")))
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        val destination = File(app.getExternalFilesDir(null), "screenshots/$name.png")
        destination.parentFile!!.mkdirs()
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        checkNotNull(bitmap)
        destination.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    @Test fun seededAppNavigationAndScreenshots() {
        val repo = LocalRepository(app)
        runBlocking { repo.update { demo(repo) }; repo.selectPass("demo") }
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitUntil(15_000) { compose.onAllNodesWithText("Estimated savings").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("S$9.20").assertIsDisplayed()
            capture("overview")
            it.onActivity { activity ->
                assertTrue(androidx.core.view.WindowCompat.getInsetsController(activity.window, activity.window.decorView).isAppearanceLightStatusBars)
            }
            compose.onNodeWithText("How many more trips?").performScrollTo().performClick()
            compose.onNodeWithText("Your next stop: enough").assertIsDisplayed()
            capture("target")
            compose.onNodeWithContentDescription("Back").performClick()
            compose.onNodeWithText("Trips", useUnmergedTree = true).performClick()
            compose.onNodeWithText("Your journeys").assertIsDisplayed()
            capture("trips")
            compose.onNodeWithText("Import", useUnmergedTree = true).performClick()
            compose.onNodeWithText("Copy formatting prompt").assertIsDisplayed()
            compose.onNodeWithText("Copy formatting prompt").performClick()
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                val clipboard = app.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val prompt = clipboard.primaryClip!!.getItemAt(0).coerceToText(app).toString()
                assertTrue(prompt.contains("BP6 | Bukit Panjang | DT1"))
                assertTrue(prompt.contains("copy exactly ONE output code"))
                assertFalse(prompt.contains("BP6 / DT1"))
            }
            capture("import")
            compose.onNodeWithContentDescription("Back").performClick()
            compose.onNodeWithText("Insights", useUnmergedTree = true).performClick()
            compose.onNodeWithText("Your travels, in view").assertIsDisplayed()
            capture("charts")
            compose.onNodeWithText("Map", useUnmergedTree = true).performClick()
            capture("map")
            compose.onNodeWithContentDescription("Record a journey").performClick()
            compose.onNodeWithText("Travel date").assertExists()
            capture("add-trip")
        }
        // Verify persisted data survives activity recreation, including corrections.
        runBlocking {
            val before = repo.current()
            val restored = decodeBackup(encodeBackup(before, "2026-10-28T00:00:00Z"), repo.stations)
            assertEquals(26, restored.trips.size)
            repo.update { restored.copy(preferences = restored.preferences.copy(appearance = "dark")) }
        }
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitUntil(15_000) { compose.onAllNodesWithText("Estimated savings").fetchSemanticsNodes().isNotEmpty() }
            capture("overview-dark")
            it.onActivity { activity ->
                assertFalse(androidx.core.view.WindowCompat.getInsetsController(activity.window, activity.window.decorView).isAppearanceLightStatusBars)
            }
        }
    }

    @Test fun importTransactionAndUndoPreserveUnrelatedRecords() = runBlocking {
        val repo = LocalRepository(app)
        val original = demo(repo)
        repo.update { original }
        val text = """{"format":"farenough-trips","version":1,"trips":[{"date":"2026-10-20","origin":"Toa Payoh","destination":"Bugis"},{"date":"2026-10-20","origin":"Toa Payoh","destination":"Bugis"}]}"""
        repo.update { commitImport(reviewImport(text, repo.stations, it), setOf(0, 1), it, "2026-10-28T00:00:00Z") }
        val imported = repo.current()
        assertEquals(28, imported.trips.size)
        assertTrue(reviewImport(text, repo.stations, imported).alreadyImported)
        repo.update { undoImport(it, imported.batches.single().id) }
        assertEquals(original.trips.map { it.id }.toSet(), repo.current().trips.map { it.id }.toSet())
    }

    @Test fun createPassRecordJourneyAndImportThroughTheInterface() {
        val repo = LocalRepository(app)
        runBlocking { repo.update { Snapshot() } }
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitUntil(15_000) { compose.onAllNodesWithText("Set up a pass").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Set up a pass").performClick()
            compose.onNodeWithText("First valid day").performTextReplacement("2026-10-01")
            compose.onNodeWithText("Last valid day").performTextReplacement("2026-10-31")
            compose.onNodeWithText("Save pass").performScrollTo().performClick()
            compose.waitUntil(10_000) { runBlocking { repo.current().passes.size == 1 } }
            // The database commit can precede the form's main-thread completion callback.
            compose.waitUntil(15_000) { compose.onAllNodesWithContentDescription("Record a journey").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithContentDescription("Record a journey").performClick()
            compose.onNodeWithText("Travel date").performTextReplacement("2026-10-29")
            compose.onNodeWithText("From", useUnmergedTree = true).performScrollTo().performClick()
            compose.onNodeWithText("Station name or code").performTextInput("Toa Payoh")
            compose.onNode(hasText("Toa Payoh") and hasText("NS19")).performClick()
            compose.onNodeWithText("To", useUnmergedTree = true).performScrollTo().performClick()
            compose.onNodeWithText("Station name or code").performTextInput("Bugis")
            compose.onNode(hasText("Bugis") and hasText("DT14 / EW12")).performClick()
            compose.onNodeWithText("Regular adult fare: S$1.59", substring = true).performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("Rail fare distance: 5.3 km", substring = true).assertIsDisplayed()
            compose.onNodeWithText("This journey's corrected fare, S$ (optional)").performScrollTo().performTextInput("1.50")
            compose.onNodeWithText("Reason for correction").performScrollTo().performTextInput("Example discount")
            compose.onNodeWithText("Save journey").performScrollTo().performClick()
            compose.waitUntil(10_000) { runBlocking { repo.current().trips.size == 1 } }
            compose.waitUntil(15_000) { compose.onAllNodesWithText("S$46.50").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("S$46.50").assertExists()
            compose.onNodeWithText("Trips", useUnmergedTree = true).performClick()
            compose.onNodeWithText("Import", useUnmergedTree = true).performClick()
            val text = """{"format":"farenough-trips","version":1,"trips":[{"date":"2026-10-29","origin":"BP6 / DT1","destination":"CC4 / DT15"},{"date":"2026-10-29","origin":"Stevens (DT10 / TE11)","destination":"Promenade"}]}"""
            compose.onNodeWithText("Normalised JSON").performScrollTo().performTextInput(text)
            compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Review journeys"))
            compose.onNodeWithText("Review journeys").performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithText("2 rows", substring = true).fetchSemanticsNodes().isNotEmpty() }
            compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Import 2 journeys"))
            compose.onNodeWithText("Import 2 journeys").performClick()
            compose.onNodeWithText("Import", useUnmergedTree = true).performClick()
            compose.waitUntil(10_000) { runBlocking { repo.current().trips.size == 3 } }
            val saved = runBlocking { repo.current() }
            assertEquals(150, saved.trips.single { it.correctionCents != null }.effectiveFare)
            assertEquals(159, saved.trips.single { it.correctionCents != null }.fareCents)
            assertEquals(1, saved.trips.count { it.origin == "BP6" && it.destination == "CC4" && it.fareCents == 211 })
            assertEquals(1, saved.trips.count { it.origin == "DT10" && it.destination == "CC4" && it.fareCents == 149 })
        }
    }

    @Test fun removingSavedFareRestoresAutomaticAmountsAndPreservesCorrections() {
        val repo = LocalRepository(app)
        val fare = RouteFare(origin = "NS19", destination = "DT14", start = "2026-10-01", end = "2026-10-31", cents = 200)
        val regular = repo.engine.assess(Trip(date = "2026-10-29", origin = "NS19", destination = "DT14"), listOf(fare))
        val corrected = regular.copy(id = newId(), correctionCents = 150, correctionReason = "Existing correction")
        runBlocking { repo.update { Snapshot(passes = listOf(PassPeriod(start = "2026-10-01", end = "2026-10-31")), routeFares = listOf(fare), trips = listOf(regular, corrected)) } }
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitUntil(15_000) { compose.onAllNodesWithText("More", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("More", useUnmergedTree = true).performClick()
            compose.onNodeWithText("Saved route fares").performScrollTo().performClick()
            compose.onNodeWithText("Toa Payoh → Bugis").performClick()
            compose.onNodeWithText("Remove saved fare").performScrollTo().performClick()
            compose.onNodeWithText("Remove fare").performClick()
            compose.waitUntil(10_000) { runBlocking { repo.current().routeFares.isEmpty() } }
            val saved = runBlocking { repo.current() }
            assertEquals(setOf(regular.id, corrected.id), saved.trips.map { trip -> trip.id }.toSet())
            assertTrue(saved.trips.all { trip -> trip.fareCents == 159 })
            assertEquals(150, saved.trips.single { trip -> trip.id == corrected.id }.effectiveFare)
            assertEquals("Existing correction", saved.trips.single { trip -> trip.id == corrected.id }.correctionReason)
        }
    }

    @Test fun automaticFarePreviewChangesWithStationsAndDate() {
        val repo = LocalRepository(app)
        runBlocking { repo.update { Snapshot(passes = listOf(PassPeriod(start = "2026-10-01", end = "2026-10-31"))) } }
        fun choose(label: String, name: String, codes: String) {
            compose.onNodeWithText(label, useUnmergedTree = true).performScrollTo().performClick()
            compose.onNodeWithText("Station name or code").performTextReplacement(name)
            compose.onNode(hasText(name) and hasText(codes)).performClick()
        }
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitUntil(15_000) { compose.onAllNodesWithContentDescription("Record a journey").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithContentDescription("Record a journey").performClick()
            compose.onNodeWithText("Travel date").performTextReplacement("2026-10-29")
            choose("From", "Bukit Panjang", "BP6 / DT1")
            choose("To", "Promenade", "CC4 / DT15")
            compose.onNodeWithText("Regular adult fare: S$2.11", substring = true).performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("Rail fare distance: 16.2 km", substring = true).assertIsDisplayed()
            choose("From", "Stevens", "DT10 / TE11")
            compose.onNodeWithText("Regular adult fare: S$1.49", substring = true).performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("Rail fare distance: 5.2 km", substring = true).assertIsDisplayed()
            capture("automatic-fare")
            compose.onNodeWithText("Travel date").performScrollTo().performTextReplacement("2025-10-29")
            compose.onNodeWithText("Automatic fares are unavailable for this travel date.", substring = true).performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("Regular adult fare:", substring = true).assertDoesNotExist()
            compose.onNodeWithText("Travel date").performScrollTo().performTextReplacement("2026-10-29")
            compose.onNodeWithText("Swap stations").performScrollTo().performClick()
            compose.onNodeWithText("Regular adult fare: S$1.49", substring = true).performScrollTo().assertIsDisplayed()
            assertTrue(runBlocking { repo.current().trips.isEmpty() })
            compose.onNodeWithText("Save journey").performScrollTo().performClick()
            compose.waitUntil(10_000) { runBlocking { repo.current().trips.size == 1 } }
            val saved = runBlocking { repo.current().trips.single() }
            assertEquals(149, saved.fareCents)
            assertEquals(repo.railFares.data.id, saved.fareVersion)
            assertNull(saved.correctionCents)
            compose.waitUntil(15_000) { compose.onAllNodesWithContentDescription("Record a journey").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Insights", useUnmergedTree = true).performClick()
            compose.onNodeWithText("Rail fare distance", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("5.2 km", useUnmergedTree = true).assertIsDisplayed()
            capture("automatic-distance")
        }
    }

    @Test fun editingRecordedFareRetainsItsPriceAndCorrection() {
        val repo = LocalRepository(app)
        val original = Trip(date = "2026-10-29", origin = "CC4", destination = "DT10", fareCents = 177, fareSource = "Earlier recorded fare", fareVersion = "earlier", correctionCents = 99, correctionReason = "Earlier discount")
        // Seed before launch: separate Room instances do not share invalidation notifications.
        runBlocking { repo.update { Snapshot(passes = listOf(PassPeriod(start = "2026-10-01", end = "2026-10-31")), trips = listOf(original)) } }
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitUntil(15_000) { compose.onAllNodesWithContentDescription("Record a journey").fetchSemanticsNodes().isNotEmpty() }
            compose.onNode(hasText("Trips") and hasClickAction()).performClick()
            compose.onNodeWithText("Corrected: Earlier discount").assertExists()
            compose.onNodeWithText("Promenade → Stevens").performClick()
            compose.onNodeWithText("Regular adult fare: S$1.77", substring = true).performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("Rail fare distance: 5.2 km", substring = true).assertIsDisplayed()
            compose.onNodeWithText("Save journey").performScrollTo().performClick()
            compose.waitUntil(10_000) { runBlocking { repo.current().trips.single().edited } }
            val retained = runBlocking { repo.current().trips.single() }
            assertEquals(177, retained.fareCents)
            assertEquals(99, retained.effectiveFare)
        }
    }

}
