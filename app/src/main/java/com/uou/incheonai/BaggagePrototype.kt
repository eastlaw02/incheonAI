package com.uou.incheonai

data class AirlineRuleProfile(
    val airlineName: String,
    val batteryCabinLimitNote: String,
    val liquidCabinLimitMl: Int,
)

data class FlightContext(
    val airlineName: String,
    val flightNumber: String,
    val route: String,
    val departureMinutes: Int,
)

data class ScanPreset(
    val title: String,
    val detectedItems: List<RecognizedItem>,
)

data class RecognizedItem(
    val name: String,
    val category: String,
    val detail: String,
)

enum class DecisionSeverity {
    SAFE,
    CAUTION,
    DANGER,
}

data class ItemDecision(
    val label: String,
    val message: String,
    val severity: DecisionSeverity,
)

data class OverviewGuide(
    val headline: String,
    val body: String,
)

data class AnalysisReport(
    val recognizedItems: List<RecognizedItem>,
    val itemDecisions: List<ItemDecisionViewModel>,
    val overview: OverviewGuide,
)

data class ItemDecisionViewModel(
    val item: RecognizedItem,
    val decision: ItemDecision,
)

object PrototypeRepository {
    private var stringProvider: ((Int, Array<out Any>) -> String)? = null

    fun bindStrings(provider: (Int, Array<out Any>) -> String) {
        stringProvider = provider
    }

    private fun s(id: Int, vararg args: Any): String {
        return requireNotNull(stringProvider) { "String provider is not bound." }(id, args)
    }

    val airlineProfiles: List<AirlineRuleProfile>
        get() = listOf(
            AirlineRuleProfile(
                airlineName = s(R.string.sample_airline_korean_air),
                batteryCabinLimitNote = s(R.string.message_battery_korean),
                liquidCabinLimitMl = 100,
            ),
            AirlineRuleProfile(
                airlineName = s(R.string.sample_airline_asiana),
                batteryCabinLimitNote = s(R.string.message_battery_asiana),
                liquidCabinLimitMl = 100,
            ),
            AirlineRuleProfile(
                airlineName = s(R.string.sample_airline_jeju),
                batteryCabinLimitNote = s(R.string.message_battery_jeju),
                liquidCabinLimitMl = 100,
            ),
        )

    val departureOptions: LinkedHashMap<String, Int>
        get() = linkedMapOf(
            s(R.string.sample_departure_45m) to 45,
            s(R.string.sample_departure_90m) to 90,
            s(R.string.sample_departure_150m) to 150,
            s(R.string.sample_departure_240m) to 240,
        )

    val scanPresets: List<ScanPreset>
        get() = listOf(
            ScanPreset(
                title = s(R.string.preset_standard),
                detectedItems = listOf(
                    RecognizedItem(s(R.string.item_power_bank), s(R.string.category_electronics), s(R.string.detail_power_bank_large)),
                    RecognizedItem(s(R.string.item_hair_iron), s(R.string.category_electronics), s(R.string.detail_hair_iron)),
                    RecognizedItem(s(R.string.item_liquid), s(R.string.category_liquid), s(R.string.detail_toner)),
                ),
            ),
            ScanPreset(
                title = s(R.string.preset_electronics),
                detectedItems = listOf(
                    RecognizedItem(s(R.string.item_laptop), s(R.string.category_electronics), s(R.string.detail_laptop)),
                    RecognizedItem(s(R.string.item_power_bank), s(R.string.category_electronics), s(R.string.detail_power_bank_small)),
                    RecognizedItem(s(R.string.item_power_strip), s(R.string.category_electronics), s(R.string.detail_power_strip)),
                ),
            ),
            ScanPreset(
                title = s(R.string.preset_mix),
                detectedItems = listOf(
                    RecognizedItem(s(R.string.item_perfume), s(R.string.category_liquid), s(R.string.detail_perfume)),
                    RecognizedItem(s(R.string.item_spray), s(R.string.category_aerosol), s(R.string.detail_spray)),
                    RecognizedItem(s(R.string.item_pocket_knife), s(R.string.category_sharp), s(R.string.detail_pocket_knife)),
                ),
            ),
        )

    val sampleFlight: FlightContext
        get() = FlightContext(
            airlineName = s(R.string.sample_airline_korean_air),
            flightNumber = "KE081",
            route = s(R.string.sample_route_nyc),
            departureMinutes = 150,
        )
}

object BaggageRuleEngine {
    fun previewDecisionForItem(
        item: RecognizedItem,
        airlineRuleProfile: AirlineRuleProfile,
        stringProvider: (Int, Array<out Any>) -> String,
    ): ItemDecision {
        return decideForItem(item, airlineRuleProfile, stringProvider)
    }

