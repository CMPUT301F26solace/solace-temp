/**
 * NotificationOptOutTest.kt
 *
 * Intent tests for the entrant notification opt-out toggle on the profile screen.
 * Uses an in-memory repository instead of Firestore.
 */
package com.example.solace_temp

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.solace_temp.data.AppRepository
import com.example.solace_temp.data.MockAppRepository
import com.example.solace_temp.models.Profile
import com.example.solace_temp.ui.screens.ProfileAndRoleScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationOptOutTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun launch(receiveNotifications: Boolean): OptOutTestRepository {
        val repository = OptOutTestRepository(receiveNotifications)
        composeRule.setContent {
            MaterialTheme {
                ProfileAndRoleScreen(repository = repository)
            }
        }
        return repository
    }

    @Test
    fun toggleReflectsSavedPreference() {
        launch(receiveNotifications = false)

        composeRule.onNodeWithTag("profile_notifications_toggle")
            .performScrollTo()
            .assertIsOff()
    }

    @Test
    fun turningNotificationsOffIsSaved() {
        val repository = launch(receiveNotifications = true)

        composeRule.onNodeWithTag("profile_notifications_toggle")
            .performScrollTo()
            .assertIsOn()
            .performClick()
            .assertIsOff()

        composeRule.onNodeWithTag("profile_save")
            .performScrollTo()
            .performClick()

        composeRule.runOnIdle {
            assertEquals(1, repository.saveCalls)
            assertFalse(repository.currentProfile.value.receiveNotifications)
        }
    }

    @Test
    fun turningNotificationsBackOnIsSaved() {
        val repository = launch(receiveNotifications = false)

        composeRule.onNodeWithTag("profile_notifications_toggle")
            .performScrollTo()
            .performClick()

        composeRule.onNodeWithTag("profile_save")
            .performScrollTo()
            .performClick()

        composeRule.runOnIdle {
            assertTrue(repository.currentProfile.value.receiveNotifications)
        }
    }

    /** Stores the profile in memory and completes saves immediately. */
    private class OptOutTestRepository(receiveNotifications: Boolean) :
        AppRepository by MockAppRepository {

        private val storedProfile = MutableStateFlow(
            Profile(
                deviceId = "opt-out-test-device",
                name = "Test Camper",
                email = "camper@example.com",
                isAdmin = false,
                receiveNotifications = receiveNotifications
            )
        )

        override val currentProfile: StateFlow<Profile> = storedProfile

        var saveCalls = 0
            private set

        override fun updateProfile(profile: Profile, onResult: (Result<Unit>) -> Unit) {
            saveCalls++
            storedProfile.value = profile
            onResult(Result.success(Unit))
        }
    }
}
