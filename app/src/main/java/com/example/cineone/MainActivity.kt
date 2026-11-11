package com.example.cineone

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.example.cineone.core.common.Resource
import com.example.cineone.core.notification.AppNotificationHelper
import com.example.cineone.core.notification.NotificationEventBus
import com.example.cineone.navigation.AppNavGraph
import com.example.cineone.navigation.Screen
import com.example.cineone.ui.theme.AppThemePreferences
import com.example.cineone.ui.theme.CineBackgroundDark
import com.example.cineone.ui.theme.CineOneTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private val pendingNotificationId = mutableLongStateOf(-1L)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppThemePreferences.init(applicationContext)
        AppNotificationHelper.createChannel(applicationContext)
        extractNotificationExtra(intent)

        setContent {
            val navController = rememberNavController()
            val sessionManager = remember { CineOneApp.instance.networkModule.sessionManager }
            val notificationRepo = remember { CineOneApp.instance.repositoryModule.notificationRepository }
            val startDestination = remember {
                if (sessionManager.isLoggedIn()) Screen.Home.route else Screen.Login.route
            }

            // Xin quyền Push Notification cho Android 13+
            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val granted = ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                    if (!granted) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            // Điều hướng thẳng vào trang chi tiết nếu người dùng bấm từ Push Notification
            val targetNotifId by pendingNotificationId
            LaunchedEffect(targetNotifId) {
                if (targetNotifId > 0L && sessionManager.isLoggedIn()) {
                    navController.navigate(Screen.NotificationDetail.createRoute(targetNotifId))
                    pendingNotificationId.longValue = -1L
                }
            }

            // Đăng ký topic FCM + đồng bộ thông báo mới mỗi 10s để bắn Push Notification
            LaunchedEffect(Unit) {
                var subscribed = false
                while (true) {
                    if (sessionManager.isLoggedIn()) {
                        if (!subscribed) {
                            val uid = sessionManager.fetchUserId()
                            if (uid > 0L) {
                                AppNotificationHelper.subscribeUserTopic(applicationContext, uid)
                                subscribed = true
                            }
                        }
                        notificationRepo.getMyNotifications().collect { res ->
                            if (res is Resource.Success) {
                                AppNotificationHelper.syncNotificationsAndPush(applicationContext, res.data)
                                NotificationEventBus.updateUnreadCount(res.data.count { !it.isRead })
                                NotificationEventBus.notifyRefresh()
                            }
                        }
                    } else {
                        subscribed = false
                        NotificationEventBus.updateUnreadCount(0)
                    }
                    delay(10_000L)
                }
            }

            CineOneTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = CineBackgroundDark
                ) {
                    AppNavGraph(
                        navController = navController,
                        startDestination = startDestination
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractNotificationExtra(intent)
    }

    private fun extractNotificationExtra(intent: Intent?) {
        val id = intent?.getLongExtra(AppNotificationHelper.EXTRA_NOTIFICATION_ID, -1L) ?: -1L
        if (id > 0L) {
            pendingNotificationId.longValue = id
        }
    }
}