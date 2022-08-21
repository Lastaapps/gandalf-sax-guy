package cz.lastaapps.gandalfsaxguy.ui.components

import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.RawResourceDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import cz.lastaapps.gandalfsaxguy.VideoSource
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.lighthousegames.logging.logging

@Composable
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
fun VideoPlayer(
    startTime: Instant,
    videoSource: VideoSource,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    nowProvider: () -> Instant = { Clock.System.now() },
) {
    val context = LocalContext.current
    val log = remember { logging("VideoPlayer") }

    val exoPlayer = remember {
        log.i { "Creating player" }
        ExoPlayer.Builder(context)
            .build()
            .apply {
                playWhenReady = true
                videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
                repeatMode = Player.REPEAT_MODE_ALL
            }
    }
    remember(context, exoPlayer, videoSource) {
        log.i { "Updating player" }
        exoPlayer.apply {
            val defaultDataSourceFactory = DefaultDataSource.Factory(context)
            val dataSourceFactory: DataSource.Factory = DefaultDataSource.Factory(
                context, defaultDataSourceFactory
            )
            val uri = RawResourceDataSource.buildRawResourceUri(videoSource.resId)
            val source = ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(uri))
            setMediaSource(source)
            prepare()
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_READY -> {
                            log.i { "Seeking from a listener" }
                            seekTo((nowProvider() - startTime).inWholeMilliseconds % contentDuration)
                            removeListener(this)
                        }
                    }
                }
            })
        }
    }

    remember(exoPlayer, startTime) {
        exoPlayer.apply {
            if (this.playbackState == Player.STATE_READY) {
                log.i { "Seeking from compose" }
                seekTo((nowProvider() - startTime).inWholeMilliseconds % contentDuration)
            }
        }
    }

    remember(exoPlayer, isPlaying) {
        exoPlayer.apply {
            if (isPlaying) {
                play()
                if (playbackState == Player.STATE_READY) {
                    log.i { "Seeking from play" }
                    seekTo((nowProvider() - startTime).inWholeMilliseconds % contentDuration)
                }
            } else pause()
        }
    }

    AndroidView(modifier = modifier, factory = {
        PlayerView(context).apply {
            hideController()
            useController = false
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL

            player = exoPlayer
            layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        }
    })

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }
}