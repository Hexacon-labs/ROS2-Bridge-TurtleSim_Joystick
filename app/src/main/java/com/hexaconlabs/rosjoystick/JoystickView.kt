package com.hexaconlabs.rosjoystick

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Simple analog joystick view.
 * Reports normalized coordinates where:
 *   x: -1 (full left)  ... +1 (full right)
 *   y: -1 (full down)  ... +1 (full up)
 * Center / released reports (0, 0).
 *  * @author Shibin AK
 *  * @version V1.0 (2026-07-29)
 *  * Website: www.hexaconlabs.com
 *  */

class JoystickView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    interface OnJoystickMovedListener {
        fun onMoved(x: Float, y: Float)
        fun onReleased()
    }

    private var listener: OnJoystickMovedListener? = null

    private val basePaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.joystick_base)
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val baseStrokePaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.joystick_base_stroke)
        style = Paint.Style.STROKE
        strokeWidth = 3f
        alpha = 90
        isAntiAlias = true
    }
    private val knobPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.joystick_knob)
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val knobHighlightPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.joystick_knob_dark)
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
    }

    private var centerX = 0f
    private var centerY = 0f
    private var baseRadius = 0f
    private var knobRadius = 0f

    private var knobX = 0f
    private var knobY = 0f

    fun setOnJoystickMovedListener(l: OnJoystickMovedListener) {
        listener = l
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        centerX = w / 2f
        centerY = h / 2f
        baseRadius = min(w, h) / 2f * 0.9f
        knobRadius = baseRadius * 0.4f
        knobX = centerX
        knobY = centerY
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawCircle(centerX, centerY, baseRadius, basePaint)
        canvas.drawCircle(centerX, centerY, baseRadius - 2f, baseStrokePaint)
        canvas.drawCircle(knobX, knobY, knobRadius, knobPaint)
        canvas.drawCircle(knobX, knobY, knobRadius - 4f, knobHighlightPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                var dx = event.x - centerX
                var dy = event.y - centerY
                val distance = sqrt(dx * dx + dy * dy)
                val maxDistance = baseRadius - knobRadius

                if (distance > maxDistance && distance > 0f) {
                    val scale = maxDistance / distance
                    dx *= scale
                    dy *= scale
                }

                knobX = centerX + dx
                knobY = centerY + dy
                invalidate()

                val normX = (dx / maxDistance).coerceIn(-1f, 1f)
                // Screen y grows downward, so flip it: pushing up = positive y
                val normY = (-dy / maxDistance).coerceIn(-1f, 1f)
                listener?.onMoved(normX, normY)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                knobX = centerX
                knobY = centerY
                invalidate()
                listener?.onReleased()
            }
        }
        return true
    }
}