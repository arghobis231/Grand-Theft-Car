package com.example.grandtheftcar.engine

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.grandtheftcar.managers.AudioManager
import com.example.grandtheftcar.managers.CollectibleManager
import com.example.grandtheftcar.managers.CollisionManager
import com.example.grandtheftcar.managers.ControlScheme
import com.example.grandtheftcar.managers.Difficulty
import com.example.grandtheftcar.managers.SettingsManager
import com.example.grandtheftcar.managers.StageManager
import com.example.grandtheftcar.managers.TrafficManager
import com.example.grandtheftcar.models.CollectibleType
import com.example.grandtheftcar.models.PlayerCar
import com.example.grandtheftcar.models.PoliceCar
import com.example.grandtheftcar.models.Road
import com.example.grandtheftcar.ui.RetroRenderer
import kotlin.math.abs
import kotlin.random.Random

class GameEngine(private val context: Context) {

    val settings = SettingsManager(context)
    val audioManager = AudioManager(settings)
    val stageManager = StageManager()
    val trafficManager = TrafficManager(settings)
    val collectibleManager = CollectibleManager()
    val collisionManager = CollisionManager()
    val road = Road()
    val player = PlayerCar()
    val policeCar = PoliceCar()
    val renderer = RetroRenderer(settings, stageManager)

    var state: GameState = GameState.LOADING
    private var previousState: GameState = GameState.MAIN_MENU

    var screenWidth: Float = 0f
    var screenHeight: Float = 0f

    // Gameplay stats
    var score: Int = 0
    var distanceMeters: Float = 0f
    var survivalTimeSeconds: Float = 0f
    var comboMultiplier: Float = 1.0f
    var nearMissStreak: Int = 0
    var isNewHighScore: Boolean = false
    var stageBonus: Int = 0

    // Timing & animation
    private var loadingTimer: Float = 0f
    private var crashSequenceTimer: Float = 0f
    private var screenShakeAmount: Float = 0f

    // First-time control hint flag
    val showTutorialHint: Boolean
        get() = (!settings.hasSeenTutorial && stageManager.currentStage.stageNumber == 1 && survivalTimeSeconds < 5.0f)

    // Touch gesture tracking
    private var touchStartX: Float = 0f
    private var touchStartY: Float = 0f
    private var isDragging: Boolean = false

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    init {
        // Initial setup
    }

    fun onSurfaceChanged(width: Float, height: Float) {
        screenWidth = width
        screenHeight = height
        road.resize(width, height)
        renderer.update(0f)

        val config = stageManager.currentStage
        road.currentStageNumber = config.stageNumber
        player.configureStageSpeeds(config.startSpeedKmH, config.maxStageSpeedKmH)

        if (state == GameState.PLAYING || state == GameState.LOADING) {
            val carWidth = (road.laneWidth * 0.65f).coerceIn(44f, 80f)
            player.reset(road.getLaneCenterX(2), height * 0.76f, carWidth)
        }
    }

    fun update(dt: Float) {
        renderer.update(dt)

        // Screen shake decay
        if (screenShakeAmount > 0f) {
            screenShakeAmount -= dt * 18f
            if (screenShakeAmount < 0f) screenShakeAmount = 0f
        }

        when (state) {
            GameState.LOADING -> updateLoading(dt)
            GameState.MAIN_MENU -> updateMainMenu(dt)
            GameState.PLAYING -> updatePlaying(dt)
            GameState.CRASH_SEQUENCE -> updateCrashSequence(dt)
            GameState.PAUSED,
            GameState.SETTINGS,
            GameState.ABOUT,
            GameState.GAME_OVER,
            GameState.STAGE_COMPLETE,
            GameState.GAME_COMPLETE -> {
                // Static / UI only: completely frozen gameplay
            }
        }
    }

    private fun updateLoading(dt: Float) {
        loadingTimer += dt
        if (loadingTimer >= 1.4f) {
            state = GameState.MAIN_MENU
        }
    }

    private fun updateMainMenu(dt: Float) {
        road.update(dt, 90f, 3.8f)
    }

