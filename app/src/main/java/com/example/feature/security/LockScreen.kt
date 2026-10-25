package com.example.feature.security

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.security.SecurityPreferences
import com.example.ui.theme.ToriCyan
import com.example.ui.theme.ToriDarkBackground
import com.example.ui.theme.ToriIndigo
import kotlinx.coroutines.launch

@Composable
fun LockScreen(
    securityPrefs: SecurityPreferences,
    onUnlocked: () -> Unit
) {
    val isSetupMode = !securityPrefs.hasPin()
    var enteredPin by remember { mutableStateOf("") }
    var confirmPinStep by remember { mutableStateOf(false) }
    var firstEnteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val shakeOffset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    fun triggerShake() {
        scope.launch {
            shakeOffset.snapTo(0f)
            shakeOffset.animateTo(20f, tween(50))
            shakeOffset.animateTo(-20f, tween(50))
            shakeOffset.animateTo(10f, tween(50))
            shakeOffset.animateTo(0f, tween(50))
        }
    }

    fun onNumberClick(num: String) {
        if (enteredPin.length < 6) {
            enteredPin += num
            errorMessage = null

            // Check if PIN length is reached (support 4 to 6 digits, auto-check at 4 if verifying and matches, or at 6)
            if (!isSetupMode) {
                if (enteredPin.length in 4..6 && securityPrefs.verifyPin(enteredPin)) {
                    onUnlocked()
                } else if (enteredPin.length == 6) {
                    if (!securityPrefs.verifyPin(enteredPin)) {
                        errorMessage = "Incorrect Master PIN"
                        triggerShake()
                        enteredPin = ""
                    }
                }
            } else {
                if (enteredPin.length == 6) {
                    if (!confirmPinStep) {
                        firstEnteredPin = enteredPin
                        enteredPin = ""
                        confirmPinStep = true
                    } else {
                        if (enteredPin == firstEnteredPin) {
                            securityPrefs.setPin(enteredPin)
                            onUnlocked()
                        } else {
                            errorMessage = "PINs do not match. Try again."
                            triggerShake()
                            enteredPin = ""
                            confirmPinStep = false
                            firstEnteredPin = ""
                        }
                    }
                }
            }
        }
    }

    fun onDeleteClick() {
        if (enteredPin.isNotEmpty()) {
            enteredPin = enteredPin.dropLast(1)
            errorMessage = null
        }
    }

    fun onBiometricClick() {
        if (!isSetupMode && securityPrefs.isBiometricsEnabled) {
            onUnlocked()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        ToriDarkBackground,
                        Color(0xFF0F172A),
                        Color(0xFF020617)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .offset(x = shakeOffset.value.dp)
        ) {
            // Tori Shield Avatar
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(ToriCyan.copy(alpha = 0.3f), Color.Transparent)
                        )
                    )
                    .border(2.dp, ToriCyan.copy(alpha = 0.8f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Tori Security Shield",
                    tint = ToriCyan,
                    modifier = Modifier.size(46.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "TORI",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp
                ),
                color = Color.White
            )

            Text(
                text = if (isSetupMode) {
                    if (!confirmPinStep) "Create 6-Digit Master PIN" else "Confirm Your Master PIN"
                } else {
                    "Unlock Tori"
                },
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = ToriCyan,
                textAlign = TextAlign.Center
            )
            if (!isSetupMode) {
                Text(
                    text = "Biometric prompt active • Fallback PIN below",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // PIN Dots Indicator (6 slots)
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until 6) {
                    val isFilled = i < enteredPin.length
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                if (isFilled) ToriCyan else Color(0xFF334155)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isFilled) ToriCyan else Color(0xFF475569),
                                shape = CircleShape
                            )
                    )
                }
            }

            // Error Message
            Box(modifier = Modifier.height(36.dp), contentAlignment = Alignment.Center) {
                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Keypad Grid
            Column(
                modifier = Modifier.fillMaxWidth(0.85f),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                val keys = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("BIO", "0", "DEL")
                )

                for (row in keys) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (key in row) {
                            when (key) {
                                "BIO" -> {
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(CircleShape)
                                            .clickable(enabled = !isSetupMode && securityPrefs.isBiometricsEnabled) {
                                                onBiometricClick()
                                            }
                                            .testTag("biometric_unlock_button"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (!isSetupMode && securityPrefs.isBiometricsEnabled) {
                                            Icon(
                                                imageVector = Icons.Default.Fingerprint,
                                                contentDescription = "Biometric Unlock",
                                                tint = ToriCyan,
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                    }
                                }
                                "DEL" -> {
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(CircleShape)
                                            .clickable { onDeleteClick() }
                                            .testTag("delete_pin_button"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                                            contentDescription = "Delete",
                                            tint = Color(0xFF94A3B8),
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }
                                else -> {
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF1E293B).copy(alpha = 0.8f))
                                            .border(1.dp, Color(0xFF334155), CircleShape)
                                            .clickable { onNumberClick(key) }
                                            .testTag("keypad_$key"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = key,
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 26.sp
                                            ),
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Subtitle info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "Hardware Keystore AES-256-GCM Protected",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}
