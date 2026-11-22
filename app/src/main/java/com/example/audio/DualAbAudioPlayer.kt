package com.example.audio

import android.media.AudioAttributes
import android.media.MediaPlayer
import com.example.model.PlaybackSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

/**
 * Synchronized A/B audio preview player.
 * Keeps Original and AI Enhanced WAV tracks ready so toggling A <-> B during
 * playback switches seamlessly at the exact millisecond position.
 */
class DualAbAudioPlayer {
    private var originalPlayer: MediaPlayer? = null
    private var enhancedPlayer: MediaPlayer? = null
    private var activeSource: PlaybackSource = PlaybackSource.ORIGINAL
    private var tickerJob: Job? = null

    private var onStateChanged: ((isPlaying: Boolean, positionMs: Long, durationMs: Long) -> Unit)? = null

    fun setListener(listener: (isPlaying: Boolean, positionMs: Long, durationMs: Long) -> Unit) {
        onStateChanged = listener
    }

    fun loadTracks(originalWav: File?, enhancedWav: File?, preferredSource: PlaybackSource) {
        releasePlayersOnly()
        activeSource = if (preferredSource == PlaybackSource.ENHANCED && enhancedWav != null && enhancedWav.exists()) {
            PlaybackSource.ENHANCED
        } else {
            PlaybackSource.ORIGINAL
        }

        if (originalWav != null && originalWav.exists()) {
            originalPlayer = createPreparedPlayer(originalWav)
        }
        if (enhancedWav != null && enhancedWav.exists()) {
            enhancedPlayer = createPreparedPlayer(enhancedWav)
        }

        val dur = currentDurationMs()
        onStateChanged?.invoke(false, 0L, dur)
    }

    private fun createPreparedPlayer(file: File): MediaPlayer? {
        return try {
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                setDataSource(file.absolutePath)
                prepare()
                setOnCompletionListener {
                    stopTicker()
                    onStateChanged?.invoke(false, duration.toLong(), duration.toLong())
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    fun switchSource(source: PlaybackSource, scope: CoroutineScope) {
        val target = if (source == PlaybackSource.ENHANCED && enhancedPlayer != null) {
            PlaybackSource.ENHANCED
        } else {
            PlaybackSource.ORIGINAL
        }
        if (target == activeSource) return

        val oldPlayer = activePlayer()
        val wasPlaying = oldPlayer?.isPlaying == true
        val currentPos = oldPlayer?.currentPosition ?: 0

        try {
            if (wasPlaying) oldPlayer.pause()
        } catch (_: Exception) {
        }

        activeSource = target
        val newPlayer = activePlayer() ?: return

        try {
            val safePos = currentPos.coerceIn(0, (newPlayer.duration - 20).coerceAtLeast(0))
            newPlayer.seekTo(safePos)
            if (wasPlaying) {
                newPlayer.start()
                startTicker(scope)
            }
            onStateChanged?.invoke(wasPlaying, safePos.toLong(), newPlayer.duration.toLong())
        } catch (_: Exception) {
        }
    }

    fun togglePlayPause(scope: CoroutineScope) {
        val player = activePlayer() ?: return
        try {
            if (player.isPlaying) {
                player.pause()
                stopTicker()
                onStateChanged?.invoke(false, player.currentPosition.toLong(), player.duration.toLong())
            } else {
                if (player.currentPosition >= player.duration - 80) {
                    player.seekTo(0)
                }
                player.start()
                startTicker(scope)
                onStateChanged?.invoke(true, player.currentPosition.toLong(), player.duration.toLong())
            }
        } catch (_: Exception) {
        }
    }

    fun pause() {
        try {
            originalPlayer?.takeIf { it.isPlaying }?.pause()
            enhancedPlayer?.takeIf { it.isPlaying }?.pause()
        } catch (_: Exception) {
        }
        stopTicker()
        val p = activePlayer()
        onStateChanged?.invoke(false, p?.currentPosition?.toLong() ?: 0L, currentDurationMs())
    }

    fun seekTo(positionMs: Long) {
        val player = activePlayer() ?: return
        try {
            val safeMs = positionMs.toInt().coerceIn(0, player.duration.coerceAtLeast(0))
            player.seekTo(safeMs)
            onStateChanged?.invoke(player.isPlaying, safeMs.toLong(), player.duration.toLong())
        } catch (_: Exception) {
        }
    }

    fun skipBy(deltaMs: Long) {
        val player = activePlayer() ?: return
        seekTo(player.currentPosition.toLong() + deltaMs)
    }

    private fun activePlayer(): MediaPlayer? {
        return when (activeSource) {
            PlaybackSource.ORIGINAL -> originalPlayer
            PlaybackSource.ENHANCED -> enhancedPlayer ?: originalPlayer
        }
    }

    private fun currentDurationMs(): Long {
        return activePlayer()?.duration?.toLong() ?: 0L
    }

    private fun startTicker(scope: CoroutineScope) {
        stopTicker()
        tickerJob = scope.launch(Dispatchers.Main) {
            while (isActive) {
                val p = activePlayer()
                if (p == null || !p.isPlaying) {
                    onStateChanged?.invoke(false, p?.currentPosition?.toLong() ?: 0L, currentDurationMs())
                    break
                }
                onStateChanged?.invoke(true, p.currentPosition.toLong(), p.duration.toLong())
                delay(33L)
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    private fun releasePlayersOnly() {
        stopTicker()
        try {
            originalPlayer?.release()
        } catch (_: Exception) {
        }
        try {
            enhancedPlayer?.release()
        } catch (_: Exception) {
        }
        originalPlayer = null
        enhancedPlayer = null
    }

    fun release() {
        releasePlayersOnly()
        onStateChanged = null
    }
}
