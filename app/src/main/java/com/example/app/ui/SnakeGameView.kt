package com.example.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.app.*
import kotlinx.coroutines.delay
import kotlin.math.abs

@Composable
fun SnakeGameView(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val soundManager = remember { SoundManager(context) }
    val engine = remember {
        SnakeGameEngine().apply {
            onFoodEaten = { soundManager.playSound("apple") }
            onFoodSpawned = { soundManager.playSound("whoosh") }
            onGameOver = { soundManager.playSound("game_over") }
        }
    }
    val gameState by engine.gameState.collectAsState()

    var dragAccumulatorX by remember { mutableStateOf(0f) }
    var dragAccumulatorY by remember { mutableStateOf(0f) }

    // Game Loop
    LaunchedEffect(Unit) {
        var lastTime = System.currentTimeMillis()
        while (true) {
            if (!engine.gameState.value.isGameOver) {
                val currentTime = System.currentTimeMillis()
                val deltaTime = (currentTime - lastTime) / 1000f
                engine.update(deltaTime)
                lastTime = currentTime
            } else {
                lastTime = System.currentTimeMillis()
            }
            delay(16) // ~60 FPS
        }
    }

    DisposableEffect(Unit) {
        onDispose { soundManager.release() }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1B5E20)) // Dark green for garden
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        dragAccumulatorX = 0f
                        dragAccumulatorY = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragAccumulatorX += dragAmount.x
                        dragAccumulatorY += dragAmount.y

                        val threshold = 50f
                        if (abs(dragAccumulatorX) > threshold || abs(dragAccumulatorY) > threshold) {
                            if (abs(dragAccumulatorX) > abs(dragAccumulatorY)) {
                                if (dragAccumulatorX > threshold) engine.onDirectionRequest(Direction.EAST)
                                else if (dragAccumulatorX < -threshold) engine.onDirectionRequest(Direction.WEST)
                                dragAccumulatorX = 0f
                            } else {
                                if (dragAccumulatorY > threshold) engine.onDirectionRequest(Direction.SOUTH)
                                else if (dragAccumulatorY < -threshold) engine.onDirectionRequest(Direction.NORTH)
                                dragAccumulatorY = 0f
                            }
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            val canvasSize = size
            val boardScale = minOf(canvasSize.width, canvasSize.height) / (GameConstants.BOARD_SIZE + 1f)
            val center = Offset(canvasSize.width / 2f, canvasSize.height / 2f)

            fun worldToCanvas(pos: Offset): Offset {
                return center + (pos * boardScale)
            }

            // Draw Boundary
            val wallSize = GameConstants.WALL_DISTANCE * boardScale
            drawRect(
                color = Color.White,
                topLeft = center - Offset(wallSize, wallSize),
                size = Size(wallSize * 2f, wallSize * 2f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
            )

            // Draw Food
            val foodPos = worldToCanvas(gameState.foodPosition)
            drawCircle(
                color = Color.Red,
                radius = 0.4f * boardScale,
                center = foodPos
            )

            // Draw Snake Body
            gameState.bodySegments.forEach { segment ->
                val segmentPos = worldToCanvas(segment)
                drawRect(
                    color = Color(0xFF4CAF50), // Light green body
                    topLeft = segmentPos - Offset(0.45f * boardScale, 0.45f * boardScale),
                    size = Size(0.9f * boardScale, 0.9f * boardScale)
                )
            }

            // Draw Snake Head
            val headPos = worldToCanvas(gameState.snakeHead)
            drawRect(
                color = Color(0xFF2E7D32), // Darker green head
                topLeft = headPos - Offset(0.5f * boardScale, 0.5f * boardScale),
                size = Size(1.0f * boardScale, 1.0f * boardScale)
            )
        }

        // UI Overlay
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Text(
                text = "Score: ${gameState.score}",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White
            )
        }

        if (gameState.isGameOver) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f))
            ) {
                Text(
                    text = "GAME OVER",
                    style = MaterialTheme.typography.displayMedium,
                    color = Color.Red
                )
                Text(
                    text = "Final Score: ${gameState.score}",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = { engine.resetGame() }) {
                    Text("RESTART")
                }
            }
        }
    }
}
