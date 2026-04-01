package com.uou.incheonai

import android.net.Uri

object PrototypeSession {
    var currentFlight: FlightContext = PrototypeRepository.sampleFlight
    var selectedPresetTitle: String = PrototypeRepository.scanPresets.first().title
    var selectedImageUri: Uri? = null
    var analysisReport: AnalysisReport? = null

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
    }
}
