/**
 * ProfileEditingScreenTest.kt
 *
 * Tests profile editing for US 01.07.03.
 * Uses controlled save callbacks without writing to Firestore.
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
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileEditingScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var repository: ProfileTestRepository

    @Before
    fun setUp() {
        repository = ProfileTestRepository()

        composeRule.setContent {
            MaterialTheme {
                ProfileAndRoleScreen(repository = repository)
            }
        }
    }

    @Test
    fun savedInformationIsDisplayed() {
        composeRule.onNodeWithTag("profile_name")
            .assertTextContains("Test Camper")

        composeRule.onNodeWithTag("profile_email")
            .assertTextContains("camper@example.com")

        composeRule.onNodeWithTag("profile_phone")
            .assertTextContains("7805550142")
    }

    @Test
    fun blankNameShowsAnErrorWithoutSaving() {
        assertRejected(
            tag = "profile_name",
            value = "",
            message = "Enter your name."
        )
    }

    @Test
    fun invalidEmailShowsAnErrorWithoutSaving() {
        assertRejected(
            tag = "profile_email",
            value = "not-an-email",
            message = "Enter a valid email address."
        )
    }

    @Test
    fun invalidPhoneShowsAnErrorWithoutSaving() {
        assertRejected(
            tag = "profile_phone",
            value = "abc",
            message = "Enter a valid phone number, or leave it empty."
        )
    }

    @Test
    fun emptyOptionalPhoneCanBeSaved() {
        replaceField("profile_phone", "")
        pressSave()

        composeRule.runOnIdle {
            assertEquals("", repository.requestedProfile?.phone)
            repository.completeSave(Result.success(Unit))
        }

        assertFeedback("Profile updated.")
    }

    @Test
    fun validChangesShowSavingThenConfirmation() {
        replaceField("profile_name", "  Updated Camper  ")
        replaceField("profile_email", "  updated@example.com  ")
        replaceField("profile_phone", "  (780) 555-0142  ")
        pressSave()

        composeRule.onNodeWithTag("profile_save")
            .assertIsNotEnabled()
            .assertTextContains("Saving...")

        composeRule.runOnIdle {
            val expected = repository.initialProfile.copy(
                name = "Updated Camper",
                email = "updated@example.com",
                phone = "(780) 555-0142"
            )

            assertEquals(1, repository.saveCalls)
            assertEquals(expected, repository.requestedProfile)

            repository.completeSave(Result.success(Unit))
        }

        assertFeedback("Profile updated.")

        composeRule.onNodeWithTag("profile_save")
            .assertIsEnabled()
    }

    @Test
    fun failedSaveShowsAnErrorAndCanBeRetried() {
        pressSave()

        composeRule.runOnIdle {
            repository.completeSave(
                Result.failure(
                    IllegalStateException("Simulated save failure")
                )
            )
        }

        assertFeedback(
            "Could not save your profile. Please try again."
        )

        composeRule.onNodeWithTag("profile_save")
            .assertIsEnabled()

        pressSave()

        composeRule.runOnIdle {
            assertEquals(2, repository.saveCalls)
            repository.completeSave(Result.success(Unit))
        }

        assertFeedback("Profile updated.")
    }

    private fun replaceField(tag: String, value: String) {
        composeRule.onNodeWithTag(tag)
            .performScrollTo()
            .performTextReplacement(value)
    }

    private fun pressSave() {
        composeRule.onNodeWithTag("profile_save")
            .performScrollTo()
            .performClick()
    }

    private fun assertRejected(
        tag: String,
        value: String,
        message: String
    ) {
        replaceField(tag, value)
        pressSave()

        composeRule.onNodeWithText(message)
            .performScrollTo()
            .assertIsDisplayed()

        composeRule.runOnIdle {
            assertEquals(0, repository.saveCalls)
        }
    }

    private fun assertFeedback(message: String) {
        composeRule.onNodeWithTag("profile_save_feedback")
            .performScrollTo()
            .assertTextEquals(message)
    }

    /**
     * Controls profile data and save completion for these tests.
     * Unrelated repository members use the existing mock implementation.
     */
    private class ProfileTestRepository :
        AppRepository by MockAppRepository {

        val initialProfile = Profile(
            deviceId = "profile-ui-test-device",
            name = "Test Camper",
            email = "camper@example.com",
            phone = "7805550142",
            isAdmin = false
        )

        private val storedProfile = MutableStateFlow(initialProfile)

        override val currentProfile: StateFlow<Profile> = storedProfile

        var saveCalls = 0
            private set

        var requestedProfile: Profile? = null
            private set

        private var pendingResult: ((Result<Unit>) -> Unit)? = null

        override fun updateProfile(profile: Profile) {
            error("Expected the profile-save callback overload.")
        }

        override fun updateProfile(
            profile: Profile,
            onResult: (Result<Unit>) -> Unit
        ) {
            check(pendingResult == null) {
                "A profile save is already pending."
            }

            saveCalls++
            requestedProfile = profile
            pendingResult = onResult
        }

        fun completeSave(result: Result<Unit>) {
            val callback = checkNotNull(pendingResult)
            pendingResult = null

            if (result.isSuccess) {
                storedProfile.value = checkNotNull(requestedProfile)
            }

            callback(result)
        }
    }
}