package com.example.androidpdfviewwer

import android.content.Context
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.RectF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.appcompat.widget.AppCompatImageView

class ZoomableImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr),
    View.OnTouchListener,
    ScaleGestureDetector.OnScaleGestureListener {

    private enum class Mode { NONE, DRAG, ZOOM }

    private var mode = Mode.NONE
    private val matrix = Matrix()
    private val prevMatrix = Matrix()

    private val startPoint = PointF()
    private val midPoint = PointF()

    private var minScale = 1.0f
    private var maxScale = 4.0f
    private var saveScale = 1.0f

    private var viewWidth = 0f
    private var viewHeight = 0f
    private var origWidth = 0f
    private var origHeight = 0f

    private val scaleDetector: ScaleGestureDetector = ScaleGestureDetector(context, this)
    private val gestureDetector: GestureDetector

    init {
        super.setClickable(true)
        imageMatrix = matrix
        scaleType = ScaleType.MATRIX
        setOnTouchListener(this)

        gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                val targetScale = if (saveScale > 1.5f) minScale else 2.5f
                val factor = targetScale / saveScale
                zoomTo(factor, e.x, e.y)
                return true
            }
        })
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        viewWidth = MeasureSpec.getSize(widthMeasureSpec).toFloat()
        viewHeight = MeasureSpec.getSize(heightMeasureSpec).toFloat()

        val drawable = drawable
        if (drawable != null && drawable.intrinsicWidth != 0 && drawable.intrinsicHeight != 0) {
            origWidth = drawable.intrinsicWidth.toFloat()
            origHeight = drawable.intrinsicHeight.toFloat()
            if (saveScale == 1.0f) {
                fitToScreen()
            }
        }
    }

    private fun fitToScreen() {
        if (origWidth == 0f || origHeight == 0f || viewWidth == 0f || viewHeight == 0f) return

        val scale: Float
        val scaleX = viewWidth / origWidth
        val scaleY = viewHeight / origHeight
        scale = scaleX.coerceAtMost(scaleY)

        matrix.setScale(scale, scale)
        saveScale = 1.0f

        val redundantYSpace = viewHeight - (scale * origHeight)
        val redundantXSpace = viewWidth - (scale * origWidth)

        matrix.postTranslate(redundantXSpace / 2f, redundantYSpace / 2f)
        imageMatrix = matrix
    }

    fun resetZoom() {
        fitToScreen()
    }

    private fun zoomTo(factor: Float, focusX: Float, focusY: Float) {
        var scaleFactor = factor
        val oldScale = saveScale
        saveScale *= scaleFactor

        if (saveScale > maxScale) {
            saveScale = maxScale
            scaleFactor = maxScale / oldScale
        } else if (saveScale < minScale) {
            saveScale = minScale
            scaleFactor = minScale / oldScale
        }

        if (origWidth * saveScale <= viewWidth || origHeight * saveScale <= viewHeight) {
            matrix.postScale(scaleFactor, scaleFactor, viewWidth / 2f, viewHeight / 2f)
        } else {
            matrix.postScale(scaleFactor, scaleFactor, focusX, focusY)
        }

        fixTranslation()
        imageMatrix = matrix
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        scaleDetector.onTouchEvent(event)

        val currentPoint = PointF(event.x, event.y)

        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                prevMatrix.set(matrix)
                startPoint.set(currentPoint)
                mode = Mode.DRAG
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                startPoint.set(event.getX(0), event.getY(0))
                midPoint.set((event.getX(0) + event.getX(1)) / 2f, (event.getY(0) + event.getY(1)) / 2f)
                mode = Mode.ZOOM
            }

            MotionEvent.ACTION_MOVE -> {
                if (mode == Mode.DRAG && saveScale > minScale) {
                    val deltaX = currentPoint.x - startPoint.x
                    val deltaY = currentPoint.y - startPoint.y
                    matrix.set(prevMatrix)
                    matrix.postTranslate(deltaX, deltaY)
                    fixTranslation()
                }
            }

            MotionEvent.ACTION_POINTER_UP -> mode = Mode.NONE
            MotionEvent.ACTION_UP -> mode = Mode.NONE
        }

        imageMatrix = matrix
        // Allow parent RecyclerView to scroll vertically when zoomed out completely
        parent?.requestDisallowInterceptTouchEvent(saveScale > minScale)
        return true
    }

    override fun onScale(detector: ScaleGestureDetector): Boolean {
        var scaleFactor = detector.scaleFactor
        val oldScale = saveScale
        saveScale *= scaleFactor

        if (saveScale > maxScale) {
            saveScale = maxScale
            scaleFactor = maxScale / oldScale
        } else if (saveScale < minScale) {
            saveScale = minScale
            scaleFactor = minScale / oldScale
        }

        if (origWidth * saveScale <= viewWidth || origHeight * saveScale <= viewHeight) {
            matrix.postScale(scaleFactor, scaleFactor, viewWidth / 2f, viewHeight / 2f)
        } else {
            matrix.postScale(scaleFactor, scaleFactor, detector.focusX, detector.focusY)
        }

        fixTranslation()
        return true
    }

    override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
        mode = Mode.ZOOM
        return true
    }

    override fun onScaleEnd(detector: ScaleGestureDetector) {}

    private fun fixTranslation() {
        val rect = RectF()
        matrix.mapRect(rect)

        val transX = when {
            rect.width() <= viewWidth -> (viewWidth - rect.width()) / 2f - rect.left
            rect.left > 0 -> -rect.left
            rect.right < viewWidth -> viewWidth - rect.right
            else -> 0f
        }

        val transY = when {
            rect.height() <= viewHeight -> (viewHeight - rect.height()) / 2f - rect.top
            rect.top > 0 -> -rect.top
            rect.bottom < viewHeight -> viewHeight - rect.bottom
            else -> 0f
        }

        matrix.postTranslate(transX, transY)
    }
}
