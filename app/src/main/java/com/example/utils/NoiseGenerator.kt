package com.example.utils

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import java.util.Random

object NoiseGenerator {
    private var audioTrack: AudioTrack? = null
    private var isPlaying = false
    private var currentType: String? = null

    @Suppress("DEPRECATION")
    @Synchronized
    fun start(type: String) {
        if (isPlaying && currentType == type) return
        stop()

        isPlaying = true
        currentType = type

        Thread {
            try {
                val sampleRate = 22050 // Lower sample rate for efficiency
                val minBufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )

                val track = AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBufferSize * 2,
                    AudioTrack.MODE_STREAM
                )
                audioTrack = track
                track.play()

                val buffer = ShortArray(minBufferSize)
                var lastValue = 0f
                val random = Random()
                var phase = 0.0

                while (isPlaying) {
                    for (i in buffer.indices) {
                        val white = random.nextFloat() * 2f - 1f
                        when (type) {
                            "Rain" -> {
                                // Pink-like rumble low-pass filter
                                lastValue = 0.94f * lastValue + 0.06f * white
                                // Soft crackling drops
                                val drop = if (random.nextFloat() > 0.992f) (random.nextFloat() * 0.35f) else 0f
                                val sample = (lastValue * 0.5f + drop) * 15000
                                buffer[i] = sample.toInt().coerceIn(-32768, 32767).toShort()
                            }
                            "Forest" -> {
                                // Deeper brown noise wind rumble
                                lastValue = 0.985f * lastValue + 0.015f * white
                                phase += 0.0003
                                val windOsc = Math.sin(phase) * Math.sin(phase * 0.37) * 0.4
                                val sample = (lastValue * (0.6f + windOsc.toFloat())) * 15000
                                buffer[i] = sample.toInt().coerceIn(-32768, 32767).toShort()
                            }
                            "Cafe" -> {
                                // Cafe chatter: overlapping low-pass hum & soft mid frequency oscillations
                                lastValue = 0.88f * lastValue + 0.12f * white
                                phase += 0.0015
                                val chatter = Math.sin(phase) * Math.cos(phase * 0.43) * 0.15
                                val clink = if (random.nextFloat() > 0.998f) (random.nextFloat() * 0.2f) else 0f
                                val sample = (lastValue * 0.35f + chatter.toFloat() + clink) * 15000
                                buffer[i] = sample.toInt().coerceIn(-32768, 32767).toShort()
                            }
                            else -> {
                                // Plain Soft White Noise
                                buffer[i] = (white * 4000).toInt().toShort()
                            }
                        }
                    }
                    track.write(buffer, 0, buffer.size)
                }
            } catch (e: Exception) {
                Log.e("NoiseGenerator", "Error in AudioTrack generation loop", e)
            }
        }.start()
    }

    @Synchronized
    fun stop() {
        isPlaying = false
        currentType = null
        try {
            audioTrack?.let {
                if (it.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            Log.e("NoiseGenerator", "Error stopping AudioTrack", e)
        }
        audioTrack = null
    }

    @Synchronized
    fun isPlayingNoise(type: String): Boolean {
        return isPlaying && currentType == type
    }
}
