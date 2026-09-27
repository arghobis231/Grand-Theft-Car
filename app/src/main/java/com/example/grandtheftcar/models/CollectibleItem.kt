package com.example.grandtheftcar.models

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import kotlin.math.sin
import kotlin.random.Random

enum class CollectibleType(
    val displayName: String,
    val bonusScore: Int,
    val primaryColor: Int,
    val secondaryColor: Int
) {
    NITRO(
        displayName = "NITRO BOOST",
        bonusScore = 300,
        primaryColor = Color.parseColor("#06B6D4"), // Cyan
        secondaryColor = Color.parseColor("#38BDF8")
    ),
    CASH(
        displayName = "CASH STACK",
        bonusScore = 750,
        primaryColor = Color.parseColor("#10B981"), // Emerald
        secondaryColor = Color.parseColor("#FDE047")
    ),
    SHIELD(
        displayName = "SHIELD PROTECTION",
        bonusScore = 400,
        primaryColor = Color.parseColor("#3B82F6"), // Royal Blue
        secondaryColor = Color.parseColor("#93C5FD")
    ),
    MULTIPLIER(
        displayName = "2X COMBO",
        bonusScore = 500,
        primaryColor = Color.parseColor("#F59E0B"), // Amber Gold
        secondaryColor = Color.parseColor("#FEF08A")
    )
}

class CollectibleItem {
    var active: Boolean = false
    var lane: Int = 0
    var x: Float = 0f
    var y: Float = 0f
    var width: Float = 42f
    var height: Float = 42f
    var type: CollectibleType = CollectibleType.CASH

    val bounds = RectF()

    private var animTimer: Float = 0f
    private val auraPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    fun spawn(spawnLane: Int, posX: Float, posY: Float, itemType: CollectibleType) {
        active = true
        lane = spawnLane
        x = posX
        y = posY
        type = itemType
        width = 46f
        height = 46f
        animTimer = Random.nextFloat() * 10f
        updateBounds()
    }

    fun update(dt: Float, speedKmH: Float, pixelsPerMeter: Float) {
        if (!active) return

        animTimer += dt

        // Relative speed moves down the screen with road speed
        val speedMps = speedKmH * (1000f / 3600f)
        val dy = speedMps * pixelsPerMeter * dt
        y += dy

        updateBounds()
    }

    private fun updateBounds() {
        val halfW = width * 0.5f
        val halfH = height * 0.5f
        bounds.set(x - halfW, y - halfH, x + halfW, y + halfH)
    }

    fun draw(canvas: Canvas) {
        if (!active) return

        canvas.save()
        val bobOffset = sin(animTimer * 6.0f) * 4f
        canvas.translate(x, y + bobOffset)

        val pulse = (sin(animTimer * 8.0f) * 0.25f + 0.75f)

        // 1. Pulsing glowing aura
        auraPaint.color = type.primaryColor
        auraPaint.alpha = (pulse * 80).toInt().coerceIn(0, 255)
        canvas.drawCircle(0f, 0f, 28f * pulse, auraPaint)

        // 2. Diamond / Hexagon token base
        bodyPaint.color = Color.parseColor("#0F172A")
        canvas.drawCircle(0f, 0f, 20f, bodyPaint)

        borderPaint.color = type.secondaryColor
        borderPaint.strokeWidth = 3.5f
        canvas.drawCircle(0f, 0f, 20f, borderPaint)

        // 3. Icon artwork
        when (type) {
            CollectibleType.NITRO -> drawNitroIcon(canvas)
            CollectibleType.CASH -> drawCashIcon(canvas)
            CollectibleType.SHIELD -> drawShieldIcon(canvas)
            CollectibleType.MULTIPLIER -> drawMultiplierIcon(canvas)
        }

        canvas.restore()
    }

    private fun drawNitroIcon(canvas: Canvas) {
        // Cyan lightning bolt / N2O bottle
        val p = Path().apply {
            moveTo(-2f, -12f)
            lineTo(8f, -2f)
            lineTo(1f, -1f)
            lineTo(4f, 12f)
            lineTo(-8f, 2f)
            lineTo(-1f, 1f)
            close()
        }
        bodyPaint.color = Color.parseColor("#38BDF8")
        canvas.drawPath(p, bodyPaint)
    }

    private fun drawCashIcon(canvas: Canvas) {
        // Gold dollar sign
        iconPaint.color = Color.parseColor("#FBBF24")
        iconPaint.textSize = 22f
        canvas.drawText("$", 0f, 8f, iconPaint)
    }

    private fun drawShieldIcon(canvas: Canvas) {
        // Blue crest shield
        val p = Path().apply {
            moveTo(0f, -11f)
            lineTo(11f, -7f)
            lineTo(9f, 5f)
            lineTo(0f, 13f)
            lineTo(-9f, 5f)
            lineTo(-11f, -7f)
            close()
        }
        bodyPaint.color = Color.parseColor("#60A5FA")
        canvas.drawPath(p, bodyPaint)
    }

    private fun drawMultiplierIcon(canvas: Canvas) {
        iconPaint.color = Color.parseColor("#F59E0B")
        iconPaint.textSize = 15f
        canvas.drawText("2X", 0f, 6f, iconPaint)
    }
}
