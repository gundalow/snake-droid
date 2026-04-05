package dev.gundalow.snake

import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.random.Random

class SnakeGameEngine {
    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private var currentDirection = Direction.NORTH
    private var nextRequestedDirection: Direction? = null
    private var history = mutableListOf<Offset>()
    private var distanceTraveledSinceLastHistoryPoint = 0f
    private var accumulatedTime = 0f
    private var invulnerabilityTimer = 0f
    private var ufoTimer = 0f
    private var stomperTimer = 0f
    private var stomperActiveTimer = 0f
    private var burpDelayTimer = 0f

    init {
        resetGame()
    }

    fun resetGame(playerName: String = "Snake") {
        _gameState.value = GameState(playerName = playerName)
        currentDirection = Direction.NORTH
        nextRequestedDirection = null
        history.clear()
        history.add(Offset(0f, 0f))
        distanceTraveledSinceLastHistoryPoint = 0f
        accumulatedTime = 0f
        invulnerabilityTimer = GameConstants.INVULNERABILITY_TIME
        ufoTimer = GameConstants.UFO_SPAWN_INTERVAL
        stomperTimer = GameConstants.STOMPER_INTERVAL
        stomperActiveTimer = 0f
        burpDelayTimer = 0f
        spawnFood()
    }

    fun onDirectionRequest(direction: Direction) {
        if (!currentDirection.isOpposite(direction)) {
            nextRequestedDirection = direction
        }
    }

    fun update(deltaTimeSeconds: Float) {
        if (_gameState.value.isGameOver) return
        if (handleInvulnerabilityAndBurp(deltaTimeSeconds)) return

        updateHazards(deltaTimeSeconds)

        val moveSpeed = calculateMoveSpeed()
        var remainingDistance = moveSpeed * deltaTimeSeconds

        while (remainingDistance > 0) {
            val currentPos = _gameState.value.snakeHead
            val boundary = calculateNextGridBoundary(currentPos)
            val distToBoundary = calculateDistanceToBoundary(currentPos, boundary)

            val stepDistance = minOf(remainingDistance, distToBoundary)
            val newPos = currentPos + (currentDirection.offset * stepDistance)
            _gameState.update { it.copy(snakeHead = newPos) }

            updateHistory(stepDistance, newPos)
            remainingDistance -= stepDistance

            if (stepDistance >= distToBoundary) {
                snapAndTurn(boundary, newPos)
            }
        }
        updateSegments()
        checkCollisions()
    }

    private fun handleInvulnerabilityAndBurp(dt: Float): Boolean {
        if (invulnerabilityTimer > 0) invulnerabilityTimer -= dt
        if (burpDelayTimer > 0) {
            burpDelayTimer -= dt
            if (burpDelayTimer <= 0) {
                onBurp()
                _gameState.update { it.copy(isSlowedDown = false) }
                spawnFood()
            }
            return true
        }
        return false
    }

    private fun calculateMoveSpeed(): Float {
        val baseSpeed = _gameState.value.moveSpeed
        val mult = if (_gameState.value.isSlowedDown) GameConstants.MEGA_FOOD_SPEED_MULT else 1.0f
        return baseSpeed * mult
    }

    private fun calculateNextGridBoundary(pos: Offset): Float {
        val eps = GameConstants.GRID_BOUNDARY_EPSILON
        return when (currentDirection) {
            Direction.NORTH -> floor(pos.y - eps)
            Direction.SOUTH -> ceil(pos.y + eps)
            Direction.EAST -> ceil(pos.x + eps)
            Direction.WEST -> floor(pos.x - eps)
        }
    }

    private fun calculateDistanceToBoundary(
        pos: Offset,
        boundary: Float,
    ): Float {
        return when (currentDirection) {
            Direction.NORTH -> pos.y - boundary
            Direction.SOUTH -> boundary - pos.y
            Direction.EAST -> boundary - pos.x
            Direction.WEST -> pos.x - boundary
        }
    }

    private fun updateHistory(
        step: Float,
        pos: Offset,
    ) {
        distanceTraveledSinceLastHistoryPoint += step
        if (distanceTraveledSinceLastHistoryPoint >= GameConstants.HISTORY_RESOLUTION) {
            history.add(0, pos)
            distanceTraveledSinceLastHistoryPoint -= GameConstants.HISTORY_RESOLUTION
            val maxHistory =
                GameConstants.SEGMENT_SPACING *
                    (_gameState.value.score + GameConstants.INITIAL_HISTORY_SIZE)
            if (history.size > maxHistory) history.removeAt(history.size - 1)
        }
    }

