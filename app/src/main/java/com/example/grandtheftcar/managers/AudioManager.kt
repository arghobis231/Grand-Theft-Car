package com.example.grandtheftcar.managers

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

class AudioManager(private val settings: SettingsManager) {

    private val sampleRate = 22050
    private val executor: ExecutorService = Executors.newFixedThreadPool(2)

    private var engineTrack: AudioTrack? = null
    private var isEngineRunning = false
    private var engineThread: Thread? = null
    private var currentEngineFreq = 75.0

    private var sirenTrack: AudioTrack? = null
    private var isSirenRunning = false
    private var sirenThread: Thread? = null

    private var isBgmRunning = false
    private var bgmThread: Thread? = null

    // Pre-calculated sound buffers
    private val clickSoundData = generateToneBuffer(1200.0, 0.05f)
    private val crashSoundData = generateCrashBuffer(0.8f)
    private val screechSoundData = generateScreechBuffer(0.35f)
    private val stageCompleteData = generateFanfareBuffer()
    private val nitroSoundData = generateNitroBuffer(0.40f)
    private val pickupSoundData = generatePickupBuffer(0.18f)
    private val whooshSoundData = generateWhooshBuffer(0.22f)
    private val shieldDeflectData = generateShieldBuffer(0.28f)

    init {
        startBgmLoop()
    }

    private fun getEffectiveSfxVolume(): Float {
        return if (settings.isMuted) 0f else settings.masterVolume * settings.sfxVolume
    }

    private fun getEffectiveMusicVolume(): Float {
        return if (settings.isMuted) 0f else settings.masterVolume * settings.musicVolume
    }

    fun playClick() {
        val volume = getEffectiveSfxVolume()
        if (volume <= 0f) return
        executor.execute {
            playRawBuffer(clickSoundData, volume)
        }
    }

    fun playCrash() {
        val volume = getEffectiveSfxVolume()
        if (volume <= 0f) return
        executor.execute {
            playRawBuffer(crashSoundData, volume * 1.2f)
        }
    }

    fun playScreech() {
        val volume = getEffectiveSfxVolume()
        if (volume <= 0f) return
        executor.execute {
            playRawBuffer(screechSoundData, volume * 0.7f)
        }
    }

    fun playStageComplete() {
        val volume = getEffectiveSfxVolume()
        if (volume <= 0f) return
        executor.execute {
            playRawBuffer(stageCompleteData, volume)
        }
    }

    fun playNitro() {
        val volume = getEffectiveSfxVolume()
        if (volume <= 0f) return
        executor.execute {
            playRawBuffer(nitroSoundData, volume * 0.9f)
        }
    }

    fun playPickup() {
        val volume = getEffectiveSfxVolume()
        if (volume <= 0f) return
        executor.execute {
            playRawBuffer(pickupSoundData, volume * 0.85f)
        }
    }

    fun playWhoosh() {
        val volume = getEffectiveSfxVolume()
        if (volume <= 0f) return
        executor.execute {
            playRawBuffer(whooshSoundData, volume * 0.65f)
        }
    }

    fun playShieldDeflect() {
        val volume = getEffectiveSfxVolume()
        if (volume <= 0f) return
        executor.execute {
            playRawBuffer(shieldDeflectData, volume * 0.9f)
        }
    }

    fun startEngine() {
        if (isEngineRunning) return
        isEngineRunning = true
        engineThread = Thread({
            try {
                val bufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(sampleRate / 10)

                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                engineTrack = track
                track.play()

                val buffer = ShortArray(1024)
                var phase = 0.0

                while (isEngineRunning) {
                    val vol = getEffectiveSfxVolume() * 0.45f
                    val freq = currentEngineFreq
                    for (i in buffer.indices) {
                        // Mix fundamental frequency + sub-harmonics for throaty retro car rumble
                        val s1 = sin(phase)
                        val s2 = sin(phase * 0.5) * 0.6
                        val s3 = sin(phase * 2.0) * 0.3
                        val noise = (Random.nextFloat() - 0.5f) * 0.15f
                        val sample = ((s1 + s2 + s3 + noise) * 0.45 * vol * 32767).toInt()
                        buffer[i] = sample.coerceIn(-32768, 32767).toShort()

                        phase += 2.0 * PI * freq / sampleRate
                        if (phase > 2.0 * PI) phase -= 2.0 * PI
                    }
                    track.write(buffer, 0, buffer.size)
                }

                track.stop()
                track.release()
            } catch (_: Exception) {
            }
        }, "GtcEngineSoundThread").apply { start() }
    }

    fun updateEnginePitch(speedRatio: Float) {
        // Dynamic pitch scaling mapping speed directly to engine roar
        currentEngineFreq = 65.0 + (speedRatio.coerceIn(0f, 1.8f) * 140.0)
    }

