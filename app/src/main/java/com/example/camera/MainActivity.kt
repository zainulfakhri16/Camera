package com.example.camera

import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.system.Os.shutdown
import android.util.Log
import android.widget.Toast
import androidx.camera.camera2.internal.annotation.CameraExecutor
import androidx.camera.core.CameraX.shutdown
import androidx.camera.core.ImageCapture
import androidx.camera.core.*
import androidx.core.content.ContextCompat
import androidx.core.app.ActivityCompat
import androidx.camera.lifecycle.ProcessCameraProvider
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
typealias LumaListener = (luma:Double)->Unit

class MainActivity : AppCompatActivity() {
    private var imageCapture: ImageCapture?=null
    private lateinit var outputDirectory:File
    private lateinit var cameraExecutor:ExecutorService
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (allPermissionGranted()){
            startCamera()
        }else{
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)

        }
        bt_camera.setOnClickListener{takePhoto()}
        outputDirectory = getOutputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun takePhoto(){
        val imageCapture=imageCapture?:return
        val photoFile=File(
            outputDirectory,
            SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis())+".jpg"
        )
        val outputOptions=ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,ContextCompat.getMainExecutor(this),
            object :ImageCapture.OnImageSavedCallback{
                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG,"Photo capture failed:${exception.message}",exception)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri=Uri.fromFile(photoFile)
                    val msg = "photo capture succeded: $savedUri"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG,msg)
                }
            }
        )
    }

    private fun startCamera(){
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(Runnable {
            val cameraProvider:ProcessCameraProvider=cameraProviderFuture.get()

            val preview=Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewfinder.surfaceProvider)
                }
            imageCapture=ImageCapture.Builder()
                .build()
            val cameraSelector=CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,cameraSelector,preview,imageCapture
                )
            }catch (exc:Exception){
                Log.e(TAG,"Use case binding failed",exc)
            }
        },ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionGranted()= REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext,it)==PackageManager.PERMISSION_GRANTED
    }
    private fun getOutputDirectory():File{
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it,resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir!=null && mediaDir.exists())
            mediaDir else filesDir
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
    companion object{
        private const val TAG = "CameraXbasic"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(android.Manifest.permission.CAMERA)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode== REQUEST_CODE_PERMISSIONS){
            if (allPermissionGranted()){
                startCamera()
            }else{
                Toast.makeText(this, "Permissions not granted by the user", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}