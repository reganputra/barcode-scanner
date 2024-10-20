package com.dicoding.picodiploma.mycamera

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.CameraController.COORDINATE_SYSTEM_VIEW_REFERENCED
import androidx.camera.view.LifecycleCameraController
import androidx.core.content.ContextCompat
import com.dicoding.picodiploma.mycamera.databinding.ActivityCameraBinding
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode

class CameraActivity : AppCompatActivity() {
    private lateinit var barcodeScanner: BarcodeScanner
    private lateinit var binding: ActivityCameraBinding
    private var cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

    }

    public override fun onResume() {
        super.onResume()
        hideSystemUI()
        startCamera()
    }

    private var firstCall = true
    private fun startCamera() {
        // mendefinisikan format barcode
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        barcodeScanner = BarcodeScanning.getClient(options)

        // siapkan MLKitAnalyzer dari library ML Kit Vision
        val analyzer = MlKitAnalyzer(
            listOf(barcodeScanner), // detector
            COORDINATE_SYSTEM_VIEW_REFERENCED, // mengambil koordinat dari hasil pemindaian gambar dan mengonversinya ke koordinat PreviewView secara otomatis
            ContextCompat.getMainExecutor(this)// Executor, menampilkan hasil analisis di UI secara langsung
        ) { result: MlKitAnalyzer.Result? ->
            showResult(result)
        }

        // digunakan untuk menghubungkan MLKitAnalyzer yang dibuat sebelumnya dengan CameraX
        val cameraController = LifecycleCameraController(baseContext)
        cameraController.setImageAnalysisAnalyzer(
            ContextCompat.getMainExecutor(this),
            analyzer
        )
        cameraController.bindToLifecycle(this)
        binding.viewFinder.controller = cameraController

    }

    // fungsi yang berguna untuk menampilkan hasil deteksi pada AlertDialog
    private fun showResult(result: MlKitAnalyzer.Result?) {
        if (firstCall) {
            val barcodeResults = result?.getValue(barcodeScanner)
            if ((barcodeResults != null) &&
                (barcodeResults.size != 0) &&
                (barcodeResults.first() != null)
            ){
                firstCall = false
                val barcode = barcodeResults[0]
                val alertDialog = AlertDialog.Builder(this)
                    .setMessage(barcode.rawValue)
                    .setPositiveButton(
                        "Buka"
                    ) { _, _ ->
                        firstCall = true
                        when (barcode.valueType) {
                            Barcode.TYPE_URL -> {
                                val openBrowserIntent = Intent(Intent.ACTION_VIEW)
                                openBrowserIntent.data = Uri.parse(barcode.url?.url)
                                startActivity(openBrowserIntent)
                            }

                            else -> {
                                Toast.makeText(this, "Unsupported data type", Toast.LENGTH_SHORT)
                                    .show()
                                startCamera()
                            }
                        }
                    }
                    .setNegativeButton("Scan lagi") { _, _ ->
                        firstCall = true
                    }
                    .setCancelable(false)
                    .create()
                alertDialog.show()
            }
        }
    }

    private fun hideSystemUI() {
        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        } else {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }
        supportActionBar?.hide()
    }

    companion object {
        private const val TAG = "CameraActivity"
        const val EXTRA_CAMERAX_IMAGE = "CameraX Image"
        const val CAMERAX_RESULT = 200
    }
}