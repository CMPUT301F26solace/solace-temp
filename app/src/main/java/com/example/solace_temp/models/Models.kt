/**
 * Models.kt
 * 
 * Purpose: Contains all the data classes and enums used across the application.
 * 
 * Needs Change/Issues:
 * - This is a monolithic file containing multiple distinct models. Each model (e.g., Profile, Campsite, Lottery) should be moved to its own file for better maintainability.
 */
package com.example.solace_temp.models

import com.google.firebase.firestore.PropertyName

/**
 * Defines the roles a user can assume within the application.
 */
enum class UserRole {
    /** Regular user seeking to join lotteries and book campsites. */
    ENTRANT,
    /** User who owns and manages campsites and runs lotteries. */
    CAMPSITE_OWNER,
    /** Administrator with privileges to moderate users and campsites. */
    ADMIN
}

/**
 * Represents geographical coordinates and associated location name.
 * 
 * @property latitude The latitude of the location.
 * @property longitude The longitude of the location.
 * @property locationName The human-readable name of the location.
 */
data class Geolocation(
    val latitude: Double = 51.1784,
    val longitude: Double = -115.5708,
    val locationName: String = "Banff National Park, AB"
)

/** Enum defining the administrative status of a user account. */
enum class AccountStatus {
    ACTIVE, INACTIVE, SUSPENDED
}

/**
 * Represents a user profile in the system.
 * 
 * @property deviceId The unique hardware ID for the user's device.
 * @property name The user's full name.
 * @property email The user's email address.
 * @property phone The user's phone number.
 * @property profileImageUrl Optional URL to the user's profile picture.
 * @property receiveNotifications Indicates whether the user opted in to receive app notifications.
 * @property role The current active [UserRole] of this user.
 * @property isAdmin Indicates if the user has admin privileges.
 * @property accountStatus The administrative status of the user account ([AccountStatus.ACTIVE], [AccountStatus.INACTIVE], or [AccountStatus.SUSPENDED]).
 * @property registrationDateMillis The timestamp in milliseconds when the profile was registered.
 */
data class Profile(
    val deviceId: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val profileImageUrl: String? = null,
    val receiveNotifications: Boolean = true,
    val role: UserRole = UserRole.ENTRANT,
    @get:PropertyName("isAdmin") @field:PropertyName("isAdmin")
    val isAdmin: Boolean = true, // True by default in dev so TAs/team can test Admin role easily
    val accountStatus: AccountStatus = AccountStatus.ACTIVE,
    val registrationDateMillis: Long = System.currentTimeMillis()
)

/** Enum defining the administrative status of a campsite. */
enum class CampsiteStatus {
    ACTIVE, DRAFT, FLAGGED
}

/**
 * Represents a campsite registered in the system.
 * 
 * @property id The unique identifier for the campsite.
 * @property ownerId The device ID of the user who owns the campsite.
 * @property name The name of the campsite.
 * @property description Details and amenities of the campsite.
 * @property location The [Geolocation] of the campsite.
 * @property maxOccupancy The maximum number of guests allowed.
 * @property pricePerNight The cost per night to stay.
 * @property rating The average user rating of the campsite.
 * @property imageUrl Optional URL for a cover image of the campsite.
 * @property amenities A list of amenities available at the campsite.
 * @property status The administrative status of the campsite ([CampsiteStatus.ACTIVE], [CampsiteStatus.DRAFT], or [CampsiteStatus.FLAGGED]).
 * @property dateCreatedMillis Timestamp in milliseconds when the campsite was created.
 * @property tags A list of categorical tags describing the campsite.
 * @property rulesAndPolicy The rules and refund policies for the campsite.
 * @property qrCodePayload The deep link payload encoded in the campsite's QR code.
 * @property availableDates A list of string-formatted dates representing availability.
 * @property bookingStartDateMillis The earliest permissible check-in date in milliseconds.
 * @property bookingEndDateMillis The latest permissible check-out date in milliseconds.
 */
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
    val amenities: List<String> = emptyList(),
    val status: CampsiteStatus = CampsiteStatus.ACTIVE,
    val dateCreatedMillis: Long = System.currentTimeMillis(),
    val tags: List<String> = emptyList(),
    val rulesAndPolicy: String = "Quiet hours 11 PM - 7 AM. Full refund if forest fire warning.",
    val qrCodePayload: String = "", // Deep link: solace://campsite/{id}
    val availableDates: List<String> = emptyList(), // e.g., ["2027-05-07", "2027-05-08"]
    val bookingStartDateMillis: Long? = null,
    val bookingEndDateMillis: Long? = null
)

