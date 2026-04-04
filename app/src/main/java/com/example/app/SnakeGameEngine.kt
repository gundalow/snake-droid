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

    init {
        resetGame()
    }

    fun resetGame() {
        _gameState.value = GameState()
        currentDirection = Direction.NORTH
        nextRequestedDirection = null
        history.clear()
        history.add(Offset(0f, 0f))
        distanceTraveledSinceLastHistoryPoint = 0f
        accumulatedTime = 0f
        invulnerabilityTimer = GameConstants.INVULNERABILITY_TIME
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

        val moveSpeed = _gameState.value.moveSpeed
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
        var valid = false
        var newFood = Offset(0f, 0f)
        while (!valid) {
            newFood = Offset(
                Random.nextInt(-GameConstants.WALL_DISTANCE.toInt(), GameConstants.WALL_DISTANCE.toInt()).toFloat(),
                Random.nextInt(-GameConstants.WALL_DISTANCE.toInt(), GameConstants.WALL_DISTANCE.toInt()).toFloat()
            )
            valid = true
            if ((_gameState.value.snakeHead - newFood).getDistance() < 2f) valid = false
            _gameState.value.bodySegments.forEach { if ((it - newFood).getDistance() < 1f) valid = false }
        }
        _gameState.update { it.copy(foodPosition = newFood) }
        onFoodSpawned?.invoke()
    }

    var onFoodEaten: (() -> Unit)? = null
    var onFoodSpawned: (() -> Unit)? = null
    var onGameOver: (() -> Unit)? = null

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
            _gameState.update { it.copy(
                score = it.score + 1,
                moveSpeed = it.moveSpeed + GameConstants.SPEED_INCREMENT
            ) }
            onFoodEaten?.invoke()
            spawnFood()
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
}