    private fun updatePlaying(dt: Float) {
        // REQ 1 & 2: Physically accelerated world scaling (5.2 pixels per real meter)
        val pixelsPerMeter = 5.2f

        // Real distance & score calculation directly using player.speedKmH
        val speedMps = player.speedKmH * (1000f / 3600f)
        val deltaDist = speedMps * dt
        distanceMeters += deltaDist
        survivalTimeSeconds += dt

        // Check tutorial completion
        if (survivalTimeSeconds >= 5.0f && !settings.hasSeenTutorial) {
            settings.hasSeenTutorial = true
        }

        // Base score increases with distance driven and actual speed
        score += (deltaDist * 1.8f * comboMultiplier).toInt()

        // Audio engine pitch mapped dynamically to current true physical speed
        audioManager.updateEnginePitch(player.speedKmH / 220f)

        // Update Road & roadside items at true physical speed
        road.update(dt, player.speedKmH, pixelsPerMeter)

        // Update Player car with smooth gradual acceleration towards stage peak speed
        player.update(dt, road.roadLeft, road.roadRight, stageManager.progressFraction)

        // Update Traffic oncoming closing velocity
        trafficManager.update(dt, road, player.speedKmH, survivalTimeSeconds, pixelsPerMeter)

        // Update Collectibles (spawns in clear lanes)
        collectibleManager.update(dt, road, player.speedKmH, pixelsPerMeter, trafficManager.getOccupiedLanesAhead())

        // Check Collectible pickups
        collectibleManager.checkCollection(player) { type, bonus ->
            audioManager.playPickup()
            vibrate(30)
            when (type) {
                CollectibleType.NITRO -> {
                    player.addNitro(0.55f)
                    audioManager.playNitro()
                }
                CollectibleType.CASH -> {
                    score += (bonus * comboMultiplier).toInt()
                }
                CollectibleType.SHIELD -> {
                    player.hasShield = true
                }
                CollectibleType.MULTIPLIER -> {
                    comboMultiplier = (comboMultiplier + 1.0f).coerceAtMost(4.0f)
                }
            }
        }

        // Update Stage progression & tunnel check
        stageManager.update(
            dt,
            onTunnelApproach = {
                road.triggerTunnel()
            },
            onStageComplete = {
                onStageCleared()
            }
        )
        if (stageManager.isEnteringTunnel) {
            road.isInsideTunnel = true
        }

        // Check Collisions (with shield deflection protection!)
        collisionManager.checkCollisions(
            player = player,
            vehicles = trafficManager.activeVehicles,
            onCollision = { vehicle ->
                triggerCrash()
            },
            onNearMiss = { bonus ->
                nearMissStreak++
                comboMultiplier = (1.0f + (nearMissStreak * 0.25f)).coerceAtMost(3.5f)
                score += (bonus * comboMultiplier).toInt()
                audioManager.playWhoosh()
                audioManager.playScreech()
                vibrate(28) // Subtle near-miss haptic
            },
            onShieldAbsorbed = { vehicle ->
                audioManager.playShieldDeflect()
                vibrate(85)
                // Deflect the oncoming vehicle away
                vehicle.speedKmH *= 0.4f
                vehicle.y -= 50f
            }
        )

        collisionManager.update(dt)
    }

    private fun updateCrashSequence(dt: Float) {
        crashSequenceTimer += dt
        collisionManager.update(dt)
        policeCar.update(dt)

        // Stop road movement smoothly
        road.update(dt * 0.12f, player.speedKmH * 0.1f, 4.2f)

        if (crashSequenceTimer >= 3.6f) {
            audioManager.stopPoliceSiren()
            isNewHighScore = (score > settings.highScore)
            settings.highScore = score
            settings.highestStage = stageManager.currentStage.stageNumber
            state = GameState.GAME_OVER
        }
    }

    private fun triggerCrash() {
        state = GameState.CRASH_SEQUENCE
        crashSequenceTimer = 0f
        screenShakeAmount = 24f

        audioManager.stopEngine()
        audioManager.playCrash()
        audioManager.startPoliceSiren()

        vibrate(350) // Strong collision haptic

        policeCar.startChase(player.x, player.y, player.width, screenHeight)
    }

    private fun onStageCleared() {
        audioManager.stopEngine()
        audioManager.playStageComplete()

        stageBonus = 5000 * stageManager.currentStage.stageNumber
        score += stageBonus

        settings.highScore = score
        settings.highestStage = (stageManager.currentStage.stageNumber + 1).coerceAtMost(3)

        if (stageManager.isLastStage) {
            state = GameState.GAME_COMPLETE
        } else {
            state = GameState.STAGE_COMPLETE
        }
    }

