package com.dikdur.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

/** Ana ekran: izinleri ister, arka plan servisini başlatır/durdurur. */
class MainActivity : ComponentActivity() {

    private lateinit var dugme: Button
    private lateinit var bilgi: TextView

    private val izinIste = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()) { baslatmayiDene() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val yogunluk = resources.displayMetrics.density
        val kok = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding((24 * yogunluk).toInt(), (64 * yogunluk).toInt(), (24 * yogunluk).toInt(), 0)
            setBackgroundColor(Color.parseColor("#F4F7FB"))
        }
        kok.addView(ImageView(this).apply {
            setImageResource(R.drawable.ic_logo)
            layoutParams = LinearLayout.LayoutParams((120 * yogunluk).toInt(), (120 * yogunluk).toInt())
        })
        kok.addView(TextView(this).apply {
            text = getString(R.string.app_name); textSize = 34f
            setTypeface(typeface, Typeface.BOLD); setTextColor(Color.parseColor("#16202C"))
        })
        kok.addView(TextView(this).apply {
            text = getString(R.string.slogan); textSize = 17f
            setTextColor(Color.parseColor("#2F7BD9")); setTypeface(typeface, Typeface.BOLD)
        })
        bilgi = TextView(this).apply {
            textSize = 15f; setTextColor(Color.parseColor("#5B6878")); gravity = Gravity.CENTER
            setPadding(0, (24 * yogunluk).toInt(), 0, (24 * yogunluk).toInt())
        }
        kok.addView(bilgi)
        dugme = Button(this).apply { textSize = 17f; setOnClickListener { tikla() } }
        kok.addView(dugme)
        setContentView(kok)
    }

    override fun onResume() { super.onResume(); arayuzuGuncelle() }

    private fun arayuzuGuncelle() {
        if (PostureService.calisiyor) {
            dugme.text = "Takibi durdur"
            bilgi.text = "DikDur arka planda çalışıyor. Diğer uygulamaları kullanabilirsin; " +
                    "boynun fazla eğilirse ekran kararır."
        } else {
            dugme.text = "Takibi başlat"
            bilgi.text = "Başlatınca önce 3 sn dik otur, sonra başını hafifçe öne eğ (kalibrasyon). " +
                    "Görüntü kaydedilmez, telefondan çıkmaz."
        }
    }

    private fun tikla() {
        if (PostureService.calisiyor) {
            startService(Intent(this, PostureService::class.java).setAction(PostureService.DURDUR))
            dugme.postDelayed({ arayuzuGuncelle() }, 300)
        } else baslatmayiDene()
    }

    private fun baslatmayiDene() {
        // 1) Kamera ve bildirim izni
        val gerekli = buildList {
            add(Manifest.permission.CAMERA)
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        }.filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (Manifest.permission.CAMERA in gerekli) { izinIste.launch(gerekli.toTypedArray()); return }

        // 2) Diğer uygulamaların üzerinde gösterme izni (ekranı karartmak için)
        if (!Settings.canDrawOverlays(this)) {
            bilgi.text = "Ekranı karartabilmek için açılan listede DikDur'a " +
                    "\"Diğer uygulamaların üzerinde göster\" izni ver, sonra geri dön."
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")))
            return
        }

        // 3) Arka plan servisini başlat
        ContextCompat.startForegroundService(this, Intent(this, PostureService::class.java))
        dugme.postDelayed({ arayuzuGuncelle() }, 300)
    }
}
