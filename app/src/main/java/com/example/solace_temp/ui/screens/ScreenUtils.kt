/**
 * ScreenUtils.kt
 * 
 * Purpose: Helper utilities for screen formatting, QR code image generation, sharing payloads, and date string parsing.
 * 
 * Issues / Outstanding Issues:
 * - QR code bitmatrix generation uses ZXing library defaults.
 */
package com.example.solace_temp.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.core.content.FileProvider
import com.example.solace_temp.models.Campsite
import com.example.solace_temp.models.Lottery
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility object for generating and sharing QR codes.
 */
object QrCodeGeneratorUtil {
    
    /**
     * Generates a QR Code Bitmap from the provided string payload.
     * 
     * @param payload The string data to encode into the QR code.
     * @param size The width and height in pixels for the generated QR code (default 512).
     * @return A [Bitmap] object representing the QR code.
     */
    fun generateQrBitmap(payload: String, size: Int = 512): Bitmap {
        val bitMatrix = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, size, size)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) AndroidColor.BLACK else AndroidColor.WHITE)
            }
        }
        return bitmap
    }

    /**
     * Invokes the system share sheet to share a campsite's generated QR code image and deep link.
     * 
     * @param context The application context used to start the share intent.
     * @param campsite The [Campsite] object containing the data to share.
     * @param bitmap The generated QR code [Bitmap] to share.
     */
    fun shareQrCodeImage(context: Context, campsite: Campsite, bitmap: Bitmap) {
        try {
            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs()
            val file = File(cachePath, "campsite_${campsite.id}_qr.png")
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Check out ${campsite.name} on Solace Campsites! Scan or open: ${campsite.qrCodePayload}"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Campsite QR Code"))
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to sharing just the text link if saving/sharing the image fails
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Check out ${campsite.name} on Solace Campsites! Scan or open: ${campsite.qrCodePayload}"
                )
                type = "text/plain"
            }
            context.startActivity(Intent.createChooser(sendIntent, "Share Campsite QR Link"))
        }
    }
}

private val dateFormatter = object : ThreadLocal<SimpleDateFormat>() {
    override fun initialValue(): SimpleDateFormat =
        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
}

/**
 * Formats a given timestamp in milliseconds into a readable date string.
 * @param millis The timestamp in milliseconds.
 * @return A formatted string like "Oct 24, 2026", or "Not Set" if null.
 */
fun formatDateMillis(millis: Long?): String {
    if (millis == null) return "Not Set"
    return dateFormatter.get()?.format(Date(millis)) ?: "Not Set"
}

/**
 * Determines the current status of a lottery based on registration and booking timestamps.
 * 
 * @param lottery The [Lottery] object to evaluate.
 * @return A string representing the status: "Upcoming", "Active", "Booking", "Finished", or "N/A".
 */
fun getLotteryStatus(lottery: Lottery?, campsite: Campsite? = null): String {
    if (lottery == null) return "N/A"
    val now = System.currentTimeMillis()
    val regStart = lottery.registrationStartMillis ?: 0L
    val regEnd = lottery.registrationEndMillis ?: Long.MAX_VALUE
    val bookingStart = lottery.bookingStartMillis ?: campsite?.bookingStartDateMillis ?: regEnd
    val bookingEnd = lottery.bookingEndMillis ?: campsite?.bookingEndDateMillis ?: Long.MAX_VALUE

    return when {
        now < regStart -> "Upcoming"
        now in regStart..regEnd -> "Active"
        lottery.isDrawn && now > bookingEnd -> "Finished"
        now in bookingStart..bookingEnd || now in (regEnd + 1)..bookingEnd || lottery.isDrawn -> "Booking"
        now > bookingEnd -> "Finished"
        else -> "N/A"
    }
}

/**
 * Checks whether a campsite is currently available for creating a new lottery.
 * A campsite is available only if it is not part of any ongoing lottery (Upcoming, Active, or Booking).
 *
 * @param campsite The [Campsite] to check.
 * @param lotteries The list of all known lotteries.
 * @return True if no active/ongoing lottery is assigned to this campsite, false otherwise.
 */
fun isCampsiteAvailableForLottery(campsite: Campsite, lotteries: List<Lottery>): Boolean {
    return !lotteries.any { lot ->
        lot.campsiteIds.contains(campsite.id) &&
            getLotteryStatus(lot, campsite) != "Finished" &&
            getLotteryStatus(lot, campsite) != "N/A"
    }
}
