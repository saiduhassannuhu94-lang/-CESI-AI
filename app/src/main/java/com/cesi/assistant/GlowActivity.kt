package com.cesi.assistant

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import android.view.animation.AlphaAnimation
import android.view.animation.Animation

class GlowActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        val root = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(8, 8, 28), Color.rgb(30, 8, 55), Color.rgb(5, 30, 45))
            )
        }

        val orb = TextView(this).apply {
            text = "✦\nCESI"
            textSize = 28f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.rgb(35, 20, 70))
                setStroke(5, Color.rgb(0, 229, 255))
            }
            elevation = 24f
        }

        val state = TextView(this).apply {
            text = "Ina sauraro…"
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(0, 229, 255))
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            addView(orb, LinearLayout.LayoutParams(210, 210))
            addView(state, LinearLayout.LayoutParams(-1, 70))
        }

        root.addView(content)
        setContentView(root)

        val pulse = AlphaAnimation(0.45f, 1f).apply {
            duration = 700
            repeatMode = Animation.REVERSE
            repeatCount = Animation.INFINITE
        }
        orb.startAnimation(pulse)

        orb.setOnClickListener { finish() }
        root.setOnClickListener { finish() }
    }
}