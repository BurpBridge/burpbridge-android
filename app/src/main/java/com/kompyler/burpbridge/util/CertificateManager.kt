package com.kompyler.burpbridge.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CertificateManager(private val context: Context) {

    companion object {
        private const val CERT_FILE_NAME = "burp_cacert.der"
        private const val CERT_DIR = "burpsuite"
    }

    data class DownloadResult(
        val bytes: ByteArray,
        val uri: Uri,
        val filePath: String
    )

    suspend fun downloadCertificate(ip: String, port: Int): Result<DownloadResult> = withContext(Dispatchers.IO) {
        val urlStr = "http://$ip:$port/cert"
        Log.d("BurpBridge-Cert", "Downloading certificate from $urlStr")
        try {
            val url = URL(urlStr)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val responseCode = connection.responseCode
            Log.d("BurpBridge-Cert", "HTTP response code: $responseCode")
            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.e("BurpBridge-Cert", "Server returned non-200: $responseCode")
                return@withContext Result.failure(
                    Exception("Server returned HTTP $responseCode")
                )
            }

            val bytes = connection.inputStream.use { it.readBytes() }
            Log.d("BurpBridge-Cert", "Downloaded ${bytes.size} bytes")
            if (bytes.isEmpty()) {
                Log.e("BurpBridge-Cert", "Server returned empty certificate")
                return@withContext Result.failure(
                    Exception("Server returned an empty certificate")
                )
            }

            Log.d("BurpBridge-Cert", "Saving to Downloads/$CERT_DIR/")
            val saved = saveToDownloads(bytes)
            saved?.let {
                Log.d("BurpBridge-Cert", "Certificate saved: ${it.filePath} (uri=${it.uri})")
                Result.success(it)
            } ?: run {
                Log.e("BurpBridge-Cert", "Failed to save certificate to Downloads/$CERT_DIR/")
                Result.failure(Exception("Could not save certificate to Downloads/$CERT_DIR/"))
            }
        } catch (e: Exception) {
            Log.e("BurpBridge-Cert", "Certificate download failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun saveToDownloads(data: ByteArray): DownloadResult? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Log.d("BurpBridge-Cert", "Using MediaStore for API ${Build.VERSION.SDK_INT}")
            saveViaMediaStore(data)
        } else {
            Log.d("BurpBridge-Cert", "Using legacy file API for API ${Build.VERSION.SDK_INT}")
            saveLegacy(data)
        }
    }

    private fun saveViaMediaStore(data: ByteArray): DownloadResult? {
        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, CERT_FILE_NAME)
            put(MediaStore.Downloads.MIME_TYPE, "application/x-x509-ca-cert")
            put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$CERT_DIR")
        }
        Log.d("BurpBridge-Cert", "Inserting into MediaStore.Downloads...")
        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        if (uri == null) {
            Log.e("BurpBridge-Cert", "MediaStore.insert returned null URI")
            return null
        }
        Log.d("BurpBridge-Cert", "MediaStore URI: $uri")
        return try {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(data)
                Log.d("BurpBridge-Cert", "Wrote ${data.size} bytes to $uri")
            } ?: run {
                Log.e("BurpBridge-Cert", "openOutputStream returned null for $uri")
                return null
            }
            DownloadResult(
                bytes = data,
                uri = uri,
                filePath = "Downloads/$CERT_DIR/$CERT_FILE_NAME"
            )
        } catch (e: Exception) {
            Log.e("BurpBridge-Cert", "Error writing to MediaStore: ${e.message}", e)
            null
        }
    }

    private fun saveLegacy(data: ByteArray): DownloadResult? {
        return try {
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                CERT_DIR
            )
            Log.d("BurpBridge-Cert", "Legacy save dir: ${dir.absolutePath}")
            dir.mkdirs()
            val file = File(dir, CERT_FILE_NAME)
            FileOutputStream(file).use { it.write(data) }
            Log.d("BurpBridge-Cert", "Saved to ${file.absolutePath}")
            DownloadResult(
                bytes = data,
                uri = Uri.fromFile(file),
                filePath = file.absolutePath
            )
        } catch (e: Exception) {
            Log.e("BurpBridge-Cert", "Legacy save failed: ${e.message}", e)
            null
        }
    }

    fun getDownloadUrl(ip: String, port: Int): String {
        return "http://$ip:$port/cert"
    }

    fun getDownloadedMessage(): String {
        return "The Burp CA certificate has been saved to your Downloads/$CERT_DIR/ folder as '$CERT_FILE_NAME'.\n\n" +
               "Due to Android security restrictions, automatic installation was not possible.\n\n" +
               "1. Go to Settings → Encryption & Credentials\n" +
               "2. Tap 'Install a certificate'\n" +
               "3. Select 'CA certificate'\n" +
               "4. Choose '$CERT_DIR/$CERT_FILE_NAME' from the file picker"
    }

    fun getFailedMessage(downloadUrl: String): String {
        return "Could not download the certificate from the proxy server.\n\n" +
               "Make sure Burp Suite is running and accessible at:\n$downloadUrl\n\n" +
               "To install the Burp CA certificate manually:\n" +
               "1. Open Burp Suite on your computer (must be running)\n" +
               "2. Open this URL in a browser on this device:\n   $downloadUrl\n" +
               "3. Save the downloaded file to your Downloads folder\n" +
               "4. Go to Settings → Encryption & Credentials\n" +
               "5. Tap 'Install a certificate'\n" +
               "6. Select 'CA certificate'\n" +
               "7. Choose the downloaded file"
    }
}
