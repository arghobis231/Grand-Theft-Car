package com.example.grandtheftcar.models

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import kotlin.math.sin

class PoliceCar {
    var active: Boolean = false
    var x: Float = 0f
    var y: Float = 0f
    var targetX: Float = 0f
    var targetY: Float = 0f

    var width: Float = 60f
    var height: Float = 115f

    private var strobeTimer: Float = 0f
    private var isRedStrobe: Boolean = false

    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#F8FAFC")
    }
    private val blackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#0F172A")
    }
    private val glassPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#1E293B")
    }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(120, 0, 0, 0)
    }
    private val sirenRedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val sirenBluePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val flashGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    fun startChase(playerX: Float, playerY: Float, carWidth: Float, screenHeight: Float) {
        active = true
        width = carWidth
        height = carWidth * 1.85f

        // Police car enters from bottom or side and pulls up right next to the player's car
        x = playerX + (if (playerX > 300f) -width * 1.3f else width * 1.3f)
        y = screenHeight + height
        targetX = playerX + (if (playerX > 300f) -width * 1.15f else width * 1.15f)
        targetY = playerY - height * 0.15f
        strobeTimer = 0f
    }

    fun update(dt: Float) {
        if (!active) return

        strobeTimer += dt * 14f
        isRedStrobe = (strobeTimer.toInt() % 2 == 0)

        // Interpolate smoothly toward target position beside player
        x += (targetX - x) * (dt * 3.5f)
        y += (targetY - y) * (dt * 3.2f)
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

        // Police Interceptor Black and White Body
        // Main black body
        canvas.drawRoundRect(-halfW, -halfH, halfW, halfH, 12f, 12f, blackPaint)

        // White doors and roof center section (Classic black & white cop cruiser)
        canvas.drawRect(-halfW, -halfH * 0.35f, halfW, halfH * 0.45f, whitePaint)

        // Front push bumper bar
        val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#334155")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(-halfW * 0.65f, -halfH - 4f, halfW * 0.65f, -halfH + 2f, 2f, 2f, barPaint)

        // Windshield (facing up)
        val glassPath = Path()
        glassPath.moveTo(-halfW * 0.7f, -halfH * 0.22f)
        glassPath.lineTo(halfW * 0.7f, -halfH * 0.22f)
        glassPath.lineTo(halfW * 0.55f, halfH * 0.25f)
        glassPath.lineTo(-halfW * 0.55f, halfH * 0.25f)
        glassPath.close()
        canvas.drawPath(glassPath, glassPaint)

        // Rear window
        canvas.drawRoundRect(-halfW * 0.65f, halfH * 0.5f, halfW * 0.65f, halfH * 0.75f, 4f, 4f, glassPaint)

        // Emergency Lightbar on Roof
        val barTop = -halfH * 0.05f
        val barBottom = halfH * 0.12f
        val lightbarFrame = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E293B")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(-halfW * 0.7f, barTop, halfW * 0.7f, barBottom, 3f, 3f, lightbarFrame)

        // Flashing Red & Blue Strobes
        sirenRedPaint.color = if (isRedStrobe) Color.parseColor("#EF4444") else Color.parseColor("#7F1D1D")
        sirenBluePaint.color = if (!isRedStrobe) Color.parseColor("#3B82F6") else Color.parseColor("#1E3A8A")

        canvas.drawRoundRect(-halfW * 0.65f, barTop + 1f, -2f, barBottom - 1f, 2f, 2f, sirenRedPaint)
        canvas.drawRoundRect(2f, barTop + 1f, halfW * 0.65f, barBottom - 1f, 2f, 2f, sirenBluePaint)

        // Big flashing halo light burst
        val activeHaloColor = if (isRedStrobe) Color.argb(90, 239, 68, 68) else Color.argb(90, 59, 130, 246)
        flashGlowPaint.color = activeHaloColor
        canvas.drawCircle(if (isRedStrobe) -halfW * 0.35f else halfW * 0.35f, (barTop + barBottom) * 0.5f, width * 1.2f, flashGlowPaint)

        // Headlights
        val lightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FEF08A")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(-halfW * 0.8f, -halfH - 2f, -halfW * 0.5f, -halfH + 4f, 2f, 2f, lightPaint)
        canvas.drawRoundRect(halfW * 0.5f, -halfH - 2f, halfW * 0.8f, -halfH + 4f, 2f, 2f, lightPaint)

        // "POLICE" text on roof
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = width * 0.16f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        canvas.drawText("POLICE", 0f, halfH * 0.40f, textPaint)

        canvas.restore()
    }

    fun reset() {
        active = false
    }
}
