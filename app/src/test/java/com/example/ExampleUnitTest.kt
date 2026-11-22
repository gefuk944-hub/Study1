package com.example

import com.example.audio.StudioAudioProcessor
import com.example.model.AcousticProfile
import com.example.model.ProcessingMode
import com.example.model.VoicePreset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

class ExampleUnitTest {

    @Test
    fun `adaptive ai calibration tailors processing to clean, noisy, quiet, and loud recordings`() {
        val processor = StudioAudioProcessor()

        val cleanProfile = AcousticProfile(
            durationMs = 4000L, sampleRate = 48_000, channels = 1,
            noiseFloorDb = -62f, estimatedSnrDb = 34f, integratedLufs = -16f,
            peakDbfs = -4f, dynamicRangeDb = 11f, lowRumbleScore = 0.05f,
            hum5060Score = 0.04f, sibilanceScore = 0.15f, muddinessScore = 0.20f,
            speechRatio = 0.68f, detectedIssues = listOf("Clean Source Audio")
        )
        val noisyProfile = cleanProfile.copy(
            noiseFloorDb = -35f, estimatedSnrDb = 10f, muddinessScore = 0.52f,
            detectedIssues = listOf("Background Hiss / Fan", "Room Muddiness")
        )
        val quietProfile = cleanProfile.copy(
            noiseFloorDb = -52f, estimatedSnrDb = 20f, integratedLufs = -29f, peakDbfs = -14f
        )
        val loudProfile = cleanProfile.copy(
            noiseFloorDb = -46f, estimatedSnrDb = 24f, integratedLufs = -9.5f, peakDbfs = -0.3f
        )

        val cleanAdj = processor.computeAdaptiveAdjustments(cleanProfile, ProcessingMode.AUTO)
        val noisyAdj = processor.computeAdaptiveAdjustments(noisyProfile, ProcessingMode.AUTO)
        val quietAdj = processor.computeAdaptiveAdjustments(quietProfile, ProcessingMode.AUTO)
        val loudAdj = processor.computeAdaptiveAdjustments(loudProfile, ProcessingMode.AUTO)

        assertTrue("Clean audio should get light noise reduction (${cleanAdj.noiseReduction})", cleanAdj.noiseReduction < 0.35f)
        assertTrue("Noisy audio should get stronger noise reduction (${noisyAdj.noiseReduction})", noisyAdj.noiseReduction > 0.55f)
        assertTrue("Quiet audio should get stronger voice balance (${quietAdj.voiceBalance})", quietAdj.voiceBalance > cleanAdj.voiceBalance)
        assertTrue("Loud audio should pull back volume trim (${loudAdj.voiceVolumeDb})", loudAdj.voiceVolumeDb < 0f)
    }

