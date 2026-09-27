package com.example.grandtheftcar.models

import android.graphics.Color

enum class VehicleType(
    val displayName: String,
    val lengthFactor: Float, // relative to lane width
    val widthFactor: Float,  // relative to lane width
    val speedVarianceMin: Float,
    val speedVarianceMax: Float,
    val isEmergency: Boolean = false,
    val defaultColors: IntArray
) {
    SMALL_CAR(
        displayName = "Compact Hatchback",
        lengthFactor = 1.30f,
        widthFactor = 0.58f,
        speedVarianceMin = 0.90f,
        speedVarianceMax = 1.15f,
        defaultColors = intArrayOf(
            Color.parseColor("#EAB308"), // Taxi yellow
            Color.parseColor("#06B6D4"), // Electric cyan
            Color.parseColor("#A855F7"), // Vivid purple
            Color.parseColor("#10B981")  // Emerald
        )
    ),
    SEDAN(
        displayName = "Sedan",
        lengthFactor = 1.60f,
        widthFactor = 0.62f,
        speedVarianceMin = 0.92f,
        speedVarianceMax = 1.10f,
        defaultColors = intArrayOf(
            Color.parseColor("#F8FAFC"), // Pearl white
            Color.parseColor("#3B82F6"), // Royal blue
            Color.parseColor("#64748B"), // Steel grey
            Color.parseColor("#991B1B")  // Dark maroon
        )
    ),
    SPORTS_CAR(
        displayName = "Sports Coupe",
        lengthFactor = 1.55f,
        widthFactor = 0.64f,
        speedVarianceMin = 1.20f,
        speedVarianceMax = 1.50f,
        defaultColors = intArrayOf(
            Color.parseColor("#F97316"), // Neon orange
            Color.parseColor("#EF4444"), // Racing red
            Color.parseColor("#84CC16"), // Lime green
            Color.parseColor("#EC4899")  // Hot pink
        )
    ),
    SUV(
        displayName = "Full-Size SUV",
        lengthFactor = 1.80f,
        widthFactor = 0.70f,
        speedVarianceMin = 0.85f,
        speedVarianceMax = 1.05f,
        defaultColors = intArrayOf(
            Color.parseColor("#1E293B"), // Midnight black
            Color.parseColor("#475569"), // Dark slate
            Color.parseColor("#047857"), // Forest green
            Color.parseColor("#B45309")  // Bronze
        )
    ),
    VAN(
        displayName = "Delivery Cargo Van",
        lengthFactor = 2.00f,
        widthFactor = 0.72f,
        speedVarianceMin = 0.78f,
        speedVarianceMax = 0.98f,
        defaultColors = intArrayOf(
            Color.parseColor("#E2E8F0"), // White fleet
            Color.parseColor("#78716C"), // Cargo grey
            Color.parseColor("#0284C7"), // Express courier blue
            Color.parseColor("#D97706")  // Amber transport
        )
    ),
    TRUCK(
        displayName = "Semi-Trailer Rig",
        lengthFactor = 3.30f,
        widthFactor = 0.78f,
        speedVarianceMin = 0.65f,
        speedVarianceMax = 0.85f,
        defaultColors = intArrayOf(
            Color.parseColor("#DC2626"), // Red tractor cab
            Color.parseColor("#2563EB"), // Blue tractor cab
            Color.parseColor("#059669"), // Green tractor cab
            Color.parseColor("#D97706")  // Orange tractor cab
        )
    ),
    EMERGENCY_AMBULANCE(
        displayName = "Emergency Ambulance",
        lengthFactor = 2.10f,
        widthFactor = 0.73f,
        speedVarianceMin = 1.25f,
        speedVarianceMax = 1.45f,
        isEmergency = true,
        defaultColors = intArrayOf(
            Color.parseColor("#FFFFFF") // Crisp white with orange/red stripes
        )
    ),
    EMERGENCY_POLICE(
        displayName = "Police Interceptor",
        lengthFactor = 1.68f,
        widthFactor = 0.65f,
        speedVarianceMin = 1.30f,
        speedVarianceMax = 1.55f,
        isEmergency = true,
        defaultColors = intArrayOf(
            Color.parseColor("#0F172A") // Police black & white
        )
    )
}
