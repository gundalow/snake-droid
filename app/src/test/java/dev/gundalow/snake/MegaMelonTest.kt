package dev.gundalow.snake

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MegaMelonTest {
    private lateinit var engine: SnakeGameEngine

    @Before
    fun setup() {
        engine = SnakeGameEngine()
    }

    @Test
    fun `test mega food nibbling requires multiple entries`() {
        // Setup initial state with a Mega-Melon at (5, 5)
        val initialMegaState =
            GameState(
                snakeHead = Offset(0f, 0f),
                foodPosition = Offset(5f, 5f),
                foodType = FoodType.MEGA,
                megaBitesLeft = 3,
                score = 0,
                isSlowedDown = false,
                isInsideMegaFood = false,
            )
        engine.debugSetGameState(initialMegaState)

        // 1. Move into the food range
        engine.debugSetGameState(engine.gameState.value.copy(snakeHead = Offset(5f, 5f)))
        engine.update(0.01f) // Trigger collision check

        // Check bite 1
        assertEquals(2, engine.gameState.value.megaBitesLeft)
        assertEquals(1, engine.gameState.value.score)
        assertTrue(engine.gameState.value.isInsideMegaFood)
        assertTrue(engine.gameState.value.isSlowedDown)

        // 2. Stay in range, should NOT trigger bite 2
        engine.update(0.01f)
        assertEquals(2, engine.gameState.value.megaBitesLeft)
        assertEquals(1, engine.gameState.value.score)
        assertTrue(engine.gameState.value.isInsideMegaFood)

        // 3. Move out of range
        engine.debugSetGameState(engine.gameState.value.copy(snakeHead = Offset(0f, 0f)))
        engine.update(0.01f)
        assertFalse(engine.gameState.value.isInsideMegaFood)
        assertEquals(2, engine.gameState.value.megaBitesLeft)

        // 4. Move back into range for bite 2
        engine.debugSetGameState(engine.gameState.value.copy(snakeHead = Offset(5f, 5f)))
        engine.update(0.01f)
        assertEquals(1, engine.gameState.value.megaBitesLeft)
        assertEquals(2, engine.gameState.value.score)
        assertTrue(engine.gameState.value.isInsideMegaFood)

        // 5. Move out and back for bite 3
        engine.debugSetGameState(engine.gameState.value.copy(snakeHead = Offset(0f, 0f)))
        engine.update(0.01f)
        engine.debugSetGameState(engine.gameState.value.copy(snakeHead = Offset(5f, 5f)))
        engine.update(0.01f)

        assertEquals(0, engine.gameState.value.megaBitesLeft)
        assertEquals(3, engine.gameState.value.score)
        // Burp delay should be active
        // After final bite, isSlowedDown is still true during burp delay
        assertTrue(engine.gameState.value.isSlowedDown)
    }
}
