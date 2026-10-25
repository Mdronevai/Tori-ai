package com.example.feature.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.data.VaultCategory
import com.example.core.data.VaultEntry
import com.example.ui.theme.ToriCyan
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun VaultItemDetailDialog(
    item: VaultEntry,
    decryptedSecret: String,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSecretCopied: () -> Unit
) {
    var isSecretVisible by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Vault Entry?") },
            text = { Text("Are you sure you want to delete \"${item.title}\"? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        onDelete()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
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
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header with category badge & action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SuggestionChip(
                        onClick = {},
                        label = {
                            Text(
                                when (item.category) {
                                    VaultCategory.PASSWORDS.name -> "Passwords"
                                    VaultCategory.ACCOUNTS.name -> "Accounts"
                                    VaultCategory.SITES_APIS.name -> "Sites/APIs"
                                    else -> "Others"
                                }
                            )
                        }
                    )

                    Row {
                        IconButton(onClick = onEdit, modifier = Modifier.testTag("edit_vault_item_button")) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Entry")
                        }
                        IconButton(onClick = { showDeleteConfirmation = true }, modifier = Modifier.testTag("delete_vault_item_button")) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Entry",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                Text(
                    text = item.title,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Username section
                    if (!item.username.isNullOrEmpty()) {
                        DetailRow(
                            label = if (item.category == VaultCategory.ACCOUNTS.name) "Email / Account" else "Username",
                            value = item.username,
                            onCopy = {
                                clipboardManager.setText(AnnotatedString(item.username))
                                onSecretCopied()
                            }
                        )
                    }

                    // Secret section with masked password and tap to reveal button (Section 2.D / Step 4)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (item.category == VaultCategory.SITES_APIS.name) "API Key / Secret" else "Password / Secret",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (isSecretVisible) decryptedSecret else "••••••••••••••••",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Button(
                                    onClick = { isSecretVisible = !isSecretVisible },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSecretVisible) MaterialTheme.colorScheme.outlineVariant else ToriCyan,
                                        contentColor = if (isSecretVisible) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimary
                                    ),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(34.dp).testTag("tap_to_reveal_button")
                                ) {
                                    Icon(
                                        imageVector = if (isSecretVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        if (isSecretVisible) "Hide" else "Tap to reveal",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(decryptedSecret))
                                        onSecretCopied()
                                    },
                                    modifier = Modifier.testTag("copy_secret_detail_button")
                                ) {
                                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy Secret")
                                }
                            }
                        }
                    }

                    // Extra field (Recovery info, docs link)
                    if (!item.extraField.isNullOrEmpty()) {
                        DetailRow(
                            label = if (item.category == VaultCategory.ACCOUNTS.name) "Recovery Info" else "Docs Link / Extra",
                            value = item.extraField,
                            onCopy = {
                                clipboardManager.setText(AnnotatedString(item.extraField))
                                onSecretCopied()
                            }
                        )
                    }

                    // Notes
                    if (!item.notes.isNullOrEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Notes", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(item.notes, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    // Timestamps & Security
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Last updated: ${dateFormatter.format(Date(item.updatedAt))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "Security: Hardware Keystore AES-256-GCM Encrypted",
                            style = MaterialTheme.typography.labelSmall,
                            color = ToriCyan
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    onCopy: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
        }
        IconButton(onClick = onCopy) {
            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy")
        }
    }
}
