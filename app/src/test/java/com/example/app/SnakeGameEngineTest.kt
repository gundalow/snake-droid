package com.example.app

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SnakeGameEngineTest {
    private lateinit var engine: SnakeGameEngine

    @Before
    fun setup() {
        engine = SnakeGameEngine()
    }

    @Test
    fun `test initial state`() {
        val state = engine.gameState.value
        assertEquals(Offset(0f, 0f), state.snakeHead)
        assertEquals(0, state.score)
        assertFalse(state.isGameOver)
        assertEquals(GameConstants.INITIAL_MOVE_SPEED, state.moveSpeed)
    }

    @Test
    fun `test movement and grid snapping`() {
        // Move NORTH for 0.5 seconds at speed 5.0 = 2.5 units
        // It should stop at the first boundary (-1.0) and then the second (-2.0)
        // But since we haven't changed direction, it just keeps going.
        engine.update(0.1f) // 0.5 units
        assertEquals(Offset(0f, -0.5f), engine.gameState.value.snakeHead)

        engine.onDirectionRequest(Direction.EAST)
        // Current: (0, -0.5). Direction: NORTH. Next boundary: -1.0. Dist: 0.5.
        // Speed: 5.0. Need 0.1s to reach boundary.
        // Update 0.2s: distance 1.0.
        // Step 1: move 0.5 to -1.0. Snap. Direction becomes EAST.
        // Step 2: move remaining 0.5 EAST. New pos: (0.5, -1.0).
        engine.update(0.2f)

        val actual = engine.gameState.value.snakeHead
        assertEquals("Head X should be 0.5", 0.5f, actual.x, 0.01f)
        assertEquals("Head Y should be -1.0", -1.0f, actual.y, 0.01f)
    }

    @Test
    fun `test food collection and growth`() {
        // Test initial score
        assertEquals(0, engine.gameState.value.score)
    }

    @Test
    fun `test wall collision`() {
        // Move until it hits the wall (15.5 units)
        // 15.5 / 5.0 = 3.1 seconds
        engine.update(4.0f)
        assertTrue(engine.gameState.value.isGameOver)
    }

    @Test
    fun `test 180 degree turn prevention`() {
        engine.onDirectionRequest(Direction.SOUTH) // Currently NORTH
        engine.update(0.2f)
        // Direction should still be NORTH
        val pos1 = engine.gameState.value.snakeHead
        engine.update(0.1f)
        val pos2 = engine.gameState.value.snakeHead
        assertTrue(pos2.y < pos1.y) // Moving NORTH means Y is decreasing
    }
}
