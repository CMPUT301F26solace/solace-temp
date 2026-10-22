package com.example.solace_temp.models

enum class UserRole {
    ENTRANT, CAMPSITE_OWNER, ADMIN
}

data class Geolocation(
    val latitude: Double = 51.1784,
    val longitude: Double = -115.5708,
    val locationName: String = "Banff National Park, AB"
)

data class Profile(
    val deviceId: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val profileImageUrl: String? = null,
    val receiveNotifications: Boolean = true,
    val role: UserRole = UserRole.ENTRANT,
    val isAdmin: Boolean = true // True by default in dev so TAs/team can test Admin role easily
)

data class Campsite(
    val id: String = "",
    val ownerId: String = "",
    val name: String = "",
    val description: String = "",
    val location: Geolocation = Geolocation(),
    val maxOccupancy: Int = 4,
    val pricePerNight: Double = 45.0,
    val rating: Double = 4.8,
    val imageUrl: String? = null,
    val rulesAndPolicy: String = "Quiet hours 11 PM - 7 AM. Full refund if forest fire warning.",
    val qrCodePayload: String = "", // Deep link: solace://campsite/{id}
    val availableDates: List<String> = emptyList() // e.g., ["2027-05-07", "2027-05-08"]
)

data class Lottery(
    val id: String = "",
    val campsiteIds: List<String> = emptyList(), // Supports single or multiple campsites (US 02.04.02)
    val ownerId: String = "",
    val registrationStart: String = "2026-10-01",
    val registrationEnd: String = "2026-10-19",
    val bookingStart: String = "2027-05-01",
    val bookingEnd: String = "2027-05-31",
    val maxWinners: Int = 5,
    val waitlistCapacity: Int? = null, // Optional limit (US 02.04.07)
    val waitingListEntrantIds: List<String> = emptyList(),
    val invitedWinnerIds: List<String> = emptyList(),
    val isDrawn: Boolean = false
)

enum class ReservationStatus {
    INVITED, ACTIVE, PAST, CANCELLED, REJECTED
}

data class Reservation(
    val id: String = "",
    val campsiteId: String = "",
    val campsiteName: String = "",
    val entrantId: String = "",
    val ownerId: String = "",
    val bookedDates: List<String> = emptyList(),
    val status: ReservationStatus = ReservationStatus.ACTIVE,
    val agreedToRules: Boolean = true
)

data class AppNotification(
    val id: String = "",
    val recipientId: String = "",
    val senderId: String = "",
    val campsiteId: String = "",
    val title: String = "",
    val message: String = "",
    val timestamp: String = "Today · 9:12 AM",
    val isLotteryWin: Boolean = false
)