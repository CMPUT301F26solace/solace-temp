package com.example.solace_temp

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.*
import com.example.solace_temp.data.AppRepository
import com.example.solace_temp.data.MockAppRepository
import com.example.solace_temp.models.AppNotification
import com.example.solace_temp.models.Campsite
import com.example.solace_temp.models.Lottery
import com.example.solace_temp.models.Profile
import com.example.solace_temp.models.Reservation
import com.example.solace_temp.models.UserRole
import com.example.solace_temp.ui.screens.AdminProfilesScreen
import com.example.solace_temp.ui.screens.ProfileAndRoleScreen
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

class AdminProfilesScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun adminProfilesScreen_navigatesOnProfileClick() {
        var navigatedDeviceId = ""

        // 1. Create a dummy profile with a specific ID
        val testProfile = Profile(
            name = "Test User",
            deviceId = "test_device_123",
            role = UserRole.ENTRANT
        )

        // 2. Inject it into a custom test repository just like the passing tests
        val testRepo = object : AppRepository {
            override val currentProfile = MutableStateFlow(Profile(role = UserRole.ADMIN))
            override val profiles = MutableStateFlow(listOf(testProfile))

            // Add the empty dummy flows so it compiles
            override val campsites = MutableStateFlow(emptyList<Campsite>())
            override val lotteries = MutableStateFlow(emptyList<Lottery>())
            override val reservations = MutableStateFlow(emptyList<Reservation>())
            override val notifications = MutableStateFlow(emptyList<AppNotification>())

            override fun switchRole(newRole: UserRole) {}
            override fun updateProfile(profile: Profile) {}
            override fun createCampsite(campsite: Campsite): Campsite = campsite
            override fun createLottery(lottery: Lottery): Lottery = lottery
            override fun joinWaitingList(lotteryId: String, entrantId: String) {}
            override fun leaveWaitingList(lotteryId: String, entrantId: String) {}
            override fun drawLotteryWinners(lotteryId: String) {}
            override fun removeCampsiteAsAdmin(campsiteId: String) {}
        }

        composeTestRule.setContent {
            AdminProfilesScreen(
                repository = testRepo,
                onSelectProfile = { deviceId -> navigatedDeviceId = deviceId }
            )
        }

        // 3. The empty state should definitely not exist now
        composeTestRule.onNodeWithText("No profiles registered in the platform.")
            .assertDoesNotExist()

        // 4. Click the profile card
        composeTestRule.onAllNodesWithText("Tap for detailed account overview →")
            .onFirst()
            .performClick()

