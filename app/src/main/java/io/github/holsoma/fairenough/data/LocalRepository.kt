package io.github.holsoma.fairenough.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.*
import io.github.holsoma.fairenough.core.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString

/** Records are individually keyed; JSON payloads retain the portable versioned model. */
@Entity(tableName = "records", primaryKeys = ["kind", "id"])
data class StoredRecord(val kind: String, val id: String, val payload: String)

@Dao
interface RecordDao {
    @Query("SELECT * FROM records ORDER BY kind, id") fun observe(): Flow<List<StoredRecord>>
    @Query("SELECT * FROM records ORDER BY kind, id") suspend fun all(): List<StoredRecord>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(rows: List<StoredRecord>)
    @Query("DELETE FROM records") suspend fun clear()
}

@Database(entities = [StoredRecord::class], version = 1, exportSchema = true)
abstract class LocalDatabase: RoomDatabase() { abstract fun records(): RecordDao }
private val Context.viewPreferences by preferencesDataStore("view_preferences")

class LocalRepository(context: Context) {
    private val app = context.applicationContext
    private val db = Room.databaseBuilder(app, LocalDatabase::class.java, "fairenough.db").build()
    private val mutex = Mutex()
    val stations: List<Station> = ExchangeJson.decodeFromString(app.assets.open("stations.json").bufferedReader().use { it.readText() })
    val coastline: List<List<List<Double>>> = ExchangeJson.decodeFromString(app.assets.open("coastline.json").bufferedReader().use { it.readText() })
    val state: Flow<Snapshot> = db.records().observe().map(::decode).flowOn(Dispatchers.IO)
    val selectedPass: Flow<String?> = app.viewPreferences.data.map { it[stringPreferencesKey("pass")] }
    suspend fun selectPass(id: String) { app.viewPreferences.edit { it[stringPreferencesKey("pass")] = id } }

    suspend fun current(): Snapshot = decode(db.records().all())
    suspend fun update(change: (Snapshot) -> Snapshot) = mutex.withLock {
        db.withTransaction {
            val after = change(decode(db.records().all()))
            db.records().clear()
            db.records().insert(encode(after))
        }
    }
    private fun decode(rows: List<StoredRecord>): Snapshot = Snapshot(
        passes = rows.filter { it.kind == "pass" }.map { ExchangeJson.decodeFromString<PassPeriod>(it.payload) },
        trips = rows.filter { it.kind == "trip" }.map { ExchangeJson.decodeFromString<Trip>(it.payload) },
        routeFares = rows.filter { it.kind == "fare" }.map { ExchangeJson.decodeFromString<RouteFare>(it.payload) },
        batches = rows.filter { it.kind == "batch" }.map { ExchangeJson.decodeFromString<ImportBatch>(it.payload) },
        preferences = rows.firstOrNull { it.kind == "preferences" }?.let { ExchangeJson.decodeFromString<Preferences>(it.payload) } ?: Preferences(),
    )
    private fun encode(s: Snapshot): List<StoredRecord> = buildList {
        addAll(s.passes.map { StoredRecord("pass", it.id, ExchangeJson.encodeToString(it)) })
        addAll(s.trips.map { StoredRecord("trip", it.id, ExchangeJson.encodeToString(it)) })
        addAll(s.routeFares.map { StoredRecord("fare", it.id, ExchangeJson.encodeToString(it)) })
        addAll(s.batches.map { StoredRecord("batch", it.id, ExchangeJson.encodeToString(it)) })
        add(StoredRecord("preferences", "main", ExchangeJson.encodeToString(s.preferences)))
    }
}
