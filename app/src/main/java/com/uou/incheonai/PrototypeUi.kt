package com.uou.incheonai

import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView

object PrototypeUi {
    fun renderRecognizedItems(
        inflater: LayoutInflater,
        container: LinearLayout,
        items: List<RecognizedItem>,
        emptyText: String,
    ) {
        container.removeAllViews()
        if (items.isEmpty()) {
            val chipView = inflater.inflate(R.layout.item_recognized_chip, container, false) as TextView
            chipView.text = emptyText
            container.addView(chipView)
            return
        }

        items.forEach { item ->
            val chipView = inflater.inflate(R.layout.item_recognized_chip, container, false) as TextView
            chipView.text = "${item.name} · ${item.detail}"
            container.addView(chipView)
        }
    }

    fun renderAnalysisResults(
        activity: BasePrototypeActivity,
        inflater: LayoutInflater,
        container: LinearLayout,
        results: List<ItemDecisionViewModel>,
    ) {
        container.removeAllViews()
        if (results.isEmpty()) {
            val view = inflater.inflate(R.layout.item_analysis_result, container, false)
            val card = view as MaterialCardView
            view.findViewById<TextView>(R.id.itemNameText).text = activity.getString(R.string.empty_result)
            view.findViewById<TextView>(R.id.itemCategoryText).text = ""
            view.findViewById<TextView>(R.id.itemDecisionText).text = activity.getString(R.string.empty_result_detail)
            view.findViewById<TextView>(R.id.itemMessageText).text = activity.getString(R.string.empty_result_message)
            container.addView(card)
            return
        }

        results.forEach { result ->
            val view = inflater.inflate(R.layout.item_analysis_result, container, false)
            val card = view as MaterialCardView
            val nameText = view.findViewById<TextView>(R.id.itemNameText)
            val categoryText = view.findViewById<TextView>(R.id.itemCategoryText)
            val decisionText = view.findViewById<TextView>(R.id.itemDecisionText)
            val messageText = view.findViewById<TextView>(R.id.itemMessageText)

            nameText.text = result.item.name
            categoryText.text = "${result.item.category} · ${result.item.detail}"
            decisionText.text = result.decision.label
            messageText.text = result.decision.message

            val backgroundColor = when (result.decision.severity) {
                DecisionSeverity.SAFE -> R.color.result_safe_background
                DecisionSeverity.CAUTION -> R.color.result_caution_background
                DecisionSeverity.DANGER -> R.color.result_danger_background
            }
            val textColor = when (result.decision.severity) {
                DecisionSeverity.SAFE -> R.color.safe_text
                DecisionSeverity.CAUTION -> R.color.caution_text
                DecisionSeverity.DANGER -> R.color.danger_text
            }

            card.setCardBackgroundColor(ContextCompat.getColor(activity, backgroundColor))
            decisionText.setTextColor(ContextCompat.getColor(activity, textColor))
            container.addView(card)
        }
    }
}
