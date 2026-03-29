package com.yourname.telegramcloud.ui.files

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.telegramcloud.database.entities.StorageFile
import com.yourname.telegramcloud.database.repository.StorageRepository
import com.yourname.telegramcloud.storage.FileManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class FileListViewModel(
    private val repository: StorageRepository,
    private val fileManager: FileManager
) : ViewModel() {
    
    private val _userId = MutableStateFlow(0L)
    private val _currentFolder = MutableStateFlow("/")
    private val _searchQuery = MutableStateFlow("")
    
    val currentFolder: StateFlow<String> = _currentFolder.asStateFlow()
    
    val files: StateFlow<List<StorageFile>> = combine(
        _userId,
        _currentFolder,
        _searchQuery
    ) { userId, folder, query ->
        Triple(userId, folder, query)
    }.flatMapLatest { (userId, folder, query) ->
        if (query.isBlank()) {
            fileManager.getFilesInFolder(userId, folder)
        } else {
            fileManager.searchFiles(userId, query)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    val fileCount: StateFlow<Int> = files.map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    
    val totalStorage: StateFlow<Long> = _userId.flatMapLatest { userId ->
        flow { emit(fileManager.getUserStorageStats(userId).totalSizeBytes) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)
    
    fun setUserId(userId: Long) {
        _userId.value = userId
    }
    
    fun navigateToFolder(folderPath: String) {
        _currentFolder.value = folderPath
        _searchQuery.value = ""
    }
    
    fun navigateUp() {
        val current = _currentFolder.value
        if (current != "/") {
            val parent = current.substringBeforeLast("/", "")
            _currentFolder.value = if (parent.isEmpty()) "/" else parent
        }
    }
    
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    fun clearSearch() {
        _searchQuery.value = ""
    }
    
    fun toggleFavorite(fileId: Int, isFavorite: Boolean) {
        viewModelScope.launch {
            fileManager.toggleFavorite(fileId, isFavorite)
        }
    }
    
    fun deleteFile(file: StorageFile) {
        viewModelScope.launch {
            fileManager.deleteFile(file, _userId.value)
        }
    }
    
    fun createFolder(folderName: String) {
        val newPath = if (_currentFolder.value == "/") {
            "/$folderName"
        } else {
            "${_currentFolder.value}/$folderName"
        }
        fileManager.createFolder(_userId.value, newPath)
        // Refresh files
        viewModelScope.launch {
            fileManager.getFilesInFolder(_userId.value, _currentFolder.value).first()
        }
    }
}