package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.ExportChannelMode
import com.example.model.ExportConfig
import com.example.model.ExportFormat
import com.example.model.ExportSampleRate
import com.example.model.WavBitDepth
import com.example.model.WorkspaceStorageInfo
import com.example.ui.theme.StudioAccent
import com.example.ui.theme.StudioAccentContainer
import com.example.ui.theme.StudioAccentGlow
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioBorderSubtle
import com.example.ui.theme.StudioMonoReadout
import com.example.ui.theme.StudioRecordRed
import com.example.ui.theme.StudioRecordRedContainer
import com.example.ui.theme.StudioSuccess
import com.example.ui.theme.StudioSuccessContainer
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.StudioTextMuted
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.ui.theme.StudioVoid
import com.example.ui.theme.StudioWarning
import com.example.ui.theme.StudioWarningContainer
import java.util.Locale

/**
 * Step 5 — Dedicated Final Export Screen.
 * Supports editable file name, WAV Lossless (16-bit & 24-bit), MP3 320 kbps,
 * M4A High Quality, 44.1/48 kHz, Mono/Stereo, live size estimate, Save to Downloads, and Share Audio.
 */
@Composable
fun FinalExportScreen(
    exportConfig: ExportConfig,
    sourceSampleRate: Int,
    sourceChannels: Int,
    sourceBitsPerSample: Int,
    durationMs: Long,
    isExporting: Boolean,
    exportProgress: Float,
    lastExportedDisplayPath: String?,
    canShare: Boolean,
    onBackClick: () -> Unit,
    onFileNameChange: (String) -> Unit,
    onSelectWav16: () -> Unit,
    onSelectWav24: () -> Unit,
    onSelectMp3: () -> Unit,
    onSelectM4a: () -> Unit,
    onBitrateSelected: (Int) -> Unit,
    onSampleRateSelected: (ExportSampleRate) -> Unit,
    onChannelModeSelected: (ExportChannelMode) -> Unit,
    onExportClick: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fidelityNotes = buildList {
        if (exportConfig.sampleRate.hz > sourceSampleRate) {
            add("Source is ${formatSampleRate(sourceSampleRate)} — exporting at ${exportConfig.sampleRate.label} resamples cleanly without inventing lost frequencies.")
        }
        if (exportConfig.format == ExportFormat.WAV && exportConfig.wavBitDepth.bits > sourceBitsPerSample) {
            add("Source is $sourceBitsPerSample-bit — 24-bit WAV preserves full DSP headroom honestly.")
        }
        if (exportConfig.channelMode == ExportChannelMode.STEREO && sourceChannels == 1) {
            add("Source is Mono — Mono is ideal for crisp podcast and commentary playback.")
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Screen Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onBackClick,
                enabled = !isExporting,
                modifier = Modifier.testTag("export_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_navigate_back),
                    tint = StudioTextPrimary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.export_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = StudioTextPrimary
                )
                val nativeCh = if (sourceChannels == 1) "Mono" else "Stereo"
                Text(
                    text = "Enhanced Master · ${formatSampleRate(sourceSampleRate)} · $nativeCh · ${formatTimecode(durationMs)}",
                    style = StudioMonoReadout,
                    color = StudioTextMuted
                )
            }
        }

        // Export Settings Card
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Editable File Name
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OptionGroupHeader(stringResource(R.string.export_filename_label))
                    OutlinedTextField(
                        value = exportConfig.customFileName,
                        onValueChange = onFileNameChange,
                        enabled = !isExporting,
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_filename_input"),
                        shape = RoundedCornerShape(12.dp),
                        suffix = {
                            Text(
                                text = ".${exportConfig.format.extension}",
                                style = StudioMonoReadout,
                                color = StudioAccentGlow
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = StudioAccent,
                            unfocusedBorderColor = StudioBorder,
                            focusedContainerColor = StudioVoid,
                            unfocusedContainerColor = StudioVoid,
                            focusedTextColor = StudioTextPrimary,
                            unfocusedTextColor = StudioTextPrimary
                        )
                    )
                }

                // 2. Format & Quality (WAV 16-bit, WAV 24-bit, MP3 320 kbps, M4A HQ)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OptionGroupHeader(stringResource(R.string.export_format_label))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StudioOptionPill(
                            title = "WAV 16-bit",
                            subtitle = "Lossless PCM",
                            selected = exportConfig.format == ExportFormat.WAV && exportConfig.wavBitDepth == WavBitDepth.BIT_16,
                            enabled = !isExporting,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_format_wav16"),
                            onClick = onSelectWav16
                        )
                        StudioOptionPill(
                            title = "WAV 24-bit",
                            subtitle = "Lossless Master",
                            selected = exportConfig.format == ExportFormat.WAV && exportConfig.wavBitDepth == WavBitDepth.BIT_24,
                            enabled = !isExporting,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_format_wav24"),
                            onClick = onSelectWav24
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StudioOptionPill(
                            title = "MP3 320k",
                            subtitle = "High Quality",
                            selected = exportConfig.format == ExportFormat.MP3,
                            enabled = !isExporting,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_format_mp3"),
                            onClick = onSelectMp3
                        )
                        StudioOptionPill(
                            title = "M4A AAC",
                            subtitle = "High Quality",
                            selected = exportConfig.format == ExportFormat.M4A,
                            enabled = !isExporting,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_format_m4a"),
                            onClick = onSelectM4a
                        )
                    }
                }

                if (exportConfig.format.isLossless) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(StudioSuccessContainer)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = StudioSuccess,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(R.string.export_lossless_badge),
                            style = MaterialTheme.typography.bodySmall,
                            color = StudioTextPrimary
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OptionGroupHeader(stringResource(R.string.export_bitrate_label))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            exportConfig.format.supportedBitratesKbps.forEach { kbps ->
                                StudioOptionPill(
                                    title = "$kbps",
                                    subtitle = "kbps",
                                    selected = exportConfig.bitrateKbps == kbps,
                                    enabled = !isExporting,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("export_bitrate_$kbps"),
                                    onClick = { onBitrateSelected(kbps) }
                                )
                            }
                        }
                    }
                }

                // 3. Sample Rate (44.1 / 48 kHz)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OptionGroupHeader(stringResource(R.string.export_sample_rate_label))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ExportSampleRate.entries.forEach { sr ->
                            val isNative = sr.hz == sourceSampleRate
                            StudioOptionPill(
                                title = sr.label,
                                subtitle = if (isNative) "Native match" else if (sr == ExportSampleRate.SR_48000) "Video standard" else "Audio standard",
                                selected = exportConfig.sampleRate == sr,
                                enabled = !isExporting,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("export_sr_${sr.hz}"),
                                onClick = { onSampleRateSelected(sr) }
                            )
                        }
                    }
                }

                // 4. Channels (Mono / Stereo)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OptionGroupHeader(stringResource(R.string.export_channels_label))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ExportChannelMode.entries.forEach { ch ->
                            StudioOptionPill(
                                title = ch.label,
                                subtitle = if (ch == ExportChannelMode.MONO) "Best for voice" else "Dual channel",
                                selected = exportConfig.channelMode == ch,
                                enabled = !isExporting,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("export_ch_${ch.name.lowercase()}"),
                                onClick = { onChannelModeSelected(ch) }
                            )
                        }
                    }
                }

                if (fidelityNotes.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(StudioWarningContainer)
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = StudioWarning,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = fidelityNotes.first(),
                            style = MaterialTheme.typography.bodySmall,
                            color = StudioTextSecondary
                        )
                    }
                }

                // 5. Estimated Size & Destination Summary
                val estSizeText = estimateOutputSize(durationMs, exportConfig)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(StudioVoid)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.export_estimated_size),
                            style = MaterialTheme.typography.labelMedium,
                            color = StudioTextSecondary
                        )
                        Text(
                            text = stringResource(R.string.export_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = StudioTextMuted
                        )
                    }
                    Text(
                        text = "~$estSizeText",
                        style = StudioMonoReadout,
                        color = StudioTextPrimary
                    )
                }

                // 6. Export Button / Progress
                if (isExporting) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.export_in_progress),
                                style = MaterialTheme.typography.labelLarge,
                                color = StudioTextPrimary
                            )
                            Text(
                                text = "${(exportProgress * 100).toInt()}%",
                                style = StudioMonoReadout,
                                color = StudioAccentGlow
                            )
                        }
                        LinearProgressIndicator(
                            progress = { exportProgress.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = StudioAccent,
                            trackColor = StudioSurfaceElevated
                        )
                    }
                } else {
                    Button(
                        onClick = onExportClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("confirm_export_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StudioAccent,
                            contentColor = StudioTextPrimary
                        )
                    ) {
                        Icon(
                            imageVector = StudioIcons.Download,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${stringResource(R.string.export_button_save)} (${exportConfig.format.label})",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }

        // Export Confirmation & Share Audio Card
        if (lastExportedDisplayPath != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = StudioSuccessContainer,
                border = BorderStroke(1.dp, StudioSuccess.copy(alpha = 0.45f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = StudioSuccess,
                            modifier = Modifier.size(22.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.export_complete_title),
                                style = MaterialTheme.typography.titleSmall,
                                color = StudioSuccess
                            )
                            Text(
                                text = lastExportedDisplayPath,
                                style = MaterialTheme.typography.bodySmall,
                                color = StudioTextPrimary
                            )
                        }
                    }

                    if (canShare) {
                        OutlinedButton(
                            onClick = onShareClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("share_exported_audio_button"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, StudioSuccess.copy(alpha = 0.6f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = StudioVoid.copy(alpha = 0.4f),
                                contentColor = StudioTextPrimary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = StudioSuccess,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.action_share_audio),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyWorkspaceBottomSheet(
    workspaceInfo: WorkspaceStorageInfo,
    hasActiveTrack: Boolean,
    onDismiss: () -> Unit,
    onRemoveCurrentTrack: () -> Unit,
    onWipeAllTempFiles: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = StudioSurface,
        contentColor = StudioTextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = stringResource(R.string.privacy_title),
                style = MaterialTheme.typography.titleLarge,
                color = StudioTextPrimary
            )

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = StudioVoid,
                border = BorderStroke(1.dp, StudioBorderSubtle)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = StudioSuccess,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(R.string.privacy_guarantee),
                        style = MaterialTheme.typography.bodySmall,
                        color = StudioTextSecondary
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(StudioSurfaceElevated)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Temporary workspace cache",
                        style = MaterialTheme.typography.titleSmall,
                        color = StudioTextPrimary
                    )
                    Text(
                        text = "${workspaceInfo.fileCount} private file(s)",
                        style = MaterialTheme.typography.bodySmall,
                        color = StudioTextMuted
                    )
                }
                Text(
                    text = formatBytes(workspaceInfo.totalBytes),
                    style = StudioMonoReadout,
                    color = StudioTextPrimary
                )
            }

            if (hasActiveTrack) {
                OutlinedButton(
                    onClick = onRemoveCurrentTrack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("privacy_remove_current_button"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, StudioBorder)
                ) {
                    Icon(
                        imageVector = StudioIcons.RemoveCircle,
                        contentDescription = null,
                        tint = StudioTextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.privacy_delete_current),
                        color = StudioTextPrimary
                    )
                }
            }

            Button(
                onClick = onWipeAllTempFiles,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("privacy_wipe_temp_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = StudioRecordRedContainer,
                    contentColor = StudioRecordRed
                ),
                border = BorderStroke(1.dp, StudioRecordRed.copy(alpha = 0.4f))
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.privacy_wipe_temp))
            }
        }
    }
}

@Composable
private fun OptionGroupHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = StudioTextSecondary
    )
}

@Composable
private fun StudioOptionPill(
    title: String,
    subtitle: String,
    selected: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(58.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) StudioAccentContainer else StudioSurfaceElevated,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) StudioAccent else StudioBorderSubtle
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = if (selected) StudioTextPrimary else StudioTextSecondary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) StudioAccentGlow else StudioTextMuted
            )
        }
    }
}

private fun estimateOutputSize(durationMs: Long, config: ExportConfig): String {
    val sec = (durationMs / 1000.0).coerceAtLeast(1.0)
    val bytes = if (config.format.isLossless) {
        val bytesPerSample = config.wavBitDepth.bits / 8
        (sec * config.sampleRate.hz * config.channelMode.count * bytesPerSample).toLong() + 44L
    } else {
        ((sec * config.bitrateKbps * 1000.0) / 8.0).toLong()
    }
    return formatBytes(bytes)
}

fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 KB"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return when {
        mb >= 1.0 -> String.format(Locale.US, "%.2f MB", mb)
        else -> String.format(Locale.US, "%.0f KB", kb.coerceAtLeast(1.0))
    }
}
