package com.example.model

import android.net.Uri
import java.io.File
import kotlin.math.abs

enum class StudioStage {
    HOME,
    PRE_ENHANCE,
    PROCESSING,
    STUDIO,
    EXPORT
}

/**
 * Simple optional enhancement mode selectable before running one-tap AI Enhance.
 */
enum class ProcessingMode(
    val label: String,
    val shortDescription: String,
    val noiseMultiplier: Float,
    val eqMultiplier: Float,
    val compressionMultiplier: Float,
    val minSpectralFloor: Float
) {
    AUTO(
        label = "Auto",
        shortDescription = "Automatically calibrates noise cleanup, EQ, and leveling to your recording.",
        noiseMultiplier = 0.88f,
        eqMultiplier = 0.90f,
        compressionMultiplier = 0.88f,
        minSpectralFloor = 0.14f
    ),
    LIGHT(
        label = "Light",
        shortDescription = "Gentle cleanup that preserves an untouched natural voice.",
        noiseMultiplier = 0.55f,
        eqMultiplier = 0.55f,
        compressionMultiplier = 0.55f,
        minSpectralFloor = 0.24f
    ),
    ULTRA_CLEAN(
        label = "Ultra Clean",
        shortDescription = "Stronger background noise and room echo cleanup for noisy spaces.",
        noiseMultiplier = 1.15f,
        eqMultiplier = 1.0f,
        compressionMultiplier = 1.0f,
        minSpectralFloor = 0.085f
    )
}

/**
 * Post-enhancement studio presets.
 * Selecting a preset intelligently shifts the studio adjustments on top of the AI baseline
 * without destroying the completed AI analysis.
 */
enum class VoicePreset(
    val title: String,
    val shortTitle: String,
    val subtitle: String,
    val clarityDelta: Float,
    val balanceDelta: Float,
    val warmthDbDelta: Float,
    val presenceDbDelta: Float,
    val noiseDelta: Float,
    val volumeDbDelta: Float,
    val bassDbDelta: Float,
    val trebleDbDelta: Float,
    val compressionDelta: Float,
    val deEsserDelta: Float,
    val reverbDelta: Float,
    val highPassHz: Float
) {
    YOUTUBE_CREATOR(
        title = "YouTube Creator",
        shortTitle = "YouTube",
        subtitle = "Balanced, prominent creator voice calibrated for -14 LUFS video playback.",
        clarityDelta = 0.08f,
        balanceDelta = 0.06f,
        warmthDbDelta = 0.5f,
        presenceDbDelta = 0.9f,
        noiseDelta = 0.0f,
        volumeDbDelta = 0.4f,
        bassDbDelta = 0.2f,
        trebleDbDelta = 0.6f,
        compressionDelta = 0.08f,
        deEsserDelta = 0.04f,
        reverbDelta = 0.04f,
        highPassHz = 82f
    ),
    GAMING_COMMENTARY(
        title = "Gaming Commentary",
        shortTitle = "Gaming",
        subtitle = "Crisp, clear, prominent commentary tuned for gameplay & UGW / BGMI mixes.",
        clarityDelta = 0.14f,
        balanceDelta = 0.12f,
        warmthDbDelta = -0.2f,
        presenceDbDelta = 1.4f,
        noiseDelta = 0.10f,
        volumeDbDelta = 0.6f,
        bassDbDelta = -0.4f,
        trebleDbDelta = 0.8f,
        compressionDelta = 0.15f,
        deEsserDelta = 0.10f,
        reverbDelta = 0.12f,
        highPassHz = 92f
    ),
    PODCAST_STUDIO(
        title = "Podcast Studio",
        shortTitle = "Podcast",
        subtitle = "Warm, smooth, intimate broadcast tone for long listening sessions.",
        clarityDelta = 0.02f,
        balanceDelta = 0.05f,
        warmthDbDelta = 1.3f,
        presenceDbDelta = 0.3f,
        noiseDelta = 0.0f,
        volumeDbDelta = 0.0f,
        bassDbDelta = 0.7f,
        trebleDbDelta = -0.2f,
        compressionDelta = 0.06f,
        deEsserDelta = 0.08f,
        reverbDelta = 0.06f,
        highPassHz = 74f
    ),
    CINEMATIC_VOICE(
        title = "Cinematic Voice",
        shortTitle = "Cinematic",
        subtitle = "Rich low-mid depth and polished air without artificial boominess.",
        clarityDelta = 0.04f,
        balanceDelta = 0.04f,
        warmthDbDelta = 1.6f,
        presenceDbDelta = 0.5f,
        noiseDelta = 0.0f,
        volumeDbDelta = 0.2f,
        bassDbDelta = 1.2f,
        trebleDbDelta = 0.5f,
        compressionDelta = 0.05f,
        deEsserDelta = 0.05f,
        reverbDelta = 0.04f,
        highPassHz = 68f
    ),
    NATURAL_VOICE(
        title = "Natural Voice",
        shortTitle = "Natural",
        subtitle = "Light transparent polish keeping your original mic tone untouched.",
        clarityDelta = -0.18f,
        balanceDelta = -0.18f,
        warmthDbDelta = -0.4f,
        presenceDbDelta = -0.6f,
        noiseDelta = -0.14f,
        volumeDbDelta = -0.5f,
        bassDbDelta = 0.0f,
        trebleDbDelta = -0.3f,
        compressionDelta = -0.20f,
        deEsserDelta = -0.12f,
        reverbDelta = -0.12f,
        highPassHz = 74f
    ),
    ULTRA_CLEAN(
        title = "Ultra Clean",
        shortTitle = "Ultra Clean",
        subtitle = "Maximum fan, room echo, and background noise suppression.",
        clarityDelta = 0.10f,
        balanceDelta = 0.10f,
        warmthDbDelta = 0.0f,
        presenceDbDelta = 0.8f,
        noiseDelta = 0.22f,
        volumeDbDelta = 0.3f,
        bassDbDelta = -0.6f,
        trebleDbDelta = 0.3f,
        compressionDelta = 0.12f,
        deEsserDelta = 0.12f,
        reverbDelta = 0.28f,
        highPassHz = 95f
    )
}

