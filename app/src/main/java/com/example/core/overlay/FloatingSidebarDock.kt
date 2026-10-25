package com.example.core.overlay

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainActivity
import com.example.core.data.ToriRepository
import com.example.core.data.VaultCategory
import com.example.core.data.VaultEntry
import com.example.core.security.SecurityPreferences
import com.example.feature.vault.PasswordGenerator
import com.example.ui.theme.ToriCyan
import com.example.ui.theme.ToriIndigo
import com.example.ui.theme.ToriSuccessGreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun FloatingSidebarDock(
    repository: ToriRepository,
    securityPrefs: SecurityPreferences,
    onCloseService: () -> Unit,
    onLaunchVoice: () -> Unit,
    onOpenAppHome: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isExpanded by remember { mutableStateOf(false) }
    var detectedItem by remember { mutableStateOf<DetectedItem?>(null) }
    var showQuickNoteDialog by remember { mutableStateOf(false) }
    var quickNoteText by remember { mutableStateOf("") }

    Box(
        modifier = Modifier.wrapContentSize(),
        contentAlignment = Alignment.CenterEnd
    ) {
        if (!isExpanded) {
            // Collapsed Sleek Handle (Matches User Screenshot 1)
            Box(
                modifier = Modifier
                    .width(22.dp)
                    .height(84.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xDD0B132B),
                                Color(0xFF1C2541)
                            )
                        )
                    )
                    .border(
                        width = 1.5.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(ToriCyan, Color(0xFF48CAE4))
                        ),
                        shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                    )
                    .clickable { isExpanded = true },
                contentAlignment = Alignment.Center
            ) {
                // Subtle glowing cyan pill accent
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(36.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(ToriCyan)
                )
            }
        } else {
            // Expanded Vertical Scrollable Icon Dock (Matches User Screenshot 2)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 4.dp)
            ) {
                // If an item was detected by Quick-Scan, show confirmation card to the left of the dock
                if (detectedItem != null) {
                    DetectedItemCard(
                        item = detectedItem!!,
                        onDismiss = { detectedItem = null },
                        onConfirmSave = { title, secret, category, extra ->
                            coroutineScope.launch(Dispatchers.IO) {
                                repository.saveVaultEntry(
                                    id = 0,
                                    category = category,
                                    title = title,
                                    username = if (category == VaultCategory.ACCOUNTS.name) extra else null,
                                    plainSecret = secret,
                                    extraField = extra,
                                    notes = "Captured via Tori Floating Sidebar"
                                )
                            }
                            Toast.makeText(context, "Saved to $category in Vault!", Toast.LENGTH_SHORT).show()
                            detectedItem = null
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                if (showQuickNoteDialog) {
                    QuickNoteCard(
                        noteText = quickNoteText,
                        onTextChange = { quickNoteText = it },
                        onDismiss = { showQuickNoteDialog = false },
                        onSave = { note ->
                            if (note.isNotBlank()) {
                                coroutineScope.launch(Dispatchers.IO) {
                                    repository.saveVaultEntry(
                                        id = 0,
                                        category = VaultCategory.OTHERS.name,
                                        title = if (note.length > 20) note.take(20) + "..." else note,
                                        username = null,
                                        plainSecret = note,
                                        notes = "Quick note from Floating Sidebar"
                                    )
                                }
                                Toast.makeText(context, "Prompt saved to Vault!", Toast.LENGTH_SHORT).show()
                            }
                            showQuickNoteDialog = false
                            quickNoteText = ""
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                // Vertical Rounded Dock Container (Screenshot 2 styling)
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xEE141C2A)
                    ),
                    modifier = Modifier
                        .width(68.dp)
                        .heightIn(max = 440.dp)
                        .shadow(16.dp, RoundedCornerShape(28.dp))
                        .border(
                            width = 1.5.dp,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    ToriCyan.copy(alpha = 0.8f),
                                    Color(0xFF3A86FF).copy(alpha = 0.4f),
                                    Color(0x3300E5FF)
                                )
                            ),
                            shape = RoundedCornerShape(28.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 1. Collapse / Minimize button
                        DockIconButton(
                            icon = Icons.Default.ChevronRight,
                            contentDescription = "Collapse Sidebar",
                            bgColor = Color(0x33FFFFFF),
                            iconColor = Color.White,
                            onClick = {
                                isExpanded = false
                                detectedItem = null
                                showQuickNoteDialog = false
                            }
                        )

                        // 2. Voice Assistant Mode (User prompt: "Like voice mod icon")
                        DockIconButton(
                            icon = Icons.Default.Mic,
                            contentDescription = "Voice Assistant",
                            bgColor = Brush.linearGradient(listOf(Color(0xFF00B4D8), Color(0xFF0077B6))),
                            iconColor = Color.White,
                            onClick = {
                                onLaunchVoice()
                            }
                        )

                        // 3. Web & Screen Scanner / Quick Info Collect (User prompt: "website page scening or auto password api othars info collect")
                        DockIconButton(
                            icon = Icons.Default.DocumentScanner,
                            contentDescription = "Scan & Auto Collect",
                            bgColor = Brush.linearGradient(listOf(Color(0xFF38B000), Color(0xFF007200))),
                            iconColor = Color.White,
                            onClick = {
                                val detected = scanClipboardForInfo(context, securityPrefs)
                                if (detected != null) {
                                    detectedItem = detected
                                } else {
                                    Toast.makeText(context, "No new credentials or tokens in active clipboard", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )

                        // 4. Quick High-Entropy Password Generator
                        DockIconButton(
                            icon = Icons.Default.VpnKey,
                            contentDescription = "Quick Password Generator",
                            bgColor = Brush.linearGradient(listOf(Color(0xFFFF9E00), Color(0xFFE85D04))),
                            iconColor = Color.White,
                            onClick = {
                                val newPass = PasswordGenerator.generate(18)
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                cm?.setPrimaryClip(ClipData.newPlainText("Tori Password", newPass))
                                Toast.makeText(context, "18-char key copied: $newPass", Toast.LENGTH_SHORT).show()
                            }
                        )

                        // 5. Quick Prompt / Developer Note Logger
                        DockIconButton(
                            icon = Icons.Default.PostAdd,
                            contentDescription = "Quick Prompt Logger",
                            bgColor = Brush.linearGradient(listOf(Color(0xFF7209B7), Color(0xFF560BAD))),
                            iconColor = Color.White,
                            onClick = {
                                showQuickNoteDialog = !showQuickNoteDialog
                            }
                        )

                        // 6. Tori App / HUD Home Launcher
                        DockIconButton(
                            icon = Icons.Default.Home,
                            contentDescription = "Tori Desk HUD",
                            bgColor = Brush.linearGradient(listOf(Color(0xFF4361EE), Color(0xFF3A0CA3))),
                            iconColor = Color.White,
                            onClick = {
                                onOpenAppHome()
                            }
                        )

                        // 7. Quick Vault Access
                        DockIconButton(
                            icon = Icons.Default.Lock,
                            contentDescription = "Open Vault",
                            bgColor = Brush.linearGradient(listOf(Color(0xFF2B2D42), Color(0xFF1A1A24))),
                            iconColor = ToriCyan,
                            onClick = {
                                val intent = Intent(context, MainActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                    putExtra("EXTRA_NAV_TAB", "VAULT")
                                }
                                context.startActivity(intent)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DockIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    bgColor: Any, // Color or Brush
    iconColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .then(
                if (bgColor is Brush) Modifier.background(bgColor)
                else Modifier.background(bgColor as Color)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
    }
}

data class DetectedItem(
    val category: String,
    val title: String,
    val secret: String,
    val extra: String
)

private fun scanClipboardForInfo(context: Context, prefs: SecurityPreferences): DetectedItem? {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return null
    val clip = clipboard.primaryClip?.getItemAt(0)?.text?.toString()?.trim() ?: return null
    if (clip.isEmpty()) return null

    // 1. API Keys & Bearer Tokens
    if (prefs.autoDetectApis) {
        val apiKeyRegex = Regex("(?:sk-[a-zA-Z0-9_-]{20,}|AIza[0-9A-Za-z-_]{35}|ghp_[a-zA-Z0-9]{36}|Bearer\\s+[a-zA-Z0-9_.-]{20,}|api_key[=:][a-zA-Z0-9_.-]+)", RegexOption.IGNORE_CASE)
        val match = apiKeyRegex.find(clip)
        if (match != null) {
            val key = match.value.replace(Regex("^Bearer\\s+", RegexOption.IGNORE_CASE), "")
            return DetectedItem(
                category = VaultCategory.SITES_APIS.name,
                title = "API Key / Token",
                secret = key,
                extra = "Captured from clipboard"
            )
        }
    }

    // 2. Email Address / Accounts
    if (prefs.autoDetectEmails) {
        val emailRegex = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
        val match = emailRegex.find(clip)
        if (match != null && !clip.contains(" ") && clip.length < 60) {
            val domain = match.value.substringAfter("@").substringBefore(".")
            return DetectedItem(
                category = VaultCategory.ACCOUNTS.name,
                title = "${domain.replaceFirstChar { it.uppercase() }} Account",
                secret = "",
                extra = match.value
            )
        }
    }

    // 3. URLs / Sites
    if (prefs.autoDetectSites) {
        val urlRegex = Regex("https?://[a-zA-Z0-9.-]+(?:/[^\\s]*)?")
        val match = urlRegex.find(clip)
        if (match != null && clip.startsWith("http")) {
            val host = UriRegexHelper.getHost(match.value)
            return DetectedItem(
                category = VaultCategory.SITES_APIS.name,
                title = host.ifEmpty { "Website" },
                secret = match.value,
                extra = match.value
            )
        }
    }

    // 4. Passwords / High entropy tokens
    if (prefs.autoDetectPasswords) {
        if (!clip.contains(" ") && clip.length in 8..64 && clip.any { it.isDigit() } && clip.any { it.isLetter() }) {
            return DetectedItem(
                category = VaultCategory.PASSWORDS.name,
                title = "Captured Password",
                secret = clip,
                extra = ""
            )
        }
    }

    // 5. Prompts / Developer Snippets
    if (prefs.autoDetectPrompts) {
        if (clip.length > 30 && (clip.startsWith("Write") || clip.startsWith("Act as") || clip.startsWith("Generate") || clip.contains("\n") || clip.contains("```"))) {
            val title = if (clip.length > 25) clip.take(25) + "..." else clip
            return DetectedItem(
                category = VaultCategory.OTHERS.name,
                title = "AI Prompt: $title",
                secret = clip,
                extra = "Prompt"
            )
        }
    }

    // 6. Generic item if others enabled
    if (prefs.autoDetectOthers && clip.isNotEmpty() && clip.length < 200) {
        return DetectedItem(
            category = VaultCategory.OTHERS.name,
            title = "Clipboard Item",
            secret = clip,
            extra = ""
        )
    }

    return null
}

object UriRegexHelper {
    fun getHost(url: String): String {
        return try {
            val clean = url.removePrefix("https://").removePrefix("http://")
            clean.substringBefore("/").substringBefore(":")
        } catch (_: Exception) {
            ""
        }
    }
}

@Composable
fun DetectedItemCard(
    item: DetectedItem,
    onDismiss: () -> Unit,
    onConfirmSave: (title: String, secret: String, category: String, extra: String) -> Unit
) {
    var title by remember { mutableStateOf(item.title) }
    var secret by remember { mutableStateOf(item.secret) }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xF0101726)),
        modifier = Modifier
            .width(260.dp)
            .shadow(16.dp, RoundedCornerShape(20.dp))
            .border(1.5.dp, ToriCyan, RoundedCornerShape(20.dp))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AUTO-DETECTED",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = ToriCyan
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = ToriIndigo.copy(alpha = 0.3f)
                ) {
                    Text(
                        text = item.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title", fontSize = 11.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ToriCyan,
                    unfocusedBorderColor = Color(0xFF334155)
                )
            )

            if (secret.isNotEmpty()) {
                OutlinedTextField(
                    value = secret,
                    onValueChange = { secret = it },
                    label = { Text("Secret / Token", fontSize = 11.sp) },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ToriCyan,
                        unfocusedBorderColor = Color(0xFF334155)
                    )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.LightGray)
                ) {
                    Text("Skip", fontSize = 12.sp)
                }
                Button(
                    onClick = { onConfirmSave(title, secret, item.category, item.extra) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = ToriCyan)
                ) {
                    Text("Save", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun QuickNoteCard(
    noteText: String,
    onTextChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xF0101726)),
        modifier = Modifier
            .width(260.dp)
            .shadow(16.dp, RoundedCornerShape(20.dp))
            .border(1.5.dp, Color(0xFF7209B7), RoundedCornerShape(20.dp))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "QUICK PROMPT / NOTE",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFB5179E)
            )

            OutlinedTextField(
                value = noteText,
                onValueChange = onTextChange,
                placeholder = { Text("Write prompt, API note, or quick idea...", fontSize = 12.sp) },
                maxLines = 4,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFB5179E),
                    unfocusedBorderColor = Color(0xFF334155)
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.LightGray)
                ) {
                    Text("Cancel", fontSize = 12.sp)
                }
                Button(
                    onClick = { onSave(noteText) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7209B7))
                ) {
                    Text("Save", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}
