package com.kompyler.burpbridge

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BurpBridgeScreen()
                }
            }
        }
    }
}

@Composable
fun BurpBridgeScreen() {
    val context = LocalContext.current

    // State variables for our text fields
    var ipAddress by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("8080") }

    // Helper function to start the VPN service
    fun startVpnService() {
        if (ipAddress.isBlank() || port.isBlank()) {
            Toast.makeText(context, "Please enter IP and Port", Toast.LENGTH_SHORT).show()
            return
        }

        val targetAddress = "${ipAddress.trim()}:${port.trim()}"
        val serviceIntent = Intent(context, BurpBridgeVpnService::class.java).apply {
            putExtra("TARGET_ADDRESS", targetAddress)
            action = "START_VPN"
        }

        context.startService(serviceIntent)
        Toast.makeText(context, "Starting BurpBridge...", Toast.LENGTH_SHORT).show()
    }

    // The modern Compose way to handle the VPN Permission Dialog
    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            startVpnService()
        } else {
            Toast.makeText(context, "VPN Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    // The UI Layout
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "BurpBridge Engine",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        OutlinedTextField(
            value = ipAddress,
            onValueChange = { ipAddress = it },
            label = { Text("Mac IP Address") },
            placeholder = { Text("e.g., 192.168.1.50") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = port,
            onValueChange = { port = it },
            label = { Text("Burp Port") },
            placeholder = { Text("e.g., 8080") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        )

        Button(
            onClick = {
                val intent = VpnService.prepare(context)
                if (intent != null) {
                    // Ask for permission
                    vpnPermissionLauncher.launch(intent)
                } else {
                    // Already have permission, start engine
                    startVpnService()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
        ) {
            Text("Start Transparent Proxy", fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val stopIntent = Intent(context, BurpBridgeVpnService::class.java).apply {
                    action = "STOP_VPN"
                }
                context.startService(stopIntent)
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
        ) {
            Text("Stop Proxy", fontSize = 16.sp)
        }
    }
}
