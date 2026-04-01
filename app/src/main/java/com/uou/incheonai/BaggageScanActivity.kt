package com.uou.incheonai

import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import com.google.android.material.button.MaterialButton
import java.io.File

class BaggageScanActivity : BasePrototypeActivity() {
    private lateinit var presetInput: AutoCompleteTextView
    private lateinit var previewImage: ImageView
    private lateinit var scanStatusText: TextView
    private lateinit var recognizedItemsContainer: LinearLayout
    private var pendingCameraUri: Uri? = null

    private val cameraCapture =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && pendingCameraUri != null) {
                PrototypeSession.selectedImageUri = pendingCameraUri
                previewImage.setImageURI(pendingCameraUri)
                previewImage.scaleType = ImageView.ScaleType.CENTER_CROP
                scanStatusText.text = getString(R.string.status_camera_success)
            } else {
                scanStatusText.text = getString(R.string.camera_capture_failed)
            }
        }

    private val photoPicker =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                PrototypeSession.selectedImageUri = uri
                previewImage.setImageURI(uri)
                previewImage.scaleType = ImageView.ScaleType.CENTER_CROP
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

        presetInput = findViewById(R.id.presetInput)
        previewImage = findViewById(R.id.previewImage)
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
        findViewById<MaterialButton>(R.id.previousButton).setOnClickListener { finish() }
        findViewById<MaterialButton>(R.id.nextButton).setOnClickListener {
            if (PrototypeSession.analysisReport == null) analyze()
            openNext(RuleComparisonActivity::class.java)
        }

        renderState()
    }

    private fun renderState() {
        scanStatusText.text = getString(R.string.status_image_idle)
        PrototypeSession.selectedImageUri?.let {
            previewImage.setImageURI(it)
            previewImage.scaleType = ImageView.ScaleType.CENTER_CROP
        }
        PrototypeUi.renderRecognizedItems(
            layoutInflater,
            recognizedItemsContainer,
            PrototypeSession.analysisReport?.recognizedItems ?: emptyList(),
            getString(R.string.empty_recognized_items),
        )
    }

    private fun analyze() {
        PrototypeSession.selectedPresetTitle = presetInput.text?.toString()
            ?.takeIf { it.isNotBlank() }
            ?: PrototypeRepository.scanPresets.first().title
        val report = BaggageRuleEngine.analyze(
            flight = PrototypeSession.currentFlight,
            airlineRuleProfile = PrototypeSession.selectedAirlineProfile(),
            recognizedItems = PrototypeSession.selectedPreset().detectedItems,
            stringProvider = { id, args -> getString(id, *args) },
        )
        PrototypeSession.analysisReport = report
        scanStatusText.text = if (PrototypeSession.selectedImageUri != null) {
            getString(R.string.status_analysis_with_image, PrototypeSession.selectedPresetTitle)
        } else {
            getString(R.string.status_analysis_without_image, PrototypeSession.selectedPresetTitle)
        }
        PrototypeUi.renderRecognizedItems(
            layoutInflater,
            recognizedItemsContainer,
            report.recognizedItems,
            getString(R.string.empty_recognized_items),
        )
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
