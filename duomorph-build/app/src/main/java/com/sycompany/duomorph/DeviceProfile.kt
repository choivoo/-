package com.sycompany.duomorph

import android.os.Build
import kotlin.math.max
import kotlin.math.min

data class DeviceProfile(
    val name: String,
    val coverColumns: Int,
    val innerColumns: Int,
    val iconScale: Float,
    val transitionMs: Long
) {
    companion object {
        fun current(): DeviceProfile {
            val model = Build.MODEL.uppercase()
            return when {
                model.startsWith("SM-F976") -> DeviceProfile(
                    name = "Galaxy Z Fold8 Ultra",
                    coverColumns = 4,
                    innerColumns = 7,
                    iconScale = 1.00f,
                    transitionMs = 670L
                )
                model.startsWith("SM-F971") -> DeviceProfile(
                    name = "Galaxy Z Fold8",
                    coverColumns = 4,
                    innerColumns = 6,
                    iconScale = 0.96f,
                    transitionMs = 650L
                )
                else -> DeviceProfile(
                    name = Build.MODEL,
                    coverColumns = 4,
                    innerColumns = 6,
                    iconScale = 0.96f,
                    transitionMs = 670L
                )
            }
        }

        fun isInner(width: Int, height: Int): Boolean {
            if (width <= 0 || height <= 0) return false
            val shortSide = min(width, height).toFloat()
            val longSide = max(width, height).toFloat()
            return shortSide / longSide >= 0.70f
        }
    }
}
