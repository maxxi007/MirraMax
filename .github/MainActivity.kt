package com.maxxi007.mirramax

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

private const val REQ_CODE_CAPTURE = 2345

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val btnStart: Button = findViewById(R.id.btnStart)
        val btnStop: Button = findViewById(R.id.btnStop)

        btnStart.setOnClickListener { startCaptureIntent() }
        btnStop.setOnClickListener { stopService(Intent(this, ScreenEncoderService::class.java)) }
    }

    private fun startCaptureIntent() {
        val mpm = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(mpm.createScreenCaptureIntent(), REQ_CODE_CAPTURE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQ_CODE_CAPTURE && resultCode == Activity.RESULT_OK && data != null) {
            val svc = Intent(this, ScreenEncoderService::class.java)
            svc.putExtra("resultCode", resultCode)
            svc.putExtra("resultIntent", data)
            startForegroundService(svc)
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }
}
