package vn.familytv.parentalcontrol.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.util.Log
import android.view.KeyEvent

class AudioFocusInterrupter(private val context: Context) {
    companion object {
        private const val TAG = "FamilyTime_Audio"
    }

    private val audioManager = context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var focusRequest: AudioFocusRequest? = null
    private var hasFocus: Boolean = false
    private var wasMuted: Boolean = false

    @Synchronized
    fun acquireFocus(): Boolean {
        if (hasFocus) return true
        val am = audioManager ?: return false

        // 1. Request USAGE_MEDIA with AUDIOFOCUS_GAIN (full media focus takeover)
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()

        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(attributes)
            .setAcceptsDelayedFocusGain(false)
            .setOnAudioFocusChangeListener { change ->
                Log.i(TAG, "AUDIOFOCUS_CHANGED change=$change")
            }
            .build()

        val result = am.requestAudioFocus(request)
        hasFocus = (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
        focusRequest = request
        Log.i(TAG, "AUDIOFOCUS_REQUEST result=$result granted=$hasFocus")

        // 2. Dispatch Media Pause key event to actively pause active media session (e.g. MyTV, YouTube)
        runCatching {
            am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE))
            am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PAUSE))
            Log.i(TAG, "DISPATCH_MEDIA_PAUSE sent")
        }

        // 3. Guaranteed silence & Visual Mute: Mute STREAM_MUSIC with FLAG_SHOW_UI so TV displays the 🔇 icon
        runCatching {
            val isAlreadyMuted = am.isStreamMute(AudioManager.STREAM_MUSIC)
            if (!isAlreadyMuted) {
                am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, AudioManager.FLAG_SHOW_UI)
                wasMuted = true
                Log.i(TAG, "STREAM_MUSIC muted with FLAG_SHOW_UI")
            }
        }

        return hasFocus
    }

    @Synchronized
    fun releaseFocus() {
        if (!hasFocus && !wasMuted) return
        val am = audioManager ?: return

        focusRequest?.let {
            am.abandonAudioFocusRequest(it)
            Log.i(TAG, "AUDIOFOCUS_ABANDONED")
        }
        focusRequest = null
        hasFocus = false

        // Unmute STREAM_MUSIC with UI indication
        if (wasMuted) {
            runCatching {
                am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, AudioManager.FLAG_SHOW_UI)
                Log.i(TAG, "STREAM_MUSIC unmuted with FLAG_SHOW_UI")
            }
            wasMuted = false
        }

        // Dispatch Media Play to resume video
        runCatching {
            am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY))
            am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY))
            Log.i(TAG, "DISPATCH_MEDIA_PLAY sent")
        }
    }

    @Synchronized
    fun hasFocus(): Boolean = hasFocus
}
