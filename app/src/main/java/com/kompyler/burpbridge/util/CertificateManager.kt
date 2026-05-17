package com.kompyler.burpbridge.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.security.KeyChain
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

            val saved = saveToDownloads(bytes)
            saved?.let {
                Result.success(it)
            } ?: Result.failure(Exception("Could not save certificate to Downloads/burpsuite/"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun saveToDownloads(data: ByteArray): DownloadResult? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveViaMediaStore(data)
        } else {
            saveLegacy(data)
        }
    }

    private fun saveViaMediaStore(data: ByteArray): DownloadResult? {
        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, CERT_FILE_NAME)
            put(MediaStore.Downloads.MIME_TYPE, "application/x-x509-ca-cert")
            put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$CERT_DIR")
        }
        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        return uri?.let {
            context.contentResolver.openOutputStream(it)?.use { stream ->
                stream.write(data)
            }
            DownloadResult(
                bytes = data,
                uri = it,
                filePath = "Downloads/$CERT_DIR/$CERT_FILE_NAME"
            )
        }
    }

    private fun saveLegacy(data: ByteArray): DownloadResult? {
        return try {
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                CERT_DIR
            )
            dir.mkdirs()
            val file = File(dir, CERT_FILE_NAME)
            FileOutputStream(file).use { it.write(data) }
            DownloadResult(
                bytes = data,
                uri = Uri.fromFile(file),
                filePath = file.absolutePath
            )
        } catch (e: Exception) {
            null
        }
    }

    fun createKeyChainInstallIntent(certBytes: ByteArray): Intent {
        return KeyChain.createInstallIntent().apply {
            putExtra(KeyChain.EXTRA_CERTIFICATE, certBytes)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun createFileInstallIntent(certUri: Uri): Intent {
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(certUri, "application/x-x509-ca-cert")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun createSecuritySettingsIntent(): Intent {
        return Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun getDownloadUrl(ip: String, port: Int): String {
        return "http://$ip:$port/cert"
    }

    fun getSuccessMessage(): String {
        return "Certificate installation has been initiated. Follow the system prompts to complete the installation."
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
