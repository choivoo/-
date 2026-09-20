package com.sycompany.duomorph

import android.content.ComponentName
import android.graphics.drawable.Drawable

data class AppEntry(
    val label: String,
    val component: ComponentName,
    val icon: Drawable
)
