package com.example.bambootunnel

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class FileAdapter(
    private val baseUrl: String,                // Backend URL (chosen from tailscale)
    private val onClick: (FileItem) -> Unit
) : RecyclerView.Adapter<FileAdapter.ViewHolder>() {

    private var files = listOf<FileItem>()

    // updates list of files used by recycler view
    fun submitList(newFiles: List<FileItem>) {
        files = newFiles
        notifyDataSetChanged()
    }

    // Inflates item layout and returns a new ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.file_item, parent, false)
        return ViewHolder(view)
    }

    // Gets total amount of items in viewable directory
    override fun getItemCount(): Int = files.size

    // Binds data to a view for a specific list position.
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val file = files[position]
        holder.textView.text = file.name

        val layoutParams = holder.imageView.layoutParams
        layoutParams.width = holder.imageView.context.resources.getDimensionPixelSize(R.dimen.icon_size_large)
        layoutParams.height = holder.imageView.context.resources.getDimensionPixelSize(R.dimen.icon_size_large)
        holder.imageView.layoutParams = layoutParams

        holder.imageView.adjustViewBounds = false
        holder.imageView.scaleType = ImageView.ScaleType.CENTER_INSIDE

        if (!file.preview.isNullOrEmpty()) {
            val previewUrl = baseUrl.trimEnd('/') + file.preview

            // Let Glide naturally load the image using the layout size
            Glide.with(holder.imageView.context)
                .load(previewUrl)
                .placeholder(R.drawable.ic_file)
                .error(R.drawable.ic_file)
                .into(holder.imageView)
        } else {
            val drawableRes = if (file.type == "directory") R.drawable.ic_folder else R.drawable.ic_file
            holder.imageView.setImageResource(drawableRes)
        }

        holder.itemView.setOnClickListener { onClick(file) }
    }

    // Holds item view references for fast access and reuse.
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.fileName)
        val imageView: ImageView = view.findViewById(R.id.previewImage)
    }
}
