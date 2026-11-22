package com.example.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Sample-exact streaming STFT Decision-Directed Wiener Spectral Denoiser.
 *
 * Key upgrades for natural human voice preservation:
 * - Exact 512-sample overlap-add latency compensation + clean flush() so output
 *   matches input frame count down to the exact sample (zero sync drift, zero
 *   cut-off sentence endings, zero chunk-boundary crackles).
 * - 3-bin frequency-domain gain smoothing to prevent metallic / underwater artifacts.
 * - Adaptive noise-condition scaling: backs off automatically on already quiet audio.
 * - Vocal formant & Hindi/Hinglish/English consonant protection band.
 */
class SpectralDenoiser(
    private val sampleRate: Int,
    initialNoisePower: FloatArray,
    private val effectiveStrength: Float,
    private val minSpectralFloor: Float,
    private val reverbReductionStrength: Float
) {
    companion object {
        const val FFT_SIZE = 1024
        const val HOP_SIZE = 512
        const val NUM_BINS = FFT_SIZE / 2 + 1

        fun createHannWindow(size: Int): FloatArray {
            return FloatArray(size) { i ->
                (0.5 * (1.0 - cos(2.0 * PI * i / size))).toFloat()
            }
        }

        fun fftInPlace(real: FloatArray, imag: FloatArray, inverse: Boolean) {
            val n = real.size
            var j = 0
            for (i in 1 until n) {
                var bit = n shr 1
                while (j and bit != 0) {
                    j = j xor bit
                    bit = bit shr 1
                }
                j = j xor bit
                if (i < j) {
                    val tr = real[i]; real[i] = real[j]; real[j] = tr
                    val ti = imag[i]; imag[i] = imag[j]; imag[j] = ti
                }
            }
            var len = 2
            while (len <= n) {
                val halfLen = len shr 1
                val angle = (if (inverse) 2.0 else -2.0) * PI / len
                val wLenR = cos(angle).toFloat()
                val wLenI = sin(angle).toFloat()
                var i = 0
                while (i < n) {
                    var wR = 1f
                    var wI = 0f
                    for (k in 0 until halfLen) {
                        val uR = real[i + k]
                        val uI = imag[i + k]
                        val vR = real[i + k + halfLen] * wR - imag[i + k + halfLen] * wI
                        val vI = real[i + k + halfLen] * wI + imag[i + k + halfLen] * wR
                        real[i + k] = uR + vR
                        imag[i + k] = uI + vI
                        real[i + k + halfLen] = uR - vR
                        imag[i + k + halfLen] = uI - vI
                        val nextWR = wR * wLenR - wI * wLenI
                        wI = wR * wLenI + wI * wLenR
                        wR = nextWR
                    }
                    i += len
                }
                len = len shl 1
            }
            if (inverse) {
                val invN = 1f / n
                for (i in 0 until n) {
                    real[i] *= invN
                    imag[i] *= invN
                }
            }
        }
    }

    private val sqrtHann = FloatArray(FFT_SIZE) { i ->
        sqrt(0.5 * (1.0 - cos(2.0 * PI * i / FFT_SIZE))).toFloat()
    }

    private val noisePower = FloatArray(NUM_BINS) { idx ->
        max(initialNoisePower.getOrElse(idx) { 1e-9f }, 1e-10f)
    }
    private val prevCleanPower = FloatArray(NUM_BINS)
    private val prevGain = FloatArray(NUM_BINS) { 1f }
    private val rawGain = FloatArray(NUM_BINS) { 1f }
    private val reverberantTail = FloatArray(NUM_BINS)

    private val inputRing = FloatArray(FFT_SIZE)
    private val overlapBuffer = FloatArray(HOP_SIZE)
    private val fftReal = FloatArray(FFT_SIZE)
    private val fftImag = FloatArray(FFT_SIZE)
    private val hopOut = FloatArray(HOP_SIZE)

    private var ringFill = 0
    private var totalFedSamples = 0L
    private var totalEmittedSamples = 0L
    private var warmupSamplesRemaining = HOP_SIZE

    private val hzPerBin = sampleRate.toFloat() / FFT_SIZE
    private val voiceLowBin = (110f / hzPerBin).toInt().coerceIn(2, NUM_BINS - 1)
    private val voiceHighBin = (4600f / hzPerBin).toInt().coerceIn(voiceLowBin + 1, NUM_BINS - 1)
    private val clickLowBin = (3400f / hzPerBin).toInt().coerceIn(voiceLowBin + 1, NUM_BINS - 1)

    /**
     * Streams arbitrary-length mono chunks through overlap-add STFT.
     * Returns newly ready latency-compensated samples.
     */
    fun processChunk(samples: FloatArray, count: Int): FloatArray {
        if (count <= 0) return FloatArray(0)
        if (effectiveStrength <= 0.01f && reverbReductionStrength <= 0.02f) {
            totalFedSamples += count
            totalEmittedSamples += count
            return samples.copyOf(count)
        }

        totalFedSamples += count
        val maxPossibleHops = ((ringFill + count) / HOP_SIZE) + 1
        val outBuf = FloatArray(maxPossibleHops * HOP_SIZE)
        var outCount = 0

        var inPos = 0
        while (inPos < count) {
            val needed = HOP_SIZE - ringFill
            val toCopy = min(needed, count - inPos)
            System.arraycopy(samples, inPos, inputRing, HOP_SIZE + ringFill, toCopy)
            ringFill += toCopy
            inPos += toCopy

            if (ringFill == HOP_SIZE) {
                synthesizeHop()
                outCount = appendCompensatedHop(outBuf, outCount)
                System.arraycopy(inputRing, HOP_SIZE, inputRing, 0, HOP_SIZE)
                ringFill = 0
            }
        }

        totalEmittedSamples += outCount
        return if (outCount == outBuf.size) outBuf else outBuf.copyOf(outCount)
    }

    /**
     * Drains all remaining samples at EOF so total output frame count equals
     * total input frame count with zero truncation and smooth sentence endings.
     */
    fun flush(): FloatArray {
        val remainingNeeded = (totalFedSamples - totalEmittedSamples).toInt()
        if (remainingNeeded <= 0) return FloatArray(0)
        if (effectiveStrength <= 0.01f && reverbReductionStrength <= 0.02f) return FloatArray(0)

        val outBuf = FloatArray(remainingNeeded + HOP_SIZE * 2)
        var outCount = 0

        // Zero-pad pending partial hop + one trailing overlap hop
        val zeros = FloatArray(HOP_SIZE)
        while (outCount < remainingNeeded) {
            val needed = HOP_SIZE - ringFill
            System.arraycopy(zeros, 0, inputRing, HOP_SIZE + ringFill, needed)
            synthesizeHop()
            outCount = appendCompensatedHop(outBuf, outCount)
            System.arraycopy(inputRing, HOP_SIZE, inputRing, 0, HOP_SIZE)
            ringFill = 0
        }

        val exactLen = min(remainingNeeded, outCount)
        totalEmittedSamples += exactLen
        return outBuf.copyOf(exactLen)
    }

    private fun appendCompensatedHop(dest: FloatArray, destOffset: Int): Int {
        var offset = destOffset
        if (warmupSamplesRemaining >= HOP_SIZE) {
            warmupSamplesRemaining -= HOP_SIZE
            return offset
        }
        val start = warmupSamplesRemaining
        warmupSamplesRemaining = 0
        val len = HOP_SIZE - start
        System.arraycopy(hopOut, start, dest, offset, len)
        offset += len
        return offset
    }

    private fun synthesizeHop() {
        for (i in 0 until FFT_SIZE) {
            fftReal[i] = inputRing[i] * sqrtHann[i]
            fftImag[i] = 0f
        }
        fftInPlace(fftReal, fftImag, inverse = false)

        // Frame-level vocal band energy vs noise floor
        var voiceEnergy = 0f
        var voiceNoise = 0f
        for (k in voiceLowBin..voiceHighBin) {
            val p = fftReal[k] * fftReal[k] + fftImag[k] * fftImag[k]
            voiceEnergy += p
            voiceNoise += noisePower[k]
        }
        val frameSnr = voiceEnergy / max(voiceNoise, 1e-10f)
        val isLikelySpeech = frameSnr > 2.2f

        // High-band transient click detection (keyboard / mouse taps) during unvoiced moments
        var highBandEnergy = 0f
        var highBandNoise = 0f
        for (k in clickLowBin until NUM_BINS) {
            highBandEnergy += fftReal[k] * fftReal[k] + fftImag[k] * fftImag[k]
            highBandNoise += noisePower[k]
        }
        val isTransientClick = !isLikelySpeech && (highBandEnergy / max(highBandNoise, 1e-10f) > 5.0f)

        // Conservative oversubtraction & floor keep voice warm, human, and never underwater
        val strength = effectiveStrength.coerceIn(0f, 1f)
        val overSub = 0.72f + strength * 1.15f
        val spectralFloor = max(minSpectralFloor, 0.30f - strength * 0.21f).coerceIn(0.08f, 0.34f)
        val decisionAlpha = 0.95f

        for (k in 0 until NUM_BINS) {
            val re = fftReal[k]
            val im = fftImag[k]
            val power = re * re + im * im

            // Gentle online noise tracking strictly on confirmed quiet non-click frames
            if (!isLikelySpeech && !isTransientClick) {
                noisePower[k] = 0.975f * noisePower[k] + 0.025f * power
            }

            var effectiveNoise = noisePower[k] * overSub
            val reverbAmount = reverbReductionStrength.coerceIn(0f, 1f)
            if (reverbAmount > 0.02f) {
                reverberantTail[k] = 0.74f * reverberantTail[k] + 0.26f * prevCleanPower[k]
                val echoWeight = (0.04f + 0.24f * reverbAmount) * if (isLikelySpeech) 0.38f else 0.96f
                effectiveNoise += reverberantTail[k] * echoWeight
            }

            // Decision-directed a-priori SNR estimation
            val postSnr = power / max(effectiveNoise, 1e-10f)
            val instPrior = max(postSnr - 1f, 0f)
            val prevSnr = prevCleanPower[k] / max(effectiveNoise, 1e-10f)
            val priorSnr = decisionAlpha * prevSnr + (1f - decisionAlpha) * instPrior

            var g = priorSnr / (1f + priorSnr)

            if (isTransientClick && k >= clickLowBin) {
                g *= (1f - 0.50f * strength)
            }

            // Protect core human speech & Hindi/Hinglish consonant band during active speech
            val floorForBin = if (isLikelySpeech && k in voiceLowBin..voiceHighBin) {
                max(spectralFloor, 0.20f)
            } else {
                spectralFloor
            }
            rawGain[k] = g.coerceIn(floorForBin, 1f)
        }

        // 3-bin frequency smoothing eliminates narrow-band metallic musical-noise spikes
        for (k in 0 until NUM_BINS) {
            val left = rawGain[max(0, k - 1)]
            val mid = rawGain[k]
            val right = rawGain[min(NUM_BINS - 1, k + 1)]
            val freqSmoothed = 0.20f * left + 0.60f * mid + 0.20f * right

            // Fast attack for crisp syllables/plosives, smooth natural release for word endings
            val smoothGain = if (freqSmoothed > prevGain[k]) {
                0.28f * prevGain[k] + 0.72f * freqSmoothed
            } else {
                0.78f * prevGain[k] + 0.22f * freqSmoothed
            }
            prevGain[k] = smoothGain

            val mixAmount = max(strength, reverbReductionStrength.coerceIn(0f, 1f) * 0.72f).coerceIn(0f, 1f)
            val finalGain = 1f - mixAmount + mixAmount * smoothGain
            val cleanRe = fftReal[k] * finalGain
            val cleanIm = fftImag[k] * finalGain
            prevCleanPower[k] = cleanRe * cleanRe + cleanIm * cleanIm

            fftReal[k] = cleanRe
            fftImag[k] = cleanIm
            if (k in 1 until (FFT_SIZE / 2)) {
                fftReal[FFT_SIZE - k] = cleanRe
                fftImag[FFT_SIZE - k] = -cleanIm
            }
        }

        fftInPlace(fftReal, fftImag, inverse = true)

        for (i in 0 until HOP_SIZE) {
            hopOut[i] = overlapBuffer[i] + fftReal[i] * sqrtHann[i]
            overlapBuffer[i] = fftReal[i + HOP_SIZE] * sqrtHann[i + HOP_SIZE]
        }
    }
}
