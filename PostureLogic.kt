package com.dikdur.app

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign

/**
 * DikDur'un karar mantığı (Android'e bağımlı değildir, test edilebilir).
 * Web sürümündeki mantığın aynısıdır:
 *   θ = 0.7 · (pitch − pitch0) + 0.3 · 90 · (1 − oran / oran0)
 *   Karartma(%) = 10 + (θ − 15) / (45 − 15) · 60   (10…70 arası)
 */
class PostureLogic(
    var esik1: Float = 15f,          // uyarı eşiği (derece)
    var esik2: Float = 45f,          // en fazla karartma açısı
    var bekleme: Long = 2000,        // eşik aşıldıktan sonra uyarıya kadar bekleme (ms)
    var duzelmeBekleme: Long = 1000, // dik durduktan sonra ekranın açılması için bekleme (ms)
    private val ema: Float = 0.2f
) {
    val duzelmeEsigi get() = max(3f, esik1 - 5f)

    enum class Asama { DIK_OTUR, EGIL, TAKIP }

    data class Durum(
        val asama: Asama,
        val aci: Float,
        val kambur: Boolean,
        val karartma: Float,        // 0.0 – 0.7
        val mesaj: String?,
        val yeniUyari: Boolean
    )

    var asama = Asama.DIK_OTUR; private set
    private var asamaBitis = 0L
    private val pitchler = mutableListOf<Float>()
    private val oranlar = mutableListOf<Float>()
    private val egikler = mutableListOf<Float>()

    private var pitch0 = 0f
    private var oran0: Float? = null
    private var yon = 1f

    private var theta = 0f
    private var kambur = false
    private var esikZamani: Long? = null
    private var duzZamani: Long? = null

    fun kalibrasyonuBaslat(simdi: Long) {
        asama = Asama.DIK_OTUR; asamaBitis = simdi + 3000
        pitchler.clear(); oranlar.clear(); egikler.clear()
        theta = 0f; kambur = false; esikZamani = null; duzZamani = null
    }

    /** pitch: baş eğimi (derece) veya null; oran: (omuzY − burunY) / omuz genişliği veya null */
    fun guncelle(pitch: Float?, oran: Float?, simdi: Long): Durum {
        val kalan = ((asamaBitis - simdi) / 1000 + 1).coerceAtLeast(0)
        when (asama) {
            Asama.DIK_OTUR -> {
                pitch?.let { pitchler += it }; oran?.let { oranlar += it }
                if (simdi >= asamaBitis) {
                    if (pitchler.isEmpty()) {
                        asamaBitis = simdi + 3000
                        return Durum(asama, 0f, false, 0f, "Yüzün görünmüyor, kameraya bak", false)
                    }
                    asama = Asama.EGIL; asamaBitis = simdi + 2500
                }
                return Durum(asama, 0f, false, 0f, "Kalibrasyon 1/2: Dik otur ($kalan)", false)
            }
            Asama.EGIL -> {
                pitch?.let { egikler += it }
                if (simdi >= asamaBitis) {
                    val p0 = pitchler.average().toFloat()
                    val fark = if (egikler.isEmpty()) 0f else egikler.average().toFloat() - p0
                    if (abs(fark) < 4f) {
                        egikler.clear(); asamaBitis = simdi + 2500
                        return Durum(asama, 0f, false, 0f, "Biraz daha fazla eğ", false)
                    }
                    pitch0 = p0
                    oran0 = if (oranlar.isEmpty()) null else oranlar.average().toFloat()
                    yon = sign(fark)
                    asama = Asama.TAKIP
                    return Durum(asama, 0f, false, 0f, null, false)
                }
                return Durum(asama, 0f, false, 0f, "Kalibrasyon 2/2: Başını öne eğ ($kalan)", false)
            }
            Asama.TAKIP -> Unit
        }

        // --- Açı hesabı ---
        val pitchTerimi = pitch?.let { max(0f, yon * (it - pitch0)) }
        val oranTerimi = if (oran != null && oran0 != null) max(0f, 90f * (1f - oran / oran0!!)) else null
        val ham = when {
            pitchTerimi != null && oranTerimi != null -> 0.7f * pitchTerimi + 0.3f * oranTerimi
            else -> pitchTerimi ?: oranTerimi
        }
        if (ham != null) theta = ema * ham + (1 - ema) * theta

        // --- Durum makinesi (histerezisli) ---
        var yeniUyari = false
        if (theta >= esik1) {
            duzZamani = null
            if (esikZamani == null) esikZamani = simdi
            if (!kambur && simdi - esikZamani!! >= bekleme) { kambur = true; yeniUyari = true }
        } else {
            esikZamani = null
            if (kambur && theta < duzelmeEsigi) {
                if (duzZamani == null) duzZamani = simdi
                if (simdi - duzZamani!! >= duzelmeBekleme) { kambur = false; duzZamani = null }
            } else duzZamani = null
        }

        val karartma = if (!kambur) 0f
        else min(70f, max(10f, 10f + (theta - esik1) / (esik2 - esik1) * 60f)) / 100f

        return Durum(asama, theta, kambur, karartma,
            if (kambur) "Boynunu dik tut!" else null, yeniUyari)
    }
}