    fun startNewGame(stageNum: Int = 1) {
        score = 0
        distanceMeters = 0f
        survivalTimeSeconds = 0f
        comboMultiplier = 1.0f
        nearMissStreak = 0
        isNewHighScore = false

        stageManager.startStage(stageNum)
        val config = stageManager.currentStage
        road.currentStageNumber = config.stageNumber
        road.timeOfDay = config.timeOfDay
        road.resetTunnel()

        player.configureStageSpeeds(config.startSpeedKmH, config.maxStageSpeedKmH)
        val carWidth = (road.laneWidth * 0.65f).coerceIn(44f, 80f)
        player.reset(road.getLaneCenterX(2), screenHeight * 0.76f, carWidth)

        trafficManager.configureForStage(stageNum, settings.difficulty)
        trafficManager.reset()
        collectibleManager.reset()
        collisionManager.reset()
        policeCar.reset()

        state = GameState.PLAYING
        audioManager.startEngine()
    }

    fun continueNextStage() {
        if (stageManager.nextStage()) {
            survivalTimeSeconds = 0f
            val config = stageManager.currentStage
            road.currentStageNumber = config.stageNumber
            road.timeOfDay = config.timeOfDay
            road.resetTunnel()

            player.configureStageSpeeds(config.startSpeedKmH, config.maxStageSpeedKmH)
            val carWidth = (road.laneWidth * 0.65f).coerceIn(44f, 80f)
            player.reset(road.getLaneCenterX(2), screenHeight * 0.76f, carWidth)

            trafficManager.configureForStage(config.stageNumber, settings.difficulty)
            trafficManager.reset()
            collectibleManager.reset()
            collisionManager.reset()
            policeCar.reset()

            state = GameState.PLAYING
            audioManager.startEngine()
        } else {
            state = GameState.GAME_COMPLETE
        }
    }

    fun pauseGame() {
        if (state == GameState.PLAYING) {
            state = GameState.PAUSED
            audioManager.stopEngine()
        }
    }

    fun resumeGame() {
        if (state == GameState.PAUSED) {
            state = GameState.PLAYING
            audioManager.startEngine()
        }
    }

    fun handleBackPress(): Boolean {
        when (state) {
            GameState.PLAYING -> {
                pauseGame()
                return true
            }
            GameState.PAUSED -> {
                resumeGame()
                return true
            }
            GameState.SETTINGS, GameState.ABOUT -> {
                state = previousState
                return true
            }
            GameState.GAME_OVER, GameState.STAGE_COMPLETE, GameState.GAME_COMPLETE -> {
                state = GameState.MAIN_MENU
                return true
            }
            else -> return false
        }
    }

    // ==========================================
    // TOUCH & GESTURE INPUT HANDLING
    // ==========================================
    fun onTouchDown(x: Float, y: Float) {
        touchStartX = x
        touchStartY = y
        isDragging = false

        when (state) {
            GameState.MAIN_MENU -> {
                if (renderer.btnStart.contains(x, y)) {
                    audioManager.playClick()
                    startNewGame(1)
                } else if (renderer.btnSettings.contains(x, y)) {
                    audioManager.playClick()
                    previousState = GameState.MAIN_MENU
                    state = GameState.SETTINGS
                } else if (renderer.btnAbout.contains(x, y)) {
                    audioManager.playClick()
                    previousState = GameState.MAIN_MENU
                    state = GameState.ABOUT
                }
            }
            GameState.PLAYING -> {
                // Large comfortable Pause touch target
                val pauseHitPad = 12f
                if (x >= renderer.btnPause.left - pauseHitPad &&
                    x <= renderer.btnPause.right + pauseHitPad &&
                    y >= renderer.btnPause.top - pauseHitPad &&
                    y <= renderer.btnPause.bottom + pauseHitPad) {
                    audioManager.playClick()
                    pauseGame()
                    return
                }

                // Dedicated Quick Nitro button (available across all schemes)
                if (renderer.btnQuickNitro.contains(x, y)) {
                    if (player.nitroAmount > 0.05f) {
                        player.isBoosting = true
                        audioManager.playNitro()
                        vibrate(35)
                    }
                    return
                }

                // Virtual buttons if active
                if (settings.controlScheme == ControlScheme.ON_SCREEN_BUTTONS) {
                    if (renderer.btnLeft.contains(x, y)) {
                        shiftLane(-1)
                        return
                    } else if (renderer.btnRight.contains(x, y)) {
                        shiftLane(1)
                        return
                    } else if (renderer.btnBoost.contains(x, y)) {
                        if (player.nitroAmount > 0.05f) {
                            player.isBoosting = true
                            audioManager.playNitro()
                        }
                        return
                    } else if (renderer.btnBrake.contains(x, y)) {
                        player.isBraking = true
                        return
                    }
                }

                if (settings.controlScheme == ControlScheme.TOUCH_DRAG) {
                    isDragging = true
                    player.targetX = x
                }
            }
            GameState.PAUSED -> {
                if (renderer.btnResume.contains(x, y)) {
                    audioManager.playClick()
                    resumeGame()
                } else if (renderer.btnRestart.contains(x, y)) {
                    audioManager.playClick()
                    startNewGame(stageManager.currentStage.stageNumber)
                } else if (renderer.btnSettings.contains(x, y)) {
                    audioManager.playClick()
                    previousState = GameState.PAUSED
                    state = GameState.SETTINGS
                } else if (renderer.btnMenu.contains(x, y)) {
                    audioManager.playClick()
                    state = GameState.MAIN_MENU
                }
            }
            GameState.GAME_OVER -> {
                if (renderer.btnRestart.contains(x, y)) {
                    audioManager.playClick()
                    startNewGame(stageManager.currentStage.stageNumber)
                } else if (renderer.btnMenu.contains(x, y)) {
                    audioManager.playClick()
                    state = GameState.MAIN_MENU
                }
            }
            GameState.STAGE_COMPLETE -> {
                if (renderer.btnNextStage.contains(x, y)) {
                    audioManager.playClick()
                    continueNextStage()
                } else if (renderer.btnMenu.contains(x, y)) {
                    audioManager.playClick()
                    state = GameState.MAIN_MENU
                }
            }
            GameState.GAME_COMPLETE -> {
                if (renderer.btnMenu.contains(x, y)) {
                    audioManager.playClick()
                    state = GameState.MAIN_MENU
                }
            }
            GameState.SETTINGS -> handleSettingsTouch(x, y)
            GameState.ABOUT -> {
                if (renderer.btnAboutBack.contains(x, y)) {
                    audioManager.playClick()
                    state = previousState
                }
            }
            else -> {}
        }
    }

