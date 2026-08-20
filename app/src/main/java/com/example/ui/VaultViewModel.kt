package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AlbumEntity
import com.example.data.local.CloudBackupSnapshotEntity
import com.example.data.local.EncryptedPhotoEntity
import com.example.data.local.VaultDatabase
import com.example.data.repository.CloudBackupManager
import com.example.data.repository.VaultRepository
import com.example.security.BiometricHelper
import com.example.security.CryptoManager
import com.example.security.SecurityPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class PinSetupStep {
    ENTER_NEW_PIN,
    CONFIRM_PIN,
    SECURITY_QUESTION,
    UNLOCK
}

enum class VaultTab {
    ALBUMS,
    ALL_PHOTOS,
    FAVORITES,
    BACKUP,
    TRASH,
    SETTINGS
}

data class ImportProgressState(
    val isImporting: Boolean = false,
    val currentItem: Int = 0,
    val totalItems: Int = 0,
    val message: String = ""
)

class VaultViewModel(application: Application) : AndroidViewModel(application) {

    val cryptoManager = CryptoManager(application)
    private val database = VaultDatabase.getDatabase(application)
    val repository = VaultRepository(application, database.vaultDao(), cryptoManager)
    val backupManager = CloudBackupManager(application, database.vaultDao(), repository, cryptoManager)
    val securityPrefs = SecurityPreferences(application)

    // === AUTH STATE ===
    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    private val _pinSetupStep = MutableStateFlow(
        if (securityPrefs.isInitialized) PinSetupStep.UNLOCK else PinSetupStep.ENTER_NEW_PIN
    )
    val pinSetupStep: StateFlow<PinSetupStep> = _pinSetupStep.asStateFlow()

    private val _tempPin = MutableStateFlow("")
    private val _lockError = MutableStateFlow<String?>(null)
    val lockError: StateFlow<String?> = _lockError.asStateFlow()

    private val _lockoutSec = MutableStateFlow(securityPrefs.getLockoutRemainingSeconds())
    val lockoutSec: StateFlow<Long> = _lockoutSec.asStateFlow()

    val biometricAvailable = BiometricHelper.isBiometricAvailable(application)
    val biometricEnabled: Boolean get() = securityPrefs.biometricEnabled

    // === NAVIGATION & SELECTION STATE ===
    private val _currentTab = MutableStateFlow(VaultTab.ALBUMS)
    val currentTab: StateFlow<VaultTab> = _currentTab.asStateFlow()

    private val _currentAlbumId = MutableStateFlow<String?>(null)
    val currentAlbumId: StateFlow<String?> = _currentAlbumId.asStateFlow()

    private val _breadcrumbs = MutableStateFlow<List<AlbumEntity>>(emptyList())
    val breadcrumbs: StateFlow<List<AlbumEntity>> = _breadcrumbs.asStateFlow()

    private val _selectedPhotoIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedPhotoIds: StateFlow<Set<String>> = _selectedPhotoIds.asStateFlow()

    private val _importProgress = MutableStateFlow(ImportProgressState())
    val importProgress: StateFlow<ImportProgressState> = _importProgress.asStateFlow()

    private val _backupProgress = MutableStateFlow<CloudBackupManager.BackupProgress>(CloudBackupManager.BackupProgress.Idle)
    val backupProgress: StateFlow<CloudBackupManager.BackupProgress> = _backupProgress.asStateFlow()

    private val _restoreProgress = MutableStateFlow<CloudBackupManager.RestoreProgress>(CloudBackupManager.RestoreProgress.Idle)
    val restoreProgress: StateFlow<CloudBackupManager.RestoreProgress> = _restoreProgress.asStateFlow()