/**
 * All 11 post-enhancement studio controls, each with safe limits to prevent audio damage.
 */
data class StudioAdjustments(
    val voiceClarity: Float = 0.52f,      // 0.0 .. 1.0 (0% .. 100%)
    val voiceBalance: Float = 0.58f,      // 0.0 .. 1.0 (0% .. 100%)
    val warmthDb: Float = 1.0f,           // -3.0 .. +4.0 dB
    val presenceDb: Float = 1.6f,         // -3.0 .. +4.5 dB
    val noiseReduction: Float = 0.56f,    // 0.0 .. 1.0 (0% .. 100%)
    val voiceVolumeDb: Float = 0.0f,      // -4.0 .. +4.0 dB trim
    val bassDb: Float = 0.0f,             // -4.0 .. +4.0 dB
    val trebleDb: Float = 0.8f,           // -4.0 .. +4.0 dB
    val compression: Float = 0.48f,       // 0.0 .. 1.0 (0% .. 100%)
    val deEsser: Float = 0.42f,           // 0.0 .. 1.0 (0% .. 100%)
    val reverbReduction: Float = 0.40f,   // 0.0 .. 1.0 (0% .. 100%)
    val highPassHz: Float = 80f,
    val removeHum5060Hz: Boolean = true,
    val preserveNaturalBreath: Boolean = true
) {
    fun clamped(): StudioAdjustments = copy(
        voiceClarity = voiceClarity.coerceIn(0f, 1f),
        voiceBalance = voiceBalance.coerceIn(0f, 1f),
        warmthDb = warmthDb.coerceIn(-3.0f, 4.0f),
        presenceDb = presenceDb.coerceIn(-3.0f, 4.5f),
        noiseReduction = noiseReduction.coerceIn(0f, 0.94f),
        voiceVolumeDb = voiceVolumeDb.coerceIn(-4.0f, 4.0f),
        bassDb = bassDb.coerceIn(-4.0f, 4.0f),
        trebleDb = trebleDb.coerceIn(-4.0f, 4.0f),
        compression = compression.coerceIn(0f, 1f),
        deEsser = deEsser.coerceIn(0f, 1f),
        reverbReduction = reverbReduction.coerceIn(0f, 1f),
        highPassHz = highPassHz.coerceIn(65f, 110f)
    )

    fun withPresetOverlay(preset: VoicePreset): StudioAdjustments = StudioAdjustments(
        voiceClarity = (voiceClarity + preset.clarityDelta).coerceIn(0.08f, 0.95f),
        voiceBalance = (voiceBalance + preset.balanceDelta).coerceIn(0.10f, 0.95f),
        warmthDb = (warmthDb + preset.warmthDbDelta).coerceIn(-3.0f, 4.0f),
        presenceDb = (presenceDb + preset.presenceDbDelta).coerceIn(-3.0f, 4.5f),
        noiseReduction = (noiseReduction + preset.noiseDelta).coerceIn(0.05f, 0.92f),
        voiceVolumeDb = (voiceVolumeDb + preset.volumeDbDelta).coerceIn(-4.0f, 4.0f),
        bassDb = (bassDb + preset.bassDbDelta).coerceIn(-4.0f, 4.0f),
        trebleDb = (trebleDb + preset.trebleDbDelta).coerceIn(-4.0f, 4.0f),
        compression = (compression + preset.compressionDelta).coerceIn(0.08f, 0.92f),
        deEsser = (deEsser + preset.deEsserDelta).coerceIn(0.05f, 0.92f),
        reverbReduction = (reverbReduction + preset.reverbDelta).coerceIn(0.0f, 0.92f),
        highPassHz = preset.highPassHz,
        removeHum5060Hz = removeHum5060Hz,
        preserveNaturalBreath = preserveNaturalBreath
    )

    fun differsFrom(other: StudioAdjustments): Boolean {
        return abs(voiceClarity - other.voiceClarity) > 0.01f ||
            abs(voiceBalance - other.voiceBalance) > 0.01f ||
            abs(warmthDb - other.warmthDb) > 0.05f ||
            abs(presenceDb - other.presenceDb) > 0.05f ||
            abs(noiseReduction - other.noiseReduction) > 0.01f ||
            abs(voiceVolumeDb - other.voiceVolumeDb) > 0.05f ||
            abs(bassDb - other.bassDb) > 0.05f ||
            abs(trebleDb - other.trebleDb) > 0.05f ||
            abs(compression - other.compression) > 0.01f ||
            abs(deEsser - other.deEsser) > 0.01f ||
            abs(reverbReduction - other.reverbReduction) > 0.01f
    }
}

