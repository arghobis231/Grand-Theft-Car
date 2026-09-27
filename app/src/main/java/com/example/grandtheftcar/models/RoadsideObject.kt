package com.example.grandtheftcar.models

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import kotlin.random.Random

enum class RoadsideType {
    // Stage 1 (Sunny Suburban Highway)
    TREE_PINE,
    TREE_OAK,
    BUSH,
    FLOWER_BED,
    BILLBOARD_RETRO,
    SIGN_WRONG_WAY,
    SIGN_SPEED_LIMIT,
    SIGN_TUNNEL_AHEAD,
    LIGHT_POST,

    // Stage 2 (Sunset Desert / Canyon)
    CACTUS_SAGUARO,
    DESERT_ROCK,
    DESERT_SCRUB,
    CANYON_PILLAR,
    BILLBOARD_DESERT,
    SIGN_HEAT_WARNING,

    // Stage 3 (Neon Midnight Urban Expressway)
    SKYSCRAPER_TOWER,
    SKYSCRAPER_WIDE,
    NEON_BILLBOARD_CYBER,
    STREET_LAMP_NEON,
    OVERHEAD_GANTRY,
    CONCRETE_BARRIER_BLOCK
}

class RoadsideObject {
    var active: Boolean = false
    var x: Float = 0f
    var y: Float = 0f
    var isLeftSide: Boolean = true
    var type: RoadsideType = RoadsideType.TREE_OAK
    var scale: Float = 1.0f

    // Seeded random variation per object
    private var variantSeed: Int = 0

