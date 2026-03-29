package com.yourname.telegramcloud.ui.files

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.yourname.telegramcloud.R
import com.yourname.telegramcloud.database.entities.StorageFile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FileListAdapter(
    private val onItemClick: (StorageFile) -> Unit,
    private val onItemLongClick: (StorageFile) -> Unit,
    private val onOptionsClick: (StorageFile, View) -> Unit
) : RecyclerView.Adapter<FileListAdapter.FileViewHolder>() {
    
    private var files = listOf<StorageFile>()
    private val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    
    fun submitList(newFiles: List<StorageFile>) {
        files = newFiles
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_file, parent, false)
        return FileViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        holder.bind(files[position])
    }
    
    override fun getItemCount(): Int = files.size
    
    inner class FileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        
        private val ivIcon: ImageView = itemView.findViewById(R.id.ivIcon)
        private val tvFileName: TextView = itemView.findViewById(R.id.tvFileName)
        private val tvFileSize: TextView = itemView.findViewById(R.id.tvFileSize)
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val ivFavorite: ImageView = itemView.findViewById(R.id.ivFavorite)
        private val ivMore: ImageButton = itemView.findViewById(R.id.ivMore)
        
        fun bind(file: StorageFile) {
            tvFileName.text = file.fileName
            tvFileSize.text = formatFileSize(file.fileSize)
            tvDate.text = dateFormat.format(Date(file.uploadedAt))
            
            // Set icon based on file type using Android default icons
            setFileIcon(file.mimeType)
            
            // Show favorite star if favorited
            ivFavorite.visibility = if (file.isFavorite) View.VISIBLE else View.GONE
            
            // Click listeners
            itemView.setOnClickListener { onItemClick(file) }
            itemView.setOnLongClickListener {
                onItemLongClick(file)
                true
            }
            ivMore.setOnClickListener { onOptionsClick(file, it) }
        }
        
        private fun setFileIcon(mimeType: String?) {
            val iconRes = when {
                mimeType?.startsWith("image/") == true -> android.R.drawable.ic_menu_gallery
                mimeType?.startsWith("video/") == true -> android.R.drawable.ic_media_play
                mimeType?.startsWith("audio/") == true -> android.R.drawable.ic_media_play
                mimeType == "application/pdf" -> android.R.drawable.ic_menu_report_image
                mimeType?.contains("zip") == true || mimeType?.contains("rar") == true -> android.R.drawable.ic_menu_archive
                mimeType?.startsWith("text/") == true -> android.R.drawable.ic_menu_edit
                mimeType?.startsWith("application/") == true -> android.R.drawable.ic_menu_info_details
                else -> android.R.drawable.ic_menu_gallery
            }
            ivIcon.setImageResource(iconRes)
        }
        
        private fun formatFileSize(bytes: Long): String {
            return when {
                bytes >= 1024 * 1024 * 1024 -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
                bytes >= 1024 * 1024 -> String.format("%.2f MB", bytes / (1024.0 * 1024.0))
                bytes >= 1024 -> String.format("%.2f KB", bytes / 1024.0)
                else -> "$bytes B"
            }
        }
    }
}