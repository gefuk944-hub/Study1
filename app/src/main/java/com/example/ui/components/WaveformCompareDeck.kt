package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.AcousticProfile
import com.example.model.PlaybackSource
import com.example.model.QualityValidationReport
import com.example.model.StudioTrack
import com.example.ui.theme.StudioAccent
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioBorderSubtle
import com.example.ui.theme.StudioMonoReadout
import com.example.ui.theme.StudioNeutralActive
import com.example.ui.theme.StudioRecordRed
import com.example.ui.theme.StudioSuccess
import com.example.ui.theme.StudioSuccessContainer
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.StudioSurfaceHighlight
import com.example.ui.theme.StudioTextMuted
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.ui.theme.StudioVoid
import com.example.ui.theme.StudioWarning
import com.example.ui.theme.StudioWarningContainer
import java.util.Locale

@Composable
fun LiveRecordingDeck(
    durationMs: Long,
    isPaused: Boolean,
    liveLevels: List<Float>,
    onTogglePause: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "rec_dot")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(650),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rec_dot_alpha"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = StudioSurface,
        border = BorderStroke(1.dp, StudioBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(if (isPaused) StudioWarning else StudioRecordRed.copy(alpha = dotAlpha))
                    )
                    Text(
                        text = if (isPaused) "Recording paused" else "Recording microphone",
                        style = MaterialTheme.typography.titleSmall,
                        color = StudioTextPrimary
                    )
                }
                Text(
                    text = formatTimecode(durationMs),
                    style = StudioMonoReadout.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                    color = StudioTextPrimary
                )
            }

            // Clean real-time level waveform strip
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(82.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(StudioVoid)
                    .border(1.dp, StudioBorderSubtle, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 10.dp)
            ) {
                val barCount = 52
                val gap = 3.dp.toPx()
                val barWidth = ((size.width - gap * (barCount - 1)) / barCount).coerceAtLeast(2f)
                val centerY = size.height / 2f

                drawLine(
                    color = StudioBorderSubtle,
                    start = Offset(0f, centerY),
                    end = Offset(size.width, centerY),
                    strokeWidth = 1.dp.toPx()
                )

                val offsetCount = (barCount - liveLevels.size).coerceAtLeast(0)
                for (i in 0 until barCount) {
                    val level = if (i >= offsetCount) liveLevels[i - offsetCount] else 0.04f
                    val barH = (level * size.height * 0.90f).coerceAtLeast(3.dp.toPx())
                    val x = i * (barWidth + gap)
                    val topY = centerY - barH / 2f
                    val barColor = when {
                        level > 0.88f -> StudioRecordRed
                        level > 0.62f -> StudioWarning
                        else -> StudioNeutralActive
                    }
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(x, topY),
                        size = Size(barWidth, barH),
                        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onTogglePause,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("recording_pause_button"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, StudioBorder)
                ) {
                    Icon(
                        imageVector = if (isPaused) Icons.Default.PlayArrow else StudioIcons.Pause,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = StudioTextPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(if (isPaused) R.string.action_resume_recording else R.string.action_pause_recording),
                        color = StudioTextPrimary
                    )
                }

                Button(
                    onClick = onStop,
                    modifier = Modifier
                        .weight(1.2f)
                        .height(48.dp)
                        .testTag("recording_stop_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StudioRecordRed,
                        contentColor = StudioTextPrimary
                    )
                ) {
                    Icon(
                        imageVector = StudioIcons.Stop,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = stringResource(R.string.action_stop_recording))
                }
            }
        }
    }
}

