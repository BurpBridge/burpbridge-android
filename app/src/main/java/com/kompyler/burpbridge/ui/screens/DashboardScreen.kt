package com.kompyler.burpbridge.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kompyler.burpbridge.ui.TargetProfile
import com.kompyler.burpbridge.ui.theme.CyberBackground
import com.kompyler.burpbridge.ui.theme.CyberOrange
import com.kompyler.burpbridge.ui.theme.CyberSecondaryText
import com.kompyler.burpbridge.ui.theme.currentCyberColors

@Composable
fun DashboardScreen(
    isVpnConnected: Boolean,
    onVpnStatusChange: (Boolean) -> Unit,
    onStartProxy: () -> Unit,
    onStopProxy: () -> Unit,
    targetIp: String,
    httpPort: Int,
    onIpChange: (String) -> Unit,
    onPortChange: (Int) -> Unit,
    logs: List<String>,
    sessionDuration: String = "00:00:00",
    selectedProfileId: String? = null,
    targetProfiles: List<TargetProfile> = emptyList(),
    onSelectProfile: (String?) -> Unit = {},
    onNavigateToTargetProfiles: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val editable = !isVpnConnected

    var showProfileSheet by remember { mutableStateOf(false) }

    if (showProfileSheet) {
        ProfileSelectorSheet(
            profiles = targetProfiles,
            selectedProfileId = selectedProfileId,
            onSelect = { id ->
                onSelectProfile(id)
                showProfileSheet = false
            },
            onDismiss = { showProfileSheet = false }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        DottedGridBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            DashboardTopBar(
                sessionDuration = sessionDuration,
                isActive = isVpnConnected
            )

            Spacer(modifier = Modifier.height(24.dp))

            HeroSection(
                isActive = isVpnConnected,
                onStart = onStartProxy,
                onStop = onStopProxy
            )

            Spacer(modifier = Modifier.height(24.dp))

            TargetConfigCard(
                ipAddress = targetIp,
                port = httpPort.toString(),
                onIpChange = onIpChange,
                onPortChange = { onPortChange(it.toIntOrNull() ?: httpPort) },
                editable = editable,
                selectedProfileId = selectedProfileId,
                profiles = targetProfiles,
                onShowProfileSheet = { showProfileSheet = true },
                onNavigateToProfiles = onNavigateToTargetProfiles
            )

            Spacer(modifier = Modifier.height(20.dp))

            SystemLogCard(logs = logs)

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DottedGridBackground() {
    val cyber = currentCyberColors()
    Canvas(modifier = Modifier.fillMaxSize()) {
        val dotSpacing = 24.dp.toPx()
        val dotRadius = 1.2.dp.toPx()
        var x = dotSpacing
        while (x < size.width) {
            var y = dotSpacing
            while (y < size.height) {
                drawCircle(
                    color = cyber.gridDot,
                    radius = dotRadius,
                    center = Offset(x, y)
                )
                y += dotSpacing
            }
            x += dotSpacing
        }
    }
}

@Composable
private fun DashboardTopBar(
    sessionDuration: String,
    isActive: Boolean
) {
    val cyber = currentCyberColors()
    val pulseTransition = rememberInfiniteTransition(label = "pulse")
    val dotAlpha by pulseTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_alpha"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Security,
            contentDescription = null,
            tint = CyberOrange,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = "BURPBRIDGE",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif,
            color = MaterialTheme.colorScheme.onSurface,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.weight(1f))

        Box(
            modifier = Modifier
                .background(
                    color = cyber.cardSurface,
                    shape = RoundedCornerShape(6.dp)
                )
                .border(
                    width = 1.dp,
                    color = cyber.cardOutline,
                    shape = RoundedCornerShape(6.dp)
                )
                .padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isActive) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(CyberOrange.copy(alpha = dotAlpha))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = sessionDuration,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    color = if (isActive) CyberOrange else cyber.secondaryText
                )
            }
        }
    }
}

@Composable
private fun HeroSection(
    isActive: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = if (isActive) "PROXY ACTIVE" else "PROXY OFFLINE",
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) CyberOrange else CyberSecondaryText,
                    letterSpacing = 3.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .width(80.dp)
                .height(1.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = if (isActive) {
                            listOf(Color.Transparent, CyberOrange, Color.Transparent)
                        } else {
                            listOf(Color.Transparent, CyberSecondaryText.copy(alpha = 0.3f), Color.Transparent)
                        }
                    )
                )
        )

        Spacer(modifier = Modifier.height(32.dp))

        PowerButton(
            isActive = isActive,
            onClick = { if (isActive) onStop() else onStart() }
        )
    }
}