data class AcousticProfile(
    val durationMs: Long,
    val sampleRate: Int,
    val channels: Int,
    val noiseFloorDb: Float,
    val estimatedSnrDb: Float,
    val integratedLufs: Float,
    val peakDbfs: Float,
    val dynamicRangeDb: Float,
    val lowRumbleScore: Float,
    val hum5060Score: Float,
    val sibilanceScore: Float,
    val muddinessScore: Float,
    val speechRatio: Float,
    val detectedIssues: List<String>
) {
    val isUltraCleanPotentiallyAggressive: Boolean
        get() = noiseFloorDb < -54f || estimatedSnrDb > 28f || estimatedSnrDb < 7f

    val isAlreadyClean: Boolean
        get() = noiseFloorDb < -55f && estimatedSnrDb > 26f

    val isNoisy: Boolean
        get() = noiseFloorDb > -44f || estimatedSnrDb < 14f

    val isTooQuiet: Boolean
        get() = integratedLufs < -23f

    val isTooLoud: Boolean
        get() = peakDbfs > -1.5f || integratedLufs > -11.5f
}

data class QualityValidationReport(
    val passed: Boolean,
    val autoRetriedSafer: Boolean,
    val durationExactMatch: Boolean,
    val peakClipFree: Boolean,
    val speechIntact: Boolean,
    val naturalDynamicsPreserved: Boolean,
    val summary: String
)

data class AiMasteringAdvice(
    val headline: String,
    val summary: String,
    val recommendedPreset: VoicePreset,
    val detectedNoiseSources: List<String>,
    val dspNotes: List<String>,
    val isPlaceholder: Boolean = true
)

enum class ExportFormat(
    val label: String,
    val extension: String,
    val mimeType: String,
    val isLossless: Boolean,
    val supportedBitratesKbps: List<Int>,
    val defaultBitrateKbps: Int
) {
    WAV(
        label = "WAV",
        extension = "wav",
        mimeType = "audio/wav",
        isLossless = true,
        supportedBitratesKbps = emptyList(),
        defaultBitrateKbps = 0
    ),
    MP3(
        label = "MP3",
        extension = "mp3",
        mimeType = "audio/mpeg",
        isLossless = false,
        supportedBitratesKbps = listOf(192, 256, 320),
        defaultBitrateKbps = 320
    ),
    M4A(
        label = "M4A",
        extension = "m4a",
        mimeType = "audio/mp4",
        isLossless = false,
        supportedBitratesKbps = listOf(192, 256, 320, 360),
        defaultBitrateKbps = 320
    )
}