    @Test
    fun `studio dsp pipeline preserves exact sample duration, improves SNR, supports presets, and prevents clipping`() = runTest {
        val sampleRate = 48_000
        // Intentionally non-multiple of FFT HOP_SIZE (512) to verify zero truncation & exact duration sync
        val totalFrames = sampleRate * 2 + 317
        val tempDir = Files.createTempDirectory("voice_studio_test").toFile()
        try {
            val inputPcm = File(tempDir, "noisy_voice.pcm")
            val outPcm = File(tempDir, "clean_voice.pcm")
            val outWav = File(tempDir, "clean_voice.wav")
            val outMp3 = File(tempDir, "clean_voice.mp3")

            val rng = Random(42)
            FileOutputStream(inputPcm).use { fos ->
                val bytes = ByteArray(totalFrames * 2)
                for (i in 0 until totalFrames) {
                    val t = i.toDouble() / sampleRate
                    val fanNoise = (rng.nextDouble() * 2.0 - 1.0) * 0.032
                    val hum60 = sin(2.0 * PI * 60.0 * t) * 0.022
                    val voice = if (t in 0.5..1.7) {
                        (sin(2.0 * PI * 185.0 * t) * 0.15 +
                            sin(2.0 * PI * 555.0 * t) * 0.08 +
                            sin(2.0 * PI * 1400.0 * t) * 0.04)
                    } else 0.0
                    val sample = ((voice + fanNoise + hum60) * 32767.0).toInt().coerceIn(-32768, 32767)
                    bytes[i * 2] = (sample and 0xFF).toByte()
                    bytes[i * 2 + 1] = ((sample shr 8) and 0xFF).toByte()
                }
                fos.write(bytes)
            }

            val processor = StudioAudioProcessor()
            val analysis = processor.analyzePcmFile(inputPcm, sampleRate, 1)
            val adaptiveAdj = processor.computeAdaptiveAdjustments(analysis.profile, ProcessingMode.AUTO)

            val observedStages = mutableSetOf<String>()
            val result = processor.enhanceAudio(
                inputPcmFile = inputPcm,
                outputPcmFile = outPcm,
                outputWavFile = outWav,
                sampleRate = sampleRate,
                channels = 1,
                adjustments = adaptiveAdj,
                mode = ProcessingMode.AUTO,
                analysis = analysis,
                allowAutoRetry = true,
                onProgress = { observedStages.add(it.stageLabel) }
            )

            assertTrue("Enhanced PCM should exist", result.enhancedPcmFile.exists())
            assertEquals(
                "Output PCM must match exact input frame count",
                inputPcm.length(),
                result.enhancedPcmFile.length()
            )
            assertEquals(
                "Enhanced WAV should equal PCM size + 44-byte header",
                outPcm.length() + 44L,
                result.enhancedWavFile.length()
            )
            assertEquals("Waveform should contain 120 bars", 120, result.enhancedWaveform.size)
            assertTrue("Quality validation duration sync should pass", result.validationReport.durationExactMatch)
            assertTrue("Quality validation peak clip-free check should pass", result.validationReport.peakClipFree)
            assertTrue(
                "Peak should stay safely under 0 dBFS (peak=${result.enhancedProfile.peakDbfs})",
                result.enhancedProfile.peakDbfs <= -0.25f
            )
            assertTrue(
                "Estimated SNR should improve after denoising (orig=${analysis.profile.estimatedSnrDb}, enh=${result.enhancedProfile.estimatedSnrDb})",
                result.enhancedProfile.estimatedSnrDb >= analysis.profile.estimatedSnrDb
            )
            assertTrue("Real processing stages should be reported", observedStages.contains("Preparing enhanced audio"))

            // Verify post-enhancement Gaming Commentary preset overlay + manual fine-tune render
            val gamingAdj = adaptiveAdj.withPresetOverlay(VoicePreset.GAMING_COMMENTARY).copy(
                warmthDb = 1.4f,
                bassDb = -0.5f,
                trebleDb = 1.2f,
                reverbReduction = 0.55f
            )
            val gamingPcm = File(tempDir, "gaming_voice.pcm")
            val gamingWav = File(tempDir, "gaming_voice.wav")
            val gamingResult = processor.enhanceAudio(
                inputPcmFile = inputPcm,
                outputPcmFile = gamingPcm,
                outputWavFile = gamingWav,
                sampleRate = sampleRate,
                channels = 1,
                adjustments = gamingAdj,
                mode = ProcessingMode.AUTO,
                analysis = analysis,
                allowAutoRetry = false,
                onProgress = {}
            )
            assertEquals(inputPcm.length(), gamingResult.enhancedPcmFile.length())
            assertTrue(gamingResult.enhancedProfile.peakDbfs <= -0.25f)

            // Verify genuine Jump3r LAME 320 kbps MP3 encoding
            de.sciss.jump3r.Main().run(
                arrayOf("-b", "320", "-q", "2", "-m", "m", "--silent", outWav.absolutePath, outMp3.absolutePath)
            )
            assertTrue("Encoded 320kbps MP3 should be non-empty", outMp3.exists() && outMp3.length() > 10_000L)
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
