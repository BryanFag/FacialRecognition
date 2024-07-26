import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.graphics.PointF
import android.graphics.Rect
import android.util.Log
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import androidx.camera.view.PreviewView
import br.com.bryan.facial.recognition.FaceOverlayView
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

@SuppressLint("SetTextI18n")
@OptIn(ExperimentalGetImage::class)
fun processImageProxy(
    imageProxy: ImageProxy,
    faceOverlay: FaceOverlayView,
    previewView: PreviewView,
    similarityTextView: TextView
) {
    Log.d(TAG, "processImageProxy called")

    imageProxy.image?.let { mediaImage ->
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        val detector = FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                .build()
        )

        detector.process(image)
            .addOnSuccessListener { faces ->
                Log.d(TAG, "Face detection successful")

                faces.firstOrNull()?.let { face ->
                    val adjustedFaceRect = adjustRectToPreview(
                        face.boundingBox, imageProxy.width, imageProxy.height,
                        previewView.width, previewView.height, true
                    )
                    val points = face.allContours.flatMap { it.points }.map { point ->
                        adjustPointToPreview(
                            point, imageProxy.width, imageProxy.height,
                            previewView.width, previewView.height, true
                        )
                    }

                    // Log de todos os pontos detectados
                    Log.d(TAG, "Face points: $points")

                    // Filtrar pontos para remover outliers
                    val filteredPoints = filterPoints(points)

                    // Verificar se temos pontos suficientes para os cálculos
                    if (filteredPoints.size >= 68) { // Verifica se temos pelo menos 68 pontos
                        // Selecionar pontos específicos para olhos, nariz e boca
                        val leftEyePoint = filteredPoints[36]
                        val rightEyePoint = filteredPoints[45]
                        val noseBasePoint = filteredPoints[30]
                        val mouthLeftPoint = filteredPoints[48]
                        val mouthRightPoint = filteredPoints[54]
                        val chinPoint = filteredPoints[8]
                        val foreheadPoint = filteredPoints[19]

                        // Calcular as distâncias desejadas
                        val eyeDistance = calculateDistance(leftEyePoint, rightEyePoint)
                        val noseToMouthDistance = calculateDistance(noseBasePoint, PointF((mouthLeftPoint.x + mouthRightPoint.x) / 2, (mouthLeftPoint.y + mouthRightPoint.y) / 2))
                        val foreheadToChinDistance = calculateDistance(foreheadPoint, chinPoint)

                        // Atualizar a sobreposição com as novas medições
                        faceOverlay.updateFace(adjustedFaceRect, points)

                        // Calcular a similaridade
                        val similarity = calculateSimilarity(
                            eyeDistance,
                            foreheadToChinDistance,
                            noseToMouthDistance
                        )

                        // Exibir as medições de distância na tela
                        val resultText = "Eye Distance: ${eyeDistance.toInt()} pixels\n" +
                                "Forehead to Chin Distance: ${foreheadToChinDistance.toInt()} pixels\n" +
                                "Nose to Mouth Distance: ${noseToMouthDistance.toInt()} pixels\n" +
                                "Similarity: ${similarity.toInt()}%"
                        similarityTextView.text = resultText

                        // Log dos resultados
                        Log.d(TAG, "Eye Distance: ${eyeDistance.toInt()} pixels")
                        Log.d(TAG, "Forehead to Chin Distance: ${foreheadToChinDistance.toInt()} pixels")
                        Log.d(TAG, "Nose to Mouth Distance: ${noseToMouthDistance.toInt()} pixels")
                        Log.d(TAG, "Similarity: ${similarity.toInt()}%")
                    } else {
                        Log.d(TAG, "Not enough points for calculation")
                        similarityTextView.text = "Not enough points for calculation"
                    }
                } ?: run {
                    Log.d(TAG, "No face detected")
                    faceOverlay.updateFace(null, null)
                    similarityTextView.text = "No face detected"
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Face detection failed", e)
                similarityTextView.text = "Face detection failed"
            }
            .addOnCompleteListener { imageProxy.close() }
    } ?: imageProxy.close()
}

// Função para calcular a similaridade com base nas distâncias fornecidas
fun calculateSimilarity(eyeDistance: Float, foreheadToChinDistance: Float, noseToMouthDistance: Float): Float {
    val referenceEyeDistance = 29f
    val referenceForeheadToChinDistance = 251f
    val referenceNoseToMouthDistance = 94f

    val eyeDistanceDifference = abs(eyeDistance - referenceEyeDistance)
    val foreheadToChinDistanceDifference = abs(foreheadToChinDistance - referenceForeheadToChinDistance)
    val noseToMouthDistanceDifference = abs(noseToMouthDistance - referenceNoseToMouthDistance)

    val maxDifference = referenceEyeDistance + referenceForeheadToChinDistance + referenceNoseToMouthDistance

    val totalDifference = eyeDistanceDifference + foreheadToChinDistanceDifference + noseToMouthDistanceDifference

    val similarity = (1 - totalDifference / maxDifference) * 100

    return similarity.coerceIn(0f, 100f)
}

// Função para filtrar os pontos
fun filterPoints(points: List<PointF>): List<PointF> {
    // Filtrar pontos que estão muito longe da média
    val meanX = points.map { it.x }.average()
    val meanY = points.map { it.y }.average()
    val stdDevX = points.map { abs(it.x - meanX) }.average()
    val stdDevY = points.map { abs(it.y - meanY) }.average()

    return points.filter { abs(it.x - meanX) < 2 * stdDevX && abs(it.y - meanY) < 2 * stdDevY }
}

// Função para calcular a distância entre dois pontos
fun calculateDistance(point1: PointF, point2: PointF): Float {
    return sqrt((point1.x - point2.x).pow(2) + (point1.y - point2.y).pow(2))
}

// Funções para ajustar as coordenadas para a visualização da câmera
fun adjustRectToPreview(
    faceRect: Rect, imageWidth: Int, imageHeight: Int,
    previewWidth: Int, previewHeight: Int, isFrontCamera: Boolean
): Rect {
    val scaleX = previewWidth.toFloat() / imageWidth
    val scaleY = previewHeight.toFloat() / imageHeight

    var left = faceRect.left * scaleX
    var right = faceRect.right * scaleX

    val top = faceRect.top * scaleY
    val bottom = faceRect.bottom * scaleY

    if (isFrontCamera) {
        val tempLeft = left
        left = previewWidth - right
        right = previewWidth - tempLeft
    }

    return Rect(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())
}

fun adjustPointToPreview(
    point: PointF, imageWidth: Int, imageHeight: Int,
    previewWidth: Int, previewHeight: Int, isFrontCamera: Boolean
): PointF {
    val scaleX = previewWidth.toFloat() / imageWidth
    val scaleY = previewHeight.toFloat() / imageHeight

    var x = point.x * scaleX
    val y = point.y * scaleY

    if (isFrontCamera) {
        x = previewWidth - x
    }

    return PointF(x, y)
}
