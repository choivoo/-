package com.sycompany.duomorph

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import kotlin.math.*

class DuoLauncherView(
    context: Context,
    private val apps: List<AppEntry>,
    private val profile: DeviceProfile
) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans", Typeface.NORMAL)
    }
    private val debugPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 27f
        typeface = Typeface.MONOSPACE
    }

    private var progress = 0f
    private var debug = HingeController.DebugState(null, "idle", 0, false)
    private var diagnostics = false
    private var hapticMidpointDone = false

    private val gesture = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean = true
        override fun onLongPress(e: MotionEvent) {
            diagnostics = !diagnostics
            performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
            invalidate()
        }
        override fun onSingleTapUp(e: MotionEvent): Boolean = launchAt(e.x, e.y)
    })

    fun setFoldProgress(value: Float, state: HingeController.DebugState) {
        progress = value.coerceIn(0f, 1f)
        debug = state

        val nowPastMid = progress in 0.48f..0.62f
        if (nowPastMid && !hapticMidpointDone) {
            hapticMidpointDone = true
            tickHaptic()
        }
        if (progress < 0.35f || progress > 0.75f) hapticMidpointDone = false

        if (Build.VERSION.SDK_INT >= 31) {
            val blur = foldStrength(progress) * min(width, height) * 0.018f
            renderEffect = if (blur > 0.7f) {
                RenderEffect.createBlurEffect(blur, blur, Shader.TileMode.CLAMP)
            } else null
        }
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean = gesture.onTouchEvent(event)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width <= 0 || height <= 0) return

        val inner = DeviceProfile.isInner(width, height)

        drawBackground(canvas)

        val pivotX = if (inner) width * 0.54f else width * 0.08f
        canvas.save()
        val sx = 0.955f + 0.045f * ease(progress)
        val skew = (1f - progress) * if (inner) -0.018f else 0.018f
        val matrix = Matrix().apply {
            postTranslate(-pivotX, -height / 2f)
            postScale(sx, 1f)
            postSkew(skew, 0f)
            postTranslate(pivotX, height / 2f)
        }
        canvas.concat(matrix)
        drawApps(canvas, inner)
        drawDock(canvas, inner)
        canvas.restore()

        drawFoldGlass(canvas, inner)
        if (diagnostics) drawDiagnostics(canvas, inner)
    }

    private fun drawBackground(canvas: Canvas) {
        val top = Color.rgb(18, 87, 164)
        val bottom = Color.rgb(4, 24, 52)
        paint.shader = LinearGradient(0f, 0f, width.toFloat(), height.toFloat(), top, bottom, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.shader = null

        paint.color = Color.argb(55, 255, 255, 255)
        canvas.drawOval(RectF(width*0.08f, height*0.18f, width*0.85f, height*0.32f), paint)
        paint.color = Color.argb(80, 5, 25, 38)
        val path = Path().apply {
            moveTo(0f, height*0.72f)
            cubicTo(width*0.18f,height*0.62f,width*0.32f,height*0.76f,width*0.52f,height*0.66f)
            cubicTo(width*0.70f,height*0.58f,width*0.82f,height*0.72f,width.toFloat(),height*0.60f)
            lineTo(width.toFloat(),height.toFloat())
            lineTo(0f,height.toFloat())
            close()
        }
        canvas.drawPath(path, paint)
    }

    private data class Cell(val x: Float, val y: Float, val size: Float)

    private fun coverCell(index: Int, onInnerCanvas: Boolean): Cell {
        val columns = profile.coverColumns
        val rows = max(1, ceil(min(apps.size, 24) / columns.toDouble()).toInt())
        val panelW = if (onInnerCanvas) width * 0.43f else width.toFloat()
        val left = if (onInnerCanvas) width - panelW - width*0.03f else 0f
        val top = height * 0.17f
        val usableH = height * 0.62f
        val cellW = panelW / columns
        val cellH = usableH / max(rows, 6)
        val c = index % columns
        val r = index / columns
        val size = min(cellW, cellH) * 0.57f * profile.iconScale
        return Cell(left + cellW*(c+0.5f), top + cellH*(r+0.5f), size)
    }

    private fun innerCell(index: Int): Cell {
        val columns = profile.innerColumns
        val rows = max(1, ceil(min(apps.size, columns*5) / columns.toDouble()).toInt())
        val left = width * 0.055f
        val rightDock = width * 0.13f
        val top = height * 0.14f
        val usableW = width - left - rightDock
        val usableH = height * 0.68f
        val cellW = usableW / columns
        val cellH = usableH / max(rows, 5)
        val c = index % columns
        val r = index / columns
        val size = min(cellW, cellH) * 0.55f * profile.iconScale
        return Cell(left + cellW*(c+0.5f), top + cellH*(r+0.5f), size)
    }

    private fun drawApps(canvas: Canvas, inner: Boolean) {
        val count = min(apps.size, if (inner) profile.innerColumns*5 else 24)
        for (i in 0 until count) {
            val start = coverCell(i, onInnerCanvas = inner)
            val end = if (inner) innerCell(i) else coverCell(i, onInnerCanvas = false)
            val t = if (inner) ease(progress) else 0f
            val x = lerp(start.x, end.x, t)
            val y = lerp(start.y, end.y, t)
            val size = lerp(start.size, end.size, t)
            drawApp(canvas, apps[i], x, y, size)
        }
    }

    private fun drawApp(canvas: Canvas, app: AppEntry, cx: Float, cy: Float, iconSize: Float) {
        val radius = iconSize*0.25f
        paint.color = Color.argb(52, 255, 255, 255)
        canvas.drawRoundRect(RectF(cx-iconSize/2-6, cy-iconSize/2-6, cx+iconSize/2+6, cy+iconSize/2+6), radius, radius, paint)

        val d: Drawable = app.icon
        d.setBounds((cx-iconSize/2).toInt(), (cy-iconSize/2).toInt(), (cx+iconSize/2).toInt(), (cy+iconSize/2).toInt())
        d.draw(canvas)

        textPaint.textSize = iconSize * 0.22f
        textPaint.setShadowLayer(3f,0f,1f,Color.BLACK)
        val label = if (app.label.length > 10) app.label.take(9) + "…" else app.label
        canvas.drawText(label, cx, cy + iconSize*0.75f, textPaint)
        textPaint.clearShadowLayer()
    }

    private fun drawDock(canvas: Canvas, inner: Boolean) {
        if (apps.isEmpty()) return
        val dockApps = apps.take(5)
        if (inner) {
            val right = width * 0.965f
            val dockW = width * 0.09f
            paint.color = Color.argb(100, 0, 0, 0)
            canvas.drawRoundRect(RectF(right-dockW, height*0.24f, right, height*0.76f), dockW/2, dockW/2, paint)
            dockApps.forEachIndexed { i, app ->
                val y = lerp(height*0.31f, height*0.69f, i / max(1f,(dockApps.size-1).toFloat()))
                val s = dockW*0.58f
                app.icon.setBounds((right-dockW/2-s/2).toInt(), (y-s/2).toInt(), (right-dockW/2+s/2).toInt(), (y+s/2).toInt())
                app.icon.draw(canvas)
            }
        } else {
            val dockH = height*0.105f
            paint.color = Color.argb(105,0,0,0)
            canvas.drawRoundRect(RectF(width*0.09f,height-dockH-height*0.035f,width*0.91f,height-height*0.035f), dockH/2,dockH/2,paint)
            dockApps.forEachIndexed { i, app ->
                val x = lerp(width*0.19f, width*0.81f, i / max(1f,(dockApps.size-1).toFloat()))
                val s = dockH*0.58f
                app.icon.setBounds((x-s/2).toInt(), (height-dockH/2-height*0.035f-s/2).toInt(), (x+s/2).toInt(), (height-dockH/2-height*0.035f+s/2).toInt())
                app.icon.draw(canvas)
            }
        }
    }

    private fun drawFoldGlass(canvas: Canvas, inner: Boolean) {
        val strength = foldStrength(progress)
        if (strength <= 0.005f) return

        paint.color = Color.argb((85f*strength).toInt(), 2, 8, 20)
        canvas.drawRect(0f,0f,width.toFloat(),height.toFloat(),paint)

        val hx = if (inner) width*0.52f else width*0.06f
        val radius = width * (0.08f + 0.18f*strength)
        paint.shader = LinearGradient(
            hx-radius, 0f, hx+radius, 0f,
            intArrayOf(
                Color.TRANSPARENT,
                Color.argb((118f*strength).toInt(), 218, 236, 255),
                Color.argb((45f*strength).toInt(), 255,255,255),
                Color.TRANSPARENT
            ),
            floatArrayOf(0f,0.38f,0.53f,1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(max(0f,hx-radius),0f,min(width.toFloat(),hx+radius),height.toFloat(),paint)
        paint.shader = null
    }

    private fun drawDiagnostics(canvas: Canvas, inner: Boolean) {
        paint.color = Color.argb(190,0,0,0)
        canvas.drawRoundRect(RectF(18f, 24f, min(width-18f, 880f), 270f), 24f,24f,paint)
        val lines = listOf(
            "DuoMorph Home 1.0",
            "device=${profile.name}",
            "window=${width}x${height} inner=$inner p=${"%.3f".format(progress)}",
            "hinge=${debug.rawAngle?.let { "%.1f°".format(it) } ?: "n/a"}",
            "source=${debug.source} distinct=${debug.distinctAngles} continuous=${debug.continuous}",
            "long-press empty area: hide diagnostics"
        )
        var y = 62f
        lines.forEach {
            canvas.drawText(it, 42f, y, debugPaint)
            y += 36f
        }
    }

    private fun launchAt(x: Float, y: Float): Boolean {
        val inner = DeviceProfile.isInner(width,height)
        val count = min(apps.size, if (inner) profile.innerColumns*5 else 24)
        for (i in 0 until count) {
            val start = coverCell(i, onInnerCanvas = inner)
            val end = if (inner) innerCell(i) else coverCell(i, onInnerCanvas = false)
            val t = if (inner) ease(progress) else 0f
            val cx = lerp(start.x,end.x,t)
            val cy = lerp(start.y,end.y,t)
            val s = lerp(start.size,end.size,t)
            if (hypot(x-cx,y-cy) <= s*0.72f) {
                try {
                    context.startActivity(android.content.Intent().setComponent(apps[i].component).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
                } catch (_: Throwable) {}
                return true
            }
        }
        return true
    }

    private fun tickHaptic() {
        try {
            if (Build.VERSION.SDK_INT >= 31) {
                val vm = context.getSystemService(VibratorManager::class.java)
                vm?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(12, 70))
            } else {
                @Suppress("DEPRECATION")
                (context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)?.vibrate(12)
            }
        } catch (_: Throwable) {}
    }

    private fun foldStrength(p: Float): Float = sin(Math.PI * p.toDouble()).toFloat().coerceIn(0f,1f)
    private fun ease(v: Float): Float {
        val t = v.coerceIn(0f,1f)
        return t*t*(3f-2f*t)
    }
    private fun lerp(a: Float,b: Float,t: Float) = a + (b-a)*t
}
