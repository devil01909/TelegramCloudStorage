package com.yourname.telegramcloud.ui.files

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.yourname.telegramcloud.R
import com.yourname.telegramcloud.auth.AuthManager
import com.yourname.telegramcloud.database.repository.StorageRepository
import com.yourname.telegramcloud.storage.FileManager
import com.yourname.telegramcloud.ui.upload.UploadActivity
import kotlinx.coroutines.launch

class FileListFragment : Fragment() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: FileListAdapter
    private lateinit var emptyState: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var fabUpload: FloatingActionButton
    private lateinit var tvFileCount: TextView
    private lateinit var tvStorageUsed: TextView
    private lateinit var etSearch: EditText
    private lateinit var breadcrumbContainer: LinearLayout
    
    private val viewModel: FileListViewModel by viewModels {
        object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                val repository = StorageRepository(requireContext())
                val fileManager = FileManager.getInstance(requireContext())
                return FileListViewModel(repository, fileManager) as T
            }
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_file_list, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        setupRecyclerView()
        setupObservers()
        setupListeners()
        loadUser()
    }
    
    private fun initViews(view: View) {
        recyclerView = view.findViewById(R.id.recyclerView)
        emptyState = view.findViewById(R.id.emptyState)
        progressBar = view.findViewById(R.id.progressBar)
        fabUpload = view.findViewById(R.id.fabUpload)
        tvFileCount = view.findViewById(R.id.tvFileCount)
        tvStorageUsed = view.findViewById(R.id.tvStorageUsed)
        etSearch = view.findViewById(R.id.etSearch)
        breadcrumbContainer = view.findViewById(R.id.breadcrumbContainer)
        
        view.findViewById<Button>(R.id.btnUploadEmpty)?.setOnClickListener {
            startUpload()
        }
    }
    
    private fun setupRecyclerView() {
        adapter = FileListAdapter(
            onItemClick = { file ->
                // Handle file click (download or preview)
                downloadFile(file)
            },
            onItemLongClick = { file ->
                showFileOptions(file)
                true
            },
            onOptionsClick = { file, view ->
                showFileOptions(file)
            }
        )
        recyclerView.adapter = adapter
    }
    
    private fun setupObservers() {
        viewModel.files.observe(viewLifecycleOwner) { files ->
            adapter.submitList(files)
            updateEmptyState(files.isEmpty())
            updateBreadcrumb()
        }
        
        viewModel.fileCount.observe(viewLifecycleOwner) { count ->
            tvFileCount.text = "$count files"
        }
        
        viewModel.totalStorage.observe(viewLifecycleOwner) { size ->
            tvStorageUsed.text = formatFileSize(size)
        }
        
        viewModel.currentFolder.observe(viewLifecycleOwner) { folder ->
            updateBreadcrumb()
        }
    }
    
    private fun setupListeners() {
        fabUpload.setOnClickListener {
            startUpload()
        }
        
        etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setSearchQuery(s.toString())
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }
    
    private fun loadUser() {
        lifecycleScope.launch {
            val authManager = AuthManager.getInstance(requireContext())
            val user = authManager.authState.value
            if (user is AuthManager.AuthState.Authenticated) {
                viewModel.setUserId(user.user.userId)
            }
        }
    }
    
    private fun updateEmptyState(isEmpty: Boolean) {
        emptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
    }
    
    private fun updateBreadcrumb() {
        breadcrumbContainer.removeAllViews()
        val folder = viewModel.currentFolder.value
        val parts = folder.split("/").filter { it.isNotEmpty() }
        
        // Add root button
        addBreadcrumbItem("Root", "/")
        
        var currentPath = ""
        parts.forEach { part ->
            currentPath += "/$part"
            addBreadcrumbItem(part, currentPath)
        }
    }
    
    private fun addBreadcrumbItem(name: String, path: String) {
        val button = TextView(requireContext()).apply {
            text = name
            setTextColor(resources.getColor(R.color.primary, null))
            setPadding(8, 4, 8, 4)
            textSize = 14f
            setOnClickListener {
                viewModel.navigateToFolder(path)
            }
        }
        breadcrumbContainer.addView(button)
        
        // Add separator
        val separator = TextView(requireContext()).apply {
            text = "/"
            setPadding(4, 4, 4, 4)
            textSize = 14f
            setTextColor(resources.getColor(R.color.text_secondary, null))
        }
        breadcrumbContainer.addView(separator)
    }
    
    private fun startUpload() {
        val intent = Intent(requireContext(), UploadActivity::class.java)
        startActivity(intent)
    }
    
    private fun downloadFile(file: StorageFile) {
        // Show download dialog
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Download")
            .setMessage("Download ${file.fileName}?")
            .setPositiveButton("Download") { _, _ ->
                // TODO: Implement download
            }
            .setNegativeButton("Cancel", null)
            .create()
        dialog.show()
    }
    
    private fun showFileOptions(file: StorageFile) {
        val dialog = BottomSheetDialog(requireContext())
        dialog.setContentView(R.layout.bottom_sheet_options)
        
        val optionShare = dialog.findViewById<LinearLayout>(R.id.optionShare)
        val optionRename = dialog.findViewById<LinearLayout>(R.id.optionRename)
        val optionMove = dialog.findViewById<LinearLayout>(R.id.optionMove)
        val optionFavorite = dialog.findViewById<LinearLayout>(R.id.optionFavorite)
        val optionDelete = dialog.findViewById<LinearLayout>(R.id.optionDelete)
        val tvFavoriteOption = dialog.findViewById<TextView>(R.id.tvFavoriteOption)
        val ivFavoriteOption = dialog.findViewById<ImageView>(R.id.ivFavoriteOption)
        
        // Update favorite text based on current state
        if (file.isFavorite) {
            tvFavoriteOption?.text = "Remove from Favorites"
            ivFavoriteOption?.setImageResource(R.drawable.ic_favorite)
        } else {
            tvFavoriteOption?.text = "Add to Favorites"
            ivFavoriteOption?.setImageResource(R.drawable.ic_favorite)
        }
        
        optionShare?.setOnClickListener {
            shareFile(file)
            dialog.dismiss()
        }
        
        optionRename?.setOnClickListener {
            renameFile(file)
            dialog.dismiss()
        }
        
        optionMove?.setOnClickListener {
            moveFile(file)
            dialog.dismiss()
        }
        
        optionFavorite?.setOnClickListener {
            viewModel.toggleFavorite(file.id, !file.isFavorite)
            dialog.dismiss()
        }
        
        optionDelete?.setOnClickListener {
            confirmDelete(file)
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    private fun shareFile(file: StorageFile) {
        // TODO: Implement sharing with Telegram
        Toast.makeText(requireContext(), "Share: ${file.fileName}", Toast.LENGTH_SHORT).show()
    }
    
    private fun renameFile(file: StorageFile) {
        val editText = EditText(requireContext())
        editText.setText(file.fileName)
        
        AlertDialog.Builder(requireContext())
            .setTitle("Rename File")
            .setView(editText)
            .setPositiveButton("Rename") { _, _ ->
                val newName = editText.text.toString()
                if (newName.isNotBlank()) {
                    lifecycleScope.launch {
                        val fileManager = FileManager.getInstance(requireContext())
                        fileManager.renameFile(file.id, newName)
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun moveFile(file: StorageFile) {
        // TODO: Implement folder picker for moving
        Toast.makeText(requireContext(), "Move: ${file.fileName}", Toast.LENGTH_SHORT).show()
    }
    
    private fun confirmDelete(file: StorageFile) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete File")
            .setMessage("Are you sure you want to delete ${file.fileName}?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteFile(file)
                Toast.makeText(requireContext(), "File deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
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