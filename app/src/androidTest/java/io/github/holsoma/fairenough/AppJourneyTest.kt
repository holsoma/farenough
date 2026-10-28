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
            compose.onNodeWithText("How many more trips?").performScrollTo().performClick()
            compose.onNodeWithText("Your next stop: enough").assertIsDisplayed()
            capture("target")
            compose.onNodeWithContentDescription("Back").performClick()
            compose.onNodeWithText("Trips", useUnmergedTree = true).performClick()
            compose.onNodeWithText("Your journeys").assertIsDisplayed()
            capture("trips")
            compose.onNodeWithText("Import", useUnmergedTree = true).performClick()
            compose.onNodeWithText("Copy formatting prompt").assertIsDisplayed()
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
        }
    }

    @Test fun importTransactionAndUndoPreserveUnrelatedRecords() = runBlocking {
        val repo = LocalRepository(app)
        val original = demo(repo)
        repo.update { original }
        val text = """{"format":"fairenough-trips","version":1,"trips":[{"date":"2026-10-20","origin":"Toa Payoh","destination":"Bugis"},{"date":"2026-10-20","origin":"Toa Payoh","destination":"Bugis"}]}"""
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
            compose.onNodeWithText("Save pass").performScrollTo().performClick()
            compose.waitUntil(10_000) { runBlocking { repo.current().passes.size == 1 } }
            compose.onNodeWithContentDescription("Record a journey").performClick()
            compose.onNodeWithText("From", useUnmergedTree = true).performScrollTo().performClick()
            compose.onNodeWithText("Station name or code").performTextInput("Toa Payoh")
            compose.onNode(hasText("Toa Payoh") and hasText("NS19")).performClick()
            compose.onNodeWithText("To", useUnmergedTree = true).performScrollTo().performClick()
            compose.onNodeWithText("Station name or code").performTextInput("Bugis")
            compose.onNode(hasText("Bugis") and hasText("DT14 / EW12")).performClick()
            compose.onNodeWithText("This journey's corrected fare, S$ (optional)").performScrollTo().performTextInput("1.50")
            compose.onNodeWithText("Reason for correction").performScrollTo().performTextInput("Example discount")
            compose.onNodeWithText("Save journey").performScrollTo().performClick()
            compose.waitUntil(10_000) { runBlocking { repo.current().trips.size == 1 } }
            compose.onNodeWithText("S$46.50").assertExists()
            compose.onNodeWithText("Trips", useUnmergedTree = true).performClick()
            compose.onNodeWithText("Import", useUnmergedTree = true).performClick()
            val date = LocalDate.now(java.time.ZoneId.of("Asia/Singapore"))
            val text = """{"format":"fairenough-trips","version":1,"trips":[{"date":"$date","origin":"Woodlands","destination":"Marina Bay"},{"date":"$date","origin":"Woodlands","destination":"Marina Bay"}]}"""
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
            assertEquals(2, saved.trips.count { it.fareCents == null && it.correctionCents == null })
        }
    }

    @Test fun reportInstalledStateFingerprint() = runBlocking {
        val state = LocalRepository(app).current()
        val status = android.os.Bundle()
        status.putString("snapshot_sha256", fingerprint(encodeBackup(state, "fixed-for-comparison")))
        status.putString("snapshot_records", "${state.passes.size} passes, ${state.trips.size} trips, ${state.routeFares.size} fares")
        InstrumentationRegistry.getInstrumentation().sendStatus(0, status)
    }
}
