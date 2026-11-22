package com.example.audio

import de.sciss.jump3r.Main as Jump3rMain
import java.io.File

/**
 * Isolated MP3 LAME encoder wrapper.
 * Kept in a standalone class so ART never loads or verifies jump3r bytecode
 * during app startup or normal UI rendering—only when exporting an MP3 file.
 */
object Mp3Jump3rEncoder {
    fun encodeWavToMp3(
        inputWavFile: File,
        outMp3File: File,
        bitrateKbps: Int,
        channels: Int
    ) {
        val mode = if (channels == 1) "m" else "j"
        val args = arrayOf(
            "-b", bitrateKbps.toString(),
            "-q", "2",
            "-m", mode,
            "--silent",
            inputWavFile.absolutePath,
            outMp3File.absolutePath
        )
        try {
            val lame = Jump3rMain()
            lame.run(args)
        } catch (e: LinkageError) {
            throw IllegalStateException(
                "MP3 encoder is unavailable on this Android build. Please export as M4A or WAV instead.",
                e
            )
        }
        if (!outMp3File.exists() || outMp3File.length() < 128L) {
            throw IllegalStateException("MP3 encoding did not produce a valid audio file.")
        }
    }
}
