package cz.lastaapps.gandalfsaxguy

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal class CounterStore(context: Context) {

    companion object {
        private val Context.counterStore by preferencesDataStore("counter_store")

        private val lastVideoIdKey = intPreferencesKey("last_video_id")
        private val totalTimeKey = longPreferencesKey("total_time")
        private val maxTimeKey = longPreferencesKey("max_time")
    }

    private val store = context.counterStore


    // --- last video ------
    val lastVideoId: Flow<Int>
        get() = store.data.map {
            it[lastVideoIdKey] ?: VideoSource.sources[0].id
        }

    suspend fun setLastVideoId(id: Int) {
        store.edit { it[lastVideoIdKey] = id }
    }

    // --- total -----------
    val totalTime: Flow<Duration>
        get() = store.data.map { it[totalTimeKey] ?: 0L }.distinctUntilChanged().map { it.seconds }

    suspend fun incTotalTimeBy(inc: Duration) {
        val current = totalTime.first()
        store.edit { it[totalTimeKey] = (current + inc).inWholeSeconds }
    }

    // --- max -------------
    val maxTime: Flow<Duration>
        get() = store.data.map { it[maxTimeKey] ?: 0L }.distinctUntilChanged().map { it.seconds }

    suspend fun trySetMaxTo(newMax: Duration) {
        val current = maxTime.first()
        if (newMax > current)
            store.edit { it[maxTimeKey] = newMax.inWholeSeconds }
    }
}