package com.cesi.assistant

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.*
import android.view.Gravity
import android.view.WindowManager
import android.view.animation.AlphaAnimation
import android.widget.LinearLayout
import android.widget.TextView

class GlowActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        val root = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            setPadding(28, 28, 28, 28)
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(5, 5, 22), Color.rgb(28, 7, 55), Color.rgb(4, 35, 48))
            )
        }
        val orb = TextView(this).apply {
            text = "✦\nCESI"
            textSize = 28f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.rgb(30, 18, 65))
                setStroke(6, Color.rgb(0, 229, 255))
            }
            elevation = 30f
        }
        val state = TextView(this).apply {
            text = "Ina sauraronka…"
            textSize = 17f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(0, 229, 255))
        }
        val hint = TextView(this).apply {
            text = "Faɗi abin da kake so"
            textSize = 13f
            gravity = Gravity.CENTER
            setTextColor(Color.LTGRAY)
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            addView(orb, LinearLayout.LayoutParams(220, 220))
            addView(state, LinearLayout.LayoutParams(-1, 62))
            addView(hint, LinearLayout.LayoutParams(-1, 50))
        }
        root.addView(content)
        setContentView(root)

        val pulse = AlphaAnimation(0.55f, 1f).apply {
            duration = 650
            repeatMode = android.view.animation.Animation.REVERSE
            repeatCount = android.view.animation.Animation.INFINITE
        }
        orb.startAnimation(pulse)
        root.setOnClickListener { finish() }
        orb.setOnClickListener { finish() }
    }
}