package com.dikdur.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import kotlin.math.atan2
import kotlin.math.hypot

/**
 * Kamera karelerini MediaPipe yapay zekâ modellerine verir,
 * baş eğimini (pitch) ve burun–omuz oranını hesaplar.
 */
class PostureAnalyzer(
    context: Context,
    private val sonuc: (pitch: Float?, oran: Float?) -> Unit
) : ImageAnalysis.Analyzer {

    private val yuz: FaceLandmarker = FaceLandmarker.createFromOptions(
        context,
        FaceLandmarker.FaceLandmarkerOptions.builder()
            .setBaseOptions(BaseOptions.builder().setModelAssetPath("face_landmarker.task").build())
            .setRunningMode(RunningMode.IMAGE)
            .setNumFaces(1)
            .setOutputFacialTransformationMatrixes(true)
            .build()
    )

    private val poz: PoseLandmarker = PoseLandmarker.createFromOptions(
        context,
        PoseLandmarker.PoseLandmarkerOptions.builder()
            .setBaseOptions(BaseOptions.builder().setModelAssetPath("pose_landmarker_lite.task").build())
            .setRunningMode(RunningMode.IMAGE)
            .setNumPoses(1)
            .build()
    )

    private var sonAnaliz = 0L

    override fun analyze(kare: ImageProxy) {
        val simdi = SystemClock.uptimeMillis()
        if (simdi - sonAnaliz < 100) { kare.close(); return }   // saniyede ~10 kare yeterli
        sonAnaliz = simdi

        val bitmap = try { donmusBitmap(kare) } finally { kare.close() }
        val goruntu = BitmapImageBuilder(bitmap).build()

        // 1) Baş eğimi: yüzün 3B dönüşüm matrisinden (4x4, sütun öncelikli)
        var pitch: Float? = null
        val yuzSonuc = yuz.detect(goruntu)
        val matrisler = yuzSonuc.facialTransformationMatrixes()
        if (matrisler.isPresent && matrisler.get().isNotEmpty()) {
            val m = matrisler.get()[0]
            pitch = Math.toDegrees(atan2(m[6].toDouble(), m[10].toDouble())).toFloat()
        }

        // 2) Burun ile omuz çizgisi arasındaki mesafe / omuz genişliği
        var oran: Float? = null
        val pozSonuc = poz.detect(goruntu)
        if (pozSonuc.landmarks().isNotEmpty()) {
            val p = pozSonuc.landmarks()[0]
            val burun = p[0]; val sol = p[11]; val sag = p[12]
            val gorunur = sol.visibility().orElse(1f) > 0.5f && sag.visibility().orElse(1f) > 0.5f
            if (gorunur) {
                val omuzY = (sol.y() + sag.y()) / 2f
                val genislik = hypot(sol.x() - sag.x(), sol.y() - sag.y())
                if (genislik > 0.05f) oran = (omuzY - burun.y()) / genislik
            }
        }
        sonuc(pitch, oran)
    }

    private fun donmusBitmap(kare: ImageProxy): Bitmap {
        val ham = kare.toBitmap()
        val derece = kare.imageInfo.rotationDegrees
        if (derece == 0) return ham
        val matris = Matrix().apply { postRotate(derece.toFloat()) }
        return Bitmap.createBitmap(ham, 0, 0, ham.width, ham.height, matris, true)
    }

    fun kapat() { yuz.close(); poz.close() }
}
