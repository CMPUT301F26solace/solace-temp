/**
 * WaitlistNotificationDialog.kt
 *
 * Purpose: Dialog where a campsite owner writes a title and message to send to their lottery waitlist.
 *
 * Issues / Outstanding Issues:
 * - Recipients get a phone notification only while their app is open or running in the background.
 *   Push delivery to closed apps would need Firebase Cloud Messaging and a server, which the project does not have.
 * - Firestore batches are limited to 500 writes, so a single message supports up to 499 recipients.
 */
package com.example.solace_temp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.solace_temp.validation.WaitlistNotificationRules

/**
 * Lets the owner compose a notification for the entrants on a lottery waitlist.
 *
 * @param recipientCount Number of entrants who will receive the message.
 * @param isSending True while the message is being saved, which disables the inputs.
 * @param errorMessage Error from the last send attempt, if it failed.
 * @param onSend Called with the trimmed title and message once both are filled in.
 * @param onDismiss Called when the owner closes the dialog.
 */
@Composable
fun WaitlistNotificationDialog(
    recipientCount: Int,
    isSending: Boolean,
    errorMessage: String?,
    onSend: (title: String, message: String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!isSending) onDismiss() },
        title = { Text("Notify Waitlist") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Sends to $recipientCount ${if (recipientCount == 1) "entrant" else "entrants"}.",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    enabled = !isSending,
                    modifier = Modifier.fillMaxWidth().testTag("waitlist_notify_title")
                )
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Message") },
                    minLines = 3,
                    enabled = !isSending,
                    modifier = Modifier.fillMaxWidth().testTag("waitlist_notify_message")
                )
                (validationError ?: errorMessage)?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag("waitlist_notify_error")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    validationError = WaitlistNotificationRules.messageError(title, message)
                    if (validationError == null) {
                        onSend(title.trim(), message.trim())
                    }
                },
                enabled = !isSending,
                modifier = Modifier.testTag("waitlist_notify_send")
            ) {
                Text(if (isSending) "Sending..." else "Send")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSending) {
                Text("Cancel")
            }
        }
    )
}
