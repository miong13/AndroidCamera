package com.markramosonline.androidcamera

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
                Log.d("RAFIDCAM", "Image URI: $imgUrl")
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
