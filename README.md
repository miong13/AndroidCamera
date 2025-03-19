# AndroidCamera

## Demo : (click image to watch video)

[![ Camera Library](http://img.youtube.com/vi/4_vE8tw8XaM/0.jpg)](http://www.youtube.com/watch?v=4_vE8tw8XaM)

## How to :

1. Add it in your `settings.gradle.kts` at the end of repositories:

```
	dependencyResolutionManagement {
		repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
		repositories {
			mavenCentral()
			maven { url = uri("https://jitpack.io") }
		}
	}
```

2. Add dependency `build.gradle.kt(Module:app)`

```
dependencies {
		implementation("com.github.miong13:AndroidCamera:1.0") // CAMERA LIBRARY

    implementation 'com.github.bumptech.glide:glide:4.16.0' // for Glide
}
```


## Code Usage:

0. Add Fragment Container, Button and RecyclerView for the image results on your Activity Layout XML (ex. `activity_main.xml`)
```
<!-- NEED TO ADD THIS -->
<!--  CAMERA REQUIREMENTS : START -->
<!-- RecyclerView for camera results -->
<androidx.recyclerview.widget.RecyclerView
    android:id="@+id/mramosCamResultsRV"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_margin="@dimen/_10sdp"
    android:gravity="center"
    android:scrollbars="vertical" />
<!--  CAMERA REQUIREMENTS : END -->

<!-- Button to start camera -->
<Button
    android:id="@+id/btnStartCamera"
    android:text="START CAMERA"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_gravity="bottom" />

<!--  CAMERA REQUIREMENTS : START -->
<FrameLayout
    android:id="@+id/camera_fragment_container"
    android:name="com..cameralibrary.CameraFragment"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
<!--  CAMERA REQUIREMENTS : END -->
```

1. Declared variables need on your `Activity` Ex. `MainActivity`

```
/*
*  RESULTS VARIABLES : START
* */
private lateinit var btnStartCamera: Button
private lateinit var CamResultRecyclerView: RecyclerView
private lateinit var CamResultAdapter: CamResultAdapter
private val ResultImageList = mutableListOf<String>() // Store image URIs
/*  RESULTS VARIABLES : END */
```

2. On the `onCreate` function on your `Activity`

```
        // Set up RecyclerView for  CAM results
        CamResultRecyclerView = findViewById(R.id.mramosCamResultsRV)
        CamResultRecyclerView.layoutManager = GridLayoutManager(this, 6) // 3 columns
        CamResultAdapter = CamResultAdapter(this, ResultImageList)
        CamResultRecyclerView.adapter = CamResultAdapter
        /*  RecyclerView CODE : END */

        btnStartCamera = findViewById(R.id.btnStartCamera)
        btnStartCamera.setOnClickListener {
            // CALL  CAM MODULE
            CamModule.start(this, MainActivity::class.java, R.id.camera_fragment_container)
        }

        /*  CAM ADDITIONAL CODE : START */
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
        /*  CAM ADDITIONAL CODE : END */

```


3. Additional function needed on your `Activity` to handle the values when device has changed orientation and for appending images on the recyclerview

```
    /*
    *  CAM FUNCTIONS : START
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
    /*  CAM FUNCTIONS : END */
```

4. Create `CamResultAdapter.kt` for your `RecyclerView.Adapter`

```
package com..cameraapp

import android.app.AlertDialog
import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions

class CamResultAdapter(
    private val context: Context,
    private val imagesList: MutableList<String>
) :
    RecyclerView.Adapter<CamResultAdapter.ViewHolder>() {
    val options: RequestOptions =
        RequestOptions().override(50)
            .transform(CenterCrop(), RoundedCorners(50))

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imageThumbnail)
        val deleteImageView : ImageView = itemView.findViewById(R.id.delete_iv)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.cam_results, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val uri = Uri.parse(imagesList[position])
//        holder.imageView.setImageURI(uri)
        // Load image with Glide
        Glide.with(context).load(uri).into(holder.imageView)

//        // Long-press to delete image
//        holder.itemView.setOnLongClickListener {
//            showDeleteDialog(position)
//            true
//        }

        holder.itemView.setOnClickListener {
            // Get the position of the clicked item
            if (position != RecyclerView.NO_POSITION) {
                val imgUrl = Uri.parse(imagesList[position])
                Log.d("CAM", "Image URI: $imgUrl")
                // Show the image in a dialog
                showImageInDialog(context, imgUrl.toString())
            }
        }

        holder.deleteImageView.setOnClickListener {

            val builder: AlertDialog.Builder = AlertDialog.Builder(holder.deleteImageView.context)
            builder
                .setMessage("Are you sure you want to remove?")
                .setTitle("Remove Image")
                .setPositiveButton("Remove") { dialog, which ->
                    // Do something.
                    removeImage(position)
                }
                .setNegativeButton("Cancel") { dialog, which ->
                    // Do something else.
                }

            val dialog: AlertDialog = builder.create()
            dialog.show()
        }

    }

    override fun getItemCount(): Int = imagesList.size


    private fun showDeleteDialog(position: Int) {
        AlertDialog.Builder(context)
            .setTitle("Delete Image")
            .setMessage("Are you sure you want to remove this image?")
            .setPositiveButton("Yes") { _, _ -> removeImage(position) }
            .setNegativeButton("No", null)
            .show()
    }

    private fun removeImage(position: Int) {

            imagesList.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, imagesList.size)
            Toast.makeText(context, "Image removed!", Toast.LENGTH_SHORT).show()
    }

    fun showImageInDialog(context: Context, uri: String) {
        val dialogBuilder = AlertDialog.Builder(context)
        val inflater = LayoutInflater.from(context)
        val dialogView = inflater.inflate(R.layout.cam_imagedialog, null)
        dialogBuilder.setView(dialogView)

        // Initialize the ImageView in the dialog layout
        val imageView = dialogView.findViewById<ImageView>(R.id.dialogImageView)
        // Load the image using Glide into the ImageView
        Glide.with(context)
            .load(uri)
//            .override(1080, 1080) // Adjust size as needed
            .placeholder(R.drawable.ic_launcher_foreground)
            .error(R.drawable.ic_launcher_foreground)
            .into(imageView)

        // Create and show the dialog
        val alertDialog = dialogBuilder.create()
        alertDialog.show()
    }

}

```

5. Create a resource layout `cam_results.xml`

```
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="100dp"
    android:layout_height="100dp"
    android:orientation="vertical"
    android:layout_margin="4dp">

    <ImageView
        android:id="@+id/imageThumbnail"
        android:layout_width="100dp"
        android:layout_height="100dp"
        android:scaleType="centerCrop"
        android:background="@android:color/darker_gray"
        tools:ignore="MissingConstraints" />

    <ImageView
        android:id="@+id/delete_iv"
        android:layout_gravity="center"
        android:layout_width="@dimen/_32sdp"
        android:layout_height="@dimen/_32sdp"
        android:visibility="visible"
        android:src="@drawable/ic_delete_"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintTop_toTopOf="parent" />
</androidx.constraintlayout.widget.ConstraintLayout>

```

### 6. Create resource layout `cam_imagedialog.xml`

```
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:padding="20dp"
    android:gravity="center"
    android:orientation="vertical">

    <ImageView
        android:id="@+id/dialogImageView"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:adjustViewBounds="true"
        android:scaleType="fitCenter"
        android:padding="10dp" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

#### Buy me 🍺
