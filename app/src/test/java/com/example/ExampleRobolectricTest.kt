package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("AI Voice Studio", appName)
    }

    @Test
    fun `home screen follows ai-first uncluttered flow without exposing advanced sliders before enhance`() {
        composeRule.waitForIdle()

        // Home screen displays elegant branding, hero drop card, and primary Import + Record actions
        composeRule.onNodeWithText("AI VOICE STUDIO").assertIsDisplayed()
        composeRule.onNodeWithText("Drop or import your voice recording").assertIsDisplayed()
        composeRule.onNodeWithTag("upload_audio_button").assertIsDisplayed()
        composeRule.onNodeWithTag("record_voice_button").assertIsDisplayed()
        composeRule.onNodeWithText("WAV").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("MP3").performScrollTo().assertIsDisplayed()

        // Advanced adjustments and post-enhancement presets MUST NOT appear before AI enhancement
        composeRule.onNodeWithTag("advanced_adjustments_panel").assertDoesNotExist()
        composeRule.onNodeWithTag("slider_warmth").assertDoesNotExist()
        composeRule.onNodeWithTag("slider_bass").assertDoesNotExist()
        composeRule.onNodeWithTag("preset_gaming_commentary").assertDoesNotExist()

        // Privacy workspace sheet opens smoothly
        composeRule.onNodeWithTag("privacy_shield_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Workspace & Privacy").assertIsDisplayed()
    }
}
