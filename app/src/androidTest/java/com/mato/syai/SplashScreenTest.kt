package com.mato.syai

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import com.mato.syai.presentation.splash.SplashScreen
import org.junit.Assert.assertTrue

class SplashScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun splashScreen_displaysAnimation_and_callsCallbackAfterDelay() = runTest {
        var callbackCalled = false

        composeTestRule.setContent {
            SplashScreen(onSplashFinished = { callbackCalled = true })
        }

        // Check if the animation is displayed
        composeTestRule.onNodeWithTag("SplashAnimation").assertIsDisplayed()

        // Wait for more than 2 seconds to allow delay to complete
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            callbackCalled
        }

        // Final check
        assertTrue(callbackCalled)
    }
}
