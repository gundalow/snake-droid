package com.example.app

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

class SoundManager(context: Context) {
    private val soundPool: SoundPool
    private val soundMap = mutableMapOf<String, Int>()

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(audioAttributes)
            .build()

        loadSound(context, "whoosh", "sounds/whoosh.wav")
        loadSound(context, "apple", "sounds/foods/apple.ogg")
        loadSound(context, "game_over", "sounds/game_over.wav")
    }

    private fun loadSound(context: Context, key: String, assetPath: String) {
        try {
            val assetFileDescriptor = context.assets.openFd(assetPath)
            val soundId = soundPool.load(assetFileDescriptor, 1)
            soundMap[key] = soundId
        } catch (e: Exception) {
            e.printStackTrace()
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
