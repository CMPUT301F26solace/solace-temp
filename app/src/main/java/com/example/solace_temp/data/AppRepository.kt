/**
 * AppRepository.kt
 * 
 * Purpose: Defines the core data repository interface for the application, exposing state flows and actions.
 * It also provides a mocked implementation [MockAppRepository] used for testing or preview purposes.
 * 
 * Needs Change/Issues: 
 * - The file contains both the interface and the mock implementation; these should be separated into their own files.
 */
package com.example.solace_temp.data

import android.net.Uri
import com.example.solace_temp.models.*
import com.example.solace_temp.validation.WaitingListException
import com.example.solace_temp.validation.WaitingListRules
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Interface defining the primary data access and domain operations for the application.
 * Provides streams of data via [StateFlow] and methods for data manipulation.
 */
fun interface WaitingListErrorCallback {
    fun onResult(error: String?)
}

interface AppRepository {
    /** The currently active user's profile. */
    val currentProfile: StateFlow<Profile>

    /** List of all registered profiles in the system. */
    val profiles: StateFlow<List<Profile>>
    
    /** List of all campsites available in the system. */
    val campsites: StateFlow<List<Campsite>>
    
    /** List of all active lotteries for campsites. */
    val lotteries: StateFlow<List<Lottery>>

    /** Null when lottery loading succeeds, otherwise a user-readable database error. */
    val lotteryLoadError: StateFlow<String?>
    
    /** List of reservations made across the platform. */
    val reservations: StateFlow<List<Reservation>>
    
    /** List of notifications intended for the current user. */
    val notifications: StateFlow<List<AppNotification>>

    /** Direct chat messages sent between campsite owners and reserving entrants. */
    val ownerEntrantMessages: StateFlow<List<OwnerEntrantMessage>>
        get() = MutableStateFlow(emptyList())

    /** Administrative audit log recording access to communication logs. */
    val adminAuditLogs: StateFlow<List<AdminAuditAccessLog>>
        get() = MutableStateFlow(emptyList())

    /** Logs an administrator reviewing a conversation thread. */
    fun logAdminAuditAccess(
        adminId: String,
        adminName: String,
        reservationId: String,
        ownerId: String,
        entrantId: String
    ) {}

    /**
     * Switches the active role of the current user profile.
     * @param newRole The new [UserRole] to switch to.
     */
    fun switchRole(newRole: UserRole)

    /**
     * Updates the current user's profile information.
     * @param profile The updated [Profile] data.
     */
    fun updateProfile(profile: Profile)

    /**
     * Updates a profile and reports the result.
     *
     * Asynchronous repositories override this default implementation.
     *
     * @param profile The updated profile.
     * @param onResult Receives success or failure when saving finishes.
     */
    fun updateProfile(
        profile: Profile,
        onResult: (Result<Unit>) -> Unit
    ) {
        val result = runCatching {
            updateProfile(profile)
        }

        onResult(result)
    }

    /**
     * Creates a new campsite and registers it in the repository.
     * @param campsite The [Campsite] details to register.
     * @return The newly created [Campsite] with its generated ID.
     */
    fun createCampsite(campsite: Campsite): Campsite

    /**
     * Updates an existing campsite's details in the repository.
     * @param campsite The updated [Campsite] details to persist.
     */
    fun updateCampsite(campsite: Campsite) {}

    /**
     * Updates a campsite and reports the result via callback.
     * @param campsite The updated [Campsite] details to persist.
     * @param onResult Receives success or failure when saving finishes.
     */
    fun updateCampsite(
        campsite: Campsite,
        onResult: (Result<Unit>) -> Unit
    ) {
        val result = runCatching {
            updateCampsite(campsite)
        }
        onResult(result)
    }

    /**
     * Uploads a campsite image file/media URI to storage and returns the download URL.
     * @param campsiteId The unique ID of the campsite.
     * @param imageUri The local device Uri of the image file to upload.
     * @param onResult Receives the download URL String on success or Throwable on failure.
     */
    fun uploadCampsiteImage(
        campsiteId: String,
        imageUri: Uri,
        onResult: (Result<String>) -> Unit
    ) {
        onResult(Result.success(imageUri.toString()))
    }

    /**
     * Cancels an active campsite reservation for the current entrant and records a cancellation
     * confirmation notification containing the campsite name, cancelled dates, and confirmation ID.
     *
     * @param reservationId The unique confirmation ID of the reservation to cancel.
     * @param onResult Callback receiving a [CancellationOutcome] on success, or an error if cancellation fails.
     */
    fun cancelReservation(
        reservationId: String,
        onResult: (Result<CancellationOutcome>) -> Unit
    ) {
        onResult(
            Result.failure(
                UnsupportedOperationException("Reservation cancellation is not supported by this repository.")
            )
        )
    }

