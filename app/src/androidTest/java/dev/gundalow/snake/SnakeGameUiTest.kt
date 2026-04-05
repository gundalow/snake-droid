package dev.gundalow.snake

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import dev.gundalow.snake.ui.SnakeGameView
import dev.gundalow.snake.ui.theme.SnakeTheme
import org.junit.Rule
import org.junit.Test

class SnakeGameUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testNameEntryAndGameStart() {
        composeTestRule.setContent {
            SnakeTheme {
                SnakeGameView()
            }
        }

        // Initially should show name entry
        composeTestRule.onNodeWithText("Enter Name").assertExists()

        // Input name
        composeTestRule.onNodeWithText("Snake").performTextInput("Jules")

        // Click Start
        composeTestRule.onNodeWithText("START GAME").performClick()

        // Should show score with name
        composeTestRule.onNodeWithText("Jules: 0").assertExists()
    }
}
