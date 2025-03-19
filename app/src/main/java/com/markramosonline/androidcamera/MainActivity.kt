package com.markramosonline.androidcamera

import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.markramosonline.cameralibrary.CamModule
import java.util.ArrayList

class MainActivity : AppCompatActivity() {

    /*
    * CAM RESULTS VARIABLES : START
    * */
    private lateinit var btnStartCamera: Button
    private lateinit var CamResultRecyclerView: RecyclerView
    private lateinit var CamResultAdapter: CamResultAdapter
    private val ResultImageList = mutableListOf<String>() // Store image URIs
    /* CAM RESULTS VARIABLES : END */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Set up RecyclerView for CAM results
        CamResultRecyclerView = findViewById(R.id.mramosCamResultsRV)
        CamResultRecyclerView.layoutManager = GridLayoutManager(this, 6) // 3 columns
        CamResultAdapter = CamResultAdapter(this, ResultImageList)
        CamResultRecyclerView.adapter = CamResultAdapter
        /* RecyclerView CODE : END */

        btnStartCamera = findViewById(R.id.btnStartCamera)
        btnStartCamera.setOnClickListener {
            // CALL CAM MODULE
            CamModule.start(this, MainActivity::class.java, R.id.camera_fragment_container)
        }

        /* CAM ADDITIONAL CODE : START */
        // Listen for results from the Camera Fragment
        supportFragmentManager.setFragmentResultListener("IMAGE_RESULT", this) { _, bundle ->
            val newImages = bundle.getStringArrayList("SAVED_IMAGES")
            newImages?.let { appendImages(it) }
        }

        // Restore images if available
        savedInstanceState?.getStringArrayList("camresult_list")?.let { savedList ->
            ResultImageList.clear()
            ResultImageList.addAll(savedList)
            CamResultAdapter.notifyDataSetChanged()
        }
        /* CAM ADDITIONAL CODE : END */

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    /*
    * CAM FUNCTIONS : START
    **/
    // Function to save the state of recyclerview
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save the list of image URIs
        outState.putStringArrayList("camresult_list", ArrayList(ResultImageList))
    }

    // Function to append new images without resetting the list
    private fun appendImages(newImages: List<String>) {
        val startPosition = ResultImageList.size // Get current size before adding
        ResultImageList.addAll(newImages)
        CamResultAdapter.notifyItemRangeInserted(startPosition, newImages.size)
    }
    /* CAM FUNCTIONS : END */
}