    /**
     * Creates a new lottery for one or more campsites.
     * @param lottery The [Lottery] configuration to create.
     * @return The newly created [Lottery].
     */
    fun createLottery(lottery: Lottery): Lottery

    /**
     * Updates an existing lottery configuration in the repository.
     * @param lottery The updated [Lottery] configuration to save.
     */
    fun updateLottery(lottery: Lottery) {}

    /**
     * Updates an existing lottery configuration and reports the result via callback.
     * @param lottery The updated [Lottery] configuration to save.
     * @param onResult Receives success or failure when saving finishes.
     */
    fun updateLottery(
        lottery: Lottery,
        onResult: (Result<Unit>) -> Unit
    ) {
        val result = runCatching {
            updateLottery(lottery)
        }
        onResult(result)
    }
    
    /**
     * Adds an entrant to a lottery's waiting list.
     * @param lotteryId The unique ID of the lottery.
     * @param entrantId The unique ID of the entrant joining the waitlist.
     */
    fun joinWaitingList(lotteryId: String, entrantId: String, onResult: WaitingListErrorCallback = WaitingListErrorCallback { })
    
    /**
     * Removes an entrant from a lottery's waiting list.
     * @param lotteryId The unique ID of the lottery.
     * @param entrantId The unique ID of the entrant leaving the waitlist.
     */
    fun leaveWaitingList(lotteryId: String, entrantId: String)

    /** Confirms selected camping nights for an invited lottery winner. Null means success. */
    fun confirmBooking(
        campsiteId: String,
        lotteryId: String,
        checkInMillis: Long,
        checkOutMillis: Long,
        onResult: (String?) -> Unit
    ) {
        onResult("Booking is not supported by this repository.")
    }

    /**
     * Joins after checking the current lottery and reports completion (US 01.02.01).
     * Bridges the existing string-error callback; Firestore overrides it atomically.
     *
     * @param lotteryId The lottery to join.
     * @param entrantId The existing entrant device ID.
     * @param onResult Receives success or the reason entry failed.
     */
    fun joinWaitingListWithResult(
        lotteryId: String,
        entrantId: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        val lottery = lotteries.value.find { it.id == lotteryId }
        val error = WaitingListRules.joinError(lottery, entrantId)

        if (error != null) {
            onResult(Result.failure(WaitingListException(error)))
            return
        }

        joinWaitingList(lotteryId, entrantId) { message ->
            onResult(
                if (message == null) {
                    Result.success(Unit)
                } else {
                    Result.failure(WaitingListException(message))
                }
            )
        }
    }

    /**
     * Leaves the waiting list and reports completion (US 01.02.02).
     * Synchronous repositories use this default; asynchronous repositories override it.
     *
     * @param lotteryId The lottery to leave.
     * @param entrantId The existing entrant device ID.
     * @param onResult Receives success after removal completes, or a failure.
     */
    fun leaveWaitingList(
        lotteryId: String,
        entrantId: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        val result = runCatching {
            val lottery = lotteries.value.find { it.id == lotteryId }

            WaitingListRules.leaveError(lottery, entrantId)?.let {
                throw WaitingListException(it)
            }

            leaveWaitingList(lotteryId, entrantId)
        }

        onResult(result)
    }
    
    /**
     * Triggers the winner draw process for a specific lottery.
     * @param lotteryId The unique ID of the lottery to draw winners for.
     */
    fun drawLotteryWinners(lotteryId: String)

    /**
     * Automatically triggers winner drawing for all lotteries whose registration
     * deadline has passed and winners have not been drawn yet.
     */
    fun triggerExpiredLotteryDraws() {
        val now = System.currentTimeMillis()
        lotteries.value.filter { lottery ->
            val endMillis = lottery.registrationEndMillis
            endMillis != null && endMillis <= now && !lottery.isDrawn
        }.forEach { lottery ->
            drawLotteryWinners(lottery.id)
        }
    }

    /**
     * Redraws or picks backup winners for a specific lottery.
     * @param lotteryId The unique ID of the lottery.
     */
    fun redrawLottery(lotteryId: String)

    /** Administrative audit log recording campsite deletion events (US 03.01.01). */
    val adminCampsiteDeletionLogs: StateFlow<List<AdminCampsiteDeletionLog>>
        get() = MutableStateFlow(emptyList())