enum class WavBitDepth(val bits: Int, val label: String) {
    BIT_16(16, "16-bit PCM"),
    BIT_24(24, "24-bit PCM")
}

enum class ExportSampleRate(val hz: Int, val label: String) {
    SR_44100(44_100, "44.1 kHz"),
    SR_48000(48_000, "48 kHz")
}

enum class ExportChannelMode(val count: Int, val label: String) {
    MONO(1, "Mono (Voice)"),
    STEREO(2, "Stereo")
}

data class ExportConfig(
    val customFileName: String = "",
    val format: ExportFormat = ExportFormat.WAV,
    val wavBitDepth: WavBitDepth = WavBitDepth.BIT_16,
    val bitrateKbps: Int = 320,
    val sampleRate: ExportSampleRate = ExportSampleRate.SR_48000,
    val channelMode: ExportChannelMode = ExportChannelMode.MONO
)

data class ProcessingProgress(
    val stageLabel: String,
    val progress: Float,
    val stageIndex: Int,
    val totalStages: Int = 6
)

enum class PlaybackSource {
    ORIGINAL,
    ENHANCED
}

data class RecentSessionItem(
    val id: String,
    val displayName: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val sourceType: String,
    val timestampLabel: String,
    val sourceUri: Uri? = null,
    val pcmFile: File? = null,
    val wavFile: File? = null,
    val sampleRate: Int = 48_000,
    val channels: Int = 1
)

data class StudioTrack(
    val id: String,
    val displayName: String,
    val sourceType: String,
    val originalFileSizeBytes: Long,
    val originalPcmFile: File,
    val originalWavFile: File,
    val enhancedPcmFile: File? = null,
    val enhancedWavFile: File? = null,
    val aiBaselinePcmFile: File? = null,
    val aiBaselineWavFile: File? = null,
    val aiBaselineAdjustments: StudioAdjustments? = null,
    val aiBaselineWaveform: List<Float> = emptyList(),
    val aiBaselineProfile: AcousticProfile? = null,
    val aiBaselineValidation: QualityValidationReport? = null,
    val sampleRate: Int,
    val channels: Int,
    val sourceBitsPerSample: Int = 16,
    val durationMs: Long,
    val originalWaveform: List<Float>,
    val enhancedWaveform: List<Float> = emptyList(),
    val originalProfile: AcousticProfile? = null,
    val enhancedProfile: AcousticProfile? = null,
    val validationReport: QualityValidationReport? = null,
    val aiAdvice: AiMasteringAdvice? = null
) {
    val isEnhanced: Boolean
        get() = enhancedWavFile != null && enhancedWavFile.exists() && enhancedPcmFile != null
}

data class WorkspaceStorageInfo(
    val fileCount: Int = 0,
    val totalBytes: Long = 0L
)

data class StudioUiState(
    val stage: StudioStage = StudioStage.HOME,
    val track: StudioTrack? = null,
    val selectedMode: ProcessingMode = ProcessingMode.AUTO,
    val selectedPreset: VoicePreset? = null,
    val adjustments: StudioAdjustments = StudioAdjustments(),
    val exportConfig: ExportConfig = ExportConfig(),
    val isRecording: Boolean = false,
    val isRecordingPaused: Boolean = false,
    val recordingDurationMs: Long = 0L,
    val liveRecordingLevels: List<Float> = emptyList(),
    val isImporting: Boolean = false,
    val importStatusText: String? = null,
    val isProcessing: Boolean = false,
    val isReRenderingPreview: Boolean = false,
    val processingProgress: ProcessingProgress? = null,
    val isExporting: Boolean = false,
    val exportProgress: Float = 0f,
    val lastExportedDisplayPath: String? = null,
    val lastExportedFileName: String? = null,
    val lastExportedSizeBytes: Long = 0L,
    val lastExportedShareUri: Uri? = null,
    val lastExportedMimeType: String? = null,
    val playbackSource: PlaybackSource = PlaybackSource.ORIGINAL,
    val isPlaying: Boolean = false,
    val playbackPositionMs: Long = 0L,
    val playbackDurationMs: Long = 0L,
    val showEngineDetails: Boolean = false,
    val showPrivacySheet: Boolean = false,
    val recentSessions: List<RecentSessionItem> = emptyList(),
    val workspaceInfo: WorkspaceStorageInfo = WorkspaceStorageInfo(),
    val errorMessage: String? = null,
    val snackbarMessage: String? = null
)
