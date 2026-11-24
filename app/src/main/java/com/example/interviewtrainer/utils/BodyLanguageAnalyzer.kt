package com.example.interviewtrainer.utils

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
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import kotlin.math.abs
import kotlin.math.hypot

data class LiveVideoMetrics(
    val faceDetected: Boolean = false,
    val eyeContactOk: Boolean = true,
    val postureStable: Boolean = true,
    val statusLabelRo: String = "Calibrare cameră...",
    val statusLabelEn: String = "Calibrating camera..."
)

data class VideoSessionReport(
    val totalFramesAnalyzed: Int,
    val eyeContactScore: Int,
    val postureStabilityScore: Int,
    val engagementScore: Int,
    val feedbackRo: String,
    val feedbackEn: String
)

class BodyLanguageAnalyzer(
    context: Context,
    private val onLiveUpdate: (LiveVideoMetrics) -> Unit
) : ImageAnalysis.Analyzer {

    private var faceLandmarker: FaceLandmarker? = null

    private var totalFrames = 0
    private var eyeContactFrames = 0
    private var stablePostureFrames = 0
    private var engagementAccumulator = 0f

    private var prevNoseX: Float? = null
    private var prevNoseY: Float? = null
    private var lastProcessedTime = 0L

    init {
        try {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath("face_landmarker.task")
                .build()

            val options = FaceLandmarker.FaceLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.VIDEO)
                .setNumFaces(1)
                .setOutputFaceBlendshapes(true)
                .build()

            faceLandmarker = FaceLandmarker.createFromOptions(context, options)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun analyze(imageProxy: ImageProxy) {
        val now = SystemClock.uptimeMillis()
        // Procesăm ~5 cadre pe secundă pentru a păstra telefonul rece și UI-ul fluid
        if (now - lastProcessedTime < 200L) {
            imageProxy.close()
            return
        }
        lastProcessedTime = now

        val landmarker = faceLandmarker
        if (landmarker == null) {
            imageProxy.close()
            return
        }

        try {
            val bitmap = imageProxy.toRotatedBitmap()
            val mpImage = BitmapImageBuilder(bitmap).build()
            val result = landmarker.detectForVideo(mpImage, now)
            processResult(result)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            imageProxy.close()
        }
    }

    private fun processResult(result: FaceLandmarkerResult) {
        val landmarks = result.faceLandmarks().firstOrNull()
        if (landmarks == null || landmarks.size < 455) {
            onLiveUpdate(
                LiveVideoMetrics(
                    faceDetected = false,
                    eyeContactOk = false,
                    postureStable = false,
                    statusLabelRo = "Fața nu este în cadru",
                    statusLabelEn = "Face not in frame"
                )
            )
            return
        }

        totalFrames++

        // Puncte cheie MediaPipe Face Mesh: 1 = vârful nasului, 234 = obraz stâng, 454 = obraz drept, 10 = frunte, 152 = bărbie
        val nose = landmarks[1]
        val leftCheek = landmarks[234]
        val rightCheek = landmarks[454]
        val forehead = landmarks[10]
        val chin = landmarks[152]

        // 1. Contact vizual (orientare stânga-dreapta și sus-jos)
        val faceWidth = abs(rightCheek.x() - leftCheek.x()).coerceAtLeast(0.01f)
        val faceCenterX = (leftCheek.x() + rightCheek.x()) / 2f
        val horizontalOffsetRatio = abs(nose.x() - faceCenterX) / faceWidth

        val faceHeight = abs(chin.y() - forehead.y()).coerceAtLeast(0.01f)
        val faceCenterY = (forehead.y() + chin.y()) / 2f
        val verticalOffsetRatio = abs(nose.y() - faceCenterY) / faceHeight

        val isLookingAtCamera = horizontalOffsetRatio < 0.16f && verticalOffsetRatio < 0.18f
        if (isLookingAtCamera) eyeContactFrames++

        // 2. Stabilitatea posturii (mișcarea bruscă a capului / umerilor între cadre)
        val movementDelta = if (prevNoseX != null && prevNoseY != null) {
            hypot((nose.x() - prevNoseX!!).toDouble(), (nose.y() - prevNoseY!!).toDouble()).toFloat()
        } else 0f

        prevNoseX = nose.x()
        prevNoseY = nose.y()

        val isPostureStable = movementDelta < 0.035f
        if (isPostureStable) stablePostureFrames++

        // 3. Expresivitate / Engagement din blendshapes (TFLite output)
        var frameEngagement = 0.65f
        val blendshapes = result.faceBlendshapes().orElse(null)?.firstOrNull()
        if (blendshapes != null) {
            val smileLeft = blendshapes.find { it.categoryName() == "mouthSmileLeft" }?.score() ?: 0f
            val smileRight = blendshapes.find { it.categoryName() == "mouthSmileRight" }?.score() ?: 0f
            val browUp = blendshapes.find { it.categoryName() == "browInnerUp" }?.score() ?: 0f
            val frownLeft = blendshapes.find { it.categoryName() == "browDownLeft" }?.score() ?: 0f

            val positiveSignal = ((smileLeft + smileRight) * 0.45f) + (browUp * 0.25f) - (frownLeft * 0.30f)
            frameEngagement = (0.65f + positiveSignal).coerceIn(0.2f, 1.0f)
        }
        engagementAccumulator += frameEngagement

        val (statusRo, statusEn) = when {
            !isLookingAtCamera -> "Privește spre cameră" to "Look at the camera"
            !isPostureStable -> "Păstrează postura calmă" to "Keep steady posture"
            else -> "Contact vizual optim" to "Great eye contact"
        }

        onLiveUpdate(
            LiveVideoMetrics(
                faceDetected = true,
                eyeContactOk = isLookingAtCamera,
                postureStable = isPostureStable,
                statusLabelRo = statusRo,
                statusLabelEn = statusEn
            )
        )
    }

    fun buildSessionReport(): VideoSessionReport? {
        if (totalFrames < 3) return null

        val eyeScore = ((eyeContactFrames.toFloat() / totalFrames) * 100).toInt().coerceIn(0, 100)
        val postureScore = ((stablePostureFrames.toFloat() / totalFrames) * 100).toInt().coerceIn(0, 100)
        val engagementScore = ((engagementAccumulator / totalFrames) * 100).toInt().coerceIn(0, 100)

        val notesRo = mutableListOf<String>()
        val notesEn = mutableListOf<String>()

        if (eyeScore >= 75) {
            notesRo.add("Ai menținut un contact vizual foarte bun ($eyeScore%).")
            notesEn.add("You maintained strong eye contact ($eyeScore%).")
        } else {
            notesRo.add("Încearcă să privești mai constant spre cameră când formulezi răspunsul ($eyeScore%).")
            notesEn.add("Try to keep steadier eye contact with the camera while answering ($eyeScore%).")
        }

        if (postureScore >= 75) {
            notesRo.add("Postură calmă și stăpână pe sine.")
            notesEn.add("Calm and confident posture throughout the session.")
        } else {
            notesRo.add("S-au detectat mișcări frecvente ale capului/trunchiului — respiră și păstrează o postură stabilă.")
            notesEn.add("Frequent head/upper-body movement detected — take a breath and keep a steady posture.")
        }

        return VideoSessionReport(
            totalFramesAnalyzed = totalFrames,
            eyeContactScore = eyeScore,
            postureStabilityScore = postureScore,
            engagementScore = engagementScore,
            feedbackRo = notesRo.joinToString(" "),
            feedbackEn = notesEn.joinToString(" ")
        )
    }

    fun close() {
        try {
            faceLandmarker?.close()
        } catch (_: Exception) {
        }
        faceLandmarker = null
    }

    private fun ImageProxy.toRotatedBitmap(): Bitmap {
        val rawBitmap = toBitmap()
        val rotation = imageInfo.rotationDegrees
        if (rotation == 0) return rawBitmap
        val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
        return Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
    }
}