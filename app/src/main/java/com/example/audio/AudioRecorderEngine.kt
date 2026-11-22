package com.example.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

data class RecordedAudioResult(
    val pcmFile: File,
    val wavFile: File,
    val sampleRate: Int,
    val channels: Int,
    val durationMs: Long
)

/**
 * Low-latency live PCM voice recorder backed by Android AudioRecord.
 * Captures clean uncompressed 16-bit 48 kHz (or 44.1 kHz fallback) audio directly to
 * app-private temporary storage with live amplitude callbacks and pause/resume support.
 */
class AudioRecorderEngine(
    private val processor: StudioAudioProcessor
) {
    private var audioRecord: AudioRecord? = null
    private var recordJob: Job? = null
    private val isRecordingFlag = AtomicBoolean(false)
    private val isPausedFlag = AtomicBoolean(false)

    private var activePcmFile: File? = null
    private var activeWavFile: File? = null
    private var activeSampleRate: Int = 48_000
    private var totalFramesWritten: Long = 0L

    @SuppressLint("MissingPermission")
    fun startRecording(
        scope: CoroutineScope,
        pcmFile: File,
        wavFile: File,
        onTick: (durationMs: Long, normalizedLevel: Float) -> Unit,
        onError: (String) -> Unit
    ): Boolean {
        if (isRecordingFlag.get()) return false

        val candidateRates = intArrayOf(48_000, 44_100)
        var createdRecord: AudioRecord? = null
        var chosenRate = 48_000
        var chosenBufBytes = 8192

        for (rate in candidateRates) {
            val minBuf = AudioRecord.getMinBufferSize(
                rate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            if (minBuf <= 0) continue
            val bufSize = max(minBuf * 2, 8192)
            // Prefer VOICE_RECOGNITION or UNPROCESSED / MIC for clean raw vocal capture
            val sources = intArrayOf(
                MediaRecorder.AudioSource.MIC,
                MediaRecorder.AudioSource.VOICE_RECOGNITION
            )
            for (src in sources) {
                try {
                    val rec = AudioRecord(
                        src,
                        rate,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufSize
                    )
                    if (rec.state == AudioRecord.STATE_INITIALIZED) {
                        createdRecord = rec
                        chosenRate = rate
                        chosenBufBytes = bufSize
                        break
                    } else {
                        rec.release()
                    }
                } catch (_: Exception) {
                    // Try next audio source / sample rate
                }
            }
            if (createdRecord != null) break
        }

        val recorder = createdRecord ?: run {
            onError("Could not initialize microphone hardware on this device.")
            return false
        }

        activePcmFile = pcmFile
        activeWavFile = wavFile
        activeSampleRate = chosenRate
        totalFramesWritten = 0L
        isPausedFlag.set(false)
        isRecordingFlag.set(true)
        audioRecord = recorder

        try {
            recorder.startRecording()
        } catch (e: Exception) {
            isRecordingFlag.set(false)
            recorder.release()
            audioRecord = null
            onError("Failed to start microphone capture: ${e.localizedMessage ?: "Unknown error"}")
            return false
        }

        recordJob = scope.launch(Dispatchers.IO) {
            val shortBuf = ShortArray(chosenBufBytes / 2)
            val byteBuf = ByteArray(shortBuf.size * 2)
            var lastUiTickMs = 0L

            try {
                BufferedOutputStream(FileOutputStream(pcmFile), 32768).use { fos ->
                    while (isActive && isRecordingFlag.get()) {
                        val readShorts = recorder.read(shortBuf, 0, shortBuf.size)
                        if (readShorts <= 0) continue
                        if (isPausedFlag.get()) continue

                        var sqSum = 0.0
                        var peak = 0
                        for (i in 0 until readShorts) {
                            val s = shortBuf[i].toInt()
                            val a = abs(s)
                            if (a > peak) peak = a
                            sqSum += s * s
                            byteBuf[i * 2] = (s and 0xFF).toByte()
                            byteBuf[i * 2 + 1] = ((s shr 8) and 0xFF).toByte()
                        }

                        fos.write(byteBuf, 0, readShorts * 2)
                        totalFramesWritten += readShorts

                        val nowMs = System.currentTimeMillis()
                        if (nowMs - lastUiTickMs >= 55L) {
                            lastUiTickMs = nowMs
                            val rms = sqrt(sqSum / readShorts).toFloat() / 32768f
                            // Perceptual log-like boost so normal speech visibly animates meters
                            val visualLevel = (sqrt(rms * 3.8f)).coerceIn(0.05f, 1f)
                            val durationMs = (totalFramesWritten * 1000L) / chosenRate
                            withContext(Dispatchers.Main) {
                                onTick(durationMs, visualLevel)
                            }
                        }
                    }
                    fos.flush()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError("Recording interrupted: ${e.localizedMessage ?: "I/O error"}")
                }
            } finally {
                try {
                    recorder.stop()
                } catch (_: Exception) {
                }
                recorder.release()
                audioRecord = null
            }
        }
        return true
    }

    fun setPaused(paused: Boolean) {
        isPausedFlag.set(paused)
    }

    suspend fun stopRecording(): RecordedAudioResult? = withContext(Dispatchers.IO) {
        if (!isRecordingFlag.getAndSet(false)) return@withContext null
        isPausedFlag.set(false)
        recordJob?.join()
        recordJob = null

        val pcmFile = activePcmFile ?: return@withContext null
        val wavFile = activeWavFile ?: return@withContext null
        if (!pcmFile.exists() || pcmFile.length() < 640L) {
            pcmFile.delete()
            return@withContext null
        }

        // Wrap recorded PCM into an instant-preview WAV file
        val pcmLen = pcmFile.length()
        BufferedOutputStream(FileOutputStream(wavFile), 32768).use { wavOut ->
            processor.writeWavHeader(
                out = wavOut,
                dataLengthBytes = pcmLen,
                sampleRate = activeSampleRate,
                channels = 1,
                bitsPerSample = 16
            )
            FileInputStream(pcmFile).use { pcmIn ->
                val copyBuf = ByteArray(16384)
                while (true) {
                    val n = pcmIn.read(copyBuf)
                    if (n <= 0) break
                    wavOut.write(copyBuf, 0, n)
                }
            }
        }

        val durationMs = (pcmLen / 2L) * 1000L / activeSampleRate
        RecordedAudioResult(
            pcmFile = pcmFile,
            wavFile = wavFile,
            sampleRate = activeSampleRate,
            channels = 1,
            durationMs = durationMs
        )
    }

    fun release() {
        isRecordingFlag.set(false)
        recordJob?.cancel()
        try {
            audioRecord?.stop()
        } catch (_: Exception) {
        }
        audioRecord?.release()
        audioRecord = null
    }
}
