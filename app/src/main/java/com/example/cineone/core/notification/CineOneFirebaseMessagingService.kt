package com.example.cineone.core.notification

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class CineOneFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val data = remoteMessage.data
        val notifId = data["notificationId"]?.toLongOrNull() ?: System.currentTimeMillis()
        val title = data["title"] ?: remoteMessage.notification?.title ?: "Thông báo CineOne"
        val body = data["message"] ?: remoteMessage.notification?.body ?: ""
        val type = data["type"] ?: "SYSTEM"

        AppNotificationHelper.showPushNotification(
            context = applicationContext,
            notificationId = notifId,
            title = title,
            message = body,
            type = type
        )
        NotificationEventBus.notifyRefresh()
    }
}