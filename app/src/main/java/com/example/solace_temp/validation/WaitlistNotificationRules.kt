/**
 * WaitlistNotificationRules.kt
 * Purpose: Checks owner messages to a lottery waitlist and picks which entrants receive them.
 * Issues: Entrants whose profiles have not loaded are included, since notifications are on by default.
 */
package com.example.solace_temp.validation

import com.example.solace_temp.models.AccountStatus
import com.example.solace_temp.models.Lottery
import com.example.solace_temp.models.Profile

/** Validation and audience rules for owner notifications sent to a lottery waitlist. */
object WaitlistNotificationRules {
    /**
     * Returns why the message cannot be sent, or null when both fields are filled in.
     *
     * @param title The notification title entered by the owner.
     * @param message The notification message entered by the owner.
     */
    fun messageError(title: String, message: String): String? = when {
        title.isBlank() -> "Enter a title."
        message.isBlank() -> "Enter a message."
        else -> null
    }

    /**
     * Returns the waitlisted entrants who should receive an owner notification.
     * Entrants who opted out of notifications or whose accounts are not active are skipped.
     *
     * @param lottery The lottery whose waitlist is being notified.
     * @param profiles All loaded user profiles.
     */
    fun recipients(lottery: Lottery, profiles: List<Profile>): List<String> {
        val profilesById = profiles.associateBy { it.deviceId }
        return lottery.waitingListEntrantIds.distinct().filter { entrantId ->
            val profile = profilesById[entrantId]
            profile == null ||
                (profile.receiveNotifications && profile.accountStatus == AccountStatus.ACTIVE)
        }
    }

    /**
     * Returns why the owner cannot send this notification, or null when it can be sent.
     *
     * @param lottery The lottery whose waitlist is being notified.
     * @param senderId Device ID of the user sending the message.
     * @param title The notification title.
     * @param message The notification message.
     * @param recipients Entrants selected by [recipients].
     */
    fun sendError(
        lottery: Lottery,
        senderId: String,
        title: String,
        message: String,
        recipients: List<String>
    ): String? = when {
        lottery.ownerId != senderId -> "Only the lottery owner can notify its waitlist."
        messageError(title, message) != null -> messageError(title, message)
        recipients.isEmpty() -> "No entrants on this waitlist can receive notifications."
        else -> null
    }
}