@Composable
fun WaveformCompareDeck(
    track: StudioTrack,
    playbackSource: PlaybackSource,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    onSelectSource: (PlaybackSource) -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onSkipBy: (Long) -> Unit,
    onRemoveTrack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isEnhancedActive = playbackSource == PlaybackSource.ENHANCED && track.isEnhanced
    val waveAccent by animateColorAsState(
        targetValue = if (isEnhancedActive) StudioAccent else StudioNeutralActive,
        animationSpec = tween(180),
        label = "wave_accent"
    )
    val waveformBars = if (isEnhancedActive && track.enhancedWaveform.isNotEmpty()) {
        track.enhancedWaveform
    } else {
        track.originalWaveform
    }
    val safeDuration = durationMs.coerceAtLeast(track.durationMs).coerceAtLeast(1L)
    val progressFraction = (positionMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = StudioSurface,
        border = BorderStroke(1.dp, StudioBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // File metadata header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = StudioTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val srText = formatSampleRate(track.sampleRate)
                    val chText = if (track.channels == 1) "Mono" else "Stereo"
                    Text(
                        text = "$srText · $chText · ${formatTimecode(track.durationMs)} · ${formatBytes(track.originalFileSizeBytes)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = StudioTextMuted
                    )
                }
                IconButton(
                    onClick = onRemoveTrack,
                    modifier = Modifier.testTag("remove_track_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.cd_remove_audio),
                        tint = StudioTextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Clean native-style A / B segmented switch
            AbSegmentedSwitch(
                activeSource = playbackSource,
                isEnhancedAvailable = track.isEnhanced,
                onSelectSource = onSelectSource
            )

            // Precision Scrubbable Waveform Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(106.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(StudioVoid)
                    .border(1.dp, StudioBorderSubtle, RoundedCornerShape(10.dp))
                    .testTag("waveform_scrubber")
                    .pointerInput(safeDuration) {
                        detectTapGestures { offset ->
                            val frac = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                            onSeek((frac * safeDuration).toLong())
                        }
                    }
                    .pointerInput(safeDuration) {
                        detectHorizontalDragGestures { change, _ ->
                            change.consume()
                            val frac = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                            onSeek((frac * safeDuration).toLong())
                        }
                    }
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp, vertical = 10.dp)
                ) {
                    val count = waveformBars.size.coerceAtLeast(1)
                    val gap = 2.dp.toPx()
                    val barW = ((size.width - gap * (count - 1)) / count).coerceAtLeast(1.5f)
                    val centerY = size.height / 2f

                    // Subtle studio horizon & top ruler ticks
                    drawLine(
                        color = StudioBorderSubtle,
                        start = Offset(0f, centerY),
                        end = Offset(size.width, centerY),
                        strokeWidth = 1.dp.toPx()
                    )
                    for (t in 0..8) {
                        val tx = (t / 8f) * size.width
                        val tickH = if (t % 2 == 0) 5.dp.toPx() else 3.dp.toPx()
                        drawLine(
                            color = StudioBorder.copy(alpha = 0.6f),
                            start = Offset(tx, 0f),
                            end = Offset(tx, tickH),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    val playheadX = progressFraction * size.width

                    for (i in 0 until count) {
                        val amp = waveformBars[i].coerceIn(0.05f, 1f)
                        val barH = (amp * size.height * 0.84f).coerceAtLeast(3.dp.toPx())
                        val x = i * (barW + gap)
                        val topY = centerY - barH / 2f
                        val isPlayed = x <= playheadX

                        val color = if (isPlayed) waveAccent else waveAccent.copy(alpha = 0.25f)
                        drawRoundRect(
                            color = color,
                            topLeft = Offset(x, topY),
                            size = Size(barW, barH),
                            cornerRadius = CornerRadius(barW / 2f, barW / 2f)
                        )
                    }

                    // Clean precision playhead
                    drawLine(
                        color = StudioTextPrimary,
                        start = Offset(playheadX, 0f),
                        end = Offset(playheadX, size.height),
                        strokeWidth = 1.75.dp.toPx()
                    )
                }
            }

            // Minimal centered transport & tabular timecodes
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTimecode(positionMs),
                    style = StudioMonoReadout,
                    color = StudioTextPrimary
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(
                        onClick = { onSkipBy(-5_000L) },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("rewind_5s_button")
                    ) {
                        Icon(
                            imageVector = StudioIcons.Replay5,
                            contentDescription = stringResource(R.string.cd_rewind_5s),
                            tint = StudioTextSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(StudioTextPrimary)
                            .clickable(onClick = onTogglePlayPause)
                            .testTag("play_pause_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) StudioIcons.Pause else Icons.Default.PlayArrow,
                            contentDescription = stringResource(R.string.cd_play_pause),
                            tint = StudioVoid,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    IconButton(
                        onClick = { onSkipBy(5_000L) },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("forward_5s_button")
                    ) {
                        Icon(
                            imageVector = StudioIcons.Forward5,
                            contentDescription = stringResource(R.string.cd_forward_5s),
                            tint = StudioTextSecondary
                        )
                    }
                }

                Text(
                    text = formatTimecode(safeDuration),
                    style = StudioMonoReadout,
                    color = StudioTextMuted
                )
            }

            // Compact acoustic readout & validation badge
            val activeProfile = if (isEnhancedActive && track.enhancedProfile != null) {
                track.enhancedProfile
            } else {
                track.originalProfile
            }
            if (activeProfile != null) {
                HorizontalDivider(color = StudioBorderSubtle)
                AcousticMeterStrip(
                    profile = activeProfile,
                    originalProfile = track.originalProfile,
                    isEnhancedView = isEnhancedActive
                )
            }

            val validation = track.validationReport
            if (track.isEnhanced && validation != null) {
                ValidationStatusRow(report = validation)
            }
        }
    }
}

@Composable
private fun AbSegmentedSwitch(
    activeSource: PlaybackSource,
    isEnhancedAvailable: Boolean,
    onSelectSource: (PlaybackSource) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(StudioVoid)
            .border(1.dp, StudioBorderSubtle, RoundedCornerShape(10.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val originalSelected = activeSource == PlaybackSource.ORIGINAL || !isEnhancedAvailable
        val enhancedSelected = activeSource == PlaybackSource.ENHANCED && isEnhancedAvailable

        Box(
            modifier = Modifier
                .weight(1f)
                .height(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (originalSelected) StudioSurfaceHighlight else Color.Transparent)
                .clickable { onSelectSource(PlaybackSource.ORIGINAL) }
                .testTag("ab_original_button"),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.ab_original),
                style = MaterialTheme.typography.labelLarge,
                color = if (originalSelected) StudioTextPrimary else StudioTextMuted
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .height(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (enhancedSelected) StudioAccent else Color.Transparent)
                .clickable(enabled = isEnhancedAvailable) {
                    onSelectSource(PlaybackSource.ENHANCED)
                }
                .testTag("ab_enhanced_button"),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isEnhancedAvailable) stringResource(R.string.ab_enhanced) else "Not enhanced yet",
                style = MaterialTheme.typography.labelLarge,
                color = when {
                    enhancedSelected -> StudioTextPrimary
                    isEnhancedAvailable -> StudioTextSecondary
                    else -> StudioTextMuted.copy(alpha = 0.55f)
                }
            )
        }
    }
}

