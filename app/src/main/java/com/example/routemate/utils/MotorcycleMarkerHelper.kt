package com.example.routemate.utils

import android.content.Context
import android.graphics.*
import org.maplibre.android.annotations.Icon
import org.maplibre.android.annotations.IconFactory

enum class RiderMarkerType {
    CURRENT_USER,
    PARTNER_ORANGE,
    PARTNER_GREEN,
    PARTNER_PURPLE
}

object MotorcycleMarkerHelper {

    private val iconCache = mutableMapOf<String, Icon>()

    fun getMotorcycleIcon(
        context: Context,
        name: String,
        speedKmh: Float,
        bearing: Float,
        isOnline: Boolean,
        type: RiderMarkerType = RiderMarkerType.CURRENT_USER
    ): Icon {
        // Quantize bearing to 5-degree increments to allow high-performance cache reuse
        val roundedBearing = ((bearing / 5f).toInt() * 5).toFloat()
        val speedInt = speedKmh.toInt()
        val cleanName = if (name.length > 10) name.take(9) + "…" else name
        val cacheKey = "${cleanName}_${speedInt}_${roundedBearing}_${isOnline}_$type"

        iconCache[cacheKey]?.let { return it }

        val bitmap = render3DMotorcycleMarker(
            name = cleanName,
            speedKmh = speedKmh,
            bearing = roundedBearing,
            isOnline = isOnline,
            type = type
        )

        val icon = IconFactory.getInstance(context).fromBitmap(bitmap)
        iconCache[cacheKey] = icon
        return icon
    }

    fun getStartMarkerIcon(context: Context, title: String): Icon {
        val cacheKey = "start_marker_$title"
        iconCache[cacheKey]?.let { return it }

        val bitmap = renderLocationPin(
            pinColor = Color.parseColor("#10B981"),
            label = "START",
            subLabel = title
        )
        val icon = IconFactory.getInstance(context).fromBitmap(bitmap)
        iconCache[cacheKey] = icon
        return icon
    }

    fun getDestinationMarkerIcon(context: Context, title: String): Icon {
        val cacheKey = "dest_marker_$title"
        iconCache[cacheKey]?.let { return it }

        val bitmap = renderLocationPin(
            pinColor = Color.parseColor("#EF4444"),
            label = "DEST",
            subLabel = title
        )
        val icon = IconFactory.getInstance(context).fromBitmap(bitmap)
        iconCache[cacheKey] = icon
        return icon
    }

    fun getWaypointMarkerIcon(context: Context, title: String): Icon {
        val cacheKey = "waypoint_marker_$title"
        iconCache[cacheKey]?.let { return it }

        val bitmap = renderLocationPin(
            pinColor = Color.parseColor("#F59E0B"),
            label = "STOP",
            subLabel = title
        )
        val icon = IconFactory.getInstance(context).fromBitmap(bitmap)
        iconCache[cacheKey] = icon
        return icon
    }

    fun getMeetingPointMarkerIcon(context: Context, title: String): Icon {
        val cacheKey = "meeting_marker_$title"
        iconCache[cacheKey]?.let { return it }

        val bitmap = renderLocationPin(
            pinColor = Color.parseColor("#8B5CF6"),
            label = "MEET",
            subLabel = title
        )
        val icon = IconFactory.getInstance(context).fromBitmap(bitmap)
        iconCache[cacheKey] = icon
        return icon
    }

    fun getJoiningPointMarkerIcon(
        context: Context,
        placeName: String,
        userDistKm: Double,
        userEtaMins: Double,
        partnerName: String,
        partnerDistKm: Double,
        partnerEtaMins: Double,
        riderCount: Int,
        status: JoiningStatus
    ): Icon {
        val cacheKey = "join_marker_${placeName}_${userDistKm.toInt()}_${partnerDistKm.toInt()}_$status"
        iconCache[cacheKey]?.let { return it }

        val bitmap = renderDynamicJoiningPin(
            placeName = placeName,
            userDistKm = userDistKm,
            userEtaMins = userEtaMins,
            partnerName = partnerName,
            partnerDistKm = partnerDistKm,
            partnerEtaMins = partnerEtaMins,
            riderCount = riderCount,
            status = status
        )
        val icon = IconFactory.getInstance(context).fromBitmap(bitmap)
        iconCache[cacheKey] = icon
        return icon
    }

