package com.example.app

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

        if (invulnerabilityTimer > 0) {
            invulnerabilityTimer -= deltaTimeSeconds
        }

        if (burpDelayTimer > 0) {
            burpDelayTimer -= deltaTimeSeconds
            if (burpDelayTimer <= 0) {
                onBurp()
                _gameState.update { it.copy(isSlowedDown = false) }
                spawnFood()
            }
            return
        }

        updateHazards(deltaTimeSeconds)

        val baseSpeed = _gameState.value.moveSpeed
        val multiplier = if (_gameState.value.isSlowedDown) GameConstants.MEGA_FOOD_SPEED_MULT else 1.0f
        val moveSpeed = baseSpeed * multiplier
        val distanceToMove = moveSpeed * deltaTimeSeconds

        var remainingDistance = distanceToMove
        while (remainingDistance > 0) {
            val currentPos = _gameState.value.snakeHead

            // Calculate distance to next grid boundary in current direction
            // If we are at 0.0 and moving NORTH (-y), the next boundary is -1.0.
            // If we are at -0.1 and moving NORTH, the next boundary is still -1.0.
            val nextGridBoundary = when (currentDirection) {
                Direction.NORTH -> floor(currentPos.y - 0.001f)
                Direction.SOUTH -> ceil(currentPos.y + 0.001f)
                Direction.EAST -> ceil(currentPos.x + 0.001f)
                Direction.WEST -> floor(currentPos.x - 0.001f)
            }

            val distanceToBoundary = when (currentDirection) {
                Direction.NORTH -> currentPos.y - nextGridBoundary
                Direction.SOUTH -> nextGridBoundary - currentPos.y
                Direction.EAST -> nextGridBoundary - currentPos.x
                Direction.WEST -> currentPos.x - nextGridBoundary
            }

            val stepDistance = minOf(remainingDistance, distanceToBoundary)
            val newPos = currentPos + (currentDirection.offset * stepDistance)

            _gameState.update { it.copy(snakeHead = newPos) }

            distanceTraveledSinceLastHistoryPoint += stepDistance
            if (distanceTraveledSinceLastHistoryPoint >= GameConstants.HISTORY_RESOLUTION) {
                history.add(0, newPos)
                distanceTraveledSinceLastHistoryPoint -= GameConstants.HISTORY_RESOLUTION
                if (history.size > (GameConstants.SEGMENT_SPACING * (_gameState.value.score + 5))) {
                    history.removeAt(history.size - 1)
                }
            }

            remainingDistance -= stepDistance

            // Check if reached boundary
            if (stepDistance >= distanceToBoundary) {
                // Snap to grid for turn
                val snappedPos = Offset(
                    nextGridBoundary.takeIf { currentDirection == Direction.EAST || currentDirection == Direction.WEST } ?: newPos.x.roundToInt().toFloat(),
                    nextGridBoundary.takeIf { currentDirection == Direction.NORTH || currentDirection == Direction.SOUTH } ?: newPos.y.roundToInt().toFloat()
                )
                _gameState.update { it.copy(snakeHead = snappedPos) }

                // Change direction if requested
                nextRequestedDirection?.let {
                    currentDirection = it
                    nextRequestedDirection = null
                }
            }
        }

        updateSegments()
        checkCollisions()
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
        val nextIsMega = (_gameState.value.foodCount + 1) % 5 == 0
        var valid = false
        var newFood = Offset(0f, 0f)
        while (!valid) {
            newFood = Offset(
                Random.nextInt(-GameConstants.WALL_DISTANCE.toInt() + 1, GameConstants.WALL_DISTANCE.toInt() - 1).toFloat(),
                Random.nextInt(-GameConstants.WALL_DISTANCE.toInt() + 1, GameConstants.WALL_DISTANCE.toInt() - 1).toFloat()
            )
            valid = true
            if ((_gameState.value.snakeHead - newFood).getDistance() < 2f) valid = false
            _gameState.value.bodySegments.forEach { if ((it - newFood).getDistance() < 1f) valid = false }
        }
        _gameState.update { it.copy(
            foodPosition = newFood,
            foodType = if (nextIsMega) FoodType.MEGA else FoodType.NORMAL,
            megaBitesLeft = if (nextIsMega) GameConstants.MEGA_FOOD_BITES else 0
        ) }
        onFoodSpawned?.invoke()
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

        // Wall collision
        if (head.x < -GameConstants.WALL_DISTANCE || head.x > GameConstants.WALL_DISTANCE ||
            head.y < -GameConstants.WALL_DISTANCE || head.y > GameConstants.WALL_DISTANCE) {
            _gameState.update { it.copy(isGameOver = true) }
            onGameOver?.invoke()
            return
        }

        // Food collision (simple distance check)
        val foodPos = _gameState.value.foodPosition
        if ((head - foodPos).getDistance() < 0.8f) {
            if (_gameState.value.foodType == FoodType.MEGA) {
                handleMegaBite()
            } else {
                _gameState.update { it.copy(
                    score = it.score + 1,
                    foodCount = it.foodCount + 1,
                    moveSpeed = it.moveSpeed + GameConstants.SPEED_INCREMENT
                ) }
                checkAchievements()
                onFoodEaten?.invoke()
                spawnFood()
            }
        }

        // Self-collision
        if (invulnerabilityTimer <= 0) {
            _gameState.value.bodySegments.forEach { segment ->
                if ((head - segment).getDistance() < 0.5f) {
                    _gameState.update { it.copy(isGameOver = true) }
                    onGameOver?.invoke()
                    return@forEach
                }
            }
        }
    }

    private fun handleMegaBite() {
        val bitesLeft = _gameState.value.megaBitesLeft - 1
        _gameState.update { it.copy(
            score = it.score + 1,
            megaBitesLeft = bitesLeft,
            isSlowedDown = true
        ) }
        onMegaBite?.invoke()
        if (bitesLeft <= 0) {
            _gameState.update { it.copy(foodCount = it.foodCount + 1) }
            checkAchievements()
            burpDelayTimer = 0.5f
        }
    }

    private fun checkAchievements() {
        val score = _gameState.value.score
        val count = _gameState.value.foodCount
        var achievement: String? = null
        if (score % 10 == 0 && score > 0) achievement = "Score $score! Sssensational!"
        if (count == 10) achievement = "10 Apples! Fruit Loop!"
        if (count == 20) achievement = "20 Apples! Core Strength!"

        achievement?.let {
            _gameState.update { s -> s.copy(achievement = it) }
            onAchievement(it)
        }
    }

    private fun updateHazards(dt: Float) {
        // UFO logic
        ufoTimer -= dt
        if (ufoTimer <= 0) {
            if (_gameState.value.ufoPosition == null) {
                // Spawn UFO at edge
                _gameState.update { it.copy(ufoPosition = Offset(-15f, -15f)) }
            } else {
                val ufoPos = _gameState.value.ufoPosition!!
                val target = _gameState.value.foodPosition
                val dir = (target - ufoPos)
                if (dir.getDistance() < 0.5f) {
                    // Abduct
                    _gameState.update { it.copy(
                        ufoPosition = null,
                        score = maxOf(0, it.score - GameConstants.UFO_SCORE_PENALTY)
                    ) }
                    onUfoSteal?.invoke()
                    spawnFood()
                    ufoTimer = GameConstants.UFO_SPAWN_INTERVAL
                } else {
                    val move = dir / dir.getDistance() * GameConstants.UFO_SPEED * dt
                    _gameState.update { it.copy(ufoPosition = ufoPos + move) }
                }
            }
        }

        // Stomper logic
        stomperTimer -= dt
        if (stomperTimer <= 0) {
            stomperTimer = GameConstants.STOMPER_INTERVAL
            onStomp?.invoke()
            _gameState.update { it.copy(screenShake = 1.0f) }
            relocateFood()
        }

        if (_gameState.value.screenShake > 0) {
            _gameState.update { it.copy(screenShake = maxOf(0f, it.screenShake - dt * 2f)) }
        }
    }

    private fun relocateFood() {
        spawnFood()
    }
}
