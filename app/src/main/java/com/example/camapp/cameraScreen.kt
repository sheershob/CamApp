package com.example.camapp

import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
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
import androidx.camera.video.OutputOptions
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.contentValuesOf
import kotlin.contracts.contract
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

//@androidx.compose.ui.tooling.preview.Preview(showBackground = true, showSystemUi = true)
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

    if(permissionGranted.value){
        cameraScreen()
        } else {
        Column (
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center

        ) {
            Button(
                onClick = {
                    launcher.launch(permission.toTypedArray())
                }

            ) {
                Text(text = "Grant Permission")
            }
        }
    }
}

@Composable
fun cameraScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView: PreviewView = remember { PreviewView(context) }

    // Camera state variable
    var isBackCamera by remember { mutableStateOf(true) }

    // Ensure imageCapture is properly recreated when camera toggles
    var imageCapture by remember { mutableStateOf(ImageCapture.Builder().build()) }

    val cameraSelector = if (isBackCamera)
        CameraSelector.DEFAULT_BACK_CAMERA
    else
        CameraSelector.DEFAULT_FRONT_CAMERA

    val preview = remember { Preview.Builder().build() }

    LaunchedEffect(isBackCamera) {
        val cameraProvider = context.getCameraProvider()
        cameraProvider.unbindAll()

        // Recreate imageCapture to prevent old references
        imageCapture = ImageCapture.Builder().build()

        try{
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )
            preview.setSurfaceProvider(previewView.surfaceProvider)
        }
        catch (e: Exception){
            Log.e("CameraPreview", "Error: ${e.message}")
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        // Bottom Bar with Buttons
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Toggle Camera Button
                IconButton(
                    onClick = { isBackCamera = !isBackCamera },
                    modifier = Modifier.size(60.dp)
                        .background(Color.White, CircleShape)
                        .padding(8.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.swap_camera),
                        contentDescription = "Switch Camera"
                    )
                }

                // Capture Button
                IconButton(
                    onClick = { capturePhoto(imageCapture, context) },
                    modifier = Modifier
                        .size(60.dp)
                        .background(Color.White, CircleShape)
                        .padding(8.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.lens),
                        contentDescription = "Capture"
                    )
                }
            }
        }
    }
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

private fun capturePhoto(imageCapture: ImageCapture, context: Context){
    val name = "Img_${System.currentTimeMillis()}.jpg"

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