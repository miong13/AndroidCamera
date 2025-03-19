package com.markramosonline.cameralibrary

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.markramosonline.cameralibrary.adapters.ThumbnailAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import com.markramosonline.cameralibrary.R


import java.io.File

class CamModule : Fragment() {
    private lateinit var previewView: PreviewView
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var isFlashOn = false

    private val photoList = mutableListOf<File>() // 📸 List to store captured images
    private lateinit var thumbnailsRecyclerView: RecyclerView
    private lateinit var adapter: ThumbnailAdapter
    private lateinit var captureCounterButton: Button // 🔹 New button
    private lateinit var backButton: Button
    private lateinit var flashButton: Button

    private lateinit var closeCameraButton : ImageButton
    private lateinit var imgFlashButton : ImageButton
    private lateinit var imgCaptureButton : ImageButton
    private lateinit var imgCaptureCounterButton : ImageButton
    private lateinit var tvCounter : TextView

    private val imageUris = ArrayList<String>() // Store transferred images' URIs

    companion object {
        private var previousActivityClass: Class<*>? = null

        fun start(activity: AppCompatActivity, previousActivity: Class<*>, fragmentContainerId: Int) {
            previousActivityClass = previousActivity
            val fragment = CamModule()
            activity.supportFragmentManager.beginTransaction()
                .replace(fragmentContainerId, fragment)
                .addToBackStack(null)
                .commit()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Restore saved photos
        savedInstanceState?.getStringArrayList("photo_list")?.let { savedPaths ->
            photoList.clear()
            photoList.addAll(savedPaths.map { File(it) })
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        // Save the photo list paths
        val imagePaths = photoList.map { it.absolutePath }
        outState.putStringArrayList("photo_list", ArrayList(imagePaths))

        outState.putInt("capture_count", photoList.size)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_camera, container, false)
        previewView = view.findViewById(R.id.previewView)
        thumbnailsRecyclerView = view.findViewById(R.id.thumbnailsRecyclerView)
        captureCounterButton = view.findViewById(R.id.captureCounterButton) // 🔹 Initialize button
        backButton = view.findViewById(R.id.backButton)
        flashButton = view.findViewById(R.id.flashButton)

        closeCameraButton = view.findViewById(R.id.closeCameraButton)
        imgFlashButton = view.findViewById(R.id.imgFlashButton)
        imgCaptureButton = view.findViewById(R.id.imgCaptureButton)
        imgCaptureCounterButton = view.findViewById(R.id.imgCaptureCounterButton)
        tvCounter = view.findViewById(R.id.tvCounter)

        // Initialize RecyclerView with delete functionality
        adapter = ThumbnailAdapter(requireContext(), photoList) { file ->
            deletePhoto(file)
        }
        thumbnailsRecyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        thumbnailsRecyclerView.adapter = adapter

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (hasCameraPermission()) {
            startCamera()
        } else {
            requestCameraPermission()
        }
        // Restore capture counter
        val savedCount = savedInstanceState?.getInt("capture_count", 0) ?: photoList.size
        captureCounterButton.text = "$savedCount"
        tvCounter.text = "$savedCount"

        view.findViewById<Button>(R.id.captureButton).setOnClickListener {
            takePhoto()
        }

        imgCaptureButton.setOnClickListener {
            takePhoto()
        }

        captureCounterButton.setOnClickListener {
//            Toast.makeText(requireContext(), "Total Photos: ${photoList.size}", Toast.LENGTH_SHORT).show()
            if (photoList.isNotEmpty()) {
                saveImagesToGallery()
            } else {
                Toast.makeText(requireContext(), "No images to save!", Toast.LENGTH_SHORT).show()
            }
        }

        imgCaptureCounterButton.setOnClickListener{
            if (photoList.isNotEmpty()) {
                saveImagesToGallery()
            } else {
                Toast.makeText(requireContext(), "No images to save!", Toast.LENGTH_SHORT).show()
            }
        }

        backButton.setOnClickListener {
            deleteAllPhotos()
            parentFragmentManager.popBackStack()
        }

        closeCameraButton.setOnClickListener {
            deleteAllPhotos()
            parentFragmentManager.popBackStack()
        }

        flashButton.setOnClickListener {
            toggleFlash()
        }

        imgFlashButton.setOnClickListener {
            toggleFlash()
        }
    }

    // 🔹 Update Badge Counter
    private fun updateBadgeCounter() {
        captureCounterButton.text = "${photoList.size}"
        tvCounter.text ="${photoList.size}"
    }

    // 🔹 Request Camera Permission
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                startCamera()
            } else {
                Toast.makeText(requireContext(), "Camera permission is required", Toast.LENGTH_LONG).show()
            }
        }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        requestPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // 🔹 Start CameraX
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        Toast.makeText(requireContext(), "Preparing Camera "+ ("\uD83D\uDCF7") + " ...", Toast.LENGTH_SHORT).show()
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider?.unbindAll()
                camera = cameraProvider?.bindToLifecycle(this, cameraSelector, preview, imageCapture)
                Toast.makeText(requireContext(), ("\uD83D\uDCF7") + " Camera is now ready " + ("✅") , Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("CameraX", "Use case binding failed", e)
                Toast.makeText(requireContext(), "❌ Camera not ready. Issue loading Camera ❌", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    // 🔹 Capture Photo and Show Thumbnails in RecyclerView
    private fun takePhoto() {
        if (imageCapture == null) {
            Toast.makeText(requireContext(), "Camera not ready", Toast.LENGTH_SHORT).show()
            return
        }

        val file = File(requireContext().filesDir, "${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()

        imageCapture?.takePicture(
            outputOptions, ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    Log.d("CameraX", "Photo saved: ${file.absolutePath}")


                    photoList.add(file) // 📸 Add photo to list
                    adapter.notifyDataSetChanged() // 🔄 Refresh RecyclerView
                    updateBadgeCounter() // 🔹 Update Counter
                    Handler(Looper.getMainLooper()).postDelayed({
                        Toast.makeText(requireContext(), "Photo saved! \uD83D\uDCBE", Toast.LENGTH_SHORT).show()
                    }, 100)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("CameraX", "Error: ${exception.message}", exception)
                    Toast.makeText(requireContext(), "❌ Capture failed! ❌", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun toggleFlash() {
        camera?.cameraControl?.enableTorch(!isFlashOn)
        isFlashOn = !isFlashOn

        // Change button image based on flash state
        val flashIcon = if (isFlashOn) {
            R.drawable.flash // Replace with your actual "flash on" image
        } else {
            R.drawable.no_flash // Replace with your actual "flash off" image
        }

        imgFlashButton.setImageResource(flashIcon)
    }

    private fun showDeleteDialog(file: File) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Photo")
            .setMessage("Are you sure you want to delete this photo?")
            .setPositiveButton("Yes") { _, _ -> deletePhoto(file) }
            .setNegativeButton("No", null)
            .show()
    }

    // 🔹 Delete Photo from RecyclerView & Storage
    private fun deletePhoto(file: File) {
        if (file.exists()) {
            if (file.delete()) {
                Log.d("CameraX", "Photo deleted: ${file.absolutePath}")
                photoList.remove(file) // Remove from list
                adapter.notifyDataSetChanged() // 🔄 Refresh RecyclerView
                updateBadgeCounter() // 🔹 Update Counter
                Handler(Looper.getMainLooper()).postDelayed({
                    Toast.makeText(requireContext(), "Photo has been removed and deleted! \uD83D\uDEAE", Toast.LENGTH_SHORT).show()
                }, 100)
            } else {
                Toast.makeText(requireContext(), "❌ Failed to delete photo ❌", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteAllPhotos() {
        if (photoList.isNotEmpty()) {
            for (file in photoList) {
                file.delete()
            }
            photoList.clear()
            adapter.notifyDataSetChanged()
            updateBadgeCounter()
            Toast.makeText(requireContext(), "All photos deleted! \uD83D\uDDD1\uFE0F", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveImagesToGallery() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_MEDIA_IMAGES
            ) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestGalleryPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            )
            return
        }

        val imageUris = ArrayList<String>()

        for (file in photoList) {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, file.name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/RafidCamModule")
            }

            val uri = requireContext().contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues
            )

            uri?.let {
                requireContext().contentResolver.openOutputStream(it)?.use { outputStream ->
                    file.inputStream().copyTo(outputStream)
                }
                file.delete()
                imageUris.add(it.toString())
            }
        }

        photoList.clear()
        adapter.notifyDataSetChanged()
        updateBadgeCounter()
        Toast.makeText(requireContext(), "Images saved to Gallery!", Toast.LENGTH_SHORT).show()

        sendImagesToPreviousActivity(imageUris)
    }


    private val requestGalleryPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions[Manifest.permission.READ_MEDIA_IMAGES] == true ||
                permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true) {
                saveImagesToGallery() // Proceed with saving images
            } else {
                Toast.makeText(requireContext(), "Permission required to save images!", Toast.LENGTH_SHORT).show()
            }
        }

    private fun sendImagesToPreviousActivity(imageUris: ArrayList<String>) {
        val result = Bundle().apply {
            putStringArrayList("SAVED_IMAGES", imageUris)
        }

        parentFragmentManager.setFragmentResult("IMAGE_RESULT", result)
        parentFragmentManager.popBackStack() // Close fragment and return to previous screen

    }
}