    fun stopEngine() {
        isEngineRunning = false
        engineThread?.interrupt()
        engineThread = null
        try {
            engineTrack?.stop()
            engineTrack?.release()
        } catch (_: Exception) {}
        engineTrack = null
    }

    fun startPoliceSiren() {
        if (isSirenRunning) return
        isSirenRunning = true
        sirenThread = Thread({
            try {
                val bufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(sampleRate / 8)

                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                sirenTrack = track
                track.play()

                val buffer = ShortArray(1024)
                var phase = 0.0
                var time = 0.0

                while (isSirenRunning) {
                    val vol = getEffectiveSfxVolume() * 0.7f
                    for (i in buffer.indices) {
                        // Warble between 650Hz and 1050Hz (arcade siren)
                        val freq = 850.0 + 200.0 * sin(2.0 * PI * 2.2 * time)
                        val sample = (sin(phase) * vol * 32767).toInt()
                        buffer[i] = sample.coerceIn(-32768, 32767).toShort()

                        phase += 2.0 * PI * freq / sampleRate
                        if (phase > 2.0 * PI) phase -= 2.0 * PI
                        time += 1.0 / sampleRate
                    }
                    track.write(buffer, 0, buffer.size)
                }

                track.stop()
                track.release()
            } catch (_: Exception) {
            }
        }, "GtcPoliceSirenThread").apply { start() }
    }

    fun stopPoliceSiren() {
        isSirenRunning = false
        sirenThread?.interrupt()
        sirenThread = null
        try {
            sirenTrack?.stop()
            sirenTrack?.release()
        } catch (_: Exception) {}
        sirenTrack = null
    }

    private fun startBgmLoop() {
        if (isBgmRunning) return
        isBgmRunning = true
        bgmThread = Thread({
            try {
                val bufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(sampleRate / 4)

                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                track.play()

                // 80s Synthwave bassline notes
                val bassPattern = doubleArrayOf(
                    110.0, 110.0, 146.83, 146.83,
                    130.81, 130.81, 164.81, 164.81,
                    110.0, 110.0, 123.47, 130.81,
                    98.0, 98.0, 110.0, 110.0
                )
                val stepSeconds = 0.135
                val stepSamples = (sampleRate * stepSeconds).toInt()
                val stepBuffer = ShortArray(stepSamples)

                var stepIndex = 0

                while (isBgmRunning) {
                    val vol = getEffectiveMusicVolume() * 0.38f
                    val freq = bassPattern[stepIndex % bassPattern.size]
                    var phase = 0.0

                    for (i in 0 until stepSamples) {
                        val decay = 1.0 - (i.toDouble() / (stepSamples * 1.3))
                        val saw = ((phase / (2.0 * PI)) * 2.0 - 1.0) * 0.6
                        val sub = sin(phase * 0.5) * 0.4
                        val sample = ((saw + sub) * decay * vol * 32767).toInt()
                        stepBuffer[i] = sample.coerceIn(-32768, 32767).toShort()

                        phase += 2.0 * PI * freq / sampleRate
                        if (phase > 2.0 * PI) phase -= 2.0 * PI
                    }

                    track.write(stepBuffer, 0, stepBuffer.size)
                    stepIndex++
                }

                track.stop()
                track.release()
            } catch (_: Exception) {
            }
        }, "GtcSynthwaveBgmThread").apply { start() }
    }

    private fun playRawBuffer(buffer: ShortArray, volume: Float) {
        try {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            val scaledBuffer = ShortArray(buffer.size)
            for (i in buffer.indices) {
                scaledBuffer[i] = (buffer[i] * volume.coerceIn(0f, 1f)).toInt().toShort()
            }

            track.write(scaledBuffer, 0, scaledBuffer.size)
            track.play()

            val durationMs = (buffer.size * 1000L / sampleRate) + 60L
            Thread.sleep(durationMs)
            track.stop()
            track.release()
        } catch (_: Exception) {
        }
    }

    private fun generateToneBuffer(freq: Double, durationSec: Float): ShortArray {
        val totalSamples = (sampleRate * durationSec).toInt()
        val buffer = ShortArray(totalSamples)
        var phase = 0.0
        for (i in 0 until totalSamples) {
            val env = 1.0 - (i.toDouble() / totalSamples)
            val sample = (sin(phase) * env * 24000).toInt()
            buffer[i] = sample.coerceIn(-32768, 32767).toShort()
            phase += 2.0 * PI * freq / sampleRate
        }
        return buffer
    }

