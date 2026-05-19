package com.mato.syai.note.ai

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

class AudioRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var audioFile: File? = null

    fun startRecording(): File {
        val file = File(context.cacheDir, "recording_temp.m4a")
        audioFile = file

        @Suppress("DEPRECATION")
        recorder = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }).apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(16_000)
            setAudioEncodingBitRate(96_000)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }
        return file
    }

    fun stopRecording(): File? {
        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        recorder = null
        return audioFile
    }
}
