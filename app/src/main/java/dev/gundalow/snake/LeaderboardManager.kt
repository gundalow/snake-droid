package dev.gundalow.snake

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class ScoreEntry(val name: String, val score: Int)

class LeaderboardManager(context: Context) {
    private val prefs = context.getSharedPreferences("leaderboard", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveScore(
        name: String,
        score: Int,
    ) {
        val scores = getTopScores().toMutableList()
        scores.add(ScoreEntry(name, score))
        val limit = 10
        val sorted = scores.sortedByDescending { it.score }.take(limit)
        prefs.edit().putString("scores", gson.toJson(sorted)).apply()
    }

    fun getTopScores(): List<ScoreEntry> {
        val json = prefs.getString("scores", null) ?: return emptyList()
        val type = object : TypeToken<List<ScoreEntry>>() {}.type
        return gson.fromJson(json, type)
    }
}