    private fun generateNitroBuffer(durationSec: Float): ShortArray {
        val totalSamples = (sampleRate * durationSec).toInt()
        val buffer = ShortArray(totalSamples)
        var phase = 0.0
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / totalSamples
            val freq = 280.0 + 1100.0 * (t * t)
            val noise = (Random.nextFloat() * 2f - 1f) * 0.4
            val tone = sin(phase) * 0.6
            val env = sin(t * PI)
            val sample = ((noise + tone) * env * 26000).toInt()
            buffer[i] = sample.coerceIn(-32768, 32767).toShort()
            phase += 2.0 * PI * freq / sampleRate
        }
        return buffer
    }

    private fun generatePickupBuffer(durationSec: Float): ShortArray {
        val totalSamples = (sampleRate * durationSec).toInt()
        val buffer = ShortArray(totalSamples)
        val half = totalSamples / 2
        var phase1 = 0.0
        var phase2 = 0.0
        for (i in 0 until totalSamples) {
            if (i < half) {
                val env = 1.0 - (i.toDouble() / half * 0.3)
                buffer[i] = (sin(phase1) * env * 24000).toInt().toShort()
                phase1 += 2.0 * PI * 880.0 / sampleRate
            } else {
                val env = 1.0 - ((i - half).toDouble() / half)
                buffer[i] = (sin(phase2) * env * 26000).toInt().toShort()
                phase2 += 2.0 * PI * 1320.0 / sampleRate
            }
        }
        return buffer
    }

    private fun generateWhooshBuffer(durationSec: Float): ShortArray {
        val totalSamples = (sampleRate * durationSec).toInt()
        val buffer = ShortArray(totalSamples)
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / totalSamples
            val env = sin(t * PI)
            val noise = (Random.nextFloat() * 2f - 1f) * env * 20000
            buffer[i] = noise.toInt().coerceIn(-32768, 32767).toShort()
        }
        return buffer
    }

    private fun generateShieldBuffer(durationSec: Float): ShortArray {
        val totalSamples = (sampleRate * durationSec).toInt()
        val buffer = ShortArray(totalSamples)
        var phase = 0.0
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / totalSamples
            val freq = 900.0 - 500.0 * t
            val env = (1.0 - t) * (1.0 - t)
            val tone = sin(phase) * 0.75 + sin(phase * 1.5) * 0.25
            buffer[i] = (tone * env * 25000).toInt().coerceIn(-32768, 32767).toShort()
            phase += 2.0 * PI * freq / sampleRate
        }
        return buffer
    }

    private fun generateCrashBuffer(durationSec: Float): ShortArray {
        val totalSamples = (sampleRate * durationSec).toInt()
        val buffer = ShortArray(totalSamples)
        var bassPhase = 0.0
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / totalSamples
            val decay = (1.0 - t) * (1.0 - t)
            val whiteNoise = (Random.nextFloat() * 2f - 1f) * 0.7
            val bassFreq = 90.0 - 50.0 * t
            val bass = sin(bassPhase) * 0.8
            bassPhase += 2.0 * PI * bassFreq / sampleRate

            val mixed = (whiteNoise + bass) * decay * 28000
            buffer[i] = mixed.toInt().coerceIn(-32768, 32767).toShort()
        }
        return buffer
    }

    private fun generateScreechBuffer(durationSec: Float): ShortArray {
        val totalSamples = (sampleRate * durationSec).toInt()
        val buffer = ShortArray(totalSamples)
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / totalSamples
            val decay = if (t < 0.1) t / 0.1 else (1.0 - t)
            val noise = (Random.nextFloat() * 2f - 1f) * 0.5
            val squeal = sin(2.0 * PI * (2200.0 + 800.0 * sin(i * 0.05)) / sampleRate) * 0.5
            buffer[i] = ((noise + squeal) * decay * 20000).toInt().coerceIn(-32768, 32767).toShort()
        }
        return buffer
    }

    private fun generateFanfareBuffer(): ShortArray {
        val notes = doubleArrayOf(523.25, 659.25, 783.99, 1046.50)
        val noteLength = (sampleRate * 0.18).toInt()
        val total = noteLength * notes.size
        val buffer = ShortArray(total)

        for (n in notes.indices) {
            val freq = notes[n]
            val offset = n * noteLength
            var phase = 0.0
            for (i in 0 until noteLength) {
                val decay = 1.0 - (i.toDouble() / (noteLength * 1.5))
                val sample = (sin(phase) * decay * 24000).toInt()
                buffer[offset + i] = sample.coerceIn(-32768, 32767).toShort()
                phase += 2.0 * PI * freq / sampleRate
            }
        }
        return buffer
    }

    fun release() {
        stopEngine()
        stopPoliceSiren()
        isBgmRunning = false
        bgmThread?.interrupt()
        bgmThread = null
        try {
            executor.shutdownNow()
        } catch (_: Throwable) {}
    }
}