    /**
     * Inspects active reservations, open-window lotteries, unfulfilled waitlists, media, and QR codes
     * linked to a campsite before an admin confirms removal (US 03.01.01).
     *
     * @param campsiteId The unique ID of the campsite being evaluated for deletion.
     * @return A [CampsiteRemovalDependencies] snapshot detailing all impacted records.
     */
    fun getCampsiteRemovalDependencies(campsiteId: String): CampsiteRemovalDependencies {
        val site = campsites.value.find { it.id == campsiteId }

        // Identify currently active reservations that would be disrupted by deletion
        val activeReservations = reservations.value
            .filter { it.campsiteId == campsiteId && it.status == ReservationStatus.ACTIVE }
            .map { it.id }

        // Identify all lotteries referencing this campsite and separate out open registration windows
        val linkedLotteries = lotteries.value.filter { campsiteId in it.campsiteIds }
        val openLotteries = linkedLotteries
            .filter { canJoinLottery(it) }
            .map { it.id }

        // Count unfulfilled waitlist entries across undrawn or active lotteries
        val unfulfilledWaitlistCount = linkedLotteries
            .filter { !it.isDrawn || it.waitingListEntrantIds.isNotEmpty() }
            .sumOf { it.waitingListEntrantIds.size }

        return CampsiteRemovalDependencies(
            activeReservationIds = activeReservations,
            openLotteryIds = openLotteries,
            affectedLotteryIds = linkedLotteries.map { it.id },
            totalWaitlistedEntrants = unfulfilledWaitlistCount,
            hasMediaAsset = !site?.imageUrl.isNullOrBlank(),
            hasQrCode = !site?.qrCodePayload.isNullOrBlank()
        )
    }
    /**
     * Deletes a campsite from the platform. Requires Admin privileges.
     * Kept for backward compatibility with existing callers and test fakes.
     *
     * @param campsiteId The unique ID of the campsite to remove.
     */
    fun removeCampsiteAsAdmin(campsiteId: String)

    /**
     * Removes a campsite as an administrator with full dependency safety checks, cleanup,
     * and audit logging (US 03.01.01).
     *
     * @param campsiteId The unique ID of the campsite to remove.
     * @param forceOverride Must be true to cancel active reservations or close open-window lotteries.
     * @param onResult Callback reporting success or a descriptive failure reason to the UI.
     */
    fun removeCampsiteAsAdmin(
        campsiteId: String,
        forceOverride: Boolean = false,
        onResult: (Result<Unit>) -> Unit
    ) {
        val result = runCatching {
            val profile = currentProfile.value
            require(profile.isAdmin && profile.role == UserRole.ADMIN) {
                "Access Denied: Verified administrator privileges are required."
            }
            val deps = getCampsiteRemovalDependencies(campsiteId)
            if (deps.requiresOverrideWarning && !forceOverride) {
                throw IllegalStateException(
                    "Campsite has active reservations or open lotteries. Explicit override required."
                )
            }
            removeCampsiteAsAdmin(campsiteId)
        }
        onResult(result)
    }

    /**
     * Declines a lottery invitation for an entrant and updates notification state.
     */
    suspend fun declineLotteryInvitation(notificationId: String, campsiteId: String)

    suspend fun createReservation(
        campsiteId: String,
        notificationId: String,
        startDate: String,
        endDate: String
    ): Boolean

    /**
     * Marks notifications as seen by the current entrant.
     *
     * @param notificationIds List of notification IDs to update to seen status.
     */
    fun markNotificationsAsSeen(notificationIds: List<String>) {}
}

/**
 * A mock implementation of [AppRepository] used for testing and UI previews.
 * Holds in-memory state and dummy data.
 */
object MockAppRepository : AppRepository {
    
    /** Internal state flow holding the mock profile. */
    private val _currentProfile = MutableStateFlow(
        Profile(
            deviceId = "8F92-A104",
            name = "Alex Morgan",
            email = "alex.morgan@ualberta.ca",
            phone = "(780) 555-0142",
            role = UserRole.ENTRANT,
            isAdmin = true
        )
    )
    
    /** Exposes the mock current profile state. */
    override val currentProfile: StateFlow<Profile> = _currentProfile.asStateFlow()

    /** Internal state flow holding the mock list of all profiles. */
    private val _profiles = MutableStateFlow(listOf(_currentProfile.value))

    /** Exposes the mock list of profiles. */
    override val profiles: StateFlow<List<Profile>> = _profiles.asStateFlow()

    /** Internal state flow holding mock campsite deletion audit records (US 03.01.01). */
    private val _adminCampsiteDeletionLogs = MutableStateFlow<List<AdminCampsiteDeletionLog>>(emptyList())

