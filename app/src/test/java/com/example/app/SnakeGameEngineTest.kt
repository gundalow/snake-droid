package com.example.app

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SnakeGameEngineTest {
    private lateinit var engine: SnakeGameEngine

    @Before
    fun setup() {
        try {
            engine = SnakeGameEngine()
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
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
        engine.update(0.1f) // Should reach -1.0 and turn EAST
        // After 0.1s total movement is 1.0.
        // At 1.0 it reaches boundary, snaps, and turns.
        // Remaining distance after snap?
        // 0.1s * 5.0 = 0.5 units.
        // Total distance moved since start of this update = 0.5.
        // But it only needed 0.5 to reach -1.0.
        // So it moves 0.5 to -1.0, then 0.0 EAST? No, it should use remaining.

        // Let's be precise:
        // Current: (0, -0.5). Direction: NORTH. Next boundary: -1.0. Dist: 0.5.
        // Update 0.2s: distance 1.0.
        // Step 1: move 0.5 to -1.0. Snap. Direction becomes EAST.
        // Step 2: move remaining 0.5 EAST. New pos: (0.5, -1.0).
        engine.update(0.1f)
        assertEquals(Offset(0.5f, -1.0f), engine.gameState.value.snakeHead)
    }

    @Test
    fun `test food collection and growth`() {
        // Move towards food. Food is at random position.
        // Let's just verify that if we move a lot, eventually we might hit it,
        // but that's non-deterministic.
        // Better: Verify that score increases when snake head is at food position.

        // We'll update the engine manually to place snake at food.
        // Since we can't easily set position, we'll mock the behavior by moving.
        val state = engine.gameState.value
        val food = state.foodPosition

        // Move X then Y
        if (food.x > 0) engine.onDirectionRequest(Direction.EAST)
        else if (food.x < 0) engine.onDirectionRequest(Direction.WEST)

        // Update enough times to reach food.x
        engine.update(Math.abs(food.x) / state.moveSpeed)

        if (food.y > engine.gameState.value.snakeHead.y) engine.onDirectionRequest(Direction.SOUTH)
        else if (food.y < engine.gameState.value.snakeHead.y) engine.onDirectionRequest(Direction.NORTH)

        engine.update(Math.abs(food.y - engine.gameState.value.snakeHead.y) / state.moveSpeed)

        // Should have eaten food or be very close
        // We might need an extra update tick to trigger the collision check if we stopped exactly at food
        engine.update(0.1f)

        assertTrue("Score should increase when hitting food", engine.gameState.value.score > 0)
        assertEquals("Speed should increase", GameConstants.INITIAL_MOVE_SPEED + GameConstants.SPEED_INCREMENT, engine.gameState.value.moveSpeed, 0.01f)
    }

    @Test
    fun `test wall collision`() {
        // Move until it hits the wall (14.0 units)
        // 14.0 / 5.0 = 2.8 seconds
        engine.update(3.0f)
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
