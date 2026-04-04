package com.example.app

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.app.ui.SnakeGameView
import com.example.app.ui.theme.ExampleAppTheme
import org.junit.Rule
import org.junit.Test

class SnakeGameUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testNameEntryAndGameStart() {
        composeTestRule.setContent {
            ExampleAppTheme {
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
