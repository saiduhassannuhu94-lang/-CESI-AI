package com.cesi.assistant.core.accessibility
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
class CesiAccessibilityService:AccessibilityService(){
 override fun onServiceConnected(){serviceInfo=AccessibilityServiceInfo().apply{eventTypes=AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;feedbackType=AccessibilityServiceInfo.FEEDBACK_GENERIC;flags=AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;notificationTimeout=100}}
 override fun onAccessibilityEvent(e:AccessibilityEvent?){if(e?.packageName!="com.whatsapp")return;val p=getSharedPreferences("cesi_pending",MODE_PRIVATE);val msg=p.getString("whatsapp_message",null)?:return;val root=rootInActiveWindow?:return;val input=findEditable(root)?:return;input.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,Bundle().apply{putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,msg)});findSend(root)?.performAction(AccessibilityNodeInfo.ACTION_CLICK);p.edit().clear().apply()}
 private fun findEditable(n:AccessibilityNodeInfo):AccessibilityNodeInfo?{n.findAccessibilityNodeInfosByViewId("com.whatsapp:id/entry").firstOrNull()?.let{return it};for(i in 0 until n.childCount)n.getChild(i)?.let{findEditable(it)?.let{r->return r}};return null}
 private fun findSend(n:AccessibilityNodeInfo):AccessibilityNodeInfo?{for(id in listOf("com.whatsapp:id/send","com.whatsapp:id/send_container"))n.findAccessibilityNodeInfosByViewId(id).firstOrNull()?.let{return it};return n.findAccessibilityNodeInfosByText("Send").firstOrNull()}
 override fun onInterrupt()=Unit
}