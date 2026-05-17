package com.kompyler.burpbridge.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

class CertificateManager(private val context: Context) {

    companion object {
        private const val CERT_FILE_NAME = "burpbridge_ca.der"
    }

    private val certDir: File
        get() = File(context.filesDir, "certs").also { it.mkdirs() }

    fun prepareCertificateDownload(): Uri? {
        return try {
            val certFile = File(certDir, CERT_FILE_NAME)
            
            if (!certFile.exists()) {
                createPlaceholderCertFile(certFile)
            }

            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                certFile
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun createPlaceholderCertFile(file: File) {
        FileOutputStream(file).use { fos ->
            fos.write(ByteArray(0))
        }
    }

    fun createInstallIntent(): Intent {
        val certFile = File(certDir, CERT_FILE_NAME)
        
        if (!certFile.exists()) {
            createPlaceholderCertFile(certFile)
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            certFile
        )

        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/x-x509-ca-cert")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun showInstallPrompt(onNoApp: () -> Unit) {
        val intent = createInstallIntent()
        
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Please download Burp CA certificate from Burp Suite and open it with this app to install",
                Toast.LENGTH_LONG
            ).show()
            onNoApp()
        }
    }

    fun getInstructions(): String {
        return """
            To intercept HTTPS traffic, you need to install the Burp CA certificate:

            1. Open Burp Suite on your computer
            2. Go to Proxy → Proxy settings → Import / Export CA certificate
            3. Export as DER format
            4. Transfer the certificate to this device
            5. Open the certificate file and install it

            For Android 7+ (Nougat), you need to:
            - Root your device, OR
            - Use Magisk to systemlessly install the certificate
        """.trimIndent()
    }
}