package com.example.util

import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.activity.result.IntentSenderRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GalleryHelper {

    /**
     * Resolves incoming URIs (e.g. from photo picker or documents provider) into MediaStore Image URIs if applicable.
     */
    fun resolveToMediaStoreUri(uri: Uri): Uri {
        val lastSegment = uri.lastPathSegment ?: return uri
        val rawId = if (lastSegment.contains(":")) {
            lastSegment.substringAfterLast(":")
        } else {
            lastSegment
        }
        val numericId = rawId.toLongOrNull() ?: return uri
        return ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, numericId)
    }

    /**
     * Deletes imported photos from the phone's gallery/device storage.
     * Triggers onRequiresConsent if Android OS requires user authorization (Android 10/11+ Scoped Storage).
     */
    suspend fun requestDeleteFromGallery(
        context: Context,
        uris: List<Uri>,
        onRequiresConsent: (IntentSenderRequest) -> Unit,
        onSuccess: (deletedCount: Int) -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        if (uris.isEmpty()) {
            withContext(Dispatchers.Main) { onSuccess(0) }
            return@withContext
        }

        val contentResolver = context.contentResolver
        val mediaStoreUris = mutableListOf<Uri>()

        for (uri in uris) {
            val resolved = resolveToMediaStoreUri(uri)
            mediaStoreUris.add(resolved)
        }

        // On Android 11+ (API 30+), MediaStore.createDeleteRequest is the standard Scoped Storage API
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val pendingIntent = MediaStore.createDeleteRequest(contentResolver, mediaStoreUris)
                val request = IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                withContext(Dispatchers.Main) {
                    onRequiresConsent(request)
                }
                return@withContext
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Direct deletion or Android 10 RecoverableSecurityException
        var deletedCount = 0
        var consentRequested = false

        for (uri in uris) {
            try {
                val rows = contentResolver.delete(uri, null, null)
                if (rows > 0) {
                    deletedCount++
                } else {
                    val resolved = resolveToMediaStoreUri(uri)
                    val resolvedRows = if (resolved != uri) contentResolver.delete(resolved, null, null) else 0
                    if (resolvedRows > 0) {
                        deletedCount++
                    } else {
                        val docDeleted = try {
                            DocumentsContract.deleteDocument(contentResolver, uri)
                        } catch (ex: Exception) {
                            false
                        }
                        if (docDeleted) deletedCount++
                    }
                }
            } catch (secEx: SecurityException) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && secEx is RecoverableSecurityException) {
                    val intentSender = secEx.userAction.actionIntent.intentSender
                    withContext(Dispatchers.Main) {
                        onRequiresConsent(IntentSenderRequest.Builder(intentSender).build())
                    }
                    consentRequested = true
                    break
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (!consentRequested) {
            withContext(Dispatchers.Main) {
                onSuccess(deletedCount)
            }
        }
    }
}
