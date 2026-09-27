package com.example.grandtheftcar.managers

import android.graphics.Canvas
import com.example.grandtheftcar.models.Road
import com.example.grandtheftcar.models.TrafficVehicle
import com.example.grandtheftcar.models.VehicleType
import kotlin.math.abs
import kotlin.random.Random

class TrafficManager(private val settings: SettingsManager) {

    val activeVehicles = mutableListOf<TrafficVehicle>()
    private val vehiclePool = ArrayDeque<TrafficVehicle>()

    private var spawnTimer: Float = 0f
    private var baseSpawnInterval: Float = 2.4f
    private var baseTrafficSpeed: Float = 80f
    private var currentStageNumber: Int = 1

    init {
        // Pre-allocate 35 vehicles for zero-allocation game loop
        for (i in 0 until 35) {
            vehiclePool.add(TrafficVehicle())
        }
    }

    fun configureForStage(stageNum: Int, difficulty: Difficulty) {
        currentStageNumber = stageNum
        val difficultyMult = when (difficulty) {
            Difficulty.EASY -> 0.80f
            Difficulty.NORMAL -> 1.0f
            Difficulty.HARDCORE -> 1.25f
        }

        // Calibrated spawn intervals: relaxed density, fair reaction times
        when (stageNum) {
            1 -> {
                baseSpawnInterval = (2.6f / difficultyMult).coerceAtLeast(1.8f)
                baseTrafficSpeed = 75f * difficultyMult
            }
            2 -> {
                baseSpawnInterval = (2.1f / difficultyMult).coerceAtLeast(1.5f)
                baseTrafficSpeed = 90f * difficultyMult
            }
            3 -> {
                baseSpawnInterval = (1.75f / difficultyMult).coerceAtLeast(1.3f)
                baseTrafficSpeed = 105f * difficultyMult
            }
            else -> {
                baseSpawnInterval = (1.6f / difficultyMult).coerceAtLeast(1.2f)
                baseTrafficSpeed = 115f * difficultyMult
            }
        }
    }

    fun reset() {
        for (v in activeVehicles) {
            v.active = false
            vehiclePool.add(v)
        }
        activeVehicles.clear()
        spawnTimer = 0f
    }

    fun getOccupiedLanesAhead(): BooleanArray {
        val occupied = BooleanArray(6) { false }
        for (v in activeVehicles) {
            if (v.active && v.y < 500f) {
                if (v.lane in 0..5) {
                    occupied[v.lane] = true
                }
            }
        }
        return occupied
    }

