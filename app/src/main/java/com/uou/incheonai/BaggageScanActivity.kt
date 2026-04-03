package com.uou.incheonai

import android.graphics.Bitmap
import android.graphics.Bitmap.Config.ARGB_8888
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import com.google.android.material.button.MaterialButton
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class BaggageScanActivity : BasePrototypeActivity() {
    private companion object {
        const val TAG = "BaggageScanActivity"
    }

    private lateinit var presetInput: AutoCompleteTextView
    private lateinit var previewImage: ImageView
    private lateinit var overlayView: BaggageOverlayView
    private lateinit var scanStatusText: TextView
    private lateinit var recognizedItemsContainer: LinearLayout
    private lateinit var detector: BaggageObjectDetector
    private lateinit var inferenceExecutor: ExecutorService
    private var pendingCameraUri: Uri? = null

    private val cameraCapture =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && pendingCameraUri != null) {
                PrototypeSession.selectedImageUri = pendingCameraUri
                PrototypeSession.resetAnalysis()
                PrototypeSession.isDemoVisualization = false
                renderSelectedImageUri(pendingCameraUri!!)
                scanStatusText.text = getString(R.string.status_camera_success)
            } else {
                scanStatusText.text = getString(R.string.camera_capture_failed)
            }
        }

    private val photoPicker =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                PrototypeSession.selectedImageUri = uri
                PrototypeSession.resetAnalysis()
                PrototypeSession.isDemoVisualization = false
                renderSelectedImageUri(uri)
                scanStatusText.text = getString(R.string.status_gallery_success)
            } else {
                scanStatusText.text = getString(R.string.status_image_idle)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PrototypeRepository.bindStrings { id, args -> getString(id, *args) }
        setContentView(R.layout.activity_baggage_scan)
        applyWindowInsets(R.id.rootScan)

        detector = BaggageObjectDetector(this)
        inferenceExecutor = Executors.newSingleThreadExecutor()

        presetInput = findViewById(R.id.presetInput)
        previewImage = findViewById(R.id.previewImage)
        overlayView = findViewById(R.id.overlayView)
        scanStatusText = findViewById(R.id.scanStatusText)
        recognizedItemsContainer = findViewById(R.id.recognizedItemsContainer)

        presetInput.setAdapter(
            ArrayAdapter(
                this,
                android.R.layout.simple_list_item_1,
                PrototypeRepository.scanPresets.map { it.title },
            ),
        )
        presetInput.setText(PrototypeSession.selectedPresetTitle, false)

        findViewById<MaterialButton>(R.id.captureImageButton).setOnClickListener { launchCameraCapture() }
        findViewById<MaterialButton>(R.id.selectImageButton).setOnClickListener {
            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        findViewById<MaterialButton>(R.id.analyzeButton).setOnClickListener { analyze() }
        findViewById<MaterialButton>(R.id.demoPresetButton).setOnClickListener {
            applyDemoPresetAnalysis()
        }
        findViewById<MaterialButton>(R.id.previousButton).setOnClickListener { finish() }
        findViewById<MaterialButton>(R.id.nextButton).setOnClickListener {
            if (PrototypeSession.analysisReport == null) {
                if (PrototypeSession.selectedImageUri != null) {
                    analyze()
                } else {
                    applyDemoPresetAnalysis()
                }
            }
            if (PrototypeSession.analysisReport == null) {
                applyDemoPresetAnalysis()
            }
            if (PrototypeSession.analysisReport != null) {
                openNext(RuleComparisonActivity::class.java)
            }
        }

        renderState()
    }

    override fun onDestroy() {
        super.onDestroy()
        inferenceExecutor.shutdown()
    }

    private fun renderState() {
        scanStatusText.text = getString(R.string.status_image_idle)
        PrototypeSession.selectedImageUri?.let { renderSelectedImageUri(it) }
        PrototypeUi.renderRecognizedItems(
            layoutInflater,
            recognizedItemsContainer,
            PrototypeSession.analysisReport?.recognizedItems ?: emptyList(),
            getString(R.string.empty_recognized_items),
        )
        if (PrototypeSession.overlayDetections.isNotEmpty()) {
            overlayView.setDetections(
                PrototypeSession.overlayDetections,
                PrototypeSession.sourceImageWidth,
                PrototypeSession.sourceImageHeight,
            )
        }
        if (PrototypeSession.isDemoVisualization) {
            renderDemoPreview()
        }
    }

    private fun renderSelectedImageUri(uri: Uri) {
        previewImage.setImageURI(uri)
        previewImage.scaleType = ImageView.ScaleType.FIT_CENTER
        overlayView.setDetections(emptyList(), 1, 1)
    }

    private fun analyze() {
        PrototypeSession.selectedPresetTitle = presetInput.text?.toString()
            ?.takeIf { it.isNotBlank() }
            ?: PrototypeRepository.scanPresets.first().title

        val selectedUri = PrototypeSession.selectedImageUri
        if (selectedUri == null) {
            runPresetFallbackAnalysis()
            return
        }

        scanStatusText.text = getString(R.string.status_detecting_objects)
        inferenceExecutor.execute {
            try {
                val bitmap = decodeBitmap(selectedUri)
                val visionResult = detector.detect(bitmap)

                runOnUiThread {
                    if (visionResult.recognizedItems.isEmpty()) {
                        PrototypeSession.analysisReport = null
                        PrototypeSession.overlayDetections = emptyList()
                        PrototypeSession.isDemoVisualization = false
                        overlayView.setDetections(emptyList(), 1, 1)
                        scanStatusText.text = getString(R.string.status_no_actionable_detection)
                        PrototypeUi.renderRecognizedItems(
                            layoutInflater,
                            recognizedItemsContainer,
                            emptyList(),
                            getString(R.string.empty_recognized_items),
                        )
                        return@runOnUiThread
                    }

                    val report = BaggageRuleEngine.analyze(
                        flight = PrototypeSession.currentFlight,
                        airlineRuleProfile = PrototypeSession.selectedAirlineProfile(),
                        recognizedItems = visionResult.recognizedItems,
                        stringProvider = { id, args -> getString(id, *args) },
                    )
                    PrototypeSession.analysisReport = report
                    PrototypeSession.overlayDetections = visionResult.overlayDetections
                    PrototypeSession.sourceImageWidth = visionResult.sourceImageWidth
                    PrototypeSession.sourceImageHeight = visionResult.sourceImageHeight
                    PrototypeSession.isDemoVisualization = false

                    previewImage.setImageBitmap(bitmap)
                    previewImage.scaleType = ImageView.ScaleType.FIT_CENTER
                    overlayView.setDetections(
                        visionResult.overlayDetections,
                        visionResult.sourceImageWidth,
                        visionResult.sourceImageHeight,
                    )
                    scanStatusText.text = getString(R.string.status_analysis_with_image, PrototypeSession.selectedPresetTitle)
                    PrototypeUi.renderRecognizedItems(
                        layoutInflater,
                        recognizedItemsContainer,
                        report.recognizedItems,
                        getString(R.string.empty_recognized_items),
                    )
                }
            } catch (exception: Exception) {
                Log.e(TAG, "Object detection failed", exception)
                runOnUiThread {
                    scanStatusText.text = buildString {
                        append(getString(R.string.status_detection_error))
                        exception.message?.takeIf { it.isNotBlank() }?.let {
                            append("\n")
                            append(it)
                        }
                    }
                }
            }
        }
    }

    private fun runPresetFallbackAnalysis() {
        applyDemoPresetAnalysis()
    }

    private fun applyDemoPresetAnalysis() {
        val report = BaggageRuleEngine.analyze(
            flight = PrototypeSession.currentFlight,
            airlineRuleProfile = PrototypeSession.selectedAirlineProfile(),
            recognizedItems = PrototypeSession.selectedPreset().detectedItems,
            stringProvider = { id, args -> getString(id, *args) },
        )
        PrototypeSession.analysisReport = report
        PrototypeSession.overlayDetections = buildDemoOverlayDetections(report)
        PrototypeSession.sourceImageWidth = 320
        PrototypeSession.sourceImageHeight = 220
        PrototypeSession.isDemoVisualization = true
        renderDemoPreview()
        scanStatusText.text = getString(R.string.status_demo_preset_applied)
        PrototypeUi.renderRecognizedItems(
            layoutInflater,
            recognizedItemsContainer,
            report.recognizedItems,
            getString(R.string.empty_recognized_items),
        )
    }

    private fun renderDemoPreview() {
        previewImage.scaleType = ImageView.ScaleType.FIT_CENTER
        previewImage.setImageResource(currentDemoDrawable())
        overlayView.setDetections(
            PrototypeSession.overlayDetections,
            PrototypeSession.sourceImageWidth,
            PrototypeSession.sourceImageHeight,
        )
    }

    @DrawableRes
    private fun currentDemoDrawable(): Int {
        return when (PrototypeSession.selectedPresetTitle) {
            getString(R.string.preset_standard) -> R.drawable.ic_demo_baggage_standard
            getString(R.string.preset_electronics) -> R.drawable.ic_demo_baggage_electronics
            getString(R.string.preset_mix) -> R.drawable.ic_demo_baggage_mix
            else -> R.drawable.ic_demo_baggage_standard
        }
    }

    private fun buildDemoOverlayDetections(report: AnalysisReport): List<OverlayDetection> {
        val boxes = when (PrototypeSession.selectedPresetTitle) {
            getString(R.string.preset_electronics) -> listOf(
                android.graphics.RectF(66f, 96f, 170f, 160f),
                android.graphics.RectF(184f, 98f, 230f, 158f),
                android.graphics.RectF(224f, 120f, 278f, 176f),
            )
            getString(R.string.preset_mix) -> listOf(
                android.graphics.RectF(56f, 104f, 110f, 160f),
                android.graphics.RectF(118f, 96f, 172f, 148f),
                android.graphics.RectF(178f, 122f, 232f, 178f),
            )
            else -> listOf(
                android.graphics.RectF(182f, 62f, 252f, 106f),
                android.graphics.RectF(214f, 138f, 262f, 192f),
                android.graphics.RectF(52f, 72f, 214f, 188f),
            )
        }

        return report.itemDecisions.mapIndexed { index, itemDecision ->
            val rect = boxes.getOrElse(index) { android.graphics.RectF(48f, 48f, 180f, 150f) }
            OverlayDetection(
                rect = rect,
                title = itemDecision.item.name,
                subtitle = itemDecision.decision.label,
                severity = itemDecision.decision.severity,
                score = 0.99f,
            )
        }
    }

    private fun decodeBitmap(uri: Uri): Bitmap {
        val decoded = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.isMutableRequired = false
            }
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(contentResolver, uri)
        }

        return if (decoded.config == ARGB_8888) {
            decoded
        } else {
            decoded.copy(ARGB_8888, false)
        }
    }

    private fun launchCameraCapture() {
        val imageFile = File.createTempFile(
            "baggage_capture_",
            ".jpg",
            File(cacheDir, "images").apply { mkdirs() },
        )
        val captureUri = FileProvider.getUriForFile(
            this,
            "${applicationContext.packageName}.fileprovider",
            imageFile,
        )
        pendingCameraUri = captureUri
        cameraCapture.launch(captureUri)
    }
}
