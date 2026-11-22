package com.example.ai

import com.example.model.AcousticProfile
import com.example.model.AiMasteringAdvice
import com.example.model.VoicePreset
import java.util.Locale

/**
 * Single AI mastering advisor interface.
 * Can be backed by Firebase AI Logic (Gemini) when Firebase is enabled, or by
 * [PlaceholderVoiceAiRepository] when running fully on-device without Firebase.
 */
interface VoiceAiRepository {
    suspend fun analyzeVoiceAndRecommend(
        profile: AcousticProfile
    ): AiMasteringAdvice
}

/**
 * On-device acoustic telemetry summarizer used when Firebase AI Logic is not enabled.
 * Zero network latency, zero API cost; all DSP cleanup runs genuinely on-device.
 */
class PlaceholderVoiceAiRepository : VoiceAiRepository {
    override suspend fun analyzeVoiceAndRecommend(
        profile: AcousticProfile
    ): AiMasteringAdvice {
        val noiseSources = buildList {
            if (profile.noiseFloorDb > -48f) add("Fan / Broadband Room Hiss")
            if (profile.lowRumbleScore > 0.24f) add("Low-End Wind / Desk Rumble")
            if (profile.hum5060Score > 0.24f) add("50/60 Hz Electrical Hum")
            if (profile.muddinessScore > 0.42f) add("Room Boxiness / Low-Mid Mud")
            if (profile.sibilanceScore > 0.35f) add("Sharp Sibilance")
            if (profile.isTooQuiet) add("Low Input Gain")
            if (profile.isTooLoud) add("Hot Peaks Near Clipping")
            if (isEmpty()) add("Clean Quiet Recording")
        }

        val recommendedPreset = when {
            profile.isAlreadyClean -> VoicePreset.NATURAL_VOICE
            profile.isNoisy -> VoicePreset.GAMING_COMMENTARY
            profile.muddinessScore > 0.46f -> VoicePreset.YOUTUBE_CREATOR
            profile.dynamicRangeDb < 11f -> VoicePreset.PODCAST_STUDIO
            else -> VoicePreset.YOUTUBE_CREATOR
        }

        val conditionHeadline = when {
            profile.isAlreadyClean -> "Clean Recording • Light Transparent Polish"
            profile.isNoisy -> "Noisy Environment • Speech-Protected Cleanup"
            profile.isTooQuiet -> "Quiet Source • Gentle Loudness Recovery"
            profile.isTooLoud -> "Hot Source • Peak Control & De-Clipping Safety"
            else -> "Balanced Adaptive Vocal Master"
        }

        val notes = listOf(
            "Adaptive STFT denoiser calibrated to ${String.format(Locale.US, "%.1f", profile.noiseFloorDb)} dBFS noise floor",
            "Vocal presence & mud-cut EQ shaped to preserve natural voice identity & clarity",
            "Speech-gated sentence leveling + -14.0 LUFS broadcast target with -1.0 dBTP limiter"
        )

        return AiMasteringAdvice(
            headline = conditionHeadline,
            summary = "Measured ${String.format(Locale.US, "%.1f", profile.integratedLufs)} LUFS, SNR ${String.format(Locale.US, "%.0f", profile.estimatedSnrDb)} dB, peak ${String.format(Locale.US, "%.1f", profile.peakDbfs)} dBFS.",
            recommendedPreset = recommendedPreset,
            detectedNoiseSources = noiseSources,
            dspNotes = notes,
            isPlaceholder = true
        )
    }
}
