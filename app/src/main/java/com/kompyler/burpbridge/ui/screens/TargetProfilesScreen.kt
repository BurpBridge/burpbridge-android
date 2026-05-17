package com.kompyler.burpbridge.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kompyler.burpbridge.ui.TargetProfile
import com.kompyler.burpbridge.ui.theme.CyberCardOutline
import com.kompyler.burpbridge.ui.theme.CyberCardSurface
import com.kompyler.burpbridge.ui.theme.CyberOrange
import com.kompyler.burpbridge.ui.theme.CyberSecondaryText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TargetProfilesScreen(
    profiles: List<TargetProfile>,
    selectedProfileId: String?,
    onSelectProfile: (String?) -> Unit,
    onAddProfile: (name: String, targetIp: String, httpPort: Int, httpsPort: Int) -> Unit,
    onUpdateProfile: (TargetProfile) -> Unit,
    onDeleteProfile: (String) -> Unit,
    onBack: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingProfile by remember { mutableStateOf<TargetProfile?>(null) }

    if (showAddDialog) {
        ProfileEditDialog(
            title = "Add Profile",
            initial = null,
            onSave = { name, ip, http, https ->
                onAddProfile(name, ip, http, https)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    editingProfile?.let { profile ->
        ProfileEditDialog(
            title = "Edit Profile",
            initial = profile,
            onSave = { name, ip, http, https ->
                onUpdateProfile(profile.copy(name = name, targetIp = ip, httpPort = http, httpsPort = https))
                editingProfile = null
            },
            onDismiss = { editingProfile = null }
        )
    }

    Scaffold(
        containerColor = Color(0xFF141414),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Target Profiles",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add profile",
                            tint = CyberOrange
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CyberCardSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            if (profiles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No profiles yet.\nTap + to add one.",
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        color = CyberSecondaryText,
                        lineHeight = 22.sp
                    )
                }
            }

            profiles.forEach { profile ->
                val isSelected = profile.id == selectedProfileId
                ProfileCard(
                    profile = profile,
                    isSelected = isSelected,
                    onSelect = { onSelectProfile(profile.id) },
                    onEdit = { editingProfile = profile },
                    onDelete = { onDeleteProfile(profile.id) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = CyberOrange
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(CyberOrange.copy(alpha = 0.5f))
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ADD NEW PROFILE",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun ProfileCard(
    profile: TargetProfile,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isSelected) CyberOrange.copy(alpha = 0.06f) else CyberCardSurface
            )
            .border(
                width = 1.dp,
                color = if (isSelected) CyberOrange.copy(alpha = 0.3f) else CyberCardOutline,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onSelect() }
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = profile.name,
                        fontSize = 15.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                        color = if (isSelected) CyberOrange else Color.White
                    )
                    if (isSelected) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ACTIVE",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = CyberOrange,
                            modifier = Modifier
                                .background(
                                    CyberOrange.copy(alpha = 0.15f),
                                    RoundedCornerShape(3.dp)
                                )
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${profile.targetIp}:${profile.httpPort}",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = CyberSecondaryText
                )
            }

            IconButton(
                onClick = onEdit,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit",
                    tint = CyberSecondaryText.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color(0xFFCC4444).copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun ProfileEditDialog(
    title: String,
    initial: TargetProfile?,
    onSave: (name: String, targetIp: String, httpPort: Int, httpsPort: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var ip by remember { mutableStateOf(initial?.targetIp ?: "") }
    var httpPortText by remember { mutableStateOf(initial?.httpPort?.toString() ?: "") }
    var httpsPortText by remember { mutableStateOf(initial?.httpsPort?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CyberCardSurface,
        titleContentColor = Color.White,
        textContentColor = Color.White,
        title = {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DialogTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Profile Name",
                    placeholder = "My Proxy"
                )
                DialogTextField(
                    value = ip,
                    onValueChange = { ip = it },
                    label = "Target IP",
                    placeholder = "127.0.0.1"
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DialogTextField(
                        value = httpPortText,
                        onValueChange = { httpPortText = it },
                        label = "HTTP Port",
                        placeholder = "8080",
                        modifier = Modifier.weight(1f),
                        keyboardType = KeyboardType.Number
                    )
                    DialogTextField(
                        value = httpsPortText,
                        onValueChange = { httpsPortText = it },
                        label = "HTTPS Port",
                        placeholder = "8443",
                        modifier = Modifier.weight(1f),
                        keyboardType = KeyboardType.Number
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val httpPort = httpPortText.toIntOrNull() ?: 8080
                    val httpsPort = httpsPortText.toIntOrNull() ?: 8443
                    if (name.isNotBlank() && ip.isNotBlank()) {
                        onSave(name.trim(), ip.trim(), httpPort, httpsPort)
                    }
                },
                enabled = name.isNotBlank() && ip.isNotBlank()
            ) {
                Text(
                    text = "SAVE",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (name.isNotBlank() && ip.isNotBlank()) CyberOrange else CyberSecondaryText
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "CANCEL",
                    fontFamily = FontFamily.Monospace,
                    color = CyberSecondaryText
                )
            }
        }
    )
}

@Composable
private fun DialogTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardType: androidx.compose.ui.text.input.KeyboardType = KeyboardType.Text
) {
    var isFocused by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = CyberSecondaryText,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0D0D0D), RoundedCornerShape(8.dp))
                .border(
                    width = 1.5.dp,
                    color = if (isFocused) CyberOrange else CyberCardOutline,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 14.dp, vertical = 14.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            isFocused = true
                        } else {
                            isFocused = false
                            keyboardController?.hide()
                        }
                    },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType),
                cursorBrush = SolidColor(CyberOrange),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White
                ),
                decorationBox = { innerTextField ->
                    Box {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace,
                                color = CyberSecondaryText.copy(alpha = 0.4f)
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }
    }
}