/**
 * Represents a lottery system for one or more campsites.
 * 
 * @property id The unique identifier for the lottery.
 * @property name The name of the lottery.
 * @property campsiteIds The list of campsite IDs associated with this lottery.
 * @property ownerId The device ID of the owner running the lottery.
 * @property registrationStartMillis The start timestamp for waitlist registration in milliseconds.
 * @property registrationEndMillis The end timestamp for waitlist registration in milliseconds.
 * @property registrationStart The start date for waitlist registration.
 * @property registrationEnd The end date for waitlist registration.
 * @property bookingStartMillis Start timestamp for allowed bookings in milliseconds.
 * @property bookingEndMillis End timestamp for allowed bookings in milliseconds.
 * @property bookingStart The start date for allowed bookings.
 * @property bookingEnd The end date for allowed bookings.
 * @property maxWinners The maximum number of entrants that can be drawn.
 * @property waitlistCapacity Optional limit on how many entrants can join the waitlist.
 * @property waitingListEntrantIds List of entrant IDs currently on the waitlist.
 * @property waitlistJoinedAtMillis Map of entrant device ID to timestamp in milliseconds when they joined.
 * @property invitedWinnerIds List of entrant IDs who have won the draw.
 * @property isDrawn Indicates whether the lottery draw has already taken place.
 */
data class Lottery(
    val id: String = "",
    val name: String = "Default Lottery",
    val campsiteIds: List<String> = emptyList(), // Supports single or multiple campsites (US 02.04.02)
    val ownerId: String = "",
    val registrationStartMillis: Long? = null,
    val registrationEndMillis: Long? = null,
    val registrationStart: String = "2026-10-01",
    val registrationEnd: String = "2026-10-19",
    val bookingStartMillis: Long? = null,
    val bookingEndMillis: Long? = null,
    val bookingStart: String = "2027-05-01",
    val bookingEnd: String = "2027-05-31",
    val maxWinners: Int = 5,
    val waitlistCapacity: Int? = null, // Optional limit (US 02.04.07)
    val waitingListEntrantIds: List<String> = emptyList(),
    // Entrant device ID -> time of joining, in epoch milliseconds.
    // Older lotteries may not have timestamps.
    val waitlistJoinedAtMillis: Map<String, Long> = emptyMap(),
    val invitedWinnerIds: List<String> = emptyList(),
    val isDrawn: Boolean = false
)

/**
 * Enum defining the status of a reservation.
 */
enum class ReservationStatus {
    /** Entrant is invited but has not yet accepted. */
    INVITED,
    /** Reservation is confirmed and active. */
    ACTIVE,
    /** Reservation dates have passed. */
    PAST,
    /** Reservation was cancelled by the entrant or owner. */
    CANCELLED,
    /** Invitation was rejected by the entrant. */
    REJECTED
}

/**
 * Represents a booking/reservation for a campsite.
 * 
 * @property id The unique identifier for the reservation.
 * @property campsiteId The ID of the booked campsite.
 * @property campsiteName The name of the booked campsite.
 * @property entrantId The ID of the entrant making the booking.
 * @property ownerId The ID of the campsite owner.
 * @property bookedDates The list of dates reserved.
 * @property status The current [ReservationStatus].
 * @property agreedToRules Indicates whether the entrant has accepted the campsite rules.
 */
data class Reservation(
    val id: String = "",
    val campsiteId: String = "",
    val campsiteName: String = "",
    val entrantId: String = "",
    val ownerId: String = "",
    val bookedDates: List<String> = emptyList(),
    val status: ReservationStatus = ReservationStatus.ACTIVE,
    val agreedToRules: Boolean = true,
    val cancelledAtMillis: Long? = null
)

