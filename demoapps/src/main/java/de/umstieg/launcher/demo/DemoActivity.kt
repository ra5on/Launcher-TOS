package de.umstieg.launcher.demo

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.widget.TextView

/** Platzhalter-Bildschirm der Demo-App. */
class DemoActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(
            TextView(this).apply {
                text = getString(R.string.demo_text, getString(R.string.app_name))
                textSize = 20f
                gravity = Gravity.CENTER
                setPadding(64, 64, 64, 64)
            },
        )
    }
}