@Composable
private fun PowerButton(
    isActive: Boolean,
    onClick: () -> Unit
) {
    val cyber = currentCyberColors()

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(160.dp)
    ) {
        Canvas(modifier = Modifier.size(160.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            val rectSize = size.width * 0.65f
            val rectOffset = rectSize / 2

            drawRect(
                color = if (isActive) CyberOrange.copy(alpha = 0.06f) else CyberOrange.copy(alpha = 0.02f),
                topLeft = Offset(center.x - rectOffset, center.y - rectOffset),
                size = androidx.compose.ui.geometry.Size(rectSize, rectSize),
                style = Stroke(width = 1.dp.toPx()),
                alpha = if (isActive) 0.6f else 0.15f
            )
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = if (isActive) {
                            listOf(cyber.powerButtonBg1, cyber.powerButtonBg2)
                        } else {
                            listOf(cyber.powerButtonOff1, cyber.powerButtonOff2)
                        }
                    )
                )
                .border(
                    width = 1.5.dp,
                    color = if (isActive) {
                        CyberOrange.copy(alpha = 0.3f)
                    } else {
                        cyber.powerButtonBorder
                    },
                    shape = CircleShape
                )
                .clickable { onClick() }
        ) {
            if (isActive) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                radius = 120f,
                                colors = listOf(
                                    CyberOrange.copy(alpha = 0.15f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }

            Icon(
                imageVector = Icons.Default.PowerSettingsNew,
                contentDescription = if (isActive) "Stop Proxy" else "Start Proxy",
                tint = if (isActive) CyberOrange else cyber.secondaryText,
                modifier = Modifier.size(42.dp)
            )
        }
    }
}

@Composable
private fun TargetConfigCard(
    ipAddress: String,
    port: String,
    onIpChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    editable: Boolean,
    selectedProfileId: String?,
    profiles: List<TargetProfile>,
    onShowProfileSheet: () -> Unit,
    onNavigateToProfiles: () -> Unit
) {
    val cyber = currentCyberColors()
    val selectedProfile = profiles.find { it.id == selectedProfileId }
    val isCustom = selectedProfileId == null

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(cyber.cardSurface, RoundedCornerShape(10.dp))
            .border(1.dp, cyber.cardOutline, RoundedCornerShape(10.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Target Configuration",
                fontSize = 12.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                color = cyber.secondaryText,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.weight(1f))

            IconButton(
                onClick = onNavigateToProfiles,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Manage profiles",
                    tint = cyber.secondaryText.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onShowProfileSheet() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isCustom) "Custom" else selectedProfile?.name ?: "Custom",
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                color = if (isCustom) cyber.secondaryText else CyberOrange
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Select profile",
                tint = cyber.secondaryText,
                modifier = Modifier.size(16.dp)
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 12.dp),
            color = cyber.cardOutline,
            thickness = 1.dp
        )

        if (editable && isCustom) {
            EditableConfigRow(
                icon = Icons.Default.Dns,
                label = "Target IP",
                value = ipAddress,
                onValueChange = onIpChange,
                placeholder = "192.168.1.50"
            )

            Spacer(modifier = Modifier.height(12.dp))

            EditableConfigRow(
                icon = Icons.Default.SettingsEthernet,
                label = "Burp Port",
                value = port,
                onValueChange = onPortChange,
                placeholder = "8080",
                isNumeric = true
            )
        } else {
            ConfigRow(
                icon = Icons.Default.Dns,
                label = "Target IP",
                value = ipAddress
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = cyber.cardOutline,
                thickness = 1.dp
            )

            ConfigRow(
                icon = Icons.Default.SettingsEthernet,
                label = "Burp Port",
                value = port
            )
        }
    }
}

