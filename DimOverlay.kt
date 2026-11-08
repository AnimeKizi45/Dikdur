package com.dikdur.app

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView

/**
 * Tüm telefon ekranını karartan katman + üstte uyarı mesajı.
 * Katman dokunmaları geçirir, kullanıcı telefonu kullanmaya devam edebilir.
 * Not: Android 12+ dokunmayı geçirmek için pencere opaklığının 0.8'in altında
 * kalmasını ister; en fazla 0.7 kullandığımız için sorun yoktur.
 */
class DimOverlay(private val context: Context) {

    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val gecirgenBayraklar = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN

    private val perde = View(context).apply { setBackgroundColor(Color.BLACK) }
    private val perdeParam = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, gecirgenBayraklar, PixelFormat.TRANSLUCENT
    ).apply { alpha = 0f }

    private val uyari = TextView(context).apply {
        textSize = 18f
        setTypeface(typeface, Typeface.BOLD)
        setTextColor(Color.WHITE)
        gravity = Gravity.CENTER
        setPadding(40, 32, 40, 32)
        background = GradientDrawable().apply { cornerRadius = 36f; setColor(Color.parseColor("#D93B3B")) }
        visibility = View.GONE
    }
    private val uyariParam = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, gecirgenBayraklar, PixelFormat.TRANSLUCENT
    ).apply { gravity = Gravity.TOP; y = 120; horizontalMargin = 0.04f }

    private var animator: ValueAnimator? = null
    private var hedef = 0f

    fun ekle() {
        wm.addView(perde, perdeParam)
        wm.addView(uyari, uyariParam)
    }

    /** oran: 0.0 – 0.7 ; yumuşak geçişle (0,5 sn) uygulanır */
    fun karart(oran: Float) {
        if (kotuDegil(oran)) return
        hedef = oran
        animator?.cancel()
        animator = ValueAnimator.ofFloat(perdeParam.alpha, oran).apply {
            duration = 500
            addUpdateListener {
                perdeParam.alpha = it.animatedValue as Float
                wm.updateViewLayout(perde, perdeParam)
            }
            start()
        }
    }

    private fun kotuDegil(oran: Float) = kotlin.math.abs(oran - hedef) < 0.01f

    fun mesaj(yazi: String?, kirmizi: Boolean = true) {
        if (yazi == null) { uyari.visibility = View.GONE; return }
        uyari.text = yazi
        (uyari.background as GradientDrawable).setColor(
            Color.parseColor(if (kirmizi) "#D93B3B" else "#2F7BD9"))
        uyari.visibility = View.VISIBLE
    }

    fun kaldir() {
        animator?.cancel()
        runCatching { wm.removeView(perde) }
        runCatching { wm.removeView(uyari) }
    }
}
