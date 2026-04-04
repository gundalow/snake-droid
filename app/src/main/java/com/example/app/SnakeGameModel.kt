package com.example.app

import androidx.compose.ui.geometry.Offset

object GameConstants {
    const val HISTORY_RESOLUTION = 0.1f // Distance between recorded path points.
    const val SEGMENT_SPACING = 10 // Number of history points between segments.
    const val GRID_SIZE = 1.0f // Logical grid unit for snapping and turning.
    const val INITIAL_MOVE_SPEED = 5.0f // Starting units per second.
    const val SPEED_INCREMENT = 0.2f // Speed increase per food item.
    const val INVULNERABILITY_TIME = 0.5f // Initial grace period for self-collision (seconds).
    const val BOARD_SIZE = 28.0f // Width/Length of the playable area.
    const val WALL_DISTANCE = 14.0f // Half-width of the square board (centered at 0,0).
    const val UFO_SPAWN_INTERVAL = 30.0f
    const val UFO_SPEED = 10.0f
    const val UFO_SCORE_PENALTY = 5
    const val STOMPER_INTERVAL = 30.0f
    const val MEGA_FOOD_BITES = 3
    const val MEGA_FOOD_SPEED_MULT = 0.5f
}

enum class FoodType {
    NORMAL, MEGA
}

enum class Direction(val offset: Offset) {
    NORTH(Offset(0f, -1f)),
    SOUTH(Offset(0f, 1f)),
    EAST(Offset(1f, 0f)),
    WEST(Offset(-1f, 0f));

    fun isOpposite(other: Direction): Boolean {
        return (this == NORTH && other == SOUTH) ||
                (this == SOUTH && other == NORTH) ||
                (this == EAST && other == WEST) ||
                (this == WEST && other == EAST)
    }
}

data class SnakeTransform(
    val position: Offset,
    val direction: Direction
)

data class GameState(
    val snakeHead: Offset = Offset(0f, 0f),
    val snakeDirection: Direction = Direction.NORTH,
    val bodySegments: List<Offset> = emptyList(),
    val foodPosition: Offset = Offset(5f, 5f),
    val foodType: FoodType = FoodType.NORMAL,
    val megaBitesLeft: Int = 0,
    val score: Int = 0,
    val isGameOver: Boolean = false,
    val moveSpeed: Float = GameConstants.INITIAL_MOVE_SPEED,
    val isSlowedDown: Boolean = false,
    val ufoPosition: Offset? = null,
    val stomperActive: Boolean = false,
    val playerName: String = "Snake",
    val achievement: String? = null,
    val foodCount: Int = 0,
    val screenShake: Float = 0f
)
