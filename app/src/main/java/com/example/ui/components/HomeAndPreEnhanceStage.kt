package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.ProcessingMode
import com.example.model.ProcessingProgress
import com.example.model.RecentSessionItem
import com.example.model.StudioTrack
import com.example.ui.theme.StudioAccent
import com.example.ui.theme.StudioAccentContainer
import com.example.ui.theme.StudioAccentGlow
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

/**
 * Step 1 — Clean, uncluttered AI-First Home Upload Stage.
 * Zero sliders, zero presets, zero dashboard clutter.
 */
@Composable
fun HomeUploadStage(
    recentSessions: List<RecentSessionItem>,
    onImportClick: () -> Unit,
    onRecordClick: () -> Unit,
    onOpenRecent: (RecentSessionItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = StudioSurface,
            border = BorderStroke(1.dp, StudioBorder)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                StudioAccent.copy(alpha = 0.08f),
                                Color.Transparent,
                                Color.Transparent
                            )
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // Subtle precision studio drop zone visual
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(StudioAccentContainer)
                            .border(1.dp, StudioAccent.copy(alpha = 0.45f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = StudioIcons.UploadFile,
                            contentDescription = null,
                            tint = StudioAccentGlow,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.home_drop_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = StudioTextPrimary,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = stringResource(R.string.home_drop_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = StudioTextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Subtle architectural waveform silhouette
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .padding(horizontal = 12.dp)
                    ) {
                        val pattern = floatArrayOf(
                            0.14f, 0.22f, 0.32f, 0.20f, 0.44f, 0.72f, 0.56f, 0.36f,
                            0.84f, 0.62f, 0.30f, 0.18f, 0.42f, 0.68f, 0.48f, 0.28f,
                            0.20f, 0.54f, 0.78f, 0.46f, 0.30f, 0.18f, 0.24f, 0.14f
                        )
                        val centerY = size.height / 2f
                        val gap = 6.dp.toPx()
                        val barW = ((size.width - gap * (pattern.size - 1)) / pattern.size).coerceAtLeast(2.5f)
                        for (i in pattern.indices) {
                            val h = pattern[i] * size.height * 0.88f
                            val x = i * (barW + gap)
                            drawRoundRect(
                                color = if (i in 4..19) StudioAccent.copy(alpha = 0.36f) else StudioBorder,
                                topLeft = Offset(x, centerY - h / 2f),
                                size = Size(barW, h),
                                cornerRadius = CornerRadius(barW / 2f, barW / 2f)
                            )
                        }
                    }

                    // Primary Import Audio + Secondary Record Audio actions
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onImportClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("upload_audio_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StudioAccent,
                                contentColor = StudioTextPrimary
                            )
                        ) {
                            Icon(
                                imageVector = StudioIcons.FolderOpen,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.action_upload_audio),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }

                        OutlinedButton(
                            onClick = onRecordClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("record_voice_button"),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, StudioBorder),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = StudioSurfaceElevated,
                                contentColor = StudioTextPrimary
                            )
                        ) {
                            Icon(
                                imageVector = StudioIcons.Mic,
                                contentDescription = null,
                                tint = StudioRecordRed,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.action_record_voice),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    // Clean format strip
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("WAV", "MP3", "M4A", "AAC").forEach { fmt ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(StudioVoid)
                                    .border(1.dp, StudioBorderSubtle, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 9.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = fmt,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = StudioTextMuted
                                )
                            }
                        }
                    }
                }
            }
        }

        if (recentSessions.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.home_recent_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = StudioTextSecondary
                )
                recentSessions.forEach { item ->
                    Surface(
                        onClick = { onOpenRecent(item) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("recent_session_${item.id}"),
                        shape = RoundedCornerShape(14.dp),
                        color = StudioSurface,
                        border = BorderStroke(1.dp, StudioBorderSubtle)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(StudioSurfaceElevated),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = StudioIcons.AudioFile,
                                    contentDescription = null,
                                    tint = StudioAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.displayName,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = StudioTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${formatTimecode(item.durationMs)} · ${formatBytes(item.sizeBytes)} · ${item.sourceType}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = StudioTextMuted
                                )
                            }
                            Text(
                                text = item.timestampLabel,
                                style = StudioMonoReadout,
                                color = StudioTextMuted
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Step 2 — Pre-Enhancement Stage.
 * Strictly displays ONLY:
 * - Uploaded audio preview (name, duration, original file size, waveform, play original, replace/remove)
 * - Optional simple enhancement mode (Auto / Light / Ultra Clean)
 * - Prominent one-tap AI Enhance button
 * Never exposes advanced sliders or studio presets before AI enhancement completes.
 */
@Composable
fun PreEnhanceStage(
    track: StudioTrack,
    selectedMode: ProcessingMode,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    onSelectMode: (ProcessingMode) -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onSkipBy: (Long) -> Unit,
    onReplaceClick: () -> Unit,
    onRemoveClick: () -> Unit,
    onEnhanceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val safeDuration = durationMs.coerceAtLeast(track.durationMs).coerceAtLeast(1L)
    val progressFraction = (positionMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
    val showUltraWarning = selectedMode == ProcessingMode.ULTRA_CLEAN &&
        (track.originalProfile == null || track.originalProfile.isUltraCleanPotentiallyAggressive)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Elegant Uploaded Audio Preview Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = StudioSurface,
            border = BorderStroke(1.dp, StudioBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(StudioAccentContainer)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.pre_enhance_ready_badge),
                                style = MaterialTheme.typography.labelSmall,
                                color = StudioAccentGlow
                            )
                        }
                        Text(
                            text = track.displayName,
                            style = MaterialTheme.typography.titleLarge,
                            color = StudioTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val chText = if (track.channels == 1) "Mono" else "Stereo"
                        Text(
                            text = "${formatTimecode(track.durationMs)} · ${formatBytes(track.originalFileSizeBytes)} · ${formatSampleRate(track.sampleRate)} · $chText",
                            style = StudioMonoReadout,
                            color = StudioTextMuted
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = onReplaceClick,
                            modifier = Modifier.testTag("replace_file_button")
                        ) {
                            Icon(
                                imageVector = StudioIcons.SwapHoriz,
                                contentDescription = stringResource(R.string.action_replace_file),
                                tint = StudioTextSecondary
                            )
                        }
                        IconButton(
                            onClick = onRemoveClick,
                            modifier = Modifier.testTag("remove_track_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.cd_remove_audio),
                                tint = StudioTextMuted
                            )
                        }
                    }
                }

                // Original Voice Scrubbable Waveform
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(104.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(StudioVoid)
                        .border(1.dp, StudioBorderSubtle, RoundedCornerShape(12.dp))
                        .testTag("pre_enhance_waveform")
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
                            .padding(horizontal = 12.dp, vertical = 12.dp)
                    ) {
                        val bars = track.originalWaveform
                        val count = bars.size.coerceAtLeast(1)
                        val gap = 2.dp.toPx()
                        val barW = ((size.width - gap * (count - 1)) / count).coerceAtLeast(1.5f)
                        val centerY = size.height / 2f
                        val playheadX = progressFraction * size.width

                        drawLine(
                            color = StudioBorderSubtle,
                            start = Offset(0f, centerY),
                            end = Offset(size.width, centerY),
                            strokeWidth = 1.dp.toPx()
                        )

                        for (i in 0 until count) {
                            val amp = bars[i].coerceIn(0.05f, 1f)
                            val barH = (amp * size.height * 0.84f).coerceAtLeast(3.dp.toPx())
                            val x = i * (barW + gap)
                            val isPlayed = x <= playheadX
                            drawRoundRect(
                                color = if (isPlayed) StudioNeutralActive else StudioNeutralActive.copy(alpha = 0.24f),
                                topLeft = Offset(x, centerY - barH / 2f),
                                size = Size(barW, barH),
                                cornerRadius = CornerRadius(barW / 2f, barW / 2f)
                            )
                        }

                        drawLine(
                            color = StudioTextPrimary,
                            start = Offset(playheadX, 0f),
                            end = Offset(playheadX, size.height),
                            strokeWidth = 1.75.dp.toPx()
                        )
                    }
                }

                // Play Original Voice Transport
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
                                .size(50.dp)
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
            }
        }

        // Optional Simple Enhancement Mode Selector (Auto | Light | Ultra Clean)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = StudioSurface,
            border = BorderStroke(1.dp, StudioBorderSubtle)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.pre_enhance_mode_label),
                    style = MaterialTheme.typography.titleSmall,
                    color = StudioTextSecondary
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(StudioVoid)
                        .border(1.dp, StudioBorderSubtle, RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ProcessingMode.entries.forEach { mode ->
                        val selected = mode == selectedMode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (selected) StudioSurfaceHighlight else Color.Transparent)
                                .border(
                                    width = if (selected) 1.dp else 0.dp,
                                    color = if (selected) StudioAccent.copy(alpha = 0.7f) else Color.Transparent,
                                    shape = RoundedCornerShape(9.dp)
                                )
                                .clickable { onSelectMode(mode) }
                                .testTag("mode_${mode.name.lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = mode.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) StudioTextPrimary else StudioTextMuted
                            )
                        }
                    }
                }

                Text(
                    text = selectedMode.shortDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = StudioTextMuted
                )

                AnimatedVisibility(visible = showUltraWarning) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(StudioWarningContainer)
                            .border(1.dp, StudioWarning.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = StudioWarning,
                            modifier = Modifier.size(16.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = stringResource(R.string.ultra_clean_warning_title),
                                style = MaterialTheme.typography.labelMedium,
                                color = StudioWarning
                            )
                            Text(
                                text = stringResource(R.string.ultra_clean_warning_body),
                                style = MaterialTheme.typography.bodySmall,
                                color = StudioTextSecondary
                            )
                        }
                    }
                }
            }
        }

        // Hero One-Tap AI Enhance CTA with subtle studio glow halo
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            StudioAccent,
                            Color(0xFF477BFF),
                            StudioAccentGlow
                        )
                    )
                )
        ) {
            Button(
                onClick = onEnhanceClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .testTag("ai_enhance_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = StudioTextPrimary
                ),
                contentPadding = PaddingValues(horizontal = 20.dp)
            ) {
                Icon(
                    imageVector = StudioIcons.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.action_ai_enhance),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }

        Text(
            text = stringResource(R.string.pre_enhance_hint),
            style = MaterialTheme.typography.bodySmall,
            color = StudioTextMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Step 3 — Dedicated AI Processing Experience driven exclusively by real DSP stages & progress.
 */
@Composable
fun AiProcessingStage(
    trackName: String,
    progress: ProcessingProgress,
    onCancelClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stages = listOf(
        1 to "Analyzing recording",
        2 to "Detecting speech",
        3 to "Removing background noise",
        4 to "Improving vocal clarity",
        5 to "Balancing voice",
        6 to "Preparing enhanced audio"
    )
    val clampedProgress = progress.progress.coerceIn(0f, 1f)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = StudioSurface,
        border = BorderStroke(1.dp, StudioBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Precision circular progress ring with real percentage
            Box(
                modifier = Modifier.size(124.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round)
                    drawCircle(
                        color = StudioSurfaceElevated,
                        style = stroke
                    )
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(StudioAccent, StudioAccentGlow, StudioAccent)
                        ),
                        startAngle = -90f,
                        sweepAngle = 360f * clampedProgress,
                        useCenter = false,
                        style = stroke
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${(clampedProgress * 100).toInt()}%",
                        style = StudioMonoReadout.copy(fontSize = 24.sp, fontWeight = FontWeight.SemiBold),
                        color = StudioTextPrimary
                    )
                    Icon(
                        imageVector = StudioIcons.GraphicEq,
                        contentDescription = null,
                        tint = StudioAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.processing_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = StudioTextPrimary
                )
                Text(
                    text = trackName,
                    style = MaterialTheme.typography.bodySmall,
                    color = StudioTextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            LinearProgressIndicator(
                progress = { clampedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = StudioAccent,
                trackColor = StudioSurfaceElevated
            )

            // Real DSP stage checklist
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(StudioVoid)
                    .border(1.dp, StudioBorderSubtle, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                stages.forEach { (idx, label) ->
                    val isDone = idx < progress.stageIndex || clampedProgress >= 0.999f
                    val isActive = idx == progress.stageIndex && !isDone
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isDone -> StudioSuccessContainer
                                        isActive -> StudioAccentContainer
                                        else -> StudioSurfaceElevated
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                isDone -> Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = StudioSuccess,
                                    modifier = Modifier.size(13.dp)
                                )
                                isActive -> CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    color = StudioAccent,
                                    strokeWidth = 1.8.dp
                                )
                                else -> Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(StudioBorder)
                                )
                            }
                        }

                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = when {
                                isDone -> StudioTextSecondary
                                isActive -> StudioTextPrimary
                                else -> StudioTextMuted
                            },
                            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }

            HorizontalDivider(color = StudioBorderSubtle)

            TextButton(
                onClick = onCancelClick,
                modifier = Modifier.testTag("cancel_processing_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = StudioRecordRed,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.action_cancel_processing),
                    style = MaterialTheme.typography.labelLarge,
                    color = StudioRecordRed
                )
            }
        }
    }
}
