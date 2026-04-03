package com.uou.incheonai

import android.graphics.RectF

data class OverlayDetection(
    val rect: RectF,
    val title: String,
    val subtitle: String,
    val severity: DecisionSeverity,
    val score: Float,
)

data class VisionDetectionResult(
    val recognizedItems: List<RecognizedItem>,
    val overlayDetections: List<OverlayDetection>,
    val sourceImageWidth: Int,
    val sourceImageHeight: Int,
    val rawDetectedLabels: List<String>,
)
