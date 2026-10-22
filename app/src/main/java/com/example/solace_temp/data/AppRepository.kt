package com.example.solace_temp.data

import com.example.solace_temp.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

interface AppRepository {
    val currentProfile: StateFlow<Profile>
    val campsites: StateFlow<List<Campsite>>
    val lotteries: StateFlow<List<Lottery>>
    val reservations: StateFlow<List<Reservation>>
    val notifications: StateFlow<List<AppNotification>>

    fun switchRole(newRole: UserRole)
    fun updateProfile(profile: Profile)
    fun createCampsite(campsite: Campsite): Campsite
    fun joinWaitingList(lotteryId: String, entrantId: String)
    fun leaveWaitingList(lotteryId: String, entrantId: String)
    fun drawLotteryWinners(lotteryId: String)
    fun removeCampsiteAsAdmin(campsiteId: String)
}

object MockAppRepository : AppRepository {
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
    override val currentProfile: StateFlow<Profile> = _currentProfile.asStateFlow()

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
                qrCodePayload = "solace://campsite/camp-hinton-02",
                availableDates = listOf("2027-05-21", "2027-05-22", "2027-05-28", "2027-05-29")
            )
        )
    )
    override val campsites: StateFlow<List<Campsite>> = _campsites.asStateFlow()

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
    override val lotteries: StateFlow<List<Lottery>> = _lotteries.asStateFlow()

    private val _reservations = MutableStateFlow<List<Reservation>>(emptyList())
    override val reservations: StateFlow<List<Reservation>> = _reservations.asStateFlow()

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
    override val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    override fun switchRole(newRole: UserRole) {
        _currentProfile.update { it.copy(role = newRole) }
    }

    override fun updateProfile(profile: Profile) {
        _currentProfile.value = profile
    }

    override fun createCampsite(campsite: Campsite): Campsite {
        val newId = if (campsite.id.isBlank()) "camp-${UUID.randomUUID().toString().take(6)}" else campsite.id
        val newSite = campsite.copy(
            id = newId,
            qrCodePayload = "solace://campsite/$newId"
        )
        _campsites.update { it + newSite }
        return newSite
    }

    override fun joinWaitingList(lotteryId: String, entrantId: String) {
        _lotteries.update { list ->
            list.map {
                if (it.id == lotteryId && !it.waitingListEntrantIds.contains(entrantId)) {
                    it.copy(waitingListEntrantIds = it.waitingListEntrantIds + entrantId)
                } else it
            }
        }
    }

    override fun leaveWaitingList(lotteryId: String, entrantId: String) {
        _lotteries.update { list ->
            list.map {
                if (it.id == lotteryId) {
                    it.copy(waitingListEntrantIds = it.waitingListEntrantIds - entrantId)
                } else it
            }
        }
    }

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

    override fun removeCampsiteAsAdmin(campsiteId: String) {
        _campsites.update { list -> list.filterNot { it.id == campsiteId } }
    }
}