/**
 * Represents a notification sent to a user.
 * 
 * @property id The unique identifier for the notification.
 * @property recipientId The ID of the user receiving the notification.
 * @property senderId The ID of the user or system sending the notification.
 * @property campsiteId The relevant campsite ID.
 * @property title The notification title.
 * @property message The detailed notification message body.
 * @property timestamp A formatted timestamp for the notification.
 * @property isLotteryWin Indicates if this notification represents a lottery win.
 * @property isSeen Indicates if the notification has been viewed by the recipient.
 * @property broadcastId ID of the owner broadcast this notification belongs to, or blank if it was sent by the system.
 */
data class AppNotification(
    val id: String = "",
    val recipientId: String = "",
    val senderId: String = "",
    val campsiteId: String = "",
    val title: String = "",
    val message: String = "",
    val timestamp: String = "Today · 9:12 AM",
    @get:PropertyName("isLotteryWin")
    val isLotteryWin: Boolean = false,
    @get:PropertyName("isSeen")
    val isSeen: Boolean = false,
    val broadcastId: String = ""
)

/**
 * Record of a message a campsite owner sent to the entrants on a lottery's waitlist.
 *
 * @property id Unique identifier for the broadcast.
 * @property ownerId Device ID of the owner who sent the message.
 * @property lotteryId ID of the lottery whose waitlist was notified.
 * @property lotteryName Name of the lottery when the message was sent.
 * @property campsiteId ID of the lottery's first campsite.
 * @property campsiteName Name of that campsite when the message was sent.
 * @property title The notification title.
 * @property message The full notification message.
 * @property recipientIds Device IDs of the entrants who received the message.
 * @property recipientCount Number of entrants who received the message.
 * @property sentAtMillis Epoch timestamp in milliseconds when the message was sent.
 */
data class BroadcastNotification(
    val id: String = "",
    val ownerId: String = "",
    val lotteryId: String = "",
    val lotteryName: String = "",
    val campsiteId: String = "",
    val campsiteName: String = "",
    val title: String = "",
    val message: String = "",
    val recipientIds: List<String> = emptyList(),
    val recipientCount: Int = 0,
    val sentAtMillis: Long = System.currentTimeMillis()
)

/**
 * Enum defining the delivery status of a direct message between owner and entrant.
 */
enum class MessageDeliveryStatus {
    SENT, DELIVERED, READ
}

/**
 * Represents a direct message sent between a campsite owner and an entrant with a reservation.
 * 
 * @property id Unique identifier for the message.
 * @property ownerId Device ID of the campsite owner.
 * @property ownerName Display name of the campsite owner.
 * @property entrantId Device ID of the entrant.
 * @property entrantName Display name of the entrant.
 * @property campsiteId Identifier of the relevant campsite.
 * @property campsiteName Name of the relevant campsite.
 * @property reservationId Identifier of the associated reservation.
 * @property reservationStatus Status of the associated reservation.
 * @property messageText The text content of the message.
 * @property timestampMillis Epoch timestamp in milliseconds when the message was sent.
 * @property status Delivery status ([MessageDeliveryStatus.SENT], [MessageDeliveryStatus.DELIVERED], or [MessageDeliveryStatus.READ]).
 */
data class OwnerEntrantMessage(
    val id: String = "",
    val ownerId: String = "",
    val ownerName: String = "",
    val entrantId: String = "",
    val entrantName: String = "",
    val campsiteId: String = "",
    val campsiteName: String = "",
    val reservationId: String = "",
    val reservationStatus: ReservationStatus = ReservationStatus.ACTIVE,
    val messageText: String = "",
    val timestampMillis: Long = System.currentTimeMillis(),
    val status: MessageDeliveryStatus = MessageDeliveryStatus.READ
)

/**
 * Represents an audit record of an administrator accessing communication logs.
 * 
 * @property id Unique identifier for the audit record.
 * @property adminId Device ID of the administrator.
 * @property adminName Display name of the administrator.
 * @property targetReservationId Identifier of the reservation whose chat was audited.
 * @property targetOwnerId Device ID of the target campsite owner.
 * @property targetEntrantId Device ID of the target entrant.
 * @property timestampMillis Epoch timestamp in milliseconds when access occurred.
 * @property actionDescription Human-readable description of the audit action.
 */
