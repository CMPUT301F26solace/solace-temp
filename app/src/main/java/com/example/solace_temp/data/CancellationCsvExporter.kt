/**
 * CancellationCsvExporter.kt
 *
 * Purpose: Formats cancelled reservation records into RFC-4180 compliant CSV data
 * and writes the exported file to local device storage via Android's ContentResolver.
 */
package com.example.solace_temp.data

import android.content.Context
import android.net.Uri
import com.example.solace_temp.models.Reservation
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility responsible for generating and writing cancellation confirmation CSV files.
 */
object CancellationCsvExporter {

    private const val CSV_HEADER = "Campsite Name,Dates,Status,Cancellation Date,Confirmation ID"

    /**
     * Generates a safe default filename for the exported cancellation CSV.
     * Strips characters that are invalid in common Android filesystems.
     *
     * @param reservation The cancelled [Reservation] being exported.
     * @return A sanitized filename ending in `.csv`.
     */
    fun suggestedFileName(reservation: Reservation): String {
        val safeId = reservation.id
            .ifBlank { "record" }
            .replace(Regex("[^A-Za-z0-9_-]"), "_")
        return "cancellation_$safeId.csv"
    }

    /**
     * Builds the formatted CSV text for a single cancelled reservation.
     *
     * Columns: Campsite Name, Dates, Status, Cancellation Date, Confirmation ID.
     *
     * @param reservation The cancelled [Reservation] to serialize.
     * @return A two-line CSV string containing the header row and escaped data row.
     */
    fun buildCsvContent(reservation: Reservation): String {
        val campsiteName = reservation.campsiteName.ifBlank { "Campsite Reservation" }
        val dates = formatBookedDates(reservation.bookedDates)
        val status = reservation.status.name
        val cancellationDate = formatCancellationDate(reservation.cancelledAtMillis)
        val confirmationId = reservation.id.ifBlank { "N/A" }

        val dataRow = listOf(
            campsiteName,
            dates,
            status,
            cancellationDate,
            confirmationId
        ).joinToString(",") { escapeCsvField(it) }

        return "$CSV_HEADER\n$dataRow\n"
    }

    /**
     * Writes the generated CSV record to a user-selected destination [Uri] returned by
     * Android's `ACTION_CREATE_DOCUMENT` file intent.
     *
     * @param context Android context providing the [android.content.ContentResolver].
     * @param destinationUri Local storage [Uri] chosen by the entrant.
     * @param reservation The cancelled [Reservation] to write.
     * @return [Result.success] on completion, or [Result.failure] with a readable cause on I/O failure.
     */
    fun writeToUri(
        context: Context,
        destinationUri: Uri,
        reservation: Reservation
    ): Result<Unit> {
        return runCatching {
            val csvContent = buildCsvContent(reservation)
            val outputStream = context.contentResolver.openOutputStream(destinationUri)
                ?: throw IOException("Unable to open destination file for writing.")

            outputStream.use { stream ->
                stream.write(csvContent.toByteArray(StandardCharsets.UTF_8))
                stream.flush()
            }
        }
    }

    /** Formats booked night strings into a readable range for the CSV Dates column. */
    private fun formatBookedDates(bookedDates: List<String>): String = when {
        bookedDates.isEmpty() -> "Dates not specified"
        bookedDates.size == 1 -> bookedDates.first()
        else -> "${bookedDates.first()} to ${bookedDates.last()}"
    }

    /** Formats the cancellation epoch timestamp into a consistent sortable date-time string. */
    private fun formatCancellationDate(cancelledAtMillis: Long?): String {
        val millis = cancelledAtMillis ?: System.currentTimeMillis()
        return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(millis))
    }

    /**
     * Applies RFC-4180 escaping so commas, quotes, or line breaks inside campsite names
     * do not corrupt the CSV columns.
     */
    private fun escapeCsvField(raw: String): String {
        val needsQuoting = raw.contains(',') || raw.contains('"') || raw.contains('\n') || raw.contains('\r')
        return if (needsQuoting) {
            "\"${raw.replace("\"", "\"\"")}\""
        } else {
            raw
        }
    }
}