    fun onTouchMove(x: Float, y: Float) {
        if (state == GameState.PLAYING) {
            if (settings.controlScheme == ControlScheme.TOUCH_DRAG) {
                player.targetX = x
            }
        }
    }

    fun onTouchUp(x: Float, y: Float) {
        if (state == GameState.PLAYING) {
            if (player.isBoosting && settings.controlScheme != ControlScheme.ON_SCREEN_BUTTONS) {
                player.isBoosting = false
            }
            if (settings.controlScheme == ControlScheme.ON_SCREEN_BUTTONS) {
                player.isBoosting = false
                player.isBraking = false
            } else if (settings.controlScheme == ControlScheme.SWIPE_LANES) {
                val dx = x - touchStartX
                val dy = y - touchStartY
                if (abs(dx) > 35f && abs(dx) > abs(dy)) {
                    if (dx > 0) shiftLane(1) else shiftLane(-1)
                } else if (dy < -45f) {
                    if (player.nitroAmount > 0.05f) {
                        player.isBoosting = true
                        audioManager.playNitro()
                    }
                }
            }
            isDragging = false
        }
    }

    private fun shiftLane(direction: Int) {
        val currentLane = road.getLaneIndexForX(player.targetX)
        val newLane = (currentLane + direction).coerceIn(0, 5)
        if (newLane != currentLane) {
            player.targetX = road.getLaneCenterX(newLane)
            player.targetLane = newLane
            audioManager.playClick()
            vibrate(15) // Crisp subtle lane-change tick
        }
    }