        // 5. Assert that the callback captured the exact deviceId of our test user
        assert(navigatedDeviceId == "test_device_123")
    }

    @Test
    fun adminProfilesScreen_displaysEmptyStateOnNoMatches() {
        val profile1 = Profile(name = "Alice", email = "alice@example.com")
        val profile2 = Profile(name = "Bob", email = "bob@example.com")

        val testRepo = object : AppRepository {
            override val currentProfile = MutableStateFlow(Profile(role = UserRole.ADMIN))
            override val profiles = MutableStateFlow(listOf(profile1, profile2))
            override val campsites = MutableStateFlow(emptyList<Campsite>())
            override val lotteries = MutableStateFlow(emptyList<Lottery>())
            override val reservations = MutableStateFlow(emptyList<Reservation>())
            override val notifications = MutableStateFlow(emptyList<AppNotification>())

            override fun switchRole(newRole: UserRole) {}
            override fun updateProfile(profile: Profile) {}
            override fun createCampsite(campsite: Campsite): Campsite = campsite
            override fun createLottery(lottery: Lottery): Lottery = lottery
            override fun joinWaitingList(lotteryId: String, entrantId: String) {}
            override fun leaveWaitingList(lotteryId: String, entrantId: String) {}
            override fun drawLotteryWinners(lotteryId: String) {}

            override fun removeCampsiteAsAdmin(campsiteId: String) {}
            // Add dummy overrides for other required interface members if needed
        }

        composeTestRule.setContent {
            AdminProfilesScreen(
                repository = testRepo,
                onSelectProfile = {}
            )
        }

        val searchBar = composeTestRule.onNodeWithText("Search by name, email, or ID...")
        searchBar.performTextInput("Zebra999")

        // Assert the empty state feedback appears
        composeTestRule.onNodeWithText("No profiles match search/filter criteria.")
            .assertIsDisplayed()

        // Assert the user cards are no longer visible
        composeTestRule.onNodeWithText("Alice").assertDoesNotExist()
        composeTestRule.onNodeWithText("Bob").assertDoesNotExist()
    }

    @Test
    fun adminProfilesScreen_filtersByRole() {
        val adminProfile = Profile(name = "Admin User", role = UserRole.ADMIN)
        val ownerProfile = Profile(name = "Owner User", role = UserRole.CAMPSITE_OWNER)

        val testRepo = object : AppRepository {
            override val currentProfile = MutableStateFlow(Profile(role = UserRole.ADMIN))
            override val profiles = MutableStateFlow(listOf(adminProfile, ownerProfile))
            override val campsites = MutableStateFlow(emptyList<Campsite>())
            override val lotteries = MutableStateFlow(emptyList<Lottery>())
            override val reservations = MutableStateFlow(emptyList<Reservation>())
            override val notifications = MutableStateFlow(emptyList<AppNotification>())

            override fun switchRole(newRole: UserRole) {}
            override fun updateProfile(profile: Profile) {}
            override fun createCampsite(campsite: Campsite): Campsite = campsite
            override fun createLottery(lottery: Lottery): Lottery = lottery
            override fun joinWaitingList(lotteryId: String, entrantId: String) {}
            override fun leaveWaitingList(lotteryId: String, entrantId: String) {}
            override fun drawLotteryWinners(lotteryId: String) {}

            override fun removeCampsiteAsAdmin(campsiteId: String) {}
            // Add dummy overrides for other required interface members if needed
        }

        composeTestRule.setContent {
            AdminProfilesScreen(repository = testRepo, onSelectProfile = {})
        }

        // Click the filter chip for Campsite Owners
        composeTestRule.onNodeWithText("CAMPSITE_OWNER").performClick()

        // Assert the owner is still visible, but the admin is filtered out
        composeTestRule.onNodeWithText("Owner User").assertIsDisplayed()
        composeTestRule.onNodeWithText("Admin User").assertDoesNotExist()
    }
}

class ProfileAndRoleScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun profileAndRoleScreen_showsSwitcherForAdminOnly() {
        // 1. Test as an Admin
        val adminRepo =
            MockAppRepository // You may need to create a specific mock instance where isAdmin = true

        composeTestRule.setContent {
            ProfileAndRoleScreen(repository = adminRepo)
        }

        // Assert the Admin View Navigation card is visible
        composeTestRule.onNodeWithText("Admin View Navigation").assertIsDisplayed()
        composeTestRule.onNodeWithText("ENTRANT").assertIsDisplayed()
        composeTestRule.onNodeWithText("CAMPSITE_OWNER").assertIsDisplayed()
    }

    @Test
    fun profileAndRoleScreen_hidesSwitcherForRegularUser() {
        // 2. Test as a Regular Entrant
        // Create a custom anonymous mock repository for this test where the profile is strictly an entrant
        val entrantProfile = Profile(isAdmin = false, role = UserRole.ENTRANT)
        val entrantRepo = object : AppRepository {
            override val currentProfile = MutableStateFlow(entrantProfile)
            override val profiles = MutableStateFlow(emptyList<Profile>())
            override val campsites = MutableStateFlow(emptyList<Campsite>())
            override val lotteries = MutableStateFlow(emptyList<Lottery>())
            override val reservations = MutableStateFlow(emptyList<Reservation>())
            override val notifications = MutableStateFlow(emptyList<AppNotification>())

            override fun switchRole(newRole: UserRole) {}
            override fun updateProfile(profile: Profile) {}
            override fun createCampsite(campsite: Campsite): Campsite = campsite
            override fun createLottery(lottery: Lottery): Lottery = lottery
            override fun joinWaitingList(lotteryId: String, entrantId: String) {}
            override fun leaveWaitingList(lotteryId: String, entrantId: String) {}
            override fun drawLotteryWinners(lotteryId: String) {}

            override fun removeCampsiteAsAdmin(campsiteId: String) {}

        }

        composeTestRule.setContent {
            ProfileAndRoleScreen(repository = entrantRepo)
        }

        // Assert the Admin View Navigation card is completely hidden
        composeTestRule.onNodeWithText("Admin View Navigation").assertDoesNotExist()
    }



}