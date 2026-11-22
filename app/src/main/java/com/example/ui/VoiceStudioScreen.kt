package com.example.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.model.ExportFormat
import com.example.model.ProcessingProgress
import com.example.model.StudioAdjustments
import com.example.model.StudioStage
import com.example.model.StudioTrack
import com.example.model.StudioUiState
import com.example.model.WavBitDepth
import com.example.ui.components.AdvancedVoiceAdjustmentsPanel
import com.example.ui.components.AiProcessingStage
import com.example.ui.components.EngineDiagnosticsCard
import com.example.ui.components.FinalExportScreen
import com.example.ui.components.HomeUploadStage
import com.example.ui.components.LiveRecordingDeck
import com.example.ui.components.PreEnhanceStage
import com.example.ui.components.PrivacyWorkspaceBottomSheet
import com.example.ui.components.StudioIcons
import com.example.ui.components.StudioPresetStrip
import com.example.ui.components.WaveformCompareDeck
import com.example.ui.theme.StudioAccent
import com.example.ui.theme.StudioAccentContainer
import com.example.ui.theme.StudioAccentGlow
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioBorderSubtle
import com.example.ui.theme.StudioRecordRed
import com.example.ui.theme.StudioRecordRedContainer
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.StudioSurfaceHighlight
import com.example.ui.theme.StudioTextMuted
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.ui.theme.StudioVoid

