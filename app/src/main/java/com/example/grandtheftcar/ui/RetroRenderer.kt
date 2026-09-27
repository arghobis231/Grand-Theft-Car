package com.example.grandtheftcar.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import com.example.grandtheftcar.engine.GameState
import com.example.grandtheftcar.managers.ControlScheme
import com.example.grandtheftcar.managers.Difficulty
import com.example.grandtheftcar.managers.GraphicsQuality
import com.example.grandtheftcar.managers.SettingsManager
import com.example.grandtheftcar.managers.StageManager
import com.example.grandtheftcar.models.PlayerCar
import kotlin.math.min
import kotlin.math.sin

class RetroRenderer(
    private val settings: SettingsManager,
    private val stageManager: StageManager
) {

    // Clickable button hitboxes
    val btnStart = RectF()
    val btnSettings = RectF()
    val btnAbout = RectF()

    val btnPause = RectF()
    val btnResume = RectF()
    val btnRestart = RectF()
    val btnMenu = RectF()
    val btnNextStage = RectF()

    // Virtual control buttons
    val btnLeft = RectF()
    val btnRight = RectF()
    val btnBoost = RectF()
    val btnBrake = RectF()
    val btnQuickNitro = RectF()

    // Settings screen buttons
    val btnSettingsBack = RectF()
    val btnAboutBack = RectF()
    val btnMuteToggle = RectF()
    val btnVibrationToggle = RectF()
    val btnControlsToggle = RectF()
    val btnDifficultyToggle = RectF()
    val btnMasterDown = RectF()
    val btnMasterUp = RectF()
    val btnBrightnessDown = RectF()
    val btnBrightnessUp = RectF()

    // Animated timers
    var animTimer: Float = 0f

    // Common Paints
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val shadowTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
        color = Color.BLACK
    }
    private val panelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    private val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    fun update(dt: Float) {
        animTimer += dt
    }

    // ==========================================
    // 1. LOADING SCREEN
    // ==========================================
    fun drawLoading(canvas: Canvas, width: Float, height: Float, progress: Float) {
        canvas.drawColor(Color.parseColor("#090D16"))

        val cx = width * 0.5f
        val cy = height * 0.44f

        // Title
        drawArcadeText(canvas, "GRAND THEFT CAR", cx, cy - 85f, 50f, Color.parseColor("#EF4444"), Color.parseColor("#7F1D1D"))
        drawArcadeText(canvas, "WRONG WAY RACER", cx, cy - 35f, 22f, Color.parseColor("#FACC15"), Color.parseColor("#78350F"))

        // Car icon in center
        val carIconW = 56f
        val carIconH = 105f
        panelPaint.color = Color.parseColor("#DC2626")
        canvas.drawRoundRect(cx - carIconW * 0.5f, cy - 10f, cx + carIconW * 0.5f, cy - 10f + carIconH, 12f, 12f, panelPaint)

        // Headlights glow on icon
        val lightP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FEF08A")
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx - 16f, cy - 10f, 7f, lightP)
        canvas.drawCircle(cx + 16f, cy - 10f, 7f, lightP)

        // Progress Bar
        val barW = (width * 0.65f).coerceIn(280f, 480f)
        val barH = 26f
        val barX = cx - barW * 0.5f
        val barY = cy + 130f

        panelPaint.color = Color.parseColor("#1E293B")
        canvas.drawRoundRect(barX, barY, barX + barW, barY + barH, 10f, 10f, panelPaint)

        panelPaint.color = Color.parseColor("#38BDF8")
        canvas.drawRoundRect(barX + 3f, barY + 3f, barX + 3f + (barW - 6f) * progress.coerceIn(0f, 1f), barY + barH - 3f, 8f, 8f, panelPaint)

        borderPaint.color = Color.parseColor("#0284C7")
        canvas.drawRoundRect(barX, barY, barX + barW, barY + barH, 10f, 10f, borderPaint)

        // Loading text
        val pulse = (sin(animTimer * 6.0) * 0.3 + 0.7).toFloat()
        textPaint.color = Color.argb((pulse * 255).toInt(), 255, 255, 255)
        textPaint.textSize = 20f
        canvas.drawText("LOADING HIGHWAY ASSETS...", cx, barY + 56f, textPaint)
    }

    // ==========================================
    // 2. MAIN MENU
    // ==========================================
    fun drawMainMenu(canvas: Canvas, width: Float, height: Float) {
        val cx = width * 0.5f
        val isLandscape = width > height

        // Dark gradient backdrop over animated highway
        overlayPaint.shader = LinearGradient(
            0f, 0f, 0f, height,
            Color.argb(170, 15, 23, 42),
            Color.argb(235, 2, 6, 23),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, width, height, overlayPaint)
        overlayPaint.shader = null

        // Big Dominant Title
        val titleY = if (isLandscape) (height * 0.20f).coerceAtLeast(65f) else height * 0.16f
        val wobble = sin(animTimer * 3.5f) * 4f
        drawArcadeText(canvas, "GRAND THEFT CAR", cx, titleY + wobble, 52f, Color.parseColor("#EF4444"), Color.parseColor("#991B1B"))
        drawArcadeText(canvas, "DRIVE THE WRONG WAY", cx, titleY + wobble + 38f, 22f, Color.parseColor("#FBBF24"), Color.parseColor("#B45309"))

        // High score badge
        panelPaint.color = Color.argb(210, 30, 41, 59)
        val badgeW = (width * 0.55f).coerceIn(320f, 420f)
        val badgeH = 46f
        val badgeY = titleY + wobble + 58f
        canvas.drawRoundRect(cx - badgeW * 0.5f, badgeY, cx + badgeW * 0.5f, badgeY + badgeH, 14f, 14f, panelPaint)
        borderPaint.color = Color.parseColor("#F59E0B")
        canvas.drawRoundRect(cx - badgeW * 0.5f, badgeY, cx + badgeW * 0.5f, badgeY + badgeH, 14f, 14f, borderPaint)

        textPaint.color = Color.parseColor("#FDE047")
        textPaint.textSize = 19f
        canvas.drawText("TOP SCORE: ${settings.highScore}  |  STAGE: ${settings.highestStage}", cx, badgeY + 30f, textPaint)

        // RESPONSIVE LARGE BUTTONS: START, SETTINGS, ABOUT
        // Optimized for real finger touches: 60dp - 65dp high, wide, comfortable
        val btnW = (width * 0.44f).coerceIn(280f, 380f)
        val btnH = if (isLandscape) 58f else 64f
        val spacing = if (isLandscape) 16f else 22f
        val startY = (badgeY + badgeH + (if (isLandscape) 22f else 32f)).coerceAtMost(height - (btnH * 3 + spacing * 2 + 35f))

        btnStart.set(cx - btnW * 0.5f, startY, cx + btnW * 0.5f, startY + btnH)
        btnSettings.set(cx - btnW * 0.5f, startY + btnH + spacing, cx + btnW * 0.5f, startY + (btnH + spacing) * 2 - spacing)
        btnAbout.set(cx - btnW * 0.5f, startY + (btnH + spacing) * 2, cx + btnW * 0.5f, startY + (btnH + spacing) * 3 - spacing)

        drawArcadeButton(canvas, btnStart, "START", Color.parseColor("#10B981"), Color.parseColor("#047857"), true, 24f)
        drawArcadeButton(canvas, btnSettings, "SETTINGS", Color.parseColor("#3B82F6"), Color.parseColor("#1D4ED8"), false, 23f)
        drawArcadeButton(canvas, btnAbout, "ABOUT", Color.parseColor("#64748B"), Color.parseColor("#334155"), false, 23f)

        // Footer version
        textPaint.color = Color.parseColor("#94A3B8")
        textPaint.textSize = 15f
        canvas.drawText("v1.0  |  Developed by Argho Biswas  |  Arcade Edition", cx, height - 16f, textPaint)
    }

    // ==========================================
    // 3. HUD (HEADS UP DISPLAY)
    // ==========================================
    fun drawHUD(
        canvas: Canvas,
        width: Float,
        height: Float,
        score: Int,
        player: PlayerCar,
        currentStage: Int,
        timeRemaining: Float,
        comboMultiplier: Float,
        showTutorialHint: Boolean
    ) {
        val pad = 16f

        // Top-Left: SCORE
        panelPaint.color = Color.argb(190, 15, 23, 42)
        val scoreBoxW = 210f
        val scoreBoxH = 72f
        val scoreBox = RectF(pad, pad, pad + scoreBoxW, pad + scoreBoxH)
        canvas.drawRoundRect(scoreBox, 12f, 12f, panelPaint)
        borderPaint.color = Color.parseColor("#EF4444")
        canvas.drawRoundRect(scoreBox, 12f, 12f, borderPaint)

        textPaint.color = Color.parseColor("#94A3B8")
        textPaint.textSize = 14f
        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("SCORE", pad + 14f, pad + 24f, textPaint)

        textPaint.color = Color.WHITE
        textPaint.textSize = 28f
        canvas.drawText(String.format("%,d", score), pad + 14f, pad + 56f, textPaint)

        // Multiplier badge if > 1.0
        if (comboMultiplier > 1.05f) {
            textPaint.color = Color.parseColor("#FACC15")
            textPaint.textSize = 17f
            canvas.drawText(String.format("%.1fx", comboMultiplier), pad + 150f, pad + 56f, textPaint)
        }

        // Top-Center: TIME & STAGE PROGRESS BAR
        val timeBoxW = 240f
        val timeBoxH = 72f
        val timeBox = RectF(width * 0.5f - timeBoxW * 0.5f, pad, width * 0.5f + timeBoxW * 0.5f, pad + timeBoxH)
        canvas.drawRoundRect(timeBox, 12f, 12f, panelPaint)
        borderPaint.color = Color.parseColor("#F59E0B")
        canvas.drawRoundRect(timeBox, 12f, 12f, borderPaint)

        textPaint.textAlign = Paint.Align.CENTER
        textPaint.color = Color.parseColor("#94A3B8")
        textPaint.textSize = 13f
        canvas.drawText("SURVIVE TO TUNNEL", width * 0.5f, pad + 20f, textPaint)

        val secondsInt = timeRemaining.toInt()
        val timeColor = if (timeRemaining <= 10f) Color.parseColor("#EF4444") else Color.parseColor("#38BDF8")
        drawArcadeText(canvas, "${secondsInt}s", width * 0.5f, pad + 48f, 28f, timeColor, Color.BLACK)

        // REQ 9: STAGE PROGRESS INDICATOR [████████░░░░]
        val progressFraction = stageManager.progressFraction
        val pBarW = timeBoxW - 32f
        val pBarH = 8f
        val pBarX = timeBox.left + 16f
        val pBarY = timeBox.bottom - 16f

        panelPaint.color = Color.parseColor("#334155")
        canvas.drawRoundRect(pBarX, pBarY, pBarX + pBarW, pBarY + pBarH, 4f, 4f, panelPaint)

        panelPaint.color = if (timeRemaining <= 10f) Color.parseColor("#EF4444") else Color.parseColor("#10B981")
        canvas.drawRoundRect(pBarX, pBarY, pBarX + pBarW * progressFraction, pBarY + pBarH, 4f, 4f, panelPaint)

        // Top-Right: STAGE & PAUSE BUTTON (REQ 12: Large easy-to-tap touch area)
        val pauseBtnSize = 62f
        btnPause.set(width - pad - pauseBtnSize, pad + 5f, width - pad, pad + 5f + pauseBtnSize)
        panelPaint.color = Color.argb(220, 30, 41, 59)
        canvas.drawRoundRect(btnPause, 12f, 12f, panelPaint)
        borderPaint.color = Color.parseColor("#94A3B8")
        borderPaint.strokeWidth = 3f
        canvas.drawRoundRect(btnPause, 12f, 12f, borderPaint)

        // Pause icon (two bold vertical bars)
        val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        val barW = 6f
        val barH = 26f
        val barY = btnPause.centerY() - barH * 0.5f
        canvas.drawRoundRect(btnPause.centerX() - 9f, barY, btnPause.centerX() - 9f + barW, barY + barH, 3f, 3f, barPaint)
        canvas.drawRoundRect(btnPause.centerX() + 3f, barY, btnPause.centerX() + 3f + barW, barY + barH, 3f, 3f, barPaint)

        // STAGE info box to the left of pause button
        val stageBoxW = 120f
        val stageBox = RectF(btnPause.left - 12f - stageBoxW, pad, btnPause.left - 12f, pad + timeBoxH)
        panelPaint.color = Color.argb(190, 15, 23, 42)
        canvas.drawRoundRect(stageBox, 12f, 12f, panelPaint)
        borderPaint.color = Color.parseColor("#3B82F6")
        canvas.drawRoundRect(stageBox, 12f, 12f, borderPaint)

        textPaint.color = Color.parseColor("#94A3B8")
        textPaint.textSize = 14f
        canvas.drawText("STAGE", stageBox.centerX(), pad + 24f, textPaint)

        textPaint.color = Color.WHITE
        textPaint.textSize = 28f
        canvas.drawText("$currentStage", stageBox.centerX(), pad + 56f, textPaint)

        // Speedometer Gauge (Bottom-Left)
        drawSpeedometer(canvas, pad, height - 105f, player.speedKmH, player.nitroAmount, player.isBoosting)

        // REQ 8: FIRST-TIME CONTROL HINT
        if (showTutorialHint) {
            drawTutorialBanner(canvas, width, height)
        }

        // On-screen Virtual Buttons (if configured in Settings)
        if (settings.controlScheme == ControlScheme.ON_SCREEN_BUTTONS) {
            drawVirtualButtons(canvas, width, height, player)
        } else {
            // Dedicated Quick Nitro button on bottom right for swipe/tilt modes
            val nitroBtnW = 95f
            val nitroBtnH = 55f
            val bottomMargin = 20f
            btnQuickNitro.set(width - pad - nitroBtnW, height - bottomMargin - nitroBtnH, width - pad, height - bottomMargin)
            drawArcadeButton(canvas, btnQuickNitro, "NITRO", Color.parseColor("#06B6D4"), Color.parseColor("#0891B2"), player.isBoosting, 19f)
        }
    }

    fun drawSpeedLines(canvas: Canvas, width: Float, height: Float, roadLeft: Float, roadRight: Float, speedKmH: Float, isBoosting: Boolean) {
        if (speedKmH < 120f) return
        val intensity = ((speedKmH - 120f) / 100f).coerceIn(0f, 1f)

        if (isBoosting) {
            val boostGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 10f
                color = Color.argb(45, 6, 182, 212)
            }
            canvas.drawRect(5f, 5f, width - 5f, height - 5f, boostGlow)
        }

        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
            color = if (isBoosting) Color.argb((intensity * 200).toInt(), 125, 211, 252) else Color.argb((intensity * 140).toInt(), 255, 255, 255)
        }

        val streakCount = (10 + intensity * 14).toInt()
        val streakLen = 45f + intensity * 120f

        for (i in 0 until streakCount) {
            val lx = 10f + ((i * 37) % (roadLeft.toInt().coerceAtLeast(20))).toFloat()
            val ly = ((animTimer * (speedKmH * 5.5f) + i * 85) % (height + streakLen)) - streakLen
            canvas.drawLine(lx, ly, lx, ly + streakLen, linePaint)

            val rx = roadRight + 10f + ((i * 41) % ((width - roadRight).toInt().coerceAtLeast(20))).toFloat()
            val ry = ((animTimer * (speedKmH * 5.5f) + i * 95 + 40) % (height + streakLen)) - streakLen
            canvas.drawLine(rx, ry, rx, ry + streakLen, linePaint)
        }
    }

    private fun drawTutorialBanner(canvas: Canvas, width: Float, height: Float) {
        val cx = width * 0.5f
        val cy = height * 0.55f
        val bannerW = (width * 0.70f).coerceIn(340f, 520f)
        val bannerH = 75f

        panelPaint.color = Color.argb(220, 15, 23, 42)
        canvas.drawRoundRect(cx - bannerW * 0.5f, cy, cx + bannerW * 0.5f, cy + bannerH, 14f, 14f, panelPaint)
        borderPaint.color = Color.parseColor("#38BDF8")
        borderPaint.strokeWidth = 3f
        canvas.drawRoundRect(cx - bannerW * 0.5f, cy, cx + bannerW * 0.5f, cy + bannerH, 14f, 14f, borderPaint)

        textPaint.textAlign = Paint.Align.CENTER
        textPaint.color = Color.parseColor("#FDE047")
        textPaint.textSize = 18f
        val wobble = sin(animTimer * 5f) * 2f
        canvas.drawText("◄ SWIPE LEFT / RIGHT TO CHANGE LANES ►", cx, cy + 30f + wobble, textPaint)

        textPaint.color = Color.WHITE
        textPaint.textSize = 15f
        canvas.drawText("Dodge incoming cars & survive until the tunnel!", cx, cy + 56f, textPaint)
    }

    private fun drawSpeedometer(canvas: Canvas, x: Float, y: Float, speedKmH: Float, nitroAmount: Float, isBoosting: Boolean) {
        val gaugeW = 195f
        val gaugeH = 92f
        val gaugeBox = RectF(x, y, x + gaugeW, y + gaugeH)

        panelPaint.color = Color.argb(195, 15, 23, 42)
        canvas.drawRoundRect(gaugeBox, 14f, 14f, panelPaint)
        borderPaint.color = if (isBoosting) Color.parseColor("#38BDF8") else Color.parseColor("#64748B")
        borderPaint.strokeWidth = 2.5f
        canvas.drawRoundRect(gaugeBox, 14f, 14f, borderPaint)

        textPaint.textAlign = Paint.Align.LEFT
        textPaint.color = Color.parseColor("#94A3B8")
        textPaint.textSize = 13f
        canvas.drawText("SPEED", x + 14f, y + 22f, textPaint)

        val speedInt = speedKmH.toInt()
        val speedColor = when {
            isBoosting -> Color.parseColor("#38BDF8")
            speedInt > 180 -> Color.parseColor("#EF4444")
            speedInt > 135 -> Color.parseColor("#FACC15")
            else -> Color.WHITE
        }
        drawArcadeText(canvas, "$speedInt", x + 14f + 32f, y + 54f, 32f, speedColor, Color.BLACK)

        textPaint.color = Color.parseColor("#94A3B8")
        textPaint.textSize = 14f
        canvas.drawText("KM/H", x + 14f + 84f, y + 54f, textPaint)

        // Speed level bar
        val barW = gaugeW - 28f
        val ratio = (speedKmH / 270f).coerceIn(0f, 1f)
        panelPaint.color = Color.parseColor("#334155")
        canvas.drawRoundRect(x + 14f, y + 64f, x + 14f + barW, y + 70f, 3f, 3f, panelPaint)
        panelPaint.color = speedColor
        canvas.drawRoundRect(x + 14f, y + 64f, x + 14f + barW * ratio, y + 70f, 3f, 3f, panelPaint)

        // Nitro level bar
        val nitroRatio = nitroAmount.coerceIn(0f, 1f)
        val nitroBarW = barW * 0.75f
        panelPaint.color = Color.parseColor("#1E293B")
        canvas.drawRoundRect(x + 14f, y + 76f, x + 14f + nitroBarW, y + 83f, 3f, 3f, panelPaint)
        panelPaint.color = Color.parseColor("#06B6D4")
        canvas.drawRoundRect(x + 14f, y + 76f, x + 14f + nitroBarW * nitroRatio, y + 83f, 3f, 3f, panelPaint)

        textPaint.color = Color.parseColor("#38BDF8")
        textPaint.textSize = 10f
        canvas.drawText("NOS", x + 14f + nitroBarW + 6f, y + 83f, textPaint)
    }


    private fun drawVirtualButtons(canvas: Canvas, width: Float, height: Float, player: PlayerCar) {
        val btnSize = 80f
        val bottomMargin = 20f

        // Left & Right steering on bottom-left
        btnLeft.set(24f, height - bottomMargin - btnSize, 24f + btnSize, height - bottomMargin)
        btnRight.set(24f + btnSize + 18f, height - bottomMargin - btnSize, 24f + btnSize * 2 + 18f, height - bottomMargin)

        // Boost & Brake on bottom-right
        btnBrake.set(width - 24f - btnSize * 2 - 18f, height - bottomMargin - btnSize, width - 24f - btnSize - 18f, height - bottomMargin)
        btnBoost.set(width - 24f - btnSize, height - bottomMargin - btnSize, width - 24f, height - bottomMargin)

        drawArcadeButton(canvas, btnLeft, "◀", Color.parseColor("#3B82F6"), Color.parseColor("#1D4ED8"), false, 28f)
        drawArcadeButton(canvas, btnRight, "▶", Color.parseColor("#3B82F6"), Color.parseColor("#1D4ED8"), false, 28f)
        drawArcadeButton(canvas, btnBrake, "SLOW", Color.parseColor("#F59E0B"), Color.parseColor("#B45309"), player.isBraking, 18f)
        drawArcadeButton(canvas, btnBoost, "NITRO", Color.parseColor("#06B6D4"), Color.parseColor("#0E7490"), player.isBoosting, 18f)
    }

    // ==========================================
    // 4. CRASH & POLICE CINEMATIC
    // ==========================================
    fun drawCrashSequence(canvas: Canvas, width: Float, height: Float, sequenceTime: Float) {
        val cx = width * 0.5f

        // Strobe flash overlay at start
        if (sequenceTime < 0.35f) {
            val flashAlpha = ((0.35f - sequenceTime) / 0.35f * 220).toInt()
            overlayPaint.color = Color.argb(flashAlpha, 255, 255, 255)
            canvas.drawRect(0f, 0f, width, height, overlayPaint)
        }

        // Alternating red/blue emergency light bars at top & bottom of screen
        val isRed = (animTimer * 10f).toInt() % 2 == 0
        val alertColor = if (isRed) Color.argb(70, 239, 68, 68) else Color.argb(70, 59, 130, 246)
        overlayPaint.color = alertColor
        canvas.drawRect(0f, 0f, width, 28f, overlayPaint)
        canvas.drawRect(0f, height - 28f, width, height, overlayPaint)

        // Banner after 1.0 second: YOU GOT CAUGHT!
        if (sequenceTime > 0.9f) {
            val bannerAlpha = ((sequenceTime - 0.9f) * 2f).coerceIn(0f, 1f)
            overlayPaint.color = Color.argb((bannerAlpha * 230).toInt(), 15, 23, 42)
            val bannerH = 120f
            val bannerY = height * 0.34f
            canvas.drawRect(0f, bannerY, width, bannerY + bannerH, overlayPaint)

            borderPaint.color = Color.parseColor("#EF4444")
            borderPaint.strokeWidth = 4f
            canvas.drawLine(0f, bannerY, width, bannerY, borderPaint)
            canvas.drawLine(0f, bannerY + bannerH, width, bannerY + bannerH, borderPaint)

            drawArcadeText(canvas, "YOU GOT CAUGHT!", cx, bannerY + 52f, 42f, Color.parseColor("#EF4444"), Color.parseColor("#7F1D1D"))
            textPaint.color = Color.WHITE
            textPaint.textSize = 20f
            textPaint.textAlign = Paint.Align.CENTER
            canvas.drawText("HIGHWAY PATROL INTERCEPTED YOUR VEHICLE", cx, bannerY + 90f, textPaint)
        }
    }

    // ==========================================
    // 5. GAME OVER SCREEN
    // ==========================================
    fun drawGameOver(canvas: Canvas, width: Float, height: Float, finalScore: Int, distanceMeters: Float, timeSurvived: Float, isNewHighScore: Boolean) {
        val cx = width * 0.5f

        overlayPaint.color = Color.argb(235, 15, 23, 42)
        canvas.drawRect(0f, 0f, width, height, overlayPaint)

        val cardW = (width * 0.78f).coerceIn(340f, 540f)
        val cardH = (height * 0.88f).coerceIn(320f, 500f)
        val cardX = cx - cardW * 0.5f
        val cardY = (height - cardH) * 0.5f

        panelPaint.color = Color.parseColor("#1E293B")
        canvas.drawRoundRect(cardX, cardY, cardX + cardW, cardY + cardH, 22f, 22f, panelPaint)
        borderPaint.color = Color.parseColor("#EF4444")
        borderPaint.strokeWidth = 4f
        canvas.drawRoundRect(cardX, cardY, cardX + cardW, cardY + cardH, 22f, 22f, borderPaint)

        // Banner: CAUGHT!
        drawArcadeText(canvas, "CAUGHT!", cx, cardY + 62f, 52f, Color.parseColor("#EF4444"), Color.parseColor("#991B1B"))

        if (isNewHighScore) {
            val wobble = sin(animTimer * 6f) * 3f
            drawArcadeText(canvas, "★ NEW HIGH SCORE! ★", cx, cardY + 98f + wobble, 20f, Color.parseColor("#FACC15"), Color.parseColor("#78350F"))
        }

        // Stats summary list
        val statStartY = cardY + (if (isNewHighScore) 135f else 125f)
        val statSpacing = 40f

        drawStatRow(canvas, cardX + 36f, cardX + cardW - 36f, statStartY, "FINAL SCORE", String.format("%,d", finalScore), Color.parseColor("#FDE047"))
        drawStatRow(canvas, cardX + 36f, cardX + cardW - 36f, statStartY + statSpacing, "TIME SURVIVED", String.format("%.1fs", timeSurvived), Color.WHITE)
        drawStatRow(canvas, cardX + 36f, cardX + cardW - 36f, statStartY + statSpacing * 2, "DISTANCE", String.format("%.0fm", distanceMeters), Color.WHITE)
        drawStatRow(canvas, cardX + 36f, cardX + cardW - 36f, statStartY + statSpacing * 3, "HIGH SCORE", String.format("%,d", settings.highScore), Color.parseColor("#38BDF8"))

        // Buttons: RESTART & MAIN MENU
        val btnW = (cardW - 88f) * 0.48f
        val btnH = 58f
        val btnY = cardY + cardH - 78f

        btnRestart.set(cardX + 32f, btnY, cardX + 32f + btnW, btnY + btnH)
        btnMenu.set(cardX + cardW - 32f - btnW, btnY, cardX + cardW - 32f, btnY + btnH)

        drawArcadeButton(canvas, btnRestart, "RESTART", Color.parseColor("#10B981"), Color.parseColor("#047857"), true, 22f)
        drawArcadeButton(canvas, btnMenu, "MAIN MENU", Color.parseColor("#64748B"), Color.parseColor("#334155"), false, 22f)
    }

    // ==========================================
    // 6. STAGE COMPLETE SCREEN
    // ==========================================
    fun drawStageComplete(canvas: Canvas, width: Float, height: Float, stageNumber: Int, score: Int, stageBonus: Int) {
        val cx = width * 0.5f

        overlayPaint.color = Color.argb(235, 15, 23, 42)
        canvas.drawRect(0f, 0f, width, height, overlayPaint)

        val cardW = (width * 0.78f).coerceIn(340f, 540f)
        val cardH = (height * 0.86f).coerceIn(320f, 480f)
        val cardX = cx - cardW * 0.5f
        val cardY = (height - cardH) * 0.5f

        panelPaint.color = Color.parseColor("#1E293B")
        canvas.drawRoundRect(cardX, cardY, cardX + cardW, cardY + cardH, 22f, 22f, panelPaint)
        borderPaint.color = Color.parseColor("#10B981")
        borderPaint.strokeWidth = 4f
        canvas.drawRoundRect(cardX, cardY, cardX + cardW, cardY + cardH, 22f, 22f, borderPaint)

        drawArcadeText(canvas, "STAGE $stageNumber COMPLETE!", cx, cardY + 62f, 40f, Color.parseColor("#34D399"), Color.parseColor("#065F46"))
        textPaint.color = Color.parseColor("#94A3B8")
        textPaint.textSize = 18f
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("YOU REACHED THE HIGHWAY TUNNEL SAFELY!", cx, cardY + 98f, textPaint)

        val statStartY = cardY + 140f
        val statSpacing = 40f
        drawStatRow(canvas, cardX + 36f, cardX + cardW - 36f, statStartY, "STAGE CLEAR BONUS", "+$stageBonus", Color.parseColor("#34D399"))
        drawStatRow(canvas, cardX + 36f, cardX + cardW - 36f, statStartY + statSpacing, "TOTAL SCORE", String.format("%,d", score), Color.parseColor("#FDE047"))
        drawStatRow(canvas, cardX + 36f, cardX + cardW - 36f, statStartY + statSpacing * 2, "NEXT DESTINATION", stageManager.currentStage.name, Color.parseColor("#38BDF8"))

        val btnW = (cardW - 88f) * 0.48f
        val btnH = 58f
        val btnY = cardY + cardH - 78f

        btnNextStage.set(cardX + 32f, btnY, cardX + 32f + btnW, btnY + btnH)
        btnMenu.set(cardX + cardW - 32f - btnW, btnY, cardX + cardW - 32f, btnY + btnH)

        drawArcadeButton(canvas, btnNextStage, "CONTINUE", Color.parseColor("#10B981"), Color.parseColor("#047857"), true, 22f)
        drawArcadeButton(canvas, btnMenu, "MAIN MENU", Color.parseColor("#64748B"), Color.parseColor("#334155"), false, 22f)
    }

    // ==========================================
    // 7. GAME COMPLETE (CHAMPION) SCREEN
    // ==========================================
    fun drawGameComplete(canvas: Canvas, width: Float, height: Float, finalScore: Int) {
        val cx = width * 0.5f

        overlayPaint.color = Color.argb(240, 15, 23, 42)
        canvas.drawRect(0f, 0f, width, height, overlayPaint)

        val cardW = (width * 0.80f).coerceIn(360f, 560f)
        val cardH = (height * 0.88f).coerceIn(320f, 500f)
        val cardX = cx - cardW * 0.5f
        val cardY = (height - cardH) * 0.5f

        panelPaint.color = Color.parseColor("#1E293B")
        canvas.drawRoundRect(cardX, cardY, cardX + cardW, cardY + cardH, 22f, 22f, panelPaint)
        borderPaint.color = Color.parseColor("#F59E0B")
        borderPaint.strokeWidth = 4f
        canvas.drawRoundRect(cardX, cardY, cardX + cardW, cardY + cardH, 22f, 22f, borderPaint)

        drawArcadeText(canvas, "★ GAME COMPLETE ★", cx, cardY + 62f, 42f, Color.parseColor("#FBBF24"), Color.parseColor("#92400E"))
        textPaint.color = Color.parseColor("#38BDF8")
        textPaint.textSize = 22f
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("ULTIMATE HIGHWAY RUNAWAY MASTER!", cx, cardY + 102f, textPaint)

        textPaint.color = Color.WHITE
        textPaint.textSize = 17f
        canvas.drawText("You conquered all 3 stages against oncoming traffic!", cx, cardY + 140f, textPaint)

        val statStartY = cardY + 185f
        drawStatRow(canvas, cardX + 36f, cardX + cardW - 36f, statStartY, "FINAL TOTAL SCORE", String.format("%,d", finalScore), Color.parseColor("#FDE047"))
        drawStatRow(canvas, cardX + 36f, cardX + cardW - 36f, statStartY + 45f, "ALL-TIME HIGH SCORE", String.format("%,d", settings.highScore), Color.parseColor("#34D399"))

        val btnW = (cardW * 0.55f).coerceIn(240f, 320f)
        val btnH = 58f
        val btnY = cardY + cardH - 78f
        btnMenu.set(cx - btnW * 0.5f, btnY, cx + btnW * 0.5f, btnY + btnH)
        drawArcadeButton(canvas, btnMenu, "MAIN MENU", Color.parseColor("#3B82F6"), Color.parseColor("#1D4ED8"), true, 22f)
    }

    // ==========================================
    // 8. PAUSE SCREEN
    // ==========================================
    fun drawPause(canvas: Canvas, width: Float, height: Float) {
        val cx = width * 0.5f

        overlayPaint.color = Color.argb(220, 15, 23, 42)
        canvas.drawRect(0f, 0f, width, height, overlayPaint)

        val cardW = (width * 0.55f).coerceIn(320f, 440f)
        val cardH = (height * 0.85f).coerceIn(360f, 480f)
        val cardX = cx - cardW * 0.5f
        val cardY = (height - cardH) * 0.5f

        panelPaint.color = Color.parseColor("#1E293B")
        canvas.drawRoundRect(cardX, cardY, cardX + cardW, cardY + cardH, 20f, 20f, panelPaint)
        borderPaint.color = Color.parseColor("#38BDF8")
        borderPaint.strokeWidth = 3f
        canvas.drawRoundRect(cardX, cardY, cardX + cardW, cardY + cardH, 20f, 20f, borderPaint)

        drawArcadeText(canvas, "PAUSED", cx, cardY + 54f, 42f, Color.parseColor("#38BDF8"), Color.parseColor("#0369A1"))

        val btnW = cardW - 60f
        val btnH = 52f
        val spacing = 14f
        var curY = cardY + 88f

        btnResume.set(cx - btnW * 0.5f, curY, cx + btnW * 0.5f, curY + btnH)
        curY += btnH + spacing

        btnRestart.set(cx - btnW * 0.5f, curY, cx + btnW * 0.5f, curY + btnH)
        curY += btnH + spacing

        btnSettings.set(cx - btnW * 0.5f, curY, cx + btnW * 0.5f, curY + btnH)
        curY += btnH + spacing

        btnMenu.set(cx - btnW * 0.5f, curY, cx + btnW * 0.5f, curY + btnH)

        drawArcadeButton(canvas, btnResume, "RESUME", Color.parseColor("#10B981"), Color.parseColor("#047857"), true, 22f)
        drawArcadeButton(canvas, btnRestart, "RESTART", Color.parseColor("#F59E0B"), Color.parseColor("#B45309"), false, 21f)
        drawArcadeButton(canvas, btnSettings, "SETTINGS", Color.parseColor("#3B82F6"), Color.parseColor("#1D4ED8"), false, 21f)
        drawArcadeButton(canvas, btnMenu, "MAIN MENU", Color.parseColor("#64748B"), Color.parseColor("#334155"), false, 21f)
    }

    // ==========================================
    // 9. SETTINGS SCREEN
    // ==========================================
    fun drawSettings(canvas: Canvas, width: Float, height: Float) {
        val cx = width * 0.5f

        overlayPaint.color = Color.parseColor("#0F172A")
        canvas.drawRect(0f, 0f, width, height, overlayPaint)

        val cardW = (width * 0.90f).coerceIn(360f, 660f)
        val cardH = (height * 0.92f).coerceIn(360f, 540f)
        val cardX = cx - cardW * 0.5f
        val cardY = (height - cardH) * 0.5f

        panelPaint.color = Color.parseColor("#1E293B")
        canvas.drawRoundRect(cardX, cardY, cardX + cardW, cardY + cardH, 22f, 22f, panelPaint)
        borderPaint.color = Color.parseColor("#3B82F6")
        borderPaint.strokeWidth = 3f
        canvas.drawRoundRect(cardX, cardY, cardX + cardW, cardY + cardH, 22f, 22f, borderPaint)

        drawArcadeText(canvas, "SETTINGS", cx, cardY + 48f, 38f, Color.parseColor("#38BDF8"), Color.parseColor("#0369A1"))

        var rowY = cardY + 75f
        val rowH = 44f
        val leftX = cardX + 28f
        val rightX = cardX + cardW - 28f

        // 1. Audio Master Volume
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.color = Color.WHITE
        textPaint.textSize = 19f
        canvas.drawText("Audio Volume", leftX, rowY + 28f, textPaint)

        val stepBtnW = 44f
        btnMasterDown.set(rightX - 180f, rowY, rightX - 180f + stepBtnW, rowY + rowH)
        btnMasterUp.set(rightX - stepBtnW, rowY, rightX, rowY + rowH)

        drawArcadeButton(canvas, btnMasterDown, "-", Color.parseColor("#475569"), Color.BLACK, false, 24f)
        drawArcadeButton(canvas, btnMasterUp, "+", Color.parseColor("#475569"), Color.BLACK, false, 24f)

        textPaint.textAlign = Paint.Align.CENTER
        textPaint.color = Color.parseColor("#FDE047")
        textPaint.textSize = 20f
        val volPct = (settings.masterVolume * 100).toInt()
        canvas.drawText("$volPct%", rightX - 180f + stepBtnW + 46f, rowY + 30f, textPaint)

        // 2. Mute Toggle
        rowY += rowH + 10f
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.color = Color.WHITE
        canvas.drawText("Mute Audio", leftX, rowY + 28f, textPaint)

        btnMuteToggle.set(rightX - 140f, rowY, rightX, rowY + rowH)
        val muteText = if (settings.isMuted) "MUTED" else "SOUND ON"
        val muteColor = if (settings.isMuted) Color.parseColor("#EF4444") else Color.parseColor("#10B981")
        drawArcadeButton(canvas, btnMuteToggle, muteText, muteColor, Color.BLACK, false, 18f)

        // 3. REQ 13: Vibration / Haptic Feedback Toggle
        rowY += rowH + 10f
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.color = Color.WHITE
        canvas.drawText("Vibration / Haptics", leftX, rowY + 28f, textPaint)

        btnVibrationToggle.set(rightX - 140f, rowY, rightX, rowY + rowH)
        val vibText = if (settings.vibrationEnabled) "ENABLED" else "DISABLED"
        val vibColor = if (settings.vibrationEnabled) Color.parseColor("#10B981") else Color.parseColor("#64748B")
        drawArcadeButton(canvas, btnVibrationToggle, vibText, vibColor, Color.BLACK, false, 18f)

        // 4. In-Game Brightness
        rowY += rowH + 10f
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.color = Color.WHITE
        canvas.drawText("Brightness", leftX, rowY + 28f, textPaint)

        btnBrightnessDown.set(rightX - 180f, rowY, rightX - 180f + stepBtnW, rowY + rowH)
        btnBrightnessUp.set(rightX - stepBtnW, rowY, rightX, rowY + rowH)

        drawArcadeButton(canvas, btnBrightnessDown, "-", Color.parseColor("#475569"), Color.BLACK, false, 24f)
        drawArcadeButton(canvas, btnBrightnessUp, "+", Color.parseColor("#475569"), Color.BLACK, false, 24f)

        textPaint.textAlign = Paint.Align.CENTER
        textPaint.color = Color.parseColor("#FDE047")
        textPaint.textSize = 20f
        val brightPct = (settings.brightness * 100).toInt()
        canvas.drawText("$brightPct%", rightX - 180f + stepBtnW + 46f, rowY + 30f, textPaint)

        // 5. Control Scheme
        rowY += rowH + 10f
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.color = Color.WHITE
        canvas.drawText("Controls", leftX, rowY + 28f, textPaint)

        btnControlsToggle.set(rightX - 200f, rowY, rightX, rowY + rowH)
        val ctrlName = when (settings.controlScheme) {
            ControlScheme.TOUCH_DRAG -> "TOUCH DRAG"
            ControlScheme.SWIPE_LANES -> "SWIPE LANES"
            ControlScheme.ON_SCREEN_BUTTONS -> "BUTTONS"
        }
        drawArcadeButton(canvas, btnControlsToggle, ctrlName, Color.parseColor("#3B82F6"), Color.BLACK, false, 18f)

        // 6. Difficulty
        rowY += rowH + 10f
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.color = Color.WHITE
        canvas.drawText("Difficulty", leftX, rowY + 28f, textPaint)

        btnDifficultyToggle.set(rightX - 160f, rowY, rightX, rowY + rowH)
        val diffName = settings.difficulty.name
        val diffColor = when (settings.difficulty) {
            Difficulty.EASY -> Color.parseColor("#10B981")
            Difficulty.NORMAL -> Color.parseColor("#F59E0B")
            Difficulty.HARDCORE -> Color.parseColor("#EF4444")
        }
        drawArcadeButton(canvas, btnDifficultyToggle, diffName, diffColor, Color.BLACK, false, 18f)

        // Back Button
        val backW = (cardW * 0.45f).coerceIn(200f, 260f)
        val backH = 50f
        val backY = cardY + cardH - 64f
        btnSettingsBack.set(cx - backW * 0.5f, backY, cx + backW * 0.5f, backY + backH)
        drawArcadeButton(canvas, btnSettingsBack, "BACK", Color.parseColor("#64748B"), Color.parseColor("#334155"), true, 22f)
    }

    // ==========================================
    // 10. ABOUT SCREEN
    // ==========================================
    fun drawAbout(canvas: Canvas, width: Float, height: Float) {
        val cx = width * 0.5f

        overlayPaint.color = Color.parseColor("#0F172A")
        canvas.drawRect(0f, 0f, width, height, overlayPaint)

        val cardW = (width * 0.88f).coerceIn(360f, 640f)
        val cardH = (height * 0.90f).coerceIn(360f, 520f)
        val cardX = cx - cardW * 0.5f
        val cardY = (height - cardH) * 0.5f

        panelPaint.color = Color.parseColor("#1E293B")
        canvas.drawRoundRect(cardX, cardY, cardX + cardW, cardY + cardH, 22f, 22f, panelPaint)
        borderPaint.color = Color.parseColor("#F59E0B")
        borderPaint.strokeWidth = 3f
        canvas.drawRoundRect(cardX, cardY, cardX + cardW, cardY + cardH, 22f, 22f, borderPaint)

        drawArcadeText(canvas, "GRAND THEFT CAR", cx, cardY + 52f, 38f, Color.parseColor("#EF4444"), Color.parseColor("#991B1B"))
        drawArcadeText(canvas, "RETRO ARCADE HIGHWAY RACER", cx, cardY + 84f, 18f, Color.parseColor("#FBBF24"), Color.parseColor("#78350F"))

        textPaint.textAlign = Paint.Align.CENTER
        textPaint.color = Color.WHITE
        textPaint.textSize = 17f

        val descY = cardY + 130f
        canvas.drawText("You are driving the wrong way on a dangerous highway.", cx, descY, textPaint)
        canvas.drawText("Avoid incoming traffic, survive as long as possible,", cx, descY + 28f, textPaint)
        canvas.drawText("and reach the tunnel without getting caught.", cx, descY + 56f, textPaint)

        // Divider
        borderPaint.color = Color.parseColor("#334155")
        canvas.drawLine(cardX + 40f, descY + 85f, cardX + cardW - 40f, descY + 85f, borderPaint)

        val infoY = descY + 120f
        textPaint.color = Color.parseColor("#38BDF8")
        textPaint.textSize = 18f
        canvas.drawText("Game Version 1.0", cx, infoY, textPaint)

        textPaint.color = Color.parseColor("#FDE047")
        textPaint.textSize = 21f
        canvas.drawText("Developed by Argho Biswas", cx, infoY + 34f, textPaint)

        textPaint.color = Color.parseColor("#94A3B8")
        textPaint.textSize = 15f
        canvas.drawText("© 2026 All Rights Reserved  •  Arcade Top-Down Engine", cx, infoY + 66f, textPaint)

        val backW = (cardW * 0.45f).coerceIn(200f, 260f)
        val backH = 50f
        val backY = cardY + cardH - 64f
        btnAboutBack.set(cx - backW * 0.5f, backY, cx + backW * 0.5f, backY + backH)
        drawArcadeButton(canvas, btnAboutBack, "BACK", Color.parseColor("#64748B"), Color.parseColor("#334155"), true, 22f)
    }

    // ==========================================
    // HELPER RENDERING METHODS
    // ==========================================
    fun drawArcadeButton(
        canvas: Canvas,
        rect: RectF,
        text: String,
        baseColor: Int,
        darkColor: Int,
        highlight: Boolean,
        fontSize: Float = 22f
    ) {
        val corner = 12f

        // Shadow bottom
        panelPaint.color = darkColor
        canvas.drawRoundRect(rect.left, rect.top + 4f, rect.right, rect.bottom + 4f, corner, corner, panelPaint)

        // Face
        panelPaint.color = baseColor
        canvas.drawRoundRect(rect, corner, corner, panelPaint)

        // Highlight border
        borderPaint.color = if (highlight) Color.WHITE else Color.argb(130, 255, 255, 255)
        borderPaint.strokeWidth = if (highlight) 3.5f else 2f
        canvas.drawRoundRect(rect, corner, corner, borderPaint)

        // Text
        shadowTextPaint.textSize = fontSize
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = fontSize
        textPaint.color = Color.WHITE

        val textY = rect.centerY() + fontSize * 0.35f
        canvas.drawText(text, rect.centerX() + 2f, textY + 2f, shadowTextPaint)
        canvas.drawText(text, rect.centerX(), textY, textPaint)
    }

    private fun drawArcadeText(canvas: Canvas, text: String, x: Float, y: Float, size: Float, topColor: Int, bottomColor: Int) {
        shadowTextPaint.textSize = size
        textPaint.textSize = size

        // Drop shadow
        canvas.drawText(text, x + 3.5f, y + 3.5f, shadowTextPaint)

        // Gradient or fill text
        textPaint.color = topColor
        canvas.drawText(text, x, y, textPaint)
    }

    private fun drawStatRow(canvas: Canvas, leftX: Float, rightX: Float, y: Float, label: String, value: String, valueColor: Int) {
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.color = Color.parseColor("#94A3B8")
        textPaint.textSize = 18f
        canvas.drawText(label, leftX, y, textPaint)

        textPaint.textAlign = Paint.Align.RIGHT
        textPaint.color = valueColor
        textPaint.textSize = 21f
        canvas.drawText(value, rightX, y, textPaint)
    }

    fun drawBrightnessOverlay(canvas: Canvas, width: Float, height: Float) {
        val brightness = settings.brightness
        if (brightness < 0.98f) {
            val alpha = ((1.0f - brightness) * 200).toInt().coerceIn(0, 220)
            overlayPaint.color = Color.argb(alpha, 0, 0, 0)
            canvas.drawRect(0f, 0f, width, height, overlayPaint)
        } else if (brightness > 1.02f) {
            val alpha = ((brightness - 1.0f) * 120).toInt().coerceIn(0, 160)
            overlayPaint.color = Color.argb(alpha, 255, 255, 255)
            canvas.drawRect(0f, 0f, width, height, overlayPaint)
        }
    }
}
