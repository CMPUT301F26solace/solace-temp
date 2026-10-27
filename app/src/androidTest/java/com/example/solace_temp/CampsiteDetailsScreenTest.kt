package com.example.solace_temp

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.solace_temp.data.AppRepository
import com.example.solace_temp.models.*
import com.example.solace_temp.ui.screens.CampsiteDetailsScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CampsiteDetailsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * A simple fake repository to inject precise state into the CampsiteDetailsScreen
     * for testing without relying on the production database or singleton mocks.
     */
    class FakeAppRepository : AppRepository {
        val _currentProfile = MutableStateFlow(Profile())
        override val currentProfile: StateFlow<Profile> = _currentProfile

        val _campsites = MutableStateFlow<List<Campsite>>(emptyList())
        override val campsites: StateFlow<List<Campsite>> = _campsites

        override val lotteries: StateFlow<List<Lottery>> = MutableStateFlow(emptyList())
        override val reservations: StateFlow<List<Reservation>> = MutableStateFlow(emptyList())
        override val notifications: StateFlow<List<AppNotification>> = MutableStateFlow(emptyList())

        override fun switchRole(newRole: UserRole) {}
        override fun updateProfile(profile: Profile) {}
        override fun createCampsite(campsite: Campsite): Campsite = campsite
        override fun joinWaitingList(lotteryId: String, entrantId: String) {}
        override fun leaveWaitingList(lotteryId: String, entrantId: String) {}
        override fun drawLotteryWinners(lotteryId: String) {}
        override fun removeCampsiteAsAdmin(campsiteId: String) {}
    }

    @Test
    fun ownerViewsCampsiteDetails_displaysAllInfoAndManagementOptions() {
        val fakeRepo = FakeAppRepository()
        
        // Setup owner profile
        fakeRepo._currentProfile.value = Profile(
            deviceId = "owner-123",
            role = UserRole.CAMPSITE_OWNER,
            isAdmin = false
        )
        
        // Setup campsite owned by owner-123
        val campsite = Campsite(
            id = "camp-1",
            ownerId = "owner-123",
            name = "Test Campsite",
            description = "A beautiful test campsite.",
            location = Geolocation(50.0, -110.0, "Test Park"),
            maxOccupancy = 4,
            pricePerNight = 45.0,
            rating = 4.5,
            qrCodePayload = "solace://campsite/camp-1",
            amenities = listOf("Fire pit", "Showers"),
            tags = listOf("Mountain View", "Pet Friendly")
        )
        fakeRepo._campsites.value = listOf(campsite)

        composeTestRule.setContent {
            CampsiteDetailsScreen(
                campsiteId = "camp-1",
                repository = fakeRepo,
                onBack = {}
            )
        }

        // Verify basic details
        composeTestRule.onNodeWithText("Test Campsite").assertIsDisplayed()
        composeTestRule.onNodeWithText("$45/night").assertIsDisplayed()
        composeTestRule.onNodeWithText("📍 Test Park").assertIsDisplayed()
        composeTestRule.onNodeWithText("🌐 Coordinates: 50.0, -110.0").assertIsDisplayed()
        composeTestRule.onNodeWithText("A beautiful test campsite.").assertIsDisplayed()
        
        // Verify amenities and tags
        composeTestRule.onNodeWithText("• Fire pit").assertIsDisplayed()
        composeTestRule.onNodeWithText("• Showers").assertIsDisplayed()
        composeTestRule.onNodeWithText("Mountain View").assertIsDisplayed()
        composeTestRule.onNodeWithText("Pet Friendly").assertIsDisplayed()
        
        // Verify image placeholder
        composeTestRule.onNodeWithText("No Image Available").assertIsDisplayed()
        
        // Verify owner management options
        composeTestRule.onNodeWithText("Owner Management").assertIsDisplayed()
        composeTestRule.onNodeWithText("Edit").assertIsDisplayed()
        composeTestRule.onNodeWithText("Lottery").assertIsDisplayed()
        composeTestRule.onNodeWithText("Entrants").assertIsDisplayed()
    }

    @Test
    fun entrantViewsCampsiteDetails_doesNotDisplayManagementOptions() {
        val fakeRepo = FakeAppRepository()
        
        // Setup entrant profile
        fakeRepo._currentProfile.value = Profile(
            deviceId = "entrant-456",
            role = UserRole.ENTRANT,
            isAdmin = false
        )
        
        // Setup campsite owned by someone else
        val campsite = Campsite(
            id = "camp-1",
            ownerId = "owner-123",
            name = "Test Campsite",
            qrCodePayload = "solace://campsite/camp-1"
        )
        fakeRepo._campsites.value = listOf(campsite)

        composeTestRule.setContent {
            CampsiteDetailsScreen(
                campsiteId = "camp-1",
                repository = fakeRepo,
                onBack = {}
            )
        }

        // Verify campsite renders but NO management options
        composeTestRule.onNodeWithText("Test Campsite").assertIsDisplayed()
        composeTestRule.onNodeWithText("Owner Management").assertDoesNotExist()
        composeTestRule.onNodeWithText("Edit").assertDoesNotExist()
        composeTestRule.onNodeWithText("Lottery").assertDoesNotExist()
    }

    @Test
    fun invalidCampsiteId_showsErrorFeedback() {
        val fakeRepo = FakeAppRepository()
        fakeRepo._campsites.value = emptyList() // No campsites

        composeTestRule.setContent {
            CampsiteDetailsScreen(
                campsiteId = "invalid-id",
                repository = fakeRepo,
                onBack = {}
            )
        }

        // Verify error feedback
        composeTestRule.onNodeWithText("Error: Campsite data failed to load.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Go Back").assertIsDisplayed()
    }
}
