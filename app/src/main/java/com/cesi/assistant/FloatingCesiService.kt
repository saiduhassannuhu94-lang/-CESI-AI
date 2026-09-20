package com.cesi.assistant

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.*
import android.widget.*
import android.graphics.Color
import android.view.Gravity

class FloatingCesiService : Service() {
    private var windowManager: WindowManager? = null
    private var floatingView: LinearLayout? = null
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or 
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.CENTER
        
        floatingView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#CC0F0F1E"))
            setPadding(40,40,40,40)
            gravity = Gravity.CENTER
            
            val title = TextView(context).apply {
                text = "Cesi 👩🏽\nSannu! Me zan maka?"
                textSize = 22f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
            }
            
            val input = EditText(context).apply {
                hint = "Rubuta anan..."
                setHintTextColor(Color.GRAY)
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.parseColor("#33FFFFFF"))
            }
            
            val reply = TextView(context).apply {
                text = ""
                textSize = 18f
                setTextColor(Color.CYAN)
                setPadding(0,20,0,20)
            }
            
            val btnSend = Button(context).apply {
                text = "Aika"
                setOnClickListener {
                    val q = input.text.toString()
                    reply.text = getReply(q)
                    input.text.clear()
                }
            }
            
            val btnClose = Button(context).apply {
                text = "Rufe"
                setOnClickListener { stopSelf() }
            }
            
            addView(title)
            addView(input)
            addView(btnSend)
            addView(reply)
            addView(btnClose)
        }
        
        windowManager?.addView(floatingView, params)
    }
    
    fun getReply(s:String):String{
        val q=s.lowercase()
        return when{
            q.contains("sannu")->"Sannu dan uwa! Lafiya?"
            q.contains("sunanka")->"Ni Cesi ce, yar aikin ka."
            q.contains("yaya")->"Lafiya lau, nagode!"
            q.contains("so")||q.contains("kauna")->"Ni ma ina son ka 😊"
            else->"Na ji: $s. Ina tare da kai a background!"
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        floatingView?.let { windowManager?.removeView(it) }
    }
}
