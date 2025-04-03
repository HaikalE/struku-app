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
    @ApplicationContext private val context: Context
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
        
        try {
            _preprocessingState.value = PreprocessingState.InProgress("Initializing", 0.0f)
            
            // 1. Normalisasi ukuran gambar
            updateProgress("Normalizing image size", 0.05f)
            val normalizedBitmap = normalizeImageSize(inputBitmap)
            
            // 2. Deteksi dan koreksi perspektif dokumen
            updateProgress("Detecting document boundaries", 0.1f)
            val documentBitmap = if (config.documentDetectionConfig.enableMlKitDocumentScanner) {
                try {
                    detectAndCropDocumentMlKit(normalizedBitmap)
                } catch (e: Exception) {
                    Log.w(TAG, "ML Kit document detection failed: ${e.message}. Falling back to OpenCV")
                    detectAndCorrectPerspective(normalizedBitmap, config.documentDetectionConfig)
                }
            } else {
                detectAndCorrectPerspective(normalizedBitmap, config.documentDetectionConfig)
            }
            
            // 3. Identifikasi jenis struk jika auto-detect
            updateProgress("Analyzing receipt type", 0.2f)
            val effectiveConfig = if (config.receiptType == ReceiptType.AUTO_DETECT) {
                val detectedType = detectReceiptType(documentBitmap)
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
            
            // 5. Jika level preprocessing minimal, kembalikan hasil
            if (effectiveConfig.preprocessingLevel == PreprocessingLevel.BASIC) {
                _preprocessingState.value = PreprocessingState.Success(preprocessedBasic)
                return@withContext preprocessedBasic
            }
            
            // 6. Peningkatan gambar berdasarkan jenis struk
            updateProgress("Enhancing image", 0.4f)
            val enhancedImage = enhanceImage(preprocessedBasic, effectiveConfig)
            
            // 7. Deteksi dan koreksi skew
            updateProgress("Correcting image skew", 0.5f)
            val deskewedImage = if (effectiveConfig.layoutAnalysisConfig.enableSkewCorrection) {
                correctSkew(enhancedImage)
            } else {
                enhancedImage
            }
            
            // 8. Penerapan thresholding adaptif
            updateProgress("Optimizing for text recognition", 0.6f)
            val thresholdedImage = applyAdaptiveThreshold(deskewedImage, effectiveConfig)
            
            // 9. Analisis layout dokumen 
            updateProgress("Analyzing document layout", 0.7f)
            val layoutAnalyzedImage = if (effectiveConfig.preprocessingLevel == PreprocessingLevel.MAXIMUM &&
                                         effectiveConfig.layoutAnalysisConfig.enableStructuralAnalysis) {
                analyzeDocumentLayout(thresholdedImage, effectiveConfig)
            } else {
                thresholdedImage
            }
            
            // 10. Operasi morfologis untuk memperbaiki keterbacaan teks
            updateProgress("Performing final optimizations", 0.8f)
            val finalImage = if (effectiveConfig.imageEnhancementConfig.enableMorphologicalOperations) {
                applyMorphologicalOperations(layoutAnalyzedImage, effectiveConfig)
            } else {
                layoutAnalyzedImage
            }
            
            // 11. Peningkatan ketajaman gambar akhir
            updateProgress("Finalizing", 0.9f)
            val sharpenedImage = if (effectiveConfig.imageEnhancementConfig.enableSharpening) {
                sharpenImage(finalImage, effectiveConfig.imageEnhancementConfig.sharpeningIntensity)
            } else {
                finalImage
            }
            
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
    
    /**
     * Terapkan pre-processing dasar pada gambar
     */
    private fun applyBasicPreprocessing(
        bitmap: Bitmap,
        config: ReceiptPreprocessingConfig
    ): Bitmap {
        val sourceMat = Mat()
        Utils.bitmapToMat(bitmap, sourceMat)
        
        // 1. Konversi ke grayscale dengan weighting yang optimal
        val grayMat = Mat()
        if (config.imageEnhancementConfig.enableGrayscaleConversion) {
            val weights = config.imageEnhancementConfig.grayscaleWeights
            
            // Pisahkan channel BGR
            val channels = ArrayList<Mat>(3)
            Core.split(sourceMat, channels)
            
            // Terapkan weighting manual untuk hasil optimal pada struk
            val weightedMat = Mat(sourceMat.rows(), sourceMat.cols(), CvType.CV_8UC1)
            Core.addWeighted(
                channels[0], weights.third,  // B channel with blue weight
                channels[1], weights.second, // G channel with green weight
                0.0, weightedMat
            )
            Core.addWeighted(
                weightedMat, 1.0,
                channels[2], weights.first,  // R channel with red weight
                0.0, grayMat
            )
        } else {
            // Standard grayscale conversion
            Imgproc.cvtColor(sourceMat, grayMat, Imgproc.COLOR_BGR2GRAY)
        }
        
        // 2. Terapkan noise reduction jika diaktifkan
        val denoisedMat = if (config.imageEnhancementConfig.enableNoiseReduction) {
            when (config.imageEnhancementConfig.noiseReductionMethod) {
                NoiseReductionMethod.GAUSSIAN -> {
                    val blurredMat = Mat()
                    Imgproc.GaussianBlur(grayMat, blurredMat, OpenCVSize(3.0, 3.0), 0.0)
                    blurredMat
                }
                NoiseReductionMethod.BILATERAL -> {
                    val filteredMat = Mat()
                    Imgproc.bilateralFilter(grayMat, filteredMat, 9, 75.0, 75.0)
                    filteredMat
                }
                NoiseReductionMethod.MEDIAN -> {
                    val medianMat = Mat()
                    Imgproc.medianBlur(grayMat, medianMat, 3)
                    medianMat
                }
                NoiseReductionMethod.NON_LOCAL_MEANS -> {
                    val nlmMat = Mat()
                    val templateWindowSize = 7
                    val searchWindowSize = 21
                    val h = 10.0f
                    
                    org.opencv.photo.Photo.fastNlMeansDenoising(
                        grayMat,
                        nlmMat,
                        h,
                        templateWindowSize,
                        searchWindowSize
                    )
                    nlmMat
                }
                NoiseReductionMethod.ADAPTIVE -> {
                    // Kombinasi metode berdasarkan karakteristik gambar
                    val noiseMeasurement = Mat()
                    Imgproc.Laplacian(grayMat, noiseMeasurement, CvType.CV_64F)
                    val mean = MatOfDouble()
                    val stddev = MatOfDouble()
                    Core.meanStdDev(noiseMeasurement, mean, stddev)
                    
                    val noiseLevel = stddev.get(0, 0)[0]
                    
                    when {
                        noiseLevel < 10 -> {
                            // Noise rendah, gunakan bilateral filter
                            val filteredMat = Mat()
                            Imgproc.bilateralFilter(grayMat, filteredMat, 9, 50.0, 50.0)
                            filteredMat
                        }
                        noiseLevel < 30 -> {
                            // Noise sedang, gunakan Gaussian blur
                            val blurredMat = Mat()
                            Imgproc.GaussianBlur(grayMat, blurredMat, OpenCVSize(3.0, 3.0), 0.0)
                            blurredMat
                        }
                        else -> {
                            // Noise tinggi, gunakan median filter
                            val medianMat = Mat()
                            Imgproc.medianBlur(grayMat, medianMat, 5)
                            medianMat
                        }
                    }
                }
            }
        } else {
            grayMat
        }
        
        // 3. Peningkatan kontras
        val contrastEnhancedMat = if (config.imageEnhancementConfig.enableContrastEnhancement) {
            when (config.imageEnhancementConfig.contrastMethod) {
                ContrastMethod.HISTOGRAM_EQUALIZATION -> {
                    val eqMat = Mat()
                    Imgproc.equalizeHist(denoisedMat, eqMat)
                    eqMat
                }
                ContrastMethod.CLAHE -> {
                    val claheMat = Mat()
                    val clahe = Imgproc.createCLAHE(2.0, OpenCVSize(8.0, 8.0))
                    clahe.apply(denoisedMat, claheMat)
                    claheMat
                }
                ContrastMethod.NORMALIZATION -> {
                    val normalizedMat = Mat()
                    Core.normalize(denoisedMat, normalizedMat, 0.0, 255.0, Core.NORM_MINMAX)
                    normalizedMat
                }
            }
        } else {
            denoisedMat
        }
        
        // Konversi kembali ke Bitmap
        val resultBitmap = Bitmap.createBitmap(
            contrastEnhancedMat.cols(),
            contrastEnhancedMat.rows(),
            Bitmap.Config.ARGB_8888
        )
        Utils.matToBitmap(contrastEnhancedMat, resultBitmap)
        return resultBitmap
    }
    
    /**
     * Peningkatan gambar dengan metode yang optimal untuk jenis struk tertentu
     */
    private fun enhanceImage(
        bitmap: Bitmap,
        config: ReceiptPreprocessingConfig
    ): Bitmap {
        when (config.receiptType) {
            ReceiptType.THERMAL -> {
                // Optimasi untuk struk termal
                return enhanceThermalReceipt(bitmap, config)
            }
            ReceiptType.INKJET, ReceiptType.LASER -> {
                // Optimasi untuk struk inkjet/laser (kualitas lebih baik)
                return enhancePrintedReceipt(bitmap, config)
            }
            ReceiptType.DIGITAL -> {
                // Optimasi untuk screenshot atau struk digital
                return enhanceDigitalReceipt(bitmap, config)
            }
            else -> {
                // Optimasi default (fallback)
                return enhanceThermalReceipt(bitmap, config)
            }
        }
    }
    
    /**
     * Optimasi khusus untuk struk termal
     */
    private fun enhanceThermalReceipt(
        bitmap: Bitmap,
        config: ReceiptPreprocessingConfig
    ): Bitmap {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)
        
        // 1. Normalisasi kontras untuk meningkatkan keterbacaan teks yang pudar
        val normalizedMat = Mat()
        Core.normalize(mat, normalizedMat, 0.0, 255.0, Core.NORM_MINMAX)
        
        // 2. Deteksi dan perbaiki area dengan kontras rendah
        val adaptiveMean = Mat()
        Imgproc.adaptiveThreshold(
            normalizedMat,
            adaptiveMean,
            255.0,
            Imgproc.ADAPTIVE_THRESH_MEAN_C,
            Imgproc.THRESH_BINARY,
            11,
            2.0
        )
        
        // 3. Edge enhancement untuk memperbaiki ketajaman teks
        val edges = Mat()
        Imgproc.Laplacian(normalizedMat, edges, CvType.CV_8U, 3, 1.0, 0.0)
        
        // 4. Kombinasikan hasil dengan gambar asli
        val result = Mat()
        Core.addWeighted(normalizedMat, 0.7, edges, 0.3, 0.0, result)
        
        // Konversi kembali ke Bitmap
        val resultBitmap = Bitmap.createBitmap(
            result.cols(),
            result.rows(),
            Bitmap.Config.ARGB_8888
        )
        Utils.matToBitmap(result, resultBitmap)
        return resultBitmap
    }
    
    /**
     * Optimasi khusus untuk struk yang dicetak (inkjet/laser)
     */
    private fun enhancePrintedReceipt(
        bitmap: Bitmap,
        config: ReceiptPreprocessingConfig
    ): Bitmap {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)
        
        // 1. Bilateral filter untuk mengurangi noise dengan mempertahankan edge
        val filteredMat = Mat()
        Imgproc.bilateralFilter(mat, filteredMat, 9, 75.0, 75.0)
        
        // 2. Unsharp masking untuk meningkatkan ketajaman
        val blurredMat = Mat()
        Imgproc.GaussianBlur(filteredMat, blurredMat, OpenCVSize(0.0, 0.0), 3.0)
        
        val sharpMat = Mat()
        Core.addWeighted(filteredMat, 1.5, blurredMat, -0.5, 0.0, sharpMat)
        
        // 3. Peningkatan kontras lokal dengan CLAHE
        val claheMat = Mat()
        val clahe = Imgproc.createCLAHE(2.5, OpenCVSize(8.0, 8.0))
        clahe.apply(sharpMat, claheMat)
        
        // Konversi kembali ke Bitmap
        val resultBitmap = Bitmap.createBitmap(
            claheMat.cols(),
            claheMat.rows(),
            Bitmap.Config.ARGB_8888
        )
        Utils.matToBitmap(claheMat, resultBitmap)
        return resultBitmap
    }
    
    /**
     * Optimasi khusus untuk struk digital
     */
    private fun enhanceDigitalReceipt(
        bitmap: Bitmap,
        config: ReceiptPreprocessingConfig
    ): Bitmap {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)
        
        // 1. Peningkatan ketajaman untuk teks
        val sharpKernel = Mat(3, 3, CvType.CV_32F)
        val kernelData = floatArrayOf(
            0f, -1f, 0f,
            -1f, 5f, -1f,
            0f, -1f, 0f
        )
        sharpKernel.put(0, 0, *kernelData)
        
        val sharpMat = Mat()
        Imgproc.filter2D(mat, sharpMat, -1, sharpKernel)
        
        // 2. Peningkatan kontras ringan
        val contrastedMat = Mat()
        Core.normalize(sharpMat, contrastedMat, 0.0, 255.0, Core.NORM_MINMAX)
        
        // Konversi kembali ke Bitmap
        val resultBitmap = Bitmap.createBitmap(
            contrastedMat.cols(),
            contrastedMat.rows(),
            Bitmap.Config.ARGB_8888
        )
        Utils.matToBitmap(contrastedMat, resultBitmap)
        return resultBitmap
    }
    
    /**
     * Deteksi dan koreksi kemiringan (skew) pada gambar
     */
    private fun correctSkew(bitmap: Bitmap): Bitmap {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)
        
        // 1. Konversi ke grayscale jika belum
        val grayMat = if (mat.channels() > 1) {
            val gray = Mat()
            Imgproc.cvtColor(mat, gray, Imgproc.COLOR_BGR2GRAY)
            gray
        } else {
            mat.clone()
        }
        
        // 2. Thresholding untuk mendapatkan biner image
        val thresholdMat = Mat()
        Imgproc.threshold(grayMat, thresholdMat, 0.0, 255.0, Imgproc.THRESH_BINARY_INV + Imgproc.THRESH_OTSU)
        
        // 3. Deteksi garis dengan Hough Transform
        val lines = Mat()
        Imgproc.HoughLinesP(
            thresholdMat,
            lines,
            1.0,
            Math.PI / 180,
            100,
            mat.cols() / 3.0, // Minimal line length
            20.0 // Maksimal gap antar segmen
        )
        
        // 4. Hitung sudut kemiringan rata-rata
        var totalAngle = 0.0
        var lineCount = 0
        
        for (i in 0 until lines.rows()) {
            val line = lines.get(i, 0)
            val x1 = line[0]
            val y1 = line[1]
            val x2 = line[2]
            val y2 = line[3]
            
            // Hitung sudut (dalam radian)
            val angle = atan2(y2 - y1, x2 - x1)
            
            // Konversi ke derajat dan normalisasi
            val angleDeg = Math.toDegrees(angle)
            
            // Filter hanya untuk garis yang hampir horizontal
            // (biasanya baris teks pada struk)
            if (abs(angleDeg) < 45) {
                totalAngle += angleDeg
                lineCount++
            }
        }
        
        // Jika tidak menemukan garis yang sesuai
        if (lineCount == 0) {
            val resultBitmap = Bitmap.createBitmap(
                mat.cols(),
                mat.rows(),
                Bitmap.Config.ARGB_8888
            )
            Utils.matToBitmap(mat, resultBitmap)
            return resultBitmap
        }
        
        // Hitung sudut rata-rata
        val avgAngle = totalAngle / lineCount
        
        // 5. Rotasi gambar untuk mengoreksi kemiringan
        val center = org.opencv.core.Point(mat.cols() / 2.0, mat.rows() / 2.0)
        val rotMat = Imgproc.getRotationMatrix2D(center, avgAngle, 1.0)
        val rotatedMat = Mat()
        Imgproc.warpAffine(
            mat,
            rotatedMat,
            rotMat,
            mat.size(),
            Imgproc.INTER_CUBIC,
            Core.BORDER_CONSTANT,
            Scalar(255.0, 255.0, 255.0)
        )
        
        // Konversi kembali ke Bitmap
        val resultBitmap = Bitmap.createBitmap(
            rotatedMat.cols(),
            rotatedMat.rows(),
            Bitmap.Config.ARGB_8888
        )
        Utils.matToBitmap(rotatedMat, resultBitmap)
        return resultBitmap
    }
    
    /**
     * Terapkan thresholding adaptif untuk memisahkan teks dari background
     */
    private fun applyAdaptiveThreshold(
        bitmap: Bitmap,
        config: ReceiptPreprocessingConfig
    ): Bitmap {
        if (!config.imageEnhancementConfig.enableThresholding) {
            return bitmap
        }
        
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)
        
        // Konversi ke grayscale jika belum
        val grayMat = if (mat.channels() > 1) {
            val gray = Mat()
            Imgproc.cvtColor(mat, gray, Imgproc.COLOR_BGR2GRAY)
            gray
        } else {
            mat.clone()
        }
        
        val resultMat = Mat()
        
        when (config.imageEnhancementConfig.thresholdingMethod) {
            ThresholdingMethod.GLOBAL_OTSU -> {
                Imgproc.threshold(
                    grayMat,
                    resultMat,
                    0.0,
                    255.0,
                    Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU
                )
            }
            ThresholdingMethod.ADAPTIVE_GAUSSIAN -> {
                Imgproc.adaptiveThreshold(
                    grayMat,
                    resultMat,
                    255.0,
                    Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                    Imgproc.THRESH_BINARY,
                    11,
                    2.0
                )
            }
            ThresholdingMethod.ADAPTIVE_MEAN -> {
                Imgproc.adaptiveThreshold(
                    grayMat,
                    resultMat,
                    255.0,
                    Imgproc.ADAPTIVE_THRESH_MEAN_C,
                    Imgproc.THRESH_BINARY,
                    11,
                    2.0
                )
            }
            ThresholdingMethod.ADAPTIVE_SAUVOLA -> {
                // Implementasi Sauvola thresholding
                val integralImg = Mat()
                val integralSqImg = Mat()
                Imgproc.integral2(grayMat, integralImg, integralSqImg)
                
                resultMat.create(grayMat.size(), CvType.CV_8U)
                
                val k = 0.2 // Sauvola parameter
                val winSize = 15
                val winHalfSize = winSize / 2
                val winArea = winSize * winSize
                
                for (i in winHalfSize until grayMat.rows() - winHalfSize) {
                    for (j in winHalfSize until grayMat.cols() - winHalfSize) {
                        // Hitung statistik lokal
                        val x1 = j - winHalfSize
                        val y1 = i - winHalfSize
                        val x2 = j + winHalfSize
                        val y2 = i + winHalfSize
                        
                        val sum = integralImg.get(y2, x2)[0] -
                                  integralImg.get(y1, x2)[0] -
                                  integralImg.get(y2, x1)[0] +
                                  integralImg.get(y1, x1)[0]
                        
                        val sqSum = integralSqImg.get(y2, x2)[0] -
                                   integralSqImg.get(y1, x2)[0] -
                                   integralSqImg.get(y2, x1)[0] +
                                   integralSqImg.get(y1, x1)[0]
                        
                        val mean = sum / winArea
                        val stdDev = sqrt(sqSum / winArea - mean * mean)
                        
                        // Sauvola threshold formula: t = mean * (1 + k * ((stdDev / 128) - 1))
                        val threshold = mean * (1.0 + k * ((stdDev / 128.0) - 1.0))
                        
                        val pixelValue = grayMat.get(i, j)[0]
                        if (pixelValue > threshold) {
                            resultMat.put(i, j, 255.0)
                        } else {
                            resultMat.put(i, j, 0.0)
                        }
                    }
                }
            }
            ThresholdingMethod.ADAPTIVE_NIBLACK -> {
                // Implementasi Niblack thresholding
                val integralImg = Mat()
                val integralSqImg = Mat()
                Imgproc.integral2(grayMat, integralImg, integralSqImg)
                
                resultMat.create(grayMat.size(), CvType.CV_8U)
                
                val k = -0.2 // Niblack parameter
                val winSize = 15
                val winHalfSize = winSize / 2
                val winArea = winSize * winSize
                
                for (i in winHalfSize until grayMat.rows() - winHalfSize) {
                    for (j in winHalfSize until grayMat.cols() - winHalfSize) {
                        // Hitung statistik lokal
                        val x1 = j - winHalfSize
                        val y1 = i - winHalfSize
                        val x2 = j + winHalfSize
                        val y2 = i + winHalfSize
                        
                        val sum = integralImg.get(y2, x2)[0] -
                                  integralImg.get(y1, x2)[0] -
                                  integralImg.get(y2, x1)[0] +
                                  integralImg.get(y1, x1)[0]
                        
                        val sqSum = integralSqImg.get(y2, x2)[0] -
                                   integralSqImg.get(y1, x2)[0] -
                                   integralSqImg.get(y2, x1)[0] +
                                   integralSqImg.get(y1, x1)[0]
                        
                        val mean = sum / winArea
                        val stdDev = sqrt(sqSum / winArea - mean * mean)
                        
                        // Niblack threshold formula: t = mean + k * stdDev
                        val threshold = mean + k * stdDev
                        
                        val pixelValue = grayMat.get(i, j)[0]
                        if (pixelValue > threshold) {
                            resultMat.put(i, j, 255.0)
                        } else {
                            resultMat.put(i, j, 0.0)
                        }
                    }
                }
            }
            ThresholdingMethod.MULTI_LEVEL -> {
                // Multi-level thresholding - kombinasi beberapa metode
                val otsuMat = Mat()
                val gaussianMat = Mat()
                val meanMat = Mat()
                
                // Otsu global thresholding
                Imgproc.threshold(
                    grayMat,
                    otsuMat,
                    0.0,
                    255.0,
                    Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU
                )
                
                // Adaptive Gaussian thresholding
                Imgproc.adaptiveThreshold(
                    grayMat,
                    gaussianMat,
                    255.0,
                    Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                    Imgproc.THRESH_BINARY,
                    11,
                    2.0
                )
                
                // Adaptive Mean thresholding
                Imgproc.adaptiveThreshold(
                    grayMat,
                    meanMat,
                    255.0,
                    Imgproc.ADAPTIVE_THRESH_MEAN_C,
                    Imgproc.THRESH_BINARY,
                    11,
                    2.0
                )
                
                // Gabungkan hasil dengan kombinasi weighted
                Core.addWeighted(otsuMat, 0.4, gaussianMat, 0.4, 0.0, resultMat)
                Core.addWeighted(resultMat, 1.0, meanMat, 0.2, 0.0, resultMat)
            }
        }
        
        // Konversi kembali ke Bitmap
        val resultBitmap = Bitmap.createBitmap(
            resultMat.cols(),
            resultMat.rows(),
            Bitmap.Config.ARGB_8888
        )
        Utils.matToBitmap(resultMat, resultBitmap)
        return resultBitmap
    }
    
    /**
     * Analisis tata letak dokumen untuk mengidentifikasi struktur
     */
    private fun analyzeDocumentLayout(
        bitmap: Bitmap,
        config: ReceiptPreprocessingConfig
    ): Bitmap {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)
        
        // Analisis hanya dilakukan jika layout analysis aktif
        if (!config.layoutAnalysisConfig.enableStructuralAnalysis) {
            return bitmap
        }
        
        // 1. Segmentasi baris jika diaktifkan
        if (config.layoutAnalysisConfig.enableLineSegmentation) {
            // Implementasi sederhana untuk visualisasi baris
            // Dalam implementasi sebenarnya, ini akan digunakan untuk mengekstrak informasi struktur
            val horizontalProjection = Mat(mat.rows(), 1, CvType.CV_32F)
            
            for (i in 0 until mat.rows()) {
                var rowSum = 0.0
                for (j in 0 until mat.cols()) {
                    rowSum += mat.get(i, j)[0]
                }
                horizontalProjection.put(i, 0, rowSum)
            }
            
            // Normalisasi proyeksi
            Core.normalize(horizontalProjection, horizontalProjection, 0.0, 255.0, Core.NORM_MINMAX)
            
            // Threshold untuk menemukan baris teks
            val thresholdValue = 50.0
            val linePositions = mutableListOf<Int>()
            var inLine = false
            
            for (i in 0 until horizontalProjection.rows()) {
                val value = horizontalProjection.get(i, 0)[0]
                
                if (!inLine && value < thresholdValue) {
                    linePositions.add(i)
                    inLine = true
                } else if (inLine && value >= thresholdValue) {
                    inLine = false
                }
            }
            
            // Draw lines on visualization (optional)
            val visMatrix = mat.clone()
            for (linePos in linePositions) {
                Imgproc.line(
                    visMatrix,
                    org.opencv.core.Point(0.0, linePos.toDouble()),
                    org.opencv.core.Point(visMatrix.cols().toDouble(), linePos.toDouble()),
                    Scalar(200.0, 0.0, 0.0),
                    1
                )
            }
            
            // Convert back to bitmap
            val resultBitmap = Bitmap.createBitmap(
                visMatrix.cols(),
                visMatrix.rows(),
                Bitmap.Config.ARGB_8888
            )
            Utils.matToBitmap(visMatrix, resultBitmap)
            return resultBitmap
        }
        
        // Return original if no layout analysis was performed
        val resultBitmap = Bitmap.createBitmap(
            mat.cols(),
            mat.rows(),
            Bitmap.Config.ARGB_8888
        )
        Utils.matToBitmap(mat, resultBitmap)
        return resultBitmap
    }
    
    /**
     * Terapkan operasi morfologis untuk memperbaiki keterbacaan teks
     */
    private fun applyMorphologicalOperations(
        bitmap: Bitmap,
        config: ReceiptPreprocessingConfig
    ): Bitmap {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)
        
        val processedMat = Mat()
        
        // Pilih operasi morfologis berdasarkan jenis struk
        when (config.receiptType) {
            ReceiptType.THERMAL -> {
                // Struk termal: teks sering tipis dan putus-putus
                // Gunakan dilasi ringan untuk menghubungkan karakter
                val kernel = Imgproc.getStructuringElement(
                    Imgproc.MORPH_RECT,
                    OpenCVSize(2.0, 1.0)
                )
                Imgproc.dilate(mat, processedMat, kernel)
            }
            ReceiptType.INKJET -> {
                // Struk inkjet: teks bisa memblobbing
                // Gunakan opening untuk membersihkan blobs kecil
                val kernel = Imgproc.getStructuringElement(
                    Imgproc.MORPH_ELLIPSE,
                    OpenCVSize(2.0, 2.0)
                )
                Imgproc.morphologyEx(mat, processedMat, Imgproc.MORPH_OPEN, kernel)
            }
            ReceiptType.LASER -> {
                // Struk laser: teks biasanya sudah jelas
                // Gunakan closing untuk mengisi gap kecil
                val kernel = Imgproc.getStructuringElement(
                    Imgproc.MORPH_RECT,
                    OpenCVSize(1.0, 1.0)
                )
                Imgproc.morphologyEx(mat, processedMat, Imgproc.MORPH_CLOSE, kernel)
            }
            ReceiptType.DIGITAL -> {
                // Digital receipts biasanya tidak memerlukan operasi morfologis
                mat.copyTo(processedMat)
            }
            else -> {
                // Default untuk ReceiptType.AUTO_DETECT
                val kernel = Imgproc.getStructuringElement(
                    Imgproc.MORPH_RECT,
                    OpenCVSize(2.0, 1.0)
                )
                Imgproc.morphologyEx(mat, processedMat, Imgproc.MORPH_CLOSE, kernel)
            }
        }
        
        // Konversi kembali ke Bitmap
        val resultBitmap = Bitmap.createBitmap(
            processedMat.cols(),
            processedMat.rows(),
            Bitmap.Config.ARGB_8888
        )
        Utils.matToBitmap(processedMat, resultBitmap)
        return resultBitmap
    }
    
    /**
     * Perjarkan gambar menggunakan kernel sharpening
     */
    private fun sharpenImage(bitmap: Bitmap, intensity: Float): Bitmap {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)
        
        // Buat kernel sharpening dengan intensitas yang dapat disesuaikan
        val normalizedIntensity = intensity.coerceIn(0.0f, 1.0f)
        val centralValue = 1.0f + 4.0f * normalizedIntensity
        val surroundingValue = -1.0f * normalizedIntensity
        
        val sharpKernel = Mat(3, 3, CvType.CV_32F)
        val kernelData = floatArrayOf(
            0f, surroundingValue, 0f,
            surroundingValue, centralValue, surroundingValue,
            0f, surroundingValue, 0f
        )
        sharpKernel.put(0, 0, *kernelData)
        
        // Terapkan filter
        val sharpenedMat = Mat()
        Imgproc.filter2D(mat, sharpenedMat, -1, sharpKernel)
        
        // Konversi kembali ke Bitmap
        val resultBitmap = Bitmap.createBitmap(
            sharpenedMat.cols(),
            sharpenedMat.rows(),
            Bitmap.Config.ARGB_8888
        )
        Utils.matToBitmap(sharpenedMat, resultBitmap)
        return resultBitmap
    }
    
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
}
