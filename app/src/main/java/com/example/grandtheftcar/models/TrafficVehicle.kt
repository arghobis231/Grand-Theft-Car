package com.example.grandtheftcar.models

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

class TrafficVehicle {
    var active: Boolean = false
    var lane: Int = 0
    var x: Float = 0f
    var y: Float = 0f
    var width: Float = 60f
    var height: Float = 110f
    var speedKmH: Float = 90f
    var type: VehicleType = VehicleType.SEDAN
    var color: Int = Color.WHITE

    val bounds = RectF()

    // Autonomous behavior: Lane changes & turn signals
    private var canChangeLane: Boolean = false
    var isSignalingLeft: Boolean = false
    var isSignalingRight: Boolean = false
    private var signalTimer: Float = 0f
    private var isChangingLane: Boolean = false
    private var laneChangeStartX: Float = 0f
    private var laneChangeTargetX: Float = 0f
    private var laneChangeProgress: Float = 0f
    private var targetLaneIndex: Int = 0

    // Emergency lights strobe animation
    private var lightbarTimer: Float = 0f

    // Common Paints
    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val roofPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val glassPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#0F172A")
    }
    private val lightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(110, 0, 0, 0)
    }
    private val trailerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val detailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    fun spawn(
        spawnLane: Int,
        laneCenterX: Float,
        spawnY: Float,
        laneWidth: Float,
        vehicleType: VehicleType,
        baseSpeed: Float
    ) {
        active = true
        lane = spawnLane
        x = laneCenterX
        y = spawnY
        type = vehicleType

        width = laneWidth * vehicleType.widthFactor
        height = laneWidth * vehicleType.lengthFactor

        val variance = Random.nextFloat() * (vehicleType.speedVarianceMax - vehicleType.speedVarianceMin) + vehicleType.speedVarianceMin
        speedKmH = baseSpeed * variance

        val palette = vehicleType.defaultColors
        color = palette[Random.nextInt(palette.size)]

        // Emergency vehicles do not wander across lanes randomly, but normal cars can (18% chance)
        canChangeLane = !type.isEmergency && (Random.nextFloat() < 0.20f)
        isSignalingLeft = false
        isSignalingRight = false
        signalTimer = 0f
        isChangingLane = false
        laneChangeProgress = 0f
        targetLaneIndex = spawnLane
        lightbarTimer = Random.nextFloat() * 10f

        updateBounds()
    }

    fun update(
        dt: Float,
        playerSpeedKmH: Float,
        pixelsPerMeter: Float,
        allVehicles: List<TrafficVehicle>? = null,
        roadLeft: Float = 0f,
        laneWidth: Float = 0f
    ) {
        if (!active) return

        lightbarTimer += dt

        // Relative speed = oncoming traffic speed + player speed
        val relativeSpeedMps = (speedKmH + playerSpeedKmH) * (1000f / 3600f)
        val dy = relativeSpeedMps * pixelsPerMeter * dt
        y += dy

        // AI Lane change behavior (safe, fair, and signaled!)
        if (canChangeLane && laneWidth > 0f) {
            updateLaneChangeBehavior(dt, allVehicles, roadLeft, laneWidth)
        }

        updateBounds()
    }

    private fun updateLaneChangeBehavior(
        dt: Float,
        allVehicles: List<TrafficVehicle>?,
        roadLeft: Float,
        laneWidth: Float
    ) {
        // Only trigger lane change when vehicle is in upper/mid screen (y in 50..450)
        if (!isSignalingLeft && !isSignalingRight && !isChangingLane) {
            if (y in 80f..400f && Random.nextFloat() < (dt * 0.15f)) {
                // Decide direction: left or right
                val canGoLeft = lane > 0
                val canGoRight = lane < 5
                val goLeft = when {
                    canGoLeft && canGoRight -> Random.nextBoolean()
                    canGoLeft -> true
                    canGoRight -> false
                    else -> false
                }

                val candLane = if (goLeft) lane - 1 else lane + 1
                val candCenterX = roadLeft + (candLane + 0.5f) * laneWidth

                // Check that target lane is currently open around this vehicle
                var isTargetSafe = true
                if (allVehicles != null) {
                    for (other in allVehicles) {
                        if (other.active && other !== this && other.lane == candLane) {
                            if (abs(other.y - y) < (height + other.height) * 1.2f) {
                                isTargetSafe = false
                                break
                            }
                        }
                    }
                }

                if (isTargetSafe) {
                    targetLaneIndex = candLane
                    laneChangeStartX = x
                    laneChangeTargetX = candCenterX
                    if (goLeft) isSignalingLeft = true else isSignalingRight = true
                    signalTimer = 0f
                }
            }
        } else if (isSignalingLeft || isSignalingRight) {
            signalTimer += dt
            // Signal for 1.1 seconds before executing lane change
            if (signalTimer >= 1.1f) {
                isChangingLane = true
                isSignalingLeft = false
                isSignalingRight = false
                laneChangeProgress = 0f
            }
        } else if (isChangingLane) {
            laneChangeProgress += dt * 1.1f // takes ~0.9s to merge smoothly
            val t = laneChangeProgress.coerceIn(0f, 1f)
            // Smooth ease in-out
            val smoothT = t * t * (3f - 2f * t)
            x = laneChangeStartX + (laneChangeTargetX - laneChangeStartX) * smoothT

            if (laneChangeProgress >= 1f) {
                lane = targetLaneIndex
                x = laneChangeTargetX
                isChangingLane = false
                canChangeLane = false // only 1 lane change per spawn
            }
        }
    }

    private fun updateBounds() {
        val insetX = width * 0.10f
        val insetY = height * 0.08f
        bounds.set(
            x - width * 0.5f + insetX,
            y - height * 0.5f + insetY,
            x + width * 0.5f - insetX,
            y + height * 0.5f - insetY
        )
    }

    fun draw(canvas: Canvas) {
        if (!active) return

        canvas.save()
        canvas.translate(x, y)

        val halfW = width * 0.5f
        val halfH = height * 0.5f

        // Ground shadow
        canvas.drawRoundRect(
            -halfW + 4f, -halfH + 6f, halfW + 4f, halfH + 6f,
            12f, 12f, shadowPaint
        )

        when (type) {
            VehicleType.TRUCK -> drawTruck(canvas, halfW, halfH)
            VehicleType.VAN -> drawVan(canvas, halfW, halfH)
            VehicleType.SUV -> drawSuv(canvas, halfW, halfH)
            VehicleType.SPORTS_CAR -> drawSportsCar(canvas, halfW, halfH)
            VehicleType.SMALL_CAR -> drawSmallCar(canvas, halfW, halfH)
            VehicleType.SEDAN -> drawSedan(canvas, halfW, halfH)
            VehicleType.EMERGENCY_AMBULANCE -> drawAmbulance(canvas, halfW, halfH)
            VehicleType.EMERGENCY_POLICE -> drawPolice(canvas, halfW, halfH)
        }

        // Draw active turn signal blinks
        drawTurnSignals(canvas, halfW, halfH)

        canvas.restore()
    }

    private fun drawTurnSignals(canvas: Canvas, halfW: Float, halfH: Float) {
        // Blinks at 5 Hz
        val blink = (sin(lightbarTimer * 25.0) > 0.0)
        if (!blink) return

        lightPaint.color = Color.parseColor("#F59E0B") // Amber blinker

        if (isSignalingLeft) {
            // Front left & rear left
            canvas.drawCircle(-halfW + 3f, halfH - 4f, 5f, lightPaint)
            canvas.drawCircle(-halfW + 3f, -halfH + 4f, 4f, lightPaint)
        }
        if (isSignalingRight) {
            // Front right & rear right
            canvas.drawCircle(halfW - 3f, halfH - 4f, 5f, lightPaint)
            canvas.drawCircle(halfW - 3f, -halfH + 4f, 4f, lightPaint)
        }
    }

    private fun drawSedan(canvas: Canvas, halfW: Float, halfH: Float) {
        bodyPaint.color = color
        canvas.drawRoundRect(-halfW, -halfH, halfW, halfH, 12f, 12f, bodyPaint)

        // Sleek roof
        roofPaint.color = getDarkerColor(color, 0.85f)
        canvas.drawRoundRect(-halfW * 0.82f, -halfH * 0.55f, halfW * 0.82f, halfH * 0.45f, 8f, 8f, roofPaint)

        // Front Windshield (facing downwards towards player)
        val frontGlass = Path().apply {
            moveTo(-halfW * 0.75f, halfH * 0.45f)
            lineTo(halfW * 0.75f, halfH * 0.45f)
            lineTo(halfW * 0.65f, halfH * 0.75f)
            lineTo(-halfW * 0.65f, halfH * 0.75f)
            close()
        }
        canvas.drawPath(frontGlass, glassPaint)

        // Rear window
        canvas.drawRoundRect(-halfW * 0.70f, -halfH * 0.75f, halfW * 0.70f, -halfH * 0.55f, 4f, 4f, glassPaint)

        // Headlights
        drawHeadlights(canvas, halfW, halfH)
    }

    private fun drawSmallCar(canvas: Canvas, halfW: Float, halfH: Float) {
        bodyPaint.color = color
        canvas.drawRoundRect(-halfW, -halfH, halfW, halfH, 16f, 16f, bodyPaint)

        // Compact roof
        roofPaint.color = getDarkerColor(color, 0.88f)
        canvas.drawRoundRect(-halfW * 0.80f, -halfH * 0.50f, halfW * 0.80f, halfH * 0.38f, 10f, 10f, roofPaint)

        // Windshield
        val frontGlass = Path().apply {
            moveTo(-halfW * 0.72f, halfH * 0.38f)
            lineTo(halfW * 0.72f, halfH * 0.38f)
            lineTo(halfW * 0.60f, halfH * 0.70f)
            lineTo(-halfW * 0.60f, halfH * 0.70f)
            close()
        }
        canvas.drawPath(frontGlass, glassPaint)

        // Rear glass
        canvas.drawRoundRect(-halfW * 0.65f, -halfH * 0.70f, halfW * 0.65f, -halfH * 0.50f, 6f, 6f, glassPaint)

        drawHeadlights(canvas, halfW, halfH, isRound = true)
    }

    private fun drawSportsCar(canvas: Canvas, halfW: Float, halfH: Float) {
        bodyPaint.color = color
        canvas.drawRoundRect(-halfW, -halfH, halfW, halfH, 10f, 10f, bodyPaint)

        // Dual racing stripes
        val stripePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawRect(-halfW * 0.22f, -halfH, -halfW * 0.08f, halfH, stripePaint)
        canvas.drawRect(halfW * 0.08f, -halfH, halfW * 0.22f, halfH, stripePaint)

        // Aerodynamic cockpit
        roofPaint.color = Color.parseColor("#0F172A")
        canvas.drawRoundRect(-halfW * 0.72f, -halfH * 0.40f, halfW * 0.72f, halfH * 0.40f, 12f, 12f, roofPaint)

        // Rear Wing Spoiler
        val spoilerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(-halfW * 0.95f, -halfH - 2f, halfW * 0.95f, -halfH + 8f, 4f, 4f, spoilerPaint)

        drawHeadlights(canvas, halfW, halfH, isAggressive = true)
    }

    private fun drawSuv(canvas: Canvas, halfW: Float, halfH: Float) {
        bodyPaint.color = color
        canvas.drawRoundRect(-halfW, -halfH, halfW, halfH, 10f, 10f, bodyPaint)

        // Roof
        roofPaint.color = getDarkerColor(color, 0.85f)
        canvas.drawRoundRect(-halfW * 0.85f, -halfH * 0.70f, halfW * 0.85f, halfH * 0.45f, 6f, 6f, roofPaint)

        // Roof rack bars
        val rackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#334155")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawLine(-halfW * 0.70f, -halfH * 0.60f, -halfW * 0.70f, halfH * 0.35f, rackPaint)
        canvas.drawLine(halfW * 0.70f, -halfH * 0.60f, halfW * 0.70f, halfH * 0.35f, rackPaint)

        // Large front glass
        val frontGlass = Path().apply {
            moveTo(-halfW * 0.78f, halfH * 0.45f)
            lineTo(halfW * 0.78f, halfH * 0.45f)
            lineTo(halfW * 0.68f, halfH * 0.72f)
            lineTo(-halfW * 0.68f, halfH * 0.72f)
            close()
        }
        canvas.drawPath(frontGlass, glassPaint)

        drawHeadlights(canvas, halfW, halfH)
    }

    private fun drawVan(canvas: Canvas, halfW: Float, halfH: Float) {
        bodyPaint.color = color
        canvas.drawRoundRect(-halfW, -halfH, halfW, halfH, 8f, 8f, bodyPaint)

        // Cargo box ribs on roof
        detailPaint.color = getDarkerColor(color, 0.80f)
        val ribCount = 4
        val ribSpacing = (halfH * 1.1f) / ribCount
        for (i in 0 until ribCount) {
            val ry = -halfH * 0.65f + i * ribSpacing
            canvas.drawLine(-halfW * 0.75f, ry, halfW * 0.75f, ry, detailPaint)
        }

        // Driver cab windshield
        val frontGlass = Path().apply {
            moveTo(-halfW * 0.82f, halfH * 0.52f)
            lineTo(halfW * 0.82f, halfH * 0.52f)
            lineTo(halfW * 0.74f, halfH * 0.78f)
            lineTo(-halfW * 0.74f, halfH * 0.78f)
            close()
        }
        canvas.drawPath(frontGlass, glassPaint)

        // Rear cargo doors line
        detailPaint.color = Color.parseColor("#475569")
        canvas.drawLine(0f, -halfH, 0f, -halfH * 0.75f, detailPaint)

        drawHeadlights(canvas, halfW, halfH)
    }

    private fun drawTruck(canvas: Canvas, halfW: Float, halfH: Float) {
        val cabH = height * 0.30f
        val cabTop = halfH - cabH

        // 1. Trailer body (Top to cab)
        trailerPaint.color = Color.parseColor("#E2E8F0") // Silver/white freight trailer
        canvas.drawRoundRect(-halfW, -halfH, halfW, cabTop - 12f, 6f, 6f, trailerPaint)

        // Trailer roof ridges
        detailPaint.color = Color.parseColor("#CBD5E1")
        for (i in 0..7) {
            val ry = -halfH + 16f + i * 22f
            if (ry < cabTop - 20f) {
                canvas.drawLine(-halfW + 6f, ry, halfW - 6f, ry, detailPaint)
            }
        }

        // Red/White rear hazard stripes on trailer rear
        val barW = 10f
        var bx = -halfW + 4f
        var isRed = true
        while (bx < halfW - 4f) {
            lightPaint.color = if (isRed) Color.parseColor("#EF4444") else Color.WHITE
            canvas.drawRect(bx, -halfH, bx + barW, -halfH + 6f, lightPaint)
            bx += barW
            isRed = !isRed
        }

        // 2. Pivot hitch
        val hitchPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#334155")
            style = Paint.Style.FILL
        }
        canvas.drawRect(-halfW * 0.35f, cabTop - 12f, halfW * 0.35f, cabTop, hitchPaint)

        // 3. Tractor Cab
        bodyPaint.color = color
        canvas.drawRoundRect(-halfW * 0.95f, cabTop, halfW * 0.95f, halfH, 8f, 8f, bodyPaint)

        // Cab windshield
        val cabGlass = Path().apply {
            moveTo(-halfW * 0.80f, cabTop + 14f)
            lineTo(halfW * 0.80f, cabTop + 14f)
            lineTo(halfW * 0.72f, halfH - 10f)
            lineTo(-halfW * 0.72f, halfH - 10f)
            close()
        }
        canvas.drawPath(cabGlass, glassPaint)

        // Cab sun visor
        val visorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            style = Paint.Style.FILL
        }
        canvas.drawRect(-halfW * 0.85f, halfH - 12f, halfW * 0.85f, halfH - 5f, visorPaint)

        drawHeadlights(canvas, halfW, halfH)
    }

    private fun drawAmbulance(canvas: Canvas, halfW: Float, halfH: Float) {
        bodyPaint.color = Color.WHITE
        canvas.drawRoundRect(-halfW, -halfH, halfW, halfH, 10f, 10f, bodyPaint)

        // Orange/Red paramedic side stripes
        val stripePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#EA580C")
            style = Paint.Style.FILL
        }
        canvas.drawRect(-halfW, -halfH * 0.40f, -halfW + 6f, halfH * 0.40f, stripePaint)
        canvas.drawRect(halfW - 6f, -halfH * 0.40f, halfW, halfH * 0.40f, stripePaint)

        // Red cross on roof
        val crossPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#DC2626")
            style = Paint.Style.FILL
        }
        canvas.drawRect(-5f, -22f, 5f, 2f, crossPaint)
        canvas.drawRect(-17f, -14f, 17f, -6f, crossPaint)

        // Windshield
        val frontGlass = Path().apply {
            moveTo(-halfW * 0.80f, halfH * 0.48f)
            lineTo(halfW * 0.80f, halfH * 0.48f)
            lineTo(halfW * 0.70f, halfH * 0.76f)
            lineTo(-halfW * 0.70f, halfH * 0.76f)
            close()
        }
        canvas.drawPath(frontGlass, glassPaint)

        // Flashing Emergency Lightbar on cab roof
        drawEmergencyLightbar(canvas, halfW, halfH * 0.36f)

        drawHeadlights(canvas, halfW, halfH)
    }

    private fun drawPolice(canvas: Canvas, halfW: Float, halfH: Float) {
        // Black cruiser
        bodyPaint.color = Color.parseColor("#0F172A")
        canvas.drawRoundRect(-halfW, -halfH, halfW, halfH, 12f, 12f, bodyPaint)

        // White roof & doors
        val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(-halfW * 0.85f, -halfH * 0.45f, halfW * 0.85f, halfH * 0.35f, 6f, 6f, whitePaint)

        // Windshield
        val frontGlass = Path().apply {
            moveTo(-halfW * 0.78f, halfH * 0.38f)
            lineTo(halfW * 0.78f, halfH * 0.38f)
            lineTo(halfW * 0.68f, halfH * 0.72f)
            lineTo(-halfW * 0.68f, halfH * 0.72f)
            close()
        }
        canvas.drawPath(frontGlass, glassPaint)

        // "POLICE" text on hood
        textPaint.color = Color.WHITE
        textPaint.textSize = 10f
        canvas.drawText("POLICE", 0f, halfH * 0.88f, textPaint)

        // Flashing Emergency Lightbar on roof
        drawEmergencyLightbar(canvas, halfW, -halfH * 0.05f)

        drawHeadlights(canvas, halfW, halfH, isAggressive = true)
    }

    private fun drawEmergencyLightbar(canvas: Canvas, halfW: Float, barY: Float) {
        // Lightbar frame
        val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E293B")
            style = Paint.Style.FILL
        }
        val barW = halfW * 1.25f
        val barH = 12f
        canvas.drawRoundRect(-barW * 0.5f, barY - barH * 0.5f, barW * 0.5f, barY + barH * 0.5f, 4f, 4f, framePaint)

        // Rapid alternating red and blue strobes (10 Hz)
        val phase = (lightbarTimer * 20.0).toInt() % 2 == 0
        val redPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (phase) Color.parseColor("#EF4444") else Color.parseColor("#7F1D1D")
            style = Paint.Style.FILL
        }
        val bluePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (!phase) Color.parseColor("#3B82F6") else Color.parseColor("#1E3A8A")
            style = Paint.Style.FILL
        }

        // Left strobe (Red)
        canvas.drawRoundRect(-barW * 0.45f, barY - barH * 0.4f, -barW * 0.08f, barY + barH * 0.4f, 3f, 3f, redPaint)
        // Right strobe (Blue)
        canvas.drawRoundRect(barW * 0.08f, barY - barH * 0.4f, barW * 0.45f, barY + barH * 0.4f, 3f, 3f, bluePaint)

        // Center white flash
        val whiteFlash = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawCircle(0f, barY, 3f, whiteFlash)
    }

    private fun drawHeadlights(
        canvas: Canvas,
        halfW: Float,
        halfH: Float,
        isRound: Boolean = false,
        isAggressive: Boolean = false
    ) {
        lightPaint.color = Color.parseColor("#FEF08A") // Bright halogen yellow/white

        val lightW = if (isAggressive) 14f else 11f
        val lightH = if (isAggressive) 6f else 8f
        val edgeOffset = halfW * 0.22f

        if (isRound) {
            canvas.drawCircle(-halfW + edgeOffset, halfH - 4f, 5.5f, lightPaint)
            canvas.drawCircle(halfW - edgeOffset, halfH - 4f, 5.5f, lightPaint)
        } else {
            canvas.drawRoundRect(-halfW + edgeOffset - lightW * 0.5f, halfH - lightH, -halfW + edgeOffset + lightW * 0.5f, halfH, 3f, 3f, lightPaint)
            canvas.drawRoundRect(halfW - edgeOffset - lightW * 0.5f, halfH - lightH, halfW - edgeOffset + lightW * 0.5f, halfH, 3f, 3f, lightPaint)
        }

        // Tail lights (facing upwards away from player)
        val tailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#DC2626")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(-halfW + edgeOffset - 4f, -halfH, -halfW + edgeOffset + 4f, -halfH + 4f, 2f, 2f, tailPaint)
        canvas.drawRoundRect(halfW - edgeOffset - 4f, -halfH, halfW - edgeOffset + 4f, -halfH + 4f, 2f, 2f, tailPaint)
    }

    private fun getDarkerColor(color: Int, factor: Float): Int {
        val a = Color.alpha(color)
        val r = (Color.red(color) * factor).toInt()
        val g = (Color.green(color) * factor).toInt()
        val b = (Color.blue(color) * factor).toInt()
        return Color.argb(a, r, g, b)
    }
}