    private fun renderDynamicJoiningPin(
        placeName: String,
        userDistKm: Double,
        userEtaMins: Double,
        partnerName: String,
        partnerDistKm: Double,
        partnerEtaMins: Double,
        riderCount: Int,
        status: JoiningStatus
    ): Bitmap {
        val width = 280
        val height = 120
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val centerX = width / 2f
        val cleanPlace = if (placeName.length > 24) placeName.take(23) + "…" else placeName
        val cleanPartner = if (partnerName.length > 8) partnerName.take(7) + "…" else partnerName

        val (accentColor, statusText) = when (status) {
            JoiningStatus.ALL_JOINED_RIDING_TOGETHER -> Color.parseColor("#10B981") to "RIDING TOGETHER"
            JoiningStatus.RIDER_ARRIVED -> Color.parseColor("#F59E0B") to "RIDER ARRIVED"
            else -> Color.parseColor("#8B5CF6") to "$riderCount RIDERS"
        }

        // Card Rectangle
        val cardRect = RectF(6f, 6f, width - 6f, 92f)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F00A1128") }
        canvas.drawRoundRect(cardRect, 14f, 14f, bgPaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
        }
        canvas.drawRoundRect(cardRect, 14f, 14f, borderPaint)

        // Downward Pointer
        val pointerPath = Path().apply {
            moveTo(centerX - 8f, 92f)
            lineTo(centerX + 8f, 92f)
            lineTo(centerX, 108f)
            close()
        }
        val pointerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentColor }
        canvas.drawPath(pointerPath, pointerPaint)

