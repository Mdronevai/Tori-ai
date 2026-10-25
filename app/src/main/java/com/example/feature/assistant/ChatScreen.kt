package com.example.feature.assistant

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.data.ChatLog
import com.example.core.data.VaultCategory
import com.example.ui.theme.ToriCyan
import com.example.ui.theme.ToriSuccessGreen
import org.json.JSONObject

@Composable
fun ChatScreen(
    messages: List<ChatLog>,
    isThinking: Boolean,
    onSendMessage: (String) -> Unit,
    onConfirmAction: (ChatLog, updatedCategory: String?, updatedTitle: String?, updatedSecret: String?, updatedUser: String?) -> Unit,
    onRejectAction: (ChatLog) -> Unit,
    onDraftVaultFromPassword: (String) -> Unit,
    onActionCopied: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                inputText = spoken
            }
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val suggestionChips = listOf(
        "Save my Facebook password",
        "Amar Netflix password ki?",
        "Save API key for Stripe sk_live_9812",
        "Remind me to update bank password tomorrow",
        "Generate 16-char password"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Assistant status bar
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(ToriSuccessGreen)
                    )
                    Text(
                        text = "Tori Assistant Active",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "Confirmation Shield Enabled",
                    style = MaterialTheme.typography.labelSmall,
                    color = ToriCyan
                )
            }
        }

        // Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp)
        ) {
            items(messages, key = { it.id }) { message ->
                ChatBubble(
                    message = message,
                    onConfirm = { updatedCat, updatedTitle, updatedSecret, updatedUser ->
                        onConfirmAction(message, updatedCat, updatedTitle, updatedSecret, updatedUser)
                    },
                    onReject = { onRejectAction(message) },
                    onDraftVault = { password -> onDraftVaultFromPassword(password) },
                    onCopied = onActionCopied
                )
            }

            if (isThinking) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = ToriCyan
                        )
                        Text(
                            text = "Tori is thinking...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Suggestions row
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(suggestionChips) { chip ->
                SuggestionChip(
                    onClick = { onSendMessage(chip) },
                    label = { Text(chip, maxLines = 1) }
                )
            }
        }

        // Input row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("Ask Tori to save, remind, query...") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_field"),
                shape = RoundedCornerShape(24.dp),
                maxLines = 4
            )

            // Mic Button for Speech-to-Text
            IconButton(
                onClick = {
                    try {
                        val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to Tori...")
                        }
                        speechLauncher.launch(speechIntent)
                    } catch (_: Exception) {
                        inputText = "Save my Facebook password"
                    }
                },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .testTag("chat_mic_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Speak",
                    tint = ToriCyan
                )
            }

            // Send Button
            IconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        val text = inputText
                        inputText = ""
                        onSendMessage(text)
                    }
                },
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .testTag("chat_send_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
fun ChatBubble(
    message: ChatLog,
    onConfirm: (category: String?, title: String?, secret: String?, username: String?) -> Unit,
    onReject: () -> Unit,
    onDraftVault: (String) -> Unit,
    onCopied: () -> Unit
) {
    val isUser = message.role == "user"

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        if (!isUser) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = ToriCyan,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "Tori Assistant",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = ToriCyan
                )
            }
        }

        // Text bubble
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 2.dp,
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Text(
                text = message.message,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(14.dp)
            )
        }

        // Inline Confirmation Card
        if (message.pendingActionType != null && message.pendingActionData != null) {
            Spacer(modifier = Modifier.height(8.dp))
            InlineConfirmationCard(
                actionType = message.pendingActionType,
                actionData = message.pendingActionData,
                isConfirmed = message.isConfirmed,
                onConfirm = onConfirm,
                onReject = onReject,
                onDraftVault = onDraftVault,
                onCopied = onCopied
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InlineConfirmationCard(
    actionType: String,
    actionData: String,
    isConfirmed: Boolean?,
    onConfirm: (category: String?, title: String?, secret: String?, username: String?) -> Unit,
    onReject: () -> Unit,
    onDraftVault: (String) -> Unit,
    onCopied: () -> Unit
) {
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    var isSecretVisible by remember { mutableStateOf(false) }

    val dataJson = remember(actionData) {
        try { JSONObject(actionData) } catch (_: Exception) { JSONObject() }
    }

    val categoryOptions = listOf(
        VaultCategory.PASSWORDS.name to "Passwords",
        VaultCategory.ACCOUNTS.name to "Accounts",
        VaultCategory.SITES_APIS.name to "Sites/APIs",
        VaultCategory.OTHERS.name to "Others"
    )

    var selectedCategory by remember(actionData) {
        mutableStateOf(dataJson.optString("category", VaultCategory.PASSWORDS.name))
    }

    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    var editableTitle by remember(actionData) { mutableStateOf(dataJson.optString("title", "")) }
    var editableUser by remember(actionData) { mutableStateOf(dataJson.optString("username", "")) }
    var editableSecret by remember(actionData) { mutableStateOf(dataJson.optString("secret", "")) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                when (isConfirmed) {
                    true -> ToriSuccessGreen
                    false -> MaterialTheme.colorScheme.outlineVariant
                    null -> ToriCyan
                },
                RoundedCornerShape(16.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        imageVector = if (actionType == "ADD_TASK") Icons.Default.Alarm else Icons.Default.Lock,
                        contentDescription = null,
                        tint = ToriCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (actionType == "ADD_TASK") "Draft Reminder" else "Draft Vault Entry",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (isConfirmed != null) {
                    Text(
                        text = if (isConfirmed) "CONFIRMED & ENCRYPTED" else "CANCELLED",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isConfirmed) ToriSuccessGreen else MaterialTheme.colorScheme.outline
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            if (actionType == "SAVE_VAULT") {
                if (isConfirmed == null) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Category", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Box {
                            OutlinedButton(
                                onClick = { categoryDropdownExpanded = true },
                                modifier = Modifier.fillMaxWidth().testTag("category_dropdown_button")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(categoryOptions.firstOrNull { it.first == selectedCategory }?.second ?: selectedCategory)
                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            }
                            DropdownMenu(
                                expanded = categoryDropdownExpanded,
                                onDismissRequest = { categoryDropdownExpanded = false }
                            ) {
                                categoryOptions.forEach { (catKey, catLabel) ->
                                    DropdownMenuItem(
                                        text = { Text(catLabel) },
                                        onClick = {
                                            selectedCategory = catKey
                                            categoryDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Fields
                    OutlinedTextField(
                        value = editableTitle,
                        onValueChange = { editableTitle = it },
                        label = { Text("Title / Site Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editableUser,
                        onValueChange = { editableUser = it },
                        label = { Text(if (selectedCategory == VaultCategory.ACCOUNTS.name) "Email / Account" else "Username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editableSecret,
                        onValueChange = { editableSecret = it },
                        label = { Text(if (selectedCategory == VaultCategory.SITES_APIS.name) "API Key" else "Password / Secret") },
                        visualTransformation = if (isSecretVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isSecretVisible = !isSecretVisible }) {
                                Icon(imageVector = if (isSecretVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null)
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text("Title: $editableTitle", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    Text("Category: $selectedCategory", style = MaterialTheme.typography.bodySmall)
                    if (editableUser.isNotEmpty()) Text("Account: $editableUser", style = MaterialTheme.typography.bodySmall)
                }
            } else if (actionType == "ADD_TASK") {
                val title = dataJson.optString("title", "Task")
                Text("Task: $title", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
            } else if (actionType == "PASSWORD_GENERATED") {
                val genPass = dataJson.optString("generatedPassword", "")
                Text(genPass, style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold), color = ToriCyan)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        clipboardManager.setText(AnnotatedString(genPass))
                        onCopied()
                    }) {
                        Text("Copy")
                    }
                    OutlinedButton(onClick = { onDraftVault(genPass) }) {
                        Text("Draft Vault Item")
                    }
                }
            } else if (actionType == "QUERY_RESULT") {
                val title = dataJson.optString("title", "")
                val username = dataJson.optString("username", "")
                val secret = dataJson.optString("secret", "")
                Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                if (username.isNotEmpty()) Text("User: $username", style = MaterialTheme.typography.bodySmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (isSecretVisible) secret else "••••••••", fontFamily = FontFamily.Monospace)
                    Row {
                        IconButton(onClick = { isSecretVisible = !isSecretVisible }) {
                            Icon(imageVector = if (isSecretVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null)
                        }
                        IconButton(onClick = {
                            clipboardManager.setText(AnnotatedString(secret))
                            onCopied()
                        }) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null)
                        }
                    }
                }
            }

            // Save / Cancel Action buttons
            if (isConfirmed == null && (actionType == "SAVE_VAULT" || actionType == "ADD_TASK")) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onReject,
                        modifier = Modifier.testTag("action_cancel_button")
                    ) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onConfirm(selectedCategory, editableTitle, editableSecret, editableUser)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ToriCyan, contentColor = Color(0xFF00363D)),
                        modifier = Modifier.testTag("action_confirm_button")
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save")
                    }
                }
            }
        }
    }
}
