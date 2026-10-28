package com.example.feature.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

/**
 * Expandable Creation & AI Tools Bottom Sheet inspired by Dola AI (Left screen in reference).
 * Provides creation modes, reference inputs, model & style configuration pills, and prompt execution.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreationToolSheet(
    onDismiss: () -> Unit,
    onExecuteAction: (mode: String, prompt: String) -> Unit,
    onScanClipboard: () -> String?
) {
    var selectedTab by remember { mutableStateOf("AI Creation") } // AI Creation, Vision / Scan, Translate, Vault Draft
    var promptInput by remember { mutableStateOf("") }
    var selectedModel by remember { mutableStateOf("Gemini 3.5 Flash") }
    var selectedStyle by remember { mutableStateOf("Adaptive") }
    var referenceText by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = Color(0xF50F1117),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(42.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0x44FFFFFF))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row: Tab Pills + Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    listOf("AI Creation", "Translate", "Vault Draft").forEach { tab ->
                        val isSelected = selectedTab == tab
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) Color(0x33A855F7) else Color(0x1AFFFFFF),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) ToriNeonViolet else Color(0x22FFFFFF)
                            ),
                            modifier = Modifier.clickable { selectedTab = tab }
                        ) {
                            Text(
                                text = tab,
                                color = if (isSelected) Color.White else Color.Gray,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0x22FFFFFF))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Reference Slot Box with Dashed Border
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        val clip = onScanClipboard()
                        if (!clip.isNullOrBlank()) {
                            referenceText = clip
                            if (promptInput.isBlank()) promptInput = clip
                        }
                    },
                shape = RoundedCornerShape(16.dp),
                color = Color(0x221E2330),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x448B5CF6))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x33A855F7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DocumentScanner,
                            contentDescription = "Scan or Reference",
                            tint = ToriCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (referenceText != null) "Active Reference Attached" else "Scan Screen / Paste Reference",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Text(
                            text = referenceText?.take(36)?.plus("...") ?: "Tap to pull latest clipboard or screen content",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            maxLines = 1
                        )
                    }
                    if (referenceText != null) {
                        IconButton(onClick = { referenceText = null }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear Reference", tint = Color.Gray, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Configuration Pills Row (Model, Style, Ratio)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Model pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0x221E2330),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF)),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = ToriCyan, modifier = Modifier.size(14.dp))
                        Text(text = selectedModel, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Medium, maxLines = 1)
                    }
                }

                // Style pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0x221E2330),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF)),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = ToriAccentPurple, modifier = Modifier.size(14.dp))
                        Text(text = selectedStyle, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Medium, maxLines = 1)
                    }
                }
            }

            // Description / Prompt Input Box
            OutlinedTextField(
                value = promptInput,
                onValueChange = { promptInput = it },
                placeholder = {
                    Text(
                        text = when (selectedTab) {
                            "Translate" -> "Enter text to translate into Bangla or English..."
                            "Vault Draft" -> "Enter service name and password to store (e.g. Netflix: pass123)..."
                            else -> "Describe what you want Tori to create or answer..."
                        },
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .testTag("tool_sheet_prompt_input"),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0x1F121622),
                    unfocusedContainerColor = Color(0x1F121622),
                    focusedBorderColor = ToriNeonViolet,
                    unfocusedBorderColor = Color(0x33FFFFFF),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            // Primary Full-Width Action Button ("Generate Now" / "Execute")
            Button(
                onClick = {
                    if (promptInput.isNotBlank()) {
                        val promptToExecute = if (selectedTab == "Translate") "Translate: $promptInput" else promptInput
                        onExecuteAction(selectedTab, promptToExecute)
                        onDismiss()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("tool_sheet_generate_button"),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    ToriNeonViolet,
                                    ToriMagenta,
                                    ToriElectricPink
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (selectedTab == "Translate") "Translate Now" else "Generate Now",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