data class AdminAuditAccessLog(
    val id: String = "",
    val adminId: String = "",
    val adminName: String = "",
    val targetReservationId: String = "",
    val targetOwnerId: String = "",
    val targetEntrantId: String = "",
    val timestampMillis: Long = System.currentTimeMillis(),
    val actionDescription: String = "Reviewed conversation thread history"
)

/**
 * Encapsulates the result of an entrant cancelling a campsite reservation.
 * Separates the database cancellation from confirmation notification delivery so the UI
 * can surface clear error feedback if notification delivery fails.
 *
 * @property cancelledReservation The updated [Reservation] marked as [ReservationStatus.CANCELLED].
 * @property notificationDelivered True if the cancellation confirmation was logged in the entrant's notifications.
 * @property notificationError Optional human-readable error message when notification delivery fails.
 */
data class CancellationOutcome(
    val cancelledReservation: Reservation,
    val notificationDelivered: Boolean,
    val notificationError: String? = null
)

/**
 * Returns true if this reservation is ACTIVE and its earliest booked date is today or in the future.
 * Expects ISO-8601 date strings ("yyyy-MM-dd"); defaults to eligible if dates are missing or unparsed.
 */
fun Reservation.isUpcomingAndCancellable(
    todayIso: String = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        .format(java.util.Date())
): Boolean {
    if (status != ReservationStatus.ACTIVE) return false
    val earliestDate = bookedDates.minOrNull() ?: return true
    if (!earliestDate.matches(Regex("""\d{4}-\d{2}-\d{2}"""))) return true
    return earliestDate >= todayIso
}

/**
 * Summarizes dependent records associated with a campsite prior to administrative removal (US 03.01.01).
 * Used by the UI to warn administrators about active reservations, open lotteries, and assets
 * that will be cancelled or cleaned up upon deletion.
 *
 * @property activeReservationIds IDs of currently active reservations booked at this campsite.
 * @property openLotteryIds IDs of lotteries linked to this campsite whose registration window is currently open.
 * @property affectedLotteryIds IDs of all lotteries referencing this campsite (including closed/unfulfilled waitlists).
 * @property totalWaitlistedEntrants Total number of unfulfilled waitlist entries across affected lotteries.
 * @property hasMediaAsset True if the campsite currently references a cover image URL.
 * @property hasQrCode True if the campsite currently has an active promotional QR code payload.
 */
data class CampsiteRemovalDependencies(
    val activeReservationIds: List<String> = emptyList(),
    val openLotteryIds: List<String> = emptyList(),
    val affectedLotteryIds: List<String> = emptyList(),
    val totalWaitlistedEntrants: Int = 0,
    val hasMediaAsset: Boolean = false,
    val hasQrCode: Boolean = false
) {
    /** True when active reservations or open-window lotteries require an explicit admin override. */
    val requiresOverrideWarning: Boolean
        get() = activeReservationIds.isNotEmpty() || openLotteryIds.isNotEmpty()
}

/**
 * Audit log entry recording an administrative campsite deletion event (US 03.01.01).
 *
 * @property id Unique identifier for this audit log record.
 * @property adminId Device/account ID of the administrator who executed the removal.
 * @property adminName Display name of the administrator at the time of deletion.
 * @property campsiteId Unique ID of the removed campsite.
 * @property campsiteName Name of the removed campsite for human-readable audit trails.
 * @property ownerId Device/account ID of the campsite's owner.
 * @property timestampMillis Epoch timestamp in milliseconds when the deletion occurred.
 * @property overrideUsed True if the admin explicitly overrode active reservations or open lotteries.
 * @property cancelledReservationIds IDs of any active reservations cancelled during the override.
 * @property cleanedLotteryIds IDs of lotteries updated or deleted to clean up unfulfilled waitlists.
 */
data class AdminCampsiteDeletionLog(
    val id: String = "",
    val adminId: String = "",
    val adminName: String = "",
    val campsiteId: String = "",
    val campsiteName: String = "",
    val ownerId: String = "",
    val timestampMillis: Long = System.currentTimeMillis(),
    val overrideUsed: Boolean = false,
    val cancelledReservationIds: List<String> = emptyList(),
    val cleanedLotteryIds: List<String> = emptyList()
)