    private fun snapAndTurn(
        boundary: Float,
        pos: Offset,
    ) {
        val snappedPos =
            Offset(
                boundary.takeIf { currentDirection == Direction.EAST || currentDirection == Direction.WEST }
                    ?: pos.x.roundToInt().toFloat(),
                boundary.takeIf { currentDirection == Direction.NORTH || currentDirection == Direction.SOUTH }
                    ?: pos.y.roundToInt().toFloat(),
            )
        _gameState.update { it.copy(snakeHead = snappedPos) }
        nextRequestedDirection?.let {
            currentDirection = it
            nextRequestedDirection = null
        }
    }

    private fun updateSegments() {
        val score = _gameState.value.score
        val newSegments = mutableListOf<Offset>()
        for (i in 1..score) {
            val historyIndex = i * GameConstants.SEGMENT_SPACING
            if (historyIndex < history.size) {
                newSegments.add(history[historyIndex])
            }
        }
        _gameState.update { it.copy(bodySegments = newSegments) }
    }

    private fun spawnFood() {
        val nextIsMega = (_gameState.value.foodCount + 1) % GameConstants.MEGA_FOOD_INTERVAL == 0
        var valid = false
        var newFood = Offset(0f, 0f)
        var attempts = 0
        while (!valid && attempts < GameConstants.FOOD_SPAWN_RETRY_LIMIT) {
            val wallDistInt = GameConstants.WALL_DISTANCE.toInt()
            newFood =
                Offset(
                    Random.nextInt(-wallDistInt + 1, wallDistInt - 1).toFloat(),
                    Random.nextInt(-wallDistInt + 1, wallDistInt - 1).toFloat(),
                )
            valid = isFoodPositionValid(newFood)
            attempts++
        }
        _gameState.update {
            it.copy(
                foodPosition = newFood,
                foodType = if (nextIsMega) FoodType.MEGA else FoodType.NORMAL,
                megaBitesLeft = if (nextIsMega) GameConstants.MEGA_FOOD_BITES else 0,
            )
        }
        onFoodSpawned?.invoke()
    }

    private fun isFoodPositionValid(pos: Offset): Boolean {
        val headDist = (_gameState.value.snakeHead - pos).getDistance()
        if (headDist < GameConstants.SNAKE_HEAD_SAFE_DIST) return false
        val bodyConflict =
            _gameState.value.bodySegments.any {
                (it - pos).getDistance() < GameConstants.SNAKE_BODY_SAFE_DIST
            }
        return !bodyConflict
    }

    var onFoodEaten: (() -> Unit)? = null
    var onFoodSpawned: (() -> Unit)? = null
    var onGameOver: (() -> Unit)? = null
    var onUfoSteal: (() -> Unit)? = null
    var onStomp: (() -> Unit)? = null
    var onBurp: () -> Unit = {}
    var onMegaBite: () -> Unit = {}
    var onAchievement: (String) -> Unit = {}

    private fun checkCollisions() {
        val head = _gameState.value.snakeHead
        val wall = GameConstants.WALL_DISTANCE
        if (head.x < -wall || head.x > wall || head.y < -wall || head.y > wall) {
            _gameState.update { it.copy(isGameOver = true) }
            onGameOver?.invoke()
            return
        }

        val foodPos = _gameState.value.foodPosition
        if ((head - foodPos).getDistance() < GameConstants.COLLISION_DIST_FOOD) {
            handleFoodCollision()
        }

        if (invulnerabilityTimer <= 0) {
            _gameState.value.bodySegments.forEach { segment ->
                if ((head - segment).getDistance() < GameConstants.COLLISION_DIST_SELF) {
                    _gameState.update { it.copy(isGameOver = true) }
                    onGameOver?.invoke()
                    return@forEach
                }
            }
        }
    }

    private fun handleFoodCollision() {
        if (_gameState.value.foodType == FoodType.MEGA) {
            handleMegaBite()
        } else {
            _gameState.update {
                it.copy(
                    score = it.score + 1,
                    foodCount = it.foodCount + 1,
                    moveSpeed = it.moveSpeed + GameConstants.SPEED_INCREMENT,
                )
            }
            checkAchievements()
            onFoodEaten?.invoke()
            spawnFood()
        }
    }

    private fun handleMegaBite() {
        val bitesLeft = _gameState.value.megaBitesLeft - 1
        _gameState.update {
            it.copy(
                score = it.score + 1,
                megaBitesLeft = bitesLeft,
                isSlowedDown = true,
            )
        }
        onMegaBite?.invoke()
        if (bitesLeft <= 0) {
            _gameState.update {
                it.copy(
                    foodCount = it.foodCount + 1,
                    moveSpeed = it.moveSpeed + GameConstants.SPEED_INCREMENT,
                )
            }
            checkAchievements()
            burpDelayTimer = GameConstants.BURP_DELAY
        }
    }

