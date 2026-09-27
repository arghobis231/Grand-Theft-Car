package com.example.grandtheftcar.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.view.Choreographer
import android.view.MotionEvent
import android.view.View
import com.example.grandtheftcar.engine.GameEngine

@SuppressLint("ViewConstructor")
class GameSurfaceView(
    context: Context,
    val engine: GameEngine
) : View(context), Choreographer.FrameCallback {

    private var isRunning: Boolean = false
    private var lastFrameTimeNanos: Long = 0L

    init {
        isFocusable = true
        isFocusableInTouchMode = true
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startLoop()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopLoop()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            engine.onSurfaceChanged(w.toFloat(), h.toFloat())
        }
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!isRunning) return

        val dt = if (lastFrameTimeNanos == 0L) {
            0.0166f
        } else {
            val elapsedSec = (frameTimeNanos - lastFrameTimeNanos) / 1_000_000_000f
            elapsedSec.coerceIn(0.001f, 0.05f)
        }
        lastFrameTimeNanos = frameTimeNanos

        try {
            engine.update(dt)
            invalidate()
        } catch (_: Exception) {
        }

        if (isRunning) {
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        try {
            engine.draw(canvas)
        } catch (_: Exception) {
        }
    }

    private fun startLoop() {
        if (isRunning) return
        isRunning = true
        lastFrameTimeNanos = 0L
        Choreographer.getInstance().removeFrameCallback(this)
        Choreographer.getInstance().postFrameCallback(this)
    }

    private fun stopLoop() {
        isRunning = false
        Choreographer.getInstance().removeFrameCallback(this)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                engine.onTouchDown(event.x, event.y)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                engine.onTouchMove(event.x, event.y)
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                engine.onTouchUp(event.x, event.y)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    fun onPause() {
        engine.onPause()
        stopLoop()
    }

    fun onResume() {
        engine.onResume()
        startLoop()
    }

    fun onDestroy() {
        stopLoop()
        engine.onDestroy()
    }
}
