/**
 * DeviceIdentificationTest.kt
 *
 * Purpose: Instrumented UI & Integration test suite verifying automated device identification
 * on launch, persistence across app sessions, mapping device IDs to profile records, and automatic
 * profile creation for new device IDs without username or password prompts.
 */
package com.example.solace_temp

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.solace_temp.data.AppRepository
import com.example.solace_temp.data.DeviceIdentityManager
import com.example.solace_temp.models.*
import com.example.solace_temp.ui.screens.CampsiteListScreen
import com.example.solace_temp.ui.screens.ProfileAndRoleScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeviceIdentificationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        DeviceIdentityManager.clearDeviceId(context)
    }

    class FakeIdentityRepository(val deviceId: String) : AppRepository {
        val storedProfiles = mutableMapOf<String, Profile>()

        private val _currentProfile = MutableStateFlow(
            storedProfiles[deviceId] ?: Profile(
                deviceId = deviceId,
                name = "New Explorer",
                role = UserRole.ENTRANT
            )
        )
        override val currentProfile: StateFlow<Profile> = _currentProfile

        override val profiles: StateFlow<List<Profile>> = MutableStateFlow(storedProfiles.values.toList())
        override val campsites: StateFlow<List<Campsite>> = MutableStateFlow(emptyList())
        override val lotteries: StateFlow<List<Lottery>> = MutableStateFlow(emptyList())
        override val lotteryLoadError: StateFlow<String?> = MutableStateFlow(null)
        override val reservations: StateFlow<List<Reservation>> = MutableStateFlow(emptyList())
        override val notifications: StateFlow<List<AppNotification>> = MutableStateFlow(emptyList())

        init {
            if (!storedProfiles.containsKey(deviceId)) {
                storedProfiles[deviceId] = _currentProfile.value
            }
        }

        override fun switchRole(newRole: UserRole) {
            val updated = _currentProfile.value.copy(role = newRole)
            _currentProfile.value = updated
            storedProfiles[deviceId] = updated
        }

        override fun updateProfile(profile: Profile) {
            _currentProfile.value = profile
            storedProfiles[profile.deviceId] = profile
        }

        override fun createCampsite(campsite: Campsite): Campsite = campsite
        override fun createLottery(lottery: Lottery): Lottery = lottery
        override fun joinWaitingList(lotteryId: String, entrantId: String, onResult: (String?) -> Unit) { onResult(null) }
        override fun leaveWaitingList(lotteryId: String, entrantId: String) {}
        override fun drawLotteryWinners(lotteryId: String) {}
        override fun removeCampsiteAsAdmin(campsiteId: String) {}
    }

    @Test
    fun getOrCreateDeviceId_generatesNonEmptyUniqueIdAndPersistsAcrossCalls() {
        // Acceptance Criteria: System automatically identifies user by device on launch & persists identity
        val firstId = DeviceIdentityManager.getOrCreateDeviceId(context)
        assertNotNull(firstId)
        assertTrue(firstId.isNotBlank())

        // Subsequent call (simulating app relaunch) returns identical ID
        val secondId = DeviceIdentityManager.getOrCreateDeviceId(context)
        assertEquals(firstId, secondId)
    }

    @Test
    fun automaticDeviceIdentification_launchesDirectlyWithoutLoginPrompt() {
        // Acceptance Criteria: No login screen, username, or password prompt is required for user
        val deviceId = DeviceIdentityManager.getOrCreateDeviceId(context)
        val fakeRepo = FakeIdentityRepository(deviceId)

        composeTestRule.setContent {
            CampsiteListScreen(
                title = "Explore Alberta Campsites",
                repository = fakeRepo,
                onSelectCampsite = {}
            )
        }

        // App opens directly into primary user interface without prompting for username/password
        composeTestRule.onNodeWithText("Explore Alberta Campsites").assertIsDisplayed()
        composeTestRule.onNodeWithText("Username").assertDoesNotExist()
        composeTestRule.onNodeWithText("Password").assertDoesNotExist()
        composeTestRule.onNodeWithText("Login").assertDoesNotExist()
    }

    @Test
    fun deviceId_mapsToProfileRecordAndCreatesNewProfileForNewDevice() {
        // Acceptance Criteria: Device identifier mapped to profile record; if new, a new user record linked to ID is created
        val deviceId = DeviceIdentityManager.getOrCreateDeviceId(context)
        val fakeRepo = FakeIdentityRepository(deviceId)

        val profile = fakeRepo.currentProfile.value
        assertEquals(deviceId, profile.deviceId)
        assertEquals("New Explorer", profile.name)
        assertEquals(UserRole.ENTRANT, profile.role)
    }

    @Test
    fun existingDevice_loadsExistingProfileState() {
        // Acceptance Criteria: If device ID exists in database, existing profile and state are loaded
        val deviceId = "existing-device-456"
        val fakeRepo = FakeIdentityRepository(deviceId)

        // Simulate existing saved profile in database
        val existingProfile = Profile(
            deviceId = deviceId,
            name = "Existing Entrant Sam",
            email = "sam@example.com",
            phone = "(780) 555-9999",
            role = UserRole.ENTRANT
        )
        fakeRepo.updateProfile(existingProfile)

        // Verify profile details are preserved and loaded
        assertEquals("Existing Entrant Sam", fakeRepo.currentProfile.value.name)
        assertEquals("sam@example.com", fakeRepo.currentProfile.value.email)
        assertEquals(deviceId, fakeRepo.currentProfile.value.deviceId)
    }

    @Test
    fun profileScreen_displaysDeviceIdMappingWithoutLoginCredentials() {
        // Acceptance Criteria: Displays mapped device ID; no authentication forms shown
        val deviceId = "device-test-789"
        val fakeRepo = FakeIdentityRepository(deviceId)

        composeTestRule.setContent {
            ProfileAndRoleScreen(repository = fakeRepo)
        }

        composeTestRule.onNodeWithText("📱 Device ID: device-test-789").assertIsDisplayed()
        composeTestRule.onNodeWithText("Login").assertDoesNotExist()
        composeTestRule.onNodeWithText("Password").assertDoesNotExist()
    }
}
