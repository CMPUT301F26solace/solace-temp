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

import com.example.solace_temp.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

/**
 * Interface defining the primary data access and domain operations for the application.
 * Provides streams of data via [StateFlow] and methods for data manipulation.
 */
interface AppRepository {
    /** The currently active user's profile. */
    val currentProfile: StateFlow<Profile>
    
    /** List of all campsites available in the system. */
    val campsites: StateFlow<List<Campsite>>
    
    /** List of all active lotteries for campsites. */
    val lotteries: StateFlow<List<Lottery>>
    
    /** List of reservations made across the platform. */
    val reservations: StateFlow<List<Reservation>>
    
    /** List of notifications intended for the current user. */
    val notifications: StateFlow<List<AppNotification>>

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
     * Creates a new campsite and registers it in the repository.
     * @param campsite The [Campsite] details to register.
     * @return The newly created [Campsite] with its generated ID.
     */
    fun createCampsite(campsite: Campsite): Campsite

    /**
     * Creates a new lottery for one or more campsites.
     * @param lottery The [Lottery] configuration to create.
     * @return The newly created [Lottery].
     */
    fun createLottery(lottery: Lottery): Lottery
    
    /**
     * Adds an entrant to a lottery's waiting list.
     * @param lotteryId The unique ID of the lottery.
     * @param entrantId The unique ID of the entrant joining the waitlist.
     */
    fun joinWaitingList(lotteryId: String, entrantId: String)
    
    /**
     * Removes an entrant from a lottery's waiting list.
     * @param lotteryId The unique ID of the lottery.
     * @param entrantId The unique ID of the entrant leaving the waitlist.
     */
    fun leaveWaitingList(lotteryId: String, entrantId: String)
    
    /**
     * Triggers the winner draw process for a specific lottery.
     * @param lotteryId The unique ID of the lottery to draw winners for.
     */
    fun drawLotteryWinners(lotteryId: String)
    
    /**
     * Deletes a campsite from the platform. Requires Admin privileges.
     * @param campsiteId The unique ID of the campsite to remove.
     */
    fun removeCampsiteAsAdmin(campsiteId: String)
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

    /** Switches the user's role in the mock profile state. */
    override fun switchRole(newRole: UserRole) {
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

    /** Creates a new mock lottery and appends it to the mock state list. */
    override fun createLottery(lottery: Lottery): Lottery {
        val newId = if (lottery.id.isBlank()) "lot-${UUID.randomUUID().toString().take(6)}" else lottery.id
        val newLottery = lottery.copy(id = newId)
        _lotteries.update { it + newLottery }
        return newLottery
    }

    /** Simulates a user joining the waiting list for a mock lottery. */
    override fun joinWaitingList(lotteryId: String, entrantId: String) {
        _lotteries.update { list ->
            list.map {
                if (it.id == lotteryId && !it.waitingListEntrantIds.contains(entrantId)) {
                    it.copy(waitingListEntrantIds = it.waitingListEntrantIds + entrantId)
                } else it
            }
        }
    }

    /** Simulates a user leaving the waiting list for a mock lottery. */
    override fun leaveWaitingList(lotteryId: String, entrantId: String) {
        _lotteries.update { list ->
            list.map {
                if (it.id == lotteryId) {
                    it.copy(waitingListEntrantIds = it.waitingListEntrantIds - entrantId)
                } else it
            }
        }
    }

    /** Simulates drawing winners for a mock lottery. */
    override fun drawLotteryWinners(lotteryId: String) {
        _lotteries.update { list ->
            list.map {
                if (it.id == lotteryId) {
                    val winners = it.waitingListEntrantIds.shuffled().take(it.maxWinners)
                    it.copy(invitedWinnerIds = winners, isDrawn = true)
                } else it
            }
        }
    }

    /** Simulates removing a campsite in the mock state. */
    override fun removeCampsiteAsAdmin(campsiteId: String) {
        _campsites.update { list -> list.filterNot { it.id == campsiteId } }
    }
}