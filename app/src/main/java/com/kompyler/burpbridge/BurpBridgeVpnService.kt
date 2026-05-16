package com.kompyler.burpbridge

import android.annotation.SuppressLint
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import mobile.Mobile // This is your Go code!

@SuppressLint("VpnServicePolicy")
class BurpBridgeVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("BurpBridge", "onStartCommand received with action: ${intent?.action}")

        // Did the user click the Stop button?
        if (intent?.action == "STOP_VPN") {
            Log.d("BurpBridge", "STOP_VPN received, shutting down...")
            stopSelf() // This triggers onDestroy() which safely shuts down the Go engine
            return START_NOT_STICKY
        }

        // Extract the IP address the user typed into the UI
        val targetAddress = intent?.getStringExtra("TARGET_ADDRESS") ?: return START_NOT_STICKY

        Log.d("BurpBridge", "Starting VPN Service routing to $targetAddress...")

        Thread {
            try {
                // 1. Configure the Android TUN Interface
                val builder = Builder()
                    .addAddress("10.0.0.1", 24)
                    .addRoute("0.0.0.0", 0)
                    .setSession("BurpBridge")
                    .setMtu(1500)
                    .addDisallowedApplication(packageName) // Prevent infinite loop

                // 2. Establish tunnel
                vpnInterface = builder.establish()
                val fd = vpnInterface?.fd ?: throw Exception("Failed to establish VPN")

                // 3. Start the Go Engine using the dynamic IP!
                Mobile.startAndroidProxy(fd.toLong(), targetAddress)

            } catch (e: Exception) {
                Log.e("BurpBridge", "Error starting proxy: ${e.message}")
            }
        }.start()

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("BurpBridge", "Stopping VPN Service...")

        try {
            // Stop the Go Engine first
            Mobile.stopProxy()
        } catch (e: Exception) {
            Log.e("BurpBridge", "Error stopping Go engine: ${e.message}")
        }

        // Close the Android TUN interface
        try {
            vpnInterface?.close()
        } catch (e: Exception) {
            Log.e("BurpBridge", "Error closing VPN interface: ${e.message}")
        }
        vpnInterface = null
        Log.d("BurpBridge", "VPN Service destroyed")
    }
}