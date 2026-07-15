package com.example.trackrate.ui.image

import android.content.Context
import android.graphics.Matrix
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.appcompat.widget.AppCompatImageView
import kotlin.math.max
import kotlin.math.min

class ZoomImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private val matrix = Matrix()
    private val matrixValues = FloatArray(9)
    private var minScale = 1f
    private var maxScale = 4f

    private val scaleDetector = ScaleGestureDetector(context, ScaleListener())
    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(event: MotionEvent): Boolean {
            val currentScale = currentScale()
            val targetScale = if (currentScale > minScale + 0.05f) minScale else min(maxScale, minScale * 2f)
            zoomTo(targetScale, event.x, event.y)
            return true
        }
    })

    init {
        scaleType = ScaleType.MATRIX
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)
        return true
    }

    override fun setImageDrawable(drawable: android.graphics.drawable.Drawable?) {
        super.setImageDrawable(drawable)
        resetZoom()
    }

    fun resetZoom() {
        drawable ?: return
        matrix.reset()
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        if (viewWidth <= 0f || viewHeight <= 0f) return

        val drawableWidth = drawable.intrinsicWidth.toFloat()
        val drawableHeight = drawable.intrinsicHeight.toFloat()
        if (drawableWidth <= 0f || drawableHeight <= 0f) return

        val scale = min(viewWidth / drawableWidth, viewHeight / drawableHeight)
        minScale = scale
        maxScale = scale * 4f

        val dx = (viewWidth - drawableWidth * scale) / 2f
        val dy = (viewHeight - drawableHeight * scale) / 2f
        matrix.setScale(scale, scale)
        matrix.postTranslate(dx, dy)
        imageMatrix = matrix
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        resetZoom()
    }

    private fun currentScale(): Float {
        matrix.getValues(matrixValues)
        return matrixValues[Matrix.MSCALE_X]
    }

    private fun zoomTo(targetScale: Float, focusX: Float, focusY: Float) {
        val clampedScale = max(minScale, min(targetScale, maxScale))
        val factor = clampedScale / currentScale()
        matrix.postScale(factor, factor, focusX, focusY)
        imageMatrix = matrix
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val scaleFactor = detector.scaleFactor
            val newScale = currentScale() * scaleFactor
            if (newScale in minScale..maxScale) {
                matrix.postScale(scaleFactor, scaleFactor, detector.focusX, detector.focusY)
                imageMatrix = matrix
            }
            return true
        }
    }
}
