package com.example.cineone.core.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.cineone.MainActivity
import com.example.cineone.R
import com.example.cineone.domain.model.Notification
import com.example.cineone.ui.theme.AppThemePreferences
import com.google.firebase.Firebase
import com.google.firebase.messaging.messaging
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

object NotificationEventBus {
    private val _refreshEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 4)
    val refreshEvents = _refreshEvents.asSharedFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount = _unreadCount.asStateFlow()

    fun notifyRefresh() {
        _refreshEvents.tryEmit(Unit)
    }

    fun updateUnreadCount(count: Int) {
        _unreadCount.value = count.coerceAtLeast(0)
    }
}

object AppNotificationHelper {
    const val CHANNEL_ID = "cineone_notifications"
    const val EXTRA_NOTIFICATION_ID = "open_notification_id"
    private const val TRACKER_PREFS = "cineone_notif_tracker"
    private const val KEY_INITIALIZED = "tracker_initialized"
    private const val KEY_NOTIFIED_IDS = "notified_ids"
    private const val KEY_LAST_USER_ID = "last_topic_user_id"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Thông báo CineOne",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Nhận thông báo đặt vé, khuyến mãi và nhắc lịch chiếu phim"
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun subscribeUserTopic(context: Context, userId: Long) {
        if (userId <= 0L) return
        val prefs = context.getSharedPreferences(TRACKER_PREFS, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_LAST_USER_ID, userId).apply()
        runCatching {
            Firebase.messaging.subscribeToTopic("user_$userId")
        }
    }

    fun unsubscribeUserTopic(context: Context) {
        val prefs = context.getSharedPreferences(TRACKER_PREFS, Context.MODE_PRIVATE)
        val userId = prefs.getLong(KEY_LAST_USER_ID, -1L)
        if (userId > 0L) {
            runCatching {
                Firebase.messaging.unsubscribeFromTopic("user_$userId")
            }
        }
        prefs.edit().clear().apply()
        NotificationEventBus.updateUnreadCount(0)
    }

    fun syncNotificationsAndPush(context: Context, items: List<Notification>) {
        if (items.isEmpty()) return
        val tracker = context.getSharedPreferences(TRACKER_PREFS, Context.MODE_PRIVATE)
        val isInitialized = tracker.getBoolean(KEY_INITIALIZED, false)
        val notifiedIds = tracker.getStringSet(KEY_NOTIFIED_IDS, emptySet())?.toMutableSet() ?: mutableSetOf()

        if (!isInitialized) {
            // Lần đầu đồng bộ chỉ đánh dấu các thông báo đã có, tránh bắn hàng loạt thông báo cũ
            items.forEach { notifiedIds.add(it.notificationId.toString()) }
            tracker.edit()
                .putBoolean(KEY_INITIALIZED, true)
                .putStringSet(KEY_NOTIFIED_IDS, notifiedIds)
                .apply()
            return
        }

        val newItems = items
            .filter { !it.isRead && !notifiedIds.contains(it.notificationId.toString()) }
            .sortedBy { it.notificationId }

        newItems.forEach { item ->
            showPushNotification(
                context = context,
                notificationId = item.notificationId,
                title = item.title,
                message = item.message,
                type = item.type
            )
        }
    }

    fun showPushNotification(
        context: Context,
        notificationId: Long,
        title: String,
        message: String,
        type: String
    ) {
        if (!shouldDisplayForType(context, type)) return

        val tracker = context.getSharedPreferences(TRACKER_PREFS, Context.MODE_PRIVATE)
        val notifiedIds = tracker.getStringSet(KEY_NOTIFIED_IDS, emptySet())?.toMutableSet() ?: mutableSetOf()
        if (notificationId > 0L && notifiedIds.contains(notificationId.toString())) {
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        createChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (notificationId > 0L) {
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            }
        }
        val requestCode = if (notificationId > 0L) notificationId.toInt() else System.currentTimeMillis().toInt()
        val pendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title.ifBlank { "Thông báo CineOne" })
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        runCatching {
            NotificationManagerCompat.from(context).notify(requestCode, builder.build())
            if (notificationId > 0L) {
                notifiedIds.add(notificationId.toString())
                tracker.edit()
                    .putBoolean(KEY_INITIALIZED, true)
                    .putStringSet(KEY_NOTIFIED_IDS, notifiedIds)
                    .apply()
            }
        }
    }

    private fun shouldDisplayForType(context: Context, type: String): Boolean {
        val settings = context.getSharedPreferences(AppThemePreferences.PREFS_NAME, Context.MODE_PRIVATE)
        if (!settings.getBoolean("notif_push", true)) return false
        return when (type.trim().uppercase()) {
            "PROMOTION" -> settings.getBoolean("notif_promo", true)
            "REMINDER" -> settings.getBoolean("notif_reminder", true)
            else -> true
        }
    }
}