package com.example.camapp

import androidx.compose.material3.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.PreviewView
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Composable
fun permission() {
    val permission = listOf(
        android.Manifest.permission.CAMERA,
        android.Manifest.permission.RECORD_AUDIO

    )
    val permissionGranted = remember {
        mutableStateOf(false)
    }

    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permission ->
            permissionGranted.value = permission[android.Manifest.permission.CAMERA] == true
        }
    )

    if (permissionGranted.value) {
        cameraScreen()
    } else {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Welcome to CamApp!",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Button(
                onClick = {
                    launcher.launch(permission.toTypedArray())
                }
            ) {
                Text("Grant Permissions")
            }
        }
    }
}

@Composable
fun cameraScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView: PreviewView = remember { PreviewView(context) }
    var isBackCamera by remember { mutableStateOf(true) }
    var isRecording by remember { mutableStateOf(false) }
    val cameraSelector = remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }
    val imageCapture = remember { ImageCapture.Builder().build() }

    val scannedText = remember { mutableStateOf<String?>(null) }
    val isScanning = remember { mutableStateOf(false) }

    val recorder = Recorder.Builder()
        .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
        .build()

    val videoCapture = VideoCapture.withOutput(recorder)
    val preview = remember { Preview.Builder().build()
        .also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
    }
    var TorchEnabled by remember { mutableStateOf(false) }

    LaunchedEffect(cameraSelector.value) {
        val cameraProvider = context.getCameraProvider()
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            lifecycleOwner = lifecycleOwner,
            cameraSelector = cameraSelector.value,
            preview,
            imageCapture,
            videoCapture
        )
        preview.setSurfaceProvider(previewView.surfaceProvider)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        Column( modifier = Modifier.align(Alignment.TopEnd)) {
            IconButton(
                onClick = {
                    isBackCamera = !isBackCamera
                    cameraSelector.value =
                        if (isBackCamera) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA
                },
                modifier = Modifier
                    .size(90.dp)
                    .background(Color.Transparent)
                    .padding(20.dp),
                enabled = !isRecording
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.swap_camera),
                    contentDescription = "Switch Camera",
                    tint = if (isRecording) Color.Black else Color.LightGray
                )
            }

            IconButton(
                onClick = { TorchEnabled = !TorchEnabled },
                modifier = Modifier.size(80.dp)
                    .background(Color.Transparent)
                    .padding(20.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.flash),
                    contentDescription = "Flash",
                    tint = if (TorchEnabled) Color.Yellow else Color.LightGray
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(
                    onClick = {
                        isRecording = if (!isRecording) {
                            startRecording(videoCapture, context, TorchEnabled)
                            true
                        } else {
                            stopRecording()
                            false
                        }
                    },
                    modifier = Modifier
                        .size(60.dp)
                        .background(Color.White, CircleShape)
                        .padding(8.dp)
                ) {
                    Icon(
                        painter = painterResource(id = if (isRecording) R.drawable.stop_icon else R.drawable.video),
                        contentDescription = "Record Video",
                        tint = Color.Unspecified
                    )
                }

                IconButton(
                    onClick = {
                        capturePhoto(imageCapture, context, TorchEnabled)
                    },
                    modifier = Modifier
                        .size(60.dp)
                        .background(Color.White, CircleShape)
                        .padding(8.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.lens),
                        contentDescription = "Capture Image",
                        tint = Color.Unspecified
                    )
                }

                IconButton(
                    onClick = { isScanning.value = true },
                    modifier = Modifier
                        .size(60.dp)
                        .background(Color.White, CircleShape)
                        .padding(8.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.qr),
                        contentDescription = "QR Scanner",
                        modifier = Modifier.size(40.dp),
                        tint = Color.Unspecified
                    )
                }
                LaunchedEffect(isScanning.value) {
                    if (isScanning.value) {
                        scanQRCode(context, imageCapture) { result ->
                            scannedText.value = result
                            isScanning.value = false
                        }
                    }
                }

                scannedText.value?.let {
                    QRDialog(scannedText)
                }
            }
        }
    }
}

private var recording: Recording? = null

