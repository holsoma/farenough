package io.github.holsoma.fairenough

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.holsoma.fairenough.core.*
import io.github.holsoma.fairenough.data.LocalRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant

class AppModel(app: Application): AndroidViewModel(app) {
    val repository = LocalRepository(app)
    val state = repository.state.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val selectedPass = repository.selectedPass.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val stations get() = repository.stations
    val engine = FareEngine()
    var message by mutableStateOf<String?>(null)
    var working by mutableStateOf(false)
    var importReview by mutableStateOf<ImportReview?>(null)
    var importText by mutableStateOf("")
    var restoreData by mutableStateOf<Snapshot?>(null)
    fun station(id: String): Station = stations.first { it.id == id }
    fun name(id: String): String = stations.find { it.id == id }?.name ?: id

    fun run(done: () -> Unit = {}, block: suspend () -> Unit) {
        if (working) return
        working = true
        viewModelScope.launch {
            try { withContext(Dispatchers.IO) { block() }; done() }
            catch (e: Exception) { message = e.message ?: "The operation could not be completed." }
            finally { working = false }
        }
    }
    fun savePass(pass: PassPeriod, done: () -> Unit) = run(done) {
        repository.update { old ->
            validatePass(pass, old.passes)?.let { error(it) }
            old.copy(passes = old.passes.filterNot { it.id == pass.id } + pass)
        }
        repository.selectPass(pass.id)
    }
    fun selectPass(id: String) = run { repository.selectPass(id) }
    fun saveTrip(trip: Trip, done: () -> Unit) = run(done) {
        repository.update { old ->
            require(validDate(trip.date)) { "Enter a valid travel date." }
            require(trip.origin != trip.destination) { "Choose different start and end stations." }
            require(stations.any { it.id == trip.origin } && stations.any { it.id == trip.destination }) { "Choose recognised stations." }
            val previous = old.trips.find { it.id == trip.id }
            val changedRoute = previous == null || previous.origin != trip.origin || previous.destination != trip.destination || previous.date != trip.date
            val assessed = if (changedRoute) engine.assess(trip, old.routeFares) else trip
            old.copy(trips = old.trips.filterNot { it.id == trip.id } + assessed.copy(edited = previous != null || trip.edited))
        }
    }
    fun deleteTrip(id: String, done: () -> Unit) = run(done) { repository.update { it.copy(trips = it.trips.filterNot { t -> t.id == id }) } }
    fun saveFare(fare: RouteFare, done: () -> Unit) = run(done) {
        repository.update { old ->
            validateRouteFare(fare, old.routeFares, stations)?.let { error(it) }
            val previous = old.routeFares.find { it.id == fare.id }
            val fares = old.routeFares.filterNot { it.id == fare.id } + fare
            old.copy(routeFares = fares, trips = old.trips.map { if (fare.matches(it) || previous?.matches(it) == true) engine.assess(it, fares) else it })
        }
    }
    fun review(text: String) = run { val review = reviewImport(text, stations, repository.current(), engine); withContext(Dispatchers.Main) { importReview = review } }
    fun import(selected: Set<Int>, done: () -> Unit) = run(done) {
        repository.update { latest ->
            // Revalidate against the latest database state before committing.
            val review = reviewImport(importText, stations, latest, engine)
            commitImport(review, selected, latest, Instant.now().toString())
        }
        withContext(Dispatchers.Main) { importReview = null; importText = "" }
    }
    fun undoBatch(id: String) = run { repository.update { undoImport(it, id) } }
    fun appearance(value: String) = run { repository.update { it.copy(preferences = it.preferences.copy(appearance = value)) } }
    fun favourite(origin: String, destination: String) = run {
        repository.update {
            val key = "$origin|$destination"
            val keys = it.preferences.favourites
            it.copy(preferences = it.preferences.copy(favourites = if (key in keys) keys - key else keys + key))
        }
    }
}
