package com.uou.incheonai

import android.os.Bundle
import android.widget.LinearLayout
import com.google.android.material.button.MaterialButton

class RuleComparisonActivity : BasePrototypeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rule_comparison)
        applyWindowInsets(R.id.rootRules)

        val container = findViewById<LinearLayout>(R.id.analysisResultsContainer)
        PrototypeUi.renderAnalysisResults(
            this,
            layoutInflater,
            container,
            PrototypeSession.analysisReport?.itemDecisions ?: emptyList(),
        )

        findViewById<MaterialButton>(R.id.previousButton).setOnClickListener { finish() }
        findViewById<MaterialButton>(R.id.nextButton).setOnClickListener {
            openNext(IntegrationStatusActivity::class.java)
        }
    }
}
