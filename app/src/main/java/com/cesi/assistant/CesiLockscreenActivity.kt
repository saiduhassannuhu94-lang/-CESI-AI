package com.cesi.assistant
import android.os.Bundle
import android.speech.*
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.widget.*
import androidx.activity.ComponentActivity
import com.cesi.assistant.core.action.ActionRouter
import com.cesi.assistant.core.intent.IntentEngine
import java.util.Locale
class CesiLockscreenActivity:ComponentActivity(){
 private lateinit var r:SpeechRecognizer;private lateinit var t:TextView;private lateinit var s:TextView;private lateinit var a:ActionRouter;private lateinit var e:IntentEngine;private lateinit var speech:TextToSpeech
 override fun onCreate(b:Bundle?){super.onCreate(b);setShowWhenLocked(true);setTurnScreenOn(true);e=IntentEngine();a=ActionRouter(this);val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER;setPadding(28,28,28,40);setBackgroundColor(0xEE020712.toInt())};val orb=TextView(this).apply{text="◉";textSize=76f;gravity=Gravity.CENTER;setTextColor(0xFF66E6FF.toInt())};s=TextView(this).apply{text="CESI • Tap to speak";textSize=18f;gravity=Gravity.CENTER;setTextColor(-1)};t=TextView(this).apply{text="Lock-screen ready";textSize=15f;gravity=Gravity.CENTER;setTextColor(0xFFB8C7D9.toInt())};root.addView(orb,LinearLayout.LayoutParams(-1,190));root.addView(s,LinearLayout.LayoutParams(-1,60));root.addView(t,LinearLayout.LayoutParams(-1,100));root.setOnClickListener{listen()};setContentView(root);speech=TextToSpeech(this){speech.language=Locale.US};r=SpeechRecognizer.createSpeechRecognizer(this)}
 private fun listen(){s.text="CESI • Listening…";r.setRecognitionListener(object:RecognitionListener{override fun onReadyForSpeech(x:Bundle?){ };override fun onBeginningOfSpeech(){s.text="CESI • Hearing you…"};override fun onRmsChanged(x:Float){};override fun onBufferReceived(x:ByteArray?){};override fun onEndOfSpeech(){s.text="CESI • Processing…"};override fun onError(x:Int){s.text="CESI • Ready"};override fun onResults(x:Bundle?){val q=x?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty();t.text=q;val z=a.route(e.understand(q));s.text=z;speech.speak(z,TextToSpeech.QUEUE_FLUSH,null,"cesi_lock")};override fun onPartialResults(x:Bundle?){x?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let{t.text=it}};override fun onEvent(x:Int,y:Bundle?){}});r.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply{putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);putExtra(RecognizerIntent.EXTRA_LANGUAGE,"en-NG");putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true)})}
 override fun onDestroy(){r.destroy();speech.shutdown();super.onDestroy()}
}