package com.stickermaker.funnyemoji.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.stickermaker.funnyemoji.R
import com.stickermaker.funnyemoji.notifications.DraftNotifications

@Composable
internal fun NotificationSettingsRow() {
    val context = LocalContext.current
    val preferences = remember(context) { context.getSharedPreferences("notification-permission", 0) }
    var allowed by remember { mutableStateOf(DraftNotifications.isAllowed(context)) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        allowed = DraftNotifications.isAllowed(context)
    }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) allowed = DraftNotifications.isAllowed(context)
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    SettingsRow(
        stringResource(R.string.notification_permission_label), R.drawable.ic_notification_bell, 20f, 20f,
        stringResource(if (allowed) R.string.notification_permission_allowed else R.string.notification_permission_off),
    ) {
        val needsPermission = Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS,
        ) != PackageManager.PERMISSION_GRANTED
        if (needsPermission && !preferences.getBoolean("requested", false)) {
            preferences.edit().putBoolean("requested", true).apply()
            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            // After a denial, let the user enable notifications explicitly in Android settings.
            val appBlocked = !NotificationManagerCompat.from(context).areNotificationsEnabled()
            val intent = Intent(if (appBlocked) Settings.ACTION_APP_NOTIFICATION_SETTINGS
                else Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            if (!appBlocked) intent.putExtra(Settings.EXTRA_CHANNEL_ID, DraftNotifications.CHANNEL_ID)
            context.startActivity(intent)
        }
    }
}
