package com.uou.incheonai

import android.os.Bundle
import android.widget.TextView
import com.google.android.material.button.MaterialButton

class IntegrationStatusActivity : BasePrototypeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_integration_status)
        applyWindowInsets(R.id.rootIntegration)

        val flight = PrototypeSession.currentFlight
        val imageState = if (PrototypeSession.selectedImageUri != null) {
            getString(R.string.integration_image_connected)
        } else {
            getString(R.string.integration_image_idle)
        }

        findViewById<TextView>(R.id.integrationSummaryText).text =
            buildString {
                append(getString(R.string.integration_intro))
                append("\n\n")
                append("• ")
                append(getString(R.string.integration_booking))
                append("\n")
                append("• ")
                append(getString(R.string.integration_ai))
                append("\n")
                append("• ")
                append(getString(R.string.integration_rules))
                append("\n\n")
                append("현재 연동 항공편: ")
                append(flight.airlineName)
                append(" ")
                append(flight.flightNumber)
                append(" / ")
                append(flight.route)
                append("\n")
                append(imageState)
                append("\n")
                append(getString(R.string.integration_engine))
            }

        findViewById<MaterialButton>(R.id.previousButton).setOnClickListener { finish() }
        findViewById<MaterialButton>(R.id.nextButton).setOnClickListener {
            openNext(OverviewActivity::class.java)
        }
    }
}
