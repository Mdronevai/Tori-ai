package com.example.feature.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.data.BackupManager
import com.example.core.data.BackupPreview
import com.example.core.data.ExportFormat
import com.example.core.data.Task
import com.example.core.data.VaultEntry
import com.example.core.security.SecurityPreferences
import com.example.ui.theme.ToriCyan
import com.example.ui.theme.ToriErrorRed
import com.example.ui.theme.ToriIndigo
import com.example.ui.theme.ToriSuccessGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    securityPrefs: SecurityPreferences,
    userName: String,
    vaultEntries: List<VaultEntry>,
    tasks: List<Task>,
    decryptSecret: (String) -> String,
    onUpdateUserName: (String) -> Unit,
    onChangePin: () -> Unit,
    onLockVault: () -> Unit,
    onConfirmImport: (BackupPreview) -> Unit,
    onResetAllData: () -> Unit,
    onShowSnackbar: (String) -> Unit,
    onToggleFloatingSidebar: ((Boolean) -> Unit)? = null,
    onTestGeminiKey: ((String, (Boolean, String) -> Unit) -> Unit)? = null
) {
    val context = LocalContext.current
    val clipboardManager: ClipboardManager = LocalClipboardManager.current

    var biometricsEnabled by remember { mutableStateOf(securityPrefs.isBiometricsEnabled) }
    var editingName by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf(userName) }

    // Floating Sidebar & Category Auto-Detect states
    var floatingSidebarActive by remember { mutableStateOf(securityPrefs.isFloatingSidebarEnabled) }
    var autoDetectPasswords by remember { mutableStateOf(securityPrefs.autoDetectPasswords) }
    var autoDetectEmails by remember { mutableStateOf(securityPrefs.autoDetectEmails) }
    var autoDetectApis by remember { mutableStateOf(securityPrefs.autoDetectApis) }
    var autoDetectPrompts by remember { mutableStateOf(securityPrefs.autoDetectPrompts) }
    var autoDetectSites by remember { mutableStateOf(securityPrefs.autoDetectSites) }
    var autoDetectOthers by remember { mutableStateOf(securityPrefs.autoDetectOthers) }

    // Gemini API Key state
    var customGeminiKey by remember { mutableStateOf(securityPrefs.customGeminiApiKey) }
    var showGeminiKeyText by remember { mutableStateOf(false) }
    var isTestingGemini by remember { mutableStateOf(false) }
    var geminiTestResult by remember { mutableStateOf<String?>(null) }

    // Multi-format Export state
    var selectedExportFormat by remember { mutableStateOf(ExportFormat.JSON_ENCRYPTED) }
    var exportPassphrase by remember { mutableStateOf("") }
    var exportedPayload by remember { mutableStateOf<String?>(null) }

    // Multi-format Import state
    var importPassphrase by remember { mutableStateOf("") }
    var importPayload by remember { mutableStateOf("") }
    var importPreview by remember { mutableStateOf<BackupPreview?>(null) }
    var importError by remember { mutableStateOf<String?>(null) }

    var showResetConfirmation by remember { mutableStateOf(false) }

    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            title = { Text("Emergency Wipe All Data?") },
            text = { Text("This permanently deletes all vault credentials, accounts, tasks, and resets your master PIN. This operation is strictly irreversible.") },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirmation = false
                        onResetAllData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ToriErrorRed),
                    modifier = Modifier.testTag("confirm_wipe_button")
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // Profile / Greeting Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(ToriCyan.copy(alpha = 0.2f))
                            .border(1.5.dp, ToriCyan, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (userName.isNotEmpty()) userName.take(1).uppercase() else "R",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = ToriCyan
                        )
                    }
                    Column {
                        if (editingName) {
                            OutlinedTextField(
                                value = tempName,
                                onValueChange = { tempName = it },
                                label = { Text("Your Name") },
                                singleLine = true,
                                modifier = Modifier.width(180.dp)
                            )
                        } else {
                            Text(
                                text = "Hello, $userName",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Tori Assistant & Vault Owner",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (editingName) {
                    IconButton(onClick = {
                        if (tempName.isNotBlank()) {
                            onUpdateUserName(tempName.trim())
                            editingName = false
                            onShowSnackbar("Name updated")
                        }
                    }) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = "Save name", tint = ToriSuccessGreen)
                    }
                } else {
                    IconButton(onClick = {
                        tempName = userName
                        editingName = true
                    }) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit name")
                    }
                }
            }
        }

        // Section: System Integrations (Autofill & Home Launcher)
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.Devices, contentDescription = null, tint = ToriCyan)
                    Text("Android System Integrations", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }

                // Autofill Integration
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text("Android Autofill Service", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                        Text("Suggest credentials and auto-prompt on new logins/signups", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                    Button(
                        onClick = {
                            try {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    val intent = Intent(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                    context.startActivity(intent)
                                } else {
                                    context.startActivity(Intent(Settings.ACTION_SETTINGS))
                                }
                            } catch (e: Exception) {
                                context.startActivity(Intent(Settings.ACTION_SETTINGS))
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ToriCyan, contentColor = Color.Black),
                        modifier = Modifier.testTag("enable_autofill_button")
                    ) {
                        Text("Configure")
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Home Launcher Integration
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text("Home Screen Launcher Mode", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                        Text("Set Tori as your default Android phone home launcher desk", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = Intent(Settings.ACTION_HOME_SETTINGS)
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                try {
                                    val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
                                    context.startActivity(intent)
                                } catch (e2: Exception) {
                                    context.startActivity(Intent(Settings.ACTION_SETTINGS))
                                }
                            }
                        },
                        modifier = Modifier.testTag("set_home_launcher_button")
                    ) {
                        Text("Set Default")
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Floating Sidebar Overlay Integration
                val hasOverlayPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Settings.canDrawOverlays(context)
                } else true

                val overlayPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                    contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
                ) {
                    val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        Settings.canDrawOverlays(context)
                    } else true

                    if (granted) {
                        floatingSidebarActive = true
                        securityPrefs.isFloatingSidebarEnabled = true
                        onToggleFloatingSidebar?.invoke(true)
                        onShowSnackbar("Overlay permission granted! Floating Sidebar activated.")
                    } else {
                        onShowSnackbar("Overlay permission is required to float over other apps.")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text("Floating Edge Sidebar Dock", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                        Text(
                            if (hasOverlayPermission) "All-time active edge handle & quick tools dock over all apps"
                            else "Requires 'Display over other apps' overlay permission",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (hasOverlayPermission) MaterialTheme.colorScheme.outline else ToriErrorRed
                        )
                    }
                    if (!hasOverlayPermission) {
                        Button(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                                    overlayPermissionLauncher.launch(intent)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ToriCyan, contentColor = Color.Black),
                            modifier = Modifier.testTag("grant_overlay_permission_button")
                        ) {
                            Text("Grant")
                        }
                    } else {
                        Switch(
                            checked = floatingSidebarActive,
                            onCheckedChange = { isChecked ->
                                floatingSidebarActive = isChecked
                                securityPrefs.isFloatingSidebarEnabled = isChecked
                                onToggleFloatingSidebar?.invoke(isChecked)
                                onShowSnackbar(if (isChecked) "Floating Sidebar activated" else "Floating Sidebar stopped")
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ToriCyan,
                                checkedTrackColor = ToriIndigo
                            ),
                            modifier = Modifier.testTag("floating_sidebar_toggle")
                        )
                    }
                }
            }

        }

        // Section: Category Auto-Detection & Capture Toggles
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.FilterList, contentDescription = null, tint = ToriCyan)
                    Text("Auto-Detection & Save Controls", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }
                Text(
                    "Enable or disable automatic detection and quick-save capture for specific categories.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )

                // 1. Passwords
                CategoryToggleRow(
                    icon = Icons.Default.Key,
                    title = "Passwords & Secrets",
                    subtitle = "High-entropy credentials, website passwords",
                    checked = autoDetectPasswords,
                    onCheckedChange = {
                        autoDetectPasswords = it
                        securityPrefs.autoDetectPasswords = it
                    }
                )

                // 2. Emails & Accounts
                CategoryToggleRow(
                    icon = Icons.Default.Email,
                    title = "Emails & Accounts",
                    subtitle = "Email logins, user handles, account profiles",
                    checked = autoDetectEmails,
                    onCheckedChange = {
                        autoDetectEmails = it
                        securityPrefs.autoDetectEmails = it
                    }
                )

                // 3. API Keys & Bearer Tokens
                CategoryToggleRow(
                    icon = Icons.Default.Code,
                    title = "API Keys & Bearer Tokens",
                    subtitle = "AI Studio, Stripe, GitHub, Cloud API tokens",
                    checked = autoDetectApis,
                    onCheckedChange = {
                        autoDetectApis = it
                        securityPrefs.autoDetectApis = it
                    }
                )

                // 4. Prompts & Notes
                CategoryToggleRow(
                    icon = Icons.Default.Lightbulb,
                    title = "Prompts & Developer Notes",
                    subtitle = "AI system instructions, code snippets, memos",
                    checked = autoDetectPrompts,
                    onCheckedChange = {
                        autoDetectPrompts = it
                        securityPrefs.autoDetectPrompts = it
                    }
                )

                // 5. Sites & URLs
                CategoryToggleRow(
                    icon = Icons.Default.Language,
                    title = "Sites & URLs",
                    subtitle = "Web URLs, domains, service endpoints",
                    checked = autoDetectSites,
                    onCheckedChange = {
                        autoDetectSites = it
                        securityPrefs.autoDetectSites = it
                    }
                )

                // 6. Others
                CategoryToggleRow(
                    icon = Icons.Default.Category,
                    title = "Others / PINs & WiFi",
                    subtitle = "WiFi codes, scratchpad data, general entries",
                    checked = autoDetectOthers,
                    onCheckedChange = {
                        autoDetectOthers = it
                        securityPrefs.autoDetectOthers = it
                    }
                )
            }
        }

        // Section: Gemini AI Intelligence (Free Tier)
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.Psychology, contentDescription = null, tint = ToriCyan)
                    Text("Gemini AI Engine (Free Tier)", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }
                Text(
                    "Integrate your Google AI Studio Gemini API key to empower Tori with conversational queries, intelligent parsing, and bilingual Bangla/English dialogue.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )

                OutlinedTextField(
                    value = customGeminiKey,
                    onValueChange = { customGeminiKey = it },
                    label = { Text("Gemini API Key") },
                    placeholder = { Text("Enter Google AI Studio Key") },
                    singleLine = true,
                    visualTransformation = if (showGeminiKeyText) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showGeminiKeyText = !showGeminiKeyText }) {
                            Icon(
                                imageVector = if (showGeminiKeyText) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle visibility"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("gemini_api_key_input")
                )

                geminiTestResult?.let { result ->
                    Text(
                        text = result,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (result.contains("verified") || result.contains("successful")) ToriSuccessGreen else ToriErrorRed
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (customGeminiKey.isBlank()) {
                                geminiTestResult = "Please enter an API key to test."
                                return@OutlinedButton
                            }
                            isTestingGemini = true
                            geminiTestResult = "Testing connection to Gemini 3.5 Flash..."
                            onTestGeminiKey?.invoke(customGeminiKey.trim()) { success, msg ->
                                isTestingGemini = false
                                geminiTestResult = msg
                            }
                        },
                        enabled = !isTestingGemini,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isTestingGemini) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text("Test Key")
                    }

                    Button(
                        onClick = {
                            securityPrefs.customGeminiApiKey = customGeminiKey.trim()
                            onShowSnackbar("Gemini API key saved securely!")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ToriCyan, contentColor = Color.Black),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save Key")
                    }
                }
            }
        }

        // Section: Authentication & Security
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = ToriCyan)
                    Text("Lock & Authentication", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Master PIN", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                        Text("Secure 6-digit access code", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                    OutlinedButton(onClick = onChangePin, modifier = Modifier.testTag("change_pin_button")) {
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
                        Text("Quick unlock with fingerprint", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                    Switch(
                        checked = biometricsEnabled,
                        onCheckedChange = {
                            biometricsEnabled = it
                            securityPrefs.isBiometricsEnabled = it
                            onShowSnackbar(if (it) "Biometrics enabled" else "Biometrics disabled")
                        },
                        modifier = Modifier.testTag("biometrics_toggle")
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Button(
                    onClick = onLockVault,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
                    modifier = Modifier.fillMaxWidth().testTag("settings_lock_now_button")
                ) {
                    Icon(imageVector = Icons.Default.LockClock, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Lock Vault Immediately")
                }
            }
        }

        // Section: Multi-Format Memory Export (JSON, CSV, TXT)
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, tint = ToriCyan)
                    Text("Export Memory (JSON, CSV, TXT)", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }

                Text(
                    text = "Choose your preferred export format. You can share or upload to Google Drive, Telegram, email, or local files.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Format selector chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedExportFormat == ExportFormat.JSON_ENCRYPTED,
                        onClick = { selectedExportFormat = ExportFormat.JSON_ENCRYPTED },
                        label = { Text("Encrypted JSON", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = selectedExportFormat == ExportFormat.CSV,
                        onClick = { selectedExportFormat = ExportFormat.CSV },
                        label = { Text("CSV Table", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = selectedExportFormat == ExportFormat.TXT,
                        onClick = { selectedExportFormat = ExportFormat.TXT },
                        label = { Text("TXT Sheet", fontSize = 11.sp) }
                    )
                }

                if (selectedExportFormat == ExportFormat.JSON_ENCRYPTED) {
                    OutlinedTextField(
                        value = exportPassphrase,
                        onValueChange = { exportPassphrase = it },
                        label = { Text("Encryption Passphrase") },
                        placeholder = { Text("Min 6 characters") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("export_passphrase_input")
                    )
                }

                Button(
                    onClick = {
                        val payload = when (selectedExportFormat) {
                            ExportFormat.JSON_ENCRYPTED -> {
                                if (exportPassphrase.length < 6) {
                                    onShowSnackbar("Passphrase must be at least 6 characters")
                                    return@Button
                                }
                                BackupManager.exportToJson(
                                    vaultEntries = vaultEntries,
                                    tasks = tasks,
                                    decryptSecret = decryptSecret,
                                    encrypted = true,
                                    passphrase = exportPassphrase
                                )
                            }
                            ExportFormat.JSON_PLAIN -> {
                                BackupManager.exportToJson(
                                    vaultEntries = vaultEntries,
                                    tasks = tasks,
                                    decryptSecret = decryptSecret,
                                    encrypted = false
                                )
                            }
                            ExportFormat.CSV -> {
                                BackupManager.exportToCsv(
                                    vaultEntries = vaultEntries,
                                    decryptSecret = decryptSecret
                                )
                            }
                            ExportFormat.TXT -> {
                                BackupManager.exportToTxt(
                                    vaultEntries = vaultEntries,
                                    tasks = tasks,
                                    decryptSecret = decryptSecret
                                )
                            }
                        }

                        exportedPayload = payload

                        val ext = when (selectedExportFormat) {
                            ExportFormat.JSON_ENCRYPTED, ExportFormat.JSON_PLAIN -> "json"
                            ExportFormat.CSV -> "csv"
                            ExportFormat.TXT -> "txt"
                        }

                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, payload)
                            putExtra(Intent.EXTRA_TITLE, "Tori_Memory_Backup.$ext")
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, "Export Tori Memory ($ext)")
                        context.startActivity(shareIntent)
                        onShowSnackbar("Export prepared and shared")
                    },
                    modifier = Modifier.fillMaxWidth().testTag("export_backup_button")
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export & Share (${selectedExportFormat.name.substringBefore("_")})")
                }

                if (exportedPayload != null) {
                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(exportedPayload ?: ""))
                            onShowSnackbar("Copied export payload to clipboard")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy Export Payload to Clipboard")
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Universal Import Block
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.FileUpload, contentDescription = null, tint = ToriCyan)
                    Text("Restore Memory (JSON, CSV, or TXT)", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                }

                OutlinedTextField(
                    value = importPayload,
                    onValueChange = { importPayload = it },
                    label = { Text("Paste Backup Content (JSON, CSV, or TXT)") },
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth().testTag("import_payload_input")
                )

                OutlinedTextField(
                    value = importPassphrase,
                    onValueChange = { importPassphrase = it },
                    label = { Text("Passphrase (if encrypted JSON)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("import_passphrase_input")
                )

                Button(
                    onClick = {
                        if (importPayload.isNotBlank()) {
                            try {
                                val preview = BackupManager.parseImportContent(importPayload.trim(), importPassphrase)
                                if (preview.vaultDrafts.isEmpty() && preview.taskDrafts.isEmpty()) {
                                    importError = "No valid vault items or tasks found in payload."
                                    importPreview = null
                                } else {
                                    importPreview = preview
                                    importError = null
                                }
                            } catch (e: Exception) {
                                importError = "Failed to parse: Verify passphrase if encrypted, or check format."
                                importPreview = null
                            }
                        } else {
                            onShowSnackbar("Please paste backup content")
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("preview_import_button")
                ) {
                    Text("Preview & Verify Memory")
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
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Verified Memory Contents:", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            Text("• ${preview.vaultDrafts.size} Vault items", style = MaterialTheme.typography.bodySmall)
                            Text("• ${preview.taskDrafts.size} Tasks", style = MaterialTheme.typography.bodySmall)

                            Spacer(modifier = Modifier.height(4.dp))

                            Button(
                                onClick = {
                                    onConfirmImport(preview)
                                    importPreview = null
                                    importPayload = ""
                                    importPassphrase = ""
                                    onShowSnackbar("Memory restored & merged into Tori!")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ToriSuccessGreen),
                                modifier = Modifier.fillMaxWidth().testTag("confirm_restore_button")
                            ) {
                                Text("Restore & Merge into Vault")
                            }
                        }
                    }
                }
            }
        }

        // Section: About & Data Location
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = ToriCyan)
                    Text("About & Security Architecture", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }

                Text("Application: Tori (Vyra) Personal Assistant & Vault", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                Text("Version: 2.0.0 (Jarvis Architecture)", style = MaterialTheme.typography.bodySmall)
                Text("Autofill Service: Active (com.example.core.autofill.ToriAutofillService)", style = MaterialTheme.typography.bodySmall, color = ToriCyan)
                Text("Home Launcher: Enabled (android.intent.category.HOME)", style = MaterialTheme.typography.bodySmall, color = ToriIndigo)
                Text("Master Key: AndroidKeyStore Hardware AES-256-GCM", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                Text("Cloud Policy: 100% On-Device storage with user-directed export (JSON/CSV/TXT).", style = MaterialTheme.typography.bodySmall, color = ToriCyan)
            }
        }

        // Danger Zone
        OutlinedButton(
            onClick = { showResetConfirmation = true },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = ToriErrorRed),
            modifier = Modifier.fillMaxWidth().testTag("wipe_data_button")
        ) {
            Icon(imageVector = Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Emergency Wipe Data")
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun CategoryToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f).padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = ToriCyan, modifier = Modifier.size(20.dp))
            Column {
                Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = ToriCyan,
                checkedTrackColor = ToriIndigo
            )
        )
    }
}

