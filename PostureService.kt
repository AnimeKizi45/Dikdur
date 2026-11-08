package com.dikdur.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import java.util.concurrent.Executors

/**
 * Arka planda çalışan servis: ön kamerayı açar, duruşu izler,
 * gerektiğinde tüm telefon ekranını karartır.
 */
class PostureService : LifecycleService() {

    companion object {
        const val DURDUR = "com.dikdur.app.DURDUR"
        private const val KANAL = "dikdur_kanal"
        private const val BILDIRIM_ID = 1
        @Volatile var calisiyor = false; private set
    }

    private val anaIs = Handler(Looper.getMainLooper())
    private val kameraIs = Executors.newSingleThreadExecutor()
    private lateinit var overlay: DimOverlay
    private var analizci: PostureAnalyzer? = null
    private val mantik = PostureLogic()

    override fun onCreate() {
        super.onCreate()
        calisiyor = true
        kanalOlustur()
        val tip = if (Build.VERSION.SDK_INT >= 30) ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA else 0
        ServiceCompat.startForeground(this, BILDIRIM_ID, bildirim("Duruşun izleniyor"), tip)

        overlay = DimOverlay(this).also { it.ekle() }
        mantik.kalibrasyonuBaslat(SystemClock.uptimeMillis())
        kamerayiBaslat()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == DURDUR) stopSelf()
        return START_NOT_STICKY
    }

    private fun kamerayiBaslat() {
        val saglayiciGelecek = ProcessCameraProvider.getInstance(this)
        saglayiciGelecek.addListener({
            val saglayici = saglayiciGelecek.get()
            @Suppress("DEPRECATION")
            val analiz = ImageAnalysis.Builder()
                .setTargetResolution(android.util.Size(480, 640))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()

            kameraIs.execute {
                val a = PostureAnalyzer(this) { pitch, oran ->
                    val durum = mantik.guncelle(pitch, oran, SystemClock.uptimeMillis())
                    anaIs.post { uygula(durum) }
                }
                analizci = a
                analiz.setAnalyzer(kameraIs, a)
            }
            saglayici.unbindAll()
            saglayici.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, analiz)
        }, ContextCompat.getMainExecutor(this))
    }

    private fun uygula(d: PostureLogic.Durum) {
        overlay.karart(d.karartma)
        when {
            d.asama != PostureLogic.Asama.TAKIP -> overlay.mesaj(d.mesaj, kirmizi = false)
            d.kambur -> overlay.mesaj("⚠ Boynunu dik tut!  (${d.aci.toInt()}°)")
            else -> overlay.mesaj(null)
        }
        if (d.yeniUyari) titret()
    }

    private fun titret() {
        val v = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        v.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    private fun kanalOlustur() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(KANAL, "DikDur duruş takibi", NotificationManager.IMPORTANCE_LOW))
    }

    private fun bildirim(metin: String): Notification {
        val ac = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE)
        val durdur = PendingIntent.getService(this, 1,
            Intent(this, PostureService::class.java).setAction(DURDUR), PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, KANAL)
            .setSmallIcon(R.drawable.ic_logo)
            .setContentTitle("DikDur çalışıyor")
            .setContentText(metin)
            .setContentIntent(ac)
            .addAction(0, "Durdur", durdur)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        calisiyor = false
        overlay.kaldir()
        runCatching { ProcessCameraProvider.getInstance(this).get().unbindAll() }
        kameraIs.execute { analizci?.kapat() }
        kameraIs.shutdown()
        super.onDestroy()
    }
}
