package dev.gundalow.snake

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log

class SoundManager(context: Context) {
    private val tag = "SoundManager"
    private val soundPool: SoundPool
    private val soundMap = mutableMapOf<String, Int>()

    init {
        val audioAttributes =
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

        soundPool =
            SoundPool.Builder()
                .setMaxStreams(5)
                .setAudioAttributes(audioAttributes)
                .build()

        loadSound(context, "whoosh", "sounds/whoosh.wav")
        loadSound(context, "apple", "sounds/foods/apple.ogg")
        loadSound(context, "game_over", "sounds/game_over.wav")
        loadSound(context, "chew", "sounds/foods/mega_melon/chew.ogg")
        loadSound(context, "burp1", "sounds/foods/mega_burps/burp1.ogg")
        loadSound(context, "burp2", "sounds/foods/mega_burps/burp2.ogg")
        loadSound(context, "burp3", "sounds/foods/mega_burps/burp3.ogg")
        loadSound(context, "impact", "sounds/impact.wav")
        loadSound(context, "tractor_beam", "sounds/tractor_beam.wav")
    }

    private fun loadSound(
        context: Context,
        key: String,
        assetPath: String,
    ) {
        try {
            val assetFileDescriptor = context.assets.openFd(assetPath)
            val priority = 1
            val soundId = soundPool.load(assetFileDescriptor, priority)
            soundMap[key] = soundId
        } catch (e: Exception) {
            Log.e(tag, "Error loading sound $assetPath", e)
        }
    }

    fun playSound(key: String) {
        soundMap[key]?.let { soundId ->
            soundPool.play(soundId, 1f, 1f, 0, 0, 1f)
        }
    }

    fun release() {
        soundPool.release()
    }
}
