package com.maxxi007.mirramax

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaCodecList
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

class ScreenEncoderService : Service() {
    private val TAG = "ScreenEncoderService"
    private var mediaProjection: MediaProjection? = null
    private var encoder: MediaCodec? = null
    private var running = AtomicBoolean(false)
    private var outStream: OutputStream? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra("resultCode", -1) ?: -1
        val resultIntent = intent?.getParcelableExtra<Intent>("resultIntent")
        if (resultCode != -1 && resultIntent != null) {
            val mpm = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = mpm.getMediaProjection(resultCode, resultIntent)
            startForegroundServiceWork()
        }
        return START_STICKY
    }

    private fun startForegroundServiceWork() {
        createNotification()
        Thread {
            try {
                running.set(true)
                setupEncoderAndStream()
            } catch (e: Exception) {
                Log.e(TAG, "Error: ${e.localizedMessage}", e)
            }
        }.start()
    }

    private fun createNotification() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "mirramax_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(channelId, "MirraMax", NotificationManager.IMPORTANCE_LOW)
            nm.createNotificationChannel(ch)
        }
        val n = Notification.Builder(this, channelId)
            .setContentTitle("MirraMax")
            .setContentText("Streaming...")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .build()
        startForeground(101, n as Notification)
    }

    private fun chooseMime(): String {
        val nl = MediaCodecList(MediaCodecList.ALL_CODECS).codecInfos
        for (info in nl) {
            for (type in info.supportedTypes) {
                if (type.equals("video/hevc", ignoreCase = true)) return "video/hevc"
            }
        }
        return "video/avc"
    }

    private fun setupEncoderAndStream() {
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        wm.defaultDisplay.getRealMetrics(metrics)
        val width = 3840
        val height = 2160
        val dpi = metrics.densityDpi

        val mime = chooseMime()
        val format = MediaFormat.createVideoFormat(mime, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, 50_000_000) // 50 Mbps default
            setInteger(MediaFormat.KEY_FRAME_RATE, 60)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }

        encoder = MediaCodec.createEncoderByType(mime)
        encoder?.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        val surface = encoder?.createInputSurface()
        encoder?.start()

        mediaProjection?.createVirtualDisplay("mirramax", width, height, dpi, 0, surface, null, null)

        // connect to PC via adb forward to 127.0.0.1:5004
        val sock = Socket()
        sock.connect(InetSocketAddress("127.0.0.1", 5004), 5000)
        outStream = sock.getOutputStream()

        val bufInfo = MediaCodec.BufferInfo()
        val enc = encoder ?: return
        while (running.get()) {
            val idx = enc.dequeueOutputBuffer(bufInfo, 10000)
            if (idx >= 0) {
                val outputBuffer: ByteBuffer? = enc.getOutputBuffer(idx)
                outputBuffer?.let {
                    it.position(bufInfo.offset); it.limit(bufInfo.offset + bufInfo.size)
                    val raw = ByteArray(bufInfo.size)
                    it.get(raw)
                    val framed = ensureAnnexB(raw)
                    try {
                        outStream?.write(framed)
                        outStream?.flush()
                    } catch (e: Exception) {
                        Log.e(TAG, "socket write error", e); running.set(false)
                    }
                }
                enc.releaseOutputBuffer(idx, false)
            } else if (idx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                Log.i(TAG, "Format changed")
            }
        }
    }

    private fun ensureAnnexB(input: ByteArray): ByteArray {
        if (input.size >= 4 && input[0]==0.toByte() && input[1]==0.toByte() && input[2]==0.toByte() && input[3]==1.toByte()) return input
        val out = ByteArrayOutputStream()
        var offset = 0
        while (offset + 4 <= input.size) {
            val len = (input[offset].toInt() and 0xFF shl 24) or
                      (input[offset+1].toInt() and 0xFF shl 16) or
                      (input[offset+2].toInt() and 0xFF shl 8) or
                      (input[offset+3].toInt() and 0xFF)
            offset += 4
            if (len <= 0 || offset + len > input.size) {
                out.reset()
                out.write(byteArrayOf(0,0,0,1))
                out.write(input)
                return out.toByteArray()
            }
            out.write(byteArrayOf(0,0,0,1))
            out.write(input, offset, len)
            offset += len
        }
        return out.toByteArray()
    }

    override fun onDestroy() {
        running.set(false)
        try { encoder?.stop(); encoder?.release() } catch(_:Exception){}
        try { mediaProjection?.stop() } catch(_:Exception){}
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