    /** Exposes the mock campsite deletion audit logs. */
    override val adminCampsiteDeletionLogs: StateFlow<List<AdminCampsiteDeletionLog>> =
        _adminCampsiteDeletionLogs.asStateFlow()

    /** Internal state flow holding the mock campsites list. */
    private val _campsites = MutableStateFlow(
        listOf(
            Campsite(
                id = "camp-banff-01",
                ownerId = "8F92-A104",
                name = "Tunnel Mountain Village I",
                description = "Prime rock climbing access near Banff townsite with mountain views.",
                location = Geolocation(51.1862, -115.5398, "Banff National Park, AB"),
                maxOccupancy = 6,
                pricePerNight = 55.0,
                rating = 4.9,
                amenities = listOf("Fire pit", "Picnic table", "Potable water", "Flush toilets"),
                tags = listOf("Mountain View", "Rock Climbing", "Pet Friendly"),
                qrCodePayload = "solace://campsite/camp-banff-01",
                availableDates = listOf("2027-05-07", "2027-05-08", "2027-05-14", "2027-05-15")
            ),
            Campsite(
                id = "camp-hinton-02",
                ownerId = "8F92-A104",
                name = "Hinton-Grande Cache Pines",
                description = "Includes an outdoor cookhouse and an on-site tennis court.",
                location = Geolocation(53.4004, -117.5684, "Hinton-Grande Cache, AB"),
                maxOccupancy = 8,
                pricePerNight = 65.0,
                rating = 4.7,
                amenities = listOf("Outdoor cookhouse", "Tennis court", "Showers", "Power hookups"),
                tags = listOf("Family Friendly", "RV Accessible"),
                qrCodePayload = "solace://campsite/camp-hinton-02",
                availableDates = listOf("2027-05-21", "2027-05-22", "2027-05-28", "2027-05-29")
            )
        )
    )
    
    /** Exposes the mock list of campsites. */
    override val campsites: StateFlow<List<Campsite>> = _campsites.asStateFlow()

    /** Internal state flow holding the mock lotteries list. */
    private val _lotteries = MutableStateFlow(
        listOf(
            Lottery(
                id = "lot-01",
                campsiteIds = listOf("camp-banff-01"),
                ownerId = "8F92-A104",
                maxWinners = 5,
                waitlistCapacity = 100,
                waitingListEntrantIds = listOf("user-2", "user-3")
            )
        )
    )
    
    /** Exposes the mock lotteries state. */
    override val lotteries: StateFlow<List<Lottery>> = _lotteries.asStateFlow()

    override val lotteryLoadError: StateFlow<String?> = MutableStateFlow(null)

    /** Internal state flow holding the mock reservations list. */
    private val _reservations = MutableStateFlow<List<Reservation>>(emptyList())
    
    /** Exposes the mock reservations state. */
    override val reservations: StateFlow<List<Reservation>> = _reservations.asStateFlow()

    /** Internal state flow holding the mock notifications list. */
    private val _notifications = MutableStateFlow(
        listOf(
            AppNotification(
                id = "notif-1",
                recipientId = "8F92-A104",
                senderId = "8F92-A104",
                campsiteId = "camp-banff-01",
                title = "You won the lottery!",
                message = "Tunnel Mountain Village I · A spot is yours! Accept your invitation to confirm your campsite dates.",
                isLotteryWin = true
            )
        )
    )
    
