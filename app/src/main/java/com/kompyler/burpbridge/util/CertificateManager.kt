package com.kompyler.burpbridge.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CertificateManager(private val context: Context) {

    companion object {
        private const val CERT_FILE_NAME = "burp_cacert.der"
    }

    suspend fun downloadCertificate(ip: String, port: Int): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = URL("http://$ip:$port/cert")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext Result.failure(
                    Exception("Server returned HTTP $responseCode")
                )
            }

            val bytes = connection.inputStream.use { it.readBytes() }
            if (bytes.isEmpty()) {
                return@withContext Result.failure(
                    Exception("Server returned an empty certificate")
                )
            }

            val savedPath = saveToDownloads(bytes)
            if (savedPath != null) {
                Result.success(savedPath)
            } else {
                Result.failure(Exception("Could not save certificate to Downloads folder"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun saveToDownloads(data: ByteArray): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveViaMediaStore(data)
        } else {
            saveLegacy(data)
        }
    }

    private fun saveViaMediaStore(data: ByteArray): String? {
        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, CERT_FILE_NAME)
            put(MediaStore.Downloads.MIME_TYPE, "application/x-x509-ca-cert")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        return uri?.let {
            context.contentResolver.openOutputStream(it)?.use { stream ->
                stream.write(data)
            }
            it.toString()
        }
    }

    private fun saveLegacy(data: ByteArray): String? {
        return try {
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            dir.mkdirs()
            val file = File(dir, CERT_FILE_NAME)
            FileOutputStream(file).use { it.write(data) }
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    fun createSecuritySettingsIntent(): Intent {
        return Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun getSuccessMessage(): String {
        return "The Burp CA certificate has been saved to your Downloads folder as '$CERT_FILE_NAME'.\n\n" +
               "Due to Android security restrictions, you must install it manually.\n\n" +
               "1. Go to Encryption & Credentials\n" +
               "2. Tap 'Install a certificate'\n" +
               "3. Select 'CA certificate'\n" +
               "4. Choose the downloaded file"
    }

    fun getManualInstructions(): String {
        return "Could not download the certificate from the proxy server.\n\n" +
               "To install the Burp CA certificate manually:\n\n" +
               "1. Open Burp Suite on your computer\n" +
               "2. Go to Proxy → Proxy settings → Import / Export CA certificate\n" +
               "3. Export as DER format\n" +
               "4. Transfer the file to this device\n" +
               "5. Save it to your Downloads folder\n" +
               "6. Go to Settings → Encryption & Credentials → Install a certificate → CA certificate\n" +
               "7. Choose the file from Downloads"
    }
}
