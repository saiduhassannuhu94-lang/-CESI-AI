package com.cesi.assistant

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerForActivityResult(ActivityResultContracts.RequestPermission()){}.launch(Manifest.permission.CAMERA)
        setContent {
            MaterialTheme {
                var p by remember { mutableStateOf("") }
                var r by remember { mutableStateOf("Sannu! Ni Cesi ce. Me zan maka?") }
                Column(Modifier.fillMaxSize().padding(16.dp)) {
                    CameraPreview(Modifier.fillMaxWidth().height(350.dp).weight(1f))
                    Spacer(Modifier.height(12.dp))
                    Card(Modifier.fillMaxWidth()) { Text(r, Modifier.padding(16.dp)) }
                    OutlinedTextField(value=p, onValueChange={p=it}, label={Text("Tambaya...")}, modifier=Modifier.fillMaxWidth())
                    Button(onClick={r=getReply(p); p=""}, Modifier.fillMaxWidth().padding(top=8.dp)) { Text("Aika") }
                }
            }
        }
    }
    fun getReply(s:String):String{
        val q=s.lowercase()
        return when{
            q.contains("sannu")->"Sannu! Yaya kake? Ina shirye."
            q.contains("sunanka")->"Ni Cesi ce, mataimakiyar ka."
            q.contains("yaya")->"Lafiya lau! Kai fa?"
            q.contains("godiya")->"Babu komai!"
            q.contains("lokaci")->"Karfe ${java.text.SimpleDateFormat("HH:mm").format(java.util.Date())} ne."
            q.isEmpty()->"Rubuta tambaya..."
            else->"Na ji: \"$s\". Zan taimaka!"
        }
    }
}
@Composable
fun CameraPreview(modifier: Modifier=Modifier){
    val ctx=LocalContext.current
    val owner=LocalLifecycleOwner.current
    AndroidView(factory={PreviewView(it)}, modifier=modifier, update={view->
        val future=ProcessCameraProvider.getInstance(ctx)
        future.addListener({
            val provider=future.get()
            val preview=Preview.Builder().build().also{it.setSurfaceProvider(view.surfaceProvider)}
            try{provider.unbindAll(); provider.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, preview)}catch(e:Exception){}
        }, ContextCompat.getMainExecutor(ctx))
    })
}