@Composable
private fun ConfigRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    val cyber = currentCyberColors()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = cyber.secondaryText,
            modifier = Modifier.size(18.dp)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column {
            Text(
                text = label,
                fontSize = 10.sp,
                fontFamily = FontFamily.SansSerif,
                color = cyber.secondaryText.copy(alpha = 0.6f)
            )
            Text(
                text = value,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun EditableConfigRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isNumeric: Boolean = false
) {
    val cyber = currentCyberColors()
    var localValue by remember { mutableStateOf(value) }
    var isFocused by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(value) {
        if (!isFocused) {
            localValue = value
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = cyber.secondaryText,
            modifier = Modifier.size(18.dp)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 10.sp,
                fontFamily = FontFamily.SansSerif,
                color = cyber.secondaryText.copy(alpha = 0.6f)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(cyber.inputBackground, RoundedCornerShape(6.dp))
                    .border(
                        width = 1.5.dp,
                        color = if (isFocused) CyberOrange else cyber.cardOutline,
                        shape = RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 14.dp)
            ) {
                BasicTextField(
                    value = localValue,
                    onValueChange = { text ->
                        localValue = text
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                isFocused = true
                            } else {
                                isFocused = false
                                keyboardController?.hide()
                                if (isNumeric) {
                                    localValue.toIntOrNull()?.let { onValueChange(localValue) }
                                } else {
                                    onValueChange(localValue)
                                }
                            }
                        },
                    singleLine = true,
                    cursorBrush = SolidColor(CyberOrange),
                    textStyle = TextStyle(
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    decorationBox = { innerTextField ->
                        Box {
                            if (localValue.isEmpty()) {
                                Text(
                                    text = placeholder,
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = cyber.secondaryText.copy(alpha = 0.4f)
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileSelectorSheet(
    profiles: List<TargetProfile>,
    selectedProfileId: String?,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    val cyber = currentCyberColors()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = cyber.cardSurface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Select Profile",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (selectedProfileId == null) CyberOrange.copy(alpha = 0.1f) else Color.Transparent
                    )
                    .border(
                        width = 1.dp,
                        color = if (selectedProfileId == null) CyberOrange.copy(alpha = 0.3f) else Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { onSelect(null) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = if (selectedProfileId == null) CyberOrange else cyber.secondaryText,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Custom",
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    color = if (selectedProfileId == null) CyberOrange else cyber.secondaryText
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            profiles.forEach { profile ->
                val isSelected = profile.id == selectedProfileId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSelected) CyberOrange.copy(alpha = 0.1f) else Color.Transparent
                        )
                        .border(
                            width = 1.dp,
                            color = if (isSelected) CyberOrange.copy(alpha = 0.3f) else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { onSelect(profile.id) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Dns,
                        contentDescription = null,
                        tint = if (isSelected) CyberOrange else cyber.secondaryText,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = profile.name,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                            color = if (isSelected) CyberOrange else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${profile.targetIp}:${profile.httpPort}",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = cyber.secondaryText
                        )
                    }
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = CyberOrange,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun SystemLogCard(logs: List<String>) {
    val cyber = currentCyberColors()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(cyber.cardSurface, RoundedCornerShape(10.dp))
            .border(1.dp, cyber.cardOutline, RoundedCornerShape(10.dp))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Terminal,
                contentDescription = null,
                tint = CyberOrange,
                modifier = Modifier.size(16.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "System Log",
                fontSize = 12.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                color = cyber.secondaryText,
                letterSpacing = 1.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(cyber.terminalBackground, RoundedCornerShape(6.dp))
                .border(1.dp, cyber.cardOutline, RoundedCornerShape(6.dp))
                .padding(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                if (logs.isEmpty()) {
                    LogLine(text = "> System standing by...")
                } else {
                    logs.takeLast(15).forEach { log ->
                        LogLine(text = log)
                    }
                }

                val blinkTransition = rememberInfiniteTransition(label = "blink")
                val cursorAlpha by blinkTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(500),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "cursor_alpha"
                )

                Text(
                    text = "_",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = CyberOrange.copy(alpha = cursorAlpha)
                )
            }
        }
    }
}

@Composable
private fun LogLine(text: String) {
    val cyber = currentCyberColors()
    val annotated = buildAnnotatedString {
        val regex = Regex("\\[.*?\\]")
        var lastIndex = 0
        regex.findAll(text).forEach { match ->
            append(text.substring(lastIndex, match.range.first))
            withStyle(SpanStyle(color = CyberOrange)) {
                append(match.value)
            }
            lastIndex = match.range.last + 1
        }
        if (lastIndex < text.length) {
            append(text.substring(lastIndex))
        }
    }

    Text(
        text = annotated,
        fontSize = 12.sp,
        fontFamily = FontFamily.Monospace,
        color = cyber.secondaryText
    )
}