    private val trunkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#78350F")
    }
    private val foliagePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val signBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val signTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val postPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.parseColor("#64748B")
    }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(85, 0, 0, 0)
    }

    fun spawn(sideIsLeft: Boolean, posX: Float, posY: Float, objType: RoadsideType) {
        active = true
        isLeftSide = sideIsLeft
        x = posX
        y = posY
        type = objType
        scale = 0.85f + Random.nextFloat() * 0.35f
        variantSeed = Random.nextInt(1000)
    }

    fun update(dy: Float) {
        if (!active) return
        y += dy
    }

    fun draw(canvas: Canvas) {
        if (!active) return

        canvas.save()
        canvas.translate(x, y)
        canvas.scale(scale, scale)

        when (type) {
            RoadsideType.TREE_PINE -> drawPineTree(canvas)
            RoadsideType.TREE_OAK -> drawOakTree(canvas)
            RoadsideType.BUSH -> drawBush(canvas)
            RoadsideType.FLOWER_BED -> drawFlowerBed(canvas)
            RoadsideType.BILLBOARD_RETRO -> drawRetroBillboard(canvas)
            RoadsideType.SIGN_WRONG_WAY -> drawWrongWaySign(canvas)
            RoadsideType.SIGN_SPEED_LIMIT -> drawSpeedSign(canvas)
            RoadsideType.SIGN_TUNNEL_AHEAD -> drawTunnelSign(canvas)
            RoadsideType.LIGHT_POST -> drawLightPost(canvas)

            RoadsideType.CACTUS_SAGUARO -> drawSaguaroCactus(canvas)
            RoadsideType.DESERT_ROCK -> drawDesertRock(canvas)
            RoadsideType.DESERT_SCRUB -> drawDesertScrub(canvas)
            RoadsideType.CANYON_PILLAR -> drawCanyonPillar(canvas)
            RoadsideType.BILLBOARD_DESERT -> drawDesertBillboard(canvas)
            RoadsideType.SIGN_HEAT_WARNING -> drawHeatWarningSign(canvas)

            RoadsideType.SKYSCRAPER_TOWER -> drawSkyscraperTower(canvas)
            RoadsideType.SKYSCRAPER_WIDE -> drawSkyscraperWide(canvas)
            RoadsideType.NEON_BILLBOARD_CYBER -> drawNeonCyberBillboard(canvas)
            RoadsideType.STREET_LAMP_NEON -> drawNeonStreetLamp(canvas)
            RoadsideType.OVERHEAD_GANTRY -> drawOverheadGantry(canvas)
            RoadsideType.CONCRETE_BARRIER_BLOCK -> drawConcreteBarrier(canvas)
        }

        canvas.restore()
    }

    // ==========================================
    // STAGE 1: SUBURBAN / HIGHWAY GREENERY
    // ==========================================
    private fun drawPineTree(canvas: Canvas) {
        canvas.drawOval(-24f, -10f, 24f, 15f, shadowPaint)

        foliagePaint.color = Color.parseColor("#064E3B")
        val p1 = Path().apply { moveTo(0f, -65f); lineTo(26f, -30f); lineTo(-26f, -30f); close() }
        canvas.drawPath(p1, foliagePaint)

        foliagePaint.color = Color.parseColor("#047857")
        val p2 = Path().apply { moveTo(0f, -40f); lineTo(32f, -5f); lineTo(-32f, -5f); close() }
        canvas.drawPath(p2, foliagePaint)

        foliagePaint.color = Color.parseColor("#059669")
        val p3 = Path().apply { moveTo(0f, -15f); lineTo(38f, 20f); lineTo(-38f, 20f); close() }
        canvas.drawPath(p3, foliagePaint)
    }

    private fun drawOakTree(canvas: Canvas) {
        canvas.drawOval(-30f, 5f, 30f, 30f, shadowPaint)
        canvas.drawRect(-6f, 0f, 6f, 25f, trunkPaint)

        foliagePaint.color = Color.parseColor("#15803D")
        canvas.drawCircle(0f, -15f, 32f, foliagePaint)
        foliagePaint.color = Color.parseColor("#16A34A")
        canvas.drawCircle(-14f, -10f, 22f, foliagePaint)
        canvas.drawCircle(14f, -10f, 22f, foliagePaint)
        foliagePaint.color = Color.parseColor("#22C55E")
        canvas.drawCircle(0f, -25f, 18f, foliagePaint)
    }

    private fun drawBush(canvas: Canvas) {
        foliagePaint.color = Color.parseColor("#166534")
        canvas.drawCircle(-12f, 0f, 16f, foliagePaint)
        canvas.drawCircle(12f, 0f, 16f, foliagePaint)
        foliagePaint.color = Color.parseColor("#15803D")
        canvas.drawCircle(0f, -6f, 18f, foliagePaint)
    }

    private fun drawFlowerBed(canvas: Canvas) {
        foliagePaint.color = Color.parseColor("#14532D")
        canvas.drawRoundRect(-25f, -10f, 25f, 10f, 8f, 8f, foliagePaint)

        val flowerP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        flowerP.color = Color.parseColor("#F43F5E")
        canvas.drawCircle(-15f, -3f, 4f, flowerP)
        canvas.drawCircle(0f, -5f, 4.5f, flowerP)
        canvas.drawCircle(14f, -3f, 4f, flowerP)
        flowerP.color = Color.parseColor("#FBBF24")
        canvas.drawCircle(-7f, 4f, 3.5f, flowerP)
        canvas.drawCircle(8f, 4f, 3.5f, flowerP)
    }

    private fun drawRetroBillboard(canvas: Canvas) {
        // Double steel support posts
        postPaint.color = Color.parseColor("#475569")
        canvas.drawLine(-22f, 0f, -22f, 40f, postPaint)
        canvas.drawLine(22f, 0f, 22f, 40f, postPaint)

        // Billboard frame
        signBgPaint.color = Color.parseColor("#0F172A")
        canvas.drawRoundRect(-38f, -28f, 38f, 4f, 4f, 4f, signBgPaint)

        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FACC15")
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
        }
        canvas.drawRoundRect(-36f, -26f, 36f, 2f, 3f, 3f, border)

        signTextPaint.color = Color.parseColor("#EF4444")
        signTextPaint.textSize = 9f
        canvas.drawText("GRAND THEFT", 0f, -15f, signTextPaint)
        signTextPaint.color = Color.parseColor("#38BDF8")
        signTextPaint.textSize = 8f
        canvas.drawText("MOTORS", 0f, -3f, signTextPaint)
    }

    private fun drawWrongWaySign(canvas: Canvas) {
        canvas.drawLine(0f, 0f, 0f, 35f, postPaint)

        signBgPaint.color = Color.parseColor("#DC2626")
        canvas.drawRoundRect(-36f, -28f, 36f, 4f, 4f, 4f, signBgPaint)

        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
        }
        canvas.drawRoundRect(-34f, -26f, 34f, 2f, 3f, 3f, border)

        val bar = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawRect(-26f, -14f, 26f, -8f, bar)

        signTextPaint.color = Color.WHITE
        signTextPaint.textSize = 9f
        canvas.drawText("WRONG", 0f, -16f, signTextPaint)
        canvas.drawText("WAY", 0f, 0f, signTextPaint)
    }

    private fun drawSpeedSign(canvas: Canvas) {
        canvas.drawLine(0f, 0f, 0f, 35f, postPaint)

        signBgPaint.color = Color.WHITE
        canvas.drawRoundRect(-20f, -28f, 20f, 2f, 3f, 3f, signBgPaint)

        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRoundRect(-19f, -27f, 19f, 1f, 2f, 2f, border)

        signTextPaint.color = Color.BLACK
        signTextPaint.textSize = 8f
        canvas.drawText("SPEED", 0f, -18f, signTextPaint)
        signTextPaint.textSize = 12f
        canvas.drawText("65", 0f, -4f, signTextPaint)
    }

    private fun drawTunnelSign(canvas: Canvas) {
        canvas.drawLine(0f, 0f, 0f, 35f, postPaint)

        signBgPaint.color = Color.parseColor("#0284C7")
        canvas.drawRoundRect(-30f, -26f, 30f, 4f, 4f, 4f, signBgPaint)

        signTextPaint.color = Color.WHITE
        signTextPaint.textSize = 8f
        canvas.drawText("TUNNEL", 0f, -14f, signTextPaint)
        canvas.drawText("AHEAD", 0f, -2f, signTextPaint)
    }

    private fun drawLightPost(canvas: Canvas) {
        val lampPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8")
            style = Paint.Style.STROKE
            strokeWidth = 3.5f
        }
        val armDir = if (isLeftSide) 1f else -1f
        val path = Path().apply {
            moveTo(0f, 35f)
            lineTo(0f, -20f)
            cubicTo(0f, -35f, 15f * armDir, -40f, 25f * armDir, -38f)
        }
        canvas.drawPath(path, lampPaint)

        val bulbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FEF08A")
            style = Paint.Style.FILL
        }
        canvas.drawCircle(25f * armDir, -38f, 5f, bulbPaint)
    }

    // ==========================================
    // STAGE 2: DESERT / CANYON ENVIRONMENT
    // ==========================================
    private fun drawSaguaroCactus(canvas: Canvas) {
        canvas.drawOval(-16f, 18f, 16f, 26f, shadowPaint)

        val cactusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#15803D")
            style = Paint.Style.FILL
        }
        // Main vertical trunk
        canvas.drawRoundRect(-6f, -45f, 6f, 22f, 6f, 6f, cactusPaint)

        // Left arm
        val leftArm = Path().apply {
            moveTo(-5f, -10f)
            lineTo(-18f, -10f)
            lineTo(-18f, -32f)
            lineTo(-12f, -32f)
            lineTo(-12f, -16f)
            lineTo(-5f, -16f)
            close()
        }
        canvas.drawPath(leftArm, cactusPaint)

        // Right arm
        val rightArm = Path().apply {
            moveTo(5f, -5f)
            lineTo(18f, -5f)
            lineTo(18f, -24f)
            lineTo(12f, -24f)
            lineTo(12f, -11f)
            lineTo(5f, -11f)
            close()
        }
        canvas.drawPath(rightArm, cactusPaint)
    }

    private fun drawDesertRock(canvas: Canvas) {
        canvas.drawOval(-26f, 10f, 26f, 22f, shadowPaint)

        val rockPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#C2410C") // Canyon red terracotta rock
            style = Paint.Style.FILL
        }
        val p = Path().apply {
            moveTo(-22f, 16f)
            lineTo(-18f, -12f)
            lineTo(-4f, -24f)
            lineTo(14f, -18f)
            lineTo(24f, 6f)
            lineTo(18f, 18f)
            close()
        }
        canvas.drawPath(p, rockPaint)

        // Highlight facet
        val facetPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#EA580C")
            style = Paint.Style.FILL
        }
        val facet = Path().apply {
            moveTo(-4f, -24f)
            lineTo(14f, -18f)
            lineTo(6f, 2f)
            lineTo(-10f, -4f)
            close()
        }
        canvas.drawPath(facet, facetPaint)
    }

    private fun drawDesertScrub(canvas: Canvas) {
        val scrubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#A16207") // Dried desert brush
            style = Paint.Style.FILL
        }
        canvas.drawCircle(-10f, 0f, 12f, scrubPaint)
        canvas.drawCircle(10f, 0f, 12f, scrubPaint)
        scrubPaint.color = Color.parseColor("#CA8A04")
        canvas.drawCircle(0f, -6f, 14f, scrubPaint)
    }

    private fun drawCanyonPillar(canvas: Canvas) {
        // Red rock canyon bluff
        val bluffPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#7C2D12")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(-30f, -40f, 30f, 30f, 10f, 10f, bluffPaint)

        val ridgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#9A3412")
            style = Paint.Style.FILL
        }
        canvas.drawRect(-26f, -20f, 26f, -10f, ridgePaint)
        canvas.drawRect(-22f, 5f, 22f, 15f, ridgePaint)
    }

    private fun drawDesertBillboard(canvas: Canvas) {
        postPaint.color = Color.parseColor("#78350F")
        canvas.drawLine(-20f, 0f, -20f, 35f, postPaint)
        canvas.drawLine(20f, 0f, 20f, 35f, postPaint)

        signBgPaint.color = Color.parseColor("#451A03")
        canvas.drawRoundRect(-36f, -26f, 36f, 4f, 4f, 4f, signBgPaint)

        signTextPaint.color = Color.parseColor("#F59E0B")
        signTextPaint.textSize = 9f
        canvas.drawText("DESERT OASIS", 0f, -14f, signTextPaint)
        signTextPaint.color = Color.WHITE
        signTextPaint.textSize = 8f
        canvas.drawText("GAS • DINER • 10MI", 0f, -2f, signTextPaint)
    }

    private fun drawHeatWarningSign(canvas: Canvas) {
        canvas.drawLine(0f, 0f, 0f, 35f, postPaint)

        // Yellow diamond warning sign
        val diamond = Path().apply {
            moveTo(0f, -32f)
            lineTo(22f, -10f)
            lineTo(0f, 12f)
            lineTo(-22f, -10f)
            close()
        }
        signBgPaint.color = Color.parseColor("#FACC15")
        canvas.drawPath(diamond, signBgPaint)

        signTextPaint.color = Color.BLACK
        signTextPaint.textSize = 8f
        canvas.drawText("HIGH", 0f, -14f, signTextPaint)
        canvas.drawText("HEAT", 0f, -4f, signTextPaint)
    }

    // ==========================================
    // STAGE 3: NEON MIDNIGHT CITY
    // ==========================================
    private fun drawSkyscraperTower(canvas: Canvas) {
        val towerW = 44f
        val towerH = 110f

        // Dark skyscraper silhouette
        val towerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#090D16")
            style = Paint.Style.FILL
        }
        canvas.drawRect(-towerW * 0.5f, -towerH * 0.7f, towerW * 0.5f, towerH * 0.3f, towerPaint)

        // Illuminated window grid (Cyan/amber neon lit offices)
        val windowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        for (row in 0..6) {
            val wy = -towerH * 0.65f + row * 15f
            for (col in 0..2) {
                val wx = -towerW * 0.35f + col * 12f
                val lit = ((variantSeed + row * 3 + col) % 3 != 0)
                if (lit) {
                    windowPaint.color = if ((row + col) % 2 == 0) Color.parseColor("#38BDF8") else Color.parseColor("#FDE047")
                    canvas.drawRect(wx, wy, wx + 6f, wy + 8f, windowPaint)
                }
            }
        }
    }

    private fun drawSkyscraperWide(canvas: Canvas) {
        val bW = 60f
        val bH = 85f

        val bPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            style = Paint.Style.FILL
        }
        canvas.drawRect(-bW * 0.5f, -bH * 0.6f, bW * 0.5f, bH * 0.4f, bPaint)

        // Neon roof edge glow
        val neonP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#EC4899") // Hot magenta
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawLine(-bW * 0.5f, -bH * 0.6f, bW * 0.5f, -bH * 0.6f, neonP)

        // Window matrix
        val winP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#67E8F9")
            style = Paint.Style.FILL
        }
        for (r in 0..4) {
            val wy = -bH * 0.5f + r * 14f
            for (c in 0..4) {
                val wx = -bW * 0.4f + c * 11f
                canvas.drawRect(wx, wy, wx + 5f, wy + 6f, winP)
            }
        }
    }

    private fun drawNeonCyberBillboard(canvas: Canvas) {
        postPaint.color = Color.parseColor("#334155")
        canvas.drawLine(-24f, 0f, -24f, 35f, postPaint)
        canvas.drawLine(24f, 0f, 24f, 35f, postPaint)

        signBgPaint.color = Color.parseColor("#020617")
        canvas.drawRoundRect(-38f, -28f, 38f, 4f, 6f, 6f, signBgPaint)

        val neonBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#A855F7") // Purple neon border
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
        }
        canvas.drawRoundRect(-36f, -26f, 36f, 2f, 4f, 4f, neonBorder)

        signTextPaint.color = Color.parseColor("#38BDF8")
        signTextPaint.textSize = 10f
        canvas.drawText("NEON RUN", 0f, -14f, signTextPaint)
        signTextPaint.color = Color.parseColor("#F43F5E")
        signTextPaint.textSize = 8f
        canvas.drawText("HIGHWAY 66", 0f, -2f, signTextPaint)
    }

    private fun drawNeonStreetLamp(canvas: Canvas) {
        val polePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#475569")
            style = Paint.Style.STROKE
            strokeWidth = 3.5f
        }
        val armDir = if (isLeftSide) 1f else -1f
        val path = Path().apply {
            moveTo(0f, 35f)
            lineTo(0f, -25f)
            cubicTo(0f, -40f, 18f * armDir, -42f, 28f * armDir, -38f)
        }
        canvas.drawPath(path, polePaint)

        // Blue / Cyan neon street glow
        val neonBulb = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#38BDF8")
            style = Paint.Style.FILL
        }
        canvas.drawCircle(28f * armDir, -38f, 6f, neonBulb)

        // Ground pool of light
        val lightCone = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(45, 56, 189, 248)
            style = Paint.Style.FILL
        }
        canvas.drawOval(15f * armDir - 20f, 15f, 15f * armDir + 20f, 35f, lightCone)
    }

    private fun drawOverheadGantry(canvas: Canvas) {
        val trussPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#334155")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawLine(0f, -30f, 0f, 35f, trussPaint)

        // LED Matrix message board
        signBgPaint.color = Color.parseColor("#090D16")
        canvas.drawRoundRect(-30f, -26f, 30f, 2f, 4f, 4f, signBgPaint)

        signTextPaint.color = Color.parseColor("#F59E0B") // Amber dot-matrix
        signTextPaint.textSize = 8f
        canvas.drawText("WRONG WAY", 0f, -15f, signTextPaint)
        signTextPaint.color = Color.parseColor("#EF4444")
        canvas.drawText("TURN BACK", 0f, -3f, signTextPaint)
    }

    private fun drawConcreteBarrier(canvas: Canvas) {
        val barrierPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#475569")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(-20f, -12f, 20f, 12f, 4f, 4f, barrierPaint)

        // Diagonal reflector on barrier
        val refPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FACC15")
            style = Paint.Style.FILL
        }
        canvas.drawRect(-4f, -8f, 4f, 8f, refPaint)
    }
}
