package com.example.solace_temp

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.solace_temp.data.AppRepository
import com.example.solace_temp.data.MockAppRepository
import com.example.solace_temp.models.*
import com.example.solace_temp.ui.screens.CampsiteDetailsScreen
import com.example.solace_temp.ui.screens.QrCodeGeneratorUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CampsiteQrCodeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Fake repository for isolating UI tests.
     */
    class QrFakeRepository : AppRepository {
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
        override fun createLottery(lottery: Lottery): Lottery = lottery
        override fun joinWaitingList(lotteryId: String, entrantId: String) {}
        override fun leaveWaitingList(lotteryId: String, entrantId: String) {}
        override fun drawLotteryWinners(lotteryId: String) {}
        override fun removeCampsiteAsAdmin(campsiteId: String) {}
    }

    @Test
    fun generatesValidQrBitmap() {
        // Acceptance Criteria: System automatically generates a QR code linked to the campsite's unique identifier
        val payload = "solace://campsite/test-id-123"
        val bitmap = QrCodeGeneratorUtil.generateQrBitmap(payload, 512)
        
        assertNotNull(bitmap)
        assertEquals(512, bitmap.width)
        assertEquals(512, bitmap.height)
    }

    @Test
    fun createCampsite_generatesUniqueDeepLinkPayload() {
        // Acceptance Criteria: QR code encodes a deep link or specific identifier that routes directly to the campsite details
        val repo = MockAppRepository 
        val newCampsite = Campsite(name = "QR Test Campsite", ownerId = "owner-1")
        
        val created = repo.createCampsite(newCampsite)
        
        assertNotNull(created.id)
        assertTrue(created.id.isNotBlank())
        
        // Assert the generated code is distinct and unique to each individual campsite record
        assertEquals("solace://campsite/${created.id}", created.qrCodePayload)
    }

    @Test
    fun ownerViewsCampsite_qrCodeAndShareButtonAreDisplayed() {
        val fakeRepo = QrFakeRepository()
        
        // Set the user to be the owner of the campsite
        fakeRepo._currentProfile.value = Profile(
            deviceId = "owner-99",
            role = UserRole.CAMPSITE_OWNER,
            isAdmin = false
        )
        
        val campsite = Campsite(
            id = "camp-qr-1",
            ownerId = "owner-99",
            name = "QR Specific Campsite",
            qrCodePayload = "solace://campsite/camp-qr-1"
        )
        fakeRepo._campsites.value = listOf(campsite)

        composeTestRule.setContent {
            CampsiteDetailsScreen(
                campsiteId = "camp-qr-1",
                repository = fakeRepo,
                onBack = {}
            )
        }

        // Acceptance Criteria: Owner can view the generated QR code on the campsite details screen
        composeTestRule.onNodeWithText("Promotional QR Code")
            .performScrollTo()
            .assertIsDisplayed()
        
        // Assert the unique link is visible
        composeTestRule.onNodeWithText("Unique Link: solace://campsite/camp-qr-1")
            .performScrollTo()
            .assertIsDisplayed()
        
        // Acceptance Criteria: QR code is rendered clearly as an image or graphic within the view
        composeTestRule.onNodeWithContentDescription("Campsite QR Code")
            .performScrollTo()
            .assertIsDisplayed()
        
        // Acceptance Criteria: System provides options to share or save the QR code
        composeTestRule.onNodeWithText("Share / Save QR Code")
            .performScrollTo()
            .assertIsDisplayed()
    }
}
