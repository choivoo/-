package com.sycompany.duomorph

import android.animation.ValueAnimator
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import android.view.animation.PathInterpolator
import kotlin.math.abs
import kotlin.math.max

class HingeController(
    context: Context,
    private val durationMs: Long,
    private val onProgress: (progress: Float, debug: DebugState) -> Unit
) : SensorEventListener {

    data class DebugState(
        val rawAngle: Float?,
        val source: String,
        val distinctAngles: Int,
        val continuous: Boolean
    )

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val hingeSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_HINGE_ANGLE)

    private var progress = 0f
    private var filteredAngle = 0f
    private var hasAngle = false
    private var lastRaw: Float? = null
    private var animator: ValueAnimator? = null

    private val seenAngles = LinkedHashSet<Int>()
    private val sampleStart = SystemClock.elapsedRealtime()
    private var continuous = false

    fun start() {
        hingeSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        emit("idle")
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        animator?.cancel()
    }

    fun currentProgress(): Float = progress

    fun snapToInner(inner: Boolean) {
        val target = if (inner) 1f else 0f
        if (continuous && hasAngle) return
        animateTo(target, "staged-layout")
    }

    private fun animateTo(target: Float, source: String) {
        if (abs(target - progress) < 0.003f) {
            progress = target
            emit(source)
            return
        }
        animator?.cancel()
        animator = ValueAnimator.ofFloat(progress, target).apply {
            duration = max(220L, (durationMs * abs(target - progress)).toLong())
            interpolator = PathInterpolator(0.22f, 0.0f, 0.18f, 1.0f)
            addUpdateListener {
                progress = it.animatedValue as Float
                emit(source)
            }
            start()
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_HINGE_ANGLE || event.values.isEmpty()) return
        val raw = event.values[0].coerceIn(0f, 180f)
        lastRaw = raw

        seenAngles.add(raw.toInt())
        while (seenAngles.size > 64) {
            val first = seenAngles.firstOrNull() ?: break
            seenAngles.remove(first)
        }

        val elapsed = SystemClock.elapsedRealtime() - sampleStart
        if (!continuous && elapsed > 250L) {
            val nonCardinal = seenAngles.count { it !in setOf(0, 90, 180) }
            continuous = seenAngles.size >= 7 && nonCardinal >= 4
        }

        if (continuous) {
            animator?.cancel()
            filteredAngle = if (!hasAngle) raw else filteredAngle * 0.72f + raw * 0.28f
            hasAngle = true
            progress = (filteredAngle / 180f).coerceIn(0f, 1f)
            emit("hinge-angle")
        } else {
            when {
                raw >= 150f -> animateTo(1f, "hinge-trigger")
                raw <= 25f -> animateTo(0f, "hinge-trigger")
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun emit(source: String) {
        onProgress(
            progress,
            DebugState(
                rawAngle = lastRaw,
                source = if (hingeSensor == null) "no-hinge-sensor/$source" else source,
                distinctAngles = seenAngles.size,
                continuous = continuous
            )
        )
    }
}
