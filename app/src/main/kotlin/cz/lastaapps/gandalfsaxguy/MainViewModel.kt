package cz.lastaapps.gandalfsaxguy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.lighthousegames.logging.logging
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal class MainViewModel(
    private val counter: CounterStore,
    private val sources: List<VideoSource> = VideoSource.sources,
) : ViewModel() {
    val state = MutableStateFlow(MainState())

    companion object{
        private val log = logging()
    }

    private var countingJob: Job? = null
    fun appeared() {
        log.i { "Appeared" }
        viewModelScope.launch {
            val startVideo = counter.lastVideoId.first()
                .let { id -> sources.firstOrNull { it.id == id } ?: sources.first() }
            state.update { it.copy(videoSource = startVideo, startTime = Clock.System.now()) }
        }
        viewModelScope.launch {
            counter.totalTime.collectLatest { total -> state.update { it.copy(totalWatchTime = total) } }
        }
        viewModelScope.launch {
            counter.maxTime.collectLatest { total -> state.update { it.copy(maxWatchTime = total) } }
        }
        viewModelScope.launch {
            state.map { it.currentWatchTime }.distinctUntilChanged().collectLatest {
            }
        }
    }

    fun resume() {
        log.i { "Resumed" }
        state.update { it.copy(isPlaying = true) }
        countingJob?.cancel()
        countingJob = viewModelScope.launch {
            while (true) {
                delay(1_000 - (Clock.System.now().toEpochMilliseconds() % 1_000))
                val newCurrent = state.value.currentWatchTime + 1.seconds
                state.update { it.copy(currentWatchTime = newCurrent) }
                counter.incTotalTimeBy(1.seconds)
                counter.trySetMaxTo(newCurrent)
            }
        }
    }

    fun pause() {
        log.i { "Paused" }
        state.update { it.copy(isPlaying = false) }
        countingJob?.cancel()
        countingJob = null
    }

    fun nextSource() {
        log.i { "Next source" }
        val newIndex = (sources.indexOf(state.value.videoSource) + 1)
            .takeIf { it < sources.size } ?: 0
        val newSource = sources[newIndex]
        state.update { it.copy(videoSource = newSource) }
        viewModelScope.launch { counter.setLastVideoId(newSource.id) }
    }

    fun prevSource() {
        log.i { "Previous source" }
        val newIndex = (sources.indexOf(state.value.videoSource) - 1)
            .takeIf { it >= 0 } ?: sources.lastIndex
        val newSource = sources[newIndex]
        state.update { it.copy(videoSource = newSource) }
        viewModelScope.launch { counter.setLastVideoId(newSource.id) }
    }

    fun invertControls() {
        log.i { "Invert controls" }
        state.update { it.copy(controlsVisible = !it.controlsVisible) }
    }
}

data class MainState constructor(
    val videoSource: VideoSource? = null,
    val startTime: Instant = Clock.System.now(),
    val isPlaying: Boolean = false,
    val controlsVisible: Boolean = true,
    val totalWatchTime: Duration = 0.milliseconds,
    val maxWatchTime: Duration = 0.milliseconds,
    val currentWatchTime: Duration = 0.milliseconds,
)