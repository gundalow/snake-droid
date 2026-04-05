package dev.gundalow.snake

import androidx.compose.ui.geometry.Offset

object GameConstants {
    const val HISTORY_RESOLUTION = 0.1f
    const val SEGMENT_SPACING = 10
    const val GRID_SIZE = 1.0f
    const val INITIAL_MOVE_SPEED = 5.0f
    const val SPEED_INCREMENT = 0.2f
    const val INVULNERABILITY_TIME = 0.5f
    const val BOARD_SIZE = 31.0f
    const val WALL_DISTANCE = 15.5f
    const val UFO_SPAWN_INTERVAL = 30.0f
    const val UFO_SPEED = 10.0f
    const val UFO_SCORE_PENALTY = 5
    const val STOMPER_INTERVAL = 30.0f
    const val STOMPER_DURATION = 3.0f // Total time for shadow, stomp, and ascent
    const val STOMPER_IMPACT_TIME = 2.0f // Time when stomp hits ground
    const val MEGA_FOOD_BITES = 3
    const val MEGA_FOOD_SPEED_MULT = 0.5f
    const val FOOD_SPAWN_RETRY_LIMIT = 50
    const val SNAKE_HEAD_SAFE_DIST = 2.0f
    const val SNAKE_BODY_SAFE_DIST = 1.0f
    const val INITIAL_HISTORY_SIZE = 5
    const val COLLISION_DIST_FOOD = 0.8f
    const val COLLISION_DIST_SELF = 0.5f
    const val BURP_DELAY = 0.5f
    const val SCREEN_SHAKE_DECAY = 4.0f
    const val SCREEN_SHAKE_MAGNITUDE = 50f
    const val SCORE_ACHIEVEMENT_STEP = 10
    const val FOOD_ACHIEVEMENT_10 = 10
    const val FOOD_ACHIEVEMENT_20 = 20
    const val FOOD_ACHIEVEMENT_30 = 30
    const val FOOD_ACHIEVEMENT_50 = 50
    const val UFO_COLLISION_DIST = 0.5f
    const val UFO_SPAWN_POS = -15.0f
    const val FPS_60_DELAY = 16L
    const val ACHIEVEMENT_DISPLAY_TIME = 3000L
    const val SWIPE_THRESHOLD = 50f
    const val SHAKE_INTENSITY = 50f
    const val GRID_BOUNDARY_EPSILON = 0.001f
    const val FOOD_RADIUS_BASE = 0.4f
    const val FOOD_RADIUS_GROWTH = 0.2f
    const val UFO_RADIUS = 0.8f
    const val TRACTOR_BEAM_WIDTH = 5f
    const val SNAKE_BODY_SIZE_MULT = 0.9f
    const val SNAKE_HEAD_SIZE_MULT = 1.0f
    const val UI_PADDING_TOP = 64
    const val GAME_OVER_ALPHA = 0.7f
    const val NAME_ENTRY_ALPHA = 0.8f
    const val MEGA_FOOD_INTERVAL = 5
    const val MAX_SCORES = 10
    const val SHADOW_ALPHA_MAX = 0.3f
    const val STOMP_HEIGHT_FACTOR = 1000f
    const val STOMP_VISUAL_DIVISOR = 10f
    const val RESTART_BUTTON_SPACING = 24
    const val NAME_LIST_HEIGHT = 100
    const val STOMPER_FOOT_SIZE = 5f
    const val STOMPER_HEIGHT_THRESHOLD = 1000f
}

enum class FoodType {
    NORMAL,
    MEGA,
}

enum class Direction(val offset: Offset) {
    NORTH(Offset(0f, -1f)),
    SOUTH(Offset(0f, 1f)),
    EAST(Offset(1f, 0f)),
    WEST(Offset(-1f, 0f)),
    ;

    fun isOpposite(other: Direction): Boolean {
        return (this == NORTH && other == SOUTH) ||
            (this == SOUTH && other == NORTH) ||
            (this == EAST && other == WEST) ||
            (this == WEST && other == EAST)
    }
}

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
    val stomperProgress: Float? = null,
    val playerName: String = "Snake",
    val achievement: String? = null,
    val foodCount: Int = 0,
    val screenShake: Float = 0f,
    val isInsideMegaFood: Boolean = false,
)
