/**
 * LotteryWaitingListSection.kt
 * Purpose: Entrant join/leave controls on campsite details for US 01.01.06,
 * US 01.02.01 and US 01.02.02. Reports results after repository completion.
 * Issues: None known within the waiting-list scope.
 */
package com.example.solace_temp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.solace_temp.data.AppRepository
import com.example.solace_temp.models.Lottery
import com.example.solace_temp.validation.WaitingListRules
import kotlinx.coroutines.delay

/**
 * Shows this campsite's lotteries and allows entrants to leave closed lists.
 *
 * @param campsiteId The campsite whose details the entrant is viewing.
 * @param repository Existing app repository; Firestore handles persistence.
 * @param modifier Optional layout modifier supplied by the details screen.
 */
@Composable
fun LotteryWaitingListSection(
    campsiteId: String,
    repository: AppRepository,
    modifier: Modifier = Modifier
) {
    val lotteries by repository.lotteries.collectAsState()
    val profile by repository.currentProfile.collectAsState()
    val matchingLotteries = lotteries.filter {
        campsiteId in it.campsiteIds
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "Lottery Waiting Lists",
            style = MaterialTheme.typography.titleMedium
        )

        if (matchingLotteries.isEmpty()) {
            Text(
                "There are no lotteries available for this campsite.",
                modifier = Modifier.testTag("waitlist_empty")
            )
        }

        matchingLotteries.forEach { lottery ->
            key(lottery.id, profile.deviceId) {
                WaitingListCard(lottery, profile.deviceId, repository)
            }
        }
    }
}

@Composable
private fun WaitingListCard(
    lottery: Lottery,
    entrantId: String,
    repository: AppRepository
) {
    val storedMembership = entrantId in lottery.waitingListEntrantIds

    // A committed result can arrive just before the snapshot listener catches up.
    var confirmedMembership by remember { mutableStateOf<Boolean?>(null) }
    val isMember = confirmedMembership ?: storedMembership

    var isSaving by remember { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var failed by remember { mutableStateOf(false) }
    var nowMillis by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(storedMembership, confirmedMembership) {
        if (storedMembership == confirmedMembership) {
            confirmedMembership = null
        }
    }

    LaunchedEffect(lottery.id) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(15_000)
        }
    }

    val joinError = WaitingListRules.joinError(
        lottery,
        entrantId,
        nowMillis
    )

    fun finish(result: Result<Unit>, joined: Boolean) {
        isSaving = false
        failed = result.isFailure

        feedback = result.fold(
            onSuccess = {
                confirmedMembership = joined
                if (joined) {
                    "Joined the waiting list."
                } else {
                    "Left the waiting list."
                }
            },
            onFailure = WaitingListRules::failureMessage
        )
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                lottery.name.ifBlank { "Campsite lottery" },
                style = MaterialTheme.typography.titleSmall
            )

            Text(
                text = if (isMember) {
                    "You are on this waiting list."
                } else {
                    joinError ?: "Accepting entries"
                },
                modifier = Modifier.testTag("waitlist_status_${lottery.id}")
            )

            Button(
                onClick = {
                    if (!isSaving && !isMember) {
                        isSaving = true
                        feedback = null

                        repository.joinWaitingList(lottery.id, entrantId) {
                            finish(it, joined = true)
                        }
                    }
                },
                enabled = !isSaving && !isMember && joinError == null,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("waitlist_join_${lottery.id}")
            ) {
                Text(
                    if (isSaving) {
                        "Updating..."
                    } else if (isMember) {
                        "On Waiting List"
                    } else {
                        "Join Waiting List"
                    }
                )
            }

            if (isMember) {
                OutlinedButton(
                    onClick = { showLeaveDialog = true },
                    enabled = !isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("waitlist_leave_${lottery.id}")
                ) {
                    Text("Leave Waiting List")
                }
            }

            feedback?.let { message ->
                Text(
                    text = message,
                    color = if (failed) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.testTag(
                        "waitlist_feedback_${lottery.id}"
                    )
                )
            }
        }
    }

    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            title = { Text("Leave waiting list?") },
            text = {
                Text("You will no longer be waiting for a place in this lottery.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLeaveDialog = false

                        if (!isSaving) {
                            isSaving = true
                            feedback = null

                            repository.leaveWaitingList(
                                lottery.id,
                                entrantId
                            ) {
                                finish(it, joined = false)
                            }
                        }
                    },
                    modifier = Modifier.testTag(
                        "waitlist_confirm_leave_${lottery.id}"
                    )
                ) {
                    Text("Leave")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLeaveDialog = false },
                    modifier = Modifier.testTag(
                        "waitlist_cancel_leave_${lottery.id}"
                    )
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}