    private fun checkAchievements() {
        val score = _gameState.value.score
        val count = _gameState.value.foodCount
        var achievement: String? = null
        if (score % GameConstants.SCORE_ACHIEVEMENT_STEP == 0 && score > 0) {
            achievement = "Score $score! Sssensational!"
        }
        if (count == GameConstants.FOOD_ACHIEVEMENT_10) achievement = "10 Apples! Fruit Loop!"
        if (count == GameConstants.FOOD_ACHIEVEMENT_20) achievement = "20 Apples! Core Strength!"
        if (count == GameConstants.FOOD_ACHIEVEMENT_30) achievement = "30 Apples! Snake Eyes!"
        if (count == GameConstants.FOOD_ACHIEVEMENT_50) achievement = "50 Apples! Legend!"

        achievement?.let {
            _gameState.update { s -> s.copy(achievement = it) }
            onAchievement(it)
        }
    }

    private fun updateHazards(dt: Float) {
        updateUfo(dt)
        updateStomper(dt)
        if (_gameState.value.screenShake > 0) {
            _gameState.update { it.copy(screenShake = maxOf(0f, it.screenShake - dt * GameConstants.SCREEN_SHAKE_DECAY)) }
        }
    }

    private fun updateUfo(dt: Float) {
        ufoTimer -= dt
        if (ufoTimer <= 0) {
            if (_gameState.value.ufoPosition == null) {
                _gameState.update { it.copy(ufoPosition = Offset(GameConstants.UFO_SPAWN_POS, GameConstants.UFO_SPAWN_POS)) }
            } else {
                val ufoPos = _gameState.value.ufoPosition!!
                val target = _gameState.value.foodPosition
                val dir = (target - ufoPos)
                if (dir.getDistance() < GameConstants.UFO_COLLISION_DIST) {
                    _gameState.update { it.copy(ufoPosition = null, score = maxOf(0, it.score - GameConstants.UFO_SCORE_PENALTY)) }
                    onUfoSteal?.invoke()
                    spawnFood()
                    ufoTimer = GameConstants.UFO_SPAWN_INTERVAL
                } else {
                    val move = dir / dir.getDistance() * GameConstants.UFO_SPEED * dt
                    _gameState.update { it.copy(ufoPosition = ufoPos + move) }
                }
            }
        }
    }

    private fun updateStomper(dt: Float) {
        if (stomperActiveTimer > 0) {
            stomperActiveTimer -= dt
            val progress = (GameConstants.STOMPER_DURATION - stomperActiveTimer) / GameConstants.STOMPER_DURATION
            _gameState.update { it.copy(stomperProgress = progress.coerceIn(0f, 1f)) }

            // Check for impact
            val oldTime = GameConstants.STOMPER_DURATION - (stomperActiveTimer + dt)
            val newTime = GameConstants.STOMPER_DURATION - stomperActiveTimer
            if (oldTime < GameConstants.STOMPER_IMPACT_TIME && newTime >= GameConstants.STOMPER_IMPACT_TIME) {
                triggerStomp()
            }

            if (stomperActiveTimer <= 0) {
                _gameState.update { it.copy(stomperProgress = null) }
                stomperTimer = GameConstants.STOMPER_INTERVAL
            }
        } else {
            stomperTimer -= dt
            if (stomperTimer <= 0) {
                stomperActiveTimer = GameConstants.STOMPER_DURATION
            }
        }
    }

    private fun triggerStomp() {
        onStomp?.invoke()
        _gameState.update { it.copy(screenShake = 1.0f) }
        relocateFood()
    }

    private fun relocateFood() {
        val currentType = _gameState.value.foodType
        val currentBites = _gameState.value.megaBitesLeft
        var valid = false
        var newFood = Offset(0f, 0f)
        var attempts = 0
        val wallDistInt = GameConstants.WALL_DISTANCE.toInt()
        while (!valid && attempts < GameConstants.FOOD_SPAWN_RETRY_LIMIT) {
            newFood =
                Offset(
                    Random.nextInt(-wallDistInt + 1, wallDistInt - 1).toFloat(),
                    Random.nextInt(-wallDistInt + 1, wallDistInt - 1).toFloat(),
                )
            valid = isFoodPositionValid(newFood)
            attempts++
        }
        _gameState.update {
            it.copy(
                foodPosition = newFood,
                foodType = currentType,
                megaBitesLeft = currentBites,
            )
        }
        onFoodSpawned?.invoke()
    }
}
