/*
 * Free, open-source undetected color cheat for Overwatch!
 * Copyright (C) 2017  Thomas G. Nappo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.jire.overwatcheat.aimbot

import net.openhft.affinity.AffinityLock
import org.jire.overwatcheat.FastRandom
import org.jire.overwatcheat.Keyboard
import org.jire.overwatcheat.Mouse
import org.jire.overwatcheat.aimbot.AimBotState.aimData
import org.jire.overwatcheat.aimbot.AimBotState.flicking
import org.jire.overwatcheat.settings.Settings
import org.jire.overwatcheat.settings.Settings.mouseId
import org.jire.overwatcheat.util.PreciseSleeper
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.system.measureNanoTime

class AimBotThread(
    val captureCenterX: Int, val captureCenterY: Int,
    val maxSnapX: Int, val maxSnapY: Int,
    val preciseSleeper: PreciseSleeper,
    val cpuThreadAffinityIndex: Int,
) : Thread("Aim Bot") {

    private val random = FastRandom()

    private val sensitivityScale get() = 1F / Settings.sensitivity
    private val maxMoveX get() = min(maxSnapX, Settings.aimMaxMovePixels)
    private val maxMoveY get() = min(maxSnapY, Settings.aimMaxMovePixels)
    private val flickStabilityFrames get() = max(1, Settings.flickStabilityFrames)
    private val aimMode get() = AimMode[Settings.aimMode] ?: AimMode.TRACKING
    private val aimDurationNanos get() = (Settings.aimDurationMillis * 1_000_000).toLong()
    private val flickPauseNanos get() = TimeUnit.MILLISECONDS.toNanos(Settings.flickPause)
    val random = FastRandom()

    private val alpha = Settings.alpha
    private val aimKP = Settings.aimKP
    private val sensitivityScale = 1F / Settings.sensitivity
    private val jitterPercent = Settings.aimJitterPercent
    private val maxMoveX = min(maxSnapX, Settings.aimMaxMovePixels)
    private val maxMoveY = min(maxSnapY, Settings.aimMaxMovePixels)
    private val flickStabilityFrames = max(1, Settings.flickStabilityFrames)
    private val flickReadinessAlpha = Settings.flickReadinessAlpha

    private var previousErrorX = 0F
    private var previousErrorY = 0F

    override fun run() {
        priority = MAX_PRIORITY - 1

        val tlr = ThreadLocalRandom.current()
        var wasPressed = false
        val affinityLock: AffinityLock? =
            if (cpuThreadAffinityIndex >= 0
                && System.getProperty("os.arch") != "aarch64"
                && Runtime.getRuntime().availableProcessors() > 1 /* only acquire lock if we have at least 2 threads
                                                                     as just one core would freeze the machine */
            ) AffinityLock.acquireLock(cpuThreadAffinityIndex)
            else null
        try {
            while (true) {
                val elapsed = measureNanoTime {
                    val pressed = Keyboard.keyPressed(Settings.aimKey)
                    if (Settings.toggleInGameUI && wasPressed != pressed) {
                        AimBotState.toggleUI = true
                    }
                    wasPressed = pressed
                    if (!pressed) {
                        aimData = 0
                        return@measureNanoTime
                    } else if (aimMode.flicks) {
                        flicking = true
                    }
                    useAimData(aimData)
                }
                val sleepTimeMultiplier = max(
                    Settings.aimDurationMultiplierMax,
                    (Settings.aimDurationMultiplierBase + tlr.nextFloat())
                )
                val sleepTime = (aimDurationNanos * sleepTimeMultiplier).toLong() - elapsed
                if (sleepTime > 100_000) {
                    preciseSleeper.preciseSleep(sleepTime)
                }
            }
        } finally {
            affinityLock?.release()
        }
    }

    private fun useAimData(aimData: Long) {
        if (aimData == 0L) return resetError()

        val dX = calculateDelta(
            aimData, 48,
            Settings.aimMinTargetWidth, captureCenterX
        )
        val dY = calculateDelta(
            aimData, 16,
            Settings.aimMinTargetHeight, captureCenterY
        )
        if (dX != null && dY != null) {
            performAim(dX, dY)
        } else {
            resetError()
        }
    }

    private fun extractAimData(aimData: Long, shiftBits: Int) = (aimData ushr shiftBits) and 0xFFFF
    private fun calculateDelta(
        aimData: Long,
        shiftBitsBase: Int,
        minimumSize: Int,
        deltaSubtrahend: Int
    ): Float? {
        val low = extractAimData(aimData, shiftBitsBase)
        val high = extractAimData(aimData, shiftBitsBase - 16)
        val size = high - low
        if (size < minimumSize) return null

        val center = (low + high) / 2F

        return center - deltaSubtrahend
    }

    private fun performAim(dX: Float, dY: Float) {
        val smoothedX = lerp(previousErrorX, dX, Settings.alpha)
        val smoothedY = lerp(previousErrorY, dY, Settings.alpha)
        previousErrorX = smoothedX
        previousErrorY = smoothedY

        val moveXFloat = smoothedX * Settings.aimKP
        val moveYFloat = smoothedY * Settings.aimKP

        val randomSensitivityMultiplier =
            if (Settings.aimJitterPercent == 0) 1F else 1F - (random[Settings.aimJitterPercent] / 100F)
        val moveX = (moveXFloat * sensitivityScale * randomSensitivityMultiplier).roundToInt()
        val moveY = (moveYFloat * sensitivityScale * randomSensitivityMultiplier).roundToInt()

        val limitedMoveX = moveX.coerceIn(-maxMoveX, maxMoveX)
        val limitedMoveY = moveY.coerceIn(-maxMoveY, maxMoveY)

        if (limitedMoveX != 0 || limitedMoveY != 0) {
            Mouse.move(limitedMoveX, limitedMoveY, mouseId)
        }

        applyFlick(smoothedX, smoothedY)
    }

    private fun lerp(start: Float, end: Float, alpha: Float) = start + (end - start) * alpha

    private fun resetError() {
        previousErrorX = 0F
        previousErrorY = 0F
        smoothedFlickErrorMagnitudeSquared = 0F
        flickFramesWithinThreshold = 0
    }

    private var flickFramesWithinThreshold = 0
    private var smoothedFlickErrorMagnitudeSquared = 0F

    private fun applyFlick(smoothedErrorX: Float, smoothedErrorY: Float) {
        val errorMagnitudeSquared = (smoothedErrorX * smoothedErrorX) + (smoothedErrorY * smoothedErrorY)
        smoothedFlickErrorMagnitudeSquared = lerp(
            smoothedFlickErrorMagnitudeSquared,
            errorMagnitudeSquared,
            Settings.flickReadinessAlpha
        )

        val thresholdSquared = Settings.flickPixels * Settings.flickPixels
            flickReadinessAlpha
        )

        val thresholdSquared = flickPixels * flickPixels
        if (smoothedFlickErrorMagnitudeSquared < thresholdSquared) {
            flickFramesWithinThreshold++
        } else {
            flickFramesWithinThreshold = 0
        }

        if (flicking && flickFramesWithinThreshold >= flickStabilityFrames) {
            flicking = false
            flickFramesWithinThreshold = 0
            Mouse.click(mouseId)
            preciseSleeper.preciseSleep(flickPauseNanos)
        }
    }

}
