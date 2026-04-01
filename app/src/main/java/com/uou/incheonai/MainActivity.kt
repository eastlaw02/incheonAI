package com.uou.incheonai

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class MainActivity : BasePrototypeActivity() {
    private lateinit var airlineInput: AutoCompleteTextView
    private lateinit var flightNumberInput: TextInputEditText
    private lateinit var routeInput: TextInputEditText
    private lateinit var departureInput: AutoCompleteTextView
    private lateinit var flightSummaryText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PrototypeRepository.bindStrings { id, args -> getString(id, *args) }
        setContentView(R.layout.activity_main)
        applyWindowInsets(R.id.rootFlight)

        bindViews()
        bindDropdowns()
        bindActions()
        renderInitialState()
    }

    private fun bindViews() {
        airlineInput = findViewById(R.id.airlineInput)
        flightNumberInput = findViewById(R.id.flightNumberInput)
        routeInput = findViewById(R.id.routeInput)
        departureInput = findViewById(R.id.departureInput)
        flightSummaryText = findViewById(R.id.flightSummaryText)
    }

    private fun bindDropdowns() {
        airlineInput.setAdapter(
            ArrayAdapter(
                this,
                android.R.layout.simple_list_item_1,
                PrototypeRepository.airlineProfiles.map { it.airlineName },
            ),
        )
        departureInput.setAdapter(
            ArrayAdapter(
                this,
                android.R.layout.simple_list_item_1,
                PrototypeRepository.departureOptions.keys.toList(),
            ),
        )
    }

    private fun bindActions() {
        findViewById<MaterialButton>(R.id.applyFlightButton).setOnClickListener {
            PrototypeSession.currentFlight = readFlightFromInputs()
            PrototypeSession.resetAnalysis()
            renderSummary()
        }
        findViewById<MaterialButton>(R.id.nextButton).setOnClickListener {
            PrototypeSession.currentFlight = readFlightFromInputs()
            PrototypeSession.resetAnalysis()
            openNext(BaggageScanActivity::class.java)
        }
    }

    private fun renderInitialState() {
        applyFlightToInputs(PrototypeRepository.sampleFlight)
        renderSummary()
    }

    private fun applyFlightToInputs(flightContext: FlightContext) {
        airlineInput.setText(flightContext.airlineName, false)
        flightNumberInput.setText(flightContext.flightNumber)
        routeInput.setText(flightContext.route)
        departureInput.setText(flightContext.departureLabel(), false)
        PrototypeSession.currentFlight = flightContext
    }

    private fun readFlightFromInputs(): FlightContext {
        val sampleFlight = PrototypeRepository.sampleFlight
        val airlineName = airlineInput.text?.toString()?.takeIf { it.isNotBlank() } ?: sampleFlight.airlineName
        val flightNumber = flightNumberInput.text?.toString()?.takeIf { it.isNotBlank() } ?: sampleFlight.flightNumber
        val route = routeInput.text?.toString()?.takeIf { it.isNotBlank() } ?: sampleFlight.route
        val departureLabel = departureInput.text?.toString()?.takeIf { it.isNotBlank() }
            ?: PrototypeRepository.departureOptions.keys.first()
        val departureMinutes = PrototypeRepository.departureOptions[departureLabel] ?: sampleFlight.departureMinutes
        return FlightContext(airlineName, flightNumber, route, departureMinutes)
    }

    private fun renderSummary() {
        val flight = PrototypeSession.currentFlight
        flightSummaryText.text = getString(
            R.string.summary_connected_flight,
            flight.airlineName,
            flight.flightNumber,
            flight.route,
            flight.departureLabel(),
        )
    }

    private fun FlightContext.departureLabel(): String {
        return PrototypeRepository.departureOptions.entries.firstOrNull { it.value == departureMinutes }?.key
            ?: "${departureMinutes}분"
    }
}
