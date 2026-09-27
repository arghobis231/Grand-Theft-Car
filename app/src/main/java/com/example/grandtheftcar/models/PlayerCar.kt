package com.example.grandtheftcar.models

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

class PlayerCar {
    var x: Float = 0f
    var y: Float = 0f
    var targetX: Float = 0f
    var targetLane: Int = 2

    var width: Float = 60f
    var height: Float = 115f

    // ==========================================
    // STAGE-DRIVEN REAL SPEED VARIABLES (KM/H)
    // ==========================================
    var stageStartSpeedKmH: Float = 95f
    var stageMaxSpeedKmH: Float = 155f
    var currentCruisingSpeedKmH: Float = 95f
    var speedKmH: Float = 95f

    var isBoosting: Boolean = false
    var isBraking: Boolean = false
    var isWrecked: Boolean = false

    // Nitro system
    var nitroAmount: Float = 1.0f // 0.0 to 1.0
    val maxNitro: Float = 1.0f

    // Shield protection
    var hasShield: Boolean = false
    private var shieldAngle: Float = 0f
    private var shieldPulseTimer: Float = 0f

    private var steeringTiltAngle: Float = 0f
    private var engineVibrationOffset: Float = 0f
    private var vibrationTimer: Float = 0f

    // Exhaust & boost flame particles
    class Particle(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        var size: Float,
        var alpha: Float,
        var color: Int
    )

    private val particles = mutableListOf<Particle>()
    private val particlePool = ArrayDeque<Particle>()

