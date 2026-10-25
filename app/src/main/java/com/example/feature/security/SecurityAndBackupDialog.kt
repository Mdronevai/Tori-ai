package com.example.feature.security

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.data.BackupPreview
import com.example.core.data.Task
import com.example.core.data.VaultCategory
import com.example.core.data.VaultEntry
import com.example.core.security.SecurityPreferences
import com.example.ui.theme.ToriCyan
import com.example.ui.theme.ToriErrorRed
import com.example.ui.theme.ToriSuccessGreen
import com.example.ui.theme.ToriWarningAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityAndBackupDialog(
    vaultEntries: List<VaultEntry>,
    tasks: List<Task>,
    securityPrefs: SecurityPreferences,
    onDismiss: () -> Unit,
    onChangePin: () -> Unit,
    onExportBackup: (passphrase: String, onResult: (String) -> Unit) -> Unit,
    onImportBackupPreview: (encryptedData: String, passphrase: String, onResult: (Result<BackupPreview>) -> Unit) -> Unit,
    onConfirmImport: (BackupPreview) -> Unit,
    onResetAllData: () -> Unit,
    onNotification: (String) -> Unit
) {
    val clipboardManager: ClipboardManager = LocalClipboardManager.current

    var selectedTab by remember { mutableIntStateOf(0) }
    var biometricsEnabled by remember { mutableStateOf(securityPrefs.isBiometricsEnabled) }

    var exportPassphrase by remember { mutableStateOf("") }
    var exportedPayload by remember { mutableStateOf<String?>(null) }
    var isExporting by remember { mutableStateOf(false) }

    var importPassphrase by remember { mutableStateOf("") }
    var importPayload by remember { mutableStateOf("") }
    var importPreview by remember { mutableStateOf<BackupPreview?>(null) }
    var importError by remember { mutableStateOf<String?>(null) }
    var isImporting by remember { mutableStateOf(false) }

    var showResetConfirmation by remember { mutableStateOf(false) }

    // Calculate Audit
    val weakPasswordsCount = remember(vaultEntries) {
        vaultEntries.count { it.category == VaultCategory.PASSWORDS.name && it.secret.length < 10 }
    }
    val reusedPasswordsCount = remember(vaultEntries) {
        val secrets = vaultEntries.map { it.secret }
        secrets.size - secrets.distinct().size
    }
    val overdueTasksCount = remember(tasks) {
        tasks.count { !it.isDone && it.dueAt != null && it.dueAt < System.currentTimeMillis() }
    }

    val securityScore = remember(vaultEntries, weakPasswordsCount, reusedPasswordsCount) {
        if (vaultEntries.isEmpty()) 100
        else {
            val base = 100
            val penalties = (weakPasswordsCount * 15) + (reusedPasswordsCount * 20)
            (base - penalties).coerceIn(20, 100)
        }
    }

    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            title = { Text("Emergency Wipe Data?") },
            text = { Text("This will permanently delete all encrypted vault entries, tasks, chat logs, and reset your Master PIN. This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirmation = false
                        onResetAllData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ToriErrorRed)
                ) {
                    Text("Permanently Wipe All Data")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.9f),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = ToriCyan,
                            modifier = Modifier.size(26.dp)
                        )
                        Text(
                            text = "Security & Backups",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Navigation Tabs
                PrimaryTabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Vault Health & Lock") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Encrypted Backup") }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (selectedTab == 0) {
                        // Health Audit Card
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Security Health Score", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                    Text(
                                        "$securityScore%",
                                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (securityScore > 75) ToriSuccessGreen else if (securityScore > 50) ToriWarningAmber else ToriErrorRed
                                    )
                                }

                                LinearProgressIndicator(
                                    progress = { securityScore / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = if (securityScore > 75) ToriSuccessGreen else if (securityScore > 50) ToriWarningAmber else ToriErrorRed
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    AuditMetric(label = "Vault Items", value = "${vaultEntries.size}", color = ToriCyan)
                                    AuditMetric(label = "Weak Passwords", value = "$weakPasswordsCount", color = if (weakPasswordsCount > 0) ToriErrorRed else ToriSuccessGreen)
                                    AuditMetric(label = "Reused", value = "$reusedPasswordsCount", color = if (reusedPasswordsCount > 0) ToriWarningAmber else ToriSuccessGreen)
                                    AuditMetric(label = "Overdue Tasks", value = "$overdueTasksCount", color = if (overdueTasksCount > 0) ToriErrorRed else ToriSuccessGreen)
                                }
                            }
                        }

                        // Encryption Hardware Banner
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(ToriCyan.copy(alpha = 0.1f))
                                .border(1.dp, ToriCyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = ToriCyan)
                            Column {
                                Text(
                                    "Android Keystore Active",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "AES-256-GCM hardware key never leaves device storage. Zero silent sync.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // PIN & Lock Options
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text("Lock & Authentication", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Master PIN", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                                        Text("Configured 6-digit access PIN", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                    }
                                    OutlinedButton(onClick = onChangePin) {
                                        Text("Change PIN")
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Biometric Authentication", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                                        Text("Unlock with fingerprint / face", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                    }
                                    Switch(
                                        checked = biometricsEnabled,
                                        onCheckedChange = {
                                            biometricsEnabled = it
                                            securityPrefs.isBiometricsEnabled = it
                                        }
                                    )
                                }
                            }
                        }

                        // Danger Zone
                        OutlinedButton(
                            onClick = { showResetConfirmation = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ToriErrorRed),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Emergency Wipe Data")
                        }
                    } else {
                        // Tab 1: Encrypted Backup & Restore
                        Text(
                            text = "Manual Encrypted Backup",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Backups are encrypted using PBKDF2/AES-256 with your private passphrase. They are never transmitted over network without your action.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Export Box
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("Export Vault & Tasks", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))

                                OutlinedTextField(
                                    value = exportPassphrase,
                                    onValueChange = { exportPassphrase = it },
                                    label = { Text("Encryption Passphrase") },
                                    placeholder = { Text("Enter a strong passphrase") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Button(
                                    onClick = {
                                        if (exportPassphrase.length >= 6) {
                                            isExporting = true
                                            onExportBackup(exportPassphrase) { payload ->
                                                exportedPayload = payload
                                                isExporting = false
                                                onNotification("Backup payload encrypted successfully")
                                            }
                                        } else {
                                            onNotification("Passphrase must be at least 6 characters")
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Generate Encrypted Payload")
                                }

                                if (exportedPayload != null) {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("Encrypted Payload (AES-256):", style = MaterialTheme.typography.labelSmall)
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(max = 100.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.surface)
                                                .padding(10.dp)
                                                .verticalScroll(rememberScrollState())
                                        ) {
                                            Text(
                                                exportedPayload ?: "",
                                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                                            )
                                        }
                                        Button(
                                            onClick = {
                                                clipboardManager.setText(AnnotatedString(exportedPayload ?: ""))
                                                onNotification("Encrypted backup copied to clipboard")
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Copy Encrypted Backup String")
                                        }
                                    }
                                }
                            }
                        }

                        // Import Box
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("Restore / Import Backup", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))

                                OutlinedTextField(
                                    value = importPayload,
                                    onValueChange = { importPayload = it },
                                    label = { Text("Paste Encrypted Payload") },
                                    maxLines = 3,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = importPassphrase,
                                    onValueChange = { importPassphrase = it },
                                    label = { Text("Decryption Passphrase") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Button(
                                    onClick = {
                                        if (importPayload.isNotBlank() && importPassphrase.isNotBlank()) {
                                            isImporting = true
                                            onImportBackupPreview(importPayload.trim(), importPassphrase) { result ->
                                                isImporting = false
                                                result.fold(
                                                    onSuccess = { preview ->
                                                        importPreview = preview
                                                        importError = null
                                                    },
                                                    onFailure = {
                                                        importError = "Failed to decrypt: Check passphrase or payload."
                                                    }
                                                )
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Preview Import")
                                }

                                if (importError != null) {
                                    Text(importError ?: "", color = ToriErrorRed, style = MaterialTheme.typography.bodySmall)
                                }

                                if (importPreview != null) {
                                    val preview = importPreview!!
                                    Card(
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text("Backup Contents Found:", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                            Text("• ${preview.vaultDrafts.size} Vault items", style = MaterialTheme.typography.bodySmall)
                                            Text("• ${preview.taskDrafts.size} Tasks", style = MaterialTheme.typography.bodySmall)

                                            Spacer(modifier = Modifier.height(4.dp))

                                            Button(
                                                onClick = {
                                                    onConfirmImport(preview)
                                                    importPreview = null
                                                    importPayload = ""
                                                    importPassphrase = ""
                                                    onNotification("Backup imported and merged successfully")
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = ToriSuccessGreen),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("Confirm & Merge into Vault")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuditMetric(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
