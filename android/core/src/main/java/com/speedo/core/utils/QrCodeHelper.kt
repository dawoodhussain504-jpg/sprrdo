package com.speedo.core.utils

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * Offline-proof, native QR Code generator for Speedo applications.
 * Generates sharp Bitmap QR codes in memory without external internet requests.
 */
object QrCodeHelper {

    fun generateQrBitmap(content: String, sizePx: Int = 512): Bitmap? {
        if (content.isBlank()) return null
        return try {
            val hints = mapOf(
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.MARGIN to 1,
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M
            )
            val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) {
            android.util.Log.e("QrCodeHelper", "Failed to generate QR bitmap", e)
            null
        }
    }

    /**
     * Builds standard NPCI UPI payment intent URI.
     * Compatible with PhonePe, Google Pay, Paytm, BHIM, and Cred.
     */
    fun buildUpiPaymentUri(
        upiId: String = "speedo.pay@upi",
        payeeName: String = "Speedo Captain",
        amount: Int,
        tripRef: String = ""
    ): String {
        val safeUpi = upiId.ifBlank { "speedo.pay@upi" }
        val safeName = payeeName.ifBlank { "Speedo Captain" }.replace(" ", "%20")
        val ref = tripRef.takeLast(6)
        return "upi://pay?pa=$safeUpi&pn=$safeName&am=$amount&cu=INR&tn=SpeedoRide-$ref"
    }
}
