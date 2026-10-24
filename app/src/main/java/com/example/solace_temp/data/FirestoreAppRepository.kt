/**
 * FirestoreAppRepository.kt
 * 
 * Purpose: Provides a Firestore-backed implementation of [AppRepository].
 * It manages real-time data synchronization with Firebase Firestore for profiles, campsites, lotteries, etc.
 * 
 * Needs Change/Issues:
 * - Device ID is used as an authentication mechanism, which is not secure for production. Real Firebase Auth should be implemented.
 * - Callbacks and snapshot listeners are not properly cleaned up or cancelled, which may cause memory leaks.
 */
package com.example.solace_temp.data

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import android.util.Log
import com.example.solace_temp.models.*
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * Implementation of [AppRepository] using Firebase Firestore as the backend.
 * Provides real-time data flow using SnapshotListeners.
 * 
 * @property context The application context used to extract the device ID.
 */
class FirestoreAppRepository(context: Context) : AppRepository {

    /** Instance of Firebase Firestore used for database operations. */
    private val db = FirebaseFirestore.getInstance()

    /** 
     * Unique hardware ID for the device. 
     * Used for identifying users temporarily before authentication is implemented (US 01.09.01). 
     */
    @SuppressLint("HardwareIds")
    private val deviceId: String = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ANDROID_ID
    ) ?: "emulator-fallback-id"

    /** Mutable flow for the current user's profile state. */
    private val _currentProfile = MutableStateFlow(
        Profile(
            deviceId = deviceId,
            name = "New Explorer",
            email = "",
            phone = "",
            role = UserRole.ENTRANT,
            isAdmin = true
        )
    )
    
    /** Exposes the current profile flow. */
    override val currentProfile: StateFlow<Profile> = _currentProfile.asStateFlow()

    /** Mutable flow containing all campsites. */
    private val _campsites = MutableStateFlow<List<Campsite>>(emptyList())
    
    /** Exposes the list of campsites flow. */
    override val campsites: StateFlow<List<Campsite>> = _campsites.asStateFlow()

    /** Mutable flow containing all lotteries. */
    private val _lotteries = MutableStateFlow<List<Lottery>>(emptyList())
    
    /** Exposes the list of lotteries flow. */
    override val lotteries: StateFlow<List<Lottery>> = _lotteries.asStateFlow()

    /** Mutable flow containing all reservations. */
    private val _reservations = MutableStateFlow<List<Reservation>>(emptyList())
    
    /** Exposes the list of reservations flow. */
    override val reservations: StateFlow<List<Reservation>> = _reservations.asStateFlow()

    /** Mutable flow containing the current user's notifications. */
    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    
    /** Exposes the list of notifications flow. */
    override val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    init {
        listenToCurrentProfile()
        listenToCampsites()
        listenToLotteries()
        listenToReservations()
        listenToNotifications()
    }

    /** Sets up a Firestore listener for the current user's profile document. */
    private fun listenToCurrentProfile() {
        val docRef = db.collection("profiles").document(deviceId)
        docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("FirestoreRepo", "Profile listen failed", error)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                snapshot.toObject(Profile::class.java)?.let {
                    _currentProfile.value = it
                }
            } else {
                // First time this device opens the app -> create profile document in Firestore
                docRef.set(_currentProfile.value)
            }
        }
    }

    /** Sets up a Firestore listener for the campsites collection. */
    private fun listenToCampsites() {
        db.collection("campsites").addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener
            val list = snapshot.toObjects(Campsite::class.java)
            if (list.isEmpty()) {
                seedInitialDataIfNeeded()
            } else {
                _campsites.value = list
            }
        }
    }

    /** Sets up a Firestore listener for the lotteries collection. */
    private fun listenToLotteries() {
        db.collection("lotteries").addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener
            _lotteries.value = snapshot.toObjects(Lottery::class.java)
        }
    }

    /** Sets up a Firestore listener for the reservations collection. */
    private fun listenToReservations() {
        db.collection("reservations").addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener
            _reservations.value = snapshot.toObjects(Reservation::class.java)
        }
    }

    /** Sets up a Firestore listener for notifications directed at the current device ID. */
    private fun listenToNotifications() {
        db.collection("notifications")
            .whereEqualTo("recipientId", deviceId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                _notifications.value = snapshot.toObjects(AppNotification::class.java)
            }
    }

    /** Switches the local and remote profile role. */
    override fun switchRole(newRole: UserRole) {
        val updated = _currentProfile.value.copy(role = newRole)
        _currentProfile.value = updated
        db.collection("profiles").document(deviceId).set(updated)
    }

    /** Updates the profile information in Firestore. */
    override fun updateProfile(profile: Profile) {
        _currentProfile.value = profile
        db.collection("profiles").document(deviceId).set(profile)
    }

    /** Creates a new campsite in Firestore and initializes a default lottery for it. */
    override fun createCampsite(campsite: Campsite): Campsite {
        val newId = if (campsite.id.isBlank()) "camp-${UUID.randomUUID().toString().take(6)}" else campsite.id
        val newSite = campsite.copy(
            id = newId,
            ownerId = deviceId,
            qrCodePayload = "solace://campsite/$newId"
        )
        db.collection("campsites").document(newId).set(newSite)

        // Also create a default active lottery for this campsite so Entrants can join right away
        val defaultLottery = Lottery(
            id = "lot-$newId",
            campsiteIds = listOf(newId),
            ownerId = deviceId,
            maxWinners = 5,
            waitlistCapacity = 100
        )
        db.collection("lotteries").document(defaultLottery.id).set(defaultLottery)

        return newSite
    }

    /** Adds the given entrant to the waiting list of a specific lottery in Firestore. */
    override fun joinWaitingList(lotteryId: String, entrantId: String) {
        db.collection("lotteries").document(lotteryId)
            .update("waitingListEntrantIds", FieldValue.arrayUnion(entrantId))
    }

    /** Removes the given entrant from the waiting list of a specific lottery in Firestore. */
    override fun leaveWaitingList(lotteryId: String, entrantId: String) {
        db.collection("lotteries").document(lotteryId)
            .update("waitingListEntrantIds", FieldValue.arrayRemove(entrantId))
    }

    /** Performs a random draw for the specified lottery and updates Firestore with winners. */
    override fun drawLotteryWinners(lotteryId: String) {
        val lottery = _lotteries.value.find { it.id == lotteryId } ?: return
        val winners = lottery.waitingListEntrantIds.shuffled().take(lottery.maxWinners)

        db.collection("lotteries").document(lotteryId).update(
            mapOf(
                "invitedWinnerIds" to winners,
                "isDrawn" to true
            )
        )
    }

    /** Removes a specific campsite from Firestore. */
    override fun removeCampsiteAsAdmin(campsiteId: String) {
        db.collection("campsites").document(campsiteId).delete()
    }

    /** Seeds initial dummy data into Firestore if collections are empty. */
    private fun seedInitialDataIfNeeded() {
        MockAppRepository.campsites.value.forEach { sampleSite ->
            db.collection("campsites").document(sampleSite.id).set(sampleSite)
        }
        MockAppRepository.lotteries.value.forEach { sampleLottery ->
            db.collection("lotteries").document(sampleLottery.id).set(sampleLottery)
        }
    }
}