    // === DATA FLOWS ===
    val rootAlbums: StateFlow<List<AlbumEntity>> = repository.getRootAlbums()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAlbums: StateFlow<List<AlbumEntity>> = repository.getAllAlbums()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allActivePhotos: StateFlow<List<EncryptedPhotoEntity>> = repository.getAllActivePhotos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoritePhotos: StateFlow<List<EncryptedPhotoEntity>> = repository.getFavoritePhotos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trashPhotos: StateFlow<List<EncryptedPhotoEntity>> = repository.getTrashPhotos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cloudBackups: StateFlow<List<CloudBackupSnapshotEntity>> = repository.getAllBackups()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentAlbumPhotos: StateFlow<List<EncryptedPhotoEntity>> = _currentAlbumId.flatMapLatest { albumId ->
        if (albumId != null) {
            repository.getPhotosByAlbum(albumId)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentSubAlbums: StateFlow<List<AlbumEntity>> = _currentAlbumId.flatMapLatest { albumId ->
        if (albumId != null) {
            repository.getSubAlbums(albumId)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentAlbum: StateFlow<AlbumEntity?> = _currentAlbumId.flatMapLatest { albumId ->
        if (albumId != null) {
            repository.getAlbumByIdFlow(albumId)
        } else {
            flowOf(null)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _storageUsageBytes = MutableStateFlow(0L)
    val storageUsageBytes: StateFlow<Long> = _storageUsageBytes.asStateFlow()

    init {
        refreshStorageUsage()
        // If security is initialized and biometrics enabled, prompt if needed
        if (!securityPrefs.isInitialized) {
            _pinSetupStep.value = PinSetupStep.ENTER_NEW_PIN
        } else {
            _pinSetupStep.value = PinSetupStep.UNLOCK
        }
    }

    // === AUTH METHODS ===

    fun onPinDigit(digit: String, currentInput: String): String {
        _lockError.value = null
        if (currentInput.length < 6) {
            return currentInput + digit
        }
        return currentInput
    }

    fun submitPin(pin: String) {
        val remaining = securityPrefs.getLockoutRemainingSeconds()
        if (remaining > 0) {
            _lockoutSec.value = remaining
            _lockError.value = "Příliš mnoho pokusů. Počkejte $remaining s."
            return
        }

        when (_pinSetupStep.value) {
            PinSetupStep.ENTER_NEW_PIN -> {
                if (pin.length < 4) {
                    _lockError.value = "PIN musí mít alespoň 4 číslice"
                    return
                }
                _tempPin.value = pin
                _pinSetupStep.value = PinSetupStep.CONFIRM_PIN
                _lockError.value = null
            }
            PinSetupStep.CONFIRM_PIN -> {
                if (pin == _tempPin.value) {
                    _pinSetupStep.value = PinSetupStep.SECURITY_QUESTION
                    _lockError.value = null
                } else {
                    _lockError.value = "Zadané PIN kódy se neshodují"
                    _pinSetupStep.value = PinSetupStep.ENTER_NEW_PIN
                    _tempPin.value = ""
                }
            }
            PinSetupStep.SECURITY_QUESTION -> {
                // Done via finishSecuritySetup
            }
            PinSetupStep.UNLOCK -> {
                val success = securityPrefs.verifyPin(pin, cryptoManager)
                if (success) {
                    _isUnlocked.value = true
                    _lockError.value = null
                    refreshStorageUsage()
                } else {
                    val attempts = securityPrefs.getFailedAttempts()
                    val remLock = securityPrefs.getLockoutRemainingSeconds()
                    if (remLock > 0) {
                        _lockoutSec.value = remLock
                        _lockError.value = "Trezor zablokován na $remLock s."
                    } else {
                        _lockError.value = "Nesprávný PIN kód ($attempts/5)"
                    }
                }
            }
        }
    }

    fun finishSecuritySetup(question: String, answer: String, enableBiometrics: Boolean) {
        securityPrefs.setupPin(_tempPin.value, cryptoManager, question, answer)
        securityPrefs.biometricEnabled = enableBiometrics
        _isUnlocked.value = true
        _pinSetupStep.value = PinSetupStep.UNLOCK
        _tempPin.value = ""
        refreshStorageUsage()
    }

    fun unlockWithBiometricSuccess() {
        securityPrefs.resetFailedAttempts()
        _isUnlocked.value = true
        _lockError.value = null
        refreshStorageUsage()
    }

    fun lockVault() {
        _isUnlocked.value = false
        _currentAlbumId.value = null
        _selectedPhotoIds.value = emptySet()
        if (securityPrefs.isInitialized) {
            _pinSetupStep.value = PinSetupStep.UNLOCK
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        securityPrefs.biometricEnabled = enabled
    }

    fun setAutoLockSeconds(seconds: Int) {
        securityPrefs.autoLockSeconds = seconds
    }

    // === NAVIGATION METHODS ===

    fun selectTab(tab: VaultTab) {
        _currentTab.value = tab
        _selectedPhotoIds.value = emptySet()
        if (tab != VaultTab.ALBUMS) {
            _currentAlbumId.value = null
            _breadcrumbs.value = emptyList()
        }
    }

    fun navigateToAlbum(albumId: String?) {
        _currentAlbumId.value = albumId
        _selectedPhotoIds.value = emptySet()
        viewModelScope.launch {
            _breadcrumbs.value = repository.getBreadcrumbPath(albumId)
        }
    }

    // === SELECTION METHODS ===

    fun togglePhotoSelection(photoId: String) {
        val current = _selectedPhotoIds.value.toMutableSet()
        if (current.contains(photoId)) {
            current.remove(photoId)
        } else {
            current.add(photoId)
        }
        _selectedPhotoIds.value = current
    }

    fun selectAllPhotos(photos: List<EncryptedPhotoEntity>) {
        _selectedPhotoIds.value = photos.map { it.id }.toSet()
    }

    fun clearSelection() {
        _selectedPhotoIds.value = emptySet()
    }

    // === ALBUM & PHOTO ACTIONS ===

    fun createAlbum(
        name: String,
        description: String = "",
        parentId: String? = _currentAlbumId.value,
        colorHex: String = "#0284C7"
    ) {
        viewModelScope.launch {
            repository.createAlbum(name, description, parentId, colorHex)
        }
    }

    fun updateAlbum(album: AlbumEntity) {
        viewModelScope.launch {
            repository.updateAlbum(album)
            if (_currentAlbumId.value == album.id) {
                _breadcrumbs.value = repository.getBreadcrumbPath(album.id)
            }
        }
    }

    fun setAlbumCover(albumId: String, photoId: String) {
        viewModelScope.launch {
            repository.setAlbumCover(albumId, photoId)
        }
    }

    fun deleteCurrentAlbum(onDeleted: () -> Unit) {
        val albumId = _currentAlbumId.value ?: return
        viewModelScope.launch {
            val parent = repository.getAlbumById(albumId)?.parentId
            repository.deleteAlbum(albumId)
            navigateToAlbum(parent)
            onDeleted()
            refreshStorageUsage()
        }
    }

    fun deleteAlbumById(albumId: String) {
        viewModelScope.launch {
            repository.deleteAlbum(albumId)
            refreshStorageUsage()
        }
    }

    fun importPhotos(
        uris: List<Uri>,
        targetAlbumId: String,
        onImportCompleted: ((List<Uri>) -> Unit)? = null
    ) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _importProgress.value = ImportProgressState(
                isImporting = true,
                currentItem = 0,
                totalItems = uris.size,
                message = "Importuji a šifruji 0/${uris.size}..."
            )

            val importedUris = mutableListOf<Uri>()

            for ((index, uri) in uris.withIndex()) {
                _importProgress.value = ImportProgressState(
                    isImporting = true,
                    currentItem = index + 1,
                    totalItems = uris.size,
                    message = "Šifruji fotku ${index + 1}/${uris.size}..."
                )
                val entity = repository.importPhotoFromUri(uri, targetAlbumId)
                if (entity != null) {
                    importedUris.add(uri)
                }
            }

            _importProgress.value = ImportProgressState(isImporting = false)
            refreshStorageUsage()

            if (importedUris.isNotEmpty()) {
                onImportCompleted?.invoke(importedUris)
            }
        }
    }

    fun moveSelectedPhotos(targetAlbumId: String) {
        val selected = _selectedPhotoIds.value.toList()
        if (selected.isEmpty()) return
        viewModelScope.launch {
            repository.movePhotos(selected, targetAlbumId)
            clearSelection()
        }
    }

    fun moveSinglePhoto(photoId: String, targetAlbumId: String) {
        viewModelScope.launch {
            repository.movePhoto(photoId, targetAlbumId)
        }
    }

    fun toggleFavorite(photoId: String, isFavorite: Boolean) {
        viewModelScope.launch {
            repository.toggleFavorite(photoId, isFavorite)
        }
    }

    fun updatePhotoNote(photoId: String, note: String) {
        viewModelScope.launch {
            repository.updatePhotoNote(photoId, note)
        }
    }

    fun moveSelectedToTrash() {
        val selected = _selectedPhotoIds.value.toList()
        if (selected.isEmpty()) return
        viewModelScope.launch {
            repository.moveMultipleToTrash(selected)
            clearSelection()
            refreshStorageUsage()
        }
    }

    fun moveSingleToTrash(photoId: String) {
        viewModelScope.launch {
            repository.moveToTrash(photoId)
            refreshStorageUsage()
        }
    }

    fun restoreSelectedFromTrash() {
        val selected = _selectedPhotoIds.value.toList()
        if (selected.isEmpty()) return
        viewModelScope.launch {
            repository.restoreMultipleFromTrash(selected)
            clearSelection()
            refreshStorageUsage()
        }
    }

    fun restoreSingleFromTrash(photoId: String) {
        viewModelScope.launch {
            repository.restoreFromTrash(photoId)
            refreshStorageUsage()
        }
    }

    fun permanentlyDeleteSelected() {
        val selected = _selectedPhotoIds.value.toList()
        if (selected.isEmpty()) return
        viewModelScope.launch {
            repository.permanentlyDeleteMultiple(selected)
            clearSelection()
            refreshStorageUsage()
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            val trashed = repository.getTrashPhotos()
            repository.emptyTrash()
            clearSelection()
            refreshStorageUsage()
        }
    }

    fun exportPhotoToGallery(photo: EncryptedPhotoEntity, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.exportPhotoToGallery(photo)
            onResult(success)
        }
    }

    // === BACKUP ACTIONS ===

    fun createEncryptedCloudBackup(onCreated: ((File) -> Unit)? = null) {
        viewModelScope.launch {
            val file = backupManager.createEncryptedCloudBackup { progress ->
                _backupProgress.value = progress
            }
            if (file != null) {
                securityPrefs.lastBackupTimestamp = System.currentTimeMillis()
                onCreated?.invoke(file)
            }
        }
    }

    fun restoreBackupFromFile(file: File) {
        viewModelScope.launch {
            backupManager.restoreEncryptedBackup(file) { progress ->
                _restoreProgress.value = progress
            }
            refreshStorageUsage()
        }
    }

    fun restoreBackupFromUri(uri: Uri) {
        viewModelScope.launch {
            backupManager.restoreEncryptedBackupFromUri(uri) { progress ->
                _restoreProgress.value = progress
            }
            refreshStorageUsage()
        }
    }

    fun shareBackup(file: File) {
        backupManager.shareBackupFile(file)
    }

    fun exportBackupToUri(file: File, destinationUri: Uri, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = backupManager.exportBackupToUri(file, destinationUri)
            onResult(success)
        }
    }

    fun resetBackupProgress() {
        _backupProgress.value = CloudBackupManager.BackupProgress.Idle
    }

    fun resetRestoreProgress() {
        _restoreProgress.value = CloudBackupManager.RestoreProgress.Idle
    }

    fun deleteBackup(snapshot: CloudBackupSnapshotEntity) {
        viewModelScope.launch {
            repository.deleteBackup(snapshot)
        }
    }

    fun refreshStorageUsage() {
        viewModelScope.launch {
            _storageUsageBytes.value = repository.getTotalStorageBytes()
        }
    }
}
