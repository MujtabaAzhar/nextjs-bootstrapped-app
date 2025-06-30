package com.example.numberplatedetector

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator

class MainActivity : AppCompatActivity() {

    private lateinit var scanButton: MaterialButton
    private lateinit var imagePreview: ImageView
    private lateinit var progressIndicator: CircularProgressIndicator
    private var capturedImage: Bitmap? = null

    private val CAMERA_PERMISSION_CODE = 100

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageBitmap = result.data?.extras?.get("data") as? Bitmap
            imageBitmap?.let {
                capturedImage = it
                imagePreview.setImageBitmap(it)
                processPlateImage(it)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views
        scanButton = findViewById(R.id.scanButton)
        imagePreview = findViewById(R.id.imagePreview)
        progressIndicator = findViewById(R.id.progressIndicator)

        // Set click listener for scan button
        scanButton.setOnClickListener {
            if (checkCameraPermission()) {
                openCamera()
            } else {
                requestCameraPermission()
            }
        }
    }

    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_CODE
        )
    }

    private fun openCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraLauncher.launch(cameraIntent)
    }

    private fun processPlateImage(bitmap: Bitmap) {
        showLoading(true)
        
        // Use NumberPlateDetector to process the image
        NumberPlateDetector.detectPlate(bitmap) { plateText, error ->
            runOnUiThread {
                if (error != null) {
                    showError(error)
                    showLoading(false)
                } else if (plateText != null) {
                    // Send plate number to server
                    sendPlateToServer(plateText)
                } else {
                    showError(getString(R.string.error_no_plate))
                    showLoading(false)
                }
            }
        }
    }

    private fun sendPlateToServer(plateNumber: String) {
        ApiService.postPlate(
            plateNumber,
            onSuccess = { responseCode ->
                runOnUiThread {
                    showLoading(false)
                    when (responseCode) {
                        200 -> showSuccess(getString(R.string.success_message))
                        404 -> showError(getString(R.string.error_not_found))
                        else -> showError(getString(R.string.error_server))
                    }
                }
            },
            onFailure = { error ->
                runOnUiThread {
                    showLoading(false)
                    showError(getString(R.string.error_network))
                }
            }
        )
    }

    private fun showLoading(show: Boolean) {
        progressIndicator.visibility = if (show) View.VISIBLE else View.GONE
        scanButton.isEnabled = !show
    }

    private fun showSuccess(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CAMERA_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openCamera()
                } else {
                    showError(getString(R.string.error_camera_permission))
                }
            }
        }
    }
}