        // Header Tag: "JOIN RIDERS HERE"
        val headerRect = RectF(14f, 12f, width - 14f, 32f)
        val headerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentColor }
        canvas.drawRoundRect(headerRect, 6f, 6f, headerBgPaint)

        val headerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("⭐ JOIN RIDERS HERE • $statusText", headerRect.centerX(), headerRect.centerY() + 3.5f, headerTextPaint)

        // Landmark Name
        val placePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#00E5FF")
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(cleanPlace, 14f, 52f, placePaint)

        // Two-Column Arrival & ETA Breakdown
        val userText = "You: ${String.format("%.1f", userDistKm)}km (${userEtaMins.toInt()}m)"
        val partnerText = "$cleanPartner: ${String.format("%.1f", partnerDistKm)}km (${partnerEtaMins.toInt()}m)"

        val detailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(userText, 14f, 74f, detailPaint)

        val partnerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF9100")
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText(partnerText, width - 14f, 74f, partnerTextPaint)

        return bitmap
    }

    private fun render3DMotorcycleMarker(
        name: String,
        speedKmh: Float,
        bearing: Float,
        isOnline: Boolean,
        type: RiderMarkerType
    ): Bitmap {
        val width = 160
        val height = 180
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val centerX = width / 2f
        val centerY = 115f

        val (primaryColor, accentColor) = when (type) {
            RiderMarkerType.CURRENT_USER -> Color.parseColor("#00E5FF") to Color.parseColor("#0091EA")
            RiderMarkerType.PARTNER_ORANGE -> Color.parseColor("#FF6D00") to Color.parseColor("#FFAB00")
            RiderMarkerType.PARTNER_GREEN -> Color.parseColor("#00E676") to Color.parseColor("#00B0FF")
            RiderMarkerType.PARTNER_PURPLE -> Color.parseColor("#C51162") to Color.parseColor("#7C4DFF")
        }

        // ==========================================
        // 1. ROTATED 3D MOTORCYCLE MODEL & RIDER
        // ==========================================
        canvas.save()
        canvas.rotate(bearing, centerX, centerY)

        // Ground Drop Shadow (Oval for 3D elevation)
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#60000000")
            maskFilter = BlurMaskFilter(6f, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawOval(RectF(centerX - 20f, centerY - 38f, centerX + 20f, centerY + 42f), shadowPaint)

        // Headlight Beam Cone (forward projection)
        val beamPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                centerX, centerY - 35f,
                centerX, centerY - 78f,
                Color.argb(120, Color.red(primaryColor), Color.green(primaryColor), Color.blue(primaryColor)),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
        }
        val beamPath = Path().apply {
            moveTo(centerX - 6f, centerY - 35f)
            lineTo(centerX - 24f, centerY - 75f)
            lineTo(centerX + 24f, centerY - 75f)
            lineTo(centerX + 6f, centerY - 35f)
            close()
        }
        canvas.drawPath(beamPath, beamPaint)

        // Front Tire
        val tirePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1C1917") }
        canvas.drawRoundRect(RectF(centerX - 4.5f, centerY - 38f, centerX + 4.5f, centerY - 14f), 3f, 3f, tirePaint)

        // Rear Tire
        canvas.drawRoundRect(RectF(centerX - 5.5f, centerY + 14f, centerX + 5.5f, centerY + 38f), 3f, 3f, tirePaint)

        // Motorcycle Frame & Dual Exhaust
        val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#334155") }
        canvas.drawRoundRect(RectF(centerX - 8f, centerY - 16f, centerX + 8f, centerY + 18f), 4f, 4f, framePaint)

        val exhaustPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#CBD5E1") }
        canvas.drawRoundRect(RectF(centerX + 6.5f, centerY - 4f, centerX + 9.5f, centerY + 26f), 2f, 2f, exhaustPaint)

        // Fuel Tank & Aerodynamic Fairings (Primary vehicle body with gradient)
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                centerX - 10f, centerY,
                centerX + 10f, centerY,
                primaryColor,
                accentColor,
                Shader.TileMode.CLAMP
            )
        }
        val bodyPath = Path().apply {
            moveTo(centerX, centerY - 28f)
            lineTo(centerX + 9f, centerY - 10f)
            lineTo(centerX + 7f, centerY + 12f)
            lineTo(centerX, centerY + 16f)
            lineTo(centerX - 7f, centerY + 12f)
            lineTo(centerX - 9f, centerY - 10f)
            close()
        }
        canvas.drawPath(bodyPath, bodyPaint)

        // Racing Stripes on Tank
        val stripePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        canvas.drawRect(RectF(centerX - 1f, centerY - 26f, centerX + 1f, centerY + 12f), stripePaint)

        // Handlebars & Mirrors
        val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            strokeWidth = 3f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(centerX - 18f, centerY - 20f, centerX + 18f, centerY - 20f, handlePaint)

        // Mirrors
        val mirrorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = primaryColor }
        canvas.drawCircle(centerX - 18f, centerY - 22f, 2.5f, mirrorPaint)
        canvas.drawCircle(centerX + 18f, centerY - 22f, 2.5f, mirrorPaint)

        // Rider Torso & Riding Jacket
        val riderJacketPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#0F172A") }
        canvas.drawOval(RectF(centerX - 8f, centerY - 8f, centerX + 8f, centerY + 10f), riderJacketPaint)

        // Helmet with 3D Visor
        val helmetPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = primaryColor }
        canvas.drawCircle(centerX, centerY - 4f, 6.5f, helmetPaint)

        val visorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#09090B") }
        canvas.drawArc(RectF(centerX - 5.5f, centerY - 9.5f, centerX + 5.5f, centerY + 1.5f), 180f, 180f, true, visorPaint)

        canvas.restore()

        // ==========================================
        // 2. UPRIGHT FLOATING RIDER HUD BADGE
        // ==========================================
        val badgeWidth = 138f
        val badgeHeight = 36f
        val badgeLeft = (width - badgeWidth) / 2f
        val badgeTop = 8f
        val badgeRect = RectF(badgeLeft, badgeTop, badgeLeft + badgeWidth, badgeTop + badgeHeight)

        // Badge Background with border
        val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F00A1128") }
        canvas.drawRoundRect(badgeRect, 10f, 10f, badgeBgPaint)

        val badgeBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryColor
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRoundRect(badgeRect, 10f, 10f, badgeBorderPaint)

        // Small downward anchor pointer
        val pointerPath = Path().apply {
            moveTo(centerX - 6f, badgeTop + badgeHeight)
            lineTo(centerX + 6f, badgeTop + badgeHeight)
            lineTo(centerX, badgeTop + badgeHeight + 6f)
            close()
        }
        val pointerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = primaryColor }
        canvas.drawPath(pointerPath, pointerPaint)

        // Online / Offline Status Dot
        val statusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isOnline) Color.parseColor("#00E676") else Color.parseColor("#EF4444")
        }
        canvas.drawCircle(badgeLeft + 10f, badgeTop + (badgeHeight / 2f), 3.5f, statusPaint)

        // Rider Name Text
        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(name, badgeLeft + 18f, badgeTop + (badgeHeight / 2f) + 4f, namePaint)

        // Speed Badge Pill on the Right
        val speedText = "${speedKmh.toInt()} km/h"
        val speedBgRect = RectF(badgeLeft + badgeWidth - 54f, badgeTop + 6f, badgeLeft + badgeWidth - 6f, badgeTop + badgeHeight - 6f)
        val speedBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = primaryColor }
        canvas.drawRoundRect(speedBgRect, 6f, 6f, speedBgPaint)

        val speedTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0A1128")
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(speedText, speedBgRect.centerX(), speedBgRect.centerY() + 3.5f, speedTextPaint)

        return bitmap
    }

    private fun renderLocationPin(
        pinColor: Int,
        label: String,
        subLabel: String
    ): Bitmap {
        val width = 140
        val height = 75
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val centerX = width / 2f
        val cleanSubLabel = if (subLabel.length > 12) subLabel.take(11) + "…" else subLabel

        // Floating Card
        val cardRect = RectF(6f, 4f, width - 6f, 48f)
        val cardBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F50F172A") }
        canvas.drawRoundRect(cardRect, 10f, 10f, cardBgPaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = pinColor
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
        }
        canvas.drawRoundRect(cardRect, 10f, 10f, borderPaint)

        // Pin Pointer Triangle
        val pointerPath = Path().apply {
            moveTo(centerX - 7f, 48f)
            lineTo(centerX + 7f, 48f)
            lineTo(centerX, 62f)
            close()
        }
        val pointerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = pinColor }
        canvas.drawPath(pointerPath, pointerPaint)

        // Label Pill (e.g. START / DEST)
        val pillRect = RectF(12f, 10f, 48f, 26f)
        val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = pinColor }
        canvas.drawRoundRect(pillRect, 4f, 4f, pillPaint)

        val pillTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(label, pillRect.centerX(), pillRect.centerY() + 3f, pillTextPaint)

        // Sublabel (City Name)
        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 11.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(cleanSubLabel, 54f, 22f, namePaint)

        // Underline detail
        val detailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8")
            textSize = 8.5f
        }
        canvas.drawText("RouteMate Waypoint", 14f, 40f, detailPaint)

        return bitmap
    }
}
