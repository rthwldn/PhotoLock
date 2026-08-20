package com.example.security

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Enterprise-grade AES-256 GCM encryption manager for the photo vault.
 * Secures all photo payloads, thumbnails, and cloud backup archives.
 */
class CryptoManager(private val context: Context) {

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "PhotoVaultMasterKey_v1"
        private const val AES_GCM_NO_PADDING = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128
    }

    private val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
        load(null)
    }

    init {
        ensureMasterKey()
    }

    private fun ensureMasterKey(): SecretKey {
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )
            val builder = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)

            keyGenerator.init(builder.build())
            keyGenerator.generateKey()
        }
        return keyStore.getKey(KEY_ALIAS, null) as SecretKey
    }

    private fun getMasterKey(): SecretKey {
        return (keyStore.getKey(KEY_ALIAS, null) as? SecretKey) ?: ensureMasterKey()
    }

    /**
     * Encrypts raw byte array using AES-256-GCM.
     * Output format: [12-byte IV] + [Ciphertext with 128-bit Auth Tag]
     */
    fun encryptBytes(plainData: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(AES_GCM_NO_PADDING)
        cipher.init(Cipher.ENCRYPT_MODE, getMasterKey())
        val iv = cipher.iv
        val encryptedData = cipher.doFinal(plainData)

        val buffer = ByteBuffer.allocate(iv.size + encryptedData.size)
        buffer.put(iv)
        buffer.put(encryptedData)
        return buffer.array()
    }

    /**
     * Decrypts byte array that was encrypted with [encryptBytes].
     */
    fun decryptBytes(encryptedPayload: ByteArray): ByteArray {
        if (encryptedPayload.size < GCM_IV_LENGTH) {
            throw IllegalArgumentException("Payload too short to contain IV")
        }
        val iv = ByteArray(GCM_IV_LENGTH)
        val cipherText = ByteArray(encryptedPayload.size - GCM_IV_LENGTH)
        val buffer = ByteBuffer.wrap(encryptedPayload)
        buffer.get(iv)
        buffer.get(cipherText)

        val cipher = Cipher.getInstance(AES_GCM_NO_PADDING)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, getMasterKey(), spec)
        return cipher.doFinal(cipherText)
    }

    /**
     * Extracts rotation in degrees and flip status from EXIF metadata.
     */
    fun getExifOrientation(bytes: ByteArray): Pair<Int, Boolean> {
        return try {
            val exif = ExifInterface(ByteArrayInputStream(bytes))
            when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> Pair(90, false)
                ExifInterface.ORIENTATION_ROTATE_180 -> Pair(180, false)
                ExifInterface.ORIENTATION_ROTATE_270 -> Pair(270, false)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> Pair(0, true)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> Pair(180, true)
                ExifInterface.ORIENTATION_TRANSPOSE -> Pair(90, true)
                ExifInterface.ORIENTATION_TRANSVERSE -> Pair(270, true)
                else -> Pair(0, false)
            }
        } catch (e: Exception) {
            Pair(0, false)
        }
    }

    /**
     * Normalizes image orientation to upright JPEG bytes and extracts correct dimensions.
     */
    fun normalizeImageBytes(rawBytes: ByteArray): Triple<ByteArray, Int, Int> {
        val (rotation, flip) = getExifOrientation(rawBytes)

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, bounds)
        val origW = bounds.outWidth
        val origH = bounds.outHeight

        if (rotation == 0 && !flip && origW > 0 && origH > 0) {
            return Triple(rawBytes, origW, origH)
        }

        try {
            val bitmap = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size)
            if (bitmap != null) {
                val matrix = Matrix()
                if (rotation != 0) {
                    matrix.postRotate(rotation.toFloat())
                }
                if (flip) {
                    matrix.postScale(-1f, 1f)
                }
                val rotatedBitmap = Bitmap.createBitmap(
                    bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
                )
                val outStream = ByteArrayOutputStream()
                rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 95, outStream)
                val finalBytes = outStream.toByteArray()
                val finalW = rotatedBitmap.width
                val finalH = rotatedBitmap.height

                if (rotatedBitmap != bitmap) {
                    rotatedBitmap.recycle()
                }
                bitmap.recycle()
                return Triple(finalBytes, finalW, finalH)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val finalW = if (rotation == 90 || rotation == 270) origH else origW
        val finalH = if (rotation == 90 || rotation == 270) origW else origH
        return Triple(rawBytes, finalW, finalH)
    }

    /**
     * Encrypts a stream into a destination file.
     */
    suspend fun encryptStreamToFile(inputStream: InputStream, destinationFile: File): Long = withContext(Dispatchers.IO) {
        val plainBytes = inputStream.use { it.readBytes() }
        val encrypted = encryptBytes(plainBytes)
        destinationFile.parentFile?.mkdirs()
        FileOutputStream(destinationFile).use { it.write(encrypted) }
        encrypted.size.toLong()
    }

    /**
     * Decrypts a file into memory and decodes as Bitmap, respecting EXIF orientation.
     */
    suspend fun decryptFileToBitmap(encryptedFile: File, sampleSize: Int = 1): Bitmap? = withContext(Dispatchers.IO) {
        if (!encryptedFile.exists()) return@withContext null
        try {
            val encryptedBytes = FileInputStream(encryptedFile).use { it.readBytes() }
            val decryptedBytes = decryptBytes(encryptedBytes)
            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
            }
            val bitmap = BitmapFactory.decodeByteArray(decryptedBytes, 0, decryptedBytes.size, options)
                ?: return@withContext null

            val (rotation, flip) = getExifOrientation(decryptedBytes)
            if (rotation != 0 || flip) {
                val matrix = Matrix()
                if (rotation != 0) matrix.postRotate(rotation.toFloat())
                if (flip) matrix.postScale(-1f, 1f)
                val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                if (rotated != bitmap) {
                    bitmap.recycle()
                }
                rotated
            } else {
                bitmap
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Decrypts a file into raw byte array.
     */
    suspend fun decryptFileToBytes(encryptedFile: File): ByteArray? = withContext(Dispatchers.IO) {
        if (!encryptedFile.exists()) return@withContext null
        try {
            val encryptedBytes = FileInputStream(encryptedFile).use { it.readBytes() }
            decryptBytes(encryptedBytes)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Creates an encrypted thumbnail preserving exact aspect ratio without distortion.
     */
    suspend fun generateAndEncryptThumbnail(
        plainBytes: ByteArray,
        thumbFile: File,
        maxDimension: Int = 400
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeByteArray(plainBytes, 0, plainBytes.size, options)
            val origW = options.outWidth
            val origH = options.outHeight
            if (origW <= 0 || origH <= 0) return@withContext false

            var sampleSize = 1
            while (origW / (sampleSize * 2) >= maxDimension ||
                origH / (sampleSize * 2) >= maxDimension) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
            }
            var bitmap = BitmapFactory.decodeByteArray(plainBytes, 0, plainBytes.size, decodeOptions)
                ?: return@withContext false

            // Correct EXIF orientation if present in bytes
            val (rotation, flip) = getExifOrientation(plainBytes)
            if (rotation != 0 || flip) {
                val matrix = Matrix()
                if (rotation != 0) matrix.postRotate(rotation.toFloat())
                if (flip) matrix.postScale(-1f, 1f)
                val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                if (rotated != bitmap) {
                    bitmap.recycle()
                }
                bitmap = rotated
            }

            // Scale down preserving exact proportional aspect ratio
            val currentW = bitmap.width
            val currentH = bitmap.height
            val (scaledW, scaledH) = if (currentW > currentH) {
                val ratio = maxDimension.toFloat() / currentW
                Pair(maxDimension, (currentH * ratio).toInt().coerceAtLeast(1))
            } else {
                val ratio = maxDimension.toFloat() / currentH
                Pair((currentW * ratio).toInt().coerceAtLeast(1), maxDimension)
            }

            val scaledBitmap = if (currentW > maxDimension || currentH > maxDimension) {
                Bitmap.createScaledBitmap(bitmap, scaledW, scaledH, true)
            } else {
                bitmap
            }

            val stream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
            val thumbBytes = stream.toByteArray()

            val encryptedThumb = encryptBytes(thumbBytes)
            thumbFile.parentFile?.mkdirs()
            FileOutputStream(thumbFile).use { it.write(encryptedThumb) }

            if (scaledBitmap != bitmap) scaledBitmap.recycle()
            bitmap.recycle()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Calculates SHA-256 checksum for integrity checks.
     */
    fun sha256(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(data)
        return hash.joinToString("") { "%02x".format(it) }
    }

    /**
     * Hashes PIN with a salt.
     */
    fun hashPin(pin: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val combined = "$pin:$salt".toByteArray(Charsets.UTF_8)
        return Base64.encodeToString(digest.digest(combined), Base64.NO_WRAP)
    }

    /**
     * Encrypts arbitrary string (e.g. for backup metadata).
     */
    fun encryptString(plainText: String): String {
        val encrypted = encryptBytes(plainText.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(encrypted, Base64.NO_WRAP)
    }

    /**
     * Decrypts arbitrary string.
     */
    fun decryptString(base64Encrypted: String): String {
        val raw = Base64.decode(base64Encrypted, Base64.NO_WRAP)
        val decrypted = decryptBytes(raw)
        return String(decrypted, Charsets.UTF_8)
    }
}
