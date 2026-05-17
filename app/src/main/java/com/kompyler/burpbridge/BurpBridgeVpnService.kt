package com.kompyler.burpbridge

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.pm.ServiceInfo
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import java.util.concurrent.atomic.AtomicBoolean
import mobile.Mobile

@SuppressLint("VpnServicePolicy")
class BurpBridgeVpnService : VpnService() {

    companion object {
        const val ACTION_START = "com.kompyler.burpbridge.START_VPN"
        const val ACTION_STOP = "com.kompyler.burpbridge.STOP_VPN"
        const val ACTION_STATUS = "com.kompyler.burpbridge.STATUS_CHANGED"
        const val EXTRA_VPN_STATUS = "vpn_status"
        const val EXTRA_PERSISTENT_NOTIFICATION = "persistent_notification"
        const val EXTRA_ALERT_ON_INTERCEPT = "alert_on_intercept"
        const val VPN_STATUS_STARTED = "started"
        const val VPN_STATUS_STOPPED = "stopped"
        const val VPN_STATUS_STARTING = "starting"
        const val VPN_STATUS_STOPPING = "stopping"

        private const val STOP_TIMEOUT_MS = 5000L
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "burpbridge_vpn_channel"

        private var instance: BurpBridgeVpnService? = null
        fun isRunning(): Boolean = instance != null
    }

    private var vpnInterface: android.os.ParcelFileDescriptor? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isStopping = false
    private var stopCompleted = false
    private val stopProxyGuard = AtomicBoolean(false)
    
    private var persistentNotificationEnabled = true
    private var alertOnInterceptEnabled = false

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
        Log.d("BurpBridge", "Service created, instance set")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("BurpBridge", "onStartCommand received with action: ${intent?.action}")

        // Extract settings from intent
        persistentNotificationEnabled = intent?.getBooleanExtra(EXTRA_PERSISTENT_NOTIFICATION, true) ?: true
        alertOnInterceptEnabled = intent?.getBooleanExtra(EXTRA_ALERT_ON_INTERCEPT, false) ?: false

        when (intent?.action) {
            ACTION_STOP -> {
                Log.i("BurpBridge", "STOP_VPN command received")
                stopVpn()
                return START_NOT_STICKY
            }
            ACTION_START -> {
                val targetAddress = intent.getStringExtra("TARGET_ADDRESS")
                if (targetAddress.isNullOrBlank()) {
                    Log.e("BurpBridge", "No target address provided")
                    return START_NOT_STICKY
                }
                broadcastStatus(VPN_STATUS_STARTING)
                startVpn(targetAddress)
            }
        }
        
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "BurpBridge VPN",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when BurpBridge proxy is active"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showForegroundNotification() {
        if (!persistentNotificationEnabled) {
            Log.d("BurpBridge", "Persistent notification disabled, skipping")
            return
        }

        // Intent to open app when notification is tapped
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Disconnect action
        val disconnectIntent = Intent(this, BurpBridgeVpnService::class.java).apply {
            action = ACTION_STOP
        }
        val disconnectPendingIntent = PendingIntent.getService(
            this, 1, disconnectIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("BurpBridge Proxy Active")
            .setContentText("Intercepting network traffic")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(openAppPendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Disconnect",
                disconnectPendingIntent
            )
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

        startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        Log.d("BurpBridge", "Foreground notification started")
    }

