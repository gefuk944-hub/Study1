package com.example.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Precision 64-bit RBJ Audio EQ Cookbook Biquad IIR filter.
 * Zero per-sample heap allocation for fast streaming vocal DSP.
 */
class BiquadFilter {
    private var b0 = 1.0
    private var b1 = 0.0
    private var b2 = 0.0
    private var a1 = 0.0
    private var a2 = 0.0

    private var z1 = 0.0
    private var z2 = 0.0

    fun reset() {
        z1 = 0.0
        z2 = 0.0
    }

    fun process(sample: Float): Float {
        val x = sample.toDouble()
        val y = b0 * x + z1
        z1 = b1 * x - a1 * y + z2
        z2 = b2 * x - a2 * y
        return y.toFloat()
    }

    fun configureHighPass(sampleRate: Int, cutoffHz: Double, q: Double = 0.7071) {
        val freq = cutoffHz.coerceIn(20.0, sampleRate * 0.45)
        val w0 = 2.0 * PI * freq / sampleRate
        val cosW0 = cos(w0)
        val alpha = sin(w0) / (2.0 * q)
        val a0 = 1.0 + alpha
        b0 = ((1.0 + cosW0) / 2.0) / a0
        b1 = -(1.0 + cosW0) / a0
        b2 = ((1.0 + cosW0) / 2.0) / a0
        a1 = (-2.0 * cosW0) / a0
        a2 = (1.0 - alpha) / a0
    }

    fun configureNotch(sampleRate: Int, centerHz: Double, q: Double = 18.0) {
        val freq = centerHz.coerceIn(20.0, sampleRate * 0.45)
        val w0 = 2.0 * PI * freq / sampleRate
        val cosW0 = cos(w0)
        val alpha = sin(w0) / (2.0 * q)
        val a0 = 1.0 + alpha
        b0 = 1.0 / a0
        b1 = (-2.0 * cosW0) / a0
        b2 = 1.0 / a0
        a1 = (-2.0 * cosW0) / a0
        a2 = (1.0 - alpha) / a0
    }

    fun configurePeakingEq(sampleRate: Int, centerHz: Double, gainDb: Double, q: Double = 1.0) {
        val freq = centerHz.coerceIn(20.0, sampleRate * 0.45)
        val a = 10.0.pow(gainDb / 40.0)
        val w0 = 2.0 * PI * freq / sampleRate
        val cosW0 = cos(w0)
        val alpha = sin(w0) / (2.0 * q)
        val a0 = 1.0 + alpha / a
        b0 = (1.0 + alpha * a) / a0
        b1 = (-2.0 * cosW0) / a0
        b2 = (1.0 - alpha * a) / a0
        a1 = (-2.0 * cosW0) / a0
        a2 = (1.0 - alpha / a) / a0
    }

    fun configureLowShelf(sampleRate: Int, shelfHz: Double, gainDb: Double, slope: Double = 0.85) {
        val freq = shelfHz.coerceIn(20.0, sampleRate * 0.45)
        val a = 10.0.pow(gainDb / 40.0)
        val w0 = 2.0 * PI * freq / sampleRate
        val cosW0 = cos(w0)
        val sinW0 = sin(w0)
        val alpha = sinW0 / 2.0 * sqrt((a + 1.0 / a) * (1.0 / slope - 1.0) + 2.0)
        val twoSqrtAAlpha = 2.0 * sqrt(a) * alpha
        val a0 = (a + 1.0) + (a - 1.0) * cosW0 + twoSqrtAAlpha
        b0 = (a * ((a + 1.0) - (a - 1.0) * cosW0 + twoSqrtAAlpha)) / a0
        b1 = (2.0 * a * ((a - 1.0) - (a + 1.0) * cosW0)) / a0
        b2 = (a * ((a + 1.0) - (a - 1.0) * cosW0 - twoSqrtAAlpha)) / a0
        a1 = (-2.0 * ((a - 1.0) + (a + 1.0) * cosW0)) / a0
        a2 = ((a + 1.0) + (a - 1.0) * cosW0 - twoSqrtAAlpha) / a0
    }

    fun configureHighShelf(sampleRate: Int, shelfHz: Double, gainDb: Double, slope: Double = 0.85) {
        val freq = shelfHz.coerceIn(20.0, sampleRate * 0.45)
        val a = 10.0.pow(gainDb / 40.0)
        val w0 = 2.0 * PI * freq / sampleRate
        val cosW0 = cos(w0)
        val sinW0 = sin(w0)
        val alpha = sinW0 / 2.0 * sqrt((a + 1.0 / a) * (1.0 / slope - 1.0) + 2.0)
        val twoSqrtAAlpha = 2.0 * sqrt(a) * alpha
        val a0 = (a + 1.0) - (a - 1.0) * cosW0 + twoSqrtAAlpha
        b0 = (a * ((a + 1.0) + (a - 1.0) * cosW0 + twoSqrtAAlpha)) / a0
        b1 = (-2.0 * a * ((a - 1.0) + (a + 1.0) * cosW0)) / a0
        b2 = (a * ((a + 1.0) + (a - 1.0) * cosW0 - twoSqrtAAlpha)) / a0
        a1 = (2.0 * ((a - 1.0) - (a + 1.0) * cosW0)) / a0
        a2 = ((a + 1.0) - (a - 1.0) * cosW0 - twoSqrtAAlpha) / a0
    }

    fun configureBandPass(sampleRate: Int, centerHz: Double, q: Double = 1.8) {
        val freq = centerHz.coerceIn(20.0, sampleRate * 0.45)
        val w0 = 2.0 * PI * freq / sampleRate
        val cosW0 = cos(w0)
        val alpha = sin(w0) / (2.0 * q)
        val a0 = 1.0 + alpha
        b0 = alpha / a0
        b1 = 0.0
        b2 = -alpha / a0
        a1 = (-2.0 * cosW0) / a0
        a2 = (1.0 - alpha) / a0
    }
}

class DcBlocker(private val r: Float = 0.995f) {
    private var prevIn = 0f
    private var prevOut = 0f

    fun process(sample: Float): Float {
        val out = sample - prevIn + r * prevOut
        prevIn = sample
        prevOut = out
        return out
    }
}
