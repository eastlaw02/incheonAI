package com.uou.incheonai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.vision.detector.ObjectDetector

class BaggageObjectDetector(private val context: Context) {
    private val detector: ObjectDetector by lazy {
        val options = ObjectDetector.ObjectDetectorOptions.builder()
            .setBaseOptions(
                BaseOptions.builder()
                    .setNumThreads(4)
                    .build(),
            )
            .setMaxResults(10)
            .setScoreThreshold(0.35f)
            .build()

        ObjectDetector.createFromFileAndOptions(context, MODEL_NAME, options)
    }

    fun detect(bitmap: Bitmap): VisionDetectionResult {
        val tensorImage = TensorImage.fromBitmap(bitmap)
        val result = detector.detect(tensorImage)
        val mappedDetections = mutableListOf<OverlayDetection>()
        val recognizedItems = linkedMapOf<String, RecognizedItem>()
        val rawLabels = mutableListOf<String>()

        result.forEach { detection ->
            val category = detection.categories.firstOrNull() ?: return@forEach
            val rawLabel = category.label
            rawLabels += rawLabel
            val mappedItem = BaggageVisionMapper.mapLabel(context, rawLabel) ?: return@forEach
            recognizedItems.putIfAbsent(mappedItem.name, mappedItem)

            val policy = BaggageRuleEngine.previewDecisionForItem(
                item = mappedItem,
                airlineRuleProfile = PrototypeSession.selectedAirlineProfile(),
                stringProvider = { id, args -> context.getString(id, *args) },
            )

            val boundingBox = detection.boundingBox
            mappedDetections += OverlayDetection(
                rect = RectF(
                    boundingBox.left.toFloat(),
                    boundingBox.top.toFloat(),
                    boundingBox.right.toFloat(),
                    boundingBox.bottom.toFloat(),
                ),
                title = mappedItem.name,
                subtitle = policy.label,
                severity = policy.severity,
                score = category.score,
            )
        }

        return VisionDetectionResult(
            recognizedItems = recognizedItems.values.toList(),
            overlayDetections = mappedDetections,
            sourceImageWidth = bitmap.width,
            sourceImageHeight = bitmap.height,
            rawDetectedLabels = rawLabels,
        )
    }

    companion object {
        private const val MODEL_NAME = "efficientdet_lite0.tflite"
    }
}
