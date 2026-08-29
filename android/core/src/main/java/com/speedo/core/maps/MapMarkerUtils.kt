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
     * Generates or retrieves a cached custom styled Speedo Map Pin
     * @param color Pin background color
     * @param label Optional text or character in the circle
     */
    fun createPinDrawable(context: Context, color: Int, label: String? = null): Drawable {
        val key = "$color-$label"
        pinCache[key]?.let { return it }

        val width = 72
        val height = 96
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
            setShadowLayer(6f, 0f, 4f, Color.argb(80, 0, 0, 0))
        }

        // Draw pin teardrop
        val path = Path().apply {
            arcTo(RectF(4f, 4f, width - 4f, 68f), 180f, 180f, false)
            lineTo(width / 2f, height - 4f)
            close()
        }
        canvas.drawPath(path, paint)

        // Draw inner white circle
        val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawCircle(width / 2f, 36f, 20f, innerPaint)

        // Draw center dot or label
        if (!label.isNullOrBlank()) {
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                textSize = 20f
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
     * Generates or retrieves a cached vehicle marker (Bike, Auto, Cab)
     */
    fun createVehicleDrawable(context: Context, vehicleType: String, bearing: Float = 0f): Drawable {
        val key = vehicleType.lowercase()
        vehicleCache[key]?.let { return it }

        val size = 80
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Vehicle background color: Orange for bike, Cyan for auto, Purple for cab
        val bgColor = when (vehicleType.lowercase()) {
            "auto" -> Color.parseColor("#00B0FF")
            "cab" -> Color.parseColor("#7C4DFF")
            else -> Color.parseColor("#FF6600") // Bike
        }

        // Draw shadow and circular badge
        val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = bgColor
            style = Paint.Style.FILL
            setShadowLayer(8f, 0f, 4f, Color.argb(90, 0, 0, 0))
        }
        canvas.drawCircle(size / 2f, size / 2f, 32f, circlePaint)

        // Draw white border
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        canvas.drawCircle(size / 2f, size / 2f, 32f, borderPaint)

        // Draw vehicle abbreviation in center
        val symbol = when (vehicleType.lowercase()) {
            "auto" -> "AUTO"
            "cab" -> "CAB"
            else -> "BIKE"
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 13f
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