    private fun hideForegroundNotification() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        Log.d("BurpBridge", "Foreground notification removed")
    }

    private fun showAlertNotification(isStarting: Boolean) {
        if (!alertOnInterceptEnabled) return

        val title = if (isStarting) "BurpBridge Started" else "BurpBridge Stopped"
        val text = if (isStarting) "Proxy is now intercepting traffic" else "Proxy has been disconnected"

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID + 1, notification)
    }

    private fun startVpn(targetAddress: String) {
        if (isStopping) {
            Log.w("BurpBridge", "Cannot start VPN - service is stopping")
            return
        }

        Thread {
            try {
                val builder = Builder()
                    .addAddress("10.0.0.1", 24)
                    .addRoute("0.0.0.0", 0)
                    .setSession("BurpBridge")
                    .setMtu(1500)
                    .addDisallowedApplication(packageName)

                vpnInterface = builder.establish()
                val fd = vpnInterface?.fd ?: throw Exception("Failed to establish VPN")

                Mobile.startAndroidProxy(fd.toLong(), targetAddress)

                Log.i("BurpBridge", "VPN started successfully")
                
                handler.post {
                    showForegroundNotification()
                    if (alertOnInterceptEnabled) {
                        showAlertNotification(true)
                    }
                }
                broadcastStatus(VPN_STATUS_STARTED)

            } catch (e: Exception) {
                Log.e("BurpBridge", "Error starting proxy: ${e.message}")
                broadcastStatus(VPN_STATUS_STOPPED)
            }
        }.start()
    }

    private fun stopVpn() {
        if (stopCompleted) {
            Log.d("BurpBridge", "VPN already stopped, ignoring stop request")
            return
        }
        
        if (isStopping) {
            Log.d("BurpBridge", "Stop already in progress, waiting...")
            var waited = 0
            while (isStopping && waited < STOP_TIMEOUT_MS) {
                try {
                    Thread.sleep(100)
                    waited += 100
                } catch (e: InterruptedException) {
                    break
                }
            }
            return
        }
        
        isStopping = true
        broadcastStatus(VPN_STATUS_STOPPING)
        Log.i("BurpBridge", "Stopping VPN...")

        // Step 1: Close VPN interface immediately
        try {
            vpnInterface?.close()
            Log.d("BurpBridge", "VPN interface closed synchronously")
        } catch (e: Exception) {
            Log.e("BurpBridge", "Error closing VPN interface: ${e.message}")
        }
        vpnInterface = null

        // Step 2: Stop Go proxy in background
        Thread {
                val stopThread = Thread {
                    if (stopProxyGuard.compareAndSet(false, true)) {
                        try {
                            Log.d("BurpBridge", "Stopping Go proxy...")
                            Mobile.stopProxy()
                            Log.d("BurpBridge", "Go proxy stopped")
                        } catch (e: Exception) {
                            Log.e("BurpBridge", "Error stopping Go engine: ${e.message}")
                        }
                    } else {
                        Log.d("BurpBridge", "Go proxy already stopped by onDestroy")
                    }
                }
            
            stopThread.start()
            
            try {
                stopThread.join(5000)
                if (stopThread.isAlive) {
                    Log.w("BurpBridge", "Go stop timed out, continuing anyway")
                }
            } catch (e: InterruptedException) {
                Log.e("BurpBridge", "Interrupted while waiting for Go stop")
            }

            // Step 3: Full cleanup
            Log.i("BurpBridge", "Performing full cleanup...")
            
            handler.post {
                hideForegroundNotification()
                
                if (alertOnInterceptEnabled) {
                    showAlertNotification(false)
                }
                
                stopCompleted = true
                isStopping = false
                instance = null
                
                broadcastStatus(VPN_STATUS_STOPPED)
                
                // Now destroy the service
                stopSelf()
                Log.d("BurpBridge", "Service destroyed after full stop")
            }
        }.start()
    }

    private fun broadcastStatus(status: String) {
        val intent = Intent(ACTION_STATUS).apply {
            putExtra(EXTRA_VPN_STATUS, status)
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("BurpBridge", "VPN Service destroyed")
        
        if (stopProxyGuard.compareAndSet(false, true)) {
            try {
                Mobile.stopProxy()
            } catch (e: Exception) {
                Log.e("BurpBridge", "Error stopping Go engine: ${e.message}")
            }
        } else {
            Log.d("BurpBridge", "onDestroy: Go proxy already stopped")
        }

        try {
            vpnInterface?.close()
        } catch (e: Exception) {
            Log.e("BurpBridge", "Error closing VPN interface: ${e.message}")
        }
        vpnInterface = null
        instance = null
    }

    override fun onRevoke() {
        super.onRevoke()
        Log.w("BurpBridge", "VPN permission revoked")
        stopVpn()
    }
}