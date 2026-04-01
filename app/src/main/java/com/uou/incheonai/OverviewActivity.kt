package com.uou.incheonai

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import com.google.android.material.button.MaterialButton

class OverviewActivity : BasePrototypeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_overview)
        applyWindowInsets(R.id.rootOverview)

        val report = PrototypeSession.analysisReport
        findViewById<TextView>(R.id.overviewHeadlineText).text =
            report?.overview?.headline ?: getString(R.string.default_overview_headline)
        findViewById<TextView>(R.id.overviewBodyText).text =
            report?.overview?.body ?: getString(R.string.default_overview_body)

        findViewById<TextView>(R.id.actionOneText).text = getString(R.string.recommended_action_1)
        findViewById<TextView>(R.id.actionTwoText).text = getString(R.string.recommended_action_2)
        findViewById<TextView>(R.id.actionThreeText).text = getString(R.string.recommended_action_3)

        findViewById<MaterialButton>(R.id.previousButton).setOnClickListener { finish() }
        findViewById<MaterialButton>(R.id.restartButton).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
            finish()
        }
    }
}
