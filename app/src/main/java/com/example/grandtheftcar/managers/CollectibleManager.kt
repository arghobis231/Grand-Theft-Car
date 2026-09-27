package com.example.grandtheftcar.managers

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.example.grandtheftcar.models.CollectibleItem
import com.example.grandtheftcar.models.CollectibleType
import com.example.grandtheftcar.models.PlayerCar
import com.example.grandtheftcar.models.Road
import kotlin.random.Random

class CollectibleManager {

    class PickupPopup(
        var x: Float,
        var y: Float,
        var text: String,
        var color: Int,
        var alpha: Float = 1.0f,
        var vy: Float = -70f
    )

    val activeItems = mutableListOf<CollectibleItem>()
    private val itemPool = ArrayDeque<CollectibleItem>()
    val activePopups = mutableListOf<PickupPopup>()

    private var spawnTimer: Float = 0f
    private val spawnInterval: Float = 5.5f

    private val popupPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
        textSize = 22f
    }
    private val shadowPopupPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
        textSize = 22f
        color = Color.BLACK
    }

    init {
        for (i in 0 until 12) {
            itemPool.add(CollectibleItem())
        }
    }

    fun reset() {
        for (item in activeItems) {
            item.active = false
            itemPool.add(item)
        }
        activeItems.clear()
        activePopups.clear()
        spawnTimer = 0f
    }

    fun update(
        dt: Float,
        road: Road,
        playerSpeedKmH: Float,
        pixelsPerMeter: Float,
        occupiedLanes: BooleanArray
    ) {
        // Spawn timer
        spawnTimer += dt
        if (spawnTimer >= spawnInterval && !road.tunnelActive) {
            spawnTimer = 0f
            attemptSpawnCollectible(road, occupiedLanes)
        }

        // Update items
        val iter = activeItems.iterator()
        while (iter.hasNext()) {
            val item = iter.next()
            item.update(dt, playerSpeedKmH, pixelsPerMeter)

            if (item.y > road.screenHeight + item.height + 40f) {
                item.active = false
                iter.remove()
                itemPool.add(item)
            }
        }

        // Update popups
        val pIter = activePopups.iterator()
        while (pIter.hasNext()) {
            val p = pIter.next()
            p.y += p.vy * dt
            p.alpha -= dt * 1.3f
            if (p.alpha <= 0f) {
                pIter.remove()
            }
        }
    }

    private fun attemptSpawnCollectible(road: Road, occupiedLanes: BooleanArray) {
        val freeLanes = mutableListOf<Int>()
        for (lane in 0..5) {
            if (!occupiedLanes[lane]) {
                freeLanes.add(lane)
            }
        }
        if (freeLanes.isEmpty()) return

        val lane = freeLanes[Random.nextInt(freeLanes.size)]
        val posX = road.getLaneCenterX(lane)
        val posY = -60f

        val r = Random.nextFloat()
        val type = when {
            r < 0.35f -> CollectibleType.CASH
            r < 0.65f -> CollectibleType.NITRO
            r < 0.85f -> CollectibleType.MULTIPLIER
            else -> CollectibleType.SHIELD
        }

        val item = if (itemPool.isNotEmpty()) itemPool.removeFirst() else CollectibleItem()
        item.spawn(lane, posX, posY, type)
        activeItems.add(item)
    }

    fun checkCollection(
        player: PlayerCar,
        onCollect: (CollectibleType, Int) -> Unit
    ) {
        if (player.isWrecked) return

        val pBounds = player.bounds
        val iter = activeItems.iterator()
        while (iter.hasNext()) {
            val item = iter.next()
            if (item.active && RectF.intersects(pBounds, item.bounds)) {
                item.active = false
                iter.remove()
                itemPool.add(item)

                val bonus = item.type.bonusScore
                onCollect(item.type, bonus)

                val popupText = when (item.type) {
                    CollectibleType.NITRO -> "+NITRO BOOST!"
                    CollectibleType.CASH -> "+$$bonus CASH!"
                    CollectibleType.SHIELD -> "★ SHIELD ACTIVE! ★"
                    CollectibleType.MULTIPLIER -> "2X MULTIPLIER!"
                }
                activePopups.add(
                    PickupPopup(
                        x = player.x,
                        y = player.y - player.height * 0.7f,
                        text = popupText,
                        color = item.type.secondaryColor
                    )
                )
            }
        }
    }

    fun draw(canvas: Canvas) {
        for (item in activeItems) {
            item.draw(canvas)
        }
    }

    fun drawPopups(canvas: Canvas) {
        for (p in activePopups) {
            val alphaInt = (p.alpha.coerceIn(0f, 1f) * 255).toInt()
            shadowPopupPaint.alpha = alphaInt
            popupPaint.color = p.color
            popupPaint.alpha = alphaInt

            canvas.drawText(p.text, p.x + 2f, p.y + 2f, shadowPopupPaint)
            canvas.drawText(p.text, p.x, p.y, popupPaint)
        }
    }
}
