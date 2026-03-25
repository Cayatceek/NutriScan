package com.nutriscan.app.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nutriscan.app.MainActivity
import com.nutriscan.app.test.FirebaseAuthTestHelper
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI Tests for AuthScreen
 * Tests authentication flow with test user: test@gmail.com / test1234
 */
@RunWith(AndroidJUnit4::class)
class AuthScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        // Ensure we're starting fresh (signed out)
        FirebaseAuthTestHelper.tearDown()
    }

    @After
    fun tearDown() {
        // Clean up after tests
        FirebaseAuthTestHelper.tearDown()
    }

    /**
     * Test: Sign in with test user credentials
     * Expected: User successfully signs in and navigates to onboarding/home
     */
    @Test
    fun testSignInWithTestUser() {
        // Wait for splash screen and navigate to auth
        composeTestRule.waitForIdle()
        
        // Click "Sign in with Email" button
        composeTestRule.onNodeWithText("Sign in with Email", useUnmergedTree = true)
            .assertIsDisplayed()
            .performClick()

        // Enter test email
        composeTestRule.onNodeWithText("Email")
            .assertIsDisplayed()
            .performTextInput(FirebaseAuthTestHelper.TEST_EMAIL)

        // Enter test password
        composeTestRule.onNodeWithText("Password")
            .assertIsDisplayed()
            .performTextInput(FirebaseAuthTestHelper.TEST_PASSWORD)

        // Click Sign In button
        composeTestRule.onNodeWithText("Sign In")
            .assertIsDisplayed()
            .performClick()

        // Wait for navigation
        composeTestRule.waitForIdle()

        // Verify we're logged in (check if we navigated away from auth)
        // This could be checking for onboarding screen or home screen elements
        assert(FirebaseAuthTestHelper.isTestUserSignedIn())
    }

    /**
     * Test: Create new account with test credentials
     * Note: This will fail if user already exists
     */
    @Test
    fun testSignUpWithTestUser() {
        // First delete existing user if exists
        runBlocking {
            try {
                FirebaseAuthTestHelper.setupTestUser()
                FirebaseAuthTestHelper.deleteTestUser()
            } catch (e: Exception) {
                // User might not exist, that's fine
            }
        }

        composeTestRule.waitForIdle()

        // Click "Sign in with Email"
        composeTestRule.onNodeWithText("Sign in with Email", useUnmergedTree = true)
            .performClick()

        // Switch to Sign Up mode
        composeTestRule.onNodeWithText("Don't have an account? Sign Up")
            .assertIsDisplayed()
            .performClick()

        // Enter credentials
        composeTestRule.onNodeWithText("Email")
            .performTextInput(FirebaseAuthTestHelper.TEST_EMAIL)

        composeTestRule.onNodeWithText("Password")
            .performTextInput(FirebaseAuthTestHelper.TEST_PASSWORD)

        // Click Sign Up
        composeTestRule.onNodeWithText("Sign Up")
            .assertIsDisplayed()
            .performClick()

        composeTestRule.waitForIdle()

        // Verify sign up success
        assert(FirebaseAuthTestHelper.isTestUserSignedIn())
    }

    /**
     * Test: Skip authentication
     * Expected: Navigate to home without signing in
     */
    @Test
    fun testSkipAuth() {
        composeTestRule.waitForIdle()

        // Click Skip button
        composeTestRule.onNodeWithText("Skip for now", useUnmergedTree = true)
            .assertIsDisplayed()
            .performClick()

        composeTestRule.waitForIdle()

        // Verify we're not logged in but navigated to next screen
        assert(!FirebaseAuthTestHelper.isTestUserSignedIn())
    }

    /**
     * Test: Error handling for invalid credentials
     */
    @Test
    fun testInvalidCredentials() {
        composeTestRule.waitForIdle()

        // Navigate to email sign in
        composeTestRule.onNodeWithText("Sign in with Email", useUnmergedTree = true)
            .performClick()

        // Enter invalid credentials
        composeTestRule.onNodeWithText("Email")
            .performTextInput("invalid@email.com")

        composeTestRule.onNodeWithText("Password")
            .performTextInput("wrongpassword")

        // Try to sign in
        composeTestRule.onNodeWithText("Sign In")
            .performClick()

        composeTestRule.waitForIdle()

        // Verify error is shown or user is not logged in
        assert(!FirebaseAuthTestHelper.isTestUserSignedIn())
    }
}