    /** Exposes the mock notifications state. */
    override val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    private val _ownerEntrantMessages = MutableStateFlow(
        listOf(
            OwnerEntrantMessage(
                id = "msg-101",
                ownerId = "owner-8F92",
                ownerName = "John Campground Owner",
                entrantId = "entrant-123",
                entrantName = "Alex Morgan",
                campsiteId = "camp-banff-01",
                campsiteName = "Tunnel Mountain Village I",
                reservationId = "res-banff-1001",
                reservationStatus = ReservationStatus.ACTIVE,
                messageText = "Hi Alex, your campsite booking for Tunnel Mountain is confirmed. Please let us know if you need bear locker instructions.",
                timestampMillis = System.currentTimeMillis() - 86400000L * 2,
                status = MessageDeliveryStatus.READ
            ),
            OwnerEntrantMessage(
                id = "msg-102",
                ownerId = "owner-8F92",
                ownerName = "John Campground Owner",
                entrantId = "entrant-123",
                entrantName = "Alex Morgan",
                campsiteId = "camp-banff-01",
                campsiteName = "Tunnel Mountain Village I",
                reservationId = "res-banff-1001",
                reservationStatus = ReservationStatus.ACTIVE,
                messageText = "Thanks John! What time is check-in on the first day?",
                timestampMillis = System.currentTimeMillis() - 86400000L * 2 + 3600000L,
                status = MessageDeliveryStatus.READ
            ),
            OwnerEntrantMessage(
                id = "msg-103",
                ownerId = "owner-8F92",
                ownerName = "John Campground Owner",
                entrantId = "entrant-123",
                entrantName = "Alex Morgan",
                campsiteId = "camp-banff-01",
                campsiteName = "Tunnel Mountain Village I",
                reservationId = "res-banff-1001",
                reservationStatus = ReservationStatus.ACTIVE,
                messageText = "Check-in starts at 2:00 PM. Gate code is 4821.",
                timestampMillis = System.currentTimeMillis() - 86400000L * 2 + 7200000L,
                status = MessageDeliveryStatus.READ
            ),
            OwnerEntrantMessage(
                id = "msg-201",
                ownerId = "owner-9999",
                ownerName = "Sarah Connor",
                entrantId = "entrant-456",
                entrantName = "David Miller",
                campsiteId = "camp-hinton-02",
                campsiteName = "Hinton-Grande Cache Pines",
                reservationId = "res-hinton-2002",
                reservationStatus = ReservationStatus.CANCELLED,
                messageText = "Notice: Your reservation #res-hinton-2002 was cancelled per your request.",
                timestampMillis = System.currentTimeMillis() - 86400000L * 5,
                status = MessageDeliveryStatus.DELIVERED
            ),
            OwnerEntrantMessage(
                id = "msg-301",
                ownerId = "owner-8F92",
                ownerName = "John Campground Owner",
                entrantId = "entrant-789",
                entrantName = "Emily Watson",
                campsiteId = "camp-banff-01",
                campsiteName = "Tunnel Mountain Village I",
                reservationId = "res-banff-3003",
                reservationStatus = ReservationStatus.INVITED,
                messageText = "Invitation details for your upcoming stay at Tunnel Mountain Village.",
                timestampMillis = System.currentTimeMillis() - 86400000L * 10,
                status = MessageDeliveryStatus.SENT
            )
        )
    )

    override val ownerEntrantMessages: StateFlow<List<OwnerEntrantMessage>> = _ownerEntrantMessages.asStateFlow()

    private val _adminAuditLogs = MutableStateFlow<List<AdminAuditAccessLog>>(emptyList())
    override val adminAuditLogs: StateFlow<List<AdminAuditAccessLog>> = _adminAuditLogs.asStateFlow()

    override fun logAdminAuditAccess(
        adminId: String,
        adminName: String,
        reservationId: String,
        ownerId: String,
        entrantId: String
    ) {
        val entry = AdminAuditAccessLog(
            id = "audit-${UUID.randomUUID().toString().take(8)}",
            adminId = adminId,
            adminName = adminName.ifBlank { "Admin" },
            targetReservationId = reservationId,
            targetOwnerId = ownerId,
            targetEntrantId = entrantId,
            timestampMillis = System.currentTimeMillis()
        )
        _adminAuditLogs.update { it + entry }
    }


    /** Switches the user's role in the mock profile state. */
    override fun switchRole(newRole: UserRole) {
        if (newRole == UserRole.ADMIN && !_currentProfile.value.isAdmin) {
            return
        }
        _currentProfile.update { it.copy(role = newRole) }
    }

    /** Updates the mock user profile. */
    override fun updateProfile(profile: Profile) {
        _currentProfile.value = profile
    }

    /** Creates a new mock campsite and appends it to the mock state list. */
    override fun createCampsite(campsite: Campsite): Campsite {
        val newId = if (campsite.id.isBlank()) "camp-${UUID.randomUUID().toString().take(6)}" else campsite.id
        val newSite = campsite.copy(
            id = newId,
            qrCodePayload = "solace://campsite/$newId"
        )
        _campsites.update { it + newSite }
        return newSite
    }

    /** Updates an existing campsite in the mock state list. */
    override fun updateCampsite(campsite: Campsite) {
        _campsites.update { list ->
            list.map { if (it.id == campsite.id) campsite else it }
        }
    }

    /** Simulates uploading a campsite image in mock mode by returning the URI string. */
    override fun uploadCampsiteImage(
        campsiteId: String,
        imageUri: Uri,
        onResult: (Result<String>) -> Unit
    ) {
        onResult(Result.success(imageUri.toString()))
    }

