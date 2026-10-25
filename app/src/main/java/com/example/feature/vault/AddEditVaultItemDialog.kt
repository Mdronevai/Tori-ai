package com.example.feature.vault

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.data.VaultCategory
import com.example.core.data.VaultEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditVaultItemDialog(
    initialItem: VaultEntry? = null,
    initialPlainSecret: String = "",
    onDismiss: () -> Unit,
    onSave: (
        id: Int,
        category: String,
        title: String,
        username: String?,
        secret: String,
        extraField: String?,
        notes: String?
    ) -> Unit
) {
    var category by remember {
        mutableStateOf(
            initialItem?.category ?: VaultCategory.PASSWORDS.name
        )
    }

    var title by remember { mutableStateOf(initialItem?.title ?: "") }
    var username by remember { mutableStateOf(initialItem?.username ?: "") }
    var secret by remember { mutableStateOf(initialPlainSecret) }
    var extraField by remember { mutableStateOf(initialItem?.extraField ?: "") }
    var notes by remember { mutableStateOf(initialItem?.notes ?: "") }

    var isSecretVisible by remember { mutableStateOf(false) }
    var showGeneratorSheet by remember { mutableStateOf(false) }
    var titleError by remember { mutableStateOf(false) }

    val categories = listOf(
        VaultCategory.PASSWORDS.name to "Passwords",
        VaultCategory.ACCOUNTS.name to "Accounts",
        VaultCategory.SITES_APIS.name to "Sites/APIs",
        VaultCategory.OTHERS.name to "Others"
    )

    if (showGeneratorSheet) {
        PasswordGeneratorSheet(
            onDismiss = { showGeneratorSheet = false },
            onUsePassword = { generated ->
                secret = generated
                showGeneratorSheet = false
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
                Text(
                    text = if (initialItem == null) "New Vault Entry" else "Edit Vault Entry",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(14.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Category Selector
                    Text("Category", style = MaterialTheme.typography.labelMedium)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        categories.forEachIndexed { index, (key, label) ->
                            SegmentedButton(
                                selected = category == key,
                                onClick = { category = key },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = categories.size)
                            ) {
                                Text(
                                    label,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // Category-specific fields (Section 2.E)
                    when (category) {
                        VaultCategory.PASSWORDS.name -> {
                            // Site/App name, Username, Password, Notes
                            OutlinedTextField(
                                value = title,
                                onValueChange = { title = it; titleError = false },
                                label = { Text("Site / App Name *") },
                                placeholder = { Text("e.g. Netflix, GitHub") },
                                isError = titleError,
                                supportingText = if (titleError) { { Text("Site name cannot be empty") } } else null,
                                modifier = Modifier.fillMaxWidth().testTag("vault_title_input")
                            )

                            OutlinedTextField(
                                value = username,
                                onValueChange = { username = it },
                                label = { Text("Username / Email") },
                                modifier = Modifier.fillMaxWidth().testTag("vault_username_input")
                            )

                            OutlinedTextField(
                                value = secret,
                                onValueChange = { secret = it },
                                label = { Text("Password *") },
                                visualTransformation = if (isSecretVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = { showGeneratorSheet = true }) {
                                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "Generate", tint = MaterialTheme.colorScheme.primary)
                                        }
                                        IconButton(onClick = { isSecretVisible = !isSecretVisible }) {
                                            Icon(
                                                imageVector = if (isSecretVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = "Toggle password visibility"
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().testTag("vault_secret_input")
                            )

                            OutlinedTextField(
                                value = notes,
                                onValueChange = { notes = it },
                                label = { Text("Notes (Optional)") },
                                minLines = 2,
                                maxLines = 4,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        VaultCategory.ACCOUNTS.name -> {
                            // Platform, Email, Recovery info, Notes
                            OutlinedTextField(
                                value = title,
                                onValueChange = { title = it; titleError = false },
                                label = { Text("Platform *") },
                                placeholder = { Text("e.g. Google, Facebook") },
                                isError = titleError,
                                modifier = Modifier.fillMaxWidth().testTag("vault_title_input")
                            )

                            OutlinedTextField(
                                value = username,
                                onValueChange = { username = it },
                                label = { Text("Email *") },
                                modifier = Modifier.fillMaxWidth().testTag("vault_username_input")
                            )

                            OutlinedTextField(
                                value = extraField,
                                onValueChange = { extraField = it },
                                label = { Text("Recovery Info (Phone / Backup Email)") },
                                placeholder = { Text("e.g. backup@domain.com, +1-555-0199") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = secret,
                                onValueChange = { secret = it },
                                label = { Text("Account Password / PIN") },
                                visualTransformation = if (isSecretVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { isSecretVisible = !isSecretVisible }) {
                                        Icon(imageVector = if (isSecretVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = notes,
                                onValueChange = { notes = it },
                                label = { Text("Notes") },
                                minLines = 2,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        VaultCategory.SITES_APIS.name -> {
                            // Name, API Key, Docs link, Notes
                            OutlinedTextField(
                                value = title,
                                onValueChange = { title = it; titleError = false },
                                label = { Text("API / Service Name *") },
                                placeholder = { Text("e.g. Stripe, OpenAI") },
                                isError = titleError,
                                modifier = Modifier.fillMaxWidth().testTag("vault_title_input")
                            )

                            OutlinedTextField(
                                value = secret,
                                onValueChange = { secret = it },
                                label = { Text("API Key / Token *") },
                                visualTransformation = if (isSecretVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { isSecretVisible = !isSecretVisible }) {
                                        Icon(imageVector = if (isSecretVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().testTag("vault_secret_input")
                            )

                            OutlinedTextField(
                                value = extraField,
                                onValueChange = { extraField = it },
                                label = { Text("Docs Link / Endpoint URL") },
                                placeholder = { Text("https://docs.stripe.com") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = notes,
                                onValueChange = { notes = it },
                                label = { Text("Notes (Environment, Scope)") },
                                minLines = 2,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        else -> { // OTHERS
                            // Free-form title + note
                            OutlinedTextField(
                                value = title,
                                onValueChange = { title = it; titleError = false },
                                label = { Text("Title *") },
                                placeholder = { Text("e.g. Wi-Fi Password, Master Safe PIN") },
                                isError = titleError,
                                modifier = Modifier.fillMaxWidth().testTag("vault_title_input")
                            )

                            OutlinedTextField(
                                value = secret,
                                onValueChange = { secret = it },
                                label = { Text("Encrypted Secret / Content *") },
                                minLines = 3,
                                maxLines = 6,
                                modifier = Modifier.fillMaxWidth().testTag("vault_secret_input")
                            )

                            OutlinedTextField(
                                value = notes,
                                onValueChange = { notes = it },
                                label = { Text("Additional Notes") },
                                minLines = 2,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isBlank()) {
                                titleError = true
                                return@Button
                            }
                            onSave(
                                initialItem?.id ?: 0,
                                category,
                                title.trim(),
                                username.trim().ifEmpty { null },
                                secret,
                                extraField.trim().ifEmpty { null },
                                notes.trim().ifEmpty { null }
                            )
                        },
                        modifier = Modifier.testTag("save_vault_item_button")
                    ) {
                        Text("Save & Encrypt")
                    }
                }
            }
        }
    }
}
