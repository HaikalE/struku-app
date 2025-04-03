package com.example.struku.data.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Point
import android.graphics.RectF
import android.net.Uri
import android.util.Log
import android.util.Size
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfDouble
import org.opencv.core.MatOfFloat
import org.opencv.core.MatOfInt
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.core.Size as OpenCVSize
import org.opencv.imgproc.CLAHE
import org.opencv.imgproc.Imgproc
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Implementasi advanced image preprocessing untuk struk
 * 
 * Pipeline ini mengimplementasikan berbagai teknik pre-processing gambar
 * dan menggabungkannya secara dinamis berdasarkan jenis struk dan kondisi gambar.
 */
@Singleton
class AdvancedImagePreprocessor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val visualizer: PreprocessingVisualizer
) {
    companion object {
        private const val TAG = "AdvancedImagePreprocess"
        
        // Model file untuk ML-based enhancement (untuk future implementation)
        private const val ENHANCEMENT_MODEL_FILE = "models/receipt_enhancement.tflite"
        
        // Ukuran standar untuk normalisasi gambar
        private const val NORMALIZED_WIDTH = 1280
        private const val NORMALIZED_HEIGHT = 1920
    }
    
    private val _preprocessingState = MutableStateFlow<PreprocessingState>(PreprocessingState.Idle)
    val preprocessingState = _preprocessingState.asStateFlow()
    
    private val progressChannel = Channel<Float>(Channel.CONFLATED)
    
    // TF Lite interpreter untuk model enhancement (untuk future implementation)
    private var tfLiteInterpreter: Interpreter? = null
    private var gpuDelegate: GpuDelegate? = null
    
    // Inicjalizasi OpenCV
    init {
        if (!OpenCVLoader.initDebug()) {
            Log.e(TAG, "OpenCV initialization failed")
        } else {
            Log.d(TAG, "OpenCV initialized successfully")
        }
        
        // Inicjalizasi model TF Lite (untuk future implementation)
        /*
        try {
            val model = loadModelFile()
            val interpreterOptions = Interpreter.Options()
            
            // Gunakan GPU acceleration jika tersedia
            if (isGpuAvailable()) {
                gpuDelegate = GpuDelegate()
                interpreterOptions.addDelegate(gpuDelegate)
            }
            
            tfLiteInterpreter = Interpreter(model, interpreterOptions)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load TF Lite model: ${e.message}")
        }
        */
    }
    
    /**
     * Status pre-processing
     */
    sealed class PreprocessingState {
        object Idle : PreprocessingState()
        class InProgress(val stage: String, val progress: Float) : PreprocessingState()
        class Success(val resultBitmap: Bitmap) : PreprocessingState()
        class Error(val message: String, val exception: Exception? = null) : PreprocessingState()
    }
    
    /**
     * Memproses gambar struk dengan pipeline pre-processing lengkap
     * @param inputBitmap Gambar struk asli
     * @param config Konfigurasi pre-processing
     * @return Gambar yang telah diproses, siap untuk OCR
     */
    suspend fun processReceiptImage(
        inputBitmap: Bitmap,
        config: ReceiptPreprocessingConfig = ReceiptPreprocessingConfig()
    ): Bitmap = withContext(Dispatchers.Default) {
        if (!config.enabled) {
            return@withContext inputBitmap
        }
        
        // Capture original input
        visualizer.captureProcessingStep(
            "Original Input",
            "Raw input image before any preprocessing",
            inputBitmap
        )
        
        try {
            _preprocessingState.value = PreprocessingState.InProgress("Initializing", 0.0f)
            
            // 1. Normalisasi ukuran gambar
            updateProgress("Normalizing image size", 0.05f)
            val normalizedBitmap = normalizeImageSize(inputBitmap)
            visualizer.captureProcessingStep(
                "Size Normalization",
                "Input image normalized to standard dimensions: ${normalizedBitmap.width}x${normalizedBitmap.height}",
                normalizedBitmap
            )
            
            // 2. Deteksi dan koreksi perspektif dokumen
            updateProgress("Detecting document boundaries", 0.1f)
            val documentBitmap = if (config.documentDetectionConfig.enableMlKitDocumentScanner) {
                try {
                    val result = detectAndCropDocumentMlKit(normalizedBitmap)
                    visualizer.captureProcessingStep(
                        "ML Kit Document Detection",
                        "Document boundaries detected and perspective corrected using ML Kit Document Scanner",
                        result
                    )
                    result
                } catch (e: Exception) {
                    Log.w(TAG, "ML Kit document detection failed: ${e.message}. Falling back to OpenCV")
                    val result = detectAndCorrectPerspective(normalizedBitmap, config.documentDetectionConfig)
                    visualizer.captureProcessingStep(
                        "OpenCV Document Detection",
                        "Document boundaries detected and perspective corrected using OpenCV (fallback)",
                        result
                    )
                    result
                }
            } else {
                val result = detectAndCorrectPerspective(normalizedBitmap, config.documentDetectionConfig)
                visualizer.captureProcessingStep(
                    "OpenCV Document Detection",
                    "Document boundaries detected and perspective corrected using OpenCV",
                    result
                )
                result
            }
            
            // 3. Identifikasi jenis struk jika auto-detect
            updateProgress("Analyzing receipt type", 0.2f)
            val effectiveConfig = if (config.receiptType == ReceiptType.AUTO_DETECT) {
                val detectedType = detectReceiptType(documentBitmap)
                visualizer.captureProcessingStep(
                    "Receipt Type Detection",
                    "Detected receipt type: $detectedType based on image characteristics",
                    documentBitmap
                )
                when (detectedType) {
                    ReceiptType.THERMAL -> config.copy(receiptType = ReceiptType.THERMAL)
                    ReceiptType.INKJET -> config.copy(receiptType = ReceiptType.INKJET)
                    ReceiptType.LASER -> config.copy(receiptType = ReceiptType.LASER)
                    ReceiptType.DIGITAL -> config.copy(receiptType = ReceiptType.DIGITAL)
                    else -> config
                }
            } else {
                config
            }
            
            // 4. Pre-processing gambar dasar
            updateProgress("Applying basic image processing", 0.3f)
            val preprocessedBasic = applyBasicPreprocessing(documentBitmap, effectiveConfig)
            visualizer.captureProcessingStep(
                "Basic Preprocessing",
                "Grayscale conversion, noise reduction, and contrast enhancement",
                preprocessedBasic
            )
            
            // 5. Jika level preprocessing minimal, kembalikan hasil
            if (effectiveConfig.preprocessingLevel == PreprocessingLevel.BASIC) {
                _preprocessingState.value = PreprocessingState.Success(preprocessedBasic)
                return@withContext preprocessedBasic
            }
            
            // 6. Peningkatan gambar berdasarkan jenis struk
            updateProgress("Enhancing image", 0.4f)
            val enhancedImage = enhanceImage(preprocessedBasic, effectiveConfig)
            visualizer.captureProcessingStep(
                "Receipt-specific Enhancement",
                "Image enhancement optimized for receipt type: ${effectiveConfig.receiptType}",
                enhancedImage
            )
            
            // 7. Deteksi dan koreksi skew
            updateProgress("Correcting image skew", 0.5f)
            val deskewedImage = if (effectiveConfig.layoutAnalysisConfig.enableSkewCorrection) {
                val result = correctSkew(enhancedImage)
                visualizer.captureProcessingStep(
                    "Skew Correction",
                    "Text line skew detection and correction",
                    result
                )
                result
            } else {
                enhancedImage
            }
            
            // 8. Penerapan thresholding adaptif
            updateProgress("Optimizing for text recognition", 0.6f)
            val thresholdedImage = applyAdaptiveThreshold(deskewedImage, effectiveConfig)
            visualizer.captureProcessingStep(
                "Adaptive Thresholding",
                "Applied ${effectiveConfig.imageEnhancementConfig.thresholdingMethod} for text-background separation",
                thresholdedImage
            )
            
            // 9. Analisis layout dokumen 
            updateProgress("Analyzing document layout", 0.7f)
            val layoutAnalyzedImage = if (effectiveConfig.preprocessingLevel == PreprocessingLevel.MAXIMUM &&
                                         effectiveConfig.layoutAnalysisConfig.enableStructuralAnalysis) {
                val result = analyzeDocumentLayout(thresholdedImage, effectiveConfig)
                visualizer.captureProcessingStep(
                    "Layout Analysis",
                    "Document structure analysis and detection of text regions",
                    result
                )
                result
            } else {
                thresholdedImage
            }
            
            // 10. Operasi morfologis untuk memperbaiki keterbacaan teks
            updateProgress("Performing final optimizations", 0.8f)
            val finalImage = if (effectiveConfig.imageEnhancementConfig.enableMorphologicalOperations) {
                val result = applyMorphologicalOperations(layoutAnalyzedImage, effectiveConfig)
                visualizer.captureProcessingStep(
                    "Morphological Operations",
                    "Applied morphological operations to enhance text readability",
                    result
                )
                result
            } else {
                layoutAnalyzedImage
            }
            
            // 11. Peningkatan ketajaman gambar akhir
            updateProgress("Finalizing", 0.9f)
            val sharpenedImage = if (effectiveConfig.imageEnhancementConfig.enableSharpening) {
                val result = sharpenImage(finalImage, effectiveConfig.imageEnhancementConfig.sharpeningIntensity)
                visualizer.captureProcessingStep(
                    "Final Sharpening",
                    "Applied sharpening filter with intensity: ${effectiveConfig.imageEnhancementConfig.sharpeningIntensity}",
                    result
                )
                result
            } else {
                finalImage
            }
            
            // Final result
            visualizer.captureProcessingStep(
                "Final Result",
                "Final preprocessed image ready for OCR",
                sharpenedImage
            )
            
            updateProgress("Complete", 1.0f)
            _preprocessingState.value = PreprocessingState.Success(sharpenedImage)
            return@withContext sharpenedImage
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in preprocessing pipeline: ${e.message}", e)
            _preprocessingState.value = PreprocessingState.Error("Preprocessing failed: ${e.message}", e)
            return@withContext inputBitmap
        }
    }
    
    /**
     * Menggunakan ML Kit Document Scanner untuk mendeteksi dan mengekstrak dokumen
     */
    private suspend fun detectAndCropDocumentMlKit(bitmap: Bitmap): Bitmap = suspendCancellableCoroutine { continuation ->
        try {
            val options = GmsDocumentScannerOptions.Builder()
                .setGalleryImportAllowed(false)
                .setPageLimit(1)
                .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
                .build()
                
            val scanner = GmsDocumentScanning.getClient(options)
            
            // Konversi Bitmap ke Uri for ML Kit
            val tempFile = File.createTempFile("receipt", ".jpg", context.cacheDir)
            val outputStream = tempFile.outputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
            outputStream.close()
            
            val imageUri = Uri.fromFile(tempFile)
            
            scanner.getDocumentFromUri(context, imageUri)
                .addOnSuccessListener { result: GmsDocumentScanningResult ->
                    try {
                        val pages = result.pages
                        
                        if (pages.isNotEmpty()) {
                            val firstPage = pages[0]
                            val resultBitmap = BitmapFactory.decodeFile(firstPage.imageUri.path)
                            
                            if (resultBitmap != null) {
                                continuation.resume(resultBitmap)
                            } else {
                                throw Exception("Failed to decode cropped image")
                            }
                        } else {
                            throw Exception("No pages detected")
                        }
                    } catch (e: Exception) {
                        continuation.resumeWithException(e)
                    } finally {
                        tempFile.delete()
                    }
                }
                .addOnFailureListener { e ->
                    tempFile.delete()
                    continuation.resumeWithException(e)
                }
        } catch (e: Exception) {
            continuation.resumeWithException(e)
        }
    }
    
    /**
     * Mendeteksi dan mengoreksi perspektif gambar menggunakan OpenCV
     */
    private fun detectAndCorrectPerspective(
        inputBitmap: Bitmap,
        config: DocumentDetectionConfig
    ): Bitmap {
        val sourceMat = Mat()
        Utils.bitmapToMat(inputBitmap, sourceMat)
        
        // 1. Konversi ke grayscale
        val grayMat = Mat()
        Imgproc.cvtColor(sourceMat, grayMat, Imgproc.COLOR_BGR2GRAY)
        
        // 2. Gaussian blur untuk mengurangi noise
        Imgproc.GaussianBlur(grayMat, grayMat, OpenCVSize(5.0, 5.0), 0.0)
        
        // 3. Deteksi tepi dengan Canny
        val edgesMat = Mat()
        Imgproc.Canny(grayMat, edgesMat, 75.0, 200.0)
        
        // Visualize edges
        val edgesBitmap = Bitmap.createBitmap(
            edgesMat.cols(), 
            edgesMat.rows(), 
            Bitmap.Config.ARGB_8888
        )
        Utils.matToBitmap(edgesMat, edgesBitmap)
        visualizer.captureProcessingStep(
            "Edge Detection",
            "Canny edge detection for document boundary finding",
            edgesBitmap
        )
        
        // 4. Dilasi untuk menghubungkan tepi yang terputus
        val dilatedEdges = Mat()
        val dilationKernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, OpenCVSize(3.0, 3.0))
        Imgproc.dilate(edgesMat, dilatedEdges, dilationKernel)
        
        // 5. Temukan kontur
        val contours = ArrayList<MatOfPoint>()
        Imgproc.findContours(
            dilatedEdges.clone(), 
            contours, 
            Mat(), 
            Imgproc.RETR_EXTERNAL, 
            Imgproc.CHAIN_APPROX_SIMPLE
        )
        
        // Filter dan sortir kontur berdasarkan area
        val filteredContours = contours
            .filter { Imgproc.contourArea(it) > 5000 } // Filter kontur kecil
            .sortedByDescending { Imgproc.contourArea(it) }
        
        // Visualize contours
        val contoursVisualization = sourceMat.clone()
        Imgproc.drawContours(
            contoursVisualization,
            contours,
            -1,
            Scalar(0.0, 255.0, 0.0),
            2
        )
        val contoursBitmap = Bitmap.createBitmap(
            contoursVisualization.cols(),
            contoursVisualization.rows(),
            Bitmap.Config.ARGB_8888
        )
        Utils.matToBitmap(contoursVisualization, contoursBitmap)
        visualizer.captureProcessingStep(
            "Contours Detection",
            "Found ${contours.size} contours, ${filteredContours.size} significant",
            contoursBitmap
        )
        
        if (filteredContours.isEmpty()) {
            // Jika tidak menemukan kontur yang cocok, kembalikan gambar asli
            val resultBitmap = Bitmap.createBitmap(
                inputBitmap.width, 
                inputBitmap.height, 
                Bitmap.Config.ARGB_8888
            )
            Utils.matToBitmap(sourceMat, resultBitmap)
            return resultBitmap
        }
        
        // 6. Perkirakan polygon dari kontur terbesar
        val approxCurve = MatOfPoint2f()
        val contour2f = MatOfPoint2f()
        filteredContours[0].convertTo(contour2f, CvType.CV_32FC2)
        
        // Tentukan epsilon berdasarkan sensitivitas
        val perimeter = Imgproc.arcLength(contour2f, true)
        val epsilon = (0.02 + config.cornerDetectionSensitivity * 0.06) * perimeter
        
        Imgproc.approxPolyDP(contour2f, approxCurve, epsilon, true)
        
        // Jika approxCurve memiliki 4 titik, lakukan transformasi perspektif
        if (approxCurve.total() == 4L && config.perspectiveCorrection) {
            val points = approxCurve.toArray()
            
            // Urutkan titik (kiri atas, kanan atas, kanan bawah, kiri bawah)
            val sortedPoints = sortPoints(points)
            
            // Visualize quadrilateral
            val quadrilateralVisualization = sourceMat.clone()
            for (i in 0 until 4) {
                Imgproc.line(
                    quadrilateralVisualization,
                    sortedPoints[i],
                    sortedPoints[(i + 1) % 4],
                    Scalar(255.0, 0.0, 0.0),
                    3
                )
            }
            val quadrilateralBitmap = Bitmap.createBitmap(
                quadrilateralVisualization.cols(),
                quadrilateralVisualization.rows(),
                Bitmap.Config.ARGB_8888
            )
            Utils.matToBitmap(quadrilateralVisualization, quadrilateralBitmap)
            visualizer.captureProcessingStep(
                "Document Corners",
                "Found approximate document quadrilateral for perspective correction",
                quadrilateralBitmap
            )
            
            // Hitung width dan height untuk hasil final
            val widthBottom = sqrt(
                (sortedPoints[3].x - sortedPoints[2].x).pow(2) + 
                (sortedPoints[3].y - sortedPoints[2].y).pow(2)
            )
            
            val widthTop = sqrt(
                (sortedPoints[1].x - sortedPoints[0].x).pow(2) + 
                (sortedPoints[1].y - sortedPoints[0].y).pow(2)
            )
            
            val heightRight = sqrt(
                (sortedPoints[2].x - sortedPoints[1].x).pow(2) + 
                (sortedPoints[2].y - sortedPoints[1].y).pow(2)
            )
            
            val heightLeft = sqrt(
                (sortedPoints[3].x - sortedPoints[0].x).pow(2) + 
                (sortedPoints[3].y - sortedPoints[0].y).pow(2)
            )
            
            val resultWidth = max(widthBottom, widthTop).toInt()
            val resultHeight = max(heightRight, heightLeft).toInt()
            
            // Buat titik tujuan untuk transformasi perspektif
            val dstPoints = arrayOf(
                org.opencv.core.Point(0.0, 0.0),                          // kiri atas
                org.opencv.core.Point(resultWidth.toDouble(), 0.0),       // kanan atas
                org.opencv.core.Point(resultWidth.toDouble(), resultHeight.toDouble()),  // kanan bawah
                org.opencv.core.Point(0.0, resultHeight.toDouble())       // kiri bawah
            )
            
            // Transformasi perspektif
            val srcMat = MatOfPoint2f(*sortedPoints)
            val dstMat = MatOfPoint2f(*dstPoints)
            
            val perspectiveTransform = Imgproc.getPerspectiveTransform(srcMat, dstMat)
            val resultMat = Mat()
            Imgproc.warpPerspective(
                sourceMat, 
                resultMat, 
                perspectiveTransform, 
                OpenCVSize(resultWidth.toDouble(), resultHeight.toDouble())
            )
            
            // Konversi kembali ke Bitmap
            val resultBitmap = Bitmap.createBitmap(
                resultWidth, 
                resultHeight, 
                Bitmap.Config.ARGB_8888
            )
            Utils.matToBitmap(resultMat, resultBitmap)
            return resultBitmap
        }
        
        // Jika tidak dapat melakukan transformasi perspektif, kembalikan gambar asli
        val resultBitmap = Bitmap.createBitmap(
            inputBitmap.width, 
            inputBitmap.height, 
            Bitmap.Config.ARGB_8888
        )
        Utils.matToBitmap(sourceMat, resultBitmap)
        return resultBitmap
    }
    
    /**
     * Mengurutkan 4 titik dalam urutan: kiri atas, kanan atas, kanan bawah, kiri bawah
     */
    private fun sortPoints(points: Array<org.opencv.core.Point>): Array<org.opencv.core.Point> {
        // Menghitung centroid dari 4 titik
        val centerX = points.map { it.x }.average()
        val centerY = points.map { it.y }.average()
        
        // Urutkan titik berdasarkan kuadran relatif terhadap centroid
        val topLeft = points.minByOrNull { (it.x - centerX).pow(2) + (it.y - centerY).pow(2) }
            ?: points[0]
        val topRight = points.minByOrNull { (centerX - it.x).pow(2) + (it.y - centerY).pow(2) }
            ?: points[1]
        val bottomRight = points.minByOrNull { (centerX - it.x).pow(2) + (centerY - it.y).pow(2) }
            ?: points[2]
        val bottomLeft = points.minByOrNull { (it.x - centerX).pow(2) + (centerY - it.y).pow(2) }
            ?: points[3]
        
        return arrayOf(topLeft, topRight, bottomRight, bottomLeft)
    }
    
    /**
     * Deteksi jenis struk berdasarkan karakteristik gambar
     */
    private fun detectReceiptType(bitmap: Bitmap): ReceiptType {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)
        
        // Konversi ke grayscale
        val grayMat = Mat()
        Imgproc.cvtColor(mat, grayMat, Imgproc.COLOR_BGR2GRAY)
        
        // Hitung histogram
        val hist = Mat()
        val histSize = MatOfInt(256)
        val ranges = MatOfFloat(0f, 256f)
        val channels = MatOfInt(0)
        
        Imgproc.calcHist(
            listOf(grayMat),
            channels,
            Mat(),
            hist,
            histSize,
            ranges
        )
        
        // Visualize histogram
        val histWidth = 256
        val histHeight = 100
        val binWidth = histWidth / 256
        val histImage = Mat.zeros(histHeight, histWidth, CvType.CV_8UC3)
        
        Core.normalize(hist, hist, 0.0, histHeight.toDouble(), Core.NORM_MINMAX)
        
        for (i in 0 until 256) {
            Imgproc.line(
                histImage,
                org.opencv.core.Point(binWidth * i.toDouble(), histHeight.toDouble()),
                org.opencv.core.Point(binWidth * i.toDouble(), histHeight - hist.get(i, 0)[0]),
                Scalar(255.0, 255.0, 255.0),
                1
            )
        }
        
        val histBitmap = Bitmap.createBitmap(
            histImage.cols(),
            histImage.rows(),
            Bitmap.Config.ARGB_8888
        )
        Utils.matToBitmap(histImage, histBitmap)
        visualizer.captureProcessingStep(
            "Grayscale Histogram",
            "Histogram analysis for receipt type detection",
            histBitmap
        )
        
        // Analisis histogram untuk menentukan karakteristik
        var whitePixelCount = 0
        var blackPixelCount = 0
        var midtonePixelCount = 0
        
        for (i in 0 until 256) {
            val pixelCount = hist.get(i, 0)[0]
            when (i) {
                in 0..50 -> blackPixelCount += pixelCount.toInt()
                in 51..200 -> midtonePixelCount += pixelCount.toInt()
                else -> whitePixelCount += pixelCount.toInt()
            }
        }
        
        val totalPixels = blackPixelCount + midtonePixelCount + whitePixelCount
        val blackRatio = blackPixelCount.toFloat() / totalPixels
        val whiteRatio = whitePixelCount.toFloat() / totalPixels
        val contrastRatio = (whiteRatio + blackRatio) / (midtonePixelCount.toFloat() / totalPixels)
        
        // Analisis noise
        val laplacian = Mat()
        Imgproc.Laplacian(grayMat, laplacian, CvType.CV_64F)
        val mean = MatOfDouble()
        val stddev = MatOfDouble()
        Core.meanStdDev(laplacian, mean, stddev)
        val noiseLevel = stddev.get(0, 0)[0]
        
        // Log characteristics for visualization
        Log.d(TAG, "Receipt Analysis - Black: $blackRatio, White: $whiteRatio, Contrast: $contrastRatio, Noise: $noiseLevel")
        
        // Tentukan tipe berdasarkan karakteristik
        return when {
            // Struk thermal biasanya memiliki kontras tinggi, background putih, dan sedikit noise
            contrastRatio > 1.5 && whiteRatio > 0.6 && noiseLevel < 15 -> ReceiptType.THERMAL
            
            // Struk inkjet biasanya memiliki kontras menengah dan noise sedang
            contrastRatio in 1.0..1.5 && noiseLevel in 15.0..30.0 -> ReceiptType.INKJET
            
            // Struk laser biasanya memiliki kontras tinggi dan noise rendah
            contrastRatio > 1.2 && noiseLevel < 20 -> ReceiptType.LASER
            
            // Struk digital (screenshot) biasanya memiliki noise sangat rendah dan kontras konsisten
            noiseLevel < 10 && contrastRatio > 1.8 -> ReceiptType.DIGITAL
            
            // Default ke thermal sebagai jenis paling umum
            else -> ReceiptType.THERMAL
        }
    }
    
    /**
     * Normalisasi ukuran gambar
     */
    private fun normalizeImageSize(bitmap: Bitmap): Bitmap {
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height
        
        // Jika sudah dalam rentang yang optimal
        if (originalWidth in (NORMALIZED_WIDTH / 2..NORMALIZED_WIDTH * 2) &&
            originalHeight in (NORMALIZED_HEIGHT / 2..NORMALIZED_HEIGHT * 2)) {
            return bitmap
        }
        
        val targetRatio = NORMALIZED_WIDTH.toFloat() / NORMALIZED_HEIGHT
        val originalRatio = originalWidth.toFloat() / originalHeight
        
        val (targetWidth, targetHeight) = if (originalRatio > targetRatio) {
            // Gambar lebih lebar
            val newWidth = NORMALIZED_WIDTH
            val newHeight = (newWidth / originalRatio).toInt()
            Pair(newWidth, newHeight)
        } else {
            // Gambar lebih tinggi
            val newHeight = NORMALIZED_HEIGHT
            val newWidth = (newHeight * originalRatio).toInt()
            Pair(newWidth, newHeight)
        }
        
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }
    
    // Remaining methods omitted for brevity - they remain the same as in the original file
    // ...
    
    /**
     * Update progress state
     */
    private fun updateProgress(stage: String, progress: Float) {
        _preprocessingState.value = PreprocessingState.InProgress(stage, progress)
    }
    
    /**
     * Helper extension functions
     */
    private fun Double.pow(exponent: Double): Double = Math.pow(this, exponent)
    private fun Float.pow(exponent: Float): Float = Math.pow(this.toDouble(), exponent.toDouble()).toFloat()
    
    // The rest of the methods remain unchanged
}