@Composable
private fun AcousticMeterStrip(
    profile: AcousticProfile,
    originalProfile: AcousticProfile?,
    isEnhancedView: Boolean
) {
    val snrGain = if (isEnhancedView && originalProfile != null) {
        profile.estimatedSnrDb - originalProfile.estimatedSnrDb
    } else 0f

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        MeterColumn(
            label = "Noise floor",
            value = String.format(Locale.US, "%.0f dB", profile.noiseFloorDb),
            valueColor = if (isEnhancedView) StudioSuccess else StudioTextSecondary
        )
        MeterColumn(
            label = "Speech SNR",
            value = if (isEnhancedView && snrGain >= 1.5f) {
                String.format(Locale.US, "%.0f dB (+%.0f)", profile.estimatedSnrDb, snrGain)
            } else {
                String.format(Locale.US, "%.0f dB", profile.estimatedSnrDb)
            },
            valueColor = if (isEnhancedView) StudioAccent else StudioTextPrimary
        )
        MeterColumn(
            label = "Loudness",
            value = String.format(Locale.US, "%.1f LUFS", profile.integratedLufs),
            valueColor = StudioTextPrimary
        )
        MeterColumn(
            label = "Peak",
            value = String.format(Locale.US, "%.1f dB", profile.peakDbfs),
            valueColor = if (profile.peakDbfs > -0.3f) StudioRecordRed else StudioSuccess
        )
    }
}

@Composable
private fun MeterColumn(
    label: String,
    value: String,
    valueColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = StudioTextMuted
        )
        Text(
            text = value,
            style = StudioMonoReadout,
            color = valueColor
        )
    }
}

@Composable
private fun ValidationStatusRow(report: QualityValidationReport) {
    val bg = if (report.autoRetriedSafer) StudioWarningContainer else StudioSuccessContainer
    val fg = if (report.autoRetriedSafer) StudioWarning else StudioSuccess

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = if (report.autoRetriedSafer) Icons.Default.CheckCircle else StudioIcons.Verified,
            contentDescription = null,
            tint = fg,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = report.summary,
            style = MaterialTheme.typography.bodySmall,
            color = StudioTextPrimary
        )
    }
}

fun formatTimecode(ms: Long): String {
    val safe = ms.coerceAtLeast(0L)
    val totalSec = safe / 1000L
    val mins = totalSec / 60L
    val secs = totalSec % 60L
    val tenths = (safe % 1000L) / 100L
    return String.format(Locale.US, "%02d:%02d.%d", mins, secs, tenths)
}

fun formatSampleRate(hz: Int): String {
    return when (hz) {
        48_000 -> "48 kHz"
        44_100 -> "44.1 kHz"
        else -> String.format(Locale.US, "%.1f kHz", hz / 1000.0)
    }
}
