package com.uou.incheonai

import android.os.Bundle
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class MainActivity : BasePrototypeActivity() {
    private lateinit var airlineInput: TextInputEditText
    private lateinit var flightNumberInput: TextInputEditText
    private lateinit var routeInput: TextInputEditText
    private lateinit var departureInput: TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PrototypeRepository.bindStrings { id, args -> getString(id, *args) }
        setContentView(R.layout.activity_main)
        applyWindowInsets(R.id.rootFlight)

        bindViews()
        bindActions()
        renderInitialState()
    }

    private fun bindViews() {
        airlineInput = findViewById(R.id.airlineInput)
        flightNumberInput = findViewById(R.id.flightNumberInput)
        routeInput = findViewById(R.id.routeInput)
        departureInput = findViewById(R.id.departureInput)
    }

    private fun bindActions() {
        findViewById<MaterialButton>(R.id.nextButton).setOnClickListener {
            PrototypeSession.currentFlight = readFlightFromInputs()
            PrototypeSession.resetAnalysis()
            openNext(BaggageScanActivity::class.java)
        }
    }

    private fun renderInitialState() {
        applyFlightToInputs(PrototypeRepository.sampleFlight)
    }

    private fun applyFlightToInputs(flightContext: FlightContext) {
        airlineInput.setText(flightContext.airlineName)
        flightNumberInput.setText(flightContext.flightNumber)
        routeInput.setText(flightContext.route)
        departureInput.setText(flightContext.departureLabel())
        PrototypeSession.currentFlight = flightContext
    }

    private fun readFlightFromInputs(): FlightContext {
        val sampleFlight = PrototypeRepository.sampleFlight

        val airlineName = airlineInput.text?.toString()?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: sampleFlight.airlineName

        val flightNumber = flightNumberInput.text?.toString()?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: sampleFlight.flightNumber

        val route = routeInput.text?.toString()?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: sampleFlight.route

        val departureText = departureInput.text?.toString()?.trim()
            ?.takeIf { it.isNotBlank() }

        val departureMinutes = when {
            departureText == null -> sampleFlight.departureMinutes
            PrototypeRepository.departureOptions.containsKey(departureText) -> {
                PrototypeRepository.departureOptions[departureText] ?: sampleFlight.departureMinutes
            }
            else -> {
                departureText.replace("분", "").toIntOrNull() ?: sampleFlight.departureMinutes
            }
        }

        return FlightContext(
            airlineName = airlineName,
            flightNumber = flightNumber,
            route = route,
            departureMinutes = departureMinutes
        )
    }

    private fun FlightContext.departureLabel(): String {
        return PrototypeRepository.departureOptions.entries
            .firstOrNull { it.value == departureMinutes }
            ?.key
            ?: "${departureMinutes}분"
    }
}