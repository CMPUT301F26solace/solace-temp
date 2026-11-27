package com.example.solace_temp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.solace_temp.data.AppRepository
import com.example.solace_temp.models.Campsite
import com.example.solace_temp.models.Geolocation

// =========================================================================
// CREATE CAMPSITE SCREEN
// =========================================================================
/**
 * Composable screen allowing Owners to create a new campsite.
 * 
 * @param repository The data repository for executing the creation logic.
 * @param onCreated Callback invoked after successful creation, receiving the new campsite's ID.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCampsiteScreen(
    repository: AppRepository,
    onCreated: (String) -> Unit
) {
    val profile by repository.currentProfile.collectAsState()
    var name by remember { mutableStateOf("") }
    var parkLocation by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var maxOccupancy by remember { mutableStateOf("") }
    var imageUrlInput by remember { mutableStateOf("") }

    var feedbackMsg by remember { mutableStateOf("") }
    var feedbackIsError by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var createdCampsiteId by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 100.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Create Campsite", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
        Text(
            "Register your campsite and auto-generate a unique QR code.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )

        val textFieldShape = RoundedCornerShape(16.dp)
        val textFieldColors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
        )

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Campsite Name (Required)") },
            modifier = Modifier.fillMaxWidth(),
            shape = textFieldShape,
            colors = textFieldColors
        )
        OutlinedTextField(
            value = parkLocation,
            onValueChange = { parkLocation = it },
            label = { Text("Location (Required, e.g., Banff, Jasper)") },
            modifier = Modifier.fillMaxWidth(),
            shape = textFieldShape,
            colors = textFieldColors
        )
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description & Amenities (Required)") },
            modifier = Modifier.fillMaxWidth(),
            shape = textFieldShape,
            colors = textFieldColors
        )
        OutlinedTextField(
            value = price,
            onValueChange = { price = it },
            label = { Text("Price per Night ($) (Required)") },
            modifier = Modifier.fillMaxWidth(),
            shape = textFieldShape,
            colors = textFieldColors
        )
        OutlinedTextField(
            value = maxOccupancy,
            onValueChange = { maxOccupancy = it },
            label = { Text("Maximum Occupancy (Required)") },
            modifier = Modifier.fillMaxWidth(),
            shape = textFieldShape,
            colors = textFieldColors
        )
        OutlinedTextField(
            value = imageUrlInput,
            onValueChange = { imageUrlInput = it },
            label = { Text("Image URL (Optional, e.g., https://...)") },
            modifier = Modifier.fillMaxWidth(),
            shape = textFieldShape,
            colors = textFieldColors
        )

        if (feedbackMsg.isNotBlank()) {
            Text(
                text = feedbackMsg,
                color = if (feedbackIsError) MaterialTheme.colorScheme.error else Color(0xFF4CAF50),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Button(
            onClick = {
                val parsedPrice = price.toDoubleOrNull()
                val parsedOccupancy = maxOccupancy.toIntOrNull()

                when {
                    name.isBlank() -> {
                        feedbackIsError = true
                        feedbackMsg = "Error: Campsite name cannot be empty."
                    }
                    parkLocation.isBlank() -> {
                        feedbackIsError = true
                        feedbackMsg = "Error: Location cannot be empty."
                    }
                    description.isBlank() -> {
                        feedbackIsError = true
                        feedbackMsg = "Error: Description cannot be empty."
                    }
                    parsedPrice == null || parsedPrice < 0 -> {
                        feedbackIsError = true
                        feedbackMsg = "Error: Please enter a valid fee/price per night."
                    }
                    parsedOccupancy == null || parsedOccupancy <= 0 -> {
                        feedbackIsError = true
                        feedbackMsg = "Error: Please enter a valid maximum occupancy."
                    }
                    else -> {
                        feedbackIsError = false
                        feedbackMsg = "Campsite registered successfully!"
                        val created = repository.createCampsite(
                            Campsite(
                                ownerId = profile.deviceId,
                                name = name.trim(),
                                description = description.trim(),
                                location = Geolocation(locationName = parkLocation.trim()),
                                pricePerNight = parsedPrice,
                                maxOccupancy = parsedOccupancy,
                                imageUrl = imageUrlInput.trim().ifBlank { null }
                            )
                        )
                        createdCampsiteId = created.id
                        showSuccessDialog = true
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Generate QR Code & Publish", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }

    if (showSuccessDialog && createdCampsiteId != null) {
        AlertDialog(
            onDismissRequest = { /* Keep dialog until action */ },
            title = { Text("Campsite Created Successfully") },
            text = { Text("Your campsite has been registered and is now active. Tap below to view details.") },
            confirmButton = {
                Button(
                    onClick = {
                        val id = createdCampsiteId!!
                        showSuccessDialog = false
                        onCreated(id)
                    }
                ) {
                    Text("View Details")
                }
            }
        )
    }
}
