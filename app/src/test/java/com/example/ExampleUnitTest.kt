package com.example

import com.example.core.security.CryptoManager
import com.example.feature.vault.PasswordGenerator
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleUnitTest {
    @Test
    fun testPasswordGeneratorLengthAndStrength() {
        val password = PasswordGenerator.generate(length = 20)
        assertEquals(20, password.length)

        val strength = PasswordGenerator.calculateStrength(password)
        assertTrue(strength.entropyBits > 60)
        assertTrue(strength.scorePercent >= 0.7f)
    }

    @Test
    fun testPassphraseEncryptionAndDecryption() {
        val originalPayload = "{\"app\":\"Tori\",\"vaultItems\":[{\"title\":\"Netflix\",\"secret\":\"pass123\"}]}"
        val passphrase = "MySuperSecretMasterPassphrase123!"

        val encryptedB64 = CryptoManager.encryptWithPassphrase(originalPayload, passphrase)
        assertNotNull(encryptedB64)
        assertNotEquals(originalPayload, encryptedB64)

        val decrypted = CryptoManager.decryptWithPassphrase(encryptedB64, passphrase)
        assertEquals(originalPayload, decrypted)
    }

    @Test
    fun testBackupManagerCsvExportAndImport() {
        val entry = com.example.core.data.VaultEntry(
            id = 1,
            category = "PASSWORDS",
            title = "Facebook",
            username = "rony@gmail.com",
            secret = "plain_pass_123"
        )
        val csv = com.example.core.data.BackupManager.exportToCsv(
            vaultEntries = listOf(entry),
            decryptSecret = { it }
        )
        assertTrue(csv.contains("Facebook"))
        assertTrue(csv.contains("rony@gmail.com"))
        assertTrue(csv.contains("plain_pass_123"))

        val preview = com.example.core.data.BackupManager.parseImportContent(csv)
        assertEquals(1, preview.vaultDrafts.size)
        assertEquals("Facebook", preview.vaultDrafts[0].title)
        assertEquals("rony@gmail.com", preview.vaultDrafts[0].username)
        assertEquals("plain_pass_123", preview.vaultDrafts[0].secret)
    }

    @Test
    fun testBackupManagerTxtExportAndImport() {
        val entry = com.example.core.data.VaultEntry(
            id = 2,
            category = "ACCOUNTS",
            title = "Google Cloud",
            username = "dev@workspace.com",
            secret = "dev_secret_456"
        )
        val txt = com.example.core.data.BackupManager.exportToTxt(
            vaultEntries = listOf(entry),
            tasks = emptyList(),
            decryptSecret = { it }
        )
        assertTrue(txt.contains("Google Cloud"))
        assertTrue(txt.contains("dev@workspace.com"))

        val preview = com.example.core.data.BackupManager.parseImportContent(txt)
        assertEquals(1, preview.vaultDrafts.size)
        assertEquals("Google Cloud", preview.vaultDrafts[0].title)
        assertEquals("dev_secret_456", preview.vaultDrafts[0].secret)
    }

    @Test
    fun testSecurityPreferencesCategoryTogglesAndGeminiKey() {
        val context = org.robolectric.RuntimeEnvironment.getApplication()
        val prefs = com.example.core.security.SecurityPreferences(context)

        // Verify defaults
        assertTrue(prefs.autoDetectPasswords)
        assertTrue(prefs.autoDetectEmails)
        assertTrue(prefs.autoDetectApis)
        assertTrue(prefs.autoDetectPrompts)
        assertTrue(prefs.autoDetectSites)
        assertTrue(prefs.autoDetectOthers)
        assertFalse(prefs.isFloatingSidebarEnabled)
        assertEquals("", prefs.customGeminiApiKey)

        // Verify updates
        prefs.autoDetectApis = false
        assertFalse(prefs.autoDetectApis)

        prefs.customGeminiApiKey = "AIzaSyTestKey123"
        assertEquals("AIzaSyTestKey123", prefs.customGeminiApiKey)

        val db = com.example.core.data.ToriDatabase.getInstance(context)
        val repo = com.example.core.data.ToriRepository(db)
        val engine = com.example.feature.assistant.ToriAssistantEngine(repo, prefs)
        assertEquals("AIzaSyTestKey123", engine.getEffectiveApiKey())
    }

    @Test
    fun testGeminiServiceInitializationAndApiKeyResolution() {
        // Test custom key override
        val customService = com.example.core.network.GeminiService(customApiKey = "AIzaSyCustomKey999")
        assertEquals("AIzaSyCustomKey999", customService.getApiKey())
        assertTrue(customService.isApiKeyConfigured())

        // Test default BuildConfig key resolution
        val defaultService = com.example.core.network.GeminiService()
        assertNotNull(defaultService.getApiKey())
    }
}

