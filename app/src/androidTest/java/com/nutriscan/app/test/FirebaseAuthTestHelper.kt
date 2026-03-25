package com.nutriscan.app.test

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Test helper for Firebase Authentication
 * Provides test user credentials and authentication methods for UI tests
 */
object FirebaseAuthTestHelper {

    // Test user credentials
    const val TEST_EMAIL = "test@gmail.com"
    const val TEST_PASSWORD = "test1234"
    const val TEST_DISPLAY_NAME = "Test User"

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    /**
     * Creates or signs in the test user
     * Call this in @Before setup method of your tests
     */
    suspend fun setupTestUser() {
        withContext(Dispatchers.IO) {
            try {
                // Try to sign in first
                auth.signInWithEmailAndPassword(TEST_EMAIL, TEST_PASSWORD).await()
            } catch (e: Exception) {
                // If sign in fails, create the user
                try {
                    auth.createUserWithEmailAndPassword(TEST_EMAIL, TEST_PASSWORD).await()
                    // Update display name
                    auth.currentUser?.let { user ->
                        val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                            .setDisplayName(TEST_DISPLAY_NAME)
                            .build()
                        user.updateProfile(profileUpdates).await()
                    }
                } catch (createError: Exception) {
                    // User might already exist but password is wrong
                    // In a real scenario, you might want to handle this differently
                    throw createError
                }
            }
        }
    }

    /**
     * Signs out the current user
     * Call this in @After teardown method
     */
    fun tearDown() {
        auth.signOut()
    }

    /**
     * Gets the current authenticated user
     */
    fun getCurrentUser() = auth.currentUser

    /**
     * Checks if test user is signed in
     */
    fun isTestUserSignedIn(): Boolean {
        return auth.currentUser?.email == TEST_EMAIL
    }

    /**
     * Deletes the test user (useful for cleanup)
     */
    suspend fun deleteTestUser() {
        withContext(Dispatchers.IO) {
            auth.currentUser?.delete()?.await()
        }
    }
}
