/**
 * AdminNotificationLogsTest.kt
 *
 * Purpose: Comprehensive instrumented UI test suite verifying central notification log viewing
 * for US 03.07.01 and US 03.11.01.
 * Tests admin permission restriction, metadata display, search & filter capabilities,
 * full content & recipient breakdown inspection, empty state, and error feedback handling.
 */
package com.example.solace_temp

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.solace_temp.data.AppRepository
import com.example.solace_temp.data.WaitingListErrorCallback
import com.example.solace_temp.models.*
import com.example.solace_temp.ui.screens.AdminNotificationLogsScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AdminNotificationLogsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    class NotifLogFakeRepository(
        currentRole: UserRole = UserRole.ADMIN,
        isAdminUser: Boolean = true,
        val errorMessage: String? = null
    ) : AppRepository {

        private val _currentProfile = MutableStateFlow(
            Profile(
                deviceId = "admin-notif-device",
                name = "Notif Auditor Admin",
                email = "admin@solace.ca",
                role = currentRole,
                isAdmin = isAdminUser
            )
        )
        override val currentProfile: StateFlow<Profile> = _currentProfile

        override val profiles: StateFlow<List<Profile>> = MutableStateFlow(emptyList())
        override val campsites: StateFlow<List<Campsite>> = MutableStateFlow(emptyList())
        override val lotteries: StateFlow<List<Lottery>> = MutableStateFlow(emptyList())
        override val lotteryLoadError: StateFlow<String?> = MutableStateFlow(null)
        override val reservations: StateFlow<List<Reservation>> = MutableStateFlow(emptyList())
        override val notifications: StateFlow<List<AppNotification>> = MutableStateFlow(emptyList())

        private val _systemNotificationLogs = MutableStateFlow(
            listOf(
                SystemNotificationLog(
                    id = "notif-log-1",
                    senderId = "owner-john",
                    senderName = "John Campground Owner",
                    senderRole = UserRole.CAMPSITE_OWNER,
                    recipientIds = listOf("user-101", "user-102"),
                    targetAudienceGroup = "Waitlist Entrants",
                    campsiteId = "camp-banff-01",
                    campsiteName = "Banff Alpine Sanctuary",
                    title = "Banff Lottery Results Announcement",
                    messageBody = "Draw results released for Banff Alpine Sanctuary! Accept within 48 hours.",
                    type = NotificationType.LOTTERY_DRAW,
                    deliveryStatus = NotificationDeliveryStatus.SENT,
                    channel = NotificationChannel.BOTH,
                    timestampMillis = System.currentTimeMillis() - 100000L
                ),
                SystemNotificationLog(
                    id = "notif-log-2",
                    senderId = "owner-sarah",
                    senderName = "Sarah Connor",
                    senderRole = UserRole.CAMPSITE_OWNER,
                    recipientIds = listOf("user-201"),
                    targetAudienceGroup = "Individual User",
                    campsiteId = "camp-jasper-02",
                    campsiteName = "Jasper Wilderness Pines",
                    title = "Cookhouse Maintenance Notice",
                    messageBody = "The outdoor cookhouse will be closed tomorrow morning.",
                    type = NotificationType.OWNER_BROADCAST,
                    deliveryStatus = NotificationDeliveryStatus.FAILED,
                    channel = NotificationChannel.PUSH_NOTIFICATION,
                    timestampMillis = System.currentTimeMillis() - 500000L
                )
            )
        )
        override val systemNotificationLogs: StateFlow<List<SystemNotificationLog>> = _systemNotificationLogs

        override val notificationLogsError: StateFlow<String?> = MutableStateFlow(errorMessage)

        override fun switchRole(newRole: UserRole) {}
        override fun updateProfile(profile: Profile) {}
        override fun createCampsite(campsite: Campsite): Campsite = campsite
        override fun createLottery(lottery: Lottery): Lottery = lottery
        override fun joinWaitingList(lotteryId: String, entrantId: String, onResult: WaitingListErrorCallback) { onResult.onResult(null) }
        override fun leaveWaitingList(lotteryId: String, entrantId: String) {}
        override fun drawLotteryWinners(lotteryId: String) {}
        override fun redrawLottery(lotteryId: String) {}
        override suspend fun createReservation(campsiteId: String, notificationId: String, startDate: String, endDate: String): Boolean = error("Not used")
        override fun removeCampsiteAsAdmin(campsiteId: String) {}
        override suspend fun declineLotteryInvitation(notificationId: String, campsiteId: String) {}
    }

    @Test
    fun nonAdminUser_displaysAccessDeniedScreen() {
        val repo = NotifLogFakeRepository(currentRole = UserRole.ENTRANT, isAdminUser = false)

        composeTestRule.setContent {
            AdminNotificationLogsScreen(repository = repo, onBack = {})
        }

        // Acceptance Criteria: Screen is accessible exclusively to users with verified administrator privileges
        composeTestRule.onNodeWithTag("admin_notif_logs_access_denied").assertIsDisplayed()
        composeTestRule.onNodeWithText("Access Denied: Admin Permissions Required").assertIsDisplayed()
    }

    @Test
    fun adminUser_displaysSystemNotificationLogsAndMetadata() {
        val repo = NotifLogFakeRepository()

        composeTestRule.setContent {
            AdminNotificationLogsScreen(repository = repo, onBack = {})
        }

        // Acceptance Criteria: Admin can access a central notification log viewer
        composeTestRule.onNodeWithText("Platform Notification Logs").assertIsDisplayed()

        // Acceptance Criteria: Log entries display sender, recipient group, title, preview, timestamp, status, and channel
        composeTestRule.onNodeWithText("Banff Lottery Results Announcement").assertIsDisplayed()
        composeTestRule.onNodeWithText("Lottery Draw").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sender: John Campground Owner (CAMPSITE_OWNER)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Target: Waitlist Entrants").assertIsDisplayed()
        composeTestRule.onNodeWithText("📍 Banff Alpine Sanctuary").assertIsDisplayed()
        composeTestRule.onNodeWithText("Status: SENT · BOTH", substring = true).assertIsDisplayed()
    }

    @Test
    fun searchInput_filtersByUserCampsiteTitleAndKeywords() {
        val repo = NotifLogFakeRepository()

        composeTestRule.setContent {
            AdminNotificationLogsScreen(repository = repo, onBack = {})
        }

        // Perform search by keyword "Cookhouse"
        composeTestRule.onNodeWithTag("admin_notif_log_search").performTextInput("Cookhouse")

        // Verify Sarah's cookhouse notification is displayed and Banff lottery draw is hidden
        composeTestRule.onNodeWithText("Cookhouse Maintenance Notice").assertIsDisplayed()
        composeTestRule.onNodeWithText("Banff Lottery Results Announcement").assertDoesNotExist()
    }

    @Test
    fun notificationTypeAndStatusFilters_narrowRecords() {
        val repo = NotifLogFakeRepository()

        composeTestRule.setContent {
            AdminNotificationLogsScreen(repository = repo, onBack = {})
        }

        // Open Notification Type dropdown and select OWNER_BROADCAST
        composeTestRule.onNodeWithTag("filter_notif_type_dropdown").performClick()
        composeTestRule.onNodeWithTag("filter_notif_type_OWNER_BROADCAST").performClick()
        composeTestRule.onNodeWithText("Cookhouse Maintenance Notice").assertIsDisplayed()
        composeTestRule.onNodeWithText("Banff Lottery Results Announcement").assertDoesNotExist()

        // Reset type to All and filter by Delivery Status: FAILED
        composeTestRule.onNodeWithTag("filter_notif_type_dropdown").performClick()
        composeTestRule.onNodeWithTag("filter_notif_type_all").performClick()

        composeTestRule.onNodeWithTag("filter_delivery_dropdown").performClick()
        composeTestRule.onNodeWithTag("filter_delivery_FAILED").performClick()

        composeTestRule.onNodeWithText("Cookhouse Maintenance Notice").assertIsDisplayed()
        composeTestRule.onNodeWithText("Banff Lottery Results Announcement").assertDoesNotExist()
    }

    @Test
    fun selectingNotificationLog_revealsFullContentAndRecipientBreakdown() {
        val repo = NotifLogFakeRepository()

        composeTestRule.setContent {
            AdminNotificationLogsScreen(repository = repo, onBack = {})
        }

        // Acceptance Criteria: Selecting an entry reveals full message content and recipient breakdown
        composeTestRule.onNodeWithTag("notif_log_item_notif-log-1").performClick()

        // Verify dialog displays full details and recipient list
        composeTestRule.onNodeWithText("Draw results released for Banff Alpine Sanctuary! Accept within 48 hours.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Target Group: Waitlist Entrants").assertIsDisplayed()
        composeTestRule.onNodeWithText("Recipient Identifiers (2):").assertIsDisplayed()
        composeTestRule.onNodeWithText("user-101, user-102").assertIsDisplayed()
    }

    @Test
    fun emptySearchResults_displaysClearEmptyStateFeedback() {
        val repo = NotifLogFakeRepository()

        composeTestRule.setContent {
            AdminNotificationLogsScreen(repository = repo, onBack = {})
        }

        // Search for non-existent keyword
        composeTestRule.onNodeWithTag("admin_notif_log_search").performTextInput("nonexistent_notif_999")

        // Acceptance Criteria: Displays clear empty state feedback if no records match criteria
        composeTestRule.onNodeWithTag("admin_notif_logs_empty_state").assertIsDisplayed()
        composeTestRule.onNodeWithText("No notification logs match your search or filter criteria.").assertIsDisplayed()
    }

    @Test
    fun retrievalError_displaysInformativeErrorFeedback() {
        val repo = NotifLogFakeRepository(errorMessage = "Database index query failed.")

        composeTestRule.setContent {
            AdminNotificationLogsScreen(repository = repo, onBack = {})
        }

        // Acceptance Criteria: Displays informative error feedback if retrieval fails
        composeTestRule.onNodeWithTag("admin_notif_logs_error").assertIsDisplayed()
        composeTestRule.onNodeWithText("Error retrieving notification logs: Database index query failed.").assertIsDisplayed()
    }
}
