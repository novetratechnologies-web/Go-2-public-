package com.example.utils

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Base64
import android.util.Log
import java.io.File

class AudioRecorder(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null

    @Suppress("DEPRECATION")
    fun startRecording(): Boolean {
        try {
            audioFile = File(context.cacheDir, "gemini_voice.m4a")
            if (audioFile?.exists() == true) {
                audioFile?.delete()
            }

            // Create MediaRecorder instance compatible with current build SDK
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                MediaRecorder()
            }

            mediaRecorder = recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(audioFile?.absolutePath)
                prepare()
                start()
            }
            Log.d("AudioRecorder", "Recording started: ${audioFile?.absolutePath}")
            return true
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Failed to start recording", e)
            mediaRecorder?.release()
            mediaRecorder = null
            return false
        }
    }

    fun stopRecording(): String? {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Failed to stop media recorder", e)
        } finally {
            mediaRecorder = null
        }

        val file = audioFile ?: return null
        if (!file.exists()) return null

        return try {
            val bytes = file.readBytes()
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Failed to read recorded audio file bytes", e)
            null
        }
    }
}
