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
import com.example.solace_temp.validation.WaitingListException
import com.example.solace_temp.validation.WaitingListRules
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
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

    /** Mutable flow containing all user profiles. */
    private val _profiles = MutableStateFlow<List<Profile>>(emptyList())

    /** Exposes the list of all profiles flow. */
    override val profiles: StateFlow<List<Profile>> = _profiles.asStateFlow()

    /** Mutable flow containing all campsites. */
    private val _campsites = MutableStateFlow<List<Campsite>>(emptyList())
    
    /** Exposes the list of campsites flow. */
    override val campsites: StateFlow<List<Campsite>> = _campsites.asStateFlow()

    /** Mutable flow containing all lotteries. */
    private val _lotteries = MutableStateFlow<List<Lottery>>(emptyList())
    
    /** Exposes the list of lotteries flow. */
    override val lotteries: StateFlow<List<Lottery>> = _lotteries.asStateFlow()

    private val _lotteryLoadError = MutableStateFlow<String?>(null)
    override val lotteryLoadError: StateFlow<String?> = _lotteryLoadError.asStateFlow()

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
        listenToAllProfiles()
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

    /** Sets up a Firestore listener for all user profiles. */
    private fun listenToAllProfiles() {
        db.collection("profiles").addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener
            _profiles.value = snapshot.toObjects(Profile::class.java)
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
            if (error != null || snapshot == null) {
                _lotteryLoadError.value = error?.localizedMessage ?: "Could not load lotteries."
                return@addSnapshotListener
            }
            _lotteryLoadError.value = null
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
    /**
     * Saves the profile and reports Firestore's write result.
     *
     * @param profile The updated profile.
     * @param onResult Receives success or failure when saving finishes.
     */
    override fun updateProfile(
        profile: Profile,
        onResult: (Result<Unit>) -> Unit
    ) {
        db.collection("profiles")
            .document(deviceId)
            .set(profile)
            .addOnSuccessListener {
                _currentProfile.value = profile
                onResult(Result.success(Unit))
            }
            .addOnFailureListener { error ->
                Log.e("FirestoreRepo", "Profile update failed", error)
                onResult(Result.failure(error))
            }
    }

    /** Creates a new campsite in Firestore. */
    override fun createCampsite(campsite: Campsite): Campsite {
        val newId = if (campsite.id.isBlank()) "camp-${UUID.randomUUID().toString().take(6)}" else campsite.id
        val newSite = campsite.copy(
            id = newId,
            ownerId = deviceId,
            qrCodePayload = "solace://campsite/$newId"
        )
        db.collection("campsites").document(newId).set(newSite)
        return newSite
    }

    /** Creates a new lottery in Firestore. */
    override fun createLottery(lottery: Lottery): Lottery {
        val newId = if (lottery.id.isBlank()) "lot-${UUID.randomUUID().toString().take(6)}" else lottery.id
        val newLottery = lottery.copy(id = newId, ownerId = deviceId)
        db.collection("lotteries").document(newId).set(newLottery)
        return newLottery
    }

    /** Adds the given entrant to the waiting list of a specific lottery in Firestore. */
    override fun joinWaitingList(lotteryId: String, entrantId: String, onResult: WaitingListErrorCallback) {
        val document = db.collection("lotteries").document(lotteryId)
        // Transaction makes checking capacity and joining a single atomic operation.
        db.runTransaction { tx ->
            val lottery = tx.get(document).toObject(Lottery::class.java)
                ?: throw IllegalStateException("Lottery not found.")
            if (entrantId !in lottery.waitingListEntrantIds) {
                if (!canJoinLottery(lottery)) {
                    throw IllegalStateException("Lottery is not accepting entrants.")
                }
                val cap = lottery.waitlistCapacity
                if (cap != null && lottery.waitingListEntrantIds.size >= cap) {
                    throw IllegalStateException("Waitlist is full.")
                }
                tx.update(
                    document,
                    mapOf(
                        "waitingListEntrantIds" to (lottery.waitingListEntrantIds + entrantId),
                        "waitlistJoinedAtMillis" to (lottery.waitlistJoinedAtMillis +
                            (entrantId to System.currentTimeMillis()))
                    )
                )
            }
            true
        }.addOnSuccessListener {
            onResult.onResult(null)
        }.addOnFailureListener { e ->
            Log.e("FirestoreRepo", "Joining waitlist failed", e)
            onResult.onResult(e.localizedMessage ?: "Unable to join the waitlist.")
        }
    }

    /** Removes the given entrant from the waiting list of a specific lottery in Firestore. */
    override fun leaveWaitingList(lotteryId: String, entrantId: String) {
        db.collection("lotteries").document(lotteryId)
            .update("waitingListEntrantIds", FieldValue.arrayRemove(entrantId))
    }
    /** Atomically checks dates, duplicates and capacity before joining (US 01.02.01). */
    override fun joinWaitingListWithResult(
        lotteryId: String,
        entrantId: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        updateWaitingList(
            lotteryId,
            entrantId,
            joining = true,
            onResult = onResult
        )
    }

    /** Removes this device's entry and reports the committed result (US 01.02.02). */
    override fun leaveWaitingList(
        lotteryId: String,
        entrantId: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        updateWaitingList(
            lotteryId,
            entrantId,
            joining = false,
            onResult = onResult
        )
    }

    private fun updateWaitingList(
        lotteryId: String,
        entrantId: String,
        joining: Boolean,
        onResult: (Result<Unit>) -> Unit
    ) {
        if (entrantId != deviceId || entrantId.isBlank()) {
            onResult(
                Result.failure(
                    WaitingListException(
                        "Your device profile is unavailable. Please reopen the app."
                    )
                )
            )
            return
        }

        if (lotteryId.isBlank() || '/' in lotteryId) {
            onResult(
                Result.failure(
                    WaitingListException("This lottery is no longer available.")
                )
            )
            return
        }

        val reference = db.collection("lotteries").document(lotteryId)

        db.runTransaction { transaction ->
            // Read the latest document on every retry, before any write.
            val snapshot = transaction.get(reference)
            val lottery = snapshot.toObject(Lottery::class.java)

            val error = if (joining) {
                WaitingListRules.joinError(lottery, entrantId)
            } else {
                WaitingListRules.leaveError(lottery, entrantId)
            }

            if (error != null) throw WaitingListException(error)

            val entrants = requireNotNull(lottery).waitingListEntrantIds

            val updated = if (joining) {
                (entrants + entrantId).distinct()
            } else {
                entrants - entrantId
            }

            val joinedAt = if (joining) {
                lottery.waitlistJoinedAtMillis +
                        (entrantId to System.currentTimeMillis())
            } else {
                lottery.waitlistJoinedAtMillis - entrantId
            }

            transaction.update(
                reference,
                mapOf(
                    "waitingListEntrantIds" to updated,
                    "waitlistJoinedAtMillis" to joinedAt
                )
            )
            null
        }.addOnSuccessListener {
            onResult(Result.success(Unit))
        }.addOnFailureListener { error ->
            Log.e("FirestoreRepo", "Waiting list update failed", error)
            onResult(Result.failure(error))
        }
    }

    /** Saves an invited entrant's reservation and claims its nights atomically. */
    override fun confirmBooking(
        campsiteId: String,
        lotteryId: String,
        checkInMillis: Long,
        checkOutMillis: Long,
        onResult: (String?) -> Unit
    ) {
        val nights = bookingNights(checkInMillis, checkOutMillis)
        if (nights.isEmpty()) {
            onResult("Choose a valid check-in and check-out date (up to 60 nights).")
            return
        }

        val campsiteRef = db.collection("campsites").document(campsiteId)
        val lotteryRef = db.collection("lotteries").document(lotteryId)
        val reservationId = "res-${campsiteId}-${lotteryId}-${deviceId}"
        val reservationRef = db.collection("reservations").document(reservationId)
        // One document per campsite night prevents two simultaneous bookings of the same night.
        val slotRefs = nights.map { day ->
            db.collection("reservationSlots").document("${campsiteId}_$day")
        }

        db.runTransaction { tx ->
            // Complete all transaction reads before writing anything.
            val site = tx.get(campsiteRef).toObject(Campsite::class.java)
                ?: throw IllegalStateException("Campsite not found.")
            val lottery = tx.get(lotteryRef).toObject(Lottery::class.java)
                ?: throw IllegalStateException("Lottery not found.")
            val existing = tx.get(reservationRef)
            val occupiedSlots = slotRefs.map { tx.get(it) }

            val error = bookingValidationError(
                site, lottery, deviceId, checkInMillis, checkOutMillis, _reservations.value
            )
            if (error != null) throw IllegalStateException(error)
            if (existing.exists()) {
                throw IllegalStateException("You already have a reservation for this campsite and lottery.")
            }
            if (occupiedSlots.any { it.exists() }) {
                throw IllegalStateException("One or more selected dates are already booked.")
            }

            val reservation = Reservation(
                id = reservationId,
                campsiteId = site.id,
                campsiteName = site.name,
                entrantId = deviceId,
                ownerId = site.ownerId,
                bookedDates = nights,
                status = ReservationStatus.ACTIVE
            )
            tx.set(reservationRef, reservation)
            slotRefs.forEachIndexed { index, slotRef ->
                tx.set(slotRef, mapOf(
                    "reservationId" to reservationId,
                    "campsiteId" to campsiteId,
                    "date" to nights[index]
                ))
            }
            true
        }.addOnSuccessListener {
            onResult(null)
        }.addOnFailureListener { error ->
            Log.e("FirestoreRepo", "Booking confirmation failed", error)
            onResult(error.localizedMessage ?: "Unable to confirm the reservation.")
        }
    }

    /** Performs a random draw for the specified lottery and updates Firestore with winners. */
    override fun drawLotteryWinners(lotteryId: String) {
        val lottery = _lotteries.value.find { it.id == lotteryId } ?: return
        val count = minOf(lottery.waitingListEntrantIds.size, lottery.maxWinners)
        val winners = lottery.waitingListEntrantIds.shuffled().take(count)

        db.collection("lotteries").document(lotteryId).update(
            mapOf(
                "invitedWinnerIds" to winners,
                "isDrawn" to true
            )
        )
    }

    /** Redraws or picks backup winners from remaining waitlist in Firestore. */
    override fun redrawLottery(lotteryId: String) {
        val lottery = _lotteries.value.find { it.id == lotteryId } ?: return
        val remaining = lottery.waitingListEntrantIds.filter { id -> id !in lottery.invitedWinnerIds }
        val needed = lottery.maxWinners - lottery.invitedWinnerIds.size
        val newWinners = remaining.shuffled().take(needed)
        val updatedWinners = lottery.invitedWinnerIds + newWinners

        db.collection("lotteries").document(lotteryId).update(
            mapOf(
                "invitedWinnerIds" to updatedWinners,
                "isDrawn" to true
            )
        )
    }

    /** Removes a specific campsite from Firestore. */
    override fun removeCampsiteAsAdmin(campsiteId: String) {
        db.collection("campsites").document(campsiteId).delete()
    }

    override suspend fun declineLotteryInvitation(notificationId: String, campsiteId: String) {
        if (deviceId.isBlank()) return
        try {
            db.collection("notifications").document(notificationId).delete()
            val query = db.collection("lotteries")
                .whereArrayContains("campsiteIds", campsiteId)
                .get()
                .await()
            for (doc in query.documents) {
                val lot = doc.toObject(Lottery::class.java) ?: continue
                val updatedWinners = lot.invitedWinnerIds.filterNot { it == deviceId }
                db.collection("lotteries").document(doc.id).update("invitedWinnerIds", updatedWinners)
            }
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Error declining invitation", e)
        }
    }

    override suspend fun createReservation(
        campsiteId: String,
        notificationId: String,
        startDate: String,
        endDate: String
    ): Boolean {
        if (deviceId.isBlank()) return false
        return try {
            val reservationData = hashMapOf(
                "reservationId" to UUID.randomUUID().toString(),
                "campsiteId" to campsiteId,
                "entrantId" to deviceId,
                "startDate" to startDate,
                "endDate" to endDate,
                "status" to "CONFIRMED",
                "createdAt" to System.currentTimeMillis()
            )
            db.collection("reservations").add(reservationData).await()
            db.collection("notifications").document(notificationId).delete().await()
            true
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Error creating reservation", e)
            false
        }
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