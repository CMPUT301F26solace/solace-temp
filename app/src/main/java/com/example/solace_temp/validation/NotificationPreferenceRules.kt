/**
 * NotificationPreferenceRules.kt
 * Purpose: Applies an entrant's notification opt-out to status-bar alerts and the unread badge.
 * Issues: Opted-out entrants still get in-app records so lottery invitations can be answered.
 * A status-bar alert can slip through if notifications load before the profile on a cold start.
 */
package com.example.solace_temp.validation

import com.example.solace_temp.models.AppNotification
import com.example.solace_temp.models.Profile
import com.example.solace_temp.models.UserRole

/** Decides which notifications are surfaced to the user, based on their notification preference. */
object NotificationPreferenceRules {
    /**
     * Returns the notifications that should be posted to the status bar:
     * unseen lottery wins and owner waitlist messages.
     *
     * @param profile The current user's profile.
     * @param notifications Notifications addressed to the current user.
     * @param alreadyHandled IDs of notifications that were already posted or skipped.
     */
    fun systemAlertsToPost(
        profile: Profile,
        notifications: List<AppNotification>,
        alreadyHandled: Set<String>
    ): List<AppNotification> {
        if (!profile.receiveNotifications) return emptyList()
        return notifications.filter {
            (it.isLotteryWin || it.broadcastId.isNotBlank()) &&
                !it.isSeen &&
                it.id !in alreadyHandled
        }
    }

    /**
     * Returns the unread count shown on the notifications tab.
     * Returns 0 for non-entrants and for entrants who opted out.
     *
     * @param profile The current user's profile.
     * @param notifications Notifications addressed to the current user.
     */
    fun unreadBadgeCount(
        profile: Profile,
        notifications: List<AppNotification>
    ): Int {
        if (profile.role != UserRole.ENTRANT || !profile.receiveNotifications) return 0
        return notifications.count { !it.isSeen }
    }
}