    /** Creates a new mock lottery and appends it to the mock state list. */
    override fun createLottery(lottery: Lottery): Lottery {
        val newId = if (lottery.id.isBlank()) "lot-${UUID.randomUUID().toString().take(6)}" else lottery.id
        val newLottery = lottery.copy(id = newId)
        _lotteries.update { it + newLottery }
        return newLottery
    }

    /** Updates an existing mock lottery in the mock state list. */
    override fun updateLottery(lottery: Lottery) {
        _lotteries.update { list ->
            list.map { if (it.id == lottery.id) lottery else it }
        }
    }

    /** Simulates a user joining the waiting list for a mock lottery. */
    override fun joinWaitingList(lotteryId: String, entrantId: String, onResult: WaitingListErrorCallback) {
        val lottery = _lotteries.value.find { it.id == lotteryId }
        val error = when {
            lottery == null -> "Lottery not found."
            entrantId in lottery.waitingListEntrantIds -> null
            !canJoinLottery(lottery) -> "Lottery is not accepting entrants."
            lottery.waitlistCapacity != null &&
                lottery.waitingListEntrantIds.size >= lottery.waitlistCapacity -> "Waitlist is full."
            else -> null
        }
        if (error == null && lottery != null && entrantId !in lottery.waitingListEntrantIds) {
            _lotteries.update { list ->
                list.map {
                    if (it.id == lotteryId) it.copy(
                        waitingListEntrantIds = it.waitingListEntrantIds + entrantId,
                        waitlistJoinedAtMillis = it.waitlistJoinedAtMillis +
                            (entrantId to System.currentTimeMillis())
                    ) else it
                }
            }
        }
        onResult.onResult(error)
    }

    /** Simulates a user leaving the waiting list for a mock lottery. */
    override fun leaveWaitingList(lotteryId: String, entrantId: String) {
        _lotteries.update { list ->
            list.map {
                if (it.id == lotteryId) {
                    it.copy(
                        waitingListEntrantIds =
                            it.waitingListEntrantIds - entrantId,
                        waitlistJoinedAtMillis =
                            it.waitlistJoinedAtMillis - entrantId
                    )
                } else {
                    it
                }
            }
        }
    }

    /**
     * Simulates cancelling an active reservation in memory and appending a cancellation
     * confirmation notification to the entrant's in-app notification history.
     */
    override fun cancelReservation(
        reservationId: String,
        onResult: (Result<CancellationOutcome>) -> Unit
    ) {
        val currentUserId = _currentProfile.value.deviceId
        val target = _reservations.value.find { it.id == reservationId }

        // Validate that the reservation exists, belongs to this entrant, and is still active
        when {
            target == null -> {
                onResult(Result.failure(NoSuchElementException("Reservation not found.")))
                return
            }
            target.entrantId != currentUserId -> {
                onResult(Result.failure(SecurityException("You can only cancel your own reservations.")))
                return
            }
            target.status != ReservationStatus.ACTIVE -> {
                onResult(Result.failure(IllegalStateException("This reservation is already ${target.status.name.lowercase()}.")))
                return
            }
            !target.isUpcomingAndCancellable() -> {
                onResult(Result.failure(IllegalStateException("Only upcoming active reservations can be cancelled.")))
                return
            }
        }

        val cancelledAt = System.currentTimeMillis()
        val cancelledReservation = target.copy(
            status = ReservationStatus.CANCELLED,
            cancelledAtMillis = cancelledAt
        )

        // Update in-memory reservation state to CANCELLED
        _reservations.update { list ->
            list.map { if (it.id == reservationId) cancelledReservation else it }
        }

        // Format dates and timestamp for the in-app cancellation confirmation notification
        val dateSummary = when {
            cancelledReservation.bookedDates.isEmpty() -> "Dates not specified"
            cancelledReservation.bookedDates.size == 1 -> cancelledReservation.bookedDates.first()
            else -> "${cancelledReservation.bookedDates.first()} to ${cancelledReservation.bookedDates.last()}"
        }
        val formattedTime = SimpleDateFormat("MMM dd, yyyy · h:mm a", Locale.getDefault())
            .format(Date(cancelledAt))
        val siteName = cancelledReservation.campsiteName.ifBlank { "Campsite Reservation" }

        // Record the cancellation confirmation in the entrant's notification log
        val confirmationNotification = AppNotification(
            id = "notif-cancel-${cancelledReservation.id}",
            recipientId = currentUserId,
            senderId = cancelledReservation.ownerId.ifBlank { "system" },
            campsiteId = cancelledReservation.campsiteId,
            title = "Reservation Cancelled",
            message = "Your booking at $siteName ($dateSummary) has been cancelled. Confirmation ID: ${cancelledReservation.id}.",
            timestamp = formattedTime,
            isLotteryWin = false,
            isSeen = false
        )
        _notifications.update { list ->
            // Prepend so the newest cancellation confirmation appears at the top of the log
            listOf(confirmationNotification) + list.filterNot { it.id == confirmationNotification.id }
        }

        onResult(
            Result.success(
                CancellationOutcome(
                    cancelledReservation = cancelledReservation,
                    notificationDelivered = true
                )
            )
        )
    }

