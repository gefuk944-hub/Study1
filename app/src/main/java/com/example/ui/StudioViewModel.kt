package com.example.ui

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.PlaceholderVoiceAiRepository
import com.example.ai.VoiceAiRepository
import com.example.audio.AnalysisResult
import com.example.audio.AudioDecodeExportEngine
import com.example.audio.AudioRecorderEngine
import com.example.audio.DualAbAudioPlayer
import com.example.audio.StudioAudioProcessor
import com.example.model.ExportChannelMode
import com.example.model.ExportConfig
import com.example.model.ExportFormat
import com.example.model.ExportSampleRate
import com.example.model.PlaybackSource
import com.example.model.ProcessingMode
import com.example.model.ProcessingProgress
import com.example.model.RecentSessionItem
import com.example.model.StudioAdjustments
import com.example.model.StudioStage
import com.example.model.StudioTrack
import com.example.model.StudioUiState
import com.example.model.VoicePreset
import com.example.model.WavBitDepth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class StudioViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "StudioViewModel"
        private const val PREVIEW_DEBOUNCE_MS = 280L
    }

    private val processor = StudioAudioProcessor()
    private val decodeExportEngine = AudioDecodeExportEngine(application.applicationContext, processor)
    private val recorderEngine = AudioRecorderEngine(processor)
    private val abPlayer = DualAbAudioPlayer()
    private val aiRepository: VoiceAiRepository = PlaceholderVoiceAiRepository()

    private val _uiState = MutableStateFlow(StudioUiState())
    val uiState: StateFlow<StudioUiState> = _uiState.asStateFlow()

    private var cachedAnalysis: AnalysisResult? = null
    private var enhanceJob: Job? = null
    private var previewRenderJob: Job? = null
    private var importJob: Job? = null
    private var exportJob: Job? = null

    init {
        abPlayer.setListener { playing, posMs, durMs ->
            _uiState.update { state ->
                state.copy(
                    isPlaying = playing,
                    playbackPositionMs = posMs,
                    playbackDurationMs = if (durMs > 0L) durMs else (state.track?.durationMs ?: 0L)
                )
            }
        }

        // Clean leftover temp workspace off the main UI thread so frame 0 renders instantly
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                decodeExportEngine.wipeAllTempFiles()
                val info = decodeExportEngine.getWorkspaceStorageInfo()
                _uiState.update { it.copy(workspaceInfo = info) }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Step 1: Recording & Audio File Import (Lands on PRE_ENHANCE, never auto-enhances)
    // -------------------------------------------------------------------------

    fun startRecording() {
        if (_uiState.value.isProcessing || _uiState.value.isImporting || _uiState.value.isRecording) return

        abPlayer.pause()
        val pcmFile = decodeExportEngine.createSessionFile("rec_raw", "pcm")
        val wavFile = decodeExportEngine.createSessionFile("rec_preview", "wav")

        val started = recorderEngine.startRecording(
            scope = viewModelScope,
            pcmFile = pcmFile,
            wavFile = wavFile,
            onTick = { durationMs, level ->
                _uiState.update { state ->
                    val updatedLevels = (state.liveRecordingLevels + level).takeLast(52)
                    state.copy(
                        recordingDurationMs = durationMs,
                        liveRecordingLevels = updatedLevels
                    )
                }
            },
            onError = { err ->
                _uiState.update {
                    it.copy(
                        isRecording = false,
                        isRecordingPaused = false,
                        errorMessage = err
                    )
                }
            }
        )

        if (started) {
            _uiState.update {
                it.copy(
                    stage = StudioStage.HOME,
                    isRecording = true,
                    isRecordingPaused = false,
                    recordingDurationMs = 0L,
                    liveRecordingLevels = emptyList(),
                    errorMessage = null
                )
            }
        }
    }

    fun togglePauseRecording() {
        val current = _uiState.value
        if (!current.isRecording) return
        val nextPaused = !current.isRecordingPaused
        recorderEngine.setPaused(nextPaused)
        _uiState.update { it.copy(isRecordingPaused = nextPaused) }
    }

    fun stopRecording() {
        if (!_uiState.value.isRecording) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isRecording = false,
                    isRecordingPaused = false,
                    isImporting = true,
                    importStatusText = "Preparing recording preview…"
                )
            }
            try {
                val result = recorderEngine.stopRecording()
                if (result == null) {
                    _uiState.update {
                        it.copy(
                            isImporting = false,
                            importStatusText = null,
                            snackbarMessage = "Recording was too short to save."
                        )
                    }
                    refreshWorkspaceInfo()
                    return@launch
                }

                previewRenderJob?.cancel()
                val waveform = processor.extractWaveformPeaks(result.pcmFile, result.channels)
                val analysis = processor.analyzePcmFile(result.pcmFile, result.sampleRate, result.channels)
                cachedAnalysis = analysis

                val stamp = SimpleDateFormat("HH:mm", Locale.US).format(Date())
                val displayName = "Voice_Recording_$stamp.wav"
                val fileSizeBytes = result.wavFile.length()

                val newTrack = StudioTrack(
                    id = UUID.randomUUID().toString(),
                    displayName = displayName,
                    sourceType = "Recorded",
                    originalFileSizeBytes = fileSizeBytes,
                    originalPcmFile = result.pcmFile,
                    originalWavFile = result.wavFile,
                    sampleRate = result.sampleRate,
                    channels = result.channels,
                    durationMs = result.durationMs,
                    originalWaveform = waveform,
                    originalProfile = analysis.profile
                )

                val recentItem = RecentSessionItem(
                    id = newTrack.id,
                    displayName = displayName,
                    durationMs = result.durationMs,
                    sizeBytes = fileSizeBytes,
                    sourceType = "Microphone",
                    timestampLabel = stamp,
                    pcmFile = result.pcmFile,
                    wavFile = result.wavFile,
                    sampleRate = result.sampleRate,
                    channels = result.channels
                )

                val defaultSr = if (result.sampleRate == 44_100) ExportSampleRate.SR_44100 else ExportSampleRate.SR_48000
                abPlayer.loadTracks(newTrack.originalWavFile, null, PlaybackSource.ORIGINAL)

                _uiState.update { state ->
                    state.copy(
                        stage = StudioStage.PRE_ENHANCE,
                        track = newTrack,
                        selectedPreset = null,
                        isImporting = false,
                        importStatusText = null,
                        playbackSource = PlaybackSource.ORIGINAL,
                        playbackPositionMs = 0L,
                        playbackDurationMs = result.durationMs,
                        lastExportedDisplayPath = null,
                        lastExportedShareUri = null,
                        recentSessions = (listOf(recentItem) + state.recentSessions.filterNot { it.id == recentItem.id }).take(4),
                        exportConfig = state.exportConfig.copy(
                            customFileName = defaultExportName(displayName),
                            sampleRate = defaultSr,
                            channelMode = ExportChannelMode.MONO
                        )
                    )
                }
                refreshWorkspaceInfo()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed finalizing recording", e)
                _uiState.update {
                    it.copy(
                        isImporting = false,
                        importStatusText = null,
                        errorMessage = "Could not process recording: ${e.localizedMessage ?: "Unknown error"}"
                    )
                }
            }
        }
    }

    fun importAudioUri(uri: Uri) {
        if (_uiState.value.isRecording || _uiState.value.isProcessing) return
        abPlayer.pause()
        previewRenderJob?.cancel()
        importJob?.cancel()
        importJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isImporting = true,
                    importStatusText = "Loading audio file…",
                    errorMessage = null
                )
            }
            try {
                val decoded = decodeExportEngine.decodeUriToPcm(uri) { status ->
                    _uiState.update { it.copy(importStatusText = status) }
                }
                _uiState.update { it.copy(importStatusText = "Building waveform preview…") }

                val waveform = processor.extractWaveformPeaks(decoded.pcmFile, decoded.channels)
                val analysis = processor.analyzePcmFile(decoded.pcmFile, decoded.sampleRate, decoded.channels)
                cachedAnalysis = analysis

                val newTrack = StudioTrack(
                    id = UUID.randomUUID().toString(),
                    displayName = decoded.displayName,
                    sourceType = "Imported",
                    originalFileSizeBytes = decoded.originalFileSizeBytes,
                    originalPcmFile = decoded.pcmFile,
                    originalWavFile = decoded.wavFile,
                    sampleRate = decoded.sampleRate,
                    channels = decoded.channels,
                    durationMs = decoded.durationMs,
                    originalWaveform = waveform,
                    originalProfile = analysis.profile
                )

                val stamp = SimpleDateFormat("HH:mm", Locale.US).format(Date())
                val recentItem = RecentSessionItem(
                    id = newTrack.id,
                    displayName = decoded.displayName,
                    durationMs = decoded.durationMs,
                    sizeBytes = decoded.originalFileSizeBytes,
                    sourceType = "File",
                    timestampLabel = stamp,
                    sourceUri = uri,
                    pcmFile = decoded.pcmFile,
                    wavFile = decoded.wavFile,
                    sampleRate = decoded.sampleRate,
                    channels = decoded.channels
                )

                val defaultSr = if (decoded.sampleRate == 44_100) ExportSampleRate.SR_44100 else ExportSampleRate.SR_48000
                val defaultCh = if (decoded.channels >= 2) ExportChannelMode.STEREO else ExportChannelMode.MONO
                abPlayer.loadTracks(newTrack.originalWavFile, null, PlaybackSource.ORIGINAL)

                _uiState.update { state ->
                    state.copy(
                        stage = StudioStage.PRE_ENHANCE,
                        track = newTrack,
                        selectedPreset = null,
                        isImporting = false,
                        importStatusText = null,
                        playbackSource = PlaybackSource.ORIGINAL,
                        playbackPositionMs = 0L,
                        playbackDurationMs = decoded.durationMs,
                        lastExportedDisplayPath = null,
                        lastExportedShareUri = null,
                        recentSessions = (listOf(recentItem) + state.recentSessions.filterNot { it.displayName == recentItem.displayName }).take(4),
                        exportConfig = state.exportConfig.copy(
                            customFileName = defaultExportName(decoded.displayName),
                            sampleRate = defaultSr,
                            channelMode = defaultCh
                        )
                    )
                }
                refreshWorkspaceInfo()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Audio import failed", e)
                _uiState.update {
                    it.copy(
                        isImporting = false,
                        importStatusText = null,
                        errorMessage = "Could not import audio: ${e.localizedMessage ?: "Unsupported file"}"
                    )
                }
            }
        }
    }

    fun openRecentSession(item: RecentSessionItem) {
        val pcm = item.pcmFile
        val wav = item.wavFile
        if (pcm != null && pcm.exists() && wav != null && wav.exists()) {
            viewModelScope.launch {
                _uiState.update { it.copy(isImporting = true, importStatusText = "Re-opening ${item.displayName}…") }
                try {
                    val waveform = processor.extractWaveformPeaks(pcm, item.channels)
                    val analysis = processor.analyzePcmFile(pcm, item.sampleRate, item.channels)
                    cachedAnalysis = analysis

                    val track = StudioTrack(
                        id = item.id,
                        displayName = item.displayName,
                        sourceType = item.sourceType,
                        originalFileSizeBytes = item.sizeBytes,
                        originalPcmFile = pcm,
                        originalWavFile = wav,
                        sampleRate = item.sampleRate,
                        channels = item.channels,
                        durationMs = item.durationMs,
                        originalWaveform = waveform,
                        originalProfile = analysis.profile
                    )
                    abPlayer.loadTracks(track.originalWavFile, null, PlaybackSource.ORIGINAL)
                    _uiState.update { state ->
                        state.copy(
                            stage = StudioStage.PRE_ENHANCE,
                            track = track,
                            selectedPreset = null,
                            isImporting = false,
                            importStatusText = null,
                            playbackSource = PlaybackSource.ORIGINAL,
                            playbackPositionMs = 0L,
                            playbackDurationMs = item.durationMs,
                            exportConfig = state.exportConfig.copy(
                                customFileName = defaultExportName(item.displayName)
                            )
                        )
                    }
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            isImporting = false,
                            importStatusText = null,
                            errorMessage = "Recent session is no longer available."
                        )
                    }
                }
            }
        } else if (item.sourceUri != null) {
            importAudioUri(item.sourceUri)
        } else {
            _uiState.update { it.copy(snackbarMessage = "This recording was cleared from temporary storage.") }
        }
    }

    // -------------------------------------------------------------------------
    // Step 2: Optional Simple Mode + One-Tap AI Enhance -> Processing Screen
    // -------------------------------------------------------------------------

    fun selectProcessingMode(mode: ProcessingMode) {
        _uiState.update { it.copy(selectedMode = mode) }
    }

    fun runAiEnhance() {
        val track = _uiState.value.track ?: return
        if (_uiState.value.isProcessing || _uiState.value.isRecording) return

        abPlayer.pause()
        previewRenderJob?.cancel()
        enhanceJob?.cancel()
        enhanceJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    stage = StudioStage.PROCESSING,
                    isProcessing = true,
                    processingProgress = ProcessingProgress("Analyzing recording", 0.05f, 1, 6),
                    errorMessage = null
                )
            }

            try {
                val analysis = cachedAnalysis ?: processor.analyzePcmFile(
                    track.originalPcmFile,
                    track.sampleRate,
                    track.channels
                ).also { cachedAnalysis = it }

                val mode = _uiState.value.selectedMode
                val adaptiveAdjustments = processor.computeAdaptiveAdjustments(analysis.profile, mode)
                val advice = aiRepository.analyzeVoiceAndRecommend(analysis.profile)

                // Clean previous enhanced files (never deleting originalPcmFile / originalWavFile)
                decodeExportEngine.deleteFiles(
                    track.enhancedPcmFile,
                    track.enhancedWavFile,
                    track.aiBaselinePcmFile,
                    track.aiBaselineWavFile
                )

                val outPcm = decodeExportEngine.createSessionFile("enh_pcm", "pcm")
                val outWav = decodeExportEngine.createSessionFile("enh_preview", "wav")

                val result = processor.enhanceAudio(
                    inputPcmFile = track.originalPcmFile,
                    outputPcmFile = outPcm,
                    outputWavFile = outWav,
                    sampleRate = track.sampleRate,
                    channels = track.channels,
                    adjustments = adaptiveAdjustments,
                    mode = mode,
                    analysis = analysis,
                    allowAutoRetry = true,
                    onProgress = { prog ->
                        _uiState.update { it.copy(processingProgress = prog) }
                    }
                )

                // Snapshot the clean AI baseline files so "Reset to AI" is instantaneous
                val basePcm = decodeExportEngine.createSessionFile("ai_base_pcm", "pcm")
                val baseWav = decodeExportEngine.createSessionFile("ai_base_wav", "wav")
                withContext(Dispatchers.IO) {
                    result.enhancedPcmFile.copyTo(basePcm, overwrite = true)
                    result.enhancedWavFile.copyTo(baseWav, overwrite = true)
                }

                val updatedTrack = track.copy(
                    enhancedPcmFile = result.enhancedPcmFile,
                    enhancedWavFile = result.enhancedWavFile,
                    enhancedWaveform = result.enhancedWaveform,
                    aiBaselinePcmFile = basePcm,
                    aiBaselineWavFile = baseWav,
                    aiBaselineAdjustments = adaptiveAdjustments,
                    aiBaselineWaveform = result.enhancedWaveform,
                    aiBaselineProfile = result.enhancedProfile,
                    aiBaselineValidation = result.validationReport,
                    originalProfile = analysis.profile,
                    enhancedProfile = result.enhancedProfile,
                    validationReport = result.validationReport,
                    aiAdvice = advice
                )

                abPlayer.loadTracks(
                    originalWav = updatedTrack.originalWavFile,
                    enhancedWav = updatedTrack.enhancedWavFile,
                    preferredSource = PlaybackSource.ENHANCED
                )

                val doneMsg = if (result.validationReport.autoRetriedSafer) {
                    "Enhanced with gentle vocal-safe calibration"
                } else {
                    "AI enhancement complete • Compare Original vs Enhanced"
                }

                _uiState.update { state ->
                    state.copy(
                        stage = StudioStage.STUDIO,
                        track = updatedTrack,
                        adjustments = adaptiveAdjustments,
                        selectedPreset = null,
                        isProcessing = false,
                        processingProgress = null,
                        playbackSource = PlaybackSource.ENHANCED,
                        playbackPositionMs = 0L,
                        playbackDurationMs = updatedTrack.durationMs,
                        lastExportedDisplayPath = null,
                        lastExportedShareUri = null,
                        snackbarMessage = doneMsg
                    )
                }
                refreshWorkspaceInfo()
            } catch (e: CancellationException) {
                val hasTrack = _uiState.value.track != null
                _uiState.update {
                    it.copy(
                        stage = if (hasTrack) StudioStage.PRE_ENHANCE else StudioStage.HOME,
                        isProcessing = false,
                        processingProgress = null,
                        snackbarMessage = "AI processing cancelled"
                    )
                }
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Audio enhancement failed", e)
                val hasTrack = _uiState.value.track != null
                _uiState.update {
                    it.copy(
                        stage = if (hasTrack) StudioStage.PRE_ENHANCE else StudioStage.HOME,
                        isProcessing = false,
                        processingProgress = null,
                        errorMessage = "Enhancement failed: ${e.localizedMessage ?: "Processing error"}"
                    )
                }
            }
        }
    }

    fun cancelProcessing() {
        enhanceJob?.cancel()
        enhanceJob = null
    }

    // -------------------------------------------------------------------------
    // Step 3: Post-Enhancement Studio — Presets, 11 Fine Adjustments, Undo, Reset
    // -------------------------------------------------------------------------

    fun undoEnhancement() {
        val track = _uiState.value.track ?: return
        previewRenderJob?.cancel()
        abPlayer.pause()
        abPlayer.loadTracks(track.originalWavFile, null, PlaybackSource.ORIGINAL)

        _uiState.update {
            it.copy(
                stage = StudioStage.PRE_ENHANCE,
                playbackSource = PlaybackSource.ORIGINAL,
                playbackPositionMs = 0L,
                isReRenderingPreview = false,
                snackbarMessage = "Returned to original recording"
            )
        }
    }

    fun resetToAiResult() {
        val track = _uiState.value.track ?: return
        val baseAdj = track.aiBaselineAdjustments ?: return
        val basePcm = track.aiBaselinePcmFile
        val baseWav = track.aiBaselineWavFile

        previewRenderJob?.cancel()

        if (basePcm != null && basePcm.exists() && baseWav != null && baseWav.exists()) {
            viewModelScope.launch {
                val restorePcm = decodeExportEngine.createSessionFile("enh_pcm", "pcm")
                val restoreWav = decodeExportEngine.createSessionFile("enh_preview", "wav")
                withContext(Dispatchers.IO) {
                    basePcm.copyTo(restorePcm, overwrite = true)
                    baseWav.copyTo(restoreWav, overwrite = true)
                }
                val oldPcm = track.enhancedPcmFile
                val oldWav = track.enhancedWavFile
                val wasPlaying = _uiState.value.isPlaying
                val currentPos = _uiState.value.playbackPositionMs

                val restoredTrack = track.copy(
                    enhancedPcmFile = restorePcm,
                    enhancedWavFile = restoreWav,
                    enhancedWaveform = track.aiBaselineWaveform.ifEmpty { track.enhancedWaveform },
                    enhancedProfile = track.aiBaselineProfile ?: track.enhancedProfile,
                    validationReport = track.aiBaselineValidation ?: track.validationReport
                )
                abPlayer.loadTracks(
                    originalWav = restoredTrack.originalWavFile,
                    enhancedWav = restoredTrack.enhancedWavFile,
                    preferredSource = _uiState.value.playbackSource
                )
                if (currentPos > 0L) abPlayer.seekTo(currentPos)
                if (wasPlaying) abPlayer.togglePlayPause(viewModelScope)

                decodeExportEngine.deleteFiles(oldPcm, oldWav)

                _uiState.update {
                    it.copy(
                        track = restoredTrack,
                        adjustments = baseAdj,
                        selectedPreset = null,
                        isReRenderingPreview = false,
                        snackbarMessage = "Restored original AI enhanced result"
                    )
                }
                refreshWorkspaceInfo()
            }
        } else {
            _uiState.update { it.copy(adjustments = baseAdj, selectedPreset = null) }
            scheduleStudioPreviewRender(baseAdj)
        }
    }

    fun selectStudioPreset(preset: VoicePreset) {
        val track = _uiState.value.track ?: return
        val base = track.aiBaselineAdjustments ?: _uiState.value.adjustments
        val nextAdj = base.withPresetOverlay(preset)
        _uiState.update {
            it.copy(
                selectedPreset = preset,
                adjustments = nextAdj
            )
        }
        scheduleStudioPreviewRender(nextAdj)
    }

    fun updateAdjustment(transform: (StudioAdjustments) -> StudioAdjustments) {
        val updated = transform(_uiState.value.adjustments).clamped()
        _uiState.update {
            it.copy(
                adjustments = updated,
                selectedPreset = null
            )
        }
        scheduleStudioPreviewRender(updated)
    }

    fun resetSingleAdjustment(resetTransform: (current: StudioAdjustments, baseline: StudioAdjustments) -> StudioAdjustments) {
        val baseline = _uiState.value.track?.aiBaselineAdjustments ?: StudioAdjustments()
        val updated = resetTransform(_uiState.value.adjustments, baseline).clamped()
        _uiState.update { it.copy(adjustments = updated) }
        scheduleStudioPreviewRender(updated)
    }

    private fun scheduleStudioPreviewRender(targetAdjustments: StudioAdjustments) {
        val track = _uiState.value.track ?: return
        if (!track.isEnhanced) return

        previewRenderJob?.cancel()
        previewRenderJob = viewModelScope.launch {
            _uiState.update { it.copy(isReRenderingPreview = true) }
            delay(PREVIEW_DEBOUNCE_MS)

            try {
                val analysis = cachedAnalysis ?: processor.analyzePcmFile(
                    track.originalPcmFile,
                    track.sampleRate,
                    track.channels
                ).also { cachedAnalysis = it }

                val newPcm = decodeExportEngine.createSessionFile("enh_pcm", "pcm")
                val newWav = decodeExportEngine.createSessionFile("enh_preview", "wav")

                val result = processor.enhanceAudio(
                    inputPcmFile = track.originalPcmFile,
                    outputPcmFile = newPcm,
                    outputWavFile = newWav,
                    sampleRate = track.sampleRate,
                    channels = track.channels,
                    adjustments = targetAdjustments,
                    mode = _uiState.value.selectedMode,
                    analysis = analysis,
                    allowAutoRetry = false,
                    onProgress = {}
                )

                val latestTrack = _uiState.value.track ?: return@launch
                val oldPcm = latestTrack.enhancedPcmFile
                val oldWav = latestTrack.enhancedWavFile
                val wasPlaying = _uiState.value.isPlaying
                val currentPos = _uiState.value.playbackPositionMs

                val updatedTrack = latestTrack.copy(
                    enhancedPcmFile = result.enhancedPcmFile,
                    enhancedWavFile = result.enhancedWavFile,
                    enhancedWaveform = result.enhancedWaveform,
                    enhancedProfile = result.enhancedProfile,
                    validationReport = result.validationReport
                )

                abPlayer.loadTracks(
                    originalWav = updatedTrack.originalWavFile,
                    enhancedWav = updatedTrack.enhancedWavFile,
                    preferredSource = _uiState.value.playbackSource
                )
                if (currentPos > 0L) abPlayer.seekTo(currentPos)
                if (wasPlaying) abPlayer.togglePlayPause(viewModelScope)

                if (oldPcm != latestTrack.aiBaselinePcmFile) decodeExportEngine.deleteFiles(oldPcm)
                if (oldWav != latestTrack.aiBaselineWavFile) decodeExportEngine.deleteFiles(oldWav)

                _uiState.update {
                    it.copy(
                        track = updatedTrack,
                        isReRenderingPreview = false,
                        lastExportedDisplayPath = null,
                        lastExportedShareUri = null
                    )
                }
                refreshWorkspaceInfo()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Real-time preview re-render failed", e)
                _uiState.update {
                    it.copy(
                        isReRenderingPreview = false,
                        errorMessage = "Could not update audio preview: ${e.localizedMessage ?: "DSP error"}"
                    )
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Playback Controls (Synchronized A/B)
    // -------------------------------------------------------------------------

    fun togglePlayPause() {
        if (_uiState.value.track == null || _uiState.value.isRecording) return
        abPlayer.togglePlayPause(viewModelScope)
    }

    fun selectPlaybackSource(source: PlaybackSource) {
        val track = _uiState.value.track ?: return
        if (source == PlaybackSource.ENHANCED && !track.isEnhanced) return
        abPlayer.switchSource(source, viewModelScope)
        _uiState.update { it.copy(playbackSource = source) }
    }

    fun seekTo(positionMs: Long) {
        abPlayer.seekTo(positionMs)
    }

    fun skipBy(deltaMs: Long) {
        abPlayer.skipBy(deltaMs)
    }

    // -------------------------------------------------------------------------
    // Step 4: Final Export Screen & Sharing
    // -------------------------------------------------------------------------

    fun openExportScreen() {
        val track = _uiState.value.track ?: return
        if (!track.isEnhanced || _uiState.value.isReRenderingPreview) return
        abPlayer.pause()
        _uiState.update { state ->
            val currentName = state.exportConfig.customFileName.ifBlank { defaultExportName(track.displayName) }
            state.copy(
                stage = StudioStage.EXPORT,
                exportConfig = state.exportConfig.copy(customFileName = currentName),
                errorMessage = null
            )
        }
    }

    fun closeExportScreen() {
        if (_uiState.value.isExporting) return
        val hasEnhanced = _uiState.value.track?.isEnhanced == true
        _uiState.update {
            it.copy(stage = if (hasEnhanced) StudioStage.STUDIO else StudioStage.HOME)
        }
    }

    fun updateExportFileName(name: String) {
        _uiState.update { it.copy(exportConfig = it.exportConfig.copy(customFileName = name.take(54))) }
    }

    fun updateExportFormat(format: ExportFormat) {
        _uiState.update { state ->
            val bitrate = if (format.isLossless) 0 else format.defaultBitrateKbps
            state.copy(
                exportConfig = state.exportConfig.copy(
                    format = format,
                    bitrateKbps = bitrate
                )
            )
        }
    }

    fun updateWavBitDepth(bitDepth: WavBitDepth) {
        _uiState.update { it.copy(exportConfig = it.exportConfig.copy(wavBitDepth = bitDepth)) }
    }

    fun updateExportBitrate(kbps: Int) {
        _uiState.update { it.copy(exportConfig = it.exportConfig.copy(bitrateKbps = kbps)) }
    }

    fun updateExportSampleRate(sampleRate: ExportSampleRate) {
        _uiState.update { it.copy(exportConfig = it.exportConfig.copy(sampleRate = sampleRate)) }
    }

    fun updateExportChannelMode(mode: ExportChannelMode) {
        _uiState.update { it.copy(exportConfig = it.exportConfig.copy(channelMode = mode)) }
    }

    fun exportEnhancedAudio() {
        val track = _uiState.value.track ?: return
        val enhancedPcm = track.enhancedPcmFile ?: return
        if (_uiState.value.isExporting) return

        exportJob?.cancel()
        exportJob = viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, exportProgress = 0.04f, errorMessage = null) }
            try {
                val cfg: ExportConfig = _uiState.value.exportConfig
                val result = decodeExportEngine.exportEnhancedAudio(
                    sourcePcmFile = enhancedPcm,
                    sourceSampleRate = track.sampleRate,
                    sourceChannels = track.channels,
                    baseTrackTitle = track.displayName,
                    exportConfig = cfg,
                    onProgress = { p ->
                        _uiState.update { it.copy(exportProgress = p) }
                    }
                )
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        exportProgress = 1f,
                        lastExportedDisplayPath = result.displayPath,
                        lastExportedFileName = result.fileName,
                        lastExportedSizeBytes = result.sizeBytes,
                        lastExportedShareUri = result.shareUri,
                        lastExportedMimeType = result.mimeType,
                        snackbarMessage = "Saved to ${result.displayPath}"
                    )
                }
                refreshWorkspaceInfo()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Export failed", e)
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        exportProgress = 0f,
                        errorMessage = "Export failed: ${e.localizedMessage ?: "Could not save audio"}"
                    )
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Navigation, Diagnostics & Privacy Workspace
    // -------------------------------------------------------------------------

    fun handleBackNavigation() {
        when (_uiState.value.stage) {
            StudioStage.EXPORT -> closeExportScreen()
            StudioStage.PROCESSING -> cancelProcessing()
            StudioStage.STUDIO -> undoEnhancement()
            StudioStage.PRE_ENHANCE -> {
                abPlayer.pause()
                _uiState.update { it.copy(stage = StudioStage.HOME) }
            }
            StudioStage.HOME -> Unit
        }
    }

    fun toggleEngineDetails() {
        _uiState.update { it.copy(showEngineDetails = !it.showEngineDetails) }
    }

    fun openPrivacySheet() {
        refreshWorkspaceInfo()
        _uiState.update { it.copy(showPrivacySheet = true) }
    }

    fun dismissPrivacySheet() {
        _uiState.update { it.copy(showPrivacySheet = false) }
    }

    fun removeCurrentTrack() {
        enhanceJob?.cancel()
        previewRenderJob?.cancel()
        abPlayer.release()
        abPlayer.setListener { playing, posMs, durMs ->
            _uiState.update { state ->
                state.copy(
                    isPlaying = playing,
                    playbackPositionMs = posMs,
                    playbackDurationMs = if (durMs > 0L) durMs else (state.track?.durationMs ?: 0L)
                )
            }
        }
        cleanupTrackEnhancedFiles(_uiState.value.track)
        cachedAnalysis = null
        refreshWorkspaceInfo()
        _uiState.update {
            it.copy(
                stage = StudioStage.HOME,
                track = null,
                selectedPreset = null,
                isPlaying = false,
                isReRenderingPreview = false,
                playbackPositionMs = 0L,
                playbackDurationMs = 0L,
                playbackSource = PlaybackSource.ORIGINAL,
                lastExportedDisplayPath = null,
                lastExportedShareUri = null,
                showPrivacySheet = false,
                snackbarMessage = "Recording removed"
            )
        }
    }

    fun wipeAllTempFiles() {
        enhanceJob?.cancel()
        previewRenderJob?.cancel()
        abPlayer.pause()
        val deleted = decodeExportEngine.wipeAllTempFiles()
        cachedAnalysis = null
        abPlayer.loadTracks(null, null, PlaybackSource.ORIGINAL)
        refreshWorkspaceInfo()
        _uiState.update {
            it.copy(
                stage = StudioStage.HOME,
                track = null,
                selectedPreset = null,
                recentSessions = emptyList(),
                isPlaying = false,
                isReRenderingPreview = false,
                playbackPositionMs = 0L,
                playbackDurationMs = 0L,
                playbackSource = PlaybackSource.ORIGINAL,
                lastExportedDisplayPath = null,
                lastExportedShareUri = null,
                showPrivacySheet = false,
                snackbarMessage = "Privacy wipe complete ($deleted temporary files removed)"
            )
        }
    }

    fun showTransientMessage(msg: String) {
        _uiState.update { it.copy(snackbarMessage = msg) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    private fun defaultExportName(displayName: String): String {
        val base = displayName
            .substringBeforeLast('.')
            .replace(Regex("[^a-zA-Z0-9 _-]"), "_")
            .trim()
            .take(30)
            .ifBlank { "Voice_Studio" }
        return "${base}_Enhanced"
    }

    private fun cleanupTrackEnhancedFiles(track: StudioTrack?) {
        if (track == null) return
        decodeExportEngine.deleteFiles(
            track.enhancedPcmFile,
            track.enhancedWavFile,
            track.aiBaselinePcmFile,
            track.aiBaselineWavFile
        )
    }

    private fun refreshWorkspaceInfo() {
        _uiState.update { it.copy(workspaceInfo = decodeExportEngine.getWorkspaceStorageInfo()) }
    }

    override fun onCleared() {
        super.onCleared()
        recorderEngine.release()
        abPlayer.release()
        decodeExportEngine.wipeAllTempFiles()
    }
}
