package br.com.bryan.facial.recognition

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.pow
import kotlin.math.sqrt

class FaceOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 8f
    }
    private val pointPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        strokeWidth = 4f
    }
    private var faceRect: Rect? = null
    private var facePoints: List<PointF>? = null
    private lateinit var referenceFacePoints: List<PointF> // Pontos de referência da foto de referência

    // Método para configurar os pontos de referência da foto de referência
    fun setReferenceFacePoints(referencePoints: List<PointF>) {
        this.referenceFacePoints = referencePoints
    }

    fun updateFace(faceRect: Rect?, facePoints: List<PointF>?) {
        this.faceRect = faceRect
        facePoints.also { this.facePoints = it }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        faceRect?.let { rect ->
            canvas.drawRect(rect, paint)
        }
        facePoints?.forEach { point ->
            canvas.drawCircle(point.x, point.y, 3f, pointPaint)
        }
    }
}
