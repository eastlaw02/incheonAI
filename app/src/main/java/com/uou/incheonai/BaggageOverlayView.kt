package com.uou.incheonai

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import kotlin.math.max
import kotlin.math.min

class BaggageOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
    private val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }
    private val labelBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 34f
        typeface = resources.getFont(R.font.pretendard_bold)
    }
    private val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 26f
        typeface = resources.getFont(R.font.pretendard_medium)
    }

    private var detections: List<OverlayDetection> = emptyList()
    private var sourceWidth: Int = 1
    private var sourceHeight: Int = 1

    fun setDetections(
        detections: List<OverlayDetection>,
        sourceWidth: Int,
        sourceHeight: Int,
    ) {
        this.detections = detections
        this.sourceWidth = max(sourceWidth, 1)
        this.sourceHeight = max(sourceHeight, 1)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (detections.isEmpty()) return

        val scale = min(width.toFloat() / sourceWidth.toFloat(), height.toFloat() / sourceHeight.toFloat())
        val drawnWidth = sourceWidth * scale
        val drawnHeight = sourceHeight * scale
        val offsetX = (width - drawnWidth) / 2f
        val offsetY = (height - drawnHeight) / 2f

        detections.forEach { detection ->
            val color = when (detection.severity) {
                DecisionSeverity.SAFE -> ContextCompat.getColor(context, R.color.safe_text)
                DecisionSeverity.CAUTION -> ContextCompat.getColor(context, R.color.caution_text)
                DecisionSeverity.DANGER -> ContextCompat.getColor(context, R.color.danger_text)
            }
            boxPaint.color = color
            labelBgPaint.color = color

            val rect = RectF(
                offsetX + detection.rect.left * scale,
                offsetY + detection.rect.top * scale,
                offsetX + detection.rect.right * scale,
                offsetY + detection.rect.bottom * scale,
            )

            canvas.drawRoundRect(rect, 18f, 18f, boxPaint)

            val titleWidth = titlePaint.measureText(detection.title)
            val subtitleWidth = subtitlePaint.measureText(detection.subtitle)
            val labelWidth = max(titleWidth, subtitleWidth) + 40f
            val labelHeight = 78f
            val labelTop = if (rect.top > labelHeight + 12f) rect.top - labelHeight else rect.top + 8f
            val labelRect = RectF(rect.left, labelTop, rect.left + labelWidth, labelTop + labelHeight)
            canvas.drawRoundRect(labelRect, 18f, 18f, labelBgPaint)
            canvas.drawText(detection.title, labelRect.left + 20f, labelRect.top + 30f, titlePaint)
            canvas.drawText(detection.subtitle, labelRect.left + 20f, labelRect.top + 62f, subtitlePaint)
        }
    }
}