    private fun vibrate(durationMs: Long) {
        if (!settings.vibrationEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (_: Exception) {}
    }

    private fun handleSettingsTouch(x: Float, y: Float) {
        if (renderer.btnSettingsBack.contains(x, y)) {
            audioManager.playClick()
            state = previousState
        } else if (renderer.btnMuteToggle.contains(x, y)) {
            settings.isMuted = !settings.isMuted
            audioManager.playClick()
        } else if (renderer.btnVibrationToggle.contains(x, y)) {
            settings.vibrationEnabled = !settings.vibrationEnabled
            if (settings.vibrationEnabled) vibrate(40)
            audioManager.playClick()
        } else if (renderer.btnControlsToggle.contains(x, y)) {
            val schemes = ControlScheme.values()
            val nextIdx = (settings.controlScheme.ordinal + 1) % schemes.size
            settings.controlScheme = schemes[nextIdx]
            audioManager.playClick()
        } else if (renderer.btnDifficultyToggle.contains(x, y)) {
            val diffs = Difficulty.values()
            val nextIdx = (settings.difficulty.ordinal + 1) % diffs.size
            settings.difficulty = diffs[nextIdx]
            trafficManager.configureForStage(stageManager.currentStage.stageNumber, settings.difficulty)
            audioManager.playClick()
        } else if (renderer.btnMasterDown.contains(x, y)) {
            settings.masterVolume = (settings.masterVolume - 0.1f).coerceIn(0f, 1f)
            audioManager.playClick()
        } else if (renderer.btnMasterUp.contains(x, y)) {
            settings.masterVolume = (settings.masterVolume + 0.1f).coerceIn(0f, 1f)
            audioManager.playClick()
        } else if (renderer.btnBrightnessDown.contains(x, y)) {
            settings.brightness = (settings.brightness - 0.1f).coerceIn(0.2f, 1.0f)
            audioManager.playClick()
        } else if (renderer.btnBrightnessUp.contains(x, y)) {
            settings.brightness = (settings.brightness + 0.1f).coerceIn(0.2f, 1.0f)
            audioManager.playClick()
        }
    }

    fun draw(canvas: Canvas) = render(canvas)

    fun render(canvas: Canvas) {
        canvas.save()

        // Apply screen shake
        if (screenShakeAmount > 0f) {
            val sx = (Random.nextFloat() - 0.5f) * screenShakeAmount
            val sy = (Random.nextFloat() - 0.5f) * screenShakeAmount
            canvas.translate(sx, sy)
        }

        when (state) {
            GameState.LOADING -> {
                renderer.drawLoading(canvas, screenWidth, screenHeight, (loadingTimer / 1.4f).coerceIn(0f, 1f))
            }
            GameState.MAIN_MENU -> {
                road.draw(canvas)
                renderer.drawMainMenu(canvas, screenWidth, screenHeight)
            }
            GameState.PLAYING -> {
                road.draw(canvas)
                collectibleManager.draw(canvas)
                trafficManager.draw(canvas)
                player.draw(canvas)
                collisionManager.draw(canvas)
                collectibleManager.drawPopups(canvas)

                // Speed lines at screen edges when driving fast
                renderer.drawSpeedLines(
                    canvas, screenWidth, screenHeight,
                    road.roadLeft, road.roadRight,
                    player.speedKmH, player.isBoosting
                )

                renderer.drawHUD(
                    canvas, screenWidth, screenHeight, score, player,
                    stageManager.currentStage.stageNumber, stageManager.timeRemaining,
                    comboMultiplier, showTutorialHint
                )
            }
            GameState.CRASH_SEQUENCE -> {
                road.draw(canvas)
                trafficManager.draw(canvas)
                player.draw(canvas)
                policeCar.draw(canvas)
                collisionManager.draw(canvas)
                renderer.drawCrashSequence(canvas, screenWidth, screenHeight, crashSequenceTimer)
            }
            GameState.PAUSED -> {
                road.draw(canvas)
                collectibleManager.draw(canvas)
                trafficManager.draw(canvas)
                player.draw(canvas)
                renderer.drawHUD(
                    canvas, screenWidth, screenHeight, score, player,
                    stageManager.currentStage.stageNumber, stageManager.timeRemaining,
                    comboMultiplier, false
                )
                renderer.drawPause(canvas, screenWidth, screenHeight)
            }
            GameState.GAME_OVER -> {
                road.draw(canvas)
                trafficManager.draw(canvas)
                player.draw(canvas)
                policeCar.draw(canvas)
                renderer.drawGameOver(canvas, screenWidth, screenHeight, score, distanceMeters, survivalTimeSeconds, isNewHighScore)
            }
            GameState.STAGE_COMPLETE -> {
                road.draw(canvas)
                player.draw(canvas)
                renderer.drawStageComplete(canvas, screenWidth, screenHeight, stageManager.currentStage.stageNumber, score, stageBonus)
            }
            GameState.GAME_COMPLETE -> {
                road.draw(canvas)
                renderer.drawGameComplete(canvas, screenWidth, screenHeight, score)
            }
            GameState.SETTINGS -> {
                road.draw(canvas)
                renderer.drawSettings(canvas, screenWidth, screenHeight)
            }
            GameState.ABOUT -> {
                road.draw(canvas)
                renderer.drawAbout(canvas, screenWidth, screenHeight)
            }
        }

        // Apply in-game brightness overlay
        renderer.drawBrightnessOverlay(canvas, screenWidth, screenHeight)

        canvas.restore()
    }

    fun onPause() {
        audioManager.stopEngine()
        audioManager.stopPoliceSiren()
        if (state == GameState.PLAYING) {
            state = GameState.PAUSED
        }
    }

    fun onResume() {
        if (state == GameState.PLAYING) {
            audioManager.startEngine()
        }
    }

    fun onDestroy() {
        audioManager.release()
    }
}