private fun startRecording(videoCapture: VideoCapture<Recorder>, context: Context, TorchEnabled: Boolean) {
    val videoFile = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
        "Albums/CamApp Videos/VID_${System.currentTimeMillis()}.mp4"
    )
    val outputOptions = FileOutputOptions.Builder(videoFile).build()

    recording = videoCapture.output
        .prepareRecording(context, outputOptions)
        .apply {
            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                withAudioEnabled()
            }
        }
        .start(ContextCompat.getMainExecutor(context)) { event ->
            when (event) {
                is VideoRecordEvent.Start -> {
//                    Toast.makeText(context, "Recording started", Toast.LENGTH_SHORT).show()
                }
                is VideoRecordEvent.Finalize -> {
                    if (event.hasError()) {
                        Toast.makeText(context, "Error: ${event.error}", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Video saved: ${videoFile.absolutePath}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
}

private fun stopRecording() {
    recording?.stop()
    recording = null
}



// Async function will wait for camera provider and then resume
private suspend fun Context.getCameraProvider() : ProcessCameraProvider = suspendCoroutine {
        continuation ->
    val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
    cameraProviderFuture.addListener({
        continuation.resume(cameraProviderFuture.get())
    }, ContextCompat.getMainExecutor(this)
    )
}

private fun capturePhoto(imageCapture: ImageCapture, context: Context, torchEnabled: Boolean){
    val name = "Img_${System.currentTimeMillis()}.jpg"

    if (torchEnabled) {
        imageCapture.flashMode = ImageCapture.FLASH_MODE_ON
    } else {
        imageCapture.flashMode = ImageCapture.FLASH_MODE_OFF
    }
// Meta data of the images
    val contentValues = ContentValues().apply{
        put(MediaStore.MediaColumns.DISPLAY_NAME, name)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/CamApp Photos")    // Will create Albums folder if it doesn't exist
    }

    // Storing the output image in our device
    val outputOption = ImageCapture.OutputFileOptions.Builder(
        context.contentResolver,
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        contentValues
    ).build()

    imageCapture.takePicture(
        outputOption,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                // Play shutter sound effect
                val imageCaptureSound = MediaPlayer.create(context, R.raw.click_sound)
                imageCaptureSound.setOnCompletionListener { it.release() } // Release media player when done
                imageCaptureSound.start()

            }

            override fun onError(exception: ImageCaptureException) {
                Log.e("CameraApp", "Capture failed : ${exception.message}", exception)
                Toast.makeText(context, "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        }
    )

}

fun scanQRCode(context: Context, imageCapture: ImageCapture, onResult: (String) -> Unit) {
    val outputOptions = ImageCapture.OutputFileOptions.Builder(
        context.contentResolver,
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        ContentValues().apply {
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        }
    ).build()

    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                val uri = outputFileResults.savedUri ?: return
                processQRCode(context, uri, onResult)
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e("QRScanner", "Error capturing QR image: ${exception.message}")
            }
        }
    )
}

fun processQRCode(context: Context, uri: Uri, onResult: (String) -> Unit) {
    val image = InputImage.fromFilePath(context, uri)
    val scanner = BarcodeScanning.getClient()

    scanner.process(image)
        .addOnSuccessListener { barcodes ->
            for (barcode in barcodes) {
                barcode.displayValue?.let { onResult(it) }
            }
        }
        .addOnFailureListener { e ->
            Log.e("QRScanner", "Failed to scan QR code", e)
        }
}

fun isValidUrl(text: String?): Boolean {
    return text?.startsWith("http") == true
}

@Composable
fun QRDialog(scannedText: MutableState<String?>) {
    val context = LocalContext.current
    val text = scannedText.value ?: "No data"
    val isLink = isValidUrl(text)

    AlertDialog(
        onDismissRequest = { scannedText.value = null },
        title = { Text(if (isLink) "QR Code Scanned" else "Barcode Scanned") },
        text = { Text(text) },
        confirmButton = {
            if (isLink) {  // Show "Open Link" button only for links
                Button(onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(text))
                    context.startActivity(intent)
                    scannedText.value = null
                }) {
                    Text("Open Link")
                }
            }
        },
        dismissButton = {
            Button(onClick = { scannedText.value = null }) {
                Text("Close")
            }
        },
        modifier = Modifier.background(Color.DarkGray), // Theming the dialog background
    )
}

@Composable
fun CameraPreview(cameraSelector: CameraSelector, torchEnabled: Boolean) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner, cameraSelector, preview
                )

                // Enable/Disable Flash
                camera.cameraControl.enableTorch(torchEnabled)

            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

//@Composable
//fun CameraPreviewWithNightVision(
//    previewView: PreviewView,
//    isNightVision: Boolean
//) {
//    AndroidView(
//        factory = { previewView },
//        modifier = Modifier.fillMaxSize()
//    ) {
//        if (isNightVision) {
//            it.setColorFilter(
//                ColorMatrixColorFilter(
//                    ColorMatrix(
//                        floatArrayOf(
//                            0f, 1f, 0f, 0f, 0f,  // Red to Green
//                            0f, 1f, 0f, 0f, 0f,  // Keep Green
//                            0f, 1f, 0f, 0f, 0f,  // Blue to Green
//                            0f, 0f, 0f, 1f, 0f   // Alpha
//                        )
//                    )
//                )
//            )
//        } else {
//            it.clearColorFilter()
//        }
//    }
//}
