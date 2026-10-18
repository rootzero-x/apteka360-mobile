package uz.apteka360.app.push

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.RemoteMessage
import uz.apteka360.app.R

/**
 * Push uchun joy. FCM tokeni kelganda hozircha logga yoziladi;
 * backend endpointi kelishilgach shu yerdan API'ga yuboriladi (README → Push).
 */
object PushTokens {
    private const val CHANNEL = "apteka360_general"

    fun onNewToken(ctx: Context, token: String) {
        Log.i("Push", "FCM token: ${token.take(12)}…")
        ctx.getSharedPreferences("push", Context.MODE_PRIVATE).edit().putString("fcmToken", token).apply()
    }

    fun show(ctx: Context, msg: RemoteMessage) {
        val title = msg.notification?.title ?: msg.data["title"] ?: "Apteka 360"
        val body = msg.notification?.body ?: msg.data["text"] ?: return
        val nm = NotificationManagerCompat.from(ctx)
        nm.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL, NotificationManagerCompat.IMPORTANCE_HIGH)
                .setName("Bildirishnomalar").build()
        )
        if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            && android.os.Build.VERSION.SDK_INT >= 33) return
        nm.notify(System.currentTimeMillis().toInt(), NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title).setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true).build())
    }
}
