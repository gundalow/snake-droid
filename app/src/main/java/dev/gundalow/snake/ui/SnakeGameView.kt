package dev.gundalow.snake.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.gundalow.snake.Direction
import dev.gundalow.snake.FoodType
import dev.gundalow.snake.GameConstants
import dev.gundalow.snake.GameState
import dev.gundalow.snake.LeaderboardManager
import dev.gundalow.snake.SnakeGameEngine
import dev.gundalow.snake.SoundManager
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.random.Random

@Composable
fun SnakeGameView(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val soundManager = remember { SoundManager(context) }
    val leaderboardManager = remember { LeaderboardManager(context) }

    var playerName by remember { mutableStateOf("Snake") }
    var showNameEntry by remember { mutableStateOf(true) }
    var lastAchievement by remember { mutableStateOf<String?>(null) }

    val engine =
        remember {
            SnakeGameEngine().apply {
                onFoodEaten = { soundManager.playSound("apple") }
                onFoodSpawned = { soundManager.playSound("whoosh") }
                onGameOver = {
                    soundManager.playSound("game_over")
                    leaderboardManager.saveScore(gameState.value.playerName, gameState.value.score)
                }
                onMegaBite = { soundManager.playSound("chew") }
                onBurp = { soundManager.playSound("burp${Random.nextInt(1, 4)}") }
                onAchievement = { lastAchievement = it }
                onStomp = { soundManager.playSound("impact") }
                onUfoSteal = { soundManager.playSound("tractor_beam") }
            }
        }
    val gameState by engine.gameState.collectAsState()

    LaunchedEffect(lastAchievement) {
        if (lastAchievement != null) {
            delay(GameConstants.ACHIEVEMENT_DISPLAY_TIME)
            lastAchievement = null
        }
    }

    GameLoop(engine)

    DisposableEffect(Unit) {
        onDispose { soundManager.release() }
    }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .graphicsLayer {
                    val mag = GameConstants.SCREEN_SHAKE_MAGNITUDE
                    translationX = (Random.nextFloat() - 0.5f) * gameState.screenShake * mag
                    translationY = (Random.nextFloat() - 0.5f) * gameState.screenShake * mag
                }
                .background(Color(0xFF1B5E20))
                .handleSwipes(engine),
        contentAlignment = Alignment.Center,
    ) {
        GameCanvas(gameState)
        OverlayUi(gameState, lastAchievement)

        if (showNameEntry) {
            NameEntryScreen(
                initialName = playerName,
                leaderboardManager = leaderboardManager,
                onStart = { name ->
                    playerName = name
                    showNameEntry = false
                    engine.resetGame(name)
                },
            )
        }

        if (gameState.isGameOver) {
            GameOverScreen(
                gameState = gameState,
                leaderboardManager = leaderboardManager,
                onRestart = {
                    showNameEntry = true
                },
            )
        }
    }
}

@Composable
private fun GameLoop(engine: SnakeGameEngine) {
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
            delay(GameConstants.FPS_60_DELAY)
        }
    }
}

private fun Modifier.handleSwipes(engine: SnakeGameEngine): Modifier =
    this.pointerInput(Unit) {
        var dragAccumulatorX = 0f
        var dragAccumulatorY = 0f
        detectDragGestures(
            onDragStart = {
                dragAccumulatorX = 0f
                dragAccumulatorY = 0f
            },
            onDrag = { change, dragAmount ->
                change.consume()
                dragAccumulatorX += dragAmount.x
                dragAccumulatorY += dragAmount.y

                val threshold = GameConstants.SWIPE_THRESHOLD
                if (abs(dragAccumulatorX) > threshold || abs(dragAccumulatorY) > threshold) {
                    if (abs(dragAccumulatorX) > abs(dragAccumulatorY)) {
                        if (dragAccumulatorX > threshold) {
                            engine.onDirectionRequest(Direction.EAST)
                        } else if (dragAccumulatorX < -threshold) {
                            engine.onDirectionRequest(Direction.WEST)
                        }
                        dragAccumulatorX = 0f
                    } else {
                        if (dragAccumulatorY > threshold) {
                            engine.onDirectionRequest(Direction.SOUTH)
                        } else if (dragAccumulatorY < -threshold) {
                            engine.onDirectionRequest(Direction.NORTH)
                        }
                        dragAccumulatorY = 0f
                    }
                }
            },
        )
    }

@Composable
private fun GameCanvas(state: GameState) {
    Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        val canvasSize = size
        val boardScale = minOf(canvasSize.width, canvasSize.height) / (GameConstants.BOARD_SIZE + 1f)
        val center = Offset(canvasSize.width / 2f, canvasSize.height / 2f)

        fun worldToCanvas(pos: Offset): Offset = center + (pos * boardScale)

        drawBoundary(center, boardScale)
        drawFood(state, worldToCanvas(state.foodPosition), boardScale)
        drawUfo(state, ::worldToCanvas, boardScale)
        drawSnake(state, ::worldToCanvas, boardScale)
        drawStomper(state, center, boardScale)
    }
}

private fun DrawScope.drawBoundary(
    center: Offset,
    boardScale: Float,
) {
    val wallSize = GameConstants.WALL_DISTANCE * boardScale
    drawRect(
        color = Color.White,
        topLeft = center - Offset(wallSize, wallSize),
        size = Size(wallSize * 2f, wallSize * 2f),
        style = Stroke(width = 4f),
    )
}

