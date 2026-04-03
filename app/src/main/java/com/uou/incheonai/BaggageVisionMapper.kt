package com.uou.incheonai

import android.content.Context

object BaggageVisionMapper {
    fun mapLabel(context: Context, rawLabel: String): RecognizedItem? {
        return when (rawLabel.lowercase()) {
            "bottle" -> RecognizedItem(
                context.getString(R.string.item_liquid),
                context.getString(R.string.category_liquid),
                context.getString(R.string.detail_detected_liquid),
            )

            "laptop" -> RecognizedItem(
                context.getString(R.string.item_laptop),
                context.getString(R.string.category_electronics),
                context.getString(R.string.detail_detected_laptop),
            )

            "hair drier" -> RecognizedItem(
                context.getString(R.string.item_hair_iron),
                context.getString(R.string.category_electronics),
                context.getString(R.string.detail_detected_hair_device),
            )

            "knife", "scissors" -> RecognizedItem(
                context.getString(R.string.item_pocket_knife),
                context.getString(R.string.category_sharp),
                context.getString(R.string.detail_detected_sharp),
            )

            "cell phone" -> RecognizedItem(
                context.getString(R.string.item_power_bank),
                context.getString(R.string.category_electronics),
                context.getString(R.string.detail_detected_battery_like),
            )

            else -> null
        }
    }
}
