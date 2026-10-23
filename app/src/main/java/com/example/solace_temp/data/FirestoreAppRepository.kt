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

class FirestoreAppRepository(context: Context) : AppRepository {

    private val db = FirebaseFirestore.getInstance()

    // US 01.09.01: Identify user by device hardware ID without username/password
    @SuppressLint("HardwareIds")
    private val deviceId: String = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ANDROID_ID
    ) ?: "emulator-fallback-id"

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
    override val currentProfile: StateFlow<Profile> = _currentProfile.asStateFlow()

    private val _campsites = MutableStateFlow<List<Campsite>>(emptyList())
    override val campsites: StateFlow<List<Campsite>> = _campsites.asStateFlow()

    private val _lotteries = MutableStateFlow<List<Lottery>>(emptyList())
    override val lotteries: StateFlow<List<Lottery>> = _lotteries.asStateFlow()

    private val _reservations = MutableStateFlow<List<Reservation>>(emptyList())
    override val reservations: StateFlow<List<Reservation>> = _reservations.asStateFlow()

    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    override val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    init {
        listenToCurrentProfile()
        listenToCampsites()
        listenToLotteries()
        listenToReservations()
        listenToNotifications()
    }

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

    private fun listenToLotteries() {
        db.collection("lotteries").addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener
            _lotteries.value = snapshot.toObjects(Lottery::class.java)
        }
    }

    private fun listenToReservations() {
        db.collection("reservations").addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener
            _reservations.value = snapshot.toObjects(Reservation::class.java)
        }
    }

    private fun listenToNotifications() {
        db.collection("notifications")
            .whereEqualTo("recipientId", deviceId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                _notifications.value = snapshot.toObjects(AppNotification::class.java)
            }
    }

    override fun switchRole(newRole: UserRole) {
        val updated = _currentProfile.value.copy(role = newRole)
        _currentProfile.value = updated
        db.collection("profiles").document(deviceId).set(updated)
    }

    override fun updateProfile(profile: Profile) {
        _currentProfile.value = profile
        db.collection("profiles").document(deviceId).set(profile)
    }

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

    override fun joinWaitingList(lotteryId: String, entrantId: String) {
        db.collection("lotteries").document(lotteryId)
            .update("waitingListEntrantIds", FieldValue.arrayUnion(entrantId))
    }

    override fun leaveWaitingList(lotteryId: String, entrantId: String) {
        db.collection("lotteries").document(lotteryId)
            .update("waitingListEntrantIds", FieldValue.arrayRemove(entrantId))
    }

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

    override fun removeCampsiteAsAdmin(campsiteId: String) {
        db.collection("campsites").document(campsiteId).delete()
    }

    private fun seedInitialDataIfNeeded() {
        MockAppRepository.campsites.value.forEach { sampleSite ->
            db.collection("campsites").document(sampleSite.id).set(sampleSite)
        }
        MockAppRepository.lotteries.value.forEach { sampleLottery ->
            db.collection("lotteries").document(sampleLottery.id).set(sampleLottery)
        }
    }
}