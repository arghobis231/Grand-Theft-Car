package com.example.grandtheftcar.managers

import com.example.grandtheftcar.models.TimeOfDay

data class StageConfig(
    val stageNumber: Int,
    val name: String,
    val durationSeconds: Float,
    val startSpeedKmH: Float,
    val maxStageSpeedKmH: Float,
    val timeOfDay: TimeOfDay,
    val description: String
)

class StageManager {

    /**
     * CONFIGURABLE STAGES LIST:
     * Easily modify durations, starting/max speeds, lighting, or add new stages here.
     */
    val stages = listOf(
        StageConfig(
            stageNumber = 1,
            name = "SUNNY HIGHWAY",
            durationSeconds = 60f,
            startSpeedKmH = 95f,
            maxStageSpeedKmH = 155f,
            timeOfDay = TimeOfDay.DAY,
            description = "Suburban highway. Speed builds from 95 to 155 km/h as you approach the tunnel."
        ),
        StageConfig(
            stageNumber = 2,
            name = "SUNSET CANYON",
            durationSeconds = 60f,
            startSpeedKmH = 120f,
            maxStageSpeedKmH = 185f,
            timeOfDay = TimeOfDay.SUNSET,
            description = "Desert freeway. High speed desert run climbing up to 185 km/h into dusk!"
        ),
        StageConfig(
            stageNumber = 3,
            name = "NEON EXPRESSWAY",
            durationSeconds = 60f,
            startSpeedKmH = 145f,
            maxStageSpeedKmH = 225f,
            timeOfDay = TimeOfDay.MIDNIGHT,
            description = "Midnight urban expressway. Blistering high speeds reaching 225+ km/h!"
        )
    )

    var currentStageIndex: Int = 0
    var stageTimeElapsed: Float = 0f
    var isTunnelApproaching: Boolean = false
    var isEnteringTunnel: Boolean = false
    var tunnelEntryProgress: Float = 0f // 0 to 1

    val currentStage: StageConfig
        get() = stages[currentStageIndex.coerceIn(0, stages.size - 1)]

    val timeRemaining: Float
        get() = (currentStage.durationSeconds - stageTimeElapsed).coerceAtLeast(0f)

    val progressFraction: Float
        get() = (stageTimeElapsed / currentStage.durationSeconds).coerceIn(0f, 1f)

    val isLastStage: Boolean
        get() = currentStageIndex >= stages.size - 1

    fun startStage(stageNumber: Int) {
        currentStageIndex = (stageNumber - 1).coerceIn(0, stages.size - 1)
        stageTimeElapsed = 0f
        isTunnelApproaching = false
        isEnteringTunnel = false
        tunnelEntryProgress = 0f
    }

    fun nextStage(): Boolean {
        if (isLastStage) return false
        currentStageIndex++
        stageTimeElapsed = 0f
        isTunnelApproaching = false
        isEnteringTunnel = false
        tunnelEntryProgress = 0f
        return true
    }

    fun update(dt: Float, onTunnelApproach: () -> Unit, onStageComplete: () -> Unit) {
        stageTimeElapsed += dt

        // 4.5 seconds before stage end, trigger tunnel entrance ahead
        if (!isTunnelApproaching && timeRemaining <= 4.5f) {
            isTunnelApproaching = true
            onTunnelApproach()
        }

        // When stage timer completes, begin tunnel drive-in animation
        if (timeRemaining <= 0f) {
            isEnteringTunnel = true
            tunnelEntryProgress += dt * 0.85f
            if (tunnelEntryProgress >= 1.0f) {
                onStageComplete()
            }
        }
    }

    fun reset() {
        stageTimeElapsed = 0f
        isTunnelApproaching = false
        isEnteringTunnel = false
        tunnelEntryProgress = 0f
    }
}
