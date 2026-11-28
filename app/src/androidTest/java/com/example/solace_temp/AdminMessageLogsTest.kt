/**
 * AdminMessageLogsTest.kt
 *
 * Purpose: Instrumented UI test suite verifying the owner-to-entrant message audit log view
 * for US 03.08.01 and US 03.12.01.
 * Tests admin permission restriction, metadata display, search and status filtering,
 * conversation thread inspection with admin audit logging, and empty state feedback.
 */
package com.example.solace_temp

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.solace_temp.data.AppRepository
import com.example.solace_temp.data.WaitingListErrorCallback
import com.example.solace_temp.models.*
import com.example.solace_temp.ui.screens.AdminMessageLogsScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AdminMessageLogsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    class AuditLogFakeRepository(
        currentRole: UserRole = UserRole.ADMIN,
        isAdminUser: Boolean = true
    ) : AppRepository {

        private val _currentProfile = MutableStateFlow(
            Profile(
                deviceId = "admin-device-999",
                name = "Admin Auditor",
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

        private val _ownerEntrantMessages = MutableStateFlow(
            listOf(
                OwnerEntrantMessage(
                    id = "msg-1",
                    ownerId = "owner-john",
                    ownerName = "John Campground Owner",
                    entrantId = "entrant-alex",
                    entrantName = "Alex Morgan",
                    campsiteId = "camp-banff-01",
                    campsiteName = "Banff Alpine Sanctuary",
                    reservationId = "res-1001",
                    reservationStatus = ReservationStatus.ACTIVE,
                    messageText = "Your booking for Banff Alpine Sanctuary is confirmed.",
                    timestampMillis = System.currentTimeMillis() - 100000L,
                    status = MessageDeliveryStatus.READ
                ),
                OwnerEntrantMessage(
                    id = "msg-2",
                    ownerId = "owner-sarah",
                    ownerName = "Sarah Connor",
                    entrantId = "entrant-david",
                    entrantName = "David Miller",
                    campsiteId = "camp-jasper-02",
                    campsiteName = "Jasper Wilderness Pines",
                    reservationId = "res-2002",
                    reservationStatus = ReservationStatus.CANCELLED,
                    messageText = "Notice: Reservation #res-2002 was cancelled.",
                    timestampMillis = System.currentTimeMillis() - 500000L,
                    status = MessageDeliveryStatus.DELIVERED
                )
            )
        )
        override val ownerEntrantMessages: StateFlow<List<OwnerEntrantMessage>> = _ownerEntrantMessages

        private val _adminAuditLogs = MutableStateFlow<List<AdminAuditAccessLog>>(emptyList())
        override val adminAuditLogs: StateFlow<List<AdminAuditAccessLog>> = _adminAuditLogs

        override fun logAdminAuditAccess(
            adminId: String,
            adminName: String,
            reservationId: String,
            ownerId: String,
            entrantId: String
        ) {
            val entry = AdminAuditAccessLog(
                id = "audit-1",
                adminId = adminId,
                adminName = adminName,
                targetReservationId = reservationId,
                targetOwnerId = ownerId,
                targetEntrantId = entrantId
            )
            _adminAuditLogs.value = _adminAuditLogs.value + entry
        }

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
        val repo = AuditLogFakeRepository(currentRole = UserRole.ENTRANT, isAdminUser = false)

        composeTestRule.setContent {
            AdminMessageLogsScreen(repository = repo, onBack = {})
        }

        // Acceptance Criteria: Screen is restricted exclusively to authenticated users with verified administrator permissions
        composeTestRule.onNodeWithTag("admin_logs_access_denied").assertIsDisplayed()
        composeTestRule.onNodeWithText("Access Denied: Admin Permissions Required").assertIsDisplayed()
    }

    @Test
    fun adminUser_displaysCommunicationAuditLogsAndMetadata() {
        val repo = AuditLogFakeRepository()

        composeTestRule.setContent {
            AdminMessageLogsScreen(repository = repo, onBack = {})
        }

        // Acceptance Criteria: Admin can access a dedicated owner-to-entrant message audit log view
        composeTestRule.onNodeWithText("Communication Audit Logs").assertIsDisplayed()
        composeTestRule.onNodeWithText("🔒 Admin Audit Active", substring = true).assertIsDisplayed()

        // Acceptance Criteria: Log entries display key metadata (sender, recipient, campsite name, reservation ID, timestamp, preview, status)
        composeTestRule.onNodeWithText("📍 Banff Alpine Sanctuary").assertIsDisplayed()
        composeTestRule.onNodeWithText("Owner: John Campground Owner").assertIsDisplayed()
        composeTestRule.onNodeWithText("Entrant: Alex Morgan").assertIsDisplayed()
        composeTestRule.onNodeWithText("Your booking for Banff Alpine Sanctuary is confirmed.", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("✓✓ READ", substring = true).assertIsDisplayed()
    }

    @Test
    fun searchInput_filtersByOwnerEntrantReservationIdOrKeyword() {
        val repo = AuditLogFakeRepository()

        composeTestRule.setContent {
            AdminMessageLogsScreen(repository = repo, onBack = {})
        }

        // Perform search by owner name "Sarah"
        composeTestRule.onNodeWithTag("admin_log_search").performTextInput("Sarah")

        // Verify Sarah's message log is displayed and John's is filtered out
        composeTestRule.onNodeWithText("Owner: Sarah Connor").assertIsDisplayed()
        composeTestRule.onNodeWithText("Owner: John Campground Owner").assertDoesNotExist()
    }

    @Test
    fun statusFilterChips_narrowLogsByReservationStatus() {
        val repo = AuditLogFakeRepository()

        composeTestRule.setContent {
            AdminMessageLogsScreen(repository = repo, onBack = {})
        }

        // Open status dropdown then click CANCELLED status filter item
        composeTestRule.onNodeWithTag("filter_status_dropdown").performClick()
        composeTestRule.onNodeWithTag("filter_status_CANCELLED").performClick()

        // Verify CANCELLED message log is shown, ACTIVE log is hidden
        composeTestRule.onNodeWithText("Owner: Sarah Connor").assertIsDisplayed()
        composeTestRule.onNodeWithText("Owner: John Campground Owner").assertDoesNotExist()
    }

    @Test
    fun selectingMessageLogEntry_revealsFullThreadAndLogsAdminAuditAccess() {
        val repo = AuditLogFakeRepository()

        composeTestRule.setContent {
            AdminMessageLogsScreen(repository = repo, onBack = {})
        }

        // Acceptance Criteria: Selecting an entry reveals full message history in chronological order
        composeTestRule.onNodeWithTag("message_log_item_msg-1").performClick()

        // Verify full conversation dialog appears
        composeTestRule.onNodeWithText("Full Conversation History").assertIsDisplayed()
        composeTestRule.onNodeWithText("🔒 Log review recorded for admin Audit Trail.").assertIsDisplayed()
        composeTestRule.onNodeWithText("From: John Campground Owner").assertIsDisplayed()

        // Acceptance Criteria: Access to communication logs is audited, logging which admin reviewed conversation history
        assertEquals(1, repo.adminAuditLogs.value.size)
        assertEquals("res-1001", repo.adminAuditLogs.value.first().targetReservationId)
        assertEquals("admin-device-999", repo.adminAuditLogs.value.first().adminId)
    }

    @Test
    fun nonMatchingSearchQuery_displaysClearEmptyStateFeedback() {
        val repo = AuditLogFakeRepository()

        composeTestRule.setContent {
            AdminMessageLogsScreen(repository = repo, onBack = {})
        }

        // Enter search query that matches no log entries
        composeTestRule.onNodeWithTag("admin_log_search").performTextInput("nonexistent_query_999")

        // Acceptance Criteria: Displays clear empty state feedback if no messages match search/filter criteria
        composeTestRule.onNodeWithTag("admin_logs_empty_state").assertIsDisplayed()
        composeTestRule.onNodeWithText("No communication logs found matching your search or filter criteria.").assertIsDisplayed()
    }
}
