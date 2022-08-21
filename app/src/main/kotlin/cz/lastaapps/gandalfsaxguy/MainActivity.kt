package cz.lastaapps.gandalfsaxguy

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.os.PersistableBundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.ViewCompat.getWindowInsetsController
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.datasource.RawResourceDataSource
import cz.lastaapps.gandalfsaxguy.ui.components.KeepScreenOn
import cz.lastaapps.gandalfsaxguy.ui.components.VideoPlayer
import cz.lastaapps.gandalfsaxguy.ui.style.AppTheme
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.lighthousegames.logging.logging
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class MainActivity : ComponentActivity() {

    companion object {
        private val log = logging()
    }

    private val viewModel: MainViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        log.i { "Creating" }
        installSplashScreen()
        super.onCreate(savedInstanceState)
        hideSystemBars()

        setContent {
            KeepScreenOn()
            AppTheme {
                Surface {
                    MainScreen(viewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.resume()
    }

    override fun onPause() {
        super.onPause()
        viewModel.pause()
    }

    private fun hideSystemBars() {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }
}

@Composable
private fun MainScreen(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.appeared()
    }

    Box(Modifier.fillMaxSize()) {
        state.videoSource?.let {
            VideoPlayer(
                startTime = state.startTime,
                videoSource = it,
                isPlaying = state.isPlaying,
                Modifier.matchParentSize(),
            )
        }
        Controls(viewModel, Modifier.matchParentSize())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Controls(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsState()
    val uriHandler = LocalUriHandler.current

    BoxWithConstraints(
        modifier
            .clickable { viewModel.invertControls() }
            .padding(16.dp)) {
        val isLand = maxWidth >= maxHeight

        AnimatedVisibility(
            modifier = Modifier.align(Alignment.TopStart),
            visible = state.controlsVisible,
            enter = slideInHorizontally { it * -1 * 3 / 2 },
            exit = slideOutHorizontally { it * -1 * 3 / 2 },
        ) {
            Card {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    TimeText(stringResource(R.string.counter_total), state.totalWatchTime)
                    TimeText(stringResource(R.string.counter_max), state.maxWatchTime)
                    TimeText(stringResource(R.string.counter_current), state.currentWatchTime)
                }
            }
        }

        val icons: @Composable () -> Unit = {
            IconButton(viewModel::prevSource) {
                Icon(Icons.Default.SkipPrevious, stringResource(R.string.prev_desc))
            }
            IconButton(viewModel::nextSource) {
                Icon(Icons.Default.SkipNext, stringResource(R.string.next_desc))
            }
            IconButton({ state.videoSource?.link?.let { uriHandler.openUri(it) } }) {
                Icon(painterResource(R.drawable.ic_youtube), stringResource(R.string.youtube_desc))
            }
        }

        if (isLand)
            AnimatedVisibility(
                modifier = Modifier.align(Alignment.TopEnd),
                visible = state.controlsVisible,
                enter = slideInVertically { it * -1 * 3 / 2 },
                exit = slideOutVertically { it * -1 * 3 / 2 },
            ) {
                Card {
                    Row(
                        Modifier.padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        icons()
                    }
                }
            }
        else
            AnimatedVisibility(
                modifier = Modifier.align(Alignment.TopEnd),
                visible = state.controlsVisible,
                enter = slideInHorizontally { it * 3 / 2 },
                exit = slideOutHorizontally { it * 3 / 2 },
            ) {
                Card {
                    Column(
                        Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        icons()
                    }
                }
            }

        AnimatedVisibility(
            modifier = Modifier.align(Alignment.BottomCenter),
            visible = state.controlsVisible,
            enter = slideInVertically { it * 3 / 2 },
            exit = slideOutVertically { it * 3 / 2 },
        ) {
            Card(onClick = {uriHandler.openUri("https://github.com/Lastaapps/gandalf-sax-guy")}) {
                Text(stringResource(R.string.author), Modifier.padding(12.dp))
            }
        }
    }
}

@Composable
private fun TimeText(label: String, duration: Duration) {
    val context = LocalContext.current
    val time = remember(context, duration) {
        var tmp = duration
        val days = tmp.inWholeDays
        tmp -= days.days
        val hours = tmp.inWholeHours
        tmp -= hours.hours
        val minutes = tmp.inWholeMinutes
        tmp -= minutes.minutes
        val seconds = tmp.inWholeSeconds

        when {
            days != 0L -> context.getString(R.string.time_format_days, days, hours, minutes, seconds)
            hours != 0L -> context.getString(R.string.time_format_hours, hours, minutes, seconds)
            else -> context.getString(R.string.time_format_minutes, minutes, seconds)
        }
    }

    Text("$label: $time", style = MaterialTheme.typography.titleMedium)
}