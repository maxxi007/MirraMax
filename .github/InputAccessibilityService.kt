package com.maxxi007.mirramax

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.util.Log

class InputAccessibilityService : AccessibilityService() {
    companion object { @Volatile var INSTANCE: InputAccessibilityService? = null }
    private val TAG = "InputAccessibilitySvc"

    override fun onServiceConnected() { super.onServiceConnected(); INSTANCE = this; Log.i(TAG, "Connected") }
    override fun onInterrupt() {}
    override fun onDestroy() { INSTANCE = null; super.onDestroy() }

    fun injectTap(x: Float, y: Float, durationMs: Long = 10L) {
        val p = Path(); p.moveTo(x,y)
        val desc = GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(p,0,durationMs)).build()
        dispatchGesture(desc, object: GestureResultCallback(){
            override fun onCompleted(gestureDescription: GestureDescription) {}
            override fun onCancelled(gestureDescription: GestureDescription) { Log.w(TAG,"Cancelled") }
        }, null)
    }

    fun injectSwipe(x1: Float, y1: Float, x2: Float, y2: Float, durationMs: Long = 200L) {
        val p = Path(); p.moveTo(x1,y1); p.lineTo(x2,y2)
        val desc = GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(p,0,durationMs)).build()
        dispatchGesture(desc, object: GestureResultCallback(){}, null)
    }
}
