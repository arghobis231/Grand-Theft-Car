package com.example.grandtheftcar.models

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import kotlin.random.Random

enum class TimeOfDay {
    DAY,
    SUNSET,
    MIDNIGHT
}

class Road {
    var screenWidth: Float = 0f
    var screenHeight: Float = 0f

    // 6-Lane highway geometry
    var roadWidth: Float = 0f
    var roadLeft: Float = 0f
    var roadRight: Float = 0f
    var laneWidth: Float = 0f

    var timeOfDay: TimeOfDay = TimeOfDay.DAY
    var currentStageNumber: Int = 1

    // Road motion & perspective
    private var scrollOffset: Float = 0f

    // Tunnel entrance variables
    var tunnelActive: Boolean = false
    var tunnelY: Float = -1000f
    var isInsideTunnel: Boolean = false
    private var tunnelLightOffset: Float = 0f

    // Roadside objects pool
    val roadsideObjects = mutableListOf<RoadsideObject>()
    private val roadsidePool = ArrayDeque<RoadsideObject>()
    private var objectSpawnDistanceCounter: Float = 0f

    // Paints
    private val asphaltPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val vergePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val laneDashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4.5f
        color = Color.WHITE
    }
    private val centerLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.parseColor("#EAB308") // Double yellow
    }
    private val shoulderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
        color = Color.WHITE
    }
    private val guardrailPostPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#475569")
    }
    private val guardrailBeamPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 7f
        color = Color.parseColor("#94A3B8")
    }
    private val kerbRedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#DC2626")
    }
    private val kerbWhitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }
    private val tunnelWallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#1E293B")
    }
    private val tunnelHolePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#020617")
    }
    private val hazardStripeYellow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#FACC15")
    }
    private val hazardStripeBlack = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#0F172A")
    }

    fun resize(width: Float, height: Float) {
        screenWidth = width
        screenHeight = height

        // 6 lanes take up ~72% of width in landscape or ~92% in portrait
        val isLandscape = width > height
        val roadRatio = if (isLandscape) 0.70f else 0.90f
        roadWidth = (width * roadRatio).coerceAtLeast(360f)
        roadLeft = (width - roadWidth) * 0.5f
        roadRight = roadLeft + roadWidth
        laneWidth = roadWidth / 6f

        // Initialize roadside objects
        roadsideObjects.clear()
        for (i in 0 until 16) {
            val isLeft = (i % 2 == 0)
            val posX = if (isLeft) roadLeft - 45f - Random.nextFloat() * 45f else roadRight + 45f + Random.nextFloat() * 45f
            val posY = Random.nextFloat() * height
            val obj = RoadsideObject()
            obj.spawn(isLeft, posX, posY, getRandomRoadsideTypeForStage(currentStageNumber))
            roadsideObjects.add(obj)
        }
    }

    fun getLaneCenterX(lane: Int): Float {
        val clampedLane = lane.coerceIn(0, 5)
        return roadLeft + (clampedLane + 0.5f) * laneWidth
    }

    fun getLaneIndexForX(x: Float): Int {
        val relative = x - roadLeft
        return (relative / laneWidth).toInt().coerceIn(0, 5)
    }

    fun update(dt: Float, speedKmH: Float, pixelsPerMeter: Float) {
        val speedMps = speedKmH * (1000f / 3600f)
        val dy = speedMps * pixelsPerMeter * dt
        scrollOffset = (scrollOffset + dy) % 120f
        tunnelLightOffset = (tunnelLightOffset + dy * 1.5f) % 90f

        // Update roadside objects
        val iter = roadsideObjects.iterator()
        while (iter.hasNext()) {
            val obj = iter.next()
            obj.update(dy)
            if (obj.y > screenHeight + 90f) {
                iter.remove()
                roadsidePool.add(obj)
            }
        }

        // Spawn new roadside objects
        objectSpawnDistanceCounter += dy
        if (objectSpawnDistanceCounter > 85f) {
            objectSpawnDistanceCounter = 0f
            spawnRoadsideItem()
        }

        // Tunnel portal movement if approaching
        if (tunnelActive) {
            tunnelY += dy
        }
    }

    private fun spawnRoadsideItem() {
        val isLeft = Random.nextBoolean()
        val margin = 35f + Random.nextFloat() * 60f
        val posX = if (isLeft) (roadLeft - margin).coerceAtLeast(15f) else (roadRight + margin).coerceAtMost(screenWidth - 15f)
        val posY = -70f

        val type = getRandomRoadsideTypeForStage(currentStageNumber)
        val obj = if (roadsidePool.isNotEmpty()) roadsidePool.removeFirst() else RoadsideObject()
        obj.spawn(isLeft, posX, posY, type)
        roadsideObjects.add(obj)
    }

    private fun getRandomRoadsideTypeForStage(stageNum: Int): RoadsideType {
        val r = Random.nextFloat()
        return when (stageNum) {
            2 -> {
                // Desert / Canyon Stage
                when {
                    r < 0.35f -> RoadsideType.CACTUS_SAGUARO
                    r < 0.60f -> RoadsideType.DESERT_ROCK
                    r < 0.78f -> RoadsideType.DESERT_SCRUB
                    r < 0.88f -> RoadsideType.CANYON_PILLAR
                    r < 0.94f -> RoadsideType.BILLBOARD_DESERT
                    else -> RoadsideType.SIGN_HEAT_WARNING
                }
            }
            3 -> {
                // Neon Midnight Urban Stage
                when {
                    r < 0.32f -> RoadsideType.SKYSCRAPER_TOWER
                    r < 0.58f -> RoadsideType.SKYSCRAPER_WIDE
                    r < 0.75f -> RoadsideType.STREET_LAMP_NEON
                    r < 0.88f -> RoadsideType.NEON_BILLBOARD_CYBER
                    r < 0.94f -> RoadsideType.OVERHEAD_GANTRY
                    else -> RoadsideType.CONCRETE_BARRIER_BLOCK
                }
            }
            else -> {
                // Sunny Suburban Highway
                when {
                    r < 0.32f -> RoadsideType.TREE_OAK
                    r < 0.56f -> RoadsideType.TREE_PINE
                    r < 0.72f -> RoadsideType.BUSH
                    r < 0.84f -> RoadsideType.FLOWER_BED
                    r < 0.91f -> RoadsideType.BILLBOARD_RETRO
                    r < 0.95f -> RoadsideType.SIGN_WRONG_WAY
                    r < 0.98f -> RoadsideType.SIGN_SPEED_LIMIT
                    else -> RoadsideType.SIGN_TUNNEL_AHEAD
                }
            }
        }
    }

    fun triggerTunnel() {
        tunnelActive = true
        tunnelY = -screenHeight * 0.8f
    }

    fun resetTunnel() {
        tunnelActive = false
        tunnelY = -1000f
        isInsideTunnel = false
    }

    fun draw(canvas: Canvas) {
        drawVerge(canvas)
        drawAsphalt(canvas)
        drawRumbleStrips(canvas)
        drawCenterDivider(canvas)
        drawLaneDashes(canvas)
        drawShoulders(canvas)
        drawGuardrails(canvas)

        // Draw roadside objects
        for (obj in roadsideObjects) {
            obj.draw(canvas)
        }

        // Draw tunnel entrance if active
        if (tunnelActive) {
            drawTunnelEntrance(canvas)
        }

        // Overhead lights if inside tunnel
        if (isInsideTunnel) {
            drawTunnelInteriorLights(canvas)
        }
    }

    private fun drawVerge(canvas: Canvas) {
        when (currentStageNumber) {
            1 -> vergePaint.color = Color.parseColor("#15803D") // Lush countryside emerald grass
            2 -> vergePaint.color = Color.parseColor("#9A3412") // Canyon desert sandstone & terracotta
            3 -> vergePaint.color = Color.parseColor("#090D16") // Urban sidewalk concrete with dark shadows
            else -> vergePaint.color = Color.parseColor("#15803D")
        }

        canvas.drawRect(0f, 0f, roadLeft, screenHeight, vergePaint)
        canvas.drawRect(roadRight, 0f, screenWidth, screenHeight, vergePaint)

        // Stage 3 curb neon reflection strips
        if (currentStageNumber == 3) {
            val neonCurb = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#38BDF8")
                strokeWidth = 3f
                style = Paint.Style.STROKE
            }
            canvas.drawLine(roadLeft - 2f, 0f, roadLeft - 2f, screenHeight, neonCurb)
            canvas.drawLine(roadRight + 2f, 0f, roadRight + 2f, screenHeight, neonCurb)
        }
    }

    private fun drawAsphalt(canvas: Canvas) {
        val roadColor = when (currentStageNumber) {
            1 -> Color.parseColor("#1E293B") // Dark tarmac
            2 -> Color.parseColor("#2E1C24") // Sunset bronze asphalt
            3 -> Color.parseColor("#0B1120") // Pitch dark midnight expressway
            else -> Color.parseColor("#1E293B")
        }
        asphaltPaint.color = roadColor
        canvas.drawRect(roadLeft, 0f, roadRight, screenHeight, asphaltPaint)
    }

    private fun drawRumbleStrips(canvas: Canvas) {
        val segmentH = 30f
        var startY = (scrollOffset % (segmentH * 2)) - segmentH * 2
        val kerbW = 8f

        while (startY < screenHeight + segmentH) {
            val isRed = ((startY / segmentH).toInt() % 2 == 0)
            val paint = if (isRed) kerbRedPaint else kerbWhitePaint

            canvas.drawRect(roadLeft - kerbW, startY, roadLeft, startY + segmentH, paint)
            canvas.drawRect(roadRight, startY, roadRight + kerbW, startY + segmentH, paint)

            startY += segmentH
        }
    }

    private fun drawCenterDivider(canvas: Canvas) {
        val centerX = roadLeft + roadWidth * 0.5f

        val centerStripPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#090D16")
        }
        canvas.drawRect(centerX - 8f, 0f, centerX + 8f, screenHeight, centerStripPaint)

        // Double solid yellow lines
        canvas.drawLine(centerX - 4f, 0f, centerX - 4f, screenHeight, centerLinePaint)
        canvas.drawLine(centerX + 4f, 0f, centerX + 4f, screenHeight, centerLinePaint)

        // Center road reflectors (cats eyes) scrolling down
        val reflectorH = 80f
        var rY = (scrollOffset % reflectorH) - reflectorH
        val reflectorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#FACC15")
        }
        while (rY < screenHeight + 20f) {
            canvas.drawCircle(centerX, rY, 3.5f, reflectorPaint)
            rY += reflectorH
        }
    }

    private fun drawLaneDashes(canvas: Canvas) {
        val dashLength = 36f
        val gapLength = 48f
        val cycle = dashLength + gapLength

        val dividerIndices = intArrayOf(1, 2, 4, 5)

        for (div in dividerIndices) {
            val divX = roadLeft + div * laneWidth
            var curY = (scrollOffset % cycle) - cycle

            while (curY < screenHeight + cycle) {
                canvas.drawLine(divX, curY, divX, curY + dashLength, laneDashPaint)
                curY += cycle
            }
        }
    }

    private fun drawShoulders(canvas: Canvas) {
        canvas.drawLine(roadLeft + 2f, 0f, roadLeft + 2f, screenHeight, shoulderPaint)
        canvas.drawLine(roadRight - 2f, 0f, roadRight - 2f, screenHeight, shoulderPaint)
    }

    private fun drawGuardrails(canvas: Canvas) {
        val postSpacing = 50f
        var curY = (scrollOffset % postSpacing) - postSpacing

        val leftRailX = roadLeft - 14f
        val rightRailX = roadRight + 14f

        while (curY < screenHeight + 20f) {
            canvas.drawRect(leftRailX - 3f, curY, leftRailX + 3f, curY + 12f, guardrailPostPaint)
            canvas.drawRect(rightRailX - 3f, curY, rightRailX + 3f, curY + 12f, guardrailPostPaint)
            curY += postSpacing
        }

        canvas.drawLine(leftRailX, 0f, leftRailX, screenHeight, guardrailBeamPaint)
        canvas.drawLine(rightRailX, 0f, rightRailX, screenHeight, guardrailBeamPaint)
    }

    private fun drawTunnelEntrance(canvas: Canvas) {
        val archTop = tunnelY
        val archHeight = 280f
        val archBottom = archTop + archHeight

        // Solid darkness inside tunnel hole
        canvas.drawRoundRect(
            roadLeft - 20f, archTop + 60f, roadRight + 20f, archBottom + 400f,
            24f, 24f, tunnelHolePaint
        )

        // Massive concrete arch over highway
        canvas.drawRect(roadLeft - 40f, archTop, roadRight + 40f, archTop + 75f, tunnelWallPaint)

        // Side pillars
        canvas.drawRect(roadLeft - 45f, archTop, roadLeft - 10f, archBottom, tunnelWallPaint)
        canvas.drawRect(roadRight + 10f, archTop, roadRight + 45f, archBottom, tunnelWallPaint)

        // Yellow and black warning chevron stripes across portal beam
        val stripeW = 32f
        var sx = roadLeft - 35f
        var toggle = false
        while (sx < roadRight + 35f) {
            val paint = if (toggle) hazardStripeYellow else hazardStripeBlack
            canvas.drawRect(sx, archTop + 50f, sx + stripeW, archTop + 70f, paint)
            sx += stripeW
            toggle = !toggle
        }

        // Sign
        val signPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0284C7")
            style = Paint.Style.FILL
        }
        val signText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 20f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        val centerX = (roadLeft + roadRight) * 0.5f
        canvas.drawRoundRect(centerX - 150f, archTop + 10f, centerX + 150f, archTop + 45f, 6f, 6f, signPaint)
        canvas.drawText(">>> TUNNEL 0$currentStageNumber - FULL THROTTLE >>>", centerX, archTop + 35f, signText)

        // Interior tunnel lights in entrance
        val lightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F97316")
            style = Paint.Style.FILL
        }
        for (i in 0..6) {
            val ly = archTop + 100f + i * 50f
            canvas.drawOval(centerX - 40f, ly, centerX - 10f, ly + 8f, lightPaint)
            canvas.drawOval(centerX + 10f, ly, centerX + 40f, ly + 8f, lightPaint)
        }
    }

    private fun drawTunnelInteriorLights(canvas: Canvas) {
        // Overhead high-speed tunnel ceiling lamps streaming down
        val lampPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FEF08A") // Warm fluorescent tunnel glow
            style = Paint.Style.FILL
        }
        val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(40, 254, 240, 138)
            style = Paint.Style.FILL
        }

        val centerX = (roadLeft + roadRight) * 0.5f
        val cycle = 120f
        var ly = tunnelLightOffset - cycle

        while (ly < screenHeight + cycle) {
            // Left row and right row of ceiling strip lamps
            canvas.drawOval(centerX - 70f, ly - 10f, centerX - 10f, ly + 10f, haloPaint)
            canvas.drawOval(centerX + 10f, ly - 10f, centerX + 70f, ly + 10f, haloPaint)

            canvas.drawRoundRect(centerX - 60f, ly - 4f, centerX - 20f, ly + 4f, 4f, 4f, lampPaint)
            canvas.drawRoundRect(centerX + 20f, ly - 4f, centerX + 60f, ly + 4f, 4f, 4f, lampPaint)
            ly += cycle
        }
    }
}