@Composable
fun VoiceStudioScreen(
    viewModel: StudioViewModel
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val micDeniedMsg = stringResource(R.string.mic_permission_denied)

    // System back button navigation across secondary stages
    BackHandler(enabled = state.stage != StudioStage.HOME) {
        viewModel.handleBackNavigation()
    }

    val recordPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.startRecording()
        } else {
            viewModel.showTransientMessage(micDeniedMsg)
        }
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importAudioUri(uri)
        }
    }

    LaunchedEffect(state.snackbarMessage) {
        val msg = state.snackbarMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    val onRequestRecord: () -> Unit = {
        val hasPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPerm) {
            viewModel.startRecording()
        } else {
            recordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val onRequestUpload: () -> Unit = {
        audioPickerLauncher.launch(
            arrayOf(
                "audio/*",
                "audio/wav",
                "audio/x-wav",
                "audio/mpeg",
                "audio/mp4",
                "audio/aac"
            )
        )
    }

    val onShareExportedAudio: () -> Unit = {
        val shareUri = state.lastExportedShareUri
        val mime = state.lastExportedMimeType ?: "audio/*"
        if (shareUri != null) {
            runCatching {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = mime
                    putExtra(Intent.EXTRA_STREAM, shareUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Share Enhanced Voice"))
            }.onFailure {
                viewModel.showTransientMessage("No compatible app found to share audio.")
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = StudioVoid,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = StudioSurfaceHighlight,
                    contentColor = StudioTextPrimary,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        bottomBar = {
            if (state.stage == StudioStage.STUDIO && state.track?.isEnhanced == true) {
                StudioExportBottomBar(
                    isBusy = state.isReRenderingPreview,
                    onNewFileClick = onRequestUpload,
                    onExportClick = viewModel::openExportScreen
                )
            }
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(StudioVoid)
                .padding(innerPadding)
        ) {
            val isExpanded = maxWidth >= 820.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = if (isExpanded && state.stage == StudioStage.STUDIO) 1080.dp else 620.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (state.stage != StudioStage.EXPORT) {
                        StudioBrandHeader(
                            stage = state.stage,
                            tempFileCount = state.workspaceInfo.fileCount,
                            onOpenPrivacy = viewModel::openPrivacySheet
                        )
                    }

                    AnimatedVisibility(visible = state.errorMessage != null) {
                        ErrorBanner(
                            message = state.errorMessage.orEmpty(),
                            onDismiss = viewModel::clearError
                        )
                    }

                    if (state.isImporting) {
                        ImportingProgressCard(status = state.importStatusText ?: "Loading audio…")
                    } else if (state.isRecording) {
                        LiveRecordingDeck(
                            durationMs = state.recordingDurationMs,
                            isPaused = state.isRecordingPaused,
                            liveLevels = state.liveRecordingLevels,
                            onTogglePause = viewModel::togglePauseRecording,
                            onStop = viewModel::stopRecording
                        )
                    } else {
                        Crossfade(
                            targetState = state.stage,
                            animationSpec = tween(180),
                            label = "studio_stage_transition"
                        ) { stage ->
                            when (stage) {
                                StudioStage.HOME -> {
                                    HomeUploadStage(
                                        recentSessions = state.recentSessions,
                                        onImportClick = onRequestUpload,
                                        onRecordClick = onRequestRecord,
                                        onOpenRecent = viewModel::openRecentSession
                                    )
                                }

                                StudioStage.PRE_ENHANCE -> {
                                    val track = state.track
                                    if (track != null) {
                                        PreEnhanceStage(
                                            track = track,
                                            selectedMode = state.selectedMode,
                                            isPlaying = state.isPlaying,
                                            positionMs = state.playbackPositionMs,
                                            durationMs = state.playbackDurationMs,
                                            onSelectMode = viewModel::selectProcessingMode,
                                            onTogglePlayPause = viewModel::togglePlayPause,
                                            onSeek = viewModel::seekTo,
                                            onSkipBy = viewModel::skipBy,
                                            onReplaceClick = onRequestUpload,
                                            onRemoveClick = viewModel::removeCurrentTrack,
                                            onEnhanceClick = viewModel::runAiEnhance
                                        )
                                    } else {
                                        HomeUploadStage(
                                            recentSessions = state.recentSessions,
                                            onImportClick = onRequestUpload,
                                            onRecordClick = onRequestRecord,
                                            onOpenRecent = viewModel::openRecentSession
                                        )
                                    }
                                }

                                StudioStage.PROCESSING -> {
                                    AiProcessingStage(
                                        trackName = state.track?.displayName ?: "Audio Recording",
                                        progress = state.processingProgress
                                            ?: ProcessingProgress("Analyzing recording", 0.08f, 1, 6),
                                        onCancelClick = viewModel::cancelProcessing
                                    )
                                }

                                StudioStage.STUDIO -> {
                                    val track = state.track
                                    if (track != null && track.isEnhanced) {
                                        PostEnhancementStudioStage(
                                            state = state,
                                            track = track,
                                            isExpanded = isExpanded,
                                            viewModel = viewModel
                                        )
                                    } else {
                                        HomeUploadStage(
                                            recentSessions = state.recentSessions,
                                            onImportClick = onRequestUpload,
                                            onRecordClick = onRequestRecord,
                                            onOpenRecent = viewModel::openRecentSession
                                        )
                                    }
                                }

                                StudioStage.EXPORT -> {
                                    val track = state.track
                                    if (track != null && track.isEnhanced) {
                                        FinalExportScreen(
                                            exportConfig = state.exportConfig,
                                            sourceSampleRate = track.sampleRate,
                                            sourceChannels = track.channels,
                                            sourceBitsPerSample = track.sourceBitsPerSample,
                                            durationMs = track.durationMs,
                                            isExporting = state.isExporting,
                                            exportProgress = state.exportProgress,
                                            lastExportedDisplayPath = state.lastExportedDisplayPath,
                                            canShare = state.lastExportedShareUri != null,
                                            onBackClick = viewModel::closeExportScreen,
                                            onFileNameChange = viewModel::updateExportFileName,
                                            onSelectWav16 = {
                                                viewModel.updateExportFormat(ExportFormat.WAV)
                                                viewModel.updateWavBitDepth(WavBitDepth.BIT_16)
                                            },
                                            onSelectWav24 = {
                                                viewModel.updateExportFormat(ExportFormat.WAV)
                                                viewModel.updateWavBitDepth(WavBitDepth.BIT_24)
                                            },
                                            onSelectMp3 = { viewModel.updateExportFormat(ExportFormat.MP3) },
                                            onSelectM4a = { viewModel.updateExportFormat(ExportFormat.M4A) },
                                            onBitrateSelected = viewModel::updateExportBitrate,
                                            onSampleRateSelected = viewModel::updateExportSampleRate,
                                            onChannelModeSelected = viewModel::updateExportChannelMode,
                                            onExportClick = viewModel::exportEnhancedAudio,
                                            onShareClick = onShareExportedAudio
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }

    if (state.showPrivacySheet) {
        PrivacyWorkspaceBottomSheet(
            workspaceInfo = state.workspaceInfo,
            hasActiveTrack = state.track != null,
            onDismiss = viewModel::dismissPrivacySheet,
            onRemoveCurrentTrack = viewModel::removeCurrentTrack,
            onWipeAllTempFiles = viewModel::wipeAllTempFiles
        )
    }
}

@Composable
private fun StudioBrandHeader(
    stage: StudioStage,
    tempFileCount: Int,
    onOpenPrivacy: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(StudioAccentContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = StudioIcons.GraphicEq,
                    contentDescription = null,
                    tint = StudioAccentGlow,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column {
                Text(
                    text = if (stage == StudioStage.STUDIO) {
                        stringResource(R.string.studio_stage_title)
                    } else {
                        stringResource(R.string.studio_brand_title)
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = StudioTextPrimary
                )
                Text(
                    text = if (stage == StudioStage.STUDIO) {
                        stringResource(R.string.studio_stage_subtitle)
                    } else {
                        stringResource(R.string.studio_subtitle)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = StudioTextMuted
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Surface(
            onClick = onOpenPrivacy,
            modifier = Modifier.testTag("privacy_shield_button"),
            shape = RoundedCornerShape(10.dp),
            color = StudioSurface,
            border = BorderStroke(1.dp, StudioBorder)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = StudioIcons.Shield,
                    contentDescription = stringResource(R.string.cd_privacy_shield),
                    tint = StudioTextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = if (tempFileCount > 0) "$tempFileCount" else "Private",
                    style = MaterialTheme.typography.labelMedium,
                    color = StudioTextSecondary
                )
            }
        }
    }
}

/**
 * Step 4 — Post-Enhancement Professional Audio Studio.
 * Reveals Before/After A/B comparison, Undo/Re-Enhance/Reset to AI actions,
 * 6 Professional Presets, and all 11 Advanced Voice Adjustment sliders.
 */
@Composable
private fun PostEnhancementStudioStage(
    state: StudioUiState,
    track: StudioTrack,
    isExpanded: Boolean,
    viewModel: StudioViewModel
) {
    val baseline = track.aiBaselineAdjustments ?: StudioAdjustments()
    val hasManualChanges = state.selectedPreset != null || state.adjustments.differsFrom(baseline)

    if (isExpanded) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                WaveformCompareDeck(
                    track = track,
                    playbackSource = state.playbackSource,
                    isPlaying = state.isPlaying,
                    positionMs = state.playbackPositionMs,
                    durationMs = state.playbackDurationMs,
                    onSelectSource = viewModel::selectPlaybackSource,
                    onTogglePlayPause = viewModel::togglePlayPause,
                    onSeek = viewModel::seekTo,
                    onSkipBy = viewModel::skipBy,
                    onRemoveTrack = viewModel::removeCurrentTrack
                )
                StudioUtilityActionRow(
                    canResetToAi = hasManualChanges,
                    onUndoClick = viewModel::undoEnhancement,
                    onReEnhanceClick = viewModel::runAiEnhance,
                    onResetToAiClick = viewModel::resetToAiResult
                )
                EngineDiagnosticsCard(
                    originalProfile = track.originalProfile,
                    validationReport = track.validationReport,
                    aiAdvice = track.aiAdvice,
                    expanded = state.showEngineDetails,
                    onToggleExpanded = viewModel::toggleEngineDetails
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                StudioPresetStrip(
                    selectedPreset = state.selectedPreset,
                    onSelectPreset = viewModel::selectStudioPreset
                )
                AdvancedVoiceAdjustmentsPanel(
                    adjustments = state.adjustments,
                    aiBaseline = baseline,
                    isUpdatingPreview = state.isReRenderingPreview,
                    onUpdate = viewModel::updateAdjustment,
                    onResetControl = viewModel::resetSingleAdjustment
                )
            }
        }
    } else {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            WaveformCompareDeck(
                track = track,
                playbackSource = state.playbackSource,
                isPlaying = state.isPlaying,
                positionMs = state.playbackPositionMs,
                durationMs = state.playbackDurationMs,
                onSelectSource = viewModel::selectPlaybackSource,
                onTogglePlayPause = viewModel::togglePlayPause,
                onSeek = viewModel::seekTo,
                onSkipBy = viewModel::skipBy,
                onRemoveTrack = viewModel::removeCurrentTrack
            )

            StudioUtilityActionRow(
                canResetToAi = hasManualChanges,
                onUndoClick = viewModel::undoEnhancement,
                onReEnhanceClick = viewModel::runAiEnhance,
                onResetToAiClick = viewModel::resetToAiResult
            )

            StudioPresetStrip(
                selectedPreset = state.selectedPreset,
                onSelectPreset = viewModel::selectStudioPreset
            )

            AdvancedVoiceAdjustmentsPanel(
                adjustments = state.adjustments,
                aiBaseline = baseline,
                isUpdatingPreview = state.isReRenderingPreview,
                onUpdate = viewModel::updateAdjustment,
                onResetControl = viewModel::resetSingleAdjustment
            )

            EngineDiagnosticsCard(
                originalProfile = track.originalProfile,
                validationReport = track.validationReport,
                aiAdvice = track.aiAdvice,
                expanded = state.showEngineDetails,
                onToggleExpanded = viewModel::toggleEngineDetails
            )
        }
    }
}

@Composable
private fun StudioUtilityActionRow(
    canResetToAi: Boolean,
    onUndoClick: () -> Unit,
    onReEnhanceClick: () -> Unit,
    onResetToAiClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = onUndoClick,
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .testTag("studio_undo_button"),
            shape = RoundedCornerShape(11.dp),
            border = BorderStroke(1.dp, StudioBorder),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = StudioSurface,
                contentColor = StudioTextPrimary
            ),
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            Icon(
                imageVector = StudioIcons.Undo,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = StudioTextSecondary
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = stringResource(R.string.action_undo_enhance),
                style = MaterialTheme.typography.labelMedium
            )
        }

        OutlinedButton(
            onClick = onReEnhanceClick,
            modifier = Modifier
                .weight(1.1f)
                .height(44.dp)
                .testTag("studio_re_enhance_button"),
            shape = RoundedCornerShape(11.dp),
            border = BorderStroke(1.dp, StudioBorder),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = StudioSurface,
                contentColor = StudioTextPrimary
            ),
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            Icon(
                imageVector = StudioIcons.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(15.dp),
                tint = StudioAccentGlow
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = stringResource(R.string.action_re_enhance),
                style = MaterialTheme.typography.labelMedium
            )
        }

        OutlinedButton(
            onClick = onResetToAiClick,
            enabled = canResetToAi,
            modifier = Modifier
                .weight(1.1f)
                .height(44.dp)
                .testTag("studio_reset_ai_button"),
            shape = RoundedCornerShape(11.dp),
            border = BorderStroke(1.dp, if (canResetToAi) StudioAccent.copy(alpha = 0.6f) else StudioBorderSubtle),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (canResetToAi) StudioAccentContainer else StudioSurface,
                contentColor = if (canResetToAi) StudioTextPrimary else StudioTextMuted,
                disabledContentColor = StudioTextMuted.copy(alpha = 0.45f)
            ),
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            Icon(
                imageVector = StudioIcons.RestartAlt,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = stringResource(R.string.action_reset_ai),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
private fun StudioExportBottomBar(
    isBusy: Boolean,
    onNewFileClick: () -> Unit,
    onExportClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = StudioSurface,
        border = BorderStroke(1.dp, StudioBorderSubtle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onNewFileClick,
                modifier = Modifier
                    .weight(0.75f)
                    .height(50.dp)
                    .testTag("studio_new_file_button"),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, StudioBorder),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = StudioSurfaceElevated,
                    contentColor = StudioTextPrimary
                ),
                contentPadding = PaddingValues(horizontal = 10.dp)
            ) {
                Icon(
                    imageVector = StudioIcons.FolderOpen,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp),
                    tint = StudioTextSecondary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.action_new_recording),
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Button(
                onClick = onExportClick,
                enabled = !isBusy,
                modifier = Modifier
                    .weight(1.35f)
                    .height(50.dp)
                    .testTag("download_enhanced_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = StudioAccent,
                    contentColor = StudioTextPrimary,
                    disabledContainerColor = StudioSurfaceElevated,
                    disabledContentColor = StudioTextMuted
                )
            ) {
                Icon(
                    imageVector = StudioIcons.Download,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isBusy) "Updating…" else stringResource(R.string.action_download_enhanced),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun ImportingProgressCard(status: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = StudioSurface,
        border = BorderStroke(1.dp, StudioBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = StudioAccent,
                strokeWidth = 2.5.dp
            )
            Column {
                Text(
                    text = status,
                    style = MaterialTheme.typography.titleSmall,
                    color = StudioTextPrimary
                )
                Text(
                    text = "Streaming locally with low memory footprint",
                    style = MaterialTheme.typography.bodySmall,
                    color = StudioTextMuted
                )
            }
        }
    }
}

@Composable
private fun ErrorBanner(
    message: String,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = StudioRecordRedContainer,
        border = BorderStroke(1.dp, StudioRecordRed.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = StudioIcons.ErrorOutline,
                contentDescription = null,
                tint = StudioRecordRed,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = StudioTextPrimary,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss error",
                    tint = StudioTextSecondary
                )
            }
        }
    }
}
