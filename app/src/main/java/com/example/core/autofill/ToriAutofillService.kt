package com.example.core.autofill

import android.app.assist.AssistStructure
import android.os.Build
import android.os.CancellationSignal
import android.service.autofill.*
import android.util.Log
import android.view.View
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import com.example.R
import com.example.core.data.ToriDatabase
import com.example.core.data.ToriRepository
import com.example.core.data.VaultCategory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
class ToriAutofillService : AutofillService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onFillRequest(
        request: FillRequest,
        cancellationSignal: CancellationSignal,
        callback: FillCallback
    ) {
        val contexts = request.fillContexts
        val structure = contexts.lastOrNull()?.structure
        if (structure == null) {
            callback.onSuccess(null)
            return
        }

        val appPackage = structure.activityComponent?.packageName ?: ""
        val parsedFields = parseStructure(structure)

        if (parsedFields.passwordId == null && parsedFields.usernameId == null) {
            callback.onSuccess(null)
            return
        }

        serviceScope.launch {
            try {
                val db = ToriDatabase.getInstance(applicationContext)
                val repository = ToriRepository(db)
                val allEntries: List<com.example.core.data.VaultEntry> = db.vaultEntryDao().getAllSnapshot()

                // Filter for matching entries by package or domain or title
                val webDomain = parsedFields.webDomain?.lowercase() ?: ""
                val targetSearch = if (webDomain.isNotBlank()) webDomain else appPackage.lowercase()

                val matchedEntries = allEntries.filter { entry ->
                    val titleMatch = entry.title.lowercase().contains(cleanTargetName(targetSearch)) ||
                            targetSearch.contains(entry.title.lowercase())
                    val extraMatch = !entry.extraField.isNullOrBlank() &&
                            (entry.extraField.lowercase().contains(webDomain) || targetSearch.contains(entry.extraField.lowercase()))
                    titleMatch || extraMatch
                }.ifEmpty {
                    // If no direct domain match, provide top 3 most recently updated passwords
                    allEntries.take(3)
                }

                if (matchedEntries.isEmpty()) {
                    callback.onSuccess(null)
                    return@launch
                }

                val responseBuilder = FillResponse.Builder()
                val currentUsernameId = parsedFields.usernameId
                val currentPasswordId = parsedFields.passwordId

                for (entry in matchedEntries) {
                    val presentation = RemoteViews(packageName, R.layout.autofill_dataset_item)
                    val displayTitle = "${entry.title} (${entry.username ?: "Account"})"
                    presentation.setTextViewText(R.id.autofill_title, displayTitle)
                    presentation.setTextViewText(R.id.autofill_subtitle, "Tori • Tap to fill credentials")

                    val datasetBuilder = Dataset.Builder(presentation)
                    var hasDataSetValue = false

                    if (currentUsernameId != null && !entry.username.isNullOrBlank()) {
                        datasetBuilder.setValue(
                            currentUsernameId,
                            AutofillValue.forText(entry.username)
                        )
                        hasDataSetValue = true
                    }

                    if (currentPasswordId != null) {
                        val decrypted = repository.decryptSecret(entry.secret)
                        datasetBuilder.setValue(
                            currentPasswordId,
                            AutofillValue.forText(decrypted)
                        )
                        hasDataSetValue = true
                    }

                    if (hasDataSetValue) {
                        responseBuilder.addDataset(datasetBuilder.build())
                    }
                }

                // Register SaveInfo so Android prompts to save newly entered passwords
                val saveType = SaveInfo.SAVE_DATA_TYPE_PASSWORD or SaveInfo.SAVE_DATA_TYPE_USERNAME
                val requiredIds = mutableListOf<AutofillId>()
                if (currentPasswordId != null) requiredIds.add(currentPasswordId)

                if (requiredIds.isNotEmpty()) {
                    val saveInfoBuilder = SaveInfo.Builder(saveType, requiredIds.toTypedArray())
                    if (currentUsernameId != null) {
                        saveInfoBuilder.setOptionalIds(arrayOf(currentUsernameId))
                    }
                    saveInfoBuilder.setDescription("Save to Tori Vault")
                    responseBuilder.setSaveInfo(saveInfoBuilder.build())
                }

                callback.onSuccess(responseBuilder.build())
            } catch (e: Exception) {
                Log.e("ToriAutofill", "Fill request error", e)
                callback.onSuccess(null)
            }
        }
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        val contexts = request.fillContexts
        val structure = contexts.lastOrNull()?.structure
        if (structure == null) {
            callback.onSuccess()
            return
        }

        val appPackage = structure.activityComponent?.packageName ?: "Unknown App"
        val parsed = parseStructure(structure)

        val usernameVal = parsed.usernameValue
        val passwordVal = parsed.passwordValue

        if (passwordVal.isNullOrBlank() && usernameVal.isNullOrBlank()) {
            callback.onSuccess()
            return
        }

        val siteName = parsed.webDomain?.ifBlank { null } ?: cleanPackageToTitle(appPackage)

        serviceScope.launch {
            try {
                val db = ToriDatabase.getInstance(applicationContext)
                val repository = ToriRepository(db)

                repository.saveVaultEntry(
                    category = VaultCategory.PASSWORDS.name,
                    title = siteName,
                    username = usernameVal?.ifBlank { null },
                    plainSecret = passwordVal ?: "",
                    extraField = parsed.webDomain ?: appPackage,
                    notes = "Captured automatically via Android Autofill"
                )
                callback.onSuccess()
            } catch (e: Exception) {
                Log.e("ToriAutofill", "Save request error", e)
                callback.onFailure("Failed to save credentials to Tori")
            }
        }
    }

    private fun cleanTargetName(name: String): String {
        return name.replace("https://", "")
            .replace("http://", "")
            .replace("www.", "")
            .substringBefore("/")
            .substringBefore(".")
    }

    private fun cleanPackageToTitle(pkg: String): String {
        val lastPart = pkg.substringAfterLast(".")
        return lastPart.replaceFirstChar { it.uppercase() }
    }

    private data class ParsedFields(
        var usernameId: AutofillId? = null,
        var passwordId: AutofillId? = null,
        var usernameValue: String? = null,
        var passwordValue: String? = null,
        var webDomain: String? = null
    )

    private fun parseStructure(structure: AssistStructure): ParsedFields {
        val parsed = ParsedFields()
        val windowCount = structure.windowNodeCount
        for (w in 0 until windowCount) {
            val windowNode = structure.getWindowNodeAt(w)
            traverseNode(windowNode.rootViewNode, parsed)
        }
        return parsed
    }

    private fun traverseNode(node: AssistStructure.ViewNode, parsed: ParsedFields) {
        if (node.webDomain != null && parsed.webDomain == null) {
            parsed.webDomain = node.webDomain
        }

        val hints = node.autofillHints
        val idEntry = node.idEntry?.lowercase() ?: ""
        val hintText = node.hint?.lowercase() ?: ""

        val isPasswordHint = hints?.any { it.contains("password", ignoreCase = true) } == true ||
                idEntry.contains("password") || idEntry.contains("pass") ||
                hintText.contains("password") || hintText.contains("pin")

        val isUsernameHint = hints?.any {
            it.contains("username", ignoreCase = true) ||
                    it.contains("email", ignoreCase = true)
        } == true ||
                idEntry.contains("username") || idEntry.contains("email") || idEntry.contains("login") ||
                hintText.contains("username") || hintText.contains("email")

        val autofillId = node.autofillId
        val textValue = node.text?.toString()

        if (autofillId != null) {
            if (isPasswordHint && parsed.passwordId == null) {
                parsed.passwordId = autofillId
                parsed.passwordValue = textValue
            } else if (isUsernameHint && parsed.usernameId == null) {
                parsed.usernameId = autofillId
                parsed.usernameValue = textValue
            }
        }

        val childCount = node.childCount
        for (c in 0 until childCount) {
            traverseNode(node.getChildAt(c), parsed)
        }
    }
}