private fun DrawScope.drawFood(
    state: GameState,
    pos: Offset,
    scale: Float,
) {
    val color = if (state.foodType == FoodType.MEGA) Color.Magenta else Color.Red
    val radius =
        if (state.foodType == FoodType.MEGA) {
            (GameConstants.FOOD_RADIUS_BASE + (state.megaBitesLeft * GameConstants.FOOD_RADIUS_GROWTH)) * scale
        } else {
            GameConstants.FOOD_RADIUS_BASE * scale
        }
    drawCircle(color = color, radius = radius, center = pos)
}

private fun DrawScope.drawUfo(
    state: GameState,
    toCanvas: (Offset) -> Offset,
    scale: Float,
) {
    state.ufoPosition?.let { ufoWorldPos ->
        val ufoPos = toCanvas(ufoWorldPos)
        drawCircle(color = Color.Gray, radius = GameConstants.UFO_RADIUS * scale, center = ufoPos)
        drawLine(
            color = Color.Cyan.copy(alpha = 0.5f),
            start = ufoPos,
            end = toCanvas(state.foodPosition),
            strokeWidth = GameConstants.TRACTOR_BEAM_WIDTH,
        )
    }
}

private fun DrawScope.drawSnake(
    state: GameState,
    toCanvas: (Offset) -> Offset,
    scale: Float,
) {
    state.bodySegments.forEach { segment ->
        val pos = toCanvas(segment)
        val size = GameConstants.SNAKE_BODY_SIZE_MULT * scale
        drawRect(color = Color(0xFF4CAF50), topLeft = pos - Offset(size / 2f, size / 2f), size = Size(size, size))
    }
    val headPos = toCanvas(state.snakeHead)
    val headSize = GameConstants.SNAKE_HEAD_SIZE_MULT * scale
    drawRect(color = Color(0xFF2E7D32), topLeft = headPos - Offset(headSize / 2f, headSize / 2f), size = Size(headSize, headSize))
}

private fun DrawScope.drawStomper(
    state: GameState,
    center: Offset,
    scale: Float,
) {
    state.stomperProgress?.let { progress ->
        val impact = GameConstants.STOMPER_IMPACT_TIME / GameConstants.STOMPER_DURATION
        val footSize = GameConstants.STOMPER_FOOT_SIZE * scale

        // Shadow
        val shadowAlpha =
            if (progress < impact) {
                progress / impact
            } else {
                1f - (progress - impact) / (1f - impact)
            }
        drawCircle(
            color = Color.Black.copy(alpha = GameConstants.SHADOW_ALPHA_MAX * shadowAlpha),
            radius = footSize,
            center = center,
        )

        // Foot (Simple Rect falling)
        val hFactor = GameConstants.STOMP_HEIGHT_FACTOR
        val height = if (progress < impact) (1f - progress / impact) * hFactor else (progress - impact) / (1f - impact) * hFactor
        if (height < GameConstants.STOMPER_HEIGHT_THRESHOLD) {
            drawRect(
                color = Color(0xFF795548),
                topLeft = center - Offset(footSize, footSize) - Offset(0f, height / GameConstants.STOMP_VISUAL_DIVISOR),
                size = Size(footSize * 2f, footSize * 2f),
            )
        }
    }
}

@Composable
private fun OverlayUi(
    state: GameState,
    achievement: String?,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "${state.playerName}: ${state.score}",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
        )

        AnimatedVisibility(
            visible = achievement != null,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = GameConstants.UI_PADDING_TOP.dp),
        ) {
            Card(colors = CardDefaults.cardColors(containerColor = Color.Yellow)) {
                Text(
                    text = achievement ?: "",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Black,
                )
            }
        }
    }
}

@Composable
private fun NameEntryScreen(
    initialName: String,
    leaderboardManager: LeaderboardManager,
    onStart: (String) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    val scores = remember { leaderboardManager.getTopScores() }

    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black.copy(alpha = GameConstants.NAME_ENTRY_ALPHA)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp),
        ) {
            Text("Enter Name", style = MaterialTheme.typography.headlineMedium, color = Color.White)
            TextField(value = name, onValueChange = { name = it }, modifier = Modifier.fillMaxWidth())

            if (scores.isNotEmpty()) {
                val listHeight = GameConstants.NAME_LIST_HEIGHT
                Spacer(modifier = Modifier.height(8.dp))
                Text("Previous Players:", color = Color.Gray)
                LazyColumn(modifier = Modifier.height(listHeight.dp)) {
                    items(scores.map { it.name }.distinct()) { prevName ->
                        Text(
                            text = prevName,
                            color = Color.Cyan,
                            modifier = Modifier.clickable { name = prevName }.padding(4.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { onStart(name) }) {
                Text("START GAME")
            }
        }
    }
}

@Composable
private fun GameOverScreen(
    gameState: GameState,
    leaderboardManager: LeaderboardManager,
    onRestart: () -> Unit,
) {
    val scores = remember { leaderboardManager.getTopScores() }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = GameConstants.GAME_OVER_ALPHA)),
    ) {
        Text("GAME OVER", style = MaterialTheme.typography.displayMedium, color = Color.Red)
        Text("Final Score: ${gameState.score}", style = MaterialTheme.typography.headlineMedium, color = Color.White)

        Spacer(modifier = Modifier.height(16.dp))
        Text("Leaderboard", style = MaterialTheme.typography.titleLarge, color = Color.Yellow)
        scores.forEach { entry ->
            Text("${entry.name}: ${entry.score}", color = Color.White)
        }

        Spacer(modifier = Modifier.height(GameConstants.RESTART_BUTTON_SPACING.dp))
        Button(onClick = onRestart) {
            Text("RESTART")
        }
    }
}