    fun analyze(
        flight: FlightContext,
        airlineRuleProfile: AirlineRuleProfile,
        recognizedItems: List<RecognizedItem>,
        stringProvider: (Int, Array<out Any>) -> String,
    ): AnalysisReport {
        val decisions = recognizedItems.map { item ->
            ItemDecisionViewModel(item, decideForItem(item, airlineRuleProfile, stringProvider))
        }
        val hasDanger = decisions.any { it.decision.severity == DecisionSeverity.DANGER }
        val hasCaution = decisions.any { it.decision.severity == DecisionSeverity.CAUTION }
        val urgentDeparture = flight.departureMinutes <= 90

        val overview = when {
            hasDanger && urgentDeparture -> OverviewGuide(
                headline = stringProvider(R.string.overview_urgent_danger_title, emptyArray()),
                body = stringProvider(R.string.overview_urgent_danger_body, emptyArray()),
            )

            hasDanger -> OverviewGuide(
                headline = stringProvider(R.string.overview_danger_title, emptyArray()),
                body = stringProvider(R.string.overview_danger_body, emptyArray()),
            )

            hasCaution && urgentDeparture -> OverviewGuide(
                headline = stringProvider(R.string.overview_urgent_caution_title, emptyArray()),
                body = stringProvider(R.string.overview_urgent_caution_body, emptyArray()),
            )

            hasCaution -> OverviewGuide(
                headline = stringProvider(R.string.overview_caution_title, emptyArray()),
                body = stringProvider(R.string.overview_caution_body, emptyArray()),
            )

            else -> OverviewGuide(
                headline = stringProvider(R.string.overview_safe_title, emptyArray()),
                body = stringProvider(R.string.overview_safe_body, emptyArray()),
            )
        }

        return AnalysisReport(
            recognizedItems = recognizedItems,
            itemDecisions = decisions,
            overview = overview,
        )
    }

    private fun decideForItem(
        item: RecognizedItem,
        airlineRuleProfile: AirlineRuleProfile,
        stringProvider: (Int, Array<out Any>) -> String,
    ): ItemDecision {
        return when (item.name) {
            stringProvider(R.string.item_power_bank, emptyArray()) -> ItemDecision(
                label = stringProvider(R.string.decision_power_bank, emptyArray()),
                message = airlineRuleProfile.batteryCabinLimitNote,
                severity = DecisionSeverity.DANGER,
            )

            stringProvider(R.string.item_hair_iron, emptyArray()) -> ItemDecision(
                label = stringProvider(R.string.decision_hair_iron, emptyArray()),
                message = stringProvider(R.string.message_hair_iron, emptyArray()),
                severity = DecisionSeverity.SAFE,
            )

            stringProvider(R.string.item_liquid, emptyArray()) -> ItemDecision(
                label = stringProvider(R.string.decision_liquid, emptyArray()),
                message = stringProvider(R.string.message_liquid, arrayOf(airlineRuleProfile.liquidCabinLimitMl)),
                severity = DecisionSeverity.CAUTION,
            )

            stringProvider(R.string.item_laptop, emptyArray()) -> ItemDecision(
                label = stringProvider(R.string.decision_laptop, emptyArray()),
                message = stringProvider(R.string.message_laptop, emptyArray()),
                severity = DecisionSeverity.SAFE,
            )

            stringProvider(R.string.item_power_strip, emptyArray()) -> ItemDecision(
                label = stringProvider(R.string.decision_power_strip, emptyArray()),
                message = stringProvider(R.string.message_power_strip, emptyArray()),
                severity = DecisionSeverity.SAFE,
            )

            stringProvider(R.string.item_perfume, emptyArray()) -> ItemDecision(
                label = stringProvider(R.string.decision_perfume, emptyArray()),
                message = stringProvider(R.string.message_perfume, arrayOf(airlineRuleProfile.liquidCabinLimitMl)),
                severity = DecisionSeverity.SAFE,
            )

            stringProvider(R.string.item_spray, emptyArray()) -> ItemDecision(
                label = stringProvider(R.string.decision_spray, emptyArray()),
                message = stringProvider(R.string.message_spray, emptyArray()),
                severity = DecisionSeverity.CAUTION,
            )

            stringProvider(R.string.item_pocket_knife, emptyArray()) -> ItemDecision(
                label = stringProvider(R.string.decision_pocket_knife, emptyArray()),
                message = stringProvider(R.string.message_pocket_knife, emptyArray()),
                severity = DecisionSeverity.DANGER,
            )

            else -> ItemDecision(
                label = stringProvider(R.string.decision_unknown, emptyArray()),
                message = stringProvider(R.string.message_unknown, emptyArray()),
                severity = DecisionSeverity.CAUTION,
            )
        }
    }
}
