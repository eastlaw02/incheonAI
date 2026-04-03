package com.uou.incheonai

import android.net.Uri

object PrototypeSession {
    var currentFlight: FlightContext = PrototypeRepository.sampleFlight
    var selectedPresetTitle: String = PrototypeRepository.scanPresets.first().title
    var selectedImageUri: Uri? = null
    var analysisReport: AnalysisReport? = null
    var overlayDetections: List<OverlayDetection> = emptyList()
    var sourceImageWidth: Int = 0
    var sourceImageHeight: Int = 0
    var isDemoVisualization: Boolean = false

    fun selectedPreset(): ScanPreset {
        return PrototypeRepository.scanPresets.firstOrNull { it.title == selectedPresetTitle }
            ?: PrototypeRepository.scanPresets.first()
    }

    fun selectedAirlineProfile(): AirlineRuleProfile {
        return PrototypeRepository.airlineProfiles.firstOrNull { it.airlineName == currentFlight.airlineName }
            ?: PrototypeRepository.airlineProfiles.first()
    }

    fun resetAnalysis() {
        analysisReport = null
        overlayDetections = emptyList()
        sourceImageWidth = 0
        sourceImageHeight = 0
        isDemoVisualization = false
    }
}
