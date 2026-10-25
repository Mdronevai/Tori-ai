package com.example.core.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.core.data.ToriDatabase
import com.example.core.data.ToriRepository
import com.example.core.security.SecurityPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class ToriBackgroundService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var clipboardManager: ClipboardManager
    private lateinit var repository: ToriRepository
    private lateinit var securityPrefs: SecurityPreferences
    private var lastClipText: String = ""

    private val clipListener = ClipboardManager.OnPrimaryClipChangedListener {
        onClipboardChanged()
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        startForeground(NOTIFICATION_ID_FOREGROUND, buildForegroundNotification())

        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val db = ToriDatabase.getInstance(applicationContext)
        repository = ToriRepository(db)
        securityPrefs = SecurityPreferences(applicationContext)

        clipboardManager.addPrimaryClipChangedListener(clipListener)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun onClipboardChanged() {
        serviceScope.launch {
            try {
                val clipData = clipboardManager.primaryClip ?: return@launch
                if (clipData.itemCount == 0) return@launch
                val item = clipData.getItemAt(0)
                val text = item.text?.toString()?.trim() ?: return@launch

                if (text.isEmpty() || text == lastClipText) return@launch
                lastClipText = text

                val lower = text.lowercase()
                val isPassword = text.length in 6..40 && text.any { it.isDigit() } && text.any { !it.isLetterOrDigit() }
                val isEmail = text.contains("@") && text.contains(".") && !text.contains(" ")
                val isApi = lower.contains("api") || lower.contains("sk_live") || lower.contains("token") || lower.contains("key_")
                val isPrompt = lower.startsWith("prompt:") || lower.contains("generate ")
                val isSite = text.startsWith("http://") || text.startsWith("https://") || text.startsWith("www.")

                val shouldSave = when {
                    isPassword -> securityPrefs.autoDetectPasswords
                    isEmail -> securityPrefs.autoDetectEmails
                    isApi -> securityPrefs.autoDetectApis
                    isPrompt -> securityPrefs.autoDetectPrompts
                    isSite -> securityPrefs.autoDetectSites
                    else -> securityPrefs.autoDetectOthers
                }

                if (shouldSave) {
                    repository.saveClipboardEntry(
                        content = text,
                        siteOrAppName = "Clipboard Auto-Detect"
                    )

                    if (isPassword || isEmail || isApi) {
                        val category = when {
                            isPassword -> "PASSWORDS"
                            isEmail -> "ACCOUNTS"
                            else -> "SITES_APIS"
                        }
                        repository.saveVaultEntry(
                            category = category,
                            title = if (isEmail) "Auto-Detected Email" else if (isPassword) "Auto-Detected Password" else "Auto-Detected Key",
                            username = if (isEmail) text else null,
                            plainSecret = text,
                            notes = "Auto-captured via Tori Background Listener"
                        )
                    }

                    showSaveNotification(
                        title = "Password Saved Securely",
                        message = if (isPassword) "Your password was automatically saved to Tori Vault." else "Copied item categorized & saved to Vault."
                    )
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val foregroundChannel = NotificationChannel(
                CHANNEL_ID_FOREGROUND,
                "Tori Active Monitor",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows persistent background status of Tori Assistant."
            }

            val alertChannel = NotificationChannel(
                CHANNEL_ID_ALERTS,
                "Tori Password & Vault Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies when credentials or passwords are saved."
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(foregroundChannel)
            manager.createNotificationChannel(alertChannel)
        }
    }

    private fun buildForegroundNotification() =
        NotificationCompat.Builder(this, CHANNEL_ID_FOREGROUND)
            .setContentTitle("Tori Vault Active")
            .setContentText("Background credential auto-detection & clipboard vault running.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(createPendingIntent())
            .build()

    private fun showSaveNotification(title: String, message: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID_ALERTS)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(createPendingIntent())
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun createPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            clipboardManager.removePrimaryClipChangedListener(clipListener)
        } catch (_: Exception) {
        }
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_ID_FOREGROUND = "tori_foreground_channel"
        const val CHANNEL_ID_ALERTS = "tori_credential_channel"
        const val NOTIFICATION_ID_FOREGROUND = 9001

        fun start(context: Context) {
            val intent = Intent(context, ToriBackgroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, ToriBackgroundService::class.java)
            context.stopService(intent)
        }
    }
}
