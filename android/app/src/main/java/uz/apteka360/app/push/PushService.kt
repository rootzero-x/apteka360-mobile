package uz.apteka360.app.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class PushService : FirebaseMessagingService() {
    override fun onNewToken(token: String) { PushTokens.onNewToken(applicationContext, token) }
    override fun onMessageReceived(message: RemoteMessage) { PushTokens.show(applicationContext, message) }
}