    fun update(
        dt: Float,
        road: Road,
        playerSpeedKmH: Float,
        survivalTime: Float,
        pixelsPerMeter: Float
    ) {
        // Very gentle ramp over survival time (max +15% over full 60s)
        val rampFactor = (1f + (survivalTime / 60f) * 0.15f)
        val effectiveInterval = (baseSpawnInterval / rampFactor).coerceAtLeast(1.2f)

        spawnTimer += dt
        if (spawnTimer >= effectiveInterval && !road.tunnelActive) {
            spawnTimer = 0f
            attemptSafeTrafficSpawn(road)
        }

        // Update active vehicles with lane change and signal logic
        val iter = activeVehicles.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            v.update(
                dt = dt,
                playerSpeedKmH = playerSpeedKmH,
                pixelsPerMeter = pixelsPerMeter,
                allVehicles = activeVehicles,
                roadLeft = road.roadLeft,
                laneWidth = road.laneWidth
            )

            // When vehicle is completely off bottom of screen, recycle it
            if (v.y > road.screenHeight + v.height + 60f) {
                v.active = false
                iter.remove()
                vehiclePool.add(v)
            }
        }
    }

    /**
     * INTELLIGENT SAFE TRAFFIC SPAWNING ALGORITHM
     *
     * 1. Evaluates all currently active vehicles in the upper approach zone (y < 600px).
     * 2. Checks occupied lanes and vertical distances.
     * 3. Guarantees that at least TWO contiguous or easily navigable lanes remain 100% open.
     * 4. Prevents side-by-side walls across all 6 lanes.
     * 5. Reduces multi-car waves to rare (<= 20%), with staggered vertical spacing and at least 1 open lane between them.
     */
    private fun attemptSafeTrafficSpawn(road: Road) {
        // Step 1: Scan upcoming corridor (y from -300 to 550)
        val occupiedLanesInUpcomingZone = BooleanArray(6) { false }
        var upcomingVehiclesCount = 0

        for (v in activeVehicles) {
            if (v.active && v.y < 550f) {
                if (v.lane in 0..5) {
                    occupiedLanesInUpcomingZone[v.lane] = true
                    upcomingVehiclesCount++
                }
            }
        }

        // Count how many lanes are currently free in the upcoming zone
        var freeLanesCount = 0
        val freeLanesList = mutableListOf<Int>()
        for (lane in 0..5) {
            if (!occupiedLanesInUpcomingZone[lane]) {
                freeLanesCount++
                freeLanesList.add(lane)
            }
        }

        // Safety Rule 1: Always guarantee at least 2 safe open escape lanes in the upcoming zone.
        if (freeLanesCount <= 2) {
            return
        }

        // Safety Rule 2: Limit wave size (mostly 1 car, rarely 2 cars if >= 4 lanes are clear).
        val allowDoubleSpawn = (freeLanesCount >= 4) && (Random.nextFloat() < 0.18f)
        val spawnCount = if (allowDoubleSpawn) 2 else 1

        freeLanesList.shuffle()

        var spawnedThisWave = 0
        var firstSpawnedLane = -1

        for (candidateLane in freeLanesList) {
            if (spawnedThisWave >= spawnCount) break

            // Safety Rule 3: For double spawns, ensure they are NOT adjacent.
            if (firstSpawnedLane != -1 && abs(candidateLane - firstSpawnedLane) <= 1) {
                continue
            }

            // Safety Rule 4: Verify longitudinal distance in this specific lane.
            var tooCloseInLane = false
            for (v in activeVehicles) {
                if (v.active && v.lane == candidateLane && v.y < 450f) {
                    tooCloseInLane = true
                    break
                }
            }
            if (tooCloseInLane) continue

            // Pick appropriate vehicle type based on stage progression
            val type = pickVehicleTypeForStage(currentStageNumber)

            // Stagger multiple vehicles vertically so they are never strictly side-by-side
            val verticalStagger = if (spawnedThisWave == 0) 0f else (180f + Random.nextFloat() * 120f)
            val spawnY = -road.laneWidth * type.lengthFactor - verticalStagger - 40f

            val laneCenterX = road.getLaneCenterX(candidateLane)
            val vehicle = if (vehiclePool.isNotEmpty()) vehiclePool.removeFirst() else TrafficVehicle()

            vehicle.spawn(
                spawnLane = candidateLane,
                laneCenterX = laneCenterX,
                spawnY = spawnY,
                laneWidth = road.laneWidth,
                vehicleType = type,
                baseSpeed = baseTrafficSpeed
            )

            activeVehicles.add(vehicle)
            if (firstSpawnedLane == -1) firstSpawnedLane = candidateLane
            spawnedThisWave++
        }
    }

    private fun pickVehicleTypeForStage(stageNum: Int): VehicleType {
        val r = Random.nextFloat()
        return when (stageNum) {
            1 -> {
                when {
                    r < 0.36f -> VehicleType.SEDAN
                    r < 0.65f -> VehicleType.SMALL_CAR
                    r < 0.82f -> VehicleType.SUV
                    r < 0.92f -> VehicleType.SPORTS_CAR
                    r < 0.97f -> VehicleType.VAN
                    else -> VehicleType.TRUCK
                }
            }
            2 -> {
                when {
                    r < 0.28f -> VehicleType.SEDAN
                    r < 0.48f -> VehicleType.SUV
                    r < 0.68f -> VehicleType.VAN
                    r < 0.84f -> VehicleType.SPORTS_CAR
                    r < 0.93f -> VehicleType.TRUCK
                    else -> VehicleType.EMERGENCY_AMBULANCE
                }
            }
            3 -> {
                when {
                    r < 0.25f -> VehicleType.SPORTS_CAR
                    r < 0.45f -> VehicleType.SEDAN
                    r < 0.62f -> VehicleType.TRUCK
                    r < 0.78f -> VehicleType.SUV
                    r < 0.88f -> VehicleType.VAN
                    r < 0.94f -> VehicleType.EMERGENCY_POLICE
                    else -> VehicleType.EMERGENCY_AMBULANCE
                }
            }
            else -> VehicleType.SEDAN
        }
    }

    fun draw(canvas: Canvas) {
        for (v in activeVehicles) {
            v.draw(canvas)
        }
    }
}
