package com.example.grandtheftcar.managers

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.example.grandtheftcar.models.PlayerCar
import com.example.grandtheftcar.models.TrafficVehicle
import kotlin.math.abs
import kotlin.random.Random

class CollisionManager {

    class ExplosionParticle(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        var size: Float,
        var life: Float,
        var maxLife: Float,
        var color: Int
    )

    class NearMissPopup(
        var x: Float,
        var y: Float,
        var text: String,
        var alpha: Float,
        var vy: Float
    )

    private val particles = mutableListOf<ExplosionParticle>()
    private val popups = mutableListOf<NearMissPopup>()
    private val processedNearMissVehicles = mutableSetOf<Int>()

    // Invulnerability grace period after shield break
    private var shieldGraceTimer: Float = 0f

    private val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val popupPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
        color = Color.parseColor("#FBBF24") // Gold
    }

    fun reset() {
        particles.clear()
        popups.clear()
        processedNearMissVehicles.clear()
        shieldGraceTimer = 0f
    }

    fun checkCollisions(
        player: PlayerCar,
        vehicles: List<TrafficVehicle>,
        onCollision: (TrafficVehicle) -> Unit,
        onNearMiss: (Int) -> Unit,
        onShieldAbsorbed: (TrafficVehicle) -> Unit
    ) {
        if (player.isWrecked) return

        val pBounds = player.bounds

        for (v in vehicles) {
            if (!v.active) continue

            val vBounds = v.bounds

            // Check AABB overlap
            if (RectF.intersects(pBounds, vBounds)) {
                if (shieldGraceTimer > 0f) {
                    // In invulnerability grace window
                    continue
                }

                if (player.hasShield) {
                    // Shield absorbs the collision!
                    player.hasShield = false
                    shieldGraceTimer = 1.2f
                    spawnShieldBurst(player.x, player.y)
                    onShieldAbsorbed(v)
                    return
                }

                // Lethal collision
                player.isWrecked = true
                spawnExplosion(
                    (player.x + v.x) * 0.5f,
                    (player.y + v.y) * 0.5f
                )
                onCollision(v)
                return
            }

            // Near miss detection (passing alongside with < 22px distance)
            val vehicleId = System.identityHashCode(v)
            if (!processedNearMissVehicles.contains(vehicleId)) {
                val dx = abs(player.x - v.x) - (player.width + v.width) * 0.5f
                val dy = abs(player.y - v.y)
                if (dx in 0f..22f && dy < (player.height + v.height) * 0.4f) {
                    processedNearMissVehicles.add(vehicleId)

                    // REQ 3: Near-miss rewards scale with actual speed!
                    val speedScale = (player.speedKmH / 100f).coerceIn(1.0f, 2.6f)
                    val bonus = (250f * speedScale).toInt()

                    onNearMiss(bonus)
                    spawnNearMissPopup(player.x, player.y - player.height * 0.6f, "+$bonus NEAR MISS!")
                }
            }
        }
    }

    fun spawnExplosion(centerX: Float, centerY: Float) {
        val colors = intArrayOf(
            Color.parseColor("#EF4444"), // Red
            Color.parseColor("#F97316"), // Orange
            Color.parseColor("#FACC15"), // Yellow
            Color.parseColor("#FFFFFF"), // Spark white
            Color.parseColor("#475569")  // Dark smoke
        )

        for (i in 0 until 45) {
            val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
            val speed = 80f + Random.nextFloat() * 260f
            val life = 0.5f + Random.nextFloat() * 0.7f
            particles.add(
                ExplosionParticle(
                    x = centerX + (Random.nextFloat() - 0.5f) * 16f,
                    y = centerY + (Random.nextFloat() - 0.5f) * 16f,
                    vx = kotlin.math.cos(angle) * speed,
                    vy = kotlin.math.sin(angle) * speed,
                    size = 5f + Random.nextFloat() * 12f,
                    life = life,
                    maxLife = life,
                    color = colors[Random.nextInt(colors.size)]
                )
            )
        }
    }

    fun spawnShieldBurst(centerX: Float, centerY: Float) {
        val colors = intArrayOf(
            Color.parseColor("#38BDF8"), // Sky blue
            Color.parseColor("#60A5FA"), // Blue
            Color.parseColor("#FFFFFF"), // White spark
            Color.parseColor("#93C5FD")  // Light blue
        )

        for (i in 0 until 35) {
            val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
            val speed = 100f + Random.nextFloat() * 220f
            val life = 0.4f + Random.nextFloat() * 0.5f
            particles.add(
                ExplosionParticle(
                    x = centerX,
                    y = centerY,
                    vx = kotlin.math.cos(angle) * speed,
                    vy = kotlin.math.sin(angle) * speed,
                    size = 4f + Random.nextFloat() * 8f,
                    life = life,
                    maxLife = life,
                    color = colors[Random.nextInt(colors.size)]
                )
            )
        }
    }

    private fun spawnNearMissPopup(x: Float, y: Float, text: String) {
        popups.add(
            NearMissPopup(
                x = x,
                y = y,
                text = text,
                alpha = 1.0f,
                vy = -60f
            )
        )
    }

    fun update(dt: Float) {
        if (shieldGraceTimer > 0f) {
            shieldGraceTimer -= dt
        }

        // Update explosion particles
        val pIter = particles.iterator()
        while (pIter.hasNext()) {
            val p = pIter.next()
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.life -= dt
            p.size *= (1f - dt * 1.5f)
            if (p.life <= 0f || p.size <= 0.5f) {
                pIter.remove()
            }
        }

        // Update score popups
        val popIter = popups.iterator()
        while (popIter.hasNext()) {
            val popup = popIter.next()
            popup.y += popup.vy * dt
            popup.alpha -= dt * 1.4f
            if (popup.alpha <= 0f) {
                popIter.remove()
            }
        }
    }

    fun draw(canvas: Canvas) {
        // Draw explosion / deflection particles
        for (p in particles) {
            val alpha = (p.life / p.maxLife).coerceIn(0f, 1f)
            particlePaint.color = p.color
            particlePaint.alpha = (alpha * 255).toInt()
            canvas.drawCircle(p.x, p.y, p.size, particlePaint)
        }

        // Draw near-miss floating bonus labels
        for (popup in popups) {
            popupPaint.alpha = (popup.alpha.coerceIn(0f, 1f) * 255).toInt()
            popupPaint.textSize = 20f

            // Shadow
            canvas.drawText(popup.text, popup.x + 2f, popup.y + 2f, Paint().apply {
                color = Color.BLACK
                alpha = popupPaint.alpha
                textAlign = Paint.Align.CENTER
                textSize = 20f
                isFakeBoldText = true
            })

            canvas.drawText(popup.text, popup.x, popup.y, popupPaint)
        }
    }
}
