package com.sec.oneui.clock.alarm.service

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Utility to play a short preview of the selected alarm ringtone
 * at the exact specified volume when the user releases the volume slider.
 */
object AlarmAudioPreviewer {
    private const val TAG = "AlarmAudioPreviewer"
    private var mediaPlayer: MediaPlayer? = null
    private var previewJob: Job? = null

    fun playPreview(
        context: Context,
        volumeFraction: Float,
        customUri: String? = null,
        durationMs: Long = 1800L
    ) {
        stopPreview()

        val clampedVol = volumeFraction.coerceIn(0f, 1f)
        if (clampedVol <= 0.01f) {
            // Muted, nothing to play
            return
        }

        previewJob = CoroutineScope(Dispatchers.Main).launch {
            try {
                val candidateUris = mutableListOf<Uri>()

                if (!customUri.isNullOrBlank()) {
                    try {
                        candidateUris.add(Uri.parse(customUri))
                    } catch (e: Exception) {
                        Log.w(TAG, "Invalid customUri: $customUri")
                    }
                }

                val prefs = context.getSharedPreferences("clock_settings", Context.MODE_PRIVATE)
                val defaultSavedUri = prefs.getString("default_ringtone_uri", null)
                if (!defaultSavedUri.isNullOrBlank()) {
                    try {
                        candidateUris.add(Uri.parse(defaultSavedUri))
                    } catch (e: Exception) {
                        Log.w(TAG, "Invalid defaultSavedUri: $defaultSavedUri")
                    }
                }

                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)?.let { candidateUris.add(it) }
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)?.let { candidateUris.add(it) }
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)?.let { candidateUris.add(it) }

                var player: MediaPlayer? = null
                for (uri in candidateUris) {
                    try {
                        val mp = MediaPlayer().apply {
                            setDataSource(context.applicationContext, uri)
                            setAudioAttributes(
                                AudioAttributes.Builder()
                                    .setUsage(AudioAttributes.USAGE_ALARM)
                                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                    .build()
                            )
                            isLooping = false
                            setVolume(clampedVol, clampedVol)
                            prepare()
                        }
                        player = mp
                        break
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed preparing audio preview for $uri: ${e.message}")
                    }
                }

                if (player != null) {
                    mediaPlayer = player
                    player.start()
                    delay(durationMs)
                    stopPreview()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error playing audio preview: ${e.message}")
                stopPreview()
            }
        }
    }

    fun stopPreview() {
        previewJob?.cancel()
        previewJob = null
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping preview player: ${e.message}")
        } finally {
            mediaPlayer = null
        }
    }
}