    /** Stores a confirmed booking in memory for UI tests and previews. */
    override fun confirmBooking(
        campsiteId: String,
        lotteryId: String,
        checkInMillis: Long,
        checkOutMillis: Long,
        onResult: (String?) -> Unit
    ) {
        val site = _campsites.value.find { it.id == campsiteId }
        val lottery = _lotteries.value.find { it.id == lotteryId }
        val entrantId = _currentProfile.value.deviceId
        if (site == null || lottery == null) {
            onResult("Campsite or lottery not found.")
            return
        }
        val dates = bookingNights(checkInMillis, checkOutMillis)
        val error = bookingValidationError(
            site, lottery, entrantId, checkInMillis, checkOutMillis, _reservations.value
        )
        if (error != null) {
            onResult(error)
            return
        }
        val booking = Reservation(
            id = "res-${UUID.randomUUID().toString().take(8)}",
            campsiteId = site.id,
            campsiteName = site.name,
            entrantId = entrantId,
            ownerId = site.ownerId,
            bookedDates = dates,
            status = ReservationStatus.ACTIVE
        )
        _reservations.update { it + booking }
        onResult(null)
    }

    /** Simulates drawing winners for a mock lottery. */
    override fun drawLotteryWinners(lotteryId: String) {
        val targetLottery = _lotteries.value.find { it.id == lotteryId } ?: return
        if (targetLottery.isDrawn && targetLottery.invitedWinnerIds.isNotEmpty()) {
            return
        }
        val count = minOf(targetLottery.waitingListEntrantIds.size, targetLottery.maxWinners)
        val winners = targetLottery.waitingListEntrantIds.shuffled().take(count)

        _lotteries.update { list ->
            list.map {
                if (it.id == lotteryId) {
                    it.copy(invitedWinnerIds = winners, isDrawn = true)
                } else it
            }
        }

        sendWinnerNotifications(targetLottery, winners)
    }

    /** Simulates redrawing winners from backup or remaining waitlist for a mock lottery. */
    override fun redrawLottery(lotteryId: String) {
        val targetLottery = _lotteries.value.find { it.id == lotteryId } ?: return
        val remaining = targetLottery.waitingListEntrantIds.filter { id -> id !in targetLottery.invitedWinnerIds }
        val needed = targetLottery.maxWinners - targetLottery.invitedWinnerIds.size
        if (needed <= 0) return
        val newWinners = remaining.shuffled().take(needed)

        _lotteries.update { list ->
            list.map {
                if (it.id == lotteryId) {
                    it.copy(invitedWinnerIds = it.invitedWinnerIds + newWinners, isDrawn = true)
                } else it
            }
        }

        sendWinnerNotifications(targetLottery, newWinners)
    }

    private fun sendWinnerNotifications(lottery: Lottery, winnerIds: List<String>) {
        if (winnerIds.isEmpty()) return
        val campsiteId = lottery.campsiteIds.firstOrNull().orEmpty()
        val campsiteName = _campsites.value.find { it.id == campsiteId }?.name ?: lottery.name

        val newNotifications = winnerIds.mapNotNull { winnerId ->
            val notifId = "notif-${lottery.id}-$winnerId"
            if (_notifications.value.any { it.id == notifId }) {
                null
            } else {
                AppNotification(
                    id = notifId,
                    recipientId = winnerId,
                    senderId = lottery.ownerId,
                    campsiteId = campsiteId,
                    title = "You won the lottery!",
                    message = "$campsiteName · A spot is yours! Accept your invitation to confirm your campsite dates.",
                    timestamp = "Just now",
                    isLotteryWin = true
                )
            }
        }
        if (newNotifications.isNotEmpty()) {
            _notifications.update { current ->
                val existingIds = current.map { it.id }.toSet()
                current + newNotifications.filter { it.id !in existingIds }
            }
        }
    }

    /** delegates to the full admin removal pipeline. */
    override fun removeCampsiteAsAdmin(campsiteId: String) {
        removeCampsiteAsAdmin(campsiteId, forceOverride = true) {}
    }

