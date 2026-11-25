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
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import kotlin.math.abs
import kotlin.math.hypot

data class LiveVideoMetrics(
    val faceDetected: Boolean = false,
    val eyeContactOk: Boolean = true,
    val shouldersAligned: Boolean = true,
    val handsActive: Boolean = false,
    val nervousFaceTouch: Boolean = false,
    val statusLabelRo: String = "Calibrare cameră AI...",
    val statusLabelEn: String = "Calibrating AI camera..."
)

data class VideoSessionReport(
    val totalFramesAnalyzed: Int,
    val eyeContactScore: Int,
    val postureStabilityScore: Int,
    val engagementScore: Int,
    val handGestureScore: Int,
    val faceTouchCount: Int,
    val feedbackRo: String,
    val feedbackEn: String
)

class BodyLanguageAnalyzer(
    context: Context,
    private val onLiveUpdate: (LiveVideoMetrics) -> Unit
) : ImageAnalysis.Analyzer {

    private var faceLandmarker: FaceLandmarker? = null
    private var poseLandmarker: PoseLandmarker? = null
    private var handLandmarker: HandLandmarker? = null

    @Volatile
    private var isClosed = false

    private var totalFrames = 0
    private var eyeContactFrames = 0
    private var alignedShouldersFrames = 0
    private var handsDetectedFrames = 0
    private var faceTouchCount = 0
    private var engagementAccumulator = 0f

    private var prevShoulderMidX: Float? = null
    private var prevShoulderMidY: Float? = null
    private var lastProcessedTime = 0L

    init {
        try {
            faceLandmarker = FaceLandmarker.createFromOptions(
                context,
                FaceLandmarker.FaceLandmarkerOptions.builder()
                    .setBaseOptions(BaseOptions.builder().setModelAssetPath("face_landmarker.task").build())
                    .setRunningMode(RunningMode.VIDEO)
                    .setNumFaces(1)
                    .setOutputFaceBlendshapes(true)
                    .build()
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            poseLandmarker = PoseLandmarker.createFromOptions(
                context,
                PoseLandmarker.PoseLandmarkerOptions.builder()
                    .setBaseOptions(BaseOptions.builder().setModelAssetPath("pose_landmarker_lite.task").build())
                    .setRunningMode(RunningMode.VIDEO)
                    .setNumPoses(1)
                    .build()
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            handLandmarker = HandLandmarker.createFromOptions(
                context,
                HandLandmarker.HandLandmarkerOptions.builder()
                    .setBaseOptions(BaseOptions.builder().setModelAssetPath("hand_landmarker.task").build())
                    .setRunningMode(RunningMode.VIDEO)
                    .setNumHands(2)
                    .build()
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Synchronized
    override fun analyze(imageProxy: ImageProxy) {
        if (isClosed) {
            imageProxy.close()
            return
        }

        val now = SystemClock.uptimeMillis()
        if (now - lastProcessedTime < 220L) {
            imageProxy.close()
            return
        }
        lastProcessedTime = now

        try {
            val bitmap = imageProxy.toRotatedBitmap()
            val mpImage = BitmapImageBuilder(bitmap).build()

            val faceResult = faceLandmarker?.detectForVideo(mpImage, now)
            val poseResult = poseLandmarker?.detectForVideo(mpImage, now)
            val handResult = handLandmarker?.detectForVideo(mpImage, now)

            val faceLandmarks = faceResult?.faceLandmarks()?.firstOrNull()
            if (faceLandmarks == null || faceLandmarks.size < 455) {
                onLiveUpdate(
                    LiveVideoMetrics(
                        faceDetected = false,
                        eyeContactOk = false,
                        shouldersAligned = false,
                        statusLabelRo = "Poziționează-te în cadru",
                        statusLabelEn = "Position yourself in frame"
                    )
                )
                return
            }

            totalFrames++

            val nose = faceLandmarks[1]
            val leftCheek = faceLandmarks[234]
            val rightCheek = faceLandmarks[454]
            val forehead = faceLandmarks[10]
            val chin = faceLandmarks[152]

            val faceWidth = abs(rightCheek.x() - leftCheek.x()).coerceAtLeast(0.01f)
            val faceCenterX = (leftCheek.x() + rightCheek.x()) / 2f
            val horizontalOffset = abs(nose.x() - faceCenterX) / faceWidth

            val faceHeight = abs(chin.y() - forehead.y()).coerceAtLeast(0.01f)
            val faceCenterY = (forehead.y() + chin.y()) / 2f
            val verticalOffset = abs(nose.y() - faceCenterY) / faceHeight

            val isLookingAtCamera = horizontalOffset < 0.16f && verticalOffset < 0.18f
            if (isLookingAtCamera) eyeContactFrames++

            var shouldersOk = true
            val poseLandmarks = poseResult?.landmarks()?.firstOrNull()
            if (poseLandmarks != null && poseLandmarks.size > 12) {
                val leftShoulder = poseLandmarks[11]
                val rightShoulder = poseLandmarks[12]

                val shoulderTilt = abs(leftShoulder.y() - rightShoulder.y())
                val midX = (leftShoulder.x() + rightShoulder.x()) / 2f
                val midY = (leftShoulder.y() + rightShoulder.y()) / 2f

                val sway = if (prevShoulderMidX != null && prevShoulderMidY != null) {
                    hypot((midX - prevShoulderMidX!!).toDouble(), (midY - prevShoulderMidY!!).toDouble()).toFloat()
                } else 0f

                prevShoulderMidX = midX
                prevShoulderMidY = midY

                shouldersOk = shoulderTilt < 0.065f && sway < 0.04f
            }
            if (shouldersOk) alignedShouldersFrames++

            val hands = handResult?.landmarks().orEmpty()
            val handsVisible = hands.isNotEmpty()
            if (handsVisible) handsDetectedFrames++

            var touchingFace = false
            for (hand in hands) {
                val indexTip = hand[8]
                val distToNose = hypot((indexTip.x() - nose.x()).toDouble(), (indexTip.y() - nose.y()).toDouble()).toFloat()
                if (distToNose < faceWidth * 0.65f) {
                    touchingFace = true
                    faceTouchCount++
                    break
                }
            }

            var frameEngagement = 0.65f
            val blendshapes = faceResult.faceBlendshapes().orElse(null)?.firstOrNull()
            if (blendshapes != null) {
                val smileLeft = blendshapes.find { it.categoryName() == "mouthSmileLeft" }?.score() ?: 0f
                val smileRight = blendshapes.find { it.categoryName() == "mouthSmileRight" }?.score() ?: 0f
                val browUp = blendshapes.find { it.categoryName() == "browInnerUp" }?.score() ?: 0f
                val positive = ((smileLeft + smileRight) * 0.45f) + (browUp * 0.25f)
                frameEngagement = (0.65f + positive).coerceIn(0.25f, 1.0f)
            }
            engagementAccumulator += frameEngagement

            val (statusRo, statusEn) = when {
                touchingFace -> "Evită să îți atingi fața" to "Avoid touching your face"
                !isLookingAtCamera -> "Privește spre Amalia (cameră)" to "Look at Amalia (camera)"
                !shouldersOk -> "Îndreaptă umerii / postură calmă" to "Align shoulders / steady posture"
                handsVisible -> "Prezență & gesticulație bună" to "Great presence & gestures"
                else -> "Contact vizual & postură optimă" to "Optimal eye contact & posture"
            }

            onLiveUpdate(
                LiveVideoMetrics(
                    faceDetected = true,
                    eyeContactOk = isLookingAtCamera,
                    shouldersAligned = shouldersOk,
                    handsActive = handsVisible,
                    nervousFaceTouch = touchingFace,
                    statusLabelRo = statusRo,
                    statusLabelEn = statusEn
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            imageProxy.close()
        }
    }

    fun buildSessionReport(): VideoSessionReport? {
        if (totalFrames < 3) return null

        val eyeScore = ((eyeContactFrames.toFloat() / totalFrames) * 100).toInt().coerceIn(0, 100)
        val postureScore = ((alignedShouldersFrames.toFloat() / totalFrames) * 100).toInt().coerceIn(0, 100)
        val engagementScore = ((engagementAccumulator / totalFrames) * 100).toInt().coerceIn(0, 100)
        val gestureRatio = (handsDetectedFrames.toFloat() / totalFrames)
        val gestureScore = when {
            gestureRatio in 0.15f..0.75f -> 90
            gestureRatio > 0.75f -> 75
            else -> 60
        }

        val tipsRo = mutableListOf<String>()
        val tipsEn = mutableListOf<String>()

        if (eyeScore >= 75) {
            tipsRo.add("✓ Contact vizual excelent ($eyeScore%) — ai transmis încredere.")
            tipsEn.add("✓ Excellent eye contact ($eyeScore%) — projected confidence.")
        } else {
            tipsRo.add("• Contact Vizual ($eyeScore%): Privește direct spre camera frontală când explici soluția.")
            tipsEn.add("• Eye Contact ($eyeScore%): Look directly at the front camera lens while explaining.")
        }

        if (postureScore >= 75) {
            tipsRo.add("✓ Postură stabilă a umerilor ($postureScore%).")
            tipsEn.add("✓ Steady, aligned shoulder posture ($postureScore%).")
        } else {
            tipsRo.add("• Postură ($postureScore%): Păstrează umerii drepți și evită balansul trunchiului.")
            tipsEn.add("• Posture ($postureScore%): Keep shoulders level and avoid swaying while thinking.")
        }

        if (faceTouchCount > 2) {
            tipsRo.add("• Control Gesturi: Ai dus mâna la față de $faceTouchCount ori — folosește gesturi deschise.")
            tipsEn.add("• Gesture Control: You touched your face $faceTouchCount times — use open hand gestures.")
        } else if (gestureRatio < 0.15f) {
            tipsRo.add("• Gesticulație ($gestureScore%): Încadrează telefonul puțin mai departe pentru a folosi natural mâinile.")
            tipsEn.add("• Hand Gestures ($gestureScore%): Place camera slightly further to include natural hand gestures.")
        } else {
            tipsRo.add("✓ Gesticulație echilibrată ($gestureScore%) pe parcursul explicațiilor.")
            tipsEn.add("✓ Balanced hand gestures ($gestureScore%) during explanations.")
        }

        if (engagementScore < 70) {
            tipsRo.add("• Expresivitate ($engagementScore%): Încearcă un ton facial mai deschis și un zâmbet ușor.")
            tipsEn.add("• Engagement ($engagementScore%): Try a warmer facial expression and a slight smile.")
        }

        return VideoSessionReport(
            totalFramesAnalyzed = totalFrames,
            eyeContactScore = eyeScore,
            postureStabilityScore = postureScore,
            engagementScore = engagementScore,
            handGestureScore = gestureScore,
            faceTouchCount = faceTouchCount,
            feedbackRo = tipsRo.joinToString("\n"),
            feedbackEn = tipsEn.joinToString("\n")
        )
    }

    @Synchronized
    fun close() {
        isClosed = true
        try { faceLandmarker?.close() } catch (_: Exception) {}
        try { poseLandmarker?.close() } catch (_: Exception) {}
        try { handLandmarker?.close() } catch (_: Exception) {}
        faceLandmarker = null
        poseLandmarker = null
        handLandmarker = null
    }

    private fun ImageProxy.toRotatedBitmap(): Bitmap {
        val rawBitmap = toBitmap()
        val rotation = imageInfo.rotationDegrees
        if (rotation == 0) return rawBitmap
        val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
        return Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
    }
}