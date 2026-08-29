package com.speedo.core.maps

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import java.util.concurrent.ConcurrentHashMap

object MapMarkerUtils {

    private val pinCache = ConcurrentHashMap<String, Drawable>()
    private val vehicleCache = ConcurrentHashMap<String, Drawable>()

    /**
     * Generates or retrieves a cached Rapido-style custom styled Map Pin (Pickup / Drop)
     * @param color Pin accent color (#00C853 for Pickup, #D50000 for Drop)
     * @param label "P" for Pickup, "D" for Drop
     */
    fun createPinDrawable(context: Context, color: Int, label: String? = null): Drawable {
        val key = "rapido_pin_$color-$label"
        pinCache[key]?.let { return it }

        val width = 76
        val height = 100
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Soft Ground Shadow
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.argb(60, 0, 0, 0)
            style = Paint.Style.FILL
        }
        canvas.drawOval(RectF(16f, height - 12f, width - 16f, height - 2f), shadowPaint)

        // 2. Main Teardrop Pin Body
        val pinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
            setShadowLayer(6f, 0f, 3f, Color.argb(90, 0, 0, 0))
        }

        val path = Path().apply {
            arcTo(RectF(6f, 4f, width - 6f, 68f), 180f, 180f, false)
            lineTo(width / 2f, height - 10f)
            close()
        }
        canvas.drawPath(path, pinPaint)

        // 3. Crisp White Border
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 3.5f
        }
        canvas.drawPath(path, borderPaint)

        // 4. Center White Circle
        val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawCircle(width / 2f, 36f, 20f, innerPaint)

        // 5. Letter Label ("P" or "D") or Center Dot
        if (!label.isNullOrBlank()) {
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                textSize = 21f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val bounds = Rect()
            textPaint.getTextBounds(label, 0, label.length, bounds)
            canvas.drawText(label, width / 2f, 36f + bounds.height() / 2f, textPaint)
        } else {
            val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                style = Paint.Style.FILL
            }
            canvas.drawCircle(width / 2f, 36f, 8f, dotPaint)
        }

        val drawable = BitmapDrawable(context.resources, bitmap)
        pinCache[key] = drawable
        return drawable
    }

    /**
     * Generates Rapido User / Rider Location pulsing beacon
     */
    fun createUserLocationDrawable(context: Context): Drawable {
        val key = "rapido_user_location"
        pinCache[key]?.let { return it }

        val size = 70
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Outer translucent accuracy halo
        val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(55, 33, 150, 243)
            style = Paint.Style.FILL
        }
        canvas.drawCircle(size / 2f, size / 2f, 32f, haloPaint)

        // White border ring
        val whiteRing = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            setShadowLayer(4f, 0f, 2f, Color.argb(80, 0, 0, 0))
        }
        canvas.drawCircle(size / 2f, size / 2f, 16f, whiteRing)

        // Blue GPS Core
        val blueCore = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E88E5")
            style = Paint.Style.FILL
        }
        canvas.drawCircle(size / 2f, size / 2f, 11f, blueCore)

        val drawable = BitmapDrawable(context.resources, bitmap)
        pinCache[key] = drawable
        return drawable
    }

    /**
     * Generates Rapido Signature Vehicle Marker (Bike, Auto, Cab) with smooth bearing rotation
     */
    fun createVehicleDrawable(context: Context, vehicleType: String, bearing: Float = 0f): Drawable {
        val key = "rapido_vehicle_${vehicleType.lowercase()}"
        vehicleCache[key]?.let { return it }

        val size = 88
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Speedo vehicle palette: Speedo Moto (Yellow), Speedo Toto (Cyan/Teal), Speedo 4 (Purple)
        val v = vehicleType.lowercase()
        val bgColor = when {
            v.contains("toto") || v.contains("auto") -> Color.parseColor("#00ACC1")
            v.contains("4") || v.contains("cab") || v.contains("car") -> Color.parseColor("#673AB7")
            else -> Color.parseColor("#FFC107") // Speedo Moto
        }

        // Soft drop shadow
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(70, 0, 0, 0)
            style = Paint.Style.FILL
        }
        canvas.drawCircle(size / 2f, size / 2f + 2f, 32f, shadowPaint)

        // Outer circular disc
        val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = bgColor
            style = Paint.Style.FILL
        }
        canvas.drawCircle(size / 2f, size / 2f, 30f, circlePaint)

        // Heading direction pointer triangle (Points upward to indicate vehicle bearing)
        val pointerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = bgColor
            style = Paint.Style.FILL
        }
        val trianglePath = Path().apply {
            moveTo(size / 2f, 4f)
            lineTo(size / 2f - 9f, 22f)
            lineTo(size / 2f + 9f, 22f)
            close()
        }
        canvas.drawPath(trianglePath, pointerPaint)

        // Crisp White Rim
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 3.5f
        }
        canvas.drawCircle(size / 2f, size / 2f, 30f, borderPaint)

        // Vehicle badge text / symbol: MOTO, TOTO, 4
        val symbol = when {
            v.contains("toto") || v.contains("auto") -> "TOTO"
            v.contains("4") || v.contains("cab") || v.contains("car") -> "4"
            else -> "MOTO"
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (symbol == "MOTO") Color.parseColor("#212121") else Color.WHITE
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val bounds = Rect()
        textPaint.getTextBounds(symbol, 0, symbol.length, bounds)
        canvas.drawText(symbol, size / 2f, size / 2f + bounds.height() / 2f, textPaint)

        val drawable = BitmapDrawable(context.resources, bitmap)
        vehicleCache[key] = drawable
        return drawable
    }
}
