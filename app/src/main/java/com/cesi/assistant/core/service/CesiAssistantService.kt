package com.cesi.assistant.core.service

import android.app.*
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.*
import android.provider.Settings
import android.speech.*
import android.speech.tts.TextToSpeech
import android.view.*
import android.widget.*
import com.cesi.assistant.CesiLockscreenActivity
import com.cesi.assistant.R
import com.cesi.assistant.core.action.ActionRouter
import com.cesi.assistant.core.intent.IntentEngine
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sin

class CesiAssistantService:Service(){
    companion object{const val CHANNEL_ID="cesi_background";const val NOTIFICATION_ID=4001}
    private lateinit var engine:IntentEngine;private lateinit var router:ActionRouter;private lateinit var tts:TextToSpeech
    private var recognizer:SpeechRecognizer?=null;private var overlay:View?=null;private var transcript:TextView?=null;private var status:TextView?=null
    override fun onCreate(){super.onCreate();engine=IntentEngine();router=ActionRouter(this);tts=TextToSpeech(this){tts.language=Locale.US};channel();startForeground(NOTIFICATION_ID,notification());showOverlay()}
    private fun channel(){if(Build.VERSION.SDK_INT>=26)getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(CHANNEL_ID,"CESI Assistant",NotificationManager.IMPORTANCE_LOW))}
    private fun notification():Notification{
        val pi=PendingIntent.getActivity(this,401,Intent(this,CesiLockscreenActivity::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return Notification.Builder(this,CHANNEL_ID).setSmallIcon(R.mipmap.ic_launcher).setContentTitle("CESI is ready").setContentText("Tap to speak from lock screen").setOngoing(true).addAction(Notification.Action.Builder(Icon.createWithResource(this,R.mipmap.ic_launcher),"Speak to CESI",pi).build()).build()
    }
    private class Orb(c:android.content.Context):View(c){
        val p=Paint(Paint.ANTI_ALIAS_FLAG);var active=false;var level=.2f
        override fun onDraw(c:Canvas){val x=width/2f;val y=height/2f;val q=if(active)(sin(System.nanoTime()/180000000.0).toFloat()+1)/2 else .1f;p.color=Color.rgb(70,220,255);p.style=Paint.Style.STROKE;p.strokeWidth=4f;c.drawCircle(x,y,34+q*8,p);p.style=Paint.Style.FILL;c.drawCircle(x,y,22+q*5,p);for(i in 0..6){val xx=x-42+i*14;val h=if(active)8+level*30*(.35+.65*abs(sin(System.nanoTime()/180000000.0+i).toFloat())) else 5;c.drawRoundRect(xx,y+52-h,xx+7,y+52,4f,4f,p)}postInvalidateDelayed(40)}
    }
    private fun showOverlay(){
        if(Build.VERSION.SDK_INT>=23&&!Settings.canDrawOverlays(this))return
        if(overlay!=null)return
        val wm=getSystemService(WINDOW_SERVICE)as WindowManager
        val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER;padding(12,8,12,8);background=GradientDrawable().apply{setColor(0xDD07111F.toInt());cornerRadius=32f;setStroke(1,0x5539D8FF)}}
        val orb=Orb(this).apply{setOnClickListener{listen()}}
        status=TextView(this).apply{text="CESI • Ready";textSize=11f;gravity=Gravity.CENTER;setTextColor(Color.WHITE)}
        transcript=TextView(this).apply{text="Tap to speak";textSize=10f;gravity=Gravity.CENTER;setTextColor(0xFFB8C7D9.toInt());maxLines=2}
        box.addView(orb,LinearLayout.LayoutParams(96,100));box.addView(status,LinearLayout.LayoutParams(170,28));box.addView(transcript,LinearLayout.LayoutParams(210,42))
        val lp=WindowManager.LayoutParams(240,180,if(Build.VERSION.SDK_INT>=26)WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,PixelFormat.TRANSLUCENT);lp.gravity=Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL;lp.y=24
        wm.addView(box,lp);overlay=box
    }
    private fun listen(){
        if(!SpeechRecognizer.isRecognitionAvailable(this))return
        recognizer?.destroy();val orb=(overlay as?ViewGroup)?.getChildAt(0)as?Orb;orb?.active=true;status?.text="CESI • Listening…";transcript?.text="Ina sauraron ka…"
        recognizer=SpeechRecognizer.createSpeechRecognizer(this).apply{setRecognitionListener(object:RecognitionListener{
            override fun onReadyForSpeech(p:Bundle?){}
            override fun onBeginningOfSpeech(){status?.text="CESI • Hearing you…"}
            override fun onRmsChanged(db:Float){orb?.level=((db+2)/12).coerceIn(0f,1f)}
            override fun onBufferReceived(b:ByteArray?){}
            override fun onEndOfSpeech(){status?.text="CESI • Processing…"}
            override fun onError(e:Int){orb?.active=false;status?.text="CESI • Ready"}
            override fun onResults(b:Bundle?){val cmd=b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty();orb?.active=false;transcript?.text=cmd.ifBlank{"Ban ji umarnin ba."};val response=router.route(engine.understand(cmd));status?.text=response;tts.speak(response,TextToSpeech.QUEUE_FLUSH,null,"cesi_response");Handler(Looper.getMainLooper()).postDelayed({status?.text="CESI • Ready"},2200)}
            override fun onPartialResults(b:Bundle?){b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let{if(it.isNotBlank())transcript?.text=it}}
            override fun onEvent(t:Int,p:Bundle?){}
        });startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply{putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);putExtra(RecognizerIntent.EXTRA_LANGUAGE,"en-NG");putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true)})}
    override fun onStartCommand(i:Intent?,f:Int,s:Int):Int{showOverlay();return START_STICKY}
    override fun onDestroy(){recognizer?.destroy();if(::tts.isInitialized)tts.shutdown();overlay?.let{try{(getSystemService(WINDOW_SERVICE)as WindowManager).removeView(it)}catch(_:Exception){}};overlay=null;super.onDestroy()}
    override fun onBind(i:Intent?):IBinder?=null
}
