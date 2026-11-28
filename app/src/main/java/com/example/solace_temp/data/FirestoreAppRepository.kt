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
import android.net.Uri
import android.provider.Settings
import android.util.Log
import com.example.solace_temp.models.*
import com.example.solace_temp.validation.WaitingListException
import com.example.solace_temp.validation.WaitingListRules
import com.example.solace_temp.validation.WaitlistNotificationRules
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.tasks.await
import java.util.UUID
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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
     * Used for identifying users automatically on launch without requiring login (US 01.07.01). 
     */
    private val deviceId: String = DeviceIdentityManager.getOrCreateDeviceId(context)

    /** Mutable flow for the current user's profile state. */
    private val _currentProfile = MutableStateFlow(
        Profile(
            deviceId = deviceId,
            name = "New Explorer",
            email = "",
            phone = "",
            role = UserRole.ENTRANT,
            isAdmin = false
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

    private val _ownerEntrantMessages = MutableStateFlow<List<OwnerEntrantMessage>>(emptyList())

    override val ownerEntrantMessages: StateFlow<List<OwnerEntrantMessage>> = _ownerEntrantMessages.asStateFlow()

    private val _systemNotificationLogs = MutableStateFlow(
        listOf(
            SystemNotificationLog(
                id = "notif-log-101",
                senderId = "owner-8F92",
                senderName = "John Campground Owner",
                senderRole = UserRole.CAMPSITE_OWNER,
                recipientIds = listOf("user-101", "user-102", "user-103", "user-104"),
                targetAudienceGroup = "Waitlist Entrants",
                campsiteId = "camp-banff-01",
                campsiteName = "Tunnel Mountain Village I",
                title = "Tunnel Mountain Draw Results Released",
                messageBody = "Congratulations to all selected winners! Please check your invitation to confirm booking before the 48-hour deadline.",
                type = NotificationType.LOTTERY_DRAW,
                deliveryStatus = NotificationDeliveryStatus.SENT,
                channel = NotificationChannel.BOTH,
                timestampMillis = System.currentTimeMillis() - 3600000L * 3
            ),
            SystemNotificationLog(
                id = "notif-log-102",
                senderId = "owner-9999",
                senderName = "Sarah Connor",
                senderRole = UserRole.CAMPSITE_OWNER,
                recipientIds = listOf("user-201", "user-202"),
                targetAudienceGroup = "Registered Entrants",
                campsiteId = "camp-hinton-02",
                campsiteName = "Hinton-Grande Cache Pines",
                title = "Weather Advisory & Cookhouse Maintenance",
                messageBody = "Important update: The outdoor cookhouse will be under maintenance this Friday morning.",
                type = NotificationType.OWNER_BROADCAST,
                deliveryStatus = NotificationDeliveryStatus.SENT,
                channel = NotificationChannel.IN_APP,
                timestampMillis = System.currentTimeMillis() - 86400000L * 2
            ),
            SystemNotificationLog(
                id = "notif-log-103",
                senderId = "admin-sys-01",
                senderName = "Solace Platform Admin",
                senderRole = UserRole.ADMIN,
                recipientIds = listOf("ALL_USERS"),
                targetAudienceGroup = "All Platform Users",
                campsiteId = "",
                campsiteName = "",
                title = "System Maintenance & Terms Update",
                messageBody = "Solace platform scheduled maintenance will occur on Sunday between 2:00 AM and 4:00 AM MST.",
                type = NotificationType.SYSTEM_UPDATE,
                deliveryStatus = NotificationDeliveryStatus.SENT,
                channel = NotificationChannel.PUSH_NOTIFICATION,
                timestampMillis = System.currentTimeMillis() - 86400000L * 4
            ),
            SystemNotificationLog(
                id = "notif-log-104",
                senderId = "owner-8F92",
                senderName = "John Campground Owner",
                senderRole = UserRole.CAMPSITE_OWNER,
                recipientIds = listOf("user-305"),
                targetAudienceGroup = "Individual User",
                campsiteId = "camp-banff-01",
                campsiteName = "Tunnel Mountain Village I",
                title = "Reservation Cancellation Notice",
                messageBody = "Your reservation #res-banff-1001 status changed to CANCELLED.",
                type = NotificationType.RESERVATION_ALERT,
                deliveryStatus = NotificationDeliveryStatus.FAILED,
                channel = NotificationChannel.PUSH_NOTIFICATION,
                timestampMillis = System.currentTimeMillis() - 86400000L * 6
            )
        )
    )

    override val systemNotificationLogs: StateFlow<List<SystemNotificationLog>> = _systemNotificationLogs.asStateFlow()

    private val _notificationLogsError = MutableStateFlow<String?>(null)
    override val notificationLogsError: StateFlow<String?> = _notificationLogsError.asStateFlow()

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

    init {
        listenToCurrentProfile()
        listenToAllProfiles()
        listenToCampsites()
        listenToLotteries()
        listenToReservations()
        listenToNotifications()
        listenToOwnerEntrantMessages()
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
                val profile = snapshot.toObject(Profile::class.java)
                if (profile != null) {
                    val rawAdmin = snapshot.getBoolean("admin") ?: snapshot.getBoolean("isAdmin")
                    val updated = if (rawAdmin != null) profile.copy(isAdmin = rawAdmin) else profile
                    _currentProfile.value = updated
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
            val list = snapshot.documents.mapNotNull { doc ->
                val p = doc.toObject(Profile::class.java) ?: return@mapNotNull null
                val rawAdmin = doc.getBoolean("admin") ?: doc.getBoolean("isAdmin")
                if (rawAdmin != null) p.copy(isAdmin = rawAdmin) else p
            }
            _profiles.value = list
        }
    }

    /** Sets up a Firestore listener for the campsites collection. */
    private fun listenToCampsites() {
        db.collection("campsites").addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener
            _campsites.value = snapshot.toObjects(Campsite::class.java)
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
            triggerExpiredLotteryDraws()
        }
    }

    /** Sets up a Firestore listener for the reservations collection. */
    private fun listenToReservations() {
        db.collection("reservations").addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener
            _reservations.value = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(Reservation::class.java)?.let { res ->
                        if (res.id.isBlank()) res.copy(id = doc.id) else res
                    }
                } catch (e: Exception) {
                    Log.w("FirestoreRepo", "Fallback parsing reservation ${doc.id}", e)
                    val rawStatus = doc.getString("status").orEmpty()
                    val mappedStatus = when (rawStatus.uppercase()) {
                        "CONFIRMED", "ACTIVE" -> ReservationStatus.ACTIVE
                        "CANCELLED" -> ReservationStatus.CANCELLED
                        "INVITED" -> ReservationStatus.INVITED
                        "PAST" -> ReservationStatus.PAST
                        "REJECTED" -> ReservationStatus.REJECTED
                        else -> ReservationStatus.ACTIVE
                    }
                    val campsiteId = doc.getString("campsiteId").orEmpty()
                    val campsiteName = doc.getString("campsiteName")
                        ?: _campsites.value.find { it.id == campsiteId }?.name.orEmpty()
                    val bookedDates = (doc.get("bookedDates") as? List<*>)
                        ?.mapNotNull { it as? String }
                        ?: listOfNotNull(
                            doc.getString("startDate"),
                            doc.getString("endDate")
                        ).filter { it.isNotBlank() }

                    Reservation(
                        id = doc.id,
                        campsiteId = campsiteId,
                        campsiteName = campsiteName,
                        entrantId = doc.getString("entrantId").orEmpty(),
                        ownerId = doc.getString("ownerId").orEmpty(),
                        bookedDates = bookedDates,
                        status = mappedStatus,
                        agreedToRules = doc.getBoolean("agreedToRules") ?: true,
                        cancelledAtMillis = doc.getLong("cancelledAtMillis")
                    )
                }
            }
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

    /** Sets up a Firestore listener for the ownerEntrantMessages collection. */
    private fun listenToOwnerEntrantMessages() {
        db.collection("ownerEntrantMessages")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                _ownerEntrantMessages.value = snapshot.toObjects(OwnerEntrantMessage::class.java)
            }
    }

    /** Switches the local and remote profile role. */
    override fun switchRole(newRole: UserRole) {
        if (newRole == UserRole.ADMIN && !_currentProfile.value.isAdmin) {
            return
        }
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

    /**
     * Updates an existing campsite's details in Firestore and updates the local state flow.
     * @param campsite The updated [Campsite] object to save.
     */
    override fun updateCampsite(campsite: Campsite) {
        if (campsite.id.isNotBlank()) {
            _campsites.update { list ->
                list.map { if (it.id == campsite.id) campsite else it }
            }
            db.collection("campsites").document(campsite.id).set(campsite)
                .addOnSuccessListener {
                    Log.d("FirestoreRepo", "Campsite updated successfully: ${campsite.id}")
                }
                .addOnFailureListener { error ->
                    Log.e("FirestoreRepo", "Campsite update failed", error)
                }
        }
    }

    /**
     * Updates an existing campsite's details in Firestore with result reporting.
     * @param campsite The updated [Campsite] object to save.
     * @param onResult Receives success or failure when saving finishes.
     */
    override fun updateCampsite(
        campsite: Campsite,
        onResult: (Result<Unit>) -> Unit
    ) {
        if (campsite.id.isBlank()) {
            onResult(Result.failure(IllegalArgumentException("Campsite ID cannot be empty")))
            return
        }
        _campsites.update { list ->
            list.map { if (it.id == campsite.id) campsite else it }
        }
        db.collection("campsites").document(campsite.id).set(campsite)
            .addOnSuccessListener {
                onResult(Result.success(Unit))
            }
            .addOnFailureListener { error ->
                Log.e("FirestoreRepo", "Campsite update failed", error)
                onResult(Result.failure(error))
            }
    }

    /**
     * Uploads a campsite image file/media URI to Firebase Storage and returns the public download URL.
     * @param campsiteId The ID of the campsite.
     * @param imageUri The Uri of the local image file to upload.
     * @param onResult Callback receiving Result with the download URL String on success or Throwable on failure.
     */
    override fun uploadCampsiteImage(
        campsiteId: String,
        imageUri: Uri,
        onResult: (Result<String>) -> Unit
    ) {
        try {
            val storageRef = FirebaseStorage.getInstance()
                .reference.child("campsites/${campsiteId}_${System.currentTimeMillis()}.jpg")
            storageRef.putFile(imageUri)
                .continueWithTask { task ->
                    if (!task.isSuccessful) {
                        task.exception?.let { throw it }
                    }
                    storageRef.downloadUrl
                }
                .addOnCompleteListener { task ->
                    if (task.isSuccessful && task.result != null) {
                        val downloadUrl = task.result.toString()
                        onResult(Result.success(downloadUrl))
                    } else {
                        val exc = task.exception ?: Exception("Failed to get image download URL")
                        Log.e("FirestoreRepo", "Image upload failed", exc)
                        onResult(Result.failure(exc))
                    }
                }
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Image upload exception", e)
            onResult(Result.failure(e))
        }
    }

    /** Creates a new lottery in Firestore. */
    override fun createLottery(lottery: Lottery): Lottery {
        val newId = if (lottery.id.isBlank()) "lot-${UUID.randomUUID().toString().take(6)}" else lottery.id
        val newLottery = lottery.copy(id = newId, ownerId = deviceId)
        db.collection("lotteries").document(newId).set(newLottery)
        return newLottery
    }

    /**
     * Updates an existing lottery configuration in Firestore and updates the local state flow.
     * @param lottery The updated [Lottery] configuration to save.
     */
    override fun updateLottery(lottery: Lottery) {
        if (lottery.id.isNotBlank()) {
            _lotteries.update { list ->
                list.map { if (it.id == lottery.id) lottery else it }
            }
            db.collection("lotteries").document(lottery.id).set(lottery)
                .addOnSuccessListener {
                    Log.d("FirestoreRepo", "Lottery updated successfully: ${lottery.id}")
                }
                .addOnFailureListener { error ->
                    Log.e("FirestoreRepo", "Lottery update failed", error)
                }
        }
    }

    /**
     * Updates an existing lottery in Firestore with callback response.
     * @param lottery The updated [Lottery] configuration to save.
     * @param onResult Receives success or failure when saving finishes.
     */
    override fun updateLottery(
        lottery: Lottery,
        onResult: (Result<Unit>) -> Unit
    ) {
        if (lottery.id.isBlank()) {
            onResult(Result.failure(IllegalArgumentException("Lottery ID cannot be empty")))
            return
        }
        _lotteries.update { list ->
            list.map { if (it.id == lottery.id) lottery else it }
        }
        db.collection("lotteries").document(lottery.id).set(lottery)
            .addOnSuccessListener {
                onResult(Result.success(Unit))
            }
            .addOnFailureListener { error ->
                Log.e("FirestoreRepo", "Lottery update failed", error)
                onResult(Result.failure(error))
            }
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
            .update(
                mapOf(
                    "waitingListEntrantIds" to FieldValue.arrayRemove(entrantId),
                    "waitlistJoinedAtMillis.$entrantId" to FieldValue.delete()
                )
            )
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
        if (lotteryId.isBlank()) return
        val document = db.collection("lotteries").document(lotteryId)
        db.runTransaction { tx ->
            val lottery = tx.get(document).toObject(Lottery::class.java)
                ?: return@runTransaction null
            if (lottery.isDrawn) {
                return@runTransaction null
            }
            val count = minOf(lottery.waitingListEntrantIds.size, lottery.maxWinners)
            val winners = lottery.waitingListEntrantIds.shuffled().take(count)

            tx.update(
                document,
                mapOf(
                    "invitedWinnerIds" to winners,
                    "isDrawn" to true
                )
            )
            lottery to winners
        }.addOnSuccessListener { result ->
            if (result != null) {
                val (lottery, winners) = result
                _lotteries.update { list ->
                    list.map {
                        if (it.id == lotteryId) it.copy(invitedWinnerIds = winners, isDrawn = true) else it
                    }
                }
                sendWinnerNotifications(lottery, winners)
            }
        }.addOnFailureListener { error ->
            Log.e("FirestoreRepo", "Lottery draw failed", error)
        }
    }

    /** Redraws or picks backup winners from remaining waitlist in Firestore. */
    override fun redrawLottery(lotteryId: String) {
        if (lotteryId.isBlank()) return
        val document = db.collection("lotteries").document(lotteryId)
        db.runTransaction { tx ->
            val lottery = tx.get(document).toObject(Lottery::class.java)
                ?: return@runTransaction null
            if (!lottery.isDrawn) {
                return@runTransaction null
            }
            val remaining = lottery.waitingListEntrantIds.filter { id -> id !in lottery.invitedWinnerIds }
            val needed = lottery.maxWinners - lottery.invitedWinnerIds.size
            if (needed <= 0 || remaining.isEmpty()) {
                return@runTransaction null
            }
            val newWinners = remaining.shuffled().take(needed)
            val updatedWinners = lottery.invitedWinnerIds + newWinners

            tx.update(
                document,
                mapOf(
                    "invitedWinnerIds" to updatedWinners,
                    "isDrawn" to true
                )
            )
            Triple(lottery, updatedWinners, newWinners)
        }.addOnSuccessListener { result ->
            if (result != null) {
                val (lottery, updatedWinners, newWinners) = result
                _lotteries.update { list ->
                    list.map {
                        if (it.id == lotteryId) it.copy(invitedWinnerIds = updatedWinners, isDrawn = true) else it
                    }
                }
                sendWinnerNotifications(lottery, newWinners)
            }
        }.addOnFailureListener { error ->
            Log.e("FirestoreRepo", "Lottery redraw failed", error)
        }
    }

    override fun triggerExpiredLotteryDraws() {
        val now = System.currentTimeMillis()
        val expired = _lotteries.value.filter { lottery ->
            val endMillis = lottery.registrationEndMillis
            endMillis != null && endMillis <= now && !lottery.isDrawn
        }
        expired.forEach { lottery ->
            drawLotteryWinners(lottery.id)
        }
    }

    private fun sendWinnerNotifications(lottery: Lottery, winnerIds: List<String>) {
        if (winnerIds.isEmpty()) return
        val campsiteId = lottery.campsiteIds.firstOrNull().orEmpty()
        val campsiteName = _campsites.value.find { it.id == campsiteId }?.name ?: lottery.name

        winnerIds.forEach { winnerId ->
            val notifId = "notif-${lottery.id}-$winnerId"
            val notification = AppNotification(
                id = notifId,
                recipientId = winnerId,
                senderId = lottery.ownerId,
                campsiteId = campsiteId,
                title = "You won the lottery!",
                message = "$campsiteName · A spot is yours! Accept your invitation to confirm your campsite dates.",
                timestamp = "Just now",
                isLotteryWin = true
            )
            db.collection("notifications").document(notifId).set(notification)
        }
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
                val updatedWaitlist = lot.waitingListEntrantIds.filterNot { it == deviceId }
                val updatedJoinedAt = lot.waitlistJoinedAtMillis - deviceId
                db.collection("lotteries").document(doc.id).update(
                    mapOf(
                        "invitedWinnerIds" to updatedWinners,
                        "waitingListEntrantIds" to updatedWaitlist,
                        "waitlistJoinedAtMillis" to updatedJoinedAt
                    )
                )
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
        val bookedDates = expandIsoDateRange(startDate, endDate)
        if (bookedDates.isEmpty()) return false

        return try {
            val campsite = _campsites.value.find { it.id == campsiteId }
            val reservationId = "res-${UUID.randomUUID().toString().take(8)}"
            val reservationRef = db.collection("reservations").document(reservationId)
            val notificationRef = db.collection("notifications").document(notificationId)
            val slotRefs = bookedDates.map { day ->
                db.collection("reservationSlots").document("${campsiteId}_$day")
            }

            // Atomically verify all requested dates are free, save the reservation, and claim each date slot
            db.runTransaction { tx ->
                val occupied = slotRefs.any { tx.get(it).exists() }
                if (occupied) {
                    throw IllegalStateException("One or more selected dates are already booked.")
                }

                val reservation = Reservation(
                    id = reservationId,
                    campsiteId = campsiteId,
                    campsiteName = campsite?.name.orEmpty(),
                    entrantId = deviceId,
                    ownerId = campsite?.ownerId.orEmpty(),
                    bookedDates = bookedDates,
                    status = ReservationStatus.ACTIVE,
                    agreedToRules = true
                )

                tx.set(reservationRef, reservation)
                slotRefs.forEachIndexed { index, slotRef ->
                    tx.set(
                        slotRef,
                        mapOf(
                            "reservationId" to reservationId,
                            "campsiteId" to campsiteId,
                            "date" to bookedDates[index]
                        )
                    )
                }
                if (notificationId.isNotBlank()) {
                    tx.delete(notificationRef)
                }
                true
            }.await()
            true
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Error creating reservation", e)
            false
        }
    }

    /** Expands an inclusive YYYY-MM-DD range into individual ISO date strings (up to 60 days). */
    private fun expandIsoDateRange(startDate: String, endDate: String): List<String> {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            isLenient = false
        }
        val start = runCatching { formatter.parse(startDate.trim()) }.getOrNull() ?: return emptyList()
        val end = runCatching { formatter.parse(endDate.trim()) }.getOrNull() ?: return emptyList()
        if (end.before(start)) return emptyList()

        val calendar = Calendar.getInstance().apply { time = start }
        val dates = mutableListOf<String>()
        while (!calendar.time.after(end) && dates.size < 60) {
            dates.add(formatter.format(calendar.time))
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        return dates
    }

    /**
     * Cancels an active reservation in Firestore, frees its booked night slots, and records a
     * cancellation confirmation notification in the entrant's in-app notification log.
     *
     * Notification delivery is executed after the cancellation batch commits so that if
     * notification creation fails, the UI can inform the entrant cleanly without hiding the
     * completed cancellation or blocking CSV export.
     *
     * @param reservationId The unique confirmation ID of the reservation to cancel.
     * @param onResult Callback reporting the [CancellationOutcome] or a cancellation failure.
     */
    override fun cancelReservation(
        reservationId: String,
        onResult: (Result<CancellationOutcome>) -> Unit
    ) {
        if (deviceId.isBlank()) {
            onResult(Result.failure(IllegalStateException("Device profile unavailable. Please reopen the app.")))
            return
        }

        val target = _reservations.value.find { it.id == reservationId }
        when {
            target == null || target.id.isBlank() -> {
                onResult(Result.failure(NoSuchElementException("Reservation could not be found.")))
                return
            }
            target.entrantId != deviceId -> {
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

        // Atomically mark the reservation CANCELLED and free its reserved night slots
        val batch = db.batch()
        val reservationRef = db.collection("reservations").document(target.id)
        batch.update(
            reservationRef,
            mapOf(
                "status" to ReservationStatus.CANCELLED.name,
                "cancelledAtMillis" to cancelledAt
            )
        )
        target.bookedDates.forEach { day ->
            val slotRef = db.collection("reservationSlots").document("${target.campsiteId}_$day")
            batch.delete(slotRef)
        }

        batch.commit()
            .addOnSuccessListener {
                // Optimistically update local reservation state so the UI reflects cancellation immediately
                _reservations.update { list ->
                    list.map { if (it.id == target.id) cancelledReservation else it }
                }

                // Build the cancellation confirmation notification with campsite name, dates, and confirmation ID
                val dateSummary = when {
                    cancelledReservation.bookedDates.isEmpty() -> "Dates not specified"
                    cancelledReservation.bookedDates.size == 1 -> cancelledReservation.bookedDates.first()
                    else -> "${cancelledReservation.bookedDates.first()} to ${cancelledReservation.bookedDates.last()}"
                }
                val formattedTime = SimpleDateFormat("MMM dd, yyyy · h:mm a", Locale.getDefault())
                    .format(Date(cancelledAt))
                val siteName = cancelledReservation.campsiteName.ifBlank { "Campsite Reservation" }
                val notifId = "notif-cancel-${cancelledReservation.id}"

                val confirmationNotification = AppNotification(
                    id = notifId,
                    recipientId = deviceId,
                    senderId = cancelledReservation.ownerId.ifBlank { "system" },
                    campsiteId = cancelledReservation.campsiteId,
                    title = "Reservation Cancelled",
                    message = "Your booking at $siteName ($dateSummary) has been cancelled. Confirmation ID: ${cancelledReservation.id}.",
                    timestamp = formattedTime,
                    isLotteryWin = false,
                    isSeen = false
                )

                // Persist the confirmation notification to the entrant's Firestore notification log
                db.collection("notifications")
                    .document(notifId)
                    .set(confirmationNotification)
                    .addOnSuccessListener {
                        onResult(
                            Result.success(
                                CancellationOutcome(
                                    cancelledReservation = cancelledReservation,
                                    notificationDelivered = true
                                )
                            )
                        )
                    }
                    .addOnFailureListener { notifError ->
                        Log.e("FirestoreRepo", "Cancellation notification delivery failed for ${target.id}", notifError)
                        onResult(
                            Result.success(
                                CancellationOutcome(
                                    cancelledReservation = cancelledReservation,
                                    notificationDelivered = false,
                                    notificationError = "Reservation cancelled, but the confirmation notification failed to send (${notifError.localizedMessage ?: "network error"}). You can still export your CSV record."
                                )
                            )
                        )
                    }
            }
            .addOnFailureListener { error ->
                Log.e("FirestoreRepo", "Failed to cancel reservation ${target.id}", error)
                onResult(
                    Result.failure(
                        Exception("Could not cancel reservation: ${error.localizedMessage ?: "Please try again."}", error)
                    )
                )
            }
    }

    /**
     * Writes one notification per waitlisted recipient and a broadcast record in a single batch,
     * so either every recipient is notified or nothing is saved.
     */
    override fun sendWaitlistNotification(
        lotteryId: String,
        title: String,
        message: String,
        onResult: (Result<BroadcastNotification>) -> Unit
    ) {
        val lottery = _lotteries.value.find { it.id == lotteryId }
        if (lottery == null) {
            onResult(Result.failure(NoSuchElementException("This lottery could not be found.")))
            return
        }
        val recipients = WaitlistNotificationRules.recipients(lottery, _profiles.value)
        WaitlistNotificationRules.sendError(lottery, deviceId, title, message, recipients)?.let { error ->
            onResult(Result.failure(IllegalArgumentException(error)))
            return
        }

        val sentAt = System.currentTimeMillis()
        val campsiteId = lottery.campsiteIds.firstOrNull().orEmpty()
        val broadcast = BroadcastNotification(
            id = "broadcast-${UUID.randomUUID().toString().take(8)}",
            ownerId = deviceId,
            lotteryId = lottery.id,
            lotteryName = lottery.name,
            campsiteId = campsiteId,
            campsiteName = _campsites.value.find { it.id == campsiteId }?.name.orEmpty(),
            title = title.trim(),
            message = message.trim(),
            recipientIds = recipients,
            recipientCount = recipients.size,
            sentAtMillis = sentAt
        )
        val formattedTime = SimpleDateFormat("MMM dd, yyyy · h:mm a", Locale.getDefault())
            .format(Date(sentAt))

        val batch = db.batch()
        recipients.forEach { entrantId ->
            val notifId = "notif-${broadcast.id}-$entrantId"
            batch.set(
                db.collection("notifications").document(notifId),
                AppNotification(
                    id = notifId,
                    recipientId = entrantId,
                    senderId = deviceId,
                    campsiteId = campsiteId,
                    title = broadcast.title,
                    message = broadcast.message,
                    timestamp = formattedTime,
                    broadcastId = broadcast.id
                )
            )
        }
        batch.set(db.collection("broadcasts").document(broadcast.id), broadcast)

        batch.commit()
            .addOnSuccessListener { onResult(Result.success(broadcast)) }
            .addOnFailureListener { error ->
                Log.e("FirestoreRepo", "Failed to send waitlist notification for ${lottery.id}", error)
                onResult(
                    Result.failure(
                        Exception("Could not send the notification: ${error.localizedMessage ?: "Please try again."}", error)
                    )
                )
            }
    }

    /**
     * Marks specified notifications as seen in Firestore and updates the local state.
     *
     * @param notificationIds List of notification IDs to mark as seen.
     */
    override fun markNotificationsAsSeen(notificationIds: List<String>) {
        if (notificationIds.isEmpty()) return
        val targetIds = notificationIds.toSet()
        _notifications.update { list ->
            list.map { if (it.id in targetIds) it.copy(isSeen = true) else it }
        }
        targetIds.forEach { id ->
            db.collection("notifications").document(id).update("isSeen", true)
        }
    }
}