    /**
     * Simulates verified admin campsite removal in memory, including dependency checks,
     * waitlist/lottery cleanup, active reservation cancellation, and audit logging (US 03.01.01).
     */
    override fun removeCampsiteAsAdmin(
        campsiteId: String,
        forceOverride: Boolean,
        onResult: (Result<Unit>) -> Unit
    ) {
        val admin = _currentProfile.value
        if (!admin.isAdmin || admin.role != UserRole.ADMIN) {
            onResult(Result.failure(SecurityException("Access Denied: Administrator privileges required.")))
            return
        }

        val targetSite = _campsites.value.find { it.id == campsiteId }
        if (targetSite == null) {
            onResult(Result.failure(NoSuchElementException("Campsite not found.")))
            return
        }

        // Guard against deleting a campsite with active reservations or open lotteries without override
        val deps = getCampsiteRemovalDependencies(campsiteId)
        if (deps.requiresOverrideWarning && !forceOverride) {
            onResult(
                Result.failure(
                    IllegalStateException(
                        "Cannot remove campsite while active reservations or open lotteries exist without override."
                    )
                )
            )
            return
        }

        // 1. Cancel any active reservations linked to this campsite when override is confirmed
        if (deps.activeReservationIds.isNotEmpty()) {
            _reservations.update { list ->
                list.map { res ->
                    if (res.id in deps.activeReservationIds) {
                        res.copy(status = ReservationStatus.CANCELLED)
                    } else res
                }
            }
        }

        // 2. Clean up associated lotteries and unfulfilled waitlists
        _lotteries.update { list ->
            list.mapNotNull { lottery ->
                if (campsiteId !in lottery.campsiteIds) {
                    lottery
                } else if (lottery.campsiteIds.size <= 1) {
                    // Single-campsite lottery: remove it completely along with its unfulfilled waitlist
                    null
                } else {
                    // Multi-campsite lottery: detach only the deleted campsite
                    lottery.copy(campsiteIds = lottery.campsiteIds - campsiteId)
                }
            }
        }

        // 3. Remove pending notifications tied to the removed campsite
        _notifications.update { list -> list.filterNot { it.campsiteId == campsiteId } }

        // 4. Delete the campsite record (invalidates QR deep link and removes from all listings)
        _campsites.update { list -> list.filterNot { it.id == campsiteId } }

        // 5. Record the administrative deletion in the audit log
        val auditEntry = AdminCampsiteDeletionLog(
            id = "del-${UUID.randomUUID().toString().take(8)}",
            adminId = admin.deviceId,
            adminName = admin.name.ifBlank { "Admin" },
            campsiteId = targetSite.id,
            campsiteName = targetSite.name,
            ownerId = targetSite.ownerId,
            timestampMillis = System.currentTimeMillis(),
            overrideUsed = deps.requiresOverrideWarning && forceOverride,
            cancelledReservationIds = deps.activeReservationIds,
            cleanedLotteryIds = deps.affectedLotteryIds
        )
        _adminCampsiteDeletionLogs.update { it + auditEntry }

        onResult(Result.success(Unit))
    }

    override suspend fun declineLotteryInvitation(notificationId: String, campsiteId: String) {
        val currentDeviceId = _currentProfile.value.deviceId
        _notifications.update { list -> list.filterNot { it.id == notificationId } }
        _lotteries.update { list ->
            list.map { lot ->
                if (campsiteId in lot.campsiteIds) {
                    lot.copy(
                        invitedWinnerIds = lot.invitedWinnerIds.filterNot { it == currentDeviceId },
                        waitingListEntrantIds = lot.waitingListEntrantIds.filterNot { it == currentDeviceId },
                        waitlistJoinedAtMillis = lot.waitlistJoinedAtMillis - currentDeviceId
                    )
                } else lot
            }
        }
    }
    fun updateNotifications(newList: List<AppNotification>) {
        _notifications.value = newList
    }

    /** Marks specified notifications as seen in the mock state. */
    override fun markNotificationsAsSeen(notificationIds: List<String>) {
        val targetIds = notificationIds.toSet()
        _notifications.update { list ->
            list.map { if (it.id in targetIds) it.copy(isSeen = true) else it }
        }
    }

    override suspend fun createReservation(
        campsiteId: String,
        notificationId: String,
        startDate: String,
        endDate: String
    ): Boolean {
        // Remove the winning notification once booked
        _notifications.update { list -> list.filterNot { it.id == notificationId } }
        return true
    }
}