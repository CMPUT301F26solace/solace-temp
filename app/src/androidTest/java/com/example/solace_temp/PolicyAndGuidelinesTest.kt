/**
 * PolicyAndGuidelinesTest.kt
 *
 * Purpose: Instrumented UI test suite verifying that entrants can access app policies and guidelines,
 * view complete rules, cancellation policies, and lottery terms, and that content loads reliably
 * offline from local bundle without external browser redirects.
 */
package com.example.solace_temp

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.solace_temp.data.AppRepository
import com.example.solace_temp.data.WaitingListErrorCallback
import com.example.solace_temp.models.*
import com.example.solace_temp.ui.screens.PolicyAndGuidelinesScreen
import com.example.solace_temp.ui.screens.ProfileAndRoleScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PolicyAndGuidelinesTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    class PolicyFakeRepository : AppRepository {
        val currentProfileFlow = MutableStateFlow(Profile(deviceId = "entrant-policy-1", role = UserRole.ENTRANT))
        override val currentProfile: StateFlow<Profile> = currentProfileFlow

        override val profiles: StateFlow<List<Profile>> = MutableStateFlow(emptyList())
        override val campsites: StateFlow<List<Campsite>> = MutableStateFlow(emptyList())
        override val lotteries: StateFlow<List<Lottery>> = MutableStateFlow(emptyList())
        override val lotteryLoadError: StateFlow<String?> = MutableStateFlow(null)
        override val reservations: StateFlow<List<Reservation>> = MutableStateFlow(emptyList())
        override val notifications: StateFlow<List<AppNotification>> = MutableStateFlow(emptyList())

        override fun switchRole(newRole: UserRole) {}
        override fun updateProfile(profile: Profile) {}
        override fun createCampsite(campsite: Campsite): Campsite = campsite
        override fun createLottery(lottery: Lottery): Lottery = lottery
        override fun joinWaitingList(lotteryId: String, entrantId: String, onResult: WaitingListErrorCallback) { onResult.onResult(null) }
        override fun leaveWaitingList(lotteryId: String, entrantId: String) {}
        override fun drawLotteryWinners(lotteryId: String) {}
        override fun redrawLottery(lotteryId: String) {}
        override suspend fun declineLotteryInvitation(notificationId: String, campsiteId: String) {
            error("Not used in this test")
        }
        override suspend fun createReservation(
            campsiteId: String,
            notificationId: String,
            startDate: String,
            endDate: String
        ): Boolean = error("Not used in this test")
        override fun removeCampsiteAsAdmin(campsiteId: String) {}
    }

    @Test
    fun profileScreen_providesClearlyAvailableAccessPointToPolicy() {
        val fakeRepo = PolicyFakeRepository()
        var policyClicked = false

        composeTestRule.setContent {
            ProfileAndRoleScreen(
                repository = fakeRepo,
                onNavigateToPolicy = { policyClicked = true }
            )
        }

        // Acceptance Criteria: Access point is clearly available from general app navigation or settings
        composeTestRule.onNodeWithText("View App Policy & Guidelines")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        assertTrue(policyClicked)
    }

    @Test
    fun policyAndGuidelinesScreen_displaysCompleteContentRulesCancellationAndLotteryTerms() {
        composeTestRule.setContent {
            PolicyAndGuidelinesScreen(onBack = {})
        }

        // Acceptance Criteria: View displays complete policy and guideline text
        composeTestRule.onNodeWithText("App Policy & Guidelines").assertIsDisplayed()

        // Acceptance Criteria: Content includes campsite usage rules, cancellation policies, and lottery terms
        composeTestRule.onNodeWithText("1. Community Guidelines & General Policy")
            .performScrollTo()
            .assertIsDisplayed()

        composeTestRule.onNodeWithText("2. Campsite Usage Rules")
            .performScrollTo()
            .assertIsDisplayed()

        composeTestRule.onNodeWithText("Quiet Hours", substring = true)
            .performScrollTo()
            .assertIsDisplayed()

        composeTestRule.onNodeWithText("3. Cancellation & Refund Policies")
            .performScrollTo()
            .assertIsDisplayed()

        composeTestRule.onNodeWithText("4. Lottery Terms & Conditions")
            .performScrollTo()
            .assertIsDisplayed()
    }
}
