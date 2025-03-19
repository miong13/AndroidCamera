package com.markramosonline.cameralibrary.adapters

import android.app.AlertDialog
import android.content.Context
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.recyclerview.widget.RecyclerView
import com.markramosonline.cameralibrary.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


class ThumbnailAdapter(
    private val context: Context,
    private val photoList: MutableList<File>,
    private val onDelete: (File) -> Unit
) : RecyclerView.Adapter<ThumbnailAdapter.ThumbnailViewHolder>() {

    // Track which images have been loaded
    private val loadedImages = mutableSetOf<String>()

    class ThumbnailViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.thumbnailImageView)
        val progressBar: ProgressBar = view.findViewById(R.id.progressBar)
        val deleteImageView: ImageView = view.findViewById(R.id.deleteImageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThumbnailViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_thumbnail, parent, false)
        return ThumbnailViewHolder(view)
    }

    override fun onBindViewHolder(holder: ThumbnailViewHolder, position: Int) {
        val file = photoList[position]

        // Check if image was already loaded
        if (loadedImages.contains(file.absolutePath)) {
            // If already loaded, show the image and hide progress bar
            holder.progressBar.visibility = View.GONE
            holder.imageView.visibility = View.VISIBLE
            holder.deleteImageView.visibility = View.VISIBLE
            holder.imageView.setImageBitmap(BitmapFactory.decodeFile(file.absolutePath))
        } else {
            // Show progress bar and hide image before loading
            holder.progressBar.visibility = View.VISIBLE
            holder.imageView.visibility = View.INVISIBLE
            holder.deleteImageView.visibility = View.INVISIBLE

            // Load image in background
            CoroutineScope(Dispatchers.IO).launch {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)

                withContext(Dispatchers.Main) {
                    // Hide progress bar and show image
                    holder.progressBar.visibility = View.GONE
                    holder.imageView.setImageBitmap(bitmap)
                    holder.imageView.visibility = View.VISIBLE
                    holder.deleteImageView.visibility = View.VISIBLE

                    // Mark this image as loaded
                    loadedImages.add(file.absolutePath)
                }
            }
        }

//        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
//        holder.imageView.setImageBitmap(bitmap)

        // Tap to remove image
        holder.imageView.setOnClickListener {
            AlertDialog.Builder(context)
                .setTitle("Delete Photo")
                .setMessage("Are you sure you want to delete this photo?")
                .setPositiveButton("Yes") { _, _ ->
                    onDelete(file) // Call delete function
                }
                .setNegativeButton("No", null)
                .show()
        }
    }

    override fun getItemCount(): Int = photoList.size
}