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
     * Generates Top-View Realistic Vector Vehicle Marker (Bike on Speedo Moto, Auto on Speedo Toto, Car on Speedo 4)
     * All vehicles face UPWARDS (0° = North). OSMDroid rotates them smoothly using marker.rotation = bearing.
     */
    fun createVehicleDrawable(context: Context, vehicleType: String, bearing: Float = 0f): Drawable {
        val v = vehicleType.lowercase().trim()
        val categoryKey = when {
            v.contains("toto") || v.contains("auto") -> "auto"
            v.contains("4") || v.contains("cab") || v.contains("car") -> "car"
            else -> "bike"
        }

        val cacheKey = "top_view_vehicle_$categoryKey"
        vehicleCache[cacheKey]?.let { return it }

        val size = 110 // High resolution for crisp, sharp rendering on all DPIs
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val cx = size / 2f
        val cy = size / 2f

        when (categoryKey) {
            "bike" -> drawTopViewBike(canvas, cx, cy)
            "auto" -> drawTopViewAuto(canvas, cx, cy)
            "car" -> drawTopViewCar(canvas, cx, cy)
        }

        val drawable = BitmapDrawable(context.resources, bitmap)
        vehicleCache[cacheKey] = drawable
        return drawable
    }

    /**
     * 🏍️ Top-View Motorcycle / Bike (Speedo Moto)
     * Features: Front tire, handlebars with mirrors, fuel tank, helmeted rider, contoured seat, and rear tail
     */
    private fun drawTopViewBike(canvas: Canvas, cx: Float, cy: Float) {
        // 1. Soft Realistic Ground Shadow
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(60, 0, 0, 0)
            style = Paint.Style.FILL
            maskFilter = BlurMaskFilter(6f, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawOval(RectF(cx - 16f, cy - 38f, cx + 18f, cy + 42f), shadowPaint)

        // 2. Front Wheel / Tire (Black rubber)
        val tirePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1C1C1C")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(cx - 4f, cy - 44f, cx + 4f, cy - 24f), 3f, 3f, tirePaint)

        // Front Rim / Hub Highlight
        val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#B0BEC5")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(cx - 1.5f, cy - 38f, cx + 1.5f, cy - 30f), 1.5f, 1.5f, rimPaint)

        // 3. Front Mudguard & Fairing (Rapido Signature Yellow #FFCC00)
        val yellowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFCC00")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(cx - 5f, cy - 34f, cx + 5f, cy - 22f), 4f, 4f, yellowPaint)

        // 4. Wide Handlebars with Grips & Mirrors
        val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#37474F")
            style = Paint.Style.STROKE
            strokeWidth = 3.5f
            strokeCap = Paint.Cap.ROUND
        }
        val handlePath = Path().apply {
            moveTo(cx - 24f, cy - 20f)
            lineTo(cx, cy - 23f)
            lineTo(cx + 24f, cy - 20f)
        }
        canvas.drawPath(handlePath, handlePaint)

        // Handlebar Grips (Black rubber)
        val gripPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx - 24f, cy - 20f, 3f, gripPaint)
        canvas.drawCircle(cx + 24f, cy - 20f, 3f, gripPaint)

        // Rearview Mirrors
        val mirrorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#78909C")
            style = Paint.Style.FILL
        }
        canvas.drawOval(RectF(cx - 28f, cy - 27f, cx - 22f, cy - 22f), mirrorPaint)
        canvas.drawOval(RectF(cx + 22f, cy - 27f, cx + 28f, cy - 22f), mirrorPaint)

        // 5. Fuel Tank (Sculpted Yellow Tank with Center Stripe)
        val tankPath = Path().apply {
            moveTo(cx, cy - 24f)
            cubicTo(cx - 10f, cy - 20f, cx - 11f, cy - 6f, cx - 7f, cy - 2f)
            lineTo(cx + 7f, cy - 2f)
            cubicTo(cx + 11f, cy - 6f, cx + 10f, cy - 20f, cx, cy - 24f)
            close()
        }
        canvas.drawPath(tankPath, yellowPaint)

        // Tank Center Accent (Glossy Black)
        val tankStripe = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#212121")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(cx - 2.5f, cy - 21f, cx + 2.5f, cy - 4f), 2f, 2f, tankStripe)

        // 6. Rider Silhouette (Top-Down Helmet & Shoulders)
        // Shoulders / Jacket
        val shoulderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#263238")
            style = Paint.Style.FILL
        }
        val shoulderPath = Path().apply {
            moveTo(cx - 18f, cy + 8f)
            quadTo(cx, cy - 4f, cx + 18f, cy + 8f)
            lineTo(cx + 14f, cy + 18f)
            lineTo(cx - 14f, cy + 18f)
            close()
        }
        canvas.drawPath(shoulderPath, shoulderPaint)

        // Helmet (Gloss Black with Visor Tint)
        val helmetPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1A1A1A")
            style = Paint.Style.FILL
            setShadowLayer(3f, 0f, 2f, Color.argb(100, 0, 0, 0))
        }
        canvas.drawCircle(cx, cy + 4f, 10f, helmetPaint)

        // Helmet Visor / Shield Reflection (Cyan Glow)
        val visorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#00E5FF")
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawArc(RectF(cx - 8f, cy - 4f, cx + 8f, cy + 10f), 210f, 120f, false, visorPaint)

        // 7. Rear Seat (Saddle)
        val seatPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#212121")
            style = Paint.Style.FILL
        }
        val seatPath = Path().apply {
            moveTo(cx - 8f, cy + 18f)
            lineTo(cx + 8f, cy + 18f)
            lineTo(cx + 6f, cy + 34f)
            lineTo(cx - 6f, cy + 34f)
            close()
        }
        canvas.drawPath(seatPath, seatPaint)

        // 8. Rear Tail Cowl & Red Brake Light
        canvas.drawRoundRect(RectF(cx - 5f, cy + 32f, cx + 5f, cy + 38f), 3f, 3f, yellowPaint)
        val brakeLight = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF1744")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(cx - 3.5f, cy + 37f, cx + 3.5f, cy + 40f), 2f, 2f, brakeLight)

        // 9. Rear Tire
        canvas.drawRoundRect(RectF(cx - 4f, cy + 38f, cx + 4f, cy + 46f), 2f, 2f, tirePaint)
    }

    /**
     * 🛺 Top-View Auto-Rickshaw / Toto / Tuk-Tuk (Speedo Toto)
     * Features: Aerodynamic tapered hood, two-tone yellow/green roof canopy with roof ribs,
     * windshield, 3 wheels (front center, two rear sides), and side entry cutouts
     */
    private fun drawTopViewAuto(canvas: Canvas, cx: Float, cy: Float) {
        // 1. Soft Realistic Ground Shadow
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(60, 0, 0, 0)
            style = Paint.Style.FILL
            maskFilter = BlurMaskFilter(7f, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawRoundRect(RectF(cx - 24f, cy - 40f, cx + 26f, cy + 42f), 12f, 12f, shadowPaint)

        // 2. Wheels (3-Wheel Geometry: 1 Front, 2 Rear Sides)
        val tirePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1C1C1C")
            style = Paint.Style.FILL
        }
        // Front Center Wheel
        canvas.drawRoundRect(RectF(cx - 4f, cy - 45f, cx + 4f, cy - 31f), 3f, 3f, tirePaint)
        // Rear Left Wheel
        canvas.drawRoundRect(RectF(cx - 25f, cy + 12f, cx - 18f, cy + 32f), 3f, 3f, tirePaint)
        // Rear Right Wheel
        canvas.drawRoundRect(RectF(cx + 18f, cy + 12f, cx + 25f, cy + 32f), 3f, 3f, tirePaint)

        // 3. Auto Body Chassis Base (Tapered Tuk-Tuk Silhouette)
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFCC00") // Iconic Auto Yellow
            style = Paint.Style.FILL
        }
        val bodyPath = Path().apply {
            // Front Nose
            moveTo(cx - 10f, cy - 34f)
            quadTo(cx, cy - 39f, cx + 10f, cy - 34f)
            // Front A-pillars expanding
            lineTo(cx + 18f, cy - 16f)
            // Passenger cabin sides
            lineTo(cx + 21f, cy + 34f)
            // Rear tailgate
            quadTo(cx, cy + 37f, cx - 21f, cy + 34f)
            lineTo(cx - 18f, cy - 16f)
            close()
        }
        canvas.drawPath(bodyPath, bodyPaint)

        // 4. Curved Front Windshield Glass
        val glassPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#263238")
            style = Paint.Style.FILL
        }
        val windshieldPath = Path().apply {
            moveTo(cx - 9f, cy - 31f)
            quadTo(cx, cy - 35f, cx + 9f, cy - 31f)
            lineTo(cx + 14f, cy - 18f)
            lineTo(cx - 14f, cy - 18f)
            close()
        }
        canvas.drawPath(windshieldPath, glassPaint)

        // Windshield Glare Reflection
        val glarePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(140, 255, 255, 255)
            style = Paint.Style.STROKE
            strokeWidth = 1.8f
        }
        canvas.drawLine(cx - 6f, cy - 28f, cx + 6f, cy - 20f, glarePaint)

        // 5. Side Mirrors on Front Struts
        val mirrorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#37474F")
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx - 17f, cy - 22f, 2.5f, mirrorPaint)
        canvas.drawCircle(cx + 17f, cy - 22f, 2.5f, mirrorPaint)

        // 6. Two-Tone Canopy Roof (Yellow Front Half, Dark Emerald Green Rear Half)
        val greenCanopy = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#00695C") // Indian Auto Emerald Green
            style = Paint.Style.FILL
        }
        val canopyPath = Path().apply {
            moveTo(cx - 16f, cy - 12f)
            lineTo(cx + 16f, cy - 12f)
            lineTo(cx + 19f, cy + 32f)
            quadTo(cx, cy + 35f, cx - 19f, cy + 32f)
            close()
        }
        canvas.drawPath(canopyPath, greenCanopy)

        // Canopy Front Yellow Overhang
        val canopyOverhang = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFA000")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(cx - 15f, cy - 17f, cx + 15f, cy - 11f), 3f, 3f, canopyOverhang)

        // 7. Roof Ribs / Folds (Classic Auto Fabric Stiffener Lines)
        val ribPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#004D40")
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawLine(cx - 17f, cy, cx + 17f, cy, ribPaint)
        canvas.drawLine(cx - 18f, cy + 14f, cx + 18f, cy + 14f, ribPaint)
        canvas.drawLine(cx - 18.5f, cy + 25f, cx + 18.5f, cy + 25f, ribPaint)

        // 8. Rear Window Glass
        val rearGlass = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#263238")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(cx - 10f, cy + 29f, cx + 10f, cy + 33f), 2f, 2f, rearGlass)

        // 9. Rear Red Brake Lights
        val brakePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF1744")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(cx - 18f, cy + 33f, cx - 14f, cy + 36f), 1.5f, 1.5f, brakePaint)
        canvas.drawRoundRect(RectF(cx + 14f, cy + 33f, cx + 18f, cy + 36f), 1.5f, 1.5f, brakePaint)
    }

    /**
     * 🚗 Top-View Sleek Sedan / Cab (Speedo 4)
     * Features: 4 wheels, sculpted sedan body, dark windshield, aerodynamic roof, side mirrors, and LED tail lamps
     */
    private fun drawTopViewCar(canvas: Canvas, cx: Float, cy: Float) {
        // 1. Soft Realistic Ground Shadow
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(60, 0, 0, 0)
            style = Paint.Style.FILL
            maskFilter = BlurMaskFilter(8f, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawRoundRect(RectF(cx - 24f, cy - 43f, cx + 26f, cy + 45f), 14f, 14f, shadowPaint)

        // 2. 4 Tires (Rubber Black)
        val tirePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1A1A1A")
            style = Paint.Style.FILL
        }
        // Front-left
        canvas.drawRoundRect(RectF(cx - 24f, cy - 32f, cx - 17f, cy - 16f), 3f, 3f, tirePaint)
        // Front-right
        canvas.drawRoundRect(RectF(cx + 17f, cy - 32f, cx + 24f, cy - 16f), 3f, 3f, tirePaint)
        // Rear-left
        canvas.drawRoundRect(RectF(cx - 24f, cy + 16f, cx - 17f, cy + 32f), 3f, 3f, tirePaint)
        // Rear-right
        canvas.drawRoundRect(RectF(cx + 17f, cy + 16f, cx + 24f, cy + 32f), 3f, 3f, tirePaint)

        // 3. Sleek Car Body (Modern Silver-White / Platinum finish with crisp edges)
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FAFAFA")
            style = Paint.Style.FILL
        }
        val carBodyPath = Path().apply {
            // Front Bumper
            moveTo(cx - 14f, cy - 40f)
            quadTo(cx, cy - 44f, cx + 14f, cy - 40f)
            // Front wheel arch
            cubicTo(cx + 19f, cy - 36f, cx + 21f, cy - 12f, cx + 21f, cy)
            // Rear wheel arch
            cubicTo(cx + 21f, cy + 12f, cx + 20f, cy + 36f, cx + 15f, cy + 40f)
            // Rear Bumper
            quadTo(cx, cy + 43f, cx - 15f, cy + 40f)
            cubicTo(cx - 20f, cy + 36f, cx - 21f, cy + 12f, cx - 21f, cy)
            cubicTo(cx - 21f, cy - 12f, cx - 19f, cy - 36f, cx - 14f, cy - 40f)
            close()
        }
        canvas.drawPath(carBodyPath, bodyPaint)

        // Crisp Chassis Border
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CFD8DC")
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
        canvas.drawPath(carBodyPath, borderPaint)

        // 4. Front Headlights (Ice Blue / Xenon White Glow)
        val lightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E0F7FA")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(cx - 17f, cy - 40f, cx - 11f, cy - 35f), 2f, 2f, lightPaint)
        canvas.drawRoundRect(RectF(cx + 11f, cy - 40f, cx + 17f, cy - 35f), 2f, 2f, lightPaint)

        // Bonnet / Hood Crease Lines
        val creasePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#B0BEC5")
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
        }
        canvas.drawLine(cx - 8f, cy - 38f, cx - 10f, cy - 22f, creasePaint)
        canvas.drawLine(cx + 8f, cy - 38f, cx + 10f, cy - 22f, creasePaint)

        // 5. Front Windshield (Dark Tinted Glass with Angled Glare)
        val glassPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#212121")
            style = Paint.Style.FILL
        }
        val windshieldPath = Path().apply {
            moveTo(cx - 12f, cy - 21f)
            quadTo(cx, cy - 24f, cx + 12f, cy - 21f)
            lineTo(cx + 15f, cy - 7f)
            quadTo(cx, cy - 9f, cx - 15f, cy - 7f)
            close()
        }
        canvas.drawPath(windshieldPath, glassPaint)

        // Windshield Glass Glare Reflection
        val glarePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(120, 255, 255, 255)
            style = Paint.Style.STROKE
            strokeWidth = 2f
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(cx - 8f, cy - 18f, cx + 4f, cy - 10f, glarePaint)

        // 6. Side Wing Mirrors
        val mirrorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#37474F")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(cx - 24f, cy - 15f, cx - 18f, cy - 11f), 2f, 2f, mirrorPaint)
        canvas.drawRoundRect(RectF(cx + 18f, cy - 15f, cx + 24f, cy - 11f), 2f, 2f, mirrorPaint)

        // 7. Roof (With Speedo 4 Purple Accent Line or Sunroof)
        val roofPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F5F5F5")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(cx - 14f, cy - 6f, cx + 14f, cy + 18f), 4f, 4f, roofPaint)

        // Sunroof / Roof Glass Tint
        val sunroofPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#37474F")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(cx - 10f, cy - 3f, cx + 10f, cy + 14f), 3f, 3f, sunroofPaint)

        // Speedo 4 Brand Accent Line across Roof
        val cabAccent = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#673AB7") // Speedo 4 Purple
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(cx - 5f, cy + 4f, cx + 5f, cy + 7f), 1.5f, 1.5f, cabAccent)

        // 8. Rear Windshield Glass
        val rearGlassPath = Path().apply {
            moveTo(cx - 14f, cy + 19f)
            quadTo(cx, cy + 17f, cx + 14f, cy + 19f)
            lineTo(cx + 12f, cy + 30f)
            quadTo(cx, cy + 32f, cx - 12f, cy + 30f)
            close()
        }
        canvas.drawPath(rearGlassPath, glassPaint)

        // 9. Rear LED Tail Lights
        val brakePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D50000") // Vibrant Red LED
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(cx - 16f, cy + 37f, cx - 9f, cy + 40f), 1.5f, 1.5f, brakePaint)
        canvas.drawRoundRect(RectF(cx + 9f, cy + 37f, cx + 16f, cy + 40f), 1.5f, 1.5f, brakePaint)
    }
}