    // Paints
    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val stripePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#FFFFFF")
    }
    private val glassPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#0F172A")
    }
    private val lightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(120, 0, 0, 0)
    }
    private val wheelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#1E293B")
    }
    private val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val shieldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3.5f
    }
    private val shieldAuraPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    val bounds = RectF()

    fun reset(startX: Float, startY: Float, carWidth: Float) {
        x = startX
        y = startY
        targetX = startX
        targetLane = 2
        width = carWidth
        height = carWidth * 1.85f
        speedKmH = stageStartSpeedKmH
        currentCruisingSpeedKmH = stageStartSpeedKmH
        isBoosting = false
        isBraking = false
        isWrecked = false
        nitroAmount = 1.0f
        hasShield = false
        steeringTiltAngle = 0f
        particles.clear()
        updateBounds()
    }

    fun configureStageSpeeds(startSpeed: Float, maxSpeed: Float) {
        stageStartSpeedKmH = startSpeed
        stageMaxSpeedKmH = maxSpeed
        currentCruisingSpeedKmH = startSpeed
    }

    fun update(
        dt: Float,
        roadLeft: Float,
        roadRight: Float,
        stageProgressFraction: Float
    ) {
        // Engine rumble vibration increases with actual physical speed
        vibrationTimer += dt * (30f + speedKmH * 0.15f)
        engineVibrationOffset = if (isWrecked) 0f else sin(vibrationTimer) * (if (isBoosting) 2.2f else 1.0f)

        if (!isWrecked) {
            // REQ 2: GRADUAL ACCELERATION AS PLAYER PROGRESSES THROUGH STAGE
            // Cruising speed ramps up smoothly from stageStartSpeedKmH to stageMaxSpeedKmH
            currentCruisingSpeedKmH = stageStartSpeedKmH + (stageMaxSpeedKmH - stageStartSpeedKmH) * stageProgressFraction.coerceIn(0f, 1f)

            // Nitro management
            if (isBoosting) {
                if (nitroAmount > 0f) {
                    nitroAmount = (nitroAmount - dt * 0.22f).coerceAtLeast(0f)
                } else {
                    isBoosting = false
                }
            } else {
                // Slow passive nitro regeneration
                nitroAmount = (nitroAmount + dt * 0.04f).coerceAtMost(maxNitro)
            }

            // Target physical speed based on player action
            val targetSpeed = when {
                isBoosting -> currentCruisingSpeedKmH + 60f
                isBraking -> currentCruisingSpeedKmH * 0.70f
                else -> currentCruisingSpeedKmH
            }

            // Responsive acceleration towards target physical speed
            val accelRate = if (isBoosting) 5.5f else 3.5f
            speedKmH += (targetSpeed - speedKmH) * (dt * accelRate)

            // Snappy steering interpolation for responsive emergency swerving
            val dx = targetX - x
            x += dx * (dt * 20f).coerceAtMost(1f)

            // Dynamic roll tilt angle during steering
            val targetTilt = (dx / (width * 1.2f)).coerceIn(-15f, 15f)
            steeringTiltAngle += (targetTilt - steeringTiltAngle) * (dt * 18f)

            // Clamp car safely within road shoulders
            val halfW = width * 0.5f
            val minX = roadLeft + halfW + 6f
            val maxX = roadRight - halfW - 6f
            x = x.coerceIn(minX, maxX)
            targetX = targetX.coerceIn(minX, maxX)

            // Exhaust particles
            spawnExhaustParticles(dt)

            // Shield rotation
            if (hasShield) {
                shieldAngle = (shieldAngle + dt * 140f) % 360f
                shieldPulseTimer += dt * 6.0f
            }
        } else {
            steeringTiltAngle *= (1f - dt * 5f)
        }

        updateParticles(dt)
        updateBounds()
    }

    fun addNitro(amount: Float) {
        nitroAmount = (nitroAmount + amount).coerceAtMost(maxNitro)
        isBoosting = true
    }

    private fun spawnExhaustParticles(dt: Float) {
        val spawnCount = if (isBoosting) 3 else 1
        for (i in 0 until spawnCount) {
            val p = if (particlePool.isNotEmpty()) particlePool.removeFirst()
            else Particle(0f, 0f, 0f, 0f, 0f, 0f, 0)

            val leftExhaust = (i % 2 == 0)
            p.x = x + (if (leftExhaust) -width * 0.28f else width * 0.28f) + (Random.nextFloat() - 0.5f) * 4f
            p.y = y + height * 0.48f
            p.vx = (Random.nextFloat() - 0.5f) * 20f
            p.vy = 120f + speedKmH * 3.5f + Random.nextFloat() * 50f
            p.size = if (isBoosting) 8f + Random.nextFloat() * 8f else 4f + Random.nextFloat() * 4f
            p.alpha = 0.85f
            p.color = if (isBoosting) {
                if (Random.nextBoolean()) Color.parseColor("#38BDF8") else Color.parseColor("#60A5FA")
            } else {
                Color.argb(160, 200, 200, 200)
            }
            particles.add(p)
        }
    }

    private fun updateParticles(dt: Float) {
        val iter = particles.iterator()
        while (iter.hasNext()) {
            val p = iter.next()
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.size += dt * 10f
            p.alpha -= dt * 3.0f
            if (p.alpha <= 0f) {
                iter.remove()
                if (particlePool.size < 60) particlePool.add(p)
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
        // Draw exhaust & nitro flames
        for (p in particles) {
            particlePaint.color = p.color
            particlePaint.alpha = (p.alpha.coerceIn(0f, 1f) * 255).toInt()
            canvas.drawCircle(p.x, p.y, p.size, particlePaint)
        }

        canvas.save()
        val drawY = y + engineVibrationOffset
        canvas.translate(x, drawY)
        canvas.rotate(steeringTiltAngle)

        val halfW = width * 0.5f
        val halfH = height * 0.5f

        // Ground shadow
        shadowPaint.color = Color.argb(120, 0, 0, 0)
        canvas.drawRoundRect(-halfW + 4f, -halfH + 6f, halfW + 4f, halfH + 6f, 14f, 14f, shadowPaint)

        // Tires
        val tireW = 9f
        val tireH = 22f
        canvas.drawRoundRect(-halfW - 2f, -halfH + 18f, -halfW + tireW, -halfH + 18f + tireH, 4f, 4f, wheelPaint)
        canvas.drawRoundRect(halfW - tireW, -halfH + 18f, halfW + 2f, -halfH + 18f + tireH, 4f, 4f, wheelPaint)
        canvas.drawRoundRect(-halfW - 2f, halfH - 36f, -halfW + tireW, halfH - 36f + tireH, 4f, 4f, wheelPaint)
        canvas.drawRoundRect(halfW - tireW, halfH - 36f, halfW + 2f, halfH - 36f + tireH, 4f, 4f, wheelPaint)

        // Car Body (Retro Crimson Sports Car)
        bodyPaint.color = Color.parseColor("#DC2626")
        canvas.drawRoundRect(-halfW, -halfH, halfW, halfH, 14f, 14f, bodyPaint)

        // Dual White Racing Stripes
        canvas.drawRect(-halfW * 0.24f, -halfH, -halfW * 0.08f, halfH, stripePaint)
        canvas.drawRect(halfW * 0.08f, -halfH, halfW * 0.24f, halfH, stripePaint)

        // Cabin / Roof
        val roofPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#991B1B")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(-halfW * 0.76f, -halfH * 0.35f, halfW * 0.76f, halfH * 0.40f, 10f, 10f, roofPaint)

        // Front Windshield (aimed upward as car drives "forward" relative to screen)
        val frontWindshield = Path().apply {
            moveTo(-halfW * 0.70f, -halfH * 0.35f)
            lineTo(halfW * 0.70f, -halfH * 0.35f)
            lineTo(halfW * 0.60f, -halfH * 0.60f)
            lineTo(-halfW * 0.60f, -halfH * 0.60f)
            close()
        }
        canvas.drawPath(frontWindshield, glassPaint)

        // Rear window
        canvas.drawRoundRect(-halfW * 0.62f, halfH * 0.40f, halfW * 0.62f, halfH * 0.58f, 5f, 5f, glassPaint)

        // Side windows
        canvas.drawRect(-halfW * 0.76f, -halfH * 0.30f, -halfW * 0.68f, halfH * 0.35f, glassPaint)
        canvas.drawRect(halfW * 0.68f, -halfH * 0.30f, halfW * 0.76f, halfH * 0.35f, glassPaint)

        // Headlights (aiming forward/upward)
        lightPaint.color = Color.parseColor("#FEF08A")
        canvas.drawRoundRect(-halfW * 0.82f, -halfH, -halfW * 0.48f, -halfH + 8f, 3f, 3f, lightPaint)
        canvas.drawRoundRect(halfW * 0.48f, -halfH, halfW * 0.82f, -halfH + 8f, 3f, 3f, lightPaint)

        // Tail lights
        val tailLightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isBraking) Color.parseColor("#EF4444") else Color.parseColor("#7F1D1D")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(-halfW * 0.82f, halfH - 6f, -halfW * 0.48f, halfH, 2f, 2f, tailLightPaint)
        canvas.drawRoundRect(halfW * 0.48f, halfH - 6f, halfW * 0.82f, halfH, 2f, 2f, tailLightPaint)

        // Aerodynamic rear spoiler
        val spoilerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E293B")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(-halfW * 0.92f, halfH - 4f, halfW * 0.92f, halfH + 6f, 3f, 3f, spoilerPaint)

        // Shield barrier animation if active
        if (hasShield) {
            drawShieldBarrier(canvas, halfW, halfH)
        }

        canvas.restore()
    }

    private fun drawShieldBarrier(canvas: Canvas, halfW: Float, halfH: Float) {
        val shieldRadius = height * 0.65f
        val pulse = sin(shieldPulseTimer) * 0.15f + 0.85f

        shieldAuraPaint.color = Color.argb((45 * pulse).toInt(), 59, 130, 246)
        canvas.drawCircle(0f, 0f, shieldRadius * pulse, shieldAuraPaint)

        shieldPaint.color = Color.parseColor("#60A5FA")
        shieldPaint.alpha = (220 * pulse).toInt()
        canvas.drawCircle(0f, 0f, shieldRadius * pulse, shieldPaint)

        // Hexagonal energy points around circle
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#93C5FD")
            style = Paint.Style.FILL
        }
        for (i in 0..5) {
            val angle = Math.toRadians((shieldAngle + i * 60.0))
            val hx = (kotlin.math.cos(angle) * shieldRadius * pulse).toFloat()
            val hy = (kotlin.math.sin(angle) * shieldRadius * pulse).toFloat()
            canvas.drawCircle(hx, hy, 4.5f, dotPaint)
        }
    }
}
